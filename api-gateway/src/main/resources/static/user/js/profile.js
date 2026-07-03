// ==================== GLOBAL STATE & VARIABLES ====================
let currentCustomerId = null;
let currentCustomer = null;
let allOrders = [];
let activeOrderFilter = 'SEMUA';
let _productMapCache = null; // FIX: Cache produk untuk resolusi nama di order history

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

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', async () => {
    // 1. Session Guard Checks
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
            console.error("Gagal memulihkan customerId di profile:", e);
            localStorage.removeItem('token');
            localStorage.removeItem('loggedInCustomerId');
            localStorage.removeItem('loggedInCustomerName');
            localStorage.removeItem('loggedInUserRole');
        }
    }

    if (!token || !savedCustomerId || !savedEmail) {
        showNotification("Sesi tidak ditemukan. Silakan login terlebih dahulu.", "error");
        setTimeout(() => window.location.href = 'index.html', 1500);
        return;
    }

    currentCustomerId = parseInt(savedCustomerId);

    // Show admin portal button if logged-in user is an admin
    const userRole = localStorage.getItem('loggedInUserRole');
    const adminPortalBtn = document.getElementById('adminPortalBtn');
    if (adminPortalBtn && userRole === 'ROLE_ADMIN') {
        adminPortalBtn.classList.remove('hidden');
    }

    // 2. Fetch Profile Info
    await refreshCustomerProfile();

    // 3. Fetch Orders history
    await refreshOrdersList();

    // 4. Setup general event listeners
    setupTabListeners();
});

// Load customer data from backend
async function refreshCustomerProfile() {
    try {
        currentCustomer = await UserAPI.getCustomerById(currentCustomerId);
        if (currentCustomer) {
            // Update sidebar elements
            document.getElementById('profileName').innerText = currentCustomer.name || 'Nama Pengguna';
            document.getElementById('profileEmail').innerText = currentCustomer.email || 'user@example.com';
            document.getElementById('sidebarBalance').innerText = formatRupiah(currentCustomer.balance || 0);

            // Update avatar initial
            const initial = currentCustomer.name ? currentCustomer.name.charAt(0).toUpperCase() : 'U';
            document.getElementById('profileAvatarInitial').innerText = initial;

            // Prefill edit profile form fields
            document.getElementById('settingsName').value = currentCustomer.name || '';
            document.getElementById('settingsEmail').value = currentCustomer.email || '';
            document.getElementById('settingsAddress').value = currentCustomer.address || '';

            // Refresh financial logs history
            await refreshWalletHistory();
        }
    } catch (error) {
        console.error("Error loading customer profile:", error);
        showNotification("Gagal mengambil data profil customer.", "error");
    }
}

// Fetch orders list and render
async function refreshOrdersList() {
    const ordersContainer = document.getElementById('ordersListContainer');
    if (!ordersContainer) return;

    try {
        // FIX: Fetch orders & products bersamaan untuk resolusi nama produk
        const [orders, products] = await Promise.all([
            UserAPI.getOrders(),
            _productMapCache ? Promise.resolve(null) : UserAPI.getProducts()
        ]);
        allOrders = orders || [];
        // Bangun product map jika belum di-cache
        if (products) {
            _productMapCache = {};
            products.forEach(p => { _productMapCache[p.id] = p; });
        }
        renderOrders();
    } catch (error) {
        console.error("Error loading orders list:", error);
        ordersContainer.innerHTML = '<p class="text-red-500 text-center py-6 font-bold">Gagal memuat daftar pesanan.</p>';
    }
}

