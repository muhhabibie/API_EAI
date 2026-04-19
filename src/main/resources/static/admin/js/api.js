/**
 * EAI Admin API Client
 * Berfungsi untuk menangani semua request HTTP ke backend Spring Boot.
 */

const API_BASE_URL = 'http://localhost:8080/api';

// Helper function untuk mengirim log ke API Inspector di UI
function logToInspector(method, endpoint, responseData, requestPayload = null) {
    const reqBody = document.getElementById('reqBody');
    const resBody = document.getElementById('resBody');
    
    if (reqBody && resBody) {
        const fullUrl = `${API_BASE_URL}${endpoint}`;
        
        let curlCmd = `curl -X ${method} "${fullUrl}" \\\n-H "Content-Type: application/json"`;
        
        if (requestPayload) {
            curlCmd += ` \\\n-d '${JSON.stringify(requestPayload, null, 2)}'`;
        }

        reqBody.innerText = curlCmd;
        resBody.innerText = JSON.stringify(responseData, null, 2);
    }
}

const AdminAPI = {
    // ==========================================
    // 1. ORDER MANAGEMENT
    // ==========================================
async getOrders(silent = false) { // Tambahkan parameter
        try {
            const res = await fetch(`${API_BASE_URL}/orders`);
            const data = await res.json();
            // Jika tidak silent, baru laporkan ke Inspector
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/orders', data);
            return data;
        } catch (error) { return []; }
    },

    async updateOrderStatus(id, status) {
        try {
            const res = await fetch(`${API_BASE_URL}/orders/${id}/status?status=${status}`, { method: 'PUT' });
            const data = await res.json();
            logToInspector('PUT', `/orders/${id}/status?status=${status}`, data);
            return data;
        } catch (error) {
            console.error("Gagal update status order", error);
            throw error;
        }
    },

    // ==========================================
    // 2. SHIPPING & LOGISTICS
    // ==========================================
async getShipments(silent = false) {
        try {
            const res = await fetch(`${API_BASE_URL}/shipments`);
            if (!res.ok) return [];
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/shipments', data);
            return data;
        } catch (error) { return []; }
    },

    // ==========================================
    // 3. INVENTORY CORE (EAI)
    // ==========================================
async getReservations(silent = false) {
        try {
            const res = await fetch(`${API_BASE_URL}/inventory/reservations`);
            if (!res.ok) throw new Error("Endpoint belum tersedia");
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/inventory/reservations', data);
            return data;
        } catch (error) { return []; }
    },
    async updateProductStock(productId, quantityToAdd) {
        try {
            // Karena kita hanya menambah/mengurangi stok, asumsi backend punya endpoint khusus, 
            // ATAU kita ambil data lama lalu update. Di sini kita pakai simulasi pemanggilan yang ideal untuk EAI.
            // CATATAN: Anda mungkin perlu membuat endpoint PUT /api/products/{id}/stock di Spring Boot
            const res = await fetch(`${API_BASE_URL}/products/${productId}/stock?add=${quantityToAdd}`, { method: 'PUT' });
            if (!res.ok) throw new Error("Endpoint update stock mungkin belum siap");
            const data = await res.json();
            logToInspector('PUT', `/products/${productId}/stock?add=${quantityToAdd}`, data);
            return data;
        } catch (error) {
            console.error("Gagal update stok", error);
            throw error;
        }
    },

    // ==========================================
    // 4. MASTER DATA (Customers, Products, Categories)
    // ==========================================
    async getCustomers(silent = false) {
        try {
            const res = await fetch(`${API_BASE_URL}/customers`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/customers', data);
            return data;
        } catch (e) { return []; }
    },

    async getProducts(silent = false) {
        try {
            const res = await fetch(`${API_BASE_URL}/products`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/products', data);
            return data;
        } catch (e) { return []; }
    },

    async getCategories(silent = false) {
        try {
            const res = await fetch(`${API_BASE_URL}/categories`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/categories', data);
            return data;
        } catch (e) { return []; }
    },

    async createProduct(productData) {
        try {
            const res = await fetch(`${API_BASE_URL}/products`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(productData)
            });
            const data = await res.json();
            if(typeof logToInspector === 'function') logToInspector('POST', '/products', data, productData);
            return data;
        } catch (e) { throw e; }
    },

    async deleteProduct(id) {
        try {
            const res = await fetch(`${API_BASE_URL}/products/${id}`, { method: 'DELETE' });
            if(typeof logToInspector === 'function') logToInspector('DELETE', `/products/${id}`, { message: "Deleted" });
            return true;
        } catch (e) { throw e; }
    },

   async createCustomer(customerData) {
        try {
            const res = await fetch(`${API_BASE_URL}/customers`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(customerData)
            });
            
            if (!res.ok) throw new Error("Gagal mendaftarkan customer");
            
            const data = await res.json();
            
            // Mengirim data ke Inspector: Method, Endpoint, Response, dan Request Payload (customerData)
            if(typeof logToInspector === 'function') {
                logToInspector('POST', '/customers', data, customerData);
            }
            
            return data;
        } catch (e) { 
            console.error("EAI Error:", e);
            throw e; 
        }
    }

    
};