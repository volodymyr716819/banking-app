<template>
  <div class="dashboard-container">
    <aside class="sidebar">
      <div class="logo">BankApp</div>
      
      <!-- User information section -->
      <div class="user-info">
        <UserProfileComponent :user="auth.user" size="medium" />
      </div>
      
      <nav class="nav-links">
        <router-link :to="{path: '/dashboard'}" class="nav-link" :class="{ 'router-link-active': $route.path === '/dashboard' }">
          <span class="material-icons nav-icon">dashboard</span>
          <span>Dashboard</span>
        </router-link>
        <router-link v-if="auth.user && auth.user.role !== 'EMPLOYEE'" :to="{path: '/dashboard/accounts'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/accounts') }">
          <span class="material-icons nav-icon">account_balance</span>
          <span>Accounts</span>
        </router-link>
        <router-link v-if="auth.user && auth.user.role !== 'EMPLOYEE'" :to="{path: '/dashboard/transfer'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/transfer') }">
          <span class="material-icons nav-icon">swap_horiz</span>
          <span>Transfer Money</span>
        </router-link>
        <router-link v-if="auth.user && auth.user.role !== 'EMPLOYEE'" :to="{path: '/dashboard/history'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/history') }">
          <span class="material-icons nav-icon">history</span>
          <span>Transaction History</span>
        </router-link>
        <router-link v-if="auth.user && auth.user.role !== 'EMPLOYEE'" :to="{path: '/dashboard/atm'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/atm') }">
          <span class="material-icons nav-icon">atm</span>
          <span>ATM Operations</span>
        </router-link>
        <router-link v-if="auth.user && auth.user.role !== 'EMPLOYEE'" :to="{path: '/dashboard/pin-settings'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/pin-settings') }">
          <span class="material-icons nav-icon">pin</span>
          <span>PIN Management</span>
        </router-link>
        <router-link :to="{path: '/dashboard/search-customer'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/search-customer') }">
          <span class="material-icons nav-icon">person_search</span>
          <span>Find Customer</span>
        </router-link>

        <!-- Employee-only links -->
        <div v-if="auth.user && auth.user.role === 'EMPLOYEE'" class="nav-section">
          <div class="section-title">Employee Tools</div>
          <router-link :to="{path: '/dashboard/approve'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/approve') && !$route.path.includes('/dashboard/approve-users') }">
            <span class="material-icons nav-icon">check_circle</span>
            <span>Approve Accounts</span>
          </router-link>
          <router-link :to="{path: '/dashboard/approve-users'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/approve-users') }">
            <span class="material-icons nav-icon">how_to_reg</span>
            <span>Approve Users</span>
          </router-link>
          <router-link :to="{path: '/dashboard/users'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/users') }">
            <span class="material-icons nav-icon">people</span>
            <span>Customers</span>
          </router-link>
          <router-link :to="{path: '/dashboard/employee-accounts'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/employee-accounts') }">
            <span class="material-icons nav-icon">account_balance_wallet</span>
            <span>Accounts List</span>
          </router-link>
          <router-link :to="{path: '/dashboard/history'}" class="nav-link" :class="{ 'router-link-active': $route.path.includes('/dashboard/history') }">
            <span class="material-icons nav-icon">history</span>
            <span>Transaction History</span>
          </router-link>
        </div>

        <button @click="logout" class="logout-button">
          <span class="material-icons nav-icon">logout</span>
          <span>Logout</span>
        </button>
      </nav>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <header class="content-header">
        <h1 class="page-title">
          {{ currentPageTitle }}
        </h1>
        <div class="header-right">
          <span class="welcome-text">Welcome, {{ auth.userName }}</span>
          <UserProfileComponent :user="auth.user" size="small" class="header-profile" />
        </div>
      </header>
      <div class="content-body">
        <router-view v-slot="{ Component }">
          <component :is="Component" @vue:mounted="onComponentMounted" />
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { ref, computed, onMounted, watch } from 'vue';
import { useAuthStore } from '../store/auth';
import UserProfileComponent from '../components/UserProfileComponent.vue';

const router = useRouter();
const auth = useAuthStore();
const currentComponent = ref(null);
const currentPageTitle = ref('Banking Dashboard');

