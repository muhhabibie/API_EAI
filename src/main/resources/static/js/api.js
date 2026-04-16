const API = 'http://localhost:8080/api';

const ApiService = {
    async fetchData(endpoint) {
        const res = await fetch(`${API}/${endpoint}`);
        if (!res.ok) throw new Error(`Gagal mengambil data ${endpoint}`);
        return await res.json();
    },

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

    async adjustStock(productId, amount) {
        return await fetch(`${API}/products/${productId}/adjustment?amount=${amount}`, { method: 'PATCH' });
    },

    async deleteProduct(productId) {
        return await fetch(`${API}/products/${productId}`, { method: 'DELETE' });
    }
};