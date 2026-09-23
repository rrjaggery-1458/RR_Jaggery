-- =============================================================================
-- RR JAGGERY TRADERS — DATABASE SCHEMAS INITIALIZATION SCRIPT
-- PostgreSQL 16 Multi-Schema Separation for Single-VPS Low-Cost Deployment
-- =============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Identity, Roles & Security Schema
CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE IF NOT EXISTS auth_schema.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    customer_type VARCHAR(50) NOT NULL DEFAULT 'RETAIL',
    business_name VARCHAR(255),
    gstin VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auth_schema.user_roles (
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- 2. Products, Categories, Cart & Orders Schema
CREATE SCHEMA IF NOT EXISTS commerce_schema;

CREATE TABLE IF NOT EXISTS commerce_schema.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS commerce_schema.products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES commerce_schema.categories(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    grade VARCHAR(50) NOT NULL,
    package_type VARCHAR(50) NOT NULL,
    unit_weight_kg NUMERIC(10, 3) NOT NULL,
    retail_price NUMERIC(12, 2) NOT NULL,
    wholesale_price NUMERIC(12, 2) NOT NULL,
    wholesale_moq INT NOT NULL DEFAULT 1,
    image_url VARCHAR(1024),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS commerce_schema.cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES commerce_schema.products(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE IF NOT EXISTS commerce_schema.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(100) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(50),
    customer_type VARCHAR(50) NOT NULL,
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    tax_amount NUMERIC(12, 2) NOT NULL,
    shipping_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL DEFAULT 'TEST_PAYMENT',
    recipient_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    street_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS commerce_schema.order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES commerce_schema.orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_weight_kg NUMERIC(10, 3) NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    tax_amount NUMERIC(12, 2) NOT NULL
);

-- 3. Retail, B2B & Offline Wholesale Customers & Financial Ledgers Schema
CREATE SCHEMA IF NOT EXISTS customer_schema;

CREATE TABLE IF NOT EXISTS customer_schema.customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_type VARCHAR(50) NOT NULL DEFAULT 'RETAIL',
    business_name VARCHAR(255),
    contact_person VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    gstin VARCHAR(50),
    billing_street TEXT,
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_postal_code VARCHAR(20),
    shipping_street TEXT,
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_postal_code VARCHAR(20),
    payment_terms VARCHAR(50) NOT NULL DEFAULT 'IMMEDIATE_CASH',
    credit_limit NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    credit_days INT NOT NULL DEFAULT 0,
    current_outstanding NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    auth_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS customer_schema.offline_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(100) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customer_schema.customers(id) ON DELETE RESTRICT,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    tax_amount NUMERIC(12, 2) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    outstanding_amount NUMERIC(12, 2) NOT NULL,
    order_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    due_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CREDIT',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS customer_schema.offline_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offline_order_id UUID NOT NULL REFERENCES customer_schema.offline_orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_weight_kg NUMERIC(10, 3) NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    tax_amount NUMERIC(12, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_schema.customer_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customer_schema.customers(id) ON DELETE RESTRICT,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    transaction_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    debit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    credit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    balance_after NUMERIC(12, 2) NOT NULL,
    due_date TIMESTAMP WITH TIME ZONE,
    description TEXT,
    idempotency_key VARCHAR(128) UNIQUE,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS customer_schema.customer_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_number VARCHAR(100) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customer_schema.customers(id) ON DELETE RESTRICT,
    offline_order_id UUID REFERENCES customer_schema.offline_orders(id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(50) NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    reference_notes TEXT,
    idempotency_key VARCHAR(128) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

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
    RAISE NOTICE 'RR Jaggery Traders: All domain schemas and tables successfully initialized.';
END $$;
