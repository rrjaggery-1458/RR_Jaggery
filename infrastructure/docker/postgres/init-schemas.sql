-- =============================================================================
-- RR JAGGERY TRADERS — DATABASE SCHEMAS & LEAST-PRIVILEGE INITIALIZATION SCRIPT
-- PostgreSQL 16 Multi-Schema Separation for Single-VPS Low-Cost Deployment
-- Baseline Commit: 2d902e1
-- =============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- STAGE 1: ROLE INITIALIZATION
-- =============================================================================
DO $$
BEGIN
    -- Migration Owner Role (NOLOGIN)
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rr_migrator') THEN
        CREATE ROLE rr_migrator NOLOGIN;
    END IF;

    -- Service-Specific Runtime Roles (LOGIN)
    -- In production, passwords are provided via environment secrets
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rr_auth') THEN
        CREATE ROLE rr_auth WITH LOGIN PASSWORD 'rr_auth_dev_password';
    END IF;

    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rr_commerce') THEN
        CREATE ROLE rr_commerce WITH LOGIN PASSWORD 'rr_commerce_dev_password';
    END IF;

    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rr_custfin') THEN
        CREATE ROLE rr_custfin WITH LOGIN PASSWORD 'rr_custfin_dev_password';
    END IF;

    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rr_invproc') THEN
        CREATE ROLE rr_invproc WITH LOGIN PASSWORD 'rr_invproc_dev_password';
    END IF;

    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rr_production') THEN
        CREATE ROLE rr_production WITH LOGIN PASSWORD 'rr_production_dev_password';
    END IF;
END $$;

-- =============================================================================
-- STAGE 2: SCHEMAS CREATION & OWNERSHIP
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS commerce_schema;
CREATE SCHEMA IF NOT EXISTS customer_schema;
CREATE SCHEMA IF NOT EXISTS finance_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;
CREATE SCHEMA IF NOT EXISTS inventory_schema;
CREATE SCHEMA IF NOT EXISTS procurement_schema;
CREATE SCHEMA IF NOT EXISTS production_schema;

ALTER SCHEMA auth_schema OWNER TO rr_migrator;
ALTER SCHEMA commerce_schema OWNER TO rr_migrator;
ALTER SCHEMA customer_schema OWNER TO rr_migrator;
ALTER SCHEMA finance_schema OWNER TO rr_migrator;
ALTER SCHEMA notification_schema OWNER TO rr_migrator;
ALTER SCHEMA inventory_schema OWNER TO rr_migrator;
ALTER SCHEMA procurement_schema OWNER TO rr_migrator;
ALTER SCHEMA production_schema OWNER TO rr_migrator;

-- Revoke default public schema create privileges for security
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- =============================================================================
-- 1. Identity, Roles & Security Schema (auth_schema)
-- =============================================================================
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

CREATE TABLE IF NOT EXISTS auth_schema.password_reset_otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_token UUID NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    attempts INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_pwr_email ON auth_schema.password_reset_otps(email);

CREATE TABLE IF NOT EXISTS auth_schema.registration_email_otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_token UUID NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    business_name VARCHAR(150),
    gstin VARCHAR(20),
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    send_window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    send_count INT NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    email_delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

ALTER TABLE auth_schema.users OWNER TO rr_migrator;
ALTER TABLE auth_schema.user_roles OWNER TO rr_migrator;
ALTER TABLE auth_schema.password_reset_otps OWNER TO rr_migrator;
ALTER TABLE auth_schema.registration_email_otps OWNER TO rr_migrator;

-- =============================================================================
-- 2. Products, Categories, Cart, Orders & Reviews (commerce_schema)
-- =============================================================================
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
    unit VARCHAR(20) NOT NULL DEFAULT 'kg',
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
    packaging_fee NUMERIC(12, 2) NOT NULL DEFAULT 10.00,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    street_address TEXT NOT NULL,
    apartment_name VARCHAR(200),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    nearest_landmark VARCHAR(300),
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

