package com.realestate.transactions.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.transactions.application.FeeDetailRepository;
import com.realestate.transactions.domain.FeeDetail;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class FeeDetailAdapter extends JpaStore<FeeDetail, FeeDetailEntity>
        implements FeeDetailRepository {
    public FeeDetailAdapter(FeeDetailJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                FeeDetailEntity.class,
                FeeDetailMapper::toDomain,
                FeeDetailMapper::toEntity);
    }
}
