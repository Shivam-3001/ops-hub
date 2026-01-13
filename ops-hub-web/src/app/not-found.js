import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50 flex items-center justify-center p-4">
      <div className="max-w-2xl w-full text-center">
        {/* 404 Number */}
        <div className="mb-8">
          <h1 className="text-9xl md:text-[12rem] font-bold text-transparent bg-clip-text bg-gradient-to-r from-slate-900 via-slate-700 to-slate-900 leading-none">
            404
          </h1>
        </div>

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
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
          </div>
        </div>

        {/* Title */}
        <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4">
          Page Not Found
        </h2>

        {/* Description */}
        <p className="text-xl text-slate-600 mb-8 max-w-lg mx-auto">
          Sorry, we couldn't find the page you're looking for. The page might have been moved, deleted, or doesn't exist.
        </p>

        {/* Suggestions */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-8 text-left max-w-md mx-auto">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">You might want to:</h3>
          <ul className="space-y-3">
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <span className="text-slate-600 text-xs font-bold">1</span>
              </div>
              <span className="text-slate-700">Check the URL for typos</span>
            </li>
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <span className="text-slate-600 text-xs font-bold">2</span>
              </div>
              <span className="text-slate-700">Go back to the previous page</span>
            </li>
            <li className="flex items-start gap-3">
              <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                <span className="text-slate-600 text-xs font-bold">3</span>
              </div>
              <span className="text-slate-700">Return to the dashboard</span>
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link
            href="/dashboard"
            className="px-6 py-3 bg-slate-900 text-white rounded-lg font-semibold hover:bg-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2"
          >
            Go to Dashboard
          </Link>
          <Link
            href="/login"
            className="px-6 py-3 bg-white border-2 border-slate-900 text-slate-900 rounded-lg font-semibold hover:bg-slate-50 transition-colors focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2"
          >
            Back to Login
          </Link>
        </div>

        {/* Footer Note */}
        <p className="mt-8 text-sm text-slate-500">
          If you believe this is an error, please{" "}
          <a href="#" className="text-slate-900 font-medium hover:underline">
            contact support
          </a>
        </p>
      </div>
    </div>
  );
}
