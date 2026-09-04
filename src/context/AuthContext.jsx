import { createContext, useState, useEffect, useCallback, useRef } from 'react';
import tokenManager from '../services/TokenManager';
import { authApi, AUTH_EVENTS } from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const refreshTimeoutRef = useRef(null);
    // Guards against a late in-flight token refresh resurrecting the session
    // after the user has explicitly logged out.
    const loggedOutRef = useRef(false);

    const logout = useCallback(async () => {
        loggedOutRef.current = true;
        // Always attempt server-side revocation — even with an expired access
        // token — by presenting the refresh token (endpoint is public now).
        const refreshToken = tokenManager.getRefreshToken();
        if (refreshToken) {
            await authApi.logout(refreshToken).catch(() => {});
        }
        tokenManager.clearAll();
        setUser(null);
        if (refreshTimeoutRef.current) {
            clearTimeout(refreshTimeoutRef.current);
            refreshTimeoutRef.current = null;
        }
    }, []);

    const refreshPromiseRef = useRef(null);
    const refreshAccessTokenRef = useRef(null);
    const scheduleTokenRefreshRef = useRef(null);

    const refreshAccessToken = useCallback(async () => {
        // Prevent multiple concurrent refreshes (race condition)
        if (refreshPromiseRef.current) {
            return refreshPromiseRef.current;
        }

        // Add retry logic (up to 3 attempts)
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
                        scheduleTokenRefreshRef.current(expiry);
                        return true;
                    }
                    return false;
                } catch {
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
    }, [logout]);

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
                refreshAccessTokenRef.current();
            }, refreshTime);
        }
    }, []);

    useEffect(() => {
        refreshAccessTokenRef.current = refreshAccessToken;
    }, [refreshAccessToken]);

    useEffect(() => {
        scheduleTokenRefreshRef.current = scheduleTokenRefresh;
    }, [scheduleTokenRefresh]);

    // ─── Graceful session lifecycle ────────────────────────────────────────────
    // api.js dispatches these events when a token refresh fails (session expired)
    // or succeeds (tokens were rotated out-of-band by the 401 interceptor).
    useEffect(() => {
        const onSessionExpired = () => {
            sessionStorage.setItem('rms_auth_message', 'session');
            logout();
        };
        const onTokenRefreshed = (e) => {
            if (e?.detail && !loggedOutRef.current) setUser(e.detail);
        };

        window.addEventListener(AUTH_EVENTS.SESSION_EXPIRED, onSessionExpired);
        window.addEventListener(AUTH_EVENTS.TOKEN_REFRESHED, onTokenRefreshed);
        return () => {
            window.removeEventListener(AUTH_EVENTS.SESSION_EXPIRED, onSessionExpired);
            window.removeEventListener(AUTH_EVENTS.TOKEN_REFRESHED, onTokenRefreshed);
        };
    }, [logout]);

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

            loggedOutRef.current = false;
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

export default AuthContext;

// Backward-compatibility shim: stale/cached module graphs (e.g. a browser tab
// from before the useAuth refactor) may still import `useAuth` from this file.
// Re-exporting the canonical hook keeps those imports resolving correctly.
// The canonical implementation lives in src/hooks/useAuth.js.
// eslint-disable-next-line react-refresh/only-export-components -- deliberate re-export shim, see comment above
export { useAuth } from '../hooks/useAuth';
