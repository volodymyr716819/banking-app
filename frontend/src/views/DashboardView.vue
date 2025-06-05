<template>
  <div class="dashboard-welcome">
    <div class="welcome-card">
      <div class="welcome-header">
        <h1 class="welcome-title">Welcome, {{ auth.user?.name || 'Guest' }}</h1>
        <p class="welcome-subtitle">
          You are now logged into your personal banking dashboard.
        </p>
        <div class="welcome-stats">
          <div class="welcome-stat">
            <span class="material-icons">account_balance</span>
            <div class="welcome-stat-content">
              <div class="welcome-stat-value">{{accounts.length || '0'}}</div>
              <div class="welcome-stat-label">Accounts</div>
            </div>
          </div>
          <div class="welcome-stat">
            <span class="material-icons">today</span>
            <div class="welcome-stat-content">
              <div class="welcome-stat-value">{{formatDate(new Date())}}</div>
              <div class="welcome-stat-label">Last Login</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="dashboard-grid">
    <!-- User Profile Card -->
    <div class="dashboard-card user-profile">
      <div class="card-header">
        <h2 class="card-title">Your Profile</h2>
      </div>
      <div class="card-body">
        <UserProfileComponent :user="auth.user" size="large" class="profile-component" />
        <div class="profile-details">
          <div class="profile-row">
            <span class="profile-label">Name:</span>
            <span class="profile-value">{{ auth.userName }}</span>
          </div>
          <div class="profile-row">
            <span class="profile-label">Email:</span>
            <span class="profile-value">{{ auth.userEmail }}</span>
          </div>
          <div class="profile-row">
            <span class="profile-label">Role:</span>
            <span class="profile-value">{{ formatRole(auth.user?.role) }}</span>
          </div>
          <div class="profile-row">
            <span class="profile-label">User ID:</span>
            <span class="profile-value">{{ auth.userId || 'Not available' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions Card -->
    <div class="dashboard-card quick-actions">
      <div class="card-header">
        <h2 class="card-title">Quick Actions</h2>
      </div>
      <div class="card-body">
        <div class="action-buttons">
          <router-link to="/dashboard/accounts" class="quick-action accounts">
            <div class="quick-action__icon">
              <span class="material-icons">account_balance</span>
            </div>
            <span class="quick-action__label">View Accounts</span>
          </router-link>
          <router-link to="/dashboard/transfer" class="quick-action transfer">
            <div class="quick-action__icon">
              <span class="material-icons">swap_horiz</span>
            </div>
            <span class="quick-action__label">Transfer Money</span>
          </router-link>
          <router-link to="/dashboard/history" class="quick-action history">
            <div class="quick-action__icon">
              <span class="material-icons">history</span>
            </div>
            <span class="quick-action__label">Transaction History</span>
          </router-link>
          <router-link to="/dashboard/atm" class="quick-action atm">
            <div class="quick-action__icon">
              <span class="material-icons">atm</span>
            </div>
            <span class="quick-action__label">ATM Operations</span>
          </router-link>
        </div>
      </div>
      <div class="security-badge">
        <span class="material-icons">security</span>
        <span>Transactions secured</span>
      </div>
    </div>

    <!-- Employee-only Quick Access Card -->
    <div v-if="auth.isEmployee" class="dashboard-card employee-tools">
      <div class="card-header">
        <h2 class="card-title">Employee Tools</h2>
      </div>
      <div class="card-body">
        <div class="action-buttons">
          <router-link to="/dashboard/approve" class="quick-action approve">
            <div class="quick-action__icon">
              <span class="material-icons">check_circle</span>
            </div>
            <span class="quick-action__label">Approve Accounts</span>
          </router-link>
          <router-link to="/dashboard/approve-users" class="quick-action users">
            <div class="quick-action__icon">
              <span class="material-icons">how_to_reg</span>
            </div>
            <span class="quick-action__label">Approve Users</span>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from "../store/auth";
import { useRouter } from "vue-router";
import { defineComponent, ref, onMounted } from 'vue';
import UserProfileComponent from '../components/UserProfileComponent.vue';
import axios from 'axios';

const router = useRouter();
const auth = useAuthStore();
const accounts = ref([]);

// Format user role for display
const formatRole = (role) => {
  if (!role) return 'Guest';
  return role.charAt(0).toUpperCase() + role.slice(1).toLowerCase();
};

// Format date to a readable format
const formatDate = (date) => {
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
};

// Get user initials for avatar
const getUserInitials = () => {
  if (!auth.user?.name) return 'U';
  
  const nameParts = auth.user.name.split(' ');
  if (nameParts.length === 1) {
    return nameParts[0].charAt(0).toUpperCase();
  }
  
  return (
    nameParts[0].charAt(0).toUpperCase() + 
    nameParts[nameParts.length - 1].charAt(0).toUpperCase()
  );
};

// Fetch user accounts for the welcome stats
onMounted(async () => {
  if (auth.user && auth.user.id) {
    try {
      const response = await axios.get(`http://localhost:8080/api/accounts/user/${auth.user.id}`, {
        headers: {
          Authorization: `Bearer ${auth.token}`
        }
      });
      accounts.value = response.data;
    } catch (error) {
      console.error("Failed to fetch accounts:", error);
    }
  }
});

// Define the page title for the component
const __pageTitle = 'Dashboard';

// Expose the page title to the parent component
defineExpose({ __pageTitle });
</script>

<style scoped>
.dashboard-welcome {
  margin-bottom: var(--spacing-6);
}

.welcome-stats {
  display: flex;
  gap: var(--spacing-4);
  margin-top: var(--spacing-5);
}

.welcome-stat {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--border-radius);
  padding: var(--spacing-3);
  gap: var(--spacing-3);
  flex: 1;
}

.welcome-stat .material-icons {
  font-size: 24px;
}

.welcome-stat-content {
  display: flex;
  flex-direction: column;
}

.welcome-stat-value {
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-lg);
}

