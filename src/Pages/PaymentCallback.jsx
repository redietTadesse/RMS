// src/Pages/PaymentCallback.jsx
import { useEffect, useRef, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { arifpayApi } from '../services/api';
import { useAuth } from '../hooks/useAuth';

const SESSION_KEY = 'arifpaySessionId';
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

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
      // Guard against React StrictMode double-invoke / re-mounts.
      if (confirmedRef.current) return;
      confirmedRef.current = true;

      const statusParam = searchParams.get('status');
      const sessionId = sessionStorage.getItem(SESSION_KEY);

      // User cancelled on ArifPay, or ArifPay reported an error
      if (statusParam === 'return') {
        sessionStorage.removeItem(SESSION_KEY);
        setStatus('error');
        setMessage('Payment was not completed. Please try again.');
        setTimeout(() => navigate(paymentsPathRef.current), 3000);
        return;
      }

      if (statusParam !== 'success') {
        setStatus('error');
        setMessage('Unknown payment status. Please contact admin.');
        return;
      }

      if (!sessionId) {
        setStatus('error');
        setMessage('Payment session not found. If you were charged, please contact admin.');
        return;
      }

      try {
        // ArifPay can redirect a few seconds before it marks the payment SUCCESS,
        // so check up to 4 times before giving up.
        let result = null;
        for (let attempt = 0; attempt < 4; attempt++) {
          result = await arifpayApi.verify(sessionId);
          if (result?.verified) break;
          if (attempt < 3) await sleep(2500);
        }

        if (result?.verified) {
          sessionStorage.removeItem(SESSION_KEY);
          setStatus('success');
          setMessage('✅ Payment successful! Your invoice has been marked as paid.');
          setTimeout(() => navigate(paymentsPathRef.current), 3000);
        } else {
          setStatus('error');
          setMessage(
            'Payment not confirmed yet. If you were charged, your invoice will update shortly. Otherwise please contact admin.'
          );
        }
      } catch (err) {
        console.error('Verification error:', err);
        setStatus('error');
        setMessage(err.message || 'Failed to verify payment');
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