package com.newproject.web.controller;

import com.newproject.web.dto.Coupon;
import com.newproject.web.dto.CouponAutoTranslateRequest;
import com.newproject.web.dto.CouponAutoTranslateResponse;
import com.newproject.web.dto.CouponRequest;
import com.newproject.web.dto.LocalizedContent;
import com.newproject.web.i18n.LanguageSupport;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping({"/admin/coupons", "/admin/marketing/coupon"})
public class AdminCouponController {
    private static final Logger logger = LoggerFactory.getLogger(AdminCouponController.class);

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
        coupon.setTranslations(ensureCouponTranslations(null, null));
        model.addAttribute("coupon", coupon);
        model.addAttribute("formTitleKey", "admin.coupon.title.new");
        model.addAttribute("formAction", "/admin/marketing/coupon");
        model.addAttribute("translationSourceLanguage", LanguageSupport.DEFAULT_LANGUAGE);
        model.addAttribute("autoTranslateDefault", true);
        model.addAttribute("overwriteTranslationsDefault", true);
        return "admin/coupon-form";
    }

    @PostMapping
    public String create(
        @ModelAttribute CouponRequest request,
        @RequestParam(name = "translationSourceLanguage", required = false) String translationSourceLanguage,
        @RequestParam(name = "autoTranslate", defaultValue = "false") boolean autoTranslate,
        @RequestParam(name = "overwriteTranslations", defaultValue = "false") boolean overwriteTranslations
    ) {
        normalize(request);
        applyAutoTranslationsIfRequested(request, translationSourceLanguage, autoTranslate, overwriteTranslations);
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
        request.setTranslations(ensureCouponTranslations(coupon.getTranslations(), coupon));

        model.addAttribute("coupon", request);
        model.addAttribute("formTitleKey", "admin.coupon.title.edit");
        model.addAttribute("formAction", "/admin/marketing/coupon/" + id);
        model.addAttribute("translationSourceLanguage", LanguageSupport.DEFAULT_LANGUAGE);
        model.addAttribute("autoTranslateDefault", true);
        model.addAttribute("overwriteTranslationsDefault", true);
        return "admin/coupon-form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @ModelAttribute CouponRequest request,
        @RequestParam(name = "translationSourceLanguage", required = false) String translationSourceLanguage,
        @RequestParam(name = "autoTranslate", defaultValue = "false") boolean autoTranslate,
        @RequestParam(name = "overwriteTranslations", defaultValue = "false") boolean overwriteTranslations
    ) {
        normalize(request);
        applyAutoTranslationsIfRequested(request, translationSourceLanguage, autoTranslate, overwriteTranslations);
        gatewayClient.updateCoupon(id, request);
        return "redirect:/admin/marketing/coupon";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gatewayClient.deleteCoupon(id);
        return "redirect:/admin/marketing/coupon";
    }

    private void applyAutoTranslationsIfRequested(
        CouponRequest request,
        String translationSourceLanguage,
        boolean autoTranslate,
        boolean overwriteTranslations
    ) {
        if (!autoTranslate) {
            return;
        }

        try {
            String sourceLanguage = LanguageSupport.normalizeLanguage(translationSourceLanguage);
            if (sourceLanguage == null) {
                sourceLanguage = LanguageSupport.DEFAULT_LANGUAGE;
            }

            CouponAutoTranslateRequest translateRequest = new CouponAutoTranslateRequest();
            translateRequest.setSourceLanguage(sourceLanguage);
            translateRequest.setOverwriteExisting(overwriteTranslations);
            translateRequest.setTranslations(request.getTranslations());

            CouponAutoTranslateResponse translateResponse = gatewayClient.autoTranslateCoupon(translateRequest);
            if (translateResponse == null || translateResponse.getTranslations() == null || translateResponse.getTranslations().isEmpty()) {
                return;
            }

            request.setTranslations(ensureCouponTranslations(translateResponse.getTranslations(), null));
            LocalizedContent italian = request.getTranslations().get(LanguageSupport.DEFAULT_LANGUAGE);
            request.setName(firstNonBlank(
                italian != null ? italian.getName() : null,
                request.getName(),
                request.getCode()
            ));
        } catch (Exception ex) {
            logger.warn("Coupon auto-translation failed: {}", ex.getMessage());
        }
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

        request.setTranslations(ensureCouponTranslations(request.getTranslations(), null));
        LocalizedContent italian = request.getTranslations().get(LanguageSupport.DEFAULT_LANGUAGE);
        request.setName(firstNonBlank(
            italian != null ? italian.getName() : null,
            request.getName(),
            request.getCode()
        ));
    }

    private Map<String, LocalizedContent> ensureCouponTranslations(Map<String, LocalizedContent> input, Coupon sourceCoupon) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();
        String fallbackName = sourceCoupon != null ? sourceCoupon.getName() : null;

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent src = input != null ? input.get(language) : null;
            LocalizedContent content = new LocalizedContent();
            content.setName(firstNonBlank(src != null ? src.getName() : null, fallbackName, ""));
            normalized.put(language, content);
        }

        return normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
