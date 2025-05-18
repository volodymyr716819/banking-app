<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="auth-header">
        <h1 class="auth-title">Create Account</h1>
        <p class="auth-subtitle">Sign up to start using our banking services</p>
      </div>
      
      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="form-group">
          <label for="name">Full Name</label>
          <input 
            v-model="name" 
            type="text" 
            id="name" 
            class="form-control" 
            placeholder="Enter your full name" 
            autocomplete="name"
            required 
          />
        </div>
        
        <div class="form-group">
          <label for="email">Email Address</label>
          <input 
            v-model="email" 
            type="email" 
            id="email" 
            class="form-control" 
            placeholder="Enter your email" 
            autocomplete="email"
            required 
          />
        </div>
        
        <div class="form-group">
          <label for="password">Password</label>
          <div class="password-field">
            <input 
              v-model="password" 
              :type="showPassword ? 'text' : 'password'" 
              id="password" 
              class="form-control" 
              placeholder="Create a password"
              autocomplete="new-password"
              required 
            />
            <button 
              type="button" 
              class="password-toggle" 
              @click="showPassword = !showPassword"
              tabindex="-1"
            >
              {{ showPassword ? "Hide" : "Show" }}
            </button>
          </div>
        </div>

        <div v-if="successMessage" class="auth-success">
          <div class="success-icon">✓</div>
          <div>{{ successMessage }}</div>
        </div>

        <div v-if="errorMessage" class="auth-error">
          <div class="error-icon">⚠️</div>
          <div>{{ errorMessage }}</div>
        </div>

        <button type="submit" class="auth-button" :disabled="isLoading">
          <span v-if="isLoading" class="spinner"></span>
          <span v-else>Create Account</span>
        </button>

        <div class="auth-footer">
          <p>
            Already have an account?
            <router-link to="/login" class="auth-link">Sign in here</router-link>
          </p>
        </div>
      </form>
    </div>
    
    <div class="auth-banner">
      <div class="banner-content">
        <h2>Bank</h2>
        <p>Create your account in seconds and get started with digital banking</p>
        <ul class="feature-list">
          <li>Free account setup</li>
          <li>24/7 online access</li>
        
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const name = ref('');
const email = ref('');
const password = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const isLoading = ref(false);
const showPassword = ref(false);
const router = useRouter();

async function handleRegister() {
  errorMessage.value = '';
  successMessage.value = '';
  isLoading.value = true;

  try {
    const res = await axios.post('http://localhost:8080/api/auth/register', {
      name: name.value,
      email: email.value,
      password: password.value
    });

    successMessage.value = res.data.message || "Registration successful. Please wait for approval.";
    setTimeout(() => router.push('/login'), 2000);
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Registration failed';
    isLoading.value = false;
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
  background-color: var(--ultra-light-gray);
}

.auth-container {
  flex: 1;
  max-width: 480px;
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-banner {
  flex: 1;
  background: linear-gradient(135deg, var(--primary-dark), var(--primary-color));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  padding: 60px;
}

.banner-content {
  max-width: 400px;
}

.banner-content h2 {
  font-size: 2.2rem;
  margin-bottom: 20px;
  font-weight: 700;
}

.banner-content p {
  font-size: 1.1rem;
  margin-bottom: 30px;
  opacity: 0.9;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  padding: 12px 0;
  position: relative;
  padding-left: 30px;
  font-size: 1.05rem;
}

.feature-list li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: var(--secondary-color);
  font-weight: bold;
}

.auth-header {
  margin-bottom: 40px;
}

.auth-title {
  font-size: 2.25rem;
  font-weight: 700;
  color: var(--primary-dark);
  margin-bottom: 10px;
}

.auth-subtitle {
  color: var(--text-secondary);
  font-size: 1.1rem;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 500;
  color: var(--text-secondary);
}

.password-field {
  position: relative;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0;
}

.password-toggle:hover {
  color: var(--primary-color);
}

.form-control {
  padding: 14px 16px;
  border-radius: var(--border-radius);
  border: 1px solid var(--light-gray);
  background-color: white;
  font-size: 1rem;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.form-control:focus {
  border-color: var(--primary-color);
  outline: none;
  box-shadow: 0 0 0 3px rgba(30, 136, 229, 0.2);
}

.auth-button {
  padding: 14px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
}

.auth-button:not(:disabled):hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
}

.auth-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-error {
  background-color: #fdf2f2;
  color: #c53030;
  padding: 12px 16px;
  border-radius: var(--border-radius);
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.9rem;
}

.auth-success {
  background-color: #f0fff4;
  color: #2f855a;
  padding: 12px 16px;
  border-radius: var(--border-radius);
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.9rem;
}

.error-icon, .success-icon {
  font-size: 1.2rem;
}

.auth-footer {
  text-align: center;
  margin-top: 20px;
  color: var(--text-secondary);
}

.auth-link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s;
}

.auth-link:hover {
  color: var(--primary-dark);
  text-decoration: underline;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 992px) {
  .auth-page {
    flex-direction: column-reverse;
  }
  
  .auth-container {
    max-width: 100%;
    padding: 40px 20px;
  }
  
  .auth-banner {
    padding: 40px 20px;
  }
}
</style>