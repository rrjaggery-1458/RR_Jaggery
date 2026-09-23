import { useState, useEffect, useCallback } from 'react';
import {
  Users, BookOpen, Wallet, Phone, Mail, FileSpreadsheet,
  AlertTriangle, Plus, Search, RefreshCw, Receipt,
  Printer, AlertCircle, X, Building2, ShieldCheck, FileText
} from 'lucide-react';

// ─── Constants ───────────────────────────────────────────────────────────────

export const CUSTOMER_LEDGER_BASE = 'http://localhost:8083/api/v1/customers';
export const COMMERCE_BASE = 'http://localhost:8082/api/v1/commerce';

// ─── Interfaces ──────────────────────────────────────────────────────────────

export interface AddressDto {
  street?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

export interface Customer {
  id: string;
  customerType: 'RETAIL' | 'REGISTERED_WHOLESALE' | 'OFFLINE_WHOLESALE';
  businessName?: string;
  contactPerson?: string;
  phone: string;
  email?: string;
  gstin?: string;
  billingAddress?: AddressDto;
  shippingAddress?: AddressDto;
  paymentTerms: string;
  creditLimit: number;
  creditDays: number;
  currentOutstanding: number;
  overdueAmount: number;
  active: boolean;
  authUserId?: string;
  createdAt: string;
}

export interface OfflineOrderItem {
  id?: string;
  productId?: string;
  productName: string;
  grade: string;
  packageType: string;
  unitWeightKg: number;
  quantity: number;
  wholesalePrice: number;
  totalWeightKg: number;
  subtotal: number;
  gstRate: number;
  gstAmount: number;
  totalAmount: number;
}

export interface OfflineOrder {
  id: string;
  orderNumber: string;
  customerId: string;
  customerName?: string;
  businessName?: string;
  orderDate?: string;
  paymentTerms?: string;
  paymentMode?: 'CREDIT' | 'CASH' | 'PARTIAL';
  paymentStatus?: 'PAID' | 'PARTIALLY_PAID' | 'UNPAID' | 'PENDING';
  subtotalAmount: number;
  taxAmount?: number;
  totalGstAmount?: number;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  notes?: string;
  createdBy?: string;
  createdAt?: string;
  items: OfflineOrderItem[];
}

export interface LedgerEntry {
  id: string;
  customerId: string;
  transactionDate: string;
  transactionType: string;
  referenceType: string;
  referenceId: string;
  debitAmount: number;
  creditAmount: number;
  balanceAfter: number;
  dueDate?: string;
  overdue: boolean;
  description?: string;
  createdBy?: string;
  createdAt: string;
}

export interface CustomerStatement {
  customerId: string;
  customerName: string;
  businessName?: string;
  phone: string;
  email?: string;
  gstin?: string;
  customerType: string;
  creditLimit: number;
  creditDays: number;
  totalDebits: number;
  totalCredits: number;
  currentOutstanding: number;
  totalOverdue: number;
  generatedAt: string;
  entries: LedgerEntry[];
}

export interface OfflineInvoice {
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  customerId: string;
  customerName: string;
  businessName?: string;
  phone: string;
  email?: string;
  gstin?: string;
  billingAddress?: AddressDto;
  shippingAddress?: AddressDto;
  items: OfflineOrderItem[];
  subtotalAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  totalGstAmount: number;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  paymentTerms: string;
  paymentStatus: string;
  sellerName: string;
  sellerAddress: string;
  sellerGstin: string;
  sellerPan: string;
  sellerFssai: string;
  sellerState: string;
  sellerStateCode: string;
  bankName: string;
  accountNumber: string;
  ifscCode: string;
  branch: string;
}

interface ProductOption {
  id: string;
  name: string;
  sku: string;
  grade: string;
  packageType: string;
  unitWeightKg: number;
  wholesalePrice: number;
  retailPrice: number;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function formatPrice(val: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(val || 0);
}

function authHeaders(token: string) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

// ─── Component: Customer Directory & B2B Management ──────────────────────────

export function CustomerDirectoryPage({
  token,
  onOpenLedger,
  onOpenPayment,
  onOpenNewOrder,
}: {
  token: string;
  onOpenLedger: (customer: Customer) => void;
  onOpenPayment: (customer: Customer) => void;
  onOpenNewOrder: (customer?: Customer) => void;
}) {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isAddCustomerOpen, setIsAddCustomerOpen] = useState(false);

  const fetchCustomers = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError('');
    try {
      const res = await fetch(CUSTOMER_LEDGER_BASE, { headers: authHeaders(token) });
      if (!res.ok) throw new Error('Failed to fetch customers');
      const data = await res.json();
      setCustomers(Array.isArray(data) ? data : data.data || []);
    } catch (err: any) {
      setError(err.message || 'Error loading customers');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchCustomers();
  }, [fetchCustomers]);

  // Compute Metrics
  const totalCustomers = customers.length;
  const wholesaleCustomers = customers.filter(c => c.customerType === 'OFFLINE_WHOLESALE' || c.customerType === 'REGISTERED_WHOLESALE').length;
  const totalOutstanding = customers.reduce((sum, c) => sum + (c.currentOutstanding || 0), 0);
  const totalCreditLimit = customers.reduce((sum, c) => sum + (c.creditLimit || 0), 0);

  // Filtered List
  const filtered = customers.filter(c => {
    const matchesType = filterType === 'ALL' || c.customerType === filterType;
    const query = searchQuery.toLowerCase();
    const matchesSearch =
      !searchQuery ||
      c.businessName?.toLowerCase().includes(query) ||
      c.contactPerson?.toLowerCase().includes(query) ||
      c.phone?.toLowerCase().includes(query) ||
      c.gstin?.toLowerCase().includes(query) ||
      c.email?.toLowerCase().includes(query);
    return matchesType && matchesSearch;
  });

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Top Banner & Quick Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-400">Total B2B Accounts</p>
              <h3 id="stat-total-customers" className="text-2xl font-black text-white mt-1">{wholesaleCustomers} <span className="text-xs text-slate-500">/ {totalCustomers} total</span></h3>
            </div>
            <div className="p-3 bg-amber-500/10 text-amber-400 rounded-xl border border-amber-500/20">
              <Users className="h-6 w-6" />
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-400">Total Outstanding (Receivables)</p>
              <h3 id="stat-total-outstanding" className="text-2xl font-black text-amber-400 mt-1">{formatPrice(totalOutstanding)}</h3>
            </div>
            <div className="p-3 bg-red-500/10 text-red-400 rounded-xl border border-red-500/20">
              <BookOpen className="h-6 w-6" />
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-400">Active Credit Facility</p>
              <h3 id="stat-total-credit-limit" className="text-2xl font-black text-emerald-400 mt-1">{formatPrice(totalCreditLimit)}</h3>
            </div>
            <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-xl border border-emerald-500/20">
              <ShieldCheck className="h-6 w-6" />
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-slate-400">GST Compliance</p>
              <h3 className="text-2xl font-black text-sky-400 mt-1">5.0% <span className="text-xs text-slate-400 font-normal">(CGST+SGST)</span></h3>
            </div>
            <div className="p-3 bg-sky-500/10 text-sky-400 rounded-xl border border-sky-500/20">
              <FileSpreadsheet className="h-6 w-6" />
            </div>
          </div>
        </div>
      </div>

