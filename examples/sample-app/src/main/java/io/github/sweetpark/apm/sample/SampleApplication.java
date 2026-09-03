package io.github.sweetpark.apm.sample;

import io.github.sweetpark.apm.sample.entity.Author;
import io.github.sweetpark.apm.sample.entity.Book;
import io.github.sweetpark.apm.sample.repository.AuthorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Runnable demo app for mini-apm-spring-boot-starter. Start it with {@code
 * ./gradlew :examples:sample-app:bootRun} and see docs/examples/sample-app/README.md for the
 * endpoints to try and what each one demonstrates in the APM log output.
 */
@SpringBootApplication
public class SampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(SampleApplication.class, args);
  }

  @Bean
  CommandLineRunner seedData(AuthorRepository authorRepository) {
    return args -> {
      for (int i = 1; i <= 5; i++) {
        Author author = new Author("Author " + i, "author" + i + "@example.com");
        for (int j = 1; j <= 3; j++) {
          author.addBook(new Book("Book " + i + "-" + j));
        }
        authorRepository.save(author);
      }
    };
  }
}
