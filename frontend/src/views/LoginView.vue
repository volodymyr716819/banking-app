<template>
    <div class="login-page">
        <div class="auth-container">
            <div class="auth-header">
                <div class="bank-logo">
                    <span class="material-icons logo-icon">account_balance</span>
                    <h1 class="logo-text">BankApp</h1>
                </div>
                <h2 class="auth-title">Welcome Back</h2>
                <p class="auth-subtitle">Sign in to access your accounts</p>
            </div>
            
            <form @submit.prevent="handleLogin" class="auth-form">
                <div class="form-group">
                    <label for="email">Email</label>
                    <div class="input-wrapper">
                        <span class="material-icons input-icon">email</span>
                        <input 
                            id="email"
                            v-model="email" 
                            type="email" 
                            placeholder="Enter your email" 
                            required 
                        />
                    </div>
                </div>
                
                <div class="form-group">
                    <div class="label-group">
                        <label for="password">Password</label>
                    </div>
                    <div class="input-wrapper">
                        <span class="material-icons input-icon">lock</span>
                        <input 
                            id="password"
                            v-model="password" 
                            :type="showPassword ? 'text' : 'password'"
                            placeholder="Enter your password" 
                            required 
                        />
                        <button 
                            type="button" 
                            class="toggle-password" 
                            @click="showPassword = !showPassword"
                        >
                            <span class="material-icons">
                                {{ showPassword ? 'visibility_off' : 'visibility' }}
                            </span>
                        </button>
                    </div>
                </div>

              
                <div class="error-container" v-if="errorMessage">
                    <div class="error-message">
                        <span class="material-icons error-icon">error_outline</span>
                        <span>{{ errorMessage }}</span>
                    </div>
                </div>

                <button type="submit" class="submit-button">
                    <span class="button-text">Sign In</span>
                    <span class="material-icons button-icon">arrow_forward</span>
                </button>
            </form>

            <div class="auth-footer">
                <p class="register-prompt">
                    Don't have an account?
                    <router-link to="/register" class="register-link">Create Account</router-link>
                </p>
                <div class="security-info">
                    <span class="material-icons security-icon">security</span>
                    <span class="security-text">Your information is secured</span>
                </div>
            </div>
        </div>
        
        <div class="auth-graphics">
            <div class="bank-name">
                <span class="material-icons bank-logo-icon">account_balance</span>
                <span class="bank-name-text">BankApp</span>
            </div>
            <div class="auth-decoration"></div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const email = ref('')
const password = ref('')
const errorMessage = ref('')
const showPassword = ref(false)
const isLoading = ref(false)

const authStore = useAuthStore()
const router = useRouter()

async function handleLogin() {
  errorMessage.value = ''
  isLoading.value = true

  // basic validation
  if (!email.value || !password.value) {
    errorMessage.value = 'Please fill in all fields'
    isLoading.value = false
    return
  }
  if (!email.value.includes('@')) {
    errorMessage.value = 'Please enter a valid email address'
    isLoading.value = false
    return
  }

  try {
    // perform login, store JWT in authStore.token
    await authStore.login(email.value, password.value)

    // Use the registration status from the user object
    if (authStore.user.registrationStatus === 'PENDING') {
      router.replace('/awaiting-approval')
    } else {
      router.replace('/dashboard')
    }
  } catch (err) {
    // show error for invalid creds or pending (fallback)
    if (err.message.toLowerCase().includes('pending approval')) {
      errorMessage.value = 'Your account is pending approval. Please wait for an employee to approve you.'
    } else {
      errorMessage.value = 'Login failed: Invalid email or password.'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
    display: flex;
    min-height: 100vh;
    background-color: var(--gray-50);
    position: relative;
    overflow: hidden;
}

.auth-container {
    flex: 5;
    max-width: 800px;
    padding: var(--spacing-8);
    background-color: var(--white);
    box-shadow: var(--shadow-lg);
    display: flex;
    flex-direction: column;
    z-index: 1;
}


.auth-graphics {
    flex: 1;
    background: var(--gradient-primary);
    position: relative;
    overflow: hidden;
    display: none; /* Hide on mobile */
    min-width: 200px;
    z-index: 0;
}


.bank-name {
    position: absolute;
    top: 20%;
    left: 0;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--white);
    text-align: center;
    z-index: 1;
}

.bank-logo-icon {
    font-size: 80px;
    margin-bottom: var(--spacing-4);
}

.bank-name-text {
    font-size: 54px;
    font-weight: var(--font-weight-bold);
}

.auth-decoration {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='%23FFFFFF' fill-opacity='0.05' fill-rule='evenodd'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/svg%3E");
}

.auth-decoration::after {
    content: "";
    position: absolute;
    top: -100%;
    left: -100%;
    height: 300%;
    width: 300%;
    background: linear-gradient(
        to right,
        rgba(255, 255, 255, 0) 0%,
        rgba(255, 255, 255, 0.1) 50%,
        rgba(255, 255, 255, 0) 100%
    );
    transform: rotate(45deg);
    animation: shimmer 4s infinite;
}

@keyframes shimmer {
    0% {
        transform: translateX(-50%) rotate(45deg);
    }
    100% {
        transform: translateX(150%) rotate(45deg);
    }
}

.auth-header {
    margin-bottom: var(--spacing-6);
    text-align: center;
}

.bank-logo {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: var(--spacing-4);
}

.logo-icon {
    font-size: 32px;
    color: var(--primary-color);
    margin-right: var(--spacing-2);
}

.logo-text {
    font-size: var(--font-size-2xl);
    font-weight: var(--font-weight-bold);
    color: var(--primary-color);
    margin: 0;
}

.auth-title {
    font-size: var(--font-size-2xl);
    font-weight: var(--font-weight-semibold);
    color: var(--gray-900);
    margin: 0 0 var(--spacing-2) 0;
}

.auth-subtitle {
    color: var(--gray-600);
    font-size: var(--font-size-base);
    margin: 0;
}

.auth-form {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-4);
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-2);
}

