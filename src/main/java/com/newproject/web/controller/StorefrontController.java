package com.newproject.web.controller;

import com.newproject.web.dto.*;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StorefrontController {
    private final GatewayClient gatewayClient;
    private final CustomerResolver customerResolver;
    private final String currency;

    public StorefrontController(GatewayClient gatewayClient,
                                CustomerResolver customerResolver,
                                @Value("${app.currency}") String currency) {
        this.gatewayClient = gatewayClient;
        this.customerResolver = customerResolver;
        this.currency = currency;
    }

    @GetMapping({"/", "/shop"})
    public String home(Model model) {
        List<Product> products = gatewayClient.listProducts();
        products.sort(Comparator.comparing(Product::getId));
        model.addAttribute("products", products);
        return "index";
    }

    @GetMapping("/shop/products/{id}")
    public String product(@PathVariable Long id, Model model) {
        Product product = gatewayClient.getProduct(id);
        model.addAttribute("product", product);
        return "shop/product";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/";
        }

        Cart cart = resolveOrCreateCart(customerId);
        Product product = gatewayClient.getProduct(productId);

        CartItemRequest request = new CartItemRequest();
        request.setProductId(productId);
        request.setQuantity(quantity != null && quantity > 0 ? quantity : 1);
        request.setUnitPrice(product.getPrice());
        gatewayClient.addCartItem(cart.getId(), request);

        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String viewCart(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            model.addAttribute("items", List.of());
            model.addAttribute("total", BigDecimal.ZERO);
            return "cart/view";
        }

        Optional<Cart> cartOpt = resolveCart(customerId);
        if (cartOpt.isEmpty()) {
            model.addAttribute("items", List.of());
            model.addAttribute("total", BigDecimal.ZERO);
            return "cart/view";
        }

        Cart cart = cartOpt.get();
        List<CartItem> cartItems = gatewayClient.listCartItems(cart.getId());
        List<CartItemView> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            Product product = gatewayClient.getProduct(item.getProductId());
            CartItemView view = new CartItemView();
            view.setId(item.getId());
            view.setProductId(item.getProductId());
            view.setProductName(product.getName());
            view.setQuantity(item.getQuantity());
            view.setUnitPrice(item.getUnitPrice());
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            view.setLineTotal(lineTotal);
            total = total.add(lineTotal);
            items.add(view);
        }

        model.addAttribute("items", items);
        model.addAttribute("total", total);
        return "cart/view";
    }

    @PostMapping("/cart/items/{id}/delete")
    public String deleteCartItem(@PathVariable Long id) {
        gatewayClient.deleteCartItem(id);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/";
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

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setCustomerId(customerId);
        orderRequest.setCurrency(currency);
        orderRequest.setTotal(total);
        orderRequest.setStatus("NEW");
        Order order = gatewayClient.createOrder(orderRequest);

        for (CartItem item : cartItems) {
            Product product = gatewayClient.getProduct(item.getProductId());
            OrderItemRequest request = new OrderItemRequest();
            request.setProductId(item.getProductId());
            request.setSku(product.getSku());
            request.setName(product.getName());
            request.setQuantity(item.getQuantity());
            request.setUnitPrice(item.getUnitPrice());
            gatewayClient.addOrderItem(order.getId(), request);
            gatewayClient.deleteCartItem(item.getId());
        }

        return "redirect:/account/orders";
    }

    @GetMapping("/account/orders")
    public String accountOrders(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        List<Order> orders = gatewayClient.listOrders(customerId);
        model.addAttribute("orders", orders);
        return "account/orders";
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
}
