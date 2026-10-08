import React, { useEffect, useState } from 'react';
import { Sliders, Info, FileText } from 'lucide-react';

export interface FormOption {
  id?: string;
  value: string;
  label: string;
  displayOrder?: number;
  active: boolean;
}

export interface FormField {
  id?: string;
  fieldKey: string;
  label: string;
  fieldType: string;
  required: boolean;
  active: boolean;
  displayOrder?: number;
  placeholder?: string;
  helpText?: string;
  validationJson?: string;
  options?: FormOption[];
}

export interface FormDefinition {
  id?: string;
  formKey: string;
  displayName: string;
  description?: string;
  active: boolean;
  version: number;
  fields: FormField[];
}

interface DynamicFormSectionProps {
  formKey: string;
  service: 'finance' | 'customers' | 'production';
  answers: Record<string, any>;
  onAnswerChange: (fieldKey: string, value: any) => void;
  token?: string;
  readOnly?: boolean;
  excludeKeys?: string[];
}

const getBaseUrl = (service: 'finance' | 'customers' | 'production') => {
  if (import.meta.env.PROD || (typeof window !== 'undefined' && (window.location.port === '' || window.location.port === '80' || window.location.port === '443'))) {
    if (service === 'customers') return '/api/v1/customers/forms';
    if (service === 'production') return '/api/v1/production/forms';
    return '/api/v1/finance/forms';
  }
  if (service === 'customers') return 'http://localhost:8083/api/v1/customers/forms';
  if (service === 'production') return 'http://localhost:8086/api/v1/production/forms';
  return 'http://localhost:8083/api/v1/finance/forms';
};

