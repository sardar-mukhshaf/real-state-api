package com.realestate.properties.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyJpaRepository extends JpaRepository<PropertyEntity, String> {}
