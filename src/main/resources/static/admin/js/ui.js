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
        this.renderOrderTableOnly(orders);
        // 1. Update Statistik Overview (Dashboard Utama)
        const activeRes = reservations ? reservations.filter(r => r.status === 'ACTIVE').length : 0;
        const pendingShip = shipments ? shipments.filter(s => s.status === 'PENDING').length : 0;
        
        document.getElementById('statReservations').innerText = activeRes;
        document.getElementById('statShipments').innerText = pendingShip;

        //Update Transaction Flow Pipeline
        const newOrders = orders ? orders.filter(o => o.status === 'PENDING').length : 0;
        const paidOrders = orders ? orders.filter(o => o.status === 'PAID').length : 0;
        const shippedOrders = orders ? orders.filter(o => o.status === 'SHIPPED' || o.status === 'COMPLETED').length : 0;

        document.getElementById('flowNew').innerText = newOrders;
        document.getElementById('flowReserved').innerText = activeRes; 
        document.getElementById('flowPaid').innerText = paidOrders;
        document.getElementById('flowShipped').innerText = shippedOrders;

        // 2. Render Order History
        const orderBody = document.getElementById('orderTableBody');
        if (orderBody && orders) {
            orderBody.innerHTML = orders.map(o => {
                const itemsList = o.items.map(i => `<span class="text-[9px] bg-slate-100 text-slate-600 px-1 py-0.5 rounded mr-1">${i.product.name} (x${i.quantity})</span>`).join('');
                
                return `
                <tr class="hover:bg-slate-50 transition">
                    <td class="p-4 font-black text-blue-600">#${o.id}</td>
                    <td class="p-4 font-mono text-[10px] text-slate-500">${o.orderNumber}<br><span class="text-[9px]">${this.formatDate(o.createdAt)}</span></td>
                    <td class="p-4">
                        <div class="font-bold text-xs uppercase">${o.customer ? o.customer.name : 'Unknown'}</div>
                        <div class="mt-1">${itemsList}</div>
                    </td>
                    <td class="p-4 text-right font-black text-xs">${this.formatRupiah(o.totalAmount)}</td>
                    <td class="p-4 text-center">
                        <span class="px-2 py-1 rounded text-[9px] font-black uppercase
                            ${o.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 
                              o.status === 'PAID' ? 'bg-blue-100 text-blue-700' :
                              o.status === 'SHIPPED' ? 'bg-orange-100 text-orange-700' : 'bg-green-100 text-green-700'}">
                            ${o.status}
                        </span>
                        ${o.status === 'PENDING' ? 
                            `<button onclick="handleApprovePayment(${o.id})" class="block w-full mt-2 bg-blue-600 hover:bg-blue-700 text-white text-[8px] px-2 py-1.5 rounded font-bold shadow-sm transition">APPROVE PAY</button>` : ''
                        }
                    </td>
                </tr>
            `}).reverse().join('');
        }

        // 3. Render Inventory Reservations
        const liveStockBody = document.getElementById('liveStockBody');
        const adjProductSelect = document.getElementById('adjProductId');
        
        if (liveStockBody && products) {
            liveStockBody.innerHTML = products.map(p => `
                <tr class="hover:bg-slate-50 transition">
                    <td class="p-3 font-bold text-slate-700">${p.name}</td>
                    <td class="p-3 text-center font-black ${p.stock < 5 ? 'text-red-500 bg-red-50' : 'text-slate-800'}">${p.stock}</td>
                </tr>
            `).join('');
            
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

        this.renderCharts(orders);
    },

    renderOrderTableOnly(orders) {
        const orderBody = document.getElementById('orderTableBody');
        if (!orderBody) return;

        if (!orders || orders.length === 0) {
            orderBody.innerHTML = `<tr><td colspan="5" class="p-8 text-center text-xs text-slate-400 font-bold">Tidak ada pesanan yang sesuai kriteria pencarian.</td></tr>`;
            return;
        }

        orderBody.innerHTML = orders.map(o => {
            const itemsList = o.items.map(i => `<span class="text-[9px] bg-slate-100 text-slate-600 px-1 py-0.5 rounded mr-1">${i.product.name} (x${i.quantity})</span>`).join('');
            
            return `
            <tr class="hover:bg-slate-50 transition">
                <td class="p-4 font-black text-blue-600">#${o.id}</td>
                <td class="p-4 font-mono text-[10px] text-slate-500">${o.orderNumber || '-'}<br><span class="text-[9px]">${this.formatDate(o.createdAt)}</span></td>
                <td class="p-4">
                    <div class="font-bold text-xs uppercase">${o.customer ? o.customer.name : 'Unknown'}</div>
                    <div class="mt-1">${itemsList}</div>
                </td>
                <td class="p-4 text-right font-black text-xs">${this.formatRupiah(o.totalAmount)}</td>
                <td class="p-4 text-center">
                    <span class="px-2 py-1 rounded text-[9px] font-black uppercase
                        ${o.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 
                          o.status === 'PAID' ? 'bg-blue-100 text-blue-700' :
                          o.status === 'SHIPPED' ? 'bg-orange-100 text-orange-700' : 'bg-green-100 text-green-700'}">
                        ${o.status}
                    </span>
                    ${o.status === 'PENDING' ? 
                        `<button onclick="handleApprovePayment(${o.id})" class="block w-full mt-2 bg-blue-600 hover:bg-blue-700 text-white text-[8px] px-2 py-1.5 rounded font-bold shadow-sm transition">APPROVE PAY</button>` : 
                      o.status === 'PAID' ? 
                        `<button onclick="navigate('shipping-section')" class="block w-full mt-2 bg-orange-500 hover:bg-orange-600 text-white text-[8px] px-2 py-1.5 rounded font-bold shadow-sm transition">GO TO SHIPPING</button>` : ''
                    }
                </td>
            </tr>
        `}).reverse().join('');
    },

    renderCharts(orders) {
        if (!orders || orders.length === 0) return;

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
    
};