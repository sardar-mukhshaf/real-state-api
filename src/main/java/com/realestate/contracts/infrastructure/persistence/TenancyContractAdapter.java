package com.realestate.contracts.infrastructure.persistence;

import com.realestate.contracts.application.TenancyContractRepository;
import com.realestate.contracts.domain.TenancyContract;
import com.realestate.shared.infrastructure.persistence.JpaStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class TenancyContractAdapter extends JpaStore<TenancyContract, TenancyContractEntity>
        implements TenancyContractRepository {
    public TenancyContractAdapter(TenancyContractJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                TenancyContractEntity.class,
                TenancyContractMapper::toDomain,
                TenancyContractMapper::toEntity);
    }
}
