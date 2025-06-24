<template>
  <div class="users-management-page">
      <h1 class="page-title">Users Management</h1>

      <div v-if="message" class="status-message" :class="messageType">{{ message }}</div>

      <table v-if="users.length" class="users-table">
          <thead>
              <tr>
                  <th>User ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>BSN</th>
                  <th>Role</th>
                  <th>Registration Date</th>
                  <th>Actions</th>
              </tr>
          </thead>
          <tbody>
              <tr v-for="user in users" :key="user.id">
                  <td>{{ user.id }}</td>
                  <td>{{ user.name }}</td>
                  <td>{{ user.email }}</td>
                  <td>{{ user.bsn || 'Not provided' }}</td>
                  <td>{{ user.role }}</td>
                  <td>{{ formatDate(user.registrationDate) }}</td>
                  <td>
                      <button @click="openEditModal(user)" class="edit-button">Edit</button>
                      <button @click="confirmDelete(user)" class="delete-button">Delete</button>
                  </td>
              </tr>
          </tbody>
      </table>

      <p v-else class="no-data">No approved customers found.</p>

      <!-- Edit Modal -->
      <div v-if="showEditModal" class="modal-overlay" @click="closeEditModal">
          <div class="modal-content" @click.stop>
              <h2>Edit User</h2>
              <form @submit.prevent="saveUser">
                  <div class="form-group">
                      <label for="editName">Name:</label>
                      <input 
                          type="text" 
                          id="editName" 
                          v-model="editForm.name" 
                          required 
                          class="form-input"
                      />
                  </div>
                  <div class="form-group">
                      <label for="editEmail">Email:</label>
                      <input 
                          type="email" 
                          id="editEmail" 
                          v-model="editForm.email" 
                          required 
                          class="form-input"
                      />
                  </div>
                  <div class="form-actions">
                      <button type="submit" class="save-button" :disabled="saving">
                          {{ saving ? 'Saving...' : 'Save' }}
                      </button>
                      <button type="button" @click="closeEditModal" class="cancel-button">Cancel</button>
                  </div>
              </form>
          </div>
      </div>

      <!-- Delete Confirmation Modal -->
      <div v-if="showDeleteModal" class="modal-overlay" @click="closeDeleteModal">
          <div class="modal-content" @click.stop>
              <h2>Confirm Delete</h2>
              <p>Are you sure you want to delete user <strong>{{ userToDelete.name }}</strong>?</p>
              <p class="warning-text">This action cannot be undone.</p>
              <div class="form-actions">
                  <button @click="deleteUser" class="confirm-delete-button" :disabled="deleting">
                      {{ deleting ? 'Deleting...' : 'Delete' }}
                  </button>
                  <button @click="closeDeleteModal" class="cancel-button">Cancel</button>
              </div>
          </div>
      </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../lib/api';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const users = ref([]);
const message = ref('');
const messageType = ref('');

// Edit modal state
const showEditModal = ref(false);
const editForm = ref({ id: null, name: '', email: '' });
const saving = ref(false);

// Delete modal state  
const showDeleteModal = ref(false);
const userToDelete = ref({});
const deleting = ref(false);

const formatDate = (dateString) => {
  if (!dateString) return 'Unknown';
  
  try {
      const date = new Date(dateString);
      
      if (isNaN(date.getTime())) {
          return 'Invalid date';
      }
      
      return new Intl.DateTimeFormat('en-GB', {
          year: 'numeric',
          month: 'short',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
      }).format(date);
  } catch (error) {
      return 'Date error';
  }
};

const fetchApprovedUsers = async () => {
  try {
      const res = await api.get('/users/approved', {
          headers: {
              Authorization: `Bearer ${auth.token}`
          }
      });
      
      const processedUsers = res.data.map(user => {
          if (!user.registrationDate) {
              user.registrationDate = new Date().toISOString();
          }
          return user;
      });
      
      users.value = processedUsers;
      messageType.value = '';
  } catch (err) {
      message.value = 'Failed to fetch approved users.';
      messageType.value = 'error';
  }
};

const openEditModal = (user) => {
  editForm.value = {
      id: user.id,
      name: user.name,
      email: user.email
  };
  showEditModal.value = true;
};

const closeEditModal = () => {
  showEditModal.value = false;
  editForm.value = { id: null, name: '', email: '' };
  saving.value = false;
};

const saveUser = async () => {
  if (!editForm.value.name.trim() || !editForm.value.email.trim()) {
      message.value = 'Name and email are required.';
      messageType.value = 'error';
      return;
  }

  saving.value = true;
  try {
      await api.put(`/users/${editForm.value.id}`, {
          name: editForm.value.name.trim(),
          email: editForm.value.email.trim()
      }, {
          headers: {
              Authorization: `Bearer ${auth.token}`
          }
      });
      
      message.value = 'User updated successfully.';
      messageType.value = 'success';
      closeEditModal();
      await fetchApprovedUsers();
  } catch (err) {
      message.value = err.response?.data?.message || 'Failed to update user.';
      messageType.value = 'error';
      saving.value = false;
  }
};

const confirmDelete = (user) => {
  userToDelete.value = user;
  showDeleteModal.value = true;
};

const closeDeleteModal = () => {
  showDeleteModal.value = false;
  userToDelete.value = {};
  deleting.value = false;
};

const deleteUser = async () => {
  deleting.value = true;
  try {
      await api.delete(`/users/${userToDelete.value.id}`, {
          headers: {
              Authorization: `Bearer ${auth.token}`
          }
      });
      
      message.value = `User ${userToDelete.value.name} deleted successfully.`;
      messageType.value = 'success';
      closeDeleteModal();
      await fetchApprovedUsers();
  } catch (err) {
      message.value = err.response?.data || 'Failed to delete user.';
      messageType.value = 'error';
      deleting.value = false;
  }
};

onMounted(fetchApprovedUsers);
</script>

<style scoped>
.users-management-page {
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
  font-weight: bold;
  padding: 10px;
  border-radius: 4px;
}

.status-message.success {
  color: #155724;
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
}

.status-message.error {
  color: #721c24;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
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

.users-table th {
  background-color: #f8f9fa;
  font-weight: bold;
}

.edit-button {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 8px;
}

.edit-button:hover {
  background-color: #0056b3;
}

.delete-button {
  background-color: #dc3545;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.delete-button:hover {
  background-color: #c82333;
}

.no-data {
  color: #777;
  font-style: italic;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #007bff;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.save-button {
  background-color: #28a745;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.save-button:hover:not(:disabled) {
  background-color: #218838;
}

.save-button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.cancel-button {
  background-color: #6c757d;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.cancel-button:hover {
  background-color: #5a6268;
}

.confirm-delete-button {
  background-color: #dc3545;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.confirm-delete-button:hover:not(:disabled) {
  background-color: #c82333;
}

.confirm-delete-button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.warning-text {
  color: #856404;
  font-style: italic;
  margin-bottom: 15px;
}
</style>