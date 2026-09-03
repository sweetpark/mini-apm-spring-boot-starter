package io.github.sweetpark.apm.sample.repository;

import io.github.sweetpark.apm.sample.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {}
