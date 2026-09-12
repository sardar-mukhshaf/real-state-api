package com.realestate.contracts.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenancyContractJpaRepository
        extends JpaRepository<TenancyContractEntity, String> {}