CREATE TABLE IF NOT EXISTS commerce_schema.product_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID,
    product_name VARCHAR(255),
    user_id UUID,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    rating INT NOT NULL,
    title VARCHAR(255),
    comment TEXT,
    verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS commerce_schema.stock_outbox (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    command_key VARCHAR(255) NOT NULL,
    depends_on_command_key VARCHAR(255),
    operation VARCHAR(20) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    quantity NUMERIC(14, 3) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_commerce_stock_outbox_command UNIQUE (command_key)
);

ALTER TABLE commerce_schema.categories OWNER TO rr_migrator;
ALTER TABLE commerce_schema.products OWNER TO rr_migrator;
ALTER TABLE commerce_schema.cart_items OWNER TO rr_migrator;
ALTER TABLE commerce_schema.orders OWNER TO rr_migrator;
ALTER TABLE commerce_schema.order_items OWNER TO rr_migrator;
ALTER TABLE commerce_schema.product_reviews OWNER TO rr_migrator;
ALTER TABLE commerce_schema.stock_outbox OWNER TO rr_migrator;
ALTER SEQUENCE commerce_schema.stock_outbox_id_seq OWNER TO rr_migrator;

-- =============================================================================
-- 3. Customers, Offline Orders, Ledgers & Outbox (customer_schema)
-- =============================================================================
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

CREATE TABLE IF NOT EXISTS customer_schema.stock_outbox (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    command_key VARCHAR(255) NOT NULL,
    depends_on_command_key VARCHAR(255),
    operation VARCHAR(20) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    quantity NUMERIC(14, 3) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_customer_stock_outbox_command UNIQUE (command_key)
);

ALTER TABLE customer_schema.customers OWNER TO rr_migrator;
ALTER TABLE customer_schema.offline_orders OWNER TO rr_migrator;
ALTER TABLE customer_schema.offline_order_items OWNER TO rr_migrator;
ALTER TABLE customer_schema.customer_ledger OWNER TO rr_migrator;
ALTER TABLE customer_schema.customer_payments OWNER TO rr_migrator;
ALTER TABLE customer_schema.stock_outbox OWNER TO rr_migrator;
ALTER SEQUENCE customer_schema.stock_outbox_id_seq OWNER TO rr_migrator;

-- =============================================================================
-- 4. Factory Operating Expenses, Investments & Payroll (finance_schema)
-- =============================================================================
CREATE TABLE IF NOT EXISTS finance_schema.operating_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
    category VARCHAR(60) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    payee_vendor VARCHAR(150),
    description VARCHAR(500),
    reference_number VARCHAR(100),
    receipt_url VARCHAR(1000),
    notes TEXT,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID',
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS finance_schema.investments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_name VARCHAR(200) NOT NULL,
    investment_type VARCHAR(120) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    investment_date DATE NOT NULL,
    details VARCHAR(1000),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
);
CREATE INDEX IF NOT EXISTS idx_investment_date ON finance_schema.investments(investment_date);

CREATE TABLE IF NOT EXISTS finance_schema.employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    wage_type VARCHAR(30) NOT NULL DEFAULT 'DAILY',
    rate_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    joining_date DATE DEFAULT CURRENT_DATE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS finance_schema.attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES finance_schema.employees(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
    overtime_hours NUMERIC(6, 2) NOT NULL DEFAULT 0.00,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_employee_date UNIQUE (employee_id, attendance_date)
);

