<template>
    <div class="approved-accounts-page">
        <h1 class="page-title">All Approved Accounts</h1>

        <div v-if="message" :class="['status-message', messageType]">{{ message }}</div>

        <table v-if="accounts.length" class="accounts-table">
            <thead>
                <tr>
                    <th>Account ID</th>
                    <th>Account Type</th>
                    <th>IBAN</th>
                    <th>Customer Name</th>
                    <th>Customer Email</th>
                    <th>Limits</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="account in accounts" :key="account.accountId">
                    <td>{{ account.accountId }}</td>
                    <td>{{ account.accountType }}</td>
                    <td class="iban">{{ account.iban }}</td>
                    <td>{{ account.userName }}</td>
                    <td>{{ account.userEmail }}</td>
                    <td>
                        <span v-if="account.limits">
                            <div><strong>Daily Transfer:</strong> €{{ account.limits.dailyTransferLimit }}</div>
                            <div><strong>Minimum Balance:</strong> €{{ account.limits.minimumBalanceLimit }}</div>
                        </span>
                        <span v-else>Loading limits...</span>
                    </td>
                    <td>
                        <button @click="openLimitsModal(account)" class="edit-button">Edit Limits</button>
                    </td>
                </tr>
            </tbody>
        </table>

        <p v-else class="no-data">No approved accounts found.</p>

        <!-- Modal for Editing Limits -->
        <div v-if="showLimitsModal" class="modal-overlay">
            <div class="modal-content">
                <h2>Edit Account Limits</h2>
                <p>Account ID: {{ selectedAccount.accountId }}</p>
                <p>Account Holder: {{ selectedAccount.userName }}</p>
                
                <div class="form-group">
                    <label for="dailyTransferLimit">Daily Transfer Limit ($):</label>
                    <input 
                        type="number" 
                        id="dailyTransferLimit" 
                        v-model="editLimits.dailyTransferLimit" 
                        min="0" 
                        step="0.01" 
                        class="form-control"
                    />
                </div>
                
                <div class="form-group">
                    <label for="minimumBalanceLimit">Minimum Balance Limit ($):</label>
                    <input 
                        type="number" 
                        id="minimumBalanceLimit" 
                        v-model="editLimits.minimumBalanceLimit" 
                        min="0" 
                        step="0.01" 
                        class="form-control"
                    />
                </div>
                
                <div class="modal-actions">
                    <button @click="saveLimits" class="save-button">Save</button>
                    <button @click="closeLimitsModal" class="cancel-button">Cancel</button>
                </div>
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
const message = ref('');
const messageType = ref('info');
const showLimitsModal = ref(false);
const selectedAccount = ref({});
const editLimits = ref({
    dailyTransferLimit: 0,
    minimumBalanceLimit: 0
});

const fetchApprovedAccounts = async () => {
    try {
        const res = await axios.get('http://localhost:8080/api/accounts/approved', {
            headers: {
                Authorization: `Bearer ${auth.token}`
            }
        });
        console.log('Received accounts data:', res.data);
        accounts.value = res.data;
        
        // Fetch limits for each account
        for (const account of accounts.value) {
            fetchAccountLimits(account.accountId);
        }
    } catch (err) {
        message.value = 'Failed to fetch approved accounts.';
        messageType.value = 'error';
        console.error(err);
    }
};

const fetchAccountLimits = async (accountId) => {
    try {
        const res = await axios.get(`http://localhost:8080/api/accounts/${accountId}/limits`, {
            headers: {
                Authorization: `Bearer ${auth.token}`
            }
        });
        
        // Find the account and add the limits
        const accountIndex = accounts.value.findIndex(a => a.accountId === accountId);
        if (accountIndex !== -1) {
            accounts.value[accountIndex].limits = res.data;
        }
    } catch (err) {
        console.error(`Failed to fetch limits for account ${accountId}:`, err);
    }
};

