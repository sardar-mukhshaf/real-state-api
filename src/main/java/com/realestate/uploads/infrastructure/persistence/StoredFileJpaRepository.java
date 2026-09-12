package com.realestate.uploads.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileJpaRepository extends JpaRepository<StoredFileEntity, String> {}
