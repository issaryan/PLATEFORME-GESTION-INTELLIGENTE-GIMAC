package cm.gimac.des.gateway.service.appareils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.domain.appareils.Appareil;
import cm.gimac.des.gateway.domain.appareils.ResultatTentative;
import cm.gimac.des.gateway.domain.appareils.TentativeConnexion;
import cm.gimac.des.gateway.domain.appareils.TypeAppareil;
import cm.gimac.des.gateway.domain.appareils.port.AlerteNouvelAppareil;
import cm.gimac.des.gateway.domain.appareils.port.HistoriqueConnexions;
import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.DemandeConnexion;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.domain.live.Couche;
import cm.gimac.des.gateway.infrastructure.appareils.HistoriqueConnexionsMemoire;
import cm.gimac.des.gateway.security.session.FermetureSession;
import cm.gimac.des.gateway.security.session.ParametresSession;
import cm.gimac.des.gateway.security.session.RegistreSessions;
import cm.gimac.des.gateway.security.session.SessionGimac;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Suivi des connexions, sessions du compte, purge de l'historique (docs/lot1/mon-compte.md §3). */
class SuiviEtServiceAppareilsTest {

    static final Instant T = Instant.parse("2026-10-01T08:00:00Z");
    static final String FIREFOX = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0";
    static final String IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 Version/17.1 Mobile/15E148 Safari/604.1";
    static final ParametresSession PARAMETRES = new ParametresSession(Duration.ofMinutes(30), Duration.ofHours(10));

    static DemandeConnexion demande(String ua) {
        return new DemandeConnexion("agent.sna", "secret", "123456", Couche.DES, "41.202.10.20", ua);
    }

    @Test
    @DisplayName("Réussite : tentative inscrite ; première connexion sans alerte ; nouvel appareil : alerte ; appareil connu : rien")
    void reussites() {
        HistoriqueConnexionsMemoire h = new HistoriqueConnexionsMemoire();
        AlerteNouvelAppareil alerte = mock(AlerteNouvelAppareil.class);
        when(alerte.alerter(anyString(), any(), anyString(), anyString(), any())).thenReturn(Mono.empty());
        SuiviConnexions s = new SuiviConnexions(h, alerte, Clock.fixed(T, ZoneOffset.UTC));
        s.reussie(demande(FIREFOX), "u1", true, null).block();
        s.reussie(demande(IPHONE), "u1", true, null).block();
        s.reussie(demande(FIREFOX.replace("131", "132")), "u1", true, null).block();
        // Session restreinte (premier accès) : inscrite, jamais d'alerte.
        s.reussie(demande("curl/8"), "u1", false, null).block();
        verify(alerte, timeout(2000)).alerter(eq("u1"), eq(new Appareil("Safari 17", "iOS 17", TypeAppareil.MOBILE)), eq("41.202.10.20"), eq("des"), eq(T));
        verify(alerte, timeout(500).times(1)).alerter(anyString(), any(), anyString(), anyString(), any());
        HistoriqueConnexions.Page p = h.lire("u1", Set.of(), 0, 10).block();
        assertThat(p.total()).isEqualTo(4);
        assertThat(p.contenu()).allMatch(t -> t.resultat() == ResultatTentative.REUSSIE && t.empreinteIdentifiant().equals(SuiviConnexions.empreinte("Agent.SNA ")));
    }

    @Test
    @DisplayName("Échecs : mot de passe (sans sujet), code (avec sujet), blocage ; requête invalide non inscrite")
    void echecs() {
        HistoriqueConnexionsMemoire h = new HistoriqueConnexionsMemoire();
        SuiviConnexions s = new SuiviConnexions(h, null, Clock.fixed(T, ZoneOffset.UTC));
        s.echouee(demande(FIREFOX), new EchecConnexion(CauseEchec.IDENTIFIANTS_INVALIDES)).block();
        s.echouee(demande(FIREFOX), new EchecConnexion(CauseEchec.IDENTIFIANTS_INVALIDES, "code", "u1", true)).block();
        s.echouee(demande(FIREFOX), new EchecConnexion(CauseEchec.TROP_DE_TENTATIVES)).block();
        s.echouee(demande(FIREFOX), new EchecConnexion(CauseEchec.REQUETE_INVALIDE)).block();
        s.echouee(new DemandeConnexion(null, null, null, null, null, null), new EchecConnexion(CauseEchec.TROP_DE_TENTATIVES)).block();
        List<TentativeConnexion> t = h.lire("u1", SuiviConnexions.empreintes("agent.sna", null, " "), 0, 10).block().contenu();
        assertThat(t).extracting(TentativeConnexion::resultat).containsExactlyInAnyOrder(ResultatTentative.ECHEC_MOT_DE_PASSE, ResultatTentative.ECHEC_CODE, ResultatTentative.BLOQUEE);
        assertThat(h.lire(null, null, 0, 10).block().total()).isZero();
        // Suivi désactivé : rien.
        SuiviConnexions.aucun().echouee(demande(FIREFOX), new EchecConnexion(CauseEchec.TROP_DE_TENTATIVES)).block();
        SuiviConnexions.aucun().reussie(demande(FIREFOX), "u1", true, null).block();
        assertThat(new EchecConnexion(CauseEchec.IDENTIFIANTS_INVALIDES, "x", "u2", false).pour("u3").sujet()).isEqualTo("u2");
    }

