package com.newproject.web.controller;

import com.newproject.web.i18n.LanguageSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {
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

        model.addAttribute("languageOptions", options);
        model.addAttribute("currentLanguage", current);
        model.addAttribute("currentRequestUri", currentRequestUri);
    }
}
