
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function ProtectedRoute({ children, requiredRole }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  console.log('🔒 ProtectedRoute - loading:', loading, 'user:', !!user, 'role:', user?.role);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!user) {
    console.log('🔒 No user, redirecting to login');
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  
  if (requiredRole && user.role !== requiredRole) {
    console.log(`🔒 Role mismatch: required ${requiredRole}, got ${user.role}`);
    
    if (user.role === 'ADMIN') {
      return <Navigate to="/dashboard" replace />;
    } else if (user.role === 'TENANT') {
      return <Navigate to="/tenant-dashboard" replace />;
    }
    return <Navigate to="/login" replace />;
  }

  console.log('🔒 User authenticated, rendering children');
  return children;
}