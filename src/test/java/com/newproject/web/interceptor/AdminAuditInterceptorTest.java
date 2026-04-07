package com.newproject.web.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.newproject.web.dto.AdminAuditEventRequest;
import com.newproject.web.service.GatewayClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminAuditInterceptorTest {

    @Mock
    private GatewayClient gatewayClient;

    @InjectMocks
    private AdminAuditInterceptor interceptor;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAdminMutationAsAuditEvent() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a", "ROLE_ADMIN"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/orders/42/status");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(302);

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AdminAuditEventRequest> captor = ArgumentCaptor.forClass(AdminAuditEventRequest.class);
        verify(gatewayClient).recordAdminAuditEvent(captor.capture());
        AdminAuditEventRequest event = captor.getValue();
        assertThat(event.getActorUsername()).isEqualTo("admin");
        assertThat(event.getActionType()).isEqualTo("STATE_CHANGE");
        assertThat(event.getTargetType()).isEqualTo("ORDERS");
        assertThat(event.getTargetId()).isEqualTo("42");
        assertThat(event.getOutcome()).isEqualTo("SUCCESS");
    }

    @Test
    void ignoresAnonymousTraffic() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/orders/42/status");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, new Object(), null);

        verifyNoInteractions(gatewayClient);
    }
}
