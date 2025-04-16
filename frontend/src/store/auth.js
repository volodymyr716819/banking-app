import { defineStore } from "pinia"

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("user")) || null,
    token: localStorage.getItem("token") || null,
    users: JSON.parse(localStorage.getItem("users")) || [
      { email: "admin@gmail.com", password: "secret123", role: "admin" },
      { email: "user@gmail.com", password: "user123", role: "customer" },
    ],
  }),
  actions: {
    async login(email, password) {
      const match = this.users.find(
        (user) => user.email === email && user.password === password
      )

      if (match) {
        this.token = "mock-token"
        this.user = { email: match.email, role: match.role }

        // Persist login
        localStorage.setItem("user", JSON.stringify(this.user))
        localStorage.setItem("token", this.token)
      } else {
        throw new Error("Login or password is invalid.")
      }
    },

    async register(email, password) {
      const exists = this.users.find((user) => user.email === email)
      if (exists) {
        throw new Error("Email is already registered")
      }

      const newUser = { email, password, role: "customer" }
      this.users.push(newUser)

      // Save users to localStorage
      localStorage.setItem("users", JSON.stringify(this.users))

      // Auto login
      this.user = { email, role: "customer" }
      this.token = "mock-token"
      localStorage.setItem("user", JSON.stringify(this.user))
      localStorage.setItem("token", this.token)
    },

    logout() {
      this.user = null
      this.token = null
      localStorage.removeItem("user")
      localStorage.removeItem("token")
    },
  },
})
