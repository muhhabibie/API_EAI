// ==================== KONFIGURASI ====================
// API_BASE_URL sudah defined di api.js
// Jadi kita tidak perlu hardcode URL di sini

// ==================== GLOBAL VARIABLES ====================
let currentCustomerId = null;
let currentCustomerName = "";
let currentCustomerEmail = "";

// ==================== HELPER FUNCTIONS ====================
function formatRupiah(angka) {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka);
}
// Fungsi untuk membuka/menutup mata password
function togglePassword(inputId, iconId) {
    const input = document.getElementById(inputId);
    const icon = document.getElementById(iconId);
    
    // Ikon Mata Terbuka
    const eyeOpen = `<path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" /><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />`;
    // Ikon Mata Tertutup (Dicoret)
    const eyeClosed = `<path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />`;

    if (input.type === "password") {
        input.type = "text";
        icon.innerHTML = eyeClosed;
    } else {
        input.type = "password";
        icon.innerHTML = eyeOpen;
    }
}

function showNotification(message, type = "success") {
    const bgColor = type === "success" ? "bg-green-500" : (type === "error" ? "bg-red-500" : "bg-blue-500");
    const notification = document.createElement('div');
    notification.className = `fixed top-20 right-5 z-50 ${bgColor} text-white px-4 py-2 rounded-lg shadow-lg text-sm font-bold transition-all duration-300`;
    notification.innerText = message;
    document.body.appendChild(notification);
    setTimeout(() => notification.remove(), 3000);
}

// Fungsi pengganti fetch() biasa, otomatis menyelipkan JWT Token
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('token');
    
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, { ...options, headers });
    
    // Jika token kedaluwarsa atau tidak valid (Error 401 Unauthorized)
    if (response.status === 401) {
        logout();
        showNotification("Sesi habis, silakan login kembali.", "error");
    }
    return response;
}

// ==================== CART (LOCALSTORAGE) ====================
function getCartStorageKey() {
    const userEmail = localStorage.getItem('loggedInCustomerName');
    // Jika login, namanya "shoppingCart_emailnya". Jika belum, "shoppingCart_guest"
    return userEmail ? `shoppingCart_${userEmail}` : 'shoppingCart_guest';
}

function getCart() {
    const key = getCartStorageKey(); // Gunakan laci yang spesifik
    const cart = localStorage.getItem(key);
    return cart ? JSON.parse(cart) : [];
}