    @Test
    @DisplayName("Base en panne ou alerte refusée : la connexion n'est ni bloquée ni en échec")
    void pannes() {
        HistoriqueConnexions h = mock(HistoriqueConnexions.class);
        when(h.appareilsConnus(anyString())).thenReturn(Flux.just(new Appareil("Chrome 1", "Linux", TypeAppareil.ORDINATEUR)));
        when(h.enregistrer(any())).thenReturn(Mono.error(new IllegalStateException("base indisponible")), Mono.empty());
        AlerteNouvelAppareil alerte = mock(AlerteNouvelAppareil.class);
        when(alerte.alerter(anyString(), any(), anyString(), anyString(), any())).thenReturn(Mono.error(new IllegalStateException("MS-IAM 503")));
        SuiviConnexions s = new SuiviConnexions(h, alerte, Clock.fixed(T, ZoneOffset.UTC));
        s.reussie(demande(FIREFOX), "u1", true, null).block();
        verify(alerte, never()).alerter(anyString(), any(), anyString(), anyString(), any());
        s.reussie(demande(FIREFOX), "u1", true, null).block();
        verify(alerte, timeout(2000)).alerter(anyString(), any(), anyString(), anyString(), any());
        when(h.enregistrer(any())).thenReturn(Mono.never());
        long debut = System.nanoTime();
        s.echouee(demande(FIREFOX), new EchecConnexion(CauseEchec.IDENTIFIANTS_INVALIDES)).block();
        assertThat(Duration.ofNanos(System.nanoTime() - debut)).isLessThan(Duration.ofSeconds(5));
    }

    WebSession session(MockServerWebExchange e, String sujet, Instant debut) {
        WebSession s = e.getSession().block();
        s.getAttributes().put(SessionGimac.DEBUT, debut);
        s.getAttributes().put(SessionGimac.DERNIERE_ACTIVITE, debut);
        SessionGimac.decrire(s, sujet, "Firefox 131", "Windows 10/11", "ORDINATEUR", "41.202.10.20", "des");
        return s;
    }

    @Test
    @DisplayName("Sessions du compte : échues et étrangères écartées, courante d'abord, adresse masquée hors courante ; description par la connexion")
    void sessions() {
        RegistreSessions registre = new RegistreSessions(Duration.ofHours(10));
        FermetureSession fermeture = mock(FermetureSession.class);
        when(fermeture.fermer(any(), eq(false))).thenReturn(Mono.empty());
        ServiceAppareils service = new ServiceAppareils(registre, fermeture, PARAMETRES, new HistoriqueConnexionsMemoire(), Clock.fixed(T, ZoneOffset.UTC));
        MockServerWebExchange e1 = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        MockServerWebExchange e2 = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        MockServerWebExchange e3 = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        MockServerWebExchange e4 = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        WebSession courante = session(e1, "u1", T.minusSeconds(60));
        WebSession autre = session(e2, "u1", T.minusSeconds(120));
        WebSession echue = session(e3, "u1", T.minus(Duration.ofHours(11)));
        WebSession etrangere = session(e4, "u2", T);
        List.of(courante, autre, echue, etrangere).forEach(registre::enregistrer);
        var vues = service.sessions("u1", courante);
        assertThat(vues).hasSize(2);
        assertThat(vues.get(0).courante()).isTrue();
        assertThat(vues.get(0).adresseIp()).isEqualTo("41.202.10.20");
        assertThat(vues.get(1).adresseIp()).isEqualTo("41.202.10.*");
        assertThat(vues.get(1).ouverteLe()).isEqualTo(T.minusSeconds(120));
        assertThat(vues.get(1).expireA()).isEqualTo(T.minusSeconds(120).plus(Duration.ofMinutes(30)));
        assertThat(service.sessions(null, courante)).isEmpty();
        assertThat(service.fermerAutres("u1", courante).block()).isEqualTo(2);
        verify(fermeture).fermer(autre, false);
        verify(fermeture, never()).fermer(courante, false);
        assertThat(service.fermerAutres("u3", null).block()).isZero();
        // Description par le suivi : seulement une session de la plateforme (gérée).
        MockServerWebExchange e5 = MockServerWebExchange.builder(MockServerHttpRequest.get("/")).build();
        new SuiviConnexions(null, null, Clock.systemUTC()).reussie(demande(IPHONE), "u5", true, e5).block();
        assertThat(SessionGimac.texte(e5.getSession().block(), SessionGimac.SUJET)).isNull();
        e5.getSession().block().getAttributes().put(SessionGimac.DEBUT, T);
        new SuiviConnexions(null, null, Clock.systemUTC()).reussie(demande(IPHONE), "u5", true, e5).block();
        assertThat(SessionGimac.texte(e5.getSession().block(), SessionGimac.SUJET)).isEqualTo("u5");
        assertThat(SessionGimac.texte(e5.getSession().block(), SessionGimac.TYPE_APPAREIL)).isEqualTo("MOBILE");
    }