.welcome-stat-label {
  font-size: var(--font-size-sm);
  opacity: 0.8;
}

.welcome-card {
  background: var(--gradient-primary);
  color: var(--white);
  border-radius: var(--border-radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-md);
  position: relative;
  overflow: hidden;
}

.welcome-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='%23FFFFFF' fill-opacity='0.05' fill-rule='evenodd'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.7;
}

.welcome-card::after {
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

.welcome-title {
  font-size: var(--font-size-3xl);
  margin: 0 0 var(--spacing-2) 0;
  font-weight: var(--font-weight-semibold);
  color: white;
}

.welcome-subtitle {
  font-size: var(--font-size-base);
  margin: 0;
  opacity: 0.9;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-5);
}

.dashboard-card {
  background: var(--white);
  background-image: var(--pattern-light);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow);
  overflow: hidden;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
  position: relative;
}

.dashboard-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
}

.dashboard-card.quick-actions::before,
.dashboard-card.employee-tools::before,
.dashboard-card.user-profile::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  z-index: 1;
}

.dashboard-card.quick-actions::before {
  background: var(--gradient-primary);
}

.dashboard-card.employee-tools::before {
  background: linear-gradient(to right, var(--accent-color), var(--accent-dark));
}

.dashboard-card.user-profile::before {
  background: linear-gradient(to right, var(--secondary-color), var(--secondary-dark));
}

.card-header {
  padding: var(--spacing-4) var(--spacing-5);
  border-bottom: 1px solid var(--gray-200);
  background-color: var(--gray-50);
}

.card-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--gray-900);
}

.card-body {
  padding: var(--spacing-5);
}

/* User Profile Styles */
.user-profile .card-body {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.profile-component {
  margin-bottom: var(--spacing-5);
  width: 100%;
  display: flex;
  justify-content: center;
}

.profile-details {
  width: 100%;
}

.profile-row {
  display: flex;
  padding: var(--spacing-3) 0;
  border-bottom: 1px solid var(--gray-200);
  position: relative;
}

.profile-row:last-child {
  border-bottom: none;
}

.profile-row::before {
  content: '';
  position: absolute;
  left: -24px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--secondary-color);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.profile-row:hover::before {
  opacity: 1;
}

.profile-label {
  width: 40%;
  font-weight: var(--font-weight-semibold);
  color: var(--gray-600);
}

.profile-value {
  width: 60%;
  color: var(--gray-900);
  font-weight: var(--font-weight-medium);
}

/* Quick Actions Styles */
.action-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-4);
}

.action-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: var(--gray-900);
  padding: var(--spacing-4);
  border-radius: var(--border-radius-md);
  transition: all var(--transition-fast);
  background-color: var(--gray-50);
  border: 1px solid var(--gray-200);
}

.action-button:hover {
  background-color: var(--gray-100);
  border-color: var(--gray-300);
  transform: scale(1.05);
  text-decoration: none;
}

.button-icon {
  margin-bottom: var(--spacing-2);
  display: flex;
  justify-content: center;
  align-items: center;
  width: 48px;
  height: 48px;
  border-radius: var(--border-radius-full);
  background-color: rgba(255, 255, 255, 0.5);
}

.button-icon .material-icons {
  font-size: 24px;
}

.action-button span {
  font-size: var(--font-size-sm);
  text-align: center;
  font-weight: var(--font-weight-medium);
}

/* Specific action button styles */
.action-button.accounts {
  background-color: rgba(25, 118, 210, 0.1);
  border-color: rgba(25, 118, 210, 0.2);
}

.action-button.transfer {
  background-color: rgba(76, 175, 80, 0.1);
  border-color: rgba(76, 175, 80, 0.2);
}

.action-button.history {
  background-color: rgba(33, 150, 243, 0.1);
  border-color: rgba(33, 150, 243, 0.2);
}

.action-button.atm {
  background-color: rgba(255, 152, 0, 0.1);
  border-color: rgba(255, 152, 0, 0.2);
}

.action-button.approve {
  background-color: rgba(76, 175, 80, 0.1);
  border-color: rgba(76, 175, 80, 0.2);
}

.action-button.users {
  background-color: rgba(33, 150, 243, 0.1);
  border-color: rgba(33, 150, 243, 0.2);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>