import { useState } from 'react';
import { 
  Building2, 
  Store, 
  LayoutDashboard, 
  Factory, 
  Boxes, 
  Receipt, 
  Users, 
  ShieldCheck, 
  CheckCircle2, 
  ArrowRight,
  TrendingUp,
  Flame,
  FileSpreadsheet,
  Server
} from 'lucide-react';

interface ServiceNode {
  name: string;
  code: string;
  port: number;
  schema: string;
  role: string;
  status: 'ONLINE' | 'STANDBY' | 'INITIALIZING';
  description: string;
}

const SERVICES: ServiceNode[] = [
  { name: 'Auth Service', code: 'auth-service', port: 8081, schema: 'auth_schema', role: 'Security & Identity', status: 'ONLINE', description: 'JWT tokens, role-based access control, security audits' },
  { name: 'Commerce Service', code: 'commerce-service', port: 8082, schema: 'commerce_schema', role: 'Catalog & Retail', status: 'ONLINE', description: 'Products, categories, shopping cart, online order checkout' },
  { name: 'Customer & Ledger Service', code: 'customer-ledger-service', port: 8083, schema: 'customer_schema', role: 'Wholesale & Ledgers', status: 'ONLINE', description: 'Retail, B2B, offline wholesale without login, double-entry ledgers' },
  { name: 'Inventory Service', code: 'inventory-service', port: 8084, schema: 'inventory_schema', role: 'Stock & Movements', status: 'ONLINE', description: 'Raw cane, fuel, finished jaggery, append-only stock movement log' },
  { name: 'Procurement Service', code: 'procurement-service', port: 8085, schema: 'procurement_schema', role: 'Cane Inward & POs', status: 'ONLINE', description: 'Farmer suppliers, weighment receipts, purchase orders & ledger' },
  { name: 'Production Service', code: 'production-service', port: 8086, schema: 'production_schema', role: 'Batch Processing', status: 'ONLINE', description: 'BOM recipes, crushing/boiling stages, yield %, wastage & cost/kg' },
  { name: 'Finance Service', code: 'finance-service', port: 8087, schema: 'finance_schema', role: 'Costing & Payroll', status: 'ONLINE', description: 'Factory expenses, batch cost allocation, piece-rate worker wages' },
  { name: 'Notification Service', code: 'notification-service', port: 8088, schema: 'notification_schema', role: 'Alerts & Messages', status: 'ONLINE', description: 'Low stock alerts, credit overdue warnings, dispatch receipts' },
];

