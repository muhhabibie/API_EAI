// ==================== GLOBAL VARIABLES & STATE ====================
let currentCustomerId = null;
let currentCustomer = null;
let selectedCourier = 'JNE';
let selectedShippingFee = 15000;
let cartItems = [];

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

function clearCart() {
    const key = getCartStorageKey();
    localStorage.removeItem(key);
}

function getCartTotal() {
    return cartItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
}

// Log topup event to localStorage (consistent with profile.js wallet history)
function logTopupEvent(customerId, amount) {
    const topupsKey = `topups_${customerId}`;
    const savedTopups = localStorage.getItem(topupsKey);
    const topupList = savedTopups ? JSON.parse(savedTopups) : [];
    topupList.push({
        id: `TX-TOP-${Date.now()}`,
        amount: amount,
        date: new Date().toISOString()
    });
    localStorage.setItem(topupsKey, JSON.stringify(topupList));
}

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', async () => {
    // 1. Auth Guard Checks
    const token = localStorage.getItem('token');
    let savedCustomerId = localStorage.getItem('loggedInCustomerId');
    const savedEmail = localStorage.getItem('loggedInCustomerName');

    // Proteksi sesi korup jika ID bukan angka
    if (savedCustomerId && isNaN(parseInt(savedCustomerId))) {
        localStorage.removeItem('token');
        localStorage.removeItem('loggedInCustomerId');
        localStorage.removeItem('loggedInCustomerName');
        localStorage.removeItem('loggedInUserRole');
        window.location.href = 'index.html';
        return;
    }

    if (token && savedEmail && !savedCustomerId) {
        try {
            const cust = await UserAPI.getCustomerByEmail(savedEmail);
            if (cust) {
                savedCustomerId = cust.id.toString();
                localStorage.setItem('loggedInCustomerId', savedCustomerId);
            } else {
                localStorage.removeItem('token');
                localStorage.removeItem('loggedInCustomerId');
                localStorage.removeItem('loggedInCustomerName');
                localStorage.removeItem('loggedInUserRole');
            }
        } catch (e) {
            console.error("Gagal memulihkan customerId di checkout:", e);
            localStorage.removeItem('token');
            localStorage.removeItem('loggedInCustomerId');
            localStorage.removeItem('loggedInCustomerName');
            localStorage.removeItem('loggedInUserRole');
        }
    }

    if (!token || !savedCustomerId || !savedEmail) {
        showNotification("Sesi tidak ditemukan. Silakan login kembali di halaman utama.", "error");
        setTimeout(() => window.location.href = 'index.html', 2000);
        return;
    }

    currentCustomerId = parseInt(savedCustomerId);
    cartItems = getCart();

    if (cartItems.length === 0) {
        showNotification("Keranjang Anda kosong. Silakan tambahkan barang belanjaan dahulu.", "error");
        setTimeout(() => window.location.href = 'index.html', 2000);
        return;
    }

    // Show admin portal button if logged-in user is an admin
    const userRole = localStorage.getItem('loggedInUserRole');
    const adminPortalBtn = document.getElementById('adminPortalBtn');
    if (adminPortalBtn && userRole === 'ROLE_ADMIN') {
        adminPortalBtn.classList.remove('hidden');
    }

    // 2. Fetch Customer profile (balance, default address)
    await loadCustomerProfile();

    // 3. Render items & update costs
    renderCheckoutSummary();

    // 4. Setup listeners
    setupEventListeners();
});

// Load customer data from backend
async function loadCustomerProfile() {
    try {
        currentCustomer = await UserAPI.getCustomerById(currentCustomerId);
        if (currentCustomer) {
            // Update email badge
            document.getElementById('customerEmail').innerText = currentCustomer.email;
            // Update balance
            document.getElementById('customerBalance').innerText = formatRupiah(currentCustomer.balance || 0);
            
            // Prefill shipping info if empty
            const nameField = document.getElementById('shippingReceiverName');
            const addressField = document.getElementById('shippingAddress');
            
            if (nameField && !nameField.value) {
                nameField.value = currentCustomer.name || '';
            }
            if (addressField && !addressField.value) {
                addressField.value = currentCustomer.address || '';
            }
        }
    } catch (error) {
        console.error("Error loading customer profile:", error);
        showNotification("Gagal mengambil data profil customer.", "error");
    }
}

