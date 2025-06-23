<template>
  <div class="register-page">
    <div class="auth-container">
      <div class="auth-header">
        <div class="bank-logo">
          <span class="material-icons logo-icon">account_balance</span>
          <h1 class="logo-text">BankApp</h1>
        </div>
        <h2 class="auth-title">Create Account</h2>
        <p class="auth-subtitle">Join our secure banking platform</p>
      </div>
      
      <form @submit.prevent="handleRegister" class="auth-form">
        <!-- Step indicator - shows registration process steps -->
        <div class="step-indicator">
          <div class="step active">
            <div class="step-number">1</div>
            <div class="step-label">Account</div>
          </div>
          <div class="step-connector"></div>
          <div class="step">
            <div class="step-number">2</div>
            <div class="step-label">Verification</div>
          </div>
          <div class="step-connector"></div>
          <div class="step">
            <div class="step-number">3</div>
            <div class="step-label">Approval</div>
          </div>
        </div>

        <!-- Name Field -->
        <div class="form-group">
          <label for="name">Full Name</label>
          <div class="input-wrapper">
            <span class="material-icons input-icon">person</span>
            <input 
              id="name"
              v-model="name" 
              type="text" 
              placeholder="Enter your full name" 
              required 
            />
          </div>
        </div>
        
        <!-- Email Field -->
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
        
        <!-- BSN Field -->
        <div class="form-group">
          <label for="bsn">BSN (Burgerservicenummer)</label>
          <div class="input-wrapper">
            <span class="material-icons input-icon">badge</span>
            <input 
              id="bsn"
              v-model="bsn" 
              type="text" 
              placeholder="Enter your BSN number" 
              required 
              pattern="[0-9]+"
              maxlength="9"
              @input="validateBsn"
            />
          </div>
          <div class="input-hint" v-if="bsn">
            <span v-if="isBsnValid" class="valid-hint">
              <span class="material-icons hint-icon">check_circle</span>
              Valid BSN format
            </span>
            <span v-else class="error-hint">
              <span class="material-icons hint-icon">error_outline</span>
              BSN must be 8-9 digits
            </span>
          </div>
        </div>
        
        <!-- Password Field -->
        <div class="form-group">
          <label for="password">Password</label>
          <div class="input-wrapper">
            <span class="material-icons input-icon">lock</span>
            <input 
              id="password"
              v-model="password" 
              :type="showPassword ? 'text' : 'password'"
              placeholder="Create a strong password" 
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
          <div class="password-strength" v-if="password">
            <div class="strength-meter">
              <div 
                class="strength-value" 
                :style="{ width: passwordStrength + '%' }"
                :class="strengthClass"
              ></div>
            </div>
            <div class="strength-text" :class="strengthClass">{{ strengthText }}</div>
          </div>
          <div class="password-requirements">
            <div class="requirement" :class="{ fulfilled: password.length >= 8 }">
              <span class="material-icons requirement-icon">
                {{ password.length >= 8 ? 'check_circle' : 'radio_button_unchecked' }}
              </span>
              <span>At least 8 characters</span>
            </div>
            <div class="requirement" :class="{ fulfilled: /[A-Z]/.test(password) }">
              <span class="material-icons requirement-icon">
                {{ /[A-Z]/.test(password) ? 'check_circle' : 'radio_button_unchecked' }}
              </span>
              <span>At least one uppercase letter</span>
            </div>
            <div class="requirement" :class="{ fulfilled: /[0-9]/.test(password) }">
              <span class="material-icons requirement-icon">
                {{ /[0-9]/.test(password) ? 'check_circle' : 'radio_button_unchecked' }}
              </span>
              <span>At least one number</span>
            </div>
          </div>
        </div>


        <!-- Error and Success Messages -->
        <div class="error-container" v-if="errorMessage">
          <div class="error-message">
            <span class="material-icons error-icon">error_outline</span>
            <span>{{ errorMessage }}</span>
          </div>
        </div>
        
        <div class="success-container" v-if="successMessage">
          <div class="success-message">
            <span class="material-icons success-icon">check_circle</span>
            <span>{{ successMessage }}</span>
          </div>
        </div>

        <!-- Submit Button -->
        <button 
          type="submit" 
          class="submit-button" 
          :disabled="isLoading || !isFormValid"
        >
          <span class="button-text" v-if="!isLoading">Create Account</span>
          <span class="material-icons button-icon loading" v-if="isLoading">autorenew</span>
          <span class="material-icons button-icon" v-else>arrow_forward</span>
        </button>
      </form>

      <div class="auth-footer">
        <p class="login-prompt">
          Already have an account?
          <router-link to="/login" class="login-link">Sign In</router-link>
        </p>
        <div class="security-info">
          <span class="material-icons security-icon">security</span>
          <span class="security-text">Your information is protected</span>
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
import { ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import api from '../lib/api';

const name = ref('');
const email = ref('');
const password = ref('');
const bsn = ref('');
const isBsnValid = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const showPassword = ref(false);
const isLoading = ref(false);
const router = useRouter();

// Password strength calculation
const passwordStrength = computed(() => {
  if (!password.value) return 0;
  
  let strength = 0;
  
  // Length check
  if (password.value.length >= 8) strength += 25;
  
  // Uppercase letter check
  if (/[A-Z]/.test(password.value)) strength += 25;
  
  // Number check
  if (/[0-9]/.test(password.value)) strength += 25;
  
  // Special character check
  if (/[^A-Za-z0-9]/.test(password.value)) strength += 25;
  
  return strength;
});

// Strength class based on calculated strength
const strengthClass = computed(() => {
  if (passwordStrength.value < 50) return 'weak';
  if (passwordStrength.value < 75) return 'medium';
  return 'strong';
});

// Text description of strength
const strengthText = computed(() => {
  if (passwordStrength.value < 50) return 'Weak';
  if (passwordStrength.value < 75) return 'Medium';
  return 'Strong';
});

// BSN validation
function validateBsn() {
  // Remove any non-numeric characters
  bsn.value = bsn.value.replace(/\D/g, '');
  
  // Check if BSN is a valid format (8-9 digits)
  isBsnValid.value = /^\d{8,9}$/.test(bsn.value);
}

// Check if form is valid for submission
const isFormValid = computed(() => {
  return (
    name.value.trim() !== '' &&
    email.value.includes('@') &&
    password.value.length >= 8 &&
    isBsnValid.value
  );
});

async function handleRegister() {
  errorMessage.value = '';
  successMessage.value = '';
  isLoading.value = true;

  // Email validation
  if (!email.value.includes('@') || !email.value.includes('.')) {
    errorMessage.value = 'Please enter a valid email address';
    isLoading.value = false;
    return;
  }

  // Password validation
  if (password.value.length < 8) {
    errorMessage.value = 'Password must be at least 8 characters long';
    isLoading.value = false;
    return;
  }

  // Name validation
  if (name.value.trim().length < 3) {
    errorMessage.value = 'Please enter your full name';
    isLoading.value = false;
    return;
  }
  
  // BSN validation
  if (!isBsnValid.value) {
    errorMessage.value = 'Please enter a valid BSN (8-9 digits)';
    isLoading.value = false;
    return;
  }

  try {
    const res = await api.post('/auth/register', {
      name: name.value,
      email: email.value,
      password: password.value,
      bsn: bsn.value
    });

    successMessage.value = res.data.message || "Registration successful. Please wait for approval.";
    
    // Redirect after a delay
    setTimeout(() => router.push('/login'), 3000);
  } catch (err) {
    if (err.response?.status === 409) {
      errorMessage.value = 'This email is already registered. Please use a different email address.';
    } else {
      errorMessage.value = err.response?.data?.message || 'Registration failed. Please try again later.';
    }
    isLoading.value = false;
  }
}
</script>

<style scoped>
.register-page {
    display: flex;
    min-height: 100vh;
    background-color: var(--gray-50);
    position: relative;
    overflow: hidden;
}

.auth-container {
    flex: 5;
    max-width: 850px;
    padding: var(--spacing-8);
    background-color: var(--white);
    box-shadow: var(--shadow-lg);
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    max-height: 100vh;
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
    background-image: url('../assets/pattern.svg');
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

/* Step indicator styling */
.step-indicator {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--spacing-6);
    padding: var(--spacing-4) var(--spacing-2);
    background-color: var(--gray-50);
    border-radius: var(--border-radius);
}

.step {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--spacing-1);
    width: 70px;
}

