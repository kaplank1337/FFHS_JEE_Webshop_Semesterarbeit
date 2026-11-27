package com.jee.shop.order.adapter.in.rest;

import com.jee.shop.order.application.dto.ProductDto;
import com.jee.shop.order.domain.model.ProductEntity;
import com.jee.shop.order.application.mapper.ProductMapper;
import com.jee.shop.order.application.port.in.ProductCatalogUseCases;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product REST Resource.
 * Handles product catalog endpoints.
 *
 * Public Endpoints:
 * - GET /products
 * - GET /products/{id}
 *
 * Admin Endpoints:
 * - POST /products
 * - PUT /products/{id}
 * - DELETE /products/{id}
 */
@Path("/products")
@RequestScoped
public class ProductResource {

    private static final Logger logger = LoggerFactory.getLogger(ProductResource.class);

    @Inject
    private ProductCatalogUseCases productService;

    /**
     * GET /products — List all active products (public)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllProducts() {
        logger.debug("GET /products - Listing all active products");

        List<ProductDto> products = productService.getAllActiveProducts().stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());

        logger.debug("Returning {} products", products.size());
        return Response.ok(products).build();
    }

    /**
     * GET /products/{id} — Get specific product (public)
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductById(@PathParam("id") UUID id) {
        logger.debug("GET /products/{} - Retrieving product", id);

        ProductDto product = productService.getProductById(id)
                .map(ProductMapper::toDto)
                .orElse(null);

        if (product == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Product not found\"}")
                    .build();
        }

        return Response.ok(product).build();
    }

    /**
     * POST /products — Create new product (ADMIN only)
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProduct(@Valid ProductDto dto) {
        logger.info("POST /products - Creating new product: {}", dto.name());

        ProductEntity entity = ProductMapper.toEntity(dto);
        ProductEntity created = productService.createProduct(entity);

        logger.info("Product created: {}", created.getId());
        return Response.status(Response.Status.CREATED)
                .entity(ProductMapper.toDto(created))
                .build();
    }

    /**
     * PUT /products/{id} — Update product (ADMIN only)
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProduct(@PathParam("id") UUID id, @Valid ProductDto dto) {
        logger.info("PUT /products/{} - Updating product", id);

        ProductEntity entity = ProductMapper.toEntity(dto);
        entity.setId(id);
        ProductEntity updated = productService.updateProduct(entity);

        logger.info("Product updated: {}", id);
        return Response.ok(ProductMapper.toDto(updated)).build();
    }

    /**
     * DELETE /products/{id} — Delete product (ADMIN only)
     */
    @DELETE
    @Path("/{id}")
    public Response deleteProduct(@PathParam("id") UUID id) {
        logger.info("DELETE /products/{} - Deleting product", id);

        productService.deleteProduct(id);

        logger.info("Product deleted: {}", id);
        return Response.noContent().build();
    }
}

