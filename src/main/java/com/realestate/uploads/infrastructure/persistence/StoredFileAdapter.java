package com.realestate.uploads.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.uploads.application.StoredFileRepository;
import com.realestate.uploads.domain.StoredFile;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class StoredFileAdapter extends JpaStore<StoredFile, StoredFileEntity>
        implements StoredFileRepository {
    public StoredFileAdapter(StoredFileJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                StoredFileEntity.class,
                StoredFileMapper::toDomain,
                StoredFileMapper::toEntity);
    }
}
