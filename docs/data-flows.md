# RR Jaggery Traders — Core Business Data Flows

## 1. End-to-End Supply-to-Sale Data Flow

The lifecycle connects agricultural procurement, production transformations, finished goods stocking, and commercial fulfillment.

```mermaid
sequenceDiagram
    autonumber
    actor Farmer as Cane Farmer / Supplier
    participant Procure as Procurement Service
    participant Inv as Inventory Service
    participant Prod as Production Service
    participant Comm as Commerce Service
    participant Cust as Customer & Ledger Svc
    participant Fin as Finance Service

    Farmer->>Procure: Delivers Raw Sugarcane (Weighbridge Challan)
    Procure->>Inv: Inward Stock Movement (Raw Sugarcane +300 Quintals)
    Procure->>Procure: Record Supplier Ledger Credit Entry
    
    Prod->>Inv: Check Raw Material Availability (Sugarcane, Lime, Fuel)
    Prod->>Prod: Start Batch (Crushing -> Boiling -> Setting)
    Prod->>Inv: Record Outward Movement (Raw Material Consumption)
    Prod->>Prod: Final Output (30 Quintals Jaggery Blocks + Bagasse Scrap)
    Prod->>Inv: Inward Stock Movement (Finished Goods Stock +30 Quintals)
    Prod->>Fin: Sync Batch Production Costs (Raw Cost + Fuel + Labor)
    
    Comm->>Inv: Check Finished Goods Available Stock
    Comm->>Cust: Validate Customer Account / Credit Limit
    Comm->>Cust: Issue Invoice & Create Ledger Debit Entry
    Comm->>Inv: Record Outward Movement (Sale Dispatch)
    Cust->>Cust: Record Customer Payment (Cash / UPI / Bank) & Ledger Credit Entry
```

---

## 2. Offline / Unregistered Wholesale Flow (Critical Business Workflow)

This workflow serves wholesale traders who order directly via phone, dispatch desk, or in-person at the manufacturing facility without having a website user account.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin / Sales Operator
    participant CustSvc as Customer & Ledger Svc
    participant InvSvc as Inventory Svc
    participant CommSvc as Commerce Svc

    Admin->>CustSvc: 1. Create Offline Wholesale Customer\n(Business Name, Contact, GSTIN, Credit Limit: ₹5,00,000, Credit Days: 30)
    Note over CustSvc: Stored with user_id = NULL in customer_schema
    CustSvc-->>Admin: Customer Created (ID: CUST-OFF-101)

    Admin->>CommSvc: 2. Create Offline Wholesale Order\n(Selected: 200 Boxes x 10kg Blocks @ ₹42/kg = ₹84,000 + 5% GST = ₹88,200)
    
    CommSvc->>CustSvc: Check Outstanding Balance + New Order <= Credit Limit
    CustSvc-->>CommSvc: Credit Approved (Current: ₹0, Limit: ₹5,00,000)

    CommSvc->>InvSvc: Verify & Reserve Stock (2,000 KG Block Jaggery)
    InvSvc-->>CommSvc: Stock Reserved Successfully

    CommSvc->>CommSvc: Generate Formal Tax Invoice (INV-2026-0042)
    
    CommSvc->>InvSvc: Execute Stock Movement\n(Type: SALE_DISPATCH, Ref: INV-2026-0042, Delta: -2000 KG)
    InvSvc-->>CommSvc: Stock Deducted & Balance Updated

    CommSvc->>CustSvc: Record Ledger Debit Transaction\n(Type: INVOICE, Amount: ₹88,200, Ref: INV-2026-0042)
    CustSvc->>CustSvc: Update Running Outstanding Balance (New Balance: ₹88,200)

    Admin->>CustSvc: 3. Record Immediate Partial Payment (e.g. ₹50,000 via NEFT)
    CustSvc->>CustSvc: Record Ledger Credit Transaction\n(Type: PAYMENT, Amount: ₹50,000, Mode: NEFT)
    CustSvc->>CustSvc: Recalculate Outstanding (₹88,200 - ₹50,000 = ₹38,200 Outstanding)
    CustSvc-->>Admin: Updated Customer Ledger Profile (₹38,200 due in 30 days)
```

---

## 3. Batch Production & Costing Flow

The production process transforms agricultural feedstock into packaged jaggery while capturing cost variances.

```mermaid
sequenceDiagram
    autonumber
    actor PM as Production Manager
    participant ProdSvc as Production Service
    participant InvSvc as Inventory Service
    participant FinSvc as Finance Service

    PM->>ProdSvc: Create Batch (Recipe: Organic Solid Block, Target: 1,000 KG)
    ProdSvc->>InvSvc: Check Stock for Sugarcane (10,000 KG), Lime (20 KG), Firewood (1,500 KG)
    InvSvc-->>ProdSvc: Stock Available
    
    PM->>ProdSvc: Start Stage 1: Crushing (Juice Extraction)
    PM->>ProdSvc: Start Stage 2: Boiling & Clarification (Scum Removal)
    PM->>ProdSvc: Start Stage 3: Concentration & Cooling in cooling pans
    PM->>ProdSvc: Start Stage 4: Moulding & Setting
    
    PM->>ProdSvc: Record Actual Material Consumption\n(Sugarcane: 10,200 KG, Lime: 22 KG, Fuel: 1,600 KG)
    ProdSvc->>InvSvc: Execute Stock Movements (Type: PRODUCTION_CONSUMPTION)
    
    PM->>ProdSvc: Record Output & Wastage\n(Actual FG Output: 980 KG, Wastage: 40 KG Scum + Bagasse)
    ProdSvc->>InvSvc: Execute Stock Movement (Type: PRODUCTION_OUTPUT, Delta: +980 KG)
    
    ProdSvc->>FinSvc: Query Labor & Allocated Utilities for Batch
    FinSvc-->>ProdSvc: Direct Labor: ₹4,200, Allocated Power/Fuel: ₹3,800
    
    ProdSvc->>ProdSvc: Calculate Batch Costing:\nTotal Raw Material Cost + Labor + Utilities = Total Batch Cost\nCost / KG = Total Cost / 980 KG\nYield % = (980 / 1000) * 100 = 98.0%
    ProdSvc-->>PM: Batch Closed (Cost: ₹38.50/KG, Yield: 98.0%)
```

---

## 4. Financial Ledger Audit Flow

```mermaid
stateDiagram-v2
    [*] --> InvoiceGenerated : Sale / Order Finalized
    InvoiceGenerated --> DebitPosted : Post Debit to Customer Ledger
    DebitPosted --> BalanceIncreased : Outstanding Balance Increases
    
    BalanceIncreased --> PaymentReceived : Customer Submits Payment
    PaymentReceived --> CreditPosted : Post Credit to Customer Ledger
    CreditPosted --> BalanceReduced : Outstanding Balance Decreases
    
    BalanceReduced --> Reconciled : Balance = 0.00
    BalanceReduced --> OverdueAlert : Days Elapsed > Credit Days
    OverdueAlert --> [*]
```
