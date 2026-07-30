package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.ReconResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ============================================================================
 * ReconResultRepository — TICKET-I061 (Day 5)
 * ============================================================================
 * WHAT:    JPA repository for ReconResult.
 * HOW:     extends JpaRepository<ReconResult, Long>.
 * ============================================================================
 */
@Repository
public interface ReconResultRepository extends JpaRepository<ReconResult, Long> {

    List<ReconResult> findByStatus(String status);

    long countByStatus(String status);

    List<ReconResult> findByTradeId(Long tradeId);

    @Query("""
           select r from ReconResult r
           where r.status = 'OPEN'
             and r.tradeId in (
                 select t.id from Trade t where t.counterpartyId = :counterpartyId
             )
           """)
    List<ReconResult> findUnresolvedByCounterparty(@Param("counterpartyId") Long counterpartyId);
}
