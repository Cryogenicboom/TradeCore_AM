package com.tradecore.event;

import com.tradecore.ledger.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket and in-memory implementation of EventPublisher.
 * Pushes settled trade events to /topic/trades for real-time subscribers and UI dashboards.
 */
@Service
public class WebSocketTradeEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketTradeEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketTradeEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(Trade trade) {
        if (trade == null) {
            return;
        }
        log.info("Publishing trade {} to /topic/trades (price: {}, qty: {}, buyer: {}, seller: {})",
                trade.tradeId(), trade.price(), trade.qty(), trade.buyerId(), trade.sellerId());

        messagingTemplate.convertAndSend("/topic/trades", trade);
    }
}
