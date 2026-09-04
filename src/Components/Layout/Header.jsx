import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Menu, Bell, LogOut,
  User, Settings, ChevronDown, Home
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { notificationsApi } from '../../services/api';

export default function Header({ onMenuClick, title }) {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [showDropdown, setShowDropdown] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const loadNotifications = async () => {
    if (!user) return;
    try {
      const [items, unread] = await Promise.all([
        notificationsApi.getMine(),
        notificationsApi.getUnreadCount(),
      ]);
      setNotifications(items || []);
      setUnreadCount(unread?.count || 0);
    } catch (err) {
      console.error('Failed to load notifications:', err);
    }
  };

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    Promise.all([
      notificationsApi.getMine(),
      notificationsApi.getUnreadCount(),
    ])
      .then(([items, unread]) => {
        if (cancelled) return;
        setNotifications(items || []);
        setUnreadCount(unread?.count || 0);
      })
      .catch((err) => {
        console.error('Failed to load notifications:', err);
      });
    return () => {
      cancelled = true;
    };
  }, [user]);

  const handleLogout = () => {
    sessionStorage.setItem('rms_auth_message', 'logout');
    logout();
    navigate('/login');
  };

  const handleNotificationClick = async (notification) => {
    try {
      if (!notification.readFlag) {
        await notificationsApi.markRead(notification.id);
        await loadNotifications();
      }
      setShowNotifications(false);
      if (notification.link) {
        navigate(notification.link);
      }
    } catch (err) {
      console.error('Failed to update notification:', err);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationsApi.markAllRead();
      await loadNotifications();
    } catch (err) {
      console.error('Failed to mark notifications as read:', err);
    }
  };


  const getInitials = () => {
    if (!user?.name) return '?';
    return user.name
      .split(' ')
      .map(word => word[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-md border-b border-slate-200/80 shadow-sm">
      <div className="flex items-center justify-between px-4 sm:px-6 lg:px-8 h-16">
        {}
        <div className="flex items-center gap-3">
          <button
            onClick={onMenuClick}
            className="lg:hidden p-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
            aria-label="Toggle menu"
          >
            <Menu className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-2">
            <div className="hidden sm:flex items-center gap-2.5">
              <div className="w-8 h-8 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center shadow-lg shadow-indigo-500/25">
                <span className="text-white font-bold text-sm">R</span>
              </div>
            </div>
            <h1 className="text-lg sm:text-xl font-bold text-slate-800 truncate">
              {title}
            </h1>
          </div>
        </div>

        {}
        <div className="flex items-center gap-1 sm:gap-2">
          {}
          <div className="relative">
            <button
              onClick={() => {
                setShowNotifications(!showNotifications);
                setShowDropdown(false);
                if (!showNotifications) loadNotifications();
              }}
              className="relative p-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
              aria-label="Notifications"
            >
              <Bell className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 min-w-5 h-5 px-1 bg-rose-500 text-white text-[10px] font-bold rounded-full ring-2 ring-white flex items-center justify-center">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </button>

            {showNotifications && (
              <>
                <div
                  className="fixed inset-0 z-40"
                  onClick={() => setShowNotifications(false)}
                />
                <div className="absolute right-0 mt-2 w-80 max-w-[calc(100vw-2rem)] bg-white rounded-xl shadow-xl border border-slate-100 z-50 overflow-hidden animate-fade-in">
                  <div className="px-4 py-3 border-b border-slate-100 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800">Notifications</p>
                      <p className="text-xs text-slate-500">{unreadCount} unread</p>
                    </div>
                    {unreadCount > 0 && (
                      <button
                        onClick={handleMarkAllRead}
                        className="text-xs font-medium text-indigo-600 hover:text-indigo-700"
                      >
                        Mark all read
                      </button>
                    )}
                  </div>

                  <div className="max-h-96 overflow-y-auto">
                    {notifications.length === 0 ? (
                      <div className="px-4 py-8 text-center text-sm text-slate-500">
                        No notifications yet.
                      </div>
                    ) : (
                      notifications.slice(0, 10).map((notification) => (
                        <button
                          key={notification.id}
                          onClick={() => handleNotificationClick(notification)}
                          className={`w-full px-4 py-3 text-left border-b border-slate-100 last:border-0 hover:bg-slate-50 transition ${
                            notification.readFlag ? 'bg-white' : 'bg-indigo-50/50'
                          }`}
                        >
                          <div className="flex items-start gap-3">
                            <span className={`mt-1.5 w-2 h-2 rounded-full shrink-0 ${
                              notification.readFlag ? 'bg-slate-200' : 'bg-rose-500'
                            }`} />
                            <div className="min-w-0">
                              <p className="text-sm font-medium text-slate-800 truncate">
                                {notification.title}
                              </p>
                              <p className="text-xs text-slate-600 mt-0.5 line-clamp-2">
                                {notification.message}
                              </p>
                              <p className="text-[11px] text-slate-400 mt-1">
                                {notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ''}
                              </p>
                            </div>
                          </div>
                        </button>
                      ))
                    )}
                  </div>
                </div>
              </>
            )}
          </div>

          {}
          <div className="relative">
            <button
              onClick={() => setShowDropdown(!showDropdown)}
              className="flex items-center gap-2 pl-2 pr-1 py-1 rounded-full hover:bg-slate-100 transition-colors group"
            >
              {}
              <div className="w-9 h-9 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full flex items-center justify-center text-white font-semibold text-sm shadow-md shadow-indigo-500/20">
                {getInitials()}
              </div>

              {}
              <div className="hidden sm:block text-left">
                <p className="text-sm font-medium text-slate-800 leading-tight">
                  {user?.name || 'User'}
                </p>
                <p className="text-xs text-slate-500 leading-tight">
                  @{user?.username || 'username'}
                </p>
              </div>

              <ChevronDown className={`w-4 h-4 text-slate-400 transition-transform duration-200 ${showDropdown ? 'rotate-180' : ''}`} />
            </button>

            {}
            {showDropdown && (
              <>
                {}
                <div
                  className="fixed inset-0 z-40"
                  onClick={() => setShowDropdown(false)}
                />

                <div className="absolute right-0 mt-2 w-56 bg-white rounded-xl shadow-xl border border-slate-100 py-1 z-50 overflow-hidden animate-fade-in">
                  {}
                  <div className="px-4 py-3 border-b border-slate-100">
                    <p className="font-semibold text-slate-800">{user?.name || 'User'}</p>
                    <p className="text-xs text-slate-500">@{user?.username || 'username'}</p>
                    <span className="inline-block mt-1 px-2 py-0.5 bg-indigo-50 text-indigo-600 text-[10px] font-semibold rounded-full">
                      {user?.role || 'ADMIN'}
                    </span>
                  </div>

                  {}
                  <div className="py-1">
                    <Link
                      to="/dashboard"
                      className="flex items-center gap-3 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
                      onClick={() => setShowDropdown(false)}
                    >
                      <Home className="w-4 h-4 text-slate-400" />
                      Dashboard
                    </Link>
                    <Link
                      to="/profile"
                      className="flex items-center gap-3 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
                      onClick={() => setShowDropdown(false)}
                    >
                      <User className="w-4 h-4 text-slate-400" />
                      Profile
                    </Link>
                    <Link
                      to="/settings"
                      className="flex items-center gap-3 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
                      onClick={() => setShowDropdown(false)}
                    >
                      <Settings className="w-4 h-4 text-slate-400" />
                      Settings
                    </Link>
                  </div>

                  {}
                  <div className="border-t border-slate-100"></div>

                  {}
                  <button
                    onClick={handleLogout}
                    className="flex items-center gap-3 w-full px-4 py-2 text-sm text-rose-600 hover:bg-rose-50 transition-colors"
                  >
                    <LogOut className="w-4 h-4" />
                    Sign Out
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}