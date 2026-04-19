// ==================== KONFIGURASI ====================
const API_BASE_URL = "http://localhost:8080/api";

// ==================== GLOBAL VARIABLES ====================
let currentCustomerId = null;

// ==================== HELPER FUNCTIONS ====================
function formatRupiah(angka) {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka);
}

function showNotification(message, type = "success") {
    const bgColor = type === "success" ? "bg-green-500" : (type === "error" ? "bg-red-500" : "bg-blue-500");
    const notification = document.createElement('div');
    notification.className = `fixed top-20 right-5 z-50 ${bgColor} text-white px-4 py-2 rounded-lg shadow-lg text-sm font-bold transition-all duration-300`;
    notification.innerText = message;
    document.body.appendChild(notification);
    setTimeout(() => notification.remove(), 3000);
}

// ==================== CART (LOCALSTORAGE) ====================
function getCart() {
    const cart = localStorage.getItem('shoppingCart');
    return cart ? JSON.parse(cart) : [];
}

function saveCart(cart) {
    localStorage.setItem('shoppingCart', JSON.stringify(cart));
    updateCartBadge();
    renderCartModal();
}

function updateCartBadge() {
    const cart = getCart();
    const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
    const badge = document.getElementById('cartBadge');
    if (badge) badge.innerText = totalItems;
}

function addToCart(productId, name, price) {
    let cart = getCart();
    const existing = cart.find(item => item.productId === productId);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ productId, name, price, quantity: 1 });
    }
    saveCart(cart);
    showNotification(`${name} ditambahkan ke keranjang`, "success");
}

function updateCartItemQuantity(productId, delta) {
    let cart = getCart();
    const index = cart.findIndex(item => item.productId === productId);
    if (index !== -1) {
        const newQty = cart[index].quantity + delta;
        if (newQty <= 0) {
            cart.splice(index, 1);
        } else {
            cart[index].quantity = newQty;
        }
        saveCart(cart);
        renderCartModal();
    }
}

function clearCart() {
    localStorage.removeItem('shoppingCart');
    updateCartBadge();
    renderCartModal();
}

function getCartTotal() {
    const cart = getCart();
    return cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
}

function renderCartModal() {
    const cart = getCart();
    const container = document.getElementById('cartItems');
    const totalSpan = document.getElementById('cartTotal');
    
    if (!container) return;
    
    if (cart.length === 0) {
        container.innerHTML = '<p class="text-center text-gray-400">Keranjang kosong</p>';
        if (totalSpan) totalSpan.innerText = formatRupiah(0);
        return;
    }
    
    container.innerHTML = cart.map(item => `
        <div class="flex justify-between items-center border-b pb-2">
            <div class="flex-1">
                <p class="font-bold text-sm">${item.name}</p>
                <p class="text-xs text-gray-500">${formatRupiah(item.price)}</p>
            </div>
            <div class="flex items-center gap-2">
                <button onclick="updateCartItemQuantity(${item.productId}, -1)" class="w-6 h-6 bg-gray-200 rounded-full">-</button>
                <span class="w-6 text-center">${item.quantity}</span>
                <button onclick="updateCartItemQuantity(${item.productId}, 1)" class="w-6 h-6 bg-gray-200 rounded-full">+</button>
            </div>
        </div>
    `).join('');
    
    if (totalSpan) totalSpan.innerText = formatRupiah(getCartTotal());
}

