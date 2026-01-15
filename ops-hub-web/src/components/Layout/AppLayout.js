'use client';

import Sidebar from './Sidebar';
import RightSidebar from './RightSidebar';
import Header from './Header';
import ProtectedRoute from '@/components/ProtectedRoute';

export default function AppLayout({ children, title, subtitle }) {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-slate-50">
        <Sidebar />
        <div className="ml-64 mr-64">
          <Header title={title} subtitle={subtitle} />
          <main className="p-8">{children}</main>
        </div>
        <RightSidebar />
      </div>
    </ProtectedRoute>
  );
}
