// src/Pages/ForgotPassword.jsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Building, Mail, ArrowRight, Loader2, CheckCircle } from 'lucide-react';
import { authApi } from '../services/api';

export default function ForgotPassword() {
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await authApi.forgotPassword(usernameOrEmail);
      setSent(true);
    } catch (err) {
      setError(err.message || 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-6">
      <div className="w-full max-w-md">
        <div className="flex items-center gap-2.5 mb-10 justify-center">
          <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center shadow-lg shadow-indigo-500/30">
            <Building className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-slate-800 font-bold text-xl leading-none">RMS</h1>
            <p className="text-[10px] text-slate-400 uppercase tracking-wider mt-0.5">Rental System</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-8">
          {sent ? (
            <div className="text-center">
              <CheckCircle className="w-12 h-12 text-emerald-500 mx-auto mb-4" />
              <h2 className="text-xl font-bold text-slate-800">Check your email</h2>
              <p className="text-slate-500 mt-2 text-sm">
                If an account exists for <span className="font-medium">{usernameOrEmail}</span>,
                we've sent a password reset link. It expires in 30 minutes.
              </p>
              <Link
                to="/login"
                className="inline-block mt-6 text-sm font-medium text-indigo-600 hover:text-indigo-700"
              >
                Back to sign in
              </Link>
            </div>
          ) : (
            <>
              <h2 className="text-2xl font-bold text-slate-800">Forgot password?</h2>
              <p className="text-slate-500 mt-2 mb-6 text-sm">
                Enter your username or email and we'll send you a reset link.
              </p>

              {error && (
                <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">
                    Username or Email
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                    <input
                      type="text"
                      required
                      value={usernameOrEmail}
                      onChange={(e) => setUsernameOrEmail(e.target.value)}
                      className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                      placeholder="admin or admin@example.com"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full flex items-center justify-center gap-2 py-3 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-xl shadow-lg shadow-indigo-500/30 transition-all disabled:opacity-70"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin" /> Sending...
                    </>
                  ) : (
                    <>
                      Send reset link <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              </form>

              <p className="text-center text-sm text-slate-500 mt-6">
                <Link to="/login" className="font-medium text-indigo-600 hover:text-indigo-700">
                  Back to sign in
                </Link>
              </p>
            </>
          )}
        </div>
      </div>
    </div>
  );
}