// Render order summary details
function renderCheckoutSummary() {
    const container = document.getElementById('checkoutItemsContainer');
    const summaryItemCount = document.getElementById('summaryItemCount');
    if (!container) return;

    // Item count badge
    const totalItems = cartItems.reduce((sum, item) => sum + item.quantity, 0);
    if (summaryItemCount) {
        summaryItemCount.innerText = `${totalItems} Item`;
    }

    // Render list
    container.innerHTML = cartItems.map(item => `
        <div class="flex items-center gap-3 py-3 border-b border-gray-50 last:border-0">
            <div class="w-12 h-12 rounded-xl bg-brand-surface flex-shrink-0 flex items-center justify-center text-brand font-black text-xs">
                🛍️
            </div>
            <div class="flex-grow min-w-0">
                <h4 class="font-bold text-xs text-gray-800 truncate">${item.name}</h4>
                <p class="text-[10px] text-gray-400 font-bold">${item.quantity} x ${formatRupiah(item.price)}</p>
            </div>
            <span class="font-black text-xs text-gray-700 flex-shrink-0">${formatRupiah(item.price * item.quantity)}</span>
        </div>
    `).join('');

    updateBreakdownCosts();
}

// Update calculated fields based on selected courier
function updateBreakdownCosts() {
    const subtotal = getCartTotal();
    const serviceFee = 2000;
    const grandTotal = subtotal + selectedShippingFee + serviceFee;

    document.getElementById('summarySubtotal').innerText = formatRupiah(subtotal);
    document.getElementById('summaryShippingFee').innerText = formatRupiah(selectedShippingFee);
    document.getElementById('summarySelectedCourier').innerText = selectedCourier;
    document.getElementById('summaryTotalBill').innerText = formatRupiah(grandTotal);
}

// Handle courier selecting
function selectCourier(courierName, shippingFee) {
    selectedCourier = courierName;
    selectedShippingFee = shippingFee;

    // Reset styles on all cards
    document.querySelectorAll('.courier-card').forEach(card => {
        card.classList.remove('border-brand', 'bg-brand-surface/40');
        card.classList.add('border-gray-100');
        
        // Remove brand styling inside card elements
        const title = card.querySelector('h4');
        const price = card.querySelector('span');
        if (title) title.classList.remove('text-brand');
        if (price) {
            price.classList.remove('text-brand');
            price.classList.add('text-gray-700');
        }
    });

    // Style the active card
    const activeCard = document.getElementById(`courier_${courierName}`);
    if (activeCard) {
        activeCard.classList.remove('border-gray-100');
        activeCard.classList.add('border-brand', 'bg-brand-surface/40');
        
        const title = activeCard.querySelector('h4');
        const price = activeCard.querySelector('span');
        if (title) title.classList.add('text-brand');
        if (price) {
            price.classList.add('text-brand');
            price.classList.remove('text-gray-700');
        }
    }

    updateBreakdownCosts();
}

// Set Topup preset amounts
function setTopupPreset(amount) {
    const input = document.getElementById('topupAmount');
    if (input) {
        input.value = amount;
    }
}

