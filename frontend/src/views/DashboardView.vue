<template>
  <div class="dashboard-container">

    <!-- Main Content -->
    <main class="main-content">
      <h1 class="title">Welcome, {{ user.name || user.email || "Guest" }}</h1>
      <div class="user-details">
        <div class="detail-item">
          <strong>Email:</strong> {{ user.email }}
        </div>
        <div class="detail-item">
          <strong>Account Type:</strong> <span class="role-badge">{{ user.role || 'Customer' }}</span>
        </div>
      </div>
      
      <p class="subtitle">
        You are now logged into your personal banking dashboard.
      </p>
      <div class="info-message">
        Choose an option from the sidebar to manage your accounts, transfer
        money, or view your transaction history.
      </div>
    </main>
  </div>
</template>

<script>
import { useAuthStore } from "../store/auth";
import { useRouter } from "vue-router";

export default {
  setup() {
    const router = useRouter();
    const authStore = useAuthStore();
    
    const logout = () => {
      localStorage.removeItem("authToken");
      router.push("/login");
    };

    return {
      logout,
      user: authStore.user,
    };
  },
};
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  font-family: "Segoe UI", sans-serif;
}

.main-content {
  flex-grow: 1;
  padding: 40px;
  background-color: #fff;
}

.title {
  font-size: 2rem;
  margin-bottom: 10px;
}

.subtitle {
  color: #555;
  margin-bottom: 20px;
}

.info-message {
  font-size: 1rem;
  color: #777;
}

.user-details {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
  border: 1px solid #e9ecef;
}

.detail-item {
  margin-bottom: 8px;
}

.role-badge {
  background-color: #4CAF50;
  color: white;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 0.85rem;
  text-transform: capitalize;
}
</style>
