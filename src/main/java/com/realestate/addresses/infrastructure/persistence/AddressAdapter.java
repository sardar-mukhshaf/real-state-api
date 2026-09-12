package com.realestate.addresses.infrastructure.persistence;

import com.realestate.addresses.application.AddressRepository;
import com.realestate.addresses.domain.Address;
import com.realestate.shared.infrastructure.persistence.JpaStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class AddressAdapter extends JpaStore<Address, AddressEntity> implements AddressRepository {
    public AddressAdapter(AddressJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                AddressEntity.class,
                AddressMapper::toDomain,
                AddressMapper::toEntity);
    }
}
