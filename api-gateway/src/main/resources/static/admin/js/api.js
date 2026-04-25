/**
 * EAI Admin API Client - Microservices Integration
 * Menghubungkan ke 6 Backend Microservices (Port 8081-8086)
 */

// Microservices URLs
const API_BASE = {
  auth: 'http://localhost:8081/api',
  product: 'http://localhost:8082/api',
  customer: 'http://localhost:8083/api',
  order: 'http://localhost:8084/api',
  inventory: 'http://localhost:8085/api',
  shipping: 'http://localhost:8086/api'
};

// Get JWT Token dari localStorage
function getToken() {
  return localStorage.getItem('token') || '';
}

// Helper untuk fetch dengan JWT token
async function fetchWithToken(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers
  };
  
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  return fetch(url, { ...options, headers });
}

const API_BASE_URL = '/api'; // Keep untuk backward compatibility dengan logToInspector

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
    // 0. AUTHENTICATION (Port 8081)
    // ==========================================
    async login(username, password) {
        try {
            const res = await fetch(`${API_BASE.auth}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });
            
            if (!res.ok) {
                throw new Error(`Login failed: ${res.status}`);
            }
            
            const data = await res.json();
            
            if (data.token) {
                // Simpan token ke localStorage
                localStorage.setItem('token', data.token);
                localStorage.setItem('username', data.username || username);
                return data;
            } else {
                throw new Error("Token tidak diterima dari server");
            }
        } catch (error) {
            console.error("Login error:", error);
            throw error;
        }
    },

    // ==========================================
    // 1. ORDER MANAGEMENT (Port 8084)
    // ==========================================
    async getOrders(silent = false) {
        try {
            const res = await fetchWithToken(`${API_BASE.order}/orders`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/orders', data);
            return Array.isArray(data) ? data : [];
        } catch (error) { 
            console.error("Error getOrders:", error);
            return []; 
        }
    },

    async updateOrderStatus(id, status) {
        try {
            const res = await fetchWithToken(`${API_BASE.order}/orders/${id}/status?status=${status}`, { method: 'PUT' });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            logToInspector('PUT', `/orders/${id}/status?status=${status}`, data);
            return data;
        } catch (error) {
            console.error("Gagal update status order", error);
            throw error;
        }
    },

    // ==========================================
    // 2. SHIPPING & LOGISTICS (Port 8086)
    // ==========================================
    async getShipments(silent = false) {
        try {
            const res = await fetchWithToken(`${API_BASE.shipping}/shipments`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/shipments', data);
            return Array.isArray(data) ? data : [];
        } catch (error) { 
            console.error("Error getShipments:", error);
            return []; 
        }
    },

    // ==========================================
    // 3. INVENTORY (Port 8085)
    // ==========================================
    async getReservations(silent = false) {
        try {
            const res = await fetchWithToken(`${API_BASE.inventory}/inventory/reservations`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/inventory/reservations', data);
            return Array.isArray(data) ? data : [];
        } catch (error) { 
            console.error("Error getReservations:", error);
            return []; 
        }
    },

    // ==========================================
    // 4. MASTER DATA
    // ==========================================
    async getCustomers(silent = false) {
        try {
            const res = await fetchWithToken(`${API_BASE.customer}/customers`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/customers', data);
            return Array.isArray(data) ? data : [];
        } catch (e) { 
            console.error("Error getCustomers:", e);
            return []; 
        }
    },

    async getProducts(silent = false) {
        try {
            const res = await fetchWithToken(`${API_BASE.product}/products`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/products', data);
            return Array.isArray(data) ? data : [];
        } catch (e) { 
            console.error("Error getProducts:", e);
            return []; 
        }
    },

    async getCategories(silent = false) {
        try {
            const res = await fetchWithToken(`${API_BASE.product}/categories`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if(!silent && typeof logToInspector === 'function') logToInspector('GET', '/categories', data);
            return Array.isArray(data) ? data : [];
        } catch (e) { 
            console.error("Error getCategories:", e);
            return []; 
        }
    },

    async createProduct(productData) {
        try {
            const res = await fetchWithToken(`${API_BASE.product}/products`, {
                method: 'POST',
                body: JSON.stringify(productData)
            });
            const data = await res.json();
            if(typeof logToInspector === 'function') logToInspector('POST', '/products', data, productData);
            return data;
        } catch (e) { 
            console.error("Error createProduct:", e);
            throw e; 
        }
    },

    async deleteProduct(id) {
        try {
            const res = await fetchWithToken(`${API_BASE.product}/products/${id}`, { method: 'DELETE' });
            if(typeof logToInspector === 'function') logToInspector('DELETE', `/products/${id}`, { message: "Deleted" });
            return true;
        } catch (e) { 
            console.error("Error deleteProduct:", e);
            throw e; 
        }
    },

    async createCustomer(customerData) {
        try {
            const res = await fetchWithToken(`${API_BASE.customer}/customers`, {
                method: 'POST',
                body: JSON.stringify(customerData)
            });
            
            if (!res.ok) throw new Error("Gagal mendaftarkan customer");
            
            const data = await res.json();
            
            if(typeof logToInspector === 'function') {
                logToInspector('POST', '/customers', data, customerData);
            }
            
            return data;
        } catch (e) { 
            console.error("Error createCustomer:", e);
            throw e; 
        }
    }
};