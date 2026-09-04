// src/Pages/PaymentCallback.jsx
import { useEffect, useRef, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { chapaApi } from '../services/api';
import { useAuth } from '../hooks/useAuth';

export default function PaymentCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [status, setStatus] = useState('verifying');
  const [message, setMessage] = useState('');
  const confirmedRef = useRef(false);
  // Keep the redirect target in a ref so timers always navigate to the current
  // value even if `user` (and thus paymentsPath) settles after the effect ran.
  const paymentsPathRef = useRef('/payments');

  // Tenants land on /my-payments; admins on /payments.
  const paymentsPath = user?.role === 'TENANT' ? '/my-payments' : '/payments';

  useEffect(() => {
    paymentsPathRef.current = paymentsPath;
  }, [paymentsPath]);

  useEffect(() => {
    const verifyPayment = async () => {
      // Guard against React StrictMode double-invoke / re-mounts so we don't
      // call the confirm endpoint (which is idempotent, but no need to redo it).
      if (confirmedRef.current) return;
      confirmedRef.current = true;

      const txRef = searchParams.get('tx_ref');
      const statusParam = searchParams.get('status');

      console.log('🔍 Payment callback received:', { txRef, statusParam });

      // If no tx_ref, show error
      if (!txRef) {
        setStatus('error');
        setMessage('Invalid payment reference. Please contact support.');
        return;
      }

      // Check if payment was successful from URL param (Chapa redirect)
      if (statusParam === 'success') {
        try {
          // Confirm (and record) with Chapa API — marks invoice PAID
          const result = await chapaApi.confirm(txRef);

          if (result.verified) {
            setStatus('success');
            setMessage('✅ Payment successful! Your invoice has been marked as paid.');
            setTimeout(() => navigate(paymentsPathRef.current), 3000);
          } else {
            setStatus('error');
            setMessage(result.message || 'Payment verification failed. Please contact admin.');
          }
        } catch (err) {
          console.error('Verification error:', err);
          setStatus('error');
          setMessage(err.message || 'Failed to verify payment');
        }
      } else if (statusParam === 'return') {
        // User returned from Chapa but didn't complete payment
        setStatus('error');
        setMessage('Payment was not completed. Please try again.');
        setTimeout(() => navigate(paymentsPathRef.current), 3000);
      } else {
        // Unknown status
        setStatus('error');
        setMessage('Unknown payment status. Please contact admin.');
      }
    };

    verifyPayment();
  }, [searchParams, navigate]);

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl p-8 max-w-md w-full text-center">
        {status === 'verifying' && (
          <>
            <Loader2 className="w-16 h-16 mx-auto text-indigo-600 animate-spin" />
            <h2 className="text-xl font-bold text-slate-800 mt-4">Verifying Payment...</h2>
            <p className="text-slate-500 mt-2">Please wait while we confirm your payment.</p>
          </>
        )}

        {status === 'success' && (
          <>
            <CheckCircle className="w-16 h-16 mx-auto text-emerald-500" />
            <h2 className="text-xl font-bold text-slate-800 mt-4">Payment Successful! 🎉</h2>
            <p className="text-slate-600 mt-2">{message}</p>
            <p className="text-sm text-slate-400 mt-4">Redirecting to invoices...</p>
          </>
        )}

        {status === 'error' && (
          <>
            <XCircle className="w-16 h-16 mx-auto text-rose-500" />
            <h2 className="text-xl font-bold text-slate-800 mt-4">Payment Failed</h2>
            <p className="text-slate-600 mt-2">{message}</p>
            <button
              onClick={() => navigate(paymentsPath)}
              className="mt-4 px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition"
            >
              Return to {user?.role === 'TENANT' ? 'My Payments' : 'Payments'}
            </button>
          </>
        )}
      </div>
    </div>
  );
}