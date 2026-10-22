package cm.gimac.des.participant.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.web.rest.lot1.RegistreSandboxInterneResource;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Journal des tentatives refusées des comptes de service de la sandbox (US-EXI-21, critère 3). */
@ExtendWith(OutputCaptureExtension.class)
class JournalRefusSandboxTest {

    final JournalRefusSandbox journal = new JournalRefusSandbox();

    static Authentication jeton(String cle, String client) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("service").issuedAt(Instant.EPOCH);
        if (cle != null) {
            b.claim(cle, client);
        }
        return new JwtAuthenticationToken(b.build(), List.of());
    }

    static AuthorizationDeniedEvent<Object> refus(Authentication a, Object cible) {
        return new AuthorizationDeniedEvent<>(() -> a, cible, new AuthorizationDecision(false));
    }

    @Test
    @DisplayName("Critère 3 : tentative d'écriture du compte de service sbx-espace journalisée avec la méthode et le chemin")
    void tentativeEcriture(CapturedOutput sortie) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/v1/institutions/i1/transitions");
        journal.refus(refus(jeton("azp", "sbx-espace"), r));
        assertThat(sortie.getOut()).contains(JournalRefusSandbox.EVENEMENT, "client=sbx-espace", "cible=POST /api/v1/institutions/i1/transitions");
    }

    @Test
    @DisplayName("Client lu dans client_id à défaut d'azp ; refus d'une méthode protégée")
    void clientIdEtMethode(CapturedOutput sortie) throws Exception {
        MethodInvocation m = mock(MethodInvocation.class);
        Method methode = RegistreSandboxInterneResource.class.getMethod("fiche", String.class);
        when(m.getMethod()).thenReturn(methode);
        journal.refus(refus(jeton("client_id", "sbx-referentiel"), m));
        assertThat(sortie.getOut()).contains("client=sbx-referentiel", "cible=RegistreSandboxInterneResource.fiche");
        journal.refus(refus(jeton("azp", "sbx-raccordement"), "autre"));
        assertThat(sortie.getOut()).contains("cible=autre");
    }

    @Test
    @DisplayName("Refus hors sandbox non journalisés ici : autre client, jeton sans client, authentification non JWT ou absente")
    void horsSandbox(CapturedOutput sortie) {
        journal.refus(refus(jeton("azp", "web_app"), "x"));
        journal.refus(refus(jeton(null, null), "x"));
        journal.refus(refus(new TestingAuthenticationToken("u", "p"), "x"));
        journal.refus(new AuthorizationDeniedEvent<>(() -> null, "x", new AuthorizationDecision(false)));
        assertThat(JournalRefusSandbox.client(null)).isNull();
        assertThat(sortie.getOut()).doesNotContain(JournalRefusSandbox.EVENEMENT);
    }
}
