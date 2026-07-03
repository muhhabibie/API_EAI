/**
 * EAI Admin Application Controller
 * Mengatur alur logika inisialisasi dan aksi dari pengguna.
 */

// ==========================================
// LOGIN HANDLER
// ==========================================
async function handleAdminLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    
    try {
        const response = await AdminAPI.login(username, password);
        
        // Token sudah disimpan di AdminAPI.login()
        console.log("Login berhasil!");
        
        // Sembunyikan modal login
        document.getElementById('loginModal').classList.add('hidden');
        
        // Tampilkan dashboard
        document.querySelector('nav').style.display = '';
        document.querySelector('main').style.display = '';
        document.querySelector('.max-w-7xl').style.display = '';
        
        // Initialize dashboard
        initAdmin();
        
    } catch (error) {
        console.error("Login gagal:", error);
        alert("Login gagal: " + (error.message || "Username atau password salah"));
    }
}

window.globalOrders = [];
// Fungsi inisialisasi utama (Dipanggil saat web dimuat atau tombol SYNC diklik)
async function initAdmin(silent = false) {
    if(!silent) console.log("Memulai sinkronisasi data EAI...");
    
    const syncText = document.getElementById('apiSyncText');
    const syncDot = document.getElementById('apiSyncDot');
    
    try {
        // Teruskan parameter 'silent' ke semua fungsi API
        const results = await Promise.all([
            AdminAPI.getCustomers(silent), 
            AdminAPI.getProducts(silent), 
            AdminAPI.getOrders(silent),
            AdminAPI.getCategories(silent), 
            AdminAPI.getShipments(silent), 
            AdminAPI.getReservations(silent)
        ]);
        
        const [customers, products, orders, categories, shipments, reservations] = results;

        window.globalOrders = orders;
        AdminUI.renderDashboard(customers, products, orders, categories, shipments, reservations);
        
        if(!silent) console.log("Sinkronisasi berhasil.");
        
        // Indikator Sehat
        if(syncText && syncDot) {
            syncText.innerText = "Connected";
            syncText.className = "text-2xl font-black text-green-600";
            syncDot.className = "w-3 h-3 bg-green-500 rounded-full animate-pulse mb-2";
        }
    } catch (error) {
        console.error("Kesalahan sistem saat sinkronisasi:", error);
        
        // Indikator Error EAI
        if(syncText && syncDot) {
            syncText.innerText = "Offline";
            syncText.className = "text-2xl font-black text-red-600";
            syncDot.className = "w-3 h-3 bg-red-500 rounded-full mb-2";
        }
        AdminUI.showNotification("Gagal menghubungi server Backend EAI. Pastikan semua services running di port 8081-8086.", "bg-red-600");
    }
}

// ==========================================
// ACTION HANDLERS (Fungsi Tombol)
// ==========================================

// Aksi: Proses pembayaran resmi (memanggil Payment microservice)
async function handleProcessPayment(orderId) {
    if(confirm(`Proses pembayaran resmi untuk Order #${orderId} menggunakan saldo customer?`)) {
        try {
            await AdminAPI.processPayment(orderId, 'BALANCE');
            initAdmin(true); // Refresh data setelah berhasil
            AdminUI.showNotification("Pembayaran berhasil diproses!", "bg-green-600");
        } catch (e) {
            alert("Gagal memproses pembayaran: " + e.message);
        }
    }
}

function isShipmentDispatchable(shipment) {
    return shipment && (shipment.status === 'PENDING' || shipment.status === 'PROCESSING' || shipment.status === 'PICKED_UP');
}

async function handleDispatchExistingShipment(shipment) {
    if (!shipment) return false;

    if (isShipmentDispatchable(shipment)) {
        await AdminAPI.updateShipmentStatus(shipment.id, 'IN_TRANSIT');
        return true;
    }

    if (shipment.status === 'IN_TRANSIT') {
        await AdminAPI.updateShipmentStatus(shipment.id, 'DELIVERED');
        return true;
    }

    return false;
}

