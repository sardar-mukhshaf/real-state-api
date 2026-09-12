package com.realestate.uploads.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDocumentJpaRepository
        extends JpaRepository<PropertyDocumentEntity, String> {}
