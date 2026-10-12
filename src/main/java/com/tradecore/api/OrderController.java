package com.tradecore.api;

import com.tradecore.ledger.model.Account;
import com.tradecore.ledger.service.LedgerService;
import com.tradecore.pipeline.TradeCorePipelineService;
import com.tradecore.risk.model.Order;
import com.tradecore.risk.model.RiskRejectReason;
import com.tradecore.risk.web.dto.OrderRequest;
import com.trading.engine.OrderBook;
import com.trading.engine.OrderBookMatchingEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for TradeCore exchange operations:
 * - POST /orders: Submit limit orders (triggers Risk -> MatchingEngine -> Ledger -> WebSocket publish)
 * - GET /orderbook: Retrieve current resting orderbook depth
 * - GET /balances/{user}: Retrieve cash and asset balances for a user
 */
@RestController
@CrossOrigin(origins = "*")
public class OrderController {

    private final TradeCorePipelineService pipelineService;
    private final OrderBookMatchingEngine matchingEngine;
    private final LedgerService ledgerService;

    public OrderController(TradeCorePipelineService pipelineService,
                           OrderBookMatchingEngine matchingEngine,
                           LedgerService ledgerService) {
        this.pipelineService = pipelineService;
        this.matchingEngine = matchingEngine;
        this.ledgerService = ledgerService;
    }

    /**
     * Submit an order into the TradeCore pipeline.
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> submitOrder(@RequestBody(required = false) OrderRequest request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "REJECTED",
                    "reason", "ORDER_NULL",
                    "message", "Order payload is required"
            ));
        }

        // Generate ID if missing
        String orderId = (request.id() != null && !request.id().isBlank())
                ? request.id()
                : "ord-" + UUID.randomUUID().toString().substring(0, 8);

        OrderRequest resolvedRequest = new OrderRequest(
                orderId,
                request.userId(),
                request.side(),
                request.price(),
                request.qty()
        );

        Order domainOrder = resolvedRequest.toDomain();
        TradeCorePipelineService.PipelineResult result = pipelineService.processOrder(domainOrder);

        if (!result.accepted()) {
            HttpStatus status = mapRejectReasonToHttpStatus(result.riskResult().rejectReason());
            return ResponseEntity.status(status).body(Map.of(
                    "status", "REJECTED",
                    "reason", result.riskResult().rejectReason() != null ? result.riskResult().rejectReason().name() : "UNKNOWN",
                    "message", result.riskResult().message()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "ACCEPTED",
                "orderId", domainOrder.id(),
                "userId", domainOrder.userId(),
                "executedTrades", result.executedTradeCount(),
                "settledTrades", result.settledTrades()
        ));
    }

    /**
     * Retrieve resting order book depth.
     */
    @GetMapping("/orderbook")
    public ResponseEntity<OrderBook.Snapshot> getOrderBook() {
        return ResponseEntity.ok(matchingEngine.snapshot());
    }

    /**
     * Retrieve account balance for a user.
     */
    @GetMapping("/balances/{user}")
    public ResponseEntity<?> getBalance(@PathVariable("user") String userId) {
        Account account = ledgerService.getAccount(userId);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "User not found",
                    "userId", userId
            ));
        }
        return ResponseEntity.ok(account);
    }

    private HttpStatus mapRejectReasonToHttpStatus(RiskRejectReason reason) {
        if (reason == null) {
            return HttpStatus.BAD_REQUEST;
        }

        return switch (reason) {
            case TRADING_HALTED -> HttpStatus.SERVICE_UNAVAILABLE; // 503
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS; // 429
            case MAX_QUANTITY_EXCEEDED, POSITION_LIMIT_EXCEEDED, POSITION_DATA_UNAVAILABLE -> HttpStatus.UNPROCESSABLE_ENTITY; // 422
            case ORDER_NULL, ORDER_ID_INVALID, USER_ID_INVALID, SIDE_INVALID, INVALID_QUANTITY, INVALID_PRICE -> HttpStatus.BAD_REQUEST; // 400
        };
    }
}
