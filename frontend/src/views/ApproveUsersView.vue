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
                    <th>BSN</th>
                    <th>Role</th>
                    <th>Registration Date</th>
                    <th>Action</th>
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
                        <button @click="approveUser(user.id)" class="approve-button">Approve</button>
                        <button class="decline-button" @click="declineUser(user.id)">Decline</button>
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

const fetchPendingUsers = async () => {
    try {
        const res = await api.get('/users/pending', {
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

.decline-button {
    background-color: #f44336;
    color: white;
    border: none;
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
    margin-left: 8px;
}

.decline-button:hover {
    background-color: #d32f2f;
}

.no-data {
    color: #777;
    font-style: italic;
}
</style>