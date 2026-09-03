package io.github.sweetpark.apm.sample.controller;

import io.github.sweetpark.apm.sample.dto.CreateAuthorRequest;
import io.github.sweetpark.apm.sample.entity.Author;
import io.github.sweetpark.apm.sample.repository.AuthorRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo endpoints for mini-apm-spring-boot-starter. Each one is designed to light up a specific
 * line in the APM log output -- see docs/examples/sample-app/README.md for what to look for.
 */
@RestController
public class AuthorController {

  private final AuthorRepository authorRepository;

  public AuthorController(AuthorRepository authorRepository) {
    this.authorRepository = authorRepository;
  }

  /** A single, fast query -- shows up as a normal [HTTP] + [SQL] log line. */
  @GetMapping("/api/authors")
  public List<Author> listAuthors() {
    return authorRepository.findAll();
  }

  /**
   * Loads all authors, then lazily touches each author's book collection in a loop. Each
   * iteration issues its own SELECT, so the same sql_id repeats past
   * apm.limit.n1-detection-threshold and mini-apm logs a [N1_QUERY] warning.
   */
  @GetMapping("/api/authors/n-plus-one")
  @Transactional
  public List<Map<String, Object>> listAuthorsNPlusOne() {
    return authorRepository.findAll().stream()
        .map(author -> Map.<String, Object>of("author", author.getName(), "bookCount", author.getBooks().size()))
        .collect(Collectors.toList());
  }

  /** Deliberately sleeps past apm.slow.api-ms so the request is logged as a slow API call. */
  @GetMapping("/api/authors/slow")
  public Map<String, String> slowEndpoint() throws InterruptedException {
    Thread.sleep(1200);
    return Map.of("status", "done");
  }

  /** Always throws, to show [EXCEPTION] logging with the SHA-256 error fingerprint. */
  @GetMapping("/api/authors/{id}/error")
  public Author triggerError(@PathVariable Long id) {
    return authorRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No such author: " + id));
  }

  /**
   * The request body contains an email and phone number; with apm.security.masking-enabled=true
   * (the default), both are masked in the [HTTP_DETAIL] log line.
   */
  @PostMapping("/api/authors")
  public Author createAuthor(@RequestBody CreateAuthorRequest request) {
    Author author = new Author(request.getName(), request.getEmail());
    return authorRepository.save(author);
  }
}
