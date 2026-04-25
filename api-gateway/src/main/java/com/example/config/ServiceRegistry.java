package com.example.config;

import org.springframework.stereotype.Component;

@Component
public class ServiceRegistry {
    
    private static final String BASE_URL = "http://localhost";
    
    public static final String AUTH_SERVICE_URL = BASE_URL + ":8081";
    public static final String PRODUCT_SERVICE_URL = BASE_URL + ":8082";
    public static final String CUSTOMER_SERVICE_URL = BASE_URL + ":8083";
    public static final String ORDER_SERVICE_URL = BASE_URL + ":8084";
    public static final String INVENTORY_SERVICE_URL = BASE_URL + ":8085";
    public static final String SHIPPING_SERVICE_URL = BASE_URL + ":8086";
    
    /**
     * Route path to service URL mapping
     */
    public String getServiceUrl(String path) {
        if (path.startsWith("/api/auth") || path.startsWith("/api/login")) {
            return AUTH_SERVICE_URL;
        } else if (path.startsWith("/api/products") || path.startsWith("/api/categories")) {
            return PRODUCT_SERVICE_URL;
        } else if (path.startsWith("/api/customers")) {
            return CUSTOMER_SERVICE_URL;
        } else if (path.startsWith("/api/orders")) {
            return ORDER_SERVICE_URL;
        } else if (path.startsWith("/api/inventory") || path.startsWith("/api/reservations")) {
            return INVENTORY_SERVICE_URL;
        } else if (path.startsWith("/api/shipments")) {
            return SHIPPING_SERVICE_URL;
        }
        return null;
    }
}
