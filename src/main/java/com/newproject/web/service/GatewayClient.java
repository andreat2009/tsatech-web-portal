package com.newproject.web.service;

import com.newproject.web.dto.*;
import com.newproject.web.i18n.LanguageSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GatewayClient {
    private static final Logger logger = LoggerFactory.getLogger(GatewayClient.class);

    private final WebClient defaultWebClient;
    private final WebClient oauth2WebClient;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String baseUrl;
    private final String gatewayPublicBaseUrl;
    private final Duration translationRequestTimeout;

    public GatewayClient(
        @Qualifier("defaultWebClient") WebClient defaultWebClient,
        @Qualifier("oauth2WebClient") WebClient oauth2WebClient,
        OAuth2AuthorizedClientRepository authorizedClientRepository,
        OAuth2AuthorizedClientManager authorizedClientManager,
        OAuth2AuthorizedClientService authorizedClientService,
        @Value("${app.gateway-base-url}") String baseUrl,
        @Value("${app.translation-request-timeout-ms:120000}") int translationRequestTimeoutMs
    ) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        this.defaultWebClient = defaultWebClient.mutate().baseUrl(normalizedBaseUrl).build();
        this.oauth2WebClient = oauth2WebClient.mutate().baseUrl(normalizedBaseUrl).build();
        this.authorizedClientRepository = authorizedClientRepository;
        this.authorizedClientManager = authorizedClientManager;
        this.authorizedClientService = authorizedClientService;
        this.baseUrl = "";
        this.gatewayPublicBaseUrl = normalizedBaseUrl;
        this.translationRequestTimeout = Duration.ofMillis(Math.max(1_000, translationRequestTimeoutMs));
    }

    private String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null) {
            return "";
        }
        String trimmed = rawUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private WebClient client() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OAuth2AuthenticationToken oauth) {
            String accessToken = resolveAccessToken(oauth);
            if (accessToken != null && !accessToken.isBlank()) {
                return defaultWebClient.mutate()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .build();
            }
            return oauth2WebClient;
        }
        return defaultWebClient;
    }

    private String resolveAccessToken(OAuth2AuthenticationToken authenticationToken) {
        OAuth2AuthorizedClient authorizedClient = resolveAuthorizedClient(authenticationToken);
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return null;
        }
        return authorizedClient.getAccessToken().getTokenValue();
    }

    private OAuth2AuthorizedClient resolveAuthorizedClient(OAuth2AuthenticationToken authenticationToken) {
        if (authenticationToken == null) {
            return null;
        }

        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (authorizedClientManager != null && requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            HttpServletResponse response = servletRequestAttributes.getResponse();
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(authenticationToken.getAuthorizedClientRegistrationId())
                .principal(authenticationToken)
                .attribute(HttpServletRequest.class.getName(), request)
                .attribute(HttpServletResponse.class.getName(), response)
                .build();
            try {
                OAuth2AuthorizedClient refreshed = authorizedClientManager.authorize(authorizeRequest);
                if (refreshed != null && refreshed.getAccessToken() != null) {
                    return refreshed;
                }
            } catch (Exception ex) {
                logger.debug("Unable to authorize OAuth2 client via manager: {}", ex.getMessage());
            }
        }

        if (authorizedClientRepository != null && requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            try {
                OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
                    authenticationToken.getAuthorizedClientRegistrationId(),
                    authenticationToken,
                    servletRequestAttributes.getRequest()
                );
                if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                    return authorizedClient;
                }
            } catch (Exception ex) {
                logger.debug("Unable to load OAuth2 client from repository: {}", ex.getMessage());
            }
        }

        if (authorizedClientService == null) {
            return null;
        }
        try {
            return authorizedClientService.loadAuthorizedClient(
                authenticationToken.getAuthorizedClientRegistrationId(),
                authenticationToken.getName()
            );
        } catch (Exception ex) {
            logger.debug("Unable to load OAuth2 client from service: {}", ex.getMessage());
            return null;
        }
    }

    private String currentLanguage() {
        return LanguageSupport.fromLocale(LocaleContextHolder.getLocale());
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
            () -> {
                List<Product> products = client().get()
                    .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/api/catalog/products")
                        .queryParamIfPresent("q", Optional.ofNullable(query))
                        .queryParamIfPresent("categoryId", Optional.ofNullable(categoryId))
                        .queryParamIfPresent("active", Optional.ofNullable(active))
                        .queryParamIfPresent("minPrice", Optional.ofNullable(minPrice))
                        .queryParamIfPresent("maxPrice", Optional.ofNullable(maxPrice))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .queryParam("lang", currentLanguage())
                        .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Product>>() {})
                    .blockOptional()
                    .orElse(List.of());
                products.forEach(this::normalizeProductMedia);
                return products;
            },
            "/api/catalog/products"
        );
    }

    public Product getProduct(Long id) {
        Product product = client().get()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/catalog/products/{id}")
                .queryParam("lang", currentLanguage())
                .build(id))
            .retrieve()
            .bodyToMono(Product.class)
            .block();
        normalizeProductMedia(product);
        return product;
    }


    public List<Manufacturer> listManufacturers() {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/catalog/manufacturers")
                    .queryParam("lang", currentLanguage())
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Manufacturer>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/catalog/manufacturers"
        );
    }

    public Manufacturer createManufacturer(ManufacturerRequest request) {
        return client().post()
            .uri(baseUrl + "/api/catalog/manufacturers")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Manufacturer.class)
            .block();
    }

    public Manufacturer updateManufacturer(Long id, ManufacturerRequest request) {
        return client().put()
            .uri(baseUrl + "/api/catalog/manufacturers/{id}", id)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Manufacturer.class)
            .block();
    }

    public void deleteManufacturer(Long id) {
        client().delete()
            .uri(baseUrl + "/api/catalog/manufacturers/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public Optional<Manufacturer> getManufacturerSafe(Long id) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/api/catalog/manufacturers/{id}")
                        .queryParam("lang", currentLanguage())
                        .build(id))
                    .retrieve()
                    .bodyToMono(Manufacturer.class)
                    .block()
            ),
            "/api/catalog/manufacturers/" + id,
            Optional.empty()
        );
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
                    .queryParam("lang", currentLanguage())
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
                    .queryParam("lang", currentLanguage())
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
                    .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/api/catalog/categories/{id}")
                        .queryParam("lang", currentLanguage())
                        .build(id))
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
        Product product = client().post()
            .uri(baseUrl + "/api/catalog/products")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Product.class)
            .block();
        normalizeProductMedia(product);
        return product;
    }

    public Product updateProduct(Long id, ProductRequest request) {
        Product product = client().put()
            .uri(baseUrl + "/api/catalog/products/{id}", id)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Product.class)
            .block();
        normalizeProductMedia(product);
        return product;
    }

    public ProductAutoTranslateResponse autoTranslateProduct(ProductAutoTranslateRequest request) {
        return safeCall(
            () -> client().post()
                .uri(baseUrl + "/api/catalog/products/translate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProductAutoTranslateResponse.class)
                .block(),
            "/api/catalog/products/translate",
            null
        );
    }

    public CategoryAutoTranslateResponse autoTranslateCategory(CategoryAutoTranslateRequest request) {
        return safeCall(
            () -> client().post()
                .uri(baseUrl + "/api/catalog/categories/translate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CategoryAutoTranslateResponse.class)
                .block(),
            "/api/catalog/categories/translate",
            null
        );
    }

    public CouponAutoTranslateResponse autoTranslateCoupon(CouponAutoTranslateRequest request) {
        return safeCall(
            () -> client().post()
                .uri(baseUrl + "/api/pricing/coupons/translate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CouponAutoTranslateResponse.class)
                .block(),
            "/api/pricing/coupons/translate",
            null
        );
    }

    public void deleteProduct(Long id) {
        client().delete()
            .uri(baseUrl + "/api/catalog/products/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public void uploadProductCover(Long productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource())
            .filename(file.getOriginalFilename())
            .contentType(resolveContentType(file));

        client().post()
            .uri(baseUrl + "/api/catalog/products/{productId}/images/cover", productId)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public void uploadProductGallery(Long productId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        boolean hasValidFile = false;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            hasValidFile = true;
            bodyBuilder.part("files", file.getResource())
                .filename(file.getOriginalFilename())
                .contentType(resolveContentType(file));
        }
        if (!hasValidFile) {
            return;
        }

        client().post()
            .uri(baseUrl + "/api/catalog/products/{productId}/images/gallery", productId)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public void deleteProductImage(Long productId, Long imageId) {
        client().delete()
            .uri(baseUrl + "/api/catalog/products/{productId}/images/{imageId}", productId, imageId)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public void setProductCoverImage(Long productId, Long imageId) {
        client().patch()
            .uri(baseUrl + "/api/catalog/products/{productId}/images/{imageId}/cover", productId, imageId)
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

    public PagedResponse<Order> listOrdersPage(Long customerId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        return safeCall(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/orders/paged")
                    .queryParamIfPresent("customerId", Optional.ofNullable(customerId))
                    .queryParam("page", safePage)
                    .queryParam("size", safeSize)
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<Order>>() {})
                .blockOptional()
                .orElse(PagedResponse.empty(safePage, safeSize)),
            "/api/orders/paged",
            PagedResponse.empty(safePage, safeSize)
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

    public Order updateOrder(Long orderId, OrderRequest request) {
        return client().put()
            .uri(baseUrl + "/api/orders/{orderId}", orderId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Order.class)
            .block();
    }

    public Order updateOrderStatus(Long orderId, String status) {
        Order existing = getOrderSafe(orderId)
            .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        OrderRequest request = new OrderRequest();
        request.setCustomerId(existing.getCustomerId());
        request.setCurrency(existing.getCurrency());
        request.setTotal(existing.getTotal());
        request.setStatus(status);
        request.setCustomerEmail(existing.getCustomerEmail());
        request.setCustomerFirstName(existing.getCustomerFirstName());
        request.setCustomerLastName(existing.getCustomerLastName());
        request.setCustomerPhone(existing.getCustomerPhone());
        request.setCustomerLocale(existing.getCustomerLocale());
        request.setOrderComment(existing.getOrderComment());
        request.setGuestCheckout(existing.getGuestCheckout());

        return updateOrder(orderId, request);
    }

    public void deleteOrder(Long orderId) {
        client().delete()
            .uri(baseUrl + "/api/orders/{orderId}", orderId)
            .retrieve()
            .toBodilessEntity()
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
        return safeCall(
            () -> client().post()
                .uri(baseUrl + "/api/customers")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Customer.class)
                .block(),
            "/api/customers",
            null
        );
    }

    public List<CustomFieldDefinition> listCustomFields(String scope, Boolean active) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/customers/custom-fields")
                    .queryParamIfPresent("scope", Optional.ofNullable(scope))
                    .queryParamIfPresent("active", Optional.ofNullable(active))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CustomFieldDefinition>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers/custom-fields"
        );
    }

    public List<CustomerCustomFieldValue> listCustomerCustomFieldValues(Long customerId, String scope) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/customers/{customerId}/custom-fields/values")
                    .queryParamIfPresent("scope", Optional.ofNullable(scope))
                    .build(customerId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CustomerCustomFieldValue>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers/{customerId}/custom-fields/values"
        );
    }

    public List<CustomerCustomFieldValue> saveCustomerCustomFieldValues(Long customerId, List<CustomerCustomFieldValueRequest> requests) {
        return client().put()
            .uri(baseUrl + "/api/customers/{customerId}/custom-fields/values", customerId)
            .bodyValue(requests)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<CustomerCustomFieldValue>>() {})
            .blockOptional()
            .orElse(List.of());
    }

    public CustomFieldDefinition getCustomField(Long id) {
        return client().get()
            .uri(baseUrl + "/api/customers/custom-fields/{id}", id)
            .retrieve()
            .bodyToMono(CustomFieldDefinition.class)
            .block();
    }

    public CustomFieldDefinition createCustomField(CustomFieldDefinition request) {
        return client().post()
            .uri(baseUrl + "/api/customers/custom-fields")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CustomFieldDefinition.class)
            .block();
    }

    public CustomFieldDefinition updateCustomField(Long id, CustomFieldDefinition request) {
        return client().put()
            .uri(baseUrl + "/api/customers/custom-fields/{id}", id)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CustomFieldDefinition.class)
            .block();
    }

    public void deleteCustomField(Long id) {
        client().delete()
            .uri(baseUrl + "/api/customers/custom-fields/{id}", id)
            .retrieve()
            .toBodilessEntity()
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

    public List<PaymentMethod> listPaymentMethods() {
        return safeList(
            () -> defaultWebClient.get()
                .uri(baseUrl + "/api/payments/methods")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PaymentMethod>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/payments/methods"
        );
    }

    public List<PaymentMethod> listAdminPaymentMethods() {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/payments/admin/methods")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PaymentMethod>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/payments/admin/methods"
        );
    }

    public PaymentMethod getAdminPaymentMethod(Long id) {
        return client().get()
            .uri(baseUrl + "/api/payments/admin/methods/{id}", id)
            .retrieve()
            .bodyToMono(PaymentMethod.class)
            .block();
    }

    public PaymentMethod createAdminPaymentMethod(AdminPaymentMethodForm form) {
        return client().post()
            .uri(baseUrl + "/api/payments/admin/methods")
            .bodyValue(form)
            .retrieve()
            .bodyToMono(PaymentMethod.class)
            .block();
    }

    public PaymentMethod updateAdminPaymentMethod(Long id, AdminPaymentMethodForm form) {
        return client().put()
            .uri(baseUrl + "/api/payments/admin/methods/{id}", id)
            .bodyValue(form)
            .retrieve()
            .bodyToMono(PaymentMethod.class)
            .block();
    }

    public void deleteAdminPaymentMethod(Long id) {
        client().delete()
            .uri(baseUrl + "/api/payments/admin/methods/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public Payment createPayment(PaymentRequest request) {
        return client().post()
            .uri(baseUrl + "/api/payments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Payment.class)
            .block();
    }

    public Payment capturePayPalPayment(Long paymentId, String token) {
        return defaultWebClient.post()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/payments/{paymentId}/capture/paypal")
                .queryParamIfPresent("token", Optional.ofNullable(token))
                .build(paymentId))
            .retrieve()
            .bodyToMono(Payment.class)
            .block();
    }

    public Payment completeFabrickPayment(Long paymentId, FabrickCompletionRequest request) {
        return defaultWebClient.post()
            .uri(baseUrl + "/api/payments/{paymentId}/complete/fabrick", paymentId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Payment.class)
            .block();
    }

    public Payment reconcilePayment(Long paymentId) {
        return client().post()
            .uri(baseUrl + "/api/payments/{paymentId}/reconcile", paymentId)
            .retrieve()
            .bodyToMono(Payment.class)
            .block();
    }

    public Payment refundPayment(Long paymentId, PaymentRefundRequest request) {
        return client().post()
            .uri(baseUrl + "/api/payments/{paymentId}/refund", paymentId)
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

    public Optional<InventoryItem> getInventorySafe(Long productId) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(baseUrl + "/api/inventory/{productId}", productId)
                    .retrieve()
                    .bodyToMono(InventoryItem.class)
                    .block()
            ),
            "/api/inventory/{productId}",
            Optional.empty()
        );
    }

    public InventoryItem createInventory(InventoryRequest request) {
        return client().post()
            .uri(baseUrl + "/api/inventory")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(InventoryItem.class)
            .block();
    }

    public InventoryItem updateInventory(Long productId, InventoryRequest request) {
        return client().put()
            .uri(baseUrl + "/api/inventory/{productId}", productId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(InventoryItem.class)
            .block();
    }

    public InventoryItem upsertInventory(Long productId, InventoryRequest request) {
        return getInventorySafe(productId)
            .map(existing -> updateInventory(productId, request))
            .orElseGet(() -> createInventory(request));
    }

    public void deleteInventory(Long productId) {
        client().delete()
            .uri(baseUrl + "/api/inventory/{productId}", productId)
            .retrieve()
            .toBodilessEntity()
            .block();
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
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/pricing/coupons")
                    .queryParam("lang", currentLanguage())
                    .build())
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
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/pricing/quote")
                    .queryParam("lang", currentLanguage())
                    .build())
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

    public boolean sendOrderConfirmationEmail(OrderConfirmationNotificationRequest request) {
        return safeCall(
            () -> {
                client().post()
                    .uri(baseUrl + "/api/notifications/order-confirmation")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
                return true;
            },
            "/api/notifications/order-confirmation",
            false
        );
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


    public Customer updateCustomer(Long customerId, CustomerRequest request) {
        return client().put()
            .uri(baseUrl + "/api/customers/{customerId}", customerId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Customer.class)
            .block();
    }

    public Optional<Customer> getCustomerSafe(Long customerId) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(baseUrl + "/api/customers/{customerId}", customerId)
                    .retrieve()
                    .bodyToMono(Customer.class)
                    .block()
            ),
            "/api/customers/{customerId}",
            Optional.empty()
        );
    }

    public NewsletterPreference getNewsletterPreference(Long customerId) {
        NewsletterPreference fallback = new NewsletterPreference();
        fallback.setCustomerId(customerId);
        fallback.setSubscribed(Boolean.FALSE);
        return safeCall(
            () -> client().get()
                .uri(baseUrl + "/api/customers/{customerId}/newsletter", customerId)
                .retrieve()
                .bodyToMono(NewsletterPreference.class)
                .block(),
            "/api/customers/{customerId}/newsletter",
            fallback
        );
    }

    public NewsletterPreference updateNewsletterPreference(Long customerId, boolean subscribed) {
        return client().put()
            .uri(baseUrl + "/api/customers/{customerId}/newsletter", customerId)
            .bodyValue(Map.of("subscribed", subscribed))
            .retrieve()
            .bodyToMono(NewsletterPreference.class)
            .block();
    }

    public RewardSummary getRewardSummary(Long customerId) {
        return safeCall(
            () -> client().get()
                .uri(baseUrl + "/api/customers/{customerId}/rewards/summary", customerId)
                .retrieve()
                .bodyToMono(RewardSummary.class)
                .block(),
            "/api/customers/{customerId}/rewards/summary",
            new RewardSummary()
        );
    }

    public List<RewardTransaction> listRewardTransactions(Long customerId) {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/customers/{customerId}/rewards/transactions", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RewardTransaction>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers/{customerId}/rewards/transactions"
        );
    }

    public RewardTransaction addRewardTransaction(Long customerId, RewardTransactionRequest request) {
        return client().post()
            .uri(baseUrl + "/api/customers/{customerId}/rewards/transactions", customerId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(RewardTransaction.class)
            .block();
    }

    public List<StoreTransaction> listStoreTransactions(Long customerId) {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/customers/{customerId}/transactions", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<StoreTransaction>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers/{customerId}/transactions"
        );
    }

    public StoreTransaction addStoreTransaction(Long customerId, StoreTransactionRequest request) {
        return client().post()
            .uri(baseUrl + "/api/customers/{customerId}/transactions", customerId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(StoreTransaction.class)
            .block();
    }

    public List<CustomerSubscription> listSubscriptions(Long customerId) {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/customers/{customerId}/subscriptions", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CustomerSubscription>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers/{customerId}/subscriptions"
        );
    }

    public CustomerSubscription createSubscription(Long customerId, CustomerSubscriptionRequest request) {
        return client().post()
            .uri(baseUrl + "/api/customers/{customerId}/subscriptions", customerId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CustomerSubscription.class)
            .block();
    }

    public CustomerSubscription updateSubscription(Long customerId, Long subscriptionId, CustomerSubscriptionRequest request) {
        return client().put()
            .uri(baseUrl + "/api/customers/{customerId}/subscriptions/{subscriptionId}", customerId, subscriptionId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CustomerSubscription.class)
            .block();
    }

    public List<CustomerDownload> listDownloads(Long customerId) {
        return safeList(
            () -> client().get()
                .uri(baseUrl + "/api/customers/{customerId}/downloads", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CustomerDownload>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/customers/{customerId}/downloads"
        );
    }

    public CustomerDownload createDownload(Long customerId, CustomerDownloadRequest request) {
        return client().post()
            .uri(baseUrl + "/api/customers/{customerId}/downloads", customerId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CustomerDownload.class)
            .block();
    }

    public List<InformationPage> listInformationPages(Boolean active) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/cms/information")
                    .queryParamIfPresent("active", Optional.ofNullable(active))
                    .queryParam("lang", currentLanguage())
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<InformationPage>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/cms/information"
        );
    }


    public Optional<InformationPage> getInformationPageSafe(Long id) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/api/cms/information/{id}")
                        .queryParam("lang", currentLanguage())
                        .build(id))
                    .retrieve()
                    .bodyToMono(InformationPage.class)
                    .block()
            ),
            "/api/cms/information/{id}",
            Optional.empty()
        );
    }

    public Optional<InformationPage> getInformationBySlug(String slug) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/api/cms/information/slug/{slug}")
                        .queryParam("lang", currentLanguage())
                        .build(slug))
                    .retrieve()
                    .bodyToMono(InformationPage.class)
                    .block()
            ),
            "/api/cms/information/slug/{slug}",
            Optional.empty()
        );
    }

    public InformationPage createInformationPage(InformationRequest request) {
        return client().post()
            .uri(baseUrl + "/api/cms/information")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(InformationPage.class)
            .block();
    }

    public InformationAutoTranslateResponse autoTranslateInformationPage(InformationAutoTranslateRequest request) {
        return client().post()
            .uri(baseUrl + "/api/cms/information/translate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(InformationAutoTranslateResponse.class)
            .timeout(translationRequestTimeout)
            .block();
    }

    public InformationPage updateInformationPage(Long id, InformationRequest request) {
        return client().put()
            .uri(baseUrl + "/api/cms/information/{id}", id)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(InformationPage.class)
            .block();
    }

    public void deleteInformationPage(Long id) {
        client().delete()
            .uri(baseUrl + "/api/cms/information/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
    }



    public AnalyticsEvent trackAnalyticsEvent(AnalyticsEventRequest request) {
        return safeCall(
            () -> client().post()
                .uri(baseUrl + "/api/cms/analytics/events")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AnalyticsEvent.class)
                .block(),
            "/api/cms/analytics/events",
            null
        );
    }

    public AnalyticsSummary getAnalyticsSummary() {
        return safeCall(
            () -> client().get()
                .uri(baseUrl + "/api/cms/analytics/summary")
                .retrieve()
                .bodyToMono(AnalyticsSummary.class)
                .block(),
            "/api/cms/analytics/summary",
            null
        );
    }

    public List<AnalyticsEvent> listAnalyticsEvents(Integer limit) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/cms/analytics/events")
                    .queryParamIfPresent("limit", Optional.ofNullable(limit))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AnalyticsEvent>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/cms/analytics/events"
        );
    }

    public List<BlogPost> listBlogPosts(Boolean active) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/cms/blog/posts")
                    .queryParamIfPresent("active", Optional.ofNullable(active))
                    .queryParam("lang", currentLanguage())
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BlogPost>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/cms/blog/posts"
        );
    }


    public Optional<BlogPost> getBlogPostSafe(Long id) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/api/cms/blog/posts/{id}")
                        .queryParam("lang", currentLanguage())
                        .build(id))
                    .retrieve()
                    .bodyToMono(BlogPost.class)
                    .block()
            ),
            "/api/cms/blog/posts/{id}",
            Optional.empty()
        );
    }

    public Optional<BlogPost> getBlogPostBySlug(String slug) {
        return safeCall(
            () -> Optional.ofNullable(
                client().get()
                    .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + "/api/cms/blog/posts/slug/{slug}")
                        .queryParam("lang", currentLanguage())
                        .build(slug))
                    .retrieve()
                    .bodyToMono(BlogPost.class)
                    .block()
            ),
            "/api/cms/blog/posts/slug/{slug}",
            Optional.empty()
        );
    }

    public BlogPost createBlogPost(BlogPostRequest request) {
        return client().post()
            .uri(baseUrl + "/api/cms/blog/posts")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(BlogPost.class)
            .block();
    }

    public BlogPost updateBlogPost(Long id, BlogPostRequest request) {
        return client().put()
            .uri(baseUrl + "/api/cms/blog/posts/{id}", id)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(BlogPost.class)
            .block();
    }

    public void deleteBlogPost(Long id) {
        client().delete()
            .uri(baseUrl + "/api/cms/blog/posts/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public List<BlogComment> listBlogCommentsByPost(Long postId, Boolean approved) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/cms/blog/posts/{postId}/comments")
                    .queryParamIfPresent("approved", Optional.ofNullable(approved))
                    .build(postId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BlogComment>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/cms/blog/posts/{postId}/comments"
        );
    }

    public List<BlogComment> listBlogComments(Boolean approved) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/cms/blog/comments")
                    .queryParamIfPresent("approved", Optional.ofNullable(approved))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BlogComment>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/cms/blog/comments"
        );
    }

    public BlogComment createBlogComment(Long postId, BlogCommentRequest request) {
        return client().post()
            .uri(baseUrl + "/api/cms/blog/posts/{postId}/comments", postId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(BlogComment.class)
            .block();
    }

    public BlogComment setBlogCommentApproval(Long commentId, boolean approved) {
        return client().patch()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/cms/blog/comments/{id}/approval")
                .queryParam("approved", approved)
                .build(commentId))
            .retrieve()
            .bodyToMono(BlogComment.class)
            .block();
    }

    public ContactMessage createContactMessage(ContactMessageRequest request) {
        return client().post()
            .uri(baseUrl + "/api/cms/contact")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ContactMessage.class)
            .block();
    }

    public List<ContactMessage> listContactMessages(String status) {
        return safeList(
            () -> client().get()
                .uri(uriBuilder -> uriBuilder
                    .path(baseUrl + "/api/cms/contact")
                    .queryParamIfPresent("status", Optional.ofNullable(status))
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ContactMessage>>() {})
                .blockOptional()
                .orElse(List.of()),
            "/api/cms/contact"
        );
    }

    public ContactMessage updateContactMessageStatus(Long id, String status) {
        return client().patch()
            .uri(uriBuilder -> uriBuilder
                .path(baseUrl + "/api/cms/contact/{id}/status")
                .queryParam("status", status)
                .build(id))
            .retrieve()
            .bodyToMono(ContactMessage.class)
            .block();
    }

    public PublicStoreSettings getPublicStoreSettings() {
        PublicStoreSettings settings = safeCall(
            () -> defaultWebClient.get()
                .uri(baseUrl + "/api/cms/settings/public")
                .retrieve()
                .bodyToMono(PublicStoreSettings.class)
                .block(),
            "/api/cms/settings/public",
            defaultPublicStoreSettings()
        );
        return settings != null ? settings : defaultPublicStoreSettings();
    }

    public StoreSettings getStoreSettings() {
        return safeCall(
            () -> client().get()
                .uri(baseUrl + "/api/cms/settings")
                .retrieve()
                .bodyToMono(StoreSettings.class)
                .block(),
            "/api/cms/settings",
            null
        );
    }

    public StoreSettings updateStoreSettings(StoreSettings request) {
        return client().put()
            .uri(baseUrl + "/api/cms/settings")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(StoreSettings.class)
            .block();
    }

    private PublicStoreSettings defaultPublicStoreSettings() {
        PublicStoreSettings fallback = new PublicStoreSettings();
        fallback.setSiteName("TSATech Store");
        fallback.setLogoMaxHeightPx(96);
        fallback.setSiteNameFontSizePx(28);
        fallback.setContactEmail("andrea.terrasi78@gmail.com");
        fallback.setSupportEmail("andrea.terrasi78@gmail.com");
        fallback.setSupportPhone("+39 800 000 000");
        fallback.setAddressLine1("Via Roma 1");
        fallback.setCity("Milano");
        fallback.setPostalCode("20100");
        fallback.setCountry("IT");
        return fallback;
    }

    private <T> List<T> safeList(Supplier<List<T>> supplier, String endpoint) {
        return safeCall(supplier, endpoint, List.of());
    }

    private void normalizeProductMedia(Product product) {
        if (product == null) {
            return;
        }

        product.setImage(normalizeMediaUrl(product.getImage()));
        product.setCoverImageUrl(normalizeMediaUrl(product.getCoverImageUrl()));

        if (product.getGalleryImages() != null) {
            for (ProductImage image : product.getGalleryImages()) {
                image.setUrl(normalizeMediaUrl(image.getUrl()));
            }
        }
    }

    private String normalizeMediaUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }

        String mediaProxyUrl = toPortalMediaPath(raw);
        if (mediaProxyUrl != null) {
            return mediaProxyUrl;
        }

        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }

        // Legacy seeded products store image paths like /img/products/*.png,
        // but those files are not published by services. Return null to trigger
        // UI placeholders instead of broken images.
        if (raw.startsWith("/img/")) {
            return null;
        }

        if (raw.startsWith("/")) {
            return gatewayPublicBaseUrl + raw;
        }
        return gatewayPublicBaseUrl + "/" + raw;
    }

    private MediaType resolveContentType(MultipartFile file) {
        if (file == null || file.getContentType() == null || file.getContentType().isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(file.getContentType());
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private <T> T safeCall(Supplier<T> supplier, String endpoint, T fallback) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            logger.warn("Gateway call failed for {}: {}", endpoint, ex.getMessage());
            return fallback;
        }
    }

    public Optional<MediaAsset> loadCatalogMedia(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }
        return safeCall(
            () -> defaultWebClient.get()
                .uri(baseUrl + "/api/catalog/media/{filename}", filename)
                .exchangeToMono(response -> {
                    if (!response.statusCode().is2xxSuccessful()) {
                        return Mono.empty();
                    }
                    MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
                    long contentLength = response.headers().contentLength().orElse(-1L);
                    return response.bodyToMono(byte[].class)
                        .map(bytes -> new MediaAsset(bytes, contentType, contentLength));
                })
                .blockOptional(),
            "/api/catalog/media/" + filename,
            Optional.empty()
        );
    }

    private String toPortalMediaPath(String raw) {
        String marker = "/api/catalog/media/";
        int markerIndex = raw.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }

        String filename = raw.substring(markerIndex + marker.length());
        int queryStart = filename.indexOf('?');
        if (queryStart >= 0) {
            filename = filename.substring(0, queryStart);
        }
        if (filename.isBlank()) {
            return null;
        }

        return "/catalogo/media/" + filename;
    }

    public record MediaAsset(byte[] content, MediaType contentType, long contentLength) {}
}
