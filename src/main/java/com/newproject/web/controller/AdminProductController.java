package com.newproject.web.controller;

import com.newproject.web.dto.InventoryRequest;
import com.newproject.web.dto.LocalizedContent;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductAutoTranslateRequest;
import com.newproject.web.dto.ProductAutoTranslateResponse;
import com.newproject.web.dto.ProductPrice;
import com.newproject.web.dto.ProductRequest;
import com.newproject.web.dto.ProductVariant;
import com.newproject.web.dto.ProductVariantRequest;
import com.newproject.web.i18n.LanguageSupport;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping({"/admin/products", "/admin/catalogo/prodotti"})
public class AdminProductController {
    private static final Logger logger = LoggerFactory.getLogger(AdminProductController.class);

    private final GatewayClient gatewayClient;

    public AdminProductController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(@RequestParam(name = "uploadError", required = false) String uploadError, Model model) {
        List<Product> products = gatewayClient.listProducts();
        model.addAttribute("products", products);
        model.addAttribute("uploadError", uploadError != null);
        return "admin/products";
    }

    @GetMapping({"/new", "/nuovo"})
    public String createForm(Model model) {
        ProductRequest product = new ProductRequest();
        product.setActive(true);
        product.setCategoryIds(new HashSet<>());
        product.setTranslations(ensureProductTranslations(null, null));
        product.setVariants(new ArrayList<>(List.of(blankVariant())));
        model.addAttribute("product", product);
        model.addAttribute("productView", null);
        model.addAttribute("categories", gatewayClient.listCategories(true));
        model.addAttribute("formTitleKey", "admin.product.title.new");
        model.addAttribute("formAction", "/admin/catalogo/prodotti");
        model.addAttribute("translationSourceLanguage", LanguageSupport.DEFAULT_LANGUAGE);
        model.addAttribute("autoTranslateDefault", true);
        model.addAttribute("overwriteTranslationsDefault", true);
        return "admin/product-form";
    }

