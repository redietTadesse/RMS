import { createContext, useState, useEffect, useCallback, useRef, useMemo } from 'react';
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
    // Stable user ref so callbacks/effects don't need `user` as a dependency.
    const userRef = useRef(null);

    const setUserStable = useCallback((userData) => {
        userRef.current = userData;
        setUser(userData);
    }, []);

    const logout = useCallback(async () => {
        loggedOutRef.current = true;
      
        const refreshToken = tokenManager.getRefreshToken();
        if (refreshToken) {
            await authApi.logout(refreshToken).catch(() => {});
        }
        tokenManager.clearAll();
        setUserStable(null);
        if (refreshTimeoutRef.current) {
            clearTimeout(refreshTimeoutRef.current);
            refreshTimeoutRef.current = null;
        }
    }, [setUserStable]);

    const refreshPromiseRef = useRef(null);
    const scheduleTokenRefreshRef = useRef(null);
    const refreshAccessTokenRef = useRef(null);

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

    const refreshAccessToken = useCallback(async () => {
        // Prevent multiple concurrent refreshes (race condition)
        if (refreshPromiseRef.current) {
            return refreshPromiseRef.current;
        }

        const attemptRefresh = async () => {
            const maxAttempts = 3;
            for (let attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    const refreshToken = tokenManager.getRefreshToken();
                    if (!refreshToken) {
                        await logout();
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
                            setUserStable(userData);
                        }

                        const expiry = tokenManager.getTokenExpiry();
                        scheduleTokenRefresh(expiry);
                        return true;
                    }
                    return false;
                } catch {
                    if (attempt >= maxAttempts) {
                        await logout();
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
    }, [logout, scheduleTokenRefresh, setUserStable]);



    useEffect(() => {
        refreshAccessTokenRef.current = refreshAccessToken;
    }, [refreshAccessToken]);

    useEffect(() => {
        scheduleTokenRefreshRef.current = scheduleTokenRefresh;
    }, [scheduleTokenRefresh]);

    // ─── Graceful session lifecycle ────────────────────────────────────────────
    // api.js dispatches these events when a token refresh fails (session expired)
    // or succeeds (tokens were rotated out-of-band by the 401 interceptor).
    // Uses a ref for logout so this effect subscribes once and never re-subscribes.
    const logoutRef = useRef(logout);
    useEffect(() => {
        logoutRef.current = logout;
    });
    useEffect(() => {
        const onSessionExpired = () => {
            sessionStorage.setItem('rms_auth_message', 'session');
            logoutRef.current?.();
        };
        const onTokenRefreshed = (e) => {
            // Only update if the user actually changed (avoid identity churn)
            if (e?.detail && !loggedOutRef.current) {
                const prev = userRef.current;
                const next = e.detail;
                if (!prev || prev.id !== next.id || prev.role !== next.role ||
                    prev.username !== next.username || prev.name !== next.name) {
                    setUserStable(next);
                }
            }
        };

        window.addEventListener(AUTH_EVENTS.SESSION_EXPIRED, onSessionExpired);
        window.addEventListener(AUTH_EVENTS.TOKEN_REFRESHED, onTokenRefreshed);
        return () => {
            window.removeEventListener(AUTH_EVENTS.SESSION_EXPIRED, onSessionExpired);
            window.removeEventListener(AUTH_EVENTS.TOKEN_REFRESHED, onTokenRefreshed);
        };
    }, [setUserStable]);

    // Stable init — uses an initRef guard so StrictMode's double-invoke doesn't
    // re-run auth initialization or re-create timers.
    const initRef = useRef(false);
    useEffect(() => {
        if (initRef.current) return;
        initRef.current = true;

        const initAuth = async () => {
            const token = tokenManager.getAccessToken();
            const storedUser = tokenManager.getUser();

            if (token && tokenManager.isTokenValid(token)) {
                setUserStable(storedUser);
                const expiry = tokenManager.getTokenExpiry();
                scheduleTokenRefresh(expiry);
            } else if (token) {
                try {
                    await refreshAccessToken();
                } catch {
                    /* handled in refreshAccessToken */
                }
            }
            setLoading(false);
        };

        initAuth();

        return () => {
            if (refreshTimeoutRef.current) {
                clearTimeout(refreshTimeoutRef.current);
            }
        };
    }, [refreshAccessToken, scheduleTokenRefresh, setUserStable]);

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
            setUserStable(userData);

            const expiry = tokenManager.getTokenExpiry();
            scheduleTokenRefresh(expiry);

            return userData;
        } catch (err) {
            setError(err.message || 'Login failed');
            throw err;
        }
    }, [scheduleTokenRefresh, setUserStable]);

    const updateUser = useCallback((userData) => {
        const current = userRef.current;
        const updated = { ...current, ...userData };
        tokenManager.setUser(updated);
        setUserStable(updated);
    }, [setUserStable]);

    // Memoize the context value so consumers only re-render when user/loading/error
    // actually change — not on every AuthProvider render.
    const value = useMemo(() => ({
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
    }), [user, loading, error, login, logout, updateUser, refreshAccessToken]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;

// Backward-compatibility shim: stale/cached module graphs (e.g. a browser tab
// from before the useAuth refactor) may still import `useAuth` from this file.
// Re-exporting the canonical hook keeps those imports resolving correctly.
// The canonical implementation lives in src/hooks/useAuth.js.
// eslint-disable-next-line react-refresh/only-export-components -- deliberate re-export shim, see comment above
export { useAuth } from '../hooks/useAuth';
