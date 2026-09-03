package io.github.sweetpark.apm.sample.repository;

import io.github.sweetpark.apm.sample.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {}
