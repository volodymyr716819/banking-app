<template>
    <div class="customers-page">
        <h1 class="page-title">All Customers</h1>

        <div v-if="message" class="status-message">{{ message }}</div>

        <table v-if="customers.length" class="customers-table">
            <thead>
                <tr>
                    <th>Customer ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="customer in customers" :key="customer.id">
                    <td>{{ customer.id }}</td>
                    <td>{{ customer.name }}</td>
                    <td>{{ customer.email }}</td>
                    <td>{{ customer.role }}</td>
                </tr>
            </tbody>
        </table>

        <p v-else class="no-data">No customers found.</p>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const customers = ref([]);
const message = ref('');

const fetchCustomers = async () => {
    try {
        const res = await axios.get('http://localhost:8080/api/users/customers', {
            headers: {
                Authorization: `Bearer ${auth.token}`
            }
        });
        customers.value = res.data;
    } catch (err) {
        message.value = 'Failed to fetch customers.';
        console.error(err);
    }
};

onMounted(fetchCustomers);
</script>

<style scoped>
.customers-page {
    padding: 40px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
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
    padding: 10px;
    background-color: rgba(43, 108, 176, 0.1);
    border-radius: 4px;
}

.customers-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 20px;
}

.customers-table th,
.customers-table td {
    padding: 12px 15px;
    border: 1px solid #ddd;
    text-align: left;
}

.customers-table th {
    background-color: #f8f9fa;
    font-weight: 600;
}

.customers-table tr:nth-child(even) {
    background-color: #f9f9f9;
}

.customers-table tr:hover {
    background-color: #f1f7fd;
}

.no-data {
    color: #777;
    font-style: italic;
    text-align: center;
    padding: 20px;
}
</style>