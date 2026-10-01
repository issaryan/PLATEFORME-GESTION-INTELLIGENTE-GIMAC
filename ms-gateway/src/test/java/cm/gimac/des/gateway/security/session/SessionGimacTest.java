package cm.gimac.des.gateway.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.port.FournisseurIdentite;
import cm.gimac.des.gateway.test.util.HorlogeReglable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import reactor.core.publisher.Mono;

/** Règles des sessions (D-65 §2 et §6) : échéances, appels de fond, chemins restreints, fermeture. */
class SessionGimacTest {

    static final Instant T0 = Instant.parse("2026-09-30T08:00:00Z");
    static final ParametresSession P = new ParametresSession(Duration.ofMinutes(30), Duration.ofHours(10));

    static MockWebSession geree(Instant debut, Instant activite, boolean restreinte) {
        MockWebSession s = new MockWebSession();
        s.getAttributes().put(SessionGimac.DEBUT, debut);
        if (activite != null) {
            s.getAttributes().put(SessionGimac.DERNIERE_ACTIVITE, activite);
        }
        s.getAttributes().put(SessionGimac.RESTREINTE, restreinte);
        return s;
    }

    @Test
    @DisplayName("Paramètres : inactivité positive, durée maximale au moins égale")
    void parametres() {
        assertThatThrownBy(() -> new ParametresSession(Duration.ZERO, Duration.ofHours(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametresSession(null, Duration.ofHours(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametresSession(Duration.ofMinutes(-1), Duration.ofHours(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametresSession(Duration.ofMinutes(30), Duration.ofMinutes(10))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametresSession(Duration.ofMinutes(30), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Échéance : dernière activité + 30 min, bornée par début + 10 h ; hors session gérée : aucune")
    void echeance() {
        assertThat(SessionGimac.expireA(new MockWebSession(), P)).isEmpty();
        assertThat(SessionGimac.expiree(new MockWebSession(), T0, P)).isFalse();
        MockWebSession s = geree(T0, null, false);
        assertThat(SessionGimac.expireA(s, P)).contains(T0.plus(Duration.ofMinutes(30)));
        s.getAttributes().put(SessionGimac.DERNIERE_ACTIVITE, T0.plus(Duration.ofHours(9).plusMinutes(45)));
        assertThat(SessionGimac.expireA(s, P)).contains(T0.plus(Duration.ofHours(10)));
        assertThat(SessionGimac.expiree(s, T0.plus(Duration.ofHours(10)), P)).isTrue();
        assertThat(SessionGimac.expiree(s, T0.plus(Duration.ofHours(9)), P)).isFalse();
    }

    @Test
    @DisplayName("Prolongation : seulement pour une session gérée")
    void prolonger() {
        MockWebSession s = geree(T0, T0, false);
        SessionGimac.prolonger(s, T0.plusSeconds(60));
        assertThat((Instant) s.getAttribute(SessionGimac.DERNIERE_ACTIVITE)).isEqualTo(T0.plusSeconds(60));
        MockWebSession autre = new MockWebSession();
        SessionGimac.prolonger(autre, T0);
        assertThat(autre.getAttributes()).isEmpty();
        assertThat(SessionGimac.restreinte(autre)).isFalse();
        assertThat(SessionGimac.restreinte(geree(T0, T0, true))).isTrue();
    }

    @Test
    @DisplayName("Appels de fond : X-Gimac-Fond: 1 et canal poussé")
    void fond() {
        assertThat(SessionGimac.appelDeFond("/api/session", "1")).isTrue();
        assertThat(SessionGimac.appelDeFond("/bff/des/live", null)).isTrue();
        assertThat(SessionGimac.appelDeFond("/bff/des/live/autre", null)).isFalse();
        assertThat(SessionGimac.appelDeFond("/api/session", "0")).isFalse();
        assertThat(SessionGimac.appelDeFond("/api/session", null)).isFalse();
    }

    @Test
    @DisplayName("Session restreinte : chemins de l'assistant, session, prolongation, déconnexion, connexion")
    void cheminsRestreints() {
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.GET, "/bff/des/iam/moi")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.POST, "/bff/portail/iam/moi/premiere-configuration")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.POST, "/bff/console/iam/moi/mot-de-passe")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.POST, "/bff/des/iam/moi/second-facteur/totp")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.GET, "/bff/des/iam/moi/second-facteur")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.DELETE, "/bff/des/iam/moi/second-facteur")).isFalse();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.GET, "/bff/des/iam/moi/sessions")).isFalse();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.GET, "/bff/autre/iam/moi")).isFalse();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.GET, "/api/session")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.POST, "/api/session")).isFalse();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.POST, "/api/session/prolongation")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.POST, "/api/logout")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.GET, "/api/logout")).isFalse();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.POST, "/api/connexion")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.POST, "/api/connexion/mot-de-passe-oublie")).isTrue();
        assertThat(SessionGimac.admisEnSessionRestreinte(HttpMethod.GET, "/api/account")).isFalse();
    }

    @Test
    @DisplayName("Registre : sessions suivies, identifiants expirés retenus puis oubliés")
    void registre() {
        RegistreSessions r = new RegistreSessions(Duration.ofHours(1));
        MockWebSession s = new MockWebSession();
        r.enregistrer(s);
        assertThat(r.sessions()).containsExactly(s);
        r.retirer(s);
        assertThat(r.sessions()).isEmpty();
        r.marquerExpiree("abc");
        assertThat(r.estExpiree("abc")).isTrue();
        assertThat(r.estExpiree(null)).isFalse();
        r.oublierExpiree("abc");
        r.oublierExpiree(null);
        assertThat(r.estExpiree("abc")).isFalse();
    }

    @Test
    @DisplayName("Problème : corps RFC 9457 avec code ; échappement JSON")
    void probleme() {
        Map<String, Object> c = ReponseProbleme.corps(CauseEchec.SESSION_EXPIREE, "/api/x");
        assertThat(c).containsEntry("status", 401).containsEntry("code", "CNX_SESSION_EXPIREE").containsEntry("title", "Unauthorized");
        assertThat(ReponseProbleme.echapper("a\"b\\c\nd")).isEqualTo("a\\\"b\\\\c\\u000ad");
        var e = MockServerWebExchange.from(MockServerHttpRequest.get("/api/\"x\""));
        ReponseProbleme.ecrire(e, CauseEchec.SESSION_RESTREINTE).block();
        assertThat(e.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(e.getResponse().getBodyAsString().block()).contains("\"code\":\"CNX_SESSION_RESTREINTE\"").contains("\"status\":403");
    }

    @Test
    @DisplayName("Garde : sondes publiques servies sans lire la session")
    void sondes() {
        assertThat(GardeSessionWebFilter.sondePublique("/management/health")).isTrue();
        assertThat(GardeSessionWebFilter.sondePublique("/management/health/readiness")).isTrue();
        assertThat(GardeSessionWebFilter.sondePublique("/management/info")).isTrue();
        assertThat(GardeSessionWebFilter.sondePublique("/management/env")).isFalse();
        FermetureSession f = mock(FermetureSession.class);
        GardeSessionWebFilter g = new GardeSessionWebFilter(f, new RegistreSessions(Duration.ofHours(1)), P, new HorlogeReglable(T0), "SESSION");
        assertThat(g.getOrder()).isEqualTo(GardeSessionWebFilter.ORDRE);
        boolean[] transmis = { false };
        g.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/management/health")), e -> {
            transmis[0] = true;
            return Mono.empty();
        }).block();
        assertThat(transmis[0]).isTrue();
    }

    @Test
    @DisplayName("Fermeture : révocation incomplète sans échec ; session sans jetons simplement invalidée")
    void fermeture() {
        ReactiveOAuth2AuthorizedClientService clients = mock(ReactiveOAuth2AuthorizedClientService.class);
        when(clients.loadAuthorizedClient(anyString(), anyString())).thenReturn(Mono.error(new IllegalStateException("panne")));
        FournisseurIdentite fi = mock(FournisseurIdentite.class);
        RegistreSessions r = new RegistreSessions(Duration.ofHours(1));
        FermetureSession f = new FermetureSession(clients, fi, r, P, new HorlogeReglable(T0));
        MockWebSession s = geree(T0, T0, false);
        s.getAttributes().put(SessionGimac.ENREGISTREMENT, "oidc");
        s.getAttributes().put(SessionGimac.PRINCIPAL, "p");
        f.fermer(s, true).block();
        assertThat(s.isExpired()).isTrue();
        assertThat(r.estExpiree(s.getId())).isTrue();
        MockWebSession vide = new MockWebSession();
        vide.start();
        f.fermer(vide, false).block();
        assertThat(r.estExpiree(vide.getId())).isFalse();
        when(clients.loadAuthorizedClient(anyString(), anyString())).thenReturn(Mono.empty());
        when(clients.removeAuthorizedClient(anyString(), anyString())).thenReturn(Mono.empty());
        MockWebSession v = geree(T0, T0, false);
        v.getAttributes().put(SessionGimac.ENREGISTREMENT, "oidc");
        v.getAttributes().put(SessionGimac.PRINCIPAL, "p");
        f.vider(v).block();
        assertThat(v.getAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Balayage périodique : actif puis arrêté")
    void balayeur() throws InterruptedException {
        FermetureSession f = mock(FermetureSession.class);
        when(f.balayer()).thenReturn(Mono.just(1), Mono.error(new IllegalStateException("x")), Mono.just(0));
        BalayeurSessions b = new BalayeurSessions(f, Duration.ofMillis(20));
        assertThat(b.actif()).isTrue();
        Thread.sleep(150);
        b.close();
        assertThat(b.actif()).isFalse();
        org.mockito.Mockito.verify(f, org.mockito.Mockito.atLeast(2)).balayer();
    }
}
