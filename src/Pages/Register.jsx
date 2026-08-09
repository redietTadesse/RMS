
import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Building, User, Lock, Eye, EyeOff, ArrowRight, Loader2, CheckCircle, Shield, Users, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { adminApi } from '../services/api';

export default function Register() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  
  const [formData, setFormData] = useState({
    username: '',
    name: '',
    password: '',
    confirmPassword: '',
    role: 'TENANT',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  useEffect(() => {
    if (user && !isAdmin) {
      navigate('/dashboard');
    }
  }, [user, isAdmin, navigate]);

  if (user && !isAdmin) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-xl p-8 max-w-md text-center">
          <div className="w-16 h-16 mx-auto bg-red-100 rounded-full flex items-center justify-center mb-4">
            <AlertCircle className="w-8 h-8 text-red-600" />
          </div>
          <h2 className="text-xl font-bold text-slate-800">Access Denied</h2>
          <p className="text-slate-500 mt-2">Only administrators can create new users.</p>
          <Link to="/dashboard" className="mt-4 inline-block text-indigo-600 hover:text-indigo-700">
            ← Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);

    
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    if (formData.username.length < 3) {
      setError('Username must be at least 3 characters');
      return;
    }

    if (!formData.name.trim()) {
      setError('Full name is required');
      return;
    }

    setLoading(true);
    try {
      await adminApi.createUser({
        username: formData.username,
        name: formData.name,
        password: formData.password,
        role: formData.role,
      });
      setSuccess(true);
      setTimeout(() => {
        setSuccess(false);
        setFormData({
          username: '',
          name: '',
          password: '',
          confirmPassword: '',
          role: 'TENANT',
        });
      }, 3000);
    } catch (err) {
      setError(err.message || 'Failed to create user. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen grid grid-cols-1 lg:grid-cols-2 bg-slate-50">
      
      {}
      <div className="flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-md animate-fade-in">
          
          {}
          <div className="flex items-center gap-2.5 lg:hidden mb-10">
            <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <Building className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-slate-800 font-bold text-xl leading-none">RMS</h1>
              <p className="text-[10px] text-slate-400 uppercase tracking-wider mt-0.5">Rental System</p>
            </div>
          </div>

          <h2 className="text-3xl font-bold text-slate-800">Create New User 🚀</h2>
          <p className="text-slate-500 mt-2 mb-8">Admins can create tenant or admin users here.</p>

          {}
          {success && (
            <div className="mb-4 p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-sm text-emerald-700 flex items-center gap-2">
              <CheckCircle className="w-5 h-5" />
              User created successfully.
            </div>
          )}

          {}
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Username *</label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type="text"
                  name="username"
                  required
                  value={formData.username}
                  onChange={handleChange}
                  className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                  placeholder="Choose a username (min 3 chars)"
                  autoComplete="username"
                />
              </div>
            </div>

            {}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Full Name *</label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type="text"
                  name="name"
                  required
                  value={formData.name}
                  onChange={handleChange}
                  className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                  placeholder="Enter your full name"
                  autoComplete="name"
                />
              </div>
            </div>

            {}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Password *</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  required
                  value={formData.password}
                  onChange={handleChange}
                  className="w-full pl-11 pr-11 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                  placeholder="Min 6 characters"
                  autoComplete="new-password"
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

            {}
            {isAdmin && (
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Register as</label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setFormData({ ...formData, role: 'ADMIN' })}
                    className={`flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg border-2 transition-all ${
                      formData.role === 'ADMIN'
                        ? 'border-indigo-600 bg-indigo-50 text-indigo-700'
                        : 'border-slate-200 hover:border-slate-300 text-slate-600'
                    }`}
                  >
                    <Shield className="w-4 h-4" />
                    Admin
                  </button>
                  <button
                    type="button"
                    onClick={() => setFormData({ ...formData, role: 'TENANT' })}
                    className={`flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg border-2 transition-all ${
                      formData.role === 'TENANT'
                        ? 'border-indigo-600 bg-indigo-50 text-indigo-700'
                        : 'border-slate-200 hover:border-slate-300 text-slate-600'
                    }`}
                  >
                    <Users className="w-4 h-4" />
                    Tenant
                  </button>
                </div>
              </div>
            )}

            {}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Confirm Password *</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  required
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className="w-full pl-11 pr-11 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                  placeholder="Confirm your password"
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            {}
            <button
              type="submit"
              disabled={loading || success}
              className="w-full flex items-center justify-center gap-2 py-3 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-xl shadow-lg shadow-indigo-500/30 transition-all disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" /> Creating account...
                </>
              ) : (
                <>
                  Create Account <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {}
          <p className="text-center text-sm text-slate-500 mt-8">
            Admin-only user creation.{' '}
            <Link to="/users" className="font-medium text-indigo-600 hover:text-indigo-700">
              View all users
            </Link>
          </p>
        </div>
      </div>

      {}
      <div className="hidden lg:flex relative items-center justify-center p-12 bg-gradient-to-br from-slate-900 via-indigo-900 to-slate-900 overflow-hidden">
        
        <div className="absolute top-0 left-0 w-96 h-96 bg-indigo-500/20 rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2"></div>
        <div className="absolute bottom-0 right-0 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl translate-x-1/3 translate-y-1/3"></div>

        <div className="relative z-10 max-w-md text-white text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-white/10 backdrop-blur-md rounded-2xl mb-6 border border-white/20">
            <Building className="w-8 h-8 text-indigo-300" />
          </div>
          <h2 className="text-4xl font-bold leading-tight">
            Join the RMS Community
          </h2>
          <p className="text-indigo-200/80 mt-4 text-lg">
            Create your account and start managing commercial spaces, tenants, and leases efficiently.
          </p>

          <div className="mt-10 bg-white/10 backdrop-blur-md border border-white/20 rounded-2xl p-6 text-left shadow-2xl">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-3 h-3 bg-emerald-400 rounded-full animate-pulse"></div>
              <span className="text-sm text-indigo-200">Free forever • No credit card required</span>
            </div>
            <div className="space-y-2 text-sm text-indigo-200/80">
              <p>✅ Manage unlimited spaces</p>
              <p>✅ Track tenants and leases</p>
              <p>✅ Automated invoice generation</p>
              <p>✅ Real-time payment tracking</p>
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}