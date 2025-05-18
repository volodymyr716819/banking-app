<template>
  <div class="atm-page">
    <h1 class="page-title">ATM Operations</h1>

    <div v-if="error && !accounts.length" class="alert alert-warning">
      <div class="alert-icon">⚠️</div>
      <div>{{ error }}</div>
    </div>

    <div v-if="message" class="alert alert-success">
      <div class="alert-icon">✓</div>
      <div>{{ message }}</div>
    </div>

    <div v-if="error && accounts.length" class="alert alert-danger">
      <div class="alert-icon">⚠️</div>
      <div>{{ error }}</div>
    </div>

    <div v-if="accounts.length" class="atm-container">
      <div class="atm-card">
        <div class="account-selector">
          <h2 class="section-title">Select Account</h2>
          <div class="form-group">
            <label for="account-select">Choose account:</label>
            <select 
              id="account-select" 
              v-model.number="selectedAccountId" 
              @change="loadBalance"
              class="form-control"
            >
              <option v-for="account in accounts" :key="account.id" :value="account.id">
                {{ account.type }} - IBAN: {{ account.iban }}
              </option>
            </select>
          </div>

          <div v-if="typeof balance === 'number'" class="balance-display">
            <div class="balance-label">Current Balance:</div>
            <div class="balance-amount">€{{ balance.toFixed(2) }}</div>
          </div>
        </div>

        <div class="atm-operations">
          <h2 class="section-title">Transaction</h2>
          <form @submit.prevent="handleSubmit" class="atm-form">
            <div class="form-group">
              <label for="amount">Amount:</label>
              <div class="amount-input-wrapper">
                <span class="currency-symbol">€</span>
                <input 
                  type="number" 
                  id="amount"
                  v-model.number="form.amount" 
                  class="form-control amount-input"
                  min="0.01" 
                  step="0.01"
                  placeholder="0.00"
                  required 
                />
              </div>
            </div>

            <div class="atm-buttons">
              <button 
                type="button" 
                @click="submit('deposit')" 
                class="atm-button deposit-button"
                :disabled="isLoading"
              >
                <span v-if="isLoading && operationType === 'deposit'" class="spinner"></span>
                <span v-else>Deposit</span>
              </button>
              
              <button 
                type="button" 
                @click="submit('withdraw')" 
                class="atm-button withdraw-button"
                :disabled="isLoading"
              >
                <span v-if="isLoading && operationType === 'withdraw'" class="spinner"></span>
                <span v-else>Withdraw</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <div class="atm-info">
        <h3>ATM Instructions</h3>
        <ul class="instruction-list">
          <li>Select your account from the dropdown menu</li>
          <li>Enter the amount you wish to deposit or withdraw</li>
          <li>Click the appropriate button for your transaction</li>
          <li>All transactions are recorded in your transaction history</li>
          <li>Only approved accounts can be used for ATM operations</li>
        </ul>
      </div>
    </div>
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
const isLoading = ref(false)
const operationType = ref(null)

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
    const allAccounts = await res.json()
    // Filter to only approved accounts
    accounts.value = allAccounts.filter(account => account.approved)
    
    if (accounts.value.length > 0) {
      selectedAccountId.value = accounts.value[0].id
      await loadBalance()
    } else {
      if (allAccounts.length > 0) {
        error.value = "You have accounts, but none are approved yet. Please wait for approval."
      } else {
        error.value = "No accounts found"
      }
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
    // Clear any previous error
    error.value = ''
  } catch (err) {
    error.value = "Failed to load balance: " + err.message
  }
}

