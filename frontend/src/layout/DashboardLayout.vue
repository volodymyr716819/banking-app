<template>
  <div class="dashboard-container">
    <aside class="sidebar">
      <div class="logo">BankApp</div>
      
      <div class="user-info">
        <div class="user-name">{{ auth.user?.name || "Guest" }}</div>
        <div class="user-email">{{ auth.user?.email }}</div>
        <div class="user-role">{{ auth.user?.role || "Customer" }}</div>
      </div>
      
      <nav class="nav-links">
        <router-link to="/dashboard/accounts" class="nav-link">Accounts</router-link>
        <router-link to="/dashboard/transfer" class="nav-link">Transfer Money</router-link>
        <router-link to="/dashboard/transactions" class="nav-link">Transaction History</router-link>
        <router-link to="/dashboard/atm" class="nav-link">ATM Operations</router-link>

        <!-- Employee-only links -->
        <router-link v-if="auth.user?.role?.toLowerCase() === 'employee'" to="/dashboard/approve"
          class="nav-link">Approve Accounts</router-link>

        <router-link v-if="auth.user?.role?.toLowerCase() === 'employee'" to="/dashboard/approve-users"
          class="nav-link">Approve Users</router-link>
          
        <router-link v-if="auth.user?.role?.toLowerCase() === 'employee'" to="/dashboard/customers"
          class="nav-link">View Customers</router-link>
          
        <router-link v-if="auth.user?.role?.toLowerCase() === 'employee'" to="/dashboard/approved-accounts"
          class="nav-link">View Approved Accounts</router-link>

        <button @click="logout" class="logout-button">Logout</button>
      </nav>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <router-view></router-view>
    </main>
  </div>
</template>

<script>
import { useRouter } from 'vue-router';
import { ref, onMounted } from 'vue';
import { useAuthStore } from '../store/auth';

export default {
  setup() {
    const router = useRouter();
    const user = ref({ username: 'Guest' });
    const auth = useAuthStore();

    onMounted(() => {
      const storedUsername = localStorage.getItem('username');
      if (storedUsername) {
        user.value.username = storedUsername;
      }
    });

    const logout = () => {
      auth.logout(); 
      router.push('/login');
    };

    return {
      logout,
      user,
      auth
    };
  }
};
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 280px;
  min-width: 280px; /* Fix to prevent shrinking */
  background: linear-gradient(to bottom, var(--primary-dark), var(--primary-color));
  color: var(--text-on-primary);
  box-shadow: 3px 0 10px rgba(0, 0, 0, 0.1);
  padding: 0;
  display: flex;
  flex-direction: column;
  z-index: 10;
  position: relative;
}

.logo {
  font-size: 1.8rem;
  font-weight: bold;
  text-align: center;
  padding: 25px 0;
  background-color: rgba(255, 255, 255, 0.05);
  letter-spacing: 1px;
  text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.user-info {
  background-color: rgba(255, 255, 255, 0.1);
  padding: 20px;
  margin: 20px;
  border-radius: var(--border-radius);
  text-align: center;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.user-name {
  font-weight: 600;
  font-size: 1.2rem;
  margin-bottom: 5px;
  color: white;
}

.user-email {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 10px;
}

.user-role {
  font-size: 0.8rem;
  background-color: var(--secondary-color);
  color: white;
  display: inline-block;
  padding: 5px 12px;
  border-radius: 50px;
  text-transform: capitalize;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.nav-links {
  display: flex;
  flex-direction: column;
  padding: 10px 20px;
  flex-grow: 1;
}

.nav-link {
  padding: 14px 20px;
  margin: 5px 0;
  border-radius: var(--border-radius);
  text-decoration: none;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  position: relative;
}

.nav-link:hover {
  background-color: rgba(255, 255, 255, 0.1);
  transform: translateX(5px);
}

.nav-link.router-link-active {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: 600;
}

.nav-link.router-link-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 4px;
  background-color: var(--secondary-color);
  border-radius: 0 4px 4px 0;
}

.logout-button {
  margin: 20px;
  background-color: rgba(244, 67, 54, 0.1);
  color: white;
  border: none;
  cursor: pointer;
  padding: 14px;
  border-radius: var(--border-radius);
  text-align: center;
  transition: all 0.2s ease;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logout-button:hover {
  background-color: var(--error-color);
  transform: translateY(-2px);
}

.main-content {
  flex-grow: 1;
  padding: 30px;
  background-color: var(--ultra-light-gray);
  overflow-y: auto;
}

@media (max-width: 768px) {
  .dashboard-container {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    position: sticky;
    top: 0;
    z-index: 100;
  }
  
  .nav-links {
    padding: 10px;
  }
  
  .user-info {
    margin: 10px;
  }
  
  .logo {
    padding: 15px 0;
  }
  
  .main-content {
    padding: 20px;
  }
}
</style>
