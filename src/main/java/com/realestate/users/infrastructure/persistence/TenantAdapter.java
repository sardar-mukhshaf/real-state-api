package com.realestate.users.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.users.application.TenantRepository;
import com.realestate.users.domain.Tenant;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class TenantAdapter extends JpaStore<Tenant, TenantEntity> implements TenantRepository {
    public TenantAdapter(TenantJpaRepository repository, EntityManager em) {
        super(repository, em, TenantEntity.class, TenantMapper::toDomain, TenantMapper::toEntity);
    }
}
