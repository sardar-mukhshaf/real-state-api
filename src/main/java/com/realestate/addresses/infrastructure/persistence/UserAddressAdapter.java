package com.realestate.addresses.infrastructure.persistence;

import com.realestate.addresses.application.UserAddressRepository;
import com.realestate.addresses.domain.UserAddress;
import com.realestate.shared.infrastructure.persistence.JpaStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class UserAddressAdapter extends JpaStore<UserAddress, UserAddressEntity>
        implements UserAddressRepository {
    public UserAddressAdapter(UserAddressJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                UserAddressEntity.class,
                UserAddressMapper::toDomain,
                UserAddressMapper::toEntity);
    }
}