    @Test
    @DisplayName("Historique : bornes de page et de taille ; base en panne : 503 CNX_HISTORIQUE_INDISPONIBLE")
    void historique() {
        HistoriqueConnexions h = mock(HistoriqueConnexions.class);
        when(h.lire(eq("u1"), any(), eq(0), eq(100))).thenReturn(Mono.just(new HistoriqueConnexions.Page(List.of(), 0)));
        when(h.lire(eq("u1"), any(), eq(2), eq(1))).thenReturn(Mono.error(new IllegalStateException("base")));
        when(h.lire(eq("u1"), any(), eq(3), eq(1))).thenReturn(Mono.error(new EchecConnexion(CauseEchec.APPAREIL_INCONNU)));
        ServiceAppareils service = new ServiceAppareils(new RegistreSessions(Duration.ofHours(1)), mock(FermetureSession.class), PARAMETRES, h, Clock.systemUTC());
        assertThat(service.historique("u1", Set.of(), -1, 500).block().total()).isZero();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.historique("u1", Set.of(), 2, 0).block()).isInstanceOfSatisfying(EchecConnexion.class, e ->
            assertThat(e.causeEchec()).isEqualTo(CauseEchec.HISTORIQUE_INDISPONIBLE)
        );
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.historique("u1", Set.of(), 3, 1).block()).isInstanceOfSatisfying(EchecConnexion.class, e ->
            assertThat(e.causeEchec()).isEqualTo(CauseEchec.APPAREIL_INCONNU)
        );
    }

    @Test
    @DisplayName("Rétention : tentatives de plus de 90 jours supprimées ; purge périodique arrêtée à la fermeture")
    void purge() throws Exception {
        HistoriqueConnexionsMemoire h = new HistoriqueConnexionsMemoire();
        Appareil a = Appareil.inconnu();
        h.enregistrer(new TentativeConnexion(T.minus(Duration.ofDays(91)), "u1", null, ResultatTentative.REUSSIE, a, null, null)).block();
        h.enregistrer(new TentativeConnexion(T.minus(Duration.ofDays(89)), "u1", null, ResultatTentative.REUSSIE, a, null, null)).block();
        try (PurgeHistorique p = new PurgeHistorique(h, Duration.ofDays(90), Duration.ofHours(1), Clock.fixed(T, ZoneOffset.UTC))) {
            assertThat(p.actif()).isTrue();
            assertThat(p.purger().block()).isEqualTo(1L);
            assertThat(p.purger().block()).isZero();
            assertThat(h.lire("u1", null, 0, 10).block().total()).isEqualTo(1);
            assertThat(h.appareilsConnus("u1").collectList().block()).containsExactly(a);
        }
        HistoriqueConnexions enPanne = mock(HistoriqueConnexions.class);
        when(enPanne.purger(any())).thenReturn(Mono.error(new IllegalStateException("base")));
        PurgeHistorique p = new PurgeHistorique(enPanne, Duration.ofDays(90), Duration.ofMillis(20), Clock.systemUTC());
        verify(enPanne, timeout(2000).atLeast(2)).purger(any());
        assertThat(p.actif()).isTrue();
        p.close();
        assertThat(p.actif()).isFalse();
    }
}
