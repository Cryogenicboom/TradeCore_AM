package com.tradecore.event;

import com.tradecore.ledger.model.Trade;

/**
 * Event publisher interface for notifying downstream consumers of settled trades.
 * Decouples in-process or WebSocket dispatching from messaging brokers (e.g. Kafka).
 */
public interface EventPublisher {
    void publish(Trade trade);
}
