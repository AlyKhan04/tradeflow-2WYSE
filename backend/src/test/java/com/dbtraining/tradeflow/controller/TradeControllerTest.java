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