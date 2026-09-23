import { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  ClipboardList,
  Eye,
  Factory,
  Flame,
  Layers,
  Percent,
  Plus,
  RefreshCw,
  Scale,
  Trash2,
  X,
  AlertTriangle
} from 'lucide-react';

const PRODUCTION_BASE = '/api/v1/production';

type Props = {
  token: string;
  userName?: string;
};

export type BatchStatus = 'PLANNED' | 'MATERIALS_READY' | 'IN_PRODUCTION' | 'QUALITY_CHECK' | 'COMPLETED' | 'CANCELLED';

export interface RecipeItemDto {
  id?: string;
  rawMaterialSku: string;
  rawMaterialName: string;
  requiredQuantity: number;
  unitOfMeasure?: string;
  notes?: string;
}

export interface RecipeDto {
  id: string;
  recipeCode: string;
  recipeName: string;
  outputProductSku: string;
  outputProductName: string;
  standardBatchSize: number;
  unitOfMeasure: string;
  standardYieldPercentage?: number;
  estimatedDurationMinutes?: number;
  isActive?: boolean;
  description?: string;
  createdAt?: string;
  items: RecipeItemDto[];
}

export interface BatchConsumption {
  id: string;
  rawMaterialSku: string;
  rawMaterialName: string;
  plannedQuantity?: number;
  consumedQuantity: number;
  unitOfMeasure: string;
  consumedAt: string;
  actor?: string;
  notes?: string;
}

export interface BatchOutput {
  id: string;
  outputProductSku: string;
  outputProductName: string;
  quantityProduced: number;
  unitOfMeasure: string;
  batchLot?: string;
  qualityGrade?: string;
  producedAt: string;
  actor?: string;
  notes?: string;
}

export interface BatchWastage {
  id: string;
  materialSku: string;
  materialName: string;
  wastageQuantity: number;
  unitOfMeasure: string;
  reason?: string;
  wastageType?: string;
  recordedAt: string;
  actor?: string;
}

export interface ProductionBatchDto {
  id: string;
  batchNumber: string;
  recipeId?: string;
  recipeCode?: string;
  recipeName?: string;
  targetProductSku: string;
  targetProductName: string;
  plannedQuantity: number;
  actualQuantity: number;
  unitOfMeasure: string;
  status: BatchStatus;
  plannedStartDate?: string;
  actualStartDate?: string;
  completionDate?: string;
  supervisor?: string;
  batchLot?: string;
  notes?: string;
  totalWastageQuantity: number;
  yieldPercentage?: number;
  createdAt?: string;
  updatedAt?: string;
  consumptions: BatchConsumption[];
  outputs: BatchOutput[];
  wastages: BatchWastage[];
}

export interface MaterialAvailabilityItem {
  rawMaterialSku: string;
  rawMaterialName: string;
  requiredQuantity: number;
  currentStock: number;
  allocatedStock: number;
  availableStock: number;
  unitOfMeasure: string;
  sufficient: boolean;
  shortfall: number;
}

export interface MaterialAvailabilityDto {
  batchId: string;
  batchNumber: string;
  allMaterialsAvailable: boolean;
  materials: MaterialAvailabilityItem[];
}

