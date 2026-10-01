package cm.gimac.des.gateway.infrastructure.connexion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.audit.GatewayAuditEventPublisher;
import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import cm.gimac.des.gateway.domain.connexion.ClientConnexion;
import cm.gimac.des.gateway.domain.connexion.DemandeConnexion;
import cm.gimac.des.gateway.domain.connexion.EchecConnexion;
import cm.gimac.des.gateway.domain.connexion.Jetons;
import cm.gimac.des.gateway.domain.live.Couche;
import cm.gimac.des.gateway.ratelimit.TokenBucketRateLimiter;
import cm.gimac.des.gateway.test.util.HorlogeReglable;
import cm.gimac.des.gateway.test.util.KeycloakSimule;
import cm.gimac.des.gateway.test.util.ServeurHttpLocal;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Adaptateurs de la connexion (D-65) : Keycloak (octroi direct, validation des jetons par le JWKS,
 * révocation), MS-IAM, limitation (mémoire, Valkey avec secours), journal d'audit, jeton de service.
 */
// Premier cas de la classe : chargement de Nimbus et des clés RSA du Keycloak simulé, lent sur une machine chargée.
@org.junit.jupiter.api.Timeout(60)
class AdaptateursConnexionTest {

    static final KeycloakSimule KC = new KeycloakSimule();

    static ClientRegistration.Builder base(String id, String client, String secret) {
        String p = KC.emetteur() + "/protocol/openid-connect";
        return ClientRegistration.withRegistrationId(id)
            .clientId(client)
            .clientSecret(secret)
            .issuerUri(KC.emetteur())
            .tokenUri(p + "/token")
            .jwkSetUri(p + "/certs")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scope("openid");
    }

    final KeycloakFournisseurIdentite fournisseur = new KeycloakFournisseurIdentite(
        new InMemoryReactiveClientRegistrationRepository(
            base("connexion", "gimac-connexion", "secret-connexion").build(),
            base("oidc", "web_app", "mauvais-secret").build()
        ),
        WebClient.create(),
        Duration.ofSeconds(3)
    );

    @AfterAll
    static void arreter() {
        KC.close();
    }

    @BeforeEach
    void comptes() {
        KC.reinitialiser();
        KC.compte("agent", KeycloakSimule.interne("u-1", "ROLE_SNA"));
    }

    static void echec(Mono<?> m, CauseEchec cause) {
        StepVerifier.create(m).expectErrorSatisfies(e -> assertThat(((EchecConnexion) e).causeEchec()).isEqualTo(cause)).verify();
    }

    @Test
    @DisplayName("Keycloak : octroi puis validation (JWKS, émetteur, audience, sujets concordants)")
    void octroiEtValidation() {
        Jetons j = fournisseur.octroyer(ClientConnexion.MOT_DE_PASSE, "agent", "Mot-de-passe-1", " ").block();
        assertThat(j.jetonRafraichissement()).startsWith("rt-");
        assertThat(j.dureeAcces()).isEqualTo(300);
        assertThat(KC.octrois().get(0).avecCode()).isFalse();
        var identite = fournisseur.valider(ClientConnexion.MOT_DE_PASSE, j).block();
        assertThat(identite.sujet()).isEqualTo("u-1");
        assertThat(identite.texte("acr")).isEqualTo("1");
        assertThat(identite.claimsAcces()).containsKey("roles");
    }

    @Test
    @DisplayName("Keycloak : secret du client refusé (invalid_client) ou Keycloak injoignable : SERVICE_INDISPONIBLE")
    void indisponible() {
        echec(fournisseur.octroyer(ClientConnexion.DEUX_FACTEURS, "agent", "Mot-de-passe-1", "123456"), CauseEchec.SERVICE_INDISPONIBLE);
        KeycloakFournisseurIdentite injoignable = new KeycloakFournisseurIdentite(
            new InMemoryReactiveClientRegistrationRepository(
                ClientRegistration.withRegistrationId("connexion")
                    .clientId("c")
                    .tokenUri("http://127.0.0.1:1/token")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .build()
            ),
            WebClient.create(),
            Duration.ofSeconds(2)
        );
        echec(injoignable.octroyer(ClientConnexion.MOT_DE_PASSE, "a", "b", null), CauseEchec.SERVICE_INDISPONIBLE);
        // Enregistrement absent.
        echec(injoignable.octroyer(ClientConnexion.DEUX_FACTEURS, "a", "b", null), CauseEchec.SERVICE_INDISPONIBLE);
        // La révocation n'échoue jamais.
        StepVerifier.create(injoignable.revoquer("connexion", "rt-x")).verifyComplete();
        StepVerifier.create(injoignable.revoquer("absent", "rt-x")).verifyComplete();
        StepVerifier.create(injoignable.revoquer("connexion", null)).verifyComplete();
        StepVerifier.create(injoignable.revoquer("connexion", " ")).verifyComplete();
    }

