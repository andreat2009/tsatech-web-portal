package com.newproject.web.controller;

import com.newproject.web.dto.Category;
import com.newproject.web.dto.CategoryRequest;
import com.newproject.web.service.GatewayClient;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/admin/categories", "/admin/catalogo/categorie"})
public class AdminCategoryController {
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
        model.addAttribute("category", category);
        model.addAttribute("allCategories", gatewayClient.listCategories(true));
        model.addAttribute("formTitle", "Nuova categoria");
        model.addAttribute("formAction", "/admin/catalogo/categorie");
        return "admin/category-form";
    }

    @PostMapping
    public String create(@ModelAttribute CategoryRequest request) {
        normalizeCategoryRequest(request);
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

        model.addAttribute("category", request);
        model.addAttribute("allCategories", gatewayClient.listCategories(true));
        model.addAttribute("editingCategoryId", id);
        model.addAttribute("formTitle", "Modifica categoria");
        model.addAttribute("formAction", "/admin/catalogo/categorie/" + id);
        return "admin/category-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute CategoryRequest request) {
        normalizeCategoryRequest(request);
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

    private void normalizeCategoryRequest(CategoryRequest request) {
        if (request.getActive() == null) {
            request.setActive(true);
        }
        if (request.getSortOrder() == null) {
            request.setSortOrder(0);
        }
    }
}
