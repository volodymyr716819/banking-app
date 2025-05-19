<template>
    <div class="employee-accounts-page">
      <h1 class="page-title">Approved Accounts</h1>
  
      <table v-if="accounts.length" class="accounts-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Balance (€)</th>
            <th>Daily Limit (€)</th>
            <th>Absolute Limit (€)</th>
            <th>User ID</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="acc in accounts" :key="acc.id">
            <td>{{ acc.id }}</td>
            <td>{{ acc.type }}</td>
            <td>{{ acc.balance.toFixed(2) }}</td>
            <td>{{ acc.dailyLimit.toFixed(2) }}</td>
            <td>{{ acc.absoluteLimit.toFixed(2) }}</td>
            <td>{{ acc.userId }}</td>
            <td>
              <button class="edit-btn" @click="editAccount(acc)">Edit Limits</button>
              <button class="close-btn" @click="closeAccount(acc.id)">Close</button>
            </td>
          </tr>
        </tbody>
      </table>
  
      <p v-else class="no-data">No approved accounts found.</p>
  
      <div v-if="selectedAccount" class="modal">
        <h3>Edit Limits for Account {{ selectedAccount.id }}</h3>
        <label>Daily Limit (€):</label>
        <input type="number" v-model.number="selectedAccount.dailyLimit" />
  
        <label>Absolute Limit (€):</label>
        <input type="number" v-model.number="selectedAccount.absoluteLimit" />
  
        <div class="actions">
          <button @click="updateLimits">Save</button>
          <button @click="selectedAccount = null">Cancel</button>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import axios from 'axios';
  import { useAuthStore } from '../store/auth';
  
  const auth = useAuthStore();
  const accounts = ref([]);
  const selectedAccount = ref(null);
  
  const fetchApprovedAccounts = async () => {
    try {
      const res = await axios.get('http://localhost:8080/api/accounts/approved', {
        headers: {
          Authorization: `Bearer ${auth.token}`
        }
      });
      accounts.value = res.data.filter(account => !account.closed);
    } catch (err) {
      console.error('Failed to fetch approved accounts:', err);
    }
  };
  
  const editAccount = (account) => {
    selectedAccount.value = { ...account };
  };
  
  const updateLimits = async () => {
    try {
      await axios.put(`http://localhost:8080/api/accounts/${selectedAccount.value.id}/limits`, {
        dailyLimit: selectedAccount.value.dailyLimit,
        absoluteLimit: selectedAccount.value.absoluteLimit
      }, {
        headers: {
          Authorization: `Bearer ${auth.token}`
        }
      });
      await fetchApprovedAccounts();
      selectedAccount.value = null;
    } catch (err) {
      console.error('Failed to update account limits:', err);
    }
  };
  
  const closeAccount = async (accountId) => {
    try {
        await axios.put(`http://localhost:8080/api/accounts/${accountId}/close`, {}, {
        headers: {
          Authorization: `Bearer ${auth.token}`
        }
      });
      await fetchApprovedAccounts();
    } catch (err) {
      console.error('Failed to close account:', err);
    }
  };
  
  onMounted(fetchApprovedAccounts);
  </script>
  
  <style scoped>
  .employee-accounts-page {
    padding: 40px;
    background-color: #fff;
  }
  
  .page-title {
    font-size: 2rem;
    margin-bottom: 20px;
    color: #333;
  }
  
  .accounts-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 20px;
  }
  
  .accounts-table th,
  .accounts-table td {
    padding: 10px;
    border: 1px solid #ddd;
    text-align: left;
  }
  
  .edit-btn {
    background-color: #3498db;
    color: white;
    border: none;
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
    margin-right: 6px;
  }
  
  .close-btn {
    background-color: #e74c3c;
    color: white;
    border: none;
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
  }
  
  .modal {
    position: fixed;
    top: 20%;
    left: 50%;
    transform: translateX(-50%);
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  }
  
  .modal label {
    display: block;
    margin-top: 10px;
  }
  
  .modal input {
    width: 100%;
    padding: 8px;
    margin-top: 5px;
    margin-bottom: 15px;
    border: 1px solid #ccc;
    border-radius: 4px;
  }
  
  .actions {
    display: flex;
    justify-content: space-between;
  }
  
  .actions button {
    padding: 8px 16px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
  
  .actions button:first-child {
    background-color: #2ecc71;
    color: white;
  }
  
  .actions button:last-child {
    background-color: #e74c3c;
    color: white;
  }
  
  .no-data {
    color: #777;
    font-style: italic;
  }
  </style>  