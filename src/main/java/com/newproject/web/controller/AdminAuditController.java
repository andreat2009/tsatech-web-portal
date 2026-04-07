package com.newproject.web.controller;

import com.newproject.web.dto.AdminAuditEvent;
import com.newproject.web.dto.PagedResponse;
import com.newproject.web.service.GatewayClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping({"/admin/audit", "/amministrazione/audit"})
public class AdminAuditController {
    private static final int PAGE_SIZE = 25;

    private final GatewayClient gatewayClient;

    public AdminAuditController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
        int currentPage = Math.max(1, page);
        PagedResponse<AdminAuditEvent> auditPage = gatewayClient.listAdminAuditEvents(currentPage - 1, PAGE_SIZE);
        model.addAttribute("auditEvents", auditPage.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("hasPrevious", currentPage > 1);
        model.addAttribute("hasNext", currentPage < auditPage.getTotalPages());
        model.addAttribute("totalAuditEvents", auditPage.getTotalElements());
        return "admin/audit";
    }
}