// Fetch transaction history dynamically from Payment-service + Local topup logs
async function refreshWalletHistory() {
    const container = document.getElementById('walletHistoryContainer');
    if (!container) return;

    try {
        const transactions = [];

        // 1. Fetch payments associated with client's orders
        if (allOrders && allOrders.length > 0) {
            for (const order of allOrders) {
                try {
                    const payRecord = await UserAPI.getPaymentByOrderId(order.id);
                    if (payRecord) {
                        // Successful payment belanja
                        if (payRecord.status === 'SUCCESS') {
                            transactions.push({
                                type: 'BELANJA',
                                id: payRecord.transactionId || `TX-PAY-${order.id}`,
                                orderId: order.id,
                                orderNumber: order.orderNumber,
                                amount: -payRecord.amount,
                                status: 'SUCCESS',
                                date: payRecord.paymentDate || order.createdAt
                            });
                        } 
                        // Refunded payments
                        else if (payRecord.status === 'REFUNDED') {
                            // Belanja (-)
                            transactions.push({
                                type: 'BELANJA',
                                id: payRecord.transactionId || `TX-PAY-${order.id}`,
                                orderId: order.id,
                                orderNumber: order.orderNumber,
                                amount: -payRecord.amount,
                                status: 'REFUNDED',
                                date: payRecord.paymentDate || order.createdAt
                            });
                            // Refund (+)
                            transactions.push({
                                type: 'REFUND',
                                id: `TX-REF-${order.id}`,
                                orderId: order.id,
                                orderNumber: order.orderNumber,
                                amount: payRecord.amount,
                                status: 'REFUNDED',
                                date: order.createdAt
                            });
                        }
                    }
                } catch (err) {
                    // Fail silently for order payments that don't exist yet
                }
            }
        }

        // 2. Load top-up records from localStorage
        const topupsKey = `topups_${currentCustomerId}`;
        const savedTopups = localStorage.getItem(topupsKey);
        if (savedTopups) {
            const topupList = JSON.parse(savedTopups);
            topupList.forEach(topup => {
                transactions.push({
                    type: 'TOPUP',
                    id: topup.id,
                    amount: topup.amount,
                    status: 'SUCCESS',
                    date: topup.date
                });
            });
        }

        // 3. Sort chronologically (newest first)
        transactions.sort((a, b) => new Date(b.date) - new Date(a.date));

        if (transactions.length === 0) {
            container.innerHTML = `
                <p class="text-xs text-gray-400 text-center py-6 font-semibold">Belum ada riwayat transaksi keuangan pada dompet Anda.</p>
            `;
            return;
        }

        // 4. Render html
        container.innerHTML = transactions.map(tx => {
            let typeBadge = '';
            let amountText = '';
            let icon = '';

            if (tx.type === 'TOPUP') {
                typeBadge = '<span class="text-[9px] bg-green-50 text-green-600 px-2 py-0.5 rounded-full font-bold">TOP UP</span>';
                amountText = `<span class="text-green-600 font-black">+${formatRupiah(tx.amount)}</span>`;
                icon = '💰';
            } else if (tx.type === 'REFUND') {
                typeBadge = '<span class="text-[9px] bg-teal-50 text-teal-600 px-2 py-0.5 rounded-full font-bold">REFUND</span>';
                amountText = `<span class="text-teal-600 font-black">+${formatRupiah(tx.amount)}</span>`;
                icon = '🔄';
            } else {
                const isRefunded = tx.status === 'REFUNDED';
                typeBadge = isRefunded 
                    ? '<span class="text-[9px] bg-gray-100 text-gray-400 px-2 py-0.5 rounded-full font-bold line-through">BELANJA</span>' 
                    : '<span class="text-[9px] bg-red-50 text-red-500 px-2 py-0.5 rounded-full font-bold">BELANJA</span>';
                amountText = isRefunded
                    ? `<span class="text-gray-400 font-bold line-through">${formatRupiah(Math.abs(tx.amount))}</span>`
                    : `<span class="text-red-500 font-black">-${formatRupiah(Math.abs(tx.amount))}</span>`;
                icon = '🛒';
            }

            const formattedDate = new Date(tx.date).toLocaleString('id-ID');

            return `
                <div class="flex items-center justify-between p-3.5 bg-white border border-gray-100 rounded-2xl shadow-sm hover:border-brand/10 transition">
                    <div class="flex items-center gap-3">
                        <div class="w-9 h-9 bg-gray-50 rounded-xl flex items-center justify-center text-sm">${icon}</div>
                        <div>
                            <div class="flex items-center gap-1.5">
                                <h4 class="font-black text-xs text-gray-700">${tx.type === 'BELANJA' ? (tx.orderNumber || 'Pembelian') : (tx.type === 'REFUND' ? 'Refund Saldo' : 'Isi Saldo Wallet')}</h4>
                                ${typeBadge}
                            </div>
                            <p class="text-[9px] text-gray-400 font-bold mt-0.5">${formattedDate} | ID: ${tx.id}</p>
                        </div>
                    </div>
                    <div class="text-right text-xs">
                        ${amountText}
                    </div>
                </div>
            `;
        }).join('');

    } catch (error) {
        console.error("Error loading wallet history:", error);
        container.innerHTML = '<p class="text-red-500 text-xs text-center py-4 font-semibold">Gagal memuat mutasi saldo.</p>';
    }
}

