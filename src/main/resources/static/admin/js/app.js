const API = 'http://localhost:8080/api';

const ApiService = {
    async fetchData(endpoint) {
        const res = await fetch(`${API}/${endpoint}`);
        if (!res.ok) throw new Error(`Gagal mengambil data ${endpoint}`);
        return await res.json();
    },
    async postOrder(customerId, payload) {
        return await fetch(`${API}/orders?customerId=${customerId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },
    async payOrder(orderId, courier) {
        return await fetch(`${API}/orders/${orderId}/pay?courier=${courier}`, { method: 'POST' });
    },
    async updateOrderStatus(orderId, status) {
        return await fetch(`${API}/orders/${orderId}/status?status=${status}`, { method: 'PUT' });
    },
    async adjustStock(productId, amount) {
        return await fetch(`${API}/products/${productId}/adjustment?amount=${amount}`, { method: 'PATCH' });
    },
    async deleteProduct(productId) {
        return await fetch(`${API}/products/${productId}`, { method: 'DELETE' });
    },
    async createProduct(payload) {
        return await fetch(`${API}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },
    async updateProduct(productId, payload) {
        return await fetch(`${API}/products/${productId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },
    async createCustomer(payload) {
        return await fetch(`${API}/customers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },
    async updateCustomer(customerId, payload) {
        return await fetch(`${API}/customers/${customerId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
    },
    async deleteCustomer(customerId) {
        return await fetch(`${API}/customers/${customerId}`, { method: 'DELETE' });
    }
};

const UI = {
    renderResponse(data) {
        document.getElementById('resBody').innerText = JSON.stringify(data, null, 2);
    },
    updateInspector(method, url, payload = null) {
        let text = `METHOD: ${method}\nURL: ${url}\n\n`;
        if (payload) {
            text += `PAYLOAD:\n${JSON.stringify(payload, null, 2)}\n\n`;
            text += `CURL:\ncurl -X ${method} "${url}" -H "Content-Type: application/json" -d '${JSON.stringify(payload)}'`;
        } else {
            text += `CURL:\ncurl -X ${method} "${url}"`;
        }
        document.getElementById('reqBody').innerText = text;
    },
    showNotification(msg, colorClass = "bg-slate-900") {
        const n = document.createElement('div');
        n.className = `fixed top-5 right-5 ${colorClass} text-white px-6 py-4 rounded-xl shadow-2xl z-50 notif-animation border-b-4 border-white/20`;
        n.innerHTML = `<p class="text-[9px] font-black uppercase mb-1">System Message</p><p class="text-sm font-bold">${msg}</p>`;
        document.body.appendChild(n);
        setTimeout(() => { n.style.opacity = '0'; setTimeout(() => n.remove(), 500); }, 3000);
    },
    renderDashboard(customers, products, orders, categories) {
        const custSelect = document.getElementById('custId');
        const prodSelect = document.getElementById('prodId');

        if (custSelect.innerHTML.trim() === "") {
            custSelect.innerHTML = customers.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }

        const currentPid = prodSelect.value;
        prodSelect.innerHTML = products.map(p => `<option value="${p.id}">${p.name} (Stock: ${p.stock})</option>`).join('');
        if (currentPid) prodSelect.value = currentPid;

        document.getElementById('orderTableBody').innerHTML = orders.map(o => {
            const itemsHtml = o.items.map(item => `
                <div class="flex items-center gap-2 mb-1 border-b border-slate-50 last:border-0 pb-1">
                    <span class="font-bold text-slate-700 text-[11px]">${item.product.name}</span>
                    <span class="text-blue-600 font-black text-[10px]">x${item.quantity}</span>
                </div>
            `).join('');

            const date = new Date(o.createdAt).toLocaleString('id-ID');

            return `
                <tr class="hover:bg-slate-50 border-t transition">
                    <td class="p-4 font-bold text-blue-600 text-xs">#${o.id}</td>
                    <td class="p-4">
                        <div class="text-[10px] font-mono text-slate-400 mb-1">${o.orderNumber}</div>
                        <div class="text-[9px] font-medium text-slate-400 uppercase tracking-tighter">${date}</div>
                    </td>
                    <td class="p-4">
                        <div class="flex items-center gap-2 mb-2">
                            <div class="w-6 h-6 bg-blue-600 text-white rounded-full flex items-center justify-center text-[10px] font-black">
                                ${o.customer.name ? o.customer.name.charAt(0) : '?'}
                            </div>
                            <div class="text-[11px] font-black text-slate-800 uppercase">${o.customer.name}</div>
                        </div>
                        <div class="pl-8">${itemsHtml}</div>
                    </td>
                    <td class="p-4 font-black text-slate-700 text-xs">Rp${o.totalAmount.toLocaleString()}</td>
                    <td class="p-4 text-center">
                        <span class="px-2 py-1 rounded text-[9px] font-black ${
                            o.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 
                            o.status === 'PAID' ? 'bg-blue-100 text-blue-700' :
                            o.status === 'SHIPPED' ? 'bg-purple-100 text-purple-700' :
                            'bg-green-100 text-green-700'
                        }">
                            ${o.status}
                        </span>
                        
                        ${o.status === 'PENDING' ? `
                            <div class="mt-2 text-left">
                                <label class="text-[8px] font-bold text-slate-400 uppercase mb-1 block">Pilih Kurir:</label>
                                <select id="courier-${o.id}" class="block w-full text-[9px] p-1.5 border border-slate-300 rounded mb-1 outline-none">
                                    <option value="JNE">JNE Reguler</option>
                                    <option value="JNT">J&T Express</option>
                                    <option value="SICEPAT">SiCepat HALU</option>
                                    <option value="POS_INDONESIA">Pos Indonesia</option>
                                    <option value="GOSEND">GoSend Instant</option>
                                    <option value="GRAB_EXPRESS">GrabExpress</option>
                                </select>
                                <button onclick="handlePayOrder(${o.id})" class="block w-full bg-green-500 text-white text-[9px] px-2 py-1.5 rounded font-black hover:bg-green-600 shadow-sm transition uppercase tracking-wider">
                                    💳 PAY & SHIP
                                </button>
                            </div>
                        ` : o.status === 'PAID' ? `
                            <div class="mt-2 text-left">
                                <button onclick="handleShippingAction(${o.id}, 'IN_TRANSIT')" class="block w-full bg-blue-500 text-white text-[9px] px-2 py-1.5 rounded font-black hover:bg-blue-600 shadow-sm transition uppercase tracking-wider">
                                    🚚 PICK UP BY COURIER
                                </button>
                            </div>
                        ` : o.status === 'SHIPPED' ? `
                            <div class="mt-2 text-left">
                                <button onclick="handleShippingAction(${o.id}, 'DELIVERED')" class="block w-full bg-purple-500 text-white text-[9px] px-2 py-1.5 rounded font-black hover:bg-purple-600 shadow-sm transition uppercase tracking-wider">
                                    📦 MARK AS DELIVERED
                                </button>
                            </div>
                        ` : ''}
                    </td>
                </tr>
            `;
        }).reverse().join('');

        this.renderDatabaseView(customers, products, orders);
    },
    renderDatabaseView(customers, products, orders) {
        document.getElementById('dbCustBody').innerHTML = customers.map(c => `
            <tr class="border-b">
                <td class="p-2 font-bold text-blue-600">${c.id}</td>
                <td class="p-2 font-medium">${c.name}</td>
                <td class="p-2 text-[10px] text-slate-500">${c.email}</td>
                <td class="p-2">
                    <button onclick="handleEditCustomer(${c.id}, '${c.name.replace(/'/g, "\\'")}', '${c.email.replace(/'/g, "\\'")}', '${c.address ? c.address.replace(/'/g, "\\'") : ''}')" class="text-blue-500 hover:text-blue-700 mr-2 text-xs">✏️</button>
                    <button onclick="handleDeleteCustomer(${c.id}, '${c.name.replace(/'/g, "\\'")}')" class="text-red-500 hover:text-red-700 text-xs">🗑️</button>
                </td>
            </tr>
        `).join('');

        document.getElementById('dbProdBody').innerHTML = products.map(p => `
            <tr class="border-b">
                <td class="p-2 font-bold text-slate-400">${p.id}</td>
                <td class="p-2 font-medium">${p.name}</td>
                <td class="p-2 text-[10px]">Rp${p.price.toLocaleString()}</td>
                <td class="p-2 flex items-center gap-1">
                    <button onclick="handleAdjustStock(${p.id}, -1)" class="w-4 h-4 bg-red-100 text-red-600 rounded flex items-center justify-center font-bold text-[8px]">-</button>
                    <span class="font-black text-blue-600 mx-1 text-[10px]">${p.stock}</span>
                    <button onclick="handleAdjustStock(${p.id}, 1)" class="w-4 h-4 bg-green-100 text-green-600 rounded flex items-center justify-center font-bold text-[8px]">+</button>
                    <button onclick="handleEditProduct(${p.id}, '${p.name.replace(/'/g, "\\'")}', ${p.price}, ${p.stock})" class="ml-2 text-blue-500 hover:text-blue-700 text-xs">✏️</button>
                    <button onclick="handleDeleteProduct(${p.id}, '${p.name.replace(/'/g, "\\'")}')" class="text-red-500 hover:text-red-700 text-xs">🗑️</button>
                 </td>
            </tr>
        `).join('');

        document.getElementById('dbOrderBody').innerHTML = orders.map(o => `
            <tr class="border-b">
                <td class="p-2 font-bold text-blue-600">${o.id}</td>
                <td class="p-2 text-[8px] font-mono">${o.orderNumber}</td>
                <td class="p-2 text-[10px] font-bold">Rp${o.totalAmount.toLocaleString()}</td>
                <td class="p-2">
                    <span class="px-1 py-0.5 rounded text-[8px] font-black ${o.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 'bg-green-100 text-green-700'}">
                        ${o.status}
                    </span>
                 </td>
            </tr>
        `).join('');
    }
};

async function syncAll() {
    try {
        const [customers, products, allOrders, categories] = await Promise.all([
            ApiService.fetchData('customers'),
            ApiService.fetchData('products'),
            ApiService.fetchData('orders'),
            ApiService.fetchData('categories').catch(() => []) 
        ]);

        const catSelect = document.getElementById('newProdCategory');
        if (catSelect && categories.length > 0) {
            catSelect.innerHTML = '<option value="">Pilih Kategori</option>' + 
                categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }

        UI.renderDashboard(customers, products, allOrders, categories);
    } catch (e) {
        UI.showNotification("🚫 Gagal memuat data!", "bg-black");
    }
}

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
            UI.showNotification(`✅ ${customerName} membuat order ${productName}`, "bg-green-600");
            syncAll();
        } else {
            UI.showNotification(`⚠️ Gagal: ${data.message || data.error}`, "bg-orange-600");
        }
    } catch (e) {
        UI.showNotification("🚫 Kesalahan Jaringan!", "bg-black");
    }
}

async function handlePayOrder(orderId) {
    const courierSelect = document.getElementById(`courier-${orderId}`);
    const courier = courierSelect.value;

    if (!courier) {
        UI.showNotification("Pilih kurir terlebih dahulu!", "bg-orange-600");
        return;
    }

    UI.updateInspector('POST', `${API}/orders/${orderId}/pay?courier=${courier}`);
    try {
        const res = await ApiService.payOrder(orderId, courier);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            syncAll();
            UI.showNotification(`✅ Pembayaran Sukses! Resi via ${courier} dibuat.`, "bg-green-600");
        } else {
            UI.showNotification(`❌ Gagal: ${data.message || data.error}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error jaringan", "bg-black");
    }
}

