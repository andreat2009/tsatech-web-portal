package com.newproject.web.controller;

import com.newproject.web.dto.*;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

    private static final String GUEST_CART_SESSION_KEY = "GUEST_CART_ITEMS";
    private static final String GUEST_CUSTOMER_ID_SESSION_KEY = "GUEST_CUSTOMER_ID";
    private static final String GUEST_CUSTOMER_EMAIL_SESSION_KEY = "GUEST_CUSTOMER_EMAIL";
    private static final String GUEST_ORDER_SUMMARY_SESSION_KEY = "GUEST_ORDER_SUMMARY";

    private final GatewayClient gatewayClient;
    private final CustomerResolver customerResolver;
    private final String currency;
    private final MessageSource messageSource;

    public StorefrontController(
        GatewayClient gatewayClient,
        CustomerResolver customerResolver,
        @Value("${app.currency}") String currency,
        MessageSource messageSource
    ) {
        this.gatewayClient = gatewayClient;
        this.customerResolver = customerResolver;
        this.currency = currency;
        this.messageSource = messageSource;
    }

    @GetMapping({"/", "/shop", "/catalogo"})
    public String home(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false, defaultValue = "name_asc") String sort,
        Model model,
        Authentication authentication,
        HttpSession session
    ) {
        List<Product> products = gatewayClient.listProducts(q, categoryId, true, null, null, sort);

        Long customerId = null;
        if (isAuthenticated(authentication)) {
            customerId = customerResolver.resolveCustomerId(authentication);
            if (customerId != null) {
                mergeGuestCartIntoCustomerIfPresent(customerId, session);
            }

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

    @GetMapping({
        "/shop/products/{id}",
        "/shop/prodotto/{id:\\d+}-{slug}",
        "/catalogo/prodotto/{id:\\d+}-{slug}",
        "/catalogo/prodotto/{id:\\d+}"
    })
    public String product(@PathVariable Long id, @PathVariable(required = false) String slug, Model model, Authentication authentication) {
        Optional<Product> productOpt = gatewayClient.getProductSafe(id);
        if (productOpt.isEmpty()) {
            return "redirect:/catalogo";
        }
        Product product = productOpt.get();
        if (slug == null || !product.getSeoSlug().equals(slug)) {
            return "redirect:" + product.getSeoPath();
        }
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

    @PostMapping({
        "/shop/products/{id}/reviews",
        "/shop/prodotto/{id:\\d+}-{slug}/recensioni",
        "/catalogo/prodotto/{id:\\d+}-{slug}/recensioni",
        "/catalogo/prodotto/{id:\\d+}/recensioni"
    })
    public String addReview(@PathVariable Long id, @PathVariable(required = false) String slug, @ModelAttribute ProductReviewRequest reviewForm, Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        if (reviewForm.getRating() == null || reviewForm.getRating() < 1 || reviewForm.getRating() > 5) {
            return "redirect:" + productDetailsPath(id);
        }

        if (reviewForm.getText() == null || reviewForm.getText().isBlank()) {
            return "redirect:" + productDetailsPath(id);
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
        return "redirect:" + productDetailsPath(id);
    }

    @PostMapping("/cart/add")
    public String addToCart(
        @RequestParam Long productId,
        @RequestParam(defaultValue = "1") Integer quantity,
        Authentication authentication,
        HttpSession session
    ) {
        Optional<Product> productOpt = gatewayClient.getProductSafe(productId);
        if (productOpt.isEmpty()) {
            return "redirect:/catalogo";
        }

        int normalizedQuantity = quantity != null && quantity > 0 ? quantity : 1;
        Product product = productOpt.get();

        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            addGuestCartItem(session, productId, normalizedQuantity);
            return "redirect:/carrello";
        }

        mergeGuestCartIntoCustomerIfPresent(customerId, session);

        Cart cart = resolveOrCreateCart(customerId);
        CartItemRequest request = new CartItemRequest();
        request.setProductId(productId);
        request.setQuantity(normalizedQuantity);
        request.setUnitPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
        gatewayClient.addCartItem(cart.getId(), request);

        return "redirect:/carrello";
    }

    @GetMapping({"/cart", "/carrello"})
    public String viewCart(Model model, Authentication authentication, HttpSession session) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            CartSummary summary = buildGuestCartSummary(session);
            applyCartModel(model, summary);
            model.addAttribute("guestCheckout", true);
            return "cart/view";
        }

        mergeGuestCartIntoCustomerIfPresent(customerId, session);

        Optional<Cart> cartOpt = resolveCart(customerId);
        if (cartOpt.isEmpty()) {
            applyEmptyCartModel(model);
            model.addAttribute("guestCheckout", false);
            return "cart/view";
        }

        CartSummary summary = buildCartSummary(cartOpt.get());
        applyCartModel(model, summary);
        model.addAttribute("guestCheckout", false);
        return "cart/view";
    }

    @PostMapping("/cart/items/{id}/quantity")
    public String updateCartItemQuantity(
        @PathVariable Long id,
        @RequestParam Integer quantity,
        Authentication authentication,
        HttpSession session
    ) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            if (quantity == null || quantity <= 0) {
                removeGuestCartItem(session, id);
            } else {
                updateGuestCartItemQuantity(session, id, quantity);
            }
            return "redirect:/carrello";
        }

        mergeGuestCartIntoCustomerIfPresent(customerId, session);

        if (quantity == null || quantity <= 0) {
            gatewayClient.deleteCartItem(id);
            return "redirect:/carrello";
        }

        gatewayClient.updateCartItemQuantity(id, quantity);
        return "redirect:/carrello";
    }

    @PostMapping("/cart/items/{id}/delete")
    public String deleteCartItem(@PathVariable Long id, Authentication authentication, HttpSession session) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            removeGuestCartItem(session, id);
            return "redirect:/carrello";
        }

        mergeGuestCartIntoCustomerIfPresent(customerId, session);
        gatewayClient.deleteCartItem(id);
        return "redirect:/carrello";
    }

    @GetMapping({"/checkout", "/checkout-rapido"})
    public String checkoutPage(
        @RequestParam(required = false) String error,
        Model model,
        Authentication authentication,
        HttpSession session
    ) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId != null) {
            mergeGuestCartIntoCustomerIfPresent(customerId, session);

            Optional<Cart> cartOpt = resolveCart(customerId);
            if (cartOpt.isEmpty()) {
                return "redirect:/carrello";
            }

            CartSummary summary = buildCartSummary(cartOpt.get());
            if (summary.items().isEmpty()) {
                return "redirect:/carrello";
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
            model.addAttribute("guestCheckout", false);
            model.addAttribute("checkoutError", "processing".equalsIgnoreCase(error)
                ? msg("checkout.error.services")
                : null);
            return "checkout/index";
        }

        CartSummary summary = buildGuestCartSummary(session);
        if (summary.items().isEmpty()) {
            return "redirect:/carrello";
        }

        CheckoutForm guestForm = guestCheckoutFormFromSession(session);
        PriceQuoteRequest initialQuoteRequest = new PriceQuoteRequest();
        initialQuoteRequest.setSubtotal(summary.subtotal());
        initialQuoteRequest.setShipping(shippingMethods().get(SHIPPING_FLAT));
        initialQuoteRequest.setCouponCode(guestForm.getCouponCode());
        PriceQuoteResponse initialQuote = gatewayClient.quote(initialQuoteRequest);

        model.addAttribute("items", summary.items());
        model.addAttribute("subtotal", summary.subtotal());
        model.addAttribute("shipping", initialQuote.getShipping());
        model.addAttribute("discount", initialQuote.getDiscount());
        model.addAttribute("total", initialQuote.getTotal());
        model.addAttribute("quoteMessage", initialQuote.getMessage());
        model.addAttribute("addresses", List.of());
        model.addAttribute("shippingMethods", shippingMethods());
        model.addAttribute("paymentMethods", paymentMethods());
        model.addAttribute("checkoutForm", guestForm);
        model.addAttribute("guestCheckout", true);
        model.addAttribute("checkoutError", "processing".equalsIgnoreCase(error)
            ? msg("checkout.error.data")
            : null);
        return "checkout/index";
    }

    @PostMapping("/checkout/confirm")
    public String checkoutConfirm(@ModelAttribute CheckoutForm checkoutForm, Authentication authentication, HttpSession session) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId != null) {
            mergeGuestCartIntoCustomerIfPresent(customerId, session);
            return checkoutConfirmAuthenticated(customerId, checkoutForm);
        }
        return checkoutConfirmGuest(checkoutForm, session);
    }

    private String checkoutConfirmAuthenticated(Long customerId, CheckoutForm checkoutForm) {
        Optional<Cart> cartOpt = resolveCart(customerId);
        if (cartOpt.isEmpty()) {
            return "redirect:/carrello";
        }

        Cart cart = cartOpt.get();
        List<CartItem> cartItems = gatewayClient.listCartItems(cart.getId());
        if (cartItems.isEmpty()) {
            return "redirect:/carrello";
        }

        List<Address> addresses = gatewayClient.listCustomerAddresses(customerId);
        boolean addressValid = checkoutForm.getAddressId() != null
            && addresses.stream().anyMatch(address -> checkoutForm.getAddressId().equals(address.getId()));
        if (!addressValid) {
            return "redirect:/checkout-rapido";
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
            paymentRequest.setProvider(quote.getAppliedCoupon() != null ? paymentMethod + ":" + quote.getAppliedCoupon() : paymentMethod);
            gatewayClient.createPayment(paymentRequest);

            ShipmentRequest shipmentRequest = new ShipmentRequest();
            shipmentRequest.setOrderId(order.getId());
            shipmentRequest.setCarrier(shippingMethod.startsWith("pickup") ? "PICKUP" : "FLAT_RATE");
            shipmentRequest.setTrackingNumber("INIT-" + order.getId() + "-" + OffsetDateTime.now().toEpochSecond());
            shipmentRequest.setStatus("CREATED");
            gatewayClient.createShipment(shipmentRequest);

            return "redirect:/checkout/confermato?orderId=" + order.getId();
        } catch (Exception ex) {
            logger.warn("Checkout flow failed for customer {}: {}", customerId, ex.getMessage());
            return "redirect:/checkout-rapido?error=processing";
        }
    }

    private String checkoutConfirmGuest(CheckoutForm checkoutForm, HttpSession session) {
        CartSummary summary = buildGuestCartSummary(session);
        if (summary.items().isEmpty()) {
            return "redirect:/carrello";
        }

        if (!isGuestCheckoutFormValid(checkoutForm)) {
            return "redirect:/checkout-rapido";
        }

        String shippingMethod = normalizeShippingMethod(checkoutForm.getShippingMethod());
        String paymentMethod = normalizePaymentMethod(checkoutForm.getPaymentMethod());

        BigDecimal shippingCost = shippingMethods().get(shippingMethod);
        PriceQuoteRequest quoteRequest = new PriceQuoteRequest();
        quoteRequest.setSubtotal(summary.subtotal());
        quoteRequest.setShipping(shippingCost);
        quoteRequest.setCouponCode(checkoutForm.getCouponCode());
        PriceQuoteResponse quote = gatewayClient.quote(quoteRequest);

        BigDecimal total = quote.getTotal() != null ? quote.getTotal() : summary.subtotal().add(shippingCost);

        try {
            Long guestCustomerId = ensureGuestCustomer(checkoutForm, session);
            createGuestAddress(guestCustomerId, checkoutForm);

            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setCustomerId(guestCustomerId);
            orderRequest.setCurrency(currency);
            orderRequest.setTotal(total);
            orderRequest.setStatus("PENDING_PAYMENT");
            Order order = gatewayClient.createOrder(orderRequest);

            for (CartItemView item : summary.items()) {
                Product product = gatewayClient.getProductSafe(item.getProductId()).orElse(null);
                OrderItemRequest request = new OrderItemRequest();
                request.setProductId(item.getProductId());
                request.setSku(product != null ? product.getSku() : "N/A");
                request.setName(product != null ? product.getName() : item.getProductName());
                request.setQuantity(item.getQuantity());
                request.setUnitPrice(item.getUnitPrice());
                gatewayClient.addOrderItem(order.getId(), request);
            }

            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setAmount(total);
            paymentRequest.setCurrency(currency);
            paymentRequest.setStatus("CREATED");
            paymentRequest.setProvider(quote.getAppliedCoupon() != null ? paymentMethod + ":" + quote.getAppliedCoupon() : paymentMethod);
            gatewayClient.createPayment(paymentRequest);

            ShipmentRequest shipmentRequest = new ShipmentRequest();
            shipmentRequest.setOrderId(order.getId());
            shipmentRequest.setCarrier(shippingMethod.startsWith("pickup") ? "PICKUP" : "FLAT_RATE");
            shipmentRequest.setTrackingNumber("INIT-" + order.getId() + "-" + OffsetDateTime.now().toEpochSecond());
            shipmentRequest.setStatus("CREATED");
            gatewayClient.createShipment(shipmentRequest);

            saveGuestOrderSummary(session, order, summary.items());
            clearGuestCart(session);
            return "redirect:/checkout/confermato?orderId=" + order.getId();
        } catch (Exception ex) {
            logger.warn("Guest checkout flow failed: {}", ex.getMessage());
            return "redirect:/checkout-rapido?error=processing";
        }
    }

    @GetMapping({"/checkout/success", "/checkout/confermato"})
    public String checkoutSuccess(@RequestParam(required = false) Long orderId, Model model, Authentication authentication, HttpSession session) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            GuestOrderSummary guestSummary = readGuestOrderSummary(session);
            if (guestSummary == null) {
                return "redirect:/catalogo";
            }
            if (orderId != null && !orderId.equals(guestSummary.orderId())) {
                return "redirect:/catalogo";
            }

            Order order = new Order();
            order.setId(guestSummary.orderId());
            order.setTotal(guestSummary.total());
            order.setStatus(guestSummary.status());
            order.setCurrency(currency);

            model.addAttribute("order", order);
            model.addAttribute("orderItems", toOrderItems(guestSummary.orderId(), guestSummary.items()));
            model.addAttribute("payments", List.of());
            model.addAttribute("shipments", List.of());
            model.addAttribute("guestCheckout", true);
            return "checkout/success";
        }

        if (orderId == null) {
            return "redirect:/account/ordini";
        }

        Optional<Order> orderOpt = gatewayClient.getOrderSafe(orderId)
            .filter(order -> customerId.equals(order.getCustomerId()));
        if (orderOpt.isEmpty()) {
            return "redirect:/account/ordini";
        }

        model.addAttribute("order", orderOpt.get());
        model.addAttribute("orderItems", gatewayClient.listOrderItems(orderId));
        model.addAttribute("payments", gatewayClient.listPayments(orderId));
        model.addAttribute("shipments", gatewayClient.listShipments(orderId));
        model.addAttribute("guestCheckout", false);
        return "checkout/success";
    }

    @GetMapping({"/account/orders", "/account/ordini"})
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

    @GetMapping({"/account/orders/{id}", "/account/ordini/{id}"})
    public String accountOrderDetail(@PathVariable Long id, Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        Optional<Order> orderOpt = gatewayClient.getOrderSafe(id)
            .filter(order -> customerId.equals(order.getCustomerId()));

        if (orderOpt.isEmpty()) {
            return "redirect:/account/ordini";
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

    @GetMapping({"/account/addresses", "/account/indirizzi"})
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

    @PostMapping({"/account/addresses", "/account/indirizzi"})
    public String createAddress(@ModelAttribute AddressRequest addressForm, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        if (addressForm.getLine1() == null || addressForm.getLine1().isBlank()
            || addressForm.getCity() == null || addressForm.getCity().isBlank()
            || addressForm.getCountry() == null || addressForm.getCountry().isBlank()
            || addressForm.getPostalCode() == null || addressForm.getPostalCode().isBlank()) {
            return "redirect:/account/indirizzi";
        }

        if (addressForm.getIsDefault() == null) {
            addressForm.setIsDefault(false);
        }

        gatewayClient.createCustomerAddress(customerId, addressForm);
        return "redirect:/account/indirizzi";
    }

    @GetMapping({"/account/returns", "/account/resi"})
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

    @PostMapping({"/account/returns", "/account/resi"})
    public String createReturn(@ModelAttribute OrderReturnRequest returnForm, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        if (returnForm.getOrderId() == null || returnForm.getReason() == null || returnForm.getReason().isBlank()) {
            return "redirect:/account/resi";
        }

        returnForm.setCustomerId(customerId);
        gatewayClient.createReturn(returnForm);
        return "redirect:/account/resi";
    }

    @GetMapping({"/account/wishlist", "/account/lista-desideri"})
    public String wishlist(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        List<WishlistEntry> items = buildWishlist(customerId);
        model.addAttribute("wishlistItems", items);
        return "account/wishlist";
    }

    @PostMapping({"/account/wishlist/add", "/account/lista-desideri/aggiungi"})
    public String addWishlist(@RequestParam Long productId, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        gatewayClient.addWishlistItem(customerId, productId);
        return "redirect:/account/lista-desideri";
    }

    @PostMapping({"/account/wishlist/{productId}/delete", "/account/lista-desideri/{productId}/elimina"})
    public String removeWishlist(@PathVariable Long productId, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        gatewayClient.removeWishlistItem(customerId, productId);
        return "redirect:/account/lista-desideri";
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

    private void mergeGuestCartIntoCustomerIfPresent(Long customerId, HttpSession session) {
        Map<Long, Integer> guestItems = getGuestCartItems(session);
        if (guestItems.isEmpty()) {
            return;
        }

        Cart cart = resolveOrCreateCart(customerId);
        for (Map.Entry<Long, Integer> entry : guestItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            if (productId == null || quantity == null || quantity <= 0) {
                continue;
            }

            Product product = gatewayClient.getProductSafe(productId).orElse(null);
            if (product == null) {
                continue;
            }

            CartItemRequest request = new CartItemRequest();
            request.setProductId(productId);
            request.setQuantity(quantity);
            request.setUnitPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
            gatewayClient.addCartItem(cart.getId(), request);
        }

        clearGuestCart(session);
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

    private CartSummary buildGuestCartSummary(HttpSession session) {
        return buildGuestCartSummary(getGuestCartItems(session));
    }

    private CartSummary buildGuestCartSummary(Map<Long, Integer> guestCart) {
        List<CartItemView> items = new LinkedList<>();

        for (Map.Entry<Long, Integer> entry : guestCart.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue() != null && entry.getValue() > 0 ? entry.getValue() : 1;
            Product product = gatewayClient.getProductSafe(productId).orElse(null);
            if (product == null) {
                continue;
            }

            BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            CartItemView item = new CartItemView();
            item.setId(productId);
            item.setProductId(productId);
            item.setProductName(product.getName() != null ? product.getName() : "Product #" + productId);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            items.add(item);
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

    private void applyCartModel(Model model, CartSummary summary) {
        model.addAttribute("items", summary.items());
        model.addAttribute("subtotal", summary.subtotal());
        model.addAttribute("shipping", summary.shipping());
        model.addAttribute("total", summary.total());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getGuestCartItems(HttpSession session) {
        Object raw = session.getAttribute(GUEST_CART_SESSION_KEY);
        if (raw instanceof Map<?, ?> map) {
            Map<Long, Integer> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Long productId = asLong(entry.getKey());
                Integer quantity = asInteger(entry.getValue());
                if (productId != null && quantity != null && quantity > 0) {
                    normalized.put(productId, quantity);
                }
            }
            session.setAttribute(GUEST_CART_SESSION_KEY, normalized);
            return normalized;
        }

        Map<Long, Integer> empty = new LinkedHashMap<>();
        session.setAttribute(GUEST_CART_SESSION_KEY, empty);
        return empty;
    }

    private void addGuestCartItem(HttpSession session, Long productId, int quantity) {
        Map<Long, Integer> guestCart = getGuestCartItems(session);
        guestCart.put(productId, guestCart.getOrDefault(productId, 0) + quantity);
        session.setAttribute(GUEST_CART_SESSION_KEY, guestCart);
    }

    private void updateGuestCartItemQuantity(HttpSession session, Long productId, int quantity) {
        Map<Long, Integer> guestCart = getGuestCartItems(session);
        if (quantity <= 0) {
            guestCart.remove(productId);
        } else {
            guestCart.put(productId, quantity);
        }
        session.setAttribute(GUEST_CART_SESSION_KEY, guestCart);
    }

    private void removeGuestCartItem(HttpSession session, Long productId) {
        Map<Long, Integer> guestCart = getGuestCartItems(session);
        guestCart.remove(productId);
        session.setAttribute(GUEST_CART_SESSION_KEY, guestCart);
    }

    private void clearGuestCart(HttpSession session) {
        session.removeAttribute(GUEST_CART_SESSION_KEY);
    }

    private CheckoutForm guestCheckoutFormFromSession(HttpSession session) {
        CheckoutForm form = new CheckoutForm();
        form.setShippingMethod(SHIPPING_FLAT);
        form.setPaymentMethod(PAYMENT_COD);
        form.setCouponCode("");
        String previousEmail = asString(session.getAttribute(GUEST_CUSTOMER_EMAIL_SESSION_KEY));
        if (previousEmail != null) {
            form.setGuestEmail(previousEmail);
        }
        return form;
    }

    private boolean isGuestCheckoutFormValid(CheckoutForm form) {
        return !isBlank(form.getGuestEmail())
            && !isBlank(form.getGuestFirstName())
            && !isBlank(form.getGuestLastName())
            && !isBlank(form.getGuestAddressLine1())
            && !isBlank(form.getGuestCity())
            && !isBlank(form.getGuestCountry())
            && !isBlank(form.getGuestPostalCode());
    }

    private Long ensureGuestCustomer(CheckoutForm checkoutForm, HttpSession session) {
        String requestedEmail = normalizeGuestEmail(checkoutForm.getGuestEmail());
        Long existingCustomerId = asLong(session.getAttribute(GUEST_CUSTOMER_ID_SESSION_KEY));
        String existingEmail = asString(session.getAttribute(GUEST_CUSTOMER_EMAIL_SESSION_KEY));
        if (existingCustomerId != null && existingEmail != null && requestedEmail.equalsIgnoreCase(existingEmail)) {
            return existingCustomerId;
        }

        CustomerRequest request = new CustomerRequest();
        request.setKeycloakUserId(null);
        request.setEmail(requestedEmail);
        request.setFirstName(checkoutForm.getGuestFirstName().trim());
        request.setLastName(checkoutForm.getGuestLastName().trim());
        request.setPhone(safeTrim(checkoutForm.getGuestPhone()));
        request.setNewsletter(false);
        request.setActive(true);

        Customer created = gatewayClient.createCustomer(request);
        if (created == null || created.getId() == null) {
            throw new IllegalStateException("Unable to create or resolve guest customer");
        }

        session.setAttribute(GUEST_CUSTOMER_ID_SESSION_KEY, created.getId());
        session.setAttribute(GUEST_CUSTOMER_EMAIL_SESSION_KEY, requestedEmail);
        return created.getId();
    }

    private void createGuestAddress(Long customerId, CheckoutForm checkoutForm) {
        AddressRequest addressRequest = new AddressRequest();
        addressRequest.setLine1(checkoutForm.getGuestAddressLine1().trim());
        addressRequest.setLine2(safeTrim(checkoutForm.getGuestAddressLine2()));
        addressRequest.setCity(checkoutForm.getGuestCity().trim());
        addressRequest.setRegion(safeTrim(checkoutForm.getGuestRegion()));
        addressRequest.setCountry(checkoutForm.getGuestCountry().trim());
        addressRequest.setPostalCode(checkoutForm.getGuestPostalCode().trim());
        addressRequest.setIsDefault(true);
        gatewayClient.createCustomerAddress(customerId, addressRequest);
    }

    private void saveGuestOrderSummary(HttpSession session, Order order, List<CartItemView> items) {
        List<CartItemView> copy = items.stream().map(item -> {
            CartItemView cloned = new CartItemView();
            cloned.setId(item.getId());
            cloned.setProductId(item.getProductId());
            cloned.setProductName(item.getProductName());
            cloned.setQuantity(item.getQuantity());
            cloned.setUnitPrice(item.getUnitPrice());
            cloned.setLineTotal(item.getLineTotal());
            return cloned;
        }).collect(Collectors.toList());

        GuestOrderSummary summary = new GuestOrderSummary(order.getId(), order.getTotal(), order.getStatus(), copy);
        session.setAttribute(GUEST_ORDER_SUMMARY_SESSION_KEY, summary);
    }

    private GuestOrderSummary readGuestOrderSummary(HttpSession session) {
        Object raw = session.getAttribute(GUEST_ORDER_SUMMARY_SESSION_KEY);
        return raw instanceof GuestOrderSummary summary ? summary : null;
    }

    private List<OrderItem> toOrderItems(Long orderId, List<CartItemView> items) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemView item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setProductId(item.getProductId());
            orderItem.setSku("SKU-" + item.getProductId());
            orderItem.setName(item.getProductName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    private String productDetailsPath(Long productId) {
        return gatewayClient.getProductSafe(productId)
            .map(Product::getSeoPath)
            .orElse("/catalogo");
    }

    private Map<String, String> sortOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("name_asc", msg("catalog.sort.name.asc"));
        options.put("name_desc", msg("catalog.sort.name.desc"));
        options.put("price_asc", msg("catalog.sort.price.asc"));
        options.put("price_desc", msg("catalog.sort.price.desc"));
        options.put("newest", msg("catalog.sort.newest"));
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
        methods.put(PAYMENT_COD, msg("checkout.payment.cod"));
        methods.put(PAYMENT_BANK, msg("checkout.payment.bank"));
        return methods;
    }

    private String normalizeShippingMethod(String method) {
        return shippingMethods().containsKey(method) ? method : SHIPPING_FLAT;
    }

    private String normalizePaymentMethod(String method) {
        return paymentMethods().containsKey(method) ? method : PAYMENT_COD;
    }

    private String msg(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    private String normalizeGuestEmail(String email) {
        String normalized = safeTrim(email);
        if (normalized == null || !normalized.contains("@")) {
            throw new IllegalArgumentException("Invalid guest email");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Long asLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value instanceof String text ? text : null;
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

    private record GuestOrderSummary(Long orderId, BigDecimal total, String status, List<CartItemView> items) {
    }
}
