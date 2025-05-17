<template>
  <div class="transaction-history-page">
    <h1 class="page-title">Transaction History</h1>
    
    <div class="filter-controls">
      <label>Filter by Account Type:</label>
      <select v-model="selectedAccountType" @change="fetchTransactions">
        <option value="">All Accounts</option>
        <option value="CHECKING">Checking</option>
        <option value="SAVINGS">Savings</option>
      </select>
    </div>
    
    <div v-if="loading" class="loading">
      Loading transactions...
    </div>
    
    <div v-else-if="error" class="error-message">
      {{ error }}
    </div>
    
    <div v-else-if="transactions.length === 0" class="no-transactions">
      No transactions found. Try a different filter or make some transfers first.
    </div>
    
    <div v-else class="transactions-container">
      <table class="transactions-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Description</th>
            <th>From</th>
            <th>To</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="transaction in transactions" :key="transaction.id" 
              :class="getTransactionClass(transaction)">
            <td>{{ formatDate(transaction.timestamp) }}</td>
            <td>{{ transaction.description || 'N/A' }}</td>
            <td>{{ getAccountInfo(transaction.senderAccount) }}</td>
            <td>{{ getAccountInfo(transaction.receiverAccount) }}</td>
            <td :class="getAmountClass(transaction)">
              {{ formatAmount(transaction) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const transactions = ref([]);
const loading = ref(true);
const error = ref('');
const selectedAccountType = ref('');

const fetchTransactions = async () => {
  try {
    loading.value = true;
    error.value = '';
    
    const response = await axios.get(
      `http://localhost:8080/api/transactions/user/${auth.user.id}`, {
        params: { 
          accountType: selectedAccountType.value 
        },
        headers: {
          Authorization: `Bearer ${auth.token}`
        }
      }
    );
    
    transactions.value = response.data;
    loading.value = false;
  } catch (err) {
    console.error('Failed to fetch transactions:', err);
    error.value = 'Failed to load transactions. Please try again later.';
    loading.value = false;
  }
};

const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
};

const formatAmount = (transaction) => {
  if (!transaction.amount) return '€0.00';
  
  // Format with 2 decimal places
  return `€${parseFloat(transaction.amount).toFixed(2)}`;
};

const getAccountInfo = (account) => {
  if (!account) return 'External Account';
  return `${account.type} (ID: ${account.id})`;
};

const getTransactionClass = (transaction) => {
  const isUserSender = isCurrentUserSender(transaction);
  return {
    'transaction-row': true,
    'outgoing': isUserSender,
    'incoming': !isUserSender
  };
};

const getAmountClass = (transaction) => {
  const isUserSender = isCurrentUserSender(transaction);
  return {
    'transaction-amount': true,
    'negative': isUserSender,
    'positive': !isUserSender
  };
};

const isCurrentUserSender = (transaction) => {
  // Check if the current user is the sender
  if (!transaction.senderAccount) return false;
  
  // Simply check if the sender account belongs to the current user's accounts
  // We know all transactions in our list are related to the current user already
  return transaction.senderAccount.user && 
         transaction.senderAccount.user.id === auth.user.id;
};

onMounted(() => {
  fetchTransactions();
});
</script>

<style scoped>
.transaction-history-page {
  padding: 40px;
  background-color: #fff;
}

.page-title {
  font-size: 2rem;
  margin-bottom: 20px;
  color: #333;
}

.filter-controls {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-controls select {
  padding: 8px;
  border-radius: 6px;
  border: 1px solid #ccc;
  background-color: #fff;
}

.loading,
.error-message,
.no-transactions {
  padding: 20px;
  text-align: center;
  color: #666;
  background-color: #f5f5f5;
  border-radius: 8px;
  margin-top: 20px;
}

.error-message {
  color: #e74c3c;
  background-color: #fde8e6;
}

.transactions-container {
  margin-top: 20px;
  overflow-x: auto;
}

.transactions-table {
  width: 100%;
  border-collapse: collapse;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.transactions-table th,
.transactions-table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.transactions-table th {
  background-color: #f5f5f5;
  font-weight: bold;
  color: #333;
}

.transactions-table tr:last-child td {
  border-bottom: none;
}

.transaction-row {
  transition: background-color 0.2s;
}

.transaction-row:hover {
  background-color: #f9f9f9;
}

.transaction-row.outgoing {
  background-color: #f9f9f9;
}

.transaction-row.incoming {
  background-color: #f0f9f0;
}

.transaction-amount {
  font-weight: bold;
}

.transaction-amount.positive {
  color: #2ecc71;
}

.transaction-amount.negative {
  color: #e74c3c;
}
</style>