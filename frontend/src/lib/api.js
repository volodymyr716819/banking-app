// src/lib/api.js
import axios from "axios";

/**
 * One and only Axios instance for the whole app.
 * Reads the base URL from an environment variable so
 *   - dev → talks to localhost
 *   - prod → talks to Render
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
  withCredentials: false,        // keep false unless you use cookies
});

export default api;
