import React from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Building2, Users, FileText,
  CreditCard, X, Building, LogOut, Shield,
  Settings, HelpCircle, ChevronRight, BarChart3
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export default function Sidebar({ open, onClose }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/login');
    onClose();
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

  const isAdmin = user?.role === 'ADMIN';
  const isTenant = user?.role === 'TENANT';

  // ✅ Fixed: closed the array with ]
  const navItems = [
    {
      to: isAdmin ? '/dashboard' : '/tenant-dashboard',
      icon: LayoutDashboard,
      label: 'Dashboard'
    },
    ...(isAdmin ? [
      { to: '/spaces', icon: Building2, label: 'Spaces' },
      { to: '/tenants', icon: Users, label: 'Tenants' },
      { to: '/leases', icon: FileText, label: 'Leases' },
      { to: '/payments', icon: CreditCard, label: 'Payments' },
      { to: '/reports', icon: BarChart3, label: 'Reports' },
      { to: '/users', icon: Shield, label: 'Users' },
    ] : []),
  ]; // ✅ THIS WAS MISSING

  return (
    <>
      {/* Overlay for mobile */}
      {open && (
        <div
          className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-40 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={`fixed lg:sticky top-0 left-0 h-screen w-64 bg-slate-900 text-slate-300
        flex flex-col z-50 transition-transform duration-300 ease-out
        ${open ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-800/80">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <Building className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-white font-bold text-lg leading-none">RMS</h1>
              <p className="text-[10px] text-slate-500 uppercase tracking-wider mt-0.5">Rental System</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="lg:hidden text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-5 space-y-1 overflow-y-auto">
          <p className="px-3 text-[10px] font-semibold text-slate-500 uppercase tracking-wider mb-3">
            {isAdmin ? 'Admin Menu' : 'My Account'}
          </p>

          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) =>
                `group flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200
                ${isActive
                  ? 'bg-gradient-to-r from-indigo-500/20 to-purple-600/20 text-white shadow-lg shadow-indigo-500/10'
                  : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'}`
              }
            >
              <div className="flex items-center gap-3">
                <item.icon className={`w-[18px] h-[18px] transition-colors ${
                  location.pathname === item.to ? 'text-indigo-400' : 'text-slate-500 group-hover:text-slate-300'
                }`} />
                <span>{item.label}</span>
              </div>
              {location.pathname === item.to && (
                <ChevronRight className="w-4 h-4 text-indigo-400" />
              )}
            </NavLink>
          ))}

          {/* Support section */}
          <div className="my-4 border-t border-slate-800/60"></div>

          <p className="px-3 text-[10px] font-semibold text-slate-500 uppercase tracking-wider mb-3">Support</p>

          <NavLink
            to="/settings"
            onClick={onClose}
            className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-slate-400 hover:bg-slate-800/50 hover:text-white transition-all duration-200"
          >
            <Settings className="w-[18px] h-[18px] text-slate-500" />
            Settings
          </NavLink>

          <NavLink
            to="/help"
            onClick={onClose}
            className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-slate-400 hover:bg-slate-800/50 hover:text-white transition-all duration-200"
          >
            <HelpCircle className="w-[18px] h-[18px] text-slate-500" />
            Help & Support
          </NavLink>
        </nav>

        {/* Footer */}
        <div className="px-3 py-4 border-t border-slate-800/80">
          <div className="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-slate-800/40 hover:bg-slate-800/60 transition-colors group cursor-pointer">
            <div className="w-9 h-9 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full flex items-center justify-center text-white font-semibold text-sm shadow-lg shadow-indigo-500/20 shrink-0">
              {getInitials()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate">
                {user?.name || 'User'}
              </p>
              <p className="text-xs text-slate-500 truncate">
                @{user?.username || 'username'}
              </p>
            </div>
          </div>

          {/* Role badge */}
          <div className="mt-2 px-3">
            <span className={`inline-block px-2.5 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider
              ${isAdmin ? 'bg-indigo-500/20 text-indigo-400' : isTenant ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-700/20 text-slate-400'}`}>
              {user?.role || 'USER'}
            </span>
          </div>

          {/* Logout */}
          <button
            onClick={handleLogout}
            className="flex items-center justify-center gap-2 w-full mt-2 px-3 py-2 rounded-lg text-sm font-medium text-rose-400 hover:bg-rose-500/10 hover:text-rose-300 transition-colors"
          >
            <LogOut className="w-4 h-4" />
            Sign Out
          </button>
        </div>
      </aside>
    </>
  );
}