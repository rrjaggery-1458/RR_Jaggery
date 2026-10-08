import React from 'react';
import { RefreshCw } from 'lucide-react';

interface LoadingOverlayProps {
  show: boolean;
  message?: string;
  subtext?: string;
  fullScreen?: boolean;
}

export const LoadingOverlay: React.FC<LoadingOverlayProps> = ({
  show,
  message = 'Saving changes...',
  subtext = 'RR Jaggery Traders ERP Engine',
  fullScreen = true,
}) => {
  if (!show) return null;

  return (
    <div
      className={`${
        fullScreen ? 'fixed inset-0 z-[9999]' : 'absolute inset-0 z-50 rounded-2xl'
      } bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in duration-200`}
    >
      <div className="bg-white/95 border border-emerald-200/80 shadow-2xl rounded-3xl p-6 sm:p-8 max-w-sm w-full mx-auto flex flex-col items-center text-center transform animate-in zoom-in-95 duration-200 backdrop-saturate-150">
        {/* Animated Brand Pulse & Spinner */}
        <div className="relative flex items-center justify-center w-20 h-20 mb-4">
          {/* Outer glowing pulse ring */}
          <div className="absolute inset-0 rounded-full bg-emerald-400/20 animate-ping" />
          
          {/* Outer spinning gradient ring */}
          <div className="absolute inset-0 rounded-full border-4 border-transparent border-t-emerald-600 border-r-amber-500 animate-spin" />
          
          {/* Inner counter-spinning ring */}
          <div className="absolute inset-2 rounded-full border-2 border-transparent border-b-emerald-700 border-l-amber-600 animate-[spin_1.5s_linear_infinite_reverse]" />
          
          {/* Center brand logo badge */}
          <div className="relative z-10 w-12 h-12 rounded-full overflow-hidden flex items-center justify-center shadow-lg shadow-emerald-900/30 border border-emerald-400/40">
            <img src="/rr-logo.png" className="w-full h-full object-cover animate-pulse" alt="RR Jaggery Logo" />
          </div>
        </div>

        {/* Text Details */}
        <h4 className="text-base font-bold text-gray-900 font-serif tracking-tight mb-1">
          {message}
        </h4>
        <p className="text-xs text-emerald-800 font-medium tracking-wide">
          {subtext}
        </p>

        {/* Dynamic Progress Indicator */}
        <div className="w-full bg-emerald-100 rounded-full h-1.5 mt-5 overflow-hidden">
          <div className="bg-gradient-to-r from-emerald-600 via-amber-500 to-emerald-600 h-full rounded-full animate-indeterminate-bar" />
        </div>
      </div>
    </div>
  );
};

export const TopProgressBar: React.FC<{ active: boolean }> = ({ active }) => {
  if (!active) return null;
  return (
    <div className="fixed top-0 left-0 right-0 z-[10000] h-1 bg-emerald-100/50 overflow-hidden">
      <div className="h-full bg-gradient-to-r from-emerald-600 via-amber-400 to-emerald-700 animate-indeterminate-bar shadow-sm" />
    </div>
  );
};

export const ButtonSpinner: React.FC<{ className?: string }> = ({ className = 'w-4 h-4' }) => (
  <RefreshCw className={`${className} animate-spin text-white`} />
);
