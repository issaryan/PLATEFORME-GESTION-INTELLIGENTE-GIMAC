package cm.gimac.des.gateway.web.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class OAuth2ReactiveRefreshTokensWebFilterTest {

    @Test
    void shouldNotRefreshTokenWhenLoggingOut() {
        ReactiveOAuth2AuthorizedClientManager clientManager = mock(ReactiveOAuth2AuthorizedClientManager.class);
        WebFilterChain chain = mock(WebFilterChain.class);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/logout").build());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        new OAuth2ReactiveRefreshTokensWebFilter(clientManager).filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verifyNoInteractions(clientManager);
    }

    static org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken auth() {
        var id = new org.springframework.security.oauth2.core.oidc.OidcIdToken(
            "t", java.time.Instant.now(), java.time.Instant.now().plusSeconds(60), java.util.Map.of("sub", "u"));
        return new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
            new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(java.util.List.of(), id), java.util.List.of(), "oidc");
    }

    @Test
    void rafraichissementReussi() {
        ReactiveOAuth2AuthorizedClientManager clientManager = mock(ReactiveOAuth2AuthorizedClientManager.class);
        when(clientManager.authorize(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/session").build()).mutate().principal(Mono.just(auth())).build();
        boolean[] transmis = { false };
        new OAuth2ReactiveRefreshTokensWebFilter(clientManager).filter(exchange, e -> {
            transmis[0] = true;
            return Mono.empty();
        }).block();
        org.assertj.core.api.Assertions.assertThat(transmis[0]).isTrue();
    }

    @Test
    void rafraichissementRefuseFermeLaSession() {
        ReactiveOAuth2AuthorizedClientManager clientManager = mock(ReactiveOAuth2AuthorizedClientManager.class);
        when(clientManager.authorize(org.mockito.ArgumentMatchers.any())).thenReturn(
            Mono.error(new org.springframework.security.oauth2.core.OAuth2AuthorizationException(
                new org.springframework.security.oauth2.core.OAuth2Error("invalid_grant")))
        );
        var session = new org.springframework.mock.web.server.MockWebSession();
        var fermeture = mock(cm.gimac.des.gateway.security.session.FermetureSession.class);
        when(fermeture.fermer(session, true)).thenReturn(Mono.empty());
        @SuppressWarnings("unchecked")
        org.springframework.beans.factory.ObjectProvider<cm.gimac.des.gateway.security.session.FermetureSession> fournisseur = mock(
            org.springframework.beans.factory.ObjectProvider.class);
        when(fournisseur.getIfAvailable()).thenReturn(fermeture);
        var base = MockServerWebExchange.builder(MockServerHttpRequest.get("/bff/des/x")).session(session).build();
        var exchange = base.mutate().principal(Mono.just(auth())).build();
        boolean[] transmis = { false };
        new OAuth2ReactiveRefreshTokensWebFilter(clientManager, fournisseur).filter(exchange, e -> {
            transmis[0] = true;
            return Mono.empty();
        }).block();
        org.assertj.core.api.Assertions.assertThat(transmis[0]).isFalse();
        org.assertj.core.api.Assertions.assertThat(base.getResponse().getStatusCode().value()).isEqualTo(401);
        org.assertj.core.api.Assertions.assertThat(base.getResponse().getBodyAsString().block()).contains("CNX_SESSION_EXPIREE");
        verify(fermeture).fermer(session, true);
        // Sans fermeture disponible : session invalidée.
        var s2 = new org.springframework.mock.web.server.MockWebSession();
        var b2 = MockServerWebExchange.builder(MockServerHttpRequest.get("/bff/des/x")).session(s2).build();
        new OAuth2ReactiveRefreshTokensWebFilter(clientManager).filter(b2.mutate().principal(Mono.just(auth())).build(), e -> Mono.empty()).block();
        org.assertj.core.api.Assertions.assertThat(s2.isExpired()).isTrue();
    }
}
