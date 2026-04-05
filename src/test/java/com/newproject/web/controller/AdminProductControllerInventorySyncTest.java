package com.newproject.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newproject.web.dto.InventoryRequest;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductRequest;
import com.newproject.web.service.GatewayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminProductControllerInventorySyncTest {

    @Mock
    private GatewayClient gatewayClient;

    private AdminProductController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminProductController(gatewayClient);
    }

    @Test
    void createSyncsInventoryWithAdminQuantity() {
        ProductRequest request = new ProductRequest();
        request.setSku("SKU-1008");
        request.setName("Prodotto di test");
        request.setQuantity(20);

        Product created = new Product();
        created.setId(1008L);

        when(gatewayClient.createProduct(any(ProductRequest.class))).thenReturn(created);

        String redirect = controller.create(request, null, null, null, false, false);

        ArgumentCaptor<InventoryRequest> inventoryCaptor = ArgumentCaptor.forClass(InventoryRequest.class);
        verify(gatewayClient).upsertInventory(eq(1008L), inventoryCaptor.capture());
        assertThat(redirect).isEqualTo("redirect:/admin/catalogo/prodotti/1008/modifica");
        assertThat(inventoryCaptor.getValue().getProductId()).isEqualTo(1008L);
        assertThat(inventoryCaptor.getValue().getOnHand()).isEqualTo(20);
        assertThat(inventoryCaptor.getValue().getReserved()).isZero();
    }

    @Test
    void deleteAlsoRemovesInventoryRow() {
        String redirect = controller.delete(1008L);

        verify(gatewayClient).deleteProduct(1008L);
        verify(gatewayClient).deleteInventory(1008L);
        assertThat(redirect).isEqualTo("redirect:/admin/catalogo/prodotti");
    }
}