.step-number {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background-color: var(--gray-300);
    color: var(--gray-600);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: var(--font-weight-medium);
    font-size: var(--font-size-sm);
}

.step.active .step-number {
    background-color: var(--primary-color);
    color: var(--white);
}

.step-label {
    font-size: var(--font-size-xs);
    color: var(--gray-600);
    text-align: center;
}

.step.active .step-label {
    color: var(--primary-color);
    font-weight: var(--font-weight-medium);
}

.step-connector {
    flex-grow: 1;
    height: 2px;
    background-color: var(--gray-300);
    margin: 0 var(--spacing-1);
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-2);
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

/* Password strength meter */
.password-strength {
    margin-top: var(--spacing-2);
}

.strength-meter {
    height: 4px;
    background-color: var(--gray-200);
    border-radius: var(--border-radius-full);
    margin-bottom: var(--spacing-1);
    overflow: hidden;
}

.strength-value {
    height: 100%;
    transition: width 0.3s ease, background-color 0.3s ease;
}

.strength-value.weak {
    background-color: var(--error-color);
}

.strength-value.medium {
    background-color: var(--warning-color);
}

.strength-value.strong {
    background-color: var(--success-color);
}

.strength-text {
    font-size: var(--font-size-xs);
    text-align: right;
}

.strength-text.weak {
    color: var(--error-color);
}

