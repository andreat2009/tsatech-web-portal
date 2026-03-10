package com.newproject.web.controller;

import com.newproject.web.dto.LocalizedContent;
import com.newproject.web.dto.Manufacturer;
import com.newproject.web.dto.ManufacturerRequest;
import com.newproject.web.i18n.LanguageSupport;
import com.newproject.web.service.GatewayClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/admin/manufacturers", "/admin/catalogo/produttori"})
public class AdminManufacturerController {
    private final GatewayClient gatewayClient;

    public AdminManufacturerController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("manufacturers", gatewayClient.listManufacturers());
        return "admin/manufacturers";
    }

    @GetMapping({"/new", "/nuovo"})
    public String createForm(Model model) {
        ManufacturerRequest form = new ManufacturerRequest();
        form.setActive(true);
        form.setTranslations(ensureTranslations(null, null));
        model.addAttribute("manufacturer", form);
        model.addAttribute("formTitle", "Nuovo produttore");
        model.addAttribute("formAction", "/admin/catalogo/produttori");
        return "admin/manufacturer-form";
    }

    @PostMapping
    public String create(@ModelAttribute ManufacturerRequest request) {
        normalize(request);
        gatewayClient.createManufacturer(request);
        return "redirect:/admin/catalogo/produttori";
    }

    @GetMapping({"/{id}/edit", "/{id}/modifica"})
    public String editForm(@PathVariable Long id, Model model) {
        Manufacturer manufacturer = gatewayClient.getManufacturerSafe(id).orElse(null);
        if (manufacturer == null) {
            return "redirect:/admin/catalogo/produttori";
        }

        ManufacturerRequest form = new ManufacturerRequest();
        form.setName(manufacturer.getName());
        form.setImage(manufacturer.getImage());
        form.setActive(manufacturer.getActive());
        form.setTranslations(ensureTranslations(manufacturer.getTranslations(), manufacturer));

        model.addAttribute("manufacturer", form);
        model.addAttribute("formTitle", "Modifica produttore");
        model.addAttribute("formAction", "/admin/catalogo/produttori/" + id);
        return "admin/manufacturer-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute ManufacturerRequest request) {
        normalize(request);
        gatewayClient.updateManufacturer(id, request);
        return "redirect:/admin/catalogo/produttori";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gatewayClient.deleteManufacturer(id);
        return "redirect:/admin/catalogo/produttori";
    }

    private void normalize(ManufacturerRequest request) {
        if (request.getActive() == null) {
            request.setActive(true);
        }
        request.setTranslations(ensureTranslations(request.getTranslations(), null));

        LocalizedContent italian = request.getTranslations().get(LanguageSupport.DEFAULT_LANGUAGE);
        request.setName(firstNonBlank(
            italian != null ? italian.getName() : null,
            request.getName(),
            "Manufacturer"
        ));
    }

    private Map<String, LocalizedContent> ensureTranslations(Map<String, LocalizedContent> input, Manufacturer source) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();
        String fallbackName = source != null ? source.getName() : null;

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
