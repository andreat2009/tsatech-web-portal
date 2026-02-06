package com.newproject.web.controller;

import com.newproject.web.dto.Order;
import com.newproject.web.dto.Payment;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.Shipment;
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
        List<Order> orders = gatewayClient.listOrders(null);
        List<Payment> payments = gatewayClient.listPayments(null);
        List<Shipment> shipments = gatewayClient.listShipments(null);
        model.addAttribute("productCount", products.size());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("paymentCount", payments.size());
        model.addAttribute("shipmentCount", shipments.size());
        return "admin/dashboard";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        List<Order> orders = gatewayClient.listOrders(null);
        model.addAttribute("orders", orders);
        return "admin/orders";
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
}
