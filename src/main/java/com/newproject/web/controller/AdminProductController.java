package com.newproject.web.controller;

import com.newproject.web.dto.LocalizedContent;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductRequest;
import com.newproject.web.i18n.LanguageSupport;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping({"/admin/products", "/admin/catalogo/prodotti"})
public class AdminProductController {
    private final GatewayClient gatewayClient;

    public AdminProductController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(Model model) {
        List<Product> products = gatewayClient.listProducts();
        model.addAttribute("products", products);
        return "admin/products";
    }

    @GetMapping({"/new", "/nuovo"})
    public String createForm(Model model) {
        ProductRequest product = new ProductRequest();
        product.setActive(true);
        product.setCategoryIds(new HashSet<>());
        product.setTranslations(ensureProductTranslations(null, null));
        model.addAttribute("product", product);
        model.addAttribute("productView", null);
        model.addAttribute("categories", gatewayClient.listCategories(true));
        model.addAttribute("formTitle", "Nuovo prodotto");
        model.addAttribute("formAction", "/admin/catalogo/prodotti");
        return "admin/product-form";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String create(
        @ModelAttribute ProductRequest request,
        @RequestParam(name = "coverImageFile", required = false) MultipartFile coverImageFile,
        @RequestParam(name = "galleryImageFiles", required = false) MultipartFile[] galleryImageFiles
    ) {
        normalizeProductRequest(request);
        Product created = gatewayClient.createProduct(request);

        if (created != null && created.getId() != null) {
            uploadImages(created.getId(), coverImageFile, galleryImageFiles);
            return "redirect:/admin/catalogo/prodotti/" + created.getId() + "/modifica";
        }

        return "redirect:/admin/catalogo/prodotti";
    }

    @GetMapping({"/{id}/edit", "/{id}/modifica"})
    public String editForm(@PathVariable Long id, Model model) {
        Product product = gatewayClient.getProduct(id);
        ProductRequest request = new ProductRequest();
        request.setSku(product.getSku());
        request.setModel(product.getModel());
        request.setName(product.getName());
        request.setDescription(product.getDescription());
        request.setPrice(product.getPrice());
        request.setQuantity(product.getQuantity());
        request.setActive(product.getActive());
        request.setImage(product.getImage());
        request.setManufacturerId(product.getManufacturerId());
        request.setCategoryIds(product.getCategoryIds());
        request.setTranslations(ensureProductTranslations(product.getTranslations(), product));

        model.addAttribute("product", request);
        model.addAttribute("productView", product);
        model.addAttribute("categories", gatewayClient.listCategories(true));
        model.addAttribute("formTitle", "Modifica prodotto");
        model.addAttribute("formAction", "/admin/catalogo/prodotti/" + id);
        return "admin/product-form";
    }

    @PostMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String update(
        @PathVariable Long id,
        @ModelAttribute ProductRequest request,
        @RequestParam(name = "coverImageFile", required = false) MultipartFile coverImageFile,
        @RequestParam(name = "galleryImageFiles", required = false) MultipartFile[] galleryImageFiles,
        @RequestParam(name = "deleteImageIds", required = false) List<Long> deleteImageIds,
        @RequestParam(name = "selectedCoverImageId", required = false) Long selectedCoverImageId
    ) {
        normalizeProductRequest(request);
        gatewayClient.updateProduct(id, request);

        Set<Long> removedIds = Set.of();
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            removedIds = deleteImageIds.stream().collect(Collectors.toSet());
            for (Long imageId : removedIds) {
                gatewayClient.deleteProductImage(id, imageId);
            }
        }

        uploadImages(id, coverImageFile, galleryImageFiles);

        if (selectedCoverImageId != null && !removedIds.contains(selectedCoverImageId)) {
            gatewayClient.setProductCoverImage(id, selectedCoverImageId);
        }

        return "redirect:/admin/catalogo/prodotti/" + id + "/modifica";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gatewayClient.deleteProduct(id);
        return "redirect:/admin/catalogo/prodotti";
    }

    private void uploadImages(Long productId, MultipartFile coverImageFile, MultipartFile[] galleryImageFiles) {
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            gatewayClient.uploadProductCover(productId, coverImageFile);
        }

        if (galleryImageFiles != null && galleryImageFiles.length > 0) {
            List<MultipartFile> files = Arrays.stream(galleryImageFiles)
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());
            if (!files.isEmpty()) {
                gatewayClient.uploadProductGallery(productId, files);
            }
        }
    }

    private Map<String, LocalizedContent> ensureProductTranslations(Map<String, LocalizedContent> input, Product sourceProduct) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();
        String fallbackName = sourceProduct != null ? sourceProduct.getName() : null;
        String fallbackDescription = sourceProduct != null ? sourceProduct.getDescription() : null;

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent src = input != null ? input.get(language) : null;
            LocalizedContent content = new LocalizedContent();
            content.setName(firstNonBlank(src != null ? src.getName() : null, fallbackName, ""));
            content.setDescription(firstNonBlank(src != null ? src.getDescription() : null, fallbackDescription, ""));
            normalized.put(language, content);
        }
        return normalized;
    }

    private void normalizeProductRequest(ProductRequest request) {
        if (request.getSku() == null || request.getSku().isBlank()) {
            request.setSku("SKU-" + Instant.now().getEpochSecond());
        }
        if (request.getPrice() == null) {
            request.setPrice(BigDecimal.ZERO);
        }
        if (request.getQuantity() == null) {
            request.setQuantity(0);
        }
        if (request.getActive() == null) {
            request.setActive(true);
        }
        if (request.getCategoryIds() == null) {
            request.setCategoryIds(new HashSet<>());
        }

        request.setTranslations(ensureProductTranslations(request.getTranslations(), null));

        LocalizedContent italian = request.getTranslations().get(LanguageSupport.DEFAULT_LANGUAGE);
        request.setName(firstNonBlank(
            italian != null ? italian.getName() : null,
            request.getName(),
            request.getSku()
        ));
        request.setDescription(firstNonBlank(
            italian != null ? italian.getDescription() : null,
            request.getDescription(),
            ""
        ));
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
