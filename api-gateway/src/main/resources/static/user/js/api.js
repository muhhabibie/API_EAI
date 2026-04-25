/**
 * EAI User API Client - Microservices Integration
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
      localStorage.setItem('token', data.token);
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
      const response = await fetchWithToken(`${API_BASE.customer}/customers`);
      if (!response.ok) return null;
      const customers = await response.json();
      return customers.find(c => c.email === email) || null;
    } catch (error) {
      console.error('Error getCustomerByEmail:', error);
      return null;
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

      return await response.json();
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
      return await response.json();
    } catch (error) {
      console.error('Error getProducts:', error);
      return [];
    }
  },

  async getProductById(id) {
    try {
      const response = await fetchWithToken(`${API_BASE.product}/products/${id}`);
      if (!response.ok) throw new Error('Produk tidak ditemukan');
      return await response.json();
    } catch (error) {
      console.error('Error getProductById:', error);
      return null;
    }
  },

  // ==========================================
  // ORDER (Port 8084)
  // ==========================================
  async createOrder(customerId, items) {
    try {
      const response = await fetchWithToken(
        `${API_BASE.order}/orders?customerId=${customerId}`,
        {
          method: 'POST',
          body: JSON.stringify(items)
        }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Gagal membuat order');
      }

      return await response.json();
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
      return await response.json();
    } catch (error) {
      console.error('Error getOrders:', error);
      return [];
    }
  },

  async getOrderById(id) {
    try {
      const response = await fetchWithToken(`${API_BASE.order}/orders/${id}`);
      if (!response.ok) throw new Error('Order tidak ditemukan');
      return await response.json();
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

      return await response.json();
    } catch (error) {
      console.error('Error cancelOrder:', error);
      throw error;
    }
  },

  // ==========================================
  // SHIPPING (Port 8086)
  // ==========================================
  async getShipmentByOrder(orderId) {
    try {
      const response = await fetchWithToken(
        `${API_BASE.shipping}/shipments/order/${orderId}`
      );
      
      if (!response.ok) return null;
      return await response.json();
    } catch (error) {
      console.error('Error getShipmentByOrder:', error);
      return null;
    }
  }
};
