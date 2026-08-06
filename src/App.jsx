// src/App.jsx
import { Routes, Route } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import LandingPage from './pages/LandingPage';
import Login from './pages/Login';
import Register from './pages/Register';  // ← ADD THIS
import Dashboard from './pages/Dashboards';
import Spaces from './pages/Spaces';
import Tenants from './pages/Tenants';
import Leases from './pages/Leases';
import Payments from './pages/Payments';
import ProtectedRoute from './components/auth/ProtectedRoute';

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
      <Route path="/register" element={<Register />} />  {/* ← ADD THIS */}
      <Route 
        path="/dashboard" 
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/spaces" 
        element={
          <ProtectedRoute>
            <Spaces />
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/tenants" 
        element={
          <ProtectedRoute>
            <Tenants />
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/leases" 
        element={
          <ProtectedRoute>
            <Leases />
          </ProtectedRoute>
        } 
      />

      
      <Route 
  path="/payments" 
  element={
    <ProtectedRoute>
      <Payments />
    </ProtectedRoute> 
  }
  />


      <Route 
        path="/payments" 
        element={
          <ProtectedRoute>
            <Payments />
          </ProtectedRoute>
        } 
      />

      
    </Routes>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}

export default App;