      {/* Action Bar & Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-2xl border border-slate-800">
        <div className="flex flex-wrap items-center gap-2">
          {['ALL', 'OFFLINE_WHOLESALE', 'REGISTERED_WHOLESALE', 'RETAIL'].map(type => (
            <button
              key={type}
              id={`filter-${type.toLowerCase()}`}
              onClick={() => setFilterType(type)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                filterType === type
                  ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                  : 'bg-slate-800 text-slate-400 hover:text-white'
              }`}
            >
              {type === 'ALL' ? 'All Customers' : type.replace('_', ' ')}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3">
          <div className="relative flex-1 sm:w-64">
            <Search className="h-4 w-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              id="input-search-customers"
              type="text"
              placeholder="Search business, GSTIN, phone..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500"
            />
          </div>

          <button
            id="btn-refresh-customers"
            onClick={fetchCustomers}
            title="Refresh"
            className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl transition"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            id="btn-open-add-customer"
            onClick={() => setIsAddCustomerOpen(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs rounded-xl shadow-lg shadow-amber-500/20 transition whitespace-nowrap"
          >
            <Plus className="h-4 w-4" /> <span>Add Business Customer</span>
          </button>

          <button
            id="btn-open-offline-order"
            onClick={() => onOpenNewOrder()}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-amber-400 border border-amber-500/30 font-bold text-xs rounded-xl transition whitespace-nowrap"
          >
            <Receipt className="h-4 w-4" /> <span>+ New Offline Order</span>
          </button>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="p-4 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-xs flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Customers Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950 border-b border-slate-800 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
              <tr>
                <th className="py-3.5 px-4">Business / Customer</th>
                <th className="py-3.5 px-4">Type & GSTIN</th>
                <th className="py-3.5 px-4">Contact Info</th>
                <th className="py-3.5 px-4">Credit Terms</th>
                <th className="py-3.5 px-4 text-right">Outstanding Balance</th>
                <th className="py-3.5 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    <RefreshCw className="h-6 w-6 animate-spin mx-auto text-amber-500 mb-2" />
                    Loading customer directory & ledgers...
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-500">
                    No customers found matching the criteria.
                  </td>
                </tr>
              ) : (
                filtered.map(c => {
                  const isWholesale = c.customerType === 'OFFLINE_WHOLESALE' || c.customerType === 'REGISTERED_WHOLESALE';
                  const hasOutstanding = (c.currentOutstanding || 0) > 0;
                  return (
                    <tr key={c.id} id={`customer-row-${c.id}`} className="hover:bg-slate-800/40 transition">
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-white text-sm">
                          {c.businessName || c.contactPerson || 'Individual Retail Customer'}
                        </div>
                        {c.contactPerson && c.businessName && (
                          <div className="text-[11px] text-slate-400 mt-0.5">Contact: {c.contactPerson}</div>
                        )}
                        {c.billingAddress?.city && (
                          <div className="text-[10px] text-slate-500 mt-0.5">{c.billingAddress.city}, {c.billingAddress.state}</div>
                        )}
                      </td>

                      <td className="py-3.5 px-4">
                        <div>
                          <span
                            className={`inline-block text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                              c.customerType === 'REGISTERED_WHOLESALE'
                                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                                : c.customerType === 'OFFLINE_WHOLESALE'
                                ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                                : 'bg-slate-800 text-slate-400 border-slate-700'
                            }`}
                          >
                            {c.customerType.replace('_', ' ')}
                          </span>
                        </div>
                        <div className="font-mono text-[11px] text-slate-400 mt-1">
                          {c.gstin ? (
                            <span className="text-amber-300 font-semibold">{c.gstin}</span>
                          ) : (
                            <span className="text-slate-600">No GSTIN</span>
                          )}
                        </div>
                      </td>

                      <td className="py-3.5 px-4 space-y-0.5">
                        <div className="flex items-center gap-1.5 text-slate-300">
                          <Phone className="h-3 w-3 text-slate-500" />
                          <span>{c.phone || '—'}</span>
                        </div>
                        {c.email && (
                          <div className="flex items-center gap-1.5 text-slate-400 text-[11px]">
                            <Mail className="h-3 w-3 text-slate-500" />
                            <span>{c.email}</span>
                          </div>
                        )}
                      </td>

                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-white">{c.paymentTerms || 'IMMEDIATE'}</div>
                        <div className="text-[10px] text-slate-400 mt-0.5">
                          Limit: <strong className="text-emerald-400">{formatPrice(c.creditLimit)}</strong> ({c.creditDays || 0}d)
                        </div>
                      </td>

                      <td className="py-3.5 px-4 text-right">
                        <div
                          id={`customer-outstanding-${c.id}`}
                          className={`font-mono text-sm font-black ${
                            hasOutstanding ? 'text-amber-400' : 'text-slate-400'
                          }`}
                        >
                          {formatPrice(c.currentOutstanding)}
                        </div>
                        {hasOutstanding && (
                          <span className="text-[9px] font-bold text-amber-400/80 uppercase">Debit Balance</span>
                        )}
                      </td>

                      <td className="py-3.5 px-4">
                        <div className="flex items-center justify-center gap-1.5">
                          <button
                            id={`btn-ledger-${c.id}`}
                            onClick={() => onOpenLedger(c)}
                            title="View Statement & Ledger"
                            className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-amber-400 font-semibold rounded-lg text-xs border border-slate-700 transition flex items-center gap-1"
                          >
                            <BookOpen className="h-3 w-3" /> <span>Ledger</span>
                          </button>

                          {isWholesale && (
                            <button
                              id={`btn-pay-${c.id}`}
                              onClick={() => onOpenPayment(c)}
                              title="Record Payment / Collection"
                              className="px-2.5 py-1 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 font-semibold rounded-lg text-xs border border-emerald-500/20 transition flex items-center gap-1"
                            >
                              <Wallet className="h-3 w-3" /> <span>Pay</span>
                            </button>
                          )}

                          <button
                            id={`btn-order-${c.id}`}
                            onClick={() => onOpenNewOrder(c)}
                            title="Create Offline Order"
                            className="px-2.5 py-1 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold rounded-lg text-xs transition flex items-center gap-1"
                          >
                            <Plus className="h-3 w-3" /> <span>Order</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Customer Modal */}
      {isAddCustomerOpen && (
        <AddCustomerModal
          token={token}
          onClose={() => setIsAddCustomerOpen(false)}
          onCustomerAdded={() => {
            setIsAddCustomerOpen(false);
            fetchCustomers();
          }}
        />
      )}
    </div>
  );
}

