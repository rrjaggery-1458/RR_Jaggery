import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Store, LayoutDashboard, LogIn, UserPlus, LogOut,
  ArrowRight, RefreshCw, Activity, Server,
  CheckCircle2, XCircle, Package, Tag, Search,
  Building2, ChevronRight, Eye, EyeOff, AlertCircle,
  TrendingUp, Loader2, Plus, Edit2, X, Check,
  Sparkles, ShoppingCart, Trash2, CreditCard,
  FileText, Truck, Printer, Receipt, ArrowLeft, Users, Factory,
  Shield, BarChart3, AlertTriangle, Star, MapPin, Phone, Mail,
  MessageSquare, Send, Leaf, Award, Clock, Info, User, Upload,
  ExternalLink, ShieldCheck
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
import { ManagerPortal } from './ManagerPortal';
import { TermsPage, PrivacyPage, RefundPolicyPage, ShippingPolicyPage } from './components/LegalPages';
import { ProfilePage } from './components/ProfilePage';

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
  unit?: string;
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
  packagingFee?: number;
  totalAmount: number;
}

interface Address {
  recipientName: string;
  phone: string;
  apartmentName: string;
  streetAddress: string;
  city: string;
  state: string;
  postalCode: string;
  nearestLandmark: string;
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
  packagingFee?: number;
  discountAmount: number;
  totalAmount: number;
  status: 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'DISPATCHED' | 'DELIVERED' | 'CANCELLED';
  paymentStatus: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
  paymentMethod: 'TEST_PAYMENT' | 'CASH_ON_DELIVERY' | 'UPI' | 'BANK_TRANSFER' | 'RAZORPAY';
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
  sellerGstin?: string;
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
  packagingFee?: number;
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

interface ProductReview {
  id: string;
  productId: string;
  productName: string;
  userId?: string;
  customerName: string;
  rating: number;
  comment: string;
  status: string;
  createdAt: string;
}

type StorefrontPage = 'shop' | 'about' | 'contact' | 'reviews' | 'terms' | 'privacy' | 'refund' | 'shipping';

// ─── API Constants ────────────────────────────────────────────────────────────

const getApiBaseUrl = (port: number, servicePath: string) => {
  if (import.meta.env.PROD || (typeof window !== 'undefined' && (window.location.port === '' || window.location.port === '80' || window.location.port === '443'))) {
    return `/api/v1/${servicePath}`;
  }
  return `http://localhost:${port}/api/v1/${servicePath}`;
};

const AUTH_BASE = getApiBaseUrl(8081, 'auth');
const COMMERCE_BASE = getApiBaseUrl(8082, 'commerce');
const CONTACT_BASE = getApiBaseUrl(8083, 'contact');

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;

const INITIAL_SERVICES: ServiceHealth[] = [
  { name: 'Auth Service', code: 'auth', port: 8081, endpoint: `${AUTH_BASE}/health`, schema: 'auth_schema', role: 'Security & Identity', status: 'CONNECTING' },
  { name: 'Commerce Service', code: 'commerce', port: 8082, endpoint: `${COMMERCE_BASE}/health`, schema: 'commerce_schema', role: 'Catalog, Cart & Orders', status: 'CONNECTING' },
  { name: 'Customer & Ledger', code: 'customer-ledger', port: 8083, endpoint: `${getApiBaseUrl(8083, 'customers')}/health`, schema: 'customer_schema', role: 'Wholesale & Ledgers', status: 'CONNECTING' },
  { name: 'Inventory Service', code: 'inventory', port: 8084, endpoint: `${getApiBaseUrl(8084, 'inventory')}/health`, schema: 'inventory_schema', role: 'Stock & Movements', status: 'CONNECTING' },
  { name: 'Procurement', code: 'procurement', port: 8084, endpoint: `${getApiBaseUrl(8084, 'procurement')}/health`, schema: 'procurement_schema', role: 'Cane Inward & POs', status: 'CONNECTING' },
  { name: 'Production', code: 'production', port: 8086, endpoint: `${getApiBaseUrl(8086, 'production')}/health`, schema: 'production_schema', role: 'Batch Processing', status: 'CONNECTING' },
  { name: 'Finance Service', code: 'finance', port: 8083, endpoint: `${getApiBaseUrl(8083, 'finance')}/health`, schema: 'finance_schema', role: 'Costing & Payroll', status: 'CONNECTING' },
  { name: 'Notifications', code: 'notifications', port: 8083, endpoint: `${getApiBaseUrl(8083, 'notifications')}/health`, schema: 'notification_schema', role: 'Alerts & Messages', status: 'CONNECTING' },
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
const LAST_ACTIVITY_KEY = 'rr_last_activity';
const AUTH_INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000;

function loadAuthState(): AuthState {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    const userStr = localStorage.getItem(USER_KEY);
    const lastActivity = Number(localStorage.getItem(LAST_ACTIVITY_KEY));
    const elapsed = Date.now() - lastActivity;
    if (token && userStr && Number.isFinite(lastActivity)
      && lastActivity > 0 && elapsed >= 0 && elapsed < AUTH_INACTIVITY_TIMEOUT_MS) {
      return { token, user: JSON.parse(userStr) };
    }
  } catch { }
  clearAuthState();
  return { token: null, user: null };
}

function saveAuthState(token: string, user: User) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()));
}

function clearAuthState() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(LAST_ACTIVITY_KEY);
}

// ─── UI Helper Components ─────────────────────────────────────────────────────

function Spinner({ size = 4 }: { size?: number }) {
  return <Loader2 className={`h-${size} w-${size} animate-spin text-emerald-700`} />;
}

