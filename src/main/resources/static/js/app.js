// Sinkronisasi data utama
async function syncAll() {
    try {
        // Ambil semua data secara paralel agar loading lebih cepat
        const [customers, products, allOrders] = await Promise.all([
            ApiService.fetchData('customers'),
            ApiService.fetchData('products'),
            ApiService.fetchData('orders')
        ]);

        // Langsung tampilkan semua tanpa filter dropdown
        UI.renderDashboard(customers, products, allOrders);

    } catch (e) {
        console.error("Sync Error:", e);
        UI.showNotification("🚫 Gagal memuat data riwayat transaksi!", "bg-black");
    }
}

// Handler POST Order
async function handleExecuteOrder() {
    const custSelect = document.getElementById('custId');
    const prodSelect = document.getElementById('prodId');
    
    const cid = custSelect.value;
    const pid = prodSelect.value;
    const qty = parseInt(document.getElementById('prodQty').value);
    
    // Ambil Nama untuk Notifikasi
    const customerName = custSelect.options[custSelect.selectedIndex].text;
    const productFullName = prodSelect.options[prodSelect.selectedIndex].text;
    const productName = productFullName.split(' (')[0]; // Ambil nama produk saja
    
    // Validasi Stok
    const stockMatch = productFullName.match(/Stock: (\d+)/);
    const availableStock = stockMatch ? parseInt(stockMatch[1]) : 0;
    
    if (qty > availableStock) {
        UI.showNotification(`❌ Stok tidak cukup! ${productName} sisa ${availableStock}`, "bg-red-600");
        return;
    }

    const payload = [{ productId: parseInt(pid), quantity: qty }];
    UI.updateInspector('POST', `${API}/orders?customerId=${cid}`, payload);

    try {
        const res = await ApiService.postOrder(cid, payload);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            // NOTIFIKASI INFORMATIF
            UI.showNotification(`✅ ${customerName} berhasil membeli ${productName} (x${qty})`, "bg-green-600");
            syncAll();
        } else {
            UI.showNotification(`⚠️ Gagal: ${data.message}`, "bg-orange-600");
        }
    } catch (e) {
        UI.showNotification("🚫 Kesalahan Jaringan!", "bg-black");
    }
}


async function handleUpdateStatus(id, stat) {
    UI.updateInspector('PUT', `${API}/orders/${id}/status?status=${stat}`);
    try {
        const res = await ApiService.updateOrderStatus(id, stat);
        
        
        const data = await res.json(); 
        UI.renderResponse(data); 
       
        if (res.ok) { 
            syncAll(); 
            UI.showNotification(`🚀 Order #${id} Status: ${stat}`, "bg-blue-600"); 
        }
    } catch (e) { 
        UI.showNotification("❌ Gagal Update!", "bg-red-600"); 
    }
}

async function handleAdjustStock(id, amount) {
    UI.updateInspector('PATCH', `${API}/products/${id}/adjustment?amount=${amount}`);
    try {
        const res = await ApiService.adjustStock(id, amount);

        const data = await res.json();
        UI.renderResponse(data);
       

        if (res.ok) { 
            syncAll(); 
            UI.showNotification("📦 Stok Diupdate!", "bg-purple-600"); 
        }
    } catch (e) { 
        UI.showNotification("❌ Gagal Adjust!", "bg-red-600"); 
    }
}
async function handleDeleteProduct(id, name) {
    if (!confirm(`Hapus ${name}?`)) return;
    UI.updateInspector('DELETE', `${API}/products/${id}`);
    try {
        const res = await ApiService.deleteProduct(id);
        if (res.ok) { syncAll(); UI.showNotification("🗑️ Berhasil Dihapus!", "bg-red-600"); }
        else { UI.showNotification("⚠️ Gagal! Data berelasi.", "bg-orange-600"); }
    } catch (e) { UI.showNotification("🚫 Error!"); }
}

// Initial Load
window.onload = syncAll;