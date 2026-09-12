package com.realestate.uploads.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.uploads.application.PropertyImageRepository;
import com.realestate.uploads.domain.PropertyImage;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyImageAdapter extends JpaStore<PropertyImage, PropertyImageEntity>
        implements PropertyImageRepository {
    public PropertyImageAdapter(PropertyImageJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                PropertyImageEntity.class,
                PropertyImageMapper::toDomain,
                PropertyImageMapper::toEntity);
    }
}