// Aksi: Buat manifest pengiriman baru, atau lanjutkan shipment existing jika resi sudah ada
async function handleCreateShipment(orderId, receiverName, deliveryAddress, courierName, shippingFee) {
    const courier = courierName || "JNE";
    const fee = shippingFee || 15000;
    const existingShipment = window._shipmentMap ? window._shipmentMap[orderId] : null;

    if (existingShipment) {
        if (!isShipmentDispatchable(existingShipment) && existingShipment.status !== 'IN_TRANSIT') {
            AdminUI.showNotification(`Shipment Order #${orderId} sudah berstatus ${existingShipment.status}`, "bg-slate-700");
            return;
        }

        if (confirm(`Shipment untuk Order #${orderId} sudah punya resi ${existingShipment.trackingNumber}.\nLanjutkan status pengiriman sekarang?`)) {
            try {
                const changed = await handleDispatchExistingShipment(existingShipment);
                initAdmin(true);
                AdminUI.showNotification(changed ? "Status pengiriman berhasil diperbarui!" : "Tidak ada status yang perlu diubah.", "bg-green-600");
            } catch (e) {
                alert("Gagal update status pengiriman: " + e.message);
            }
        }
        return;
    }
    
    if (confirm(`Konfirmasi pembuatan pengiriman untuk Order #${orderId}?\nKurir: ${courier}\nOngkir: Rp ${fee.toLocaleString('id-ID')}`)) {
        try {
            await AdminAPI.createShipment(orderId, courier, receiverName, deliveryAddress || 'Alamat Default', fee);
            initAdmin(true);
            AdminUI.showNotification("Pengiriman berhasil didaftarkan!", "bg-green-600");
            navigate('shipping-section');
        } catch (e) {
            if ((e.message || '').toLowerCase().includes('shipment sudah ada')) {
                try {
                    const shipment = await AdminAPI.getShipmentByOrder(orderId);
                    const changed = await handleDispatchExistingShipment(shipment);
                    initAdmin(true);
                    AdminUI.showNotification(changed ? "Shipment sudah ada, status pengiriman dilanjutkan!" : `Shipment sudah berstatus ${shipment.status}`, "bg-green-600");
                    navigate('shipping-section');
                    return;
                } catch (lookupError) {
                    alert("Shipment sudah ada, tapi gagal mengambil detailnya: " + lookupError.message);
                    return;
                }
            }
            alert("Gagal membuat pengiriman: " + e.message);
        }
    }
}

// Aksi: Batalkan order reguler (sebelum bayar)
async function handleCancelOrder(orderId) {
    if (confirm(`Apakah Anda yakin ingin membatalkan Order #${orderId}? \nStok yang direservasi akan dikembalikan.`)) {
        try {
            await AdminAPI.cancelOrder(orderId);
            initAdmin(true);
            AdminUI.showNotification("Pesanan berhasil dibatalkan.", "bg-green-600");
        } catch (e) {
            alert("Gagal membatalkan pesanan: " + e.message);
        }
    }
}

