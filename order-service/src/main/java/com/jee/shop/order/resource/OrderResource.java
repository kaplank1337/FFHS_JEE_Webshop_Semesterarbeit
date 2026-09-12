package com.jee.shop.order.resource;

import com.jee.shop.order.dto.CreateOrderRequest;
import com.jee.shop.order.dto.OrderDto;
import com.jee.shop.order.entity.OrderEntity;
import com.jee.shop.order.mapper.OrderMapper;
import com.jee.shop.order.service.CheckoutService;
import com.jee.shop.order.service.OrderService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order REST Resource.
 * Handles order management endpoints.
 *
 * Endpoints (authenticated):
 * - POST /orders — Create new order (checkout)
 * - GET /orders — Get user's orders
 * - GET /orders/{id} — Get specific order
 */
@Path("/orders")
@RequestScoped
public class OrderResource {

    private static final Logger logger = LoggerFactory.getLogger(OrderResource.class);

    @Inject
    private CheckoutService checkoutService;

    @Inject
    private OrderService orderService;

    /**
     * POST /orders — Create new order (checkout)
     *
     * @param request checkout request with items
     * @param headers HTTP headers (contains Authorization)
     * @return created order or error
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(@Valid CreateOrderRequest request, @Context HttpHeaders headers) {
        logger.info("POST /orders - Creating new order");

        // Extract userId from token (simplified - normally via filter)
        // For now, use a test UUID (in production, extract from JWT)
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        try {
            OrderDto order = checkoutService.createOrder(request, userId);
            logger.info("Order created: {}", order.id());

            return Response.status(Response.Status.CREATED)
                    .entity(order)
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warn("Order validation failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * GET /orders — Get all user's orders
     *
     * @param headers HTTP headers (contains Authorization)
     * @return list of user's orders
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserOrders(@Context HttpHeaders headers) {
        logger.debug("GET /orders - Fetching user's orders");

        // Extract userId from token (simplified)
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        List<OrderDto> orders = orderService.getUserOrders(userId).stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());

        logger.debug("Returning {} orders for user {}", orders.size(), userId);
        return Response.ok(orders).build();
    }

    /**
     * GET /orders/{id} — Get specific order
     *
     * @param orderId the order ID
     * @return order details or 404
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrder(@PathParam("id") UUID orderId) {
        logger.debug("GET /orders/{} - Retrieving order", orderId);

        OrderDto order = orderService.getOrderById(orderId)
                .map(OrderMapper::toDto)
                .orElse(null);

        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Order not found\"}")
                    .build();
        }

        logger.debug("Order found: {}", orderId);
        return Response.ok(order).build();
    }
}

