<template>
    <div class="dashboard">
        <h1>Welcome to your dashboard</h1>
        <p v-if="user">Logged in as: {{ user.email }}</p>

        <button @click="handleLogout">Logout</button>
        <button @click="handleATM">ATM</button>
    </div>
</template>

<script setup>
import { useAuthStore } from '../store/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const user = authStore.user
const router = useRouter()

function handleLogout() {
    authStore.logout()
    router.push('/login')
}

function handleATM() {
    router.push('/atm')
}
</script>

<style scoped>
.dashboard {
    max-width: 600px;
    margin: 60px auto;
    text-align: center;
}

button {
    margin-left: 10px;
    margin-top: 2rem;
    padding: 0.75rem 1.5rem;
    font-size: 1rem;
    background-color: #e74c3c;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
}

button:hover {
    background-color: #c0392b;
}
</style>