import { defineStore } from 'pinia';
import axios from 'axios';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null
  }),

  actions: {
    async login(email, password) {
      try {
        const res = await axios.post('http://localhost:8080/api/auth/login', {
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

        localStorage.setItem('user', JSON.stringify(this.user));
        localStorage.setItem('token', this.token);
      } catch (err) {
        throw new Error(
          "Login failed: " + (err.response?.data?.message || err.message)
        );
      }
    },

    logout() {
      this.user = null;
      this.token = null;
      localStorage.removeItem('user');
      localStorage.removeItem('token');
    }
  }
});