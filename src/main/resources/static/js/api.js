const API = 'http://localhost:8080/api';

const ApiService = {
    // GET data
    async fetchData(endpoint) {
        const res = await fetch(`${API}/${endpoint}`);
        if (!res.ok) throw new Error(`Gagal mengambil data ${endpoint}`);
        return await res.json();
    },

    // ORDER
    async postOrder(customerId, payload) {
        return await fetch(`${API}/orders?customerId=${customerId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },

    async updateOrderStatus(orderId, status) {
        return await fetch(`${API}/orders/${orderId}/status?status=${status}`, { method: 'PUT' });
    },

    // PRODUCT
    async adjustStock(productId, amount) {
        return await fetch(`${API}/products/${productId}/adjustment?amount=${amount}`, { method: 'PATCH' });
    },

    async deleteProduct(productId) {
        return await fetch(`${API}/products/${productId}`, { method: 'DELETE' });
    },

    async createProduct(payload) {
        return await fetch(`${API}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },

    async updateProduct(productId, payload) {
        return await fetch(`${API}/products/${productId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },

    // CUSTOMER
    async createCustomer(payload) {
        return await fetch(`${API}/customers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },

    async updateCustomer(customerId, payload) {
        return await fetch(`${API}/customers/${customerId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },

    async deleteCustomer(customerId) {
        return await fetch(`${API}/customers/${customerId}`, { method: 'DELETE' });
    }
};