    @PostMapping
    public String create(
        @ModelAttribute ProductRequest request,
        @RequestParam(name = "coverImageFile", required = false) MultipartFile coverImageFile,
        @RequestParam(name = "galleryImageFiles", required = false) MultipartFile[] galleryImageFiles,
        @RequestParam(name = "translationSourceLanguage", required = false) String translationSourceLanguage,
        @RequestParam(name = "autoTranslate", defaultValue = "false") boolean autoTranslate,
        @RequestParam(name = "overwriteTranslations", defaultValue = "false") boolean overwriteTranslations
    ) {
        normalizeProductRequest(request, translationSourceLanguage);
        applyAutoTranslationsIfRequested(request, translationSourceLanguage, autoTranslate, overwriteTranslations);
        Product created = gatewayClient.createProduct(request);

        if (created != null && created.getId() != null) {
            syncCommercialState(created.getId(), request, List.of());
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
        request.setSeoKeywords(product.getSeoKeywords());
        request.setManufacturerId(product.getManufacturerId());
        request.setCategoryIds(product.getCategoryIds());
        request.setTranslations(ensureProductTranslations(product.getTranslations(), product));
        request.setVariants(toVariantRequests(product.getVariants()));
        if (request.getVariants().isEmpty()) {
            request.setVariants(new ArrayList<>(List.of(blankVariant())));
        }

        model.addAttribute("product", request);
        model.addAttribute("productView", product);
        model.addAttribute("categories", gatewayClient.listCategories(true));
        model.addAttribute("formTitleKey", "admin.product.title.edit");
        model.addAttribute("formAction", "/admin/catalogo/prodotti/" + id);
        model.addAttribute("translationSourceLanguage", LanguageSupport.DEFAULT_LANGUAGE);
        model.addAttribute("autoTranslateDefault", true);
        model.addAttribute("overwriteTranslationsDefault", true);
        return "admin/product-form";
    }

    @PostMapping(path = "/{id}")
    public String update(
        @PathVariable Long id,
        @ModelAttribute ProductRequest request,
        @RequestParam(name = "coverImageFile", required = false) MultipartFile coverImageFile,
        @RequestParam(name = "galleryImageFiles", required = false) MultipartFile[] galleryImageFiles,
        @RequestParam(name = "deleteImageIds", required = false) List<Long> deleteImageIds,
        @RequestParam(name = "selectedCoverImageId", required = false) Long selectedCoverImageId,
        @RequestParam(name = "translationSourceLanguage", required = false) String translationSourceLanguage,
        @RequestParam(name = "autoTranslate", defaultValue = "false") boolean autoTranslate,
        @RequestParam(name = "overwriteTranslations", defaultValue = "false") boolean overwriteTranslations
    ) {
        Product previous = gatewayClient.getProductSafe(id).orElse(null);
        normalizeProductRequest(request, translationSourceLanguage);
        applyAutoTranslationsIfRequested(request, translationSourceLanguage, autoTranslate, overwriteTranslations);
        gatewayClient.updateProduct(id, request);
        syncCommercialState(id, request, previous != null ? previous.getVariants() : List.of());

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
        Product existing = gatewayClient.getProductSafe(id).orElse(null);
        gatewayClient.deleteProduct(id);
        deleteCommercialStateQuietly(id, existing != null ? existing.getVariants() : List.of());
        return "redirect:/admin/catalogo/prodotti";
    }

    private void syncCommercialState(Long productId, ProductRequest request, List<ProductVariant> previousVariants) {
        syncInventoryAfterCatalogChange(productId, request.getQuantity());
        syncPriceAfterCatalogChange(productId, request.getPrice());
        syncVariantCommercialState(productId, request.getVariants(), previousVariants);
    }

    private void deleteCommercialStateQuietly(Long productId, List<ProductVariant> variants) {
        deleteInventoryQuietly(productId);
        deletePriceQuietly(productId);
        for (ProductVariant variant : variants != null ? variants : List.<ProductVariant>of()) {
            String variantKey = trimToNull(variant != null ? variant.getVariantKey() : null);
            if (variantKey == null) {
                continue;
            }
            deleteVariantInventoryQuietly(productId, variantKey);
            deleteVariantPriceQuietly(productId, variantKey);
        }
    }

    private void syncInventoryAfterCatalogChange(Long productId, Integer quantity) {
        if (productId == null) {
            return;
        }

        InventoryRequest inventoryRequest = new InventoryRequest();
        inventoryRequest.setProductId(productId);
        inventoryRequest.setOnHand(quantity != null && quantity > 0 ? quantity : 0);
        inventoryRequest.setReserved(0);
        gatewayClient.upsertInventory(productId, inventoryRequest);
    }

    private void syncPriceAfterCatalogChange(Long productId, BigDecimal price) {
        if (productId == null) {
            return;
        }

        ProductPrice productPrice = new ProductPrice();
        productPrice.setProductId(productId);
        productPrice.setVariantKey("");
        productPrice.setAmount(price != null ? price : BigDecimal.ZERO);
        productPrice.setCurrency("EUR");
        productPrice.setActive(true);
        gatewayClient.upsertPrice(productId, productPrice);
    }

    private void syncVariantCommercialState(Long productId, List<ProductVariantRequest> variants, List<ProductVariant> previousVariants) {
        Set<String> staleVariantKeys = new LinkedHashSet<>((previousVariants != null ? previousVariants : List.<ProductVariant>of()).stream()
            .map(ProductVariant::getVariantKey)
            .filter(key -> key != null && !key.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new)));

        for (ProductVariantRequest variant : variants != null ? variants : List.<ProductVariantRequest>of()) {
            if (variant == null || isBlank(variant.getVariantKey())) {
                continue;
            }
            String variantKey = variant.getVariantKey().trim();
            staleVariantKeys.remove(variantKey);

            InventoryRequest inventoryRequest = new InventoryRequest();
            inventoryRequest.setProductId(productId);
            inventoryRequest.setVariantKey(variantKey);
            inventoryRequest.setOnHand(variant.getQuantity() != null && variant.getQuantity() > 0 ? variant.getQuantity() : 0);
            inventoryRequest.setReserved(0);
            gatewayClient.upsertVariantInventory(productId, variantKey, inventoryRequest);

            if (variant.getPriceOverride() != null) {
                ProductPrice price = new ProductPrice();
                price.setProductId(productId);
                price.setVariantKey(variantKey);
                price.setAmount(variant.getPriceOverride());
                price.setCurrency("EUR");
                price.setActive(Boolean.TRUE.equals(variant.getActive()));
                gatewayClient.upsertVariantPrice(productId, variantKey, price);
            } else {
                deleteVariantPriceQuietly(productId, variantKey);
            }
        }

        for (String staleVariantKey : staleVariantKeys) {
            deleteVariantInventoryQuietly(productId, staleVariantKey);
            deleteVariantPriceQuietly(productId, staleVariantKey);
        }
    }

