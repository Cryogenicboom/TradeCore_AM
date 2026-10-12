package com.tradecore.ledger.service;

import com.tradecore.ledger.model.Trade;

/**
 * Service interface for settling matched trades and recording ledger entries.
 */
public interface LedgerService {

    /**
     * Settles a trade atomically and idempotently.
     *
     * @param trade the trade details to settle
     * @return true if the trade was newly settled, false if it was already settled (idempotent no-op)
     */
    boolean settle(Trade trade);

    /**
     * Retrieves account balance for a given user.
     *
     * @param userId user identifier
     * @return Account or null if not found
     */
    com.tradecore.ledger.model.Account getAccount(String userId);
}
