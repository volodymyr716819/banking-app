<template>
    <div class="auth-card">
        <h2>Login</h2>
        <form @submit.prevent="login">
            <input v-model="email" type="email" placeholder="Email" required />
            <input v-model="password" type="password" placeholder="Password" required />
            <button type="submit">Log In</button>
        </form>
        <p class="error" v-if="error">{{ error }}</p>
        <router-link to="/register">No account? Register</router-link>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../store/auth'

const email = ref('')
const password = ref('')
const error = ref('')
const auth = useAuthStore()

const login = async () => {
    error.value = ''
    try {
        await auth.login(email.value, password.value)
        window.location.href = '/dashboard'
    } catch (err) {
        error.value = 'Invalid email or password'
    }
}
</script>

<style scoped>
.auth-card {
    max-width: 400px;
    margin: 3rem auto;
    padding: 2rem;
    border-radius: 10px;
    background: #f4f4f4;
    box-shadow: 0 0 10px #ccc;
}

input,
button {
    display: block;
    width: 100%;
    margin: 0.5rem 0;
    padding: 0.6rem;
    font-size: 1rem;
}

button {
    background-color: #3b82f6;
    color: white;
    border: none;
    border-radius: 5px;
}

.error {
    color: red;
    margin-top: 0.5rem;
}
</style>