function authHeaders(token: string) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`
  };
}

async function apiRequest<T>(url: string, token: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: { ...authHeaders(token), ...(init?.headers || {}) }
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.message || body.error || (Array.isArray(body.errors) ? body.errors.join(', ') : null) || `Request failed (${response.status})`);
  }
  return (body.data ?? body) as T;
}

const statusColors: Record<BatchStatus, { bg: string; text: string; border: string }> = {
  PLANNED: { bg: 'bg-blue-500/10', text: 'text-blue-400', border: 'border-blue-500/30' },
  MATERIALS_READY: { bg: 'bg-cyan-500/10', text: 'text-cyan-400', border: 'border-cyan-500/30' },
  IN_PRODUCTION: { bg: 'bg-amber-500/10', text: 'text-amber-400', border: 'border-amber-500/30' },
  QUALITY_CHECK: { bg: 'bg-purple-500/10', text: 'text-purple-400', border: 'border-purple-500/30' },
  COMPLETED: { bg: 'bg-emerald-500/10', text: 'text-emerald-400', border: 'border-emerald-500/30' },
  CANCELLED: { bg: 'bg-rose-500/10', text: 'text-rose-400', border: 'border-rose-500/30' }
};

export function Sprint5Production({ token, userName = 'PRODUCTION_MANAGER' }: Props) {
  const [subTab, setSubTab] = useState<'batches' | 'recipes'>('batches');
  const [statusFilter, setStatusFilter] = useState<BatchStatus | 'ALL'>('ALL');
  const [batches, setBatches] = useState<ProductionBatchDto[]>([]);
  const [recipes, setRecipes] = useState<RecipeDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Modals
  const [activeModal, setActiveModal] = useState<
    'newRecipe' | 'newBatch' | 'availability' | 'consume' | 'output' | 'wastage' | 'details' | null
  >(null);
  const [selectedBatch, setSelectedBatch] = useState<ProductionBatchDto | null>(null);
  const [availabilityData, setAvailabilityData] = useState<MaterialAvailabilityDto | null>(null);
  const [recipeForBatch, setRecipeForBatch] = useState<RecipeDto | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [batchList, recipeList] = await Promise.all([
        apiRequest<ProductionBatchDto[]>(`${PRODUCTION_BASE}/batches`, token),
        apiRequest<RecipeDto[]>(`${PRODUCTION_BASE}/recipes`, token)
      ]);
      setBatches(batchList || []);
      setRecipes(recipeList || []);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const filteredBatches = batches.filter(b => statusFilter === 'ALL' || b.status === statusFilter);

  // Statistics
  const totalBatches = batches.length;
  const activeBatches = batches.filter(b => b.status === 'IN_PRODUCTION' || b.status === 'QUALITY_CHECK' || b.status === 'MATERIALS_READY').length;
  const completedBatches = batches.filter(b => b.status === 'COMPLETED');
  const totalFinishedKg = completedBatches.reduce((acc, b) => acc + (Number(b.actualQuantity) || 0), 0);
  const avgYield = completedBatches.length > 0
    ? (completedBatches.reduce((acc, b) => acc + (Number(b.yieldPercentage) || 0), 0) / completedBatches.length).toFixed(1)
    : '0.0';

  // Modal Triggers
  const openAvailability = async (batch: ProductionBatchDto) => {
    setSelectedBatch(batch);
    setError('');
    try {
      const data = await apiRequest<MaterialAvailabilityDto>(`${PRODUCTION_BASE}/batches/${batch.id}/availability`, token);
      setAvailabilityData(data);
      setActiveModal('availability');
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const openDetails = (batch: ProductionBatchDto) => {
    setSelectedBatch(batch);
    setActiveModal('details');
  };

  const openConsume = (batch: ProductionBatchDto) => {
    setSelectedBatch(batch);
    setActiveModal('consume');
  };

  const openOutput = (batch: ProductionBatchDto) => {
    setSelectedBatch(batch);
    setActiveModal('output');
  };

  const openWastage = (batch: ProductionBatchDto) => {
    setSelectedBatch(batch);
    setActiveModal('wastage');
  };

  const updateStatus = async (batchId: string, nextStatus: BatchStatus, note: string) => {
    setError('');
    setNotice('');
    try {
      await apiRequest(`${PRODUCTION_BASE}/batches/${batchId}/status`, token, {
        method: 'PATCH',
        body: JSON.stringify({ status: nextStatus, actor: userName, notes: note })
      });
      setNotice(`Batch transitioned to ${nextStatus} successfully.`);
      setActiveModal(null);
      await loadData();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header & Metrics */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h2 className="text-2xl font-black text-white flex items-center gap-3">
            <Factory className="h-7 w-7 text-amber-500" />
            Mill Production & Batch Management
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Bill of Materials (Recipes), material consumption, output lots, yield & wastage tracking
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={loadData}
            title="Refresh"
            className="p-2.5 rounded-xl border border-slate-700 bg-slate-900/80 hover:bg-slate-800 transition"
          >
            <RefreshCw className={`h-4 w-4 text-amber-400 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            id="btn-new-recipe"
            onClick={() => setActiveModal('newRecipe')}
            className="px-3.5 py-2 rounded-xl border border-amber-500/40 bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 font-bold text-xs transition flex items-center gap-1.5"
          >
            <Plus className="h-3.5 w-3.5" />
            New Recipe
          </button>
          <button
            id="btn-new-batch"
            onClick={() => {
              setRecipeForBatch(null);
              setActiveModal('newBatch');
            }}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 text-slate-950 font-bold text-xs hover:brightness-110 shadow-lg shadow-amber-500/20 transition flex items-center gap-1.5"
          >
            <Flame className="h-3.5 w-3.5" />
            Start Batch
          </button>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-card rounded-2xl p-4 border border-slate-800 bg-slate-900/40 space-y-1">
          <div className="flex items-center justify-between text-slate-400 text-xs font-semibold">
            <span>Total Batches</span>
            <ClipboardList className="h-4 w-4 text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white">{totalBatches}</div>
          <div className="text-[11px] text-slate-500">{recipes.length} BOM Recipes Defined</div>
        </div>
        <div className="glass-card rounded-2xl p-4 border border-slate-800 bg-slate-900/40 space-y-1">
          <div className="flex items-center justify-between text-slate-400 text-xs font-semibold">
            <span>Active in Mill</span>
            <Flame className="h-4 w-4 text-amber-400 animate-pulse" />
          </div>
          <div className="text-2xl font-black text-amber-400">{activeBatches}</div>
          <div className="text-[11px] text-slate-500">In Prep, Boiling & QA</div>
        </div>
        <div className="glass-card rounded-2xl p-4 border border-slate-800 bg-slate-900/40 space-y-1">
          <div className="flex items-center justify-between text-slate-400 text-xs font-semibold">
            <span>Completed Output</span>
            <Scale className="h-4 w-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">{totalFinishedKg.toLocaleString()} KG</div>
          <div className="text-[11px] text-slate-500">{completedBatches.length} Finished Batches</div>
        </div>
        <div className="glass-card rounded-2xl p-4 border border-slate-800 bg-slate-900/40 space-y-1">
          <div className="flex items-center justify-between text-slate-400 text-xs font-semibold">
            <span>Average Yield</span>
            <Percent className="h-4 w-4 text-purple-400" />
          </div>
          <div className="text-2xl font-black text-purple-400">{avgYield}%</div>
          <div className="text-[11px] text-slate-500">Actual vs Planned Yield</div>
        </div>
      </div>

      {/* Alerts */}
      {error && (
        <div className="p-3.5 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-300 text-sm flex items-start gap-2.5">
          <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}
      {notice && (
        <div className="p-3.5 rounded-xl border border-emerald-500/30 bg-emerald-500/10 text-emerald-300 text-sm flex items-start gap-2.5">
          <CheckCircle2 className="h-4 w-4 shrink-0 mt-0.5" />
          <span>{notice}</span>
        </div>
      )}

      {/* Tab Navigation */}
      <div className="flex items-center justify-between">
        <div className="flex gap-1 bg-slate-900/80 p-1 rounded-xl border border-slate-800 text-xs w-fit">
          <button
            id="tab-btn-batches"
            onClick={() => setSubTab('batches')}
            className={`px-3.5 py-1.5 rounded-lg font-semibold transition flex items-center gap-1.5 ${
              subTab === 'batches' ? 'bg-amber-500 text-slate-950 shadow-sm' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Factory className="h-3.5 w-3.5" />
            Production Batches ({batches.length})
          </button>
          <button
            id="tab-btn-recipes"
            onClick={() => setSubTab('recipes')}
            className={`px-3.5 py-1.5 rounded-lg font-semibold transition flex items-center gap-1.5 ${
              subTab === 'recipes' ? 'bg-amber-500 text-slate-950 shadow-sm' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Layers className="h-3.5 w-3.5" />
            Recipes & BOM ({recipes.length})
          </button>
        </div>

        {subTab === 'batches' && (
          <div className="flex items-center gap-1 overflow-x-auto py-1 text-xs">
            {(['ALL', 'PLANNED', 'MATERIALS_READY', 'IN_PRODUCTION', 'QUALITY_CHECK', 'COMPLETED', 'CANCELLED'] as const).map(
              status => (
                <button
                  key={status}
                  onClick={() => setStatusFilter(status)}
                  className={`px-2.5 py-1 rounded-lg font-medium text-[11px] transition ${
                    statusFilter === status
                      ? 'bg-slate-700 text-white font-bold border border-slate-600'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {status}
                </button>
              )
            )}
          </div>
        )}
      </div>

      {/* Subtab Content: Batches */}
      {subTab === 'batches' && (
        <div className="space-y-3">
          {filteredBatches.length === 0 ? (
            <div className="glass-card rounded-2xl p-12 text-center border border-slate-800 bg-slate-900/20 text-slate-400 space-y-3">
              <Factory className="h-10 w-10 mx-auto text-slate-600" />
              <p className="font-semibold text-sm">No production batches found.</p>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Create a recipe or launch a production batch to begin tracking raw material consumption and finished goods output.
              </p>
              <button
                onClick={() => setActiveModal('newBatch')}
                className="px-4 py-2 bg-amber-500 text-slate-950 font-bold text-xs rounded-xl hover:brightness-110"
              >
                Launch First Batch
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3">
              {filteredBatches.map(batch => {
                const style = statusColors[batch.status] || statusColors.PLANNED;
                return (
                  <div
                    key={batch.id}
                    className="glass-card rounded-2xl p-5 border border-slate-800 bg-slate-900/40 hover:border-slate-700 transition space-y-4"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2.5">
                          <span className="font-mono font-bold text-base text-white">{batch.batchNumber}</span>
                          <span
                            className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border ${style.bg} ${style.text} ${style.border}`}
                          >
                            {batch.status}
                          </span>
                          {batch.yieldPercentage != null && (
                            <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-purple-500/10 text-purple-300 border border-purple-500/20">
                              Yield: {batch.yieldPercentage}%
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-slate-400 flex flex-wrap items-center gap-x-4 gap-y-1">
                          <span>
                            Target: <strong className="text-slate-200">{batch.targetProductName}</strong> ({batch.targetProductSku})
                          </span>
                          {batch.recipeCode && (
                            <span>
                              Recipe: <strong className="text-amber-400">{batch.recipeCode}</strong>
                            </span>
                          )}
                          {batch.batchLot && (
                            <span>
                              Lot: <strong className="text-slate-300 font-mono">{batch.batchLot}</strong>
                            </span>
                          )}
                          {batch.supervisor && <span>Supervisor: {batch.supervisor}</span>}
                        </div>
                      </div>

                      {/* Quantities Overview */}
                      <div className="flex items-center gap-4 bg-slate-950/60 px-3.5 py-2 rounded-xl border border-slate-800 text-xs">
                        <div>
                          <div className="text-[10px] text-slate-500">Planned</div>
                          <div className="font-bold text-slate-200">
                            {batch.plannedQuantity} {batch.unitOfMeasure}
                          </div>
                        </div>
                        <div className="h-6 w-px bg-slate-800" />
                        <div>
                          <div className="text-[10px] text-slate-500">Output</div>
                          <div className="font-bold text-emerald-400">
                            {batch.actualQuantity} {batch.unitOfMeasure}
                          </div>
                        </div>
                        <div className="h-6 w-px bg-slate-800" />
                        <div>
                          <div className="text-[10px] text-slate-500">Wastage</div>
                          <div className="font-bold text-rose-400">
                            {batch.totalWastageQuantity} {batch.unitOfMeasure}
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Action Bar */}
                    <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-800/80 text-xs">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <button
                          onClick={() => openAvailability(batch)}
                          className="px-2.5 py-1 rounded-lg border border-slate-700 bg-slate-800/60 hover:bg-slate-700 text-slate-300 font-semibold"
                        >
                          Check Materials
                        </button>

                        {(batch.status === 'PLANNED' || batch.status === 'MATERIALS_READY' || batch.status === 'IN_PRODUCTION') && (
                          <button
                            onClick={() => openConsume(batch)}
                            className="px-2.5 py-1 rounded-lg border border-amber-500/30 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 font-semibold"
                          >
                            + Consume Raw Material
                          </button>
                        )}

                        {(batch.status === 'IN_PRODUCTION' || batch.status === 'QUALITY_CHECK') && (
                          <button
                            onClick={() => openOutput(batch)}
                            className="px-2.5 py-1 rounded-lg border border-emerald-500/30 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 font-semibold"
                          >
                            + Record Output
                          </button>
                        )}

                        {(batch.status === 'IN_PRODUCTION' || batch.status === 'MATERIALS_READY' || batch.status === 'QUALITY_CHECK') && (
                          <button
                            onClick={() => openWastage(batch)}
                            className="px-2.5 py-1 rounded-lg border border-rose-500/30 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 font-semibold"
                          >
                            + Wastage / Scrap
                          </button>
                        )}
                      </div>

                      {/* State Transitions */}
                      <div className="flex items-center gap-1.5">
                        {batch.status === 'PLANNED' && (
                          <button
                            onClick={() => updateStatus(batch.id, 'MATERIALS_READY', 'Materials verified ready for mill')}
                            className="px-3 py-1 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white font-bold text-xs"
                          >
                            Materials Ready →
                          </button>
                        )}

                        {batch.status === 'MATERIALS_READY' && (
                          <button
                            onClick={() => updateStatus(batch.id, 'IN_PRODUCTION', 'Boiling & milling started')}
                            className="px-3 py-1 rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs"
                          >
                            Start Production →
                          </button>
                        )}

                        {batch.status === 'IN_PRODUCTION' && (
                          <button
                            onClick={() => updateStatus(batch.id, 'QUALITY_CHECK', 'Batch sent for quality inspection')}
                            className="px-3 py-1 rounded-lg bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs"
                          >
                            Send to QA →
                          </button>
                        )}

                        {batch.status === 'QUALITY_CHECK' && (
                          <button
                            onClick={() => updateStatus(batch.id, 'COMPLETED', 'Quality check passed & approved')}
                            className="px-3 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs"
                          >
                            Complete Batch ✓
                          </button>
                        )}

                        {batch.status !== 'COMPLETED' && batch.status !== 'CANCELLED' && (
                          <button
                            onClick={() => updateStatus(batch.id, 'CANCELLED', 'Cancelled by supervisor')}
                            className="px-2 py-1 rounded-lg border border-slate-700 text-slate-400 hover:text-rose-400 hover:border-rose-500/30 text-xs"
                          >
                            Cancel
                          </button>
                        )}

                        <button
                          onClick={() => openDetails(batch)}
                          className="p-1.5 rounded-lg border border-slate-700 bg-slate-800 hover:bg-slate-700 text-slate-300"
                          title="View Batch Dossier"
                        >
                          <Eye className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Subtab Content: Recipes */}
      {subTab === 'recipes' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-sm font-bold text-slate-300">Standard Recipes & Bill of Materials (BOM)</h3>
            <button
              onClick={() => setActiveModal('newRecipe')}
              className="px-3 py-1.5 rounded-xl bg-amber-500 text-slate-950 font-bold text-xs hover:brightness-110 flex items-center gap-1"
            >
              <Plus className="h-3.5 w-3.5" />
              Add Recipe
            </button>
          </div>

          {recipes.length === 0 ? (
            <div className="glass-card rounded-2xl p-12 text-center border border-slate-800 bg-slate-900/20 text-slate-400 space-y-3">
              <Layers className="h-10 w-10 mx-auto text-slate-600" />
              <p className="font-semibold text-sm">No recipes defined.</p>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Define standard recipes with raw material ratios to auto-calculate required sugarcane juice, clarifies, and packaging.
              </p>
              <button
                onClick={() => setActiveModal('newRecipe')}
                className="px-4 py-2 bg-amber-500 text-slate-950 font-bold text-xs rounded-xl hover:brightness-110"
              >
                Create Recipe
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {recipes.map(recipe => (
                <div
                  key={recipe.id}
                  className="glass-card rounded-2xl p-5 border border-slate-800 bg-slate-900/40 hover:border-slate-700 transition space-y-4"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-amber-400">{recipe.recipeCode}</span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          Active
                        </span>
                      </div>
                      <h4 className="font-bold text-base text-white mt-1">{recipe.recipeName}</h4>
                      <p className="text-xs text-slate-400">
                        Outputs: <strong className="text-slate-200">{recipe.outputProductName}</strong> ({recipe.outputProductSku})
                      </p>
                    </div>
                    <button
                      onClick={() => {
                        setRecipeForBatch(recipe);
                        setActiveModal('newBatch');
                      }}
                      className="px-3 py-1.5 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/30 hover:bg-amber-500 hover:text-slate-950 font-bold text-xs transition shrink-0 flex items-center gap-1"
                    >
                      <Flame className="h-3 w-3" />
                      Make Batch
                    </button>
                  </div>

                  <div className="grid grid-cols-3 gap-2 bg-slate-950/60 p-2.5 rounded-xl border border-slate-800 text-xs">
                    <div>
                      <span className="text-[10px] text-slate-500 block">Std Batch</span>
                      <strong className="text-slate-200 font-bold">
                        {recipe.standardBatchSize} {recipe.unitOfMeasure}
                      </strong>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500 block">Std Yield</span>
                      <strong className="text-purple-400 font-bold">{recipe.standardYieldPercentage}%</strong>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500 block">Duration</span>
                      <strong className="text-blue-400 font-bold">{recipe.estimatedDurationMinutes} mins</strong>
                    </div>
                  </div>

                  {/* BOM Ingredients */}
                  <div className="space-y-1.5">
                    <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                      Ingredients / BOM ({recipe.items?.length || 0})
                    </div>
                    <div className="space-y-1 max-h-36 overflow-y-auto pr-1">
                      {recipe.items?.map((item, idx) => (
                        <div
                          key={idx}
                          className="flex items-center justify-between text-xs py-1 px-2 rounded-lg bg-slate-800/40 border border-slate-800 text-slate-300"
                        >
                          <span className="truncate mr-2">
                            {item.rawMaterialName} <span className="text-[10px] text-slate-500">({item.rawMaterialSku})</span>
                          </span>
                          <span className="font-mono font-bold text-amber-300 shrink-0">
                            {item.requiredQuantity} {item.unitOfMeasure || 'KG'}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>

                  {recipe.description && (
                    <p className="text-[11px] text-slate-500 italic border-t border-slate-800 pt-2">{recipe.description}</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ─── MODALS ──────────────────────────────────────────────────────────── */}

      {/* Modal: Material Availability */}
      {activeModal === 'availability' && selectedBatch && availabilityData && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl max-w-2xl w-full border border-slate-800 bg-slate-900 p-6 space-y-5 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div>
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  <Scale className="h-5 w-5 text-amber-400" />
                  Raw Material Availability Check
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Batch: <strong className="text-slate-200">{selectedBatch.batchNumber}</strong> ({selectedBatch.targetProductName})
                </p>
              </div>
              <button onClick={() => setActiveModal(null)} className="p-1 rounded-lg text-slate-400 hover:text-white">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div
              className={`p-4 rounded-xl border flex items-center gap-3 ${
                availabilityData.allMaterialsAvailable
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                  : 'bg-amber-500/10 border-amber-500/30 text-amber-300'
              }`}
            >
              {availabilityData.allMaterialsAvailable ? (
                <CheckCircle2 className="h-5 w-5 text-emerald-400 shrink-0" />
              ) : (
                <AlertTriangle className="h-5 w-5 text-amber-400 shrink-0" />
              )}
              <div className="text-xs">
                <strong>
                  {availabilityData.allMaterialsAvailable
                    ? 'All required raw materials are in stock!'
                    : 'Material shortfall detected in inventory.'}
                </strong>
                <p className="mt-0.5 text-slate-400">
                  {availabilityData.allMaterialsAvailable
                    ? 'You can safely proceed to mark materials ready and start milling.'
                    : 'Procure additional raw materials or adjust the batch planned size before starting.'}
                </p>
              </div>
            </div>

            {/* Materials Table */}
            <div className="space-y-2">
              <div className="text-xs font-bold text-slate-400">Required Ingredients Breakdown:</div>
              <div className="border border-slate-800 rounded-xl overflow-hidden text-xs">
                <table className="w-full text-left">
                  <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800">
                    <tr>
                      <th className="p-2.5">Material</th>
                      <th className="p-2.5 text-right">Required</th>
                      <th className="p-2.5 text-right">In Stock</th>
                      <th className="p-2.5 text-right">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800 text-slate-300">
                    {availabilityData.materials.map((m, idx) => (
                      <tr key={idx} className="hover:bg-slate-800/30">
                        <td className="p-2.5">
                          <div className="font-semibold text-slate-200">{m.rawMaterialName}</div>
                          <div className="text-[10px] text-slate-500 font-mono">{m.rawMaterialSku}</div>
                        </td>
                        <td className="p-2.5 text-right font-mono font-bold text-amber-300">
                          {m.requiredQuantity} {m.unitOfMeasure}
                        </td>
                        <td className="p-2.5 text-right font-mono text-slate-200">
                          {m.currentStock} {m.unitOfMeasure}
                        </td>
                        <td className="p-2.5 text-right">
                          {m.sufficient ? (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              Available
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                              Short {m.shortfall} {m.unitOfMeasure}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
              <button
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800 text-slate-300 font-semibold text-xs"
              >
                Close
              </button>
              {selectedBatch.status === 'PLANNED' && (
                <button
                  onClick={() => updateStatus(selectedBatch.id, 'MATERIALS_READY', 'Materials checked and allocated')}
                  className="px-4 py-2 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white font-bold text-xs"
                >
                  Confirm Materials Ready →
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Modal: New Batch */}
      {activeModal === 'newBatch' && (
        <CreateBatchModal
          token={token}
          recipes={recipes}
          preselectedRecipe={recipeForBatch}
          userName={userName}
          onClose={() => setActiveModal(null)}
          onSuccess={() => {
            setActiveModal(null);
            setNotice('Production batch created successfully.');
            loadData();
          }}
        />
      )}

      {/* Modal: New Recipe */}
      {activeModal === 'newRecipe' && (
        <CreateRecipeModal
          token={token}
          onClose={() => setActiveModal(null)}
          onSuccess={() => {
            setActiveModal(null);
            setNotice('New BOM Recipe created successfully.');
            loadData();
          }}
        />
      )}

      {/* Modal: Record Consumption */}
      {activeModal === 'consume' && selectedBatch && (
        <RecordConsumptionModal
          token={token}
          batch={selectedBatch}
          userName={userName}
          onClose={() => setActiveModal(null)}
          onSuccess={() => {
            setActiveModal(null);
            setNotice('Raw material consumption recorded.');
            loadData();
          }}
        />
      )}

      {/* Modal: Record Output */}
      {activeModal === 'output' && selectedBatch && (
        <RecordOutputModal
          token={token}
          batch={selectedBatch}
          userName={userName}
          onClose={() => setActiveModal(null)}
          onSuccess={() => {
            setActiveModal(null);
            setNotice('Finished goods output added to stock.');
            loadData();
          }}
        />
      )}

      {/* Modal: Record Wastage */}
      {activeModal === 'wastage' && selectedBatch && (
        <RecordWastageModal
          token={token}
          batch={selectedBatch}
          userName={userName}
          onClose={() => setActiveModal(null)}
          onSuccess={() => {
            setActiveModal(null);
            setNotice('Wastage / loss entry recorded.');
            loadData();
          }}
        />
      )}

      {/* Modal: Batch Dossier / Details */}
      {activeModal === 'details' && selectedBatch && (
        <BatchDetailsModal
          batch={selectedBatch}
          onClose={() => setActiveModal(null)}
        />
      )}
    </div>
  );
}

// ─── Sub-Components: Modals ───────────────────────────────────────────────────

function CreateBatchModal({
  token,
  recipes,
  preselectedRecipe,
  userName,
  onClose,
  onSuccess
}: {
  token: string;
  recipes: RecipeDto[];
  preselectedRecipe: RecipeDto | null;
  userName: string;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [selectedRecipeCode, setSelectedRecipeCode] = useState(preselectedRecipe?.recipeCode || (recipes[0]?.recipeCode ?? ''));
  const [targetProductSku, setTargetProductSku] = useState(preselectedRecipe?.outputProductSku || 'JAG-ORG-500G');
  const [targetProductName, setTargetProductName] = useState(preselectedRecipe?.outputProductName || 'Organic Jaggery 500g');
  const [plannedQuantity, setPlannedQuantity] = useState<number>(preselectedRecipe?.standardBatchSize || 100);
  const [unitOfMeasure, setUnitOfMeasure] = useState('KG');
  const [supervisor, setSupervisor] = useState(userName || 'PRODUCTION_MANAGER');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleRecipeChange = (code: string) => {
    setSelectedRecipeCode(code);
    const r = recipes.find(rec => rec.recipeCode === code);
    if (r) {
      setTargetProductSku(r.outputProductSku);
      setTargetProductName(r.outputProductName);
      setPlannedQuantity(r.standardBatchSize);
      setUnitOfMeasure(r.unitOfMeasure);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await apiRequest(`${PRODUCTION_BASE}/batches`, token, {
        method: 'POST',
        body: JSON.stringify({
          recipeCode: selectedRecipeCode || undefined,
          targetProductSku,
          targetProductName,
          plannedQuantity,
          unitOfMeasure,
          supervisor,
          notes
        })
      });
      onSuccess();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-card rounded-3xl max-w-lg w-full border border-slate-800 bg-slate-900 p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Flame className="h-5 w-5 text-amber-500" />
            Launch Production Batch
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && (
          <div className="p-3 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-300 text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3 text-xs">
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Select BOM Recipe (Optional)</label>
            <select
              value={selectedRecipeCode}
              onChange={e => handleRecipeChange(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
            >
              <option value="">-- Custom Batch (No Preset Recipe) --</option>
              {recipes.map(r => (
                <option key={r.id} value={r.recipeCode}>
                  {r.recipeCode} - {r.recipeName} ({r.outputProductName})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Target Product SKU *</label>
              <input
                type="text"
                required
                value={targetProductSku}
                onChange={e => setTargetProductSku(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Target Product Name</label>
              <input
                type="text"
                value={targetProductName}
                onChange={e => setTargetProductName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Planned Quantity *</label>
              <input
                type="number"
                step="0.1"
                min="0.1"
                required
                value={plannedQuantity}
                onChange={e => setPlannedQuantity(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Unit of Measure</label>
              <select
                value={unitOfMeasure}
                onChange={e => setUnitOfMeasure(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              >
                <option value="KG">KG (Kilograms)</option>
                <option value="BLOCK">BLOCK</option>
                <option value="BOX">BOX</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-slate-400 font-semibold mb-1">Supervisor / Miller</label>
            <input
              type="text"
              value={supervisor}
              onChange={e => setSupervisor(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
            />
          </div>

          <div>
            <label className="block text-slate-400 font-semibold mb-1">Production Notes</label>
            <textarea
              rows={2}
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder="e.g. Kolhapur sugarcane harvest run, double clarify test..."
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800 text-slate-300 font-semibold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Launch Batch'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function CreateRecipeModal({
  token,
  onClose,
  onSuccess
}: {
  token: string;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [recipeCode, setRecipeCode] = useState('');
  const [recipeName, setRecipeName] = useState('');
  const [outputProductSku, setOutputProductSku] = useState('');
  const [outputProductName, setOutputProductName] = useState('');
  const [standardBatchSize, setStandardBatchSize] = useState<number>(100);
  const [standardYieldPercentage, setStandardYieldPercentage] = useState<number>(95);
  const [estimatedDurationMinutes, setEstimatedDurationMinutes] = useState<number>(120);
  const [description, _setDescription] = useState('');
  const [items, setItems] = useState<RecipeItemDto[]>([
    { rawMaterialSku: 'RAW-SUGARCANE-JUICE', rawMaterialName: 'Fresh Sugarcane Juice', requiredQuantity: 120, unitOfMeasure: 'KG' }
  ]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const addItem = () => {
    setItems(prev => [
      ...prev,
      { rawMaterialSku: '', rawMaterialName: '', requiredQuantity: 1, unitOfMeasure: 'KG' }
    ]);
  };

  const removeItem = (idx: number) => {
    setItems(prev => prev.filter((_, i) => i !== idx));
  };

  const updateItem = (idx: number, field: keyof RecipeItemDto, value: any) => {
    setItems(prev => {
      const copy = [...prev];
      copy[idx] = { ...copy[idx], [field]: value };
      return copy;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (items.length === 0) {
      setError('At least one BOM raw material item is required.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await apiRequest(`${PRODUCTION_BASE}/recipes`, token, {
        method: 'POST',
        body: JSON.stringify({
          recipeCode,
          recipeName,
          outputProductSku,
          outputProductName,
          standardBatchSize,
          unitOfMeasure: 'KG',
          standardYieldPercentage,
          estimatedDurationMinutes,
          description,
          items
        })
      });
      onSuccess();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-card rounded-3xl max-w-2xl w-full border border-slate-800 bg-slate-900 p-6 space-y-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Layers className="h-5 w-5 text-amber-500" />
            New Bill of Materials (Recipe)
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && (
          <div className="p-3 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-300 text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Recipe Code *</label>
              <input
                type="text"
                required
                placeholder="e.g. RCP-JAG-ORG-500G"
                value={recipeCode}
                onChange={e => setRecipeCode(e.target.value.toUpperCase())}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Recipe Name *</label>
              <input
                type="text"
                required
                placeholder="e.g. Organic Jaggery 500g Blocks Formula"
                value={recipeName}
                onChange={e => setRecipeName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Output Product SKU *</label>
              <input
                type="text"
                required
                placeholder="e.g. JAG-ORG-500G"
                value={outputProductSku}
                onChange={e => setOutputProductSku(e.target.value.toUpperCase())}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Output Product Name *</label>
              <input
                type="text"
                required
                placeholder="e.g. Organic Jaggery 500g"
                value={outputProductName}
                onChange={e => setOutputProductName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Std Batch Size (KG) *</label>
              <input
                type="number"
                step="0.1"
                min="0.1"
                required
                value={standardBatchSize}
                onChange={e => setStandardBatchSize(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Std Yield %</label>
              <input
                type="number"
                step="0.1"
                value={standardYieldPercentage}
                onChange={e => setStandardYieldPercentage(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Est. Duration (Mins)</label>
              <input
                type="number"
                value={estimatedDurationMinutes}
                onChange={e => setEstimatedDurationMinutes(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white focus:outline-none focus:border-amber-500"
              />
            </div>
          </div>

          {/* BOM Items */}
          <div className="space-y-2 pt-2 border-t border-slate-800">
            <div className="flex items-center justify-between">
              <span className="font-bold text-slate-300">Bill of Materials / Ingredients</span>
              <button
                type="button"
                onClick={addItem}
                className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-amber-400 rounded-lg text-xs font-semibold flex items-center gap-1"
              >
                <Plus className="h-3 w-3" />
                Add Ingredient
              </button>
            </div>

            <div className="space-y-2">
              {items.map((item, idx) => (
                <div key={idx} className="flex items-center gap-2 bg-slate-950/60 p-2 rounded-xl border border-slate-800">
                  <input
                    type="text"
                    required
                    placeholder="Material SKU"
                    value={item.rawMaterialSku}
                    onChange={e => updateItem(idx, 'rawMaterialSku', e.target.value.toUpperCase())}
                    className="w-1/3 px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-white"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Material Name"
                    value={item.rawMaterialName}
                    onChange={e => updateItem(idx, 'rawMaterialName', e.target.value)}
                    className="w-1/3 px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-white"
                  />
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    placeholder="Qty"
                    value={item.requiredQuantity}
                    onChange={e => updateItem(idx, 'requiredQuantity', Number(e.target.value))}
                    className="w-20 px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  />
                  <span className="text-slate-500 text-[11px]">KG</span>
                  <button
                    type="button"
                    onClick={() => removeItem(idx)}
                    className="p-1.5 text-slate-500 hover:text-rose-400"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800 text-slate-300 font-semibold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold disabled:opacity-50"
            >
              {loading ? 'Saving...' : 'Save Recipe'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function RecordConsumptionModal({
  token,
  batch,
  userName,
  onClose,
  onSuccess
}: {
  token: string;
  batch: ProductionBatchDto;
  userName: string;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [rawMaterialSku, setRawMaterialSku] = useState('RAW-SUGARCANE-JUICE');
  const [rawMaterialName, setRawMaterialName] = useState('Fresh Sugarcane Juice');
  const [consumedQuantity, setConsumedQuantity] = useState<number>(100);
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await apiRequest(`${PRODUCTION_BASE}/batches/${batch.id}/consume`, token, {
        method: 'POST',
        body: JSON.stringify({
          rawMaterialSku,
          rawMaterialName,
          consumedQuantity,
          unitOfMeasure: 'KG',
          actor: userName,
          notes
        })
      });
      onSuccess();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-card rounded-3xl max-w-md w-full border border-slate-800 bg-slate-900 p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Flame className="h-5 w-5 text-amber-400" />
            Consume Raw Material
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        <p className="text-xs text-slate-400">
          Batch <strong className="text-slate-200">{batch.batchNumber}</strong> will deduct inventory stock via <code className="text-amber-400">PRODUCTION_CONSUMPTION</code>.
        </p>

        {error && (
          <div className="p-3 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-300 text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3 text-xs">
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Raw Material SKU *</label>
            <input
              type="text"
              required
              value={rawMaterialSku}
              onChange={e => setRawMaterialSku(e.target.value.toUpperCase())}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
            />
          </div>
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Material Name</label>
            <input
              type="text"
              value={rawMaterialName}
              onChange={e => setRawMaterialName(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
            />
          </div>
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Quantity to Consume (KG) *</label>
            <input
              type="number"
              step="0.1"
              min="0.1"
              required
              value={consumedQuantity}
              onChange={e => setConsumedQuantity(Number(e.target.value))}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white font-mono"
            />
          </div>
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Notes</label>
            <input
              type="text"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder="e.g. Tank 1 boiler load"
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800 text-slate-300 font-semibold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold disabled:opacity-50"
            >
              {loading ? 'Deducting...' : 'Record Consumption'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function RecordOutputModal({
  token,
  batch,
  userName,
  onClose,
  onSuccess
}: {
  token: string;
  batch: ProductionBatchDto;
  userName: string;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [outputProductSku, setOutputProductSku] = useState(batch.targetProductSku);
  const [outputProductName, _setOutputProductName] = useState(batch.targetProductName);
  const [quantityProduced, setQuantityProduced] = useState<number>(batch.plannedQuantity || 100);
  const [qualityGrade, setQualityGrade] = useState('A_GRADE');
  const [batchLot, setBatchLot] = useState(batch.batchLot || '');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await apiRequest(`${PRODUCTION_BASE}/batches/${batch.id}/output`, token, {
        method: 'POST',
        body: JSON.stringify({
          outputProductSku,
          outputProductName,
          quantityProduced,
          qualityGrade,
          batchLot,
          actor: userName,
          notes
        })
      });
      onSuccess();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-card rounded-3xl max-w-md w-full border border-slate-800 bg-slate-900 p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Scale className="h-5 w-5 text-emerald-400" />
            Record Finished Goods Output
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        <p className="text-xs text-slate-400">
          Adds finished stock into inventory with lot <code className="text-emerald-400">{batchLot}</code>.
        </p>

        {error && (
          <div className="p-3 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-300 text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3 text-xs">
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Product SKU *</label>
            <input
              type="text"
              required
              value={outputProductSku}
              onChange={e => setOutputProductSku(e.target.value.toUpperCase())}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Produced Qty (KG) *</label>
              <input
                type="number"
                step="0.1"
                min="0.1"
                required
                value={quantityProduced}
                onChange={e => setQuantityProduced(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white font-mono"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Quality Grade</label>
              <select
                value={qualityGrade}
                onChange={e => setQualityGrade(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
              >
                <option value="A_GRADE">A-Grade (Golden)</option>
                <option value="PREMIUM">Premium Export</option>
                <option value="B_GRADE">B-Grade (Commercial)</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Batch Lot Number</label>
            <input
              type="text"
              value={batchLot}
              onChange={e => setBatchLot(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white font-mono"
            />
          </div>
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Notes</label>
            <input
              type="text"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder="e.g. Molds set, moisture < 4%"
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800 text-slate-300 font-semibold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold disabled:opacity-50"
            >
              {loading ? 'Recording...' : 'Add Finished Stock'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function RecordWastageModal({
  token,
  batch,
  userName,
  onClose,
  onSuccess
}: {
  token: string;
  batch: ProductionBatchDto;
  userName: string;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [materialSku, setMaterialSku] = useState('RAW-SUGARCANE-JUICE');
  const [materialName, _setMaterialName] = useState('Sugarcane Juice');
  const [wastageQuantity, setWastageQuantity] = useState<number>(2.5);
  const [wastageType, setWastageType] = useState('LOSS');
  const [reason, setReason] = useState('Boiling evaporation / scum removal');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await apiRequest(`${PRODUCTION_BASE}/batches/${batch.id}/wastage`, token, {
        method: 'POST',
        body: JSON.stringify({
          materialSku,
          materialName,
          wastageQuantity,
          wastageType,
          reason,
          actor: userName
        })
      });
      onSuccess();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-card rounded-3xl max-w-md w-full border border-slate-800 bg-slate-900 p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Trash2 className="h-5 w-5 text-rose-400" />
            Record Wastage / Scrap / Loss
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && (
          <div className="p-3 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-300 text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3 text-xs">
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Material SKU *</label>
            <input
              type="text"
              required
              value={materialSku}
              onChange={e => setMaterialSku(e.target.value.toUpperCase())}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Wastage Qty (KG) *</label>
              <input
                type="number"
                step="0.1"
                min="0.1"
                required
                value={wastageQuantity}
                onChange={e => setWastageQuantity(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white font-mono"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-semibold mb-1">Wastage Category</label>
              <select
                value={wastageType}
                onChange={e => setWastageType(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
              >
                <option value="LOSS">LOSS (Evaporation/Scum)</option>
                <option value="SCRAP">SCRAP (Charred/Trimmings)</option>
                <option value="SPILLAGE">SPILLAGE</option>
                <option value="DEFECT">DEFECT (Off-spec)</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-slate-400 font-semibold mb-1">Reason / Cause</label>
            <input
              type="text"
              required
              value={reason}
              onChange={e => setReason(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-700 bg-slate-800 text-slate-300 font-semibold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold disabled:opacity-50"
            >
              {loading ? 'Recording...' : 'Record Wastage'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function BatchDetailsModal({
  batch,
  onClose
}: {
  batch: ProductionBatchDto;
  onClose: () => void;
}) {
  const style = statusColors[batch.status] || statusColors.PLANNED;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-card rounded-3xl max-w-2xl w-full border border-slate-800 bg-slate-900 p-6 space-y-5 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-base font-bold text-white">{batch.batchNumber}</span>
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase border ${style.bg} ${style.text} ${style.border}`}>
                {batch.status}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Target: <strong className="text-slate-200">{batch.targetProductName}</strong> ({batch.targetProductSku})
            </p>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-950/60 p-3.5 rounded-2xl border border-slate-800 text-xs">
          <div>
            <span className="text-[10px] text-slate-500 block">Planned</span>
            <strong className="text-slate-200 font-bold">{batch.plannedQuantity} {batch.unitOfMeasure}</strong>
          </div>
          <div>
            <span className="text-[10px] text-slate-500 block">Actual Output</span>
            <strong className="text-emerald-400 font-bold">{batch.actualQuantity} {batch.unitOfMeasure}</strong>
          </div>
          <div>
            <span className="text-[10px] text-slate-500 block">Wastage</span>
            <strong className="text-rose-400 font-bold">{batch.totalWastageQuantity} {batch.unitOfMeasure}</strong>
          </div>
          <div>
            <span className="text-[10px] text-slate-500 block">Yield %</span>
            <strong className="text-purple-400 font-bold">{batch.yieldPercentage != null ? `${batch.yieldPercentage}%` : 'N/A'}</strong>
          </div>
        </div>

        {/* Consumptions */}
        <div className="space-y-2">
          <h4 className="text-xs font-bold text-slate-300">Raw Material Consumptions ({batch.consumptions?.length || 0})</h4>
          {batch.consumptions?.length ? (
            <div className="border border-slate-800 rounded-xl overflow-hidden text-xs">
              <table className="w-full text-left">
                <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="p-2">Material</th>
                    <th className="p-2 text-right">Qty</th>
                    <th className="p-2 text-right">Consumed At</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800 text-slate-300">
                  {batch.consumptions.map((c, i) => (
                    <tr key={i}>
                      <td className="p-2">{c.rawMaterialName} <span className="text-slate-500 text-[10px]">({c.rawMaterialSku})</span></td>
                      <td className="p-2 text-right font-mono font-bold text-amber-400">{c.consumedQuantity} {c.unitOfMeasure}</td>
                      <td className="p-2 text-right text-slate-400">{new Date(c.consumedAt).toLocaleTimeString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-xs text-slate-500 italic">No consumption records logged yet.</p>
          )}
        </div>

        {/* Outputs */}
        <div className="space-y-2">
          <h4 className="text-xs font-bold text-slate-300">Finished Output Lots ({batch.outputs?.length || 0})</h4>
          {batch.outputs?.length ? (
            <div className="border border-slate-800 rounded-xl overflow-hidden text-xs">
              <table className="w-full text-left">
                <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="p-2">Lot</th>
                    <th className="p-2">Grade</th>
                    <th className="p-2 text-right">Qty Produced</th>
                    <th className="p-2 text-right">Time</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800 text-slate-300">
                  {batch.outputs.map((o, i) => (
                    <tr key={i}>
                      <td className="p-2 font-mono text-emerald-400 font-bold">{o.batchLot}</td>
                      <td className="p-2">{o.qualityGrade}</td>
                      <td className="p-2 text-right font-mono font-bold text-emerald-400">{o.quantityProduced} {o.unitOfMeasure}</td>
                      <td className="p-2 text-right text-slate-400">{new Date(o.producedAt).toLocaleTimeString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-xs text-slate-500 italic">No output lots recorded yet.</p>
          )}
        </div>

        {/* Wastages */}
        <div className="space-y-2">
          <h4 className="text-xs font-bold text-slate-300">Wastage & Loss Records ({batch.wastages?.length || 0})</h4>
          {batch.wastages?.length ? (
            <div className="border border-slate-800 rounded-xl overflow-hidden text-xs">
              <table className="w-full text-left">
                <thead className="bg-slate-950/60 text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="p-2">Type</th>
                    <th className="p-2">Reason</th>
                    <th className="p-2 text-right">Qty Lost</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800 text-slate-300">
                  {batch.wastages.map((w, i) => (
                    <tr key={i}>
                      <td className="p-2 text-rose-400 font-semibold">{w.wastageType}</td>
                      <td className="p-2 text-slate-300">{w.reason}</td>
                      <td className="p-2 text-right font-mono font-bold text-rose-400">{w.wastageQuantity} {w.unitOfMeasure}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-xs text-slate-500 italic">No wastage logged.</p>
          )}
        </div>

        <div className="flex justify-end pt-3 border-t border-slate-800">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 hover:text-white font-semibold text-xs"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
