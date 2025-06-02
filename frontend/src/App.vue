<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useAuthStore } from './store/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

onMounted(async () => {
  // Initialize the auth state
  authStore.initAuth();
  
  // Validate token when app loads to ensure we have valid authentication
  if (authStore.isAuthenticated) {
    try {
      const isValid = await authStore.validateToken();
      
      // Redirect to login if token is invalid and we're on a protected route
      if (!isValid && router.currentRoute.value.meta.requiresAuth) {
        router.push('/login');
      }
    } catch (error) {
      // If validation fails for any reason and we're on a protected route, redirect to login
      if (router.currentRoute.value.meta.requiresAuth) {
        router.push('/login');
      }
    }
  } else if (router.currentRoute.value.meta.requiresAuth) {
    // No authentication but trying to access protected route
    router.push('/login');
  }
});
</script>

<style>
/* Global styles */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  font-size: 16px;
  line-height: 1.5;
  color: #333;
  background-color: #f5f7fa;
}

/* Custom scrollbar for a modern look */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #aaa;
}

/* Utility classes */
.text-center {
  text-align: center;
}

.w-100 {
  width: 100%;
}

.mb-1 {
  margin-bottom: 0.25rem;
}

.mb-2 {
  margin-bottom: 0.5rem;
}

.mb-3 {
  margin-bottom: 1rem;
}

.mt-1 {
  margin-top: 0.25rem;
}

.mt-2 {
  margin-top: 0.5rem;
}

.mt-3 {
  margin-top: 1rem;
}
</style>