// js/user-logic.js

const API_BASE_URL = "http://localhost:8080/api";

async function fetchProducts() {
    try {
        const response = await fetch(`${API_BASE_URL}/products`); // Ganti sesuai endpoint kamu
        if (!response.ok) throw new Error("Gagal mengambil data");
        
        const products = await response.json();
        renderProducts(products);
    } catch (error) {
        console.error("Error:", error);
    }
}

function renderProducts(products) {
    const grid = document.getElementById('productGrid');
    grid.innerHTML = ''; // Bersihkan skeleton/loader

    products.forEach(prod => {
        const card = `
            <div class="group border border-gray-100 rounded-3xl p-4 bg-white shadow-sm">
                <div class="relative w-full h-40 rounded-2xl overflow-hidden mb-4">
                    <img src="${prod.imageUrl || '/assets/placeholder.jpg'}" class="w-full h-full object-cover">
                </div>
                <h4 class="font-black text-gray-800 text-sm mb-2">${prod.name}</h4>
                <div class="flex justify-between items-center">
                    <span class="font-black text-brand text-lg">Rp ${prod.price.toLocaleString()}</span>
                    <button onclick="addToCart(${prod.id})" class="w-8 h-8 bg-brand-surface text-brand rounded-full">+</button>
                </div>
            </div>
        `;
        grid.innerHTML += card;
    });
}

// Jalankan fungsi saat halaman selesai dimuat
document.addEventListener('DOMContentLoaded', fetchProducts);