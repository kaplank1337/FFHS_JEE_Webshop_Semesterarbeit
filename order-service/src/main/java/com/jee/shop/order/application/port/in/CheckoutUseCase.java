package com.jee.shop.order.application.port.in;

import com.jee.shop.order.application.dto.CreateOrderRequest;
import com.jee.shop.order.application.dto.OrderDto;
import java.util.UUID;

/** Inbound use case for creating a checked-out order. */
public interface CheckoutUseCase {
    OrderDto createOrder(CreateOrderRequest request, UUID userId);
}
