<template>
    <div class="auth-card">
        <h2>Register</h2>
        <form @submit.prevent="register">
            <input v-model="email" type="email" placeholder="Email" required />
            <input v-model="password" type="password" placeholder="Password" required />
            <button type="submit">Register</button>
        </form>
        <p class="success" v-if="success">Account created! You can now <router-link to="/login">log in</router-link>.
        </p>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../store/auth'

const email = ref('')
const password = ref('')
const success = ref(false)
const auth = useAuthStore()

const register = async () => {
    await auth.register(email.value, password.value)
    success.value = true
    email.value = ''
    password.value = ''
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
    background-color: #10b981;
    color: white;
    border: none;
    border-radius: 5px;
}

.success {
    color: green;
    margin-top: 1rem;
}
</style>