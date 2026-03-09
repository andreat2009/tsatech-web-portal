package com.newproject.web.controller;

import com.newproject.web.dto.Coupon;
import com.newproject.web.dto.CouponRequest;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/admin/coupons", "/admin/marketing/coupon"})
public class AdminCouponController {
    private final GatewayClient gatewayClient;

    public AdminCouponController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(Model model) {
        List<Coupon> coupons = gatewayClient.listCoupons();
        model.addAttribute("coupons", coupons);
        return "admin/coupons";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        CouponRequest coupon = new CouponRequest();
        coupon.setDiscountType("PERCENT");
        coupon.setValue(new BigDecimal("10"));
        coupon.setMinTotal(BigDecimal.ZERO);
        coupon.setCurrency("EUR");
        coupon.setActive(true);
        model.addAttribute("coupon", coupon);
        model.addAttribute("formTitle", "Nuovo coupon");
        model.addAttribute("formAction", "/admin/marketing/coupon");
        return "admin/coupon-form";
    }

    @PostMapping
    public String create(@ModelAttribute CouponRequest request) {
        normalize(request);
        gatewayClient.createCoupon(request);
        return "redirect:/admin/marketing/coupon";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Coupon coupon = gatewayClient.listCoupons().stream()
            .filter(c -> id.equals(c.getId()))
            .findFirst()
            .orElse(null);

        if (coupon == null) {
            return "redirect:/admin/marketing/coupon";
        }

        CouponRequest request = new CouponRequest();
        request.setCode(coupon.getCode());
        request.setName(coupon.getName());
        request.setDiscountType(coupon.getDiscountType());
        request.setValue(coupon.getValue());
        request.setMinTotal(coupon.getMinTotal());
        request.setMaxDiscount(coupon.getMaxDiscount());
        request.setCurrency(coupon.getCurrency());
        request.setActive(coupon.getActive());
        request.setUsageLimit(coupon.getUsageLimit());

        model.addAttribute("coupon", request);
        model.addAttribute("formTitle", "Modifica coupon");
        model.addAttribute("formAction", "/admin/marketing/coupon/" + id);
        return "admin/coupon-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute CouponRequest request) {
        normalize(request);
        gatewayClient.updateCoupon(id, request);
        return "redirect:/admin/marketing/coupon";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gatewayClient.deleteCoupon(id);
        return "redirect:/admin/marketing/coupon";
    }

    private void normalize(CouponRequest request) {
        if (request.getDiscountType() == null || request.getDiscountType().isBlank()) {
            request.setDiscountType("PERCENT");
        }
        if (request.getValue() == null) {
            request.setValue(BigDecimal.ZERO);
        }
        if (request.getMinTotal() == null) {
            request.setMinTotal(BigDecimal.ZERO);
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            request.setCurrency("EUR");
        }
        if (request.getActive() == null) {
            request.setActive(true);
        }
    }
}
