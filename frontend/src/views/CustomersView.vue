<template>
  <div class="customers-view">
    <h2>Approved Users</h2>
    
    <table class="customer-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Email</th>
          <th>BSN</th>
          <th>Role</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in customers" :key="user.id">
          <td>{{ user.id }}</td>
          <td>
            <input v-if="editingId === user.id" v-model="editForm.name" />
            <span v-else>{{ user.name }}</span>
          </td>
          <td>
            <input v-if="editingId === user.id" v-model="editForm.email" />
            <span v-else>{{ user.email }}</span>
          </td>
          <td>{{ user.bsn }}</td>
          <td>{{ user.role }}</td>
          <td>
            <button v-if="editingId === user.id" @click="saveUser(user.id)" class="save-btn">Save</button>
            <button v-if="editingId === user.id" @click="cancelEdit" class="cancel-btn">Cancel</button>
            <button v-else @click="startEdit(user)" class="edit-btn">Edit</button>
            <button @click="deleteUser(user.id)" class="delete-btn">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../lib/api';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const customers = ref([]);
const editingId = ref(null);
const editForm = ref({ name: '', email: '' });

const fetchCustomers = async () => {
  try {
    const response = await api.get('/users/approved', {
      headers: { Authorization: `Bearer ${auth.token}` }
    });
    customers.value = response.data;
  } catch (error) {
    console.error('Failed to load customers');
  }
};

const startEdit = (user) => {
  editingId.value = user.id;
  editForm.value = { name: user.name, email: user.email };
};

const saveUser = async (id) => {
  try {
    await api.put(`/users/${id}`, editForm.value, {
      headers: { Authorization: `Bearer ${auth.token}` }
    });
    await fetchCustomers();
    editingId.value = null;
  } catch (error) {
    alert('Failed to update user');
  }
};

const cancelEdit = () => {
  editingId.value = null;
};

const formatDate = (date) => {
  return new Date(date).toLocaleDateString();
};

const deleteUser = async (id) => {
  if (!confirm('Are you sure you want to delete this user?')) return;
  
  try {
    await api.delete(`/users/${id}`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    });
    await fetchCustomers();
  } catch (error) {
    alert(error.response?.data || 'Failed to delete user');
  }
};

onMounted(fetchCustomers);
</script>

<style scoped>
.customer-table {
  width: 100%;
  border-collapse: collapse;
}
.customer-table th, .customer-table td {
  padding: 0.75rem;
  border: 1px solid #ddd;
  text-align: left;
}
.customer-table input {
  width: 100%;
  padding: 0.25rem;
}
.edit-btn, .save-btn {
  background: #3498db;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
  cursor: pointer;
  border-radius: 4px;
}
.delete-btn, .cancel-btn {
  background: #e74c3c;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  cursor: pointer;
  border-radius: 4px;
}
</style>