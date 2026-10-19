import React from 'react';
import {
  User, Mail, Phone, Building2, Package, ShoppingCart, LogOut,
  LogIn, ShieldCheck, CheckCircle2, ChevronRight, MapPin,
  HelpCircle, Award, Sparkles
} from 'lucide-react';

interface ProfilePageProps {
  auth: {
    token: string | null;
    user: {
      id: string;
      email: string;
      fullName: string;
      phone?: string;
      customerType: 'RETAIL' | 'REGISTERED_WHOLESALE' | 'INTERNAL';
      businessName?: string;
      gstin?: string;
      roles: string[];
      enabled: boolean;
    } | null;
  };
  onNavigateToOrders: () => void;
  onOpenCart: () => void;
  onNavigateToAdmin?: () => void;
  onNavigateToShop: () => void;
  onOpenAuth: () => void;
  onLogout: () => void;
  cartItemCount?: number;
}

export const ProfilePage: React.FC<ProfilePageProps> = ({
  auth,
  onNavigateToOrders,
  onOpenCart,
  onNavigateToAdmin,
  onNavigateToShop,
  onOpenAuth,
  onLogout,
  cartItemCount = 0,
}) => {
  const user = auth.user;
  const isLoggedIn = !!auth.token && !!user;

  const isAdmin = user?.roles.includes('ADMIN') || user?.roles.includes('ROLE_ADMIN') || false;
  const isManager = user?.roles.includes('MANAGER') || user?.roles.includes('ROLE_MANAGER') || false;
  const isInternal = isAdmin || isManager;
  const isWholesale = user?.customerType === 'REGISTERED_WHOLESALE';

  // Get user initials for avatar
  const initials = user?.fullName
    ? user.fullName
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2)
    : 'U';

  return (
    <div className="max-w-4xl mx-auto space-y-5 pb-8 animate-fadeIn">
      {/* ── Top Header / User Card ── */}
      <div className="bg-white rounded-2xl border border-gray-200/90 shadow-sm p-4 sm:p-6 relative overflow-hidden">
        {/* Subtle accent background flourish */}
        <div className="absolute top-0 right-0 -mr-16 -mt-16 w-56 h-56 rounded-full bg-emerald-50 pointer-events-none" />

        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 relative z-10">
          <div className="flex items-center gap-3.5 sm:gap-4">
            {/* Avatar */}
            <div className="h-16 w-16 sm:h-20 sm:w-20 rounded-2xl bg-gradient-to-tr from-emerald-800 to-emerald-600 flex items-center justify-center text-white text-xl sm:text-2xl font-black shadow-md shrink-0 ring-2 ring-emerald-100">
              {isLoggedIn ? initials : <User className="h-8 w-8 text-white/90" />}
            </div>

            {/* User Info */}
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h1 className="text-lg sm:text-2xl font-black text-gray-900 tracking-tight truncate">
                  {isLoggedIn ? user.fullName : 'Guest Visitor'}
                </h1>
                {isLoggedIn && (
                  <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800 border border-emerald-200">
                    <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                    Verified
                  </span>
                )}
              </div>

              <p className="text-xs sm:text-sm text-gray-500 truncate mt-0.5">
                {isLoggedIn ? user.email : 'Sign in to access your orders, wholesale quotes, and mill status'}
              </p>

              {/* Role / Customer Type Badge */}
              {isLoggedIn && (
                <div className="flex items-center gap-2 mt-2">
                  <span
                    className={`inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-0.5 rounded-lg border ${isAdmin
                        ? 'bg-purple-50 text-purple-800 border-purple-200'
                        : isManager
                          ? 'bg-blue-50 text-blue-800 border-blue-200'
                          : isWholesale
                            ? 'bg-amber-50 text-amber-800 border-amber-200'
                            : 'bg-emerald-50 text-emerald-800 border-emerald-200'
                      }`}
                  >
                    {isAdmin
                      ? 'Administrator'
                      : isManager
                        ? 'Operations Manager'
                        : isWholesale
                          ? '⭐ Wholesale Partner'
                          : 'Retail Customer'}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Action button in header */}
          <div className="shrink-0 flex items-center gap-2">
            {!isLoggedIn ? (
              <button
                id="profile-sign-in-cta"
                onClick={onOpenAuth}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white text-xs sm:text-sm font-bold shadow-sm transition active:scale-95"
              >
                <LogIn className="w-4 h-4" />
                Sign In / Register
              </button>
            ) : (
              <button
                id="profile-logout-btn"
                onClick={onLogout}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl border border-rose-200 bg-rose-50/60 hover:bg-rose-100 text-rose-700 text-xs font-bold transition active:scale-95"
              >
                <LogOut className="w-3.5 h-3.5" />
                Sign Out
              </button>
            )}
          </div>
        </div>
      </div>

      {/* ── Quick Action Tiles (Flipkart style 2 or 3 columns) ── */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
        {/* Orders Tile */}
        <button
          id="profile-tile-orders"
          onClick={isLoggedIn ? onNavigateToOrders : onOpenAuth}
          className="flex flex-col items-start p-4 rounded-xl bg-white border border-gray-200 hover:border-emerald-500 hover:shadow-sm transition text-left group"
        >
          <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-700 group-hover:bg-emerald-700 group-hover:text-white transition mb-2.5">
            <Package className="w-5 h-5" />
          </div>
          <span className="text-xs font-bold text-gray-900 group-hover:text-emerald-700 transition">
            My Orders
          </span>
          <span className="text-[11px] text-gray-500 mt-0.5">Track, cancel & invoices</span>
        </button>

        {/* Cart Tile */}
        <button
          id="profile-tile-cart"
          onClick={onOpenCart}
          className="flex flex-col items-start p-4 rounded-xl bg-white border border-gray-200 hover:border-emerald-500 hover:shadow-sm transition text-left group"
        >
          <div className="relative p-2.5 rounded-xl bg-emerald-50 text-emerald-700 group-hover:bg-emerald-700 group-hover:text-white transition mb-2.5">
            <ShoppingCart className="w-5 h-5" />
            {cartItemCount > 0 && (
              <span className="absolute -top-1 -right-1 h-4 w-4 rounded-full bg-emerald-700 text-white text-[9px] font-black flex items-center justify-center">
                {cartItemCount}
              </span>
            )}
          </div>
          <span className="text-xs font-bold text-gray-900 group-hover:text-emerald-700 transition">
            My Cart ({cartItemCount})
          </span>
          <span className="text-[11px] text-gray-500 mt-0.5">View & checkout</span>
        </button>

        {/* Store Catalogue */}
        <button
          id="profile-tile-shop"
          onClick={onNavigateToShop}
          className="flex flex-col items-start p-4 rounded-xl bg-white border border-gray-200 hover:border-emerald-500 hover:shadow-sm transition text-left group"
        >
          <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-700 group-hover:bg-emerald-700 group-hover:text-white transition mb-2.5">
            <Sparkles className="w-5 h-5" />
          </div>
          <span className="text-xs font-bold text-gray-900 group-hover:text-emerald-700 transition">
            Shop Pure Jaggery
          </span>
          <span className="text-[11px] text-gray-500 mt-0.5">Explore mill products</span>
        </button>

        {/* ERP Operations Portal (if internal user) */}
        {isInternal && onNavigateToAdmin && (
          <button
            id="profile-tile-erp"
            onClick={onNavigateToAdmin}
            className="flex flex-col items-start p-4 rounded-xl bg-emerald-50/70 border border-emerald-200 hover:border-emerald-500 hover:shadow-sm transition text-left group"
          >
            <div className="p-2.5 rounded-xl bg-emerald-700 text-white mb-2.5">
              <Building2 className="w-5 h-5" />
            </div>
            <span className="text-xs font-black text-emerald-950 group-hover:text-emerald-700 transition">
              Mill ERP Portal
            </span>
            <span className="text-[11px] text-emerald-700 font-medium mt-0.5">
              {isAdmin ? 'Full Admin & Controls' : 'Mill Production'}
            </span>
          </button>
        )}
      </div>

      {/* ── User Account Information (When Logged In) ── */}
      {isLoggedIn && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Personal Information */}
          <div className="bg-white rounded-2xl border border-gray-200 p-4 sm:p-5 shadow-sm space-y-3.5">
            <h2 className="text-sm font-bold text-gray-900 flex items-center gap-2">
              <User className="w-4 h-4 text-emerald-700" />
              Account Details
            </h2>

            <div className="space-y-2.5 text-xs">
              <div className="flex items-center justify-between py-1.5 border-b border-gray-100">
                <span className="text-gray-500">Full Name</span>
                <span className="font-semibold text-gray-900">{user.fullName}</span>
              </div>
              <div className="flex items-center justify-between py-1.5 border-b border-gray-100">
                <span className="text-gray-500">Email Address</span>
                <span className="font-semibold text-gray-900">{user.email}</span>
              </div>
              {user.phone && (
                <div className="flex items-center justify-between py-1.5 border-b border-gray-100">
                  <span className="text-gray-500">Phone Number</span>
                  <span className="font-semibold text-gray-900">{user.phone}</span>
                </div>
              )}
              <div className="flex items-center justify-between py-1.5 border-b border-gray-100">
                <span className="text-gray-500">Account Type</span>
                <span className="font-semibold text-emerald-700">{user.customerType}</span>
              </div>
              <div className="flex items-center justify-between py-1.5">
                <span className="text-gray-500">Account Status</span>
                <span className="inline-flex items-center gap-1 font-semibold text-emerald-700">
                  <ShieldCheck className="w-3.5 h-3.5" /> Active & Verified
                </span>
              </div>
            </div>
          </div>

          {/* Wholesale / Business Info OR Delivery Info */}
          <div className="bg-white rounded-2xl border border-gray-200 p-4 sm:p-5 shadow-sm space-y-3.5">
            <h2 className="text-sm font-bold text-gray-900 flex items-center gap-2">
              <Building2 className="w-4 h-4 text-emerald-700" />
              {isWholesale ? 'Business & Wholesale Info' : 'Delivery & Fulfillment'}
            </h2>

            {isWholesale ? (
              <div className="space-y-2.5 text-xs">
                <div className="flex items-center justify-between py-1.5 border-b border-gray-100">
                  <span className="text-gray-500">Business Name</span>
                  <span className="font-semibold text-gray-900">
                    {user.businessName || 'Verified Wholesale Partner'}
                  </span>
                </div>
                <div className="flex items-center justify-between py-1.5 border-b border-gray-100">
                  <span className="text-gray-500">GSTIN</span>
                  <span className="font-mono font-semibold text-gray-900">
                    {user.gstin || 'Registered on file'}
                  </span>
                </div>
                <div className="flex items-center justify-between py-1.5 border-b border-gray-100">
                  <span className="text-gray-500">Pricing Tier</span>
                  <span className="font-bold text-emerald-700">B2B Wholesale (Tier 1)</span>
                </div>
                <div className="flex items-center justify-between py-1.5">
                  <span className="text-gray-500">Commercial Invoices</span>
                  <span className="text-emerald-700 font-semibold">Available for all orders</span>
                </div>
              </div>
            ) : (
              <div className="space-y-2.5 text-xs">
                <div className="flex items-start gap-2.5 p-3 rounded-xl bg-emerald-50/50 border border-emerald-100 text-gray-700">
                  <MapPin className="w-4 h-4 text-emerald-700 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold text-gray-900 block">Default Delivery Region</span>
                    <span className="text-[11px] text-gray-600 block mt-0.5">
                      Direct Mill dispatch from Vallurupallem, Andhra Pradesh across all Indian pincodes via express logistics.
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between py-1.5 border-b border-gray-100">
                  <span className="text-gray-500">Packaging</span>
                  <span className="font-semibold text-gray-900">Food-grade airtight sealed</span>
                </div>
                <div className="flex items-center justify-between py-1.5">
                  <span className="text-gray-500">Dispatch Timeline</span>
                  <span className="font-semibold text-emerald-700">Within 24–48 hours</span>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── Direct Support & Mill Quality Assurance ── */}
      <div className="bg-white rounded-2xl border border-gray-200 p-4 sm:p-5 shadow-sm space-y-3">
        <h2 className="text-sm font-bold text-gray-900 flex items-center gap-2">
          <HelpCircle className="w-4 h-4 text-emerald-700" />
          Customer Support & Mill Help Desk
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
          <div className="p-3 rounded-xl bg-gray-50 border border-gray-100 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-emerald-100 text-emerald-800 shrink-0">
              <Phone className="w-4 h-4" />
            </div>
            <div>
              <span className="text-[10px] text-gray-500 font-medium block">Order Helpline</span>
              <span className="font-bold text-gray-900 block">+91 9948533315</span>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-gray-50 border border-gray-100 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-emerald-100 text-emerald-800 shrink-0">
              <Mail className="w-4 h-4" />
            </div>
            <div>
              <span className="text-[10px] text-gray-500 font-medium block">Email Support</span>
              <a href="mailto:rrjaggery@gmail.com" className="font-bold text-gray-900 block hover:text-emerald-700 transition">rrjaggery@gmail.com</a>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-gray-50 border border-gray-100 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-emerald-100 text-emerald-800 shrink-0">
              <MapPin className="w-4 h-4" />
            </div>
            <div>
              <span className="text-[10px] text-gray-500 font-medium block">Mill Location</span>
              <span className="font-bold text-gray-900 block">Vallurupallem, AP</span>
            </div>
          </div>
        </div>
      </div>

      {/* ── Trust & Quality Guarantee ── */}
      <div className="p-4 rounded-2xl bg-gradient-to-r from-emerald-50 to-teal-50 border border-emerald-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-emerald-700 text-white shrink-0">
            <Award className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-bold text-gray-900 block">
              100% Pure, Naturally Clarified Sugarcane Jaggery
            </span>
            <span className="text-[11px] text-gray-600 block mt-0.5">
              Zero refined sugar, zero chemicals, zero artificial preservatives. Laboratory tested purity.
            </span>
          </div>
        </div>
        <button
          onClick={onNavigateToShop}
          className="self-start sm:self-auto inline-flex items-center gap-1 text-xs font-bold text-emerald-800 hover:text-emerald-950 shrink-0"
        >
          Explore Catalog <ChevronRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
