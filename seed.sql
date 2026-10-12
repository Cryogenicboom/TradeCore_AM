-- Standalone seed script for TradeCore prototype
-- Can be executed via psql or DBeaver if resetting data manually

TRUNCATE TABLE ledger_entries, notifications, settlements CASCADE;

INSERT INTO accounts (user_id, cash_balance, asset_balance)
VALUES
    ('alice', 10000.00000000, 5.00000000),
    ('bob', 2000.00000000, 10.00000000),
    ('charlie', 50000.00000000, 20.00000000),
    ('dave', 25000.00000000, 15.00000000)
ON CONFLICT (user_id) 
DO UPDATE SET 
    cash_balance = EXCLUDED.cash_balance,
    asset_balance = EXCLUDED.asset_balance;