// Aksi: Batalkan order yang sudah PAID (belum dikirim) — dengan alasan
function handleCancelPaidOrder(orderId) {
    // Hapus modal lama jika ada
    const existing = document.getElementById('cancelRefundModal');
    if (existing) existing.remove();

    const reasons = [
        'Barang fisik jelek/rusak saat diterima',
        'Pembeli tidak menerima barang',
        'Salah produk dikirim',
        'Pembatalan atas kesepakatan bersama',
        'Lainnya'
    ];

    const optionsHtml = reasons.map((r, i) =>
        `<label class="flex items-center gap-2 p-2 rounded hover:bg-red-50 cursor-pointer">
            <input type="radio" name="cancelReason" value="${r}" ${i === 0 ? 'checked' : ''} class="accent-red-600">
            <span class="text-xs text-slate-700">${r}</span>
        </label>`
    ).join('');

    const modal = document.createElement('div');
    modal.id = 'cancelRefundModal';
    modal.className = 'fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm';
    modal.innerHTML = `
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-sm mx-4 overflow-hidden">
            <div class="bg-red-600 px-5 py-4">
                <h3 class="text-white font-black text-sm">⚠ Batalkan & Refund Order #${orderId}</h3>
                <p class="text-red-100 text-[11px] mt-0.5">Tindakan ini akan mengembalikan stok & saldo customer via SAGA.</p>
            </div>
            <div class="p-5">
                <p class="text-xs font-bold text-slate-600 mb-3">Pilih Alasan Pembatalan:</p>
                <div class="space-y-1 mb-4">${optionsHtml}</div>
                <div class="flex gap-2 justify-end pt-3 border-t border-slate-100">
                    <button id="cancelRefundClose" class="px-4 py-2 rounded-lg text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 transition">Batal</button>
                    <button id="cancelRefundConfirm" class="px-4 py-2 rounded-lg text-xs font-bold bg-red-600 hover:bg-red-700 text-white transition shadow-sm">Konfirmasi Refund</button>
                </div>
            </div>
        </div>
    `;
    document.body.appendChild(modal);

    document.getElementById('cancelRefundClose').onclick = () => modal.remove();
    modal.addEventListener('click', (e) => { if (e.target === modal) modal.remove(); });

    document.getElementById('cancelRefundConfirm').onclick = async () => {
        const selected = modal.querySelector('input[name="cancelReason"]:checked');
        const reason = selected ? selected.value : reasons[0];
        modal.remove();
        try {
            await AdminAPI.cancelPaidOrder(orderId, reason);
            initAdmin(true);
            AdminUI.showNotification(`Order #${orderId} dibatalkan. Refund sedang diproses.`, 'bg-green-600');
        } catch (e) {
            alert('Gagal membatalkan pesanan: ' + e.message);
        }
    };
}


// Aksi: Saat kurir mengirim barang (Tombol "SET IN TRANSIT" di menu Shipping)
async function handleShipItem(shipmentId) {
    if(confirm(`Tandai pengiriman #${shipmentId} sedang dalam perjalanan? \n(Ini akan mencatat waktu 'Shipped' secara otomatis)`)) {
        try {
            // PERBAIKAN: Menggunakan IN_TRANSIT agar sesuai dengan backend Java
            await AdminAPI.updateShipmentStatus(shipmentId, 'IN_TRANSIT');
            initAdmin(true); // Refresh seluruh dashboard
            AdminUI.showNotification("Status pengiriman diperbarui menjadi IN_TRANSIT", "bg-green-600");
        } catch (e) {
            alert("Gagal update status pengiriman");
        }
    }
}

// Aksi: Saat barang sampai tujuan (Tombol "DELIVERED" di menu Shipping)
async function handleDeliverItem(shipmentId) {
    if(confirm(`Tandai barang telah diterima? \n(Ini otomatis akan mengubah status Order menjadi COMPLETED)`)) {
        try {
            await AdminAPI.updateShipmentStatus(shipmentId, 'DELIVERED');
            initAdmin(true); // Refresh seluruh dashboard
        } catch (e) {
            alert("Gagal update status pengiriman");
        }
    }
}

// FUNGSI ADVANCED FILTER (Real-time & Smart ID Search)
function filterOrders() {
    const rawSearch = document.getElementById('orderSearch').value.toLowerCase().trim();
    const statusFilter = document.getElementById('filterOrderStatus').value;

    // 1. Deteksi apakah admin ingin mencari KHUSUS berdasarkan ID
    const isSearchByIdOnly = rawSearch.startsWith('#');

    // 2. DAPATKAN ANGKA MURNI (Baris ini terhapus di kode Anda sebelumnya)
    const searchText = rawSearch.replace('#', '').trim(); 

    // Lakukan filter dari data master (globalOrders)
    const filteredOrders = window.globalOrders.filter(o => {
        // Filter berdasarkan status dropdown
        const matchStatus = statusFilter === 'ALL' || o.status === statusFilter;
        
        // Jika kotak pencarian kosong, langsung kembalikan hasil filter status
        if (searchText === '') return matchStatus;

        // Persiapan data untuk pencocokan
        const orderId = o.id.toString();
        const orderNum = o.orderNumber ? o.orderNumber.toLowerCase() : '';

        let matchSearch = false;

        if (isSearchByIdOnly) {
            // LOGIKA BARU: Harus EXACT MATCH (Pencocokan Pasti)
            // Menghapus .includes() agar mencari #1 TIDAK memunculkan #10 atau #11
            matchSearch = (orderId === searchText);
        } else {
            // PENCARIAN UMUM: Cari berdasarkan Referensi atau bagian dari ID
            matchSearch = orderNum.includes(searchText) || orderId.includes(searchText);
        }

        return matchStatus && matchSearch;
    });

    // Render ulang hanya bagian tabel order
    AdminUI.renderOrderTableOnly(filteredOrders);
}

