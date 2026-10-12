-- Seed test accounts
-- Uses standard SQL INSERT WHERE NOT EXISTS for full cross-database compatibility (PostgreSQL & H2)
INSERT INTO accounts (user_id, cash_balance, asset_balance)
SELECT 'alice', 10000.00000000, 5.00000000
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE user_id = 'alice');

INSERT INTO accounts (user_id, cash_balance, asset_balance)
SELECT 'bob', 2000.00000000, 10.00000000
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE user_id = 'bob');

INSERT INTO accounts (user_id, cash_balance, asset_balance)
SELECT 'charlie', 50000.00000000, 20.00000000
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE user_id = 'charlie');

INSERT INTO accounts (user_id, cash_balance, asset_balance)
SELECT 'dave', 25000.00000000, 15.00000000
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE user_id = 'dave');