function StatusBadge({ status }: { status: 'ONLINE' | 'STANDBY' | 'CONNECTING' }) {
  const cfg = {
    ONLINE: 'bg-emerald-500/10 text-emerald-700 border-emerald-500/20',
    STANDBY: 'bg-gray-100 text-gray-500 border-gray-300',
    CONNECTING: 'bg-emerald-700/10 text-emerald-700 border-emerald-200',
  }[status];
  return (
    <span className={`flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full border ${cfg}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${status === 'ONLINE' ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`} />
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
    email: '', password: '', fullName: '', phone: '',
  });
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Registration OTP state
  const [registrationOtpSent, setRegistrationOtpSent] = useState(false);
  const [registrationOtp, setRegistrationOtp] = useState('');
  const [registrationChallengeId, setRegistrationChallengeId] = useState('');
  const [registrationEmailStatus, setRegistrationEmailStatus] = useState<'PENDING' | 'SENT' | 'FAILED'>('PENDING');

  // Forgot Password state
  const [isForgotPassword, setIsForgotPassword] = useState(false);
  const [forgotStep, setForgotStep] = useState<'email' | 'otp' | 'newPassword' | 'success'>('email');
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotChallengeToken, setForgotChallengeToken] = useState('');
  const [forgotOtp, setForgotOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPw, setShowNewPw] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setTab('login');
      setError('');
      setSuccessMsg('');
      setIsForgotPassword(false);
      setForgotStep('email');
      setForgotEmail('');
      setForgotChallengeToken('');
      setForgotOtp('');
      setNewPassword('');
      setConfirmPassword('');
      setRegistrationOtpSent(false);
      setRegistrationOtp('');
      setRegistrationChallengeId('');
      setRegistrationEmailStatus('PENDING');
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen || !registrationOtpSent || !registrationChallengeId) return;

    let cancelled = false;
    let timer: number | undefined;
    const checkDelivery = async () => {
      try {
        const res = await fetch(
          `${AUTH_BASE}/register/status?challengeId=${encodeURIComponent(registrationChallengeId)}`,
        );
        const body = await res.json();
        if (!res.ok || !body.success) {
          throw new Error(body.error?.message ?? 'Could not check verification email status.');
        }

        const status = body.data?.status;
        if (status === 'SENT' || status === 'FAILED') {
          if (!cancelled) setRegistrationEmailStatus(status);
          return;
        }
        if (!cancelled) timer = window.setTimeout(checkDelivery, 1000);
      } catch {
        if (!cancelled) {
          setError('Could not check email delivery status. The code may still arrive; try again shortly.');
        }
      }
    };

    checkDelivery();
    return () => {
      cancelled = true;
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [isOpen, registrationOtpSent, registrationChallengeId]);

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
      const res = await fetch(`${AUTH_BASE}${registrationOtpSent ? '/register/verify' : '/register'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(registrationOtpSent
          ? { email: regForm.email, challengeId: registrationChallengeId, otp: registrationOtp }
          : regForm),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        if (registrationOtpSent) {
          const { token, user } = body.data;
          saveAuthState(token, user);
          onLogin(token, user);
          onClose();
        } else {
          if (!body.data?.challengeId) throw new Error('Registration verification could not be started.');
          setRegistrationChallengeId(body.data.challengeId);
          setRegistrationEmailStatus('PENDING');
          setRegistrationOtpSent(true);
        }
      } else {
        setError(body.error?.message ?? 'Registration failed. Check input details.');
      }
    } catch (error) {
      setError(error instanceof Error && error.message !== 'Failed to fetch'
        ? error.message
        : 'Cannot reach Auth Service. Please ensure backend services are running.');
    } finally {
      setLoading(false);
    }
  }

  // ── Forgot Password Handlers ──
  async function handleRequestForgotOtp(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    const emailToVerify = forgotEmail.trim();
    if (!emailToVerify) {
      setError('Please enter your email address.');
      return;
    }
    setLoading(true);
    try {
      const res = await fetch(`${AUTH_BASE}/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: emailToVerify }),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        setForgotChallengeToken(body.data?.challengeToken);
        setSuccessMsg(`A 6-digit OTP has been sent to ${emailToVerify}. Please check your inbox.`);
        setForgotStep('otp');
      } else {
        setError(body.error?.message ?? 'This email is not registered with RR Jaggery.');
      }
    } catch {
      setError('Cannot reach Auth Service. Please ensure backend services are running.');
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifyForgotOtp(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    const otpCode = forgotOtp.trim();
    if (!otpCode || otpCode.length !== 6) {
      setError('Please enter the 6-digit OTP code.');
      return;
    }
    setLoading(true);
    try {
      const res = await fetch(`${AUTH_BASE}/forgot-password/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          challengeToken: forgotChallengeToken,
          otp: otpCode,
        }),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        setSuccessMsg('OTP verified successfully! Now create your new password.');
        setForgotStep('newPassword');
      } else {
        setError(body.error?.message ?? 'Invalid OTP code. Please check your email.');
      }
    } catch {
      setError('Cannot reach Auth Service. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  async function handleResetPasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match. Please re-enter.');
      return;
    }
    setLoading(true);
    try {
      const res = await fetch(`${AUTH_BASE}/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          challengeToken: forgotChallengeToken,
          otp: forgotOtp.trim(),
          newPassword: newPassword,
        }),
      });
      const body = await res.json();
      if (res.ok && body.success) {
        setForgotStep('success');
        setLoginEmail(forgotEmail);
      } else {
        setError(body.error?.message ?? 'Failed to reset password. Please try again.');
      }
    } catch {
      setError('Cannot reach Auth Service. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-emerald-50/60 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white border border-gray-200 rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl relative">
        <button
          id="close-auth-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-gray-100 text-gray-500 hover:text-gray-900 hover:bg-emerald-100 transition"
        >
          <X className="h-4 w-4" />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <img src="/rr-logo.png" className="h-10 w-10 rounded-full object-cover shadow-md" alt="RR Jaggery Logo" />
          <div>
            <div className="font-extrabold text-lg text-gray-900">RR JAGGERY TRADERS</div>
            <div className="text-[10px] text-gray-500">Authentication & B2B Portal Access</div>
          </div>
        </div>

        {/* ── FORGOT PASSWORD FLOW ── */}
        {isForgotPassword ? (
          <div>
            <button
              type="button"
              onClick={() => {
                setIsForgotPassword(false);
                setError('');
                setSuccessMsg('');
                setForgotStep('email');
              }}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-gray-600 hover:text-emerald-700 mb-4 transition"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Back to Sign In</span>
            </button>

            <div className="mb-5">
              <h3 className="text-lg font-bold text-gray-900">Reset Your Password</h3>
              <p className="text-xs text-gray-500 mt-0.5">
                {forgotStep === 'email' && 'Enter your registered email address. We will verify your account and send a 6-digit OTP.'}
                {forgotStep === 'otp' && 'Enter the 6-digit verification code sent to your email.'}
                {forgotStep === 'newPassword' && 'Create a new secure password for your account.'}
                {forgotStep === 'success' && 'Your password has been reset successfully.'}
              </p>
            </div>

            {error && (
              <div id="forgot-error-message" className="mb-4 flex items-center gap-2 text-xs text-red-600 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2.5">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {successMsg && forgotStep !== 'success' && (
              <div className="mb-4 flex items-center gap-2 text-xs text-emerald-800 bg-emerald-50 border border-emerald-200 rounded-xl px-3 py-2.5">
                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
                <span>{successMsg}</span>
              </div>
            )}

            {forgotStep === 'email' && (
              <form onSubmit={handleRequestForgotOtp} className="space-y-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Registered Email</label>
                  <input
                    id="forgot-email-input"
                    type="email"
                    required
                    value={forgotEmail}
                    onChange={e => setForgotEmail(e.target.value)}
                    placeholder="Your email"
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                  />
                </div>
                <button
                  id="forgot-submit-email"
                  type="submit"
                  disabled={loading}
                  className="w-full py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-sm shadow-lg shadow-emerald-700/20 hover:brightness-105 disabled:opacity-50 flex items-center justify-center gap-2 transition"
                >
                  {loading ? <><Spinner size={4} /><span>Checking & Sending OTP…</span></> : <><Mail className="h-4 w-4" /><span>Send Reset Code</span></>}
                </button>
              </form>
            )}

            {forgotStep === 'otp' && (
              <form onSubmit={handleVerifyForgotOtp} className="space-y-4">
                <div className="p-3 rounded-xl bg-amber-50 border border-amber-200/60 text-xs text-amber-900 flex items-start gap-2">
                  <Mail className="h-4 w-4 text-amber-700 shrink-0 mt-0.5" />
                  <div>
                    We sent a branded OTP email to <strong className="font-semibold">{forgotEmail}</strong>. Please check your inbox or spam folder.
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">6-Digit Verification Code</label>
                  <input
                    id="forgot-otp-input"
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    required
                    value={forgotOtp}
                    onChange={e => setForgotOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    placeholder="Your 6-digit code"
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-3 text-center text-xl font-mono font-bold tracking-widest text-emerald-900 placeholder-gray-300 focus:outline-none focus:border-emerald-600 transition"
                  />
                </div>

                <div className="flex items-center justify-between text-xs pt-1">
                  <button
                    type="button"
                    onClick={() => {
                      setForgotStep('email');
                      setError('');
                      setSuccessMsg('');
                    }}
                    className="text-gray-500 hover:text-gray-800 underline transition"
                  >
                    Change Email
                  </button>
                  <button
                    type="button"
                    disabled={loading}
                    onClick={handleRequestForgotOtp}
                    className="text-emerald-700 font-semibold hover:underline transition"
                  >
                    Resend Code
                  </button>
                </div>

                <button
                  id="forgot-submit-otp"
                  type="submit"
                  disabled={loading || forgotOtp.length !== 6}
                  className="w-full py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-sm shadow-lg shadow-emerald-700/20 hover:brightness-105 disabled:opacity-50 flex items-center justify-center gap-2 transition"
                >
                  {loading ? <><Spinner size={4} /><span>Verifying Code…</span></> : <><Check className="h-4 w-4" /><span>Verify OTP</span></>}
                </button>
              </form>
            )}

            {forgotStep === 'newPassword' && (
              <form onSubmit={handleResetPasswordSubmit} className="space-y-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">New Password</label>
                  <div className="relative">
                    <input
                      id="forgot-new-password"
                      type={showNewPw ? 'text' : 'password'}
                      required
                      minLength={8}
                      value={newPassword}
                      onChange={e => setNewPassword(e.target.value)}
                      placeholder="Your new password"
                      className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 pr-10 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                    />
                    <button type="button" onClick={() => setShowNewPw(v => !v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-800">
                      {showNewPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Confirm New Password</label>
                  <input
                    id="forgot-confirm-password"
                    type="password"
                    required
                    minLength={8}
                    value={confirmPassword}
                    onChange={e => setConfirmPassword(e.target.value)}
                    placeholder="Confirm your password"
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                  />
                </div>

                <button
                  id="forgot-submit-reset"
                  type="submit"
                  disabled={loading}
                  className="w-full py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-sm shadow-lg shadow-emerald-700/20 hover:brightness-105 disabled:opacity-50 flex items-center justify-center gap-2 transition"
                >
                  {loading ? <><Spinner size={4} /><span>Updating Password…</span></> : <><Sparkles className="h-4 w-4" /><span>Update Password</span></>}
                </button>
              </form>
            )}

            {forgotStep === 'success' && (
              <div className="text-center py-4 space-y-4">
                <div className="h-16 w-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto shadow-inner">
                  <CheckCircle2 className="h-9 w-9 text-emerald-600" />
                </div>
                <div>
                  <h4 className="text-base font-bold text-gray-900">Password Reset Successful!</h4>
                  <p className="text-xs text-gray-600 mt-1 max-w-xs mx-auto">
                    Your password has been securely updated. You can now log into your account using your new credentials.
                  </p>
                </div>
                <button
                  type="button"
                  id="forgot-success-signin"
                  onClick={() => {
                    setIsForgotPassword(false);
                    setTab('login');
                    setForgotStep('email');
                    setError('');
                    setSuccessMsg('');
                  }}
                  className="w-full py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-sm shadow-md transition"
                >
                  Sign In Now
                </button>
              </div>
            )}
          </div>
        ) : (
          /* ── REGULAR SIGN IN / REGISTER TABS ── */
          <>
            <div className="flex bg-white p-1 rounded-xl border border-gray-200 mb-6">
              <button
                id="tab-login"
                onClick={() => { setTab('login'); setError(''); }}
                className={`flex-1 py-2 rounded-lg text-xs font-semibold transition ${tab === 'login' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
              >
                Sign In
              </button>
              <button
                id="tab-register"
                onClick={() => { setTab('register'); setError(''); }}
                className={`flex-1 py-2 rounded-lg text-xs font-semibold transition ${tab === 'register' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
              >
                Register
              </button>
            </div>

            {error && (
              <div id="auth-error-message" className="mb-4 flex items-center gap-2 text-xs text-red-600 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2.5">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {tab === 'login' ? (
              <form onSubmit={handleLoginSubmit} className="space-y-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Email Address</label>
                  <input
                    id="login-email"
                    type="email"
                    required
                    value={loginEmail}
                    onChange={e => setLoginEmail(e.target.value)}
                    placeholder="Your email"
                    className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                  />
                </div>
                <div className="space-y-1">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Password</label>
                    <button
                      type="button"
                      id="link-forgot-password"
                      onClick={() => {
                        setIsForgotPassword(true);
                        setForgotStep('email');
                        setForgotEmail(loginEmail || '');
                        setError('');
                        setSuccessMsg('');
                      }}
                      className="text-xs font-semibold text-emerald-700 hover:text-emerald-800 hover:underline transition"
                    >
                      Forgot Password?
                    </button>
                  </div>
                  <div className="relative">
                    <input
                      id="login-password"
                      type={showPw ? 'text' : 'password'}
                      required
                      value={loginPassword}
                      onChange={e => setLoginPassword(e.target.value)}
                      placeholder="Your password"
                      className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2.5 pr-10 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                    />
                    <button type="button" onClick={() => setShowPw(v => !v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-800">
                      {showPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <button
                  id="login-submit"
                  type="submit"
                  disabled={loading}
                  className="w-full py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-sm shadow-lg shadow-amber-600/20 hover:brightness-105 disabled:opacity-50 flex items-center justify-center gap-2 transition"
                >
                  {loading ? <><Spinner size={4} /><span>Signing In…</span></> : <><LogIn className="h-4 w-4" /><span>Sign In</span></>}
                </button>
              </form>
            ) : (
              <form onSubmit={handleRegisterSubmit} className="space-y-3">
                {!registrationOtpSent ? (
                  <>
                    <div className="space-y-1">
                      <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Full Name</label>
                      <input
                        id="reg-fullname"
                        type="text"
                        required
                        value={regForm.fullName}
                        onChange={e => setRegForm(f => ({ ...f, fullName: e.target.value }))}
                        placeholder="Your name"
                        className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Email Address</label>
                      <input
                        id="reg-email"
                        type="email"
                        required
                        value={regForm.email}
                        onChange={e => setRegForm(f => ({ ...f, email: e.target.value }))}
                        placeholder="Your email"
                        className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Phone (Optional)</label>
                      <input
                        id="reg-phone"
                        type="tel"
                        value={regForm.phone}
                        onChange={e => setRegForm(f => ({ ...f, phone: e.target.value }))}
                        placeholder="Your phone number"
                        className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Password</label>
                      <input
                        id="reg-password"
                        type="password"
                        required
                        minLength={6}
                        value={regForm.password}
                        onChange={e => setRegForm(f => ({ ...f, password: e.target.value }))}
                        placeholder="Your password"
                        className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                      />
                    </div>
                    <p className="text-[11px] text-gray-500">
                      All new accounts are retail customer accounts. Verify your email to finish creating your account.
                    </p>
                  </>
                ) : (
                  <>
                    <div className={`rounded-xl border p-3 text-xs ${registrationEmailStatus === 'FAILED'
                      ? 'border-red-200 bg-red-50 text-red-800'
                      : 'border-emerald-200 bg-emerald-50 text-emerald-800'
                      }`}>
                      {registrationEmailStatus === 'PENDING' && <>Sending a verification code to <strong>{regForm.email}</strong>…</>}
                      {registrationEmailStatus === 'SENT' && <>A verification code was sent to <strong>{regForm.email}</strong>.</>}
                      {registrationEmailStatus === 'FAILED' && <>Could not send a verification code to <strong>{regForm.email}</strong>. Edit your details and try again.</>}
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Email Verification Code</label>
                      <input
                        id="register-email-otp"
                        type="text"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        required
                        value={registrationOtp}
                        onChange={e => setRegistrationOtp(e.target.value.replace(/\D/g, '').slice(0, 8))}
                        placeholder="Your verification code"
                        className="w-full bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-emerald-600 transition"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setRegistrationOtpSent(false);
                        setRegistrationOtp('');
                        setRegistrationChallengeId('');
                        setError('');
                      }}
                      className="text-xs text-emerald-700 font-semibold"
                    >
                      Edit registration details
                    </button>
                  </>
                )}
                <button
                  id="register-submit"
                  type="submit"
                  disabled={loading || (registrationOtpSent && !registrationOtp)}
                  className="w-full mt-2 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-sm shadow-lg shadow-amber-600/20 hover:brightness-105 disabled:opacity-50 flex items-center justify-center gap-2 transition"
                >
                  {loading
                    ? <><Spinner size={4} /><span>{registrationOtpSent ? 'Verifying email…' : 'Sending verification code…'}</span></>
                    : <><UserPlus className="h-4 w-4" /><span>{registrationOtpSent ? 'Verify Email & Create Account' : 'Send Email Verification Code'}</span></>}
                </button>
              </form>
            )}
          </>
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
      <div className="absolute inset-0 bg-emerald-50/60 backdrop-blur-sm transition-opacity" onClick={onClose} />
      <div className="fixed inset-y-0 right-0 max-w-full flex pl-0 sm:pl-10">
        <div className="w-screen max-w-md bg-white border-l border-gray-200 shadow-2xl flex flex-col justify-between">
          {/* Header */}
          <div className="p-6 border-b border-gray-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShoppingCart className="h-5 w-5 text-emerald-700" />
              <h2 className="text-lg font-bold text-gray-900">Your Shopping Cart</h2>
              <span className="text-xs bg-emerald-700/10 text-emerald-700 border border-emerald-200 px-2 py-0.5 rounded-full font-bold">
                {cart?.totalItems ?? 0} items
              </span>
            </div>
            <button id="close-cart-drawer" onClick={onClose} className="p-1.5 bg-gray-100 hover:bg-emerald-100 text-gray-500 hover:text-gray-900 rounded-xl transition">
              <X className="h-4 w-4" />
            </button>
          </div>

          {/* Cart Items List */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {!cart || cart.items.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 space-y-3 text-center">
                <ShoppingCart className="h-12 w-12 text-gray-600" />
                <p className="text-sm font-semibold text-gray-700">Your cart is empty</p>
                <p className="text-xs text-gray-500">Browse our traditional jaggery products and add them to your cart.</p>
              </div>
            ) : (
              cart.items.map(item => (
                <div key={item.id} id={`cart-item-${item.sku}`} className="bg-white p-4 rounded-2xl border border-gray-200 flex flex-col justify-between space-y-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-emerald-700/10 text-emerald-800 border border-emerald-200">
                        {gradeLabel(item.grade)}
                      </span>
                      <h4 className="font-bold text-sm text-gray-900 mt-1">{item.productName}</h4>
                      <p className="text-[11px] text-gray-500 font-mono">SKU: {item.sku} • {item.unitWeightKg}kg {packageLabel(item.packageType)}</p>
                    </div>
                    <button
                      id={`remove-cart-item-${item.sku}`}
                      onClick={() => onRemoveItem(item.id)}
                      className="p-1 text-gray-500 hover:text-red-600 transition"
                      title="Remove item"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-gray-200">
                    <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-xl px-2 py-1">
                      <button
                        id={`dec-qty-${item.sku}`}
                        onClick={() => onUpdateQty(item.id, item.quantity - 1)}
                        className="h-6 w-6 flex items-center justify-center text-gray-700 hover:text-gray-900 font-bold"
                      >
                        -
                      </button>
                      <span className="text-xs font-mono font-bold text-gray-900 px-1.5">{item.quantity}</span>
                      <button
                        id={`inc-qty-${item.sku}`}
                        onClick={() => onUpdateQty(item.id, item.quantity + 1)}
                        className="h-6 w-6 flex items-center justify-center text-gray-700 hover:text-gray-900 font-bold"
                      >
                        +
                      </button>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-gray-500">{formatPrice(item.unitPrice)} each</div>
                      <div className="text-sm font-bold text-emerald-700">{formatPrice(item.lineTotal)}</div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer & Checkout Summary */}
          {cart && cart.items.length > 0 && (
            <div className="p-6 bg-white border-t border-gray-200 space-y-4">
              <div className="space-y-1.5 text-xs text-gray-500">
                <div className="flex justify-between">
                  <span>Subtotal:</span>
                  <span className="font-medium text-gray-900">{formatPrice(cart.subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Delivery Charges:</span>
                  <span className="font-medium text-gray-900">{formatPrice(cart.shippingAmount)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Packaging Fee:</span>
                  <span className="font-medium text-gray-900">{formatPrice(cart.packagingFee ?? 10)}</span>
                </div>
                <div className="flex justify-between text-sm font-bold text-gray-900 pt-2 border-t border-gray-200">
                  <span>Grand Total:</span>
                  <span className="text-emerald-700 text-base">{formatPrice(cart.totalAmount)}</span>
                </div>
              </div>

              <button
                id="btn-proceed-to-checkout"
                onClick={onProceedToCheckout}
                className="w-full py-3 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-extrabold text-sm shadow-lg shadow-amber-600/20 hover:brightness-105 transition flex items-center justify-center gap-2"
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

// ─── Location & Maps Helper ───────────────────────────────────────────────────

function extractLocationLink(landmark?: string, notes?: string): string | null {
  const combined = `${landmark || ''} ${notes || ''}`;
  const mapLinkMatch = combined.match(/\[Map Link:\s*([^\s\]]+)\]/i);
  if (mapLinkMatch && mapLinkMatch[1]) return mapLinkMatch[1];

  const gpsMatch = combined.match(/\[GPS:\s*(-?\d+\.\d+),\s*(-?\d+\.\d+)\]/i) ||
    combined.match(/GPS:\s*(-?\d+\.\d+),\s*(-?\d+\.\d+)/i);
  if (gpsMatch && gpsMatch[1] && gpsMatch[2]) {
    return `https://maps.google.com/?q=${gpsMatch[1]},${gpsMatch[2]}`;
  }

  const urlMatch = combined.match(/(https?:\/\/(?:maps\.app\.goo\.gl|goo\.gl\/maps|www\.google\.com\/maps|maps\.google\.com)[^\s\]]*)/i);
  if (urlMatch && urlMatch[1]) return urlMatch[1];

  return null;
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
    phone: auth.user?.phone ?? '',
    apartmentName: '',
    streetAddress: '',
    city: '',
    state: 'Andhra Pradesh',
    postalCode: '',
    nearestLandmark: '',
  });
  const [paymentMethod, setPaymentMethod] = useState<'RAZORPAY' | 'CASH_ON_DELIVERY' | 'TEST_PAYMENT'>('RAZORPAY');
  const [locationLink, setLocationLink] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);
  const [locationCaptured, setLocationCaptured] = useState<{ lat: number; lng: number } | null>(null);
  const [locationFeedback, setLocationFeedback] = useState<{
    type: 'info' | 'error' | 'success';
    message: string;
  } | null>(null);
  const [error, setError] = useState('');

  if (!isOpen || !cart) return null;

  function handleShareLocation() {
    setLocationFeedback(null);
    if (!navigator.geolocation) {
      setLocationFeedback({
        type: 'info',
        message: 'Geolocation is not supported by your browser. You can enter or paste your Google Maps link below.',
      });
      return;
    }
    setLocating(true);

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const { latitude, longitude } = pos.coords;
        setLocationCaptured({ lat: latitude, lng: longitude });
        setLocating(false);
        const mapUrl = `https://maps.google.com/?q=${latitude},${longitude}`;
        if (!locationLink) {
          setLocationLink(mapUrl);
        }
        setLocationFeedback({
          type: 'success',
          message: `GPS location captured (${latitude.toFixed(5)}, ${longitude.toFixed(5)}) and linked to delivery address.`,
        });

        // Reverse geocoding via OpenStreetMap Nominatim
        try {
          const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}`);
          if (res.ok) {
            const data = await res.json();
            const addr = data.address || {};
            setAddress(prev => ({
              ...prev,
              city: addr.city || addr.town || addr.village || addr.suburb || prev.city,
              state: addr.state || prev.state,
              postalCode: addr.postcode || prev.postalCode,
              streetAddress: prev.streetAddress || [addr.road, addr.suburb].filter(Boolean).join(', ') || prev.streetAddress,
              nearestLandmark: prev.nearestLandmark
                ? (prev.nearestLandmark.includes('GPS:') ? prev.nearestLandmark : `${prev.nearestLandmark} (GPS: ${latitude.toFixed(5)}, ${longitude.toFixed(5)})`)
                : `GPS: ${latitude.toFixed(5)}, ${longitude.toFixed(5)}`,
            }));
          } else {
            setAddress(prev => ({
              ...prev,
              nearestLandmark: prev.nearestLandmark || `GPS: ${latitude.toFixed(5)}, ${longitude.toFixed(5)}`,
            }));
          }
        } catch {
          setAddress(prev => ({
            ...prev,
            nearestLandmark: prev.nearestLandmark || `GPS: ${latitude.toFixed(5)}, ${longitude.toFixed(5)}`,
          }));
        }
      },
      (err) => {
        setLocating(false);
        let msg = 'Unable to access device location.';
        if (err.code === 1) {
          msg = 'Location permission is disabled in your browser. You can paste your Google Maps link below or fill your address manually.';
        } else if (err.code === 2) {
          msg = 'GPS location is temporarily unavailable on your device. Please paste your Google Maps link below.';
        } else if (err.code === 3) {
          msg = 'Location request timed out. Please try again or paste your Google Maps link below.';
        }
        setLocationFeedback({
          type: 'info',
          message: msg,
        });
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  }

  async function handleSubmitOrder(e: React.FormEvent) {
    e.preventDefault();
    if (!auth.token || !cart) return;
    const currentToken = auth.token;
    const currentCart = cart;
    setError('');
    setLoading(true);

    const locationDetails: string[] = [];
    if (locationCaptured) {
      locationDetails.push(`[GPS: ${locationCaptured.lat.toFixed(5)}, ${locationCaptured.lng.toFixed(5)}]`);
    }
    if (locationLink.trim()) {
      locationDetails.push(`[Map Link: ${locationLink.trim()}]`);
    }
    const finalNotes = [notes, ...locationDetails].filter(Boolean).join(' ');

    if (paymentMethod === 'RAZORPAY') {
      // ── Razorpay Payment Flow ──
      const rzpKey = (import.meta as any).env?.VITE_RAZORPAY_KEY_ID || 'rzp_live_Sygkkdt4sA85ne';
      let rzpOrderId = '';

      try {
        const orderRes = await fetch(`${COMMERCE_BASE}/payments/razorpay/create-order`, {
          method: 'POST',
          headers: authHeaders(currentToken),
          body: JSON.stringify({
            amount: currentCart.totalAmount.toString(),
          }),
        });
        if (orderRes.ok) {
          const orderData = await orderRes.json();
          if (orderData?.success && orderData.data?.razorpayOrderId) {
            rzpOrderId = orderData.data.razorpayOrderId;
          }
          if (orderData?.data?.keyId && orderData.data.keyId !== 'PENDING') {
            // Can optionally use authoritative public key from backend
          }
        }
      } catch (err) {
        console.warn('Backend Razorpay order creation warning:', err);
      }

      if (typeof (window as any).Razorpay === 'undefined') {
        setError('Razorpay SDK is loading. Please check your internet connection or try again in a moment.');
        setLoading(false);
        return;
      }

      const rzpOptions = {
        key: rzpKey,
        amount: Math.round(currentCart.totalAmount * 100),
        currency: 'INR',
        name: 'RR Jaggery Traders',
        description: `Natural Sugarcane Jaggery (${currentCart.totalItems} items)`,
        order_id: rzpOrderId || undefined,
        prefill: {
          name: address.recipientName,
          email: auth.user?.email || '',
          contact: address.phone,
        },
        notes: {
          deliveryCity: address.city,
          address: `${address.streetAddress}, ${address.apartmentName || ''}`,
          ...(locationCaptured ? { gps: `${locationCaptured.lat},${locationCaptured.lng}` } : {}),
          ...(locationLink.trim() ? { mapLink: locationLink.trim() } : {}),
        },
        theme: {
          color: '#065f46',
        },
        handler: async function (response: any) {
          // Verify signature on backend if razorpay_order_id is returned
          if (response.razorpay_order_id && response.razorpay_signature) {
            try {
              await fetch(`${COMMERCE_BASE}/payments/razorpay/verify`, {
                method: 'POST',
                headers: authHeaders(currentToken),
                body: JSON.stringify({
                  razorpay_order_id: response.razorpay_order_id,
                  razorpay_payment_id: response.razorpay_payment_id,
                  razorpay_signature: response.razorpay_signature,
                }),
              });
            } catch (vErr) {
              console.warn('Backend payment verification log:', vErr);
            }
          }

          const orderNotes = [
            finalNotes,
            `[Razorpay ID: ${response.razorpay_payment_id}]`
          ].filter(Boolean).join(' ');

          try {
            const res = await fetch(`${COMMERCE_BASE}/orders/checkout`, {
              method: 'POST',
              headers: authHeaders(currentToken),
              body: JSON.stringify({
                shippingAddress: address,
                paymentMethod: 'RAZORPAY',
                notes: orderNotes,
              }),
            });
            let body: any = null;
            try { body = await res.json(); } catch { }
            if (res.ok && body?.success) {
              onOrderPlaced(body.data);
            } else {
              setError(body?.error?.message ?? `Payment received (${response.razorpay_payment_id}), but order could not be registered. Contact support.`);
            }
          } catch {
            setError(`Payment successful (${response.razorpay_payment_id}), but connection to server failed. Please contact mill support.`);
          } finally {
            setLoading(false);
          }
        },
        modal: {
          ondismiss: function () {
            setLoading(false);
          },
        },
      };

      try {
        const rzpInstance = new (window as any).Razorpay(rzpOptions);
        rzpInstance.on('payment.failed', function (resp: any) {
          setError(resp?.error?.description || 'Payment was not completed. Please try again.');
          setLoading(false);
        });
        rzpInstance.open();
      } catch (e: any) {
        setError(e?.message || 'Could not open Razorpay checkout modal.');
        setLoading(false);
      }
      return;
    }

    // ── COD or TEST_PAYMENT Flow ──
    try {
      const res = await fetch(`${COMMERCE_BASE}/orders/checkout`, {
        method: 'POST',
        headers: authHeaders(currentToken),
        body: JSON.stringify({
          shippingAddress: address,
          paymentMethod,
          notes: finalNotes,
        }),
      });
      let body: any = null;
      try {
        body = await res.json();
      } catch { }
      if (res.ok && body?.success) {
        onOrderPlaced(body.data);
      } else {
        setError(body?.error?.message ?? (res.status === 504 ? 'Request timed out. Please try again.' : `Checkout error (${res.status}): Please check cart and try again.`));
      }
    } catch {
      setError('Cannot connect to Commerce Service. Please check backend.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-emerald-50/60 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white border border-gray-200 rounded-3xl p-6 sm:p-8 w-full max-w-2xl shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <button
          id="close-checkout-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-gray-100 text-gray-500 hover:text-gray-900 transition"
        >
          <X className="h-4 w-4" />
        </button>

        <h2 className="text-xl font-bold text-gray-900 mb-2 flex items-center gap-2">
          <CreditCard className="h-5 w-5 text-emerald-700" /> Secure Checkout & Order Placement
        </h2>
        <p className="text-xs text-gray-500 mb-6">Authoritative server-side price validation and transaction safety</p>

        {error && (
          <div className="mb-4 flex items-center gap-2 text-xs text-red-600 bg-red-500/10 border border-red-500/20 rounded-xl px-3 py-2.5">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmitOrder} className="space-y-6">
          {/* Shipping Address */}
          <div className="space-y-3">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-700">1. Delivery Address</h3>
              <button
                type="button"
                id="btn-share-location"
                onClick={handleShareLocation}
                disabled={locating}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-300 text-xs font-bold transition shadow-2xs hover:shadow-xs disabled:opacity-50"
                title="Detect GPS coordinates using your device location"
              >
                {locating ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 animate-spin text-emerald-700" />
                    <span>Detecting GPS…</span>
                  </>
                ) : (
                  <>
                    <MapPin className="h-3.5 w-3.5 text-emerald-700" />
                    <span>Share / Use Current Location</span>
                  </>
                )}
              </button>
            </div>

            {/* Non-blocking Location Feedback Notice */}
            {locationFeedback && (
              <div
                className={`flex items-start justify-between text-xs rounded-xl px-3.5 py-2.5 transition border ${locationFeedback.type === 'success'
                  ? 'bg-emerald-50 text-emerald-900 border-emerald-300'
                  : 'bg-amber-50 text-amber-900 border-amber-300'
                  }`}
              >
                <div className="flex items-start gap-2">
                  {locationFeedback.type === 'success' ? (
                    <CheckCircle2 className="h-4 w-4 text-emerald-700 shrink-0 mt-0.5" />
                  ) : (
                    <Info className="h-4 w-4 text-amber-700 shrink-0 mt-0.5" />
                  )}
                  <span>{locationFeedback.message}</span>
                </div>
                <button
                  type="button"
                  onClick={() => setLocationFeedback(null)}
                  className="text-stone-400 hover:text-stone-700 ml-2 p-0.5"
                  title="Dismiss notice"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            )}

            {locationCaptured && (
              <div className="flex items-center justify-between text-xs bg-emerald-50/90 border border-emerald-200 rounded-xl px-3.5 py-2.5 text-emerald-900 shadow-2xs">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-emerald-700 shrink-0" />
                  <span>GPS Location Attached: <strong>{locationCaptured.lat.toFixed(5)}, {locationCaptured.lng.toFixed(5)}</strong></span>
                </div>
                <a
                  href={`https://maps.google.com/?q=${locationCaptured.lat},${locationCaptured.lng}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-bold underline text-emerald-800 hover:text-emerald-950 flex items-center gap-1 shrink-0"
                >
                  <span>View on Map</span>
                  <ExternalLink className="h-3 w-3" />
                </a>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div>
                <label className="text-gray-500 font-semibold">Recipient Full Name</label>
                <input
                  id="checkout-recipient"
                  type="text"
                  required
                  value={address.recipientName}
                  onChange={e => setAddress(a => ({ ...a, recipientName: e.target.value }))}
                  placeholder="Your name"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold">Contact Phone</label>
                <input
                  id="checkout-phone"
                  type="tel"
                  required
                  pattern="(?:\+91[ -]?)?[6-9][0-9]{9}"
                  title="Enter a valid Indian mobile number, optionally starting with +91."
                  value={address.phone}
                  onChange={e => setAddress(a => ({ ...a, phone: e.target.value }))}
                  placeholder="Your phone number"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold">Apartment / Building Name</label>
                <input
                  id="checkout-apartment"
                  type="text"
                  required
                  value={address.apartmentName}
                  onChange={e => setAddress(a => ({ ...a, apartmentName: e.target.value }))}
                  placeholder="Your apartment or building name"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold">Street Name</label>
                <input
                  id="checkout-street"
                  type="text"
                  required
                  value={address.streetAddress}
                  onChange={e => setAddress(a => ({ ...a, streetAddress: e.target.value }))}
                  placeholder="Your street address"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold">City / Village</label>
                <input
                  id="checkout-city"
                  type="text"
                  required
                  value={address.city}
                  onChange={e => setAddress(a => ({ ...a, city: e.target.value }))}
                  placeholder="Your city"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold">Nearest Landmark</label>
                <input
                  id="checkout-landmark"
                  type="text"
                  required
                  value={address.nearestLandmark}
                  onChange={e => setAddress(a => ({ ...a, nearestLandmark: e.target.value }))}
                  placeholder="Your landmark"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold">Verified Account Email</label>
                <input
                  id="checkout-email"
                  type="email"
                  value={auth.user?.email ?? ''}
                  readOnly
                  aria-readonly="true"
                  className="w-full mt-1 bg-gray-100 border border-gray-200 rounded-xl px-3 py-2 text-gray-700"
                />
                <p className="mt-1 text-[10px] text-emerald-700">Your verified email is linked to this account and cannot be changed at checkout.</p>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-gray-500 font-semibold">State</label>
                  <input
                    id="checkout-state"
                    type="text"
                    required
                    value={address.state}
                    onChange={e => setAddress(a => ({ ...a, state: e.target.value }))}
                    placeholder="Your state"
                    className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                  />
                </div>
                <div>
                  <label className="text-gray-500 font-semibold">PIN Code</label>
                  <input
                    id="checkout-postalcode"
                    type="text"
                    required
                    inputMode="numeric"
                    pattern="[0-9]{6}"
                    maxLength={6}
                    title="Enter a 6-digit PIN code."
                    value={address.postalCode}
                    onChange={e => setAddress(a => ({ ...a, postalCode: e.target.value }))}
                    placeholder="Your 6-digit PIN code"
                    className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                  />
                </div>
              </div>

              {/* Google Maps / Delivery Location Link Input */}
              <div className="sm:col-span-2 bg-stone-50/80 p-3.5 rounded-2xl border border-stone-200/90 space-y-2">
                <div className="flex items-center justify-between">
                  <label htmlFor="checkout-location-link" className="text-gray-800 font-bold flex items-center gap-1.5 text-xs">
                    <MapPin className="h-3.5 w-3.5 text-emerald-700" />
                    <span>Google Maps / Delivery Location Link (Optional)</span>
                  </label>
                  {locationLink.trim() && (
                    <a
                      href={locationLink.trim().startsWith('http') ? locationLink.trim() : `https://${locationLink.trim()}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[11px] font-bold text-emerald-800 hover:text-emerald-950 underline flex items-center gap-1"
                    >
                      <span>Preview Map</span>
                      <ExternalLink className="h-3 w-3" />
                    </a>
                  )}
                </div>
                <p className="text-[11px] text-gray-500 leading-tight">
                  Paste your Google Maps link (e.g. <code>https://maps.app.goo.gl/...</code>) or live location link so the delivery agent can reach your exact location without calling.
                </p>
                <div className="flex gap-2">
                  <input
                    id="checkout-location-link"
                    type="text"
                    value={locationLink}
                    onChange={e => {
                      const val = e.target.value;
                      setLocationLink(val);
                      if (val.trim()) {
                        setLocationFeedback(null);
                      }
                    }}
                    placeholder="Paste Google Maps URL here (e.g. https://maps.app.goo.gl/xyz or https://goo.gl/maps/...)"
                    className="flex-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600 text-xs placeholder:text-gray-400"
                  />
                  {locationLink.trim() && (
                    <button
                      type="button"
                      onClick={() => setLocationLink('')}
                      className="px-2.5 py-1 text-gray-400 hover:text-gray-700 rounded-xl bg-gray-100 hover:bg-gray-200 text-xs font-semibold"
                      title="Clear location link"
                    >
                      Clear
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Payment Method */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-700">2. Payment Method</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <label
                id="payment-method-razorpay"
                className={`p-3.5 rounded-2xl border cursor-pointer flex flex-col justify-between gap-2 transition ${paymentMethod === 'RAZORPAY' ? 'bg-emerald-700/10 border-emerald-600 text-emerald-800 ring-2 ring-emerald-600/20' : 'bg-white border-gray-200 text-gray-500 hover:border-gray-300'}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="RAZORPAY"
                      checked={paymentMethod === 'RAZORPAY'}
                      onChange={() => setPaymentMethod('RAZORPAY')}
                      className="text-emerald-700 focus:ring-emerald-600"
                    />
                    <CreditCard className="h-4 w-4 text-emerald-700" />
                  </div>
                  <span className="text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-300">
                    Instant
                  </span>
                </div>
                <div>
                  <div className="text-xs font-bold text-gray-900">Razorpay Online</div>
                  <div className="text-[10px] text-gray-500 leading-tight mt-0.5">UPI (GPay/PhonePe), Cards, NetBanking</div>
                </div>
              </label>

              <label
                id="payment-method-cod"
                className={`p-3.5 rounded-2xl border cursor-pointer flex flex-col justify-between gap-2 transition ${paymentMethod === 'CASH_ON_DELIVERY' ? 'bg-emerald-700/10 border-emerald-600 text-emerald-800 ring-2 ring-emerald-600/20' : 'bg-white border-gray-200 text-gray-500 hover:border-gray-300'}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="CASH_ON_DELIVERY"
                      checked={paymentMethod === 'CASH_ON_DELIVERY'}
                      onChange={() => setPaymentMethod('CASH_ON_DELIVERY')}
                      className="text-emerald-700 focus:ring-emerald-600"
                    />
                    <Truck className="h-4 w-4 text-emerald-700" />
                  </div>
                </div>
                <div>
                  <div className="text-xs font-bold text-gray-900">Cash on Delivery</div>
                  <div className="text-[10px] text-gray-500 leading-tight mt-0.5">Pay upon mill delivery / weighment receipt</div>
                </div>
              </label>

              <label
                id="payment-method-test"
                className={`p-3.5 rounded-2xl border cursor-pointer flex flex-col justify-between gap-2 transition ${paymentMethod === 'TEST_PAYMENT' ? 'bg-emerald-700/10 border-emerald-600 text-emerald-800 ring-2 ring-emerald-600/20' : 'bg-white border-gray-200 text-gray-500 hover:border-gray-300'}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="TEST_PAYMENT"
                      checked={paymentMethod === 'TEST_PAYMENT'}
                      onChange={() => setPaymentMethod('TEST_PAYMENT')}
                      className="text-emerald-700 focus:ring-emerald-600"
                    />
                    <Sparkles className="h-4 w-4 text-emerald-700" />
                  </div>
                  <span className="text-[10px] font-bold text-stone-500 bg-stone-100 px-1.5 py-0.5 rounded">
                    Test
                  </span>
                </div>
                <div>
                  <div className="text-xs font-bold text-gray-900">Test Payment</div>
                  <div className="text-[10px] text-gray-500 leading-tight mt-0.5">Instant simulation without gateway</div>
                </div>
              </label>
            </div>
          </div>

          {/* Delivery Notes / Special Instructions */}
          <div className="space-y-1 text-xs">
            <label className="text-gray-500 font-semibold">Special Delivery Instructions / Mill Notes (Optional)</label>
            <input
              id="checkout-notes"
              type="text"
              placeholder="Your delivery notes (optional)"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              className="w-full bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
            />
          </div>

          {/* Order Summary Breakdown */}
          <div className="bg-white p-4 rounded-2xl border border-gray-200 space-y-2 text-xs">
            <div className="flex justify-between text-gray-500">
              <span>Items ({cart.totalItems}):</span>
              <span className="text-gray-900 font-medium">{formatPrice(cart.subtotal)}</span>
            </div>
            <div className="flex justify-between text-gray-500">
              <span>Delivery Charges:</span>
              <span className="text-gray-900 font-medium">{formatPrice(cart.shippingAmount)}</span>
            </div>
            <div className="flex justify-between text-gray-500">
              <span>Packaging Fee:</span>
              <span className="text-gray-900 font-medium">{formatPrice(cart.packagingFee ?? 10)}</span>
            </div>
            <div className="flex justify-between text-sm font-bold text-gray-900 pt-2 border-t border-gray-200">
              <span>Grand Total Payable:</span>
              <span className="text-emerald-700 text-base">{formatPrice(cart.totalAmount)}</span>
            </div>
          </div>

          {/* Place Order CTA */}
          <button
            id="btn-place-order"
            type="submit"
            disabled={loading}
            className="w-full py-3.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-black text-sm shadow-xl shadow-amber-600/30 hover:brightness-105 disabled:opacity-50 flex items-center justify-center gap-2 transition"
          >
            {loading ? (
              <>
                <Spinner size={5} />
                <span>Processing Order…</span>
              </>
            ) : paymentMethod === 'RAZORPAY' ? (
              <>
                <ShieldCheck className="h-4 w-4 text-emerald-200" />
                <span>Pay {formatPrice(cart.totalAmount)} via Razorpay</span>
                <ArrowRight className="h-4 w-4" />
              </>
            ) : (
              <>
                <span>Confirm & Place Order ({formatPrice(cart.totalAmount)})</span>
                <ArrowRight className="h-4 w-4" />
              </>
            )}
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-emerald-50/60 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white border border-gray-200 rounded-3xl p-6 sm:p-8 w-full max-w-lg shadow-2xl relative text-center space-y-5">
        <button
          id="close-order-confirmation-modal"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl bg-gray-100 text-gray-500 hover:text-gray-900 transition"
        >
          <X className="h-4 w-4" />
        </button>

        <div className="h-16 w-16 bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 rounded-3xl flex items-center justify-center mx-auto shadow-lg shadow-emerald-500/10">
          <CheckCircle2 className="h-8 w-8" />
        </div>

        <div>
          <h2 className="text-2xl font-black text-gray-900">Order Confirmed!</h2>
          <p className="text-xs text-gray-500 mt-1">Thank you for your order with RR Jaggery Traders.</p>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-gray-200 text-left text-xs space-y-2">
          <div className="flex justify-between">
            <span className="text-gray-500">Order Reference:</span>
            <span id="confirmed-order-number" className="font-mono font-bold text-emerald-700">{order.orderNumber}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Payment Method:</span>
            <span className="font-bold text-gray-900">
              {order.paymentMethod === 'RAZORPAY' ? 'Razorpay Online (Verified)' : order.paymentMethod === 'CASH_ON_DELIVERY' ? 'Cash on Delivery (COD)' : order.paymentMethod}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Payment Status:</span>
            <span className={`font-bold ${order.paymentStatus === 'PAID' ? 'text-emerald-700' : 'text-amber-700'}`}>
              {order.paymentStatus}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Total Amount:</span>
            <span className="font-bold text-gray-900">{formatPrice(order.totalAmount)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Delivery To:</span>
            <span className="text-gray-700 text-right max-w-[65%]">
              {order.shippingAddress.recipientName}, {order.shippingAddress.apartmentName ? `${order.shippingAddress.apartmentName}, ` : ''}{order.shippingAddress.streetAddress}, {order.shippingAddress.city}
              {order.shippingAddress.nearestLandmark ? ` (${order.shippingAddress.nearestLandmark})` : ''}
            </span>
          </div>
          {extractLocationLink(order.shippingAddress?.nearestLandmark, order.notes) && (
            <div className="flex justify-between items-center bg-emerald-50/80 p-2.5 rounded-xl border border-emerald-200">
              <span className="text-gray-600 font-semibold flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-emerald-700" />
                <span>Delivery Location:</span>
              </span>
              <a
                href={extractLocationLink(order.shippingAddress?.nearestLandmark, order.notes)!}
                target="_blank"
                rel="noopener noreferrer"
                className="font-bold underline text-emerald-800 hover:text-emerald-950 flex items-center gap-1 text-[11px]"
              >
                <span>Open in Google Maps</span>
                <ExternalLink className="h-3 w-3" />
              </a>
            </div>
          )}
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <button
            id="btn-confirm-view-invoice"
            onClick={() => onViewInvoice(order.id)}
            className="flex-1 py-2.5 rounded-xl bg-gray-100 hover:bg-emerald-100 text-gray-800 font-bold text-xs border border-gray-300 transition flex items-center justify-center gap-1.5"
          >
            <FileText className="h-4 w-4 text-emerald-700" /> View Commercial Invoice
          </button>
          <button
            id="btn-confirm-view-orders"
            onClick={() => onViewOrders()}
            className="flex-1 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-xs shadow-lg shadow-amber-600/20 hover:brightness-105 transition flex items-center justify-center gap-1.5"
          >
            <Receipt className="h-4 w-4" /> My Orders History
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Printable Commercial Invoice Modal ──────────────────────────────────────

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

  const handlePrintInvoice = () => {
    const content = document.getElementById('invoice-modal-content');
    if (!content) {
      window.print();
      return;
    }

    let printFrame = document.getElementById('print-invoice-frame') as HTMLIFrameElement;
    if (!printFrame) {
      printFrame = document.createElement('iframe');
      printFrame.id = 'print-invoice-frame';
      printFrame.style.position = 'fixed';
      printFrame.style.right = '0';
      printFrame.style.bottom = '0';
      printFrame.style.width = '0';
      printFrame.style.height = '0';
      printFrame.style.border = '0';
      document.body.appendChild(printFrame);
    }

    const frameDoc = printFrame.contentWindow?.document || printFrame.contentDocument;
    if (!frameDoc) {
      window.print();
      return;
    }

    frameDoc.open();
    frameDoc.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Invoice - ${invoice?.invoiceNumber || 'RR-Jaggery'}</title>
          <script src="https://cdn.tailwindcss.com"></script>
          <style>
            @page { size: A4 portrait; margin: 12mm; }
            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; -webkit-print-color-adjust: exact; print-color-adjust: exact; background: #ffffff !important; color: #111827 !important; }
          </style>
        </head>
        <body class="p-6">
          ${content.innerHTML}
        </body>
      </html>
    `);
    frameDoc.close();

    setTimeout(() => {
      try {
        printFrame.contentWindow?.focus();
        printFrame.contentWindow?.print();
      } catch {
        window.print();
      }
    }, 450);
  };

  if (!orderId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 md:p-6 bg-slate-900/60 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white border border-stone-200 rounded-2xl sm:rounded-3xl w-full max-w-3xl shadow-2xl relative max-h-[96vh] sm:max-h-[92vh] flex flex-col overflow-hidden">
        {/* ── Modal Sticky Header ── */}
        <div className="px-4 sm:px-6 py-3.5 bg-gradient-to-r from-stone-50 via-white to-stone-50 border-b border-stone-200 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="h-8 w-8 rounded-xl bg-emerald-100/70 border border-emerald-200/80 flex items-center justify-center">
              <FileText className="h-4 w-4 text-emerald-800" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-black text-gray-900 tracking-tight">
                Tax & Commercial Invoice
              </h2>
              <p className="text-[10px] text-gray-400 hidden sm:block">
                Original for Recipient · RR Jaggery Traditional Products
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              id="btn-print-invoice"
              onClick={handlePrintInvoice}
              className="px-3 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 hover:border-emerald-400 rounded-xl text-xs font-bold flex items-center gap-1.5 transition shadow-2xs"
              title="Print or Save PDF"
            >
              <Printer className="h-3.5 w-3.5 text-emerald-700" />
              <span>Print Invoice</span>
            </button>
            <button
              id="close-invoice-modal"
              onClick={onClose}
              className="p-1.5 bg-gray-100 hover:bg-gray-200 text-gray-600 hover:text-gray-900 rounded-xl transition"
              title="Close modal"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* ── Scrollable Invoice Document ── */}
        <div className="flex-1 overflow-y-auto p-3 sm:p-6 md:p-8 bg-stone-50/40">
          {loading ? (
            <div className="flex flex-col items-center justify-center h-64 bg-white rounded-2xl p-8 border border-stone-200">
              <Spinner size={8} />
              <p className="text-xs text-gray-400 mt-3 font-medium">Generating official invoice...</p>
            </div>
          ) : error ? (
            <div className="text-center py-12 text-red-600 bg-white rounded-2xl p-8 border border-red-200">
              <XCircle className="h-10 w-10 text-red-500 mx-auto mb-2" />
              <p className="text-sm font-semibold">{error}</p>
            </div>
          ) : invoice ? (
            <div
              id="invoice-modal-content"
              className="bg-white p-4 sm:p-7 md:p-8 rounded-2xl border-2 border-stone-200/90 text-xs text-gray-700 space-y-6 shadow-sm"
            >
              {/* ── Brand Header with RR Jaggery Logo & Invoice Details ── */}
              <div className="flex flex-col md:flex-row md:items-start justify-between gap-5 border-b border-stone-200 pb-5">
                {/* Brand & Mill Details with Logo */}
                <div className="flex items-start gap-3.5">
                  <img
                    src="/rr-logo.png"
                    alt="RR Jaggery Logo"
                    className="h-14 w-14 sm:h-16 sm:w-16 rounded-2xl object-cover border-2 border-emerald-600/30 shadow-xs shrink-0"
                  />
                  <div>
                    <div id="invoice-seller-name" className="font-black text-lg sm:text-2xl text-emerald-950 tracking-tight flex items-center gap-2">
                      {invoice.sellerName?.toLowerCase().includes('traders') ? invoice.sellerName : 'RR Jaggery Traders'}
                    </div>
                    <div className="text-[11px] font-bold text-emerald-700 tracking-wide mt-0.5">
                      Authentic Sugarcane & Palm Jaggery Mill
                    </div>
                    <div className="text-gray-500 text-[11px] sm:text-xs mt-1 leading-relaxed max-w-sm">
                      {invoice.sellerAddress}
                    </div>
                    <div className="text-gray-500 text-[11px] sm:text-xs font-medium mt-0.5">
                      📞 Contact: {invoice.sellerContact}
                    </div>
                    {invoice.sellerGstin && (
                      <div className="text-gray-500 text-[11px] font-mono mt-0.5">
                        GSTIN: <span className="font-bold text-gray-700">{invoice.sellerGstin}</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Invoice Meta on Right */}
                <div className="bg-stone-50/80 rounded-2xl p-3.5 sm:p-4 border border-stone-200/80 md:text-right space-y-1.5 shrink-0 min-w-[200px]">
                  <div>
                    <span className="text-[10px] uppercase font-extrabold tracking-wider text-gray-400 block md:inline-block">Invoice No:</span>{' '}
                    <span className="text-sm sm:text-base font-black text-emerald-800 font-mono tracking-wide">
                      {invoice.invoiceNumber}
                    </span>
                  </div>
                  <div className="text-gray-500 text-[11px]">
                    Order Ref: <span className="font-mono font-bold text-gray-800">{invoice.orderNumber}</span>
                  </div>
                  <div className="text-gray-500 text-[11px]">
                    Date: <span className="font-semibold text-gray-800">{new Date(invoice.invoiceDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
                  </div>
                  <div className="pt-1">
                    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold border ${invoice.paymentStatus === 'PAID'
                      ? 'bg-emerald-100/80 text-emerald-800 border-emerald-300'
                      : 'bg-amber-100/80 text-amber-800 border-amber-300'
                      }`}>
                      <CreditCard className="h-3 w-3" />
                      {invoice.paymentStatus === 'PAID' ? 'Paid Online' : `Pending (${invoice.paymentMethod})`}
                    </span>
                  </div>
                </div>
              </div>

              {/* ── Billed To & Business Details ── */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 border-b border-stone-200 pb-5">
                <div className="bg-stone-50/60 rounded-2xl p-4 border border-stone-200/80">
                  <div className="text-gray-400 font-black uppercase tracking-wider text-[10px] flex items-center gap-1.5 mb-2">
                    <MapPin className="h-3.5 w-3.5 text-emerald-700" />
                    Billed & Shipped To
                  </div>
                  <div className="text-gray-900 font-black text-sm">{invoice.customerName}</div>
                  <div className="text-gray-600 text-xs mt-1 leading-relaxed">
                    {invoice.shippingAddress.apartmentName && <div>{invoice.shippingAddress.apartmentName}</div>}
                    <div>{invoice.shippingAddress.streetAddress}</div>
                    <div>{invoice.shippingAddress.city}, {invoice.shippingAddress.state} - {invoice.shippingAddress.postalCode}</div>
                    {invoice.shippingAddress.nearestLandmark && (
                      <div className="text-gray-500 text-[11px] mt-0.5">Near: {invoice.shippingAddress.nearestLandmark}</div>
                    )}
                  </div>
                  <div className="text-gray-500 text-[11px] mt-2 pt-2 border-t border-stone-200/60">
                    <span>📞 {invoice.shippingAddress.phone || invoice.customerPhone}</span>
                    {invoice.customerEmail && <span> · ✉️ {invoice.customerEmail}</span>}
                  </div>
                </div>

                <div className="bg-stone-50/60 rounded-2xl p-4 border border-stone-200/80 flex flex-col justify-between">
                  <div>
                    <div className="text-gray-400 font-black uppercase tracking-wider text-[10px] flex items-center gap-1.5 mb-2">
                      <Leaf className="h-3.5 w-3.5 text-emerald-700" />
                      Product & Mill Profile
                    </div>
                    <div className="text-gray-900 font-bold text-xs">Jaggery Manufacturing & Direct Distribution</div>
                    <div className="text-gray-500 text-[11px] mt-1 leading-relaxed">
                      All products supplied directly from the sugarcane crushing and organic boiling unit at Kankipadu.
                    </div>
                  </div>
                  <div className="mt-3 pt-2 border-t border-stone-200/60 flex items-center justify-between text-[11px] text-gray-500">
                    <span>Supply Type: <strong className="text-gray-700">Intra-State / B2C</strong></span>
                    <span className="text-emerald-700 font-bold">100% Natural</span>
                  </div>
                </div>
              </div>

              {/* ── Items: Desktop/Tablet Table View ── */}
              <div className="hidden sm:block overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="bg-stone-100/80 rounded-xl text-stone-600 text-[10px] uppercase font-black tracking-wider">
                      <th className="py-2.5 px-3 rounded-l-xl">Item Description</th>
                      <th className="py-2.5 px-2">SKU</th>
                      <th className="py-2.5 px-2 text-center">Qty</th>
                      <th className="py-2.5 px-2 text-right">Unit Rate</th>
                      <th className="py-2.5 px-3 text-right rounded-r-xl">Total Amount</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-stone-200/60">
                    {invoice.items.map(item => (
                      <tr key={item.id} className="hover:bg-stone-50/60 transition-colors">
                        <td className="py-3 px-3">
                          <div className="flex items-center gap-2">
                            <span className="text-base">🍯</span>
                            <div>
                              <div className="font-bold text-gray-900 text-xs">{item.productName}</div>
                              <div className="text-[10px] text-gray-400">{item.unitWeightKg}kg pack</div>
                            </div>
                          </div>
                        </td>
                        <td className="py-3 px-2 font-mono text-[11px] text-gray-500">{item.sku}</td>
                        <td className="py-3 px-2 text-center font-bold text-gray-900">{item.quantity}</td>
                        <td className="py-3 px-2 text-right text-gray-700 font-medium">{formatPrice(item.unitPrice)}</td>
                        <td className="py-3 px-3 text-right font-black text-emerald-800">{formatPrice(item.lineTotal)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* ── Items: Mobile Friendly Responsive Cards ── */}
              <div className="block sm:hidden space-y-2.5">
                <div className="text-[10px] font-black uppercase tracking-wider text-gray-400">
                  Ordered Items ({invoice.items.length})
                </div>
                {invoice.items.map(item => (
                  <div
                    key={item.id}
                    className="bg-stone-50/80 border border-stone-200 rounded-xl p-3 flex items-start justify-between gap-2"
                  >
                    <div className="flex items-start gap-2.5 min-w-0">
                      <span className="text-lg shrink-0 mt-0.5">🍯</span>
                      <div className="min-w-0">
                        <div className="font-bold text-gray-900 text-xs leading-snug">
                          {item.productName}
                        </div>
                        <div className="text-[10px] text-gray-500 font-mono mt-0.5">
                          SKU: {item.sku} · {item.unitWeightKg}kg
                        </div>
                        <div className="text-[11px] text-gray-600 font-medium mt-1">
                          Qty: <strong className="text-gray-900">{item.quantity}</strong> × {formatPrice(item.unitPrice)}
                        </div>
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <div className="text-xs font-black text-emerald-800">
                        {formatPrice(item.lineTotal)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* ── Totals Breakdown ── */}
              <div className="flex flex-col sm:flex-row justify-between items-start gap-4 pt-4 border-t border-stone-200">
                <div className="text-gray-500 text-[11px] max-w-sm leading-relaxed">
                  <div className="font-bold text-gray-700">Commercial Tax Invoice Declaration:</div>
                  <div>This is a valid commercial purchase invoice. Standard agricultural jaggery is non-GST exempted or rated as per applicable tax notifications.</div>
                </div>

                <div className="bg-stone-50/80 rounded-2xl p-4 border border-stone-200/80 w-full sm:w-72 space-y-2">
                  <div className="flex justify-between text-xs">
                    <span className="text-gray-500">Items Subtotal:</span>
                    <span className="text-gray-900 font-bold">{formatPrice(invoice.subtotal)}</span>
                  </div>

                  {Number(invoice.totalTax || 0) > 0 && (
                    <div className="flex justify-between text-xs text-gray-500">
                      <span>Taxes:</span>
                      <span className="text-gray-900 font-medium">{formatPrice(invoice.totalTax)}</span>
                    </div>
                  )}

                  <div className="flex justify-between text-xs">
                    <span className="text-gray-500">Shipping & Delivery:</span>
                    <span className="text-gray-900 font-bold">{formatPrice(invoice.shippingAmount)}</span>
                  </div>

                  <div className="flex justify-between text-xs">
                    <span className="text-gray-500">Packaging Fee:</span>
                    <span className="text-gray-900 font-bold">{formatPrice(invoice.packagingFee ?? 10)}</span>
                  </div>

                  <div className="pt-2 border-t border-stone-200 flex justify-between items-center">
                    <span className="text-xs font-black uppercase text-emerald-950">Grand Total:</span>
                    <span className="text-base sm:text-lg font-black text-emerald-800 font-mono">
                      {formatPrice(invoice.grandTotal)}
                    </span>
                  </div>
                </div>
              </div>

              {/* ── Authentic Mill Footer Seal ── */}
              <div className="pt-4 border-t border-stone-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-center sm:text-left text-[11px] text-gray-400">
                <div className="flex items-center gap-2 text-emerald-800 font-medium">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                  <span>Authorized computer-generated invoice from RR Jaggery Traders</span>
                </div>
                <div className="text-gray-500">
                  Support: <strong className="text-gray-700">+91 7207063315</strong>
                </div>
              </div>
            </div>
          ) : null}
        </div>
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
  const [filter, setFilter] = useState<'ALL' | 'ACTIVE' | 'DELIVERED' | 'CANCELLED'>('ALL');

  const fetchOrders = useCallback(async () => {
    if (!auth.token) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${COMMERCE_BASE}/orders`, { headers: authHeaders(auth.token) });
      const body = await res.json().catch(() => null);
      if (res.ok && body && (body.success !== false)) {
        const orderList = Array.isArray(body) ? body : (body.data || []);
        setOrders(orderList);
      } else {
        setError(body?.error?.message || body?.message || `Failed to load order history (${res.status}).`);
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

  const activeOrdersCount = orders.filter(o => ['PENDING', 'CONFIRMED', 'PROCESSING', 'DISPATCHED'].includes(o.status)).length;
  const deliveredOrdersCount = orders.filter(o => o.status === 'DELIVERED').length;
  const cancelledOrdersCount = orders.filter(o => o.status === 'CANCELLED').length;

  const filteredOrders = orders.filter(order => {
    if (filter === 'ACTIVE') return ['PENDING', 'CONFIRMED', 'PROCESSING', 'DISPATCHED'].includes(order.status);
    if (filter === 'DELIVERED') return order.status === 'DELIVERED';
    if (filter === 'CANCELLED') return order.status === 'CANCELLED';
    return true;
  });

  return (
    <div className="space-y-6 sm:space-y-8 animate-fadeIn max-w-5xl mx-auto pb-12">
      {/* ── Top Navigation & Header ── */}
      <div className="bg-white rounded-3xl p-5 sm:p-7 border border-emerald-900/10 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <button
            onClick={onBackToStore}
            className="p-2.5 bg-gray-50 hover:bg-emerald-50 border border-gray-200 hover:border-emerald-300 rounded-2xl text-gray-600 hover:text-emerald-800 transition-all shadow-xs"
            title="Back to Store"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-2xl font-black text-gray-900 tracking-tight">My Orders</h2>
              <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-xs font-black">
                {orders.length}
              </span>
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Review your purchase history, track shipments, and download GST commercial invoices.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-center">
          <button
            onClick={fetchOrders}
            className="px-3.5 py-2 rounded-xl bg-gray-50 hover:bg-emerald-50 border border-gray-200 hover:border-emerald-300 text-gray-700 hover:text-emerald-800 text-xs font-bold transition flex items-center gap-2 shadow-xs"
            title="Refresh Orders"
          >
            <RefreshCw className={`h-3.5 w-3.5 text-emerald-700 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* ── Filter Tabs ── */}
      {orders.length > 0 && (
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          <button
            onClick={() => setFilter('ALL')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 ${filter === 'ALL'
              ? 'bg-emerald-800 text-white shadow-md'
              : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
              }`}
          >
            All Orders ({orders.length})
          </button>
          <button
            onClick={() => setFilter('ACTIVE')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 ${filter === 'ACTIVE'
              ? 'bg-amber-600 text-white shadow-md'
              : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
              }`}
          >
            Active In-Flight ({activeOrdersCount})
          </button>
          <button
            onClick={() => setFilter('DELIVERED')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 ${filter === 'DELIVERED'
              ? 'bg-emerald-600 text-white shadow-md'
              : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
              }`}
          >
            Delivered ({deliveredOrdersCount})
          </button>
          {cancelledOrdersCount > 0 && (
            <button
              onClick={() => setFilter('CANCELLED')}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 ${filter === 'CANCELLED'
                ? 'bg-red-600 text-white shadow-md'
                : 'bg-white text-gray-600 hover:bg-gray-100 border border-gray-200'
                }`}
            >
              Cancelled ({cancelledOrdersCount})
            </button>
          )}
        </div>
      )}

      {loading ? (
        <div className="flex flex-col items-center justify-center h-64 bg-white rounded-3xl border border-gray-200 p-8 shadow-sm">
          <Spinner size={8} />
          <p className="text-xs text-gray-400 mt-3 font-medium">Fetching your orders...</p>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center h-64 space-y-3 text-center bg-white rounded-3xl border border-red-200 p-8 shadow-sm">
          <XCircle className="h-10 w-10 text-red-500" />
          <p className="text-sm text-red-600 font-medium">{error}</p>
          <button
            onClick={fetchOrders}
            className="px-4 py-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-700 text-xs font-bold transition"
          >
            Try Again
          </button>
        </div>
      ) : filteredOrders.length === 0 ? (
        <div className="bg-white border border-gray-200/90 rounded-3xl p-12 text-center space-y-4 shadow-sm">
          <div className="h-16 w-16 rounded-2xl bg-emerald-50 flex items-center justify-center mx-auto text-3xl">
            📦
          </div>
          <div>
            <h3 className="font-black text-gray-900 text-lg">
              {filter === 'ALL' ? 'No orders placed yet' : `No ${filter.toLowerCase()} orders found`}
            </h3>
            <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
              {filter === 'ALL'
                ? 'Explore our premium organic jaggery catalogue directly produced from our farm mills.'
                : 'Switch filters or view all orders to see your full order history.'}
            </p>
          </div>
          {filter === 'ALL' ? (
            <button
              onClick={onBackToStore}
              className="mt-2 px-6 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-xs transition shadow-md"
            >
              Browse Catalogue
            </button>
          ) : (
            <button
              onClick={() => setFilter('ALL')}
              className="mt-2 px-6 py-2.5 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs transition"
            >
              Show All Orders
            </button>
          )}
        </div>
      ) : (
        /* ── Individual Distinct Order Cards ── */
        <div className="space-y-7 sm:space-y-8">
          {filteredOrders.map(order => {
            const isCancellable = order.status === 'PENDING' || order.status === 'CONFIRMED';

            const statusConfig: Record<string, {
              borderLeft: string;
              headerBg: string;
              badgeBg: string;
              badgeText: string;
              badgeBorder: string;
              label: string;
              stepIndex: number;
            }> = {
              PENDING: {
                borderLeft: 'border-l-amber-500',
                headerBg: 'bg-gradient-to-r from-amber-50/70 via-stone-50/50 to-white',
                badgeBg: 'bg-amber-100/90',
                badgeText: 'text-amber-800',
                badgeBorder: 'border-amber-300',
                label: 'Order Placed',
                stepIndex: 1,
              },
              CONFIRMED: {
                borderLeft: 'border-l-blue-500',
                headerBg: 'bg-gradient-to-r from-blue-50/70 via-stone-50/50 to-white',
                badgeBg: 'bg-blue-100/90',
                badgeText: 'text-blue-800',
                badgeBorder: 'border-blue-300',
                label: 'Order Confirmed',
                stepIndex: 2,
              },
              PROCESSING: {
                borderLeft: 'border-l-purple-500',
                headerBg: 'bg-gradient-to-r from-purple-50/70 via-stone-50/50 to-white',
                badgeBg: 'bg-purple-100/90',
                badgeText: 'text-purple-800',
                badgeBorder: 'border-purple-300',
                label: 'Processing in Mill',
                stepIndex: 2,
              },
              DISPATCHED: {
                borderLeft: 'border-l-teal-500',
                headerBg: 'bg-gradient-to-r from-teal-50/70 via-stone-50/50 to-white',
                badgeBg: 'bg-teal-100/90',
                badgeText: 'text-teal-800',
                badgeBorder: 'border-teal-300',
                label: 'Out for Delivery',
                stepIndex: 3,
              },
              DELIVERED: {
                borderLeft: 'border-l-emerald-600',
                headerBg: 'bg-gradient-to-r from-emerald-50/70 via-stone-50/50 to-white',
                badgeBg: 'bg-emerald-100/90',
                badgeText: 'text-emerald-800',
                badgeBorder: 'border-emerald-300',
                label: 'Delivered',
                stepIndex: 4,
              },
              CANCELLED: {
                borderLeft: 'border-l-red-500',
                headerBg: 'bg-gradient-to-r from-red-50/60 via-stone-50/50 to-white',
                badgeBg: 'bg-red-100/90',
                badgeText: 'text-red-800',
                badgeBorder: 'border-red-300',
                label: 'Cancelled',
                stepIndex: -1,
              },
            };

            const cfg = statusConfig[order.status] || {
              borderLeft: 'border-l-gray-400',
              headerBg: 'bg-gradient-to-r from-gray-50 via-stone-50/50 to-white',
              badgeBg: 'bg-gray-100',
              badgeText: 'text-gray-800',
              badgeBorder: 'border-gray-300',
              label: order.status,
              stepIndex: 1,
            };

            const isPaid = order.paymentStatus === 'PAID';
            const orderDate = new Date(order.createdAt);
            const formattedDate = orderDate.toLocaleDateString('en-IN', {
              day: 'numeric',
              month: 'short',
              year: 'numeric',
            });
            const formattedTime = orderDate.toLocaleTimeString('en-IN', {
              hour: '2-digit',
              minute: '2-digit',
              hour12: true,
            });

            return (
              <div
                key={order.id}
                id={`order-card-${order.orderNumber}`}
                className={`bg-white rounded-3xl border-2 border-stone-200/90 shadow-[0_6px_25px_-5px_rgba(0,0,0,0.08)] hover:shadow-[0_14px_35px_-8px_rgba(0,0,0,0.13)] transition-all duration-300 overflow-hidden border-l-[7px] sm:border-l-[10px] ${cfg.borderLeft}`}
              >
                {/* ── 1. Top Card Bar ── */}
                <div className={`${cfg.headerBg} px-5 sm:px-7 py-4 sm:py-5 border-b border-stone-200/80 flex flex-col md:flex-row md:items-center justify-between gap-4`}>
                  <div className="flex items-start sm:items-center gap-3.5">
                    <div className="h-11 w-11 rounded-2xl bg-white border border-stone-200/90 shadow-xs flex items-center justify-center shrink-0">
                      <Package className="h-5 w-5 text-emerald-800" />
                    </div>
                    <div>
                      <div className="flex items-center flex-wrap gap-2">
                        <span className="font-mono font-black text-gray-900 text-sm sm:text-base tracking-wide bg-white px-2.5 py-0.5 rounded-lg border border-stone-200/80 shadow-2xs">
                          {order.orderNumber}
                        </span>
                        {/* Status Badge */}
                        <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-extrabold border ${cfg.badgeBg} ${cfg.badgeText} ${cfg.badgeBorder}`}>
                          {order.status === 'PENDING' && (
                            <span className="relative flex h-2 w-2">
                              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
                              <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
                            </span>
                          )}
                          {order.status === 'CONFIRMED' && <CheckCircle2 className="h-3.5 w-3.5" />}
                          {order.status === 'DISPATCHED' && <Truck className="h-3.5 w-3.5" />}
                          {order.status === 'DELIVERED' && <Check className="h-3.5 w-3.5" />}
                          {order.status === 'CANCELLED' && <XCircle className="h-3.5 w-3.5" />}
                          {cfg.label}
                        </span>
                        {/* Payment Pill */}
                        <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold border ${isPaid
                          ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                          : 'bg-amber-50 text-amber-800 border-amber-200'
                          }`}>
                          <CreditCard className="h-3 w-3" />
                          {isPaid ? (order.paymentMethod === 'RAZORPAY' ? 'Paid via Razorpay' : 'Paid') : 'Cash on Delivery'}
                        </span>
                      </div>
                      <p className="text-[11px] sm:text-xs text-gray-500 mt-1 flex items-center gap-1.5">
                        <Clock className="h-3 w-3 text-gray-400" />
                        Placed on {formattedDate} at {formattedTime}
                      </p>
                    </div>
                  </div>

                  {/* Actions (Invoice & Cancel) */}
                  <div className="flex items-center gap-2 self-start md:self-center shrink-0">
                    <button
                      id={`view-invoice-${order.orderNumber}`}
                      onClick={() => onViewInvoice(order.id)}
                      className="px-4 py-2 rounded-xl bg-white hover:bg-emerald-50 text-emerald-800 hover:text-emerald-900 text-xs font-bold border border-emerald-300 hover:border-emerald-500 flex items-center gap-1.5 shadow-2xs hover:shadow-xs transition"
                    >
                      <FileText className="h-3.5 w-3.5 text-emerald-700" />
                      View Invoice
                    </button>
                    {isCancellable && (
                      <button
                        id={`cancel-order-${order.orderNumber}`}
                        onClick={() => handleCancelOrder(order.id)}
                        className="px-3.5 py-2 rounded-xl bg-white hover:bg-red-50 text-red-600 hover:text-red-700 text-xs font-bold border border-red-200 hover:border-red-300 flex items-center gap-1.5 shadow-2xs hover:shadow-xs transition"
                      >
                        <X className="h-3.5 w-3.5" />
                        Cancel Order
                      </button>
                    )}
                  </div>
                </div>

                {/* ── 2. Visual Progress Timeline (Only for non-cancelled orders) ── */}
                {order.status !== 'CANCELLED' ? (
                  <div className="px-5 sm:px-7 py-3 bg-stone-50/50 border-b border-stone-200/60">
                    <div className="max-w-2xl mx-auto py-1">
                      <div className="relative flex items-center justify-between">
                        {/* Connecting track */}
                        <div className="absolute left-0 right-0 top-1/2 -translate-y-1/2 h-1 bg-stone-200 z-0"></div>
                        {/* Active Progress Line */}
                        <div
                          className="absolute left-0 top-1/2 -translate-y-1/2 h-1 bg-emerald-600 transition-all duration-500 z-0"
                          style={{
                            width:
                              cfg.stepIndex === 1
                                ? '15%'
                                : cfg.stepIndex === 2
                                  ? '45%'
                                  : cfg.stepIndex === 3
                                    ? '75%'
                                    : '100%',
                          }}
                        ></div>

                        {/* Step 1: Placed */}
                        <div className="relative z-10 flex flex-col items-center">
                          <div className="h-7 w-7 rounded-full bg-emerald-600 text-white flex items-center justify-center shadow-xs text-xs font-bold">
                            ✓
                          </div>
                          <span className="text-[10px] font-bold text-gray-800 mt-1">Placed</span>
                        </div>

                        {/* Step 2: Confirmed */}
                        <div className="relative z-10 flex flex-col items-center">
                          <div
                            className={`h-7 w-7 rounded-full flex items-center justify-center shadow-xs text-xs font-bold transition-colors ${cfg.stepIndex >= 2
                              ? 'bg-emerald-600 text-white'
                              : 'bg-white border-2 border-stone-300 text-stone-400'
                              }`}
                          >
                            {cfg.stepIndex >= 2 ? '✓' : '2'}
                          </div>
                          <span className={`text-[10px] font-bold mt-1 ${cfg.stepIndex >= 2 ? 'text-gray-800' : 'text-stone-400'}`}>
                            Confirmed
                          </span>
                        </div>

                        {/* Step 3: Out for Delivery */}
                        <div className="relative z-10 flex flex-col items-center">
                          <div
                            className={`h-7 w-7 rounded-full flex items-center justify-center shadow-xs text-xs font-bold transition-colors ${cfg.stepIndex >= 3
                              ? 'bg-emerald-600 text-white'
                              : 'bg-white border-2 border-stone-300 text-stone-400'
                              }`}
                          >
                            {cfg.stepIndex >= 3 ? '✓' : '3'}
                          </div>
                          <span className={`text-[10px] font-bold mt-1 ${cfg.stepIndex >= 3 ? 'text-gray-800' : 'text-stone-400'}`}>
                            Dispatched
                          </span>
                        </div>

                        {/* Step 4: Delivered */}
                        <div className="relative z-10 flex flex-col items-center">
                          <div
                            className={`h-7 w-7 rounded-full flex items-center justify-center shadow-xs text-xs font-bold transition-colors ${cfg.stepIndex >= 4
                              ? 'bg-emerald-600 text-white'
                              : 'bg-white border-2 border-stone-300 text-stone-400'
                              }`}
                          >
                            {cfg.stepIndex >= 4 ? '✓' : '4'}
                          </div>
                          <span className={`text-[10px] font-bold mt-1 ${cfg.stepIndex >= 4 ? 'text-gray-800' : 'text-stone-400'}`}>
                            Delivered
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="px-5 sm:px-7 py-2.5 bg-red-50/70 border-b border-red-200/70 flex items-center gap-2 text-xs text-red-700 font-semibold">
                    <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
                    <span>This order has been cancelled and will not be dispatched.</span>
                  </div>
                )}

                {/* ── 3. Order Items Section ── */}
                <div className="px-5 sm:px-7 py-5">
                  <div className="text-[11px] font-black uppercase tracking-wider text-gray-400 mb-3 flex items-center gap-2">
                    <Tag className="h-3.5 w-3.5 text-gray-400" />
                    Ordered Items ({order.items.length})
                  </div>

                  <div className="bg-stone-50/70 rounded-2xl p-3 sm:p-4 border border-stone-200/80 divide-y divide-stone-200/60">
                    {order.items.map(item => (
                      <div
                        key={item.id}
                        className="flex items-center justify-between gap-4 py-2.5 first:pt-0 last:pb-0"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className="h-10 w-10 rounded-xl bg-amber-100/70 border border-amber-200/70 flex items-center justify-center shrink-0 text-lg shadow-2xs">
                            🍯
                          </div>
                          <div className="min-w-0">
                            <h4 className="font-bold text-gray-900 text-sm truncate">
                              {item.productName}
                            </h4>
                            <div className="flex items-center flex-wrap gap-2 text-[11px] text-gray-500 mt-0.5">
                              <span className="font-mono bg-white px-1.5 py-0.5 rounded border border-stone-200">
                                SKU: {item.sku}
                              </span>
                              <span>•</span>
                              <span className="font-medium text-gray-600">
                                Qty: <strong className="text-gray-900">{item.quantity}</strong>
                              </span>
                              <span>•</span>
                              <span className="text-gray-500">
                                {formatPrice(item.unitPrice)} each
                              </span>
                            </div>
                          </div>
                        </div>

                        <div className="text-right shrink-0">
                          <div className="text-sm font-black text-emerald-800">
                            {formatPrice(item.lineTotal)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Shipping Address Sneak Peek if available */}
                  {order.shippingAddress && (
                    <div className="mt-3.5 pt-3 border-t border-stone-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs text-gray-500">
                      <div className="flex items-start gap-2">
                        <MapPin className="h-3.5 w-3.5 text-emerald-700 shrink-0 mt-0.5" />
                        <div>
                          <span className="font-bold text-gray-700">Delivery Address: </span>
                          <span>
                            {order.shippingAddress.recipientName}
                            {order.shippingAddress.apartmentName ? `, ${order.shippingAddress.apartmentName}` : ''}
                            {order.shippingAddress.streetAddress ? `, ${order.shippingAddress.streetAddress}` : ''}
                            {order.shippingAddress.city ? `, ${order.shippingAddress.city}` : ''}
                            {order.shippingAddress.postalCode ? ` - ${order.shippingAddress.postalCode}` : ''}
                            {order.shippingAddress.nearestLandmark ? ` (${order.shippingAddress.nearestLandmark})` : ''}
                          </span>
                        </div>
                      </div>
                      {extractLocationLink(order.shippingAddress.nearestLandmark, order.notes) && (
                        <a
                          href={extractLocationLink(order.shippingAddress.nearestLandmark, order.notes)!}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-800 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200 hover:bg-emerald-100 shrink-0 self-start sm:self-center transition"
                        >
                          <MapPin className="h-3 w-3 text-emerald-700" />
                          <span>View Location Map</span>
                          <ExternalLink className="h-3 w-3" />
                        </a>
                      )}
                    </div>
                  )}
                </div>

                {/* ── 4. Card Bottom Summary Bar ── */}
                <div className="px-5 sm:px-7 py-4 bg-gradient-to-r from-stone-100/90 via-stone-50 to-stone-100/70 border-t border-stone-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex flex-wrap items-center gap-2 sm:gap-4 text-xs text-gray-500 font-medium">
                    <span>
                      Items Subtotal: <strong className="text-gray-800">{formatPrice(order.subtotalAmount || (order.totalAmount - (order.shippingAmount || 50) - (order.packagingFee || 10)))}</strong>
                    </span>
                    <span className="text-stone-300">•</span>
                    <span>
                      Delivery: <strong className="text-gray-800">{formatPrice(order.shippingAmount || 50)}</strong>
                    </span>
                    <span className="text-stone-300">•</span>
                    <span>
                      Packaging: <strong className="text-gray-800">{formatPrice(order.packagingFee || 10)}</strong>
                    </span>
                  </div>

                  <div className="flex items-center gap-2 self-end sm:self-center">
                    <span className="text-xs uppercase font-extrabold tracking-wider text-gray-500">
                      Order Total:
                    </span>
                    <span className="text-lg sm:text-xl font-black text-emerald-800 font-mono bg-white px-3 py-1 rounded-xl border border-stone-200/90 shadow-2xs">
                      {formatPrice(order.totalAmount)}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
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
      const body = await res.json().catch(() => ({}));
      if (res.ok && body.success) {
        fetchOrders();
      } else {
        alert(body.error?.message || body.message || 'The quantity of this product in the order is more than what we have in our stock kindly add production then try again to mark it delivered.');
        fetchOrders();
      }
    } catch (err: any) {
      alert(err.message || 'Error updating order status.');
      fetchOrders();
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <Receipt className="h-5 w-5 text-emerald-700" /> Commercial Order Fulfillment Console
          </h2>
          <p className="text-xs text-gray-500 mt-1">Manage online orders, transition fulfillment lifecycle, inspect invoices (ROLE_ADMIN)</p>
        </div>
        <div className="flex items-center gap-3">
          <select
            id="admin-order-status-filter"
            value={selectedStatus}
            onChange={e => setSelectedStatus(e.target.value)}
            className="bg-white border border-gray-300 rounded-xl px-3 py-2 text-xs text-gray-900 focus:outline-none focus:border-emerald-600"
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="PROCESSING">Processing</option>
            <option value="DISPATCHED">Dispatched</option>
            <option value="DELIVERED">Delivered</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button onClick={fetchOrders} className="p-2 bg-white border border-gray-200 rounded-xl hover:bg-gray-100 transition" title="Refresh">
            <RefreshCw className="h-4 w-4 text-emerald-700" />
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64"><Spinner size={8} /></div>
      ) : error ? (
        <div className="text-center py-12 text-red-600">{error}</div>
      ) : orders.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-12 text-center text-gray-500 text-xs">
          No customer orders found matching filter.
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-gray-500 font-semibold">Order Number</th>
                  <th className="text-left px-4 py-3 text-gray-500 font-semibold">Customer</th>
                  <th className="text-left px-4 py-3 text-gray-500 font-semibold">Type</th>
                  <th className="text-right px-4 py-3 text-gray-500 font-semibold">Amount</th>
                  <th className="text-center px-4 py-3 text-gray-500 font-semibold">Payment</th>
                  <th className="text-center px-4 py-3 text-gray-500 font-semibold">Fulfillment Stage</th>
                  <th className="text-center px-4 py-3 text-gray-500 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map(o => (
                  <tr key={o.id} id={`admin-order-row-${o.orderNumber}`} className="border-b border-gray-200 hover:bg-gray-100/30 transition">
                    <td className="px-4 py-3 font-mono font-bold text-emerald-700">{o.orderNumber}</td>
                    <td className="px-4 py-3">
                      <div className="text-gray-900 font-medium">{o.customerName}</div>
                      <div className="text-[11px] text-gray-500">{o.customerEmail}</div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-[10px] px-2 py-0.5 rounded bg-gray-100 text-gray-700 font-bold">{o.customerType}</span>
                    </td>
                    <td className="px-4 py-3 text-right font-bold text-gray-900">{formatPrice(o.totalAmount)}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${o.paymentStatus === 'PAID' ? 'bg-emerald-500/10 text-emerald-700' : 'bg-emerald-700/10 text-emerald-700'}`}>
                        {o.paymentStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <select
                        id={`select-status-${o.orderNumber}`}
                        value={o.status}
                        onChange={e => handleStatusChange(o.id, e.target.value)}
                        className="bg-white border border-gray-300 rounded-lg px-2.5 py-1 text-xs text-gray-900 focus:outline-none focus:border-emerald-600"
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
                        className="p-1.5 bg-gray-100 hover:bg-emerald-100 text-gray-700 rounded-lg transition"
                        title="View Commercial Invoice"
                      >
                        <FileText className="h-3.5 w-3.5 text-emerald-700" />
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
  onAddToCart,
  onNavigate,
  searchQuery = '',
  onSearchChange,
}: {
  auth: AuthState;
  onAddToCart: (productId: string, quantity: number) => void;
  onNavigate?: (page: StorefrontPage) => void;
  searchQuery?: string;
  onSearchChange?: (q: string) => void;
}) {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [localQuery, setLocalQuery] = useState('');
  const query = onSearchChange ? searchQuery : localQuery;
  const setQuery = onSearchChange || setLocalQuery;
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeModalProduct, setActiveModalProduct] = useState<Product | null>(null);
  const [cardQuantities] = useState<Record<string, number>>({});
  const [modalQty, setModalQty] = useState<number>(1);

  const isWholesale = auth.user?.customerType === 'REGISTERED_WHOLESALE';

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [catRes, prodRes, revRes] = await Promise.all([
        fetch(`${COMMERCE_BASE}/categories`),
        fetch(`${COMMERCE_BASE}/products`),
        fetch(`${COMMERCE_BASE}/reviews`),
      ]);

      const [catBody, prodBody, revBody] = await Promise.all([catRes.json(), prodRes.json(), revRes.json()]);
      if (catBody.success) setCategories(catBody.data);
      if (prodBody.success) setProducts(prodBody.data);
      if (revBody.success) setReviews(revBody.data);
    } catch {
      setError('Cannot reach Commerce Service. Please check if services are running.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const displayProducts = useMemo(() => {
    return products.filter((p) => {
      const matchCat = !selectedCategory || p.categoryId === selectedCategory;
      const q = query.trim().toLowerCase();
      const matchQuery = !q ||
        p.name.toLowerCase().includes(q) ||
        (p.description && p.description.toLowerCase().includes(q)) ||
        (p.sku && p.sku.toLowerCase().includes(q));
      return matchCat && matchQuery;
    });
  }, [products, selectedCategory, query]);

  const getProductAverageRating = (productId: string) => {
    const prodReviews = reviews.filter(r => r.productId === productId && r.status === 'APPROVED');
    if (prodReviews.length === 0) return { avg: 4.8, count: 12 };
    const avg = prodReviews.reduce((sum, r) => sum + r.rating, 0) / prodReviews.length;
    return { avg: Math.round(avg * 10) / 10, count: prodReviews.length };
  };

  return (
    <div className="min-h-screen bg-emerald-50/30 text-gray-800 pb-16 font-sans">
      {/* Organic Light Hero Banner */}
      <section className="relative overflow-hidden hero-organic py-6 sm:py-10 lg:py-16 px-3 sm:px-6 lg:px-8 border-b border-emerald-200">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-5 sm:gap-8 items-center">
          <div className="lg:col-span-7 space-y-3 sm:space-y-6">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-[10px] sm:text-xs font-bold tracking-wide uppercase">
              <Sparkles className="h-3 w-3 text-emerald-700" /> 20+ Years of Excellence · 100% Chemical Free ORGANIC JAGGERY
            </div>
            <h1 className="text-2xl sm:text-4xl lg:text-6xl font-extrabold tracking-tight text-emerald-950 font-serif leading-tight">
              100% Naturally Made,<span className="text-emerald-700"> Organic</span> Jaggery
            </h1>
            <p className="text-xs sm:text-base lg:text-lg text-gray-600 max-w-2xl leading-relaxed">
              With over 20+ years of hands-on experience, we craft 100% chemical-free, hygienic jaggery made with authentic heritage care and uncompromising purity.
            </p>

            <div className="flex flex-wrap items-center gap-2.5 sm:gap-4 pt-1 sm:pt-2">
              <a
                href="#catalog-section"
                className="px-4 py-2.5 sm:px-6 sm:py-3.5 rounded-xl organic-gradient text-white font-bold text-xs sm:text-sm shadow-md hover:shadow-lg transition transform hover:-translate-y-0.5 flex items-center gap-2"
              >
                Explore Products <ArrowRight className="h-4 w-4" />
              </a>
              {onNavigate && (
                <button
                  onClick={() => onNavigate('about')}
                  className="px-4 py-2.5 sm:px-6 sm:py-3.5 rounded-xl bg-white text-emerald-800 font-bold text-xs sm:text-sm border border-emerald-200 hover:bg-emerald-50 transition shadow-sm"
                >
                  Our Heritage
                </button>
              )}
            </div>

            {/* Feature Pills */}
            <div className="grid grid-cols-3 gap-2 sm:gap-4 pt-3 sm:pt-6 border-t border-emerald-200/60 text-[10px] sm:text-xs text-gray-600">
              <div className="flex items-center gap-1.5 sm:gap-2 font-medium">
                <Award className="h-4 w-4 sm:h-5 sm:w-5 text-emerald-700 shrink-0" />
                <span className="truncate">20+ Years Legacy</span>
              </div>
              <div className="flex items-center gap-1.5 sm:gap-2 font-medium">
                <Shield className="h-4 w-4 sm:h-5 sm:w-5 text-emerald-600 shrink-0" />
                <span className="truncate">100% Chemical Free</span>
              </div>
              <div className="flex items-center gap-1.5 sm:gap-2 font-medium">
                <Truck className="h-4 w-4 sm:h-5 sm:w-5 text-amber-600 shrink-0" />
                <span className="truncate">Express Shipping</span>
              </div>
            </div>
          </div>

          <div className="lg:col-span-5 relative flex justify-center lg:justify-end items-center">
            <div className="relative w-64 h-64 sm:w-80 sm:h-80 lg:w-[420px] lg:h-[420px] xl:w-[460px] xl:h-[460px] aspect-square rounded-3xl overflow-hidden shadow-2xl border-4 border-white ring-2 ring-emerald-200/90 bg-white flex items-center justify-center p-3 sm:p-4">
              <img
                src="/RR_Jaggery_Logo.png"
                alt="RR Jaggery - 100% Naturally Made Traditional Jaggery"
                className="w-full h-full object-contain rounded-2xl transform hover:scale-105 transition duration-500 drop-shadow-sm"
              />
            </div>
          </div>
        </div>
      </section>

      {/* ─── Why Choose RR Jaggery — Homepage Highlights ─── */}
      <section className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 pt-8 sm:pt-14 pb-4 sm:pb-8">
        <div className="text-center space-y-2 mb-6 sm:mb-8">
          <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-900 text-[11px] font-bold tracking-wide border border-emerald-300 inline-flex items-center gap-1.5">
            <Sparkles className="h-3 w-3 text-amber-600" />
            20+ YEARS OF EXCELLENCE · 100% CHEMICAL FREE ORGANIC JAGGERY
          </span>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif leading-tight">
            Real Manufacturers, <span className="text-emerald-700">Not Just Resellers</span>
          </h2>
          <p className="text-xs sm:text-sm text-gray-600 max-w-2xl mx-auto">
            With 20+ years of hands-on experience, RR Jaggery makes 100% chemical-free Organic jaggery in our own facility — we are direct Manufacturers, not traders. Every batch is made under our direct care from farm to your kitchen.
          </p>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-5">
          {/* Card 1 */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 border border-emerald-200 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300 space-y-2.5">
            <div className="p-2.5 w-fit rounded-xl bg-emerald-100 text-emerald-800 border border-emerald-300">
              <Factory className="h-5 w-5" />
            </div>
            <h3 className="font-extrabold text-gray-900 text-sm sm:text-base">Quality Control</h3>
            <p className="text-[11px] sm:text-xs text-gray-600 leading-relaxed">
              20+ years making our own jaggery, not reselling. Direct chemical-free control at every step gives you 100% pure jaggery.
            </p>
          </div>

          {/* Card 2 */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 border border-emerald-200 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300 space-y-2.5">
            <div className="p-2.5 w-fit rounded-xl bg-emerald-100 text-emerald-800 border border-emerald-300">
              <Award className="h-5 w-5" />
            </div>
            <h3 className="font-extrabold text-gray-900 text-sm sm:text-base">Jaggery Specialists</h3>
            <p className="text-[11px] sm:text-xs text-gray-600 leading-relaxed">
              We only make 100% chemical-free and Organic— Jaggery cubes, Jaggery powder, Liquid Jaggery & solid blocks made with pure care.
            </p>
          </div>

          {/* Card 3 */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 border border-emerald-200 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300 space-y-2.5">
            <div className="p-2.5 w-fit rounded-xl bg-emerald-100 text-emerald-800 border border-emerald-300">
              <Leaf className="h-5 w-5" />
            </div>
            <h3 className="font-extrabold text-gray-900 text-sm sm:text-base">Premium Sugarcane</h3>
            <p className="text-[11px] sm:text-xs text-gray-600 leading-relaxed">
              Sourced directly from Krishna river belt farmers at premium price for the finest cane.
            </p>
          </div>

          {/* Card 4 */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 border border-emerald-200 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300 space-y-2.5">
            <div className="p-2.5 w-fit rounded-xl bg-emerald-100 text-emerald-800 border border-emerald-300">
              <Shield className="h-5 w-5" />
            </div>
            <h3 className="font-extrabold text-gray-900 text-sm sm:text-base">Zero Bacteria</h3>
            <p className="text-[11px] sm:text-xs text-gray-600 leading-relaxed">
              Chemical-free processing — each cane washed with pure water to achieve certified nil bacterial count.
            </p>
          </div>
        </div>

        {/* Quick Nutrition Strip + CTA */}
        <div className="mt-5 sm:mt-8 bg-gradient-to-r from-emerald-950 via-emerald-900 to-teal-950 rounded-2xl p-4 sm:p-6 flex flex-col sm:flex-row items-center justify-between gap-4 border-2 border-emerald-700/40 shadow-lg">
          <div className="flex flex-wrap items-center justify-center sm:justify-start gap-3 sm:gap-5">
            <div className="text-center px-3">
              <span className="text-lg sm:text-xl font-black text-amber-400">20+</span>
              <span className="block text-[10px] sm:text-xs text-emerald-200 font-medium">Years Experience</span>
            </div>
            <div className="w-px h-8 bg-emerald-700/60 hidden sm:block"></div>
            <div className="text-center px-3">
              <span className="text-lg sm:text-xl font-black text-emerald-400">100%</span>
              <span className="block text-[10px] sm:text-xs text-emerald-200 font-medium">Chemical Free</span>
            </div>
            <div className="w-px h-8 bg-emerald-700/60 hidden sm:block"></div>
            <div className="text-center px-3">
              <span className="text-lg sm:text-xl font-black text-amber-400">282</span>
              <span className="block text-[10px] sm:text-xs text-emerald-200 font-medium">Kcal / 100g</span>
            </div>
            <div className="w-px h-8 bg-emerald-700/60 hidden sm:block"></div>
            <div className="text-center px-3">
              <span className="text-lg sm:text-xl font-black text-emerald-400">Nil</span>
              <span className="block text-[10px] sm:text-xs text-emerald-200 font-medium">Fat Content</span>
            </div>
          </div>
          {onNavigate && (
            <button
              onClick={() => onNavigate('about')}
              className="px-5 py-2.5 rounded-full bg-amber-500 hover:bg-amber-400 text-emerald-950 font-bold text-xs sm:text-sm shadow-lg hover:shadow-xl transition transform hover:-translate-y-0.5 flex items-center gap-2 whitespace-nowrap shrink-0"
            >
              <span>Our Heritage &amp; Story</span>
              <ChevronRight className="h-4 w-4" />
            </button>
          )}
        </div>
      </section>

      {/* Main Content Area */}
      <div className="bg-[#e8f5f1] pb-12 border-t border-emerald-100/60">
        <div id="catalog-section" className="max-w-7xl mx-auto px-2 sm:px-6 lg:px-8 pt-4 sm:pt-10">
          {/* Flipkart-Style Horizontal Category Strip (Mobile & Tablet) */}
          <div className="mb-3 sm:mb-6">
            <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-1 scroll-smooth">
              <button
                id="category-pill-all"
                onClick={() => setSelectedCategory(null)}
                className={`px-3.5 py-2 rounded-full text-xs font-bold whitespace-nowrap shrink-0 transition-all flex items-center gap-1.5 shadow-sm ${!selectedCategory
                  ? 'bg-emerald-700 text-white shadow-emerald-700/20 ring-2 ring-emerald-500/40'
                  : 'bg-white text-gray-700 border border-gray-200 hover:bg-emerald-50'
                  }`}
              >
                <Leaf className="h-3.5 w-3.5" />
                <span>All Products</span>
              </button>
              {categories.map((cat) => (
                <button
                  key={cat.id}
                  id={`category-pill-${cat.slug || cat.id}`}
                  onClick={() => setSelectedCategory(cat.id)}
                  className={`px-3.5 py-2 rounded-full text-xs font-bold whitespace-nowrap shrink-0 transition-all shadow-sm ${selectedCategory === cat.id
                    ? 'bg-emerald-700 text-white shadow-emerald-700/20 ring-2 ring-emerald-500/40'
                    : 'bg-white text-gray-700 border border-gray-200 hover:bg-emerald-50'
                    }`}
                >
                  <span>{cat.name}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Filter Controls & Search */}
          <div className="bg-white rounded-2xl p-3 sm:p-6 shadow-sm border border-emerald-200 mb-4 sm:mb-8 space-y-3 sm:space-y-4">
            <div className="flex flex-col md:flex-row gap-3 sm:gap-4 items-center justify-between">
              <div className="relative w-full md:w-80">
                <Search className="absolute left-3.5 top-3 h-4 w-4 text-emerald-700" />
                <input
                  id="catalogue-search"
                  type="text"
                  placeholder="Search jaggery powder, blocks, syrup..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200 text-sm outline-none transition bg-[#f2f8f3]"
                />
              </div>

              {/* Filter Dropdowns: Category */}
              <div className="flex flex-wrap sm:flex-nowrap items-center gap-2 sm:gap-3 w-full md:w-auto">
                {/* Category Dropdown */}
                <div className="flex items-center gap-2 w-full sm:w-auto flex-1 sm:flex-initial">
                  <span className="text-xs text-gray-600 font-semibold shrink-0">Category:</span>
                  <select
                    id="select-category"
                    value={selectedCategory || ''}
                    onChange={(e) => setSelectedCategory(e.target.value || null)}
                    className="px-3 py-2 rounded-xl border border-gray-200 bg-[#f2f8f3] text-xs font-medium text-gray-900 focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none w-full sm:w-auto cursor-pointer"
                  >
                    <option value="">All Categories</option>
                    {categories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
          </div>

          {/* Products Grid */}
          {loading ? (
            <div className="py-20 text-center">
              <Loader2 className="h-10 w-10 text-emerald-600 animate-spin mx-auto mb-4" />
              <p className="text-sm font-medium text-gray-500">Fetching our finest natural jaggery...</p>
            </div>
          ) : error ? (
            <div className="bg-red-50 border border-red-200 text-red-700 p-6 rounded-2xl text-center max-w-lg mx-auto">
              <AlertCircle className="h-8 w-8 mx-auto mb-2 text-red-500" />
              <p className="font-semibold text-sm">{error}</p>
              <button
                onClick={fetchData}
                className="mt-4 px-4 py-2 bg-red-600 text-white text-xs font-bold rounded-xl hover:bg-red-700 transition"
              >
                Try Again
              </button>
            </div>
          ) : displayProducts.length === 0 ? (
            <div className="bg-white rounded-2xl p-12 text-center border border-emerald-200 max-w-md mx-auto my-8">
              <Package className="h-12 w-12 text-emerald-300 mx-auto mb-3" />
              <h3 className="text-lg font-bold text-gray-800">No products found</h3>
              <p className="text-xs text-gray-500 mt-1">Try clearing filters or searching for another term.</p>
              <button
                onClick={() => { setSelectedCategory(null); setQuery(''); }}
                className="mt-4 px-4 py-2 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-xl hover:bg-emerald-200 transition"
              >
                Reset All Filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 gap-2.5 sm:gap-4 lg:gap-5">
              {displayProducts.map((product) => {
                const { avg, count } = getProductAverageRating(product.id);
                const price = isWholesale ? product.wholesalePrice : product.retailPrice;
                const qty = cardQuantities[product.id] || (isWholesale ? product.wholesaleMoq || 1 : 1);

                return (
                  <div
                    key={product.id}
                    id={`product-card-${product.sku}`}
                    className="sf-card group bg-white rounded-xl sm:rounded-2xl overflow-hidden border border-emerald-200/80 shadow-sm hover:shadow-md transition-all duration-300 flex flex-col justify-between"
                  >
                    <div>
                      <div className="relative aspect-square bg-gradient-to-br from-amber-50/60 to-emerald-50/40 overflow-hidden cursor-pointer" onClick={() => setActiveModalProduct(product)}>
                        <img
                          src={product.imageUrl || 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&q=80&w=600'}
                          alt={product.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                          loading="lazy"
                        />
                        <div className="absolute top-1.5 left-1.5 sm:top-2.5 sm:left-2.5 flex flex-col gap-1 items-start">
                          {product.featured && (
                            <span className="px-1.5 py-0.5 sm:px-2 rounded text-[8px] sm:text-[10px] font-bold tracking-wider bg-amber-500 text-gray-950 shadow-sm">
                              BESTSELLER
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="p-2 sm:p-3.5 lg:p-4 space-y-1.5 sm:space-y-2">
                        <div className="flex items-center justify-between gap-1">
                          <span className="text-[9px] sm:text-[10px] font-semibold text-emerald-800 bg-emerald-50 px-1.5 py-0.5 rounded truncate">
                            {product.categoryName || 'Pure Jaggery'}
                          </span>
                          <div className="flex items-center gap-0.5 sm:gap-1 bg-emerald-700 text-white px-1 sm:px-1.5 py-0.5 rounded text-[9px] sm:text-[10px] font-bold shrink-0">
                            <span>{avg}</span>
                            <Star className="h-2 w-2 sm:h-2.5 sm:w-2.5 fill-white text-white" />
                            <span className="text-emerald-100 font-normal">({count})</span>
                          </div>
                        </div>

                        <h3
                          onClick={() => setActiveModalProduct(product)}
                          className="font-bold text-gray-900 text-xs sm:text-sm group-hover:text-emerald-700 transition line-clamp-2 leading-snug cursor-pointer min-h-[2rem] sm:min-h-[2.5rem]"
                          title={product.name}
                        >
                          {product.name}
                        </h3>

                        <div className="text-[9px] sm:text-[10px] text-gray-500 flex items-center gap-1">
                          <span className="bg-gray-100 px-1 py-0.5 rounded font-medium text-gray-700 truncate">{product.packageType || '1 kg Block'}</span>
                          <span>•</span>
                          <span>{product.unitWeightKg} {product.unit || 'kg'}</span>
                        </div>
                      </div>
                    </div>

                    <div className="p-2 sm:p-3.5 lg:p-4 pt-0">
                      <div className="pt-2 border-t border-gray-100 flex flex-col xs:flex-row xs:items-center justify-between gap-1.5 sm:gap-2">
                        <div className="min-w-0">
                          <span className="text-[8px] sm:text-[9px] text-gray-400 uppercase font-semibold block truncate">
                            {isWholesale ? 'Wholesale Price' : 'Price'}
                          </span>
                          <div className="flex items-baseline gap-1">
                            <span className="text-sm sm:text-base lg:text-lg font-black text-emerald-800 truncate block">
                              {formatPrice(price)}
                            </span>
                            <span className="text-[9px] text-gray-400 line-through hidden xs:inline">
                              {formatPrice(Math.round(price * 1.25))}
                            </span>
                          </div>
                        </div>

                        <div className="flex items-center gap-1 sm:gap-1.5 shrink-0 w-full xs:w-auto">
                          <button
                            id={`view-details-${product.sku}`}
                            onClick={() => setActiveModalProduct(product)}
                            className="p-1.5 sm:p-2 rounded-lg sm:rounded-xl text-gray-500 hover:text-emerald-700 hover:bg-emerald-50 border border-gray-200 transition hidden xs:flex lg:flex"
                            title="Quick View"
                            aria-label={`View details of ${product.name}`}
                          >
                            <Eye className="h-3.5 w-3.5 sm:h-4 sm:w-4" />
                          </button>
                          <button
                            id={`add-to-cart-${product.sku}`}
                            onClick={() => onAddToCart(product.id, qty)}
                            className="flex-1 xs:flex-initial px-2.5 py-1.5 sm:px-3 sm:py-2 rounded-lg sm:rounded-xl organic-gradient text-white text-[11px] sm:text-xs font-bold shadow-sm hover:shadow-md transition flex items-center justify-center gap-1 hover:brightness-105 active:scale-95"
                            aria-label={`Add ${product.name} to cart`}
                          >
                            <ShoppingCart className="h-3 w-3 sm:h-3.5 sm:w-3.5" />
                            <span>Add</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Quick View Modal */}
      {activeModalProduct && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 animate-fadeIn">
          <div className="bg-white rounded-3xl max-w-xl w-full p-5 sm:p-6 shadow-2xl border border-emerald-200 relative max-h-[90vh] overflow-y-auto">
            <button
              id="close-product-modal"
              onClick={() => setActiveModalProduct(null)}
              className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-700 rounded-full hover:bg-gray-100 transition z-10"
              aria-label="Close product details"
            >
              <X className="h-5 w-5" />
            </button>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 sm:gap-6 items-center pt-2 sm:pt-0">
              <div className="aspect-square rounded-2xl bg-amber-50/40 overflow-hidden border border-emerald-200">
                <img
                  src={activeModalProduct.imageUrl || 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&q=80&w=600'}
                  alt={activeModalProduct.name}
                  className="w-full h-full object-cover"
                />
              </div>

              <div className="space-y-3.5">
                <div>
                  <span className="text-xs font-bold text-emerald-800 uppercase tracking-wider bg-emerald-50 px-2.5 py-1 rounded-md">
                    100% PURE & NATURAL
                  </span>
                  <h3 className="text-lg sm:text-xl font-bold text-gray-900 mt-2">{activeModalProduct.name}</h3>
                  <p className="text-xs text-gray-500 mt-1 leading-relaxed">{activeModalProduct.description}</p>
                </div>

                <div className="bg-slate-900 text-white rounded-2xl p-4 space-y-2 shadow-inner">
                  <div className="text-2xl font-black text-amber-400">
                    {formatPrice(isWholesale ? activeModalProduct.wholesalePrice : activeModalProduct.retailPrice)}
                  </div>
                  {isWholesale && (
                    <p className="text-[11px] text-amber-300 font-semibold">
                      Wholesale MOQ: {activeModalProduct.wholesaleMoq} units
                    </p>
                  )}
                  <p className="text-xs text-slate-300">
                    Specifications: <strong className="text-white">{activeModalProduct.packageType}</strong> • <strong className="text-white">{activeModalProduct.unitWeightKg} {activeModalProduct.unit || 'kg'}</strong>
                  </p>
                </div>

                <div className="flex items-center gap-3 pt-2">
                  <div className="flex items-center border border-gray-200 rounded-xl overflow-hidden bg-gray-50">
                    <button
                      onClick={() => setModalQty(Math.max(1, modalQty - 1))}
                      className="px-3.5 py-2 text-gray-600 hover:bg-gray-200 text-sm font-bold min-w-[36px]"
                      aria-label="Decrease quantity"
                    >
                      -
                    </button>
                    <span className="px-3 py-2 text-xs font-bold text-gray-800 min-w-[30px] text-center">{modalQty}</span>
                    <button
                      onClick={() => setModalQty(modalQty + 1)}
                      className="px-3.5 py-2 text-gray-600 hover:bg-gray-200 text-sm font-bold min-w-[36px]"
                      aria-label="Increase quantity"
                    >
                      +
                    </button>
                  </div>

                  <button
                    onClick={() => {
                      onAddToCart(activeModalProduct.id, modalQty);
                      setActiveModalProduct(null);
                    }}
                    className="flex-1 py-2.5 px-4 rounded-xl organic-gradient text-gray-900 text-xs font-bold shadow-md hover:shadow-lg transition flex items-center justify-center gap-2 hover:brightness-105"
                  >
                    <ShoppingCart className="h-4 w-4" /> Add to Cart
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Star Rating Helper ────────────────────────────────────────────────────────

function StarRating({ rating, setRating, readOnly = false, size = 'sm' }: { rating: number; setRating?: (r: number) => void; readOnly?: boolean; size?: 'sm' | 'md' | 'lg' }) {
  const iconSize = size === 'lg' ? 'h-6 w-6' : size === 'md' ? 'h-5 w-5' : 'h-4 w-4';
  return (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={readOnly}
          onClick={() => setRating && setRating(star)}
          className={`${readOnly ? 'cursor-default' : 'cursor-pointer hover:scale-110'} transition-transform text-emerald-700`}
        >
          <Star className={`${iconSize} ${star <= rating ? 'fill-amber-400 text-emerald-700' : 'text-gray-300'}`} />
        </button>
      ))}
    </div>
  );
}

// ─── Customer Reviews Page Component ──────────────────────────────────────────

function ReviewsPage({ auth }: { auth: AuthState }) {
  const [reviews, setReviews] = useState<ProductReview[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [selectedProductId, setSelectedProductId] = useState('');
  const [customerName, setCustomerName] = useState(auth.user?.fullName || '');
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');

  const loadReviewsAndProducts = useCallback(async () => {
    setLoading(true);
    try {
      const [revRes, prodRes] = await Promise.all([
        fetch(`${COMMERCE_BASE}/reviews`),
        fetch(`${COMMERCE_BASE}/products`),
      ]);
      const [revData, prodData] = await Promise.all([revRes.json(), prodRes.json()]);
      if (revData.success) setReviews(revData.data);
      if (prodData.success) setProducts(prodData.data);
    } catch (err) {
      console.error('Failed to load reviews', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReviewsAndProducts();
  }, [loadReviewsAndProducts]);

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProductId) {
      setError('Please select a product to review');
      return;
    }
    if (!comment.trim()) {
      setError('Please write a review comment');
      return;
    }

    setSubmitting(true);
    setError('');
    setSuccess('');

    try {
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (auth.token) headers['Authorization'] = `Bearer ${auth.token}`;

      const res = await fetch(`${COMMERCE_BASE}/reviews`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          productId: selectedProductId,
          customerName: customerName || 'Valued Customer',
          rating,
          comment,
        }),
      });

      const body = await res.json();
      if (body.success) {
        setSuccess('Thank you! Your review has been submitted for approval.');
        setComment('');
        setRating(5);
        loadReviewsAndProducts();
      } else {
        setError(body.message || 'Failed to submit review');
      }
    } catch (err: any) {
      setError('Network error while submitting review');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f2f8f3] text-gray-800 py-12 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-5xl mx-auto space-y-10">
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">
            <MessageSquare className="h-4 w-4 text-emerald-600" /> Customer Testimonials
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-gray-900 font-serif">
            What Our Customers Say About <span className="text-emerald-700">RR Jaggery</span>
          </h1>
          <p className="text-sm text-gray-600 max-w-xl mx-auto">
            Authentic feedback from customers, families, and businesses who enjoy our naturally made traditional jaggery products.
          </p>
        </div>

        <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-emerald-200 space-y-6">
          <div className="border-b border-gray-100 pb-4 flex items-center justify-between">
            <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
              <Star className="h-5 w-5 text-emerald-700 fill-amber-500" /> Leave a Verified Product Review
            </h2>
            {!auth.token && (
              <span className="text-xs text-amber-700 bg-amber-50 px-3 py-1 rounded-full font-medium">
                Guest Review Mode
              </span>
            )}
          </div>

          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-xs font-semibold text-red-700 flex items-center gap-2">
              <AlertCircle className="h-4 w-4 text-red-500 shrink-0" /> {error}
            </div>
          )}

          {success && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-xs font-semibold text-emerald-800 flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" /> {success}
            </div>
          )}

          <form onSubmit={handleSubmitReview} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Select Product *</label>
                <select
                  value={selectedProductId}
                  onChange={(e) => setSelectedProductId(e.target.value)}
                  className="w-full p-3 rounded-xl border border-gray-200 bg-[#f2f8f3] text-xs font-medium focus:border-emerald-500 outline-none"
                  required
                >
                  <option value="">-- Choose Product --</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Your Name *</label>
                <input
                  type="text"
                  placeholder="Your name"
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                  className="w-full p-3 rounded-xl border border-gray-200 bg-[#f2f8f3] text-xs font-medium focus:border-emerald-500 outline-none"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">Your Rating *</label>
              <div className="py-2">
                <StarRating rating={rating} setRating={setRating} size="lg" />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">Your Review *</label>
              <textarea
                rows={3}
                placeholder="Your review"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                className="w-full p-3 rounded-xl border border-gray-200 bg-[#f2f8f3] text-xs font-medium focus:border-emerald-500 outline-none"
                required
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full sm:w-auto px-8 py-3 rounded-xl organic-gradient text-gray-900 text-xs font-bold shadow-md hover:shadow-lg transition flex items-center justify-center gap-2"
            >
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              Submit Review
            </button>
          </form>
        </div>

        <div className="space-y-4">
          <h2 className="text-xl font-bold text-gray-900 border-b border-gray-200 pb-3">
            Recent Verified Reviews ({reviews.length})
          </h2>

          {loading ? (
            <div className="py-12 text-center text-xs text-gray-400">Loading reviews...</div>
          ) : reviews.length === 0 ? (
            <div className="bg-white rounded-2xl p-8 text-center text-xs text-gray-500 border border-gray-100">
              No reviews published yet. Be the first to review our organic jaggery products!
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {reviews.map((rev) => (
                <div key={rev.id} className="bg-white p-5 rounded-2xl border border-emerald-200 shadow-sm space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="font-bold text-gray-900 text-xs">{rev.customerName}</h4>
                      <p className="text-[10px] text-emerald-700 font-semibold">{rev.productName}</p>
                    </div>
                    <StarRating rating={rev.rating} readOnly size="sm" />
                  </div>
                  <p className="text-xs text-gray-600 italic">"{rev.comment}"</p>
                  <p className="text-[10px] text-gray-400">{new Date(rev.createdAt).toLocaleDateString()}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── About Us Page Component ──────────────────────────────────────────────────

function AboutPage({
  onBackToStore,
  onNavigate,
}: {
  onBackToStore?: () => void;
  onNavigate?: (page: StorefrontPage) => void;
}) {
  return (
    <div className="min-h-screen bg-[#f2f8f3] text-gray-900 py-8 sm:py-14 px-3 sm:px-6 lg:px-8 font-sans space-y-12 max-w-6xl mx-auto">
      {/* Hero Header */}
      <div className="text-center space-y-4">
        <div className="flex justify-center mb-2">
          <div className="relative w-24 h-24 sm:w-28 sm:h-28 rounded-2xl p-2 bg-white border-2 border-amber-400 shadow-md flex items-center justify-center">
            <img
              src="/RR_Jaggery_Logo.png"
              alt="RR Jaggery Official Logo"
              className="w-full h-full object-contain rounded-xl"
            />
          </div>
        </div>
        <span className="px-3.5 py-1 rounded-full bg-emerald-100 text-emerald-900 text-xs font-bold tracking-wide border border-emerald-300 inline-flex items-center gap-1.5 shadow-sm">
          <Sparkles className="h-3.5 w-3.5 text-amber-600" />
          20+ YEARS OF EXCELLENCE · 100% CHEMICAL FREE ORGANIC JAGGERY
        </span>
        <h1 className="text-3xl sm:text-5xl font-extrabold text-emerald-950 font-serif leading-tight">
          Introducing <span className="text-emerald-700">RR Jaggery</span>
        </h1>
        <p className="text-base sm:text-lg font-medium text-amber-800">
          Your Natural, Pure, Chemical-Free, Organic &amp; Hygienic Sweet Delight!
        </p>
        <p className="text-sm sm:text-base text-gray-700 max-w-3xl mx-auto leading-relaxed">
          With over <strong>20+ years of hands-on experience</strong>, RR Jaggery is the exclusive <strong>100% chemical-free Organic</strong> jaggery producer in Andhra Pradesh. While many other brands only buy and resell jaggery from open markets, we make it ourselves from Cane to Jaggery. Our direct commitment to hygiene, chemical-free processing, and pure quality makes RR Jaggery the natural first choice for your kitchen.
        </p>
      </div>

      {/* 4 Pillars of Excellence */}
      <div className="space-y-6">
        <div className="text-center space-y-1">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif">
            Why Choose RR Jaggery?
          </h2>
          <p className="text-xs sm:text-sm text-gray-600">
            20+ years of pure, chemical-free Organic jaggery making, clean processes, and trusted farmer partnerships.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Pillar 1 */}
          <div className="bg-white p-6 sm:p-7 rounded-3xl border border-emerald-200 shadow-sm hover:shadow-md transition space-y-3">
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-2xl bg-emerald-100 text-emerald-800 border border-emerald-300 shrink-0">
                <Factory className="h-6 w-6" />
              </div>
              <div>
                <span className="text-[11px] font-bold uppercase tracking-wider text-amber-700">Pillar 01</span>
                <h3 className="font-extrabold text-gray-900 text-lg">Control Over Quality</h3>
              </div>
            </div>
            <p className="text-sm text-gray-700 leading-relaxed">
              With <strong>20+ years of experience</strong>, RR Jaggery is a pioneer in making <strong>100% chemical-free Organic Jaggery Cubes, Liquid Jaggey </strong> and <strong>Jaggery Powder</strong>. We make our own jaggery — we are <strong>direct Manufacturers, not resellers</strong>. Unlike brands who simply buy mixed stock from local open markets, we maintain complete chemical-free control over quality at every single step. When you choose RR Jaggery, you get 100% pure, honest and Organic sweetener.
            </p>
          </div>

          {/* Pillar 2 */}
          <div className="bg-white p-6 sm:p-7 rounded-3xl border border-emerald-200 shadow-sm hover:shadow-md transition space-y-3">
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-2xl bg-emerald-100 text-emerald-800 border border-emerald-300 shrink-0">
                <Award className="h-6 w-6" />
              </div>
              <div>
                <span className="text-[11px] font-bold uppercase tracking-wider text-amber-700">Pillar 02</span>
                <h3 className="font-extrabold text-gray-900 text-lg">Specialization in Jaggery</h3>
              </div>
            </div>
            <p className="text-sm text-gray-700 leading-relaxed">
              For over 20+ years, our only focus has been making the finest <strong>chemical-free Organic</strong> jaggery, not general grocery trading. Because we specialize only in jaggery, we maintain strict cleanliness and zero synthetic chemicals. We take great pride in making pure, clean jaggery and delivering it fresh to your kitchen.
            </p>
          </div>

          {/* Pillar 3 */}
          <div className="bg-white p-6 sm:p-7 rounded-3xl border border-emerald-200 shadow-sm hover:shadow-md transition space-y-3">
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-2xl bg-emerald-100 text-emerald-800 border border-emerald-300 shrink-0">
                <Leaf className="h-6 w-6" />
              </div>
              <div>
                <span className="text-[11px] font-bold uppercase tracking-wider text-amber-700">Pillar 03</span>
                <h3 className="font-extrabold text-gray-900 text-lg">Quality Raw Materials</h3>
              </div>
            </div>
            <p className="text-sm text-gray-700 leading-relaxed">
              Quality begins with the raw materials, and we understand that intimately. We pay our sugarcane growing farmers <strong> Premium price</strong>, making us the top paymaster in the region. This ensures we receive the highest quality sugarcane, adhering to RR Jaggery's strict Standard Operating Procedures (SOP).
            </p>
          </div>

          {/* Pillar 4 */}
          <div className="bg-white p-6 sm:p-7 rounded-3xl border border-emerald-200 shadow-sm hover:shadow-md transition space-y-3">
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-2xl bg-emerald-100 text-emerald-800 border border-emerald-300 shrink-0">
                <Shield className="h-6 w-6" />
              </div>
              <div>
                <span className="text-[11px] font-bold uppercase tracking-wider text-amber-700">Pillar 04</span>
                <h3 className="font-extrabold text-gray-900 text-lg">Bacterial Count Nil</h3>
              </div>
            </div>
            <p className="text-sm text-gray-700 leading-relaxed">
              We take hygiene seriously. RR Jaggery is made <strong>100% chemical-free</strong> &mdash; after washing each sugarcane stalk thoroughly with salt water to achieve a certified <strong>bacterial count of nil</strong>. No synthetic preservatives, no bleaching agents, no artificial additives. With RR Jaggery, your family&apos;s health comes first.
            </p>
          </div>
        </div>
      </div>

      {/* Easy Quality Check at Home */}
      <div className="bg-white rounded-3xl p-6 sm:p-10 border border-emerald-200 shadow-sm space-y-8">
        <div className="text-center space-y-2">
          <span className="px-3.5 py-1 rounded-full bg-amber-100 text-amber-900 text-xs font-bold tracking-wide border border-amber-300 inline-block">
            AUTHENTICITY TEST
          </span>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-emerald-950 font-serif">
            Easy Quality Check at Home
          </h2>
          <p className="text-sm text-gray-700 max-w-2xl mx-auto leading-relaxed">
            We are confident that once you experience RR Jaggery, you will never go back. If you ever need to confirm the quality, here is how you can do it at home:
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {/* Check 1 */}
          <div className="p-5 rounded-2xl bg-emerald-50/70 border border-emerald-200 space-y-2.5">
            <div className="flex items-center gap-2 text-emerald-900 font-bold text-base">
              <CheckCircle2 className="h-5 w-5 text-emerald-700 shrink-0" />
              <h4>No Fermented Smell</h4>
            </div>
            <p className="text-xs text-gray-700 leading-relaxed">
              Local jaggery, often produced in unhygienic conditions, can carry a pungent fermented smell. RR Jaggery offers the natural, appetizing aroma of pure cooked sugarcane.
            </p>
          </div>

          {/* Check 2 */}
          <div className="p-5 rounded-2xl bg-emerald-50/70 border border-emerald-200 space-y-2.5">
            <div className="flex items-center gap-2 text-emerald-900 font-bold text-base">
              <CheckCircle2 className="h-5 w-5 text-emerald-700 shrink-0" />
              <h4>No Burping in Tea</h4>
            </div>
            <p className="text-xs text-gray-700 leading-relaxed">
              Local jaggery may contain adulterants like baking soda, causing acidity and discomfort when stirred in tea. With RR Jaggery, enjoy pure tea with zero unpleasant burping.
            </p>
          </div>

          {/* Check 3 */}
          <div className="p-5 rounded-2xl bg-emerald-50/70 border border-emerald-200 space-y-2.5">
            <div className="flex items-center gap-2 text-emerald-900 font-bold text-base">
              <CheckCircle2 className="h-5 w-5 text-emerald-700 shrink-0" />
              <h4>Sucrose &lt; 85%</h4>
            </div>
            <p className="text-xs text-gray-700 leading-relaxed">
              Our jaggery guarantees a sucrose percentage of less than 85%. Excessive sucrose is a clear indicator of refined white sugar adulteration.
            </p>
          </div>

          {/* Check 4 */}
          <div className="p-5 rounded-2xl bg-emerald-50/70 border border-emerald-200 space-y-2.5">
            <div className="flex items-center gap-2 text-emerald-900 font-bold text-base">
              <CheckCircle2 className="h-5 w-5 text-emerald-700 shrink-0" />
              <h4>Sulphur &lt; 5 ppm</h4>
            </div>
            <p className="text-xs text-gray-700 leading-relaxed">
              RR Jaggery is 100% chemical-free with sulphur and phosphate levels below 5 ppm — naturally occurring, not added chemicals. Higher levels indicate artificial bleaching and chemical preservatives.
            </p>
          </div>
        </div>
      </div>

      {/* Nutritional Information & Storage Conditions */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        {/* Nutrition Table (Black & Gold Premium Card) */}
        <div className="lg:col-span-6 bg-gray-950 text-white rounded-3xl p-6 sm:p-8 border-2 border-amber-500/60 shadow-xl flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-amber-500/40">
              <h3 className="text-lg sm:text-xl font-bold text-amber-400 font-serif">
                Nutritional Information per 100 gms
              </h3>
              <span className="text-[11px] px-2 py-0.5 rounded bg-amber-400/20 text-amber-300 font-semibold border border-amber-400/30">
                100g Serving
              </span>
            </div>

            <div className="mt-5 space-y-3 font-sans">
              <div className="flex justify-between items-center py-2.5 px-3 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition">
                <span className="text-sm font-semibold text-gray-200">Energy ( Kcal )</span>
                <span className="text-base font-extrabold text-amber-300">282</span>
              </div>
              <div className="flex justify-between items-center py-2.5 px-3 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition">
                <span className="text-sm font-semibold text-gray-200">Fat</span>
                <span className="text-base font-extrabold text-emerald-400">Nill</span>
              </div>
              <div className="flex justify-between items-center py-2.5 px-3 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition">
                <span className="text-sm font-semibold text-gray-200">Carbohydrate</span>
                <span className="text-base font-extrabold text-amber-300">1.40 gms</span>
              </div>
              <div className="flex justify-between items-center py-2.5 px-3 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition">
                <span className="text-sm font-semibold text-gray-200">Protein</span>
                <span className="text-base font-extrabold text-amber-300">2.25</span>
              </div>
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-gray-800 text-xs text-amber-300/80 font-medium">
            ** Approximate Values based on traditional unrefined sugarcane crystallization.
          </div>
        </div>

        {/* Storage Guidelines & Whitish Layer Note */}
        <div className="lg:col-span-6 bg-white rounded-3xl p-6 sm:p-8 border border-emerald-200 shadow-sm flex flex-col justify-between space-y-5">
          <div className="space-y-4">
            <div className="flex items-center gap-2.5 text-emerald-950 font-serif text-xl font-bold">
              <div className="p-2 rounded-xl bg-emerald-100 text-emerald-800">
                <Info className="h-5 w-5" />
              </div>
              <h3>Storage &amp; Product Characteristics</h3>
            </div>

            <div className="space-y-3 text-sm text-gray-700">
              <div className="p-3.5 rounded-2xl bg-emerald-50/70 border border-emerald-200 flex items-start gap-3">
                <Clock className="h-5 w-5 text-emerald-800 shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-900">Recommended Storage Temperature</p>
                  <p className="text-xs text-gray-600 mt-0.5">
                    Store in cool, dry, and hygienic conditions <strong>below 22°C</strong>.
                  </p>
                </div>
              </div>

              <div className="p-3.5 rounded-2xl bg-amber-50/70 border border-amber-200 flex items-start gap-3">
                <AlertTriangle className="h-5 w-5 text-amber-800 shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-gray-900">Sunlight &amp; Moisture Protection</p>
                  <p className="text-xs text-gray-600 mt-0.5">
                    Keep away from direct sunlight and store in an airtight container once opened.
                  </p>
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-[#f2f8f3] border border-emerald-200 text-xs leading-relaxed text-gray-700 space-y-1.5">
                <p className="font-bold text-emerald-950 flex items-center gap-1.5">
                  <Sparkles className="h-4 w-4 text-amber-600" />
                  Natural Whitish Layer Note
                </p>
                <p>
                  Temperature and humidity changes may cause natural jaggery to develop a thin whitish layer on the surface. This is a natural crystallization of unrefined sucrose and moisture breathing, completely normal and <strong>without affecting its fitness, purity, or safety for consumption</strong>.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Brand Creed & Closing */}
      <div className="bg-gradient-to-r from-emerald-950 via-emerald-900 to-teal-950 text-white rounded-3xl p-5 sm:p-10 md:p-12 shadow-xl border-2 border-emerald-700/60 text-center space-y-4 sm:space-y-6 relative overflow-hidden">
        <div className="max-w-2xl mx-auto space-y-3 sm:space-y-4">
          <span className="px-3 py-1 rounded-full bg-emerald-800/80 text-amber-300 text-[11px] sm:text-xs font-bold tracking-wide border border-emerald-500/40 inline-block uppercase">
            The Choice is Crystal Clear
          </span>
          <h2 className="text-xl sm:text-3xl md:text-4xl font-extrabold font-serif text-white leading-snug sm:leading-tight">
            RR Jaggery &mdash; 20+ Years of Pure, Chemical-Free Tradition
          </h2>
          <p className="text-xs sm:text-sm md:text-base text-emerald-100/90 leading-relaxed font-sans max-w-xl mx-auto">
            Experience the difference of 20+ years of pure organic jaggery making &mdash; 100% chemical-free, hygienic, and naturally wholesome. Enjoy authentic sweetness made with direct care for you and your family.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4 pt-2 max-w-xs sm:max-w-none mx-auto w-full">
          {onBackToStore && (
            <button
              onClick={onBackToStore}
              className="w-full sm:w-auto px-6 py-3 rounded-full bg-amber-500 hover:bg-amber-400 text-emerald-950 font-bold text-xs sm:text-sm shadow-lg hover:shadow-xl transition transform hover:-translate-y-0.5 flex items-center justify-center gap-2"
            >
              <Package className="h-4 w-4" />
              <span>Explore All Products</span>
            </button>
          )}
          {onNavigate && (
            <button
              onClick={() => onNavigate('contact')}
              className="w-full sm:w-auto px-6 py-3 rounded-full bg-white/10 hover:bg-white/20 text-white font-bold text-xs sm:text-sm border border-white/20 transition flex items-center justify-center gap-2"
            >
              <Mail className="h-4 w-4" />
              <span>Contact Our Team</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── Contact Us Page Component ────────────────────────────────────────────────

function ContactPage() {
  const [submitted, setSubmitted] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [sendError, setSendError] = useState('');
  const [formData, setFormData] = useState({ name: '', email: '', phone: '', message: '' });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSending(true);
    setSendError('');
    try {
      const response = await fetch(CONTACT_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });
      const body: unknown = await response.json().catch(() => null);
      if (!response.ok) {
        const errorMessage = isRecord(body) && typeof body.message === 'string'
          ? body.message
          : isRecord(body) && isRecord(body.error) && typeof body.error.message === 'string'
            ? body.error.message
            : 'Unable to send your message. Please try again or contact us by email.';
        throw new Error(errorMessage);
      }
      setSubmitted(true);
    } catch (error) {
      setSendError(error instanceof Error ? error.message : 'Unable to send your message. Please try again.');
    } finally {
      setIsSending(false);
    }
  };

  const whatsappHref = `https://wa.me/917207063315?text=${encodeURIComponent(
    `Website inquiry from ${formData.name} (${formData.email}${formData.phone ? `, ${formData.phone}` : ''}): ${formData.message}`
  )}`;

  return (
    <div className="min-h-screen bg-[#f2f8f3] text-gray-900 py-12 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-5xl mx-auto space-y-10">
        <div className="text-center space-y-3">
          <span className="px-3.5 py-1 rounded-full bg-emerald-100 text-emerald-900 text-xs font-bold tracking-wide border border-emerald-200 inline-block">
            GET IN TOUCH
          </span>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-emerald-950 font-serif leading-tight">
            We’d Love to Hear <span className="text-emerald-700">From You</span>
          </h1>
          <p className="text-sm text-gray-700 max-w-lg mx-auto font-normal leading-relaxed">
            Whether you have questions about bulk wholesale orders, retail distribution, or product specifications.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
          {/* Contact Information Card */}
          <div className="md:col-span-5 bg-emerald-900 text-white rounded-3xl p-8 space-y-6 shadow-xl border border-emerald-800 relative overflow-hidden">
            <div className="space-y-1.5 border-b border-emerald-800/80 pb-4">
              <h3 className="text-2xl font-bold font-serif text-white tracking-tight">RR Jaggery</h3>
              <p className="text-xs text-emerald-100 font-medium">Traditional Natural Jaggery Processing Unit</p>
            </div>

            <div className="space-y-5 text-xs">
              <div className="flex items-start gap-3.5">
                <div className="p-2 rounded-xl bg-emerald-800/80 text-emerald-300 border border-emerald-700 shrink-0 mt-0.5">
                  <MapPin className="h-4 w-4" />
                </div>
                <div>
                  <strong className="block text-white text-xs font-bold mb-0.5">Factory & Works:</strong>
                  <p className="text-emerald-50 text-xs leading-relaxed font-medium">
                    Kankipadu, Andhra Pradesh – 521163
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3.5">
                <div className="p-2 rounded-xl bg-emerald-800/80 text-emerald-300 border border-emerald-700 shrink-0 mt-0.5">
                  <Phone className="h-4 w-4" />
                </div>
                <div>
                  <strong className="block text-white text-xs font-bold mb-0.5">Phone / WhatsApp:</strong>
                  <p className="text-emerald-50 text-xs font-medium">
                    +91 7207063315
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3.5">
                <div className="p-2 rounded-xl bg-emerald-800/80 text-emerald-300 border border-emerald-700 shrink-0 mt-0.5">
                  <Mail className="h-4 w-4" />
                </div>
                <div>
                  <strong className="block text-white text-xs font-bold mb-0.5">Email Enquiries:</strong>
                  <p className="text-emerald-50 text-xs font-medium">
                    rrjaggery@gmail.com
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3.5">
                <div className="p-2 rounded-xl bg-emerald-800/80 text-emerald-300 border border-emerald-700 shrink-0 mt-0.5">
                  <Clock className="h-4 w-4" />
                </div>
                <div>
                  <strong className="block text-white text-xs font-bold mb-0.5">Support Hours:</strong>
                  <p className="text-emerald-50 text-xs font-medium">
                    10:00 AM – 6:00 PM (Mon–Sat)
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Direct Message Form Card */}
          <div className="md:col-span-7 bg-white rounded-3xl p-8 shadow-sm border border-emerald-200">
            {submitted ? (
              <div className="text-center py-12 space-y-4">
                <div className="w-16 h-16 bg-emerald-100 text-emerald-700 rounded-full flex items-center justify-center mx-auto border border-emerald-200">
                  <CheckCircle2 className="h-8 w-8" />
                </div>
                <h3 className="text-2xl font-bold text-emerald-950 font-serif">Message Sent Successfully!</h3>
                <p className="text-sm text-gray-700 max-w-md mx-auto">Thank you for contacting RR Jaggery. Our sales team will get back to you within 24 hours.</p>
                <a
                  href={whatsappHref}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white text-xs font-bold transition shadow-sm"
                >
                  <MessageSquare className="h-4 w-4" />
                  Continue in WhatsApp
                </a>
                <p className="text-xs text-gray-500">WhatsApp opens with your message ready for you to send.</p>
                <button
                  onClick={() => setSubmitted(false)}
                  className="px-6 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white text-xs font-bold transition shadow-sm"
                >
                  Send Another Message
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="border-b border-gray-200 pb-3 mb-4">
                  <h3 className="text-xl font-bold text-emerald-950 font-serif">Send Us a Direct Message</h3>
                  <p className="text-xs text-gray-600">Fill in the details below and we will get in touch promptly.</p>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-800 mb-1">Your Name *</label>
                    <input
                      type="text"
                      required
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      placeholder="Your name"
                      className="w-full p-3 rounded-xl border border-gray-300 bg-white text-gray-900 text-xs font-medium focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none placeholder:text-gray-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-800 mb-1">Email Address *</label>
                    <input
                      type="email"
                      required
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      placeholder="Your email"
                      className="w-full p-3 rounded-xl border border-gray-300 bg-white text-gray-900 text-xs font-medium focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none placeholder:text-gray-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-800 mb-1">Phone Number</label>
                  <input
                    type="tel"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    placeholder="Your phone number"
                    className="w-full p-3 rounded-xl border border-gray-300 bg-white text-gray-900 text-xs font-medium focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none placeholder:text-gray-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-800 mb-1">Message / Inquiry *</label>
                  <textarea
                    rows={4}
                    required
                    value={formData.message}
                    onChange={(e) => setFormData({ ...formData, message: e.target.value })}
                    placeholder="Your message"
                    className="w-full p-3 rounded-xl border border-gray-300 bg-white text-gray-900 text-xs font-medium focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none placeholder:text-gray-500 resize-none"
                  />
                </div>

                <button
                  type="submit"
                  disabled={isSending}
                  className="w-full py-3.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white text-xs font-bold shadow-md hover:shadow-lg transition flex items-center justify-center gap-2"
                >
                  <Send className="h-4 w-4" /> {isSending ? 'Sending...' : 'Send Message'}
                </button>
                {sendError && (
                  <div role="alert" className="text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-lg px-3 py-2">
                    <p>{sendError}</p>
                    <a href={whatsappHref} target="_blank" rel="noopener noreferrer" className="mt-2 inline-block font-semibold underline">
                      Continue this inquiry on WhatsApp
                    </a>
                  </div>
                )}
              </form>
            )}
          </div>
        </div>
      </div>
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
    unitWeightKg: 1.0, unit: 'kg', retailPrice: 150, wholesalePrice: 110, wholesaleMoq: 20,
    imageUrl: '', featured: false, active: true
  });
  const [imageUploading, setImageUploading] = useState(false);
  const [catForm, setCatForm] = useState({ name: '', slug: '', description: '', active: true, displayOrder: 1 });

  const handleImageFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !auth.token) return;

    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    const fileExt = file.name.split('.').pop()?.toLowerCase() || '';
    const allowedExts = ['jpg', 'jpeg', 'png', 'webp', 'jfif'];

    if (!allowedTypes.includes(file.type) && !allowedExts.includes(fileExt)) {
      alert('Invalid file format. Please upload a valid JPG, PNG, or WEBP image.');
      if (e.target) e.target.value = '';
      return;
    }

    // Validate file size (20MB)
    const MAX_SIZE_MB = 20;
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      alert(`Image size (${(file.size / (1024 * 1024)).toFixed(1)}MB) exceeds maximum allowed limit of ${MAX_SIZE_MB}MB.`);
      if (e.target) e.target.value = '';
      return;
    }

    setImageUploading(true);
    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await fetch(`${COMMERCE_BASE}/admin/products/upload-image`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${auth.token}`,
        },
        body: formData,
      });

      const data = await res.json().catch(() => null);

      if (!res.ok) {
        let message = data?.error?.message || data?.message;
        if (!message) {
          if (res.status === 413) {
            message = 'Image size exceeds maximum upload limit allowed by server.';
          } else if (res.status === 401 || res.status === 403) {
            message = 'Session expired or insufficient admin permissions. Please log in again.';
          } else {
            message = `Failed to upload image (HTTP ${res.status}).`;
          }
        }
        alert(message);
        return;
      }

      if (data?.data?.imageUrl) {
        setProductForm(prev => ({ ...prev, imageUrl: data.data.imageUrl }));
      }
    } catch (err: any) {
      alert(err?.message || 'Network error occurred while uploading image. Please check connection and try again.');
    } finally {
      setImageUploading(false);
      if (e.target) {
        e.target.value = '';
      }
    }
  };

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
        unit: productForm.unit || 'kg',
        retailPrice: Number(productForm.retailPrice),
        wholesalePrice: Number(productForm.wholesalePrice),
        wholesaleMoq: Number(productForm.wholesaleMoq),
      };

      const res = await fetch(url, {
        method,
        headers: authHeaders(auth.token),
        body: JSON.stringify(payload),
      });
      const body = await res.json().catch(() => null);
      if (res.ok && body?.success) {
        setShowCreateModal(false);
        setEditingProduct(null);
        fetchData();
      } else {
        const errorDetail = body?.error?.details?.map((d: any) => d.message || `${d.field}: invalid`).join('; ');
        const errorMsg = errorDetail || body?.error?.message || body?.message || 'Failed to save product.';
        alert(errorMsg);
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
          <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <Package className="h-5 w-5 text-emerald-700" /> Master Catalogue Administration
          </h2>
          <p className="text-xs text-gray-500 mt-1">Manage products, categories, and wholesale rates (ROLE_ADMIN)</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center bg-white p-1 rounded-xl border border-gray-200 text-xs">
            <button
              id="admin-tab-products"
              onClick={() => setTab('products')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'products' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
            >
              Products ({products.length})
            </button>
            <button
              id="admin-tab-categories"
              onClick={() => setTab('categories')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'categories' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
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
                  packageType: 'BOX', unitWeightKg: 1.0, unit: 'kg', retailPrice: 150,
                  wholesalePrice: 110, wholesaleMoq: 20, imageUrl: '', featured: false, active: true
                });
                setShowCreateModal(true);
              }}
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-emerald-700 hover:bg-emerald-600 text-gray-900 font-bold rounded-xl text-xs transition"
            >
              <Plus className="h-4 w-4" /> Add Product
            </button>
          ) : (
            <button
              id="btn-add-category"
              onClick={() => setShowCategoryModal(true)}
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-emerald-700 hover:bg-emerald-600 text-gray-900 font-bold rounded-xl text-xs transition"
            >
              <Plus className="h-4 w-4" /> Add Category
            </button>
          )}

          <button onClick={fetchData} className="p-2 bg-white border border-gray-200 rounded-xl hover:bg-gray-100 transition" title="Refresh">
            <RefreshCw className="h-4 w-4 text-emerald-700" />
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-48"><Spinner size={8} /></div>
      ) : error ? (
        <div className="text-center py-12 text-red-600">{error}</div>
      ) : tab === 'products' ? (
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-gray-500 font-semibold">Product Name</th>
                  <th className="text-left px-4 py-3 text-gray-500 font-semibold">SKU</th>
                  <th className="text-right px-4 py-3 text-gray-500 font-semibold">Retail Rate</th>
                  <th className="text-right px-4 py-3 text-gray-500 font-semibold">Wholesale Rate</th>
                  <th className="text-center px-4 py-3 text-gray-500 font-semibold">Active</th>
                  <th className="text-center px-4 py-3 text-gray-500 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {products.map(p => (
                  <tr key={p.id} id={`admin-product-row-${p.sku}`} className="border-b border-gray-200 hover:bg-gray-100/30 transition">
                    <td className="px-4 py-3 text-gray-900 font-medium">{p.name}</td>
                    <td className="px-4 py-3 text-gray-500 font-mono">{p.sku}</td>
                    <td className="px-4 py-3 text-right text-emerald-700 font-semibold">{formatPrice(p.retailPrice)}</td>
                    <td className="px-4 py-3 text-right text-emerald-700 font-semibold">{formatPrice(p.wholesalePrice)}</td>
                    <td className="px-4 py-3 text-center">
                      <button
                        id={`toggle-active-${p.sku}`}
                        onClick={() => handleToggleActive(p)}
                        className={`px-2.5 py-1 rounded-lg text-[10px] font-bold transition ${p.active ? 'bg-emerald-500/20 text-emerald-700 border border-emerald-500/30' : 'bg-red-500/20 text-red-600 border border-red-500/30'}`}
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
                            unitWeightKg: p.unitWeightKg, unit: p.unit ?? 'kg', retailPrice: p.retailPrice,
                            wholesalePrice: p.wholesalePrice, wholesaleMoq: p.wholesaleMoq,
                            imageUrl: p.imageUrl ?? '', featured: p.featured, active: p.active
                          });
                          setShowCreateModal(true);
                        }}
                        className="p-1.5 bg-gray-100 hover:bg-emerald-100 text-gray-700 rounded-lg transition"
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
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-4 py-3 text-gray-500 font-semibold">Category Name</th>
                  <th className="text-left px-4 py-3 text-gray-500 font-semibold">Slug</th>
                  <th className="text-left px-4 py-3 text-gray-500 font-semibold">Description</th>
                  <th className="text-center px-4 py-3 text-gray-500 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody>
                {categories.map(c => (
                  <tr key={c.id} className="border-b border-gray-200 hover:bg-gray-100/30 transition">
                    <td className="px-4 py-3 text-gray-900 font-medium flex items-center gap-2"><Tag className="h-3.5 w-3.5 text-emerald-700" />{c.name}</td>
                    <td className="px-4 py-3 text-gray-500 font-mono">{c.slug}</td>
                    <td className="px-4 py-3 text-gray-500">{c.description ?? '—'}</td>
                    <td className="px-4 py-3 text-center">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-700 border border-emerald-500/20">ACTIVE</span>
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
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-emerald-50/60 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white border border-gray-200 rounded-3xl p-6 sm:p-8 w-full max-w-2xl shadow-2xl relative max-h-[90vh] overflow-y-auto">
            <button
              id="close-create-product-modal"
              onClick={() => setShowCreateModal(false)}
              className="absolute top-5 right-5 p-1.5 rounded-xl bg-gray-100 text-gray-500 hover:text-gray-900 transition"
            >
              <X className="h-4 w-4" />
            </button>
            <h2 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2 font-serif">
              <Package className="w-5 h-5 text-emerald-700" />
              {editingProduct ? 'Edit Product' : 'Create New Jaggery Product'}
            </h2>
            <form onSubmit={handleSaveProduct} className="space-y-3.5 text-xs">
              <div>
                <label className="text-gray-700 font-semibold mb-1 block">Product Name *</label>
                <input
                  id="form-prod-name"
                  type="text"
                  required
                  placeholder="e.g. Traditional Organic Jaggery Block"
                  value={productForm.name}
                  onChange={e => {
                    const val = e.target.value;
                    setProductForm(f => {
                      const slug = editingProduct ? f.slug : val.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
                      let sku = f.sku;
                      if (!editingProduct) {
                        const baseSku = val ? 'RR-' + val.toUpperCase().replace(/[^A-Z0-9]/g, '').substring(0, 6) + '-' + (f.packageType ? f.packageType.substring(0, 3) : 'PKG') : '';
                        if (baseSku) {
                          const exists = products.some(p => p.sku.toUpperCase() === baseSku);
                          sku = exists ? `${baseSku}-${Math.floor(100 + Math.random() * 900)}` : baseSku;
                        }
                      }
                      return { ...f, name: val, slug, sku };
                    });
                  }}
                  className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-gray-700 font-semibold mb-1 block">SKU *</label>
                  <input
                    id="form-prod-sku"
                    type="text"
                    required
                    placeholder="e.g. RR-BLK-10KG"
                    value={productForm.sku}
                    onChange={e => setProductForm(f => ({ ...f, sku: e.target.value }))}
                    className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600 font-mono text-xs"
                  />
                </div>
                <div>
                  <label className="text-gray-700 font-semibold mb-1 block">Slug *</label>
                  <input
                    id="form-prod-slug"
                    type="text"
                    required
                    placeholder="e.g. traditional-farm-block-10kg"
                    value={productForm.slug}
                    onChange={e => setProductForm(f => ({ ...f, slug: e.target.value }))}
                    className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600 font-mono text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="text-gray-700 font-semibold mb-1 block">Category *</label>
                <select
                  id="form-prod-category"
                  value={productForm.categoryId}
                  onChange={e => setProductForm(f => ({ ...f, categoryId: e.target.value }))}
                  className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                >
                  {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>

              {/* Product Image Upload Section */}
              <div className="p-3 bg-emerald-50/60 rounded-xl border border-gray-200 space-y-2">
                <label className="block text-gray-700 font-semibold">Product Image</label>
                <div className="flex items-center gap-3">
                  {productForm.imageUrl ? (
                    <img
                      src={productForm.imageUrl}
                      alt="Preview"
                      className="w-14 h-14 rounded-lg object-cover border border-gray-300 shadow-sm"
                    />
                  ) : (
                    <div className="w-14 h-14 rounded-lg bg-white border border-gray-200 flex items-center justify-center text-gray-400">
                      <Upload className="w-5 h-5 text-emerald-700" />
                    </div>
                  )}
                  <div className="flex-1 space-y-1">
                    <label className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-emerald-700 hover:bg-emerald-600 text-white font-bold rounded-lg cursor-pointer transition text-xs shadow-sm">
                      <Upload className="w-3.5 h-3.5" />
                      {imageUploading ? 'Uploading...' : 'Choose Image'}
                      <input
                        type="file"
                        accept="image/jpeg,image/jpg,image/png,image/webp,.jpg,.jpeg,.png,.webp,.jfif"
                        onChange={handleImageFileChange}
                        disabled={imageUploading}
                        className="hidden"
                      />
                    </label>
                    <p className="text-[10px] text-gray-500">Supports JPG, PNG, WEBP up to 20MB</p>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
                <div>
                  <label className="text-gray-700 font-semibold mb-1 block">Packaging</label>
                  <select
                    id="form-prod-pkg"
                    value={productForm.packageType}
                    onChange={e => setProductForm(f => ({ ...f, packageType: e.target.value }))}
                    className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                  >
                    <option value="BOX">Box</option>
                    <option value="POUCH">Pouch</option>
                    <option value="JAR">Jar</option>
                    <option value="BOTTLE">Bottle</option>
                    <option value="CAN">Can</option>
                    <option value="BAG">Bag</option>
                    <option value="BUCKET">Bucket</option>
                    <option value="TIN">Tin</option>
                  </select>
                </div>
                <div>
                  <label className="text-gray-700 font-semibold mb-1 block">Quantity *</label>
                  <input
                    id="form-prod-weight"
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={productForm.unitWeightKg}
                    onChange={e => setProductForm(f => ({ ...f, unitWeightKg: parseFloat(e.target.value) || 0 }))}
                    className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                  />
                </div>
                <div>
                  <label className="text-gray-700 font-semibold mb-1 block">Unit *</label>
                  <select
                    id="form-prod-unit"
                    value={productForm.unit || 'kg'}
                    onChange={e => setProductForm(f => ({ ...f, unit: e.target.value }))}
                    className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600 font-medium"
                  >
                    <option value="kg">kg (Kilogram)</option>
                    <option value="L">L (Liter)</option>
                    <option value="g">g (Gram)</option>
                    <option value="ml">ml (Milliliter)</option>
                  </select>
                </div>
                <div>
                  <label className="text-gray-700 font-semibold mb-1 block">Wholesale MOQ *</label>
                  <input
                    id="form-prod-moq"
                    type="number"
                    min="1"
                    required
                    value={productForm.wholesaleMoq}
                    onChange={e => setProductForm(f => ({ ...f, wholesaleMoq: parseInt(e.target.value) || 1 }))}
                    className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-gray-700 font-semibold mb-1 block">Retail Price (₹) *</label>
                  <input
                    id="form-prod-retailprice"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={productForm.retailPrice}
                    onChange={e => setProductForm(f => ({ ...f, retailPrice: parseFloat(e.target.value) || 0 }))}
                    className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                  />
                </div>
                <div>
                  <label className="text-gray-700 font-semibold mb-1 block">Wholesale Price (₹) *</label>
                  <input
                    id="form-prod-wholesaleprice"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={productForm.wholesalePrice}
                    onChange={e => setProductForm(f => ({ ...f, wholesalePrice: parseFloat(e.target.value) || 0 }))}
                    className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                  />
                </div>
              </div>

              <div>
                <label className="text-gray-700 font-semibold mb-1 block">Description</label>
                <textarea
                  id="form-prod-desc"
                  rows={2}
                  placeholder="Short description of taste, packing, and quality..."
                  value={productForm.description}
                  onChange={e => setProductForm(f => ({ ...f, description: e.target.value }))}
                  className="w-full bg-white border border-gray-300 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="form-prod-active"
                  checked={productForm.active}
                  onChange={e => setProductForm(f => ({ ...f, active: e.target.checked }))}
                  className="w-4 h-4 rounded text-emerald-700 focus:ring-emerald-700"
                />
                <label htmlFor="form-prod-active" className="text-gray-700 font-medium cursor-pointer">
                  Active for sale
                </label>
              </div>

              <div className="pt-3 border-t border-gray-200 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl font-medium transition"
                >
                  Cancel
                </button>
                <button
                  id="submit-save-product"
                  type="submit"
                  className="px-5 py-2 bg-emerald-700 hover:bg-emerald-600 text-white font-bold rounded-xl shadow-md transition"
                >
                  {editingProduct ? 'Save Product Changes' : 'Create Jaggery Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create Category Modal */}
      {showCategoryModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-emerald-50/60 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white border border-gray-200 rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl relative">
            <button
              id="close-create-cat-modal"
              onClick={() => setShowCategoryModal(false)}
              className="absolute top-5 right-5 p-1.5 rounded-xl bg-gray-100 text-gray-500 hover:text-gray-900 transition"
            >
              <X className="h-4 w-4" />
            </button>
            <h2 className="text-lg font-bold text-gray-900 mb-4">Add Jaggery Category</h2>
            <form onSubmit={handleSaveCategory} className="space-y-3 text-xs">
              <div>
                <label className="text-gray-500 font-semibold">Category Name</label>
                <input
                  id="form-cat-name"
                  type="text"
                  required
                  value={catForm.name}
                  onChange={e => setCatForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold">Slug</label>
                <input
                  id="form-cat-slug"
                  type="text"
                  required
                  value={catForm.slug}
                  onChange={e => setCatForm(f => ({ ...f, slug: e.target.value }))}
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <button
                id="submit-save-category"
                type="submit"
                className="w-full mt-4 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-xs shadow-lg shadow-amber-600/20 hover:brightness-105 transition"
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
          <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <Activity className="h-5 w-5 text-emerald-700" /> Live Microservice Mesh Status
          </h2>
          <p className="text-xs text-gray-500 mt-1">
            {onlineCount}/{services.length} services online • {lastCheck ? `Last ping: ${lastCheck}` : 'Checking…'}
          </p>
        </div>
        <button
          onClick={checkAll}
          disabled={isChecking}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-white hover:bg-gray-100 border border-gray-200 text-gray-800 rounded-xl text-xs font-semibold transition disabled:opacity-50"
        >
          <RefreshCw className={`h-3.5 w-3.5 text-emerald-700 ${isChecking ? 'animate-spin' : ''}`} />
          {isChecking ? 'Testing…' : 'Ping Services'}
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {services.map(svc => (
          <div key={svc.code} className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 space-y-3">
            <div className="flex items-start justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-sm text-gray-900">{svc.name}</h3>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-700/10 text-emerald-700 font-mono font-bold">:{svc.port}</span>
                </div>
                <span className="text-xs text-gray-500">{svc.role}</span>
              </div>
              <div className="flex items-center gap-2">
                <StatusBadge status={svc.status} />
                {svc.latencyMs !== undefined && svc.status === 'ONLINE' && (
                  <span className="text-[10px] font-mono text-emerald-300">{svc.latencyMs}ms</span>
                )}
              </div>
            </div>

            {svc.payload && (
              <div className="bg-white/60 p-2.5 rounded-xl border border-gray-200 text-[11px] font-mono text-gray-700 space-y-1">
                <div className="flex justify-between text-gray-500">
                  <span>Status: <strong className="text-emerald-700">{svc.payload.status}</strong></span>
                  {svc.payload.version && <span>v{svc.payload.version}</span>}
                </div>
                <div className="text-gray-500 truncate">Schema: <span className="text-emerald-700">{svc.schema}</span></div>
              </div>
            )}

            <div className="pt-2 border-t border-gray-200 text-[11px] text-gray-500 font-mono truncate">
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
    <div className="bg-white border border-gray-200 rounded-xl rounded-3xl p-6 sm:p-8 space-y-4">
      <h2 className="text-lg font-bold text-gray-900">Agile Sprint Delivery Roadmap</h2>
      <div className="space-y-3">
        {sprints.map((s, i) => (
          <div key={i} className={`p-4 rounded-xl border flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${s.status === 'CURRENT' ? 'bg-amber-950/20 border-emerald-200' : 'bg-gray-50 border-gray-200'}`}>
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                {s.status === 'DONE' && <CheckCircle2 className="h-4 w-4 text-emerald-700 shrink-0" />}
                {s.status === 'CURRENT' && <Activity className="h-4 w-4 text-emerald-700 animate-pulse shrink-0" />}
                {s.status === 'PENDING' && <ChevronRight className="h-4 w-4 text-gray-600 shrink-0" />}
                <span className="font-bold text-sm text-gray-900">{s.s}: {s.name}</span>
              </div>
              <p className="text-xs text-gray-500 pl-6">{s.desc}</p>
            </div>
            <span className={`text-[10px] px-2.5 py-1 rounded-full font-bold uppercase tracking-wider self-start sm:self-center shrink-0 ${s.status === 'DONE' ? 'bg-emerald-500/10 text-emerald-700 border border-emerald-500/20'
              : s.status === 'CURRENT' ? 'bg-emerald-700/20 text-emerald-800 border border-emerald-300 animate-pulse'
                : 'bg-gray-100 text-gray-500 border border-gray-300'
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
          fetch(`${getApiBaseUrl(8084, 'inventory')}/items`, { headers }),
          fetch(`${getApiBaseUrl(8084, 'inventory')}/items/low-stock`, { headers }),
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
          fetch(`${getApiBaseUrl(8085, 'procurement')}/suppliers`, { headers }),
          fetch(`${getApiBaseUrl(8085, 'procurement')}/purchase-orders`, { headers }),
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
        const prodRes = await fetch(`${getApiBaseUrl(8086, 'production')}/batches`, { headers });
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
        const custRes = await fetch(`${getApiBaseUrl(8083, 'customers')}`, { headers });
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
        <p className="text-xs text-gray-500">Loading live operational and system metrics…</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-emerald-700" />
            Executive Overview & Operational Metrics
          </h2>
          <p className="text-xs text-gray-500">
            Authoritative real-time aggregation across all active microservices (Sprints 0–5).
          </p>
        </div>
        <button
          onClick={fetchOverviewData}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-white hover:bg-gray-100 border border-gray-200 text-gray-800 rounded-xl text-xs font-semibold transition"
        >
          <RefreshCw className="h-3.5 w-3.5 text-emerald-700" /> Refresh Metrics
        </button>
      </div>

      {/* Grid of Real-Data Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* Users Card */}
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 border border-gray-200 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Auth & User Accounts</span>
            <span className="p-2 rounded-xl bg-blue-500/10 text-blue-600"><Users className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-gray-900">{authStats?.totalUsers ?? 0}</div>
            <p className="text-xs text-gray-500 mt-1">
              <span className="text-red-600 font-bold">{authStats?.adminCount ?? 0}</span> Admin •{' '}
              <span className="text-blue-600 font-bold">{authStats?.managerCount ?? 0}</span> Managers •{' '}
              <span className="text-emerald-700 font-bold">{authStats?.retailCustomerCount ?? 0}</span> Retail Customers
            </p>
          </div>
          <div className="text-[11px] text-gray-500 pt-2 border-t border-gray-200 font-mono">
            Auth Service: auth_schema.users
          </div>
        </div>

        {/* Wholesale Businesses Card */}
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 border border-gray-200 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Wholesale Businesses (B2B)</span>
            <span className="p-2 rounded-xl bg-emerald-700/10 text-emerald-700"><Building2 className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-gray-900">{wholesaleBusinessCount}</div>
            <p className="text-xs text-emerald-700/90 mt-1">
              Internal business records (No portal login)
            </p>
          </div>
          <div className="text-[11px] text-gray-500 pt-2 border-t border-gray-200 font-mono">
            Ledger Service: customer_schema.customers
          </div>
        </div>

        {/* Inventory Items Card */}
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 border border-gray-200 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Inventory & Materials</span>
            <span className="p-2 rounded-xl bg-purple-500/10 text-purple-700"><Package className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-gray-900">{inventoryCount}</div>
            <p className="text-xs text-gray-500 mt-1">
              {lowStockCount > 0 ? (
                <span className="text-emerald-700 font-bold flex items-center gap-1">
                  <AlertTriangle className="h-3 w-3 inline" /> {lowStockCount} items below reorder level
                </span>
              ) : (
                <span className="text-emerald-700 font-bold">All stock levels healthy</span>
              )}
            </p>
          </div>
          <div className="text-[11px] text-gray-500 pt-2 border-t border-gray-200 font-mono">
            Inventory Service: inventory_schema
          </div>
        </div>

        {/* Procurement Card */}
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 border border-gray-200 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Cane Procurement & POs</span>
            <span className="p-2 rounded-xl bg-emerald-500/10 text-emerald-700"><Truck className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-gray-900">{poCount}</div>
            <p className="text-xs text-gray-500 mt-1">
              <span className="text-emerald-700 font-bold">{supplierCount}</span> active cane & packing suppliers
            </p>
          </div>
          <div className="text-[11px] text-gray-500 pt-2 border-t border-gray-200 font-mono">
            Procurement Service: procurement_schema
          </div>
        </div>

        {/* Production Card */}
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 border border-gray-200 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Mill Production Batches</span>
            <span className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400"><Factory className="h-4 w-4" /></span>
          </div>
          <div>
            <div className="text-2xl font-black text-gray-900">{batchCount}</div>
            <p className="text-xs text-gray-500 mt-1">
              <span className="text-indigo-400 font-bold">{activeBatchCount}</span> in-progress / planned batches
            </p>
          </div>
          <div className="text-[11px] text-gray-500 pt-2 border-t border-gray-200 font-mono">
            Production Service: production_schema
          </div>
        </div>

        {/* Operating Expenses (Sprint 6 Placeholder) */}
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 border border-gray-200 bg-white/30 space-y-3 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Milling Expenses</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-700/10 text-emerald-700 border border-emerald-200">Sprint 6</span>
          </div>
          <div>
            <div className="text-lg font-bold text-gray-500 italic">Not available yet</div>
            <p className="text-xs text-gray-500 mt-1">
              Operating expenses & diesel costing scheduled in Sprint 6.
            </p>
          </div>
          <div className="text-[11px] text-gray-600 pt-2 border-t border-gray-200 font-mono">
            Finance Service: finance_schema (Upcoming)
          </div>
        </div>

        {/* Payroll (Sprint 6 Placeholder) */}
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 border border-gray-200 bg-white/30 space-y-3 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Mill Payroll & Wages</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-700/10 text-emerald-700 border border-emerald-200">Sprint 6</span>
          </div>
          <div>
            <div className="text-lg font-bold text-gray-500 italic">Not available yet</div>
            <p className="text-xs text-gray-500 mt-1">
              Piece-rate boiling and packaging wages scheduled in Sprint 6.
            </p>
          </div>
          <div className="text-[11px] text-gray-600 pt-2 border-t border-gray-200 font-mono">
            Finance Service: finance_schema (Upcoming)
          </div>
        </div>

        {/* Direct Workforce (Sprint 6 Placeholder) */}
        <div className="bg-white border border-gray-200 rounded-xl rounded-2xl p-5 border border-gray-200 bg-white/30 space-y-3 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Mill Workforce</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-700/10 text-emerald-700 border border-emerald-200">Sprint 6</span>
          </div>
          <div>
            <div className="text-lg font-bold text-gray-500 italic">Not available yet</div>
            <p className="text-xs text-gray-500 mt-1">
              Internal employee business records (no login) scheduled in Sprint 6.
            </p>
          </div>
          <div className="text-[11px] text-gray-600 pt-2 border-t border-gray-200 font-mono">
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
          <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
            <Shield className="h-5 w-5 text-emerald-700" />
            User & Access Management (3-Role System)
          </h2>
          <p className="text-xs text-gray-500">
            Authoritative login access: ADMIN, MANAGER, and CUSTOMER. Wholesale accounts have portal logins disabled.
          </p>
        </div>
        <button
          id="btn-add-manager"
          onClick={() => { setShowCreateModal(true); setError(''); setSuccess(''); }}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-xs shadow-md hover:brightness-105 transition"
        >
          <Plus className="h-4 w-4" /> Add Operations Manager
        </button>
      </div>

      {success && (
        <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 rounded-xl text-xs flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{success}</span>
        </div>
      )}

      {error && (
        <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-600 rounded-xl text-xs flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* User Table */}
      <div className="bg-white border border-gray-200 rounded-xl rounded-2xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 space-y-2">
            <Spinner size={6} />
            <span className="text-xs text-gray-500">Loading user accounts…</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-white text-gray-500 uppercase font-semibold border-b border-gray-200">
                <tr>
                  <th className="px-5 py-3.5">User / Full Name</th>
                  <th className="px-5 py-3.5">Email & Phone</th>
                  <th className="px-5 py-3.5">Account Type</th>
                  <th className="px-5 py-3.5">Assigned Roles</th>
                  <th className="px-5 py-3.5">Login Status</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200/60">
                {users.map(u => {
                  return (
                    <tr key={u.id} className="hover:bg-gray-50 transition">
                      <td className="px-5 py-4 font-medium text-gray-800">
                        <div className="font-bold">{u.fullName}</div>
                        {u.businessName && <div className="text-[11px] text-gray-500">{u.businessName}</div>}
                      </td>
                      <td className="px-5 py-4 text-gray-700 font-mono text-[11px]">
                        <div>{u.email}</div>
                        {u.phone && <div className="text-gray-500">{u.phone}</div>}
                      </td>
                      <td className="px-5 py-4">
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${u.customerType === 'INTERNAL' ? 'bg-purple-500/10 text-purple-300 border border-purple-500/20'
                          : u.customerType === 'REGISTERED_WHOLESALE' ? 'bg-emerald-700/10 text-emerald-800 border border-emerald-200'
                            : 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                          }`}>
                          {u.customerType}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap gap-1">
                          {u.roles.map(r => (
                            <span key={r} className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${r === 'ADMIN' ? 'bg-red-500/20 text-red-600 border border-red-500/30'
                              : r === 'MANAGER' ? 'bg-blue-500/20 text-blue-700 border border-blue-500/30'
                                : 'bg-gray-100 text-gray-700 border border-gray-300'
                              }`}>
                              {r}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-5 py-4">
                        <span className={`flex items-center gap-1.5 text-[11px] font-semibold ${u.enabled ? 'text-emerald-700' : 'text-red-600'
                          }`}>
                          <span className={`h-2 w-2 rounded-full ${u.enabled ? 'bg-emerald-400' : 'bg-red-400'}`} />
                          {u.enabled ? 'Enabled' : 'Disabled'}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-right">
                        {u.email !== auth.user?.email && (
                          <button
                            onClick={() => handleToggleEnabled(u.id, u.enabled)}
                            className={`px-3 py-1 rounded-lg text-xs font-semibold border transition ${u.enabled
                              ? 'bg-red-500/10 text-red-600 border-red-500/20 hover:bg-red-500/20'
                              : 'bg-emerald-500/10 text-emerald-700 border-emerald-500/20 hover:bg-emerald-500/20'
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
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-emerald-50/60 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white border border-gray-200 rounded-3xl p-6 sm:p-8 w-full max-w-md shadow-2xl relative">
            <button
              onClick={() => setShowCreateModal(false)}
              className="absolute top-5 right-5 p-1.5 rounded-xl bg-gray-100 text-gray-500 hover:text-gray-900 transition"
            >
              <X className="h-4 w-4" />
            </button>
            <h2 className="text-lg font-bold text-gray-900 mb-2 flex items-center gap-2">
              <UserPlus className="h-5 w-5 text-emerald-700" />
              Add Operations Manager
            </h2>
            <p className="text-xs text-gray-500 mb-4">
              Operations Managers have full operational access to Inventory, Procurement, Mill Production, and Wholesale Ledgers.
            </p>
            <form onSubmit={handleCreateManager} className="space-y-3 text-xs">
              <div>
                <label className="text-gray-500 font-semibold uppercase">Full Name</label>
                <input
                  id="form-manager-name"
                  type="text"
                  required
                  value={createForm.fullName}
                  onChange={e => setCreateForm(f => ({ ...f, fullName: e.target.value }))}
                  placeholder="Your name"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold uppercase">Email Address (Login ID)</label>
                <input
                  id="form-manager-email"
                  type="email"
                  required
                  value={createForm.email}
                  onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))}
                  placeholder="Your email"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold uppercase">Phone Number</label>
                <input
                  id="form-manager-phone"
                  type="tel"
                  value={createForm.phone}
                  onChange={e => setCreateForm(f => ({ ...f, phone: e.target.value }))}
                  placeholder="Your phone number"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <div>
                <label className="text-gray-500 font-semibold uppercase">Initial Password</label>
                <input
                  id="form-manager-password"
                  type="password"
                  required
                  minLength={6}
                  value={createForm.password}
                  onChange={e => setCreateForm(f => ({ ...f, password: e.target.value }))}
                  placeholder="Your password"
                  className="w-full mt-1 bg-white border border-gray-200 rounded-xl px-3 py-2 text-gray-900 focus:outline-none focus:border-emerald-600"
                />
              </div>
              <button
                id="submit-create-manager"
                type="submit"
                disabled={createLoading}
                className="w-full mt-4 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-bold text-xs shadow-lg shadow-amber-600/20 hover:brightness-105 disabled:opacity-50 flex items-center justify-center gap-2 transition"
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

export function AdminDashboard({ auth, onViewInvoice }: { auth: AuthState; onViewInvoice: (orderId: string) => void }) {
  const isAdmin = auth.user?.roles.includes('ADMIN') ?? false;

  const [tab, setTab] = useState<'overview' | 'users' | 'customers' | 'orders' | 'catalogue' | 'operations' | 'production' | 'services' | 'roadmap'>(
    'catalogue'
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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-gray-900 flex items-center gap-3">
            <LayoutDashboard className="h-7 w-7 text-emerald-700" />
            {isAdmin ? 'Admin Master ERP' : 'Operations Manager Portal'}
          </h1>
          <p className="text-xs text-gray-500 mt-1">
            Logged in as <span className="text-emerald-700 font-semibold">{auth.user?.fullName}</span>
            {isAdmin ? (
              <span className="ml-2 px-2 py-0.5 bg-red-500/10 text-red-600 border border-red-500/20 rounded-full text-[10px] font-bold">ADMIN</span>
            ) : (
              <span className="ml-2 px-2 py-0.5 bg-blue-500/10 text-blue-600 border border-blue-500/20 rounded-full text-[10px] font-bold">MANAGER</span>
            )}
          </p>
        </div>
        <div className="flex items-center gap-1 bg-white p-1 rounded-xl border border-gray-200 text-xs overflow-x-auto no-scrollbar whitespace-nowrap lg:flex-wrap w-full sm:w-auto">
          {isAdmin && (
            <button
              id="tab-btn-overview"
              onClick={() => setTab('overview')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'overview' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
            >
              <BarChart3 className="h-3.5 w-3.5 inline mr-1" />Overview
            </button>
          )}
          {isAdmin && (
            <button
              id="tab-btn-users"
              onClick={() => setTab('users')}
              className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'users' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
            >
              <Shield className="h-3.5 w-3.5 inline mr-1" />User Access
            </button>
          )}
          <button
            id="tab-btn-customers"
            onClick={() => setTab('customers')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'customers' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
          >
            <Users className="h-3.5 w-3.5 inline mr-1" />Wholesale & B2B
          </button>
          <button
            id="tab-btn-admin-orders"
            onClick={() => setTab('orders')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'orders' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
          >
            <Receipt className="h-3.5 w-3.5 inline mr-1" />Retail Orders
          </button>
          <button
            id="tab-btn-catalogue"
            onClick={() => setTab('catalogue')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'catalogue' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
          >
            <Package className="h-3.5 w-3.5 inline mr-1" />Product Master
          </button>
          <button
            id="tab-btn-operations"
            onClick={() => setTab('operations')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'operations' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
          >
            <Truck className="h-3.5 w-3.5 inline mr-1" />Inventory & Procurement
          </button>
          <button
            id="tab-btn-production"
            onClick={() => setTab('production')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'production' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
          >
            <Factory className="h-3.5 w-3.5 inline mr-1" />Mill Production
          </button>
          <button
            id="tab-btn-services"
            onClick={() => setTab('services')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'services' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
          >
            <Server className="h-3.5 w-3.5 inline mr-1" />Service Mesh
          </button>
          <button
            id="tab-btn-roadmap"
            onClick={() => setTab('roadmap')}
            className={`px-3 py-1.5 rounded-lg font-semibold transition ${tab === 'roadmap' ? 'bg-emerald-700 text-white' : 'text-gray-500 hover:text-gray-900'}`}
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

      {/* Offline Order Commercial Invoice Modal */}
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
  const [view, setView] = useState<'storefront' | 'orders' | 'admin' | 'profile'>('storefront');
  const [storefrontPage, setStorefrontPage] = useState<StorefrontPage>('shop');
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [confirmedOrder, setConfirmedOrder] = useState<Order | null>(null);
  const [activeInvoiceOrderId, setActiveInvoiceOrderId] = useState<string | null>(null);
  const [cart, setCart] = useState<Cart | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [erpView, setErpView] = useState<'admin' | 'manager'>('admin');

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
    localStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()));
    setAuth({ token, user });
    if (user.roles.includes('ADMIN') || user.roles.includes('MANAGER')) {
      setView('admin');
      if (user.roles.includes('MANAGER') && !user.roles.includes('ADMIN')) {
        setErpView('manager');
      } else {
        setErpView('admin');
      }
    } else {
      setView('storefront');
    }
  }

  const handleLogout = useCallback(() => {
    clearAuthState();
    setAuth({ token: null, user: null });
    setCart(null);
    setView('storefront');
  }, []);

  useEffect(() => {
    if (!auth.token) return;

    const checkSession = () => {
      if (localStorage.getItem(TOKEN_KEY) !== auth.token) {
        handleLogout();
        return true;
      }
      const lastActivity = Number(localStorage.getItem(LAST_ACTIVITY_KEY));
      const elapsed = Date.now() - lastActivity;
      if (!lastActivity || elapsed < 0 || elapsed >= AUTH_INACTIVITY_TIMEOUT_MS) {
        handleLogout();
        return true;
      }
      return false;
    };

    const recordActivity = () => {
      if (checkSession()) return;

      const now = Date.now();
      const lastActivity = Number(localStorage.getItem(LAST_ACTIVITY_KEY));
      if (now - lastActivity >= 15_000) {
        localStorage.setItem(LAST_ACTIVITY_KEY, String(now));
      }
    };

    const activityEvents = ['pointerdown', 'pointermove', 'keydown', 'scroll', 'touchstart', 'focus'];
    activityEvents.forEach(eventName => window.addEventListener(eventName, recordActivity, { passive: true }));
    const intervalId = window.setInterval(checkSession, 15_000);
    window.addEventListener('storage', checkSession);

    return () => {
      activityEvents.forEach(eventName => window.removeEventListener(eventName, recordActivity));
      window.clearInterval(intervalId);
      window.removeEventListener('storage', checkSession);
    };
  }, [auth.token, handleLogout]);

  function handleOrderPlaced(order: Order) {
    setIsCheckoutOpen(false);
    setIsCartOpen(false);
    setCart(null);
    setConfirmedOrder(order);
  }

  return (
    <div className="min-h-screen flex flex-col font-sans bg-gray-50 text-gray-900 overflow-x-hidden w-full max-w-[100vw]">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b backdrop-blur-md bg-white/95 border-emerald-200 shadow-sm transition-colors w-full">
        <div className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8">
          {/* Main Top Header Bar */}
          <div className="h-16 flex items-center justify-between gap-2 sm:gap-4">
            {/* Brand */}
            <div
              className="flex items-center gap-2 sm:gap-3 cursor-pointer shrink-0"
              onClick={() => { setView('storefront'); setStorefrontPage('shop'); }}
            >
              <img src="/rr-logo.png" className="h-8 w-8 sm:h-10 sm:w-10 rounded-full object-cover shadow-sm ring-1 ring-emerald-200" alt="RR Jaggery Logo" />
              <div>
                <div className="flex items-center gap-1.5 sm:gap-2">
                  <span className="font-extrabold text-sm sm:text-xl tracking-tight text-gray-900 font-serif whitespace-nowrap">RR JAGGERY</span>
                  <span className="text-[9px] sm:text-[10px] px-1.5 py-0.5 rounded-full font-bold bg-emerald-100 text-emerald-800 border border-emerald-200 uppercase">
                    TRADERS
                  </span>
                </div>
                <p className="hidden sm:block text-[10px] text-gray-500 font-medium">100% Naturally Made Traditional Jaggery</p>
              </div>
            </div>

            {/* Desktop Search Bar (Flipkart style centered) */}
            {view === 'storefront' && (
              <div className="hidden lg:flex flex-1 max-w-md mx-4">
                <div className="relative w-full">
                  <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-emerald-700" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search pure jaggery blocks, powder, syrup..."
                    className="w-full pl-10 pr-4 py-2 rounded-xl border border-emerald-200 bg-emerald-50/40 text-xs text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white transition"
                  />
                </div>
              </div>
            )}

            {/* Navigation & User controls */}
            <div className="flex items-center gap-1.5 sm:gap-3 shrink-0">
              {/* Desktop View Switcher Pills (Only on laptop/desktop screens - mobile and tablet use bottom nav) */}
              <div className="hidden lg:flex items-center gap-1 p-1 rounded-xl border bg-gray-100 border-gray-200">
                <button
                  id="nav-storefront"
                  onClick={() => { setView('storefront'); setStorefrontPage('shop'); }}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${view === 'storefront' ? 'bg-emerald-700 text-white shadow-sm' : 'text-gray-600 hover:text-emerald-700'}`}
                >
                  <Store className="h-3.5 w-3.5" /> <span>Storefront</span>
                </button>

                {auth.token && (
                  <button
                    id="nav-orders"
                    onClick={() => setView('orders')}
                    className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${view === 'orders' ? 'bg-emerald-700 text-white shadow-sm' : 'text-gray-600 hover:text-emerald-700'}`}
                  >
                    <Receipt className="h-3.5 w-3.5" /> <span>My Orders</span>
                  </button>
                )}

                {auth.token && (
                  <button
                    id="nav-profile"
                    onClick={() => setView('profile')}
                    className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${view === 'profile' ? 'bg-emerald-700 text-white shadow-sm' : 'text-gray-600 hover:text-emerald-700'}`}
                  >
                    <User className="h-3.5 w-3.5" /> <span>Profile</span>
                  </button>
                )}

                {isInternal && (
                  <button
                    id="nav-admin"
                    onClick={() => setView('admin')}
                    className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${view === 'admin' ? 'bg-emerald-700 text-white shadow-sm' : 'text-gray-600 hover:text-emerald-700'}`}
                  >
                    <Building2 className="h-3.5 w-3.5" /> <span>{isAdmin ? 'Admin' : 'Operations'}</span>
                  </button>
                )}
              </div>

              {/* Cart Button */}
              {auth.token && (
                <button
                  id="header-cart-btn"
                  onClick={() => setIsCartOpen(true)}
                  className="relative p-2 sm:p-2.5 rounded-xl border transition bg-white border-gray-200 text-emerald-800 hover:bg-emerald-50 shrink-0"
                  title="Shopping Cart"
                >
                  <ShoppingCart className="h-4 w-4 text-emerald-700" />
                  {cart && cart.totalItems > 0 && (
                    <span id="cart-badge-count" className="absolute -top-1.5 -right-1.5 h-5 w-5 rounded-full bg-emerald-700 text-white font-black text-[10px] flex items-center justify-center shadow-md animate-pulse">
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
                  className="flex items-center gap-1 px-3 sm:px-4 py-2 rounded-xl organic-gradient text-white font-bold text-xs shadow-sm hover:shadow-md transition shrink-0"
                >
                  <LogIn className="h-3.5 w-3.5" /> <span>Sign In</span>
                </button>
              ) : (
                <div className="flex items-center gap-1.5 sm:gap-2">
                  <div
                    onClick={() => setView('profile')}
                    className="hidden lg:block text-right cursor-pointer hover:opacity-80 transition"
                    title="View Profile"
                  >
                    <div id="user-display-name" className="text-xs font-semibold text-gray-900 truncate max-w-[130px]">{auth.user?.fullName}</div>
                    <div className="text-[10px] text-emerald-700 font-bold">
                      {isAdmin ? 'Administrator' : isManager ? 'Operations Manager' : auth.user?.customerType === 'REGISTERED_WHOLESALE' ? 'Wholesale Buyer' : 'Customer'}
                    </div>
                  </div>
                  <button
                    id="logout-btn"
                    onClick={handleLogout}
                    title="Sign out"
                    className="p-2 rounded-lg border transition bg-gray-100 border-gray-200 text-gray-600 hover:text-red-600 shrink-0"
                  >
                    <LogOut className="h-4 w-4" />
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Mobile Search Bar (Flipkart style second row) */}
          {view === 'storefront' && (
            <div className="pb-3 lg:hidden">
              <div className="relative w-full">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-emerald-700" />
                <input
                  id="header-search-input"
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search pure jaggery blocks, powder, syrup..."
                  className="w-full pl-9 pr-3 py-2 rounded-xl border border-emerald-200 bg-emerald-50/50 text-xs text-gray-800 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white transition"
                />
              </div>
            </div>
          )}
        </div>

        {/* Storefront Secondary Sub-navigation Bar */}
        {view === 'storefront' && (
          <div className="w-full bg-white lg:bg-emerald-950/95 border-b border-gray-200/90 lg:border-t lg:border-emerald-900 shadow-[0_1px_3px_rgba(0,0,0,0.03)] lg:shadow-inner py-2 px-3 sm:px-6 lg:px-8">
            <div className="max-w-7xl mx-auto flex items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-2 overflow-x-auto no-scrollbar whitespace-nowrap w-full md:w-auto py-0.5 scroll-smooth">
                <button
                  id="subnav-shop"
                  onClick={() => setStorefrontPage('shop')}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-bold whitespace-nowrap shrink-0 transition-all flex items-center gap-1.5 ${storefrontPage === 'shop'
                    ? 'bg-emerald-700 text-white shadow-sm ring-1 ring-emerald-600 lg:bg-emerald-500 lg:ring-emerald-300/40'
                    : 'bg-emerald-50/70 text-emerald-900 hover:bg-emerald-100 border border-emerald-200/80 lg:bg-emerald-900/60 lg:text-emerald-200 lg:hover:bg-emerald-800 lg:hover:text-white lg:border-emerald-800/60'
                    }`}
                >
                  <Store className="w-3.5 h-3.5 lg:hidden text-emerald-200" />
                  <span>Shop Catalogue</span>
                </button>
                <button
                  id="subnav-about"
                  onClick={() => setStorefrontPage('about')}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-bold whitespace-nowrap shrink-0 transition-all flex items-center gap-1.5 ${storefrontPage === 'about'
                    ? 'bg-emerald-700 text-white shadow-sm ring-1 ring-emerald-600 lg:bg-emerald-500 lg:ring-emerald-300/40'
                    : 'bg-emerald-50/70 text-emerald-900 hover:bg-emerald-100 border border-emerald-200/80 lg:bg-emerald-900/60 lg:text-emerald-200 lg:hover:bg-emerald-800 lg:hover:text-white lg:border-emerald-800/60'
                    }`}
                >
                  <Info className="w-3.5 h-3.5 lg:hidden text-emerald-600" />
                  <span>Our Heritage & About Us</span>
                </button>
                <button
                  id="subnav-reviews"
                  onClick={() => setStorefrontPage('reviews')}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-bold whitespace-nowrap shrink-0 transition-all flex items-center gap-1.5 ${storefrontPage === 'reviews'
                    ? 'bg-emerald-700 text-white shadow-sm ring-1 ring-emerald-600 lg:bg-emerald-500 lg:ring-emerald-300/40'
                    : 'bg-emerald-50/70 text-emerald-900 hover:bg-emerald-100 border border-emerald-200/80 lg:bg-emerald-900/60 lg:text-emerald-200 lg:hover:bg-emerald-800 lg:hover:text-white lg:border-emerald-800/60'
                    }`}
                >
                  <Star className="w-3.5 h-3.5 lg:hidden fill-amber-400 text-amber-500" />
                  <span>Verified Reviews</span>
                </button>
                <button
                  id="subnav-contact"
                  onClick={() => setStorefrontPage('contact')}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-bold whitespace-nowrap shrink-0 transition-all flex items-center gap-1.5 ${storefrontPage === 'contact'
                    ? 'bg-emerald-700 text-white shadow-sm ring-1 ring-emerald-600 lg:bg-emerald-500 lg:ring-emerald-300/40'
                    : 'bg-emerald-50/70 text-emerald-900 hover:bg-emerald-100 border border-emerald-200/80 lg:bg-emerald-900/60 lg:text-emerald-200 lg:hover:bg-emerald-800 lg:hover:text-white lg:border-emerald-800/60'
                    }`}
                >
                  <Phone className="w-3.5 h-3.5 lg:hidden text-emerald-600" />
                  <span>Contact & Distribution</span>
                </button>
              </div>

              <div className="hidden lg:flex items-center gap-4 text-emerald-300 text-[11px] shrink-0 font-medium">
                <span>📞 Order Line: +91 7207063315</span>
                <span>📍 Kankipadu, Andhra Pradesh</span>
              </div>
            </div>
          </div>
        )}
      </header>

      {/* Main Content View Switcher */}
      <main className={`flex-1 w-full max-w-full overflow-x-hidden ${view === 'admin' ? 'p-0 pb-20 lg:pb-0' : 'max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 py-4 sm:py-6 pb-20 lg:pb-6'}`}>
        {view === 'storefront' ? (
          storefrontPage === 'about' ? (
            <AboutPage onBackToStore={() => setStorefrontPage('shop')} onNavigate={(page) => setStorefrontPage(page)} />
          ) : storefrontPage === 'reviews' ? (
            <ReviewsPage auth={auth} />
          ) : storefrontPage === 'contact' ? (
            <ContactPage />
          ) : storefrontPage === 'terms' ? (
            <TermsPage onBackToStore={() => setStorefrontPage('shop')} />
          ) : storefrontPage === 'privacy' ? (
            <PrivacyPage onBackToStore={() => setStorefrontPage('shop')} />
          ) : storefrontPage === 'refund' ? (
            <RefundPolicyPage onBackToStore={() => setStorefrontPage('shop')} />
          ) : storefrontPage === 'shipping' ? (
            <ShippingPolicyPage onBackToStore={() => setStorefrontPage('shop')} />
          ) : (
            <Storefront
              auth={auth}
              onAddToCart={handleAddToCart}
              onNavigate={(page) => setStorefrontPage(page)}
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
            />
          )
        ) : view === 'orders' ? (
          <OrderHistoryView
            auth={auth}
            onViewInvoice={id => setActiveInvoiceOrderId(id)}
            onBackToStore={() => setView('storefront')}
          />
        ) : view === 'profile' && auth.token ? (
          <ProfilePage
            auth={auth}
            onNavigateToOrders={() => setView('orders')}
            onOpenCart={() => setIsCartOpen(true)}
            onNavigateToAdmin={isInternal ? () => setView('admin') : undefined}
            onNavigateToShop={() => { setView('storefront'); setStorefrontPage('shop'); }}
            onOpenAuth={() => setIsAuthModalOpen(true)}
            onLogout={handleLogout}
            cartItemCount={cart?.totalItems ?? 0}
          />
        ) : isInternal ? (
          erpView === 'manager' ? (
            <div className="w-full">
              {isAdmin && (
                <div className="bg-emerald-900 text-white px-4 py-2 flex items-center justify-between text-xs">
                  <span className="font-semibold flex items-center gap-1.5"><Factory className="h-3.5 w-3.5 text-emerald-300" /> Operations & Mill ERP</span>
                  <button
                    id="switch-to-admin-dashboard"
                    onClick={() => setErpView('admin')}
                    className="px-2.5 py-1 bg-white/10 hover:bg-white/20 rounded-lg text-emerald-200 font-medium transition"
                  >
                    Switch to Admin Master & B2B
                  </button>
                </div>
              )}
              <ManagerPortal
                token={auth.token!}
                user={auth.user! as any}
                onLogout={handleLogout}
              />
            </div>
          ) : (
            <div className="w-full max-w-7xl mx-auto px-3 sm:px-6 lg:px-8 py-6">
              <div className="mb-4 flex items-center justify-end">
                <button
                  id="switch-to-manager-portal"
                  onClick={() => setErpView('manager')}
                  className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-emerald-100 hover:bg-emerald-200 text-emerald-900 text-xs font-bold border border-emerald-300 shadow-sm transition"
                >
                  <Factory className="h-3.5 w-3.5 text-emerald-700" />
                  <span>Switch to Unified Operations & Mill ERP</span>
                </button>
              </div>
              <AdminDashboard
                auth={auth}
                onViewInvoice={id => setActiveInvoiceOrderId(id)}
              />
            </div>
          )
        ) : (
          <Storefront
            auth={auth}
            onAddToCart={handleAddToCart}
            onNavigate={(page) => setStorefrontPage(page)}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
          />
        )}
      </main>

      {/* Flipkart-Style Bottom Navigation Bar (Mobile & Tablet - visible across Storefront, Orders, Profile & ERP) */}
      <nav className="fixed bottom-0 inset-x-0 bg-white/95 backdrop-blur-md border-t border-gray-200 z-40 lg:hidden flex items-center justify-around py-2 px-1 shadow-[0_-2px_12px_rgba(0,0,0,0.06)]">
        <button
          id="bottom-nav-shop"
          onClick={() => { setView('storefront'); setStorefrontPage('shop'); }}
          className={`flex flex-col items-center gap-0.5 text-[10px] font-semibold transition ${view === 'storefront' && storefrontPage === 'shop' ? 'text-emerald-700 font-bold' : 'text-gray-500 hover:text-gray-900'
            }`}
        >
          <Store className="h-5 w-5" />
          <span>Shop</span>
        </button>

        <button
          id="bottom-nav-cart"
          onClick={() => setIsCartOpen(true)}
          className="relative flex flex-col items-center gap-0.5 text-[10px] font-semibold text-gray-500 hover:text-gray-900 transition"
        >
          <div className="relative">
            <ShoppingCart className="h-5 w-5 text-emerald-700" />
            {cart && cart.totalItems > 0 && (
              <span className="absolute -top-1.5 -right-2 h-4 w-4 rounded-full bg-emerald-700 text-white font-black text-[9px] flex items-center justify-center shadow">
                {cart.totalItems}
              </span>
            )}
          </div>
          <span>Cart</span>
        </button>

        <button
          id="bottom-nav-orders"
          onClick={() => {
            if (auth.token) {
              setView('orders');
            } else {
              setIsAuthModalOpen(true);
            }
          }}
          className={`flex flex-col items-center gap-0.5 text-[10px] font-semibold transition ${view === 'orders' ? 'text-emerald-700 font-bold' : 'text-gray-500 hover:text-gray-900'
            }`}
        >
          <Receipt className="h-5 w-5" />
          <span>Orders</span>
        </button>

        {auth.token && (
          <button
            id="bottom-nav-profile"
            onClick={() => setView('profile')}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-semibold transition ${view === 'profile' ? 'text-emerald-700 font-bold' : 'text-gray-500 hover:text-gray-900'
              }`}
          >
            <User className="h-5 w-5" />
            <span>Profile</span>
          </button>
        )}

        {isInternal && (
          <button
            id="bottom-nav-admin"
            onClick={() => setView('admin')}
            className={`flex flex-col items-center gap-0.5 text-[10px] font-semibold transition ${view === 'admin' ? 'text-emerald-700 font-bold' : 'text-gray-500 hover:text-gray-900'
              }`}
          >
            <Building2 className="h-5 w-5" />
            <span>ERP</span>
          </button>
        )}
      </nav>

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

      {/* Commercial Invoice Modal */}
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
      {view !== 'admin' && (
        <footer className="bg-emerald-950 text-white border-t border-emerald-900 py-10 px-4 sm:px-6 lg:px-8 mt-auto pb-24 sm:pb-10">
          <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-5 gap-8 mb-8 text-xs">
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <img src="/rr-logo.png" className="h-6 w-6 rounded-full object-cover border border-emerald-400/40" alt="RR Jaggery Logo" />
                <span className="font-extrabold text-base text-white font-serif tracking-tight">RR JAGGERY</span>
              </div>
              <p className="text-emerald-200/90 leading-relaxed">
                Producer of naturally made, Organic jaggery blocks,Powder and syrup crafted with care.
              </p>
            </div>

            <div>
              <h4 className="font-bold text-white text-sm mb-3 font-serif">Quick Navigation</h4>
              <ul className="space-y-2 text-emerald-200/90">
                <li><button onClick={() => { setView('storefront'); setStorefrontPage('shop'); }} className="hover:text-white transition">Shop Catalog</button></li>
                <li><button onClick={() => { setView('storefront'); setStorefrontPage('about'); }} className="hover:text-white transition">About Our Heritage</button></li>
                <li><button onClick={() => { setView('storefront'); setStorefrontPage('reviews'); }} className="hover:text-white transition">Verified Reviews</button></li>
                <li><button onClick={() => { setView('storefront'); setStorefrontPage('contact'); }} className="hover:text-white transition">Contact Us</button></li>
              </ul>
            </div>

            <div>
              <h4 className="font-bold text-white text-sm mb-3 font-serif">Legal & Policies</h4>
              <ul className="space-y-2 text-emerald-200/90">
                <li><button onClick={() => { setView('storefront'); setStorefrontPage('terms'); }} className="hover:text-white transition">Terms & Conditions</button></li>
                <li><button onClick={() => { setView('storefront'); setStorefrontPage('privacy'); }} className="hover:text-white transition">Privacy Policy</button></li>
                <li><button onClick={() => { setView('storefront'); setStorefrontPage('refund'); }} className="hover:text-white transition">Refund & Cancellation</button></li>
                <li><button onClick={() => { setView('storefront'); setStorefrontPage('shipping'); }} className="hover:text-white transition">Shipping Policy</button></li>
              </ul>
            </div>

            <div>
              <h4 className="font-bold text-white text-sm mb-3 font-serif">Quality Standards</h4>
              <ul className="space-y-2 text-emerald-200/90">
                <li>✓ Traditional Natural Processing</li>
                <li>✓ Authentic Traditional Craft</li>
                <li>✓ Naturally Made with Care</li>
                <li>✓ Natural Taste & Aroma</li>
              </ul>
            </div>

            <div>
              <h4 className="font-bold text-white text-sm mb-3 font-serif">Factory Works</h4>
              <p className="text-emerald-200/90 leading-relaxed">
                Kankipadu,<br />
                Andhra Pradesh 521163
              </p>
            </div>
          </div>

          <div className="max-w-7xl mx-auto border-t border-emerald-900 pt-6 text-center text-[11px] text-emerald-300/80">
            <p>© {new Date().getFullYear()} RR Jaggery Traders • Unified Commercial & Mill ERP Platform (Sprint 3 / Sprint 10 Production). All rights reserved.</p>
          </div>
        </footer>
      )}
    </div>
  );
}

export default App;
