<template>
    <div class="approve-users-page">
        <h1 class="page-title">Pending User Registrations</h1>

        <div v-if="message" class="status-message">{{ message }}</div>

        <table v-if="users.length" class="users-table">
            <thead>
                <tr>
                    <th>User ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="user in users" :key="user.id">
                    <td>{{ user.id }}</td>
                    <td>{{ user.name }}</td>
                    <td>{{ user.email }}</td>
                    <td>{{ user.role }}</td>
                    <td>
                        <button @click="approveUser(user.id)" class="approve-button">Approve</button>
                        <button class="bg-red-600 hover:bg-red-700 text-white px-2 py-1 rounded" @click="declineUser(user.id)">Decline</button>
                    </td>
                </tr>
            </tbody>
        </table>

        <p v-else class="no-data">No pending users.</p>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../lib/api';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const users = ref([]);
const message = ref('');

const fetchPendingUsers = async () => {
    try {
        const res = await api.get('/users/pending', { // move to .env
            headers: {
                Authorization: `Bearer ${auth.token}`
            }
        });
        users.value = res.data;
    } catch (err) {
        message.value = 'Failed to fetch pending users.';
    }
};

const approveUser = async (userId) => {
    try {
        await api.post(`/users/${userId}/approve`, {}, {
            headers: {
                Authorization: `Bearer ${auth.token}`
            }
        });
        message.value = `User ${userId} approved.`;
        await fetchPendingUsers();
    } catch (err) {
        message.value = 'Approval failed. You might not have permission.';
    }
};

const declineUser = async (userId) => {
    try {
        await api.post(
            `/users/${userId}/decline`,
            {},
            {
                headers: {
                    Authorization: `Bearer ${auth.token}`
                }
            }
        );
        message.value = `User ${userId} declined.`;
        await fetchPendingUsers();
    } catch (err) {
        message.value = 'Decline failed. You might not have permission.';
    }
};

onMounted(fetchPendingUsers);
</script>

<style scoped>
.approve-users-page {
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