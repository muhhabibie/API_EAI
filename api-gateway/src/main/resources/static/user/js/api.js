/**
 * EAI User API Client - Microservices Integration
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

// User API Functions
const UserAPI = {
  // ==========================================
  // AUTHENTICATION (Port 8081)
  // ==========================================
  async login(email, password) {
    try {
      const response = await fetch(`${API_BASE.auth}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Login gagal');
      }

      const data = await response.json();
      localStorage.setItem('token', data.data ? data.data.token : data.token);
      return data;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  },

  // ==========================================
  // CUSTOMER (Port 8083)
  // ==========================================
  async getCustomerByEmail(email) {
    try {
      const response = await fetchWithToken(`${API_BASE.customer}/customers/by-email?email=${encodeURIComponent(email)}`);
      if (!response.ok) return null;
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error getCustomerByEmail:', error);
      return null;
    }
  },

  async getCustomerById(id) {
    try {
      const response = await fetchWithToken(`${API_BASE.customer}/customers/${id}`);
      if (!response.ok) return null;
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error getCustomerById:', error);
      return null;
    }
  },

  async addBalance(id, amount) {
    try {
      const response = await fetchWithToken(`${API_BASE.customer}/customers/${id}/add-balance?amount=${amount}`, {
        method: 'PUT'
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Gagal menambahkan saldo');
      }
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error addBalance:', error);
      throw error;
    }
  },

  async updateCustomer(id, customerData) {
    try {
      const response = await fetchWithToken(`${API_BASE.customer}/customers/${id}`, {
        method: 'PUT',
        body: JSON.stringify(customerData)
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Gagal memperbarui profil');
      }
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error updateCustomer:', error);
      throw error;
    }
  },

  // ==========================================
  // CUSTOMER REGISTRATION (Port 8083)
  // ==========================================
  async registerCustomer(customerData) {
    try {
      const response = await fetch(`${API_BASE.customer}/customers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(customerData)
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Registrasi gagal');
      }

      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Register error:', error);
      throw error;
    }
  },

  // ==========================================
  // PRODUCT (Port 8082)
  // ==========================================
  async getProducts() {
    try {
      const response = await fetchWithToken(`${API_BASE.product}/products`);
      if (!response.ok) throw new Error('Gagal mengambil produk');
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error getProducts:', error);
      return [];
    }
  },

  async getProductById(id) {
    try {
      const response = await fetchWithToken(`${API_BASE.product}/products/${id}`);
      if (!response.ok) throw new Error('Produk tidak ditemukan');
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error getProductById:', error);
      return null;
    }
  },

  // ==========================================
  // ORDER (Port 8084)
  // ==========================================
  async createOrder(customerId, items, courierName, shippingFee) {
    try {
      const response = await fetchWithToken(
        `${API_BASE.order}/orders`,
        {
          method: 'POST',
          body: JSON.stringify({
            customerId: customerId,
            courierName: courierName,
            shippingFee: shippingFee,
            items: items.map(item => ({
              productId: item.productId,
              quantity: item.quantity
            }))
          })
        }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Gagal membuat order');
      }

      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error createOrder:', error);
      throw error;
    }
  },

  async getOrders(customerId = null) {
    try {
      const url = customerId
        ? `${API_BASE.order}/orders?customerId=${customerId}`
        : `${API_BASE.order}/orders`;
      
      const response = await fetchWithToken(url);
      if (!response.ok) throw new Error('Gagal mengambil order');
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error getOrders:', error);
      return [];
    }
  },

  async getOrderById(id) {
    try {
      const response = await fetchWithToken(`${API_BASE.order}/orders/${id}`);
      if (!response.ok) throw new Error('Order tidak ditemukan');
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error getOrderById:', error);
      return null;
    }
  },

  async cancelOrder(orderId) {
    try {
      const response = await fetchWithToken(
        `${API_BASE.order}/orders/${orderId}/cancel`,
        { method: 'POST' }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Gagal membatalkan order');
      }

      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error cancelOrder:', error);
      throw error;
    }
  },

  // ==========================================
  // SHIPPING (Port 8086)
  // ==========================================
  async createShipment(shipmentData) {
    try {
      const response = await fetchWithToken(`${API_BASE.shipping}/shipments`, {
        method: 'POST',
        body: JSON.stringify(shipmentData)
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Gagal memproses pengiriman');
      }
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error createShipment:', error);
      throw error;
    }
  },

  async getShipmentByOrder(orderId) {
    try {
      const response = await fetchWithToken(
        `${API_BASE.shipping}/shipments/order/${orderId}`
      );
      
      if (!response.ok) return null;
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error getShipmentByOrder:', error);
      return null;
    }
  },

  // ==========================================
  // PAYMENT (Port 8087)
  // ==========================================
  async payOrder(orderId, method) {
    try {
      const response = await fetchWithToken(`${API_BASE.payment}/payments`, {
        method: 'POST',
        body: JSON.stringify({ orderId, method })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Pembayaran gagal');
      }

      const data = await response.json();
      return data.data || data;
    } catch (error) {
      console.error('Error payOrder:', error);
      throw error;
    }
  },

  async getPaymentByOrderId(orderId) {
    try {
      const response = await fetchWithToken(`${API_BASE.payment}/payments/order/${orderId}`);
      if (!response.ok) return null;
      const res = await response.json();
      return res.data || res;
    } catch (error) {
      console.error('Error getPaymentByOrderId:', error);
      return null;
    }
  },

};
