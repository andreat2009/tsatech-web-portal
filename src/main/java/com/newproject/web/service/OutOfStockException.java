package com.newproject.web.service;

/**
 * Sollevata da {@link GatewayClient#reserveStock} quando l'inventory-service risponde 409:
 * almeno una riga del carrello non ha stock sufficiente.
 */
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}
