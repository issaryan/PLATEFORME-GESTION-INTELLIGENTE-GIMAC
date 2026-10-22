package cm.gimac.des.gateway.infrastructure.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.audit.GatewayAuditEventPublisher;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import cm.gimac.des.gateway.domain.sandbox.JetonReduit;
import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import cm.gimac.des.gateway.security.CertificatMtlsPasserelle;
import cm.gimac.des.gateway.security.EmetteurJetonReduitJwt;
import cm.gimac.des.gateway.security.EmetteurJwtInterne;
import cm.gimac.des.gateway.security.RsaKeyLoader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/** Adaptateurs de la sandbox dans la passerelle (US-EXI-07) : projection mémoire, journal, jeton réduit, consommateur. */
class AdaptateursSandboxTest {

    static final RouteSandbox ROUTE = new RouteSandbox("portail", "simulation");

    @Test
    @DisplayName("Projection mémoire : appliquée si la version est supérieure, redélivrance et instantané périmé sans effet")
    void projectionMemoire() {
        ProjectionPerimetresMemoire p = new ProjectionPerimetresMemoire();
        assertThat(p.lire("u").blockOptional()).isEmpty();
        assertThat(p.appliquer(new PerimetreMandats("u", "99010", Map.of(), 7)).block()).isTrue();
        assertThat(p.appliquer(new PerimetreMandats("u", "99010", Map.of("99002", Instant.EPOCH), 7)).block()).isFalse();
        assertThat(p.appliquer(new PerimetreMandats("u", "99010", Map.of("99002", Instant.EPOCH), 6)).block()).isFalse();
        assertThat(p.lire("u").block().mandantes()).isEmpty();
        assertThat(p.appliquer(new PerimetreMandats("u", "99010", Map.of("99002", Instant.EPOCH), 8)).block()).isTrue();
        assertThat(p.lire("u").block().version()).isEqualTo(8);
    }

    @Test
    @DisplayName("Critère 1 : décision de la passerelle journalisée vers MS-AUDIT (gateway.sandbox-decision.v1), sans jeton")
    void journal() {
        GatewayAuditEventPublisher publication = mock(GatewayAuditEventPublisher.class);
        JournalDecisionsSandboxAudit j = new JournalDecisionsSandboxAudit(publication);
        j.consigner(new JournalDecisionsSandbox.Entree("u-1", ROUTE, "GET", "/bff/portail/sbx-simulation/essais", "99001", null, DecisionSandbox.de(Motif.INSTITUTION_PROPRE, "99001"), 200, "10.0.0.1"));
        j.consigner(new JournalDecisionsSandbox.Entree("u-2", ROUTE, "POST", "/bff/portail/sbx-simulation/essais", null, "99002", DecisionSandbox.de(Motif.MANDAT_ECHU, ""), 404, ""));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> details = ArgumentCaptor.forClass(Map.class);
        verify(publication).publishSecurityEvent(eq(JournalDecisionsSandbox.EVENEMENT), eq("u-1"), eq("10.0.0.1"), eq(200), details.capture());
        assertThat(details.getValue())
            .containsEntry("source", "sandbox")
            .containsEntry("service", "sbx-simulation")
            .containsEntry("couche", "portail")
            .containsEntry("institutionEffective", "99001")
            .containsEntry("institutionDemandee", "")
            .containsEntry("decision", "ADMISE")
            .containsEntry("motif", "INSTITUTION_PROPRE");
        verify(publication).publishSecurityEvent(eq(JournalDecisionsSandbox.EVENEMENT), eq("u-2"), eq(""), eq(404), details.capture());
        assertThat(details.getValue()).containsEntry("decision", "REFUSEE").containsEntry("institutionJeton", "").containsEntry("motif", "MANDAT_ECHU");
        // Sans publication (journal des décisions inactif) : journal applicatif seulement.
        new JournalDecisionsSandboxAudit(null).consigner(new JournalDecisionsSandbox.Entree("u", ROUTE, "GET", "/", "", "", DecisionSandbox.de(Motif.COMPTE_GIMAC, ""), 200, ""));
    }

    @SuppressWarnings("unchecked")
    static <T> ObjectProvider<T> fournisseur(T valeur) {
        ObjectProvider<T> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(valeur);
        return p;
    }

