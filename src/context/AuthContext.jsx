
import { createContext, useState, useContext, useEffect } from 'react';
import { authApi } from '../services/api';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');
    
    console.log('🔍 Auth check - token:', !!token, 'userData:', !!userData);
    
    if (token && userData) {
      try {
        const parsedUser = JSON.parse(userData);
        setUser(parsedUser);
        console.log('✅ User restored from localStorage:', parsedUser.username, 'Role:', parsedUser.role);
      } catch (err) {
        console.error('❌ Failed to parse user data:', err);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    } else {
      console.log('❌ No token or user data found');
    }
    setLoading(false);
  }, []);

  const login = async (username, password) => {
    setError(null);
    try {
      const data = await authApi.login(username, password);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data));
      setUser(data);
      console.log('✅ Login successful:', data.username, 'Role:', data.role);
      return data;
    } catch (err) {
      setError(err.message || 'Login failed');
      throw err;
    }
  };

  const register = async (userData) => {
    setError(null);
    try {
      const data = await authApi.register(userData);
      console.log('✅ Registration successful:', data.username);
      return data;
    } catch (err) {
      setError(err.message || 'Registration failed');
      throw err;
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    console.log('👋 Logged out');
  };

  const value = {
    user,
    loading,
    error,
    login,
    register,
    logout,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ADMIN',
    isTenant: user?.role === 'TENANT',
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};