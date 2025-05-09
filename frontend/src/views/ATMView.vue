<template>
  <div class="dashboard">
    <h2>ATM - Deposit / Withdraw</h2>

    <p v-if="typeof balance === 'number'">
      💰 Current balance: {{ balance.toFixed(2) }} €
    </p>
    <p v-else-if="typeof balance === 'string'">
      ⚠️ {{ balance }}
    </p>

    <form @submit.prevent="handleSubmit">
      <label>Amount</label>
      <input type="number" v-model.number="form.amount" min="0.01" required />

      <div>
        <button @click.prevent="submit('deposit')">Deposit</button>
        <button @click.prevent="submit('withdraw')">Withdraw</button>
      </div>
    </form>

    <div v-if="message" class="message">{{ message }}</div>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'

const authStore = useAuthStore()
const user = authStore.user

const balance = ref(null)
const form = ref({ amount: 0 })
const message = ref('')
const error = ref('')

onMounted(async () => {
  if (!user || !user.id) {
    balance.value = "Not logged in";
    error.value = "Missing user data";
    return;
  }

  try {
    const res = await fetch(`/api/atm/balance?userId=${user.id}`);
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    balance.value = data;
  } catch (err) {
    balance.value = "Could not load balance";
    error.value = err.message;
  }
});

async function submit(type) {
  message.value = ''
  error.value = ''
  try {
    const res = await fetch(`http://localhost:8080/api/atm/${type}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: user.id, amount: form.value.amount })
    })

    const text = await res.text()
    if (!res.ok) throw new Error(text)
    message.value = text
    form.value.amount = 0

    // Reload balance
    const res2 = await fetch(`http://localhost:8080/api/atm/balance?userId=${user.id}`)
    balance.value = await res2.json()

  } catch (err) {
    error.value = err.message
  }
}
</script>


<style scoped>
.dashboard {
  max-width: 600px;
  margin: 60px auto;
  text-align: center;
}
input {
  margin: 1rem;
  padding: 0.5rem;
  font-size: 1rem;
}
button {
  margin: 1rem 0.5rem;
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
.message {
  color: green;
  margin-top: 1rem;
}
.error {
  color: red;
  margin-top: 1rem;
}
</style>