.strength-text.medium {
    color: var(--warning-color);
}

.strength-text.strong {
    color: var(--success-color);
}

/* Password requirements */
.password-requirements {
    margin-top: var(--spacing-2);
    display: flex;
    flex-direction: column;
    gap: var(--spacing-1);
}

.requirement {
    display: flex;
    align-items: center;
    gap: var(--spacing-2);
    font-size: var(--font-size-xs);
    color: var(--gray-600);
}

.requirement.fulfilled {
    color: var(--success-color);
}

.requirement-icon {
    font-size: 14px;
}

.requirement.fulfilled .requirement-icon {
    color: var(--success-color);
}

/* Input hints for BSN */
.input-hint {
    display: flex;
    margin-top: var(--spacing-1);
    font-size: var(--font-size-xs);
}

.valid-hint {
    display: flex;
    align-items: center;
    gap: var(--spacing-1);
    color: var(--success-color);
}

.error-hint {
    display: flex;
    align-items: center;
    gap: var(--spacing-1);
    color: var(--error-color);
}

.hint-icon {
    font-size: 14px;
}

/* Terms and conditions */
.terms-group {
    margin-top: var(--spacing-1);
}

.terms-label {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-2);
    font-size: var(--font-size-sm);
    color: var(--gray-700);
    line-height: 1.4;
}

.terms-label input[type="checkbox"] {
    margin-top: 2px;
    flex-shrink: 0;
    width: 16px;
    height: 16px;
    accent-color: var(--primary-color);
}

.terms-link {
    color: var(--primary-color);
    text-decoration: none;
}

.terms-link:hover {
    text-decoration: underline;
}

/* Error and success messages */
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

.success-container {
    margin-top: var(--spacing-2);
}

.success-message {
    display: flex;
    align-items: center;
    gap: var(--spacing-2);
    padding: var(--spacing-3);
    background-color: rgba(0, 132, 90, 0.1);
    color: var(--success-color);
    border-radius: var(--border-radius);
    font-size: var(--font-size-sm);
}

.success-icon {
    font-size: 18px;
}

/* Submit button */
.submit-button {
    margin-top: var(--spacing-4);
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

.submit-button:hover:not(:disabled) {
    opacity: 0.9;
}

.submit-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.button-text {
    font-size: var(--font-size-base);
}

.button-icon {
    font-size: 18px;
}

.button-icon.loading {
    animation: spin 1s linear infinite;
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}

/* Auth footer */
.auth-footer {
    margin-top: var(--spacing-8);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--spacing-4);
}

.login-prompt {
    color: var(--gray-600);
    font-size: var(--font-size-sm);
    margin: 0;
}

.login-link {
    color: var(--primary-color);
    font-weight: var(--font-weight-medium);
    text-decoration: none;
    margin-left: var(--spacing-1);
}

.login-link:hover {
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