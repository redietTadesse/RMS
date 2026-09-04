// src/App.jsx
import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './hooks/useAuth';
import LandingPage from './Pages/LandingPage';
import Login from './Pages/Login';
import Register from './Pages/Register';
import Dashboard from './Pages/Dashboards';
import Spaces from './Pages/Spaces';
import Tenants from './Pages/Tenants';
import Leases from './Pages/Leases';
import Payments from './Pages/Payments';
import TenantDashboard from './Pages/TenantDashboard';
import TenantPayment from './Pages/TenantPayment';
import PaymentCallback from './Pages/PaymentCallback';
import Profile from './Pages/Profile';
import Settings from './Pages/Settings';
import Reports from './Pages/Reports';
import UserManagement from './Pages/UserManagement';
import ProtectedRoute from './components/auth/ProtectedRoute';
import TimeTravelControls from './components/Test/TimeTravelControls';

function AppRoutes() {
  const { loading } = useAuth();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<Login />} />
      <Route path="/payment-callback" element={<PaymentCallback />} />

      <Route
        path="/register"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Register />
          </ProtectedRoute>
        }
      />
      <Route
        path="/reports"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Reports />
          </ProtectedRoute>
        }
      />
      <Route
        path="/users"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <UserManagement />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <Profile />
          </ProtectedRoute>
        }
      />
      <Route
        path="/settings"
        element={
          <ProtectedRoute>
            <Settings />
          </ProtectedRoute>
        }
      />

      <Route
        path="/dashboard"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Dashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/spaces"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Spaces />
          </ProtectedRoute>
        }
      />
      <Route
        path="/tenants"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Tenants />
          </ProtectedRoute>
        }
      />
      <Route
        path="/leases"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Leases />
          </ProtectedRoute>
        }
      />
      <Route
        path="/payments"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Payments />
          </ProtectedRoute>
        }
      />

      <Route
        path="/tenant-dashboard"
        element={
          <ProtectedRoute requiredRole="TENANT">
            <TenantDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/my-payments"
        element={
          <ProtectedRoute requiredRole="TENANT">
            <TenantPayment />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

function App() {
  return (
    <AuthProvider>   // ← MUST wrap everything
      <AppRoutes />
      <TimeTravelControls />
    </AuthProvider>
  );
}

export default App;