CREATE TABLE IF NOT EXISTS finance_schema.payroll_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES finance_schema.employees(id) ON DELETE RESTRICT,
    period_month INTEGER NOT NULL,
    period_year INTEGER NOT NULL,
    days_worked NUMERIC(6, 2) DEFAULT 0.00,
    overtime_hours NUMERIC(6, 2) DEFAULT 0.00,
    base_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    bonus_allowance NUMERIC(12, 2) DEFAULT 0.00,
    deductions NUMERIC(12, 2) DEFAULT 0.00,
    net_amount_paid NUMERIC(12, 2) NOT NULL CHECK (net_amount_paid > 0),
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    reference_notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS finance_schema.salary_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    reference_number VARCHAR(100),
    notes VARCHAR(500),
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_payment_employee ON finance_schema.salary_payments(employee_id);
CREATE INDEX IF NOT EXISTS idx_payment_period ON finance_schema.salary_payments(period_year, period_month);

CREATE TABLE IF NOT EXISTS finance_schema.salary_advances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    advance_date DATE NOT NULL DEFAULT CURRENT_DATE,
    amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    reference_number VARCHAR(100),
    notes VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    recovered_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_advance_employee ON finance_schema.salary_advances(employee_id);
CREATE INDEX IF NOT EXISTS idx_advance_date ON finance_schema.salary_advances(advance_date);

CREATE TABLE IF NOT EXISTS finance_schema.salary_deductions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    deduction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    reason VARCHAR(200) NOT NULL,
    notes VARCHAR(500),
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_deduction_employee ON finance_schema.salary_deductions(employee_id);
CREATE INDEX IF NOT EXISTS idx_deduction_period ON finance_schema.salary_deductions(period_year, period_month);

CREATE TABLE IF NOT EXISTS finance_schema.employee_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    transaction_type VARCHAR(50) NOT NULL,
    debit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    credit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    running_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_emp_ledger_employee ON finance_schema.employee_ledger(employee_id);
CREATE INDEX IF NOT EXISTS idx_emp_ledger_date ON finance_schema.employee_ledger(transaction_date);

CREATE TABLE IF NOT EXISTS finance_schema.expense_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(300),
    parent_category VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS finance_schema.payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS finance_schema.payroll_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(150) NOT NULL DEFAULT 'Standard Factory Payroll Standards',
    standard_working_days NUMERIC(5, 2) NOT NULL DEFAULT 26.00,
    standard_hours_per_day NUMERIC(5, 2) NOT NULL DEFAULT 8.00,
    overtime_multiplier NUMERIC(5, 2) NOT NULL DEFAULT 1.50,
    half_day_factor NUMERIC(5, 2) NOT NULL DEFAULT 0.50,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS finance_schema.advance_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS finance_schema.deduction_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE finance_schema.operating_expenses OWNER TO rr_migrator;
ALTER TABLE finance_schema.investments OWNER TO rr_migrator;
ALTER TABLE finance_schema.employees OWNER TO rr_migrator;
ALTER TABLE finance_schema.attendance OWNER TO rr_migrator;
ALTER TABLE finance_schema.payroll_records OWNER TO rr_migrator;
ALTER TABLE finance_schema.salary_payments OWNER TO rr_migrator;
ALTER TABLE finance_schema.salary_advances OWNER TO rr_migrator;
ALTER TABLE finance_schema.salary_deductions OWNER TO rr_migrator;
ALTER TABLE finance_schema.employee_ledger OWNER TO rr_migrator;
ALTER TABLE finance_schema.expense_categories OWNER TO rr_migrator;
ALTER TABLE finance_schema.payment_methods OWNER TO rr_migrator;
ALTER TABLE finance_schema.payroll_rules OWNER TO rr_migrator;
ALTER TABLE finance_schema.advance_types OWNER TO rr_migrator;
ALTER TABLE finance_schema.deduction_types OWNER TO rr_migrator;

-- =============================================================================
-- 5. Notifications Schema (notification_schema)
-- =============================================================================
CREATE TABLE IF NOT EXISTS notification_schema.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    role VARCHAR(50),
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    read_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE notification_schema.notifications OWNER TO rr_migrator;

