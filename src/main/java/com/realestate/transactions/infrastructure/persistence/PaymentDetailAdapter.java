package com.realestate.transactions.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.transactions.application.PaymentDetailRepository;
import com.realestate.transactions.domain.PaymentDetail;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentDetailAdapter extends JpaStore<PaymentDetail, PaymentDetailEntity>
        implements PaymentDetailRepository {
    public PaymentDetailAdapter(PaymentDetailJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                PaymentDetailEntity.class,
                PaymentDetailMapper::toDomain,
                PaymentDetailMapper::toEntity);
    }
}