// ─── Component: Add Business Customer Modal ───────────────────────────────────

export function AddCustomerModal({
  token,
  onClose,
  onCustomerAdded,
}: {
  token: string;
  onClose: () => void;
  onCustomerAdded: () => void;
}) {
  const [customerType, setCustomerType] = useState<'OFFLINE_WHOLESALE' | 'REGISTERED_WHOLESALE'>('OFFLINE_WHOLESALE');
  const [businessName, setBusinessName] = useState('');
  const [contactPerson, setContactPerson] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [gstin, setGstin] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('Karnataka');
  const [postalCode, setPostalCode] = useState('');
  const [paymentTerms, setPaymentTerms] = useState('NET_30');
  const [creditLimit, setCreditLimit] = useState('50000');
  const [creditDays, setCreditDays] = useState('30');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    const payload = {
      customerType,
      businessName,
      contactPerson,
      phone,
      email: email || undefined,
      gstin: gstin || undefined,
      billingAddress: { street, city, state, postalCode, country: 'India' },
      shippingAddress: { street, city, state, postalCode, country: 'India' },
      paymentTerms,
      creditLimit: parseFloat(creditLimit) || 0,
      creditDays: parseInt(creditDays, 10) || 0,
    };

    try {
      const res = await fetch(CUSTOMER_LEDGER_BASE, {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Failed to create customer');
      }
      onCustomerAdded();
    } catch (err: any) {
      setError(err.message || 'Error creating customer');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-2xl shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <button
          id="close-add-customer-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
        >
          <X className="h-4 w-4" />
        </button>

        <h2 className="text-xl font-bold text-white mb-2 flex items-center gap-2">
          <Building2 className="h-5 w-5 text-amber-400" /> Add Wholesale / B2B Customer
        </h2>
        <p className="text-xs text-slate-400 mb-6">Create offline wholesale customer with ledger account & credit terms</p>

        {error && (
          <div className="mb-4 flex items-center gap-2 text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2.5">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5 text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-slate-400 font-semibold">Customer Account Type</label>
              <select
                id="select-customer-type"
                value={customerType}
                onChange={e => setCustomerType(e.target.value as any)}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
              >
                <option value="OFFLINE_WHOLESALE">Offline Wholesale (Mill Buyer)</option>
                <option value="REGISTERED_WHOLESALE">Registered Wholesale (Online Account)</option>
              </select>
            </div>

            <div>
              <label className="text-slate-400 font-semibold">Business / Firm Name *</label>
              <input
                id="input-business-name"
                type="text"
                required
                placeholder="e.g. Mysuru Sweet Center"
                value={businessName}
                onChange={e => setBusinessName(e.target.value)}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
              />
            </div>

            <div>
              <label className="text-slate-400 font-semibold">Contact Person</label>
              <input
                id="input-contact-person"
                type="text"
                placeholder="e.g. Anand Murthy"
                value={contactPerson}
                onChange={e => setContactPerson(e.target.value)}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
              />
            </div>

            <div>
              <label className="text-slate-400 font-semibold">Phone Number *</label>
              <input
                id="input-customer-phone"
                type="tel"
                required
                placeholder="+91 98450 12345"
                value={phone}
                onChange={e => setPhone(e.target.value)}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
              />
            </div>

            <div>
              <label className="text-slate-400 font-semibold">Email Address</label>
              <input
                id="input-customer-email"
                type="email"
                placeholder="buyer@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
              />
            </div>

            <div>
              <label className="text-slate-400 font-semibold">GSTIN (Optional)</label>
              <input
                id="input-customer-gstin"
                type="text"
                placeholder="29ABCDE1234F1Z5"
                value={gstin}
                onChange={e => setGstin(e.target.value.toUpperCase())}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono uppercase focus:outline-none focus:border-amber-500"
              />
            </div>
          </div>

          <div className="pt-3 border-t border-slate-800 space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-amber-400">Address & Terms</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <label className="text-slate-400 font-semibold">Street / Market Address</label>
                <input
                  id="input-customer-street"
                  type="text"
                  placeholder="e.g. 45, Devaraja Market"
                  value={street}
                  onChange={e => setStreet(e.target.value)}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>

              <div>
                <label className="text-slate-400 font-semibold">City</label>
                <input
                  id="input-customer-city"
                  type="text"
                  placeholder="e.g. Mysuru"
                  value={city}
                  onChange={e => setCity(e.target.value)}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-slate-400 font-semibold">State</label>
                  <input
                    id="input-customer-state"
                    type="text"
                    value={state}
                    onChange={e => setState(e.target.value)}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
                <div>
                  <label className="text-slate-400 font-semibold">PIN Code</label>
                  <input
                    id="input-customer-pincode"
                    type="text"
                    placeholder="570001"
                    value={postalCode}
                    onChange={e => setPostalCode(e.target.value)}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-slate-400 font-semibold">Payment Terms</label>
                <select
                  id="select-payment-terms"
                  value={paymentTerms}
                  onChange={e => setPaymentTerms(e.target.value)}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                >
                  <option value="IMMEDIATE_CASH">Immediate / Cash</option>
                  <option value="NET_7">NET 7 Days</option>
                  <option value="NET_15">NET 15 Days</option>
                  <option value="NET_30">NET 30 Days</option>
                  <option value="NET_45">NET 45 Days</option>
                  <option value="NET_60">NET 60 Days</option>
                  <option value="ADVANCE">Advance Payment</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-slate-400 font-semibold">Credit Limit (₹)</label>
                  <input
                    id="input-credit-limit"
                    type="number"
                    min="0"
                    step="1000"
                    value={creditLimit}
                    onChange={e => setCreditLimit(e.target.value)}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono focus:outline-none focus:border-amber-500"
                  />
                </div>
                <div>
                  <label className="text-slate-400 font-semibold">Credit Days</label>
                  <input
                    id="input-credit-days"
                    type="number"
                    min="0"
                    value={creditDays}
                    onChange={e => setCreditDays(e.target.value)}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="pt-4 flex justify-end gap-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold transition"
            >
              Cancel
            </button>
            <button
              id="btn-submit-add-customer"
              type="submit"
              disabled={loading}
              className="px-5 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold shadow-lg shadow-amber-500/20 transition flex items-center gap-2"
            >
              {loading && <RefreshCw className="h-4 w-4 animate-spin" />}
              <span>Create Customer Account</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Component: Create Offline Wholesale Order Modal ─────────────────────────

export function CreateOfflineOrderModal({
  token,
  preselectedCustomer,
  onClose,
  onOrderCreated,
}: {
  token: string;
  preselectedCustomer?: Customer;
  onClose: () => void;
  onOrderCreated: (order: OfflineOrder) => void;
}) {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [selectedCustomerId, setSelectedCustomerId] = useState<string>(preselectedCustomer?.id || '');
  const [products, setProducts] = useState<ProductOption[]>([]);
  const [paymentMode, setPaymentMode] = useState<'CREDIT' | 'CASH' | 'PARTIAL'>('CREDIT');
  const [paidAmount, setPaidAmount] = useState<string>('0');
  const [paymentMethod, setPaymentMethod] = useState<string>('CASH');
  const [paymentReference, setPaymentReference] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Line items state
  const [items, setItems] = useState<
    {
      productId: string;
      productName: string;
      grade: string;
      packageType: string;
      unitWeightKg: number;
      wholesalePrice: number;
      quantity: number;
    }[]
  >([
    {
      productId: '',
      productName: 'Organic Traditional Jaggery Blocks (10kg Master Box)',
      grade: 'GRADE_A_TRADITIONAL',
      packageType: 'BOX',
      unitWeightKg: 10,
      wholesalePrice: 550,
      quantity: 5,
    },
  ]);

  // Load customers & products (only once on mount — do NOT include selectedCustomerId in deps
  // or the effect will re-run every time we set the default selection, causing an infinite loop)
  const [dataLoaded, setDataLoaded] = useState(false);
  useEffect(() => {
    async function loadData() {
      try {
        const [cRes, pRes] = await Promise.all([
          fetch(CUSTOMER_LEDGER_BASE, { headers: authHeaders(token) }),
          fetch(`${COMMERCE_BASE}/products`),
        ]);
        if (cRes.ok) {
          const cData = await cRes.json();
          const list = Array.isArray(cData) ? cData : cData.data || [];
          setCustomers(list);
          // Set default selection only when no customer is preselected
          setSelectedCustomerId(prev => (prev ? prev : list[0]?.id || ''));
        }
        if (pRes.ok) {
          const pData = await pRes.json();
          const list = Array.isArray(pData) ? pData : pData.data || [];
          setProducts(list);
          if (list.length > 0) {
            setItems([
              {
                productId: list[0].id,
                productName: list[0].name,
                grade: list[0].grade || 'GRADE_A_TRADITIONAL',
                packageType: list[0].packageType || 'BOX',
                unitWeightKg: list[0].unitWeightKg || 10,
                wholesalePrice: list[0].wholesalePrice || 550,
                quantity: 5,
              },
            ]);
          }
        }
      } catch (err) {
        console.warn('Failed to load customers/products', err);
      } finally {
        setDataLoaded(true);
      }
    }
    loadData();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const currentCustomer = customers.find(c => c.id === selectedCustomerId) || preselectedCustomer;

  // Compute live calculations
  const lineCalculations = items.map(item => {
    const totalWeightKg = item.unitWeightKg * item.quantity;
    const subtotal = item.wholesalePrice * item.quantity;
    const gstRate = 5.0;
    const gstAmount = (subtotal * gstRate) / 100.0;
    const totalAmount = subtotal + gstAmount;
    return { ...item, totalWeightKg, subtotal, gstRate, gstAmount, totalAmount };
  });

  const subtotalSum = lineCalculations.reduce((s, i) => s + i.subtotal, 0);
  const totalGstSum = lineCalculations.reduce((s, i) => s + i.gstAmount, 0);
  const totalOrderAmount = subtotalSum + totalGstSum;
  const upfrontPaid = paymentMode === 'CASH' ? totalOrderAmount : paymentMode === 'PARTIAL' ? parseFloat(paidAmount) || 0 : 0;
  const balanceToLedger = Math.max(0, totalOrderAmount - upfrontPaid);

  // Credit limit check
  const projectedOutstanding = (currentCustomer?.currentOutstanding || 0) + balanceToLedger;
  const exceedsCreditLimit = currentCustomer ? projectedOutstanding > currentCustomer.creditLimit : false;

  function handleProductSelect(index: number, productId: string) {
    const prod = products.find(p => p.id === productId);
    if (!prod) return;
    setItems(prev => {
      const next = [...prev];
      next[index] = {
        productId: prod.id,
        productName: prod.name,
        grade: prod.grade,
        packageType: prod.packageType,
        unitWeightKg: prod.unitWeightKg || 10,
        wholesalePrice: prod.wholesalePrice || 550,
        quantity: next[index].quantity || 1,
      };
      return next;
    });
  }

  function addItem() {
    const firstProd = products[0];
    setItems(prev => [
      ...prev,
      {
        productId: firstProd?.id || '',
        productName: firstProd?.name || 'Grade A Organic Jaggery',
        grade: firstProd?.grade || 'GRADE_A_TRADITIONAL',
        packageType: firstProd?.packageType || 'BOX',
        unitWeightKg: firstProd?.unitWeightKg || 10,
        wholesalePrice: firstProd?.wholesalePrice || 550,
        quantity: 1,
      },
    ]);
  }

  function removeItem(index: number) {
    if (items.length <= 1) return;
    setItems(prev => prev.filter((_, i) => i !== index));
  }

  async function handleSubmitOrder(e: React.FormEvent) {
    e.preventDefault();
    const custId = selectedCustomerId || (customers[0]?.id) || preselectedCustomer?.id;
    if (!custId) {
      setError('Please select a customer.');
      return;
    }
    setError('');
    setLoading(true);

    const payload = {
      customerId: custId,
      paymentMethod: paymentMode === 'CASH' ? (paymentMethod || 'CASH') : paymentMode === 'PARTIAL' ? (paymentMethod || 'UPI') : 'CREDIT',
      immediatePaidAmount: upfrontPaid,
      notes,
      items: lineCalculations.map(item => {
        const prod = products.find(p => p.id === item.productId) || products[0];
        return {
          productId: item.productId || prod?.id || '00000000-0000-0000-0000-000000000001',
          productName: item.productName || prod?.name || 'Grade A Organic Jaggery',
          sku: prod?.sku || 'RR-JAG-10KG',
          unitPrice: item.wholesalePrice,
          quantity: item.quantity,
          unitWeightKg: item.unitWeightKg,
        };
      }),
    };

    try {
      const res = await fetch(`${CUSTOMER_LEDGER_BASE}/offline-orders`, {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Failed to create offline order');
      }
      onOrderCreated(data.data || data);
    } catch (err: any) {
      setError(err.message || 'Error placing offline order');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-4xl shadow-2xl relative max-h-[92vh] overflow-y-auto">
        <button
          id="close-create-order-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
        >
          <X className="h-4 w-4" />
        </button>

        <h2 className="text-xl font-bold text-white mb-1 flex items-center gap-2">
          <Receipt className="h-5 w-5 text-amber-400" /> Create Offline Wholesale Order
        </h2>
        <p className="text-xs text-slate-400 mb-5">Direct mill dispatch, wholesale pricing, automatic 5% GST computation & ledger posting</p>

        {error && (
          <div className="mb-4 flex items-center gap-2 text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2.5">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmitOrder} className="space-y-6 text-xs">
          {/* Customer Selection & Credit Status Header */}
          <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 space-y-3">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-slate-400 font-semibold">Select Customer Account *</label>
                <select
                  id="select-order-customer"
                  value={selectedCustomerId}
                  onChange={e => setSelectedCustomerId(e.target.value)}
                  data-loaded={dataLoaded ? 'true' : 'false'}
                  className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-white font-semibold focus:outline-none focus:border-amber-500"
                >
                  {customers.map(c => (
                    <option key={c.id} value={c.id}>
                      {c.businessName || c.contactPerson} ({c.customerType.replace('_', ' ')}) — Limit: {formatPrice(c.creditLimit)}
                    </option>
                  ))}
                </select>
              </div>

              {currentCustomer && (
                <div className="flex items-center justify-between bg-slate-900/60 p-3 rounded-xl border border-slate-800/80">
                  <div>
                    <span className="text-[10px] text-slate-500 uppercase font-bold">Current Ledger State</span>
                    <div className="text-xs text-slate-300 font-medium mt-0.5">
                      Outstanding: <strong className="text-amber-400">{formatPrice(currentCustomer.currentOutstanding)}</strong>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] text-slate-500 uppercase font-bold">Credit Limit</span>
                    <div className="text-xs text-emerald-400 font-bold mt-0.5">
                      {formatPrice(currentCustomer.creditLimit)} ({currentCustomer.creditDays}d)
                    </div>
                  </div>
                </div>
              )}
            </div>

            {exceedsCreditLimit && (
              <div className="flex items-center gap-2 p-2.5 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-[11px]">
                <AlertTriangle className="h-4 w-4 shrink-0 text-red-400" />
                <span>
                  <strong>Credit Warning:</strong> Adding this order ({formatPrice(balanceToLedger)}) will result in {formatPrice(projectedOutstanding)}, exceeding credit limit of {formatPrice(currentCustomer?.creditLimit || 0)}. Backend will reject order unless upfront payment is recorded.
                </span>
              </div>
            )}
          </div>

          {/* Line Items Builder */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-amber-400">Order Line Items</h3>
              <button
                type="button"
                id="btn-add-line-item"
                onClick={addItem}
                className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-amber-400 rounded-lg text-xs font-semibold flex items-center gap-1 transition"
              >
                <Plus className="h-3.5 w-3.5" /> <span>Add Item</span>
              </button>
            </div>

            <div className="space-y-3">
              {items.map((item, idx) => (
                <div key={idx} className="bg-slate-950 p-4 rounded-2xl border border-slate-800 space-y-3">
                  <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-end">
                    {/* Catalog item picker */}
                    <div className="sm:col-span-4">
                      <label className="text-[11px] text-slate-400 font-semibold">Select Product Catalog Item</label>
                      <select
                        value={item.productId}
                        onChange={e => handleProductSelect(idx, e.target.value)}
                        className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-2.5 py-1.5 text-white text-xs focus:outline-none focus:border-amber-500"
                      >
                        <option value="">Custom Item / Manual</option>
                        {products.map(p => (
                          <option key={p.id} value={p.id}>
                            {p.name} ({p.unitWeightKg}kg - {formatPrice(p.wholesalePrice)})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="sm:col-span-3">
                      <label className="text-[11px] text-slate-400 font-semibold">Product Description / Grade</label>
                      <input
                        type="text"
                        required
                        value={item.productName}
                        onChange={e => {
                          const val = e.target.value;
                          setItems(prev => {
                            const next = [...prev];
                            next[idx].productName = val;
                            return next;
                          });
                        }}
                        className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-2.5 py-1.5 text-white text-xs focus:outline-none focus:border-amber-500"
                      />
                    </div>

                    <div className="sm:col-span-2">
                      <label className="text-[11px] text-slate-400 font-semibold">Unit Wt (kg)</label>
                      <input
                        type="number"
                        min="0.01"
                        step="any"
                        required
                        value={item.unitWeightKg}
                        onChange={e => {
                          const val = parseFloat(e.target.value) || 0;
                          setItems(prev => {
                            const next = [...prev];
                            next[idx].unitWeightKg = val;
                            return next;
                          });
                        }}
                        className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-2.5 py-1.5 text-white text-xs font-mono focus:outline-none focus:border-amber-500"
                      />
                    </div>

                    <div className="sm:col-span-1">
                      <label className="text-[11px] text-slate-400 font-semibold">Qty</label>
                      <input
                        id={`input-item-qty-${idx}`}
                        type="number"
                        min="1"
                        step="1"
                        required
                        value={item.quantity}
                        onChange={e => {
                          const val = parseInt(e.target.value, 10) || 1;
                          setItems(prev => {
                            const next = [...prev];
                            next[idx].quantity = val;
                            return next;
                          });
                        }}
                        className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-2 py-1.5 text-white text-xs font-mono focus:outline-none focus:border-amber-500"
                      />
                    </div>

                    <div className="sm:col-span-2 flex items-center gap-2">
                      <div className="flex-1">
                        <label className="text-[11px] text-slate-400 font-semibold">Wholesale Rate (₹)</label>
                        <input
                          id={`input-item-rate-${idx}`}
                          type="number"
                          min="0"
                          step="any"
                          required
                          value={item.wholesalePrice}
                          onChange={e => {
                            const val = parseFloat(e.target.value) || 0;
                            setItems(prev => {
                              const next = [...prev];
                              next[idx].wholesalePrice = val;
                              return next;
                            });
                          }}
                          className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-2.5 py-1.5 text-white text-xs font-mono focus:outline-none focus:border-amber-500"
                        />
                      </div>
                      {items.length > 1 && (
                        <button
                          type="button"
                          onClick={() => removeItem(idx)}
                          className="mt-6 p-1.5 text-slate-500 hover:text-red-400 transition"
                          title="Remove item"
                        >
                          <X className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-[11px] text-slate-400 pt-2 border-t border-slate-900">
                    <div>
                      Total Weight: <span className="text-white font-mono">{lineCalculations[idx].totalWeightKg} kg</span>
                    </div>
                    <div>
                      Taxable: <span className="text-white font-mono">{formatPrice(lineCalculations[idx].subtotal)}</span> + GST (5%): <span className="text-slate-300 font-mono">{formatPrice(lineCalculations[idx].gstAmount)}</span> = Line Total: <strong className="text-amber-400 font-mono">{formatPrice(lineCalculations[idx].totalAmount)}</strong>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Payment Mode & Financial Settlement */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-3 border-t border-slate-800">
            <div className="space-y-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-amber-400">Payment & Settlement</h3>
              <div>
                <label className="text-slate-400 font-semibold">Payment Settlement Mode</label>
                <select
                  id="select-payment-mode"
                  value={paymentMode}
                  onChange={e => setPaymentMode(e.target.value as any)}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-semibold focus:outline-none focus:border-amber-500"
                >
                  <option value="CREDIT">Full Credit (Charge to Customer Ledger)</option>
                  <option value="CASH">Full Immediate Payment (Cash / UPI / Bank)</option>
                  <option value="PARTIAL">Partial Payment (Split Upfront + Ledger)</option>
                </select>
              </div>

              {(paymentMode === 'CASH' || paymentMode === 'PARTIAL') && (
                <div className="grid grid-cols-2 gap-2 bg-slate-950 p-3 rounded-xl border border-slate-800">
                  {paymentMode === 'PARTIAL' && (
                    <div className="col-span-2">
                      <label className="text-slate-400 font-semibold">Upfront Amount Paid (₹)</label>
                      <input
                        id="input-paid-amount"
                        type="number"
                        min="1"
                        max={totalOrderAmount}
                        step="1"
                        value={paidAmount}
                        onChange={e => setPaidAmount(e.target.value)}
                        className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 text-white font-mono focus:outline-none focus:border-amber-500"
                      />
                    </div>
                  )}

                  <div>
                    <label className="text-slate-400 font-semibold">Payment Method</label>
                    <select
                      id="select-payment-method"
                      value={paymentMethod}
                      onChange={e => setPaymentMethod(e.target.value)}
                      className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-2 py-1.5 text-white text-xs focus:outline-none focus:border-amber-500"
                    >
                      <option value="CASH">Cash</option>
                      <option value="UPI">UPI</option>
                      <option value="BANK_TRANSFER">Bank Transfer (NEFT/RTGS)</option>
                      <option value="CHEQUE">Cheque</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-slate-400 font-semibold">UTR / Reference No.</label>
                    <input
                      id="input-payment-ref"
                      type="text"
                      placeholder="e.g. UPI-998811"
                      value={paymentReference}
                      onChange={e => setPaymentReference(e.target.value)}
                      className="w-full mt-1 bg-slate-900 border border-slate-800 rounded-xl px-2 py-1.5 text-white text-xs font-mono focus:outline-none focus:border-amber-500"
                    />
                  </div>
                </div>
              )}

              <div>
                <label className="text-slate-400 font-semibold">Order / Dispatch Notes</label>
                <input
                  id="input-order-notes"
                  type="text"
                  placeholder="e.g. Mandya Mill gate pickup via tempo KA-11-2026"
                  value={notes}
                  onChange={e => setNotes(e.target.value)}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
            </div>

            {/* Order Summary Calculation Box */}
            <div className="bg-slate-950 p-5 rounded-2xl border border-slate-800 space-y-3 flex flex-col justify-between">
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">Order Financial Breakdown</h4>
                <div className="space-y-2 text-slate-300">
                  <div className="flex justify-between">
                    <span>Taxable Subtotal:</span>
                    <span className="font-mono text-white">{formatPrice(subtotalSum)}</span>
                  </div>
                  <div className="flex justify-between text-slate-400 text-[11px]">
                    <span>CGST (2.5%):</span>
                    <span className="font-mono">{formatPrice(totalGstSum / 2)}</span>
                  </div>
                  <div className="flex justify-between text-slate-400 text-[11px]">
                    <span>SGST (2.5%):</span>
                    <span className="font-mono">{formatPrice(totalGstSum / 2)}</span>
                  </div>
                  <div className="flex justify-between text-xs text-sky-400 font-semibold">
                    <span>Total GST (5%):</span>
                    <span className="font-mono">{formatPrice(totalGstSum)}</span>
                  </div>
                  <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
                    <span>Grand Total:</span>
                    <span id="order-total-amount" className="text-amber-400 font-mono text-base">{formatPrice(totalOrderAmount)}</span>
                  </div>
                  <div className="flex justify-between text-xs text-emerald-400 pt-1">
                    <span>Upfront Paid:</span>
                    <span className="font-mono">{formatPrice(upfrontPaid)}</span>
                  </div>
                  <div className="flex justify-between text-xs text-amber-400 font-bold pt-1 border-t border-slate-800/60">
                    <span>Debit Added to Ledger:</span>
                    <span className="font-mono">{formatPrice(balanceToLedger)}</span>
                  </div>
                </div>
              </div>

              <div className="pt-3">
                <button
                  id="btn-submit-offline-order"
                  type="submit"
                  disabled={loading}
                  className="w-full py-3 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-extrabold text-xs shadow-lg shadow-amber-500/20 transition flex items-center justify-center gap-2"
                >
                  {loading && <RefreshCw className="h-4 w-4 animate-spin" />}
                  <span>Confirm Offline Order & Post to Ledger</span>
                </button>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Component: Customer Ledger Statement Modal ──────────────────────────────

export function LedgerStatementModal({
  token,
  customer,
  onClose,
  onRecordPayment,
}: {
  token: string;
  customer: Customer;
  onClose: () => void;
  onRecordPayment: () => void;
}) {
  const [statement, setStatement] = useState<CustomerStatement | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchStatement = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${CUSTOMER_LEDGER_BASE}/${customer.id}/ledger`, {
        headers: authHeaders(token),
      });
      if (!res.ok) throw new Error('Failed to load ledger statement');
      const data = await res.json();
      setStatement(data.data || data);
    } catch (err: any) {
      setError(err.message || 'Error fetching ledger statement');
    } finally {
      setLoading(false);
    }
  }, [customer.id, token]);

  useEffect(() => {
    fetchStatement();
  }, [fetchStatement]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-4xl shadow-2xl relative max-h-[92vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
          <div className="flex items-center gap-2.5">
            <BookOpen className="h-6 w-6 text-amber-400" />
            <div>
              <h2 className="text-lg font-black text-white">Customer Statement of Account & Ledger</h2>
              <p className="text-xs text-slate-400">
                {customer.businessName || customer.contactPerson} • Terms: {customer.paymentTerms} • Credit Limit: {formatPrice(customer.creditLimit)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              id="btn-print-statement"
              onClick={() => window.print()}
              className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition"
            >
              <Printer className="h-3.5 w-3.5" /> Print Statement
            </button>
            <button
              id="btn-record-payment-from-ledger"
              onClick={onRecordPayment}
              className="px-3 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold rounded-xl text-xs shadow-md transition flex items-center gap-1"
            >
              <Wallet className="h-3.5 w-3.5" /> Record Payment
            </button>
            <button
              id="close-ledger-modal"
              onClick={onClose}
              className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white rounded-xl transition"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        {loading ? (
          <div className="py-16 text-center text-slate-500">
            <RefreshCw className="h-8 w-8 animate-spin mx-auto text-amber-500 mb-2" />
            Loading ledger entries & calculating balances...
          </div>
        ) : error ? (
          <div className="p-4 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-xs flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        ) : statement ? (
          <div className="space-y-6">
            {/* Header Summary Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 text-xs">
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <span className="text-slate-400 font-semibold">Total Debits (Invoices)</span>
                <div className="text-base font-black text-white font-mono mt-1">{formatPrice(statement.totalDebits)}</div>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <span className="text-slate-400 font-semibold">Total Credits (Payments)</span>
                <div className="text-base font-black text-emerald-400 font-mono mt-1">{formatPrice(statement.totalCredits)}</div>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <span className="text-slate-400 font-semibold">Closing Outstanding</span>
                <div id="statement-closing-balance" className="text-base font-black text-amber-400 font-mono mt-1">
                  {formatPrice(statement.currentOutstanding)}
                </div>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <span className="text-slate-400 font-semibold">Available Credit</span>
                <div className="text-base font-black text-sky-400 font-mono mt-1">
                  {formatPrice(Math.max(0, (statement.creditLimit || 0) - (statement.currentOutstanding || 0)))}
                </div>
              </div>
            </div>

            {/* Ledger Transactions Table */}
            <div className="bg-slate-950 border border-slate-800 rounded-2xl overflow-hidden shadow-lg">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-900 border-b border-slate-800 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                    <tr>
                      <th className="py-3 px-3">Date</th>
                      <th className="py-3 px-3">Type</th>
                      <th className="py-3 px-3">Ref ID</th>
                      <th className="py-3 px-4">Description</th>
                      <th className="py-3 px-3 text-right">Debit (+)</th>
                      <th className="py-3 px-3 text-right">Credit (-)</th>
                      <th className="py-3 px-3 text-right">Balance</th>
                      <th className="py-3 px-2 text-center">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-mono text-[11px]">
                    {statement.entries.length === 0 ? (
                      <tr>
                        <td colSpan={8} className="py-8 text-center text-slate-500 font-sans">
                          No transactions recorded on this account yet.
                        </td>
                      </tr>
                    ) : (
                      statement.entries.map((e, idx) => (
                        <tr key={e.id || idx} className="hover:bg-slate-900/40 transition">
                          <td className="py-2.5 px-3 text-slate-400">
                            {new Date(e.transactionDate).toLocaleDateString()}
                          </td>

                          <td className="py-2.5 px-3">
                            <span
                              className={`text-[9px] font-bold px-1.5 py-0.5 rounded font-sans ${
                                e.transactionType === 'INVOICE_DEBIT'
                                  ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                                  : e.transactionType === 'PAYMENT_CREDIT'
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : 'bg-slate-800 text-slate-400'
                              }`}
                            >
                              {e.transactionType}
                            </span>
                          </td>

                          <td className="py-2.5 px-3 text-slate-300 font-bold">{e.referenceId || '—'}</td>

                          <td className="py-2.5 px-4 font-sans text-xs text-slate-300">{e.description}</td>

                          <td className="py-2.5 px-3 text-right text-red-400 font-semibold">
                            {e.debitAmount > 0 ? formatPrice(e.debitAmount) : '—'}
                          </td>

                          <td className="py-2.5 px-3 text-right text-emerald-400 font-semibold">
                            {e.creditAmount > 0 ? formatPrice(e.creditAmount) : '—'}
                          </td>

                          <td className="py-2.5 px-3 text-right text-white font-bold">
                            {formatPrice(e.balanceAfter)}
                          </td>

                          <td className="py-2.5 px-2 text-center font-sans">
                            {e.overdue ? (
                              <span className="text-[9px] bg-red-500/20 text-red-400 border border-red-500/30 px-1.5 py-0.5 rounded font-bold">OVERDUE</span>
                            ) : (
                              <span className="text-[9px] text-slate-500">OK</span>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
}

// ─── Component: Record Customer Payment Modal ────────────────────────────────

export function RecordPaymentModal({
  token,
  customer,
  onClose,
  onPaymentRecorded,
}: {
  token: string;
  customer: Customer;
  onClose: () => void;
  onPaymentRecorded: () => void;
}) {
  const [amount, setAmount] = useState<string>((customer.currentOutstanding || 0).toString());
  const [paymentMethod, setPaymentMethod] = useState<string>('UPI');
  const [paymentReference, setPaymentReference] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('Please enter a valid payment amount greater than ₹0.');
      return;
    }
    if (numAmount > customer.currentOutstanding) {
      setError(`Payment amount cannot exceed outstanding balance of ${formatPrice(customer.currentOutstanding)}.`);
      return;
    }

    setError('');
    setLoading(true);

    const payload = {
      amount: numAmount,
      paymentMethod,
      paymentReference: paymentReference || undefined,
      notes: notes || undefined,
    };

    try {
      const res = await fetch(`${CUSTOMER_LEDGER_BASE}/${customer.id}/payments`, {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Failed to record payment');
      }
      onPaymentRecorded();
    } catch (err: any) {
      setError(err.message || 'Error recording payment');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-lg shadow-2xl relative">
        <button
          id="close-payment-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
        >
          <X className="h-4 w-4" />
        </button>

        <h2 className="text-xl font-bold text-white mb-1 flex items-center gap-2">
          <Wallet className="h-5 w-5 text-emerald-400" /> Record Customer Payment
        </h2>
        <p className="text-xs text-slate-400 mb-5">Credit payment collection onto ledger and reduce outstanding balance</p>

        {error && (
          <div className="mb-4 flex items-center gap-2 text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2.5">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 mb-5 flex items-center justify-between">
          <div>
            <div className="text-[11px] text-slate-400 font-semibold">{customer.businessName || customer.contactPerson}</div>
            <div className="text-xs text-slate-300 mt-0.5">{customer.phone} • {customer.paymentTerms}</div>
          </div>
          <div className="text-right">
            <div className="text-[10px] text-slate-500 uppercase font-bold">Outstanding</div>
            <div id="payment-modal-outstanding" className="text-base font-black text-amber-400 font-mono">
              {formatPrice(customer.currentOutstanding)}
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div>
            <div className="flex items-center justify-between">
              <label className="text-slate-400 font-semibold">Payment Amount (₹) *</label>
              <button
                type="button"
                onClick={() => setAmount(customer.currentOutstanding.toString())}
                className="text-[10px] text-amber-400 hover:underline font-bold"
              >
                Set Full Amount ({formatPrice(customer.currentOutstanding)})
              </button>
            </div>
            <input
              id="input-payment-amount"
              type="number"
              min="1"
              max={customer.currentOutstanding}
              step="0.01"
              required
              value={amount}
              onChange={e => setAmount(e.target.value)}
              className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-white font-mono text-base focus:outline-none focus:border-emerald-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-slate-400 font-semibold">Payment Method</label>
              <select
                id="select-payment-method-record"
                value={paymentMethod}
                onChange={e => setPaymentMethod(e.target.value)}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
              >
                <option value="UPI">UPI / QR Code</option>
                <option value="BANK_TRANSFER">Bank Transfer (NEFT/RTGS)</option>
                <option value="CASH">Cash</option>
                <option value="CHEQUE">Cheque</option>
              </select>
            </div>

            <div>
              <label className="text-slate-400 font-semibold">UTR / Ref No.</label>
              <input
                id="input-payment-ref-record"
                type="text"
                placeholder="e.g. UTR-88776655"
                value={paymentReference}
                onChange={e => setPaymentReference(e.target.value)}
                className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="text-slate-400 font-semibold">Notes / Remarks</label>
            <input
              id="input-payment-notes"
              type="text"
              placeholder="e.g. Cleared invoice against Mandya order"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-emerald-500"
            />
          </div>

          <div className="pt-4 flex justify-end gap-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold transition"
            >
              Cancel
            </button>
            <button
              id="btn-submit-payment"
              type="submit"
              disabled={loading}
              className="px-5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold shadow-lg shadow-emerald-500/20 transition flex items-center gap-2"
            >
              {loading && <RefreshCw className="h-4 w-4 animate-spin" />}
              <span>Post Payment to Ledger</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Component: Offline GST Tax Invoice Modal ────────────────────────────────

export function OfflineInvoiceModal({
  order,
  onClose,
}: {
  order: OfflineOrder | null;
  onClose: () => void;
}) {
  if (!order) return null;

  const items = order.items || [];
  const tax = order.taxAmount ?? order.totalGstAmount ?? 0;
  const customerDisplayName = order.businessName || order.customerName || 'Wholesale Buyer';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-3xl shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
          <div className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-amber-400" />
            <h2 className="text-lg font-bold text-white">Wholesale GST Tax Invoice</h2>
          </div>
          <div className="flex items-center gap-2">
            <button
              id="btn-print-offline-invoice"
              onClick={() => window.print()}
              className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition"
            >
              <Printer className="h-3.5 w-3.5" /> Print Invoice
            </button>
            <button
              id="close-offline-invoice-modal"
              onClick={onClose}
              className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white rounded-xl transition"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        <div id="offline-invoice-modal-content" className="bg-slate-950 p-6 sm:p-8 rounded-2xl border border-slate-800 text-xs text-slate-300 space-y-6">
          {/* Header & Seller details */}
          <div className="flex flex-col sm:flex-row justify-between gap-4 border-b border-slate-800 pb-5">
            <div>
              <div className="font-extrabold text-xl text-white tracking-tight">RR JAGGERY TRADERS</div>
              <div className="text-slate-400 text-[11px] mt-1">Sy No. 45/2, Sugar Mill Road, Mandya, Karnataka - 571401</div>
              <div className="text-slate-400 text-[11px]">GSTIN: <strong className="text-amber-400">29ABCDE1234F1Z5</strong> | PAN: ABCDE1234F</div>
              <div className="text-slate-400 text-[11px]">FSSAI Lic: <strong>11223334000123</strong> | State Code: 29 (Karnataka)</div>
            </div>
            <div className="sm:text-right space-y-1">
              <div className="text-sm font-bold text-amber-400 font-mono">INV-{order.orderNumber}</div>
              <div className="text-slate-400">Order No: <span className="font-mono text-white">{order.orderNumber}</span></div>
              <div className="text-slate-400">Date: {new Date(order.orderDate || order.createdAt || Date.now()).toLocaleDateString()}</div>
              <div className="text-emerald-400 font-bold">Payment: {order.paymentStatus || 'CONFIRMED'} ({order.paymentMode || 'CREDIT'})</div>
            </div>
          </div>

          {/* Bill To */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 border-b border-slate-800 pb-5">
            <div>
              <div className="text-slate-500 font-bold uppercase tracking-wider text-[10px]">Billed & Shipped To</div>
              <div className="text-white font-bold mt-1">{customerDisplayName}</div>
              <div className="text-slate-400">Payment Terms: {order.paymentTerms || 'NET_30'}</div>
            </div>
            <div className="sm:text-right">
              <div className="text-slate-500 font-bold uppercase tracking-wider text-[10px]">HSN / SAC Code</div>
              <div className="text-white font-mono mt-1">17011490 (Cane Jaggery)</div>
              <div className="text-slate-400 text-[11px]">Place of Supply: Karnataka (29)</div>
            </div>
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-slate-800 text-slate-500 text-[10px] uppercase font-semibold">
                  <th className="py-2">Item Description</th>
                  <th className="py-2 text-right">Unit Wt (kg)</th>
                  <th className="py-2 text-right">Qty</th>
                  <th className="py-2 text-right">Wholesale Rate</th>
                  <th className="py-2 text-right">Tax (5%)</th>
                  <th className="py-2 text-right">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {items.map((item: any, idx) => (
                  <tr key={idx}>
                    <td className="py-2.5 text-white font-medium">{item.productName}</td>
                    <td className="py-2.5 text-right text-slate-400 font-mono">{item.unitWeightKg || 10}kg</td>
                    <td className="py-2.5 text-right text-white font-mono">{item.quantity}</td>
                    <td className="py-2.5 text-right text-white font-mono">{formatPrice(item.unitPrice || item.wholesalePrice)}</td>
                    <td className="py-2.5 text-right text-slate-400 font-mono">{formatPrice(item.taxAmount || item.gstAmount || ((item.unitPrice || item.wholesalePrice || 0) * item.quantity * 0.05))}</td>
                    <td className="py-2.5 text-right text-amber-400 font-semibold font-mono">{formatPrice(item.totalAmount || ((item.unitPrice || item.wholesalePrice || 0) * item.quantity * 1.05))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Financial Totals */}
          <div className="flex flex-col sm:flex-row justify-between items-start border-t border-slate-800 pt-4 gap-4">
            <div className="text-[11px] text-slate-400 space-y-1">
              <div>Bank: State Bank of India • Mandya Main Branch</div>
              <div>A/C No: 12345678901 • IFSC: SBIN0001234</div>
            </div>
            <div className="sm:text-right space-y-1.5 w-full sm:w-64">
              <div className="flex justify-between">
                <span>Taxable Amount:</span>
                <span className="font-mono text-white">{formatPrice(order.subtotalAmount)}</span>
              </div>
              <div className="flex justify-between text-slate-400 text-[11px]">
                <span>CGST (2.5%):</span>
                <span className="font-mono">{formatPrice(tax / 2)}</span>
              </div>
              <div className="flex justify-between text-slate-400 text-[11px]">
                <span>SGST (2.5%):</span>
                <span className="font-mono">{formatPrice(tax / 2)}</span>
              </div>
              <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
                <span>Invoice Total:</span>
                <span className="text-amber-400 font-mono">{formatPrice(order.totalAmount)}</span>
              </div>
              <div className="flex justify-between text-emerald-400 font-semibold text-xs">
                <span>Paid Amount:</span>
                <span className="font-mono">{formatPrice(order.paidAmount || 0)}</span>
              </div>
              <div className="flex justify-between text-amber-400 font-bold text-xs">
                <span>Balance to Ledger:</span>
                <span className="font-mono">{formatPrice(order.outstandingAmount || 0)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
