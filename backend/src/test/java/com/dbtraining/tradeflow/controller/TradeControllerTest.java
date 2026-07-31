package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.service.TradeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

    @Mock
    private TradeService tradeService;

    @InjectMocks
    private TradeController tradeController;

    @Test
    void softDelete_delegatesToTradeServiceAndReturns204() {
        ResponseEntity<Void> response = tradeController.softDelete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(tradeService).softDelete(1L);
    }
    @Test
@WithMockUser(roles = "TRADER")
void createTrade_missingQuantity_returns400() throws Exception {
    String body = """
            {
              "tradeRef": "TRD-NEW-0002",
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
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details.quantity").exists());
}

@Test
@WithMockUser(roles = "TRADER")
void createTrade_negativeQuantity_returns400() throws Exception {
    String body = """
            {
              "tradeRef": "TRD-NEG-0001",
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
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details.quantity",
                    org.hamcrest.Matchers.containsString("must be greater than 0")));
}

@Test
@WithMockUser(roles = "TRADER")
void createTrade_futureTradeDate_returns400() throws Exception {
    String body = """
            {
              "tradeRef": "TRD-FUT-0001",
              "instrumentId": 1,
              "counterpartyId": 1,
              "quantity": 100,
              "price": 250.50,
              "tradeDate": "2099-01-01"
            }
            """;
    mvc.perform(post("/api/v1/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details.tradeDate").exists());
}

    @Test
@WithMockUser(roles = "VIEWER")
void list_paginated_returnsPageEnvelope() throws Exception {
    Pageable pageable = PageRequest.of(0, 5);
    Page<TradeDto> page = new PageImpl<>(
            List.of(sampleDto(1L, "TRD-1"),
                    sampleDto(2L, "TRD-2"),
                    sampleDto(3L, "TRD-3")),
            pageable, 12);
    when(tradeService.findAll(any(Pageable.class))).thenReturn(page);

    mvc.perform(get("/api/v1/trades?page=0&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.totalElements").value(12))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.totalPages").value(3));
}

@Test
@WithMockUser(roles = "VIEWER")
void list_withStatusFilter_delegatesToFilteredFinder() throws Exception {
    when(tradeService.findPageByStatus(eq(TradeStatus.UNMATCHED), any()))
            .thenReturn(Page.empty());

    mvc.perform(get("/api/v1/trades?status=UNMATCHED"))
            .andExpect(status().isOk());

    verify(tradeService).findPageByStatus(eq(TradeStatus.UNMATCHED), any());
}

@Test
void list_withoutAuth_returns401() throws Exception {
    mvc.perform(get("/api/v1/trades"))
            .andExpect(status().isUnauthorized());
}

@Test
@WithMockUser(roles = "VIEWER")
void viewer_cannotPost_returns403() throws Exception {
    String body = """
            {
              "tradeRef": "TRD-RO-0001",
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
    
}

