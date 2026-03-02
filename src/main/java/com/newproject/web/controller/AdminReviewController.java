package com.newproject.web.controller;

import com.newproject.web.dto.ProductReview;
import com.newproject.web.service.GatewayClient;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {
    private final GatewayClient gatewayClient;

    public AdminReviewController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Boolean approved, Model model) {
        List<ProductReview> reviews = gatewayClient.listReviews(approved);
        model.addAttribute("reviews", reviews);
        model.addAttribute("filterApproved", approved);
        return "admin/reviews";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean approved) {
        gatewayClient.setReviewApproval(id, approved);
        return "redirect:/admin/reviews";
    }
}
