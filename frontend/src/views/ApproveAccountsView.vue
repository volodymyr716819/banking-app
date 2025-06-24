<template>
    <div class="approve-page">
      <h1 class="page-title">Pending Accounts for Approval</h1>
  
      <div v-if="message" class="status-message">{{ message }}</div>
  
      <table v-if="accounts.length" class="accounts-table">
        <thead>
          <tr>
            <th>Account ID</th>
            <th>IBAN</th>
            <th>Type</th>
            <th>User Email</th>
            <th>Balance</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="account in accounts" :key="account.id">
            <td>{{ account.id }}</td>
            <td class="iban-cell">{{ formatIban(account.iban) }}</td>
            <td>{{ account.type }}</td>
            <td>{{ account.ownerEmail }}</td>
            <td>€{{ account.balance ? account.balance.toFixed(2) : '0.00' }}</td>
            <td>{{ formatDate(account.createdAt) }}</td>
            <td>
              <button @click="approveAccount(account.id)" class="approve-button">Approve</button>
            </td>
          </tr>
        </tbody>
      </table>
  
      <p v-else class="no-data">No pending accounts.</p>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import api from '../lib/api';
  import { useAuthStore } from '../store/auth';
  
  const auth = useAuthStore();
  const accounts = ref([]);
  const message = ref('');
  
  const formatIban = (iban) => {
    if (!iban) return '';
    return iban.replace(/(.{4})/g, "$1 ").trim();
  };
  
  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = date.toLocaleString('en-US', { month: 'short' });
    const year = date.getFullYear();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${day} ${month} ${year} at ${hours}:${minutes}`;
  };
  
  const fetchPendingAccounts = async () => {
    try {
      const response = await api.get('/accounts/pending', {
        headers: {
          Authorization: `Bearer ${auth.token}`
        }
      });
      accounts.value = response.data;
    } catch (err) {
      message.value = 'Failed to load pending accounts.';
    }
  };
  
  const approveAccount = async (accountId) => {
    try {
      await api.put(`/accounts/${accountId}/approve`, {}, {
        headers: {
          Authorization: `Bearer ${auth.token}`
        }
      });
      message.value = `Account ${accountId} approved.`;
      await fetchPendingAccounts();
    } catch (err) {
      message.value = 'Approval failed. You might not have permission.';
    }
  };
  
  onMounted(fetchPendingAccounts);
  </script>
  
  <style scoped>
  .approve-page {
    padding: 40px;
    background-color: #fff;
  }
  
  .page-title {
    font-size: 2rem;
    margin-bottom: 20px;
    color: #333;
  }
  
  .status-message {
    margin-bottom: 15px;
    color: #2b6cb0;
    font-weight: bold;
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
  
  .approve-button {
    background-color: #4CAF50;
    color: white;
    border: none;
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
  }
  
  .approve-button:hover {
    background-color: #45a049;
  }
  
  .no-data {
    color: #777;
    font-style: italic;
  }
  
  .iban-cell {
    font-family: monospace;
    letter-spacing: 0.05em;
  }
  
  .user-info-cell {
    font-size: 0.9em;
    line-height: 1.4;
  }
  
  .user-info-cell div {
    margin-bottom: 3px;
  }
  </style>  