// Wire standard UI interactive elements
function setupEventListeners() {
    // 1. Topup click trigger
    const topupBtn = document.getElementById('topupBtn');
    if (topupBtn) {
        topupBtn.addEventListener('click', async () => {
            const amountInput = document.getElementById('topupAmount');
            const amount = parseFloat(amountInput.value);

            if (!amount || amount <= 0) {
                showNotification("Masukkan nominal top-up yang valid.", "error");
                return;
            }

            const spinner = topupBtn.querySelector('.spinner');
            if (spinner) spinner.classList.remove('hidden');
            topupBtn.disabled = true;

            try {
                const res = await UserAPI.addBalance(currentCustomerId, amount);
                // FIX: Log topup ke localStorage agar muncul di wallet history di halaman profil
                logTopupEvent(currentCustomerId, amount);
                showNotification(`Top-up berhasil! Saldo baru: ${formatRupiah(res)}`, "success");
                amountInput.value = '';
                await loadCustomerProfile();
            } catch (error) {
                showNotification(error.message || "Gagal melakukan top-up.", "error");
            } finally {
                if (spinner) spinner.classList.add('hidden');
                topupBtn.disabled = false;
            }
        });
    }

    // 2. Pay Now event listener
    const payNowBtn = document.getElementById('payNowBtn');
    if (payNowBtn) {
        payNowBtn.addEventListener('click', async () => {
            await handlePurchaseFlow();
        });
    }
}

// ==================== SAGA POLLING ====================
// FIX: Tunggu SAGA selesai reserve stok sebelum bayar
// Polling order status sampai AWAITING_PAYMENT (max ~12 detik)
async function waitForOrderReady(orderId) {
    const MAX_RETRIES = 8;
    const POLL_INTERVAL_MS = 1500;

    for (let i = 0; i < MAX_RETRIES; i++) {
        await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL_MS));
        try {
            const order = await UserAPI.getOrderById(orderId);
            if (!order) continue;

            if (order.status === 'AWAITING_PAYMENT') {
                return { success: true, status: order.status };
            }
            if (order.status === 'CANCELLED') {
                return { success: false, status: 'CANCELLED', message: 'Stok tidak mencukupi atau terjadi error pada reservasi.' };
            }
            // Masih PENDING → lanjut polling
        } catch (e) {
            console.warn(`[SAGA Polling] Attempt ${i + 1} gagal:`, e.message);
        }
    }
    return { success: false, status: 'TIMEOUT', message: 'Proses reservasi stok memakan waktu terlalu lama. Silakan coba lagi.' };
}

// Polling order status sampai PAID (max ~12 detik)
async function waitForOrderPaid(orderId) {
    const MAX_RETRIES = 8;
    const POLL_INTERVAL_MS = 1500;

    for (let i = 0; i < MAX_RETRIES; i++) {
        await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL_MS));
        try {
            const order = await UserAPI.getOrderById(orderId);
            if (!order) continue;

            if (order.status === 'PAID') {
                return { success: true, status: order.status };
            }
            if (order.status === 'CANCELLED') {
                return { success: false, status: 'CANCELLED', message: 'Pembayaran gagal atau terjadi pembatalan.' };
            }
            // Masih PENDING atau AWAITING_PAYMENT → lanjut polling
        } catch (e) {
            console.warn(`[SAGA Polling Paid] Attempt ${i + 1} gagal:`, e.message);
        }
    }
    return { success: false, status: 'TIMEOUT', message: 'Proses konfirmasi pembayaran memakan waktu terlalu lama.' };
}