export function App() {
  const [activeView, setActiveView] = useState<'storefront' | 'admin'>('admin');
  const [activeAdminTab, setActiveAdminTab] = useState<'overview' | 'services' | 'roadmap'>('overview');

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Top Brand Navigation Header */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="h-10 w-10 rounded-xl jaggery-gradient flex items-center justify-center shadow-lg shadow-amber-900/40">
              <Flame className="h-6 w-6 text-amber-100" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-extrabold text-xl tracking-tight text-white">RR JAGGERY</span>
                <span className="text-xs px-2 py-0.5 rounded-full font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  TRADERS ERP
                </span>
              </div>
              <p className="text-[10px] text-slate-400">Pure Organic Jaggery • Manufacturing • B2B Ledger</p>
            </div>
          </div>

          {/* Persona Mode Switcher */}
          <div className="flex items-center space-x-2 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
            <button
              onClick={() => setActiveView('storefront')}
              className={`flex items-center space-x-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeView === 'storefront'
                  ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Store className="h-3.5 w-3.5" />
              <span>Customer Storefront</span>
            </button>
            <button
              onClick={() => setActiveView('admin')}
              className={`flex items-center space-x-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeView === 'admin'
                  ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Building2 className="h-3.5 w-3.5" />
              <span>Admin & Mill ERP</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeView === 'storefront' ? (
          /* Storefront Preview Shell */
          <div className="space-y-8 animate-fadeIn">
            <div className="relative overflow-hidden rounded-3xl border border-amber-500/20 bg-gradient-to-r from-amber-950/40 via-slate-900/60 to-slate-950 p-8 md:p-12">
              <div className="max-w-2xl space-y-4">
                <span className="inline-flex items-center space-x-2 text-xs font-semibold uppercase tracking-wider text-amber-400 bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20">
                  <ShieldCheck className="h-3.5 w-3.5" />
                  <span>100% Organic • Direct From Mandya Mills</span>
                </span>
                <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white leading-tight">
                  Pure Traditional Jaggery, <span className="jaggery-text-gradient">Engineered For Quality</span>
                </h1>
                <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
                  Wholesale & retail supply of chemical-free jaggery blocks, granulated powders, and artisanal cubes. Direct farm sourcing with transparent batch traceability.
                </p>
                <div className="flex flex-wrap gap-3 pt-2">
                  <button className="px-5 py-2.5 rounded-xl jaggery-gradient text-slate-950 font-bold text-sm shadow-lg shadow-amber-600/30 hover:brightness-110 flex items-center space-x-2">
                    <span>Browse Catalogue</span>
                    <ArrowRight className="h-4 w-4" />
                  </button>
                  <button className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 font-semibold text-sm border border-slate-700">
                    B2B Wholesale Inquiries
                  </button>
                </div>
              </div>
            </div>

            {/* Product Catalogue Mock Preview */}
            <div>
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="text-xl font-bold text-white">Featured Farm Jaggery</h2>
                  <p className="text-xs text-slate-400">Available for retail online order and bulk B2B dispatch</p>
                </div>
                <span className="text-xs text-amber-400 font-medium">Sprint 1 Feature Preview</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {[
                  { name: 'Organic Traditional Block Jaggery', weight: '10 KG Box', retail: '₹550', wholesale: '₹420 / box (MOQ 20)', grade: 'Grade A Traditional' },
                  { name: 'Sulphur-Free Granular Jaggery Powder', weight: '1 KG Pouch', retail: '₹85', wholesale: '₹62 / kg (MOQ 50kg)', grade: 'Premium Crystal' },
                  { name: 'Pure Sugarcane Moulded Cubes', weight: '500g Jar', retail: '₹65', wholesale: '₹48 / jar (MOQ 30)', grade: 'Export Grade' },
                ].map((item, idx) => (
                  <div key={idx} className="glass-card rounded-2xl p-5 flex flex-col justify-between hover:shadow-xl transition-all">
                    <div>
                      <div className="flex items-center justify-between mb-3">
                        <span className="text-[10px] uppercase font-bold tracking-wider px-2.5 py-1 rounded-md bg-amber-500/10 text-amber-300 border border-amber-500/20">
                          {item.grade}
                        </span>
                        <span className="text-xs font-semibold text-slate-400">{item.weight}</span>
                      </div>
                      <h3 className="font-bold text-base text-white mb-2">{item.name}</h3>
                      <div className="space-y-1 my-4 bg-slate-900/60 p-3 rounded-xl border border-slate-800">
                        <div className="flex justify-between text-xs">
                          <span className="text-slate-400">Retail Price:</span>
                          <span className="font-bold text-white">{item.retail}</span>
                        </div>
                        <div className="flex justify-between text-xs">
                          <span className="text-amber-400 font-medium">Wholesale Rate:</span>
                          <span className="font-bold text-amber-400">{item.wholesale}</span>
                        </div>
                      </div>
                    </div>
                    <button className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-slate-200 border border-slate-700 transition">
                      Add to Cart (Coming in Sprint 2)
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : (
          /* Admin / Mill ERP Shell */
          <div className="space-y-8 animate-fadeIn">
            {/* Admin Header Stats */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
              <div>
                <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white flex items-center space-x-3">
                  <LayoutDashboard className="h-7 w-7 text-amber-500" />
                  <span>Operations & ERP Center</span>
                </h1>
                <p className="text-xs text-slate-400 mt-1">
                  Unified enterprise management for cane procurement, batch processing, offline wholesale and financial ledgers.
                </p>
              </div>

              {/* Subtabs */}
              <div className="flex items-center space-x-2 bg-slate-900 p-1 rounded-xl border border-slate-800 text-xs">
                <button
                  onClick={() => setActiveAdminTab('overview')}
                  className={`px-3 py-1.5 rounded-lg font-semibold transition ${
                    activeAdminTab === 'overview' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  System Matrix
                </button>
                <button
                  onClick={() => setActiveAdminTab('services')}
                  className={`px-3 py-1.5 rounded-lg font-semibold transition ${
                    activeAdminTab === 'services' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  Service Mesh ({SERVICES.length})
                </button>
                <button
                  onClick={() => setActiveAdminTab('roadmap')}
                  className={`px-3 py-1.5 rounded-lg font-semibold transition ${
                    activeAdminTab === 'roadmap' ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  Sprint Roadmap
                </button>
              </div>
            </div>

            {activeAdminTab === 'overview' && (
              <div className="space-y-8">
                {/* Metric Summary Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="glass-card rounded-2xl p-5">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-xs font-semibold text-slate-400">Total Services</span>
                      <Server className="h-4 w-4 text-amber-400" />
                    </div>
                    <div className="text-2xl font-black text-white">8 Services</div>
                    <p className="text-[11px] text-emerald-400 mt-1 flex items-center">
                      <CheckCircle2 className="h-3 w-3 mr-1 inline" /> 100% Java 21 / Spring Boot 3
                    </p>
                  </div>

                  <div className="glass-card rounded-2xl p-5">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-xs font-semibold text-slate-400">Database Schemas</span>
                      <FileSpreadsheet className="h-4 w-4 text-cyan-400" />
                    </div>
                    <div className="text-2xl font-black text-white">8 Isolated</div>
                    <p className="text-[11px] text-slate-400 mt-1">
                      PostgreSQL 16 Multi-Schema
                    </p>
                  </div>

                  <div className="glass-card rounded-2xl p-5">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-xs font-semibold text-slate-400">Wholesale Architecture</span>
                      <Users className="h-4 w-4 text-indigo-400" />
                    </div>
                    <div className="text-2xl font-black text-white">3 Tiers</div>
                    <p className="text-[11px] text-amber-400 mt-1 font-medium">
                      Retail • B2B • Offline (No Login)
                    </p>
                  </div>

                  <div className="glass-card rounded-2xl p-5">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-xs font-semibold text-slate-400">Inventory Integrity</span>
                      <Boxes className="h-4 w-4 text-emerald-400" />
                    </div>
                    <div className="text-2xl font-black text-white">Append-Only</div>
                    <p className="text-[11px] text-emerald-400 mt-1">
                      Zero Direct Overwrites
                    </p>
                  </div>
                </div>

                {/* Core Workflow Visualizer */}
                <div className="glass-card rounded-3xl p-6 sm:p-8 space-y-6">
                  <div>
                    <h2 className="text-lg font-bold text-white flex items-center space-x-2">
                      <TrendingUp className="h-5 w-5 text-amber-400" />
                      <span>End-to-End Enterprise Flow Architecture</span>
                    </h2>
                    <p className="text-xs text-slate-400 mt-1">
                      Visual representation of the verified data pathway from cane grower to cash settlement
                    </p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-5 gap-3 text-center">
                    {[
                      { icon: Factory, step: '1. Sourcing', title: 'Cane Procurement', detail: 'Farmer weighbridge & GRN receipt' },
                      { icon: Flame, step: '2. Processing', title: 'Batch Production', detail: 'Crushing, boiling, yield & cost/KG' },
                      { icon: Boxes, step: '3. Inventory', title: 'Stock Ledger', detail: 'Raw & finished auditable movements' },
                      { icon: Building2, step: '4. Commercial', title: 'Offline / B2B Sale', detail: 'Admin offline orders & tax invoices' },
                      { icon: Receipt, step: '5. Finance', title: 'Customer Ledger', detail: 'Double-entry debit/credit & dues' },
                    ].map((step, idx) => {
                      const Icon = step.icon;
                      return (
                        <div key={idx} className="bg-slate-900/80 p-4 rounded-2xl border border-slate-800 flex flex-col items-center justify-between space-y-2">
                          <span className="text-[10px] font-bold text-amber-400 uppercase tracking-widest">{step.step}</span>
                          <div className="h-10 w-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center my-1">
                            <Icon className="h-5 w-5 text-amber-400" />
                          </div>
                          <div className="font-bold text-sm text-white">{step.title}</div>
                          <div className="text-[11px] text-slate-400">{step.detail}</div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            )}

            {activeAdminTab === 'services' && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-lg font-bold text-white">Sprint 0 Service Skeletons</h2>
                    <p className="text-xs text-slate-400">Strict logical domain boundaries running on dedicated ports with isolated schemas</p>
                  </div>
                  <span className="text-xs px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-bold">
                    8/8 Skeletons Active
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {SERVICES.map((svc) => (
                    <div key={svc.code} className="glass-card rounded-2xl p-5 space-y-3">
                      <div className="flex items-start justify-between">
                        <div>
                          <div className="flex items-center space-x-2">
                            <h3 className="font-bold text-base text-white">{svc.name}</h3>
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400 font-mono font-bold">
                              :{svc.port}
                            </span>
                          </div>
                          <span className="text-xs text-slate-400">{svc.role}</span>
                        </div>
                        <span className="flex items-center space-x-1.5 text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                          <span>{svc.status}</span>
                        </span>
                      </div>

                      <p className="text-xs text-slate-300">{svc.description}</p>

                      <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400 font-mono">
                        <span>Schema: <strong className="text-amber-400">{svc.schema}</strong></span>
                        <span>REST: /api/v1/{svc.code.split('-')[0]}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {activeAdminTab === 'roadmap' && (
              <div className="glass-card rounded-3xl p-6 sm:p-8 space-y-6">
                <div>
                  <h2 className="text-lg font-bold text-white">Agile Sprint Delivery Roadmap</h2>
                  <p className="text-xs text-slate-400 mt-1">Incremental verifiable implementation schedule adhering to official specification</p>
                </div>

                <div className="space-y-3">
                  {[
                    { sprint: 'Sprint 0', name: 'Foundation & Architecture', status: 'IN_PROGRESS', desc: 'Architecture docs, React shell, 8 Spring Boot services, Docker Compose, PostgreSQL schemas, CI' },
                    { sprint: 'Sprint 1', name: 'Authentication & Product Catalogue', status: 'PENDING', desc: 'JWT login, RBAC roles, product catalog, categories, pricing tiers (retail vs wholesale)' },
                    { sprint: 'Sprint 2', name: 'Cart, Checkout & Orders', status: 'PENDING', desc: 'Customer shopping cart, online order lifecycle, order tracking, invoice generation' },
                    { sprint: 'Sprint 3', name: 'Customer, Wholesale & Ledger', status: 'PENDING', desc: 'Retail, B2B wholesale, offline wholesalers (no login), credit limits, ledger accounting' },
                    { sprint: 'Sprint 4', name: 'Inventory & Procurement', status: 'PENDING', desc: 'Raw material & finished goods inventory, immutable stock movements, cane supplier POs' },
                    { sprint: 'Sprint 5', name: 'Production Management', status: 'PENDING', desc: 'Recipes/BOM, production batches, stages, material consumption, output, yield & wastage' },
                    { sprint: 'Sprint 6', name: 'Costing, Expenses & Payroll', status: 'PENDING', desc: 'Operating expenses, batch costing per KG, employee attendance & piece-rate wages' },
                    { sprint: 'Sprint 7', name: 'Dashboard, Reports & Notifications', status: 'PENDING', desc: 'Executive dashboard, receivables/payables, low stock alerts, dispatch notifications' },
                    { sprint: 'Sprint 8', name: 'Hardening, Security & Production Readiness', status: 'PENDING', desc: 'End-to-end integration tests, security review, automated backups, Docker optimizations' },
                    { sprint: 'Sprint 9', name: 'Production Deployment & Stabilization', status: 'PENDING', desc: 'Low-cost VPS deployment, domain/SSL configuration, smoke testing and runbooks' },
                  ].map((s, idx) => (
                    <div key={idx} className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="space-y-1">
                        <div className="flex items-center space-x-2">
                          <span className="font-bold text-sm text-white">{s.sprint}: {s.name}</span>
                        </div>
                        <p className="text-xs text-slate-400">{s.desc}</p>
                      </div>
                      <span className={`text-xs px-2.5 py-1 rounded-full font-bold uppercase tracking-wider self-start sm:self-center ${
                        s.status === 'IN_PROGRESS' 
                          ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40 animate-pulse'
                          : 'bg-slate-800 text-slate-400 border border-slate-700'
                      }`}>
                        {s.status === 'IN_PROGRESS' ? 'Current Sprint' : 'Upcoming'}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800 bg-slate-900/40 py-6 text-center text-xs text-slate-500">
        <p>RR Jaggery Traders • Unified Commercial & Mill ERP Platform • Version 1.0 (Sprint 0 Foundation)</p>
      </footer>
    </div>
  );
}

export default App;

