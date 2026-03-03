package com.newproject.web.controller;

import com.newproject.web.dto.*;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StorefrontController {
    private static final Logger logger = LoggerFactory.getLogger(StorefrontController.class);
    private static final String SHIPPING_FLAT = "flat.flat";
    private static final String SHIPPING_PICKUP = "pickup.pickup";

    private static final String PAYMENT_COD = "cod";
    private static final String PAYMENT_BANK = "bank_transfer";

    private final GatewayClient gatewayClient;
    private final CustomerResolver customerResolver;
    private final String currency;

    public StorefrontController(
        GatewayClient gatewayClient,
        CustomerResolver customerResolver,
        @Value("${app.currency}") String currency
    ) {
        this.gatewayClient = gatewayClient;
        this.customerResolver = customerResolver;
        this.currency = currency;
    }

    @GetMapping({"/", "/shop"})
    public String home(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false, defaultValue = "name_asc") String sort,
        Model model,
        Authentication authentication
    ) {
        List<Product> products = gatewayClient.listProducts(q, categoryId, true, null, null, sort);

        Long customerId = null;
        if (isAuthenticated(authentication)) {
            customerId = customerResolver.resolveCustomerId(authentication);

            Map<Long, ProductPrice> pricesByProductId = gatewayClient.listPrices().stream()
                .filter(price -> price.getProductId() != null)
                .collect(Collectors.toMap(ProductPrice::getProductId, Function.identity(), (first, ignored) -> first));

            Map<Long, InventoryItem> inventoryByProductId = gatewayClient.listInventory().stream()
                .filter(item -> item.getProductId() != null)
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity(), (first, ignored) -> first));

            for (Product product : products) {
                ProductPrice price = pricesByProductId.get(product.getId());
                if (price != null && Boolean.TRUE.equals(price.getActive()) && price.getAmount() != null) {
                    product.setPrice(price.getAmount());
                }

                InventoryItem inventory = inventoryByProductId.get(product.getId());
                if (inventory != null && inventory.getOnHand() != null && inventory.getReserved() != null) {
                    product.setQuantity(Math.max(0, inventory.getOnHand() - inventory.getReserved()));
                }
            }
        }

        Set<Long> wishlistProductIds = customerId == null
            ? Set.of()
            : gatewayClient.listWishlist(customerId).stream().map(WishlistItem::getProductId).collect(Collectors.toSet());

        model.addAttribute("products", products);
        model.addAttribute("wishlistProductIds", wishlistProductIds);
        model.addAttribute("categoryTree", gatewayClient.listCategoryTree(true));
        model.addAttribute("searchQuery", q != null ? q : "");
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSort", sort != null ? sort : "name_asc");
        model.addAttribute("sortOptions", sortOptions());
        return "index";
    }

    @GetMapping("/shop/products/{id}")
    public String product(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Product> productOpt = gatewayClient.getProductSafe(id);
        if (productOpt.isEmpty()) {
            return "redirect:/shop";
        }
        Product product = productOpt.get();
        List<ProductReview> reviews = gatewayClient.listProductReviews(id).stream()
            .filter(review -> Boolean.TRUE.equals(review.getApproved()))
            .collect(Collectors.toList());

        double avgRating = reviews.isEmpty()
            ? 0d
            : reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0d);

        ProductReviewRequest reviewForm = new ProductReviewRequest();
        reviewForm.setRating(5);

        Long customerId = isAuthenticated(authentication) ? customerResolver.resolveCustomerId(authentication) : null;
        Set<Long> wishlistProductIds = customerId == null
            ? Set.of()
            : gatewayClient.listWishlist(customerId).stream().map(WishlistItem::getProductId).collect(Collectors.toSet());

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewForm", reviewForm);
        model.addAttribute("wishlistProductIds", wishlistProductIds);
        model.addAttribute("categoryTree", gatewayClient.listCategoryTree(true));
        return "shop/product";
    }

    @PostMapping("/shop/products/{id}/reviews")
    public String addReview(@PathVariable Long id, @ModelAttribute ProductReviewRequest reviewForm, Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        if (reviewForm.getRating() == null || reviewForm.getRating() < 1 || reviewForm.getRating() > 5) {
            return "redirect:/shop/products/" + id;
        }

        if (reviewForm.getText() == null || reviewForm.getText().isBlank()) {
            return "redirect:/shop/products/" + id;
        }

        OidcUser user = authentication.getPrincipal() instanceof OidcUser oidc ? oidc : null;
        if (user != null) {
            if (reviewForm.getAuthorName() == null || reviewForm.getAuthorName().isBlank()) {
                String fallbackName = user.getGivenName() != null
                    ? user.getGivenName() + (user.getFamilyName() != null ? " " + user.getFamilyName() : "")
                    : user.getPreferredUsername();
                reviewForm.setAuthorName(fallbackName != null ? fallbackName : "User");
            }
            if (reviewForm.getAuthorEmail() == null || reviewForm.getAuthorEmail().isBlank()) {
                reviewForm.setAuthorEmail(user.getEmail());
            }
        }

        gatewayClient.addProductReview(id, reviewForm);
        return "redirect:/shop/products/" + id;
    }

    @PostMapping("/cart/add")
    public String addToCart(
        @RequestParam Long productId,
        @RequestParam(defaultValue = "1") Integer quantity,
        Authentication authentication
    ) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        Optional<Product> productOpt = gatewayClient.getProductSafe(productId);
        if (productOpt.isEmpty()) {
            return "redirect:/shop";
        }

        Cart cart = resolveOrCreateCart(customerId);
        Product product = productOpt.get();

        CartItemRequest request = new CartItemRequest();
        request.setProductId(productId);
        request.setQuantity(quantity != null && quantity > 0 ? quantity : 1);
        request.setUnitPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
        gatewayClient.addCartItem(cart.getId(), request);

        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String viewCart(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            applyEmptyCartModel(model);
            return "cart/view";
        }

        Optional<Cart> cartOpt = resolveCart(customerId);
        if (cartOpt.isEmpty()) {
            applyEmptyCartModel(model);
            return "cart/view";
        }

        CartSummary summary = buildCartSummary(cartOpt.get());
        model.addAttribute("items", summary.items());
        model.addAttribute("subtotal", summary.subtotal());
        model.addAttribute("shipping", summary.shipping());
        model.addAttribute("total", summary.total());
        return "cart/view";
    }

    @PostMapping("/cart/items/{id}/quantity")
    public String updateCartItemQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        if (quantity == null || quantity <= 0) {
            gatewayClient.deleteCartItem(id);
            return "redirect:/cart";
        }

        gatewayClient.updateCartItemQuantity(id, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{id}/delete")
    public String deleteCartItem(@PathVariable Long id) {
        gatewayClient.deleteCartItem(id);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(
        @RequestParam(required = false) String error,
        Model model,
        Authentication authentication
    ) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        Optional<Cart> cartOpt = resolveCart(customerId);
        if (cartOpt.isEmpty()) {
            return "redirect:/cart";
        }

        CartSummary summary = buildCartSummary(cartOpt.get());
        if (summary.items().isEmpty()) {
            return "redirect:/cart";
        }

        List<Address> addresses = gatewayClient.listCustomerAddresses(customerId);

        CheckoutForm checkoutForm = new CheckoutForm();
        checkoutForm.setShippingMethod(SHIPPING_FLAT);
        checkoutForm.setPaymentMethod(PAYMENT_COD);
        checkoutForm.setCouponCode("");
        if (!addresses.isEmpty()) {
            checkoutForm.setAddressId(addresses.stream()
                .filter(address -> Boolean.TRUE.equals(address.getIsDefault()))
                .map(Address::getId)
                .findFirst()
                .orElse(addresses.get(0).getId()));
        }

        PriceQuoteRequest initialQuoteRequest = new PriceQuoteRequest();
        initialQuoteRequest.setSubtotal(summary.subtotal());
        initialQuoteRequest.setShipping(shippingMethods().get(SHIPPING_FLAT));
        initialQuoteRequest.setCouponCode(checkoutForm.getCouponCode());
        PriceQuoteResponse initialQuote = gatewayClient.quote(initialQuoteRequest);

        model.addAttribute("items", summary.items());
        model.addAttribute("subtotal", summary.subtotal());
        model.addAttribute("shipping", initialQuote.getShipping());
        model.addAttribute("discount", initialQuote.getDiscount());
        model.addAttribute("total", initialQuote.getTotal());
        model.addAttribute("quoteMessage", initialQuote.getMessage());
        model.addAttribute("addresses", addresses);
        model.addAttribute("shippingMethods", shippingMethods());
        model.addAttribute("paymentMethods", paymentMethods());
        model.addAttribute("checkoutForm", checkoutForm);
        model.addAttribute("checkoutError", "processing".equalsIgnoreCase(error)
            ? "Checkout non completato. Verifica i servizi e riprova."
            : null);
        return "checkout/index";
    }

    @PostMapping("/checkout/confirm")
    public String checkoutConfirm(@ModelAttribute CheckoutForm checkoutForm, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        Optional<Cart> cartOpt = resolveCart(customerId);
        if (cartOpt.isEmpty()) {
            return "redirect:/cart";
        }

        Cart cart = cartOpt.get();
        List<CartItem> cartItems = gatewayClient.listCartItems(cart.getId());
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        List<Address> addresses = gatewayClient.listCustomerAddresses(customerId);
        boolean addressValid = checkoutForm.getAddressId() != null
            && addresses.stream().anyMatch(address -> checkoutForm.getAddressId().equals(address.getId()));
        if (!addressValid) {
            return "redirect:/checkout";
        }

        String shippingMethod = normalizeShippingMethod(checkoutForm.getShippingMethod());
        String paymentMethod = normalizePaymentMethod(checkoutForm.getPaymentMethod());

        BigDecimal subtotal = calculateSubtotal(cartItems);
        BigDecimal shippingCost = shippingMethods().get(shippingMethod);

        PriceQuoteRequest quoteRequest = new PriceQuoteRequest();
        quoteRequest.setSubtotal(subtotal);
        quoteRequest.setShipping(shippingCost);
        quoteRequest.setCouponCode(checkoutForm.getCouponCode());
        PriceQuoteResponse quote = gatewayClient.quote(quoteRequest);

        BigDecimal total = quote.getTotal() != null ? quote.getTotal() : subtotal.add(shippingCost);

        try {
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setCustomerId(customerId);
            orderRequest.setCurrency(currency);
            orderRequest.setTotal(total);
            orderRequest.setStatus("PENDING_PAYMENT");
            Order order = gatewayClient.createOrder(orderRequest);

            for (CartItem item : cartItems) {
                Product product = gatewayClient.getProductSafe(item.getProductId()).orElse(null);
                OrderItemRequest request = new OrderItemRequest();
                request.setProductId(item.getProductId());
                request.setSku(product != null ? product.getSku() : "N/A");
                request.setName(product != null ? product.getName() : "Product #" + item.getProductId());
                request.setQuantity(item.getQuantity());
                request.setUnitPrice(item.getUnitPrice());
                gatewayClient.addOrderItem(order.getId(), request);
                gatewayClient.deleteCartItem(item.getId());
            }

            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setAmount(total);
            paymentRequest.setCurrency(currency);
            paymentRequest.setStatus("CREATED");
            paymentRequest.setProvider((quote.getAppliedCoupon() != null ? paymentMethod + ":" + quote.getAppliedCoupon() : paymentMethod));
            gatewayClient.createPayment(paymentRequest);

            ShipmentRequest shipmentRequest = new ShipmentRequest();
            shipmentRequest.setOrderId(order.getId());
            shipmentRequest.setCarrier(shippingMethod.startsWith("pickup") ? "PICKUP" : "FLAT_RATE");
            shipmentRequest.setTrackingNumber("INIT-" + order.getId() + "-" + OffsetDateTime.now().toEpochSecond());
            shipmentRequest.setStatus("CREATED");
            gatewayClient.createShipment(shipmentRequest);

            return "redirect:/checkout/success?orderId=" + order.getId();
        } catch (Exception ex) {
            logger.warn("Checkout flow failed for customer {}: {}", customerId, ex.getMessage());
            return "redirect:/checkout?error=processing";
        }
    }

    @GetMapping("/checkout/success")
    public String checkoutSuccess(@RequestParam(required = false) Long orderId, Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null || orderId == null) {
            return "redirect:/account/orders";
        }

        Optional<Order> orderOpt = gatewayClient.getOrderSafe(orderId)
            .filter(order -> customerId.equals(order.getCustomerId()));
        if (orderOpt.isEmpty()) {
            return "redirect:/account/orders";
        }

        model.addAttribute("order", orderOpt.get());
        model.addAttribute("orderItems", gatewayClient.listOrderItems(orderId));
        model.addAttribute("payments", gatewayClient.listPayments(orderId));
        model.addAttribute("shipments", gatewayClient.listShipments(orderId));
        return "checkout/success";
    }

    @GetMapping("/account/orders")
    public String accountOrders(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }
        List<Order> orders = gatewayClient.listOrders(customerId);
        orders.sort(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        model.addAttribute("orders", orders);
        return "account/orders";
    }

    @GetMapping("/account/orders/{id}")
    public String accountOrderDetail(@PathVariable Long id, Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        Optional<Order> orderOpt = gatewayClient.getOrderSafe(id)
            .filter(order -> customerId.equals(order.getCustomerId()));

        if (orderOpt.isEmpty()) {
            return "redirect:/account/orders";
        }

        OrderReturnRequest returnForm = new OrderReturnRequest();
        returnForm.setOrderId(id);

        model.addAttribute("order", orderOpt.get());
        model.addAttribute("items", gatewayClient.listOrderItems(id));
        model.addAttribute("payments", gatewayClient.listPayments(id));
        model.addAttribute("shipments", gatewayClient.listShipments(id));
        model.addAttribute("returnForm", returnForm);
        return "account/order-detail";
    }

    @GetMapping("/account/addresses")
    public String accountAddresses(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        List<Address> addresses = gatewayClient.listCustomerAddresses(customerId);
        AddressRequest addressForm = new AddressRequest();
        addressForm.setIsDefault(addresses.isEmpty());
        model.addAttribute("addresses", addresses);
        model.addAttribute("addressForm", addressForm);
        return "account/addresses";
    }

    @PostMapping("/account/addresses")
    public String createAddress(@ModelAttribute AddressRequest addressForm, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        if (addressForm.getLine1() == null || addressForm.getLine1().isBlank()
            || addressForm.getCity() == null || addressForm.getCity().isBlank()
            || addressForm.getCountry() == null || addressForm.getCountry().isBlank()
            || addressForm.getPostalCode() == null || addressForm.getPostalCode().isBlank()) {
            return "redirect:/account/addresses";
        }

        if (addressForm.getIsDefault() == null) {
            addressForm.setIsDefault(false);
        }

        gatewayClient.createCustomerAddress(customerId, addressForm);
        return "redirect:/account/addresses";
    }



    @GetMapping("/account/returns")
    public String accountReturns(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        model.addAttribute("returns", gatewayClient.listReturns(customerId, null));
        model.addAttribute("orders", gatewayClient.listOrders(customerId));
        model.addAttribute("returnForm", new OrderReturnRequest());
        return "account/returns";
    }

    @PostMapping("/account/returns")
    public String createReturn(@ModelAttribute OrderReturnRequest returnForm, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        if (returnForm.getOrderId() == null || returnForm.getReason() == null || returnForm.getReason().isBlank()) {
            return "redirect:/account/returns";
        }

        returnForm.setCustomerId(customerId);
        gatewayClient.createReturn(returnForm);
        return "redirect:/account/returns";
    }
    @GetMapping("/account/wishlist")
    public String wishlist(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        List<WishlistEntry> items = buildWishlist(customerId);
        model.addAttribute("wishlistItems", items);
        return "account/wishlist";
    }

    @PostMapping("/account/wishlist/add")
    public String addWishlist(@RequestParam Long productId, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        gatewayClient.addWishlistItem(customerId, productId);
        return "redirect:/account/wishlist";
    }

    @PostMapping("/account/wishlist/{productId}/delete")
    public String removeWishlist(@PathVariable Long productId, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        gatewayClient.removeWishlistItem(customerId, productId);
        return "redirect:/account/wishlist";
    }

    private List<WishlistEntry> buildWishlist(Long customerId) {
        return gatewayClient.listWishlist(customerId).stream()
            .map(item -> gatewayClient.getProductSafe(item.getProductId())
                .map(product -> new WishlistEntry(product, item.getCreatedAt())))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    private Optional<Cart> resolveCart(Long customerId) {
        List<Cart> carts = gatewayClient.listCarts(customerId);
        return carts.stream()
            .filter(cart -> cart.getStatus() == null || cart.getStatus().equalsIgnoreCase("OPEN"))
            .findFirst();
    }

    private Cart resolveOrCreateCart(Long customerId) {
        Optional<Cart> existing = resolveCart(customerId);
        if (existing.isPresent()) {
            return existing.get();
        }
        CartRequest request = new CartRequest();
        request.setCustomerId(customerId);
        request.setCurrency(currency);
        request.setStatus("OPEN");
        return gatewayClient.createCart(request);
    }

    private CartSummary buildCartSummary(Cart cart) {
        List<CartItem> cartItems = gatewayClient.listCartItems(cart.getId());
        List<CartItemView> items = new ArrayList<>();

        for (CartItem item : cartItems) {
            Product product = gatewayClient.getProductSafe(item.getProductId()).orElse(null);
            CartItemView view = new CartItemView();
            view.setId(item.getId());
            view.setProductId(item.getProductId());
            view.setProductName(product != null && product.getName() != null ? product.getName() : "Product #" + item.getProductId());
            int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            view.setQuantity(quantity);
            view.setUnitPrice(unitPrice);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            view.setLineTotal(lineTotal);
            items.add(view);
        }

        BigDecimal subtotal = items.stream()
            .map(CartItemView::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shipping = items.isEmpty() ? BigDecimal.ZERO : shippingMethods().get(SHIPPING_FLAT);
        BigDecimal total = subtotal.add(shipping);

        return new CartSummary(items, subtotal, shipping, total);
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
            .map(item -> {
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                return unitPrice.multiply(BigDecimal.valueOf(quantity));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyEmptyCartModel(Model model) {
        model.addAttribute("items", List.of());
        model.addAttribute("subtotal", BigDecimal.ZERO);
        model.addAttribute("shipping", BigDecimal.ZERO);
        model.addAttribute("total", BigDecimal.ZERO);
    }

    private Map<String, String> sortOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("name_asc", "Nome (A-Z)");
        options.put("name_desc", "Nome (Z-A)");
        options.put("price_asc", "Prezzo crescente");
        options.put("price_desc", "Prezzo decrescente");
        options.put("newest", "Novità");
        return options;
    }

    private Map<String, BigDecimal> shippingMethods() {
        Map<String, BigDecimal> methods = new LinkedHashMap<>();
        methods.put(SHIPPING_FLAT, new BigDecimal("8.00"));
        methods.put(SHIPPING_PICKUP, BigDecimal.ZERO);
        return methods;
    }

    private Map<String, String> paymentMethods() {
        Map<String, String> methods = new LinkedHashMap<>();
        methods.put(PAYMENT_COD, "Pagamento alla consegna");
        methods.put(PAYMENT_BANK, "Bonifico bancario");
        return methods;
    }

    private String normalizeShippingMethod(String method) {
        return shippingMethods().containsKey(method) ? method : SHIPPING_FLAT;
    }

    private String normalizePaymentMethod(String method) {
        return paymentMethods().containsKey(method) ? method : PAYMENT_COD;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private record CartSummary(List<CartItemView> items, BigDecimal subtotal, BigDecimal shipping, BigDecimal total) {
    }

    private record WishlistEntry(Product product, OffsetDateTime addedAt) {
    }
}
