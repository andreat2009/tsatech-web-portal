package com.newproject.web.controller;

import com.newproject.web.dto.Manufacturer;
import com.newproject.web.dto.ManufacturerRequest;
import com.newproject.web.service.GatewayClient;
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
        model.addAttribute("manufacturer", form);
        model.addAttribute("formTitleKey", "admin.manufacturer.title.new");
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

        model.addAttribute("manufacturer", form);
        model.addAttribute("formTitleKey", "admin.manufacturer.title.edit");
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

        String name = request.getName() != null ? request.getName().trim() : "";
        request.setName(name.isBlank() ? "Manufacturer" : name);

        if (request.getImage() != null) {
            String image = request.getImage().trim();
            request.setImage(image.isBlank() ? null : image);
        }

        // Manufacturer translation UI is intentionally disabled in admin.
        request.setTranslations(null);
    }
}
