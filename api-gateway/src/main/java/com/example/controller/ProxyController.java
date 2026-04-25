package com.example.controller;

import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.config.ServiceRegistry;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class ProxyController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ServiceRegistry serviceRegistry;
    
    /**
     * Generic proxy handler for all API requests
     */
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
                                              RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<?> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        
        try {
            String requestPath = "/api" + request.getPathInfo();
            String serviceUrl = serviceRegistry.getServiceUrl(requestPath);
            
            if (serviceUrl == null) {
                return ResponseEntity.notFound().build();
            }
            
            String fullUrl = serviceUrl + requestPath;
            
            // Add query string if present
            if (request.getQueryString() != null) {
                fullUrl += "?" + request.getQueryString();
            }
            
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpHeaders headers = new HttpHeaders();
            
            // Copy headers from request
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("content-length")) {
                    String headerValue = request.getHeader(headerName);
                    headers.add(headerName, headerValue);
                }
            }
            
            HttpEntity<?> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(fullUrl, method, entity, Object.class);
            
        } catch (RestClientException e) {
            return ResponseEntity.status(500).body("Service unavailable: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