    @Test
    @DisplayName("Keycloak : traduction des refus (compte désactivé, identifiants, autres erreurs)")
    void traduction() {
        assertThat(KeycloakFournisseurIdentite.echec(400, Map.of("error", "invalid_grant", "error_description", "Account disabled")).causeEchec()).isEqualTo(
            CauseEchec.COMPTE_INACTIF
        );
        assertThat(KeycloakFournisseurIdentite.echec(401, Map.of("error", "invalid_grant", "error_description", "Invalid user credentials")).causeEchec()).isEqualTo(
            CauseEchec.IDENTIFIANTS_INVALIDES
        );
        assertThat(KeycloakFournisseurIdentite.echec(400, Map.of("error", "invalid_grant")).causeEchec()).isEqualTo(CauseEchec.IDENTIFIANTS_INVALIDES);
        assertThat(KeycloakFournisseurIdentite.echec(500, Map.of()).causeEchec()).isEqualTo(CauseEchec.SERVICE_INDISPONIBLE);
        // Octroi à deux facteurs sur un compte sans code TOTP : Keycloak répond 500, refus pour l'utilisateur.
        assertThat(KeycloakFournisseurIdentite.echec(500, Map.of(), ClientConnexion.DEUX_FACTEURS).causeEchec()).isEqualTo(CauseEchec.IDENTIFIANTS_INVALIDES);
        assertThat(KeycloakFournisseurIdentite.echec(500, Map.of(), ClientConnexion.MOT_DE_PASSE).causeEchec()).isEqualTo(CauseEchec.SERVICE_INDISPONIBLE);
        assertThat(KeycloakFournisseurIdentite.echec(503, Map.of(), ClientConnexion.DEUX_FACTEURS).causeEchec()).isEqualTo(CauseEchec.SERVICE_INDISPONIBLE);
        assertThat(KeycloakFournisseurIdentite.encoder("a b:c")).isEqualTo("a+b%3Ac");
        assertThat(KeycloakFournisseurIdentite.encoder(null)).isEmpty();
    }

    @Test
    @DisplayName("Keycloak : point de révocation annoncé, sinon celui du domaine")
    void pointRevocation() {
        ClientRegistration sans = base("x", "c", "s").build();
        assertThat(KeycloakFournisseurIdentite.pointRevocation(sans)).isEqualTo(KC.emetteur() + "/protocol/openid-connect/revoke");
        ClientRegistration avec = base("y", "c", "s").providerConfigurationMetadata(Map.of("revocation_endpoint", "http://ailleurs/revoke")).build();
        assertThat(KeycloakFournisseurIdentite.pointRevocation(avec)).isEqualTo("http://ailleurs/revoke");
        StepVerifier.create(fournisseur.revoquer("connexion", "rt-1")).verifyComplete();
        assertThat(KC.revoques()).containsExactly("rt-1");
    }

    Map<String, Object> claims(String aud, String sub) {
        return Map.of("iss", KC.emetteur(), "sub", sub, "aud", aud, "iat", Instant.now(), "exp", Instant.now().plusSeconds(60));
    }

    @Test
    @DisplayName("Validation : jeton absent, signé par une autre clé, destiné à un autre client ou d'un autre sujet : refusé")
    void validationRefusee() {
        echec(fournisseur.valider(ClientConnexion.MOT_DE_PASSE, new Jetons("a", null, null, 60, null)), CauseEchec.SERVICE_INDISPONIBLE);
        echec(fournisseur.valider(ClientConnexion.MOT_DE_PASSE, new Jetons(null, null, "i", 60, null)), CauseEchec.SERVICE_INDISPONIBLE);
        String acces = KC.signer(claims("account", "u-1"), false);
        // Signature altérée : refusée par le JWKS du domaine.
        String valide = KC.signer(claims("gimac-connexion", "u-1"), false);
        String altere = valide.substring(0, valide.lastIndexOf('.') + 1) + new StringBuilder(valide.substring(valide.lastIndexOf('.') + 1)).reverse();
        echec(fournisseur.valider(ClientConnexion.MOT_DE_PASSE, new Jetons(acces, null, altere, 60, null)), CauseEchec.SERVICE_INDISPONIBLE);
        echec(
            fournisseur.valider(ClientConnexion.MOT_DE_PASSE, new Jetons(acces, null, KC.signer(claims("web_app", "u-1"), false), 60, null)),
            CauseEchec.SERVICE_INDISPONIBLE
        );
        echec(
            fournisseur.valider(ClientConnexion.MOT_DE_PASSE, new Jetons(acces, null, KC.signer(claims("gimac-connexion", "u-2"), false), 60, null)),
            CauseEchec.SERVICE_INDISPONIBLE
        );
        var ok = fournisseur.valider(ClientConnexion.MOT_DE_PASSE, new Jetons(acces, null, KC.signer(claims("gimac-connexion", "u-1"), false), 60, null)).block();
        assertThat(ok.sujet()).isEqualTo("u-1");
    }

