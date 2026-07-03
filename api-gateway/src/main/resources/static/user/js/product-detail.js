// ==================== GLOBAL STATE & VARIABLES ====================
let productId = null;
let currentProduct = null;
let currentQuantity = 1;
let maxStockAvailable = 0;

// ==================== HELPER FUNCTIONS ====================
function formatRupiah(angka) {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', minimumFractionDigits: 0 }).format(angka);
}

function showNotification(message, type = "success") {
    const bgColor = type === "success" ? "bg-green-500" : (type === "error" ? "bg-red-500" : "bg-blue-500");
    const notification = document.createElement('div');
    notification.className = `fixed top-5 right-5 z-50 ${bgColor} text-white px-5 py-3 rounded-2xl shadow-lg text-sm font-bold transition-all duration-300 transform translate-y-0`;
    notification.innerText = message;
    document.body.appendChild(notification);
    setTimeout(() => {
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Load cart storage key dynamically based on logged in user
function getCartStorageKey() {
    const userEmail = localStorage.getItem('loggedInCustomerName');
    return userEmail ? `shoppingCart_${userEmail}` : 'shoppingCart_guest';
}

function getCart() {
    const key = getCartStorageKey();
    const cart = localStorage.getItem(key);
    return cart ? JSON.parse(cart) : [];
}

function saveCart(cart) {
    const key = getCartStorageKey();
    localStorage.setItem(key, JSON.stringify(cart));
    updateCartBadge();
}

function updateCartBadge() {
    const cart = getCart();
    const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
    const badge = document.getElementById('cartBadge');
    if (badge) badge.innerText = totalItems;
}

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', async () => {
    // 1. Resolve productId from query string
    const urlParams = new URLSearchParams(window.location.search);
    const idParam = urlParams.get('id');

    if (!idParam) {
        showNotification("Produk tidak ditemukan. Mengalihkan ke Katalog.", "error");
        setTimeout(() => window.location.href = 'index.html', 1500);
        return;
    }

    productId = parseInt(idParam);
    updateCartBadge();

    // Show admin portal button if logged-in user is an admin
    const userRole = localStorage.getItem('loggedInUserRole');
    const adminPortalBtn = document.getElementById('adminPortalBtn');
    if (adminPortalBtn && userRole === 'ROLE_ADMIN') {
        adminPortalBtn.classList.remove('hidden');
    }

    // 2. Fetch product details
    await loadProductDetails();

    // 3. Setup listeners
    setupQtyListeners();
    setupCartActionListeners();
});

// Load details from product microservice
async function loadProductDetails() {
    try {
        currentProduct = await UserAPI.getProductById(productId);

        if (!currentProduct) {
            showNotification("Produk tidak ditemukan.", "error");
            setTimeout(() => window.location.href = 'index.html', 1500);
            return;
        }

        maxStockAvailable = currentProduct.stock !== undefined ? currentProduct.stock : 0;

        // Render fields
        document.getElementById('productTitle').innerText = currentProduct.name;
        document.getElementById('productPrice').innerText = formatRupiah(currentProduct.price);
        document.getElementById('productDescription').innerText = currentProduct.description || 'Tidak ada deskripsi produk.';
        document.getElementById('productCategory').innerText = currentProduct.category ? currentProduct.category.name : 'General';
        
        // Render Image
        const img = document.getElementById('productImage');
        if (img) {
            img.src = currentProduct.imageUrl || `https://picsum.photos/id/${currentProduct.id + 10}/600/600`;
            img.alt = currentProduct.name;
        }

        // Render Stock badge indicator
        updateStockIndicator();

        // Fetch related recommendations
        await loadRelatedProducts();

    } catch (error) {
        console.error("Error loading product details:", error);
        showNotification("Gagal mengambil data produk.", "error");
    }
}

// Render dynamic stock availability badge
function updateStockIndicator() {
    const badge = document.getElementById('stockBadge');
    if (!badge) return;

    if (maxStockAvailable === 0) {
        badge.className = "text-xs font-bold px-3 py-1.5 rounded-full border border-red-200 bg-red-50 text-red-500 shadow-sm";
        badge.innerText = "Habis Terjual";

        // Disable input buttons
        document.getElementById('qtyDeductBtn').disabled = true;
        document.getElementById('qtyAddBtn').disabled = true;
        
        // Disable checkout actions
        const addToCartBtn = document.getElementById('addToCartBtn');
        const buyNowBtn = document.getElementById('buyNowBtn');
        
        addToCartBtn.disabled = true;
        addToCartBtn.className = "flex-1 border-2 border-gray-200 text-gray-300 font-black py-4 rounded-2xl transition text-sm cursor-not-allowed bg-gray-50";
        addToCartBtn.innerText = "Stok Habis";

        buyNowBtn.disabled = true;
        buyNowBtn.className = "flex-1 bg-gray-200 text-gray-400 font-black py-4 rounded-2xl transition text-sm cursor-not-allowed";
        buyNowBtn.innerText = "Stok Habis";
    } else if (maxStockAvailable <= 10) {
        badge.className = "text-xs font-bold px-3 py-1.5 rounded-full border border-yellow-200 bg-yellow-50 text-yellow-600 shadow-sm";
        badge.innerText = `Stok Terbatas (${maxStockAvailable})`;
    } else {
        badge.className = "text-xs font-bold px-3 py-1.5 rounded-full border border-green-200 bg-green-50 text-green-600 shadow-sm";
        badge.innerText = `Stok Melimpah (${maxStockAvailable})`;
    }
}

// Setup plus/minus quantity listener triggers
function setupQtyListeners() {
    const deductBtn = document.getElementById('qtyDeductBtn');
    const addBtn = document.getElementById('qtyAddBtn');
    const qtyInput = document.getElementById('qtyInput');

    if (deductBtn) {
        deductBtn.addEventListener('click', () => {
            if (maxStockAvailable === 0) return;
            if (currentQuantity > 1) {
                currentQuantity--;
                qtyInput.innerText = currentQuantity;
            }
        });
    }

    if (addBtn) {
        addBtn.addEventListener('click', () => {
            if (maxStockAvailable === 0) return;
            if (currentQuantity < maxStockAvailable) {
                currentQuantity++;
                qtyInput.innerText = currentQuantity;
            } else {
                showNotification(`Pembelian maksimal terbatas oleh stok sisa (${maxStockAvailable} item).`, "error");
            }
        });
    }
}

// Wire buy and add-to-cart operations
function setupCartActionListeners() {
    const addToCartBtn = document.getElementById('addToCartBtn');
    const buyNowBtn = document.getElementById('buyNowBtn');

    if (addToCartBtn) {
        addToCartBtn.addEventListener('click', () => {
            if (maxStockAvailable === 0) return;
            executeAddToCart();
            showNotification(`${currentProduct.name} (${currentQuantity}x) ditambahkan ke keranjang belanja!`, "success");
        });
    }

    if (buyNowBtn) {
        buyNowBtn.addEventListener('click', () => {
            if (maxStockAvailable === 0) return;
            
            // Check login status
            const token = localStorage.getItem('token');
            if (!token) {
                showNotification("Silakan login terlebih dahulu untuk melakukan transaksi.", "error");
                // Open home with login screen triggered
                setTimeout(() => window.location.href = 'index.html', 1500);
                return;
            }

            executeAddToCart();
            // Direct redirect to checkout
            window.location.href = 'checkout.html';
        });
    }
}

// Core add-to-cart local calculation logic
function executeAddToCart() {
    let cart = getCart();
    const existing = cart.find(item => item.productId === productId);
    
    if (existing) {
        existing.quantity += currentQuantity;
        // Bound to stock sisa limit
        if (existing.quantity > maxStockAvailable) {
            existing.quantity = maxStockAvailable;
        }
    } else {
        cart.push({
            productId: currentProduct.id,
            name: currentProduct.name,
            price: currentProduct.price,
            quantity: currentQuantity
        });
    }
    
    saveCart(cart);
}

// Load dynamic related recommendations from the catalog
async function loadRelatedProducts() {
    const container = document.getElementById('relatedProductsGrid');
    if (!container) return;

    try {
        const allProds = await UserAPI.getProducts();
        
        // Filter out current product, match catalog recommendations
        const related = allProds
            .filter(p => p.id !== productId && (currentProduct.category && p.category ? p.category.id === currentProduct.category.id : true))
            .slice(0, 4);

        if (related.length === 0) {
            // Grab any 4 alternative products
            const fallback = allProds.filter(p => p.id !== productId).slice(0, 4);
            renderRelated(fallback);
            return;
        }

        renderRelated(related);
    } catch (error) {
        console.error("Error loading related recommendations:", error);
        container.innerHTML = '<p class="text-xs text-gray-400 font-bold">Rekomendasi tidak tersedia saat ini.</p>';
    }
}

function renderRelated(products) {
    const container = document.getElementById('relatedProductsGrid');
    if (!container) return;

    if (products.length === 0) {
        container.innerHTML = '<p class="text-xs text-gray-400 font-bold">Belum ada rekomendasi sejenis.</p>';
        return;
    }

    container.innerHTML = products.map(prod => `
        <div class="group border border-gray-100 rounded-3xl p-4 hover:shadow-xl hover:border-brand/30 transition bg-white flex flex-col h-full cursor-pointer" onclick="window.location.href='product-detail.html?id=${prod.id}'">
            <div class="relative w-full h-32 rounded-2xl overflow-hidden mb-3 bg-gray-50">
                <img src="${prod.imageUrl || 'https://picsum.photos/id/' + (prod.id + 10) + '/300/300'}" alt="${prod.name}" class="w-full h-full object-cover group-hover:scale-105 transition-all duration-300">
            </div>
            <div class="flex flex-col flex-grow text-left">
                <p class="text-[10px] text-gray-400 font-bold uppercase tracking-wider mb-0.5">${prod.category ? prod.category.name : 'General'}</p>
                <h4 class="font-bold text-gray-800 text-xs line-clamp-2 leading-snug group-hover:text-brand transition mb-2">${prod.name}</h4>
                <span class="font-black text-brand text-sm mt-auto">${formatRupiah(prod.price)}</span>
            </div>
        </div>
    `).join('');
}
