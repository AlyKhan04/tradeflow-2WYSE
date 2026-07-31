package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.config.SecurityConfig;
import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.exception.GlobalExceptionHandler;
import com.dbtraining.tradeflow.exception.TradeNotFoundException;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.TradeProcessor;
import com.dbtraining.tradeflow.service.TradeService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Imports the real SecurityConfig so the TICKET-I077 role matrix is what these
 * tests exercise (and so CSRF stays disabled, as it is in the app) plus the
 * GlobalExceptionHandler so error envelopes are asserted against the real
 * advice. Without SecurityConfig the slice falls back to Boot's default chain
 * and every mutating request fails CSRF with a 403.
 */
@WebMvcTest(controllers = TradeController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class TradeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TradeService tradeService;

    /**
     * TradeflowApplication declares a Day-3 demo CommandLineRunner that needs a
     * TradeProcessor. The @WebMvcTest slice loads the main class's @Bean methods
     * but not @Service beans, so without this the context fails to start.
     */
    @MockBean
    private TradeProcessor tradeProcessor;

    // =========================
    // TICKET-I083
    // Invalid create trade tests
    // =========================

    @Test
    @WithMockUser(roles = "TRADER")
    void createTrade_missingQuantity_returns400() throws Exception {

        String body = """
                {
                  "tradeRef": "TRD-2026-0002",
                  "instrumentId": 1,
                  "counterpartyId": 1,
                  "price": 250.50,
                  "tradeDate": "2026-03-01"
                }
                """;

        mvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.quantity")
                        .exists());
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void createTrade_negativeQuantity_returns400() throws Exception {

        String body = """
                {
                  "tradeRef": "TRD-2026-0003",
                  "instrumentId": 1,
                  "counterpartyId": 1,
                  "quantity": -100,
                  "price": 250.50,
                  "tradeDate": "2026-03-01"
                }
                """;

        mvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.quantity")
                        .exists());
    }

    /** TICKET-I069: tradeRef must match TRD-YYYY-NNNN. */
    @Test
    @WithMockUser(roles = "TRADER")
    void createTrade_malformedTradeRef_returns400() throws Exception {

        String body = """
                {
                  "tradeRef": "NOT-A-REF",
                  "instrumentId": 1,
                  "counterpartyId": 1,
                  "quantity": 100,
                  "price": 250.50,
                  "tradeDate": "2026-03-01"
                }
                """;

        mvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.tradeRef").exists());
    }

    // =========================
    // Added TICKET-I083
    // Valid create trade test
    // =========================

    @Test
    @WithMockUser(roles = "TRADER")
    void createTrade_validInput_returns201() throws Exception {

        TradeDto saved = sampleDto(42L, "TRD-2026-0001");

        when(tradeService.createTrade(any()))
                .thenReturn(saved);

        String body = """
                {
                  "tradeRef": "TRD-2026-0001",
                  "instrumentId": 1,
                  "counterpartyId": 1,
                  "quantity": 100,
                  "price": 250.50,
                  "tradeDate": "2026-03-01"
                }
                """;

        mvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))

                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/trades/42"
                ))
                .andExpect(jsonPath("$.tradeRef")
                        .value("TRD-2026-0001"))
                .andExpect(jsonPath("$.id")
                        .value(42))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"));

        verify(tradeService).createTrade(any());
    }

    // =========================
    // TICKET-I084
    // Pagination GET tests
    // =========================

    @Test
    @WithMockUser(roles = "VIEWER")
    void list_paginated_returnsPageEnvelope() throws Exception {

        Pageable pageable = PageRequest.of(0, 5);

        Page<TradeDto> page =
                new PageImpl<>(
                        List.of(
                                sampleDto(1L, "TRD-2026-0001"),
                                sampleDto(2L, "TRD-2026-0002"),
                                sampleDto(3L, "TRD-2026-0003")
                        ),
                        pageable,
                        12
                );

        when(tradeService.findAll(any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/v1/trades?page=0&size=5"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.content.length()")
                        .value(3))

                .andExpect(jsonPath("$.totalElements")
                        .value(12))

                .andExpect(jsonPath("$.size")
                        .value(5))

                .andExpect(jsonPath("$.number")
                        .value(0))

                .andExpect(jsonPath("$.totalPages")
                        .value(3));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void list_withStatusFilter_delegatesToFilteredFinder()
            throws Exception {

        when(tradeService.findPageByStatus(
                eq(TradeStatus.UNMATCHED),
                any()
        ))
        .thenReturn(Page.empty());

        mvc.perform(get("/api/v1/trades?status=UNMATCHED"))

                .andExpect(status().isOk());

        verify(tradeService)
                .findPageByStatus(
                        eq(TradeStatus.UNMATCHED),
                        any()
                );
    }

    /** TICKET-I068: page size above MAX_PAGE_SIZE is rejected. */
    @Test
    @WithMockUser(roles = "VIEWER")
    void list_pageSizeOverMax_returns400() throws Exception {

        mvc.perform(get("/api/v1/trades?size=101"))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void list_withoutAuth_returns401()
            throws Exception {

        mvc.perform(get("/api/v1/trades"))

                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewer_cannotPost_returns403()
            throws Exception {

        String body = """
                {
                  "tradeRef": "TRD-2026-0004",
                  "instrumentId": 1,
                  "counterpartyId": 1,
                  "quantity": 1,
                  "price": 1,
                  "tradeDate": "2026-03-01"
                }
                """;

        mvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))

                .andExpect(status().isForbidden());
    }

    // =========================
    // TICKET-I070
    // Status update tests
    // =========================

    @Test
    @WithMockUser(roles = "TRADER")
    void updateStatus_valid_returns200WithUpdatedDto() throws Exception {

        TradeDto updated = new TradeDto(
                1L, "TRD-2026-0001", 1L, 1L,
                new BigDecimal("100"), new BigDecimal("250.50"),
                LocalDate.of(2026, 3, 1), TradeStatus.MATCHED, Instant.now());

        when(tradeService.updateStatus(1L, TradeStatus.MATCHED)).thenReturn(updated);

        mvc.perform(put("/api/v1/trades/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))

                .andExpect(status().isOk())
                .andExpect(header().string("Location", "/api/v1/trades/1"))
                .andExpect(jsonPath("$.status").value("MATCHED"));

        verify(tradeService).updateStatus(1L, TradeStatus.MATCHED);
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void updateStatus_unknownId_returns404() throws Exception {

        when(tradeService.updateStatus(eq(9999L), any()))
                .thenThrow(new TradeNotFoundException("Trade 9999 not found"));

        mvc.perform(put("/api/v1/trades/9999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRADE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void updateStatus_terminalTrade_returns409() throws Exception {

        when(tradeService.updateStatus(eq(1L), any()))
                .thenThrow(new IllegalStateException("Trade 1 is in terminal status CANCELLED"));

        mvc.perform(put("/api/v1/trades/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewer_cannotUpdateStatus_returns403() throws Exception {

        mvc.perform(put("/api/v1/trades/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))

                .andExpect(status().isForbidden());
    }

    // =========================
    // TICKET-I071
    // Soft delete
    // =========================

    @Test
    @WithMockUser(roles = "TRADER")
    void softDelete_delegatesToTradeServiceAndReturns204() throws Exception {

        mvc.perform(delete("/api/v1/trades/1"))

                .andExpect(status().isNoContent());

        verify(tradeService).softDelete(1L);
    }

    private TradeDto sampleDto(Long id, String ref) {
        return new TradeDto(
                id,
                ref,
                1L,
                1L,
                new BigDecimal("100"),
                new BigDecimal("250.50"),
                LocalDate.of(2026, 3, 1),
                TradeStatus.PENDING,
                Instant.now());
    }
}
