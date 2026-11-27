package com.jee.shop.gateway.application.dto;

/** HTTP response data returned from a downstream service. */
public record ProxyResult(int status, String contentType, String body) {
    public static ProxyResult unavailable(String serviceName) {
        return new ProxyResult(503, "application/json", "{\"error\":\"" + serviceName + " unavailable\"}");
    }
}