function saveCart(cart) {
    const key = getCartStorageKey(); // Gunakan laci yang spesifik
    localStorage.setItem(key, JSON.stringify(cart));
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
    // 1. Cek apakah ada token JWT
    const token = localStorage.getItem('token');

    // 2. Jika tidak ada token, batalkan proses dan tampilkan login
    if (!token) {
        showNotification("Silakan login terlebih dahulu untuk menambah barang", "error");
        showLoginModal(); // Munculkan modal login
        return; // Hentikan fungsi di sini
    }

    // Kode di bawah ini hanya jalan jika sudah login
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
    const key = getCartStorageKey(); // Bersihkan laci yang spesifik
    localStorage.removeItem(key);
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
    // 1. Cek apakah ada token JWT
    const token = localStorage.getItem('token');

    // 2. Jika tidak ada token, arahkan ke login
    if (!token) {
        showNotification("Silakan login untuk melihat keranjang Anda", "error");
        showLoginModal();
        return;
    }

    // Kode di bawah ini hanya jalan jika sudah login
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

// ==================== LOGIN / REGISTER / LOGOUT ====================
// ==================== LOGIN / REGISTER / LOGOUT ====================

function showLoginModal() {
    const modal = document.getElementById('loginModal');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

function hideLoginModal() {
    const modal = document.getElementById('loginModal');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

async function loginUser(email, password) {
    try {
        const data = await UserAPI.login(email, password);
        
        // 1. Token sudah disimpan di UserAPI.login()
        currentCustomerName = email; 
        currentCustomerEmail = email;
        localStorage.setItem('loggedInCustomerName', email);
        
        // 2. Get customer ID dari email
        const customer = await UserAPI.getCustomerByEmail(email);
        if (customer) {
            currentCustomerId = customer.id;
            localStorage.setItem('loggedInCustomerId', customer.id);
        }
        
        // 3. Update tampilan UI Navbar
        const navAuthSection = document.getElementById('navAuthSection');
        const userInfoDiv = document.getElementById('userInfo');
        const userNameSpan = document.getElementById('loggedInUserName');
        
        if (navAuthSection) navAuthSection.classList.add('hidden'); 
        if (userInfoDiv && userNameSpan) {
            userNameSpan.innerText = email; 
            userInfoDiv.classList.remove('hidden'); 
            userInfoDiv.classList.add('flex');      
        }
        
        hideLoginModal();
        fetchOrders(); 
        updateCartBadge(); 
        
        showNotification(data.message || "Login berhasil!", "success");
    } catch (error) {
        showNotification(error.message || "Terjadi kesalahan jaringan", "error");
    }
}

function logout() {
    // 1. Hapus memori variabel
    currentCustomerId = null;
    currentCustomerName = "";
    currentCustomerEmail = "";
    
    // 2. Hapus token dan data dari Storage (TAPI JANGAN HAPUS KERANJANGNYA)
    localStorage.removeItem('token'); 
    localStorage.removeItem('loggedInCustomerId');
    localStorage.removeItem('loggedInCustomerName');
    
    // 3. Segarkan tampilan keranjang
    updateCartBadge();
    
    // 4. Reset Tampilan UI Navbar
    const navAuthSection = document.getElementById('navAuthSection');
    const userInfoDiv = document.getElementById('userInfo');
    
    if (navAuthSection) navAuthSection.classList.remove('hidden'); 
    if (userInfoDiv) {
        userInfoDiv.classList.remove('flex');
        userInfoDiv.classList.add('hidden');
    }
    
    // 5. Bersihkan riwayat pesanan di layar
    const orderContainer = document.getElementById('orderHistoryContainer');
    if (orderContainer) {
        orderContainer.innerHTML = '<p class="text-gray-400 text-center">Silakan login terlebih dahulu</p>';
    }

    // 6. BERSIHKAN BEKAS KETIKAN DI FORM LOGIN & REGISTER
    const loginEmail = document.getElementById('loginEmail');
    const loginPassword = document.getElementById('loginPassword');
    if (loginEmail) loginEmail.value = '';
    if (loginPassword) loginPassword.value = '';

    // Bersihkan juga form pendaftaran untuk berjaga-jaga
    const regName = document.getElementById('regName');
    const regEmail = document.getElementById('regEmail');
    const regAddress = document.getElementById('regAddress');
    const regPassword = document.getElementById('regPassword');
    const regConfirmPassword = document.getElementById('regConfirmPassword');
    if (regName) regName.value = '';
    if (regEmail) regEmail.value = '';
    if (regAddress) regAddress.value = '';
    if (regPassword) regPassword.value = '';
    if (regConfirmPassword) regConfirmPassword.value = '';
    
    showNotification("Anda telah logout", "info");
}

async function registerAndLogin(name, email, address, password) { 
    try {
        await UserAPI.registerCustomer({ name, email, address, password });
        showNotification(`Berhasil mendaftar! Mengalihkan...`, "success");
        
        // Setelah berhasil daftar, otomatis login dengan email dan password tersebut
        loginUser(email, password); 
    } catch (error) {
        showNotification(error.message || "Registrasi gagal", "error");
    }
}

// ==================== FETCH PRODUCTS & RENDER GRID ====================
async function fetchProducts() {
    try {
        const products = await UserAPI.getProducts();
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
    const email = localStorage.getItem('loggedInCustomerName');
    if (!email) return;
    try {
        // Get all orders (atau dengan customerId jika tersedia)
        const allOrders = await UserAPI.getOrders();
        renderOrders(allOrders || []);
    } catch (error) {
        console.error("Error fetchOrders:", error);
        const container = document.getElementById('orderHistoryContainer');
        if (container) {
            container.innerHTML = '<p class="text-red-500">Gagal memuat pesanan</p>';
        }
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
        try {
            const shipment = await UserAPI.getShipmentByOrder(order.id);
            if (shipment) {
                shippingHtml = `<div class="text-xs text-blue-600 mt-1">📦 Resi: ${shipment.trackingNumber || 'N/A'} | Status Kirim: ${shipment.status || 'PENDING'}</div>`;
            }
        } catch (e) {}
        
        const date = new Date(order.createdAt).toLocaleString('id-ID');
        html += `
            <div class="border rounded-xl p-4 bg-white shadow-sm">
                <div class="flex justify-between items-start flex-wrap gap-2">
                    <div>
                        <p class="font-bold text-brand">${order.orderNumber || 'Order #' + order.id}</p>
                        <p class="text-xs text-gray-400">${date}</p>
                    </div>
                    <div class="text-right">
                        <p class="font-black">${formatRupiah(order.totalAmount || 0)}</p>
                        <span class="text-xs px-2 py-1 rounded-full ${order.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : (order.status === 'CANCELLED' ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700')}">
                            ${order.status}
                        </span>
                    </div>
                </div>
                <div class="mt-2 text-sm">
                    ${order.items ? order.items.map(item => `<div>${item.product?.name || 'Product'} x${item.quantity}</div>`).join('') : ''}
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
        await UserAPI.cancelOrder(orderId);
        showNotification("Pesanan dibatalkan", "success");
        fetchOrders();
    } catch (error) {
        showNotification(error.message || "Gagal membatalkan", "error");
    }
}

// ==================== CHECKOUT ====================
async function checkout() {
    if (!currentCustomerId) {
        showNotification("Silakan login terlebih dahulu", "error");
        showLoginModal();
        return;
    }
    const cart = getCart();
    if (cart.length === 0) {
        showNotification("Keranjang kosong", "error");
        return;
    }
    
    const payload = cart.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        price: item.price
    }));
    
    try {
        const data = await UserAPI.createOrder(currentCustomerId, payload);
        showNotification(`Order berhasil! No: ${data.orderNumber || data.id}`, "success");
        clearCart();
        closeCartModal();
        fetchOrders();
    } catch (error) {
        showNotification(error.message || "Checkout gagal", "error");
    }
}


// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    fetchProducts();
    
    // Cek apakah sudah login (localStorage)
    const token = localStorage.getItem('token');
    const savedName = localStorage.getItem('loggedInCustomerName');
    const savedCustomerId = localStorage.getItem('loggedInCustomerId');
    
    const navAuthSection = document.getElementById('navAuthSection');
    const userInfoDiv = document.getElementById('userInfo');
    const userNameSpan = document.getElementById('loggedInUserName');

    if (token && savedName) {
        // JIKA SUDAH LOGIN:
        currentCustomerName = savedName;
        currentCustomerEmail = savedName;
        if (savedCustomerId) {
            currentCustomerId = parseInt(savedCustomerId);
        }
        if (navAuthSection) navAuthSection.classList.add('hidden');
        if (userInfoDiv && userNameSpan) {
            userNameSpan.innerText = savedName;
            userInfoDiv.classList.remove('hidden');
            userInfoDiv.classList.add('flex');
        }
        fetchOrders();
    } else {
        // JIKA BELUM LOGIN:
        if (navAuthSection) navAuthSection.classList.remove('hidden');
        if (userInfoDiv) {
            userInfoDiv.classList.remove('flex');
            userInfoDiv.classList.add('hidden');
        }
    }
    
    updateCartBadge();

    // Listener Tombol Login di Navbar
    const navLoginBtn = document.getElementById('navLoginBtn');
    if (navLoginBtn) {
        navLoginBtn.addEventListener('click', () => {
            showLoginModal();
        });
    }
    
    // Event listeners untuk cart
    const cartIcon = document.getElementById('cartIcon');
    const closeCartModalBtn = document.getElementById('closeCartModal');
    const checkoutBtn = document.getElementById('checkoutBtn');
    if (cartIcon) cartIcon.addEventListener('click', openCartModal);
    if (closeCartModalBtn) closeCartModalBtn.addEventListener('click', closeCartModal);
    if (checkoutBtn) checkoutBtn.addEventListener('click', checkout);
    
    // Login modal tab switching
    const loginTab = document.getElementById('loginTabBtn');
    const registerTab = document.getElementById('registerTabBtn');
    const loginPanel = document.getElementById('loginPanel');
    const registerPanel = document.getElementById('registerPanel');
    if (loginTab && registerTab && loginPanel && registerPanel) {
        loginTab.addEventListener('click', () => {
            loginTab.classList.add('text-brand', 'border-brand');
            loginTab.classList.remove('text-gray-400');
            registerTab.classList.remove('text-brand', 'border-brand');
            registerTab.classList.add('text-gray-400');
            loginPanel.classList.remove('hidden');
            registerPanel.classList.add('hidden');
        });
        registerTab.addEventListener('click', () => {
            registerTab.classList.add('text-brand', 'border-brand');
            registerTab.classList.remove('text-gray-400');
            loginTab.classList.remove('text-brand', 'border-brand');
            loginTab.classList.add('text-gray-400');
            registerPanel.classList.remove('hidden');
            loginPanel.classList.add('hidden');
        });
    }
    
    // Login button
    const loginBtn = document.getElementById('loginBtn');
    if (loginBtn) {
        loginBtn.addEventListener('click', () => {
            const email = document.getElementById('loginEmail').value.trim();
            const password = document.getElementById('loginPassword').value.trim();
            
            if (!email || !password) {
                showNotification("Email dan password harus diisi", "error");
                return;
            }
            loginUser(email, password); // Panggil fungsi login dengan token
        });
    }
    
    // Register button
    const registerBtn = document.getElementById('registerBtn');
    if (registerBtn) {
        registerBtn.addEventListener('click', () => {
            const name = document.getElementById('regName').value.trim();
            const email = document.getElementById('regEmail').value.trim();
            const address = document.getElementById('regAddress').value.trim();
            const password = document.getElementById('regPassword').value.trim();
            const confirmPassword = document.getElementById('regConfirmPassword').value.trim();

            if (!name || !email || !address || !password || !confirmPassword) {
                showNotification("Semua field harus diisi", "error");
                return;
            }
            
            // Cek apakah password dan konfirmasinya sama
            if (password !== confirmPassword) {
                showNotification("Password tidak cocok!", "error");
                return;
            }
            
            // Panggil fungsi daftar (tambahkan parameter password!)
            registerAndLogin(name, email, address, password);
        });
    }
    
    // Logout button
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) logoutBtn.addEventListener('click', logout);
    
    // Tutup modal jika klik di luar area
    window.addEventListener('click', (e) => {
        const modal = document.getElementById('cartModal');
        if (e.target === modal) closeCartModal();
        const loginModal = document.getElementById('loginModal');
        if (e.target === loginModal) hideLoginModal();
    });
});