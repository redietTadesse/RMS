// src/pages/Login.jsx
import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Building, User, Lock, Eye, EyeOff, ArrowRight, Loader2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const navigate = useNavigate();
  const { login, error: authError, loading: authLoading } = useAuth();
  
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      await login(username, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Login failed. Please try again.');
    }
  };

  return (
    <div className="min-h-screen grid grid-cols-1 lg:grid-cols-2 bg-slate-50">
      
      {/* Left Side - Form */}
      <div className="flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-md animate-fade-in">
          
          {/* Mobile Logo */}
          <div className="flex items-center gap-2.5 lg:hidden mb-10">
            <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <Building className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-slate-800 font-bold text-xl leading-none">RMS</h1>
              <p className="text-[10px] text-slate-400 uppercase tracking-wider mt-0.5">Rental System</p>
            </div>
          </div>

          <h2 className="text-3xl font-bold text-slate-800">Welcome back 👋</h2>
          <p className="text-slate-500 mt-2 mb-8">Please enter your details to sign in.</p>

          {/* Error Display */}
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {error}
            </div>
          )}
          {authError && !error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {authError}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Username Input */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Username</label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                  placeholder="Enter your username"
                  autoComplete="username"
                />
              </div>
            </div>

            {/* Password Input */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-sm font-medium text-slate-700">Password</label>
               <Link to="/forgot-password" className="text-xs font-medium text-indigo-600 hover:text-indigo-700">
                 Forgot password?
               </Link>
              </div>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-11 pr-11 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                  placeholder="••••••••"
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            {/* Remember Me */}
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="remember"
                className="w-4 h-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
              />
              <label htmlFor="remember" className="text-sm text-slate-600">Remember me</label>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={authLoading}
              className="w-full flex items-center justify-center gap-2 py-3 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-xl shadow-lg shadow-indigo-500/30 transition-all disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {authLoading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" /> Signing in...
                </>
              ) : (
                <>
                  Sign in <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Footer */}
          <p className="text-center text-sm text-slate-500 mt-8">
            Don't have an account?{' '}
            <Link to="/register" className="font-medium text-indigo-600 hover:text-indigo-700">
              Sign up
            </Link>
          </p>
        </div>
      </div>

      {/* Right Side - Branding/Visual */}
      <div className="hidden lg:flex relative items-center justify-center p-12 bg-gradient-to-br from-slate-900 via-indigo-900 to-slate-900 overflow-hidden">
        
        <div className="absolute top-0 left-0 w-96 h-96 bg-indigo-500/20 rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2"></div>
        <div className="absolute bottom-0 right-0 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl translate-x-1/3 translate-y-1/3"></div>

        <div className="relative z-10 max-w-md text-white text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-white/10 backdrop-blur-md rounded-2xl mb-6 border border-white/20">
            <Building className="w-8 h-8 text-indigo-300" />
          </div>
          <h2 className="text-4xl font-bold leading-tight">
            Simplify Your Rental Management
          </h2>
          <p className="text-indigo-200/80 mt-4 text-lg">
            The all-in-one platform for managing commercial spaces, tenants, leases, and payments.
          </p>

          <div className="mt-10 bg-white/10 backdrop-blur-md border border-white/20 rounded-2xl p-6 text-left shadow-2xl">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse"></div>
                <span className="text-xs text-indigo-200">System Online</span>
              </div>
              <span className="text-xs text-indigo-200">2026</span>
            </div>
            <div className="space-y-3">
              <div className="h-2 bg-white/20 rounded-full w-3/4"></div>
              <div className="h-2 bg-white/10 rounded-full"></div>
              <div className="h-2 bg-white/10 rounded-full w-5/6"></div>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <div className="bg-white/10 p-3 rounded-lg">
                <p className="text-xs text-indigo-200">Total Revenue</p>
                <p className="text-lg font-bold text-white">ETB 45,000</p>
              </div>
              <div className="bg-white/10 p-3 rounded-lg">
                <p className="text-xs text-indigo-200">Occupied</p>
                <p className="text-lg font-bold text-white">85%</p>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}