.remember-group {
    margin-top: -8px;
}

.label-group {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.form-group label {
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-medium);
    color: var(--gray-700);
}

.input-wrapper {
    position: relative;
    display: flex;
    align-items: center;
}

.input-icon {
    position: absolute;
    left: var(--spacing-3);
    color: var(--gray-500);
    font-size: 20px;
}

.input-wrapper input {
    width: 100%;
    padding: var(--spacing-3) var(--spacing-3) var(--spacing-3) var(--spacing-8);
    border: 1px solid var(--gray-300);
    border-radius: var(--border-radius);
    font-size: var(--font-size-base);
    transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.input-wrapper input:focus {
    outline: none;
    border-color: var(--primary-light);
    box-shadow: 0 0 0 3px rgba(0, 59, 112, 0.1);
}

.toggle-password {
    position: absolute;
    right: var(--spacing-3);
    background: transparent;
    border: none;
    color: var(--gray-500);
    cursor: pointer;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}

.toggle-password:hover {
    color: var(--gray-700);
}

.remember-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-2);
    font-size: var(--font-size-sm);
    color: var(--gray-700);
    cursor: pointer;
}

.remember-label input[type="checkbox"] {
    margin: 0;
    cursor: pointer;
    width: 16px;
    height: 16px;
    accent-color: var(--primary-color);
}

.forgot-password {
    font-size: var(--font-size-sm);
    color: var(--primary-color);
    text-decoration: none;
}

.forgot-password:hover {
    text-decoration: underline;
}

.error-container {
    margin-top: var(--spacing-2);
}

.error-message {
    display: flex;
    align-items: center;
    gap: var(--spacing-2);
    padding: var(--spacing-3);
    background-color: rgba(208, 52, 56, 0.1);
    color: var(--error-color);
    border-radius: var(--border-radius);
    font-size: var(--font-size-sm);
}

.error-icon {
    font-size: 18px;
}

.submit-button {
    margin-top: var(--spacing-2);
    padding: var(--spacing-3);
    background: var(--gradient-primary);
    color: var(--white);
    border: none;
    border-radius: var(--border-radius);
    font-weight: var(--font-weight-medium);
    cursor: pointer;
    transition: opacity var(--transition-fast);
    display: flex;
    justify-content: center;
    align-items: center;
    gap: var(--spacing-2);
}

.submit-button:hover {
    opacity: 0.9;
}

.button-text {
    font-size: var(--font-size-base);
}

.button-icon {
    font-size: 18px;
}

.auth-footer {
    margin-top: var(--spacing-8);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--spacing-4);
}

.register-prompt {
    color: var(--gray-600);
    font-size: var(--font-size-sm);
    margin: 0;
}

.register-link {
    color: var(--primary-color);
    font-weight: var(--font-weight-medium);
    text-decoration: none;
    margin-left: var(--spacing-1);
}

.register-link:hover {
    text-decoration: underline;
}

.security-info {
    display: flex;
    align-items: center;
    gap: var(--spacing-2);
    font-size: var(--font-size-xs);
    color: var(--gray-500);
}

.security-icon {
    font-size: 16px;
    color: var(--secondary-color);
}

/* Responsive styles */
@media (min-width: 768px) {
    .auth-graphics {
        display: block;
    }
}
</style>