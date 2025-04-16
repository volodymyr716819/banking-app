import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: null,
    token: null,
  }),
  actions: {
    async login(email, password) {
      if (email === "test@bank.com" && password === "123456") {
        this.token = "mock-token";
        this.user = { email: email, role: "customer" };
      } else {
        throw new Error("Invalid credentials");
      }
    },
    async register(email, password) {
      // Simulate a success
      return new Promise((resolve) => {
        setTimeout(() => resolve(true), 500);
      });
    },
  },
});
