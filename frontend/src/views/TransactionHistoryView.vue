<template>
  <div class="transaction-history">
    <h1>Transaction History</h1>

    <div class="filter-section">
      <label>Filter by Transaction Type:</label>
      <select v-model="selectedTransactionType">
        <option value="">All</option>
        <option value="TRANSFER">Transfers</option>
        <option value="DEPOSIT">Deposits</option>
        <option value="WITHDRAW">Withdrawals</option>
      </select>
    </div>

    <p v-if="message" class="status-message">{{ message }}</p>

    <table v-if="getFilteredTransactions().length" class="transaction-table">
      <thead>
        <tr>
          <th>Type</th>
          <th>From</th>
          <th>To</th>
          <th>Amount</th>
          <th>Date</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(tx, index) in getFilteredTransactions()" :key="index" 
            :class="getTransactionTypeClass(tx.transactionType)">
          <td>{{ formatTransactionType(tx.transactionType) }}</td>
          <td>
            <div v-if="tx.fromAccountIban">
              <div class="account-iban">{{ formatIban(tx.fromAccountIban) }}</div>
              <div class="account-holder">{{ tx.fromAccountHolderName }}</div>
            </div>
            <span v-else>-</span>
          </td>
          <td>
            <div v-if="tx.toAccountIban">
              <div class="account-iban">{{ formatIban(tx.toAccountIban) }}</div>
              <div class="account-holder">{{ tx.toAccountHolderName }}</div>
            </div>
            <span v-else>-</span>
          </td>
          <td class="amount">{{ formatAmount(tx.amount) }}</td>
          <td>{{ formatDate(tx.timestamp) }}</td>
          <td>{{ tx.description || '-' }}</td>
        </tr>
      </tbody>
    </table>

    <p v-else class="no-data">No transactions found.</p>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useAuthStore } from '../store/auth'

export default {
  setup() {
    const auth = useAuthStore()
    const transactions = ref([])
    const selectedTransactionType = ref('')
    const message = ref('')

    const fetchTransactions = async () => {
      if (!auth.user || !auth.user.id) {
        message.value = 'User information not available. Please log in again.';
        return;
      }
      
      message.value = 'Loading transactions...';
      
      try {
        const response = await axios.get(`http://localhost:8080/api/transactions/user/${auth.user.id}`, {
          headers: {
            Authorization: `Bearer ${auth.token}`
          }
        });
        
        if (response.data && Array.isArray(response.data)) {
          transactions.value = response.data;
          message.value = transactions.value.length > 0 ? '' : 'No transactions found.';
        } else {
          message.value = 'Invalid response format.';
          transactions.value = [];
        }
      } catch (error) {
        console.error('Transaction fetch error:', error);
        
        if (error.response) {
          // Server returned error code
          if (error.response.status === 403) {
            message.value = 'Access denied. You may not have permission to view these transactions.';
          } else {
            message.value = `Error: ${error.response.data || 'Failed to load transactions'}`;
          }
        } else if (error.request) {
          // Request made but no response
          message.value = 'Unable to reach the server. Please check your connection.';
        } else {
          // Request setup error
          message.value = 'Failed to load transactions.';
        }
        
        transactions.value = [];
      }
    }

    onMounted(() => {
      fetchTransactions()
    })

    const getFilteredTransactions = () => {
      if (!selectedTransactionType.value) {
        return transactions.value
      }

      return transactions.value.filter(tx => 
        tx.transactionType === selectedTransactionType.value
      )
    }

    const formatAmount = (amount) => {
      return '€' + Number(amount).toFixed(2)
    }

    const formatDate = (timestamp) => {
      const date = new Date(timestamp)
      // Format: 'Jan 01, 2025 14:30'
      return new Intl.DateTimeFormat('en-GB', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }).format(date)
    }
    
    const formatIban = (iban) => {
      // Format IBAN with spaces for readability: NL12 BANK 0123 4567 89
      return iban.replace(/(.{4})/g, '$1 ').trim()
    }
    
    const formatTransactionType = (type) => {
      switch(type) {
        case 'TRANSFER': 
          return 'Transfer'
        case 'DEPOSIT': 
          return 'Deposit'
        case 'WITHDRAW': 
          return 'Withdrawal'
        default: 
          return type
      }
    }
    
    const getTransactionTypeClass = (type) => {
      return `transaction-${type.toLowerCase()}`
    }

    return {
      selectedTransactionType,
      transactions,
      message,
      getFilteredTransactions,
      formatAmount,
      formatDate,
      formatIban,
      formatTransactionType,
      getTransactionTypeClass
    }
  }
}
</script>

<style scoped>
.transaction-history {
  padding: 40px;
  font-family: 'Segoe UI', sans-serif;
}

h1 {
  font-size: 2rem;
  margin-bottom: 20px;
}

.filter-section {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-section select {
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
}

.status-message {
  color: #2b6cb0;
  margin-bottom: 10px;
}

.transaction-table {
  width: 100%;
  border-collapse: collapse;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.transaction-table th {
  background-color: #f1f5f9;
  font-weight: 600;
  text-align: left;
  padding: 12px;
  border-bottom: 2px solid #e2e8f0;
}

.transaction-table td {
  padding: 12px;
  border-bottom: 1px solid #e2e8f0;
  vertical-align: top;
}

/* Transaction type styling */
.transaction-transfer {
  background-color: #f8fafc;
}

.transaction-deposit {
  background-color: #f0fdf4;
}

.transaction-withdraw {
  background-color: #fef2f2;
}

/* Hover effect */
.transaction-table tr:hover {
  background-color: #edf2f7;
}

.account-iban {
  font-family: monospace;
  font-size: 0.9rem;
}

.account-holder {
  font-size: 0.85rem;
  color: #64748b;
  margin-top: 4px;
}

.amount {
  font-weight: 600;
  text-align: right;
}

.no-data {
  margin-top: 20px;
  font-style: italic;
  color: #777;
  text-align: center;
  padding: 20px;
  background-color: #f8fafc;
  border-radius: 8px;
}
</style>
