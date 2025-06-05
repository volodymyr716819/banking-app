import { defineStore } from 'pinia';
import api from '../lib/api';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    lastLogin: localStorage.getItem('lastLogin') || null,
    isLoading: false,
    error: null
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isEmployee: (state) => 
      state.user?.role?.toLowerCase() === 'employee',
    isCustomer: (state) => 
      state.user?.role?.toLowerCase() === 'customer',
    userRole: (state) => 
      state.user?.role?.toLowerCase() || null,
    userName: (state) =>
      state.user?.name || 'Guest',
    userEmail: (state) =>
      state.user?.email || '',
    userId: (state) =>
      state.user?.id || null,
    formattedLastLogin: (state) => {
      if (!state.lastLogin) return 'Not available';
      return new Date(state.lastLogin).toLocaleString();
    }
  },

  actions: {
    async login(email, password) {
      this.isLoading = true;
      this.error = null;
      
      try {
        const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
        const res = await api.post(`${apiUrl}/auth/login`, {
          email,
          password
        });

        this.user = {
          id: res.data.id,
          email: res.data.email,
          name: res.data.name,
          role: res.data.role
        };

        this.token = res.data.token;
        
        // Record login time
        this.lastLogin = new Date().toISOString();

        // Store auth data in localStorage
        this.persistAuthState();
        
        // Set Authorization header for future requests
        api.defaults.headers.common['Authorization'] = `Bearer ${this.token}`;
        
        this.isLoading = false;
        return true;
      } catch (err) {
        this.isLoading = false;
        this.error = err.response?.data?.message || err.message;
        throw new Error(
          "Login failed: " + this.error
        );
      }
    },

    logout() {
      this.user = null;
      this.token = null;
      this.error = null;
      
      // Clear localStorage
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      localStorage.removeItem('lastLogin');
      
      // Remove Authorization header
      delete api.defaults.headers.common['Authorization'];
    },
    
    persistAuthState() {
      // Save authentication state to localStorage
      localStorage.setItem('user', JSON.stringify(this.user));
      localStorage.setItem('token', this.token);
      localStorage.setItem('lastLogin', this.lastLogin);
    },
    
    updateUserInfo(userData) {
      // Method to update user info if needed
      if (!this.user) return;
      
      this.user = {
        ...this.user,
        ...userData
      };
      
      // Persist updated user info
      this.persistAuthState();
    },
    
    // Check if token is still valid
    async validateToken() {
      if (!this.token) return false;
      
      try {
        const res = api.get(`/auth/validate`, {
          headers: {
            Authorization: `Bearer ${this.token}`
          }
        });
        
        // Update user data if needed
        if (res.data && res.data.valid) {
          // Update last validation time as an active session indicator
          if (!this.lastLogin) {
            this.lastLogin = new Date().toISOString();
            this.persistAuthState();
          }
          
          // Update user info if returned from validation
          if (res.data.id || res.data.name || res.data.email || res.data.role) {
            this.updateUserInfo({
              id: res.data.id || this.user.id,
              name: res.data.name || this.user.name,
              email: res.data.email || this.user.email,
              role: res.data.role || this.user.role
            });
          }
          return true;
        }
        
        return false;
      } catch (error) {
        // If token validation fails, logout the user
        if (error.response && error.response.status === 401) {
          this.logout();
        }
        return false;
      }
    },
    
    // Initialize auth state and headers when app starts
    initAuth() {
      if (this.token) {
        api.defaults.headers.common['Authorization'] = `Bearer ${this.token}`;
        
        // Optionally validate token on init
        this.validateToken().catch(() => {}); // Silently handle validation errors
      }
    }
  }
});