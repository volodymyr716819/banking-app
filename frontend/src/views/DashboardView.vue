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
  flex-direction: column;
  gap: 30px;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.title {
  font-size: 2.5rem;
  margin-bottom: 10px;
  color: var(--primary-dark);
  position: relative;
  display: inline-block;
}

.title::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 0;
  width: 80px;
  height: 4px;
  background-color: var(--secondary-color);
  border-radius: 2px;
}

.subtitle {
  color: var(--text-secondary);
  margin-bottom: 10px;
  font-size: 1.2rem;
}

.info-message {
  font-size: 1.1rem;
  color: var(--text-secondary);
  background-color: white;
  padding: 20px;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  border-left: 4px solid var(--primary-color);
}

.user-details {
  background-color: white;
  border-radius: var(--border-radius);
  padding: 25px;
  margin-bottom: 20px;
  border: none;
  box-shadow: var(--box-shadow);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  display: grid;
  grid-template-columns: 1fr;
  gap: 15px;
}

.user-details:hover {
  transform: translateY(-5px);
  box-shadow: var(--box-shadow-hover);
}

.detail-item {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: var(--border-radius);
  background-color: var(--ultra-light-gray);
}

.detail-item strong {
  color: var(--text-secondary);
  min-width: 120px;
}

.role-badge {
  background-color: var(--secondary-color);
  color: white;
  padding: 6px 12px;
  border-radius: 50px;
  font-size: 0.9rem;
  text-transform: capitalize;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* Dashboard widgets */
.dashboard-widgets {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 25px;
  margin-top: 20px;
}

.widget {
  background-color: white;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  padding: 25px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
  overflow: hidden;
}

.widget:hover {
  transform: translateY(-5px);
  box-shadow: var(--box-shadow-hover);
}

.widget-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.widget-value {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 10px;
}

.widget-footer {
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.widget::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background-color: var(--primary-color);
}

.widget.accent-green::before {
  background-color: var(--success-color);
}

.widget.accent-blue::before {
  background-color: var(--info-color);
}

.widget.accent-orange::before {
  background-color: var(--warning-color);
}

@media (max-width: 768px) {
  .main-content {
    gap: 20px;
  }
  
  .title {
    font-size: 2rem;
  }
  
  .user-details {
    padding: 15px;
  }
  
  .dashboard-widgets {
    grid-template-columns: 1fr;
    gap: 15px;
  }
}
</style>
