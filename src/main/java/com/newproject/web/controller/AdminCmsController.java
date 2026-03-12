package com.newproject.web.controller;

import com.newproject.web.dto.BlogPost;
import com.newproject.web.dto.BlogPostRequest;
import com.newproject.web.dto.InformationPage;
import com.newproject.web.dto.InformationRequest;
import com.newproject.web.dto.LocalizedContent;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.dto.StoreSettings;
import com.newproject.web.i18n.LanguageSupport;
import com.newproject.web.service.GatewayClient;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin")
public class AdminCmsController {
    private static final int DEFAULT_LOGO_MAX_HEIGHT_PX = 96;
    private static final int DEFAULT_SITE_NAME_FONT_SIZE_PX = 28;
    private static final int MIN_LOGO_MAX_HEIGHT_PX = 32;
    private static final int MAX_LOGO_MAX_HEIGHT_PX = 220;
    private static final int MIN_SITE_NAME_FONT_SIZE_PX = 14;
    private static final int MAX_SITE_NAME_FONT_SIZE_PX = 56;

    private final GatewayClient gatewayClient;

    public AdminCmsController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping("/store-settings")
    public String storeSettingsForm(Model model) {
        StoreSettings settings = gatewayClient.getStoreSettings();
        if (settings == null) {
            settings = new StoreSettings();
            PublicStoreSettings fallback = gatewayClient.getPublicStoreSettings();
            settings.setSiteName(fallback.getSiteName());
            settings.setLogoUrl(fallback.getLogoUrl());
            settings.setLogoMaxHeightPx(fallback.getLogoMaxHeightPx());
            settings.setSiteNameFontSizePx(fallback.getSiteNameFontSizePx());
            settings.setContactEmail(fallback.getContactEmail());
            settings.setSupportEmail(fallback.getSupportEmail());
            settings.setSupportPhone(fallback.getSupportPhone());
            settings.setSupportPhoneSecondary(fallback.getSupportPhoneSecondary());
            settings.setAddressLine1(fallback.getAddressLine1());
            settings.setAddressLine2(fallback.getAddressLine2());
            settings.setCity(fallback.getCity());
            settings.setPostalCode(fallback.getPostalCode());
            settings.setCountry(fallback.getCountry());
            settings.setSmtpEnabled(true);
            settings.setSmtpHost("smtp.gmail.com");
            settings.setSmtpPort(587);
            settings.setSmtpAuth(true);
            settings.setSmtpStarttls(true);
            settings.setSmtpUsername("andrea.terrasi78@gmail.com");
            settings.setMailFromEmail("andrea.terrasi78@gmail.com");
            settings.setMailFromName(firstNonBlank(fallback.getSiteName(), "TSATech Store"));
        }

        normalizeStoreSettings(settings);
        settings.setSmtpPassword("");
        model.addAttribute("settingsForm", settings);
        return "admin/store-settings";
    }

    @PostMapping("/store-settings")
    public String storeSettingsSave(
        @ModelAttribute("settingsForm") StoreSettings form,
        @RequestParam(name = "logoFile", required = false) MultipartFile logoFile,
        @RequestParam(name = "removeLogo", defaultValue = "false") boolean removeLogo
    ) {
        normalizeStoreSettings(form);

        String logoError = applyLogoUpload(form, logoFile, removeLogo);
        if (logoError != null) {
            return "redirect:/admin/store-settings?error=" + logoError;
        }

        gatewayClient.updateStoreSettings(form);
        return "redirect:/admin/store-settings?success=1";
    }
    @GetMapping("/information")
    public String informationList(Model model) {
        model.addAttribute("pages", gatewayClient.listInformationPages(null));
        return "admin/information-list";
    }

    @GetMapping("/information/new")
    public String informationCreateForm(Model model) {
        InformationRequest form = new InformationRequest();
        form.setActive(true);
        form.setSortOrder(0);
        form.setTranslations(ensureInformationTranslations(null, null));
        model.addAttribute("pageForm", form);
        model.addAttribute("formAction", "/admin/information");
        model.addAttribute("formTitleKey", "admin.information.title.new");
        return "admin/information-form";
    }

    @PostMapping("/information")
    public String informationCreate(@ModelAttribute InformationRequest form) {
        normalizeInformation(form);
        gatewayClient.createInformationPage(form);
        return "redirect:/admin/information";
    }

    @GetMapping("/information/{id}/edit")
    public String informationEdit(@PathVariable Long id, Model model) {
        InformationPage page = gatewayClient.getInformationPageSafe(id).orElse(null);
        if (page == null) {
            return "redirect:/admin/information";
        }
        InformationRequest form = new InformationRequest();
        form.setTitle(page.getTitle());
        form.setSlug(page.getSlug());
        form.setContent(page.getContent());
        form.setSortOrder(page.getSortOrder());
        form.setActive(page.getActive());
        form.setTranslations(ensureInformationTranslations(page.getTranslations(), page));

        model.addAttribute("pageForm", form);
        model.addAttribute("formAction", "/admin/information/" + id);
        model.addAttribute("formTitleKey", "admin.information.title.edit");
        return "admin/information-form";
    }

