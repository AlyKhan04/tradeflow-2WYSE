package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.dto.ReconResultDto;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconControllerTest {

    @Mock
    private ReconciliationService reconService;

    @InjectMocks
    private ReconController reconController;

    @Test
    void run_returnsSummary() {
        ReconSummary expected = new ReconSummary(10, 10, 8, 2, Map.of(DiscrepancyType.PRICE_MISMATCH, 2));
        when(reconService.runForAll()).thenReturn(expected);

        ReconSummary summary = reconController.run();
        assertNotNull(summary);
        assertEquals(10, summary.totalInternal());
        assertEquals(8, summary.matchedCount());
    }

    @Test
    void listResults_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        ReconResultDto dto = new ReconResultDto(1L, 100L, "TRD-100", DiscrepancyType.PRICE_MISMATCH, ReconResult.Status.OPEN, Instant.now(), null);
        Page<ReconResultDto> expectedPage = new PageImpl<>(List.of(dto), pageable, 1);

        when(reconService.listBreaks(ReconResult.Status.OPEN, null, pageable)).thenReturn(expectedPage);

        Page<ReconResultDto> resultPage = reconController.listResults("OPEN", null, pageable);
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals("TRD-100", resultPage.getContent().get(0).tradeRef());
    }
}
