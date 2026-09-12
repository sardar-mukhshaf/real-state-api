package com.realestate.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LandlordJpaRepository extends JpaRepository<LandlordEntity, String> {}
