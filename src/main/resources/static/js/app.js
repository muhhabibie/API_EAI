// Sinkronisasi data utama
async function syncAll() {
    try {
        // Ambil semua data secara paralel
        const [customers, products, allOrders, categories] = await Promise.all([
            ApiService.fetchData('customers'),
            ApiService.fetchData('products'),
            ApiService.fetchData('orders'),
            ApiService.fetchData('categories')
        ]);

        // Update dropdown kategori untuk Create Product
        const catSelect = document.getElementById('newProdCategory');
        if (catSelect) {
            catSelect.innerHTML = '<option value="">Pilih Kategori</option>' + 
                categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }

        // Tampilkan semua data
        UI.renderDashboard(customers, products, allOrders, categories);

    } catch (e) {
        console.error("Sync Error:", e);
        UI.showNotification("🚫 Gagal memuat data!", "bg-black");
    }
}

// Handler POST Order
async function handleExecuteOrder() {
    const custSelect = document.getElementById('custId');
    const prodSelect = document.getElementById('prodId');
    
    const cid = custSelect.value;
    const pid = prodSelect.value;
    const qty = parseInt(document.getElementById('prodQty').value);
    
    const customerName = custSelect.options[custSelect.selectedIndex]?.text || '';
    const productFullName = prodSelect.options[prodSelect.selectedIndex]?.text || '';
    const productName = productFullName.split(' (')[0];
    
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
            UI.showNotification(`✅ ${customerName} berhasil membeli ${productName} (x${qty})`, "bg-green-600");
            syncAll();
        } else {
            UI.showNotification(`⚠️ Gagal: ${data.message || data.error}`, "bg-orange-600");
        }
    } catch (e) {
        UI.showNotification("🚫 Kesalahan Jaringan!", "bg-black");
    }
}

