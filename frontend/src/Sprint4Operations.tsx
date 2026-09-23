import { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { AlertCircle, ClipboardList, Eye, Package, Plus, RefreshCw, Truck, Users, X } from 'lucide-react';

const INVENTORY_BASE = '/api/v1/inventory';
const PROCUREMENT_BASE = '/api/v1/procurement';

type Props = { token: string; userName?: string };
type Item = { id: string; sku: string; itemName: string; itemType: string; unitOfMeasure?: string; currentQuantity: number; minimumStockLevel: number };
type Supplier = { id: string; supplierCode: string; supplierName: string; contactName?: string; phone?: string; email?: string; gstin?: string; paymentTermsDays?: number };
type PurchaseOrder = { id: string; orderNumber?: string; supplierId: string; status?: string; expectedDeliveryDate?: string; totalAmount?: number; lines?: Array<{ sku: string; itemName: string; orderedQuantity: number; unitCost: number }> };

function headers(token: string) { return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }; }
async function request<T>(url: string, token: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { ...init, headers: { ...headers(token), ...(init?.headers || {}) } });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message || body.error || body.errors?.join?.(', ') || `Request failed (${response.status})`);
  return (body.data ?? body) as T;
}
const money = (value: number | string | undefined) => `₹${Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;

export function Sprint4Operations({ token, userName = 'ADMIN' }: Props) {
  const [tab, setTab] = useState<'inventory' | 'suppliers' | 'orders' | 'receipts'>('inventory');
  const [items, setItems] = useState<Item[]>([]);
  const [lowStock, setLowStock] = useState<Item[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [orders, setOrders] = useState<PurchaseOrder[]>([]);
  const [selected, setSelected] = useState<any | null>(null);
  const [movements, setMovements] = useState<any[]>([]);
  const [ledger, setLedger] = useState<any[]>([]);
  const [receipts, setReceipts] = useState<any[]>([]);
  const [outstanding, setOutstanding] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [show, setShow] = useState<'item' | 'supplier' | 'order' | 'adjust' | 'reconcile' | 'receipt' | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [inventory, low, supplierList, orderList] = await Promise.all([
        request<Item[]>(`${INVENTORY_BASE}/items`, token), request<Item[]>(`${INVENTORY_BASE}/items/low-stock`, token),
        request<Supplier[]>(`${PROCUREMENT_BASE}/suppliers`, token), request<PurchaseOrder[]>(`${PROCUREMENT_BASE}/purchase-orders`, token),
      ]);
      setItems(inventory || []); setLowStock(low || []); setSuppliers(supplierList || []); setOrders(orderList || []);
    } catch (e) { setError((e as Error).message); } finally { setLoading(false); }
  }, [token]);
  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    const close = () => setShow(null);
    window.addEventListener('sprint4-close', close);
    return () => window.removeEventListener('sprint4-close', close);
  }, []);

  const run = async (url: string, body: unknown, message: string) => {
    setError(''); setNotice('');
    try { await request(url, token, { method: 'POST', body: JSON.stringify(body) }); setNotice(message); setShow(null); await load(); }
    catch (e) { setError((e as Error).message); }
  };
  const openItem = async (item: Item) => {
    setSelected(item); setError('');
    try { setMovements(await request<any[]>(`${INVENTORY_BASE}/items/${item.id}/movements`, token)); }
    catch (e) { setError((e as Error).message); }
  };
  const openSupplier = async (supplier: Supplier) => {
    setError('');
    try {
      const [entries, balance] = await Promise.all([
        request<any[]>(`${PROCUREMENT_BASE}/suppliers/${supplier.id}/ledger`, token),
        request<number>(`${PROCUREMENT_BASE}/suppliers/${supplier.id}/outstanding`, token),
      ]);
      setLedger(entries || []); setOutstanding(Number(balance || 0)); setSelected(supplier);
    } catch (e) { setError((e as Error).message); }
  };
  const openOrder = async (order: PurchaseOrder) => {
    setError('');
    try {
      const list = await request<any[]>(`${PROCUREMENT_BASE}/purchase-orders/${order.id}/receipts`, token);
      setReceipts(list || []);
      setSelected(order);
    } catch (e) { setError((e as Error).message); }
  };

  return <div className="space-y-5 animate-fadeIn">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div><h2 className="text-2xl font-black text-white">Inventory & Procurement Operations</h2><p className="text-xs text-slate-400 mt-1">Live stock, purchasing, receipts and supplier payables</p></div>
      <div className="flex gap-2"><button onClick={load} className="p-2 rounded-xl border border-slate-700 bg-slate-900"><RefreshCw className={`h-4 w-4 text-amber-400 ${loading ? 'animate-spin' : ''}`} /></button><button onClick={() => setShow(tab === 'inventory' ? 'item' : tab === 'suppliers' ? 'supplier' : tab === 'orders' ? 'order' : 'receipt')} className="px-3 py-2 rounded-xl bg-amber-500 text-slate-950 font-bold text-xs"><Plus className="h-4 w-4 inline mr-1" />New</button></div>
    </div>
    {error && <div className="p-3 rounded-xl border border-red-500/30 bg-red-500/10 text-red-300 text-sm"><AlertCircle className="h-4 w-4 inline mr-2" />{error}</div>}
    {notice && <div className="p-3 rounded-xl border border-emerald-500/30 bg-emerald-500/10 text-emerald-300 text-sm">{notice}</div>}
    <div className="flex flex-wrap gap-1 bg-slate-900 p-1 rounded-xl border border-slate-800 w-fit">
      {([['inventory', Package, 'Inventory'], ['suppliers', Users, 'Suppliers & Ledger'], ['orders', ClipboardList, 'Purchase Orders'], ['receipts', Truck, 'Goods Receipt']] as const).map(([key, Icon, label]) => <button key={key} onClick={() => setTab(key)} className={`px-3 py-2 rounded-lg text-xs font-semibold ${tab === key ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}><Icon className="h-3.5 w-3.5 inline mr-1" />{label}</button>)}
    </div>
    {tab === 'inventory' && <InventoryTab items={items} lowStock={lowStock} onOpen={openItem} onAdjust={() => setShow('adjust')} onReconcile={() => setShow('reconcile')} />}
    {tab === 'suppliers' && <SupplierTab suppliers={suppliers} onOpen={openSupplier} />}
    {tab === 'orders' && <OrderTab orders={orders} suppliers={suppliers} onReceive={() => setShow('receipt')} onOpen={openOrder} />}
    {tab === 'receipts' && <ReceiptTab orders={orders} onSubmit={() => setShow('receipt')} />}
    {show === 'item' && <ItemForm run={run} />}
    {show === 'supplier' && <SupplierForm run={run} />}
    {show === 'order' && <OrderForm suppliers={suppliers} run={run} />}
    {show === 'adjust' && <AdjustForm items={items} run={run} userName={userName} />}
    {show === 'reconcile' && <ReconcileForm items={items} run={run} userName={userName} />}
    {show === 'receipt' && <ReceiptForm orders={orders} run={run} userName={userName} />}
    {selected && <DetailModal value={selected} movements={movements} ledger={ledger} outstanding={outstanding} receipts={receipts} onClose={() => { setSelected(null); setLedger([]); setMovements([]); setReceipts([]); }} />}
  </div>;
}

