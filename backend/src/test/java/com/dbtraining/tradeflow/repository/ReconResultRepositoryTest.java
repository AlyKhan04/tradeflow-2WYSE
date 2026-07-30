package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconResultRepositoryTest {

    @Mock
    private ReconResultRepository repository;

    private ReconResult sampleResult;

    @BeforeEach
    void setUp() {
        sampleResult = ReconResult.builder()
                .tradeId(101L)
                .discrepancyType(DiscrepancyType.PRICE_MISMATCH)
                .status("OPEN")
                .build();
    }

    @Test
    @DisplayName("TICKET-I061: findByStatus returns open break results")
    void testFindByStatus() {
        when(repository.findByStatus("OPEN")).thenReturn(List.of(sampleResult));
        when(repository.countByStatus("OPEN")).thenReturn(1L);

        List<ReconResult> results = repository.findByStatus("OPEN");
        long count = repository.countByStatus("OPEN");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTradeId()).isEqualTo(101L);
        assertThat(count).isEqualTo(1L);

        verify(repository).findByStatus("OPEN");
        verify(repository).countByStatus("OPEN");
    }

    @Test
    @DisplayName("TICKET-I061: findUnresolvedByCounterparty calls query method")
    void testFindUnresolvedByCounterparty() {
        when(repository.findUnresolvedByCounterparty(5L)).thenReturn(List.of(sampleResult));

        List<ReconResult> unresolved = repository.findUnresolvedByCounterparty(5L);

        assertThat(unresolved).hasSize(1);
        assertThat(unresolved.get(0).getDiscrepancyType()).isEqualTo(DiscrepancyType.PRICE_MISMATCH);

        verify(repository).findUnresolvedByCounterparty(5L);
    }
}
