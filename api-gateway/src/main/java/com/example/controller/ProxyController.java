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
            String requestPath = request.getRequestURI();

            // Jangan proxy path milik Swagger UI sendiri (dihandle oleh springdoc)
            if (requestPath.startsWith("/swagger-ui") || requestPath.startsWith("/v3/api-docs")) {
                return ResponseEntity.status(404)
                    .body("{\"error\": \"Path ini adalah lokal Gateway. Gunakan /swagger-ui/index.html\"}" );
            }

            String serviceUrl = serviceRegistry.getServiceUrl(requestPath);
            
            if (serviceUrl == null) {
                return ResponseEntity.status(404).body("{\"error\": \"API Gateway: No route found for path: " + requestPath + "\"}");
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
            
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            org.springframework.http.HttpHeaders errorHeaders = new org.springframework.http.HttpHeaders();
            if (e.getResponseHeaders() != null && e.getResponseHeaders().getContentType() != null) {
                errorHeaders.setContentType(e.getResponseHeaders().getContentType());
            } else {
                errorHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            }
            return ResponseEntity.status(e.getStatusCode())
                    .headers(errorHeaders)
                    .body(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            return ResponseEntity.status(500).body("Service unavailable: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
