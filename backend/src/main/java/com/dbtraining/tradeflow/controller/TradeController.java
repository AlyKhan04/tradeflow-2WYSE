package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================================
 * TradeController — TICKET-I068 + I069 + I070 + I071
 * ============================================================================
 * WHAT:    REST controller for /api/v1/trades.
 * HOW:     @RestController + @RequestMapping.
 * WHY:     Single entry-point for trade CRUD from the React UI and Postman.
 *          Stays thin: parse + validate + delegate to TradeService.
 * OBSERVE: Day-0 — every endpoint returns empty or 501-style throw. As
 *          students complete Day-1..6 tickets, the controller wires through
 *          to the real DB and the React UI populates.
 * ============================================================================
 *
 *  TICKET-I068: GET    /api/v1/trades (paginated + filterable)
 *  TICKET-I069: POST   /api/v1/trades (@Valid + 201 Created)
 *  TICKET-I070: PUT    /api/v1/trades/{id}/status
 *  TICKET-I071: DELETE /api/v1/trades/{id}  (soft delete)
 *  TICKET-I064: OpenAPI annotations on every method
 * ============================================================================
 */
@RestController
@RequestMapping("/api/v1/trades")
@Tag(name = "Trades", description = "Trade management endpoints")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    // ------------------------------------------------------------------------
    // TICKET-I068
    // ------------------------------------------------------------------------
    @Operation(summary = "List trades (paginated, filterable)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trades returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @ApiResponse(responseCode = "401", description = "Auth missing"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @GetMapping
    public List<TradeDto> list(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Start trade date filter") @RequestParam(required = false) LocalDate from,
            @Parameter(description = "End trade date filter") @RequestParam(required = false) LocalDate to
    ) {
        return tradeService.getAllTrades().stream()
                .filter(trade -> {
                    if (status != null && !status.isBlank()) {
                        try {
                            return trade.getStatus() == TradeStatus.valueOf(status.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(trade -> from == null || !trade.getTradeDate().isBefore(from))
                .filter(trade -> to == null || !trade.getTradeDate().isAfter(to))
                .map(TradeDto::from)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    // TICKET-I069
    // ------------------------------------------------------------------------
    @Operation(summary = "Create a new trade")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trade created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Auth missing"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "409", description = "Duplicate tradeRef")
    })
    @PostMapping
    public ResponseEntity<TradeDto> create(@Valid @RequestBody TradeRequest request) {
        TradeDto saved = tradeService.createTrade(request);
        return ResponseEntity.created(URI.create("/api/v1/trades/" + saved.id()))
                .body(saved);
    }

    // ------------------------------------------------------------------------
    // TICKET-I070
    // ------------------------------------------------------------------------
    @Operation(summary = "Update a trade's status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status change"),
            @ApiResponse(responseCode = "401", description = "Auth missing"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "409", description = "Trade already in terminal status")
    })
    @PutMapping("/{id}/status")
    public TradeDto updateStatus(
            @Parameter(description = "Trade id") @PathVariable Long id,
            @Parameter(description = "Status update payload") @RequestBody StatusUpdate body) {
        return tradeService.updateStatus(id, TradeStatus.valueOf(body.status().toUpperCase()));
    }

    // ------------------------------------------------------------------------
    // TICKET-I071 — soft delete
    // ------------------------------------------------------------------------
    @Operation(summary = "Soft-delete a trade (sets status to CANCELLED)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Trade cancelled"),
            @ApiResponse(responseCode = "401", description = "Auth missing"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Trade not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@Parameter(description = "Trade id") @PathVariable Long id) {
        tradeService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /** Tiny inbound record for PUT /{id}/status. */
    public record StatusUpdate(String status) {}
}