    private void deleteInventoryQuietly(Long productId) {
        if (productId == null) {
            return;
        }

        try {
            gatewayClient.deleteInventory(productId);
        } catch (Exception ex) {
            logger.warn("Unable to delete inventory for product {}: {}", productId, ex.getMessage());
        }
    }

    private void deleteVariantInventoryQuietly(Long productId, String variantKey) {
        try {
            gatewayClient.deleteVariantInventory(productId, variantKey);
        } catch (Exception ex) {
            logger.warn("Unable to delete variant inventory for product {} variant {}: {}", productId, variantKey, ex.getMessage());
        }
    }

    private void deletePriceQuietly(Long productId) {
        try {
            gatewayClient.deletePrice(productId);
        } catch (Exception ex) {
            logger.warn("Unable to delete price for product {}: {}", productId, ex.getMessage());
        }
    }

    private void deleteVariantPriceQuietly(Long productId, String variantKey) {
        try {
            gatewayClient.deleteVariantPrice(productId, variantKey);
        } catch (Exception ex) {
            logger.warn("Unable to delete variant price for product {} variant {}: {}", productId, variantKey, ex.getMessage());
        }
    }

    @ExceptionHandler(MultipartException.class)
    public String handleMultipartException(MultipartException ex) {
        logger.warn("Multipart upload issue in product admin flow: {}", ex.getMessage());
        return "redirect:/admin/catalogo/prodotti?uploadError=1";
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

    private void applyAutoTranslationsIfRequested(
        ProductRequest request,
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

            ProductAutoTranslateRequest translateRequest = new ProductAutoTranslateRequest();
            translateRequest.setSourceLanguage(sourceLanguage);
            translateRequest.setOverwriteExisting(overwriteTranslations);
            translateRequest.setTranslations(request.getTranslations());

            ProductAutoTranslateResponse translateResponse = gatewayClient.autoTranslateProduct(translateRequest);
            if (translateResponse == null || translateResponse.getTranslations() == null || translateResponse.getTranslations().isEmpty()) {
                return;
            }

            request.setTranslations(ensureProductTranslations(translateResponse.getTranslations(), null));
            syncRootFieldsFromTranslations(request, sourceLanguage);
        } catch (Exception ex) {
            logger.warn("Product auto-translation failed: {}", ex.getMessage());
        }
    }

