package com.realestate.transactions.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDetailJpaRepository extends JpaRepository<PaymentDetailEntity, String> {}
