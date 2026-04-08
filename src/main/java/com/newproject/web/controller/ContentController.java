package com.newproject.web.controller;

import com.newproject.web.dto.BlogCommentRequest;
import com.newproject.web.dto.BlogPost;
import com.newproject.web.dto.ContactMessageRequest;
import com.newproject.web.dto.InformationPage;
import com.newproject.web.service.GatewayClient;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ContentController {
    private final GatewayClient gatewayClient;
    private final String privacyPolicySlug;
    private final String privacyPolicyVersion;

    public ContentController(
        GatewayClient gatewayClient,
        @Value("${app.privacy-policy-path:/information/privacy-policy}") String privacyPolicyPath,
        @Value("${app.privacy-policy-version:2026-04}") String privacyPolicyVersion
    ) {
        this.gatewayClient = gatewayClient;
        this.privacyPolicyVersion = privacyPolicyVersion;
        String slug = "privacy-policy";
        if (privacyPolicyPath != null && !privacyPolicyPath.isBlank()) {
            String normalized = privacyPolicyPath.trim();
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            int idx = normalized.lastIndexOf('/');
            if (idx >= 0 && idx < normalized.length() - 1) {
                slug = normalized.substring(idx + 1);
            }
        }
        this.privacyPolicySlug = slug;
    }

    @GetMapping({"/information/contact", "/contatti"})
    public String contactForm(Model model) {
        ContactMessageRequest form = new ContactMessageRequest();
        model.addAttribute("contactForm", form);
        return "information/contact";
    }

    @PostMapping({"/information/contact", "/contatti"})
    public String submitContact(@ModelAttribute ContactMessageRequest form, Authentication authentication) {
        if (form.getName() == null || form.getName().isBlank()
            || form.getEmail() == null || form.getEmail().isBlank()
            || form.getSubject() == null || form.getSubject().isBlank()
            || form.getMessage() == null || form.getMessage().isBlank()) {
            return "redirect:/contatti?error=1";
        }

        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            if (form.getName().isBlank()) {
                form.setName(oidcUser.getPreferredUsername());
            }
            if (form.getEmail().isBlank()) {
                form.setEmail(oidcUser.getEmail());
            }
        }

        gatewayClient.createContactMessage(form);
        return "redirect:/contatti?success=1";
    }

    @GetMapping({"/information/sitemap", "/mappa-sito"})
    public String sitemap(Model model) {
        model.addAttribute("informationPages", gatewayClient.listInformationPages(true));
        model.addAttribute("blogPosts", gatewayClient.listBlogPosts(true));
        model.addAttribute("categories", gatewayClient.listCategoryTree(true));
        return "information/sitemap";
    }

    @GetMapping("/information/{slug}")
    public String information(@PathVariable String slug, Model model) {
        Optional<InformationPage> page = gatewayClient.getInformationBySlug(slug);
        if (page.isPresent()) {
            model.addAttribute("page", page.get());
            return "information/page";
        }
        if (privacyPolicySlug.equalsIgnoreCase(slug)) {
            model.addAttribute("privacyPolicyVersion", privacyPolicyVersion);
            return "information/privacy-fallback";
        }
        return "redirect:/mappa-sito";
    }

    @GetMapping({"/blog", "/news"})
    public String blog(Model model) {
        model.addAttribute("posts", gatewayClient.listBlogPosts(true));
        return "blog/list";
    }

    @GetMapping({"/blog/{slug}", "/news/{slug}"})
    public String blogPost(@PathVariable String slug, Model model) {
        Optional<BlogPost> post = gatewayClient.getBlogPostBySlug(slug);
        if (post.isEmpty()) {
            return "redirect:/news";
        }

        BlogCommentRequest form = new BlogCommentRequest();
        model.addAttribute("post", post.get());
        model.addAttribute("comments", gatewayClient.listBlogCommentsByPost(post.get().getId(), true));
        model.addAttribute("commentForm", form);
        return "blog/post";
    }

    @PostMapping({"/blog/{slug}/comments", "/news/{slug}/commenti"})
    public String addComment(@PathVariable String slug, @ModelAttribute BlogCommentRequest form, Authentication authentication) {
        Optional<BlogPost> post = gatewayClient.getBlogPostBySlug(slug);
        if (post.isEmpty()) {
            return "redirect:/news";
        }

        if (form.getComment() == null || form.getComment().isBlank()) {
            return "redirect:/news/" + slug;
        }

        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            if (form.getAuthorName() == null || form.getAuthorName().isBlank()) {
                String fallback = oidcUser.getGivenName() != null ? oidcUser.getGivenName() : oidcUser.getPreferredUsername();
                form.setAuthorName(fallback);
            }
            if (form.getAuthorEmail() == null || form.getAuthorEmail().isBlank()) {
                form.setAuthorEmail(oidcUser.getEmail());
            }
        }

        if (form.getAuthorName() == null || form.getAuthorName().isBlank()) {
            form.setAuthorName("Guest");
        }

        gatewayClient.createBlogComment(post.get().getId(), form);
        return "redirect:/news/" + slug;
    }
}
