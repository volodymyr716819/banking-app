<template>
    <div class="approve-page">
      <h1 class="page-title">Pending Accounts for Approval</h1>
  
      <div v-if="message" class="status-message">{{ message }}</div>
  
      <table v-if="accounts.length" class="accounts-table">
        <thead>
          <tr>
            <th>Account ID</th>
            <th>Type</th>
            <th>User ID</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="account in accounts" :key="account.id">
            <td>{{ account.id }}</td>
            <td>{{ account.type }}</td>
            <td>{{ account.user?.id }}</td>
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
  import axios from 'axios';
  
  const accounts = ref([]);
  const message = ref('');
  
  const fetchPendingAccounts = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/accounts/pending', {
        withCredentials: true
      });
      accounts.value = response.data;
    } catch (err) {
      message.value = 'Failed to load pending accounts.';
    }
  };
  
  const approveAccount = async (accountId) => {
    try {
      await axios.put(`http://localhost:8080/api/accounts/${accountId}/approve`, {}, {
        withCredentials: true
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
  </style>  