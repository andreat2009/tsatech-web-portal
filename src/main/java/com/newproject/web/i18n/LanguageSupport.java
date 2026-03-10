package com.newproject.web.i18n;

import java.util.List;
import java.util.Locale;

public final class LanguageSupport {
    public static final String DEFAULT_LANGUAGE = "it";
    public static final List<String> SUPPORTED_LANGUAGES = List.of("it", "en", "fr", "de", "es");

    private LanguageSupport() {
    }

    public static String normalizeLanguage(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }

        int separator = normalized.indexOf('-');
        if (separator >= 0) {
            normalized = normalized.substring(0, separator);
        }

        separator = normalized.indexOf('_');
        if (separator >= 0) {
            normalized = normalized.substring(0, separator);
        }

        return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : null;
    }

    public static String fromLocale(Locale locale) {
        if (locale == null) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = normalizeLanguage(locale.toLanguageTag());
        return normalized != null ? normalized : DEFAULT_LANGUAGE;
    }

    public static String label(String code) {
        return switch (code) {
            case "it" -> "Italiano";
            case "en" -> "English";
            case "fr" -> "Francais";
            case "de" -> "Deutsch";
            case "es" -> "Espanol";
            default -> code;
        };
    }

    public static String flag(String code) {
        return switch (code) {
            case "it" -> "🇮🇹";
            case "en" -> "🇬🇧";
            case "fr" -> "🇫🇷";
            case "de" -> "🇩🇪";
            case "es" -> "🇪🇸";
            default -> code.toUpperCase(Locale.ROOT);
        };
    }
}
