<template>
  <div class="transaction-history">
    <h1>Transaction History</h1>

    <div class="filter-section">
      <label>Filter by Account Type:</label>
      <select v-model="selectedType">
        <option value="">All</option>
        <option value="checking">Checking</option>
        <option value="savings">Savings</option>
      </select>
    </div>

    <p v-if="message" class="status-message">{{ message }}</p>

    <table v-if="getFilteredTransactions().length" class="transaction-table">
      <thead>
        <tr>
          <th>Type</th>
          <th>From Account</th>
          <th>To Account</th>
          <th>Amount</th>
          <th>Date</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="tx in getFilteredTransactions()" :key="tx.id">
          <td>{{ tx.type }}</td>
          <td>{{ tx.fromAccount ? tx.fromAccount.id : '-' }}</td>
          <td>{{ tx.toAccount ? tx.toAccount.id : '-' }}</td>
          <td>{{ formatAmount(tx.amount) }}</td>
          <td>{{ formatDate(tx.timestamp) }}</td>
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
    const selectedType = ref('')
    const message = ref('')

    const fetchTransactions = async () => {
      try {
        const response = await axios.get(`http://localhost:8080/api/transactions/user/${auth.user.id}`, {
          headers: {
            Authorization: `Bearer ${auth.token}`
          }
        })
        transactions.value = response.data
      } catch (error) {
        message.value = 'Failed to load transactions.'
      }
    }

    onMounted(() => {
      fetchTransactions()
    })

    const getFilteredTransactions = () => {
      if (!selectedType.value) {
        return transactions.value
      }

      return transactions.value.filter(tx => {
        const from = tx.fromAccount ? tx.fromAccount.type : ''
        const to = tx.toAccount ? tx.toAccount.type : ''
        return from === selectedType.value || to === selectedType.value
      })
    }

    const formatAmount = (amount) => {
      return '€' + Number(amount).toFixed(2)
    }

    const formatDate = (timestamp) => {
      const date = new Date(timestamp)
      return date.toLocaleString()
    }

    return {
      selectedType,
      transactions,
      message,
      getFilteredTransactions,
      formatAmount,
      formatDate
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
}

.status-message {
  color: #2b6cb0;
  margin-bottom: 10px;
}

.transaction-table {
  width: 100%;
  border-collapse: collapse;
}

.transaction-table th,
.transaction-table td {
  padding: 10px;
  border: 1px solid #ccc;
}

.no-data {
  margin-top: 20px;
  font-style: italic;
  color: #777;
}
</style>
