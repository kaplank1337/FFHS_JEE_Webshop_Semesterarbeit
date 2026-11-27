package com.jee.shop.order.adapter.in.rest;

import com.jee.shop.order.application.dto.CreateOrderRequest;
import com.jee.shop.order.application.dto.OrderDto;
import com.jee.shop.order.application.dto.UpdateOrderStatusRequest;
import com.jee.shop.order.application.mapper.OrderMapper;
import com.jee.shop.order.application.port.out.CurrentUserPort;
import com.jee.shop.order.application.port.in.CheckoutUseCase;
import com.jee.shop.order.application.port.in.OrderUseCases;
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

/**
 * Order REST Resource.
 * Handles order management endpoints.
 *
 * Endpoints (authenticated):
 * - POST /orders — Create new order (checkout)
 * - GET /orders — Get user's orders
 * - GET /orders/{id} — Get a specific order owned by the user
 * - PUT /orders/{id}/status — Change status of an owned order
 * - DELETE /orders/{id} — Delete an owned order
 */
@Path("/orders")
@RequestScoped
public class OrderResource {

    private static final Logger logger = LoggerFactory.getLogger(OrderResource.class);

    @Inject
    private CheckoutUseCase checkoutService;

    @Inject
    private OrderUseCases orderService;

    @Inject
    private CurrentUserPort currentUserService;

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

        UUID userId = currentUserService.requireUserId(
                headers.getHeaderString(HttpHeaders.AUTHORIZATION));

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

        UUID userId = currentUserService.requireUserId(
                headers.getHeaderString(HttpHeaders.AUTHORIZATION));

        List<OrderDto> orders = orderService.getUserOrders(userId).stream()
                .map(OrderMapper::toDto)
                .toList();

        logger.debug("Returning {} orders for user {}", orders.size(), userId);
        return Response.ok(orders).build();
    }

    /**
     * GET /orders/{id} — Get specific order
     *
     * @param orderId the order ID
     * @param headers HTTP headers (contains Authorization)
     * @return order details or 404
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrder(@PathParam("id") UUID orderId, @Context HttpHeaders headers) {
        logger.debug("GET /orders/{} - Retrieving order", orderId);
        UUID userId = currentUserService.requireUserId(
                headers.getHeaderString(HttpHeaders.AUTHORIZATION));

        OrderDto order = orderService.getOrderByIdForUser(orderId, userId)
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

    /** Updates the status of an order owned by the authenticated user. */
    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrderStatus(@PathParam("id") UUID orderId,
                                      @Valid UpdateOrderStatusRequest request,
                                      @Context HttpHeaders headers) {
        UUID userId = currentUserService.requireUserId(
                headers.getHeaderString(HttpHeaders.AUTHORIZATION));
        OrderDto updated = OrderMapper.toDto(
                orderService.updateOrderStatus(orderId, userId, request.status()));
        return Response.ok(updated).build();
    }

    /** Deletes an order owned by the authenticated user. */
    @DELETE
    @Path("/{id}")
    public Response deleteOrder(@PathParam("id") UUID orderId, @Context HttpHeaders headers) {
        UUID userId = currentUserService.requireUserId(
                headers.getHeaderString(HttpHeaders.AUTHORIZATION));
        orderService.deleteOrder(orderId, userId);
        return Response.noContent().build();
    }
}