// Format user role for display
const formatRole = (role) => {
  if (!role) return 'Guest';
  return role.charAt(0).toUpperCase() + role.slice(1).toLowerCase();
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

const onComponentMounted = (instance) => {
  // Look for the __pageTitle property in the component's setup context
  if (instance && instance.exposed && instance.exposed.__pageTitle) {
    currentPageTitle.value = instance.exposed.__pageTitle;
  } else if (instance && instance.$options && instance.$options.__pageTitle) {
    currentPageTitle.value = instance.$options.__pageTitle;
  } else {
    // If no page title is found, use the default
    const path = router.currentRoute.value.path;
    if (path.includes('accounts')) currentPageTitle.value = 'Your Accounts';
    else if (path.includes('transfer')) currentPageTitle.value = 'Transfer Money';
    else if (path.includes('history')) currentPageTitle.value = 'Transaction History';
    else if (path.includes('atm')) currentPageTitle.value = 'ATM Operations';
    else if (path.includes('pin-settings')) currentPageTitle.value = 'PIN Management';
    else if (path.includes('search-customer')) currentPageTitle.value = 'Find Customer';
    else if (path.includes('approve')) currentPageTitle.value = 'Approve Accounts';
    else if (path.includes('users')) currentPageTitle.value = 'User Management';
    else currentPageTitle.value = 'Banking Dashboard';
  }
};

const showLogoutConfirm = ref(false);

const logout = () => {
  // Show confirmation dialog before logging out
  if (confirm('Are you sure you want to log out?')) {
    // Perform logout
    auth.logout();
    router.push('/login');
  }
};

// Update the page title when the route changes
watch(
  () => router.currentRoute.value.path,
  (path) => {
    // Use route-based logic to determine the page title
    if (path.includes('/dashboard')) {
      if (path === '/dashboard') currentPageTitle.value = 'Dashboard';
      else if (path.includes('accounts')) currentPageTitle.value = 'Your Accounts';
      else if (path.includes('transfer')) currentPageTitle.value = 'Transfer Money';
      else if (path.includes('history')) currentPageTitle.value = 'Transaction History';
      else if (path.includes('atm')) currentPageTitle.value = 'ATM Operations';
      else if (path.includes('pin-settings')) currentPageTitle.value = 'PIN Management';
      else if (path.includes('search-customer')) currentPageTitle.value = 'Find Customer';
      else if (path.includes('approve')) currentPageTitle.value = 'Approve Accounts';
      else if (path.includes('users')) currentPageTitle.value = 'User Management';
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 260px;
  background: var(--gradient-primary);
  color: var(--white);
  padding: var(--spacing-4);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-md);
  z-index: var(--z-index-10);
  position: relative;
  overflow: hidden;
}

.sidebar::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--pattern-light);
  opacity: 0.03;
  pointer-events: none;
}

.logo {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  text-align: center;
  margin-bottom: var(--spacing-6);
  color: var(--white);
  letter-spacing: 0.5px;
}

/* User info styling */
.user-info {
  display: flex;
  align-items: center;
  padding: var(--spacing-4) 0;
  margin-bottom: var(--spacing-4);
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.user-avatar {
  width: 48px;
  height: 48px;
  background-color: var(--primary-light);
  border-radius: var(--border-radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-weight-semibold);
  margin-right: var(--spacing-3);
}

.user-details {
  overflow: hidden;
}

.user-name {
  font-weight: var(--font-weight-semibold);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: var(--font-size-sm);
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-links {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  flex-grow: 1;
}

.nav-link {
  padding: var(--spacing-3);
  border-radius: var(--border-radius);
  text-decoration: none;
  color: var(--white);
  transition: background-color var(--transition-fast), color var(--transition-fast);
  display: flex;
  align-items: center;
}

.nav-icon {
  margin-right: var(--spacing-2);
  font-size: var(--font-size-lg);
}

.nav-link:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--white);
  text-decoration: none;
}

.nav-link.router-link-active {
  font-weight: var(--font-weight-semibold);
  background-color: var(--primary-color);
  color: var(--white);
}

.nav-section {
  margin-top: var(--spacing-4);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.section-title {
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgba(255, 255, 255, 0.6);
  margin: var(--spacing-2) 0;
  padding-left: var(--spacing-3);
}

.logout-button {
  margin-top: var(--spacing-6);
  background: none;
  color: var(--error-color);
  border: none;
  cursor: pointer;
  padding: var(--spacing-3);
  border-radius: var(--border-radius);
  text-align: left;
  transition: background-color var(--transition-fast);
  font-weight: var(--font-weight-medium);
}

.logout-button:hover {
  background-color: rgba(244, 67, 54, 0.15);
}

/* Main content styling */
.main-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--gray-50);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-4) var(--spacing-8);
  background-color: var(--white);
  border-bottom: 1px solid var(--gray-200);
  box-shadow: var(--shadow-sm);
}

.page-title {
  font-size: var(--font-size-2xl);
  color: var(--gray-900);
  margin: 0;
  font-weight: var(--font-weight-semibold);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.welcome-text {
  color: var(--gray-600);
  font-size: var(--font-size-sm);
}

.header-profile {
  margin-left: var(--spacing-2);
}

.content-body {
  padding: var(--spacing-8);
  overflow-y: auto;
  flex-grow: 1;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .dashboard-container {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    padding: var(--spacing-3);
  }
  
  .main-content {
    width: 100%;
  }
  
  .content-header {
    padding: var(--spacing-3) var(--spacing-4);
  }
  
  .content-body {
    padding: var(--spacing-4);
  }
}
</style>