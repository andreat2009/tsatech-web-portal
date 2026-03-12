package com.newproject.web.controller;

import com.newproject.web.dto.Category;
import com.newproject.web.dto.CategoryAutoTranslateRequest;
import com.newproject.web.dto.CategoryAutoTranslateResponse;
import com.newproject.web.dto.CategoryRequest;
import com.newproject.web.dto.LocalizedContent;
import com.newproject.web.i18n.LanguageSupport;
import com.newproject.web.service.GatewayClient;
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
@RequestMapping({"/admin/categories", "/admin/catalogo/categorie"})
public class AdminCategoryController {
    private static final Logger logger = LoggerFactory.getLogger(AdminCategoryController.class);

    private final GatewayClient gatewayClient;

    public AdminCategoryController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(Model model) {
        List<Category> categories = gatewayClient.listCategories(null);
        model.addAttribute("categories", categories);
        return "admin/categories";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        CategoryRequest category = new CategoryRequest();
        category.setActive(true);
        category.setSortOrder(0);
        category.setTranslations(ensureCategoryTranslations(null, null));
        model.addAttribute("category", category);
        model.addAttribute("allCategories", gatewayClient.listCategories(true));
        model.addAttribute("formTitleKey", "admin.category.title.new");
        model.addAttribute("formAction", "/admin/catalogo/categorie");
        model.addAttribute("translationSourceLanguage", LanguageSupport.DEFAULT_LANGUAGE);
        model.addAttribute("autoTranslateDefault", true);
        model.addAttribute("overwriteTranslationsDefault", true);
        return "admin/category-form";
    }

    @PostMapping
    public String create(
        @ModelAttribute CategoryRequest request,
        @RequestParam(name = "translationSourceLanguage", required = false) String translationSourceLanguage,
        @RequestParam(name = "autoTranslate", defaultValue = "false") boolean autoTranslate,
        @RequestParam(name = "overwriteTranslations", defaultValue = "false") boolean overwriteTranslations
    ) {
        normalizeCategoryRequest(request);
        applyAutoTranslationsIfRequested(request, translationSourceLanguage, autoTranslate, overwriteTranslations);
        gatewayClient.createCategory(request);
        return "redirect:/admin/catalogo/categorie";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = gatewayClient.getCategorySafe(id).orElse(null);
        if (category == null) {
            return "redirect:/admin/catalogo/categorie";
        }

        CategoryRequest request = new CategoryRequest();
        request.setParentId(category.getParentId());
        request.setName(category.getName());
        request.setDescription(category.getDescription());
        request.setActive(category.getActive());
        request.setSortOrder(category.getSortOrder());
        request.setTranslations(ensureCategoryTranslations(category.getTranslations(), category));

        model.addAttribute("category", request);
        model.addAttribute("allCategories", gatewayClient.listCategories(true));
        model.addAttribute("editingCategoryId", id);
        model.addAttribute("formTitleKey", "admin.category.title.edit");
        model.addAttribute("formAction", "/admin/catalogo/categorie/" + id);
        model.addAttribute("translationSourceLanguage", LanguageSupport.DEFAULT_LANGUAGE);
        model.addAttribute("autoTranslateDefault", true);
        model.addAttribute("overwriteTranslationsDefault", true);
        return "admin/category-form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @ModelAttribute CategoryRequest request,
        @RequestParam(name = "translationSourceLanguage", required = false) String translationSourceLanguage,
        @RequestParam(name = "autoTranslate", defaultValue = "false") boolean autoTranslate,
        @RequestParam(name = "overwriteTranslations", defaultValue = "false") boolean overwriteTranslations
    ) {
        normalizeCategoryRequest(request);
        applyAutoTranslationsIfRequested(request, translationSourceLanguage, autoTranslate, overwriteTranslations);
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            request.setParentId(null);
        }
        gatewayClient.updateCategory(id, request);
        return "redirect:/admin/catalogo/categorie";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gatewayClient.deleteCategory(id);
        return "redirect:/admin/catalogo/categorie";
    }

    private void applyAutoTranslationsIfRequested(
        CategoryRequest request,
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

            CategoryAutoTranslateRequest translateRequest = new CategoryAutoTranslateRequest();
            translateRequest.setSourceLanguage(sourceLanguage);
            translateRequest.setOverwriteExisting(overwriteTranslations);
            translateRequest.setTranslations(request.getTranslations());

            CategoryAutoTranslateResponse translateResponse = gatewayClient.autoTranslateCategory(translateRequest);
            if (translateResponse == null || translateResponse.getTranslations() == null || translateResponse.getTranslations().isEmpty()) {
                return;
            }

            request.setTranslations(ensureCategoryTranslations(translateResponse.getTranslations(), null));
            LocalizedContent italian = request.getTranslations().get(LanguageSupport.DEFAULT_LANGUAGE);
            request.setName(firstNonBlank(
                italian != null ? italian.getName() : null,
                request.getName(),
                "Categoria"
            ));
            request.setDescription(firstNonBlank(
                italian != null ? italian.getDescription() : null,
                request.getDescription(),
                ""
            ));
        } catch (Exception ex) {
            logger.warn("Category auto-translation failed: {}", ex.getMessage());
        }
    }

    private void normalizeCategoryRequest(CategoryRequest request) {
        if (request.getActive() == null) {
            request.setActive(true);
        }
        if (request.getSortOrder() == null) {
            request.setSortOrder(0);
        }

        request.setTranslations(ensureCategoryTranslations(request.getTranslations(), null));
        LocalizedContent italian = request.getTranslations().get(LanguageSupport.DEFAULT_LANGUAGE);
        request.setName(firstNonBlank(
            italian != null ? italian.getName() : null,
            request.getName(),
            "Categoria"
        ));
        request.setDescription(firstNonBlank(
            italian != null ? italian.getDescription() : null,
            request.getDescription(),
            ""
        ));
    }

    private Map<String, LocalizedContent> ensureCategoryTranslations(Map<String, LocalizedContent> input, Category sourceCategory) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();
        String fallbackName = sourceCategory != null ? sourceCategory.getName() : null;
        String fallbackDescription = sourceCategory != null ? sourceCategory.getDescription() : null;

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent src = input != null ? input.get(language) : null;
            LocalizedContent content = new LocalizedContent();
            content.setName(firstNonBlank(src != null ? src.getName() : null, fallbackName, ""));
            content.setDescription(firstNonBlank(src != null ? src.getDescription() : null, fallbackDescription, ""));
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
