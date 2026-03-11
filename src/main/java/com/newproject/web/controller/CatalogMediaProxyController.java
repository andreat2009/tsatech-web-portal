package com.newproject.web.controller;

import com.newproject.web.service.GatewayClient;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogMediaProxyController {

    private static final Pattern SAFE_FILENAME = Pattern.compile("[A-Za-z0-9._-]+");

    private final GatewayClient gatewayClient;

    public CatalogMediaProxyController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping("/catalogo/media/{filename:.+}")
    public ResponseEntity<byte[]> media(@PathVariable String filename) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.badRequest().build();
        }

        return gatewayClient.loadCatalogMedia(filename)
            .map(asset -> {
                MediaType contentType = asset.contentType() != null
                    ? asset.contentType()
                    : MediaType.APPLICATION_OCTET_STREAM;

                ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .contentType(contentType);

                if (asset.contentLength() >= 0) {
                    builder.contentLength(asset.contentLength());
                }
                return builder.body(asset.content());
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
