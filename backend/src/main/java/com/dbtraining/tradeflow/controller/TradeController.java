package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

/**
 * ============================================================================
 * TradeController — TICKET-I068 + I069 + I070 + I071
 * ============================================================================
 * WHAT:    REST controller for /api/v1/trades.
 * HOW:     @RestController + @RequestMapping.
 * WHY:     Single entry-point for trade CRUD from the React UI and Postman.
 *          Stays thin: parse + validate + delegate to TradeService.
 * OBSERVE: GET returns the Spring Data page envelope
 *          {content, totalElements, size, number} that Day-7's trade blotter
 *          consumes verbatim.
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

    /** Guards against a client asking for 10,000 rows in one call. */
    private static final int MAX_PAGE_SIZE = 100;

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    // ------------------------------------------------------------------------
    // TICKET-I068
    // ------------------------------------------------------------------------
    @Operation(summary = "List trades (paginated, optional status filter)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of trades returned"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "401", description = "Auth missing"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @GetMapping
    public Page<TradeDto> list(
            @Parameter(description = "Filter trades by status")
            @RequestParam(required = false) TradeStatus status,
            @PageableDefault(size = 20, sort = "tradeDate",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page size must be <= " + MAX_PAGE_SIZE);
        }
        return status != null
                ? tradeService.findPageByStatus(status, pageable)
                : tradeService.findAll(pageable);
    }

    @Operation(summary = "List trades whose tradeDate falls within a range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of trades returned"),
            @ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    @GetMapping("/by-date")
    public Page<TradeDto> listByDate(
            @Parameter(description = "Inclusive lower bound on tradeDate")
            @RequestParam LocalDate from,
            @Parameter(description = "Inclusive upper bound on tradeDate")
            @RequestParam LocalDate to,
            @PageableDefault(size = 20, sort = "tradeDate",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page size must be <= " + MAX_PAGE_SIZE);
        }
        return tradeService.findByTradeDateBetween(from, to, pageable);
    }

    // ------------------------------------------------------------------------
    // TICKET-I069
    // ------------------------------------------------------------------------
    @Operation(summary = "Create a new trade")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trade created, Location header set"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Duplicate tradeRef")
    })
    @PostMapping
    public ResponseEntity<TradeDto> create(
            @Parameter(description = "Trade payload to create")
            @Valid @RequestBody TradeRequest request) {
        TradeDto saved = tradeService.createTrade(request);
        return ResponseEntity
                .created(URI.create("/api/v1/trades/" + saved.id()))
                .body(saved);
    }

    // ------------------------------------------------------------------------
    // TICKET-I070
    // ------------------------------------------------------------------------
    @Operation(summary = "Update a trade's status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status payload"),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "409", description = "Cannot transition from terminal status")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<TradeDto> updateStatus(
            @Parameter(description = "Trade identifier") @PathVariable Long id,
            @Parameter(description = "Status update payload")
            @Valid @RequestBody StatusUpdate body) {
        TradeDto updated = tradeService.updateStatus(id, body.status());
        return ResponseEntity.ok()
                .location(URI.create("/api/v1/trades/" + id))
                .body(updated);
    }

    // ------------------------------------------------------------------------
    // TICKET-I071 — soft delete
    // ------------------------------------------------------------------------
    @Operation(summary = "Soft-delete a trade (sets status to CANCELLED)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Trade soft-deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid trade identifier"),
            @ApiResponse(responseCode = "404", description = "Trade not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(
            @Parameter(description = "Trade identifier") @PathVariable Long id) {
        tradeService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /** Tiny inbound record for PUT /{id}/status. */
    public record StatusUpdate(@NotNull TradeStatus status) {}
}
