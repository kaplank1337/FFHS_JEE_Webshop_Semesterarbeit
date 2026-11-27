package com.jee.shop.gateway.application.port.out;

import com.jee.shop.gateway.application.dto.ProxyResult;

/** Outbound HTTP contract for the Order-Service product catalog. */
public interface ProductCatalogPort {
    ProxyResult find(String path);
}
