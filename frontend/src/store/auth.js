import { defineStore } from "pinia";
import { api } from "../api";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("user")) || null,
    token: localStorage.getItem("token") || null,
    lastLogin: localStorage.getItem("lastLogin") || null,
    isLoading: false,
    error: null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isEmployee: (state) => state.user?.role?.toLowerCase() === "employee",
    isCustomer: (state) => state.user?.role?.toLowerCase() === "customer",
    userRole: (state) => state.user?.role?.toLowerCase() || null,
    userName: (state) => state.user?.name || "Guest",
    userEmail: (state) => state.user?.email || "",
    userId: (state) => state.user?.id || null,
    formattedLastLogin: (state) => {
      if (!state.lastLogin) return "Not available";
      return new Date(state.lastLogin).toLocaleString();
    },
  },

  actions: {
    async login(email, password) {
      this.isLoading = true;
      this.error = null;

      try {
        const res = await api.post(`/auth/login`, {
          email,
          password,
        });

        this.user = {
          id: res.data.id,
          email: res.data.email,
          name: res.data.name,
          role: res.data.role,
        };

        this.token = res.data.token;
        this.lastLogin = new Date().toISOString();

        this.persistAuthState();

        api.defaults.headers.common["Authorization"] = `Bearer ${this.token}`;

        this.isLoading = false;
        return true;
      } catch (err) {
        this.isLoading = false;
        this.error = err.response?.data?.message || err.message;
        throw new Error("Login failed: " + this.error);
      }
    },

    logout() {
      this.user = null;
      this.token = null;
      this.error = null;

      localStorage.removeItem("user");
      localStorage.removeItem("token");
      localStorage.removeItem("lastLogin");

      delete api.defaults.headers.common["Authorization"];
    },

    persistAuthState() {
      localStorage.setItem("user", JSON.stringify(this.user));
      localStorage.setItem("token", this.token);
      localStorage.setItem("lastLogin", this.lastLogin);
    },

    updateUserInfo(userData) {
      if (!this.user) return;
      this.user = { ...this.user, ...userData };
      this.persistAuthState();
    },

    async validateToken() {
      if (!this.token) return false;

      try {
        const res = await api.get(`/auth/validate`, {
          headers: {
            Authorization: `Bearer ${this.token}`,
          },
        });

        if (res.data && res.data.valid) {
          if (!this.lastLogin) {
            this.lastLogin = new Date().toISOString();
            this.persistAuthState();
          }

          if (res.data.id || res.data.name || res.data.email || res.data.role) {
            this.updateUserInfo({
              id: res.data.id || this.user.id,
              name: res.data.name || this.user.name,
              email: res.data.email || this.user.email,
              role: res.data.role || this.user.role,
            });
          }
          return true;
        }

        return false;
      } catch (error) {
        if (error.response && error.response.status === 401) {
          this.logout();
        }
        return false;
      }
    },

    initAuth() {
      if (this.token) {
        api.defaults.headers.common["Authorization"] = `Bearer ${this.token}`;
        this.validateToken().catch(() => {});
      }
    },
  },
});