// FITUR BARU: Update Logistik (In Transit -> Delivered)
async function handleShippingAction(orderId, nextStatus) {
    try {
        // Cari ID Shipment berdasarkan Order ID
        UI.updateInspector('GET', `${API}/shipments/order/${orderId}`);
        const shipRes = await fetch(`${API}/shipments/order/${orderId}`);
        if (!shipRes.ok) throw new Error("Data pengiriman belum siap");
        const shipData = await shipRes.json();

        // Update Status Shipment
        UI.updateInspector('PUT', `${API}/shipments/${shipData.id}/status?status=${nextStatus}`);
        const res = await fetch(`${API}/shipments/${shipData.id}/status?status=${nextStatus}`, { method: 'PUT' });
        const data = await res.json();
        UI.renderResponse(data);
        
        if (res.ok) {
            UI.showNotification(`🚚 Logistik Update: ${nextStatus}`, "bg-blue-600");
            syncAll();
        } else {
            UI.showNotification(`❌ Gagal Update: ${data.message}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification(e.message, "bg-red-600");
    }
}

async function handleCancelOrder(orderId) {
    if (!confirm(`Batalkan order #${orderId}? Stok akan dikembalikan.`)) return;
    UI.updateInspector('POST', `${API}/orders/${orderId}/cancel`);
    try {
        const res = await fetch(`${API}/orders/${orderId}/cancel`, { method: 'POST' });
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            UI.showNotification(`✅ Order #${orderId} dibatalkan`, "bg-green-600");
            syncAll();
        } else {
            UI.showNotification(`❌ Gagal: ${data.message || data.error}`, "bg-red-600");
        }
    } catch (e) {
        UI.showNotification("Error jaringan", "bg-black");
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
        } else {
            UI.showNotification(`❌ Gagal: ${data.message}`, "bg-red-600");
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

async function handleCreateCustomer() {
    const name = document.getElementById('newCustName').value.trim();
    const email = document.getElementById('newCustEmail').value.trim();
    const address = document.getElementById('newCustAddress').value.trim();
    if (!name || !email || !address) return UI.showNotification("Semua field wajib diisi!", "bg-orange-600");
    const payload = { name, email, address };
    UI.updateInspector('POST', `${API}/customers`, payload);
    try {
        const res = await ApiService.createCustomer(payload);
        if (res.ok) {
            UI.showNotification(`✅ Customer ${name} dibuat`, "bg-green-600");
            syncAll();
        }
    } catch (e) {}
}

async function handleCreateProduct() {
    const name = document.getElementById('newProdName').value.trim();
    const price = parseFloat(document.getElementById('newProdPrice').value);
    const stock = parseInt(document.getElementById('newProdStock').value);

    const payload = { name, price, stock };
    UI.updateInspector('POST', `${API}/products`, payload);

    try {
        const data = await ApiService.createProduct(payload);
        
        // JIKA BERHASIL
        UI.renderResponse(data);
        UI.showNotification(`✅ Produk ${name} berhasil dibuat!`, "bg-green-600");
        syncAll(); // Refresh data di tabel
        
        // Kosongkan form
        document.getElementById('newProdName').value = '';
        document.getElementById('newProdPrice').value = '';
    } catch (e) {
        // JIKA ERROR (Kena validasi @Pattern, @Min, dll)
        // e.message adalah string yang kita lempar dari ApiService tadi
        UI.showNotification(`❌ Error: ${e.message}`, "bg-red-600");
        
        // Tampilkan juga detail errornya di inspector biar gampang debug
        document.getElementById('resBody').innerText = e.message;
    }
}

async function handleDeleteCustomer(id, name) {
    if (!confirm(`Hapus customer ${name}?`)) return;
    UI.updateInspector('DELETE', `${API}/customers/${id}`);
    try {
        const res = await ApiService.deleteCustomer(id);
        if (res.ok) {
            syncAll();
            UI.showNotification(`🗑️ Customer ${name} dihapus`, "bg-red-600");
        }
    } catch (e) {}
}

async function handleEditCustomer(id, currentName, currentEmail, currentAddress) {
    const name = prompt("Nama baru:", currentName);
    const email = prompt("Email baru:", currentEmail);
    const address = prompt("Alamat baru:", currentAddress);
    if (!name || !email || !address) return;
    const payload = { name, email, address };
    UI.updateInspector('PUT', `${API}/customers/${id}`, payload);
    try {
        const res = await ApiService.updateCustomer(id, payload);
        if (res.ok) {
            UI.showNotification(`✏️ Customer diupdate`, "bg-blue-600");
            syncAll();
        }
    } catch (e) {}
}

async function handleEditProduct(id, currentName, currentPrice, currentStock) {
    const name = prompt("Nama baru:", currentName);
    const price = parseFloat(prompt("Harga baru:", currentPrice));
    const stock = parseInt(prompt("Stok baru:", currentStock));
    if (!name || isNaN(price) || isNaN(stock)) return;
    const payload = { name, price, stock };
    UI.updateInspector('PUT', `${API}/products/${id}`, payload);
    try {
        const res = await ApiService.updateProduct(id, payload);
        if (res.ok) {
            UI.showNotification(`✏️ Product diupdate`, "bg-blue-600");
            syncAll();
        }
    } catch (e) {}
}

async function handleCheckStock() {
    const productId = document.getElementById('invProductId').value;
    if (!productId) return;
    UI.updateInspector('GET', `${API}/inventory/${productId}`);
    try {
        const res = await fetch(`${API}/inventory/${productId}`);
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            document.getElementById('stockResult').innerHTML = `Stok: ${data.stock}`;
        }
    } catch (e) {}
}

async function handleReserveStock() {
    const productId = document.getElementById('reserveProdId').value;
    const quantity = document.getElementById('reserveQty').value;
    if (!productId || !quantity) return;
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
            document.getElementById('reserveResult').innerHTML = `Reservasi ID: ${data.id}`;
            UI.showNotification("Reservasi sukses", "bg-green-600");
        }
    } catch (e) {}
}

async function handleReleaseReservation() {
    const reservationId = document.getElementById('releaseReserveId').value;
    if (!reservationId) return;
    UI.updateInspector('DELETE', `${API}/inventory/reserve/${reservationId}`);
    try {
        const res = await fetch(`${API}/inventory/reserve/${reservationId}`, { method: 'DELETE' });
        const data = await res.json();
        UI.renderResponse(data);
        if (res.ok) {
            document.getElementById('releaseResult').innerHTML = `Released`;
            UI.showNotification("Reservasi dilepas", "bg-green-600");
        }
    } catch (e) {}
}

window.onload = syncAll;


function switchMenu(menuId) {
    // 1. Sembunyikan semua halaman (page-section)
    document.querySelectorAll('.page-section').forEach(page => {
        page.classList.add('hidden');
        page.classList.remove('block');
    });

    // 2. Reset warna semua tombol menu di sidebar
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('bg-blue-600', 'text-white');
        btn.classList.add('text-slate-400', 'hover:bg-slate-800');
    });

    // 3. Tampilkan halaman yang dipilih
    document.getElementById(`page-${menuId}`).classList.remove('hidden');
    document.getElementById(`page-${menuId}`).classList.add('block');

    // 4. Ubah warna tombol yang aktif
    const activeBtn = document.getElementById(`menu-${menuId}`);
    activeBtn.classList.remove('text-slate-400', 'hover:bg-slate-800');
    activeBtn.classList.add('bg-blue-600', 'text-white');

    // 5. Ubah Judul Halaman di Header Atas
    const titles = {
        'orders': 'Orders Management',
        'products': 'Product Catalog',
        'customers': 'Customer Directory',
        'inventory': 'Inventory & API Logs'
    };
    document.getElementById('page-title').innerText = titles[menuId];
}