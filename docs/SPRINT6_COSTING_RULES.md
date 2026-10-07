# RR JAGGERY TRADERS — SPRINT 6 FINANCIAL & PRODUCTION COSTING RULES

## 1. Executive Summary & Purpose
This document specifies the authoritative financial and production costing rules implemented in Sprint 6 for RR Jaggery Traders.
All monetary calculations are performed using `java.math.BigDecimal` with deterministic rounding rules to guarantee auditable financial integrity.

---

## 2. Production Batch Costing Engine

### 2.1 Formula Architecture
For every completed production batch, the total production cost is the exact sum of five traceable cost categories:

$$\text{Total Batch Cost} = \text{Raw Material Cost} + \text{Labour Cost} + \text{Fuel Cost} + \text{Packaging Cost} + \text{Other Allocated Costs}$$

$$\text{Cost Per KG} = \frac{\text{Total Batch Cost}}{\text{Finished Production Output Quantity (KG)}}$$

### 2.2 Cost Components & Traceability

1. **Raw Material Cost**:
   - **Source**: Actual physical raw materials consumed recorded in `production_schema.batch_consumptions` multiplied by the unit cost of that SKU.
   - **Unit Cost Determination**:
     - Authoritative purchase receipt unit cost from `procurement_schema.purchase_order_lines` / `goods_receipts`.
     - Standard baseline raw material purchase price if unlinked directly:
       - `RM-SUGARCANE` / Cane: ₹45.00 / KG (or actual PO line unit cost)
       - `RM-LIME` / Clarifier Lime: ₹20.00 / KG
       - `RM-MUSTARD-OIL` / Oil Clarifier: ₹160.00 / L / KG
       - `RM-BAGASSE` / Biomass: ₹3.50 / KG
   - $\text{Raw Material Cost} = \sum (\text{Consumed Quantity}_i \times \text{Unit Cost}_i)$

2. **Labour Cost**:
   - **Rule**: Direct production labour allocation.
   - **Standard Production Allocation Rate**: Configured baseline of **₹12.50 per KG** of finished output (or worker attendance allocation for batch hours).
   - $\text{Labour Cost} = \text{Finished Output Quantity (KG)} \times \text{Labour Rate per KG (₹12.50)}$

3. **Fuel Cost**:
   - **Rule**: Energy and fuel (bagasse combustion, boiler wood, diesel for crusher/furnace).
   - **Standard Production Allocation Rate**: Configured baseline of **₹6.50 per KG** of finished output.
   - $\text{Fuel Cost} = \text{Finished Output Quantity (KG)} \times \text{Fuel Rate per KG (₹6.50)}$

4. **Packaging Cost**:
   - **Rule**: Food-grade moulds, corrugated boxes, vacuum bags, and labeling.
   - **Standard Production Allocation Rate**: Configured baseline of **₹4.00 per KG** of finished output.
   - $\text{Packaging Cost} = \text{Finished Output Quantity (KG)} \times \text{Packaging Rate per KG (₹4.00)}$

5. **Other Allocated Costs**:
   - **Rule**: Factory power, equipment depreciation, maintenance, and quality inspection.
   - **Standard Production Allocation Rate**: Configured baseline of **₹2.00 per KG** of finished output.
   - $\text{Other Cost} = \text{Finished Output Quantity (KG)} \times \text{Other Rate per KG (₹2.00)}$

### 2.3 Rounding & Precision Rules
- **Monetary Fields**: `BigDecimal` with scale = 2, using `RoundingMode.HALF_UP` (e.g. ₹12450.75).
- **Quantities**: `BigDecimal` with scale = 3, using `RoundingMode.HALF_UP` (e.g. 95.000 KG).
- **Cost Per KG**: `BigDecimal` with scale = 2, using `RoundingMode.HALF_UP` (e.g. ₹68.42 / KG).

### 2.4 Guard Rails & Edge Cases
1. **Zero Finished Output Guard**:
   - If $\text{Finished Quantity} \le 0$, the system **MUST NOT** perform division by zero.
   - Costing status is marked as `COSTING_INVALID`.
   - `costPerKg` is returned as `null` or marked with a clear validation flag.
2. **Incomplete or Cancelled Batches**:
   - Batches with status `PLANNED`, `MATERIALS_READY`, `IN_PRODUCTION`, `QUALITY_CHECK`, or `CANCELLED` are not finalized.
   - Only `COMPLETED` batches have final authoritative unit costing.
3. **Wastage Treatment**:
   - Normal process loss (e.g. moisture evaporation during boiling) is absorbed into the batch output cost (increasing Cost/KG naturally).
   - Abnormal scrap/wastage recorded in `batch_wastages` is tracked separately as loss.

