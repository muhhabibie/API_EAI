/**
 * EAI Admin UI Renderer
 * Bertugas memanipulasi DOM (HTML) untuk menampilkan data.
 */
let salesChartInstance = null;
let statusChartInstance = null;
// FUNGSI GLOBAL: Untuk menyalin perintah cURL dari API Inspector
function copyCurlToClipboard() {
    const curlText = document.getElementById('reqBody').innerText;
    if (!curlText || curlText.trim() === '') return;

    navigator.clipboard.writeText(curlText).then(() => {
        AdminUI.showNotification("cURL berhasil disalin ke Clipboard!", "bg-slate-800");
    }).catch(err => {
        console.error('Gagal menyalin:', err);
        alert("Gagal menyalin teks.");
    });
}
const AdminUI = {
    // Fungsi utilitas untuk format uang rupiah
    formatRupiah(number) {
        return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR' }).format(number);
    },

    // Fungsi utilitas untuk format tanggal
    formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleString('id-ID', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    },

    // Fungsi utama untuk merender seluruh halaman
    renderDashboard(customers, products, orders, categories, shipments, reservations) {
        // Buat lookup maps untuk cross-reference data
        const customerMap = {};
        if (customers) customers.forEach(c => { customerMap[c.id] = c; });
        const productMap = {};
        if (products) products.forEach(p => { productMap[p.id] = p; });
        // Simpan ke window agar renderOrderTableOnly bisa pakai juga
        window._customerMap = customerMap;
        window._productMap = productMap;
        const shipmentMap = {};
        if (shipments) shipments.forEach(s => { shipmentMap[s.orderId] = s; });
        window._shipmentMap = shipmentMap;

        this.renderOrderTableOnly(orders);

       // 1. Update Statistik Overview (Dashboard Utama)
        const activeRes = reservations ? reservations.filter(r => r.status === 'ACTIVE').length : 0;
        const pendingShip = shipments ? shipments.filter(s => s.status === 'PENDING' || s.status === 'PROCESSING').length : 0;
        
        const elStatRes = document.getElementById('statReservations');
        const elStatShip = document.getElementById('statShipments');
        if(elStatRes) elStatRes.innerText = activeRes;
        if(elStatShip) elStatShip.innerText = pendingShip;

        // ====================================================
        // PERBAIKAN: Update Transaction Flow Pipeline (EAI)
        // ====================================================
        const elFlowNew = document.getElementById('flowNew');
        const elFlowRes = document.getElementById('flowReserved');
        const elFlowPaid = document.getElementById('flowPaid');
        const elFlowShip = document.getElementById('flowShipped');

        if(elFlowNew && elFlowRes && elFlowPaid && elFlowShip) {
            // Step 1: Pesanan Baru (Dari Modul Order)
            const newOrders = orders ? orders.filter(o => o.status === 'PENDING').length : 0;
            
            // Step 2: Stok Terkunci (Dari Modul Inventory) - Sudah diwakili oleh activeRes
            
            // Step 3: Dibayar (Dari Modul Order)
            const paidOrders = orders ? orders.filter(o => o.status === 'PAID').length : 0;
            
            // Step 4: Shipped / Done (Dari Modul Shipping/Logistics)
            // Kita buat dinamis agar menangkap semua variasi kata dari backend
            const shippedOrders = shipments ? shipments.filter(s => 
                s.status === 'IN_TRANSIT' || 
                s.status === 'DELIVERED' || 
                s.status === 'SHIPPED' || 
                s.status === 'COMPLETED'
            ).length : 0;

            // Render ke HTML
            elFlowNew.innerText = newOrders;
            elFlowRes.innerText = activeRes; 
            elFlowPaid.innerText = paidOrders;
            elFlowShip.innerText = shippedOrders; // Sekarang akan otomatis naik!
        }
        
        // 3. Render Inventory Reservations
        const liveStockBody = document.getElementById('liveStockBody');
        const adjProductSelect = document.getElementById('adjProductId');
        
        if (liveStockBody && products && products.length > 0) {
            liveStockBody.innerHTML = products.map(p => {
                const stock = (p.stock !== null && p.stock !== undefined) ? p.stock : 0;
                return `
                <tr class="hover:bg-slate-50 transition">
                    <td class="p-3 font-bold text-slate-700">${p.name}</td>
                    <td class="p-3 text-center font-black ${stock < 5 ? 'text-red-500 bg-red-50' : 'text-slate-800'}">${stock}</td>
                </tr>`;
            }).join('');
            
            // Isi Dropdown
            adjProductSelect.innerHTML = '<option value="">Select Product...</option>' + 
                products.map(p => `<option value="${p.id}">${p.name} (Stok: ${p.stock})</option>`).join('');
        }

       // Cari bagian render Inventory Reservations di dalam ui.js
const resBody = document.getElementById('reservationTableBody');
if (resBody && reservations) {
    if (reservations.length === 0) {
        resBody.innerHTML = `<tr><td colspan="5" class="p-4 text-center text-xs text-slate-400 italic font-bold">No locked stock currently.</td></tr>`;
    } else {
        resBody.innerHTML = reservations.map(r => {
            // LOGIKA BARU: Mencari nama produk berdasarkan productId
            const product = products ? products.find(p => p.id === r.productId) : null;
            const productName = product ? product.name : 'Unknown Product';

            return `
                <tr class="hover:bg-slate-50 transition border-b border-slate-50">
                    <td class="p-4 font-mono text-[9px] text-slate-400">${this.formatDate(r.createdAt)}</td>
                    <td class="p-4 font-black text-blue-600 text-xs">#${r.productId}</td>
                    <td class="p-4 font-bold text-slate-700">${productName}</td>
                    <td class="p-4 text-center font-black text-blue-600">${r.quantity}</td>
                    <td class="p-4 text-center">
                        <span class="px-2 py-1 rounded text-[9px] font-black uppercase shadow-sm
                            ${r.status === 'ACTIVE' ? 'bg-yellow-100 text-yellow-700 border border-yellow-200' : 
                              r.status === 'COMPLETED' ? 'bg-green-100 text-green-700 border border-green-200' : 
                              'bg-slate-100 text-slate-500'}">
                            ${r.status}
                        </span>
                    </td>
                </tr>
            `;
        }).reverse().join('');
    }
}

        // 4. Render Shipping Manifest
        // 4. Render Shipping Manifest & Courier Performance
        const shipBody = document.getElementById('shippingTableBody');
        
        if (shipments) {
            // A. Update Courier Performance Stats
            const countJne = shipments.filter(s => s.courierName === 'JNE').length;
            const countPos = shipments.filter(s => s.courierName === 'POS').length;
            const elJne = document.getElementById('countJne');
            const elPos = document.getElementById('countPos');
            if(elJne) elJne.innerText = countJne;
            if(elPos) elPos.innerText = countPos;

            // B. Render Table
            if (shipBody) {
                if (shipments.length === 0) {
                    shipBody.innerHTML = `<tr><td colspan="5" class="p-4 text-center text-xs text-slate-400 font-bold italic">No manifest data available.</td></tr>`;
                } else {
                    shipBody.innerHTML = shipments.map(s => {
                        // Format tanggal otomatis (jika null, tampilkan '-')
                        const shippedDate = s.shippedAt ? this.formatDate(s.shippedAt) : '-';
                        const deliveredDate = s.deliveredAt ? this.formatDate(s.deliveredAt) : '-';
                        
                        return `
                        <tr class="hover:bg-slate-50 transition border-b border-slate-50">
                            <td class="p-4 font-mono text-blue-600 font-black text-xs">${s.trackingNumber || '-'}</td>
                            <td class="p-4">
                                <span class="font-black uppercase text-slate-700">${s.courierName}</span><br>
                                <a href="#" onclick="navigate('order-section'); document.getElementById('orderSearch').value = '#${s.orderId}'; filterOrders();" class="text-[9px] text-blue-500 hover:underline font-bold">View Order #${s.orderId}</a>
                            </td>
                            <td class="p-4 text-[9px] font-medium text-slate-600">
                                <div class="font-black text-slate-800 uppercase text-[10px]">${s.receiverName || '-'}</div>
                                <div class="text-slate-500 text-[9px] mt-0.5 line-clamp-2" title="${s.deliveryAddress || ''}">${s.deliveryAddress || '-'}</div>
                                <div class="text-[9px] font-bold text-slate-700 mt-1">Fee: <span class="font-black">${this.formatRupiah(s.shippingFee || 0)}</span></div>
                            </td>
                            <td class="p-4 text-[9px] text-slate-500 font-medium">
                                <div>Shipped: <span class="font-bold text-slate-800">${shippedDate}</span></div>
                                <div>Delivered: <span class="font-bold text-slate-800">${deliveredDate}</span></div>
                            </td>
                            <td class="p-4 text-center">
                                <span class="px-2 py-1 rounded text-[9px] font-black uppercase shadow-sm
                                    ${s.status === 'DELIVERED' ? 'bg-green-100 text-green-700 border border-green-200' : 
                                      s.status === 'IN_TRANSIT' ? 'bg-blue-100 text-blue-700 border border-blue-200' : 
                                      'bg-yellow-100 text-yellow-700 border border-yellow-200'}">
                                    ${s.status}
                                </span>
                            </td>
                            <td class="p-4 text-center">
                                ${s.status === 'PENDING' || s.status === 'PROCESSING' ? 
                                    `<button onclick="handleShipItem(${s.id})" class="bg-blue-600 hover:bg-blue-700 text-white px-2 py-1.5 rounded text-[9px] font-bold shadow-sm transition block w-full">SET IN TRANSIT</button>` : 
                                s.status === 'IN_TRANSIT' ?
                                    `<button onclick="handleDeliverItem(${s.id})" class="bg-green-500 hover:bg-green-600 text-white px-2 py-1.5 rounded text-[9px] font-bold shadow-sm transition block w-full">SET DELIVERED</button>` :
                                '<span class="text-[9px] font-black text-slate-400">DONE</span>'
                                }
                            </td>
                        </tr>
                    `}).reverse().join('');
                }
            }
        }

        // 5. Render Master Data (Customers & Products)
        const prodBody = document.getElementById('dbProdBody');
        if (prodBody && products) {
            prodBody.innerHTML = products.map(p => `
                <tr class="hover:bg-slate-50 transition">
                    <td class="p-3">
                        <div class="font-bold text-slate-800 text-xs">${p.name}</div>
                        <div class="text-[9px] text-slate-400 font-mono mt-0.5">ID: #${p.id}</div>
                    </td>
                    <td class="p-3 text-right text-blue-600 font-black text-xs">${this.formatRupiah(p.price)}</td>
                    <td class="p-3 text-center">
                        <span class="px-2 py-0.5 rounded font-black text-[10px] ${p.stock < 10 ? 'bg-red-100 text-red-600' : 'bg-slate-100 text-slate-700'}">${p.stock}</span>
                    </td>
                    <td class="p-3 text-center">
                        <button onclick="handleDeleteProduct(${p.id})" class="text-red-500 hover:text-red-700 font-bold text-[9px] uppercase tracking-wider">Delete</button>
                    </td>
                </tr>
            `).join('');
        }

        const custBody = document.getElementById('dbCustBody');
        if (custBody && customers) {
            custBody.innerHTML = customers.map(c => `
                <tr class="hover:bg-slate-50 transition">
                    <td class="p-3 font-bold text-slate-400 text-xs">#${c.id}</td>
                    <td class="p-3">
                        <div class="font-bold text-slate-800 text-xs">${c.name}</div>
                        <div class="text-[9px] text-slate-500 mt-0.5">${c.email}</div>
                    </td>
                    <td class="p-3 text-center">
                        <button class="text-slate-400 hover:text-blue-500 font-bold text-[9px] uppercase tracking-wider">Edit</button>
                    </td>
                </tr>
            `).join('');
        }

        try { this.renderCharts(orders); } catch(e) { console.warn('renderCharts error (non-fatal):', e); }
    },

    renderOrderTableOnly(orders) {
        const orderBody = document.getElementById('orderTableBody');
        if (!orderBody) return;

        if (!orders || orders.length === 0) {
            orderBody.innerHTML = `<tr><td colspan="5" class="p-8 text-center text-xs text-slate-400 font-bold">Tidak ada pesanan yang sesuai kriteria pencarian.</td></tr>`;
            return;
        }

        const cm = window._customerMap || {};
        const pm = window._productMap || {};
        const sm = window._shipmentMap || {};

        orderBody.innerHTML = orders.map(o => {
            const customerObj = o.customer || cm[o.customerId];
            const customerName = customerObj ? customerObj.name : (o.customerId ? `⏳ Memuat...` : 'Unknown');
            const customerAddress = customerObj ? customerObj.address : '';
            const shipment = sm[o.id];
            const shipmentAction = shipment
                ? `<button onclick="navigate('shipping-section')" class="block w-full mt-2 bg-indigo-600 hover:bg-indigo-700 text-white text-[8px] px-2 py-1.5 rounded font-bold shadow-sm transition">SHIPPING</button>
                   <span class="block text-[8px] text-green-600 font-bold mt-1 italic">${shipment.trackingNumber || 'Shipment Created'} (${shipment.courierName})</span>`
                : null;
            const itemsList = (o.items || []).map(i => {
                const productObj = i.product || pm[i.productId];
                const productName = productObj ? productObj.name : ('Produk #' + i.productId);
                return `<span class="text-[9px] bg-slate-100 text-slate-600 px-1 py-0.5 rounded mr-1">${productName} (x${i.quantity})</span>`;
            }).join('');
            
            return `
            <tr class="hover:bg-slate-50 transition" data-order-id="${o.id}">
                <td class="p-4 font-black text-blue-600">#${o.id}</td>
                <td class="p-4 font-mono text-[10px] text-slate-500">${o.orderNumber || '-'}<br><span class="text-[9px]">${this.formatDate(o.createdAt)}</span></td>
                <td class="p-4">
                    <div class="font-bold text-xs uppercase cust-name-${o.customerId}">${customerName}</div>
                    <div class="mt-1">${itemsList}</div>
                </td>
                <td class="p-4 text-right font-black text-xs">${this.formatRupiah(o.totalAmount)}</td>
                <td class="p-4 text-center">
                    <span class="px-2 py-1 rounded text-[9px] font-black uppercase
                        ${o.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 
                          o.status === 'AWAITING_PAYMENT' ? 'bg-orange-100 text-orange-700 border border-orange-200' :
                          o.status === 'PAID' ? 'bg-blue-100 text-blue-700' :
                          o.status === 'CANCELLED' ? 'bg-red-100 text-red-700' :
                          o.status === 'SHIPPED' || o.status === 'PROCESSING' ? 'bg-indigo-100 text-indigo-700' : 'bg-green-100 text-green-700'}">
                        ${o.status}
                    </span>
                    ${o.status === 'PENDING' ? 
                        `<span class="block text-[8px] text-slate-400 font-bold mt-1.5 italic">Reserving Stock...</span>` : 
                      o.status === 'AWAITING_PAYMENT' ?
                        `<button onclick="handleCancelOrder(${o.id})" class="block w-full mt-2 bg-red-500 hover:bg-red-600 text-white text-[8px] px-2 py-1.5 rounded font-bold shadow-sm transition">CANCEL</button>` :
                      o.status === 'PAID' ?
                        (shipmentAction ?
                          shipmentAction :
                          `<button onclick="handleCreateShipment(${o.id}, '${customerName.replace(/'/g, "\\'").replace(/⏳/g, '').replace(/Memuat/g, '').trim()}', '${(customerAddress || '').replace(/'/g, "\\'")}', '${o.courierName || 'JNE'}', ${o.shippingFee || 15000})" class="block w-full mt-2 bg-indigo-600 hover:bg-indigo-700 text-white text-[8px] px-2 py-1.5 rounded font-bold shadow-sm transition">SHIP ORDER</button>
                           <button onclick="handleCancelPaidOrder(${o.id})" class="block w-full mt-1 bg-red-500 hover:bg-red-600 text-white text-[8px] px-2 py-1.5 rounded font-bold shadow-sm transition">CANCEL &amp; REFUND</button>`
                        ) : ''
                    }
                </td>
            </tr>
            `;
        }).reverse().join('');

        // Trigger async lookup for unresolved customer IDs
        const unresolvedIds = orders
            .filter(o => !cm[o.customerId] && !o.customer && o.customerId)
            .map(o => o.customerId);
        const uniqueIds = [...new Set(unresolvedIds)];
        if (uniqueIds.length > 0) {
            this._resolveCustomerNamesAsync(uniqueIds);
        }
    },

    renderCharts(orders) {
        if (!orders || orders.length === 0) return;
        if (typeof ApexCharts === 'undefined') {
            console.warn('ApexCharts belum siap, chart dilewati.');
            return;
        }

        // --- A. Siapkan Data untuk Status Chart (Donut) ---
        const statusCounts = { PENDING: 0, PAID: 0, SHIPPED: 0, COMPLETED: 0 };
        orders.forEach(o => {
            if (statusCounts[o.status] !== undefined) statusCounts[o.status]++;
        });
        
        const statusLabels = Object.keys(statusCounts);
        const statusSeries = Object.values(statusCounts);

        // --- B. Siapkan Data untuk Sales Trend Chart (Area) ---
        // Mengelompokkan pendapatan berdasarkan tanggal pembuatan (createdAt)
        const salesData = {};
        // Reverse array agar grafik bergerak dari tanggal lama ke terbaru (kiri ke kanan)
        [...orders].reverse().forEach(o => { 
            // Format tanggal jadi simpel (contoh: "19 Apr")
            const date = new Date(o.createdAt).toLocaleDateString('id-ID', { day: 'numeric', month: 'short' });
            salesData[date] = (salesData[date] || 0) + o.totalAmount;
        });

        const salesLabels = Object.keys(salesData);
        const salesSeries = Object.values(salesData);

        // --- C. Render/Update Sales Chart ---
        const salesOptions = {
            series: [{ name: 'Total Revenue', data: salesSeries }],
            chart: { type: 'area', height: 250, toolbar: { show: false }, fontFamily: 'Inter, sans-serif' },
            colors: ['#2563EB'], // Biru Tailwind
            dataLabels: { enabled: false },
            stroke: { curve: 'smooth', width: 3 },
            xaxis: { categories: salesLabels, labels: { style: { fontSize: '10px' } } },
            yaxis: { 
                labels: { 
                    style: { fontSize: '10px' },
                    formatter: (value) => "Rp " + value.toLocaleString('id-ID') 
                } 
            },
            fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 90, 100] } }
        };

        if (salesChartInstance) {
            salesChartInstance.updateOptions(salesOptions);
            salesChartInstance.updateSeries([{ data: salesSeries }]);
        } else {
            salesChartInstance = new ApexCharts(document.querySelector("#salesChart"), salesOptions);
            salesChartInstance.render();
        }

        // --- D. Render/Update Status Chart ---
        const statusOptions = {
            series: statusSeries,
            chart: { type: 'donut', height: 250, fontFamily: 'Inter, sans-serif' },
            labels: statusLabels,
            // Warna disesuaikan dengan badge status kita: Kuning (Pending), Biru (Paid), Oranye (Shipped), Hijau (Completed)
            colors: ['#FEF08A', '#BFDBFE', '#FED7AA', '#BBF7D0'], 
            plotOptions: { pie: { donut: { size: '75%' } } },
            dataLabels: { enabled: false },
            stroke: { show: true, colors: ['#fff'], width: 2 },
            legend: { position: 'bottom', fontSize: '11px', fontWeight: 700 }
        };

        if (statusChartInstance) {
            statusChartInstance.updateOptions(statusOptions);
            statusChartInstance.updateSeries(statusSeries);
        } else {
            statusChartInstance = new ApexCharts(document.querySelector("#statusChart"), statusOptions);
            statusChartInstance.render();
        }
    }
    ,

    // FIX: Async resolution untuk customer name yang belum ter-resolve di customerMap
    // Dipanggil setelah render awal jika ada order dengan customerId yang tidak ada di customerMap
    async _resolveCustomerNamesAsync(customerIds) {
        for (const custId of customerIds) {
            try {
                const res = await fetch(`/api/customers/${custId}`, {
                    headers: { 'Authorization': `Bearer ${localStorage.getItem('token') || ''}` }
                });
                if (!res.ok) continue;
                const data = await res.json();
                const customer = data.data || data;
                if (!customer || !customer.name) continue;
                
                // Update semua elemen DOM yang menunggu nama customer ini
                document.querySelectorAll(`.cust-name-${custId}`).forEach(el => {
                    el.innerText = customer.name;
                });
                // Simpan ke customerMap cache untuk order yang baru dirender
                if (window._customerMap) {
                    window._customerMap[custId] = customer;
                }
            } catch (e) {
                // Fail silently; DOM akan tetap tampilkan Customer #id
                console.warn(`[CustomerResolve] Gagal fetch customer ${custId}:`, e.message);
            }
        }
    },

    showNotification(message, bgClass = 'bg-slate-800') {
        const existing = document.getElementById('adminNotif');
        if (existing) existing.remove();
        const notif = document.createElement('div');
        notif.id = 'adminNotif';
        notif.className = `fixed bottom-6 right-6 z-50 ${bgClass} text-white text-xs font-bold px-5 py-3 rounded-xl shadow-lg transition-all`;
        notif.innerText = message;
        document.body.appendChild(notif);
        setTimeout(() => { if(notif) notif.remove(); }, 4000);
    }
};