// ==================== MODAL CART ====================
function openCartModal() {
    const modal = document.getElementById('cartModal');
    if (modal) {
        renderCartModal();
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

function closeCartModal() {
    const modal = document.getElementById('cartModal');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

// ==================== FETCH CUSTOMERS (set default) ====================
async function fetchCustomers() {
    try {
        const response = await fetch(`${API_BASE_URL}/customers`);
        if (!response.ok) throw new Error("Gagal mengambil data customer");
        const customers = await response.json();
        if (customers.length > 0) {
            currentCustomerId = customers[0].id;
            showNotification(`Login sebagai: ${customers[0].name}`, "info");
            fetchOrders(); // Ambil riwayat order untuk customer default
        } else {
            showNotification("Belum ada customer, silakan daftar via admin", "error");
        }
    } catch (error) {
        console.error("Error fetchCustomers:", error);
    }
}

// ==================== FETCH PRODUCTS & RENDER GRID ====================
async function fetchProducts() {
    try {
        const response = await fetch(`${API_BASE_URL}/products`);
        if (!response.ok) throw new Error("Gagal mengambil produk");
        const products = await response.json();
        renderProducts(products);
    } catch (error) {
        console.error("Error fetchProducts:", error);
        document.getElementById('productGrid').innerHTML = '<p class="text-red-500">Gagal memuat produk</p>';
    }
}

function renderProducts(products) {
    const grid = document.getElementById('productGrid');
    if (!grid) return;
    
    if (products.length === 0) {
        grid.innerHTML = '<p class="text-gray-400">Belum ada produk</p>';
        return;
    }
    
    grid.innerHTML = products.map(prod => `
        <div class="group border border-gray-100 rounded-3xl p-4 hover:shadow-xl hover:border-brand/30 transition duration-300 bg-white flex flex-col h-full">
            <div class="relative w-full h-40 rounded-2xl overflow-hidden mb-4 bg-gray-100">
                <img src="https://picsum.photos/id/${prod.id}/200/200" alt="${prod.name}" class="w-full h-full object-cover group-hover:scale-110 transition duration-500">
                <div class="absolute top-2 right-2 bg-white/90 backdrop-blur text-xs font-bold px-2 py-1 rounded-lg">⭐ 4.8</div>
            </div>
            <div class="flex flex-col flex-grow">
                <p class="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">${prod.category ? prod.category.name : 'General'}</p>
                <h4 class="font-black text-gray-800 text-sm mb-2 line-clamp-2">${prod.name}</h4>
                <div class="mt-auto flex items-end justify-between">
                    <span class="font-black text-brand text-lg">${formatRupiah(prod.price)}</span>
                    <button onclick="addToCart(${prod.id}, '${prod.name.replace(/'/g, "\\'")}', ${prod.price})" class="w-8 h-8 bg-brand-surface text-brand rounded-full flex justify-center items-center hover:bg-brand hover:text-white transition font-bold">+</button>
                </div>
            </div>
        </div>
    `).join('');
}

// ==================== ORDER HISTORY ====================
async function fetchOrders() {
    if (!currentCustomerId) return;
    try {
        const response = await fetch(`${API_BASE_URL}/orders`);
        if (!response.ok) throw new Error("Gagal mengambil order");
        const allOrders = await response.json();
        const userOrders = allOrders.filter(order => order.customer.id == currentCustomerId);
        renderOrders(userOrders);
    } catch (error) {
        console.error("Error fetchOrders:", error);
        document.getElementById('orderHistoryContainer').innerHTML = '<p class="text-red-500">Gagal memuat pesanan</p>';
    }
}

async function renderOrders(orders) {
    const container = document.getElementById('orderHistoryContainer');
    if (!container) return;
    if (orders.length === 0) {
        container.innerHTML = '<p class="text-gray-400 text-center">Belum ada pesanan</p>';
        return;
    }
    
    let html = '';
    for (const order of orders) {
        let shippingHtml = '';
        if (order.status === 'SHIPPED' || order.status === 'DELIVERED') {
            try {
                const shipRes = await fetch(`${API_BASE_URL}/shipments?orderId=${order.id}`);
                if (shipRes.ok) {
                    const shipments = await shipRes.json();
                    const shipment = shipments[0];
                    if (shipment) {
                        shippingHtml = `<div class="text-xs text-blue-600 mt-1">📦 Resi: ${shipment.trackingNumber || 'N/A'} | Status: ${shipment.status || order.status}</div>`;
                    }
                }
            } catch (e) {}
        }
        const date = new Date(order.createdAt).toLocaleString('id-ID');
        html += `
            <div class="border rounded-xl p-4 bg-white shadow-sm">
                <div class="flex justify-between items-start flex-wrap gap-2">
                    <div>
                        <p class="font-bold text-brand">${order.orderNumber}</p>
                        <p class="text-xs text-gray-400">${date}</p>
                    </div>
                    <div class="text-right">
                        <p class="font-black">${formatRupiah(order.totalAmount)}</p>
                        <span class="text-xs px-2 py-1 rounded-full ${order.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : (order.status === 'CANCELLED' ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700')}">
                            ${order.status}
                        </span>
                    </div>
                </div>
                <div class="mt-2 text-sm">
                    ${order.items.map(item => `<div>${item.product.name} x${item.quantity}</div>`).join('')}
                </div>
                ${shippingHtml}
                ${order.status === 'PENDING' ? `<button onclick="cancelOrder(${order.id})" class="mt-2 text-xs text-red-500 hover:underline">Batalkan Pesanan</button>` : ''}
            </div>
        `;
    }
    container.innerHTML = html;
}

// ==================== CANCEL ORDER ====================
async function cancelOrder(orderId) {
    if (!confirm("Batalkan pesanan ini? Stok akan dikembalikan.")) return;
    try {
        const response = await fetch(`${API_BASE_URL}/orders/${orderId}/cancel`, { method: 'POST' });
        if (response.ok) {
            showNotification("Pesanan dibatalkan", "success");
            fetchOrders();
        } else {
            const error = await response.json();
            showNotification(error.error || "Gagal membatalkan", "error");
        }
    } catch (error) {
        showNotification("Error jaringan", "error");
    }
}

// ==================== CHECKOUT ====================
async function checkout() {
    if (!currentCustomerId) {
        showNotification("Tidak ada customer yang dipilih. Silakan refresh halaman.", "error");
        return;
    }
    const cart = getCart();
    if (cart.length === 0) {
        showNotification("Keranjang kosong", "error");
        return;
    }
    
    const payload = cart.map(item => ({
        productId: item.productId,
        quantity: item.quantity
    }));
    
    try {
        const response = await fetch(`${API_BASE_URL}/orders?customerId=${currentCustomerId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await response.json();
        if (response.ok) {
            showNotification(`Order berhasil! No: ${data.orderNumber}`, "success");
            clearCart();
            closeCartModal();
            fetchOrders();
        } else {
            showNotification(data.message || data.error || "Checkout gagal", "error");
        }
    } catch (error) {
        showNotification("Error jaringan", "error");
    }
}

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    fetchProducts();
    fetchCustomers(); // akan set currentCustomerId dan ambil riwayat
    updateCartBadge();
    
    const cartIcon = document.getElementById('cartIcon');
    const closeModal = document.getElementById('closeCartModal');
    const checkoutBtn = document.getElementById('checkoutBtn');
    
    if (cartIcon) cartIcon.addEventListener('click', openCartModal);
    if (closeModal) closeModal.addEventListener('click', closeCartModal);
    if (checkoutBtn) checkoutBtn.addEventListener('click', checkout);
    
    window.addEventListener('click', (e) => {
        const modal = document.getElementById('cartModal');
        if (e.target === modal) closeCartModal();
    });
});