    @Test
    @DisplayName("MS-IAM : état du compte, jeton de service présenté ; sans jeton de service : indisponible")
    void iam() throws IOException {
        try (ServeurHttpLocal iam = new ServeurHttpLocal()) {
            iam.json("/api/v1/interne/comptes/u-1/etat-connexion", 200, "{\"actif\":true,\"secondFacteur\":false,\"actionsRequises\":[\"FICHE\"]}");
            iam.json("/api/v1/interne/comptes/u-2/etat-connexion", 200, "{\"actif\":true}");
            iam.json("/api/v1/interne/connexion/mot-de-passe-oublie", 202, "");
            IamAnnuaireComptes annuaire = new IamAnnuaireComptes(WebClient.builder().baseUrl(iam.url()).build(), () -> Mono.just("jeton-service"), Duration.ofSeconds(3));
            var etat = annuaire.etat("u-1").block();
            assertThat(etat.actif()).isTrue();
            assertThat(etat.secondFacteur()).isFalse();
            assertThat(etat.actionsRequises()).containsExactly("FICHE");
            assertThat(annuaire.etat("u-2").block().actionsRequises()).isEmpty();
            assertThat(iam.recues().get(0).autorisation()).isEqualTo("Bearer jeton-service");
            echec(annuaire.etat("u-3"), CauseEchec.COMPTE_INACTIF);
            StepVerifier.create(annuaire.motDePasseOublie("agent", null)).verifyComplete();
            IamAnnuaireComptes sansJeton = new IamAnnuaireComptes(WebClient.builder().baseUrl(iam.url()).build(), Mono::empty, Duration.ofSeconds(3));
            echec(sansJeton.etat("u-1"), CauseEchec.SERVICE_INDISPONIBLE);
            echec(sansJeton.nouveauMotDePasse("j", "m"), CauseEchec.SERVICE_INDISPONIBLE);
        }
        IamAnnuaireComptes injoignable = new IamAnnuaireComptes(WebClient.builder().baseUrl("http://127.0.0.1:1").build(), () -> Mono.just("j"), Duration.ofSeconds(2));
        echec(injoignable.etat("u-1"), CauseEchec.SERVICE_INDISPONIBLE);
        echec(injoignable.nouveauMotDePasse("j", "m"), CauseEchec.SERVICE_INDISPONIBLE);
    }

    @Test
    @DisplayName("Limitation en mémoire : capacité puis recharge continue")
    void limitationMemoire() {
        HorlogeReglable h = new HorlogeReglable(Instant.parse("2026-09-30T08:00:00Z"));
        LimiteurTentativesMemoire l = new LimiteurTentativesMemoire(5, Duration.ofMinutes(1), h);
        for (int i = 0; i < 5; i++) {
            assertThat(l.consommer("k").block()).isTrue();
        }
        assertThat(l.consommer("k").block()).isFalse();
        assertThat(l.consommer("autre").block()).isTrue();
        h.avancer(Duration.ofSeconds(12));
        assertThat(l.consommer("k").block()).isTrue();
        assertThat(l.consommer("k").block()).isFalse();
    }

    @Test
    @DisplayName("Limitation Valkey : décision du seau partagé ; Valkey en panne : limitation en mémoire")
    void limitationValkey() {
        TokenBucketRateLimiter valkey = mock(TokenBucketRateLimiter.class);
        LimiteurTentativesMemoire secours = new LimiteurTentativesMemoire(1, Duration.ofMinutes(1), new HorlogeReglable(Instant.EPOCH));
        LimiteurTentativesValkey l = new LimiteurTentativesValkey(valkey, secours, 5, 5);
        when(valkey.isAllowed("a", 5, 5)).thenReturn(Mono.just(List.of(1L, 4L)));
        when(valkey.isAllowed("b", 5, 5)).thenReturn(Mono.just(List.of(0L, 0L)));
        when(valkey.isAllowed("c", 5, 5)).thenReturn(Mono.just(List.of(1L, -1L)));
        when(valkey.isAllowed("d", 5, 5)).thenReturn(Mono.just(List.of()));
        assertThat(l.consommer("a").block()).isTrue();
        assertThat(l.consommer("b").block()).isFalse();
        assertThat(l.consommer("c").block()).isTrue();
        assertThat(l.consommer("c").block()).isFalse();
        assertThat(l.consommer("d").block()).isFalse();
    }

