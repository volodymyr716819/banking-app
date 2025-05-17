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
  font-family: 'Segoe UI', sans-serif;
}

.sidebar {
  width: 240px;
  background-color: #f4f4f4;
  border-right: 1px solid #ddd;
  padding: 20px;
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
  text-align: center;
  margin-bottom: 20px;
}

.user-info {
  background-color: #e2e8f0;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
  text-align: center;
}

.user-name {
  font-weight: bold;
  font-size: 1.1rem;
  margin-bottom: 5px;
}

.user-email {
  font-size: 0.9rem;
  color: #555;
  margin-bottom: 5px;
}

.user-role {
  font-size: 0.8rem;
  background-color: #4CAF50;
  color: white;
  display: inline-block;
  padding: 3px 8px;
  border-radius: 12px;
  text-transform: capitalize;
}

.nav-links {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-link {
  padding: 10px;
  border-radius: 6px;
  text-decoration: none;
  color: #333;
  transition: background-color 0.2s ease;
}

.nav-link:hover {
  background-color: #e2e8f0;
}

.logout-button {
  margin-top: 20px;
  background: none;
  color: #e53e3e;
  border: none;
  cursor: pointer;
  padding: 10px;
  border-radius: 6px;
  text-align: left;
  transition: background-color 0.2s ease;
}

.logout-button:hover {
  background-color: #ffe5e5;
}

.main-content {
  flex-grow: 1;
  padding: 40px;
  background-color: #fff;
}
</style>
