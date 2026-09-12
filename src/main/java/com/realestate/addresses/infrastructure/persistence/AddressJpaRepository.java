package com.realestate.addresses.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, String> {}
