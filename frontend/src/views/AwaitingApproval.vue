<template>
  <div class="approval-page">
    <div class="approval-container">
      <div class="bank-logo">
        <span class="material-icons logo-icon">account_balance</span>
        <h1 class="logo-text">BankApp</h1>
      </div>
      
      <div class="approval-content">
        <div class="status-icon">
          <span class="material-icons">hourglass_top</span>
        </div>
        
        <h2 class="approval-title">Welcome, {{ authStore.user?.name || 'User' }}!</h2>
        
        <h3 class="approval-subtitle">Account Pending Approval</h3>
        
        <div class="approval-message">
          <p>Thank you for registering with BankApp!</p>
          <p>Your account is currently pending approval by our team.</p>
          <p>You will gain full access to all banking services once your account is approved.</p>
        </div>
        
        <div class="approval-steps">
          <div class="step completed">
            <div class="step-number">1</div>
            <div class="step-content">
              <div class="step-title">Registration Completed</div>
              <div class="step-description">You have successfully registered your account</div>
            </div>
          </div>
          
          <div class="step active">
            <div class="step-number">2</div>
            <div class="step-content">
              <div class="step-title">Approval in Progress</div>
              <div class="step-description">Our team is reviewing your information</div>
            </div>
          </div>
          
          <div class="step">
            <div class="step-number">3</div>
            <div class="step-content">
              <div class="step-title">Account Activation</div>
              <div class="step-description">Once approved, you'll have full access to your account</div>
            </div>
          </div>
        </div>
        
        <div class="actions">
          <button @click="logout" class="logout-button">
            <span class="material-icons">logout</span>
            Log Out
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
  
<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';

const router = useRouter();
const authStore = useAuthStore();

function logout() {
  // Use the auth store's logout method
  authStore.logout();
  // Redirect back to login
  router.replace('/login');
}
</script>
  
<style scoped>
.approval-page {
  display: flex;
  min-height: 100vh;
  background-color: var(--gray-50);
  justify-content: center;
  align-items: center;
  padding: var(--spacing-4);
}

.approval-container {
  max-width: 600px;
  width: 100%;
  background-color: var(--white);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--spacing-8);
  text-align: center;
}

.bank-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-6);
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

.status-icon {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-4);
}

.status-icon .material-icons {
  font-size: 64px;
  color: var(--warning-color);
  animation: pulse 2s infinite ease-in-out;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
  100% { transform: scale(1); opacity: 1; }
}

.approval-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--primary-color);
  margin-bottom: var(--spacing-2);
}

.approval-subtitle {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  color: var(--gray-700);
  margin-bottom: var(--spacing-4);
}

.approval-message {
  margin-bottom: var(--spacing-6);
}

.approval-message p {
  margin-bottom: var(--spacing-2);
  color: var(--gray-700);
}

.approval-steps {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-6);
  padding: var(--spacing-4);
  background-color: var(--gray-50);
  border-radius: var(--border-radius);
}

.step {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-3);
  text-align: left;
  padding: var(--spacing-2);
  border-radius: var(--border-radius);
}

.step.active {
  background-color: rgba(25, 118, 210, 0.1);
}

.step-number {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: var(--gray-300);
  color: var(--gray-700);
  font-weight: var(--font-weight-medium);
  flex-shrink: 0;
}

.step.completed .step-number {
  background-color: var(--success-color);
  color: var(--white);
}

.step.active .step-number {
  background-color: var(--primary-color);
  color: var(--white);
}

.step-content {
  flex-grow: 1;
}

.step-title {
  font-weight: var(--font-weight-medium);
  color: var(--gray-900);
  margin-bottom: var(--spacing-1);
}

.step-description {
  font-size: var(--font-size-sm);
  color: var(--gray-600);
}

.step.completed .step-title {
  color: var(--success-color);
}

.step.active .step-title {
  color: var(--primary-color);
}

.actions {
  display: flex;
  justify-content: center;
}

.logout-button {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-4);
  border: none;
  background-color: var(--white);
  color: var(--gray-700);
  font-weight: var(--font-weight-medium);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: background-color var(--transition-fast);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--gray-300);
}

.logout-button:hover {
  background-color: var(--gray-100);
}

@media (max-width: 640px) {
  .approval-container {
    padding: var(--spacing-4);
  }
  
  .approval-steps {
    padding: var(--spacing-3);
  }
}
</style>  