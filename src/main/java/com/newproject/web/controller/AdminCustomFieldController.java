package com.newproject.web.controller;

import com.newproject.web.dto.AdminCustomFieldForm;
import com.newproject.web.dto.CustomFieldDefinition;
import com.newproject.web.dto.CustomFieldOption;
import com.newproject.web.service.GatewayClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/admin/custom-fields", "/admin/clienti/campi-personalizzati"})
public class AdminCustomFieldController {
    private static final List<String> FIELD_TYPES = List.of("TEXT", "TEXTAREA", "NUMBER", "DATE", "BOOLEAN", "SELECT", "MULTISELECT", "RADIO");
    private static final List<String> FIELD_SCOPES = List.of("CHECKOUT", "CUSTOMER", "ADDRESS", "ORDER");

    private final GatewayClient gatewayClient;

    public AdminCustomFieldController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(
        @RequestParam(name = "saved", required = false) String saved,
        @RequestParam(name = "updated", required = false) String updated,
        @RequestParam(name = "deleted", required = false) String deleted,
        @RequestParam(name = "error", required = false) String error,
        Model model
    ) {
        model.addAttribute("customFields", gatewayClient.listCustomFields(null, null));
        model.addAttribute("saved", saved != null);
        model.addAttribute("updated", updated != null);
        model.addAttribute("deleted", deleted != null);
        model.addAttribute("error", error);
        return "admin/custom-fields";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AdminCustomFieldForm form = new AdminCustomFieldForm();
        form.setFieldType("TEXT");
        form.setFieldScope("CHECKOUT");
        form.setActive(true);
        form.setPersistForCustomer(false);
        form.setRequired(false);
        form.setSortOrder(10);
        fillFormModel(model, form, "/admin/custom-fields", "New Custom Field");
        return "admin/custom-field-form";
    }

    @PostMapping
    public String create(@ModelAttribute("customField") AdminCustomFieldForm form) {
        try {
            gatewayClient.createCustomField(toDefinition(form));
            return "redirect:/admin/custom-fields?saved=1";
        } catch (Exception ex) {
            return "redirect:/admin/custom-fields?error=create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CustomFieldDefinition definition = gatewayClient.getCustomField(id);
        fillFormModel(model, toForm(definition), "/admin/custom-fields/" + id, "Edit Custom Field");
        return "admin/custom-field-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("customField") AdminCustomFieldForm form) {
        try {
            gatewayClient.updateCustomField(id, toDefinition(form));
            return "redirect:/admin/custom-fields?updated=1";
        } catch (Exception ex) {
            return "redirect:/admin/custom-fields?error=update";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        try {
            gatewayClient.deleteCustomField(id);
            return "redirect:/admin/custom-fields?deleted=1";
        } catch (Exception ex) {
            return "redirect:/admin/custom-fields?error=delete";
        }
    }

    private void fillFormModel(Model model, AdminCustomFieldForm form, String action, String title) {
        model.addAttribute("customField", form);
        model.addAttribute("fieldTypeOptions", FIELD_TYPES);
        model.addAttribute("fieldScopeOptions", FIELD_SCOPES);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
    }

    private CustomFieldDefinition toDefinition(AdminCustomFieldForm form) {
        CustomFieldDefinition definition = new CustomFieldDefinition();
        definition.setCode(trimToNull(form.getCode()));
        definition.setLabel(trimToNull(form.getLabel()));
        definition.setPlaceholder(trimToNull(form.getPlaceholder()));
        definition.setHelpText(trimToNull(form.getHelpText()));
        definition.setFieldType(trimToNull(form.getFieldType()));
        definition.setFieldScope(trimToNull(form.getFieldScope()));
        definition.setRequired(Boolean.TRUE.equals(form.getRequired()));
        definition.setActive(form.getActive() == null || form.getActive());
        definition.setPersistForCustomer(Boolean.TRUE.equals(form.getPersistForCustomer()));
        definition.setSortOrder(form.getSortOrder() != null ? form.getSortOrder() : 0);
        definition.setOptions(parseOptions(form.getOptionsText(), form.getFieldType()));
        return definition;
    }

    private AdminCustomFieldForm toForm(CustomFieldDefinition definition) {
        AdminCustomFieldForm form = new AdminCustomFieldForm();
        form.setCode(definition.getCode());
        form.setLabel(definition.getLabel());
        form.setPlaceholder(definition.getPlaceholder());
        form.setHelpText(definition.getHelpText());
        form.setFieldType(definition.getFieldType());
        form.setFieldScope(definition.getFieldScope());
        form.setRequired(Boolean.TRUE.equals(definition.getRequired()));
        form.setActive(definition.getActive() == null || definition.getActive());
        form.setPersistForCustomer(Boolean.TRUE.equals(definition.getPersistForCustomer()));
        form.setSortOrder(definition.getSortOrder());
        form.setOptionsText(renderOptions(definition.getOptions()));
        return form;
    }

    private List<CustomFieldOption> parseOptions(String optionsText, String fieldType) {
        if (!supportsOptions(fieldType) || optionsText == null || optionsText.isBlank()) {
            return List.of();
        }
        List<CustomFieldOption> options = new ArrayList<>();
        String[] lines = optionsText.split("\\r?\\n");
        int sortOrder = 10;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", 2);
            String optionValue = trimToNull(parts[0]);
            String optionLabel = trimToNull(parts.length > 1 ? parts[1] : parts[0]);
            if (optionValue == null || optionLabel == null) {
                continue;
            }
            CustomFieldOption option = new CustomFieldOption();
            option.setOptionValue(optionValue);
            option.setLabel(optionLabel);
            option.setSortOrder(sortOrder);
            sortOrder += 10;
            options.add(option);
        }
        return options;
    }

    private boolean supportsOptions(String fieldType) {
        if (fieldType == null) {
            return false;
        }
        return switch (fieldType.trim().toUpperCase()) {
            case "SELECT", "MULTISELECT", "RADIO" -> true;
            default -> false;
        };
    }

    private String renderOptions(List<CustomFieldOption> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (CustomFieldOption option : options) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(option.getOptionValue() != null ? option.getOptionValue() : "");
            builder.append('|');
            builder.append(option.getLabel() != null ? option.getLabel() : "");
        }
        return builder.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
