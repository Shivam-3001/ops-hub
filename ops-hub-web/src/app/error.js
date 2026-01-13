"use client";

import { useEffect } from "react";

export default function Error({ error, reset }) {
  useEffect(() => {
    // Log the error to an error reporting service
    console.error("Application error:", error);
  }, [error]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50 flex items-center justify-center p-4">
      <div className="max-w-2xl w-full text-center">
        {/* Error Icon */}
        <div className="mb-8 flex justify-center">
          <div className="w-24 h-24 bg-red-100 rounded-full flex items-center justify-center">
            <svg
              className="w-12 h-12 text-red-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
        </div>

        {/* Title */}
        <h1 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4">
          Something Went Wrong
        </h1>

        {/* Description */}
        <p className="text-xl text-slate-600 mb-8 max-w-lg mx-auto">
          We encountered an unexpected error. Don't worry, our team has been notified and is working on a fix.
        </p>

        {/* Error Details (Development Only) */}
        {process.env.NODE_ENV === "development" && error?.message && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-8 text-left max-w-2xl mx-auto">
            <h3 className="text-sm font-semibold text-red-900 mb-2">Error Details:</h3>
            <p className="text-sm text-red-800 font-mono break-all">{error.message}</p>
            {error.stack && (
              <details className="mt-3">
                <summary className="text-sm text-red-700 cursor-pointer hover:text-red-900">
                  Stack Trace
                </summary>
                <pre className="mt-2 text-xs text-red-700 overflow-auto max-h-48 whitespace-pre-wrap font-mono">
                  {error.stack}
                </pre>
              </details>
            )}
          </div>
        )}

        {/* Help Section */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-8 text-left max-w-md mx-auto">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">What you can do:</h3>
          <ul className="space-y-3">
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-11a1 1 0 10-2 0v2a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V7z" clipRule="evenodd" />
                </svg>
              </div>
              <span className="text-slate-700">Try refreshing the page</span>
            </li>
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd" />
                </svg>
              </div>
              <span className="text-slate-700">Clear your browser cache</span>
            </li>
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z" />
                  <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z" />
                </svg>
              </div>
              <span className="text-slate-700">Contact support if the problem persists</span>
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <button
            onClick={reset}
            className="px-6 py-3 bg-slate-900 text-white rounded-lg font-semibold hover:bg-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2"
          >
            Try Again
          </button>
          <a
            href="/dashboard"
            className="px-6 py-3 bg-white border-2 border-slate-900 text-slate-900 rounded-lg font-semibold hover:bg-slate-50 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2 text-center"
          >
            Go to Dashboard
          </a>
        </div>

        {/* Footer Note */}
        <p className="mt-8 text-sm text-slate-500">
          Error Code: <span className="font-mono text-slate-700">ERR_UNEXPECTED</span>
        </p>
      </div>
    </div>
  );
}
