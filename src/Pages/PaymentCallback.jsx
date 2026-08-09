import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { chapaApi } from '../services/api';

export default function PaymentCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('verifying');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const verifyPayment = async () => {
      const txRef = searchParams.get('tx_ref');
      const statusParam = searchParams.get('status');

      if (!txRef) {
        setStatus('error');
        setMessage('Invalid payment reference');
        return;
      }

      try {
        const result = await chapaApi.verify(txRef);
        if (result.verified) {
          setStatus('success');
          setMessage('Payment successful! Your invoice has been marked as paid.');
          setTimeout(() => navigate('/payments'), 5000);
        } else {
          setStatus('error');
          setMessage('Payment verification failed. Please contact admin.');
        }
      } catch (err) {
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
            <p className="text-slate-500 mt-2">{message}</p>
            <p className="text-sm text-slate-400 mt-4">Redirecting to invoices...</p>
          </>
        )}

        {status === 'error' && (
          <>
            <XCircle className="w-16 h-16 mx-auto text-rose-500" />
            <h2 className="text-xl font-bold text-slate-800 mt-4">Payment Failed</h2>
            <p className="text-slate-500 mt-2">{message}</p>
            <button
              onClick={() => navigate('/payments')}
              className="mt-4 px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg transition"
            >
              Return to Payments
            </button>
          </>
        )}
      </div>
    </div>
  );
}