async function submit(type) {
  message.value = ''
  error.value = ''
  isLoading.value = true
  operationType.value = type

  if (form.value.amount <= 0) {
    error.value = "Amount must be greater than zero"
    isLoading.value = false
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
    
    // Success message
    message.value = `Successfully ${type === 'deposit' ? 'deposited' : 'withdrew'} €${form.value.amount.toFixed(2)}`
    
    // Reset the form
    form.value.amount = 0
    
    // Update balance
    await loadBalance()
    
    // Clear success message after 5 seconds
    setTimeout(() => {
      message.value = ''
    }, 5000)
    
  } catch (err) {
    error.value = err.message
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.atm-page {
  padding: 20px 0;
}

.page-title {
  font-size: 2.2rem;
  margin-bottom: 30px;
  color: var(--primary-dark);
  position: relative;
  display: inline-block;
}

.page-title::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 0;
  width: 60px;
  height: 4px;
  background-color: var(--secondary-color);
  border-radius: 2px;
}

.atm-container {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 30px;
  max-width: 1000px;
}

.atm-card {
  background-color: white;
  border-radius: var(--border-radius);
  overflow: hidden;
  box-shadow: var(--box-shadow);
  display: flex;
  flex-direction: column;
}

.account-selector {
  padding: 25px;
  border-bottom: 1px solid var(--light-gray);
}

.atm-operations {
  padding: 25px;
  background-color: var(--ultra-light-gray);
  flex-grow: 1;
}

.section-title {
  font-size: 1.3rem;
  margin: 0 0 20px 0;
  color: var(--primary-dark);
  position: relative;
  display: inline-block;
  padding-bottom: 8px;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 30px;
  height: 3px;
  background-color: var(--primary-color);
  border-radius: 1.5px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-control {
  padding: 12px 15px;
  border-radius: var(--border-radius);
  border: 1px solid var(--light-gray);
  font-size: 1rem;
  width: 100%;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.form-control:focus {
  border-color: var(--primary-color);
  outline: none;
  box-shadow: 0 0 0 3px rgba(30, 136, 229, 0.2);
}

.balance-display {
  background-color: var(--primary-color);
  color: white;
  padding: 20px;
  border-radius: var(--border-radius);
  margin-top: 20px;
  text-align: center;
}

.balance-label {
  font-size: 0.9rem;
  opacity: 0.9;
  margin-bottom: 5px;
}

.balance-amount {
  font-size: 2.5rem;
  font-weight: 700;
}

.amount-input-wrapper {
  position: relative;
}

.currency-symbol {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-secondary);
  font-weight: 500;
}

.amount-input {
  padding-left: 25px !important;
  font-size: 1.2rem !important;
}

.atm-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  margin-top: 20px;
}

.atm-button {
  padding: 14px;
  border: none;
  border-radius: var(--border-radius);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.deposit-button {
  background-color: var(--success-color);
  color: white;
}

.deposit-button:hover:not(:disabled) {
  background-color: #2e7d32;
  transform: translateY(-2px);
}

.withdraw-button {
  background-color: var(--error-color);
  color: white;
}

.withdraw-button:hover:not(:disabled) {
  background-color: #c62828;
  transform: translateY(-2px);
}

.atm-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.atm-info {
  background-color: white;
  border-radius: var(--border-radius);
  padding: 25px;
  box-shadow: var(--box-shadow);
  height: fit-content;
}

.atm-info h3 {
  font-size: 1.3rem;
  margin: 0 0 20px 0;
  color: var(--primary-dark);
  position: relative;
  display: inline-block;
  padding-bottom: 8px;
}

.atm-info h3::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 30px;
  height: 3px;
  background-color: var(--secondary-color);
  border-radius: 1.5px;
}

.instruction-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.instruction-list li {
  padding: 10px 0;
  position: relative;
  padding-left: 25px;
  border-bottom: 1px solid var(--light-gray);
}

.instruction-list li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: var(--secondary-color);
  font-weight: bold;
  font-size: 1.5rem;
  line-height: 1;
}

.instruction-list li:last-child {
  border-bottom: none;
}

.alert {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  padding: 15px 20px;
  border-radius: var(--border-radius);
  max-width: 1000px;
}

.alert-success {
  background-color: #f0fff4;
  color: #2f855a;
  border-left: 4px solid #48bb78;
}

.alert-warning {
  background-color: #fffaf0;
  color: #c05621;
  border-left: 4px solid #ed8936;
}

.alert-danger {
  background-color: #fff5f5;
  color: #c53030;
  border-left: 4px solid #f56565;
}

.alert-icon {
  font-size: 1.2rem;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 992px) {
  .atm-container {
    grid-template-columns: 1fr;
  }
}
</style>