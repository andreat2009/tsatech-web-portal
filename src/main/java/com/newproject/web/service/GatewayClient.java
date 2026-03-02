package com.newproject.web.service;

import com.newproject.web.dto.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GatewayClient {
    private static final Logger logger = LoggerFactory.getLogger(GatewayClient.class);

    private final WebClient oauth2WebClient;
    private final WebClient defaultWebClient;
    private final String baseUrl;

    public GatewayClient(
        @Qualifier("oauth2WebClient") WebClient oauth2WebClient,
        @Qualifier("defaultWebClient") WebClient defaultWebClient,
        @Value("${app.gateway-base-url}") String baseUrl
    ) {
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
        return listProducts(null, null, null, null, null, null);
    }

    public List<Product> listProducts(
        String query,
        Long categoryId,
        Boolean active,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort
    ) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/catalog/products")
                    .queryParamIfPresent("q", Optional.ofNullable(query))
                    .queryParamIfPresent("categoryId", Optional.ofNullable(categoryId))
                    .queryParamIfPresent("active", Optional.ofNullable(active))
                    .queryParamIfPresent("minPrice", Optional.ofNullable(minPrice))
                    .queryParamIfPresent("maxPrice", Optional.ofNullable(maxPrice))
                    .queryParamIfPresent("sort", Optional.ofNullable(sort))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Product>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/catalog/products"
        );
    }

    public Product getProduct(Long id) {
        return client().get()
            .uri(baseUrl + "/api/catalog/products/{id}", id)
            .retrieve()
            .bodyToMono(Product.class)
            .block();
    }

    public Optional<Product> getProductSafe(Long id) {
        try {
            return Optional.ofNullable(getProduct(id));
        } catch (Exception ex) {
            logger.warn("Gateway call failed for /api/catalog/products/{}: {}", id, ex.getMessage());
            return Optional.empty();
        }
    }

    public List<Category> listCategories(Boolean active) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/catalog/categories")
                    .queryParamIfPresent("active", Optional.ofNullable(active))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Category>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/catalog/categories"
        );
    }

    public List<CategoryTree> listCategoryTree(Boolean active) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/catalog/categories/tree")
                    .queryParamIfPresent("active", Optional.ofNullable(active))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CategoryTree>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/catalog/categories/tree"
        );
    }


    public Optional<Category> getCategorySafe(Long id) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(baseUrl + "/api/catalog/categories/{id}", id)
                    .retrieve()
                    .bodyToMono(Category.class)
                    .block()
            ),
            "/api/catalog/categories/" + id,
            Optional.empty()
        );
    }

    public Category createCategory(CategoryRequest request) {
        return client().post()
            .uri(baseUrl + "/api/catalog/categories")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Category.class)
            .block();
    }

    public Category updateCategory(Long id, CategoryRequest request) {
        return client().put()
            .uri(baseUrl + "/api/catalog/categories/{id}", id)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Category.class)
            .block();
    }

    public void deleteCategory(Long id) {
        client().delete()
            .uri(baseUrl + "/api/catalog/categories/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
    }



    public List<ProductReview> listProductReviews(Long productId) {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/catalog/products/{productId}/reviews", productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ProductReview>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/catalog/products/{productId}/reviews"
        );
    }

    public ProductReview addProductReview(Long productId, ProductReviewRequest request) {
        return client().post()
            .uri(baseUrl + "/api/catalog/products/{productId}/reviews", productId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ProductReview.class)
            .block();
    }



    public List<ProductReview> listReviews(Boolean approved) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/catalog/reviews")
                    .queryParamIfPresent("approved", Optional.ofNullable(approved))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ProductReview>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/catalog/reviews"
        );
    }

    public ProductReview setReviewApproval(Long reviewId, boolean approved) {
        return client().patch()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/catalog/reviews/{id}/approval")
                .queryParam("approved", approved)
                .build(reviewId))
            .retrieve()
            .bodyToMono(ProductReview.class)
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
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/orders")
                    .queryParamIfPresent("customerId", Optional.ofNullable(customerId))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Order>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/orders"
        );
    }

    public Optional<Order> getOrderSafe(Long orderId) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(baseUrl + "/api/orders/{id}", orderId)
                    .retrieve()
                    .bodyToMono(Order.class)
                    .block()
            ),
            "/api/orders/" + orderId,
            Optional.empty()
        );
    }

    public List<OrderItem> listOrderItems(Long orderId) {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/orders/{orderId}/items", orderId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<OrderItem>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/orders/{orderId}/items"
        );
    }


    public List<OrderReturn> listReturns(Long customerId, Long orderId) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/orders/returns")
                    .queryParamIfPresent("customerId", Optional.ofNullable(customerId))
                    .queryParamIfPresent("orderId", Optional.ofNullable(orderId))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<OrderReturn>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/orders/returns"
        );
    }

    public OrderReturn createReturn(OrderReturnRequest request) {
        return client().post()
            .uri(baseUrl + "/api/orders/returns")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OrderReturn.class)
            .block();
    }

    public OrderReturn updateReturnStatus(Long returnId, String status) {
        return client().patch()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/orders/returns/{id}/status")
                .queryParam("status", status)
                .build(returnId))
            .retrieve()
            .bodyToMono(OrderReturn.class)
            .block();
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
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/carts")
                    .queryParamIfPresent("customerId", Optional.ofNullable(customerId))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Cart>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/carts"
        );
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
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/carts/{cartId}/items", cartId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CartItem>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/carts/{cartId}/items"
        );
    }

    public CartItem addCartItem(Long cartId, CartItemRequest request) {
        return client().post()
            .uri(baseUrl + "/api/carts/{cartId}/items", cartId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CartItem.class)
            .block();
    }

    public void updateCartItemQuantity(Long itemId, Integer quantity) {
        client().patch()
            .uri(baseUrl + "/api/carts/items/{itemId}/quantity", itemId)
            .bodyValue(java.util.Map.of("quantity", quantity))
            .retrieve()
            .toBodilessEntity()
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
        return listCustomers(null, null, null);
    }

    public List<Customer> listCustomers(String email, String keycloakUserId, Boolean active) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/customers")
                    .queryParamIfPresent("email", Optional.ofNullable(email))
                    .queryParamIfPresent("keycloakUserId", Optional.ofNullable(keycloakUserId))
                    .queryParamIfPresent("active", Optional.ofNullable(active))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Customer>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers"
        );
    }

    public Customer createCustomer(CustomerRequest request) {
        return client().post()
            .uri(baseUrl + "/api/customers")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Customer.class)
            .block();
    }

    public List<Address> listCustomerAddresses(Long customerId) {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/customers/{customerId}/addresses", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Address>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers/{customerId}/addresses"
        );
    }

    public Address createCustomerAddress(Long customerId, AddressRequest request) {
        return client().post()
            .uri(baseUrl + "/api/customers/{customerId}/addresses", customerId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Address.class)
            .block();
    }



    public List<WishlistItem> listWishlist(Long customerId) {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/customers/{customerId}/wishlist", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<WishlistItem>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers/{customerId}/wishlist"
        );
    }

    public WishlistItem addWishlistItem(Long customerId, Long productId) {
        return client().post()
            .uri(baseUrl + "/api/customers/{customerId}/wishlist", customerId)
            .bodyValue(java.util.Map.of("productId", productId))
            .retrieve()
            .bodyToMono(WishlistItem.class)
            .block();
    }

    public void removeWishlistItem(Long customerId, Long productId) {
        client().delete()
            .uri(baseUrl + "/api/customers/{customerId}/wishlist/{productId}", customerId, productId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }
    public List<Payment> listPayments(Long orderId) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/payments")
                    .queryParamIfPresent("orderId", Optional.ofNullable(orderId))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Payment>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/payments"
        );
    }

    public Payment createPayment(PaymentRequest request) {
        return client().post()
            .uri(baseUrl + "/api/payments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Payment.class)
            .block();
    }

    public List<Shipment> listShipments(Long orderId) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/shipments")
                    .queryParamIfPresent("orderId", Optional.ofNullable(orderId))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Shipment>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/shipments"
        );
    }

    public Shipment createShipment(ShipmentRequest request) {
        return client().post()
            .uri(baseUrl + "/api/shipments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Shipment.class)
            .block();
    }

    public List<InventoryItem> listInventory() {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/inventory")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<InventoryItem>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/inventory"
        );
    }

    public List<ProductPrice> listPrices() {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/pricing")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ProductPrice>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/pricing"
        );
    }


    public List<Coupon> listCoupons() {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/pricing/coupons")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Coupon>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/pricing/coupons"
        );
    }

    public Coupon createCoupon(CouponRequest request) {
        return client().post()
            .uri(baseUrl + "/api/pricing/coupons")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Coupon.class)
            .block();
    }

    public Coupon updateCoupon(Long id, CouponRequest request) {
        return client().put()
            .uri(baseUrl + "/api/pricing/coupons/{id}", id)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Coupon.class)
            .block();
    }

    public void deleteCoupon(Long id) {
        client().delete()
            .uri(baseUrl + "/api/pricing/coupons/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public PriceQuoteResponse quote(PriceQuoteRequest request) {
        return safeCall(
            () -> client().post()
                .uri(baseUrl + "/api/pricing/quote")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PriceQuoteResponse.class)
                .block(),
            "/api/pricing/quote",
            fallbackQuote(request)
        );
    }

    private PriceQuoteResponse fallbackQuote(PriceQuoteRequest request) {
        PriceQuoteResponse fallback = new PriceQuoteResponse();
        fallback.setSubtotal(request.getSubtotal());
        fallback.setShipping(request.getShipping());
        fallback.setDiscount(BigDecimal.ZERO);
        fallback.setTotal(request.getSubtotal().add(request.getShipping()));
        fallback.setMessage("Pricing service non raggiungibile: nessun coupon applicato");
        return fallback;
    }

    public String notificationPing() {
        return safeCall(
            () -> client().get()
                .uri(baseUrl + "/api/notifications/ping")
                .retrieve()
                .bodyToMono(String.class)
                .blockOptional()
                .orElse("unreachable"),
            "/api/notifications/ping",
            "unreachable"
        );
    }

    private <T> List<T> safeList(Supplier<List<T>> supplier, String endpoint) {
        return safeCall(supplier, endpoint, List.of());
    }

    private <T> T safeCall(Supplier<T> supplier, String endpoint, T fallback) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            logger.warn("Gateway call failed for {}: {}", endpoint, ex.getMessage());
            return fallback;
        }
    }
}
