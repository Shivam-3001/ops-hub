"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";

export default function ComingSoonPage() {
  const router = useRouter();

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50 flex items-center justify-center p-4">
      <div className="max-w-2xl w-full text-center">
        {/* Animated Icon */}
        <div className="mb-8 flex justify-center">
          <div className="relative">
            <div className="w-32 h-32 bg-gradient-to-br from-slate-900 to-slate-700 rounded-2xl flex items-center justify-center shadow-2xl transform hover:scale-105 transition-transform">
              <svg
                className="w-16 h-16 text-white animate-pulse"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <div className="absolute -top-2 -right-2 w-8 h-8 bg-blue-500 rounded-full animate-ping"></div>
            <div className="absolute -top-2 -right-2 w-8 h-8 bg-blue-500 rounded-full"></div>
          </div>
        </div>

        {/* Title */}
        <h1 className="text-5xl md:text-6xl font-bold text-slate-900 mb-4">
          Coming Soon
        </h1>

        {/* Description */}
        <p className="text-xl text-slate-600 mb-8 max-w-lg mx-auto">
          We're working hard to bring you this feature. It will be available soon!
        </p>

        {/* Features List */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-8 text-left">
          <h2 className="text-lg font-semibold text-slate-900 mb-4">What to expect:</h2>
          <ul className="space-y-3">
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              </div>
              <span className="text-slate-700">Modern, intuitive interface</span>
            </li>
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              </div>
              <span className="text-slate-700">Enhanced functionality and features</span>
            </li>
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              </div>
              <span className="text-slate-700">Seamless integration with existing systems</span>
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <button
            onClick={() => router.back()}
            className="px-6 py-3 bg-slate-900 text-white rounded-lg font-semibold hover:bg-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2"
          >
            Go Back
          </button>
          <Link
            href="/dashboard"
            className="px-6 py-3 bg-white border-2 border-slate-900 text-slate-900 rounded-lg font-semibold hover:bg-slate-50 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2 text-center"
          >
            Return to Dashboard
          </Link>
        </div>

        {/* Footer Note */}
        <p className="mt-8 text-sm text-slate-500">
          Need immediate assistance?{" "}
          <a href="#" className="text-slate-900 font-medium hover:underline">
            Contact Support
          </a>
        </p>
      </div>
    </div>
  );
}
