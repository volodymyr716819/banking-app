<template>
  <div class="transaction-history">
    <div class="filters">
      <!-- Employee customer filter -->
      <div v-if="auth.isEmployee" class="filter">
        <label>Customer:</label>
        <select v-model="selectedUserId">
          <option value="">All Customers</option>
          <option v-for="user in customerList" :key="user.id" :value="user.id">
            {{ user.name }}
          </option>
        </select>
      </div>

      <!-- Date range -->
      <div class="filter">
        <label>From:</label>
        <input type="date" v-model="startDate" />
        <label>To:</label>
        <input type="date" v-model="endDate" />
      </div>
      
      <!-- Amount range -->
      <div class="filter">
        <label>Min €:</label>
        <input type="number" v-model="minAmount" min="0" step="0.01" />
        <label>Max €:</label>
        <input type="number" v-model="maxAmount" min="0" step="0.01" />
      </div>

      <button v-if="hasFilters" @click="clearFilters" class="clear-btn">
        Clear
      </button>
    </div>

    <p v-if="loading">Loading...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <table v-else-if="transactions.length" class="transaction-table">
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
        <tr v-for="tx in transactions" :key="tx.transactionId" :class="'tx-type-' + tx.transactionType.toLowerCase()">
          <td :data-type="tx.transactionType">{{ tx.transactionType }}</td>
          <td>{{ tx.fromAccountIban || '-' }}</td>
          <td>{{ tx.toAccountIban || '-' }}</td>
          <td>€{{ tx.amount }}</td>
          <td>{{ formatDate(tx.timestamp) }}</td>
          <td>{{ tx.description || '-' }}</td>
        </tr>
      </tbody>
    </table>

    <p v-else class="no-data">No transactions found.</p>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import api from '../lib/api';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const transactions = ref([]);
const loading = ref(false);
const error = ref('');

// Filters
const selectedUserId = ref('');
const startDate = ref('');
const endDate = ref('');
const minAmount = ref('');
const maxAmount = ref('');
const customerList = ref([]);

const hasFilters = computed(() => 
  selectedUserId.value || startDate.value || endDate.value || 
  minAmount.value || maxAmount.value
);

// Fetch transactions
const fetchTransactions = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    const params = new URLSearchParams();
    if (selectedUserId.value) params.append('userId', selectedUserId.value);
    if (startDate.value) params.append('startDate', startDate.value);
    if (endDate.value) params.append('endDate', endDate.value);
    if (minAmount.value) params.append('minAmount', minAmount.value);
    if (maxAmount.value) params.append('maxAmount', maxAmount.value);
    
    const response = await api.get(`/transactions/history?${params}`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    });
    
    transactions.value = response.data;
  } catch (err) {
    error.value = err.response?.data || 'Failed to load transactions';
  } finally {
    loading.value = false;
  }
};

// Fetch customer list for employees
const fetchCustomerList = async () => {
  if (!auth.isEmployee) return;
  
  try {
    const response = await api.get('/users/approved', {
      headers: { Authorization: `Bearer ${auth.token}` }
    });
    customerList.value = response.data;
  } catch (err) {
    console.error('Failed to load customers');
  }
};

const clearFilters = () => {
  selectedUserId.value = '';
  startDate.value = '';
  endDate.value = '';
  minAmount.value = '';
  maxAmount.value = '';
};

const formatDate = (timestamp) => {
  return new Date(timestamp).toLocaleDateString();
};

onMounted(() => {
  fetchCustomerList();
  fetchTransactions();
});

watch([selectedUserId, startDate, endDate, minAmount, maxAmount], fetchTransactions);
</script>

<style scoped>
.transaction-history {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

h1 {
  color: #2c3e50;
  margin-bottom: 2rem;
}

.filters {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 2rem;
}

.filter {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter label {
  font-weight: 600;
  color: #495057;
  margin-right: 0.5rem;
}

.filter input, .filter select {
  padding: 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  background: white;
}

.filter input:focus, .filter select:focus {
  outline: none;
  border-color: #2b6cb0;
  box-shadow: 0 0 0 3px rgba(43,108,176,0.1);
}

.clear-btn {
  background: #dc3545;
  color: white;
  border: none;
  padding: 0.5rem 1.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  transition: background 0.2s;
}

.clear-btn:hover {
  background: #c82333;
}

.error {
  background: #f8d7da;
  color: #721c24;
  padding: 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.transaction-table {
  width: 100%;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border-collapse: collapse;
}

.transaction-table th {
  background: #2b6cb0;
  color: white;
  padding: 1rem;
  text-align: left;
  font-weight: 600;
}

.transaction-table td {
  padding: 1rem;
  border-bottom: 1px solid #e9ecef;
  text-align: left;
}

.transaction-table tr:hover {
  background: #f8f9fa;
}

.transaction-table tr:last-child td {
  border-bottom: none;
}

/* Transaction type colors */
tr td:first-child {
  font-weight: 600;
}

.tx-type-transfer {
  border-left: 4px solid #17a2b8;
}

.tx-type-deposit {
  border-left: 4px solid #28a745;
}

.tx-type-withdraw {
  border-left: 4px solid #dc3545;
}

.no-data {
  background: #f8f9fa;
  padding: 3rem;
  text-align: center;
  border-radius: 8px;
  color: #6c757d;
}

@media (max-width: 768px) {
  .transaction-history {
    padding: 1rem;
  }
  
  .filters {
    flex-direction: column;
  }
  
  .filter {
    width: 100%;
    margin-bottom: 1rem;
  }
  
  .transaction-table {
    font-size: 0.875rem;
  }
  
  .transaction-table th,
  .transaction-table td {
    padding: 0.5rem;
  }
}
</style>