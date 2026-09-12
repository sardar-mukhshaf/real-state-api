package com.realestate.addresses.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyAddressJpaRepository
        extends JpaRepository<PropertyAddressEntity, String> {}