function InventoryTab({ items, lowStock, onOpen, onAdjust, onReconcile }: { items: Item[]; lowStock: Item[]; onOpen: (item: Item) => void; onAdjust: () => void; onReconcile: () => void }) {
  return <div className="space-y-4"><div className="grid grid-cols-1 sm:grid-cols-3 gap-3"><Metric label="Total SKUs" value={items.length} /><Metric label="Low stock" value={lowStock.length} danger /><Metric label="Stock quantity" value={items.reduce((n, i) => n + Number(i.currentQuantity || 0), 0).toFixed(3)} /></div><div className="flex gap-2"><button onClick={onAdjust} className="px-3 py-2 rounded-lg bg-slate-800 text-xs text-white">Stock adjustment</button><button onClick={onReconcile} className="px-3 py-2 rounded-lg bg-slate-800 text-xs text-white">Reconcile count</button></div><Table headers={['SKU / Item', 'Type', 'On hand', 'Minimum', 'Status', '']} rows={items.map(i => [<><b>{i.sku}</b><span className="block text-slate-400">{i.itemName}</span></>, i.itemType, `${i.currentQuantity} ${i.unitOfMeasure || 'KG'}`, i.minimumStockLevel, Number(i.currentQuantity) <= Number(i.minimumStockLevel) ? <span className="text-red-300">LOW</span> : <span className="text-emerald-300">OK</span>, <button onClick={() => onOpen(i)} className="text-amber-400"><Eye className="h-4 w-4" /></button>])} /></div>;
}
function SupplierTab({ suppliers, onOpen }: { suppliers: Supplier[]; onOpen: (supplier: Supplier) => void }) { return <Table headers={['Code', 'Supplier', 'Contact', 'Terms', '']} rows={suppliers.map(s => [s.supplierCode, <b>{s.supplierName}</b>, `${s.contactName || '—'} ${s.phone || ''}`, `${s.paymentTermsDays || 0} days`, <button onClick={() => onOpen(s)} className="text-amber-400"><Eye className="h-4 w-4" /></button>])} />; }
function OrderTab({ orders, suppliers, onReceive, onOpen }: { orders: PurchaseOrder[]; suppliers: Supplier[]; onReceive: () => void; onOpen: (order: PurchaseOrder) => void }) { return <><Table headers={['Order', 'Supplier', 'Expected', 'Status', 'Total', '']} rows={orders.map(o => [o.orderNumber || o.id.slice(0, 8), suppliers.find(s => s.id === o.supplierId)?.supplierName || o.supplierId?.slice(0, 8), o.expectedDeliveryDate || '—', o.status || '—', money(o.totalAmount), <button onClick={() => onOpen(o)} className="text-amber-400" title="View details & receipts"><Eye className="h-4 w-4" /></button>])} /><button onClick={onReceive} className="px-3 py-2 rounded-lg bg-amber-500 text-slate-950 text-xs font-bold">Record goods receipt</button></>; }
function ReceiptTab({ orders, onSubmit }: { orders: PurchaseOrder[]; onSubmit: () => void }) { return <div className="glass-card rounded-2xl p-5"><h3 className="font-bold text-white">Goods receipt workflow</h3><p className="text-sm text-slate-400 mt-1">{orders.length} purchase orders available for receipt. A receipt posts stock through the backend inventory integration.</p><button onClick={onSubmit} className="mt-4 px-3 py-2 rounded-lg bg-amber-500 text-slate-950 text-xs font-bold">Receive against PO</button></div>; }
function Metric({ label, value, danger }: { label: string; value: string | number; danger?: boolean }) { return <div className="glass-card rounded-2xl p-4"><p className="text-xs text-slate-400">{label}</p><p className={`text-2xl font-black mt-1 ${danger ? 'text-red-300' : 'text-white'}`}>{value}</p></div>; }
function Table({ headers, rows }: { headers: string[]; rows: React.ReactNode[][] }) { return <div className="glass-card rounded-2xl overflow-x-auto"><table className="w-full text-xs"><thead><tr className="border-b border-slate-800 bg-slate-900/70">{headers.map(h => <th key={h} className="text-left px-4 py-3 text-slate-400 font-semibold">{h}</th>)}</tr></thead><tbody>{rows.length ? rows.map((row, i) => <tr key={i} className="border-b border-slate-800/60 hover:bg-slate-800/30">{row.map((cell, j) => <td key={j} className="px-4 py-3 text-slate-200">{cell}</td>)}</tr>) : <tr><td colSpan={headers.length} className="p-8 text-center text-slate-500">No records found</td></tr>}</tbody></table></div>; }
function FormModal({ title, children }: { title: string; children: ReactNode }) { return <div className="fixed inset-0 z-50 bg-slate-950/80 flex items-center justify-center p-4"><div className="bg-slate-900 border border-slate-700 rounded-2xl p-5 w-full max-w-lg max-h-[90vh] overflow-y-auto"><h3 className="text-lg font-bold text-white mb-4">{title}</h3>{children}</div></div>; }
function Field({ label, name, type = 'text', required = true }: { label: string; name: string; type?: string; required?: boolean }) { return <label className="block text-xs text-slate-400">{label}<input name={name} type={type} required={required} min={type === 'number' ? 0 : undefined} className="mt-1 w-full rounded-lg bg-slate-950 border border-slate-700 px-3 py-2 text-sm text-white" /></label>; }
function Actions({ children }: { children: ReactNode }) { return <div className="flex justify-end gap-2 mt-5">{children}</div>; }
function Cancel() { return <button type="button" onClick={() => window.dispatchEvent(new CustomEvent('sprint4-close'))} className="px-3 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs">Cancel</button>; }
function Form({ title, onSubmit, children }: { title: string; onSubmit: (form: HTMLFormElement) => void; children: ReactNode }) { return <FormModal title={title}><form onSubmit={e => { e.preventDefault(); onSubmit(e.currentTarget); }}>{children}<Actions><Cancel /><button className="px-3 py-2 rounded-lg bg-amber-500 text-slate-950 text-xs font-bold">Save</button></Actions></form></FormModal>; }
const values = (form: HTMLFormElement) => Object.fromEntries(new FormData(form).entries());
function ItemForm({ run }: { run: (url: string, body: unknown, message: string) => void }) { return <Form title="Create inventory item" onSubmit={f => { const v = values(f); run(`${INVENTORY_BASE}/items`, { ...v, initialQuantity: Number(v.initialQuantity || 0), minimumStockLevel: Number(v.minimumStockLevel || 0) }, 'Inventory item created.'); }}><div className="grid gap-3"><Field label="SKU" name="sku" /><Field label="Item name" name="itemName" /><label className="text-xs text-slate-400">Item type<select name="itemType" className="mt-1 w-full rounded-lg bg-slate-950 border border-slate-700 px-3 py-2 text-sm text-white"><option>RAW_MATERIAL</option><option>FINISHED_GOOD</option></select></label><Field label="Initial quantity" name="initialQuantity" type="number" required={false} /><Field label="Minimum stock level" name="minimumStockLevel" type="number" required={false} /></div></Form>; }
function SupplierForm({ run }: { run: (url: string, body: unknown, message: string) => void }) { return <Form title="Add supplier" onSubmit={f => { const v = values(f); run(`${PROCUREMENT_BASE}/suppliers`, { ...v, paymentTermsDays: Number(v.paymentTermsDays || 30) }, 'Supplier created.'); }}><div className="grid gap-3"><Field label="Supplier code" name="supplierCode" /><Field label="Supplier name" name="supplierName" /><Field label="Contact name" name="contactName" required={false} /><Field label="Phone" name="phone" required={false} /><Field label="Email" name="email" type="email" required={false} /><Field label="GSTIN" name="gstin" required={false} /><Field label="Payment terms (days)" name="paymentTermsDays" type="number" required={false} /></div></Form>; }
function OrderForm({ suppliers, run }: { suppliers: Supplier[]; run: (url: string, body: unknown, message: string) => void }) { return <Form title="Create purchase order" onSubmit={f => { const v = values(f); run(`${PROCUREMENT_BASE}/purchase-orders`, { supplierId: v.supplierId, expectedDeliveryDate: v.expectedDeliveryDate, notes: v.notes, lines: [{ sku: v.sku, itemName: v.itemName, orderedQuantity: Number(v.orderedQuantity), unitCost: Number(v.unitCost) }] }, 'Purchase order created.'); }}><div className="grid gap-3"><label className="text-xs text-slate-400">Supplier<select name="supplierId" required className="mt-1 w-full rounded-lg bg-slate-950 border border-slate-700 px-3 py-2 text-sm text-white">{suppliers.map(s => <option key={s.id} value={s.id}>{s.supplierName}</option>)}</select></label><Field label="Expected delivery" name="expectedDeliveryDate" type="date" /><Field label="SKU" name="sku" /><Field label="Item name" name="itemName" /><Field label="Quantity" name="orderedQuantity" type="number" /><Field label="Unit cost" name="unitCost" type="number" /><Field label="Notes" name="notes" required={false} /></div></Form>; }
function AdjustForm({ items, run, userName }: { items: Item[]; run: (url: string, body: unknown, message: string) => void; userName: string }) { return <Form title="Stock adjustment" onSubmit={f => { const v = values(f); run(`${INVENTORY_BASE}/stock/adjust`, { itemId: v.itemId, quantity: Number(v.quantity), reason: v.reason, actor: userName, notes: v.notes }, 'Stock adjusted.'); }}><div className="grid gap-3"><SelectItem items={items} /><Field label="Quantity delta (negative to issue)" name="quantity" type="number" /><Field label="Reason" name="reason" /><Field label="Notes" name="notes" required={false} /></div></Form>; }
function ReconcileForm({ items, run, userName }: { items: Item[]; run: (url: string, body: unknown, message: string) => void; userName: string }) { return <Form title="Reconcile physical count" onSubmit={f => { const v = values(f); run(`${INVENTORY_BASE}/reconcile`, { itemId: v.itemId, countedQuantity: Number(v.countedQuantity), reason: v.reason, actor: userName, notes: v.notes }, 'Inventory reconciliation completed.'); }}><div className="grid gap-3"><SelectItem items={items} /><Field label="Counted quantity" name="countedQuantity" type="number" /><Field label="Reason" name="reason" /><Field label="Notes" name="notes" required={false} /></div></Form>; }
function SelectItem({ items }: { items: Item[] }) { return <label className="text-xs text-slate-400">Item<select name="itemId" required className="mt-1 w-full rounded-lg bg-slate-950 border border-slate-700 px-3 py-2 text-sm text-white">{items.map(i => <option key={i.id} value={i.id}>{i.sku} — {i.itemName}</option>)}</select></label>; }
function ReceiptForm({ orders, run, userName }: { orders: PurchaseOrder[]; run: (url: string, body: unknown, message: string) => void; userName: string }) { return <Form title="Record goods receipt" onSubmit={f => { const v = values(f); run(`${PROCUREMENT_BASE}/goods-receipts`, { purchaseOrderId: v.purchaseOrderId, sku: v.sku, quantity: Number(v.quantity), receivedBy: userName, notes: v.notes }, 'Goods receipt recorded and inventory updated.'); }}><div className="grid gap-3"><label className="text-xs text-slate-400">Purchase order<select name="purchaseOrderId" required className="mt-1 w-full rounded-lg bg-slate-950 border border-slate-700 px-3 py-2 text-sm text-white">{orders.map(o => <option key={o.id} value={o.id}>{o.orderNumber || o.id.slice(0, 8)}</option>)}</select></label><Field label="SKU" name="sku" /><Field label="Quantity received" name="quantity" type="number" /><Field label="Notes" name="notes" required={false} /></div></Form>; }
function DetailModal({ value, movements, ledger, outstanding, receipts, onClose }: { value: any; movements: any[]; ledger: any[]; outstanding: number | null; receipts: any[]; onClose: () => void }) {
  const isSupplier = 'supplierName' in value;
  const isOrder = 'supplierId' in value || 'orderNumber' in value;
  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl p-5 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between">
          <h3 className="text-lg font-bold text-white">
            {isSupplier ? value.supplierName : isOrder ? `Purchase Order ${value.orderNumber || value.id?.slice(0, 8)}` : `${value.sku} — ${value.itemName}`}
          </h3>
          <button onClick={onClose}><X className="text-slate-400" /></button>
        </div>
        {isSupplier ? (
          <>
            <p className="mt-3 text-amber-300">Outstanding: {money(outstanding || 0)}</p>
            <Table headers={['Date', 'Type', 'Debit', 'Credit', 'Balance']} rows={ledger.map(e => [e.createdAt || e.transactionDate, e.entryType || e.transactionType, money(e.debitAmount), money(e.creditAmount), money(e.runningBalance ?? e.balanceAfter)])} />
          </>
        ) : isOrder ? (
          <>
            <div className="grid grid-cols-3 gap-3 mt-4">
              <Metric label="Status" value={value.status || '—'} />
              <Metric label="Expected" value={value.expectedDeliveryDate || '—'} />
              <Metric label="Total" value={money(value.totalAmount)} />
            </div>
            {value.lines && value.lines.length > 0 && (
              <>
                <h4 className="font-bold text-white mt-5 mb-2">Order Lines</h4>
                <Table headers={['SKU', 'Item Name', 'Quantity', 'Unit Cost']} rows={value.lines.map((l: any) => [l.sku, l.itemName || '—', l.orderedQuantity, money(l.unitCost)])} />
              </>
            )}
            <h4 className="font-bold text-white mt-5 mb-2">Goods Receipts History</h4>
            <Table headers={['Receipt Date', 'SKU', 'Qty Received', 'Received By', 'Notes']} rows={receipts.map(r => [r.receiptTimestamp ? new Date(r.receiptTimestamp).toLocaleString('en-IN') : (r.receivedAt || '—'), r.sku, r.receivedQuantity, r.receivedBy || '—', r.notes || '—'])} />
          </>
        ) : (
          <>
            <div className="grid grid-cols-3 gap-3 mt-4">
              <Metric label="On hand" value={value.currentQuantity} />
              <Metric label="Minimum" value={value.minimumStockLevel} />
              <Metric label="Type" value={value.itemType} />
            </div>
            <h4 className="font-bold text-white mt-5 mb-2">Movement audit</h4>
            <Table headers={['Date', 'Type', 'Quantity', 'Reason', 'Actor']} rows={movements.map(m => [m.movementTime || m.movementDate || m.createdAt, m.movementType, m.quantity, m.reason || '—', m.actor || '—'])} />
          </>
        )}
      </div>
    </div>
  );
}
