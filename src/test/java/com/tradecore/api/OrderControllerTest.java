package com.tradecore.api;

import com.tradecore.TradeCoreApplication;
import com.tradecore.ledger.model.Account;
import com.tradecore.ledger.service.LedgerService;
import com.tradecore.pipeline.TradeCorePipelineService;
import com.tradecore.risk.model.RiskRejectReason;
import com.tradecore.risk.model.RiskResult;
import com.trading.engine.OrderBook;
import com.trading.engine.OrderBookMatchingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@DisplayName("Part E OrderController WebMvc Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeCorePipelineService pipelineService;

    @MockBean
    private OrderBookMatchingEngine matchingEngine;

    @MockBean
    private LedgerService ledgerService;

    @Test
    @DisplayName("POST /orders should return 200 when order passes risk checks")
    void shouldAcceptOrderWhenRiskCheckPasses() throws Exception {
        when(pipelineService.processOrder(any())).thenReturn(
                TradeCorePipelineService.PipelineResult.accepted(RiskResult.accept(), 0, List.of())
        );

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "id": "ord-101",
                            "userId": "alice",
                            "side": "BUY",
                            "price": 50000.0,
                            "qty": 1
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.orderId").value("ord-101"))
                .andExpect(jsonPath("$.userId").value("alice"));
    }

    @Test
    @DisplayName("POST /orders should return 429 when rate limit is exceeded")
    void shouldReturn429WhenRateLimited() throws Exception {
        when(pipelineService.processOrder(any())).thenReturn(
                TradeCorePipelineService.PipelineResult.rejected(
                        RiskResult.reject(RiskRejectReason.RATE_LIMIT_EXCEEDED, "Rate limit exceeded")
                )
        );

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "id": "ord-102",
                            "userId": "bob",
                            "side": "SELL",
                            "price": 50000.0,
                            "qty": 1
                        }
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("GET /orderbook should return current depth snapshot")
    void shouldReturnOrderBookSnapshot() throws Exception {
        when(matchingEngine.snapshot()).thenReturn(new OrderBook.Snapshot(
                List.of(new OrderBook.Level(new BigDecimal("50000"), 2L)),
                List.of(new OrderBook.Level(new BigDecimal("50100"), 3L))
        ));

        mockMvc.perform(get("/orderbook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bids[0].price").value(50000))
                .andExpect(jsonPath("$.bids[0].qty").value(2))
                .andExpect(jsonPath("$.asks[0].price").value(50100))
                .andExpect(jsonPath("$.asks[0].qty").value(3));
    }

    @Test
    @DisplayName("GET /balances/{user} should return balances for existing user")
    void shouldReturnBalancesForUser() throws Exception {
        when(ledgerService.getAccount("alice")).thenReturn(
                new Account("alice", new BigDecimal("10000.00"), new BigDecimal("5.00"))
        );

        mockMvc.perform(get("/balances/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("alice"))
                .andExpect(jsonPath("$.cashBalance").value(10000.00))
                .andExpect(jsonPath("$.assetBalance").value(5.00));
    }
}
