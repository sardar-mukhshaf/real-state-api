package com.realestate.addresses.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressJpaRepository extends JpaRepository<UserAddressEntity, String> {}
