import { defineStore } from "pinia";
import { api } from "../api";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("user")) || null,
    token: localStorage.getItem("token") || null,
    isLoading: false,
    error: null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isEmployee: (state) => {
      if (!state.user || !state.user.role) return false;
      const role = state.user.role.toLowerCase();
      console.log("Role check - Current role:", role);
      return role.includes("employee");
    },
    isCustomer: (state) => {
      if (!state.user || !state.user.role) return false;
      const role = state.user.role.toLowerCase();
      console.log("Role check - Current role:", role);
      return role.includes("customer");
    },
    userRole: (state) => {
      if (!state.user || !state.user.role) return null;
      return state.user.role.toLowerCase();
    },
    userName: (state) => state.user?.name || "Guest",
    userEmail: (state) => state.user?.email || "",
    userId: (state) => state.user?.id || null,
    isPending: (state) => state.user?.registrationStatus === "PENDING",
    isApproved: (state) => state.user?.registrationStatus === "APPROVED",
    registrationStatus: (state) => state.user?.registrationStatus || null,
  },

  actions: {
    async login(email, password) {
      this.isLoading = true;
      this.error = null;
      console.log("Starting login process for:", email);

      try {
        // Try both login endpoints to ensure one works
        let res;
        try {
          res = await api.post(`/auth/login`, {
            email,
            password,
          });
        } catch (primaryErr) {
          console.error("Primary login endpoint failed:", primaryErr);
          console.log("Trying alternate login endpoint...");
          res = await api.post(`/api/auth/login`, {
            email,
            password,
          });
        }

        console.log("Login successful, response data:", res.data);
        
        this.user = {
          id: res.data.id,
          email: res.data.email,
          name: res.data.name,
          role: res.data.role,
          registrationStatus: res.data.registrationStatus,
        };
        
        console.log("User object after login:", this.user);

        this.token = res.data.token;

        this.persistAuthState();

        // Set the Authorization header for all future requests
        api.defaults.headers.common["Authorization"] = `Bearer ${this.token}`;
        
        console.log("Auth token set:", this.token);

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

      // Make sure to clear the Authorization header
      delete api.defaults.headers.common["Authorization"];
      console.log("Auth token cleared");
    },

    persistAuthState() {
      localStorage.setItem("user", JSON.stringify(this.user));
      localStorage.setItem("token", this.token);
    },

    updateUserInfo(userData) {
      if (!this.user) return;
      this.user = { ...this.user, ...userData };
      this.persistAuthState();
    },

    async validateToken() {
      if (!this.token) return false;

      try {
        console.log("Validating token...");
        // Try adding /api prefix if the request fails
        let res;
        try {
          res = await api.get(`/auth/validate`, {
            headers: {
              Authorization: `Bearer ${this.token}`,
            },
          });
        } catch (primaryErr) {
          console.log("Primary validation endpoint failed, trying alternate...");
          res = await api.get(`/api/auth/validate`, {
            headers: {
              Authorization: `Bearer ${this.token}`,
            },
          });
        }

        if (res.data && res.data.valid) {
          if (res.data.id || res.data.name || res.data.email || res.data.role || res.data.registrationStatus) {
            this.updateUserInfo({
              id: res.data.id || this.user.id,
              name: res.data.name || this.user.name,
              email: res.data.email || this.user.email,
              role: res.data.role || this.user.role,
              registrationStatus: res.data.registrationStatus || this.user.registrationStatus,
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
        // Set the auth header for subsequent requests
        api.defaults.headers.common["Authorization"] = `Bearer ${this.token}`;
        console.log("Init auth with token:", this.token);
        
        // Validate the token and update user info
        this.validateToken()
          .then(valid => {
            if (!valid) {
              console.warn("Token validation failed, logging out");
              this.logout();
            }
          })
          .catch(err => {
            console.error("Token validation error:", err);
            this.logout();
          });
      } else {
        console.log("No token found during initAuth");
      }
    },
  },
});
