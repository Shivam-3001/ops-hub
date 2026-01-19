'use client';

import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import api from '@/lib/api';

export default function Header({ title, subtitle }) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
  };

  const loadNotifications = async () => {
    try {
      const data = await api.getNotifications(8);
      setNotifications(data.notifications || []);
      setUnreadCount(data.unreadCount || 0);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const handleToggleNotifications = async () => {
    const nextOpen = !open;
    setOpen(nextOpen);
    if (nextOpen) {
      await loadNotifications();
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await api.markAllNotificationsRead();
      await loadNotifications();
    } catch (error) {
      console.error('Failed to mark notifications as read:', error);
    }
  };

  const handleMarkRead = async (notificationId) => {
    try {
      await api.markNotificationRead(notificationId);
      await loadNotifications();
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  };

  return (
    <header className="bg-white border-b border-slate-200 h-16 flex items-center justify-between px-8 sticky top-0 z-20 ml-64">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title || 'Dashboard'}</h1>
        {subtitle && <p className="text-sm text-slate-500">{subtitle}</p>}
      </div>
      <div className="flex items-center gap-4">
        <div className="relative">
          <button
            className="p-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors relative"
            title="Notifications"
            onClick={handleToggleNotifications}
            type="button"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            {unreadCount > 0 && (
              <span className="absolute -top-1 -right-1 text-[10px] bg-red-600 text-white rounded-full px-1.5 py-0.5">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>
          {open && (
            <div className="absolute right-0 mt-2 w-80 bg-white border border-slate-200 rounded-xl shadow-lg z-30">
              <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200">
                <div className="text-sm font-semibold text-slate-900">Notifications</div>
                <button
                  type="button"
                  onClick={handleMarkAllRead}
                  className="text-xs text-slate-500 hover:text-slate-700"
                >
                  Mark all read
                </button>
              </div>
              <div className="max-h-80 overflow-y-auto">
                {notifications.length === 0 && (
                  <div className="px-4 py-6 text-sm text-slate-500">No notifications yet.</div>
                )}
                {notifications.map((notification) => (
                  <button
                    type="button"
                    key={notification.id}
                    onClick={() => handleMarkRead(notification.id)}
                    className={`w-full text-left px-4 py-3 border-b border-slate-100 hover:bg-slate-50 ${
                      notification.readAt ? 'bg-white' : 'bg-slate-50'
                    }`}
                  >
                    <div className="text-sm font-medium text-slate-900">{notification.title}</div>
                    <div className="text-xs text-slate-500 mt-1">{notification.message}</div>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
        <button
          onClick={handleLogout}
          className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
        >
          Logout
        </button>
      </div>
    </header>
  );
}
