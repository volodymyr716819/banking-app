<template>
  <div class="customers-view">
    <h2>Customer Management</h2>
    
    <table class="customer-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th>BSN</th>
          <th>Registered</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in customers" :key="user.id">
          <td>
            <input v-if="editingId === user.id" v-model="editForm.name" />
            <span v-else>{{ user.name }}</span>
          </td>
          <td>
            <input v-if="editingId === user.id" v-model="editForm.email" />
            <span v-else>{{ user.email }}</span>
          </td>
          <td>{{ user.bsn }}</td>
          <td>{{ formatDate(user.registrationDate) }}</td>
          <td>
            <button v-if="editingId === user.id" @click="saveUser(user.id)">Save</button>
            <button v-if="editingId === user.id" @click="cancelEdit">Cancel</button>
            <button v-else @click="startEdit(user)">Edit</button>
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
</style>