    private Map<String, LocalizedContent> ensureProductTranslations(Map<String, LocalizedContent> input, Product sourceProduct) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();
        String defaultName = sourceProduct != null ? sourceProduct.getName() : null;
        String defaultDescription = sourceProduct != null ? sourceProduct.getDescription() : null;

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent src = input != null ? input.get(language) : null;
            LocalizedContent content = new LocalizedContent();
            content.setName(firstNonBlank(
                src != null ? src.getName() : null,
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? defaultName : null
            ));
            content.setDescription(firstNonBlank(
                src != null ? src.getDescription() : null,
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? defaultDescription : null
            ));
            normalized.put(language, content);
        }
        return normalized;
    }

    private void normalizeProductRequest(ProductRequest request, String translationSourceLanguage) {
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
        request.setSeoKeywords(trimToNull(request.getSeoKeywords()));
        request.setVariants(normalizeVariants(request.getVariants(), request.getSku()));

        request.setTranslations(ensureProductTranslations(request.getTranslations(), null));
        syncRootFieldsFromTranslations(request, translationSourceLanguage);
    }

    private List<ProductVariantRequest> normalizeVariants(List<ProductVariantRequest> variants, String baseSku) {
        List<ProductVariantRequest> normalized = new ArrayList<>();
        int index = 0;
        for (ProductVariantRequest variant : variants != null ? variants : List.<ProductVariantRequest>of()) {
            if (variant == null) {
                continue;
            }
            String variantKey = trimToNull(variant.getVariantKey());
            String displayName = trimToNull(variant.getDisplayName());
            String optionSummary = trimToNull(variant.getOptionSummary());
            String sku = trimToNull(variant.getSku());
            if (variantKey == null && displayName == null && optionSummary == null && sku == null && variant.getPriceOverride() == null && variant.getQuantity() == null) {
                continue;
            }
            if (variantKey == null) {
                variantKey = slugify(firstNonBlank(displayName, optionSummary, sku, baseSku + "-variant-" + index));
            }
            ProductVariantRequest cleaned = new ProductVariantRequest();
            cleaned.setVariantKey(variantKey);
            cleaned.setSku(firstNonBlank(sku, baseSku != null ? baseSku + "-" + variantKey.toUpperCase() : null));
            cleaned.setDisplayName(firstNonBlank(displayName, optionSummary, variantKey));
            cleaned.setOptionSummary(firstNonBlank(optionSummary, displayName));
            cleaned.setImageUrl(trimToNull(variant.getImageUrl()));
            cleaned.setPriceOverride(variant.getPriceOverride());
            cleaned.setQuantity(variant.getQuantity() != null && variant.getQuantity() > 0 ? variant.getQuantity() : 0);
            cleaned.setActive(variant.getActive() == null || variant.getActive());
            cleaned.setSortOrder(variant.getSortOrder() != null ? variant.getSortOrder() : index);
            normalized.add(cleaned);
            index++;
        }
        return normalized;
    }

    private List<ProductVariantRequest> toVariantRequests(List<ProductVariant> variants) {
        List<ProductVariantRequest> requests = new ArrayList<>();
        for (ProductVariant variant : variants != null ? variants : List.<ProductVariant>of()) {
            if (variant == null) {
                continue;
            }
            ProductVariantRequest request = new ProductVariantRequest();
            request.setVariantKey(variant.getVariantKey());
            request.setSku(variant.getSku());
            request.setDisplayName(variant.getDisplayName());
            request.setOptionSummary(variant.getOptionSummary());
            request.setImageUrl(variant.getImageUrl());
            request.setPriceOverride(variant.getPriceOverride());
            request.setQuantity(variant.getQuantity());
            request.setActive(variant.getActive());
            request.setSortOrder(variant.getSortOrder());
            requests.add(request);
        }
        return requests;
    }

    private ProductVariantRequest blankVariant() {
        ProductVariantRequest request = new ProductVariantRequest();
        request.setActive(true);
        request.setQuantity(0);
        request.setSortOrder(0);
        return request;
    }

    private void syncRootFieldsFromTranslations(ProductRequest request, String preferredLanguage) {
        String rootLanguage = resolveRootLanguage(request.getTranslations(), preferredLanguage);
        LocalizedContent rootContent = request.getTranslations().get(rootLanguage);
        request.setName(firstNonBlank(
            rootContent != null ? rootContent.getName() : null,
            request.getName(),
            request.getSku()
        ));
        request.setDescription(firstNonBlank(
            rootContent != null ? rootContent.getDescription() : null,
            request.getDescription()
        ));
    }

    private String resolveRootLanguage(Map<String, LocalizedContent> translations, String preferredLanguage) {
        if (hasTranslatedContent(translations, LanguageSupport.DEFAULT_LANGUAGE)) {
            return LanguageSupport.DEFAULT_LANGUAGE;
        }

        String normalizedPreferred = LanguageSupport.normalizeLanguage(preferredLanguage);
        if (normalizedPreferred != null && hasTranslatedContent(translations, normalizedPreferred)) {
            return normalizedPreferred;
        }

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            if (hasTranslatedContent(translations, language)) {
                return language;
            }
        }

        return normalizedPreferred != null ? normalizedPreferred : LanguageSupport.DEFAULT_LANGUAGE;
    }

    private boolean hasTranslatedContent(Map<String, LocalizedContent> translations, String language) {
        if (translations == null) {
            return false;
        }
        LocalizedContent content = translations.get(language);
        return content != null
            && (trimToNull(content.getName()) != null || trimToNull(content.getDescription()) != null);
    }

    private String slugify(String value) {
        String base = trimToNull(value);
        if (base == null) {
            return "variant";
        }
        String normalized = base.toLowerCase()
            .replaceAll("[^a-z0-9._-]+", "-")
            .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "variant" : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