export const DynamicFormSection: React.FC<DynamicFormSectionProps> = ({
  formKey,
  service,
  answers,
  onAnswerChange,
  token,
  readOnly = false,
  excludeKeys = [],
}) => {
  const [definition, setDefinition] = useState<FormDefinition | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    const loadDefinition = async () => {
      try {
        setLoading(true);
        setError(null);
        const baseUrl = getBaseUrl(service);
        const authToken = token || (typeof window !== 'undefined' ? (localStorage.getItem('rr_jwt_token') || localStorage.getItem('token')) : null);
        const res = await fetch(`${baseUrl}/definitions/${formKey}`, {
          headers: authToken ? { Authorization: `Bearer ${authToken}` } : {},
        });
        if (res.ok) {
          const data = await res.json();
          if (isMounted) setDefinition(data.data);
        } else {
          if (isMounted) setDefinition(null);
        }
      } catch (err: any) {
        if (isMounted) {
          console.warn(`[DynamicForms] Could not load form definition for ${formKey}:`, err);
          setError('Dynamic questions unavailable');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadDefinition();
    return () => {
      isMounted = false;
    };
  }, [formKey, service, token]);

  if (loading) {
    return (
      <div className="p-3 bg-emerald-50/50 border border-emerald-100 rounded-xl text-gray-500 text-xs flex items-center gap-2">
        <span className="w-3.5 h-3.5 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin" />
        Loading configured business fields...
      </div>
    );
  }

  if (error || !definition || !definition.fields || definition.fields.length === 0) {
    return null; // No extra fields configured
  }

  // Deduplicate fields by fieldKey, apply excludeKeys, and filter active
  const seenKeys = new Set<string>();
  const fieldsToRender = (definition.fields || [])
    .filter(f => {
      if (!f || !f.fieldKey) return false;
      if (excludeKeys && excludeKeys.includes(f.fieldKey)) return false;
      if (readOnly ? false : f.active === false) return false;
      if (seenKeys.has(f.fieldKey)) return false;
      seenKeys.add(f.fieldKey);
      return true;
    })
    .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));

  if (fieldsToRender.length === 0) {
    return null;
  }

  return (
    <div className="bg-emerald-50/40 border border-emerald-200/80 rounded-2xl p-4 space-y-3.5 mt-3">
      <div className="flex items-center justify-between border-b border-emerald-200/60 pb-2">
        <div className="flex items-center gap-2">
          <Sliders className="w-4 h-4 text-emerald-700" />
          <h4 className="text-xs font-bold text-emerald-950 uppercase tracking-wider">
            Additional Business Attributes
          </h4>
        </div>
        <span className="text-[10px] font-semibold px-2 py-0.5 bg-emerald-100/70 text-emerald-800 rounded-full border border-emerald-200">
          Form v{definition.version || 1}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5 text-xs">
        {fieldsToRender.map(field => {
          const value = answers[field.fieldKey] ?? '';
          const activeOptions = (field.options || []).filter(o => o.active !== false);

          return (
            <div
              key={field.fieldKey}
              className={['TEXTAREA', 'FILE', 'IMAGE'].includes(field.fieldType) ? 'col-span-1 md:col-span-2' : 'col-span-1'}
            >
              <label className="block text-gray-700 font-semibold mb-1">
                {field.label} {field.required && <span className="text-rose-500">*</span>}
              </label>

              {/* TEXT, EMAIL, PHONE */}
              {['TEXT', 'EMAIL', 'PHONE'].includes(field.fieldType) && (
                <input
                  type={field.fieldType === 'EMAIL' ? 'email' : field.fieldType === 'PHONE' ? 'tel' : 'text'}
                  disabled={readOnly}
                  required={field.required}
                  value={value}
                  placeholder={field.placeholder || ''}
                  onChange={e => onAnswerChange(field.fieldKey, e.target.value)}
                  className="w-full bg-white border border-slate-300 rounded-xl px-3 py-2 text-gray-900 placeholder:text-gray-500 focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none text-xs"
                />
              )}

              {/* TEXTAREA */}
              {field.fieldType === 'TEXTAREA' && (
                <textarea
                  rows={2}
                  disabled={readOnly}
                  required={field.required}
                  value={value}
                  placeholder={field.placeholder || ''}
                  onChange={e => onAnswerChange(field.fieldKey, e.target.value)}
                  className="w-full bg-white border border-slate-300 rounded-xl px-3 py-2 text-gray-900 placeholder:text-gray-500 focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none text-xs resize-none"
                />
              )}

              {/* NUMBER, DECIMAL, PERCENTAGE */}
              {['NUMBER', 'DECIMAL', 'PERCENTAGE'].includes(field.fieldType) && (
                <div className="relative">
                  <input
                    type="number"
                    step={field.fieldType === 'NUMBER' ? '1' : '0.01'}
                    disabled={readOnly}
                    required={field.required}
                    value={value}
                    placeholder={field.placeholder || '0'}
                    onChange={e => onAnswerChange(field.fieldKey, e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full bg-white border border-slate-300 rounded-xl px-3 py-2 text-gray-900 placeholder:text-gray-500 focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none text-xs font-semibold"
                  />
                  {field.fieldType === 'PERCENTAGE' && (
                    <span className="absolute right-3 top-2 text-gray-500 font-bold">%</span>
                  )}
                </div>
              )}

              {/* CURRENCY */}
              {field.fieldType === 'CURRENCY' && (
                <div className="relative">
                  <span className="absolute left-3 top-2 text-gray-500 font-bold">₹</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    disabled={readOnly}
                    required={field.required}
                    value={value}
                    placeholder={field.placeholder || '0.00'}
                    onChange={e => onAnswerChange(field.fieldKey, e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full pl-7 bg-white border border-slate-300 rounded-xl px-3 py-2 text-gray-900 placeholder:text-gray-500 focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none text-xs font-bold text-emerald-700"
                  />
                </div>
              )}

              {/* DATE */}
              {field.fieldType === 'DATE' && (
                <input
                  type="date"
                  disabled={readOnly}
                  required={field.required}
                  value={value}
                  onChange={e => onAnswerChange(field.fieldKey, e.target.value)}
                  className="w-full bg-white border border-slate-300 rounded-xl px-3 py-2 text-gray-900 focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none text-xs"
                />
              )}

              {/* DATETIME */}
              {field.fieldType === 'DATETIME' && (
                <input
                  type="datetime-local"
                  disabled={readOnly}
                  required={field.required}
                  value={value}
                  onChange={e => onAnswerChange(field.fieldKey, e.target.value)}
                  className="w-full bg-white border border-slate-300 rounded-xl px-3 py-2 text-gray-900 focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none text-xs"
                />
              )}

              {/* BOOLEAN */}
              {field.fieldType === 'BOOLEAN' && (
                <label className="flex items-center gap-2 pt-1 cursor-pointer">
                  <input
                    type="checkbox"
                    disabled={readOnly}
                    checked={Boolean(value)}
                    onChange={e => onAnswerChange(field.fieldKey, e.target.checked)}
                    className="rounded border-slate-300 text-emerald-600 focus:ring-emerald-500 w-4 h-4"
                  />
                  <span className="text-gray-700 font-medium">Yes / Verified</span>
                </label>
              )}

              {/* SINGLE_SELECT, DROPDOWN */}
              {['SINGLE_SELECT', 'DROPDOWN'].includes(field.fieldType) && (
                <select
                  disabled={readOnly || activeOptions.length === 0}
                  required={field.required && activeOptions.length > 0}
                  value={value}
                  onChange={e => onAnswerChange(field.fieldKey, e.target.value)}
                  className="w-full bg-white border border-slate-300 rounded-xl px-3 py-2 text-gray-900 focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 outline-none text-xs font-medium"
                >
                  <option value="">{activeOptions.length === 0 ? `-- No configured options --` : `-- Select ${field.label} --`}</option>
                  {activeOptions.map(opt => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              )}

              {/* MULTI_SELECT */}
              {field.fieldType === 'MULTI_SELECT' && (
                <div className="space-y-1.5 p-2 bg-white border border-slate-300 rounded-xl max-h-32 overflow-y-auto">
                  {activeOptions.map(opt => {
                    const selectedList = Array.isArray(value) ? value : [];
                    const isChecked = selectedList.includes(opt.value);
                    return (
                      <label key={opt.value} className="flex items-center gap-2 text-gray-700 text-xs cursor-pointer">
                        <input
                          type="checkbox"
                          disabled={readOnly}
                          checked={isChecked}
                          onChange={e => {
                            const updated = e.target.checked
                              ? [...selectedList, opt.value]
                              : selectedList.filter((v: string) => v !== opt.value);
                            onAnswerChange(field.fieldKey, updated);
                          }}
                          className="rounded border-slate-300 text-emerald-600 focus:ring-emerald-500 w-3.5 h-3.5"
                        />
                        <span>{opt.label}</span>
                      </label>
                    );
                  })}
                </div>
              )}

              {/* FILE, IMAGE */}
              {['FILE', 'IMAGE'].includes(field.fieldType) && (
                <div className="bg-white border border-dashed border-emerald-300 rounded-xl p-3 text-center">
                  <FileText className="w-5 h-5 mx-auto text-emerald-600 mb-1" />
                  <p className="text-[11px] font-semibold text-gray-700">Attach Document / Receipt Reference</p>
                  <input
                    type="text"
                    disabled={readOnly}
                    value={value}
                    placeholder="Document reference or file key..."
                    onChange={e => onAnswerChange(field.fieldKey, e.target.value)}
                    className="w-full mt-2 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1 text-xs text-gray-900 focus:border-emerald-500"
                  />
                  <p className="text-[10px] text-gray-500 mt-1">
                    Secure opaque storage identifiers are preserved across deployments.
                  </p>
                </div>
              )}

              {field.helpText && (
                <p className="text-[10px] text-gray-500 mt-1 italic flex items-center gap-1">
                  <Info className="w-3 h-3 text-gray-500 shrink-0" />
                  {field.helpText}
                </p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