    @PostMapping("/information/{id}")
    public String informationUpdate(@PathVariable Long id, @ModelAttribute InformationRequest form) {
        normalizeInformation(form);
        gatewayClient.updateInformationPage(id, form);
        return "redirect:/admin/information";
    }

    @PostMapping("/information/{id}/delete")
    public String informationDelete(@PathVariable Long id) {
        gatewayClient.deleteInformationPage(id);
        return "redirect:/admin/information";
    }

    @GetMapping("/blog/posts")
    public String blogPosts(Model model) {
        model.addAttribute("posts", gatewayClient.listBlogPosts(null));
        return "admin/blog-post-list";
    }

    @GetMapping("/blog/posts/new")
    public String blogPostCreateForm(Model model) {
        BlogPostRequest form = new BlogPostRequest();
        form.setActive(true);
        form.setPublishedAt(OffsetDateTime.now());
        form.setTranslations(ensureBlogTranslations(null, null));
        model.addAttribute("postForm", form);
        model.addAttribute("formAction", "/admin/blog/posts");
        model.addAttribute("formTitleKey", "admin.blog.title.new");
        return "admin/blog-post-form";
    }

    @PostMapping("/blog/posts")
    public String blogPostCreate(@ModelAttribute BlogPostRequest form) {
        normalizePost(form);
        gatewayClient.createBlogPost(form);
        return "redirect:/admin/blog/posts";
    }

    @GetMapping("/blog/posts/{id}/edit")
    public String blogPostEdit(@PathVariable Long id, Model model) {
        BlogPost post = gatewayClient.getBlogPostSafe(id).orElse(null);
        if (post == null) {
            return "redirect:/admin/blog/posts";
        }

        BlogPostRequest form = new BlogPostRequest();
        form.setTitle(post.getTitle());
        form.setSlug(post.getSlug());
        form.setExcerpt(post.getExcerpt());
        form.setContent(post.getContent());
        form.setAuthor(post.getAuthor());
        form.setPublishedAt(post.getPublishedAt());
        form.setActive(post.getActive());
        form.setTranslations(ensureBlogTranslations(post.getTranslations(), post));

        model.addAttribute("postForm", form);
        model.addAttribute("formAction", "/admin/blog/posts/" + id);
        model.addAttribute("formTitleKey", "admin.blog.title.edit");
        return "admin/blog-post-form";
    }

    @PostMapping("/blog/posts/{id}")
    public String blogPostUpdate(@PathVariable Long id, @ModelAttribute BlogPostRequest form) {
        normalizePost(form);
        gatewayClient.updateBlogPost(id, form);
        return "redirect:/admin/blog/posts";
    }

    @PostMapping("/blog/posts/{id}/delete")
    public String blogPostDelete(@PathVariable Long id) {
        gatewayClient.deleteBlogPost(id);
        return "redirect:/admin/blog/posts";
    }

    @GetMapping("/blog/comments")
    public String blogComments(@RequestParam(required = false) Boolean approved, Model model) {
        model.addAttribute("comments", gatewayClient.listBlogComments(approved));
        model.addAttribute("filterApproved", approved);
        return "admin/blog-comment-list";
    }

    @PostMapping("/blog/comments/{id}/approval")
    public String blogCommentApproval(@PathVariable Long id, @RequestParam boolean approved) {
        gatewayClient.setBlogCommentApproval(id, approved);
        return "redirect:/admin/blog/comments";
    }

    @GetMapping("/contact-messages")
    public String contactMessages(@RequestParam(required = false) String status, Model model) {
        model.addAttribute("messages", gatewayClient.listContactMessages(status));
        model.addAttribute("status", status);
        return "admin/contact-messages";
    }

    @PostMapping("/contact-messages/{id}/status")
    public String contactMessageStatus(@PathVariable Long id, @RequestParam String status) {
        gatewayClient.updateContactMessageStatus(id, status);
        return "redirect:/admin/contact-messages";
    }

    private void normalizeInformation(InformationRequest form) {
        if (form.getSortOrder() == null) {
            form.setSortOrder(0);
        }
        if (form.getActive() == null) {
            form.setActive(true);
        }

        form.setTranslations(ensureInformationTranslations(form.getTranslations(), null));
        LocalizedContent italian = form.getTranslations().get(LanguageSupport.DEFAULT_LANGUAGE);
        form.setTitle(firstNonBlank(
            italian != null ? italian.getTitle() : null,
            form.getTitle(),
            "Pagina"
        ));
        form.setContent(firstNonBlank(
            italian != null ? italian.getContent() : null,
            form.getContent(),
            ""
        ));
    }

    private void normalizePost(BlogPostRequest form) {
        if (form.getActive() == null) {
            form.setActive(true);
        }
        if (form.getPublishedAt() == null) {
            form.setPublishedAt(OffsetDateTime.now());
        }

        form.setTranslations(ensureBlogTranslations(form.getTranslations(), null));
        LocalizedContent italian = form.getTranslations().get(LanguageSupport.DEFAULT_LANGUAGE);
        form.setTitle(firstNonBlank(
            italian != null ? italian.getTitle() : null,
            form.getTitle(),
            "Articolo"
        ));
        form.setExcerpt(firstNonBlank(
            italian != null ? italian.getExcerpt() : null,
            form.getExcerpt(),
            ""
        ));
        form.setContent(firstNonBlank(
            italian != null ? italian.getContent() : null,
            form.getContent(),
            ""
        ));
    }