    @Test
    @DisplayName("Journal : publication des deux événements, identifiant masqué ; sans publication, journal applicatif seul")
    void journal() {
        GatewayAuditEventPublisher publication = mock(GatewayAuditEventPublisher.class);
        DemandeConnexion d = new DemandeConnexion("agent.sna", "secret", "123456", Couche.PORTAIL, "10.0.0.9");
        new JournalConnexionAudit(publication).reussie(d, "u-1", "CONNECTE");
        verify(publication, timeout(2000)).publishSecurityEvent(
            eq("gateway.login-succeeded.v1"),
            eq("u-1"),
            eq("10.0.0.9"),
            eq(200),
            eq(Map.of("identifiant", "ag***a", "couche", "portail", "etat", "CONNECTE"))
        );
        new JournalConnexionAudit(publication).echouee(d, CauseEchec.COMPTE_INACTIF, null);
        verify(publication, timeout(2000)).publishSecurityEvent(
            eq("gateway.login-failed.v1"),
            eq(null),
            eq("10.0.0.9"),
            eq(403),
            eq(Map.of("identifiant", "ag***a", "couche", "portail", "code", "CNX_COMPTE_INACTIF", "cause", "COMPTE_INACTIF"))
        );
        JournalConnexionAudit seul = new JournalConnexionAudit(null);
        seul.reussie(new DemandeConnexion("a", "b", null, null, "ip"), null, "PREMIER_ACCES");
        seul.echouee(d, CauseEchec.IDENTIFIANTS_INVALIDES, "Keycloak : Invalid user credentials");
    }

    @Test
    @DisplayName("Jeton de service : client_credentials du gestionnaire ; sans gestionnaire ou en panne : vide")
    void jetonService() {
        assertThat(new JetonServicePasserelle(null).get().blockOptional()).isEmpty();
        ReactiveOAuth2AuthorizedClientManager g = mock(ReactiveOAuth2AuthorizedClientManager.class);
        ClientRegistration reg = base("gateway-service", "gateway-service", "s").build();
        OAuth2AccessToken jeton = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton", Instant.now(), Instant.now().plusSeconds(60));
        when(g.authorize(any())).thenReturn(Mono.just(new OAuth2AuthorizedClient(reg, "gateway-service", jeton)));
        assertThat(new JetonServicePasserelle(g).get().block()).isEqualTo("jeton");
        when(g.authorize(any())).thenReturn(Mono.error(new IllegalStateException("panne")));
        assertThat(new JetonServicePasserelle(g).get().blockOptional()).isEmpty();
    }

    @Test
    @DisplayName("mon-compte.md §3 : alerte « nouvel appareil » transmise à MS-IAM avec le jeton de service ; refus : erreur")
    void alerteNouvelAppareil() throws IOException {
        try (ServeurHttpLocal iam = new ServeurHttpLocal()) {
            iam.json("/api/v1/interne/comptes/u1/alerte-connexion", 204, "");
            IamAnnuaireComptes annuaire = new IamAnnuaireComptes(WebClient.builder().baseUrl(iam.url()).build(), () -> Mono.just("jeton-service"), Duration.ofSeconds(3));
            var appareil = new cm.gimac.des.gateway.domain.appareils.Appareil("Safari 17", "iOS 17", cm.gimac.des.gateway.domain.appareils.TypeAppareil.MOBILE);
            annuaire.alerter("u1", appareil, "41.202.10.20", "des", java.time.Instant.parse("2026-10-01T08:30:00Z")).block();
            var recue = iam.recues().get(0);
            assertThat(recue.methode()).isEqualTo("POST");
            assertThat(recue.autorisation()).isEqualTo("Bearer jeton-service");
            assertThat(recue.corps())
                .contains("\"navigateur\":\"Safari 17\"")
                .contains("\"type\":\"MOBILE\"")
                .contains("\"adresseIp\":\"41.202.10.20\"")
                .contains("\"le\":\"2026-10-01T08:30:00Z\"");
            annuaire.alerter("u1", appareil, null, null, null).block();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> annuaire.alerter("inconnu", appareil, "ip", "des", null).block()).isNotNull();
        }
    }
}
