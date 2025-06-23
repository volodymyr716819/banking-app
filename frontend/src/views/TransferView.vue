<template>
  <div class="transfer-page">
    <h1 class="page-title">Transfer Money</h1>

    <form class="transfer-form" @submit.prevent="submitTransfer">
      
      <!-- Sender account dropdown -->
      <div class="form-group">
        <label for="from">From Account (IBAN):</label>
        <select v-model="fromIban" required>
          <option disabled value="">Select an account</option>
          <option v-for="acc in accounts" :key="acc.id" :value="acc.iban">
            {{ acc.type }} - {{ acc.formattedIban || acc.iban }} - €{{ acc.balance.toFixed(2) }}
            {{ !acc.approved ? '- Not Approved' : '' }}
          </option>
        </select>
        <div v-if="selectedAccountIsNotApproved" class="account-warning">
          This account is not approved for transactions yet.
        </div>
      </div>

      <!-- Receiver IBAN -->
      <div class="form-group">
        <label for="to">To IBAN:</label>
        <input type="text" v-model="toIban" placeholder="Enter receiver's IBAN" required />
      </div>
      
      <!-- Amount -->
      <div class="form-group">
        <label for="amount">Amount (€):</label>
        <input type="number" v-model="amount" step="0.01" min="0.01" required />
      </div>

      <!-- Optional description -->
      <div class="form-group">
        <label for="description">Description:</label>
        <input type="text" v-model="description" placeholder="Optional description" />
      </div>

      <!-- Submit button -->
      <button type="submit" class="submit-button" :disabled="submitDisabled">Transfer</button>
      
      <!-- Block transfers if sender account is not approved -->
      <div v-if="selectedAccountIsNotApproved" class="transfer-disabled-message">
        Transfers from unapproved accounts are not allowed. Please contact support for account approval.
      </div>
    </form>
    
    <!-- Feedback message -->
    <div v-if="message" :class="['transfer-message', messageType]">
      <span v-if="messageType === 'error'" class="message-icon">❌</span>
      <span v-else-if="messageType === 'success'" class="message-icon">✓</span>
      {{ message }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../store/auth';
import api from '../lib/api';

const auth = useAuthStore();
const accounts = ref([]);
const fromAccount = ref('');
const toIban = ref('');
const amount = ref('');
const description = ref('');
const message = ref('');
const messageType = ref(''); // success | error

// Check if selected account is approved
const selectedAccountIsNotApproved = computed(() => {
  if (!fromAccount.value) return false;
  const selectedAccount = accounts.value.find(acc => acc.iban === fromAccount.value);
  return selectedAccount && !selectedAccount.approved;
});

// Computed property to disable submit button when needed
const submitDisabled = computed(() => {
  return selectedAccountIsNotApproved.value || !fromAccount.value || !toIban.value || !amount.value;
});

// Load user accounts
const fetchAccounts = async () => {
  try {
    const res = await api.get(`/accounts/user/${auth.user.id}`, {
    headers: {
       Authorization: `Bearer ${auth.token}`
      }
    });

    accounts.value = res.data;
  } catch {
    message.value = 'Failed to fetch accounts.';
    messageType.value = 'error';
  }
};

// Perform the transfer
const submitTransfer = async () => {
  message.value = '';

  if (parseFloat(amount.value) <= 0) {
    message.value = 'Amount must be greater than 0.';
    messageType.value = 'error';
    return;
  }

  try {
    const response = await api.post('/transactions/transfer', {
      senderIban: fromAccount.value,
      receiverIban: toIban.value,
      amount: parseFloat(amount.value),
      description: description.value
    }, {
      headers: { Authorization: `Bearer ${auth.token}` }
    });

    message.value = 'Transfer successful!';
    messageType.value = 'success';
    fromAccount.value = '';
    toIban.value = '';
    amount.value = '';
    description.value = '';
    await fetchAccounts(); // Refresh accounts to show updated balances
  } catch (err) {
    messageType.value = 'error';
    message.value = err.response?.data?.message || 'Transfer failed.';
  }
};

onMounted(fetchAccounts);
</script>

<style scoped>
.transfer-page {
  padding: 40px;
  background-color: #fff;
}

.page-title {
  font-size: 2rem;
  margin-bottom: 20px;
  color: #333;
}

.transfer-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-width: 500px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

label {
  font-weight: 500;
  color: #4a5568;
}

input,
select {
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e0;
  font-size: 1rem;
  transition: all 0.2s;
}

input:focus,
select:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 3px rgba(49, 130, 206, 0.1);
}

.submit-button {
  padding: 12px 24px;
  background-color: #3182ce;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s ease;
  margin-top: 10px;
  align-self: flex-start;
}

.submit-button:hover:not(:disabled) {
  background-color: #2b6cb0;
}

.submit-button:disabled {
  background-color: #a0aec0;
  cursor: not-allowed;
  opacity: 0.7;
}

.transfer-message {
  padding: 16px;
  border-radius: 6px;
  margin-top: 20px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 10px;
}

.transfer-message.success {
  background-color: #c6f6d5;
  color: #276749;
  border-left: 4px solid #48bb78;
}

.transfer-message.error {
  background-color: #fed7d7;
  color: #c53030;
  border-left: 4px solid #f56565;
}

.message-icon {
  font-size: 1.2rem;
}

.account-warning {
  font-size: 0.9rem;
  color: #c53030;
  margin-top: 4px;
}

.transfer-disabled-message {
  font-size: 0.9rem;
  padding: 10px;
  background-color: #fed7d7;
  border-radius: 4px;
  color: #c53030;
  margin-bottom: 10px;
}
</style>


