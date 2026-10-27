-- =============================================================================
-- RR JAGGERY TRADERS — SCHEMA PARITY & PERMISSION AUDIT SCRIPT
-- Title: verify_schema_parity.sql
-- Description: Non-destructive parity verification across schemas, tables, sequences, roles, ownership, and grants.
-- =============================================================================

\set ON_ERROR_STOP on

DO $$
DECLARE
    expected_schemas TEXT[] := ARRAY[
        'auth_schema', 'commerce_schema', 'customer_schema',
        'finance_schema', 'notification_schema', 'inventory_schema',
        'procurement_schema', 'production_schema'
    ];
    s TEXT;
    missing_schemas TEXT[] := '{}';

    expected_roles TEXT[] := ARRAY[
        'rr_migrator', 'rr_auth', 'rr_commerce',
        'rr_custfin', 'rr_invproc', 'rr_production'
    ];
    r TEXT;
    missing_roles TEXT[] := '{}';

    table_count INT;
    unowned_table_count INT;
    sequence_count INT;
BEGIN
    RAISE NOTICE '=============================================================';
    RAISE NOTICE '>>> STARTING RR JAGGERY SCHEMA PARITY AUDIT: %', current_database();
    RAISE NOTICE '=============================================================';

    -- 1. Check Schemas
    FOREACH s IN ARRAY expected_schemas LOOP
        IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = s) THEN
            missing_schemas := array_append(missing_schemas, s);
        END IF;
    END LOOP;

    IF array_length(missing_schemas, 1) > 0 THEN
        RAISE EXCEPTION 'Parity Failure: Missing schemas: %', missing_schemas;
    ELSE
        RAISE NOTICE ' [PASS] All 8 domain schemas exist.';
    END IF;

    -- 2. Check Roles
    FOREACH r IN ARRAY expected_roles LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            missing_roles := array_append(missing_roles, r);
        END IF;
    END LOOP;

    IF array_length(missing_roles, 1) > 0 THEN
        RAISE EXCEPTION 'Parity Failure: Missing roles: %', missing_roles;
    ELSE
        RAISE NOTICE ' [PASS] All 6 least-privilege roles exist.';
    END IF;

    -- 3. Check Tables
    SELECT COUNT(*) INTO table_count
    FROM pg_tables
    WHERE schemaname = ANY(expected_schemas)
       OR (schemaname = 'public' AND tablename LIKE 'form_%');

    IF table_count < 58 THEN
        RAISE EXCEPTION 'Parity Failure: Expected at least 58 application tables, found %', table_count;
    ELSE
        RAISE NOTICE ' [PASS] All 58 application tables confirmed present (Found: %).', table_count;
    END IF;

    -- 4. Check Table Ownership
    SELECT COUNT(*) INTO unowned_table_count
    FROM pg_tables
    WHERE (schemaname = ANY(expected_schemas) OR (schemaname = 'public' AND tablename LIKE 'form_%'))
      AND tableowner != 'rr_migrator';

    IF unowned_table_count > 0 THEN
        RAISE EXCEPTION 'Parity Failure: % tables are not owned by rr_migrator.', unowned_table_count;
    ELSE
        RAISE NOTICE ' [PASS] 100%% of application tables are strictly owned by rr_migrator.';
    END IF;

    -- 5. Check Identity Sequences
    IF pg_get_serial_sequence('commerce_schema.stock_outbox', 'id') IS NULL THEN
        RAISE EXCEPTION 'Parity Failure: Missing sequence for commerce_schema.stock_outbox';
    END IF;

    IF pg_get_serial_sequence('customer_schema.stock_outbox', 'id') IS NULL THEN
        RAISE EXCEPTION 'Parity Failure: Missing sequence for customer_schema.stock_outbox';
    END IF;

    RAISE NOTICE ' [PASS] Identity sequences for stock_outbox confirmed in commerce_schema and customer_schema.';

    -- 6. Check Specific Permissions & Isolation
    -- Test rr_auth has access to auth_schema.users
    IF NOT has_table_privilege('rr_auth', 'auth_schema.users', 'SELECT') THEN
        RAISE EXCEPTION 'Parity Failure: rr_auth lacks SELECT on auth_schema.users';
    END IF;

    -- Test rr_auth has NO access to commerce_schema.orders
    IF has_table_privilege('rr_auth', 'commerce_schema.orders', 'SELECT') THEN
        RAISE EXCEPTION 'Isolation Failure: rr_auth unexpectedly has SELECT on commerce_schema.orders';
    END IF;

    -- Test rr_commerce has access to commerce_schema.orders
    IF NOT has_table_privilege('rr_commerce', 'commerce_schema.orders', 'SELECT') THEN
        RAISE EXCEPTION 'Parity Failure: rr_commerce lacks SELECT on commerce_schema.orders';
    END IF;

    -- Test rr_commerce has NO access to auth_schema.users
    IF has_table_privilege('rr_commerce', 'auth_schema.users', 'SELECT') THEN
        RAISE EXCEPTION 'Isolation Failure: rr_commerce unexpectedly has SELECT on auth_schema.users';
    END IF;

    -- Test rr_commerce has NO access to finance_schema.operating_expenses
    IF has_table_privilege('rr_commerce', 'finance_schema.operating_expenses', 'SELECT') THEN
        RAISE EXCEPTION 'Isolation Failure: rr_commerce unexpectedly has SELECT on finance_schema.operating_expenses';
    END IF;

    -- Test rr_production has SELECT on procurement_schema.purchase_order_lines
    IF NOT has_table_privilege('rr_production', 'procurement_schema.purchase_order_lines', 'SELECT') THEN
        RAISE EXCEPTION 'Parity Failure: rr_production lacks SELECT on procurement_schema.purchase_order_lines';
    END IF;

    -- Test rr_production has NO INSERT on procurement_schema.purchase_order_lines
    IF has_table_privilege('rr_production', 'procurement_schema.purchase_order_lines', 'INSERT') THEN
        RAISE EXCEPTION 'Isolation Failure: rr_production unexpectedly has INSERT on procurement_schema.purchase_order_lines';
    END IF;

    -- Test rr_custfin has SELECT on public.form_definitions
    IF NOT has_table_privilege('rr_custfin', 'public.form_definitions', 'SELECT') THEN
        RAISE EXCEPTION 'Parity Failure: rr_custfin lacks SELECT on public.form_definitions';
    END IF;

    -- Test sequence usage
    IF NOT has_sequence_privilege('rr_commerce', 'commerce_schema.stock_outbox_id_seq', 'USAGE') THEN
        RAISE EXCEPTION 'Parity Failure: rr_commerce lacks USAGE on commerce_schema.stock_outbox_id_seq';
    END IF;

    IF NOT has_sequence_privilege('rr_custfin', 'customer_schema.stock_outbox_id_seq', 'USAGE') THEN
        RAISE EXCEPTION 'Parity Failure: rr_custfin lacks USAGE on customer_schema.stock_outbox_id_seq';
    END IF;

    RAISE NOTICE ' [PASS] Service role isolation and least-privilege matrix verified successfully.';
    RAISE NOTICE '=============================================================';
    RAISE NOTICE '>>> SCHEMA PARITY & SECURITY AUDIT: ALL CHECKS PASSED (100%%)';
    RAISE NOTICE '=============================================================';
END $$;
