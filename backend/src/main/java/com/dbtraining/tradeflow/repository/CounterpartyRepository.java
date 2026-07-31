package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {

    Optional<Counterparty> findByLeiCode(String leiCode);
}
