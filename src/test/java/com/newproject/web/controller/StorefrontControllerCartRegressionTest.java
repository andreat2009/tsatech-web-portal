package com.newproject.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newproject.web.dto.Cart;
import com.newproject.web.dto.CartItem;
import com.newproject.web.dto.Product;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.mock.web.MockHttpSession;

@ExtendWith(MockitoExtension.class)
class StorefrontControllerCartRegressionTest {

    @Mock
    private GatewayClient gatewayClient;

    @Mock
    private CustomerResolver customerResolver;

    private StorefrontController controller;

    @BeforeEach
    void setUp() {
        controller = new StorefrontController(gatewayClient, customerResolver, "EUR", "", new StaticMessageSource());
    }

    @Test
    void viewCartDoesNotMergeGuestCartIntoAdminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("GUEST_CART_ITEMS", Map.of(1008L, 2));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN");
        authentication.setAuthenticated(true);

        Cart cart = new Cart();
        cart.setId(3L);
        cart.setCustomerId(1L);
        cart.setStatus("OPEN");
        cart.setUpdatedAt(OffsetDateTime.now());

        when(customerResolver.resolveCustomerId(authentication)).thenReturn(1L);
        when(gatewayClient.listCarts(1L)).thenReturn(List.of(cart));
        when(gatewayClient.listCartItems(3L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.viewCart(model, authentication, session);

        assertThat(view).isEqualTo("cart/view");
        verify(gatewayClient, never()).addCartItem(anyLong(), any());
    }

    @Test
    void viewCartUsesMostRecentlyUpdatedOpenCart() {
        MockHttpSession session = new MockHttpSession();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "n/a", "ROLE_USER");
        authentication.setAuthenticated(true);

        Cart oldCart = new Cart();
        oldCart.setId(1L);
        oldCart.setCustomerId(2L);
        oldCart.setStatus("OPEN");
        oldCart.setUpdatedAt(OffsetDateTime.now().minusDays(1));

        Cart newestCart = new Cart();
        newestCart.setId(2L);
        newestCart.setCustomerId(2L);
        newestCart.setStatus("OPEN");
        newestCart.setUpdatedAt(OffsetDateTime.now());

        CartItem item = new CartItem();
        item.setId(99L);
        item.setProductId(1008L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("2.00"));

        Product product = new Product();
        product.setId(1008L);
        product.setName("Prodotto di test");

        when(customerResolver.resolveCustomerId(authentication)).thenReturn(2L);
        when(gatewayClient.listCarts(2L)).thenReturn(List.of(oldCart, newestCart));
        when(gatewayClient.listCartItems(2L)).thenReturn(List.of(item));
        when(gatewayClient.getProductSafe(1008L)).thenReturn(Optional.of(product));

        Model model = new ExtendedModelMap();
        String view = controller.viewCart(model, authentication, session);

        assertThat(view).isEqualTo("cart/view");
        assertThat(model.getAttribute("items")).asList().hasSize(1);
        verify(gatewayClient).listCartItems(2L);
        verify(gatewayClient, never()).listCartItems(1L);
    }

    @Test
    void guestCartSummaryPrunesMissingProductsFromSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("GUEST_CART_ITEMS", new java.util.LinkedHashMap<>(Map.of(3L, 1, 1008L, 2)));

        Product product = new Product();
        product.setId(1008L);
        product.setName("Prodotto di test");
        product.setPrice(new BigDecimal("5.00"));

        when(customerResolver.resolveCustomerId(null)).thenReturn(null);
        when(gatewayClient.getProductSafe(3L)).thenReturn(Optional.empty());
        when(gatewayClient.getProductSafe(1008L)).thenReturn(Optional.of(product));

        Model model = new ExtendedModelMap();
        String view = controller.viewCart(model, null, session);

        assertThat(view).isEqualTo("cart/view");
        @SuppressWarnings("unchecked")
        Map<String, Integer> guestCart = (Map<String, Integer>) session.getAttribute("GUEST_CART_ITEMS");
        assertThat(guestCart).containsExactlyEntriesOf(Map.of("1008", 2));
        assertThat(model.getAttribute("items")).asList().hasSize(1);
    }
}
