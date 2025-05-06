<template>
    <div class="auth-container">
        <h2>Register</h2>
        <form @submit.prevent="handleRegister">
            <input v-model="username" type="text" placeholder="Username" required />
            <input v-model="email" type="email" placeholder="Email" required />
            <input v-model="password" type="password" placeholder="Password" required />
            <input v-model="confirmPassword" type="password" placeholder="Confirm Password" required />

            <button type="submit">Register</button>

            <p class="error" v-if="errorMessage">{{ errorMessage }}</p>
            <p>
                Already have an account?
                <router-link to="/login">Login here</router-link>
            </p>
        </form>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMessage = ref('')

const authStore = useAuthStore()
const router = useRouter()

async function handleRegister() {
    errorMessage.value = ''

    if (!username.value || !email.value || !password.value || !confirmPassword.value) {
        errorMessage.value = 'Please fill in all fields'
        return
    }

    if (!email.value.includes('@')) {
        errorMessage.value = 'Please enter a valid email address'
        return
    }

    if (password.value.length < 6) {
        errorMessage.value = 'Password must be at least 6 characters'
        return
    }

    if (password.value !== confirmPassword.value) {
        errorMessage.value = "Passwords don't match"
        return
    }

    try {
        await authStore.register(email.value, password.value)
        router.push('/dashboard')
    } catch (err) {
        errorMessage.value = 'Something went wrong during registration'
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
    background-color: #27ae60;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
}

button:hover {
    background-color: #1e8449;
}

.error {
    color: red;
    margin-top: 0.5rem;
}
</style>