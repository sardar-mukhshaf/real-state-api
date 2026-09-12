package com.realestate.users.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.users.application.LandlordRepository;
import com.realestate.users.domain.Landlord;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class LandlordAdapter extends JpaStore<Landlord, LandlordEntity>
        implements LandlordRepository {
    public LandlordAdapter(LandlordJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                LandlordEntity.class,
                LandlordMapper::toDomain,
                LandlordMapper::toEntity);
    }
}
