<template>
  <div class="dashboard-container">

    <!-- Main Content -->
    <main class="main-content">
      <h1 class="title">Welcome, {{ user.email || "Guest" }}</h1>
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
</style>
