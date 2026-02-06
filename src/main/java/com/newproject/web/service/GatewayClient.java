package com.newproject.web.service;

import com.newproject.web.dto.*;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GatewayClient {
    private final WebClient oauth2WebClient;
    private final WebClient defaultWebClient;
    private final String baseUrl;

    public GatewayClient(
        @Qualifier("oauth2WebClient") WebClient oauth2WebClient,
        @Qualifier("defaultWebClient") WebClient defaultWebClient,
        @Value("${app.gateway-base-url}") String baseUrl) {
        this.oauth2WebClient = oauth2WebClient;
        this.defaultWebClient = defaultWebClient;
        this.baseUrl = baseUrl;
    }

    private WebClient client() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OAuth2AuthenticationToken) {
            return oauth2WebClient;
        }
        return defaultWebClient;
    }

    public List<Product> listProducts() {
        return client().get()
            .uri(baseUrl + "/api/catalog/products")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Product>>() {})
            .blockOptional()
            .orElse(List.of());
    }

    public Product getProduct(Long id) {
        return client().get()
            .uri(baseUrl + "/api/catalog/products/{id}", id)
            .retrieve()
            .bodyToMono(Product.class)
            .block();
    }

    public Product createProduct(ProductRequest request) {
        return client().post()
            .uri(baseUrl + "/api/catalog/products")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Product.class)
            .block();
    }

    public Product updateProduct(Long id, ProductRequest request) {
        return client().put()
            .uri(baseUrl + "/api/catalog/products/{id}", id)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Product.class)
            .block();
    }

    public void deleteProduct(Long id) {
        client().delete()
            .uri(baseUrl + "/api/catalog/products/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public List<Order> listOrders(Long customerId) {
        return client().get()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/orders")
                .queryParamIfPresent("customerId", Optional.ofNullable(customerId))
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Order>>() {})
            .blockOptional()
            .orElse(List.of());
    }

    public Order createOrder(OrderRequest request) {
        return client().post()
            .uri(baseUrl + "/api/orders")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Order.class)
            .block();
    }

    public void addOrderItem(Long orderId, OrderItemRequest request) {
        client().post()
            .uri(baseUrl + "/api/orders/{orderId}/items", orderId)
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public List<Cart> listCarts(Long customerId) {
        return client().get()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/carts")
                .queryParamIfPresent("customerId", Optional.ofNullable(customerId))
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Cart>>() {})
            .blockOptional()
            .orElse(List.of());
    }

    public Cart createCart(CartRequest request) {
        return client().post()
            .uri(baseUrl + "/api/carts")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Cart.class)
            .block();
    }

    public List<CartItem> listCartItems(Long cartId) {
        return client().get()
            .uri(baseUrl + "/api/carts/{cartId}/items", cartId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<CartItem>>() {})
            .blockOptional()
            .orElse(List.of());
    }

    public CartItem addCartItem(Long cartId, CartItemRequest request) {
        return client().post()
            .uri(baseUrl + "/api/carts/{cartId}/items", cartId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CartItem.class)
            .block();
    }

    public void deleteCartItem(Long itemId) {
        client().delete()
            .uri(baseUrl + "/api/carts/items/{itemId}", itemId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public List<Customer> listCustomers() {
        return client().get()
            .uri(baseUrl + "/api/customers")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Customer>>() {})
            .blockOptional()
            .orElse(List.of());
    }

    public Customer createCustomer(CustomerRequest request) {
        return client().post()
            .uri(baseUrl + "/api/customers")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Customer.class)
            .block();
    }

    public List<Payment> listPayments(Long orderId) {
        return client().get()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/payments")
                .queryParamIfPresent("orderId", Optional.ofNullable(orderId))
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Payment>>() {})
            .blockOptional()
            .orElse(List.of());
    }

    public List<Shipment> listShipments(Long orderId) {
        return client().get()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/shipments")
                .queryParamIfPresent("orderId", Optional.ofNullable(orderId))
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Shipment>>() {})
            .blockOptional()
            .orElse(List.of());
    }
}
