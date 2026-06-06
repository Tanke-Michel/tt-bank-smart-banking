-- =============================================================
-- Smart Banking System — Database Initialization
--
-- This script runs ONCE when the PostgreSQL container starts
-- for the very first time (Docker entrypoint /docker-entrypoint-initdb.d/).
--
-- The primary database (tt_bank_auth) is already created by
-- the POSTGRES_DB environment variable before this script runs.
--
-- All additional service databases are created here.
-- IF NOT EXISTS makes the script idempotent (safe to re-run).
-- =============================================================

-- Auth Service DB already exists (POSTGRES_DB=tt_bank_auth)

-- Wallet Service
CREATE DATABASE tt_bank_wallet;

-- Transaction Service
CREATE DATABASE tt_bank_transaction;

-- Merchant Service
CREATE DATABASE tt_bank_merchant;

-- Savings Service
CREATE DATABASE tt_bank_savings;

-- Notification Service (stateless — included for future persistence)
CREATE DATABASE tt_bank_notification;

-- Audit Service
CREATE DATABASE tt_bank_audit;

-- =============================================================
-- Grant full privileges to the admin user on every database
-- =============================================================
GRANT ALL PRIVILEGES ON DATABASE tt_bank_auth         TO admin;
GRANT ALL PRIVILEGES ON DATABASE tt_bank_wallet        TO admin;
GRANT ALL PRIVILEGES ON DATABASE tt_bank_transaction   TO admin;
GRANT ALL PRIVILEGES ON DATABASE tt_bank_merchant      TO admin;
GRANT ALL PRIVILEGES ON DATABASE tt_bank_savings       TO admin;
GRANT ALL PRIVILEGES ON DATABASE tt_bank_notification  TO admin;
GRANT ALL PRIVILEGES ON DATABASE tt_bank_audit         TO admin;
