package com.newproject.web.controller;

import com.newproject.web.dto.PaymentRefundRequest;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping({"/admin/payments", "/admin/pagamenti"})
public class AdminPaymentController {
    private final GatewayClient gatewayClient;

    public AdminPaymentController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @PostMapping("/{id}/reconcile")
    public String reconcile(@PathVariable Long id) {
        try {
            gatewayClient.reconcilePayment(id);
            return "redirect:/admin/payments?reconciled=1";
        } catch (Exception ex) {
            return "redirect:/admin/payments?error=reconcile";
        }
    }

    @PostMapping("/{id}/refund")
    public String refund(
        @PathVariable Long id,
        @RequestParam(name = "amount", required = false) BigDecimal amount,
        @RequestParam(name = "reason", required = false) String reason
    ) {
        try {
            PaymentRefundRequest request = new PaymentRefundRequest();
            request.setAmount(amount);
            request.setReason(reason);
            gatewayClient.refundPayment(id, request);
            return "redirect:/admin/payments?refunded=1";
        } catch (Exception ex) {
            return "redirect:/admin/payments?error=refund";
        }
    }
}
