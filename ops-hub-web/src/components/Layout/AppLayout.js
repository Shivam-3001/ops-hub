'use client';

import { useEffect, useState } from 'react';
import Sidebar from './Sidebar';
import RightSidebar from './RightSidebar';
import Header from './Header';
import ProtectedRoute from '@/components/ProtectedRoute';
import AiFloatingWidget from '@/components/AiFloatingWidget';

export default function AppLayout({ children, title, subtitle }) {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  useEffect(() => {
    const storedValue =
      typeof window !== 'undefined'
        ? window.localStorage.getItem('opsHubSidebarCollapsed')
        : null;

    if (storedValue === 'true') {
      setIsSidebarCollapsed(true);
    }
  }, []);

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-slate-50">
        <Sidebar
          collapsed={isSidebarCollapsed}
          onToggle={() =>
            setIsSidebarCollapsed((prev) => {
              const nextValue = !prev;
              if (typeof window !== 'undefined') {
                window.localStorage.setItem(
                  'opsHubSidebarCollapsed',
                  String(nextValue)
                );
              }
              return nextValue;
            })
          }
        />
        <div
          className={`mr-64 transition-all duration-200 ${
            isSidebarCollapsed ? 'ml-20' : 'ml-64'
          }`}
        >
          <Header title={title} subtitle={subtitle} />
          <main className="p-8">{children}</main>
        </div>
        <RightSidebar />
        <AiFloatingWidget />
      </div>
    </ProtectedRoute>
  );
}
