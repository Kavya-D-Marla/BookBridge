import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/axios';

export interface User {
  id: string;
  name: string;
  email: string;
  role: 'student' | 'admin';
  profilePicture?: string;
  rating?: number;
  createdAt?: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  updateUser: (updatedUser: Partial<User>) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('bookbridge_token');
      const storedUser = localStorage.getItem('bookbridge_user');

      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
        
        // Verify token against backend on startup
        try {
          const response = await api.get('/auth/me');
          const dbUser = response.data?.data;
          if (dbUser) {
            const mappedUser = {
              id: String(dbUser.user_id),
              name: dbUser.user_name || '',
              email: dbUser.email || '',
              role: dbUser.role || 'buyer',
              profilePicture: dbUser.profile_picture,
              rating: dbUser.rating,
              createdAt: dbUser.created_at,
            };
            setUser(mappedUser as any);
            localStorage.setItem('bookbridge_user', JSON.stringify(mappedUser));
          }
        } catch (error) {
          console.error('Failed to verify token', error);
          // If token verification fails with 401, axios interceptor handles it,
          // but if it fails for other reasons (e.g. backend offline), we keep the stored session for offline resiliency
        }
      }
      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const login = (newToken: string, newUser: User) => {
    localStorage.setItem('bookbridge_token', newToken);
    localStorage.setItem('bookbridge_user', JSON.stringify(newUser));
    setToken(newToken);
    setUser(newUser);
  };

  const logout = () => {
    localStorage.removeItem('bookbridge_token');
    localStorage.removeItem('bookbridge_user');
    setToken(null);
    setUser(null);
  };

  const updateUser = (updatedFields: Partial<User>) => {
    if (user) {
      const updatedUser = { ...user, ...updatedFields };
      setUser(updatedUser);
      localStorage.setItem('bookbridge_user', JSON.stringify(updatedUser));
    }
  };

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        isLoading,
        login,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
