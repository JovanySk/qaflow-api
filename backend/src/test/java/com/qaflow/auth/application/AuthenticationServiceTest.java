package com.qaflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.auth.api.LoginRequest;
import com.qaflow.security.JwtTokenService;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.security.UserAccountDetails;
import com.qaflow.shared.error.InvalidCredentialsException;
import com.qaflow.shared.web.RequestMetadata;
import com.qaflow.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

class AuthenticationServiceTest {

    private AuthenticationManager authenticationManager;
    private JwtTokenService jwtTokenService;
    private AuditEventRecorder auditEventRecorder;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtTokenService = mock(JwtTokenService.class);
        auditEventRecorder = mock(AuditEventRecorder.class);
        service = new AuthenticationService(authenticationManager, jwtTokenService, auditEventRecorder);
    }

    @Test
    void issuesTokenAndAuditsSuccessfulLogin() {
        QaFlowPrincipal principal = new QaFlowPrincipal(7, "operator.demo", "Demo Operator", Role.OPERATOR, 3);
        UserAccountDetails details = mock(UserAccountDetails.class);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(details);
        when(details.principal()).thenReturn(principal);
        when(jwtTokenService.issue(principal)).thenReturn(new JwtTokenService.IssuedToken("signed.jwt", 900));

        var response = service.login(
                new LoginRequest(" operator.demo ", "Demo123!"),
                new RequestMetadata("/api/v1/auth/login", "unit-login-001"));

        assertThat(response.accessToken()).isEqualTo("signed.jwt");
        assertThat(response.tokenType()).isEqualTo("BEARER");
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        assertThat(response.user().role()).isEqualTo(Role.OPERATOR);
        ArgumentCaptor<AuditEventCommand> event = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditEventRecorder).recordWithinBusinessTransaction(event.capture());
        assertThat(event.getValue().action()).isEqualTo("LOGIN_SUCCEEDED");
        assertThat(event.getValue().outcome()).isEqualTo(AuditOutcome.SUCCESS);
    }

    @Test
    void returnsGenericErrorAndAuditsRejectedCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("internal detail"));

        assertThatThrownBy(() -> service.login(
                new LoginRequest("missing.user", "Wrong123!"),
                new RequestMetadata("/api/v1/auth/login", "unit-login-002")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Las credenciales no son válidas.")
                .hasMessageNotContaining("internal detail");

        ArgumentCaptor<AuditEventCommand> event = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditEventRecorder).recordInIsolatedTransaction(event.capture());
        assertThat(event.getValue().action()).isEqualTo("LOGIN_REJECTED");
        assertThat(event.getValue().actorUserId()).isNull();
        assertThat(event.getValue().summary()).containsEntry("reason", "INVALID_CREDENTIALS");
    }
}
