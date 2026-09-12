package com.realestate.transactions.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseDetailJpaRepository extends JpaRepository<ExpenseDetailEntity, String> {}
