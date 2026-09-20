-- =============================================================================
-- RR JAGGERY TRADERS — DATABASE SCHEMAS INITIALIZATION SCRIPT
-- PostgreSQL 16 Multi-Schema Separation for Single-VPS Low-Cost Deployment
-- =============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Identity, Roles & Security Schema
CREATE SCHEMA IF NOT EXISTS auth_schema;

-- 2. Products, Categories, Cart & Orders Schema
CREATE SCHEMA IF NOT EXISTS commerce_schema;

-- 3. Retail, B2B & Offline Wholesale Customers & Financial Ledgers Schema
CREATE SCHEMA IF NOT EXISTS customer_schema;

-- 4. Raw Materials, Finished Goods & Auditable Stock Movements Schema
CREATE SCHEMA IF NOT EXISTS inventory_schema;

-- 5. Cane Farmers, Purchase Orders, Weighment Receipts & Supplier Ledgers Schema
CREATE SCHEMA IF NOT EXISTS procurement_schema;

-- 6. Recipes, Production Batches, Stages, Material Consumption & Yield Schema
CREATE SCHEMA IF NOT EXISTS production_schema;

-- 7. Factory Operating Expenses, Cost Allocation & Worker Payroll Schema
CREATE SCHEMA IF NOT EXISTS finance_schema;

-- 8. Transactional Alerts, Emails & Push Notifications Schema
CREATE SCHEMA IF NOT EXISTS notification_schema;

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'RR Jaggery Traders: All 8 domain schemas successfully initialized.';
END $$;
