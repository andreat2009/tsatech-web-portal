package com.newproject.web.controller;

import com.newproject.web.dto.Order;
import com.newproject.web.dto.Payment;
import com.newproject.web.dto.PaymentRefundRequest;
import com.newproject.web.dto.PaymentTransaction;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping({"/admin/payments", "/admin/pagamenti"})
public class AdminPaymentController {
    private static final List<String> STATUS_OPTIONS = List.of(
        "CREATED",
        "PENDING_OFFLINE",
        "REDIRECT_REQUIRED",
        "APPROVED",
        "AUTHORIZED",
        "CAPTURE_PENDING",
        "CAPTURED",
        "FAILED",
        "CANCELLED",
        "PARTIALLY_REFUNDED",
        "REFUNDED"
    );

    private final GatewayClient gatewayClient;

    public AdminPaymentController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(
        @RequestParam(name = "orderId", required = false) Long orderId,
        @RequestParam(name = "provider", required = false) String provider,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "failureOnly", defaultValue = "false") boolean failureOnly,
        @RequestParam(name = "reconciled", required = false) String reconciled,
        @RequestParam(name = "refunded", required = false) String refunded,
        @RequestParam(name = "error", required = false) String error,
        Model model
    ) {
        String normalizedProvider = trimToNull(provider);
        String normalizedStatus = trimToNull(status);
        String normalizedQuery = trimToNull(query);
        List<Payment> payments = gatewayClient.listPayments(
            orderId,
            normalizedStatus,
            normalizedProvider,
            failureOnly ? Boolean.TRUE : null,
            normalizedQuery
        );

        model.addAttribute("payments", payments);
        model.addAttribute("providerOptions", providerOptions(payments));
        model.addAttribute("statusOptions", STATUS_OPTIONS);
        model.addAttribute("orderIdFilter", orderId);
        model.addAttribute("providerFilter", normalizedProvider);
        model.addAttribute("statusFilter", normalizedStatus);
        model.addAttribute("queryFilter", normalizedQuery);
        model.addAttribute("failureOnly", failureOnly);
        model.addAttribute("hasFilters", orderId != null || normalizedProvider != null || normalizedStatus != null || normalizedQuery != null || failureOnly);
        model.addAttribute("returnTo", buildListReturnTo(orderId, normalizedProvider, normalizedStatus, normalizedQuery, failureOnly));
        model.addAttribute("totalCount", payments.size());
        model.addAttribute("failureCount", payments.stream().filter(this::hasFailure).count());
        model.addAttribute("attentionCount", payments.stream().filter(this::requiresAttention).count());
        model.addAttribute("capturedAmount", sumAmounts(payments, false));
        model.addAttribute("refundedAmount", sumAmounts(payments, true));
        model.addAttribute("reconciled", reconciled != null);
        model.addAttribute("refunded", refunded != null);
        model.addAttribute("error", error);
        return "admin/payments";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @RequestParam(name = "orderId", required = false) Long orderId,
        @RequestParam(name = "provider", required = false) String provider,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "failureOnly", defaultValue = "false") boolean failureOnly
    ) {
        List<Payment> payments = gatewayClient.listPayments(orderId, trimToNull(status), trimToNull(provider), failureOnly ? Boolean.TRUE : null, trimToNull(query));
        String csv = buildCsv(payments);
        String filename = "payments-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}")
    public String detail(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        @RequestParam(name = "reconciled", required = false) String reconciled,
        @RequestParam(name = "refunded", required = false) String refunded,
        @RequestParam(name = "error", required = false) String error,
        Model model
    ) {
        Payment payment = gatewayClient.getPayment(id);
        List<PaymentTransaction> transactions = gatewayClient.listPaymentTransactions(id);
        Optional<Order> order = payment.getOrderId() != null ? gatewayClient.getOrderSafe(payment.getOrderId()) : Optional.empty();
        String backTo = sanitizeReturnTo(returnTo);

        model.addAttribute("payment", payment);
        model.addAttribute("transactions", transactions);
        model.addAttribute("linkedOrder", order.orElse(null));
        model.addAttribute("backTo", backTo);
        model.addAttribute("actionReturnTo", buildDetailReturnTo(id, backTo));
        model.addAttribute("hasFailure", hasFailure(payment));
        model.addAttribute("requiresAttention", requiresAttention(payment));
        model.addAttribute("reconciled", reconciled != null);
        model.addAttribute("refunded", refunded != null);
        model.addAttribute("error", error);
        return "admin/payment-detail";
    }

    @PostMapping("/{id}/reconcile")
    public String reconcile(@PathVariable Long id, @RequestParam(name = "returnTo", required = false) String returnTo) {
        try {
            gatewayClient.reconcilePayment(id);
            return redirectWithFlag(returnTo, "reconciled", "1");
        } catch (Exception ex) {
            return redirectWithFlag(returnTo, "error", "reconcile");
        }
    }

    @PostMapping("/{id}/refund")
    public String refund(
        @PathVariable Long id,
        @RequestParam(name = "amount", required = false) BigDecimal amount,
        @RequestParam(name = "reason", required = false) String reason,
        @RequestParam(name = "returnTo", required = false) String returnTo
    ) {
        try {
            PaymentRefundRequest request = new PaymentRefundRequest();
            request.setAmount(amount);
            request.setReason(reason);
            gatewayClient.refundPayment(id, request);
            return redirectWithFlag(returnTo, "refunded", "1");
        } catch (Exception ex) {
            return redirectWithFlag(returnTo, "error", "refund");
        }
    }

    private List<String> providerOptions(List<Payment> payments) {
        LinkedHashSet<String> providers = new LinkedHashSet<>();
        providers.add("OFFLINE");
        providers.add("PAYPAL");
        providers.add("FABRICK");
        payments.stream()
            .map(Payment::getProvider)
            .map(this::trimToNull)
            .filter(value -> value != null)
            .map(value -> value.toUpperCase(Locale.ROOT))
            .forEach(providers::add);
        return List.copyOf(providers);
    }

    private BigDecimal sumAmounts(List<Payment> payments, boolean refunded) {
        return payments.stream()
            .map(payment -> refunded ? payment.getRefundedAmount() : payment.getAmount())
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasFailure(Payment payment) {
        if (payment == null) {
            return false;
        }
        String status = trimToNull(payment.getStatus());
        return trimToNull(payment.getFailureCode()) != null
            || trimToNull(payment.getFailureReason()) != null
            || "FAILED".equalsIgnoreCase(status)
            || "CANCELLED".equalsIgnoreCase(status);
    }

    private boolean requiresAttention(Payment payment) {
        if (payment == null) {
            return false;
        }
        String status = trimToNull(payment.getStatus());
        if (status == null) {
            return false;
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "REDIRECT_REQUIRED", "APPROVED", "AUTHORIZED", "CAPTURE_PENDING", "PENDING_OFFLINE", "PARTIALLY_REFUNDED" -> true;
            default -> hasFailure(payment);
        };
    }

    private String buildListReturnTo(Long orderId, String provider, String status, String query, boolean failureOnly) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/payments");
        if (orderId != null) {
            builder.queryParam("orderId", orderId);
        }
        if (provider != null) {
            builder.queryParam("provider", provider);
        }
        if (status != null) {
            builder.queryParam("status", status);
        }
        if (query != null) {
            builder.queryParam("query", query);
        }
        if (failureOnly) {
            builder.queryParam("failureOnly", true);
        }
        return builder.build().toUriString();
    }

    private String buildDetailReturnTo(Long paymentId, String backTo) {
        return UriComponentsBuilder.fromPath("/admin/payments/{id}")
            .queryParam("returnTo", sanitizeReturnTo(backTo))
            .buildAndExpand(paymentId)
            .toUriString();
    }

    private String sanitizeReturnTo(String returnTo) {
        String normalized = trimToNull(returnTo);
        if (normalized == null) {
            return "/admin/payments";
        }
        if (normalized.startsWith("/admin/payments") || normalized.startsWith("/admin/pagamenti")) {
            return normalized;
        }
        return "/admin/payments";
    }

    private String redirectWithFlag(String returnTo, String key, String value) {
        String target = sanitizeReturnTo(returnTo);
        String separator = target.contains("?") ? "&" : "?";
        return "redirect:" + target + separator + key + "=" + value;
    }

    private String buildCsv(List<Payment> payments) {
        StringBuilder builder = new StringBuilder();
        builder.append("payment_id,order_id,provider,method_code,status,provider_status,amount,refunded_amount,currency,provider_order_id,provider_payment_id,failure_code,failure_reason,created_at,updated_at,last_provider_sync_at,last_webhook_at,last_reconciled_at\n");
        for (Payment payment : payments) {
            builder.append(csv(payment.getId()))
                .append(',').append(csv(payment.getOrderId()))
                .append(',').append(csv(payment.getProvider()))
                .append(',').append(csv(payment.getMethodCode()))
                .append(',').append(csv(payment.getStatus()))
                .append(',').append(csv(payment.getProviderStatus()))
                .append(',').append(csv(payment.getAmount()))
                .append(',').append(csv(payment.getRefundedAmount()))
                .append(',').append(csv(payment.getCurrency()))
                .append(',').append(csv(payment.getProviderOrderId()))
                .append(',').append(csv(payment.getProviderPaymentId()))
                .append(',').append(csv(payment.getFailureCode()))
                .append(',').append(csv(payment.getFailureReason()))
                .append(',').append(csv(payment.getCreatedAt()))
                .append(',').append(csv(payment.getUpdatedAt()))
                .append(',').append(csv(payment.getLastProviderSyncAt()))
                .append(',').append(csv(payment.getLastWebhookAt()))
                .append(',').append(csv(payment.getLastReconciledAt()))
                .append('\n');
        }
        return builder.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + raw + "\"";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
