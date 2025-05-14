<template>
    <div class="register-container">
      <h1>Register</h1>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <input v-model="name" type="text" placeholder="Full Name" required />
        </div>
        <div class="form-group">
          <input v-model="email" type="email" placeholder="Email" required />
        </div>
        <div class="form-group">
          <input v-model="password" type="password" placeholder="Password" required />
        </div>
        <button type="submit" class="submit-button">Register</button>
      </form>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <p class="nav-link">Already have an account? <router-link to="/login">Login here</router-link></p>
    </div>
  </template>
  
  <script setup>
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { useAuthStore } from '../store/auth';
  import axios from 'axios';
  
  const name = ref('');
  const email = ref('');
  const password = ref('');
  const errorMessage = ref('');
  const auth = useAuthStore();
  const router = useRouter();
  
  async function handleRegister() {
    try {
      const res = await axios.post('http://localhost:8080/api/auth/register', {
        name: name.value,
        email: email.value,
        password: password.value
      });
  
      auth.user = {
        id: res.data.id,
        email: res.data.email,
        name: res.data.name,
        role: res.data.role
      };
      auth.token = res.data.token;
  
      localStorage.setItem('user', JSON.stringify(auth.user));
      localStorage.setItem('token', auth.token);
  
      router.push('/dashboard');
    } catch (err) {
      errorMessage.value = err.response?.data || 'Registration failed';
    }
  }
  </script>
  
  <style scoped>
  .register-container {
    max-width: 400px;
    margin: 5rem auto;
    padding: 2rem;
    background-color: #f8f8f8;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }
  
  h1 {
    font-size: 1.8rem;
    text-align: center;
    margin-bottom: 1.5rem;
    font-weight: bold;
    color: #111;
  }
  
  .form-group {
    margin-bottom: 1rem;
  }
  
  input {
    width: 100%;
    padding: 0.75rem;
    font-size: 1rem;
    border: 1px solid #ccc;
    border-radius: 8px;
  }
  
  .submit-button {
    width: 100%;
    padding: 0.75rem;
    background-color: #2c3e50;
    color: white;
    font-size: 1rem;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    margin-top: 0.5rem;
  }
  
  .submit-button:hover {
    background-color: #1f2e3a;
  }
  
  .error-message {
    color: red;
    text-align: center;
    margin-top: 1rem;
  }
  
  .nav-link {
    text-align: center;
    margin-top: 1.25rem;
    color: #222;
  }
  
  .nav-link a {
    color: purple;
    text-decoration: underline;
  }
  </style>  