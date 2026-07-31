package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.dto.ReconResultDto;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ReconController — TICKET-I072 + I073 + I074
 */
@RestController
@RequestMapping("/api/v1/recon")
@Tag(name = "Reconciliation", description = "Run recon + manage breaks")
public class ReconController {

    private final ReconciliationService reconService;

    public ReconController(ReconciliationService reconService) {
        this.reconService = reconService;
    }

    @Operation(summary = "Trigger a reconciliation run")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reconciliation run completed successfully"),
            @ApiResponse(responseCode = "500", description = "Reconciliation run failed")
    })
    @PostMapping("/run")
    public ReconSummary run() {
        return reconService.runForAll();
    }

    @Operation(summary = "List reconciliation results")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reconciliation results returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters")
    })
    @GetMapping("/results")
    public Page<ReconResultDto> listResults(
            @Parameter(description = "Filter results by status") @RequestParam(required = false, defaultValue = "OPEN") String status,
            @Parameter(description = "Filter results by counterparty ID") @RequestParam(required = false) Long counterpartyId,
            @PageableDefault(size = 20) Pageable pageable) {
        ReconResult.Status parsedStatus = ReconResult.Status.valueOf(status.toUpperCase());
        return reconService.listBreaks(parsedStatus, counterpartyId, pageable);
    }

    @Operation(summary = "Mark a recon break as RESOLVED")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Resolved (idempotent)"),
            @ApiResponse(responseCode = "404", description = "Break not found")
    })
    @PutMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@Parameter(description = "Reconciliation break identifier") @PathVariable Long id) {
        reconService.resolveBreak(id);
        return ResponseEntity.noContent().build();
    }
}
