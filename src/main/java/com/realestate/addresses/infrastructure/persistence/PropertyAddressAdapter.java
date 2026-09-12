package com.realestate.addresses.infrastructure.persistence;

import com.realestate.addresses.application.PropertyAddressRepository;
import com.realestate.addresses.domain.PropertyAddress;
import com.realestate.shared.infrastructure.persistence.JpaStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyAddressAdapter extends JpaStore<PropertyAddress, PropertyAddressEntity>
        implements PropertyAddressRepository {
    public PropertyAddressAdapter(PropertyAddressJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                PropertyAddressEntity.class,
                PropertyAddressMapper::toDomain,
                PropertyAddressMapper::toEntity);
    }
}
