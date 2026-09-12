package com.realestate.transactions.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, String> {}
