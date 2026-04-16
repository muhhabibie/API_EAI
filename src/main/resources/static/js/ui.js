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

    renderDashboard(customers, products, orders) {
        // --- PERBAIKAN DROPDOWN AGAR TIDAK RESET ---
        const custSelect = document.getElementById('custId');
        const prodSelect = document.getElementById('prodId');

        // Hanya isi dropdown jika masih kosong (saat window.onload pertama kali)
        if (custSelect.innerHTML.trim() === "") {
            custSelect.innerHTML = customers.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }

        // Untuk Produk, kita simpan pilihan user dulu sebelum di-update stoknya
        const currentPid = prodSelect.value;
        prodSelect.innerHTML = products.map(p => `<option value="${p.id}">${p.name} (Stock: ${p.stock})</option>`).join('');
        if (currentPid) prodSelect.value = currentPid; 
        // ------------------------------------------

        // 2. RENDER TABEL RIWAYAT TRANSAKSI
        document.getElementById('orderTableBody').innerHTML = orders.map(o => {
            const itemsHtml = o.items.map(item => `
                <div class="flex items-center gap-2 mb-1 border-b border-slate-50 last:border-0 pb-1">
                    <span class="font-bold text-slate-700 text-[11px]">${item.product.name}</span>
                    <span class="text-blue-600 font-black text-[10px]">x${item.quantity}</span>
                    <span class="bg-blue-50 text-blue-500 text-[8px] px-1 rounded uppercase font-bold">
                        ${item.product.category ? item.product.category.name : 'General'}
                    </span>
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
                        <div class="pl-8">
                            ${itemsHtml}
                        </div>
                    </td>
                    <td class="p-4 font-black text-slate-700 text-xs">Rp${o.totalAmount.toLocaleString()}</td>
                    <td class="p-4 text-center">
                        <span class="px-2 py-1 rounded text-[9px] font-black ${o.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 'bg-green-100 text-green-700'}">
                            ${o.status}
                        </span>
                        ${o.status === 'PENDING' ? 
                            `<button onclick="handleUpdateStatus(${o.id}, 'SHIPPED')" class="block mt-2 mx-auto bg-orange-500 text-white text-[8px] px-2 py-1 rounded font-bold hover:bg-orange-600 shadow-sm transition">SHIP NOW</button>` : ''
                        }
                    </td>
                </tr>
            `;
        }).reverse().join('');

        this.renderDatabaseView(customers, products, orders);
    },

    renderDatabaseView(customers, products, orders) {
        document.getElementById('dbProdBody').innerHTML = products.map(p => `
            <tr class="border-b">
                <td class="p-2 font-bold text-slate-400">${p.id}</td>
                <td class="p-2 font-medium">${p.name}</td>
                <td class="p-2 flex items-center gap-1">
                    <button onclick="handleAdjustStock(${p.id}, -1)" class="w-4 h-4 bg-red-100 text-red-600 rounded flex items-center justify-center font-bold text-[8px]">-</button>
                    <span class="font-black text-blue-600 mx-1 text-[10px]">${p.stock}</span>
                    <button onclick="handleAdjustStock(${p.id}, 1)" class="w-4 h-4 bg-green-100 text-green-600 rounded flex items-center justify-center font-bold text-[8px]">+</button>
                    <button onclick="handleDeleteProduct(${p.id}, '${p.name}')" class="ml-2 text-slate-300 hover:text-red-500">🗑️</button>
                </td>
            </tr>
        `).join('');

        document.getElementById('dbCustBody').innerHTML = customers.map(c => `<tr><td class="p-2 font-bold text-blue-600">${c.id}</td><td class="p-2 font-medium">${c.name}</td></tr>`).join('');
        document.getElementById('dbOrderBody').innerHTML = orders.map(o => `<tr><td class="p-2 font-bold text-blue-600">${o.id}</td><td class="p-2 text-[8px] font-mono">${o.orderNumber}</td></tr>`).join('');
    }
};