// Log a local top-up transaction event to persist history
function logTopupEvent(amount) {
    const topupsKey = `topups_${currentCustomerId}`;
    const savedTopups = localStorage.getItem(topupsKey);
    const topupList = savedTopups ? JSON.parse(savedTopups) : [];

    topupList.push({
        id: `TX-TOP-${Date.now()}`,
        amount: amount,
        date: new Date().toISOString()
    });

    localStorage.setItem(topupsKey, JSON.stringify(topupList));
}

// Render dynamic orders in list
async function renderOrders() {
    const container = document.getElementById('ordersListContainer');
    if (!container) return;

    // Filter orders
    let filtered = allOrders;
    if (activeOrderFilter !== 'SEMUA') {
        filtered = allOrders.filter(order => order.status === activeOrderFilter);
    }

    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="bg-white rounded-3xl p-10 text-center border border-gray-100 shadow-sm custom-shadow text-gray-400 font-bold">
                <span class="text-4xl block mb-2">📦</span>
                Belum ada pesanan dengan status ini.
            </div>
        `;
        return;
    }

    let html = '';
    for (const order of filtered) {
        // Fetch shipment info if paid or shipped
        let shippingInfo = null;
        if (order.status !== 'PENDING' && order.status !== 'CANCELLED') {
            try {
                shippingInfo = await UserAPI.getShipmentByOrder(order.id);
            } catch (e) {
                console.error("Error fetching shipment detail:", e);
            }
        }

        // Setup badge styles
        let badgeClass = 'bg-gray-100 text-gray-700';
        if (order.status === 'PENDING' || order.status === 'AWAITING_PAYMENT') {
            badgeClass = 'bg-yellow-100 text-yellow-700';
        } else if (order.status === 'CANCELLED') {
            badgeClass = 'bg-red-100 text-red-700';
        } else if (order.status === 'PAID' || order.status === 'PROCESSING') {
            badgeClass = 'bg-blue-100 text-blue-700';
        } else if (order.status === 'SHIPPED') {
            badgeClass = 'bg-indigo-100 text-indigo-700';
        } else if (order.status === 'COMPLETED' || order.status === 'DELIVERED') {
            badgeClass = 'bg-green-100 text-green-700';
        }

        // Contextual buttons logic
        let actionButtons = '';
        if (order.status === 'PENDING' || order.status === 'AWAITING_PAYMENT') {
            actionButtons = `
                <div class="mt-4 pt-4 border-t border-gray-100 flex gap-3 items-center justify-end">
                    <button onclick="cancelOrder(${order.id})" class="text-xs font-bold text-red-500 hover:underline">Batalkan Pesanan</button>
                    <button onclick="payOrder(${order.id})" class="bg-brand text-white text-xs px-4 py-2 rounded-xl hover:bg-brand/90 transition font-bold shadow-sm shadow-brand/10">Bayar Sekarang</button>
                </div>
            `;


        // Stepper status numbers matching microservice Saga flow
        // Stepper levels: 1=Ordered/Pending, 2=AwaitingPayment/Reserved, 3=Paid/Processing, 4=Shipped, 5=Completed/Delivered
        let step = 1;
        if (order.status === 'AWAITING_PAYMENT') step = 2;
        else if (order.status === 'PAID' || order.status === 'PROCESSING') step = 3;
        else if (order.status === 'SHIPPED') step = 4;
        else if (order.status === 'COMPLETED' || order.status === 'DELIVERED') step = 5;

        const dateStr = new Date(order.createdAt).toLocaleString('id-ID');

        html += `
            <div class="bg-white rounded-3xl p-6 border border-gray-100 shadow-sm custom-shadow space-y-4">
                <!-- Header Info -->
                <div class="flex justify-between items-start flex-wrap gap-2">
                    <div>
                        <p class="font-black text-brand text-sm">${order.orderNumber || 'Order #' + order.id}</p>
                        <p class="text-[10px] text-gray-400 font-bold mt-0.5">${dateStr}</p>
                    </div>
                    <div class="text-right">
                        <p class="font-black text-gray-800">${formatRupiah(order.totalAmount || 0)}</p>
                        <span class="inline-block text-[10px] px-2.5 py-1 rounded-full font-bold uppercase mt-1 ${badgeClass}">
                            ${order.status}
                        </span>
                    </div>
                </div>

                <!-- Item Breakdown list -->
                <div class="bg-gray-50 border rounded-2xl p-4 text-xs font-semibold text-gray-600 space-y-2">
                    ${order.items ? order.items.map(item => {
                        const pm = _productMapCache || {};
                        const productName = item.product?.name || pm[item.productId]?.name || `Produk #${item.productId}`;
                        return `
                        <div class="flex justify-between items-center">
                            <span>${productName} x${item.quantity}</span>
                            <span class="font-bold text-gray-700">${formatRupiah(item.price * item.quantity)}</span>
                        </div>
                    `}).join('') : ''}
                </div>

                <!-- Shipment details if present -->
                ${shippingInfo ? `
                    <div class="bg-brand-surface/40 border border-brand/5 rounded-2xl p-4 text-xs font-semibold text-gray-600 space-y-1.5">
                        <p class="text-brand font-black text-[10px] uppercase tracking-wider">Detail Pengiriman</p>
                        <p class="text-gray-700">🚚 Kurir: <span class="font-bold">${shippingInfo.courierName || 'JNE'}</span> | Resi: <span class="font-bold">${shippingInfo.trackingNumber || 'PROSES SAGA'}</span></p>
                        <p class="text-gray-500">Penerima: ${shippingInfo.receiverName} | Alamat: ${shippingInfo.deliveryAddress}</p>
                    </div>
                ` : (order.courierName && (order.status === 'PAID' || order.status === 'PROCESSING' || order.status === 'SHIPPED' || order.status === 'COMPLETED') ? `
                    <div class="bg-brand-surface/40 border border-brand/5 rounded-2xl p-4 text-xs font-semibold text-gray-600 space-y-1.5">
                        <p class="text-brand font-black text-[10px] uppercase tracking-wider">Detail Pengiriman</p>
                        <p class="text-gray-700">🚚 Kurir: <span class="font-bold">${order.courierName}</span> | Resi: <span class="font-bold text-orange-500">Menunggu Penjadwalan Admin</span></p>
                        <p class="text-gray-500">Penerima: ${currentCustomer ? currentCustomer.name : '-'} | Alamat: ${currentCustomer ? currentCustomer.address : '-'}</p>
                    </div>
                ` : '')}

                <!-- Saga Flow Timeline Stepper (Only show if not cancelled) -->
                ${order.status !== 'CANCELLED' ? `
                    <div class="pt-4 border-t border-gray-50">
                        <p class="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-4">Lacak Status Saga</p>
                        
                        <div class="flex items-center justify-between text-[10px] font-bold text-gray-400">
                            <div class="flex flex-col items-center ${step >= 1 ? 'text-brand' : ''}">
                                <div class="w-6 h-6 rounded-full flex items-center justify-center border-2 mb-1.5 text-xs font-black ${step >= 1 ? 'border-brand bg-brand/10 text-brand' : 'border-gray-200 bg-gray-50'}">1</div>
                                <span>Dibuat</span>
                            </div>
                            <div class="flex-1 h-0.5 -translate-y-4 ${step >= 2 ? 'bg-brand' : 'bg-gray-100'} mx-2"></div>
                            <div class="flex flex-col items-center ${step >= 2 ? 'text-brand' : ''}">
                                <div class="w-6 h-6 rounded-full flex items-center justify-center border-2 mb-1.5 text-xs font-black ${step >= 2 ? 'border-brand bg-brand/10 text-brand' : 'border-gray-200 bg-gray-50'}">2</div>
                                <span>Stok Aman</span>
                            </div>
                            <div class="flex-1 h-0.5 -translate-y-4 ${step >= 3 ? 'bg-brand' : 'bg-gray-100'} mx-2"></div>
                            <div class="flex flex-col items-center ${step >= 3 ? 'text-brand' : ''}">
                                <div class="w-6 h-6 rounded-full flex items-center justify-center border-2 mb-1.5 text-xs font-black ${step >= 3 ? 'border-brand bg-brand/10 text-brand' : 'border-gray-200 bg-gray-50'}">3</div>
                                <span>Lunas</span>
                            </div>
                            <div class="flex-1 h-0.5 -translate-y-4 ${step >= 4 ? 'bg-brand' : 'bg-gray-100'} mx-2"></div>
                            <div class="flex flex-col items-center ${step >= 4 ? 'text-brand' : ''}">
                                <div class="w-6 h-6 rounded-full flex items-center justify-center border-2 mb-1.5 text-xs font-black ${step >= 4 ? 'border-brand bg-brand/10 text-brand' : 'border-gray-200 bg-gray-50'}">4</div>
                                <span>Dikirim</span>
                            </div>
                            <div class="flex-1 h-0.5 -translate-y-4 ${step >= 5 ? 'bg-brand' : 'bg-gray-100'} mx-2"></div>
                            <div class="flex flex-col items-center ${step >= 5 ? 'text-brand' : ''}">
                                <div class="w-6 h-6 rounded-full flex items-center justify-center border-2 mb-1.5 text-xs font-black ${step >= 5 ? 'border-brand bg-brand/10 text-brand' : 'border-gray-200 bg-gray-50'}">5</div>
                                <span>Selesai</span>
                            </div>
                        </div>
                    </div>
                ` : `
                    <div class="bg-red-50/50 border border-red-100 rounded-2xl p-4 text-xs font-semibold text-red-500">
                        ⚠️ Pesanan dibatalkan. Dana (jika sudah bayar) & stok barang telah dikembalikan secara kompensasi Saga.
                    </div>
                `}

                <!-- Contextual Action Buttons -->
                ${actionButtons}
            </div>
        `;
    }
    container.innerHTML = html;
}

// Filter orders in history
// FIX: Terima parameter `clickedBtn` agar tidak menggunakan window.event yang deprecated
function filterOrders(statusFilter, clickedBtn = null) {
    activeOrderFilter = statusFilter;
    
    // Toggle active filter button styling
    document.querySelectorAll('.order-filter-btn').forEach(btn => {
        btn.classList.remove('bg-brand', 'text-white', 'shadow-sm');
        btn.classList.add('bg-gray-100', 'text-gray-500');
    });

    // FIX: Gunakan clickedBtn yang diteruskan, bukan window.event yang deprecated
    if (clickedBtn) {
        clickedBtn.classList.add('bg-brand', 'text-white', 'shadow-sm');
        clickedBtn.classList.remove('bg-gray-100', 'text-gray-500');
    }

    renderOrders();
}

// ==================== SIDEBAR TAB NAVIGATION ====================
function switchTab(tabName) {
    // 1. Hide all panels
    document.getElementById('panel_orders').classList.add('hidden');
    document.getElementById('panel_wallet').classList.add('hidden');
    document.getElementById('panel_settings').classList.add('hidden');

    // 2. Remove active styling on tab buttons
    document.getElementById('tab_orders').className = "w-full flex items-center gap-3.5 px-4 py-3 rounded-2xl font-bold text-sm transition-all duration-200 inactive-tab";
    document.getElementById('tab_wallet').className = "w-full flex items-center gap-3.5 px-4 py-3 rounded-2xl font-bold text-sm transition-all duration-200 inactive-tab";
    document.getElementById('tab_settings').className = "w-full flex items-center gap-3.5 px-4 py-3 rounded-2xl font-bold text-sm transition-all duration-200 inactive-tab";

    // 3. Show active panel & button styling
    document.getElementById(`panel_${tabName}`).classList.remove('hidden');
    document.getElementById(`tab_${tabName}`).className = "w-full flex items-center gap-3.5 px-4 py-3 rounded-2xl font-bold text-sm transition-all duration-200 active-tab";
}

// Set top up preset amount
function setTopupAmount(amount) {
    document.getElementById('topupInput').value = amount;
}

// Setup core tab form listener handlers
function setupTabListeners() {
    // 1. Topup balance button
    const topupActionBtn = document.getElementById('topupActionBtn');
    if (topupActionBtn) {
        topupActionBtn.addEventListener('click', async () => {
            const input = document.getElementById('topupInput');
            const amount = parseFloat(input.value);

            if (!amount || amount <= 0) {
                showNotification("Masukkan nominal pengisian saldo yang valid.", "error");
                return;
            }

            const spinner = topupActionBtn.querySelector('.spinner');
            if (spinner) spinner.classList.remove('hidden');
            topupActionBtn.disabled = true;

            try {
                const res = await UserAPI.addBalance(currentCustomerId, amount);
                
                // Log this topup transaction locally
                logTopupEvent(amount);

                showNotification(`Top-up berhasil! Saldo Wallet: ${formatRupiah(res)}`, "success");
                input.value = '';
                await refreshCustomerProfile();
            } catch (error) {
                showNotification(error.message || "Gagal mengisi saldo.", "error");
            } finally {
                if (spinner) spinner.classList.add('hidden');
                topupActionBtn.disabled = false;
            }
        });
    }

    // 2. Save profile updates button
    const saveProfileBtn = document.getElementById('saveProfileBtn');
    if (saveProfileBtn) {
        saveProfileBtn.addEventListener('click', async () => {
            const name = document.getElementById('settingsName').value.trim();
            const address = document.getElementById('settingsAddress').value.trim();

            if (!name || !address) {
                showNotification("Nama dan Alamat tidak boleh dikosongkan.", "error");
                return;
            }

            const spinner = saveProfileBtn.querySelector('.spinner');
            if (spinner) spinner.classList.remove('hidden');
            saveProfileBtn.disabled = true;

            try {
                const payload = {
                    name: name,
                    email: currentCustomer.email,
                    address: address
                };

                await UserAPI.updateCustomer(currentCustomerId, payload);
                showNotification("Profil berhasil diperbarui!", "success");
                await refreshCustomerProfile();
            } catch (error) {
                showNotification(error.message || "Gagal memperbarui profil.", "error");
            } finally {
                if (spinner) spinner.classList.add('hidden');
                saveProfileBtn.disabled = false;
            }
        });
    }

    // 3. Logout action
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            // Hapus session storage dan redirect ke beranda
            localStorage.removeItem('token');
            localStorage.removeItem('loggedInCustomerId');
            localStorage.removeItem('loggedInCustomerName');
            localStorage.removeItem('loggedInUserRole');
            showNotification("Anda berhasil keluar.", "info");
            setTimeout(() => window.location.href = 'index.html', 1000);
        });
    }
}

// ==================== REST ACTION HANDLERS ====================
async function payOrder(orderId) {
    if (!confirm("Konfirmasi proses bayar sekarang menggunakan saldo Wallet Anda?")) return;
    try {
        await UserAPI.payOrder(orderId, "BALANCE");
        showNotification("Pembayaran berhasil diproses!", "success");
        await refreshCustomerProfile();
        await refreshOrdersList();
    } catch (error) {
        showNotification(error.message || "Pembayaran gagal.", "error");
    }
}

async function cancelOrder(orderId) {
    if (!confirm("Konfirmasi pembatalan pesanan ini? Stok barang akan dibebaskan.")) return;
    try {
        await UserAPI.cancelOrder(orderId);
        showNotification("Pesanan berhasil dibatalkan.", "success");
        await refreshOrdersList();
    } catch (error) {
        showNotification(error.message || "Gagal membatalkan pesanan.", "error");
    }
}