// Update status order
async function handleUpdateStatus(id, stat) {
    UI.updateInspector('PUT', `${API}/orders/${id}/status?status=${stat}`);
    try {
        const res = await ApiService.updateOrderStatus(id, stat);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            syncAll();
            UI.showNotification(`🚀 Order #${id} Status: ${stat}`, "bg-blue-600");
        } else {
            UI.showNotification(`❌ Gagal Update: ${data.message}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("❌ Gagal Update!", "bg-red-600");
    }
}

// Cancel order
async function handleCancelOrder(orderId) {
    if (!confirm(`Batalkan order #${orderId}? Stok akan dikembalikan.`)) return;
    UI.updateInspector('POST', `${API}/orders/${orderId}/cancel`);
    try {
        const res = await fetch(`${API}/orders/${orderId}/cancel`, { method: 'POST' });
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            UI.showNotification(`✅ Order #${orderId} dibatalkan, stok dikembalikan`, "bg-green-600");
            syncAll();
        } else {
            UI.showNotification(`❌ Gagal: ${data.message || data.error}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error jaringan", "bg-black");
    }
}

// Adjust stok product (increment/decrement)
async function handleAdjustStock(id, amount) {
    UI.updateInspector('PATCH', `${API}/products/${id}/adjustment?amount=${amount}`);
    try {
        const res = await ApiService.adjustStock(id, amount);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            syncAll();
            UI.showNotification("📦 Stok Diupdate!", "bg-purple-600");
        } else {
            UI.showNotification(`❌ Gagal: ${data.message}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("❌ Gagal Adjust!", "bg-red-600");
    }
}

// Delete product
async function handleDeleteProduct(id, name) {
    if (!confirm(`Hapus ${name}?`)) return;
    UI.updateInspector('DELETE', `${API}/products/${id}`);
    try {
        const res = await ApiService.deleteProduct(id);
        if (res.ok) {
            syncAll();
            UI.showNotification("🗑️ Berhasil Dihapus!", "bg-red-600");
        } else {
            const data = await res.json();
            UI.showNotification(`⚠️ Gagal! ${data.message || 'Data mungkin berelasi.'}`, "bg-orange-600");
        }
    } catch (e) {
        UI.showNotification("🚫 Error!", "bg-black");
    }
}

// CREATE CUSTOMER
async function handleCreateCustomer() {
    const name = document.getElementById('newCustName').value.trim();
    const email = document.getElementById('newCustEmail').value.trim();
    const address = document.getElementById('newCustAddress').value.trim();
    if (!name || !email || !address) {
        UI.showNotification("Semua field customer wajib diisi!", "bg-orange-600");
        return;
    }
    const payload = { name, email, address };
    UI.updateInspector('POST', `${API}/customers`, payload);
    try {
        const res = await ApiService.createCustomer(payload);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            UI.showNotification(`✅ Customer ${name} berhasil dibuat`, "bg-green-600");
            syncAll();
            document.getElementById('newCustName').value = '';
            document.getElementById('newCustEmail').value = '';
            document.getElementById('newCustAddress').value = '';
        } else {
            UI.showNotification(`❌ Gagal: ${data.message || data.error}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error jaringan", "bg-black");
    }
}

// CREATE PRODUCT
async function handleCreateProduct() {
    const name = document.getElementById('newProdName').value.trim();
    const price = parseFloat(document.getElementById('newProdPrice').value);
    const stock = parseInt(document.getElementById('newProdStock').value);
    let categoryId = document.getElementById('newProdCategory').value;
    if (!name || isNaN(price) || isNaN(stock)) {
        UI.showNotification("Nama, harga, dan stok wajib diisi!", "bg-orange-600");
        return;
    }
    const payload = { name, price, stock };
    if (categoryId) payload.category_id = parseInt(categoryId);
    UI.updateInspector('POST', `${API}/products`, payload);
    try {
        const res = await ApiService.createProduct(payload);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            UI.showNotification(`✅ Product ${name} berhasil dibuat`, "bg-green-600");
            syncAll();
            document.getElementById('newProdName').value = '';
            document.getElementById('newProdPrice').value = '';
            document.getElementById('newProdStock').value = '';
            document.getElementById('newProdCategory').value = '';
        } else {
            UI.showNotification(`❌ Gagal: ${data.message || data.error}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error jaringan", "bg-black");
    }
}

// DELETE CUSTOMER
async function handleDeleteCustomer(id, name) {
    if (!confirm(`Hapus customer ${name}?`)) return;
    UI.updateInspector('DELETE', `${API}/customers/${id}`);
    try {
        const res = await ApiService.deleteCustomer(id);
        if (res.ok) {
            syncAll();
            UI.showNotification(`🗑️ Customer ${name} dihapus`, "bg-red-600");
        } else {
            const data = await res.json();
            UI.showNotification(`⚠️ Gagal: ${data.message}`, "bg-orange-600");
        }
    } catch (e) {
        UI.showNotification("Error!", "bg-black");
    }
}

// EDIT CUSTOMER
async function handleEditCustomer(id, currentName, currentEmail, currentAddress) {
    const newName = prompt("Nama baru:", currentName);
    if (!newName) return;
    const newEmail = prompt("Email baru:", currentEmail);
    if (!newEmail) return;
    const newAddress = prompt("Alamat baru:", currentAddress);
    if (!newAddress) return;
    const payload = { name: newName, email: newEmail, address: newAddress };
    UI.updateInspector('PUT', `${API}/customers/${id}`, payload);
    try {
        const res = await ApiService.updateCustomer(id, payload);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            UI.showNotification(`✏️ Customer ${newName} diupdate`, "bg-blue-600");
            syncAll();
        } else {
            UI.showNotification(`❌ Gagal update: ${data.message}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error!", "bg-black");
    }
}

// EDIT PRODUCT
async function handleEditProduct(id, currentName, currentPrice, currentStock) {
    const newName = prompt("Nama produk baru:", currentName);
    if (!newName) return;
    const newPrice = parseFloat(prompt("Harga baru:", currentPrice));
    if (isNaN(newPrice)) return;
    const newStock = parseInt(prompt("Stok baru:", currentStock));
    if (isNaN(newStock)) return;
    const payload = { name: newName, price: newPrice, stock: newStock };
    UI.updateInspector('PUT', `${API}/products/${id}`, payload);
    try {
        const res = await ApiService.updateProduct(id, payload);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            UI.showNotification(`✏️ Product ${newName} diupdate`, "bg-blue-600");
            syncAll();
        } else {
            UI.showNotification(`❌ Gagal update: ${data.message}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error!", "bg-black");
    }
}

// INVENTORY: Cek Stok
async function handleCheckStock() {
    const productId = document.getElementById('invProductId').value;
    if (!productId) {
        UI.showNotification("Masukkan Product ID", "bg-orange-600");
        return;
    }
    UI.updateInspector('GET', `${API}/inventory/${productId}`);
    try {
        const res = await fetch(`${API}/inventory/${productId}`);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            document.getElementById('stockResult').innerHTML = `Stok: ${data.stock}`;
            UI.showNotification(`Stok produk ID ${productId} = ${data.stock}`, "bg-blue-600");
        } else {
            document.getElementById('stockResult').innerHTML = `Error: ${data.error}`;
            UI.showNotification(`Gagal: ${data.error}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error jaringan", "bg-black");
    }
}

// INVENTORY: Reserve Stok
async function handleReserveStock() {
    const productId = document.getElementById('reserveProdId').value;
    const quantity = document.getElementById('reserveQty').value;
    if (!productId || !quantity) {
        UI.showNotification("Product ID dan Quantity wajib diisi", "bg-orange-600");
        return;
    }
    const payload = { productId: parseInt(productId), quantity: parseInt(quantity) };
    UI.updateInspector('POST', `${API}/inventory/reserve`, payload);
    try {
        const res = await fetch(`${API}/inventory/reserve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            document.getElementById('reserveResult').innerHTML = `Reservasi ID: ${data.id} (ACTIVE)`;
            UI.showNotification(`Reservasi ID ${data.id} berhasil`, "bg-green-600");
            document.getElementById('reserveProdId').value = '';
            document.getElementById('reserveQty').value = '';
        } else {
            document.getElementById('reserveResult').innerHTML = `Error: ${data.error}`;
            UI.showNotification(`Gagal: ${data.error}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error jaringan", "bg-black");
    }
}

// INVENTORY: Release Reservasi
async function handleReleaseReservation() {
    const reservationId = document.getElementById('releaseReserveId').value;
    if (!reservationId) {
        UI.showNotification("Masukkan Reservation ID", "bg-orange-600");
        return;
    }
    UI.updateInspector('DELETE', `${API}/inventory/reserve/${reservationId}`);
    try {
        const res = await fetch(`${API}/inventory/reserve/${reservationId}`, { method: 'DELETE' });
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            document.getElementById('releaseResult').innerHTML = `Reservasi ${reservationId} released`;
            UI.showNotification(`Reservasi ${reservationId} berhasil dilepas`, "bg-green-600");
            document.getElementById('releaseReserveId').value = '';
        } else {
            document.getElementById('releaseResult').innerHTML = `Error: ${data.error}`;
            UI.showNotification(`Gagal: ${data.error}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error jaringan", "bg-black");
    }
}

// Initial Load
window.onload = syncAll;