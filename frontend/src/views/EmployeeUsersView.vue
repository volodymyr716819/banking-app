<template>
    <div class="employee-users-page">
      <h1 class="page-title">Approved Users</h1>
  
      <table v-if="users.length" class="users-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.name }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.role }}</td>
            <td>
              <button class="edit-btn" >Edit</button>
              <button class="delete-btn" >Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
  
      <p v-else class="no-data">No approved users found.</p>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
  import api from '../lib/api';
  import { useAuthStore } from '../store/auth';
  
  const auth = useAuthStore();
  const users = ref([]);
  
  const fetchApprovedUsers = async () => {
    try {
      const res = await api.get('/users/approved', {
        headers: {
          Authorization: `Bearer ${auth.token}`
        }
      });
      users.value = res.data;
    } catch (err) {
      console.error('Failed to fetch users:', err);
    }
  };
  
  onMounted(fetchApprovedUsers);
  </script>
  
  <style scoped>
  .employee-users-page {
    padding: 40px;
    background-color: #fff;
  }
  
  .page-title {
    font-size: 2rem;
    margin-bottom: 20px;
    color: #333;
  }
  
  .users-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 20px;
  }
  
  .users-table th,
  .users-table td {
    padding: 10px;
    border: 1px solid #ddd;
    text-align: left;
  }
  
  .edit-btn,
  .delete-btn {
    padding: 5px 10px;
    margin-right: 5px;
    border: none;
    border-radius: 4px;
    cursor: not-allowed;
  }
  
  .edit-btn {
    background-color: #3498db;
    color: white;
  }
  
  .delete-btn {
    background-color: #e74c3c;
    color: white;
  }
  
  .no-data {
    color: #777;
    font-style: italic;
  }
  </style>  