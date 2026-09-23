import { useState, useEffect, useCallback } from 'react';
import {
  Flame, Store, LayoutDashboard, LogIn, UserPlus, LogOut,
  ShieldCheck, ArrowRight, RefreshCw, Activity, Server,
  CheckCircle2, XCircle, Package, Tag, Search,
  Building2, ChevronRight, Eye, EyeOff, AlertCircle,
  TrendingUp, Loader2, Plus, Edit2, X, Check,
  Sparkles, Info, ShoppingCart, Trash2, CreditCard,
  FileText, Truck, Printer, Receipt, ArrowLeft, Users, Factory,
  Shield, BarChart3, AlertTriangle
} from 'lucide-react';
import {
  CustomerDirectoryPage,
  LedgerStatementModal,
  RecordPaymentModal,
  CreateOfflineOrderModal,
  OfflineInvoiceModal,
} from './CustomerLedger';
import type { Customer, OfflineOrder } from './CustomerLedger';
import { Sprint4Operations } from './Sprint4Operations';
import { Sprint5Production } from './Sprint5Production';

// ─── Types ────────────────────────────────────────────────────────────────────

interface User {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  customerType: 'RETAIL' | 'REGISTERED_WHOLESALE' | 'INTERNAL';
  businessName?: string;
  gstin?: string;
  roles: string[];
  enabled: boolean;
}

interface AuthState {
  token: string | null;
  user: User | null;
}

interface Product {
  id: string;
  categoryId: string;
  name: string;
  sku: string;
  slug: string;
  description?: string;
  grade: string;
  packageType: string;
  unitWeightKg: number;
  retailPrice: number;
  wholesalePrice: number;
  wholesaleMoq: number;
  imageUrl?: string;
  featured: boolean;
  active: boolean;
  categoryName?: string;
}

interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  active: boolean;
  displayOrder?: number;
}

interface CartItem {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  grade: string;
  packageType: string;
  unitWeightKg: number;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  imageUrl?: string;
  active: boolean;
}

interface Cart {
  items: CartItem[];
  totalItems: number;
  subtotal: number;
  taxAmount: number;
  shippingAmount: number;
  totalAmount: number;
}

interface Address {
  recipientName: string;
  phone: string;
  streetAddress: string;
  city: string;
  state: string;
  postalCode: string;
}

interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  unitWeightKg: number;
  lineTotal: number;
  taxAmount: number;
}

interface Order {
  id: string;
  orderNumber: string;
  userId: string;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  customerType: string;
  subtotalAmount: number;
  taxAmount: number;
  shippingAmount: number;
  discountAmount: number;
  totalAmount: number;
  status: 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'DISPATCHED' | 'DELIVERED' | 'CANCELLED';
  paymentStatus: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
  paymentMethod: 'TEST_PAYMENT' | 'CASH_ON_DELIVERY' | 'UPI' | 'BANK_TRANSFER';
  shippingAddress: Address;
  notes?: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

interface Invoice {
  invoiceNumber: string;
  orderId: string;
  orderNumber: string;
  invoiceDate: string;
  sellerName: string;
  sellerGstin: string;
  sellerAddress: string;
  sellerContact: string;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  shippingAddress: Address;
  items: OrderItem[];
  subtotal: number;
  cgstAmount: number;
  sgstAmount: number;
  totalTax: number;
  shippingAmount: number;
  grandTotal: number;
  paymentMethod: string;
  paymentStatus: string;
}

interface ServiceHealth {
  name: string;
  code: string;
  port: number;
  endpoint: string;
  schema: string;
  role: string;
  status: 'ONLINE' | 'STANDBY' | 'CONNECTING';
  latencyMs?: number;
  lastChecked?: string;
  payload?: any;
}

// ─── API Constants ────────────────────────────────────────────────────────────

const AUTH_BASE = 'http://localhost:8081/api/v1/auth';
const COMMERCE_BASE = 'http://localhost:8082/api/v1/commerce';

const INITIAL_SERVICES: ServiceHealth[] = [
  { name: 'Auth Service', code: 'auth', port: 8081, endpoint: `${AUTH_BASE}/health`, schema: 'auth_schema', role: 'Security & Identity', status: 'CONNECTING' },
  { name: 'Commerce Service', code: 'commerce', port: 8082, endpoint: `${COMMERCE_BASE}/health`, schema: 'commerce_schema', role: 'Catalog, Cart & Orders', status: 'CONNECTING' },
  { name: 'Customer & Ledger', code: 'customer-ledger', port: 8083, endpoint: 'http://localhost:8083/api/v1/customers/health', schema: 'customer_schema', role: 'Wholesale & Ledgers', status: 'CONNECTING' },
  { name: 'Inventory Service', code: 'inventory', port: 8084, endpoint: 'http://localhost:8084/api/v1/inventory/health', schema: 'inventory_schema', role: 'Stock & Movements', status: 'CONNECTING' },
  { name: 'Procurement', code: 'procurement', port: 8085, endpoint: 'http://localhost:8085/api/v1/procurement/health', schema: 'procurement_schema', role: 'Cane Inward & POs', status: 'CONNECTING' },
  { name: 'Production', code: 'production', port: 8086, endpoint: 'http://localhost:8086/api/v1/production/health', schema: 'production_schema', role: 'Batch Processing', status: 'CONNECTING' },
  { name: 'Finance Service', code: 'finance', port: 8087, endpoint: 'http://localhost:8087/api/v1/finance/health', schema: 'finance_schema', role: 'Costing & Payroll', status: 'CONNECTING' },
  { name: 'Notifications', code: 'notifications', port: 8088, endpoint: 'http://localhost:8088/api/v1/notifications/health', schema: 'notification_schema', role: 'Alerts & Messages', status: 'CONNECTING' },
];

// ─── Helper Functions ─────────────────────────────────────────────────────────

function authHeaders(token: string) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

function formatPrice(price: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(price);
}

function gradeLabel(g: string) {
  const map: Record<string, string> = {
    GRADE_A_TRADITIONAL: 'Grade A Traditional',
    PREMIUM_POWDER: 'Premium Powder',
    EXPORT_CUBES: 'Export Cubes',
    ORGANIC_LIQUID: 'Organic Liquid (Jonna)',
    COMMERCIAL_BULK: 'Commercial Bulk',
  };
  return map[g] ?? g;
}

function packageLabel(p: string) {
  const map: Record<string, string> = {
    BOX: 'Box', POUCH: 'Pouch', JAR: 'Jar', BAG: 'Bag', BUCKET: 'Bucket', TIN: 'Tin',
  };
  return map[p] ?? p;
}

// ─── Auth Storage ─────────────────────────────────────────────────────────────

const TOKEN_KEY = 'rr_jwt_token';
const USER_KEY = 'rr_user';

function loadAuthState(): AuthState {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    const userStr = localStorage.getItem(USER_KEY);
    if (token && userStr) {
      return { token, user: JSON.parse(userStr) };
    }
  } catch {}
  return { token: null, user: null };
}

function saveAuthState(token: string, user: User) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function clearAuthState() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

// ─── UI Helper Components ─────────────────────────────────────────────────────

function Spinner({ size = 4 }: { size?: number }) {
  return <Loader2 className={`h-${size} w-${size} animate-spin text-amber-400`} />;
}

function StatusBadge({ status }: { status: 'ONLINE' | 'STANDBY' | 'CONNECTING' }) {
  const cfg = {
    ONLINE: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    STANDBY: 'bg-slate-800 text-slate-400 border-slate-700',
    CONNECTING: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  }[status];
  return (
    <span className={`flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full border ${cfg}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${status === 'ONLINE' ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`} />
      {status}
    </span>
  );
}

