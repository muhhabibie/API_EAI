/**
 * EAI Admin API Client - Microservices Integration
 * Menghubungkan ke 6 Backend Microservices (Port 8081-8086)
 */

// Microservices URLs - Routed through API Gateway (Port 8080)
const API_BASE = {
  auth: '/api',
  product: '/api',
  customer: '/api',
  order: '/api',
  inventory: '/api',
  shipping: '/api',
  payment: '/api'
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
                body: JSON.stringify({ email: username, password })
            });
            
            if (!res.ok) {
                throw new Error(`Login failed: ${res.status}`);
            }
            
            const data = await res.json();
            
            const token = data.data ? data.data.token : data.token;
            const usernameVal = data.data ? (data.data.username || data.data.email) : (data.username || username);
            
            if (token) {
                // Simpan token ke localStorage
                localStorage.setItem('token', token);
                localStorage.setItem('username', usernameVal || username);
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
            const list = data.data || data;
            return Array.isArray(list) ? list : [];
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
            return data.data || data;
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
            const list = data.data || data;
            return Array.isArray(list) ? list : [];
        } catch (error) { 
            console.error("Error getShipments:", error);
            return []; 
        }
    },

    async getShipmentByOrder(orderId) {
        try {
            const res = await fetchWithToken(`${API_BASE.shipping}/shipments/order/${orderId}`);
            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || `HTTP ${res.status}`);
            }
            const data = await res.json();
            if(typeof logToInspector === 'function') logToInspector('GET', `/shipments/order/${orderId}`, data);
            return data.data || data;
        } catch (error) {
            console.error("Error getShipmentByOrder:", error);
            throw error;
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
            const list = data.data || data;
            return Array.isArray(list) ? list : [];
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
            const list = data.data || data;
            return Array.isArray(list) ? list : [];
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
            const list = data.data || data;
            return Array.isArray(list) ? list : [];
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
            const list = data.data || data;
            return Array.isArray(list) ? list : [];
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
    },

    async updateProductStock(id, amount) {
        try {
            const res = await fetchWithToken(`${API_BASE.product}/products/${id}/adjustment?amount=${amount}`, {
                method: 'POST'
            });
            
            if (!res.ok) throw new Error("Gagal memperbarui stok produk");
            
            const data = await res.json();
            
            if(typeof logToInspector === 'function') {
                logToInspector('POST', `/products/${id}/adjustment?amount=${amount}`, data);
            }
            
            return data.data || data;
        } catch (e) {
            console.error("Error updateProductStock:", e);
            throw e;
        }
    },

    async processPayment(orderId, method = 'BALANCE') {
        try {
            const payload = { orderId, method };
            const res = await fetchWithToken(`${API_BASE.payment}/payments`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || `HTTP ${res.status}`);
            }
            const data = await res.json();
            if(typeof logToInspector === 'function') logToInspector('POST', '/payments', data, payload);
            return data.data || data;
        } catch (e) {
            console.error("Error processPayment:", e);
            throw e;
        }
    },

    async createShipment(orderId, courierName, receiverName, deliveryAddress, shippingFee = 10000) {
        try {
            const payload = { orderId, courierName, receiverName, deliveryAddress, shippingFee };
            const res = await fetchWithToken(`${API_BASE.shipping}/shipments`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || `HTTP ${res.status}`);
            }
            const data = await res.json();
            if(typeof logToInspector === 'function') logToInspector('POST', '/shipments', data, payload);
            return data.data || data;
        } catch (e) {
            console.error("Error createShipment:", e);
            throw e;
        }
    },

    async updateShipmentStatus(id, status) {
        try {
            const res = await fetchWithToken(`${API_BASE.shipping}/shipments/${id}/status?status=${status}`, {
                method: 'PUT'
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if(typeof logToInspector === 'function') logToInspector('PUT', `/shipments/${id}/status?status=${status}`, data);
            return data.data || data;
        } catch (error) {
            console.error("Gagal update status shipment", error);
            throw error;
        }
    },

    async cancelOrder(id) {
        try {
            const res = await fetchWithToken(`${API_BASE.order}/orders/${id}/cancel`, {
                method: 'POST'
            });
            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || `HTTP ${res.status}`);
            }
            const data = await res.json();
            if(typeof logToInspector === 'function') logToInspector('POST', `/orders/${id}/cancel`, data);
            return data.data || data;
        } catch (e) {
            console.error("Error cancelOrder:", e);
            throw e;
        }
    },

    async cancelPaidOrder(id, reason) {
        try {
            const res = await fetchWithToken(`${API_BASE.order}/orders/${id}/cancel-after-payment`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ reason })
            });
            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                throw new Error(errData.message || `HTTP ${res.status}`);
            }
            const data = await res.json();
            if(typeof logToInspector === 'function') logToInspector('PATCH', `/orders/${id}/cancel-after-payment`, data);
            return data.data || data;
        } catch (e) {
            console.error("Error cancelPaidOrder:", e);
            throw e;
        }
    }
};