---

## 3. Operating Expenses vs Inventory Purchases

### 3.1 Strict Separation of Concepts
To prevent double-counting:
- **Inventory Purchases (Procurement)**:
  - Recorded in `procurement_schema.purchase_orders` and `goods_receipts`.
  - Creates an Inventory Asset balance and a Supplier Payable liability.
  - **NOT** recorded as an operating expense upon invoice receipt.
- **Operating Expenses (Finance)**:
  - Recorded in `finance_schema.operating_expenses`.
  - Represents direct period overheads (Rent, Electricity, Repairs, Maintenance, Transportation, Fuel, etc.).
- **Profit & Loss Integrity**:
  - The system does not use naive formulas like `Sales - Purchases` or `Sales - All Expenses`.
  - COGS is derived from batch production costing when items are sold.

### 3.2 Supported Expense Categories
1. `Raw Materials`
2. `Fuel / Diesel`
3. `Electricity`
4. `Transport`
5. `Packaging`
6. `Maintenance`
7. `Labour`
8. `Rent`
9. `Repairs`
10. `Machinery`
11. `Moulds`
12. `Salary / Payroll`
13. `Other / Miscellaneous`

---

## 4. Employee Master & Salary Calculation Rules

### 4.1 Employee Wage Types
1. **`MONTHLY`**:
   - Base monthly salary $S_{\text{monthly}}$.
   - Working days in standard month $D_{\text{total}}$ (typically 26 or 30).
   - Payable days $D_{\text{payable}} = \text{Present Days} + (0.5 \times \text{Half Days})$.
   - $\text{Base Earned} = \frac{S_{\text{monthly}}}{D_{\text{total}}} \times D_{\text{payable}}$ (or full monthly salary if zero unauthorized absences).
   - $\text{Overtime Pay} = \text{Overtime Hours} \times \left(\frac{S_{\text{monthly}}}{D_{\text{total}} \times 8}\right) \times 1.5$.
2. **`DAILY`**:
   - Daily rate $R_{\text{daily}}$.
   - Payable days $D_{\text{payable}} = \text{Present Days} + (0.5 \times \text{Half Days})$.
   - $\text{Base Earned} = R_{\text{daily}} \times D_{\text{payable}}$.
   - $\text{Overtime Pay} = \text{Overtime Hours} \times \left(\frac{R_{\text{daily}}}{8}\right) \times 1.5$.
3. **`BATCH_BASED`**:
   - Rate per batch $R_{\text{batch}}$.
   - $\text{Base Earned} = R_{\text{batch}} \times \text{Completed Batches Assigned}$.
4. **`PER_KG`**:
   - Rate per KG $R_{\text{kg}}$.
   - $\text{Base Earned} = R_{\text{kg}} \times \text{Production Quantity (KG)}$.
5. **`CONTRACT`**:
   - Agreed lump-sum contract amount for the period or milestone.

### 4.2 Payroll Calculation Formula
For an employee $E$ in period $(Y, M)$:

$$\text{Gross Salary} = \text{Base Earned} + \text{Overtime Pay} + \text{Bonus/Allowances}$$

$$\text{Total Deductions} = \text{Salary Deductions (Penalties, Taxes)} + \text{Recovered Salary Advances}$$

$$\text{Net Payable} = \text{Gross Salary} - \text{Total Deductions}$$

$$\text{Remaining Balance} = \text{Net Payable} - \text{Salary Amount Paid}$$

---

## 5. Employee Salary Ledger & Financial Reconciliation

### 5.1 Double-Entry Principles for Employee Ledger
Each financial event writes an immutable transaction to `finance_schema.employee_ledger`:

| Transaction Type | Financial Meaning | Debit (₹) | Credit (₹) | Running Balance Effect |
|---|---|---|---|---|
| `SALARY_ACCRUED` | Gross salary earned by employee for the payroll period | ₹0.00 | Gross Salary | + Balance (Company owes employee) |
| `ADVANCE_PAID` | Advance cash/UPI given to employee | Advance Amount | ₹0.00 | - Balance (Reduces debt / creates employee receivable) |
| `DEDUCTION` | Penalties, damaged tools, or statutory deductions | Deduction Amount | ₹0.00 | - Balance (Reduces company liability) |
| `SALARY_PAYMENT` | Disbursed salary payment to worker | Paid Amount | ₹0.00 | - Balance (Settles company liability) |

### 5.2 Balance Reconciliation Formula
$$\text{Employee Closing Balance} = \sum \text{Credits} - \sum \text{Debits}$$
A positive closing balance indicates salary owed to the employee; zero indicates full settlement.
