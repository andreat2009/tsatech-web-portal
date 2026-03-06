package com.newproject.web.controller;

import com.newproject.web.dto.*;
import com.newproject.web.service.GatewayClient;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final GatewayClient gatewayClient;

    public AdminController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Product> products = gatewayClient.listProducts();
        List<Category> categories = gatewayClient.listCategories(null);
        List<Customer> customers = gatewayClient.listCustomers();
        List<Cart> carts = gatewayClient.listCarts(null);
        List<Order> orders = gatewayClient.listOrders(null);
        List<InventoryItem> inventoryItems = gatewayClient.listInventory();
        List<ProductPrice> prices = gatewayClient.listPrices();
        List<Coupon> coupons = gatewayClient.listCoupons();
        List<Payment> payments = gatewayClient.listPayments(null);
        List<Shipment> shipments = gatewayClient.listShipments(null);
        List<OrderReturn> returns = gatewayClient.listReturns(null, null);
        List<InformationPage> informationPages = gatewayClient.listInformationPages(null);
        List<BlogPost> blogPosts = gatewayClient.listBlogPosts(null);
        List<BlogComment> blogComments = gatewayClient.listBlogComments(null);
        List<ContactMessage> contactMessages = gatewayClient.listContactMessages(null);

        model.addAttribute("productCount", products.size());
        model.addAttribute("categoryCount", categories.size());
        model.addAttribute("customerCount", customers.size());
        model.addAttribute("cartCount", carts.size());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("inventoryCount", inventoryItems.size());
        model.addAttribute("priceCount", prices.size());
        model.addAttribute("couponCount", coupons.size());
        model.addAttribute("paymentCount", payments.size());
        model.addAttribute("shipmentCount", shipments.size());
        model.addAttribute("returnCount", returns.size());
        model.addAttribute("informationCount", informationPages.size());
        model.addAttribute("blogPostCount", blogPosts.size());
        model.addAttribute("blogCommentCount", blogComments.size());
        model.addAttribute("contactMessageCount", contactMessages.size());
        model.addAttribute("notificationStatus", gatewayClient.notificationPing());
        return "admin/dashboard";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        List<Order> orders = gatewayClient.listOrders(null);
        model.addAttribute("orders", orders);
        return "admin/orders";
    }

    @GetMapping("/returns")
    public String returns(Model model) {
        List<OrderReturn> returns = gatewayClient.listReturns(null, null);
        List<InformationPage> informationPages = gatewayClient.listInformationPages(null);
        List<BlogPost> blogPosts = gatewayClient.listBlogPosts(null);
        List<BlogComment> blogComments = gatewayClient.listBlogComments(null);
        List<ContactMessage> contactMessages = gatewayClient.listContactMessages(null);
        model.addAttribute("returns", returns);
        return "admin/returns";
    }

    @GetMapping("/payments")
    public String payments(Model model) {
        List<Payment> payments = gatewayClient.listPayments(null);
        model.addAttribute("payments", payments);
        return "admin/payments";
    }

    @GetMapping("/shipments")
    public String shipments(Model model) {
        List<Shipment> shipments = gatewayClient.listShipments(null);
        model.addAttribute("shipments", shipments);
        return "admin/shipments";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        List<Customer> customers = gatewayClient.listCustomers();
        model.addAttribute("customers", customers);
        return "admin/customers";
    }

    @GetMapping("/carts")
    public String carts(Model model) {
        List<Cart> carts = gatewayClient.listCarts(null);
        model.addAttribute("carts", carts);
        return "admin/carts";
    }

    @GetMapping("/inventory")
    public String inventory(Model model) {
        List<InventoryItem> inventoryItems = gatewayClient.listInventory();
        model.addAttribute("inventoryItems", inventoryItems);
        return "admin/inventory";
    }

    @GetMapping("/pricing")
    public String pricing(Model model) {
        List<ProductPrice> prices = gatewayClient.listPrices();
        model.addAttribute("prices", prices);
        return "admin/pricing";
    }
}
