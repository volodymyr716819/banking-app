<template>
    <div class="auth-container">
        <h2>Login</h2>
        <form @submit.prevent="handleLogin">
            <input v-model="email" type="email" placeholder="Email" required />
            <input v-model="password" type="password" placeholder="Password" required />

            <button type="submit">Login</button>

            <p class="error" v-if="errorMessage">{{ errorMessage }}</p>
            <p>
                Don't have an account?
                <router-link to="/register">Register here</router-link>
            </p>
        </form>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const email = ref('')
const password = ref('')
const errorMessage = ref('')

const authStore = useAuthStore()
const router = useRouter()

async function handleLogin() {
    errorMessage.value = ''

    if (!email.value || !password.value) {
        errorMessage.value = 'Please fill in all fields'
        return
    }

    if (!email.value.includes('@')) {
        errorMessage.value = 'Please enter a valid email address'
        return
    }

    try {
        await authStore.login(email.value, password.value)
        router.push('/dashboard')
    } catch (err) {
        if (err.message.includes('pending approval')) {
            errorMessage.value = 'Your account is pending employee approval.'
        } else {
            errorMessage.value = 'Login failed: Invalid email or password.'
        }
    }
}
</script>

<style scoped>
.auth-container {
    max-width: 400px;
    margin: 80px auto;
    padding: 2rem;
    border-radius: 12px;
    background: #f5f5f5;
    box-shadow: 0 0 8px rgba(0, 0, 0, 0.1);
}

input {
    display: block;
    width: 100%;
    padding: 0.75rem;
    margin-bottom: 1rem;
    border: 1px solid #ccc;
    border-radius: 8px;
}

button {
    width: 100%;
    padding: 0.75rem;
    background-color: #2c3e50;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
}

button:hover {
    background-color: #1a252f;
}

.error {
    color: red;
    margin-top: 0.5rem;
}
</style>