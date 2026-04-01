package com.newproject.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class PortalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(PortalExceptionHandler.class);

    private final MessageSource messageSource;

    public PortalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(Exception.class)
    public Object handle(Exception exception, HttpServletRequest request) {
        HttpStatus status = resolveStatus(exception);
        Locale locale = LocaleContextHolder.getLocale();
        String reference = UUID.randomUUID().toString();
        String path = request.getRequestURI();
        String message = resolveMessage(exception, status, locale);

        if (status.is5xxServerError()) {
            logger.error("Unhandled portal error [{}] on {}", reference, path, exception);
        } else {
            logger.warn("Portal request error [{}] on {}: {}", reference, path, exception.getMessage());
        }

        if (expectsJson(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", OffsetDateTime.now());
            body.put("status", status.value());
            body.put("error", status.getReasonPhrase());
            body.put("message", message);
            body.put("path", path);
            body.put("reference", reference);
            return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
        }

        ModelAndView mav = new ModelAndView("error/application-error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("errorMessage", message);
        mav.addObject("path", path);
        mav.addObject("reference", reference);
        mav.addObject("backUrl", sanitizeBackUrl(request.getHeader("Referer")));
        return mav;
    }

    private boolean expectsJson(HttpServletRequest request) {
        String accept = Optional.ofNullable(request.getHeader("Accept")).orElse("");
        String requestedWith = Optional.ofNullable(request.getHeader("X-Requested-With")).orElse("");
        String uri = Optional.ofNullable(request.getRequestURI()).orElse("");
        return uri.startsWith("/api/")
            || accept.contains(MediaType.APPLICATION_JSON_VALUE)
            || "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private String sanitizeBackUrl(String referer) {
        if (!StringUtils.hasText(referer)) {
            return "/";
        }
        return referer;
    }

    private HttpStatus resolveStatus(Exception exception) {
        String simpleName = exception.getClass().getSimpleName();
        if (exception instanceof MaxUploadSizeExceededException) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }
        if (exception instanceof MultipartException
            || exception instanceof BindException
            || exception instanceof MethodArgumentNotValidException
            || exception instanceof MissingServletRequestParameterException
            || exception instanceof MethodArgumentTypeMismatchException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (exception instanceof WebClientResponseException webClientResponseException) {
            HttpStatus status = HttpStatus.resolve(webClientResponseException.getStatusCode().value());
            return status != null ? status : HttpStatus.BAD_GATEWAY;
        }
        if (exception instanceof NoResourceFoundException || "NotFoundException".equals(simpleName)) {
            return HttpStatus.NOT_FOUND;
        }
        if ("AccessDeniedException".equals(simpleName)) {
            return HttpStatus.FORBIDDEN;
        }
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getBody().getStatus());
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Exception exception, HttpStatus status, Locale locale) {
        if (status == HttpStatus.FORBIDDEN) {
            return message("common.error.forbidden", "Insufficient permissions for this action.", locale);
        }
        if (status == HttpStatus.NOT_FOUND) {
            return message("error.page.notFound", "The requested resource was not found.", locale);
        }
        if (status == HttpStatus.PAYLOAD_TOO_LARGE) {
            return message("error.page.payloadTooLarge", "The uploaded file is too large.", locale);
        }
        if (status.is5xxServerError()) {
            return message("error.page.unavailable", "The service is temporarily unavailable. Please try again.", locale);
        }
        if (exception instanceof WebClientResponseException webClientResponseException && StringUtils.hasText(webClientResponseException.getResponseBodyAsString())) {
            return webClientResponseException.getResponseBodyAsString();
        }
        if (StringUtils.hasText(exception.getMessage())) {
            return exception.getMessage();
        }
        return message("common.error.generic", "Operation failed. Please try again.", locale);
    }

    private String message(String key, String fallback, Locale locale) {
        return messageSource.getMessage(key, null, fallback, locale);
    }
}
