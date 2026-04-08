package com.newproject.web.controller;

import com.newproject.web.dto.InformationPage;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.i18n.LanguageSupport;
import com.newproject.web.service.GatewayClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {
    private final GatewayClient gatewayClient;
    private final MessageSource messageSource;

    public GlobalModelAttributes(GatewayClient gatewayClient, MessageSource messageSource) {
        this.gatewayClient = gatewayClient;
        this.messageSource = messageSource;
    }

    @ModelAttribute
    public void enrichModel(Model model, Locale locale, HttpServletRequest request) {
        String current = LanguageSupport.fromLocale(locale);
        List<Map<String, String>> options = LanguageSupport.SUPPORTED_LANGUAGES.stream()
            .map(code -> Map.of(
                "code", code,
                "label", LanguageSupport.label(code),
                "flag", LanguageSupport.flag(code)
            ))
            .toList();

        String currentRequestUri = request != null && request.getRequestURI() != null
            ? request.getRequestURI()
            : "/";
        boolean isAdminArea = currentRequestUri.startsWith("/admin") || currentRequestUri.startsWith("/amministrazione");

        PublicStoreSettings settings = gatewayClient.getPublicStoreSettings();
        if (settings == null) {
            settings = new PublicStoreSettings();
            settings.setSiteName("TSATech Store");
            settings.setLogoMaxHeightPx(96);
            settings.setSiteNameFontSizePx(28);
            settings.setSupportPhone("+39 800 000 000");
        }

        List<InformationPage> footerInformationPages;
        try {
            footerInformationPages = gatewayClient.listInformationPages(true);
        } catch (Exception ex) {
            footerInformationPages = Collections.emptyList();
        }

        model.addAttribute("languageOptions", options);
        model.addAttribute("currentLanguage", current);
        model.addAttribute("currentRequestUri", currentRequestUri);
        model.addAttribute("isAdminArea", isAdminArea);
        model.addAttribute("storeSettings", settings);
        model.addAttribute("footerInformationPages", footerInformationPages);
        if ("expired".equalsIgnoreCase(request.getParameter("csrf"))) {
            model.addAttribute(
                "globalAlertMessage",
                messageSource.getMessage(
                    "common.error.csrf.expired",
                    null,
                    "Your session security token expired. Please try again.",
                    locale
                )
            );
        }
    }
}