// Combined checkout/purchase processing logic
async function handlePurchaseFlow() {
    const receiverName = document.getElementById('shippingReceiverName').value.trim();
    const deliveryAddress = document.getElementById('shippingAddress').value.trim();

    if (!receiverName || !deliveryAddress) {
        showNotification("Lengkapi Nama Penerima dan Alamat Pengiriman.", "error");
        return;
    }

    const subtotal = getCartTotal();
    const serviceFee = 2000;
    const grandTotal = subtotal + selectedShippingFee + serviceFee;

    // Check balance sufficiency
    const currentBalance = currentCustomer ? currentCustomer.balance : 0;
    if (currentBalance < grandTotal) {
        showNotification(`Saldo tidak mencukupi. Silakan lakukan Top Up terlebih dahulu.`, "error");
        return;
    }

    const mainBtn = document.getElementById('payNowBtn');
    const spinner = mainBtn.querySelector('.spinner');
    if (spinner) spinner.classList.remove('hidden');
    mainBtn.disabled = true;

    // Update button text saat polling
    const btnTextNode = mainBtn.lastChild;
    const originalText = mainBtn.innerText.trim();

    try {
        // Step 1: Create Order → status PENDING
        const orderPayload = cartItems.map(item => ({
            productId: item.productId,
            quantity: item.quantity,
            price: item.price
        }));

        showNotification("Membuat pesanan...", "info");
        const createdOrder = await UserAPI.createOrder(currentCustomerId, orderPayload, selectedCourier, selectedShippingFee);
        const orderId = createdOrder.id;

        // Step 2: Tunggu SAGA selesai (PENDING → AWAITING_PAYMENT)
        // Inventory service harus berhasil reserve stok dulu via Kafka
        showNotification("Memverifikasi stok produk...", "info");
        const sagaResult = await waitForOrderReady(orderId);

        if (!sagaResult.success) {
            showNotification(`Pesanan dibatalkan: ${sagaResult.message}`, "error");
            return;
        }

        // Step 3: Bayar order (status sudah AWAITING_PAYMENT, aman untuk bayar)
        showNotification("Memproses pembayaran...", "info");
        await UserAPI.payOrder(orderId, "BALANCE");

        // Tunggu SAGA/Kafka memproses pembayaran (AWAITING_PAYMENT → PAID)
        showNotification("Memverifikasi pembayaran...", "info");
        const paidResult = await waitForOrderPaid(orderId);
        if (!paidResult.success) {
            showNotification(`Pembayaran tidak terkonfirmasi: ${paidResult.message}`, "error");
            return;
        }

        showNotification("Pesanan berhasil dibayar!", "success");

        // Step 4: Show success invoice. Shipment/resi dibuat nanti oleh admin.
        showSuccessInvoice(createdOrder, { receiverName, deliveryAddress });

    } catch (error) {
        showNotification(error.message || "Gagal memproses checkout.", "error");
    } finally {
        if (spinner) spinner.classList.add('hidden');
        mainBtn.disabled = false;
    }
}

// Render and show the final success receipt
function showSuccessInvoice(order, shipment) {
    // Hide main checkout form layout
    document.getElementById('checkoutMainLayout').classList.add('hidden');

    // Populate invoice receipt
    document.getElementById('receiptOrderNumber').innerText = order.orderNumber || `ORD-${order.id}`;
    document.getElementById('receiptDate').innerText = order.createdAt ? new Date(order.createdAt).toLocaleString('id-ID') : new Date().toLocaleString('id-ID');
    document.getElementById('receiptCourier').innerText = `${selectedCourier} (Menunggu admin jadwalkan shipment)`;
    document.getElementById('receiptReceiver').innerText = shipment ? (shipment.receiverName || '-') : '-';
    document.getElementById('receiptAddress').innerText = shipment ? (shipment.deliveryAddress || '-') : '-';

    // Render receipt item list
    const itemsListContainer = document.getElementById('receiptItemsList');
    itemsListContainer.innerHTML = cartItems.map(item => `
        <div class="flex justify-between items-center py-1 border-b border-dashed border-gray-100 last:border-0 text-xs">
            <div>
                <p class="font-bold text-gray-800">${item.name}</p>
                <p class="text-[10px] text-gray-400">${item.quantity} x ${formatRupiah(item.price)}</p>
            </div>
            <span class="font-bold text-gray-700">${formatRupiah(item.price * item.quantity)}</span>
        </div>
    `).join('');

    const subtotal = getCartTotal();
    const serviceFee = 2000;
    const grandTotal = subtotal + selectedShippingFee + serviceFee;

    document.getElementById('receiptSubtotal').innerText = formatRupiah(subtotal);
    document.getElementById('receiptShippingFee').innerText = formatRupiah(selectedShippingFee);
    document.getElementById('receiptGrandTotal').innerText = formatRupiah(grandTotal);

    // Clear cart in local storage
    clearCart();

    // Reveal success container
    document.getElementById('successInvoiceLayout').classList.remove('hidden');
}
