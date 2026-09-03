import { createContext, useState, useContext, useEffect, useCallback, useRef } from 'react';
import tokenManager from '../services/TokenManager';
import { authApi } from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const refreshTimeoutRef = useRef(null);

    const scheduleTokenRefresh = useCallback((expiry) => {
        if (refreshTimeoutRef.current) {
            clearTimeout(refreshTimeoutRef.current);
        }

        if (!expiry) return;

        const now = Date.now();
        const timeUntilExpiry = expiry.getTime() - now;
        const refreshTime = Math.max(timeUntilExpiry - 5 * 60 * 1000, 60000);

        if (refreshTime > 0) {
            refreshTimeoutRef.current = setTimeout(() => {
                refreshAccessToken();
            }, refreshTime);
        }
    }, []);

    const refreshPromiseRef = useRef(null);

    const refreshAccessToken = useCallback(async () => {
        // ✅ FIX: Prevent multiple concurrent refreshes (race condition)
        if (refreshPromiseRef.current) {
            return refreshPromiseRef.current;
        }

        // ✅ FIX: Add retry logic (up to 3 attempts)
        const attemptRefresh = async () => {
            const maxAttempts = 3;
            for (let attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    const refreshToken = tokenManager.getRefreshToken();
                    if (!refreshToken) {
                        logout();
                        return false;
                    }

                    const response = await authApi.refreshToken(refreshToken);

                    if (response?.token) {
                        tokenManager.setTokens(response.token, response.refreshToken || refreshToken);

                        if (response.id && response.username) {
                            const userData = {
                                id: response.id,
                                username: response.username,
                                name: response.name,
                                email: response.email,
                                role: response.role,
                            };
                            tokenManager.setUser(userData);
                            setUser(userData);
                        }

                        const expiry = tokenManager.getTokenExpiry();
                        scheduleTokenRefresh(expiry);
                        return true;
                    }
                    return false;
                } catch (err) {
                    // On last attempt, log out; otherwise wait and retry
                    if (attempt >= maxAttempts) {
                        logout();
                        return false;
                    }
                    await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
                }
            }
            return false;
        };

        refreshPromiseRef.current = attemptRefresh();
        try {
            return await refreshPromiseRef.current;
        } finally {
            refreshPromiseRef.current = null;
        }
    }, [scheduleTokenRefresh]);

    useEffect(() => {
        const initAuth = async () => {
            const token = tokenManager.getAccessToken();
            const storedUser = tokenManager.getUser();

            if (token && tokenManager.isTokenValid(token)) {
                setUser(storedUser);
                const expiry = tokenManager.getTokenExpiry();
                scheduleTokenRefresh(expiry);
            } else if (token) {
                await refreshAccessToken();
            }
            setLoading(false);
        };

        initAuth();

        return () => {
            if (refreshTimeoutRef.current) {
                clearTimeout(refreshTimeoutRef.current);
            }
        };
    }, [refreshAccessToken, scheduleTokenRefresh]);

    const login = useCallback(async (username, password) => {
        setError(null);
        try {
            const response = await authApi.login(username, password);

            if (!response?.token) {
                throw new Error('Invalid login response');
            }

            tokenManager.setTokens(response.token, response.refreshToken);

            const userData = {
                id: response.id,
                username: response.username,
                name: response.name,
                email: response.email,
                role: response.role,
            };

            tokenManager.setUser(userData);
            setUser(userData);

            const expiry = tokenManager.getTokenExpiry();
            scheduleTokenRefresh(expiry);

            return userData;
        } catch (err) {
            setError(err.message || 'Login failed');
            throw err;
        }
    }, [scheduleTokenRefresh]);

    const logout = useCallback(async () => {
        try {
            const token = tokenManager.getAccessToken();
            if (token) {
                await authApi.logout().catch(() => {});
            }
        } catch (_) {}
        finally {
            tokenManager.clearAll();
            setUser(null);
            if (refreshTimeoutRef.current) {
                clearTimeout(refreshTimeoutRef.current);
                refreshTimeoutRef.current = null;
            }
        }
    }, []);

    const updateUser = useCallback((userData) => {
        const updated = { ...user, ...userData };
        tokenManager.setUser(updated);
        setUser(updated);
    }, [user]);

    const value = {
        user,
        loading,
        error,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'ADMIN',
        isTenant: user?.role === 'TENANT',
        login,
        logout,
        updateUser,
        refreshAccessToken,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within AuthProvider');
    }
    return context;
};

export default AuthContext;
