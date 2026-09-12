package com.realestate.transactions.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.transactions.application.RentDetailRepository;
import com.realestate.transactions.domain.RentDetail;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class RentDetailAdapter extends JpaStore<RentDetail, RentDetailEntity>
        implements RentDetailRepository {
    public RentDetailAdapter(RentDetailJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                RentDetailEntity.class,
                RentDetailMapper::toDomain,
                RentDetailMapper::toEntity);
    }
}
