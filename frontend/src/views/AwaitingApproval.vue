<template>
    <div class="min-h-screen flex flex-col items-center justify-center bg-gray-50 p-4">
      <div class="bg-white shadow-lg rounded-2xl p-8 max-w-md text-center">
        <h1 class="text-3xl font-semibold mb-4">
          Welcome to Banking App
        </h1>
        <div class="user-welcome mb-4">
          <p class="text-xl font-semibold">Hello, {{ userName }}</p>
        </div>
        <div class="status-container p-4 bg-amber-50 border border-amber-200 rounded-lg mb-6">
          <p class="text-lg mb-2">
            Your account registration is pending approval.
          </p>
          <p class="text-md text-gray-600">
            An employee will review your application shortly.<br>
            You'll gain access to all banking services once approved.
          </p>
        </div>
        <button
          @click="logout"
          class="mt-4 px-6 py-2 rounded-xl font-medium bg-blue-50 hover:bg-blue-100 transition"
        >
          Log Out
        </button>
      </div>
    </div>
  </template>
  
  <script setup>
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '../store/auth'
  import { computed } from 'vue'
  
  const router = useRouter()
  const authStore = useAuthStore()
  
  const userName = computed(() => authStore.userName || 'User')
  
  function logout() {
    // Use the auth store's logout method
    authStore.logout()
    // Redirect back to login
    router.replace({ name: 'LoginView' })
  }
  </script>
  
  <style scoped>
  /* You can adjust these or use your existing design system */
  .bg-gray-50 {
    background-color: #f9fafb;
  }
  .hover\:bg-gray-100:hover {
    background-color: #f3f4f6;
  }
  </style>  