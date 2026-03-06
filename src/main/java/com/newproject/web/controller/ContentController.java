package com.newproject.web.controller;

import com.newproject.web.dto.*;
import com.newproject.web.service.GatewayClient;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ContentController {
    private final GatewayClient gatewayClient;

    public ContentController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping("/information/contact")
    public String contactForm(Model model) {
        ContactMessageRequest form = new ContactMessageRequest();
        model.addAttribute("contactForm", form);
        return "information/contact";
    }

    @PostMapping("/information/contact")
    public String submitContact(@ModelAttribute ContactMessageRequest form, Authentication authentication) {
        if (form.getName() == null || form.getName().isBlank()
            || form.getEmail() == null || form.getEmail().isBlank()
            || form.getSubject() == null || form.getSubject().isBlank()
            || form.getMessage() == null || form.getMessage().isBlank()) {
            return "redirect:/information/contact?error=1";
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
        return "redirect:/information/contact?success=1";
    }

    @GetMapping("/information/sitemap")
    public String sitemap(Model model) {
        model.addAttribute("informationPages", gatewayClient.listInformationPages(true));
        model.addAttribute("blogPosts", gatewayClient.listBlogPosts(true));
        model.addAttribute("categories", gatewayClient.listCategoryTree(true));
        return "information/sitemap";
    }

    @GetMapping("/information/{slug}")
    public String information(@PathVariable String slug, Model model) {
        Optional<InformationPage> page = gatewayClient.getInformationBySlug(slug);
        if (page.isEmpty()) {
            return "redirect:/information/sitemap";
        }
        model.addAttribute("page", page.get());
        return "information/page";
    }

    @GetMapping("/blog")
    public String blog(Model model) {
        model.addAttribute("posts", gatewayClient.listBlogPosts(true));
        return "blog/list";
    }

    @GetMapping("/blog/{slug}")
    public String blogPost(@PathVariable String slug, Model model) {
        Optional<BlogPost> post = gatewayClient.getBlogPostBySlug(slug);
        if (post.isEmpty()) {
            return "redirect:/blog";
        }

        BlogCommentRequest form = new BlogCommentRequest();
        model.addAttribute("post", post.get());
        model.addAttribute("comments", gatewayClient.listBlogCommentsByPost(post.get().getId(), true));
        model.addAttribute("commentForm", form);
        return "blog/post";
    }

    @PostMapping("/blog/{slug}/comments")
    public String addComment(@PathVariable String slug, @ModelAttribute BlogCommentRequest form, Authentication authentication) {
        Optional<BlogPost> post = gatewayClient.getBlogPostBySlug(slug);
        if (post.isEmpty()) {
            return "redirect:/blog";
        }

        if (form.getComment() == null || form.getComment().isBlank()) {
            return "redirect:/blog/" + slug;
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
        return "redirect:/blog/" + slug;
    }
}
