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

// Aksi: Saat tombol "APPROVE PAY" di menu Order diklik
// Secara ideal di EAI, ini memanggil endpoint khusus /pay
async function handleApprovePayment(orderId) {
    if(confirm(`Konfirmasi pembayaran untuk Order #${orderId}? \nIni akan mengunci reservasi stok secara permanen.`)) {
        try {
            // Karena kita butuh konfirmasi pembayaran & integrasi kurir, panggil status PAID (atau endpoint pay jika Anda sudah buat di controller)
            await AdminAPI.updateOrderStatus(orderId, 'PAID'); 
            initAdmin(true); // Refresh data setelah berhasil
        } catch (e) {
            alert("Gagal memproses pembayaran");
        }
    }
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

    try {
        await AdminAPI.createCustomer({ name, email, address: "-" });
        document.getElementById('formAddCustomer').reset();
        initAdmin(true);
        AdminUI.showNotification("Customer berhasil didaftarkan!", "bg-green-600");
    } catch (e) {
        alert("Gagal menambahkan customer.");
    }
}
// Jalankan initAdmin saat halaman web pertama kali selesai dimuat
document.addEventListener('DOMContentLoaded', initAdmin);