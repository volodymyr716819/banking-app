import axios from "axios";
import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("user")) || null,
    token: localStorage.getItem("token") || null,
  }),
  actions: {
    async login(email, password) {
      try {
        const res = await axios.post("http://localhost:8080/api/auth/login", {
          email,
          password,
        });
        this.user = res.data;
        this.token = "mock-token"; // or real token if you add JWT later
        localStorage.setItem("user", JSON.stringify(this.user));
        localStorage.setItem("token", this.token);
      } catch (err) {
        throw new Error(
          "Login failed: " + err.response?.data?.message || err.message
        );
      }
    },

    async register(email, password) {
      try {
        const res = await axios.post(
          "http://localhost:8080/api/auth/register",
          {
            email,
            password,
          }
        );
        this.user = res.data;
        this.token = "mock-token";
        localStorage.setItem("user", JSON.stringify(this.user));
        localStorage.setItem("token", this.token);
      } catch (err) {
        throw new Error(
          "Register failed: " + err.response?.data?.message || err.message
        );
      }
    },

    logout() {
      this.user = null;
      this.token = null;
      localStorage.removeItem("user");
      localStorage.removeItem("token");
    },
  },
});
