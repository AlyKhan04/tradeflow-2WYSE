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

{
  "info": {
    "name": "TradeFlow API",
    "description": "TICKET-I085 — Postman collection covering all REST endpoints with auth + assertion tests.",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth": {
    "type": "basic",
    "basic": [
      { "key": "username", "value": "{{username}}", "type": "string" },
      { "key": "password", "value": "{{password}}", "type": "string" }
    ]
  },
  "variable": [
    { "key": "baseUrl",   "value": "http://localhost:8080" },
    { "key": "username",  "value": "trader" },
    { "key": "password",  "value": "trader-pw" },
    { "key": "tradeId",   "value": "1" },
    { "key": "breakId",   "value": "1" }
  ],
  "item": [
    {
      "name": "Health",
      "item": [
        {
          "name": "GET /actuator/health",
          "request": { "method": "GET",
                       "url": { "raw": "{{baseUrl}}/actuator/health" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('status UP', () => pm.expect(pm.response.json().status).to.eql('UP'));"
            ]}
          }]
        }
      ]
    },
    {
      "name": "Trades",
      "item": [
        {
          "name": "GET /api/v1/trades (paginated)",
          "request": { "method": "GET",
                       "url": { "raw": "{{baseUrl}}/api/v1/trades?page=0&size=20" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('page envelope', () => {",
              "  const body = pm.response.json();",
              "  pm.expect(body).to.have.property('content');",
              "  pm.expect(body).to.have.property('totalElements');",
              "});"
            ]}
          }]
        },
        {
          "name": "POST /api/v1/trades (happy path)",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": { "raw": "{{baseUrl}}/api/v1/trades" },
            "body": { "mode": "raw",
                      "raw": "{\n  \"tradeRef\": \"TRD-2026-{{$timestamp}}\",\n  \"instrumentId\": 1,\n  \"counterpartyId\": 1,\n  \"quantity\": 1000,\n  \"price\": 245.50,\n  \"tradeDate\": \"2026-03-15\"\n}" }
          },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('201', () => pm.response.to.have.status(201));",
              "pm.test('Location header', () => pm.expect(pm.response.headers.get('Location')).to.match(/\\/api\\/v1\\/trades\\/\\d+/));",
              "const loc = pm.response.headers.get('Location');",
              "if (loc) pm.collectionVariables.set('tradeId', loc.split('/').pop());"
            ]}
          }]
        },
        {
          "name": "PUT /api/v1/trades/{{tradeId}}/status",
          "request": {
            "method": "PUT",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": { "raw": "{{baseUrl}}/api/v1/trades/{{tradeId}}/status" },
            "body": { "mode": "raw", "raw": "{ \"status\": \"MATCHED\" }" }
          },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('status flipped', () => pm.expect(pm.response.json().status).to.eql('MATCHED'));"
            ]}
          }]
        },
        {
          "name": "DELETE /api/v1/trades/{{tradeId}}",
          "request": {
            "method": "DELETE",
            "url": { "raw": "{{baseUrl}}/api/v1/trades/{{tradeId}}" }
          },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('204', () => pm.response.to.have.status(204));"
            ]}
          }]
        }
      ]
    },
    {
      "name": "Recon",
      "item": [
        {
          "name": "POST /api/v1/recon/run",
          "request": { "method": "POST",
                       "url": { "raw": "{{baseUrl}}/api/v1/recon/run" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('summary shape', () => {",
              "  const b = pm.response.json();",
              "  pm.expect(b).to.have.property('matchedCount');",
              "  pm.expect(b).to.have.property('unmatchedCount');",
              "  pm.expect(b).to.have.property('breakdownByType');",
              "});"
            ]}
          }]
        },
        {
          "name": "GET /api/v1/recon/results?status=OPEN",
          "request": { "method": "GET",
                       "url": { "raw": "{{baseUrl}}/api/v1/recon/results?status=OPEN&page=0&size=20" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('page envelope', () => pm.expect(pm.response.json()).to.have.property('content'));"
            ]}
          }]
        },
        {
          "name": "PUT /api/v1/recon/{{breakId}}/resolve",
          "request": { "method": "PUT",
                       "url": { "raw": "{{baseUrl}}/api/v1/recon/{{breakId}}/resolve" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('204', () => pm.response.to.have.status(204));"
            ]}
          }]
        }
      ]
    }
  ]
}