const openLimitsModal = (account) => {
    selectedAccount.value = {...account};
    editLimits.value = {
        dailyTransferLimit: account.limits ? account.limits.dailyTransferLimit : 10000,
        minimumBalanceLimit: account.limits ? account.limits.minimumBalanceLimit : 0
    };
    showLimitsModal.value = true;
};

const closeLimitsModal = () => {
    showLimitsModal.value = false;
};

const saveLimits = async () => {
    try {
        const limitsData = {
            accountId: selectedAccount.value.accountId,
            dailyTransferLimit: parseFloat(editLimits.value.dailyTransferLimit),
            minimumBalanceLimit: parseFloat(editLimits.value.minimumBalanceLimit)
        };
        
        await axios.put('http://localhost:8080/api/accounts/limits', limitsData, {
            headers: {
                Authorization: `Bearer ${auth.token}`
            }
        });
        
        message.value = 'Account limits updated successfully.';
        messageType.value = 'success';
        
        // Update the displayed limits
        const accountIndex = accounts.value.findIndex(a => a.accountId === selectedAccount.value.accountId);
        if (accountIndex !== -1) {
            accounts.value[accountIndex].limits = limitsData;
        }
        
        closeLimitsModal();
    } catch (err) {
        message.value = 'Failed to update account limits.';
        messageType.value = 'error';
        console.error(err);
    }
};

onMounted(fetchApprovedAccounts);
</script>

<style scoped>
.approved-accounts-page {
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
    font-weight: bold;
    padding: 10px;
    border-radius: 4px;
}

.status-message.info {
    color: #2b6cb0;
    background-color: rgba(43, 108, 176, 0.1);
}

.status-message.success {
    color: #2f855a;
    background-color: rgba(47, 133, 90, 0.1);
}

.status-message.error {
    color: #c53030;
    background-color: rgba(197, 48, 48, 0.1);
}

.accounts-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 20px;
}

.accounts-table th,
.accounts-table td {
    padding: 12px 15px;
    border: 1px solid #ddd;
    text-align: left;
}

.accounts-table th {
    background-color: #f8f9fa;
    font-weight: 600;
}

.accounts-table tr:nth-child(even) {
    background-color: #f9f9f9;
}

.accounts-table tr:hover {
    background-color: #f1f7fd;
}

.iban {
    font-family: monospace;
    letter-spacing: 1px;
}

.no-data {
    color: #777;
    font-style: italic;
    text-align: center;
    padding: 20px;
}

.edit-button {
    background-color: #4299e1;
    color: white;
    border: none;
    padding: 8px 12px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.9rem;
    transition: background-color 0.2s;
}

.edit-button:hover {
    background-color: #3182ce;
}

/* Modal Styles */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}

.modal-content {
    background-color: white;
    border-radius: 8px;
    padding: 25px;
    width: 100%;
    max-width: 500px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-content h2 {
    margin-top: 0;
    margin-bottom: 15px;
    color: #2d3748;
}

.form-group {
    margin-bottom: 20px;
}

.form-group label {
    display: block;
    margin-bottom: 5px;
    font-weight: 500;
    color: #4a5568;
}

.form-control {
    width: 100%;
    padding: 10px;
    border: 1px solid #e2e8f0;
    border-radius: 4px;
    font-size: 1rem;
}

.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 20px;
}

.save-button {
    background-color: #48bb78;
    color: white;
    border: none;
    padding: 10px 16px;
    border-radius: 4px;
    cursor: pointer;
    font-weight: 500;
    transition: background-color 0.2s;
}

.save-button:hover {
    background-color: #38a169;
}

.cancel-button {
    background-color: #e2e8f0;
    color: #4a5568;
    border: none;
    padding: 10px 16px;
    border-radius: 4px;
    cursor: pointer;
    font-weight: 500;
    transition: background-color 0.2s;
}

.cancel-button:hover {
    background-color: #cbd5e0;
}
</style>