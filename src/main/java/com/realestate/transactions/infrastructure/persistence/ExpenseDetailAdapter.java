package com.realestate.transactions.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.transactions.application.ExpenseDetailRepository;
import com.realestate.transactions.domain.ExpenseDetail;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class ExpenseDetailAdapter extends JpaStore<ExpenseDetail, ExpenseDetailEntity>
        implements ExpenseDetailRepository {
    public ExpenseDetailAdapter(ExpenseDetailJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                ExpenseDetailEntity.class,
                ExpenseDetailMapper::toDomain,
                ExpenseDetailMapper::toEntity);
    }
}
