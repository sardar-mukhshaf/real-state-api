package com.realestate.uploads.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.uploads.application.PropertyDocumentRepository;
import com.realestate.uploads.domain.PropertyDocument;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyDocumentAdapter extends JpaStore<PropertyDocument, PropertyDocumentEntity>
        implements PropertyDocumentRepository {
    public PropertyDocumentAdapter(PropertyDocumentJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                PropertyDocumentEntity.class,
                PropertyDocumentMapper::toDomain,
                PropertyDocumentMapper::toEntity);
    }
}