    private Map<String, LocalizedContent> ensureInformationTranslations(Map<String, LocalizedContent> input, InformationPage sourcePage) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();
        String fallbackTitle = sourcePage != null ? sourcePage.getTitle() : null;
        String fallbackContent = sourcePage != null ? sourcePage.getContent() : null;

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent src = input != null ? input.get(language) : null;
            LocalizedContent content = new LocalizedContent();
            content.setTitle(firstNonBlank(src != null ? src.getTitle() : null, fallbackTitle, ""));
            content.setContent(firstNonBlank(src != null ? src.getContent() : null, fallbackContent, ""));
            normalized.put(language, content);
        }

        return normalized;
    }

    private Map<String, LocalizedContent> ensureBlogTranslations(Map<String, LocalizedContent> input, BlogPost sourcePost) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();
        String fallbackTitle = sourcePost != null ? sourcePost.getTitle() : null;
        String fallbackExcerpt = sourcePost != null ? sourcePost.getExcerpt() : null;
        String fallbackContent = sourcePost != null ? sourcePost.getContent() : null;

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent src = input != null ? input.get(language) : null;
            LocalizedContent content = new LocalizedContent();
            content.setTitle(firstNonBlank(src != null ? src.getTitle() : null, fallbackTitle, ""));
            content.setExcerpt(firstNonBlank(src != null ? src.getExcerpt() : null, fallbackExcerpt, ""));
            content.setContent(firstNonBlank(src != null ? src.getContent() : null, fallbackContent, ""));
            normalized.put(language, content);
        }

        return normalized;
    }

    private String applyLogoUpload(StoreSettings form, MultipartFile logoFile, boolean removeLogo) {
        if (removeLogo) {
            form.setLogoUrl(null);
            return null;
        }
        if (logoFile == null || logoFile.isEmpty()) {
            return null;
        }

        if (logoFile.getSize() > 1024 * 1024) {
            return "logo_size";
        }

        String contentType = logoFile.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return "logo_type";
        }

        try {
            String encoded = Base64.getEncoder().encodeToString(logoFile.getBytes());
            form.setLogoUrl("data:" + contentType + ";base64," + encoded);
            return null;
        } catch (IOException ex) {
            return "logo_upload";
        }
    }

    private void normalizeStoreSettings(StoreSettings form) {
        form.setSiteName(firstNonBlank(form.getSiteName(), "TSATech Store"));
        form.setLogoUrl(trimToNull(form.getLogoUrl()));
        form.setLogoMaxHeightPx(clampInt(form.getLogoMaxHeightPx(), DEFAULT_LOGO_MAX_HEIGHT_PX, MIN_LOGO_MAX_HEIGHT_PX, MAX_LOGO_MAX_HEIGHT_PX));
        form.setSiteNameFontSizePx(clampInt(form.getSiteNameFontSizePx(), DEFAULT_SITE_NAME_FONT_SIZE_PX, MIN_SITE_NAME_FONT_SIZE_PX, MAX_SITE_NAME_FONT_SIZE_PX));
        form.setContactEmail(trimToNull(form.getContactEmail()));
        form.setSupportEmail(trimToNull(form.getSupportEmail()));
        form.setSupportPhone(trimToNull(form.getSupportPhone()));
        form.setSupportPhoneSecondary(trimToNull(form.getSupportPhoneSecondary()));
        form.setAddressLine1(trimToNull(form.getAddressLine1()));
        form.setAddressLine2(trimToNull(form.getAddressLine2()));
        form.setCity(trimToNull(form.getCity()));
        form.setPostalCode(trimToNull(form.getPostalCode()));
        form.setCountry(trimToNull(form.getCountry()));

        if (form.getSmtpEnabled() == null) {
            form.setSmtpEnabled(false);
        }
        if (form.getSmtpAuth() == null) {
            form.setSmtpAuth(false);
        }
        if (form.getSmtpStarttls() == null) {
            form.setSmtpStarttls(false);
        }
        if (form.getSmtpPort() == null || form.getSmtpPort() <= 0) {
            form.setSmtpPort(587);
        }

        form.setSmtpHost(trimToNull(form.getSmtpHost()));
        form.setSmtpUsername(trimToNull(form.getSmtpUsername()));
        form.setMailFromEmail(trimToNull(form.getMailFromEmail()));
        form.setMailFromName(trimToNull(form.getMailFromName()));

        if (form.getSmtpPassword() != null) {
            String trimmed = form.getSmtpPassword().trim();
            form.setSmtpPassword(trimmed.isEmpty() ? null : trimmed);
        }
    }

    private Integer clampInt(Integer value, int defaultValue, int min, int max) {
        int resolved = value != null ? value : defaultValue;
        if (resolved < min) {
            return min;
        }
        if (resolved > max) {
            return max;
        }
        return resolved;
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
}
