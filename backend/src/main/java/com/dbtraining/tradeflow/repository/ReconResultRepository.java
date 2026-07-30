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
 *
 *  TODO(TICKET-I061):
 *    - findByStatus(String status)
 *    - @Query for findUnresolvedByCounterparty(Long counterpartyId)
 *
 *  HINT for the JOIN query:
 *    @Query("""
 *      select r from ReconResult r
 *        join r.trade t
 *      where r.status = 'OPEN' and t.counterpartyId = :cp
 *    """)
 *    List<ReconResult> findUnresolvedByCounterparty(@Param("cp") Long counterpartyId);
 * ============================================================================
 */
@Repository
public interface ReconResultRepository extends JpaRepository<ReconResult, Long> {

    List<ReconResult> findByStatus(ReconResult.Status status);

    long countByStatus(ReconResult.Status status);

    List<ReconResult> findByTradeId(Long tradeId);

    @Query("""
           select r from ReconResult r
             join r.trade t
           where r.status = com.dbtraining.tradeflow.model.ReconResult$Status.OPEN
             and t.counterparty.id = :counterpartyId
           """)
    List<ReconResult> findUnresolvedByCounterparty(@Param("counterpartyId") Long counterpartyId);
}
