package com.realestate.companies.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, String> {}