function OrderStatusBadge({ status }: { status: string }) {
  const cfg = {
    PENDING: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    CONFIRMED: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    PROCESSING: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
    DISPATCHED: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
    DELIVERED: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    CANCELLED: 'bg-red-500/10 text-red-400 border-red-500/20',
  }[status] ?? 'bg-slate-800 text-slate-400 border-slate-700';

  return (
    <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full border ${cfg}`}>
      {status}
    </span>
  );
}

// ─── Auth Modal (Login & Register) ────────────────────────────────────────────

function AuthModal({ isOpen, onClose, onLogin }: { isOpen: boolean; onClose: () => void; onLogin: (token: string, user: User) => void }) {
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [regForm, setRegForm] = useState({
    email: '', password: '', fullName: '', phone: '', customerType: 'RETAIL',
    businessName: '', gstin: '',
  });
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setTab('login');
      setError('');
    }
  }, [isOpen]);

  if (!isOpen) return null;

  async function handleLoginSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await fetch(`${AUTH_BASE}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: loginEmail, password: loginPassword }),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        const { token, user } = body.data;
        saveAuthState(token, user);
        onLogin(token, user);
        onClose();
      } else {
        setError(body.error?.message ?? 'Invalid credentials. Please try again.');
      }
    } catch {
      setError('Cannot reach Auth Service. Please ensure backend services are running.');
    } finally {
      setLoading(false);
    }
  }

  async function handleRegisterSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await fetch(`${AUTH_BASE}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(regForm),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        const { token, user } = body.data;
        saveAuthState(token, user);
        onLogin(token, user);
        onClose();
      } else {
        setError(body.error?.message ?? 'Registration failed. Check input details.');
      }
    } catch {
      setError('Cannot reach Auth Service. Please ensure backend services are running.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl relative">
        <button
          id="close-auth-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white hover:bg-slate-700 transition"
        >
          <X className="h-4 w-4" />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="h-10 w-10 rounded-xl jaggery-gradient flex items-center justify-center shadow-lg shadow-amber-900/40">
            <Flame className="h-6 w-6 text-amber-100" />
          </div>
          <div>
            <div className="font-extrabold text-lg text-white">RR JAGGERY TRADERS</div>
            <div className="text-[10px] text-slate-400">Authentication & B2B Portal Access</div>
          </div>
        </div>

        <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800 mb-6">
          <button
            id="tab-login"
            onClick={() => { setTab('login'); setError(''); }}
            className={`flex-1 py-2 rounded-lg text-xs font-semibold transition ${tab === 'login' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            Sign In
          </button>
          <button
            id="tab-register"
            onClick={() => { setTab('register'); setError(''); }}
            className={`flex-1 py-2 rounded-lg text-xs font-semibold transition ${tab === 'register' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            Register
          </button>
        </div>

        {error && (
          <div id="auth-error-message" className="mb-4 flex items-center gap-2 text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2.5">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {tab === 'login' ? (
          <form onSubmit={handleLoginSubmit} className="space-y-4">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Email Address</label>
              <input
                id="login-email"
                type="email"
                required
                value={loginEmail}
                onChange={e => setLoginEmail(e.target.value)}
                placeholder="customer@example.com"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 transition"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Password</label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPw ? 'text' : 'password'}
                  required
                  value={loginPassword}
                  onChange={e => setLoginPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 pr-10 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 transition"
                />
                <button type="button" onClick={() => setShowPw(v => !v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200">
                  {showPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <button
              id="login-submit"
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-sm shadow-lg shadow-amber-600/20 hover:brightness-110 disabled:opacity-50 flex items-center justify-center gap-2 transition"
            >
              {loading ? <><Spinner size={4} /><span>Signing In…</span></> : <><LogIn className="h-4 w-4" /><span>Sign In</span></>}
            </button>
            <p className="text-center text-[11px] text-slate-400 space-y-1">
              <div>Admin: <span className="font-mono text-amber-400 font-semibold">admin@rrjaggery.com</span> / <span className="font-mono text-amber-400">Admin@123</span></div>
              <div>Manager: <span className="font-mono text-amber-400 font-semibold">manager@rrjaggery.com</span> / <span className="font-mono text-amber-400">Manager@123</span></div>
              <div>Customer: <span className="font-mono text-amber-400 font-semibold">retail@example.com</span> / <span className="font-mono text-amber-400">Retail@123</span></div>
            </p>
          </form>
        ) : (
          <form onSubmit={handleRegisterSubmit} className="space-y-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Full Name</label>
              <input
                id="reg-fullname"
                type="text"
                required
                value={regForm.fullName}
                onChange={e => setRegForm(f => ({ ...f, fullName: e.target.value }))}
                placeholder="Rohan Sharma"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 transition"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Email Address</label>
              <input
                id="reg-email"
                type="email"
                required
                value={regForm.email}
                onChange={e => setRegForm(f => ({ ...f, email: e.target.value }))}
                placeholder="customer@example.com"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 transition"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Phone (Optional)</label>
              <input
                id="reg-phone"
                type="tel"
                value={regForm.phone}
                onChange={e => setRegForm(f => ({ ...f, phone: e.target.value }))}
                placeholder="+91 98765 43210"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 transition"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Password</label>
              <input
                id="reg-password"
                type="password"
                required
                minLength={6}
                value={regForm.password}
                onChange={e => setRegForm(f => ({ ...f, password: e.target.value }))}
                placeholder="Min 6 characters"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 transition"
              />
            </div>
            <p className="text-[11px] text-slate-400">
              Registration creates a retail customer storefront account. Wholesale B2B accounts are managed internally by administrators.
            </p>
            <button
              id="register-submit"
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-sm shadow-lg shadow-amber-600/20 hover:brightness-110 disabled:opacity-50 flex items-center justify-center gap-2 transition"
            >
              {loading ? <><Spinner size={4} /><span>Creating Account…</span></> : <><UserPlus className="h-4 w-4" /><span>Create Customer Account</span></>}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

// ─── Cart Drawer Component ────────────────────────────────────────────────────

function CartDrawer({
  isOpen,
  onClose,
  cart,
  onUpdateQty,
  onRemoveItem,
  onProceedToCheckout,
}: {
  isOpen: boolean;
  onClose: () => void;
  cart: Cart | null;
  onUpdateQty: (itemId: string, qty: number) => void;
  onRemoveItem: (itemId: string) => void;
  onProceedToCheckout: () => void;
}) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity" onClick={onClose} />
      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-slate-900 border-l border-slate-800 shadow-2xl flex flex-col justify-between">
          {/* Header */}
          <div className="p-6 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShoppingCart className="h-5 w-5 text-amber-400" />
              <h2 className="text-lg font-bold text-white">Your Shopping Cart</h2>
              <span className="text-xs bg-amber-500/10 text-amber-400 border border-amber-500/20 px-2 py-0.5 rounded-full font-bold">
                {cart?.totalItems ?? 0} items
              </span>
            </div>
            <button id="close-cart-drawer" onClick={onClose} className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white rounded-xl transition">
              <X className="h-4 w-4" />
            </button>
          </div>

          {/* Cart Items List */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {!cart || cart.items.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 space-y-3 text-center">
                <ShoppingCart className="h-12 w-12 text-slate-600" />
                <p className="text-sm font-semibold text-slate-300">Your cart is empty</p>
                <p className="text-xs text-slate-500">Browse our traditional jaggery products and add them to your cart.</p>
              </div>
            ) : (
              cart.items.map(item => (
                <div key={item.id} id={`cart-item-${item.sku}`} className="bg-slate-950 p-4 rounded-2xl border border-slate-800/80 flex flex-col justify-between space-y-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/20">
                        {gradeLabel(item.grade)}
                      </span>
                      <h4 className="font-bold text-sm text-white mt-1">{item.productName}</h4>
                      <p className="text-[11px] text-slate-400 font-mono">SKU: {item.sku} • {item.unitWeightKg}kg {packageLabel(item.packageType)}</p>
                    </div>
                    <button
                      id={`remove-cart-item-${item.sku}`}
                      onClick={() => onRemoveItem(item.id)}
                      className="p-1 text-slate-500 hover:text-red-400 transition"
                      title="Remove item"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
                    <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 rounded-xl px-2 py-1">
                      <button
                        id={`dec-qty-${item.sku}`}
                        onClick={() => onUpdateQty(item.id, item.quantity - 1)}
                        className="h-6 w-6 flex items-center justify-center text-slate-300 hover:text-white font-bold"
                      >
                        -
                      </button>
                      <span className="text-xs font-mono font-bold text-white px-1.5">{item.quantity}</span>
                      <button
                        id={`inc-qty-${item.sku}`}
                        onClick={() => onUpdateQty(item.id, item.quantity + 1)}
                        className="h-6 w-6 flex items-center justify-center text-slate-300 hover:text-white font-bold"
                      >
                        +
                      </button>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-slate-400">{formatPrice(item.unitPrice)} each</div>
                      <div className="text-sm font-bold text-amber-400">{formatPrice(item.lineTotal)}</div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer & Checkout Summary */}
          {cart && cart.items.length > 0 && (
            <div className="p-6 bg-slate-950 border-t border-slate-800 space-y-4">
              <div className="space-y-1.5 text-xs text-slate-400">
                <div className="flex justify-between">
                  <span>Subtotal:</span>
                  <span className="font-medium text-white">{formatPrice(cart.subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span>GST (5% Organic Cane Jaggery):</span>
                  <span className="font-medium text-white">{formatPrice(cart.taxAmount)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Shipping:</span>
                  <span className="font-medium text-emerald-400">
                    {cart.shippingAmount === 0 ? 'FREE (Orders > ₹500)' : formatPrice(cart.shippingAmount)}
                  </span>
                </div>
                <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
                  <span>Grand Total:</span>
                  <span className="text-amber-400 text-base">{formatPrice(cart.totalAmount)}</span>
                </div>
              </div>

              <button
                id="btn-proceed-to-checkout"
                onClick={onProceedToCheckout}
                className="w-full py-3 rounded-xl jaggery-gradient text-slate-950 font-extrabold text-sm shadow-lg shadow-amber-600/20 hover:brightness-110 transition flex items-center justify-center gap-2"
              >
                <span>Proceed to Checkout</span>
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── Checkout Modal Component ─────────────────────────────────────────────────

function CheckoutModal({
  isOpen,
  onClose,
  cart,
  auth,
  onOrderPlaced,
}: {
  isOpen: boolean;
  onClose: () => void;
  cart: Cart | null;
  auth: AuthState;
  onOrderPlaced: (order: Order) => void;
}) {
  const [address, setAddress] = useState<Address>({
    recipientName: auth.user?.fullName ?? '',
    phone: auth.user?.phone ?? '+91 9876543210',
    streetAddress: 'Mandya Highway Road No. 12',
    city: 'Mandya',
    state: 'Karnataka',
    postalCode: '571401',
  });
  const [paymentMethod, setPaymentMethod] = useState<'TEST_PAYMENT' | 'CASH_ON_DELIVERY'>('TEST_PAYMENT');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen || !cart) return null;

  async function handleSubmitOrder(e: React.FormEvent) {
    e.preventDefault();
    if (!auth.token) return;
    setError('');
    setLoading(true);

    try {
      const res = await fetch(`${COMMERCE_BASE}/orders/checkout`, {
        method: 'POST',
        headers: authHeaders(auth.token),
        body: JSON.stringify({
          shippingAddress: address,
          paymentMethod,
          notes,
        }),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        onOrderPlaced(body.data);
      } else {
        setError(body.error?.message ?? 'Checkout failed. Please review your cart.');
      }
    } catch {
      setError('Cannot connect to Commerce Service. Please check backend.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-2xl shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <button
          id="close-checkout-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
        >
          <X className="h-4 w-4" />
        </button>

        <h2 className="text-xl font-bold text-white mb-2 flex items-center gap-2">
          <CreditCard className="h-5 w-5 text-amber-400" /> Secure Checkout & Order Placement
        </h2>
        <p className="text-xs text-slate-400 mb-6">Authoritative server-side price validation and transaction safety</p>

        {error && (
          <div className="mb-4 flex items-center gap-2 text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2.5">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmitOrder} className="space-y-6">
          {/* Shipping Address */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-amber-400">1. Delivery Address</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div>
                <label className="text-slate-400 font-semibold">Recipient Full Name</label>
                <input
                  id="checkout-recipient"
                  type="text"
                  required
                  value={address.recipientName}
                  onChange={e => setAddress(a => ({ ...a, recipientName: e.target.value }))}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div>
                <label className="text-slate-400 font-semibold">Contact Phone</label>
                <input
                  id="checkout-phone"
                  type="tel"
                  required
                  value={address.phone}
                  onChange={e => setAddress(a => ({ ...a, phone: e.target.value }))}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div className="sm:col-span-2">
                <label className="text-slate-400 font-semibold">Street Address / Landmark</label>
                <input
                  id="checkout-street"
                  type="text"
                  required
                  value={address.streetAddress}
                  onChange={e => setAddress(a => ({ ...a, streetAddress: e.target.value }))}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div>
                <label className="text-slate-400 font-semibold">City</label>
                <input
                  id="checkout-city"
                  type="text"
                  required
                  value={address.city}
                  onChange={e => setAddress(a => ({ ...a, city: e.target.value }))}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-slate-400 font-semibold">State</label>
                  <input
                    id="checkout-state"
                    type="text"
                    required
                    value={address.state}
                    onChange={e => setAddress(a => ({ ...a, state: e.target.value }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
                <div>
                  <label className="text-slate-400 font-semibold">PIN Code</label>
                  <input
                    id="checkout-postalcode"
                    type="text"
                    required
                    value={address.postalCode}
                    onChange={e => setAddress(a => ({ ...a, postalCode: e.target.value }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Payment Method */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-amber-400">2. Payment Method</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label
                id="payment-method-test"
                className={`p-4 rounded-2xl border cursor-pointer flex items-center gap-3 transition ${paymentMethod === 'TEST_PAYMENT' ? 'bg-amber-500/10 border-amber-500 text-white' : 'bg-slate-950 border-slate-800 text-slate-400'}`}
              >
                <input
                  type="radio"
                  name="paymentMethod"
                  value="TEST_PAYMENT"
                  checked={paymentMethod === 'TEST_PAYMENT'}
                  onChange={() => setPaymentMethod('TEST_PAYMENT')}
                  className="hidden"
                />
                <Sparkles className="h-5 w-5 text-amber-400" />
                <div>
                  <div className="text-xs font-bold text-white">Instant Test Payment</div>
                  <div className="text-[10px] text-slate-400">Simulates instant online payment confirmation</div>
                </div>
              </label>

              <label
                id="payment-method-cod"
                className={`p-4 rounded-2xl border cursor-pointer flex items-center gap-3 transition ${paymentMethod === 'CASH_ON_DELIVERY' ? 'bg-amber-500/10 border-amber-500 text-white' : 'bg-slate-950 border-slate-800 text-slate-400'}`}
              >
                <input
                  type="radio"
                  name="paymentMethod"
                  value="CASH_ON_DELIVERY"
                  checked={paymentMethod === 'CASH_ON_DELIVERY'}
                  onChange={() => setPaymentMethod('CASH_ON_DELIVERY')}
                  className="hidden"
                />
                <Truck className="h-5 w-5 text-emerald-400" />
                <div>
                  <div className="text-xs font-bold text-white">Cash on Delivery (COD)</div>
                  <div className="text-[10px] text-slate-400">Pay upon mill delivery / weighment receipt</div>
                </div>
              </label>
            </div>
          </div>

          {/* Delivery Notes / Special Instructions */}
          <div className="space-y-1 text-xs">
            <label className="text-slate-400 font-semibold">Special Delivery Instructions / Mill Notes (Optional)</label>
            <input
              id="checkout-notes"
              type="text"
              placeholder="e.g. Call before dispatch, unload at gate #2"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
            />
          </div>

          {/* Order Summary Breakdown */}
          <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 space-y-2 text-xs">
            <div className="flex justify-between text-slate-400">
              <span>Items ({cart.totalItems}):</span>
              <span className="text-white font-medium">{formatPrice(cart.subtotal)}</span>
            </div>
            <div className="flex justify-between text-slate-400">
              <span>GST Tax (5%):</span>
              <span className="text-white font-medium">{formatPrice(cart.taxAmount)}</span>
            </div>
            <div className="flex justify-between text-slate-400">
              <span>Delivery / Shipping:</span>
              <span className="text-emerald-400 font-medium">
                {cart.shippingAmount === 0 ? 'FREE' : formatPrice(cart.shippingAmount)}
              </span>
            </div>
            <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
              <span>Grand Total Payable:</span>
              <span className="text-amber-400 text-base">{formatPrice(cart.totalAmount)}</span>
            </div>
          </div>

          {/* Place Order CTA */}
          <button
            id="btn-place-order"
            type="submit"
            disabled={loading}
            className="w-full py-3.5 rounded-xl jaggery-gradient text-slate-950 font-black text-sm shadow-xl shadow-amber-600/30 hover:brightness-110 disabled:opacity-50 flex items-center justify-center gap-2 transition"
          >
            {loading ? <><Spinner size={5} /><span>Creating Persistent Order…</span></> : <><span>Confirm & Place Order</span> <ArrowRight className="h-4 w-4" /></>}
          </button>
        </form>
      </div>
    </div>
  );
}

// ─── Order Confirmation Modal ─────────────────────────────────────────────────

function OrderConfirmationModal({
  order,
  onClose,
  onViewOrders,
  onViewInvoice,
}: {
  order: Order | null;
  onClose: () => void;
  onViewOrders: () => void;
  onViewInvoice: (orderId: string) => void;
}) {
  if (!order) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-lg shadow-2xl relative text-center space-y-5">
        <button
          id="close-order-confirmation-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
        >
          <X className="h-4 w-4" />
        </button>

        <div className="h-16 w-16 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-3xl flex items-center justify-center mx-auto shadow-lg shadow-emerald-500/10">
          <CheckCircle2 className="h-8 w-8" />
        </div>

        <div>
          <h2 className="text-2xl font-black text-white">Order Confirmed!</h2>
          <p className="text-xs text-slate-400 mt-1">Thank you for your order with RR Jaggery Traders.</p>
        </div>

        <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 text-left text-xs space-y-2">
          <div className="flex justify-between">
            <span className="text-slate-400">Order Reference:</span>
            <span id="confirmed-order-number" className="font-mono font-bold text-amber-400">{order.orderNumber}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-400">Payment Status:</span>
            <span className="font-bold text-emerald-400">{order.paymentStatus}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-400">Total Amount:</span>
            <span className="font-bold text-white">{formatPrice(order.totalAmount)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-400">Delivery To:</span>
            <span className="text-slate-300">{order.shippingAddress.recipientName}, {order.shippingAddress.city}</span>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <button
            id="btn-confirm-view-invoice"
            onClick={() => onViewInvoice(order.id)}
            className="flex-1 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-bold text-xs border border-slate-700 transition flex items-center justify-center gap-1.5"
          >
            <FileText className="h-4 w-4 text-amber-400" /> View Tax Invoice
          </button>
          <button
            id="btn-confirm-view-orders"
            onClick={() => onViewOrders()}
            className="flex-1 py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs shadow-lg shadow-amber-600/20 hover:brightness-110 transition flex items-center justify-center gap-1.5"
          >
            <Receipt className="h-4 w-4" /> My Orders History
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Printable Tax Invoice Modal ──────────────────────────────────────────────

function InvoiceModal({
  orderId,
  auth,
  onClose,
}: {
  orderId: string | null;
  auth: AuthState;
  onClose: () => void;
}) {
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!orderId || !auth.token) return;
    setLoading(true);
    setError('');
    fetch(`${COMMERCE_BASE}/orders/${orderId}/invoice`, { headers: authHeaders(auth.token) })
      .then(res => res.json())
      .then(body => {
        if (body.success) {
          setInvoice(body.data);
        } else {
          setError(body.error?.message ?? 'Failed to load invoice.');
        }
      })
      .catch(() => setError('Error loading invoice details.'))
      .finally(() => setLoading(false));
  }, [orderId, auth.token]);

  if (!orderId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-3xl shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
          <div className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-amber-400" />
            <h2 className="text-lg font-bold text-white">GST Tax Invoice</h2>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => window.print()}
              className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition"
            >
              <Printer className="h-3.5 w-3.5" /> Print Invoice
            </button>
            <button
              id="close-invoice-modal"
              onClick={onClose}
              className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white rounded-xl transition"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-64"><Spinner size={8} /></div>
        ) : error ? (
          <div className="text-center py-12 text-red-400">{error}</div>
        ) : invoice ? (
          <div id="invoice-modal-content" className="bg-slate-950 p-6 sm:p-8 rounded-2xl border border-slate-800 text-xs text-slate-300 space-y-6">
            {/* Header & Seller details */}
            <div className="flex flex-col sm:flex-row justify-between gap-4 border-b border-slate-800 pb-5">
              <div>
                <div id="invoice-seller-name" className="font-extrabold text-xl text-white tracking-tight">{invoice.sellerName}</div>
                <div className="text-slate-400 text-[11px] mt-1">{invoice.sellerAddress}</div>
                <div className="text-slate-400 text-[11px]">GSTIN: <strong className="text-amber-400">{invoice.sellerGstin}</strong></div>
                <div className="text-slate-400 text-[11px]">Contact: {invoice.sellerContact}</div>
              </div>
              <div className="sm:text-right space-y-1">
                <div className="text-sm font-bold text-amber-400 font-mono">{invoice.invoiceNumber}</div>
                <div className="text-slate-400">Order No: <span className="font-mono text-white">{invoice.orderNumber}</span></div>
                <div className="text-slate-400">Date: {new Date(invoice.invoiceDate).toLocaleDateString()}</div>
                <div className="text-emerald-400 font-bold">Payment: {invoice.paymentStatus} ({invoice.paymentMethod})</div>
              </div>
            </div>

            {/* Bill To */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 border-b border-slate-800 pb-5">
              <div>
                <div className="text-slate-500 font-bold uppercase tracking-wider text-[10px]">Billed & Shipped To</div>
                <div className="text-white font-bold mt-1">{invoice.customerName}</div>
                <div className="text-slate-400">{invoice.shippingAddress.streetAddress}</div>
                <div className="text-slate-400">{invoice.shippingAddress.city}, {invoice.shippingAddress.state} - {invoice.shippingAddress.postalCode}</div>
                <div className="text-slate-400">Phone: {invoice.shippingAddress.phone} | Email: {invoice.customerEmail}</div>
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
                    <th className="py-2">SKU</th>
                    <th className="py-2 text-right">Qty</th>
                    <th className="py-2 text-right">Unit Rate</th>
                    <th className="py-2 text-right">Tax (5%)</th>
                    <th className="py-2 text-right">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {invoice.items.map(item => (
                    <tr key={item.id}>
                      <td className="py-2.5 text-white font-medium">{item.productName} ({item.unitWeightKg}kg)</td>
                      <td className="py-2.5 text-slate-400 font-mono">{item.sku}</td>
                      <td className="py-2.5 text-right text-white">{item.quantity}</td>
                      <td className="py-2.5 text-right text-white">{formatPrice(item.unitPrice)}</td>
                      <td className="py-2.5 text-right text-slate-400">{formatPrice(item.taxAmount)}</td>
                      <td className="py-2.5 text-right text-amber-400 font-semibold">{formatPrice(item.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Totals Breakdown */}
            <div className="flex flex-col sm:flex-row justify-between gap-4 pt-4 border-t border-slate-800">
              <div className="text-slate-500 text-[11px] max-w-sm">
                Declaration: We declare that this invoice shows the actual price of the goods described and that all particulars are true and correct.
              </div>
              <div className="space-y-1.5 sm:text-right min-w-[220px]">
                <div className="flex justify-between">
                  <span className="text-slate-400">Subtotal:</span>
                  <span className="text-white font-medium">{formatPrice(invoice.subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">CGST (2.5%):</span>
                  <span className="text-white">{formatPrice(invoice.cgstAmount)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">SGST (2.5%):</span>
                  <span className="text-white">{formatPrice(invoice.sgstAmount)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Shipping:</span>
                  <span className="text-emerald-400">{invoice.shippingAmount === 0 ? 'FREE' : formatPrice(invoice.shippingAmount)}</span>
                </div>
                <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
                  <span>Grand Total:</span>
                  <span className="text-amber-400">{formatPrice(invoice.grandTotal)}</span>
                </div>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
}

// ─── Customer Order History View ──────────────────────────────────────────────

function OrderHistoryView({
  auth,
  onViewInvoice,
  onBackToStore,
}: {
  auth: AuthState;
  onViewInvoice: (orderId: string) => void;
  onBackToStore: () => void;
}) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchOrders = useCallback(async () => {
    if (!auth.token) return;
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${COMMERCE_BASE}/orders`, { headers: authHeaders(auth.token) });
      const body = await res.json();
      if (body.success) {
        setOrders(body.data);
      } else {
        setError(body.error?.message ?? 'Failed to load order history.');
      }
    } catch {
      setError('Cannot reach Commerce Service.');
    } finally {
      setLoading(false);
    }
  }, [auth.token]);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  async function handleCancelOrder(orderId: string) {
    if (!auth.token || !confirm('Are you sure you want to cancel this order?')) return;
    try {
      const res = await fetch(`${COMMERCE_BASE}/orders/${orderId}/cancel`, {
        method: 'PUT',
        headers: authHeaders(auth.token),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        fetchOrders();
      } else {
        alert(body.error?.message ?? 'Failed to cancel order.');
      }
    } catch {
      alert('Error communicating with backend.');
    }
  }

  return (
    <div className="space-y-6 animate-fadeIn">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button onClick={onBackToStore} className="p-2 bg-slate-900 border border-slate-800 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Receipt className="h-5 w-5 text-amber-400" /> My Orders & Purchase History
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">Track fulfillment stages, download tax invoices, or manage orders</p>
          </div>
        </div>
        <button onClick={fetchOrders} className="p-2 bg-slate-900 border border-slate-800 rounded-xl hover:bg-slate-800 transition" title="Refresh">
          <RefreshCw className="h-4 w-4 text-amber-400" />
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64"><Spinner size={8} /></div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center h-64 space-y-3 text-center">
          <XCircle className="h-10 w-10 text-red-400" />
          <p className="text-sm text-red-400">{error}</p>
        </div>
      ) : orders.length === 0 ? (
        <div className="glass-card rounded-3xl p-12 text-center space-y-3">
          <Package className="h-12 w-12 text-slate-600 mx-auto" />
          <h3 className="font-bold text-white text-base">No orders placed yet</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">Explore our authentic Mandya organic jaggery catalogue and place your first order.</p>
          <button onClick={onBackToStore} className="mt-4 px-5 py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs hover:brightness-110 transition">
            Browse Storefront
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map(order => (
            <div key={order.id} id={`order-card-${order.orderNumber}`} className="glass-card rounded-3xl p-6 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800/80 pb-4">
                <div>
                  <div className="flex items-center gap-3">
                    <span className="text-base font-extrabold text-white font-mono">{order.orderNumber}</span>
                    <OrderStatusBadge status={order.status} />
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${order.paymentStatus === 'PAID' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'}`}>
                      {order.paymentStatus}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">
                    Placed on {new Date(order.createdAt).toLocaleDateString()} • {order.items.length} item(s) • Total: <strong className="text-amber-400">{formatPrice(order.totalAmount)}</strong>
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    id={`view-invoice-${order.orderNumber}`}
                    onClick={() => onViewInvoice(order.id)}
                    className="px-3.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-semibold flex items-center gap-1.5 border border-slate-700 transition"
                  >
                    <FileText className="h-3.5 w-3.5 text-amber-400" /> Invoice
                  </button>
                  {(order.status === 'PENDING' || order.status === 'CONFIRMED') && (
                    <button
                      id={`cancel-order-${order.orderNumber}`}
                      onClick={() => handleCancelOrder(order.id)}
                      className="px-3.5 py-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 rounded-xl text-xs font-semibold transition"
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </div>

              {/* Order Items Preview */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                {order.items.map(item => (
                  <div key={item.id} className="bg-slate-950/60 p-3 rounded-2xl border border-slate-800/60 flex justify-between items-center">
                    <div>
                      <div className="font-semibold text-white">{item.productName}</div>
                      <div className="text-slate-400 text-[11px] font-mono">Qty: {item.quantity} • SKU: {item.sku}</div>
                    </div>
                    <div className="text-right font-bold text-amber-400">
                      {formatPrice(item.lineTotal)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Admin Order Management Component ─────────────────────────────────────────

function AdminOrderManagement({ auth, onViewInvoice }: { auth: AuthState; onViewInvoice: (orderId: string) => void }) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchOrders = useCallback(async () => {
    if (!auth.token) return;
    setLoading(true);
    setError('');
    try {
      const url = selectedStatus === 'ALL'
        ? `${COMMERCE_BASE}/admin/orders`
        : `${COMMERCE_BASE}/admin/orders?status=${selectedStatus}`;
      const res = await fetch(url, { headers: authHeaders(auth.token) });
      const body = await res.json();
      if (body.success) {
        setOrders(body.data);
      } else {
        setError(body.error?.message ?? 'Failed to load orders.');
      }
    } catch {
      setError('Cannot connect to Commerce Service admin endpoints.');
    } finally {
      setLoading(false);
    }
  }, [auth.token, selectedStatus]);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  async function handleStatusChange(orderId: string, newStatus: string) {
    if (!auth.token) return;
    try {
      const res = await fetch(`${COMMERCE_BASE}/admin/orders/${orderId}/status`, {
        method: 'PUT',
        headers: authHeaders(auth.token),
        body: JSON.stringify({ status: newStatus }),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        fetchOrders();
      } else {
        alert(body.error?.message ?? 'Failed to update order status.');
      }
    } catch {
      alert('Error updating order status.');
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Receipt className="h-5 w-5 text-amber-400" /> Commercial Order Fulfillment Console
          </h2>
          <p className="text-xs text-slate-400 mt-1">Manage online orders, transition fulfillment lifecycle, inspect invoices (ROLE_ADMIN)</p>
        </div>
        <div className="flex items-center gap-3">
          <select
            id="admin-order-status-filter"
            value={selectedStatus}
            onChange={e => setSelectedStatus(e.target.value)}
            className="bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="PROCESSING">Processing</option>
            <option value="DISPATCHED">Dispatched</option>
            <option value="DELIVERED">Delivered</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button onClick={fetchOrders} className="p-2 bg-slate-900 border border-slate-800 rounded-xl hover:bg-slate-800 transition" title="Refresh">
            <RefreshCw className="h-4 w-4 text-amber-400" />
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64"><Spinner size={8} /></div>
      ) : error ? (
        <div className="text-center py-12 text-red-400">{error}</div>
      ) : orders.length === 0 ? (
        <div className="glass-card rounded-2xl p-12 text-center text-slate-500 text-xs">
          No customer orders found matching filter.
        </div>
      ) : (
        <div className="glass-card rounded-2xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/60">
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">Order Number</th>
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">Customer</th>
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">Type</th>
                  <th className="text-right px-4 py-3 text-slate-400 font-semibold">Amount</th>
                  <th className="text-center px-4 py-3 text-slate-400 font-semibold">Payment</th>
                  <th className="text-center px-4 py-3 text-slate-400 font-semibold">Fulfillment Stage</th>
                  <th className="text-center px-4 py-3 text-slate-400 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map(o => (
                  <tr key={o.id} id={`admin-order-row-${o.orderNumber}`} className="border-b border-slate-800/60 hover:bg-slate-800/30 transition">
                    <td className="px-4 py-3 font-mono font-bold text-amber-400">{o.orderNumber}</td>
                    <td className="px-4 py-3">
                      <div className="text-white font-medium">{o.customerName}</div>
                      <div className="text-[11px] text-slate-400">{o.customerEmail}</div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-bold">{o.customerType}</span>
                    </td>
                    <td className="px-4 py-3 text-right font-bold text-white">{formatPrice(o.totalAmount)}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${o.paymentStatus === 'PAID' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'}`}>
                        {o.paymentStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <select
                        id={`select-status-${o.orderNumber}`}
                        value={o.status}
                        onChange={e => handleStatusChange(o.id, e.target.value)}
                        className="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-white focus:outline-none focus:border-amber-500"
                      >
                        <option value="PENDING">PENDING</option>
                        <option value="CONFIRMED">CONFIRMED</option>
                        <option value="PROCESSING">PROCESSING</option>
                        <option value="DISPATCHED">DISPATCHED</option>
                        <option value="DELIVERED">DELIVERED</option>
                        <option value="CANCELLED">CANCELLED</option>
                      </select>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        id={`admin-view-invoice-${o.orderNumber}`}
                        onClick={() => onViewInvoice(o.id)}
                        className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg transition"
                        title="View Tax Invoice"
                      >
                        <FileText className="h-3.5 w-3.5 text-amber-400" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Storefront Component ─────────────────────────────────────────────────────

function Storefront({
  auth,
  onOpenAuth,
  onAddToCart,
}: {
  auth: AuthState;
  onOpenAuth: () => void;
  onAddToCart: (productId: string, quantity: number) => void;
}) {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedGrade, setSelectedGrade] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeModalProduct, setActiveModalProduct] = useState<Product | null>(null);

  const isWholesale = auth.user?.customerType === 'REGISTERED_WHOLESALE';

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [catRes, prodRes] = await Promise.all([
        fetch(`${COMMERCE_BASE}/categories`),
        fetch(`${COMMERCE_BASE}/products${selectedCategory ? `?categoryId=${selectedCategory}` : ''}${query ? `${selectedCategory ? '&' : '?'}query=${encodeURIComponent(query)}` : ''}`),
      ]);

      const [catBody, prodBody] = await Promise.all([catRes.json(), prodRes.json()]);
      if (catBody.success) setCategories(catBody.data);
      if (prodBody.success) {
        let prods: Product[] = prodBody.data;
        if (selectedGrade) {
          prods = prods.filter(p => p.grade === selectedGrade);
        }
        setProducts(prods);
      }
    } catch {
      setError('Cannot reach Commerce Service. Please check if services are running.');
    } finally {
      setLoading(false);
    }
  }, [selectedCategory, selectedGrade, query]);

  useEffect(() => { fetchData(); }, [fetchData]);

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Hero Banner */}
      <div className="relative overflow-hidden rounded-3xl border border-amber-500/20 bg-gradient-to-r from-amber-950/40 via-slate-900/60 to-slate-950 p-8 md:p-12">
        <div className="max-w-2xl space-y-4">
          <span className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-amber-400 bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20">
            <ShieldCheck className="h-3.5 w-3.5" />
            100% Organic • Direct From Mandya Mills
          </span>
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white leading-tight">
            Pure Traditional Jaggery,{' '}
            <span className="jaggery-text-gradient">Engineered For Quality</span>
          </h1>
          <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
            Wholesale & retail supply of chemical-free jaggery blocks, granulated powders, and artisanal cubes.
            Direct farm sourcing with transparent batch traceability.
            {isWholesale && <span className="text-amber-400 font-semibold"> Wholesale B2B pricing active for your account.</span>}
          </p>
          <div className="flex flex-wrap gap-3 pt-2">
            {!auth.token ? (
              <button
                id="hero-signin-btn"
                onClick={onOpenAuth}
                className="px-5 py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-sm shadow-lg shadow-amber-600/30 hover:brightness-110 flex items-center gap-2"
              >
                Sign In to Order <ArrowRight className="h-4 w-4" />
              </button>
            ) : (
              <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs font-semibold text-emerald-400">
                <Check className="h-4 w-4" /> Welcome, {auth.user?.fullName} ({auth.user?.customerType})
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Catalogue Controls & Filters */}
      <div>
        <div className="flex flex-col md:flex-row md:items-center justify-between mb-6 gap-4">
          <div>
            <h2 className="text-xl font-bold text-white">Product Catalogue</h2>
            <p className="text-xs text-slate-400">
              {products.length} products available {isWholesale ? '• Wholesale pricing active' : '• Retail pricing active'}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
              <input
                id="catalogue-search"
                type="text"
                placeholder="Search jaggery products…"
                value={query}
                onChange={e => setQuery(e.target.value)}
                className="bg-slate-900 border border-slate-700 rounded-xl pl-9 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-500 transition w-56"
              />
            </div>

            <select
              id="catalogue-grade-filter"
              value={selectedGrade ?? ''}
              onChange={e => setSelectedGrade(e.target.value || null)}
              className="bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
            >
              <option value="">All Grades</option>
              <option value="GRADE_A_TRADITIONAL">Grade A Traditional</option>
              <option value="PREMIUM_POWDER">Premium Powder</option>
              <option value="EXPORT_CUBES">Export Cubes</option>
              <option value="ORGANIC_LIQUID">Organic Liquid</option>
            </select>
          </div>
        </div>

        {/* Category Pills */}
        {categories.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-6">
            <button
              id="category-pill-all"
              onClick={() => setSelectedCategory(null)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${!selectedCategory ? 'bg-amber-500 text-slate-950' : 'bg-slate-800 text-slate-400 hover:text-white border border-slate-700'}`}
            >
              All Categories
            </button>
            {categories.map(cat => (
              <button
                key={cat.id}
                id={`category-pill-${cat.slug}`}
                onClick={() => setSelectedCategory(cat.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-1 ${selectedCategory === cat.id ? 'bg-amber-500 text-slate-950' : 'bg-slate-800 text-slate-400 hover:text-white border border-slate-700'}`}
              >
                <Tag className="h-3 w-3" /> {cat.name}
              </button>
            ))}
          </div>
        )}

        {/* Product Cards Grid */}
        {loading ? (
          <div className="flex items-center justify-center h-48"><Spinner size={8} /></div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center h-48 space-y-3 text-center">
            <XCircle className="h-10 w-10 text-red-400" />
            <p className="text-sm text-red-400">{error}</p>
            <button onClick={fetchData} className="text-xs text-amber-400 hover:text-amber-300 underline">Retry</button>
          </div>
        ) : products.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 space-y-3 text-center">
            <Package className="h-10 w-10 text-slate-600" />
            <p className="text-sm text-slate-400">No products found matching your filter.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {products.map(product => (
              <div
                key={product.id}
                id={`product-card-${product.sku}`}
                className="glass-card rounded-2xl p-5 flex flex-col justify-between hover:shadow-xl hover:shadow-amber-900/10 hover:-translate-y-0.5 transition-all duration-200"
              >
                <div>
                  <div className="flex items-start justify-between mb-3">
                    <span className="text-[10px] uppercase font-bold tracking-wider px-2.5 py-1 rounded-md bg-amber-500/10 text-amber-300 border border-amber-500/20">
                      {gradeLabel(product.grade)}
                    </span>
                    <span className="text-xs text-slate-400 font-mono">{product.sku}</span>
                  </div>
                  <h3 className="font-bold text-sm text-white mb-1">{product.name}</h3>
                  {product.description && (
                    <p className="text-xs text-slate-400 mb-3 line-clamp-2">{product.description}</p>
                  )}
                  <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-800 space-y-1 my-3">
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400">Retail Price:</span>
                      <span className="font-bold text-emerald-400">{formatPrice(product.retailPrice)}</span>
                    </div>
                    {isWholesale && (
                      <div className="flex justify-between text-xs">
                        <span className="text-amber-400 font-medium">Wholesale Rate:</span>
                        <span className="font-bold text-amber-400">{formatPrice(product.wholesalePrice)}</span>
                      </div>
                    )}
                    <div className="flex justify-between text-xs text-slate-500">
                      <span>Package / Weight:</span>
                      <span>{packageLabel(product.packageType)} • {product.unitWeightKg}kg</span>
                    </div>
                  </div>
                </div>

                <div className="flex gap-2 mt-4">
                  <button
                    id={`add-to-cart-${product.sku}`}
                    onClick={() => onAddToCart(product.id, isWholesale ? product.wholesaleMoq : 1)}
                    className="flex-1 py-2 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs shadow-md shadow-amber-600/20 hover:brightness-110 transition flex items-center justify-center gap-1.5"
                  >
                    <ShoppingCart className="h-3.5 w-3.5" /> Add to Cart
                  </button>
                  <button
                    id={`view-details-${product.sku}`}
                    onClick={() => setActiveModalProduct(product)}
                    className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition"
                    title="View Details"
                  >
                    <Info className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Product Modal */}
      {activeModalProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-lg shadow-2xl relative max-h-[90vh] overflow-y-auto">
            <button
              id="close-product-modal"
              onClick={() => setActiveModalProduct(null)}
              className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
            >
              <X className="h-4 w-4" />
            </button>
            <div className="space-y-4">
              <span className="text-xs uppercase font-bold tracking-wider px-2.5 py-1 rounded-md bg-amber-500/10 text-amber-300 border border-amber-500/20">
                {gradeLabel(activeModalProduct.grade)}
              </span>
              <h2 className="text-xl font-bold text-white">{activeModalProduct.name}</h2>
              <p className="text-sm text-slate-300 leading-relaxed">{activeModalProduct.description ?? 'Traditional high-purity jaggery produced without chemical clarifiers.'}</p>
              <div className="grid grid-cols-2 gap-3 bg-slate-950 p-4 rounded-2xl border border-slate-800 text-xs">
                <div>
                  <div className="text-slate-500">Packaging Type</div>
                  <div className="text-sm font-semibold text-white">{packageLabel(activeModalProduct.packageType)}</div>
                </div>
                <div>
                  <div className="text-slate-500">Unit Weight</div>
                  <div className="text-sm font-semibold text-white">{activeModalProduct.unitWeightKg} kg</div>
                </div>
                <div>
                  <div className="text-slate-500">Retail Rate</div>
                  <div className="text-sm font-bold text-emerald-400">{formatPrice(activeModalProduct.retailPrice)}</div>
                </div>
                <div>
                  <div className="text-slate-500">Wholesale Rate</div>
                  <div className="text-sm font-bold text-amber-400">{formatPrice(activeModalProduct.wholesalePrice)}</div>
                </div>
                <div className="col-span-2 pt-2 border-t border-slate-800/80 flex justify-between text-slate-400">
                  <span>Wholesale MOQ:</span>
                  <span className="font-bold text-white">{activeModalProduct.wholesaleMoq} units</span>
                </div>
              </div>
              <button
                onClick={() => {
                  onAddToCart(activeModalProduct.id, isWholesale ? activeModalProduct.wholesaleMoq : 1);
                  setActiveModalProduct(null);
                }}
                className="w-full py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs shadow-lg shadow-amber-600/20 hover:brightness-110 transition"
              >
                Add To Cart Now
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Admin Product & Category Management ──────────────────────────────────────

function AdminCatalogue({ auth }: { auth: AuthState }) {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState<'products' | 'categories'>('products');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);

  const [productForm, setProductForm] = useState({
    name: '', sku: '', slug: '', description: '',
    categoryId: '', grade: 'GRADE_A_TRADITIONAL', packageType: 'BOX',
    unitWeightKg: 1.0, retailPrice: 100, wholesalePrice: 80, wholesaleMoq: 20,
    featured: false, active: true
  });
  const [catForm, setCatForm] = useState({ name: '', slug: '', description: '', active: true, displayOrder: 1 });

  const fetchData = useCallback(async () => {
    if (!auth.token) return;
    setLoading(true);
    setError('');
    try {
      const [catRes, prodRes] = await Promise.all([
        fetch(`${COMMERCE_BASE}/admin/categories`, { headers: authHeaders(auth.token) }),
        fetch(`${COMMERCE_BASE}/admin/products`, { headers: authHeaders(auth.token) }),
      ]);
      const [catBody, prodBody] = await Promise.all([catRes.json(), prodRes.json()]);
      if (catBody.success) setCategories(catBody.data);
      if (prodBody.success) setProducts(prodBody.data);
    } catch {
      setError('Cannot reach Commerce Service admin endpoints.');
    } finally {
      setLoading(false);
    }
  }, [auth.token]);

  useEffect(() => { fetchData(); }, [fetchData]);

  async function handleToggleActive(product: Product) {
    if (!auth.token) return;
    try {
      const res = await fetch(`${COMMERCE_BASE}/admin/products/${product.id}`, {
        method: 'PUT',
        headers: authHeaders(auth.token),
        body: JSON.stringify({ ...product, active: !product.active }),
      });
      if (res.ok) fetchData();
    } catch {
      alert('Failed to toggle product status.');
    }
  }

  async function handleSaveProduct(e: React.FormEvent) {
    e.preventDefault();
    if (!auth.token) return;
    try {
      const url = editingProduct
        ? `${COMMERCE_BASE}/admin/products/${editingProduct.id}`
        : `${COMMERCE_BASE}/admin/products`;
      const method = editingProduct ? 'PUT' : 'POST';

      const payload = {
        ...productForm,
        categoryId: productForm.categoryId || categories[0]?.id,
        unitWeightKg: Number(productForm.unitWeightKg),
        retailPrice: Number(productForm.retailPrice),
        wholesalePrice: Number(productForm.wholesalePrice),
        wholesaleMoq: Number(productForm.wholesaleMoq),
      };

      const res = await fetch(url, {
        method,
        headers: authHeaders(auth.token),
        body: JSON.stringify(payload),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        setShowCreateModal(false);
        setEditingProduct(null);
        fetchData();
      } else {
        alert(body.error?.message ?? 'Failed to save product.');
      }
    } catch {
      alert('Error connecting to Commerce Service.');
    }
  }

  async function handleSaveCategory(e: React.FormEvent) {
    e.preventDefault();
    if (!auth.token) return;
    try {
      const res = await fetch(`${COMMERCE_BASE}/admin/categories`, {
        method: 'POST',
        headers: authHeaders(auth.token),
        body: JSON.stringify(catForm),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        setShowCategoryModal(false);
        setCatForm({ name: '', slug: '', description: '', active: true, displayOrder: 1 });
        fetchData();
      } else {
        alert(body.error?.message ?? 'Failed to save category.');
      }
    } catch {
      alert('Error connecting to Commerce Service.');
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Package className="h-5 w-5 text-amber-400" /> Master Catalogue Administration
          </h2>
          <p className="text-xs text-slate-400 mt-1">Manage products, grades, categories, and wholesale rates (ROLE_ADMIN)</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center bg-slate-900 p-1 rounded-xl border border-slate-800 text-xs">
            <button
              id="admin-tab-products"
              onClick={() => setTab('products')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'products' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
            >
              Products ({products.length})
            </button>
            <button
              id="admin-tab-categories"
              onClick={() => setTab('categories')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'categories' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
            >
              Categories ({categories.length})
            </button>
          </div>

          {tab === 'products' ? (
            <button
              id="btn-add-product"
              onClick={() => {
                setEditingProduct(null);
                setProductForm({
                  name: '', sku: '', slug: '', description: '',
                  categoryId: categories[0]?.id ?? '', grade: 'GRADE_A_TRADITIONAL',
                  packageType: 'BOX', unitWeightKg: 1.0, retailPrice: 150,
                  wholesalePrice: 110, wholesaleMoq: 20, featured: false, active: true
                });
                setShowCreateModal(true);
              }}
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold rounded-xl text-xs transition"
            >
              <Plus className="h-4 w-4" /> Add Product
            </button>
          ) : (
            <button
              id="btn-add-category"
              onClick={() => setShowCategoryModal(true)}
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold rounded-xl text-xs transition"
            >
              <Plus className="h-4 w-4" /> Add Category
            </button>
          )}

          <button onClick={fetchData} className="p-2 bg-slate-900 border border-slate-800 rounded-xl hover:bg-slate-800 transition" title="Refresh">
            <RefreshCw className="h-4 w-4 text-amber-400" />
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-48"><Spinner size={8} /></div>
      ) : error ? (
        <div className="text-center py-12 text-red-400">{error}</div>
      ) : tab === 'products' ? (
        <div className="glass-card rounded-2xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/60">
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">Product Name</th>
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">SKU</th>
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">Grade</th>
                  <th className="text-right px-4 py-3 text-slate-400 font-semibold">Retail Rate</th>
                  <th className="text-right px-4 py-3 text-slate-400 font-semibold">Wholesale Rate</th>
                  <th className="text-center px-4 py-3 text-slate-400 font-semibold">Active</th>
                  <th className="text-center px-4 py-3 text-slate-400 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {products.map(p => (
                  <tr key={p.id} id={`admin-product-row-${p.sku}`} className="border-b border-slate-800/60 hover:bg-slate-800/30 transition">
                    <td className="px-4 py-3 text-white font-medium">{p.name}</td>
                    <td className="px-4 py-3 text-slate-400 font-mono">{p.sku}</td>
                    <td className="px-4 py-3">
                      <span className="px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/20">{gradeLabel(p.grade)}</span>
                    </td>
                    <td className="px-4 py-3 text-right text-emerald-400 font-semibold">{formatPrice(p.retailPrice)}</td>
                    <td className="px-4 py-3 text-right text-amber-400 font-semibold">{formatPrice(p.wholesalePrice)}</td>
                    <td className="px-4 py-3 text-center">
                      <button
                        id={`toggle-active-${p.sku}`}
                        onClick={() => handleToggleActive(p)}
                        className={`px-2.5 py-1 rounded-lg text-[10px] font-bold transition ${p.active ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-red-500/20 text-red-400 border border-red-500/30'}`}
                      >
                        {p.active ? 'ACTIVE' : 'INACTIVE'}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        id={`edit-product-${p.sku}`}
                        onClick={() => {
                          setEditingProduct(p);
                          setProductForm({
                            name: p.name, sku: p.sku, slug: p.slug, description: p.description ?? '',
                            categoryId: p.categoryId, grade: p.grade, packageType: p.packageType,
                            unitWeightKg: p.unitWeightKg, retailPrice: p.retailPrice,
                            wholesalePrice: p.wholesalePrice, wholesaleMoq: p.wholesaleMoq,
                            featured: p.featured, active: p.active
                          });
                          setShowCreateModal(true);
                        }}
                        className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg transition"
                        title="Edit Product"
                      >
                        <Edit2 className="h-3.5 w-3.5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="glass-card rounded-2xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/60">
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">Category Name</th>
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">Slug</th>
                  <th className="text-left px-4 py-3 text-slate-400 font-semibold">Description</th>
                  <th className="text-center px-4 py-3 text-slate-400 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody>
                {categories.map(c => (
                  <tr key={c.id} className="border-b border-slate-800/60 hover:bg-slate-800/30 transition">
                    <td className="px-4 py-3 text-white font-medium flex items-center gap-2"><Tag className="h-3.5 w-3.5 text-amber-400" />{c.name}</td>
                    <td className="px-4 py-3 text-slate-400 font-mono">{c.slug}</td>
                    <td className="px-4 py-3 text-slate-400">{c.description ?? '—'}</td>
                    <td className="px-4 py-3 text-center">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">ACTIVE</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Create/Edit Product Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-lg shadow-2xl relative max-h-[90vh] overflow-y-auto">
            <button
              id="close-create-product-modal"
              onClick={() => setShowCreateModal(false)}
              className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
            >
              <X className="h-4 w-4" />
            </button>
            <h2 className="text-lg font-bold text-white mb-4">
              {editingProduct ? 'Edit Product' : 'Create New Jaggery Product'}
            </h2>
            <form onSubmit={handleSaveProduct} className="space-y-3 text-xs">
              <div>
                <label className="text-slate-400 font-semibold">Product Name</label>
                <input
                  id="form-prod-name"
                  type="text"
                  required
                  value={productForm.name}
                  onChange={e => setProductForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-400 font-semibold">SKU</label>
                  <input
                    id="form-prod-sku"
                    type="text"
                    required
                    value={productForm.sku}
                    onChange={e => setProductForm(f => ({ ...f, sku: e.target.value }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
                <div>
                  <label className="text-slate-400 font-semibold">Slug</label>
                  <input
                    id="form-prod-slug"
                    type="text"
                    required
                    value={productForm.slug}
                    onChange={e => setProductForm(f => ({ ...f, slug: e.target.value }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-400 font-semibold">Category</label>
                  <select
                    id="form-prod-category"
                    value={productForm.categoryId}
                    onChange={e => setProductForm(f => ({ ...f, categoryId: e.target.value }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  >
                    {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-slate-400 font-semibold">Grade</label>
                  <select
                    id="form-prod-grade"
                    value={productForm.grade}
                    onChange={e => setProductForm(f => ({ ...f, grade: e.target.value }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  >
                    <option value="GRADE_A_TRADITIONAL">Grade A Traditional</option>
                    <option value="PREMIUM_POWDER">Premium Powder</option>
                    <option value="EXPORT_CUBES">Export Cubes</option>
                    <option value="ORGANIC_LIQUID">Organic Liquid</option>
                    <option value="COMMERCIAL_BULK">Commercial Bulk</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="text-slate-400 font-semibold">Packaging</label>
                  <select
                    id="form-prod-pkg"
                    value={productForm.packageType}
                    onChange={e => setProductForm(f => ({ ...f, packageType: e.target.value }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  >
                    <option value="BOX">Box</option>
                    <option value="POUCH">Pouch</option>
                    <option value="JAR">Jar</option>
                    <option value="BAG">Bag</option>
                    <option value="BUCKET">Bucket</option>
                    <option value="TIN">Tin</option>
                  </select>
                </div>
                <div>
                  <label className="text-slate-400 font-semibold">Unit Wt (kg)</label>
                  <input
                    id="form-prod-weight"
                    type="number"
                    step="0.1"
                    required
                    value={productForm.unitWeightKg}
                    onChange={e => setProductForm(f => ({ ...f, unitWeightKg: parseFloat(e.target.value) || 0 }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
                <div>
                  <label className="text-slate-400 font-semibold">Wholesale MOQ</label>
                  <input
                    id="form-prod-moq"
                    type="number"
                    required
                    value={productForm.wholesaleMoq}
                    onChange={e => setProductForm(f => ({ ...f, wholesaleMoq: parseInt(e.target.value) || 1 }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-slate-400 font-semibold">Retail Price (₹)</label>
                  <input
                    id="form-prod-retailprice"
                    type="number"
                    step="0.01"
                    required
                    value={productForm.retailPrice}
                    onChange={e => setProductForm(f => ({ ...f, retailPrice: parseFloat(e.target.value) || 0 }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
                <div>
                  <label className="text-slate-400 font-semibold">Wholesale Price (₹)</label>
                  <input
                    id="form-prod-wholesaleprice"
                    type="number"
                    step="0.01"
                    required
                    value={productForm.wholesalePrice}
                    onChange={e => setProductForm(f => ({ ...f, wholesalePrice: parseFloat(e.target.value) || 0 }))}
                    className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>
              <div>
                <label className="text-slate-400 font-semibold">Description</label>
                <textarea
                  id="form-prod-desc"
                  rows={2}
                  value={productForm.description}
                  onChange={e => setProductForm(f => ({ ...f, description: e.target.value }))}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <button
                id="submit-save-product"
                type="submit"
                className="w-full mt-4 py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs shadow-lg shadow-amber-600/20 hover:brightness-110 transition"
              >
                {editingProduct ? 'Save Product Changes' : 'Create Jaggery Product'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Create Category Modal */}
      {showCategoryModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl relative">
            <button
              id="close-create-cat-modal"
              onClick={() => setShowCategoryModal(false)}
              className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
            >
              <X className="h-4 w-4" />
            </button>
            <h2 className="text-lg font-bold text-white mb-4">Add Jaggery Category</h2>
            <form onSubmit={handleSaveCategory} className="space-y-3 text-xs">
              <div>
                <label className="text-slate-400 font-semibold">Category Name</label>
                <input
                  id="form-cat-name"
                  type="text"
                  required
                  value={catForm.name}
                  onChange={e => setCatForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div>
                <label className="text-slate-400 font-semibold">Slug</label>
                <input
                  id="form-cat-slug"
                  type="text"
                  required
                  value={catForm.slug}
                  onChange={e => setCatForm(f => ({ ...f, slug: e.target.value }))}
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <button
                id="submit-save-category"
                type="submit"
                className="w-full mt-4 py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs shadow-lg shadow-amber-600/20 hover:brightness-110 transition"
              >
                Create Category
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Service Health Monitor ────────────────────────────────────────────────────

function ServiceHealthMonitor() {
  const [services, setServices] = useState<ServiceHealth[]>(INITIAL_SERVICES);
  const [isChecking, setIsChecking] = useState(false);
  const [lastCheck, setLastCheck] = useState('');

  const checkAll = useCallback(async () => {
    setIsChecking(true);
    const updated = await Promise.all(
      INITIAL_SERVICES.map(async svc => {
        const start = performance.now();
        try {
          const ctrl = new AbortController();
          const tid = setTimeout(() => ctrl.abort(), 2500);
          const res = await fetch(svc.endpoint, { signal: ctrl.signal });
          clearTimeout(tid);
          const latency = Math.round(performance.now() - start);
          if (res.ok) {
            const data = await res.json();
            return { ...svc, status: 'ONLINE' as const, latencyMs: latency, lastChecked: new Date().toLocaleTimeString(), payload: data?.data ?? data };
          }
          return { ...svc, status: 'STANDBY' as const, latencyMs: latency, lastChecked: new Date().toLocaleTimeString() };
        } catch {
          return { ...svc, status: 'STANDBY' as const, lastChecked: new Date().toLocaleTimeString() };
        }
      })
    );
    setServices(updated);
    setIsChecking(false);
    setLastCheck(new Date().toLocaleTimeString());
  }, []);

  useEffect(() => {
    checkAll();
    const id = setInterval(checkAll, 15000);
    return () => clearInterval(id);
  }, [checkAll]);

  const onlineCount = services.filter(s => s.status === 'ONLINE').length;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Activity className="h-5 w-5 text-emerald-400" /> Live Microservice Mesh Status
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            {onlineCount}/{services.length} services online • {lastCheck ? `Last ping: ${lastCheck}` : 'Checking…'}
          </p>
        </div>
        <button
          onClick={checkAll}
          disabled={isChecking}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 rounded-xl text-xs font-semibold transition disabled:opacity-50"
        >
          <RefreshCw className={`h-3.5 w-3.5 text-amber-400 ${isChecking ? 'animate-spin' : ''}`} />
          {isChecking ? 'Testing…' : 'Ping Services'}
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {services.map(svc => (
          <div key={svc.code} className="glass-card rounded-2xl p-5 space-y-3">
            <div className="flex items-start justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-sm text-white">{svc.name}</h3>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400 font-mono font-bold">:{svc.port}</span>
                </div>
                <span className="text-xs text-slate-400">{svc.role}</span>
              </div>
              <div className="flex items-center gap-2">
                <StatusBadge status={svc.status} />
                {svc.latencyMs !== undefined && svc.status === 'ONLINE' && (
                  <span className="text-[10px] font-mono text-emerald-300">{svc.latencyMs}ms</span>
                )}
              </div>
            </div>

            {svc.payload && (
              <div className="bg-slate-950/60 p-2.5 rounded-xl border border-slate-800/60 text-[11px] font-mono text-slate-300 space-y-1">
                <div className="flex justify-between text-slate-400">
                  <span>Status: <strong className="text-emerald-400">{svc.payload.status}</strong></span>
                  {svc.payload.version && <span>v{svc.payload.version}</span>}
                </div>
                <div className="text-slate-500 truncate">Schema: <span className="text-amber-400">{svc.schema}</span></div>
              </div>
            )}

            <div className="pt-2 border-t border-slate-800/80 text-[11px] text-slate-500 font-mono truncate">
              {svc.endpoint}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Sprint Roadmap ────────────────────────────────────────────────────────────

function SprintRoadmap() {
  const sprints = [
    { s: 'Sprint 0', name: 'Foundation & Architecture', status: 'DONE', desc: 'Microservice topology, Docker Compose, PostgreSQL schemas, CI workflow' },
    { s: 'Sprint 1', name: 'Authentication & Product Catalogue', status: 'DONE', desc: 'JWT login/register, RBAC, product CRUD, category CRUD, public catalogue' },
    { s: 'Sprint 2', name: 'Cart, Checkout & Orders', status: 'DONE', desc: 'Shopping cart, atomic checkout, order lifecycle, invoice generation' },
    { s: 'Sprint 3', name: 'Customer, Wholesale & Ledger', status: 'DONE', desc: 'Retail, B2B wholesale, offline wholesalers, credit limits, ledger accounting' },
    { s: 'Sprint 4', name: 'Inventory & Procurement', status: 'DONE', desc: 'Raw material & finished goods inventory, immutable stock movements' },
    { s: 'Sprint 5', name: 'Production Management', status: 'CURRENT', desc: 'Recipes/BOM, production batches, stages, yield & wastage tracking' },
    { s: 'Sprint 6', name: 'Costing, Expenses & Payroll', status: 'PENDING', desc: 'Operating expenses, batch costing per KG, piece-rate wages' },
    { s: 'Sprint 7', name: 'Dashboard, Reports & Notifications', status: 'PENDING', desc: 'Executive dashboard, receivables/payables, low stock alerts' },
    { s: 'Sprint 8', name: 'Hardening & Security', status: 'PENDING', desc: 'E2E tests, security review, automated backups, Docker optimizations' },
    { s: 'Sprint 9', name: 'OCI Production Deployment', status: 'PENDING', desc: 'OCI Ampere A1 ARM64, domain/SSL, smoke testing and runbooks' },
  ];

  return (
    <div className="glass-card rounded-3xl p-6 sm:p-8 space-y-4">
      <h2 className="text-lg font-bold text-white">Agile Sprint Delivery Roadmap</h2>
      <div className="space-y-3">
        {sprints.map((s, i) => (
          <div key={i} className={`p-4 rounded-xl border flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${s.status === 'CURRENT' ? 'bg-amber-950/20 border-amber-500/30' : 'bg-slate-900/60 border-slate-800'}`}>
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                {s.status === 'DONE' && <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />}
                {s.status === 'CURRENT' && <Activity className="h-4 w-4 text-amber-400 animate-pulse shrink-0" />}
                {s.status === 'PENDING' && <ChevronRight className="h-4 w-4 text-slate-600 shrink-0" />}
                <span className="font-bold text-sm text-white">{s.s}: {s.name}</span>
              </div>
              <p className="text-xs text-slate-400 pl-6">{s.desc}</p>
            </div>
            <span className={`text-[10px] px-2.5 py-1 rounded-full font-bold uppercase tracking-wider self-start sm:self-center shrink-0 ${
              s.status === 'DONE' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
              : s.status === 'CURRENT' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40 animate-pulse'
              : 'bg-slate-800 text-slate-500 border border-slate-700'
            }`}>
              {s.status === 'DONE' ? 'Complete' : s.status === 'CURRENT' ? 'In Progress' : 'Upcoming'}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Admin Dashboard ───────────────────────────────────────────────────────────

// ─── Admin Overview (Real-Data Executive Dashboard) ──────────────────────────

function AdminOverview({ auth }: { auth: AuthState }) {
  const [loading, setLoading] = useState(true);
  const [authStats, setAuthStats] = useState<{
    totalUsers: number; adminCount: number; managerCount: number;
    retailCustomerCount: number; wholesaleCustomerCount: number; disabledCount: number;
  } | null>(null);
  const [inventoryCount, setInventoryCount] = useState<number>(0);
  const [lowStockCount, setLowStockCount] = useState<number>(0);
  const [supplierCount, setSupplierCount] = useState<number>(0);
  const [poCount, setPoCount] = useState<number>(0);
  const [batchCount, setBatchCount] = useState<number>(0);
  const [activeBatchCount, setActiveBatchCount] = useState<number>(0);
  const [wholesaleBusinessCount, setWholesaleBusinessCount] = useState<number>(0);

  const fetchOverviewData = useCallback(async () => {
    if (!auth.token) return;
    setLoading(true);
    try {
      const headers = authHeaders(auth.token);
      
      // Auth stats
      try {
        const res = await fetch(`${AUTH_BASE}/admin/stats`, { headers });
        if (res.ok) {
          const body = await res.json();
          if (body.success) setAuthStats(body.data);
        }
      } catch (e) {
        console.warn('Auth stats fetch failed', e);
      }

      // Inventory stats
      try {
        const [itemsRes, lowRes] = await Promise.all([
          fetch('http://localhost:8084/api/v1/inventory/items', { headers }),
          fetch('http://localhost:8084/api/v1/inventory/items/low-stock', { headers }),
        ]);
        if (itemsRes.ok) {
          const body = await itemsRes.json();
          if (body.success && Array.isArray(body.data)) setInventoryCount(body.data.length);
        }
        if (lowRes.ok) {
          const body = await lowRes.json();
          if (body.success && Array.isArray(body.data)) setLowStockCount(body.data.length);
        }
      } catch (e) {
        console.warn('Inventory stats fetch failed', e);
      }

      // Procurement stats
      try {
        const [supRes, poRes] = await Promise.all([
          fetch('http://localhost:8085/api/v1/procurement/suppliers', { headers }),
          fetch('http://localhost:8085/api/v1/procurement/purchase-orders', { headers }),
        ]);
        if (supRes.ok) {
          const body = await supRes.json();
          if (body.success && Array.isArray(body.data)) setSupplierCount(body.data.length);
        }
        if (poRes.ok) {
          const body = await poRes.json();
          if (body.success && Array.isArray(body.data)) setPoCount(body.data.length);
        }
      } catch (e) {
        console.warn('Procurement stats fetch failed', e);
      }

      // Production stats
      try {
        const prodRes = await fetch('http://localhost:8086/api/v1/production/batches', { headers });
        if (prodRes.ok) {
          const body = await prodRes.json();
          if (body.success && Array.isArray(body.data)) {
            setBatchCount(body.data.length);
            const active = body.data.filter((b: any) => b.status === 'IN_PROGRESS' || b.status === 'PLANNED').length;
            setActiveBatchCount(active);
          }
        }
      } catch (e) {
        console.warn('Production stats fetch failed', e);
      }

      // Wholesale Business records
      try {
        const custRes = await fetch('http://localhost:8083/api/v1/customers', { headers });
        if (custRes.ok) {
          const data = await custRes.json();
          if (Array.isArray(data)) setWholesaleBusinessCount(data.length);
        }
      } catch (e) {
        console.warn('Customer records fetch failed', e);
      }
    } finally {
      setLoading(false);
    }
  }, [auth.token]);

  useEffect(() => {
    fetchOverviewData();
  }, [fetchOverviewData]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 space-y-3">
        <Spinner size={6} />
        <p className="text-xs text-slate-400">Loading live operational and system metrics…</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-amber-400" />
            Executive Overview & Operational Metrics
          </h2>
          <p className="text-xs text-slate-400">
            Authoritative real-time aggregation across all active microservices (Sprints 0–5).
          </p>
        </div>
        <button
          onClick={fetchOverviewData}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 rounded-xl text-xs font-semibold transition"
        >
          <RefreshCw className="h-3.5 w-3.5 text-amber-400" /> Refresh Metrics
        </button>
      </div>

      {/* Grid of Real-Data Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* Users Card */}
        <div className="glass-card rounded-2xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Auth & User Accounts</span>
            <span className="p-2 rounded-xl bg-blue-500/10 text-blue-400"><Users className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-white">{authStats?.totalUsers ?? 0}</div>
            <p className="text-xs text-slate-400 mt-1">
              <span className="text-red-400 font-bold">{authStats?.adminCount ?? 0}</span> Admin •{' '}
              <span className="text-blue-400 font-bold">{authStats?.managerCount ?? 0}</span> Managers •{' '}
              <span className="text-emerald-400 font-bold">{authStats?.retailCustomerCount ?? 0}</span> Retail Customers
            </p>
          </div>
          <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80 font-mono">
            Auth Service: auth_schema.users
          </div>
        </div>

        {/* Wholesale Businesses Card */}
        <div className="glass-card rounded-2xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Wholesale Businesses (B2B)</span>
            <span className="p-2 rounded-xl bg-amber-500/10 text-amber-400"><Building2 className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-white">{wholesaleBusinessCount}</div>
            <p className="text-xs text-amber-400/90 mt-1">
              Internal business records (No portal login)
            </p>
          </div>
          <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80 font-mono">
            Ledger Service: customer_schema.customers
          </div>
        </div>

        {/* Inventory Items Card */}
        <div className="glass-card rounded-2xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Inventory & Materials</span>
            <span className="p-2 rounded-xl bg-purple-500/10 text-purple-400"><Package className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-white">{inventoryCount}</div>
            <p className="text-xs text-slate-400 mt-1">
              {lowStockCount > 0 ? (
                <span className="text-amber-400 font-bold flex items-center gap-1">
                  <AlertTriangle className="h-3 w-3 inline" /> {lowStockCount} items below reorder level
                </span>
              ) : (
                <span className="text-emerald-400 font-bold">All stock levels healthy</span>
              )}
            </p>
          </div>
          <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80 font-mono">
            Inventory Service: inventory_schema
          </div>
        </div>

        {/* Procurement Card */}
        <div className="glass-card rounded-2xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Cane Procurement & POs</span>
            <span className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400"><Truck className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-white">{poCount}</div>
            <p className="text-xs text-slate-400 mt-1">
              <span className="text-emerald-400 font-bold">{supplierCount}</span> active cane & packing suppliers
            </p>
          </div>
          <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80 font-mono">
            Procurement Service: procurement_schema
          </div>
        </div>

        {/* Production Card */}
        <div className="glass-card rounded-2xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Mill Production Batches</span>
            <span className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400"><Factory className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-white">{batchCount}</div>
            <p className="text-xs text-slate-400 mt-1">
              <span className="text-indigo-400 font-bold">{activeBatchCount}</span> in-progress / planned batches
            </p>
          </div>
          <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80 font-mono">
            Production Service: production_schema
          </div>
        </div>

        {/* Operating Expenses (Sprint 6 Placeholder) */}
        <div className="glass-card rounded-2xl p-5 border border-slate-800/60 bg-slate-900/30 space-y-3 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Milling Expenses</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">Sprint 6</span>
          </div>
          <div>
            <div className="text-lg font-bold text-slate-400 italic">Not available yet</div>
            <p className="text-xs text-slate-500 mt-1">
              Operating expenses & diesel costing scheduled in Sprint 6.
            </p>
          </div>
          <div className="text-[11px] text-slate-600 pt-2 border-t border-slate-800/60 font-mono">
            Finance Service: finance_schema (Upcoming)
          </div>
        </div>

        {/* Payroll (Sprint 6 Placeholder) */}
        <div className="glass-card rounded-2xl p-5 border border-slate-800/60 bg-slate-900/30 space-y-3 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Mill Payroll & Wages</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">Sprint 6</span>
          </div>
          <div>
            <div className="text-lg font-bold text-slate-400 italic">Not available yet</div>
            <p className="text-xs text-slate-500 mt-1">
              Piece-rate boiling and packaging wages scheduled in Sprint 6.
            </p>
          </div>
          <div className="text-[11px] text-slate-600 pt-2 border-t border-slate-800/60 font-mono">
            Finance Service: finance_schema (Upcoming)
          </div>
        </div>

        {/* Direct Workforce (Sprint 6 Placeholder) */}
        <div className="glass-card rounded-2xl p-5 border border-slate-800/60 bg-slate-900/30 space-y-3 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Mill Workforce</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">Sprint 6</span>
          </div>
          <div>
            <div className="text-lg font-bold text-slate-400 italic">Not available yet</div>
            <p className="text-xs text-slate-500 mt-1">
              Internal employee business records (no login) scheduled in Sprint 6.
            </p>
          </div>
          <div className="text-[11px] text-slate-600 pt-2 border-t border-slate-800/60 font-mono">
            Finance Service: finance_schema (Upcoming)
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Admin User Management ──────────────────────────────────────────────────

function AdminUserManagement({ auth }: { auth: AuthState }) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState({
    email: '', password: '', fullName: '', phone: '',
  });
  const [createLoading, setCreateLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const fetchUsers = useCallback(async () => {
    if (!auth.token) return;
    setLoading(true);
    try {
      const res = await fetch(`${AUTH_BASE}/admin/users`, { headers: authHeaders(auth.token) });
      if (res.ok) {
        const body = await res.json();
        if (body.success) setUsers(body.data);
      }
    } catch {
      setError('Cannot load users list.');
    } finally {
      setLoading(false);
    }
  }, [auth.token]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  async function handleCreateManager(e: React.FormEvent) {
    e.preventDefault();
    if (!auth.token) return;
    setError('');
    setSuccess('');
    setCreateLoading(true);
    try {
      const res = await fetch(`${AUTH_BASE}/admin/managers`, {
        method: 'POST',
        headers: authHeaders(auth.token),
        body: JSON.stringify(createForm),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        setSuccess(`Manager account ${body.data.email} created successfully!`);
        setCreateForm({ email: '', password: '', fullName: '', phone: '' });
        setShowCreateModal(false);
        fetchUsers();
      } else {
        setError(body.error?.message ?? 'Failed to create manager account.');
      }
    } catch {
      setError('Cannot reach Auth Service.');
    } finally {
      setCreateLoading(false);
    }
  }

  async function handleToggleEnabled(userId: string, currentStatus: boolean) {
    if (!auth.token) return;
    try {
      const res = await fetch(`${AUTH_BASE}/admin/users/${userId}/enabled`, {
        method: 'PATCH',
        headers: authHeaders(auth.token),
        body: JSON.stringify({ enabled: !currentStatus }),
      });
      if (res.ok) {
        fetchUsers();
      } else {
        alert('Failed to update user status.');
      }
    } catch {
      alert('Error connecting to Auth Service.');
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Shield className="h-5 w-5 text-amber-400" />
            User & Access Management (3-Role System)
          </h2>
          <p className="text-xs text-slate-400">
            Authoritative login access: ADMIN, MANAGER, and CUSTOMER. Wholesale accounts have portal logins disabled.
          </p>
        </div>
        <button
          id="btn-add-manager"
          onClick={() => { setShowCreateModal(true); setError(''); setSuccess(''); }}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs shadow-md hover:brightness-110 transition"
        >
          <Plus className="h-4 w-4" /> Add Operations Manager
        </button>
      </div>

      {success && (
        <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-xl text-xs flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{success}</span>
        </div>
      )}

      {error && (
        <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-xs flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* User Table */}
      <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 space-y-2">
            <Spinner size={6} />
            <span className="text-xs text-slate-400">Loading user accounts…</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-slate-400 uppercase font-semibold border-b border-slate-800">
                <tr>
                  <th className="px-5 py-3.5">User / Full Name</th>
                  <th className="px-5 py-3.5">Email & Phone</th>
                  <th className="px-5 py-3.5">Account Type</th>
                  <th className="px-5 py-3.5">Assigned Roles</th>
                  <th className="px-5 py-3.5">Login Status</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {users.map(u => {
                  return (
                    <tr key={u.id} className="hover:bg-slate-900/40 transition">
                      <td className="px-5 py-4 font-medium text-white">
                        <div className="font-bold">{u.fullName}</div>
                        {u.businessName && <div className="text-[11px] text-slate-400">{u.businessName}</div>}
                      </td>
                      <td className="px-5 py-4 text-slate-300 font-mono text-[11px]">
                        <div>{u.email}</div>
                        {u.phone && <div className="text-slate-500">{u.phone}</div>}
                      </td>
                      <td className="px-5 py-4">
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          u.customerType === 'INTERNAL' ? 'bg-purple-500/10 text-purple-300 border border-purple-500/20'
                          : u.customerType === 'REGISTERED_WHOLESALE' ? 'bg-amber-500/10 text-amber-300 border border-amber-500/20'
                          : 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                        }`}>
                          {u.customerType}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap gap-1">
                          {u.roles.map(r => (
                            <span key={r} className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${
                              r === 'ADMIN' ? 'bg-red-500/20 text-red-300 border border-red-500/30'
                              : r === 'MANAGER' ? 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                              : 'bg-slate-800 text-slate-300 border border-slate-700'
                            }`}>
                              {r}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <span className={`flex items-center gap-1.5 text-[11px] font-semibold ${
                          u.enabled ? 'text-emerald-400' : 'text-red-400'
                        }`}>
                          <span className={`h-2 w-2 rounded-full ${u.enabled ? 'bg-emerald-400' : 'bg-red-400'}`} />
                          {u.enabled ? 'Enabled' : 'Disabled'}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-right">
                        {u.email !== auth.user?.email && (
                          <button
                            onClick={() => handleToggleEnabled(u.id, u.enabled)}
                            className={`px-3 py-1 rounded-lg text-xs font-semibold border transition ${
                              u.enabled
                                ? 'bg-red-500/10 text-red-400 border-red-500/20 hover:bg-red-500/20'
                                : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 hover:bg-emerald-500/20'
                            }`}
                          >
                            {u.enabled ? 'Disable Login' : 'Enable Login'}
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Manager Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl relative">
            <button
              onClick={() => setShowCreateModal(false)}
              className="absolute top-5 right-5 p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white transition"
            >
              <X className="h-4 w-4" />
            </button>
            <h2 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
              <UserPlus className="h-5 w-5 text-amber-400" />
              Add Operations Manager
            </h2>
            <p className="text-xs text-slate-400 mb-4">
              Operations Managers have full operational access to Inventory, Procurement, Mill Production, and Wholesale Ledgers.
            </p>
            <form onSubmit={handleCreateManager} className="space-y-3 text-xs">
              <div>
                <label className="text-slate-400 font-semibold uppercase">Full Name</label>
                <input
                  id="form-manager-name"
                  type="text"
                  required
                  value={createForm.fullName}
                  onChange={e => setCreateForm(f => ({ ...f, fullName: e.target.value }))}
                  placeholder="Suresh Gowda"
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div>
                <label className="text-slate-400 font-semibold uppercase">Email Address (Login ID)</label>
                <input
                  id="form-manager-email"
                  type="email"
                  required
                  value={createForm.email}
                  onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))}
                  placeholder="manager2@rrjaggery.com"
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div>
                <label className="text-slate-400 font-semibold uppercase">Phone Number</label>
                <input
                  id="form-manager-phone"
                  type="tel"
                  value={createForm.phone}
                  onChange={e => setCreateForm(f => ({ ...f, phone: e.target.value }))}
                  placeholder="+91 98451 23456"
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <div>
                <label className="text-slate-400 font-semibold uppercase">Initial Password</label>
                <input
                  id="form-manager-password"
                  type="password"
                  required
                  minLength={6}
                  value={createForm.password}
                  onChange={e => setCreateForm(f => ({ ...f, password: e.target.value }))}
                  placeholder="Min 6 characters"
                  className="w-full mt-1 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>
              <button
                id="submit-create-manager"
                type="submit"
                disabled={createLoading}
                className="w-full mt-4 py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs shadow-lg shadow-amber-600/20 hover:brightness-110 disabled:opacity-50 flex items-center justify-center gap-2 transition"
              >
                {createLoading ? <><Spinner size={4} /><span>Creating Manager Account…</span></> : <><Check className="h-4 w-4" /><span>Create Manager Account</span></>}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Admin & Manager Operations Portal ────────────────────────────────────────

function AdminDashboard({ auth, onViewInvoice }: { auth: AuthState; onViewInvoice: (orderId: string) => void }) {
  const isAdmin = auth.user?.roles.includes('ADMIN') ?? false;

  const [tab, setTab] = useState<'overview' | 'users' | 'customers' | 'orders' | 'catalogue' | 'operations' | 'production' | 'services' | 'roadmap'>(
    isAdmin ? 'overview' : 'customers'
  );
  const [activeLedgerCustomer, setActiveLedgerCustomer] = useState<Customer | null>(null);
  const [activePaymentCustomer, setActivePaymentCustomer] = useState<Customer | null>(null);
  const [isCreateOfflineOrderOpen, setIsCreateOfflineOrderOpen] = useState(false);
  const [preselectedOfflineCustomer, setPreselectedOfflineCustomer] = useState<Customer | undefined>(undefined);
  const [createdOfflineOrderForInvoice, setCreatedOfflineOrderForInvoice] = useState<OfflineOrder | null>(null);

  function handleOpenLedger(cust: Customer) {
    setActiveLedgerCustomer(cust);
  }

  function handleOpenPayment(cust: Customer) {
    setActivePaymentCustomer(cust);
  }

  function handleOpenNewOrder(cust?: Customer) {
    setPreselectedOfflineCustomer(cust);
    setIsCreateOfflineOrderOpen(true);
  }

  function handleOrderCreated(order: OfflineOrder) {
    setIsCreateOfflineOrderOpen(false);
    setCreatedOfflineOrderForInvoice(order);
  }

  return (
    <div className="space-y-8 animate-fadeIn">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white flex items-center gap-3">
            <LayoutDashboard className="h-7 w-7 text-amber-500" />
            {isAdmin ? 'Admin Master ERP' : 'Operations Manager Portal'}
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Logged in as <span className="text-amber-400 font-semibold">{auth.user?.fullName}</span>
            {isAdmin ? (
              <span className="ml-2 px-2 py-0.5 bg-red-500/10 text-red-400 border border-red-500/20 rounded-full text-[10px] font-bold">ADMIN</span>
            ) : (
              <span className="ml-2 px-2 py-0.5 bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded-full text-[10px] font-bold">MANAGER</span>
            )}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-1 bg-slate-900 p-1 rounded-xl border border-slate-800 text-xs">
          {isAdmin && (
            <button
              id="tab-btn-overview"
              onClick={() => setTab('overview')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'overview' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
            >
              <BarChart3 className="h-3.5 w-3.5 inline mr-1" />Overview
            </button>
          )}
          {isAdmin && (
            <button
              id="tab-btn-users"
              onClick={() => setTab('users')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'users' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
            >
              <Shield className="h-3.5 w-3.5 inline mr-1" />User Access
            </button>
          )}
          <button
            id="tab-btn-customers"
            onClick={() => setTab('customers')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'customers' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            <Users className="h-3.5 w-3.5 inline mr-1" />Wholesale & B2B
          </button>
          <button
            id="tab-btn-admin-orders"
            onClick={() => setTab('orders')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'orders' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            <Receipt className="h-3.5 w-3.5 inline mr-1" />Retail Orders
          </button>
          <button
            id="tab-btn-catalogue"
            onClick={() => setTab('catalogue')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'catalogue' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            <Package className="h-3.5 w-3.5 inline mr-1" />Product Master
          </button>
          <button
            id="tab-btn-operations"
            onClick={() => setTab('operations')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'operations' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            <Truck className="h-3.5 w-3.5 inline mr-1" />Inventory & Procurement
          </button>
          <button
            id="tab-btn-production"
            onClick={() => setTab('production')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'production' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            <Factory className="h-3.5 w-3.5 inline mr-1" />Mill Production
          </button>
          <button
            id="tab-btn-services"
            onClick={() => setTab('services')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'services' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            <Server className="h-3.5 w-3.5 inline mr-1" />Service Mesh
          </button>
          <button
            id="tab-btn-roadmap"
            onClick={() => setTab('roadmap')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'roadmap' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'}`}
          >
            <TrendingUp className="h-3.5 w-3.5 inline mr-1" />Roadmap
          </button>
        </div>
      </div>

      {isAdmin && tab === 'overview' && <AdminOverview auth={auth} />}
      {isAdmin && tab === 'users' && <AdminUserManagement auth={auth} />}
      {tab === 'customers' && (
        <CustomerDirectoryPage
          token={auth.token || ''}
          onOpenLedger={handleOpenLedger}
          onOpenPayment={handleOpenPayment}
          onOpenNewOrder={handleOpenNewOrder}
        />
      )}
      {tab === 'orders' && <AdminOrderManagement auth={auth} onViewInvoice={onViewInvoice} />}
      {tab === 'catalogue' && <AdminCatalogue auth={auth} />}
      {tab === 'operations' && <Sprint4Operations token={auth.token || ''} userName={auth.user?.fullName || (isAdmin ? 'ADMIN' : 'MANAGER')} />}
      {tab === 'production' && <Sprint5Production token={auth.token || ''} userName={auth.user?.fullName || (isAdmin ? 'ADMIN' : 'MANAGER')} />}
      {tab === 'services' && <ServiceHealthMonitor />}
      {tab === 'roadmap' && <SprintRoadmap />}

      {/* Customer Ledger Modal */}
      {activeLedgerCustomer && (
        <LedgerStatementModal
          token={auth.token || ''}
          customer={activeLedgerCustomer}
          onClose={() => setActiveLedgerCustomer(null)}
          onRecordPayment={() => {
            const c = activeLedgerCustomer;
            setActiveLedgerCustomer(null);
            setActivePaymentCustomer(c);
          }}
        />
      )}

      {/* Customer Payment Modal */}
      {activePaymentCustomer && (
        <RecordPaymentModal
          token={auth.token || ''}
          customer={activePaymentCustomer}
          onClose={() => setActivePaymentCustomer(null)}
          onPaymentRecorded={() => {
            setActivePaymentCustomer(null);
            setTab('orders');
            setTimeout(() => setTab('customers'), 50);
          }}
        />
      )}

      {/* Create Offline Order Modal */}
      {isCreateOfflineOrderOpen && (
        <CreateOfflineOrderModal
          token={auth.token || ''}
          preselectedCustomer={preselectedOfflineCustomer}
          onClose={() => setIsCreateOfflineOrderOpen(false)}
          onOrderCreated={handleOrderCreated}
        />
      )}

      {/* Offline Order Tax Invoice Modal */}
      {createdOfflineOrderForInvoice && (
        <OfflineInvoiceModal
          order={createdOfflineOrderForInvoice}
          onClose={() => setCreatedOfflineOrderForInvoice(null)}
        />
      )}
    </div>
  );
}

// ─── Main App ──────────────────────────────────────────────────────────────────

export function App() {
  const [auth, setAuth] = useState<AuthState>(loadAuthState);
  const [view, setView] = useState<'storefront' | 'orders' | 'admin'>('storefront');
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [confirmedOrder, setConfirmedOrder] = useState<Order | null>(null);
  const [activeInvoiceOrderId, setActiveInvoiceOrderId] = useState<string | null>(null);
  const [cart, setCart] = useState<Cart | null>(null);

  const isAdmin = auth.user?.roles.includes('ADMIN') ?? false;
  const isManager = auth.user?.roles.includes('MANAGER') ?? false;
  const isInternal = isAdmin || isManager;

  // Fetch user cart
  const fetchCart = useCallback(async () => {
    if (!auth.token) {
      setCart(null);
      return;
    }
    try {
      const res = await fetch(`${COMMERCE_BASE}/cart`, { headers: authHeaders(auth.token) });
      const body = await res.json();
      if (body.success) {
        setCart(body.data);
      }
    } catch {
      console.warn('Cart fetch failed');
    }
  }, [auth.token]);

  useEffect(() => { fetchCart(); }, [fetchCart]);

  async function handleAddToCart(productId: string, quantity: number) {
    if (!auth.token) {
      setIsAuthModalOpen(true);
      return;
    }
    try {
      const res = await fetch(`${COMMERCE_BASE}/cart/items`, {
        method: 'POST',
        headers: authHeaders(auth.token),
        body: JSON.stringify({ productId, quantity }),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        fetchCart();
        setIsCartOpen(true);
      } else {
        alert(body.error?.message ?? 'Failed to add item to cart.');
      }
    } catch {
      alert('Error adding item to cart.');
    }
  }

  async function handleUpdateQty(itemId: string, quantity: number) {
    if (!auth.token) return;
    try {
      const res = await fetch(`${COMMERCE_BASE}/cart/items/${itemId}`, {
        method: 'PUT',
        headers: authHeaders(auth.token),
        body: JSON.stringify({ quantity }),
      });
      if (res.ok) {
        fetchCart();
      }
    } catch {
      console.warn('Cart update qty failed');
    }
  }

  async function handleRemoveCartItem(itemId: string) {
    if (!auth.token) return;
    try {
      const res = await fetch(`${COMMERCE_BASE}/cart/items/${itemId}`, {
        method: 'DELETE',
        headers: authHeaders(auth.token),
      });
      if (res.ok) {
        fetchCart();
      }
    } catch {
      console.warn('Cart remove item failed');
    }
  }

  function handleLogin(token: string, user: User) {
    setAuth({ token, user });
    if (user.roles.includes('ADMIN') || user.roles.includes('MANAGER')) {
      setView('admin');
    } else {
      setView('storefront');
    }
  }

  function handleLogout() {
    clearAuthState();
    setAuth({ token: null, user: null });
    setCart(null);
    setView('storefront');
  }

  function handleOrderPlaced(order: Order) {
    setIsCheckoutOpen(false);
    setIsCartOpen(false);
    setCart(null);
    setConfirmedOrder(order);
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Brand */}
          <div className="flex items-center gap-3 cursor-pointer" onClick={() => setView('storefront')}>
            <div className="h-10 w-10 rounded-xl jaggery-gradient flex items-center justify-center shadow-lg shadow-amber-900/40">
              <Flame className="h-6 w-6 text-amber-100" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-xl tracking-tight text-white">RR JAGGERY</span>
                <span className="text-xs px-2 py-0.5 rounded-full font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  TRADERS
                </span>
              </div>
              <p className="text-[10px] text-slate-400">Pure Organic Jaggery • Manufacturing • B2B Ledger</p>
            </div>
          </div>

          {/* Navigation & User controls */}
          <div className="flex items-center gap-3">
            {/* View Switcher for Logged In users */}
            <div className="flex items-center gap-1 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
              <button
                id="nav-storefront"
                onClick={() => setView('storefront')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${view === 'storefront' ? 'bg-amber-500 text-slate-950 shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
              >
                <Store className="h-3.5 w-3.5" /> <span>Storefront</span>
              </button>

              {auth.token && (
                <button
                  id="nav-orders"
                  onClick={() => setView('orders')}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${view === 'orders' ? 'bg-amber-500 text-slate-950 shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
                >
                  <Receipt className="h-3.5 w-3.5" /> <span>My Orders</span>
                </button>
              )}

              {isInternal && (
                <button
                  id="nav-admin"
                  onClick={() => setView('admin')}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${view === 'admin' ? 'bg-amber-500 text-slate-950 shadow-md' : 'text-slate-400 hover:text-slate-200'}`}
                >
                  <Building2 className="h-3.5 w-3.5" /> <span>{isAdmin ? 'Admin Portal' : 'Operations Portal'}</span>
                </button>
              )}
            </div>

            {/* Cart Button */}
            {auth.token && (
              <button
                id="header-cart-btn"
                onClick={() => setIsCartOpen(true)}
                className="relative p-2.5 rounded-xl bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-300 hover:text-white transition"
                title="Shopping Cart"
              >
                <ShoppingCart className="h-4 w-4 text-amber-400" />
                {cart && cart.totalItems > 0 && (
                  <span id="cart-badge-count" className="absolute -top-1.5 -right-1.5 h-5 w-5 rounded-full bg-amber-500 text-slate-950 font-black text-[10px] flex items-center justify-center shadow-md animate-pulse">
                    {cart.totalItems}
                  </span>
                )}
              </button>
            )}

            {/* Auth Actions */}
            {!auth.token ? (
              <button
                id="header-signin-btn"
                onClick={() => setIsAuthModalOpen(true)}
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl jaggery-gradient text-slate-950 font-bold text-xs shadow-md shadow-amber-600/20 hover:brightness-110 transition"
              >
                <LogIn className="h-3.5 w-3.5" /> <span>Sign In</span>
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <div className="hidden sm:block text-right">
                  <div id="user-display-name" className="text-xs font-semibold text-white">{auth.user?.fullName}</div>
                  <div className="text-[10px] text-amber-400 font-medium">
                    {isAdmin ? 'Administrator' : isManager ? 'Operations Manager' : 'Customer'}
                  </div>
                </div>
                <button
                  id="logout-btn"
                  onClick={handleLogout}
                  title="Sign out"
                  className="p-2 rounded-lg bg-slate-900 border border-slate-800 hover:bg-slate-800 transition text-slate-400 hover:text-white"
                >
                  <LogOut className="h-4 w-4" />
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main Content View Switcher */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {view === 'storefront' ? (
          <Storefront
            auth={auth}
            onOpenAuth={() => setIsAuthModalOpen(true)}
            onAddToCart={handleAddToCart}
          />
        ) : view === 'orders' ? (
          <OrderHistoryView
            auth={auth}
            onViewInvoice={id => setActiveInvoiceOrderId(id)}
            onBackToStore={() => setView('storefront')}
          />
        ) : isInternal ? (
          <AdminDashboard
            auth={auth}
            onViewInvoice={id => setActiveInvoiceOrderId(id)}
          />
        ) : (
          <Storefront
            auth={auth}
            onOpenAuth={() => setIsAuthModalOpen(true)}
            onAddToCart={handleAddToCart}
          />
        )}
      </main>

      {/* Cart Drawer */}
      <CartDrawer
        isOpen={isCartOpen}
        onClose={() => setIsCartOpen(false)}
        cart={cart}
        onUpdateQty={handleUpdateQty}
        onRemoveItem={handleRemoveCartItem}
        onProceedToCheckout={() => {
          setIsCartOpen(false);
          setIsCheckoutOpen(true);
        }}
      />

      {/* Checkout Modal */}
      <CheckoutModal
        isOpen={isCheckoutOpen}
        onClose={() => setIsCheckoutOpen(false)}
        cart={cart}
        auth={auth}
        onOrderPlaced={handleOrderPlaced}
      />

      {/* Order Confirmation Modal */}
      <OrderConfirmationModal
        order={confirmedOrder}
        onClose={() => setConfirmedOrder(null)}
        onViewOrders={() => {
          setConfirmedOrder(null);
          setView('orders');
        }}
        onViewInvoice={id => {
          setConfirmedOrder(null);
          setActiveInvoiceOrderId(id);
        }}
      />

      {/* Tax Invoice Modal */}
      <InvoiceModal
        orderId={activeInvoiceOrderId}
        auth={auth}
        onClose={() => setActiveInvoiceOrderId(null)}
      />

      {/* Auth Modal */}
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        onLogin={handleLogin}
      />

      {/* Footer */}
      <footer className="border-t border-slate-800 bg-slate-900/40 py-6 text-center text-xs text-slate-500">
        <p>RR Jaggery Traders • Unified Commercial & Mill ERP Platform • Access Model Revision (Pre-Sprint 6)</p>
      </footer>
    </div>
  );
}

export default App;
