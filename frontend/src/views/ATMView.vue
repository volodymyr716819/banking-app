<template>
  <div class="dashboard">
    <h2>ATM - Deposit / Withdraw</h2>

    <p v-if="typeof balance === 'number'">
      💰 Current balance: {{ balance.toFixed(2) }} €
    </p>
    <p v-else-if="typeof balance === 'string'">
      ⚠️ {{ balance }}
    </p>

    <div v-if="accounts.length">
      <label for="account-select">Select Account:</label>
      <select id="account-select" v-model.number="selectedAccountId" @change="loadBalance">
        <option v-for="account in accounts" :key="account.id" :value="account.id">
           {{ account.type }} (ID: {{ account.id }})
        </option>
      </select>
    </div>

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
const token = authStore.token

const accounts = ref([])
const selectedAccountId = ref(null)
const balance = ref(null)
const form = ref({ amount: 0 })
const message = ref('')
const error = ref('')

onMounted(async () => {
  if (!user?.id) {
    balance.value = "Not logged in"
    error.value = "Missing user data"
    return
  }

  try {
    const res = await fetch(`http://localhost:8080/api/accounts/user/${user.id}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    accounts.value = await res.json()
    if (accounts.value.length > 0) {
      selectedAccountId.value = accounts.value[0].id
      await loadBalance()
    } else {
      error.value = "No accounts found"
    }
  } catch (err) {
    error.value = "Failed to load accounts: " + err.message
  }
})

async function loadBalance() {
  if (!selectedAccountId.value) {
    error.value = "No account selected"
    return
  }

  try {
    const res = await fetch(`http://localhost:8080/api/atm/balance?accountId=${selectedAccountId.value}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    if (!res.ok) throw new Error(await res.text())
    balance.value = await res.json()
  } catch (err) {
    error.value = "Failed to load balance: " + err.message
  }
}


async function submit(type) {
  message.value = ''
  error.value = ''

  if (form.value.amount <= 0) {
    error.value = "Amount must be greater than zero"
    return
  }

  try {
    const res = await fetch(`http://localhost:8080/api/atm/${type}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        accountId: selectedAccountId.value,
        amount: form.value.amount
      })
    })

    const text = await res.text()
    if (!res.ok) throw new Error(text)
    message.value = text
    form.value.amount = 0
    await loadBalance()
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
input, select {
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