-- =============================================================================
-- 6. Raw Materials, Finished Goods & Stock Movements (inventory_schema)
-- =============================================================================
CREATE TABLE IF NOT EXISTS inventory_schema.inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(100) NOT NULL UNIQUE,
    item_name VARCHAR(150) NOT NULL,
    item_type VARCHAR(40) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    current_stock NUMERIC(14, 3) NOT NULL DEFAULT 0.000,
    allocated_stock NUMERIC(14, 3) NOT NULL DEFAULT 0.000,
    available_stock NUMERIC(14, 3) NOT NULL DEFAULT 0.000,
    reorder_point NUMERIC(14, 3) NOT NULL DEFAULT 0.000,
    safety_stock NUMERIC(14, 3) NOT NULL DEFAULT 0.000,
    warehouse_zone VARCHAR(60),
    storage_location VARCHAR(100),
    unit_cost NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_inventory_item_status ON inventory_schema.inventory_items(status);

CREATE TABLE IF NOT EXISTS inventory_schema.stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES inventory_schema.inventory_items(id) ON DELETE CASCADE,
    movement_type VARCHAR(40) NOT NULL,
    quantity NUMERIC(14, 3) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    reference_type VARCHAR(40),
    reference_id VARCHAR(255),
    actor VARCHAR(120),
    notes VARCHAR(500),
    idempotency_key VARCHAR(255) UNIQUE,
    movement_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_stock_movement_item ON inventory_schema.stock_movements(item_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_reference ON inventory_schema.stock_movements(reference_id);

ALTER TABLE inventory_schema.inventory_items OWNER TO rr_migrator;
ALTER TABLE inventory_schema.stock_movements OWNER TO rr_migrator;

-- =============================================================================
-- 7. Suppliers, Purchase Orders & Receipts (procurement_schema)
-- =============================================================================
CREATE TABLE IF NOT EXISTS procurement_schema.suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_code VARCHAR(60) NOT NULL UNIQUE,
    supplier_name VARCHAR(180) NOT NULL,
    contact_name VARCHAR(120),
    email VARCHAR(120),
    phone VARCHAR(40),
    address VARCHAR(500),
    category VARCHAR(60),
    notes VARCHAR(1000),
    gstin VARCHAR(60),
    payment_terms_days INT DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_outstanding NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_supplier_name ON procurement_schema.suppliers(supplier_name);
CREATE INDEX IF NOT EXISTS idx_supplier_status ON procurement_schema.suppliers(status);

CREATE TABLE IF NOT EXISTS procurement_schema.purchase_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(80) NOT NULL UNIQUE,
    supplier_id UUID NOT NULL REFERENCES procurement_schema.suppliers(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    order_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expected_delivery_date TIMESTAMP WITH TIME ZONE,
    total_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    total_received_quantity NUMERIC(19, 3) NOT NULL DEFAULT 0.000,
    notes VARCHAR(1000)
);
CREATE INDEX IF NOT EXISTS idx_purchase_order_supplier ON procurement_schema.purchase_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_status ON procurement_schema.purchase_orders(status);

CREATE TABLE IF NOT EXISTS procurement_schema.purchase_order_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id UUID NOT NULL REFERENCES procurement_schema.purchase_orders(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL,
    item_name VARCHAR(160) NOT NULL,
    ordered_quantity NUMERIC(19, 3) NOT NULL,
    received_quantity NUMERIC(19, 3) NOT NULL DEFAULT 0.000,
    unit_cost NUMERIC(19, 2) NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_purchase_order_line_po ON procurement_schema.purchase_order_lines(purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_line_sku ON procurement_schema.purchase_order_lines(sku);

CREATE TABLE IF NOT EXISTS procurement_schema.goods_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id UUID NOT NULL REFERENCES procurement_schema.purchase_orders(id) ON DELETE RESTRICT,
    sku VARCHAR(100) NOT NULL,
    item_name VARCHAR(160) NOT NULL,
    received_quantity NUMERIC(19, 3) NOT NULL,
    unit_cost NUMERIC(19, 2) NOT NULL,
    receipt_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    received_by VARCHAR(120) NOT NULL,
    notes VARCHAR(500),
    idempotency_key VARCHAR(128) UNIQUE
);
CREATE INDEX IF NOT EXISTS idx_goods_receipt_po ON procurement_schema.goods_receipts(purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_goods_receipt_sku ON procurement_schema.goods_receipts(sku);

CREATE TABLE IF NOT EXISTS procurement_schema.supplier_ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES procurement_schema.suppliers(id) ON DELETE RESTRICT,
    entry_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    debit_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    credit_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    running_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    reference_type VARCHAR(40),
    reference_id VARCHAR(120),
    actor VARCHAR(120),
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_supplier_ledger_supplier ON procurement_schema.supplier_ledger_entries(supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_ledger_reference ON procurement_schema.supplier_ledger_entries(reference_id);

ALTER TABLE procurement_schema.suppliers OWNER TO rr_migrator;
ALTER TABLE procurement_schema.purchase_orders OWNER TO rr_migrator;
ALTER TABLE procurement_schema.purchase_order_lines OWNER TO rr_migrator;
ALTER TABLE procurement_schema.goods_receipts OWNER TO rr_migrator;
ALTER TABLE procurement_schema.supplier_ledger_entries OWNER TO rr_migrator;

-- =============================================================================
-- 8. Recipes, Production Batches & Output (production_schema)
-- =============================================================================
CREATE TABLE IF NOT EXISTS production_schema.recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_code VARCHAR(60) NOT NULL UNIQUE,
    recipe_name VARCHAR(180) NOT NULL,
    output_product_sku VARCHAR(100) NOT NULL,
    output_product_name VARCHAR(180) NOT NULL,
    standard_batch_size NUMERIC(19, 3) NOT NULL DEFAULT 100.000,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    standard_yield_percentage NUMERIC(7, 2) DEFAULT 100.00,
    estimated_duration_minutes INT DEFAULT 60,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_recipe_target_sku ON production_schema.recipes(output_product_sku);
CREATE INDEX IF NOT EXISTS idx_recipe_active ON production_schema.recipes(active);

CREATE TABLE IF NOT EXISTS production_schema.recipe_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES production_schema.recipes(id) ON DELETE CASCADE,
    raw_material_sku VARCHAR(100) NOT NULL,
    raw_material_name VARCHAR(180) NOT NULL,
    required_quantity NUMERIC(19, 3) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    is_optional BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_recipe_item_recipe ON production_schema.recipe_items(recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_item_sku ON production_schema.recipe_items(raw_material_sku);

CREATE TABLE IF NOT EXISTS production_schema.production_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_number VARCHAR(80) NOT NULL UNIQUE,
    recipe_id UUID REFERENCES production_schema.recipes(id) ON DELETE SET NULL,
    target_product_sku VARCHAR(100) NOT NULL,
    target_product_name VARCHAR(180) NOT NULL,
    planned_quantity NUMERIC(19, 3) NOT NULL,
    actual_quantity NUMERIC(19, 3) NOT NULL DEFAULT 0.000,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    status VARCHAR(40) NOT NULL DEFAULT 'PLANNED',
    planned_start_date TIMESTAMP WITH TIME ZONE,
    actual_start_date TIMESTAMP WITH TIME ZONE,
    completion_date TIMESTAMP WITH TIME ZONE,
    supervisor VARCHAR(120),
    batch_lot VARCHAR(100),
    notes VARCHAR(1000),
    total_wastage_quantity NUMERIC(19, 3) NOT NULL DEFAULT 0.000,
    yield_percentage NUMERIC(7, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_batch_status ON production_schema.production_batches(status);
CREATE INDEX IF NOT EXISTS idx_batch_target_sku ON production_schema.production_batches(target_product_sku);
CREATE INDEX IF NOT EXISTS idx_batch_lot ON production_schema.production_batches(batch_lot);

CREATE TABLE IF NOT EXISTS production_schema.batch_consumptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES production_schema.production_batches(id) ON DELETE CASCADE,
    raw_material_sku VARCHAR(100) NOT NULL,
    raw_material_name VARCHAR(180) NOT NULL,
    planned_quantity NUMERIC(19, 3) NOT NULL DEFAULT 0.000,
    consumed_quantity NUMERIC(19, 3) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    actor VARCHAR(120),
    notes VARCHAR(500),
    idempotency_key VARCHAR(160)
);
CREATE INDEX IF NOT EXISTS idx_consumption_batch ON production_schema.batch_consumptions(batch_id);
CREATE INDEX IF NOT EXISTS idx_consumption_sku ON production_schema.batch_consumptions(raw_material_sku);
CREATE UNIQUE INDEX IF NOT EXISTS idx_consumption_idempotency ON production_schema.batch_consumptions(idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS production_schema.batch_outputs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES production_schema.production_batches(id) ON DELETE CASCADE,
    finished_good_sku VARCHAR(100) NOT NULL,
    finished_good_name VARCHAR(180) NOT NULL,
    quantity_produced NUMERIC(19, 3) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    batch_lot VARCHAR(100),
    quality_grade VARCHAR(50) DEFAULT 'A_GRADE',
    produced_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    actor VARCHAR(120),
    notes VARCHAR(500),
    idempotency_key VARCHAR(160)
);
CREATE INDEX IF NOT EXISTS idx_output_batch ON production_schema.batch_outputs(batch_id);
CREATE INDEX IF NOT EXISTS idx_output_sku ON production_schema.batch_outputs(finished_good_sku);
CREATE INDEX IF NOT EXISTS idx_output_lot ON production_schema.batch_outputs(batch_lot);
CREATE UNIQUE INDEX IF NOT EXISTS idx_output_idempotency ON production_schema.batch_outputs(idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS production_schema.batch_wastages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES production_schema.production_batches(id) ON DELETE CASCADE,
    material_sku VARCHAR(100) NOT NULL,
    material_name VARCHAR(180) NOT NULL,
    wastage_quantity NUMERIC(19, 3) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'KG',
    reason VARCHAR(300),
    wastage_type VARCHAR(50) DEFAULT 'SCRAP',
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    actor VARCHAR(120),
    idempotency_key VARCHAR(160)
);
CREATE INDEX IF NOT EXISTS idx_wastage_batch ON production_schema.batch_wastages(batch_id);
CREATE INDEX IF NOT EXISTS idx_wastage_sku ON production_schema.batch_wastages(material_sku);
CREATE UNIQUE INDEX IF NOT EXISTS idx_wastage_idempotency ON production_schema.batch_wastages(idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS production_schema.production_cost_masters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cost_name VARCHAR(150) NOT NULL,
    cost_type VARCHAR(50) DEFAULT 'OVERHEAD',
    calculation_basis VARCHAR(50) NOT NULL DEFAULT 'PER_KG_OUTPUT',
    unit VARCHAR(30) DEFAULT 'KG',
    rate NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE production_schema.recipes OWNER TO rr_migrator;
ALTER TABLE production_schema.recipe_items OWNER TO rr_migrator;
ALTER TABLE production_schema.production_batches OWNER TO rr_migrator;
ALTER TABLE production_schema.batch_consumptions OWNER TO rr_migrator;
ALTER TABLE production_schema.batch_outputs OWNER TO rr_migrator;
ALTER TABLE production_schema.batch_wastages OWNER TO rr_migrator;
ALTER TABLE production_schema.production_cost_masters OWNER TO rr_migrator;

-- Dynamic forms in production_schema (resolved when production-service default_schema is production_schema)
CREATE TABLE IF NOT EXISTS production_schema.form_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_key VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS production_schema.form_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES production_schema.form_definitions(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    label VARCHAR(160) NOT NULL,
    field_type VARCHAR(30) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_managed BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    placeholder VARCHAR(160),
    help_text VARCHAR(1000),
    validation_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_prod_form_field_key UNIQUE (form_definition_id, field_key)
);
CREATE INDEX IF NOT EXISTS idx_prod_form_field_def ON production_schema.form_fields(form_definition_id);

CREATE TABLE IF NOT EXISTS production_schema.form_field_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_id UUID NOT NULL REFERENCES production_schema.form_fields(id) ON DELETE CASCADE,
    option_value VARCHAR(120) NOT NULL,
    display_label VARCHAR(160) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_prod_form_option_val UNIQUE (field_id, option_value)
);
CREATE INDEX IF NOT EXISTS idx_prod_form_opt_field ON production_schema.form_field_options(field_id);

CREATE TABLE IF NOT EXISTS production_schema.form_revisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES production_schema.form_definitions(id) ON DELETE CASCADE,
    version INT NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_prod_form_revision UNIQUE (form_definition_id, version)
);

CREATE TABLE IF NOT EXISTS production_schema.form_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES production_schema.form_definitions(id) ON DELETE RESTRICT,
    form_revision_id UUID NOT NULL REFERENCES production_schema.form_revisions(id) ON DELETE RESTRICT,
    form_version INT NOT NULL,
    record_type VARCHAR(80) NOT NULL,
    record_id UUID NOT NULL,
    answers_json TEXT NOT NULL,
    submitted_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_prod_form_sub_record ON production_schema.form_submissions(record_type, record_id);
CREATE INDEX IF NOT EXISTS idx_prod_form_sub_def ON production_schema.form_submissions(form_definition_id);

CREATE TABLE IF NOT EXISTS production_schema.form_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES production_schema.form_submissions(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(120) NOT NULL UNIQUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_prod_form_att_sub ON production_schema.form_attachments(submission_id);

ALTER TABLE production_schema.form_definitions OWNER TO rr_migrator;
ALTER TABLE production_schema.form_fields OWNER TO rr_migrator;
ALTER TABLE production_schema.form_field_options OWNER TO rr_migrator;
ALTER TABLE production_schema.form_revisions OWNER TO rr_migrator;
ALTER TABLE production_schema.form_submissions OWNER TO rr_migrator;
ALTER TABLE production_schema.form_attachments OWNER TO rr_migrator;

-- =============================================================================
-- 9. Dynamic Forms in public Schema (customer-finance-service resolution)
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.form_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_key VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.form_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES public.form_definitions(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    label VARCHAR(160) NOT NULL,
    field_type VARCHAR(30) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_managed BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    placeholder VARCHAR(160),
    help_text VARCHAR(1000),
    validation_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_public_form_field_key UNIQUE (form_definition_id, field_key)
);
CREATE INDEX IF NOT EXISTS idx_public_form_field_def ON public.form_fields(form_definition_id);

CREATE TABLE IF NOT EXISTS public.form_field_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_id UUID NOT NULL REFERENCES public.form_fields(id) ON DELETE CASCADE,
    option_value VARCHAR(120) NOT NULL,
    display_label VARCHAR(160) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_public_form_option_val UNIQUE (field_id, option_value)
);
CREATE INDEX IF NOT EXISTS idx_public_form_opt_field ON public.form_field_options(field_id);

CREATE TABLE IF NOT EXISTS public.form_revisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES public.form_definitions(id) ON DELETE CASCADE,
    version INT NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_public_form_revision UNIQUE (form_definition_id, version)
);

CREATE TABLE IF NOT EXISTS public.form_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES public.form_definitions(id) ON DELETE RESTRICT,
    form_revision_id UUID NOT NULL REFERENCES public.form_revisions(id) ON DELETE RESTRICT,
    form_version INT NOT NULL,
    record_type VARCHAR(80) NOT NULL,
    record_id UUID NOT NULL,
    answers_json TEXT NOT NULL,
    submitted_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_public_form_sub_record ON public.form_submissions(record_type, record_id);
CREATE INDEX IF NOT EXISTS idx_public_form_sub_def ON public.form_submissions(form_definition_id);

CREATE TABLE IF NOT EXISTS public.form_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES public.form_submissions(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(120) NOT NULL UNIQUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_public_form_att_sub ON public.form_attachments(submission_id);

ALTER TABLE public.form_definitions OWNER TO rr_migrator;
ALTER TABLE public.form_fields OWNER TO rr_migrator;
ALTER TABLE public.form_field_options OWNER TO rr_migrator;
ALTER TABLE public.form_revisions OWNER TO rr_migrator;
ALTER TABLE public.form_submissions OWNER TO rr_migrator;
ALTER TABLE public.form_attachments OWNER TO rr_migrator;

-- =============================================================================
-- STAGE 3: LEAST-PRIVILEGE ROLE PERMISSIONS
-- =============================================================================

-- Database CONNECT
GRANT CONNECT ON DATABASE rr_jaggery_db TO rr_auth, rr_commerce, rr_custfin, rr_invproc, rr_production;

-- 1. auth-service: rr_auth
GRANT USAGE ON SCHEMA auth_schema TO rr_auth;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth_schema TO rr_auth;
GRANT pg_read_all_stats TO rr_auth; -- For SystemHealthService monitoring queries

-- 2. commerce-service: rr_commerce
GRANT USAGE ON SCHEMA commerce_schema TO rr_commerce;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA commerce_schema TO rr_commerce;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA commerce_schema TO rr_commerce;

-- 3. customer-finance-service: rr_custfin
GRANT USAGE ON SCHEMA customer_schema, finance_schema, notification_schema, public TO rr_custfin;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA customer_schema TO rr_custfin;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA finance_schema TO rr_custfin;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA notification_schema TO rr_custfin;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.form_definitions, public.form_fields,
    public.form_field_options, public.form_revisions, public.form_submissions,
    public.form_attachments TO rr_custfin;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA customer_schema TO rr_custfin;

-- 4. inventory-procurement-service: rr_invproc
GRANT USAGE ON SCHEMA inventory_schema, procurement_schema TO rr_invproc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA inventory_schema TO rr_invproc;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA procurement_schema TO rr_invproc;

-- 5. production-service: rr_production
GRANT USAGE ON SCHEMA production_schema TO rr_production;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA production_schema TO rr_production;
-- Read-only access to procurement_schema.purchase_order_lines for costing
GRANT USAGE ON SCHEMA procurement_schema TO rr_production;
GRANT SELECT ON TABLE procurement_schema.purchase_order_lines TO rr_production;

-- =============================================================================
-- STAGE 4: DEFAULT PRIVILEGES CONFIGURATION
-- Ensure objects created in future by rr_migrator or postgres retain least privilege
-- =============================================================================
DO $$
DECLARE
    creator_role TEXT;
BEGIN
    FOR creator_role IN SELECT unnest(ARRAY['rr_migrator', 'postgres']) LOOP
        -- auth_schema
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA auth_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rr_auth', creator_role);

        -- commerce_schema
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA commerce_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rr_commerce', creator_role);
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA commerce_schema GRANT USAGE, SELECT ON SEQUENCES TO rr_commerce', creator_role);

        -- customer_schema
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA customer_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rr_custfin', creator_role);
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA customer_schema GRANT USAGE, SELECT ON SEQUENCES TO rr_custfin', creator_role);

        -- finance_schema
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA finance_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rr_custfin', creator_role);

        -- notification_schema
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA notification_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rr_custfin', creator_role);

        -- inventory_schema
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA inventory_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rr_invproc', creator_role);

        -- procurement_schema
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA procurement_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rr_invproc', creator_role);

        -- production_schema
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA production_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rr_production', creator_role);
    END LOOP;
END $$;

-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'RR Jaggery Traders: All 8 domain schemas, 58 tables, least-privilege roles and permissions initialized successfully.';
END $$;
