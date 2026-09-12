package com.realestate.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionEntity, String> {}
