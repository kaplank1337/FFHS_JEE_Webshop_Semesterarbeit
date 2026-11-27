package com.jee.shop.shipping.adapter.in.rest;

import com.jee.shop.shipping.application.dto.ShipmentDto;
import com.jee.shop.shipping.domain.model.ShippingStatus;
import com.jee.shop.shipping.application.mapper.ShipmentMapper;
import com.jee.shop.shipping.application.port.in.ShippingUseCases;
import com.jee.shop.shipping.application.port.out.CurrentUserPort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
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
 * Shipment REST Resource.
 * Handles shipment management endpoints.
 *
 * Endpoints (authenticated):
 * - GET /shipments — Get user's shipments
 * - GET /shipments/{id} — Get specific shipment
 * - PUT /shipments/{id}/status — Update shipment status (optional, admin)
 */
@Path("/shipments")
@RequestScoped
public class ShipmentResource {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentResource.class);

    @Inject
    private ShippingUseCases shippingService;

    @Inject
    private CurrentUserPort currentUser;

    /**
     * GET /shipments — Get all shipments for user
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserShipments(@Context HttpHeaders headers) {
        logger.debug("GET /shipments - Fetching user's shipments");

        UUID userId = currentUser.requireUserId(headers.getHeaderString(HttpHeaders.AUTHORIZATION));

        List<ShipmentDto> shipments = shippingService.getUserShipments(userId).stream()
                .map(ShipmentMapper::toDto)
                .collect(Collectors.toList());

        logger.debug("Returning {} shipments for user: {}", shipments.size(), userId);
        return Response.ok(shipments).build();
    }

    /**
     * GET /shipments/{id} — Get specific shipment
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getShipment(@PathParam("id") UUID shipmentId, @Context HttpHeaders headers) {
        logger.debug("GET /shipments/{} - Retrieving shipment", shipmentId);

        UUID userId = currentUser.requireUserId(headers.getHeaderString(HttpHeaders.AUTHORIZATION));

        ShipmentDto shipment = shippingService.getShipmentForUser(shipmentId, userId)
                .map(ShipmentMapper::toDto)
                .orElse(null);

        if (shipment == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Shipment not found\"}")
                    .build();
        }

        logger.debug("Shipment found: {}", shipmentId);
        return Response.ok(shipment).build();
    }

    /**
     * PUT /shipments/{id}/status — Update shipment status (optional, admin)
     */
    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateShipmentStatus(@PathParam("id") UUID shipmentId, StatusUpdateRequest request,
                                         @Context HttpHeaders headers) {
        logger.info("PUT /shipments/{}/status - Updating status to: {}", shipmentId, request.status());

        try {
            ShippingStatus newStatus = ShippingStatus.valueOf(request.status());
            ShipmentDto updatedShipment = ShipmentMapper.toDto(
                shippingService.updateShipmentStatus(shipmentId,
                        currentUser.requireUserId(headers.getHeaderString(HttpHeaders.AUTHORIZATION)), newStatus)
            );

            logger.info("Shipment status updated: {}", shipmentId);
            return Response.ok(updatedShipment).build();

        } catch (IllegalArgumentException e) {
            logger.warn("Status update failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Status update request DTO.
     */
    public record StatusUpdateRequest(String status) {}
}