// Aksi: Saat admin mengatur stok manual (Stock Adjustment)
async function handleStockAdjustment() {
    const productId = document.getElementById('adjProductId').value;
    const qty = document.getElementById('adjQty').value;

    if (!productId || !qty) {
        alert("Pilih produk dan masukkan jumlah qty!");
        return;
    }

    if (confirm(`Anda yakin ingin menyesuaikan stok sebanyak ${qty} unit?`)) {
        try {
            await AdminAPI.updateProductStock(productId, qty);
            
            // Reset form dan ambil data terbaru
            document.getElementById('adjQty').value = '';
            initAdmin(true); 
            alert("Stok berhasil diperbarui!");
        } catch (e) {
            alert("Gagal update stok. Pastikan backend Spring Boot sudah memiliki endpoint PUT /api/products/{id}/stock");
        }
    }
}
// ==========================================
// MASTER DATA ACTION HANDLERS
// ==========================================

async function handleAddProduct(event) {
    event.preventDefault(); // Mencegah halaman reload
    const name = document.getElementById('newProdName').value;
    const price = parseFloat(document.getElementById('newProdPrice').value);
    const stock = parseInt(document.getElementById('newProdStock').value);

    try {
        await AdminAPI.createProduct({ name, price, stock, categoryId: 1 }); // Default category 1 sbg contoh
        document.getElementById('formAddProduct').reset();
        initAdmin(true); // Refresh data
        AdminUI.showNotification("Product berhasil ditambahkan!", "bg-blue-600");
    } catch (e) {
        alert("Gagal menambahkan produk. Pastikan endpoint POST /api/products tersedia.");
    }
}

async function handleDeleteProduct(id) {
    if (confirm(`Hati-hati! Menghapus produk #${id} dapat merusak histori pesanan jika produk tersebut pernah dibeli. Lanjutkan?`)) {
        try {
            await AdminAPI.deleteProduct(id);
            initAdmin(true);
            AdminUI.showNotification("Product berhasil dihapus!", "bg-red-600");
        } catch (e) {
            alert("Gagal menghapus produk. Kemungkinan ada constraint database (produk ini sudah ada di tabel pesanan).");
        }
    }
}

async function handleAddCustomer(event) {
    event.preventDefault();
    const name = document.getElementById('newCustName').value;
    const email = document.getElementById('newCustEmail').value;
    const username = email.split('@')[0].replace(/[^a-zA-Z0-9]/g, '_');
    const password = "password123"; // Default password for new customers

    try {
        await AdminAPI.createCustomer({ username, name, email, password, address: "-" });
        document.getElementById('formAddCustomer').reset();
        initAdmin(true);
        AdminUI.showNotification("Customer berhasil didaftarkan!", "bg-green-600");
    } catch (e) {
        alert("Gagal menambahkan customer.");
    }
}
function handleAdminLogout() {
    // Hapus token dan data dari Storage
    localStorage.removeItem('token'); 
    localStorage.removeItem('loggedInCustomerId');
    localStorage.removeItem('loggedInCustomerName');
    localStorage.removeItem('loggedInUserRole');
    
    // Tampilkan login modal dan sembunyikan dashboard
    document.getElementById('loginModal').classList.remove('hidden');
    document.querySelector('nav').style.display = 'none';
    document.querySelector('main').style.display = 'none';
    document.querySelector('.max-w-7xl').style.display = 'none';
    
    console.log("Admin logged out.");
}
// Jalankan initAdmin saat halaman web pertama kali selesai dimuat
document.addEventListener('DOMContentLoaded', initAdmin);
