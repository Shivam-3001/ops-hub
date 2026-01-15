'use client';

import { usePathname } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import PermissionGuard from '@/components/PermissionGuard';

export default function RightSidebar() {
  const pathname = usePathname();
  const { hasPermission } = useAuth();

  const isActive = (href) => {
    return pathname === href || pathname?.startsWith(href + '/');
  };

  const reportsSection = [
    {
      label: 'Reports & MIS',
      href: '/reports',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      ),
      permission: 'VIEW_REPORTS',
    },
  ];

  const usersSection = [
    {
      label: 'Users',
      href: '/users',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      ),
      permission: 'VIEW_CUSTOMERS', // Using VIEW_CUSTOMERS as placeholder, adjust based on your permissions
    },
  ];

  return (
    <aside className="fixed right-0 top-0 h-full w-64 bg-white border-l border-slate-200 flex flex-col z-20">
      {/* Header */}
      <div className="h-16 flex items-center px-6 border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wider">Quick Access</h3>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-4 py-6 space-y-6 overflow-y-auto">
        {/* Reports & MIS Section */}
        {hasPermission('VIEW_REPORTS') && (
          <div>
            <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3 px-4">
              Reports & MIS
            </h4>
            <div className="space-y-2">
              {reportsSection.map((item) => {
                const content = (
                  <Link
                    href={item.href}
                    className={`flex items-center px-4 py-3 rounded-lg transition-colors ${
                      isActive(item.href)
                        ? 'text-slate-900 bg-slate-100 font-medium'
                        : 'text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    {item.icon}
                    <span className="ml-3 text-sm">{item.label}</span>
                  </Link>
                );

                if (item.permission) {
                  return (
                    <PermissionGuard key={item.href} permission={item.permission}>
                      {content}
                    </PermissionGuard>
                  );
                }

                return <div key={item.href}>{content}</div>;
              })}
            </div>
          </div>
        )}

        {/* Users Section */}
        {hasPermission('VIEW_CUSTOMERS') && (
          <div>
            <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3 px-4">
              Users
            </h4>
            <div className="space-y-2">
              {usersSection.map((item) => {
                const content = (
                  <Link
                    href={item.href}
                    className={`flex items-center px-4 py-3 rounded-lg transition-colors ${
                      isActive(item.href)
                        ? 'text-slate-900 bg-slate-100 font-medium'
                        : 'text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    {item.icon}
                    <span className="ml-3 text-sm">{item.label}</span>
                  </Link>
                );

                if (item.permission) {
                  return (
                    <PermissionGuard key={item.href} permission={item.permission}>
                      {content}
                    </PermissionGuard>
                  );
                }

                return <div key={item.href}>{content}</div>;
              })}
            </div>
          </div>
        )}
      </nav>
    </aside>
  );
}
