<template>
    <div class="min-h-screen flex flex-col items-center justify-center bg-gray-50 p-4">
      <div class="bg-white shadow-lg rounded-2xl p-8 max-w-md text-center">
        <h1 class="text-3xl font-semibold mb-4">
          Welcome to Banking App
        </h1>
        <p class="text-lg mb-6">
          Your account registration is pending approval.<br/>
          An employee will review your application shortly.
        </p>
        <button
          @click="logout"
          class="mt-4 px-6 py-2 rounded-xl font-medium hover:bg-gray-100 transition"
        >
          Log Out
        </button>
      </div>
    </div>
  </template>
  
  <script setup>
  import { useRouter } from 'vue-router'
  
  const router = useRouter()
  
  function logout() {
    // Clear any stored auth data
    localStorage.removeItem('token')
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