    @Test
    @DisplayName("Critère 2 : jeton interne réduit signé RS256, institution mandante, institution propre, permissions SBX, audience du service")
    void jetonReduit() throws Exception {
        RSAKey cle = RsaKeyLoader.genererEphemere(2048);
        EmetteurJetonReduitJwt e = new EmetteurJetonReduitJwt(new EmetteurJwtInterne(cle, fournisseur(null)));
        String jeton = e.emettre(new JetonReduit("u-1", "99002", "99010", List.of("SBX_SIM_SIMULATION"), "2", "sbx-simulation")).orElseThrow();
        SignedJWT jwt = SignedJWT.parse(jeton);
        assertThat(jwt.verify(new RSASSAVerifier(cle.toRSAPublicKey()))).isTrue();
        JWTClaimsSet c = jwt.getJWTClaimsSet();
        assertThat(c.getSubject()).isEqualTo("u-1");
        assertThat(c.getIssuer()).isEqualTo(EmetteurJwtInterne.ISSUER);
        assertThat(c.getAudience()).containsExactly("sbx-simulation");
        assertThat(c.getStringClaim("institution_id")).isEqualTo("99002");
        assertThat(c.getStringClaim("institution_propre")).isEqualTo("99010");
        assertThat(c.getStringListClaim("roles")).containsExactly("SBX_SIM_SIMULATION");
        assertThat(c.getStringClaim("acr")).isEqualTo("2");
        assertThat(c.getJWTID()).isNotBlank();
        // Institution propre : pas de revendication institution_propre ; compte du GIMAC : aucune institution.
        JWTClaimsSet propre = SignedJWT.parse(e.emettre(new JetonReduit("u", "99001", "99001", List.of(), "2", "sbx-trace")).orElseThrow()).getJWTClaimsSet();
        assertThat(propre.getClaims()).containsEntry("institution_id", "99001").doesNotContainKey("institution_propre");
        JWTClaimsSet gimac = SignedJWT.parse(e.emettre(new JetonReduit("u", "", "", List.of(), "2", "sbx-trace")).orElseThrow()).getJWTClaimsSet();
        assertThat(gimac.getClaims()).doesNotContainKeys("institution_id", "institution_propre");
    }

    @Test
    @DisplayName("Jeton réduit : un échec de signature rend vide (la passerelle refuse alors le mandat)")
    void jetonReduitEchec() throws Exception {
        EmetteurJwtInterne emetteur = mock(EmetteurJwtInterne.class);
        when(emetteur.emettre(any())).thenThrow(new JOSEException("clé"));
        assertThat(new EmetteurJetonReduitJwt(emetteur).emettre(new JetonReduit("u", "99001", "99001", List.of(), "2", "sbx-trace"))).isEmpty();
        // Lien au certificat mTLS (RFC 8705) quand il existe.
        CertificatMtlsPasserelle certificat = mock(CertificatMtlsPasserelle.class);
        when(certificat.empreinte()).thenReturn("empreinte");
        RSAKey cle = RsaKeyLoader.genererEphemere(2048);
        String jeton = new EmetteurJetonReduitJwt(new EmetteurJwtInterne(cle, fournisseur(certificat)))
            .emettre(new JetonReduit("u", "99001", "99001", List.of(), "2", "sbx-trace"))
            .orElseThrow();
        assertThat(SignedJWT.parse(jeton).getJWTClaimsSet().getJSONObjectClaim("cnf")).containsEntry("x5t#S256", "empreinte");
    }

    @Test
    @DisplayName("Consommateur : instantané appliqué, enveloppe non conforme écartée, cycle de vie du conteneur")
    void consommateur() {
        ProjectionPerimetresMemoire projection = new ProjectionPerimetresMemoire();
        ConsommateurPerimetresKafka c = new ConsommateurPerimetresKafka(Map.of("bootstrap.servers", "127.0.0.1:1"), "sbx-espace.scope-changed.v1", projection);
        String valeur =
            "{\"specversion\":\"1.0\",\"id\":\"e1\",\"type\":\"sbx-espace.scope-changed.v1\",\"data\":{\"idCompte\":\"u-1\",\"institutionPropre\":\"99010\",\"mandantes\":{\"99002\":\"2026-11-10T00:00:00Z\"},\"version\":2}}";
        c.recevoir(new ConsumerRecord<>("sbx-espace.scope-changed.v1", 0, 0, "99010", valeur));
        c.recevoir(new ConsumerRecord<>("sbx-espace.scope-changed.v1", 0, 1, "99010", valeur));
        c.recevoir(new ConsumerRecord<>("sbx-espace.scope-changed.v1", 0, 2, "99010", "{}"));
        assertThat(projection.lire("u-1").block().version()).isEqualTo(2);
        assertThat(c.isRunning()).isFalse();
        c.start();
        assertThat(c.isRunning()).isTrue();
        c.stop();
        assertThat(c.isRunning()).isFalse();
        assertThat(ConsommateurPerimetresKafka.GROUPE).isEqualTo("gateway-sbx-perimetres");
    }
}
