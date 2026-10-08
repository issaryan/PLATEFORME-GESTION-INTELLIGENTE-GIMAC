package cm.gimac.des.certification.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.certification.application.port.out.CachetPlateformePort.Cachet;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.ContexteCertification;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.Couverture;
import cm.gimac.des.certification.application.port.out.DocumentPort.DocumentDepose;
import cm.gimac.des.certification.application.port.out.DocumentPort.Fichier;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Adaptateurs sortants des verrous : MS-HOMOLOGATION, MS-IAM, MS-DOCUMENT (réels et substituts), cachet, fiches. */
class AdaptateursDecisionTest {

    static final UUID LIAISON = UUID.randomUUID();
    static final UUID DOC = UUID.randomUUID();
    static final String SHA = "b".repeat(64);

    RestClient.Builder builder;
    MockRestServiceServer serveur;
    ServiceTokenInterceptor jeton;

    @BeforeEach
    void init() {
        builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).build();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration reg = ClientRegistration.withRegistrationId("service-token")
            .clientId("certification-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://idp/token")
            .build();
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton-service", Instant.now(), Instant.now().plusSeconds(60));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(reg, "service", token));
        jeton = new ServiceTokenInterceptor(manager);
    }

    static void indisponible(Runnable appel, String code) {
        assertThatThrownBy(appel::run).isInstanceOfSatisfying(CertificationException.class, e -> {
            assertThat(e.status()).isEqualTo(503);
            assertThat(e.code()).isEqualTo(code);
        });
    }

    @Test
    @DisplayName("Contexte de certification (MS-HOMOLOGATION) : contrat lu, liaison inconnue, service indisponible")
    void contexteHomologation() {
        HomologationContexteAdapter a = new HomologationContexteAdapter(builder, jeton, "http://homologation");
        String uri = "http://homologation/api/v1/interne/liaisons/" + LIAISON + "/contexte-certification";
        String corps = """
            {"liaisonId":"%s","reference":"LIA-2026-041","institutionId":"INST-0042","institutionNom":"Banque A",
             "typeProjet":"PAIEMENT_MOBILE","fintech":true,"statut":"EN_TEST",
             "couvertureCertification":{"taux":100,"numerateur":3,"denominateur":3},"couvertureConstatee":true,
             "servicesCouverts":["CASH_IN","CASH_OUT","PAIEMENT"],"conducteurId":"c1","conducteurNom":"Conducteur",
             "modeleTypeProjet":"PAIEMENT_MOBILE","attestationsExigees":["ISO_27001","GSMA","COBAC"]}""".formatted(LIAISON);
        serveur.expect(requestTo(uri)).andExpect(header("Authorization", "Bearer jeton-service")).andRespond(withSuccess(corps, MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withSuccess("{\"liaisonId\":\"" + LIAISON + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withSuccess("{\"liaisonId\":\"" + LIAISON + "\",\"couvertureCertification\":{\"taux\":50}}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withResourceNotFound());
        serveur.expect(requestTo(uri)).andRespond(withServerError());
        ContexteCertification c = a.contexte(LIAISON).orElseThrow();
        assertThat(c.fintech()).isTrue();
        assertThat(c.couvertureConstatee()).isTrue();
        assertThat(c.couvertureCertification()).isEqualTo(new Couverture(100, 3, 3));
        assertThat(c.servicesCouverts()).containsExactly("CASH_IN", "CASH_OUT", "PAIEMENT");
        assertThat(c.modele()).isEqualTo("PAIEMENT_MOBILE");
        assertThat(c.attestationsExigees()).containsExactly("ISO_27001", "GSMA", "COBAC");
        ContexteCertification vide = a.contexte(LIAISON).orElseThrow();
        assertThat(vide.fintech()).isFalse();
        assertThat(vide.couvertureCertification()).isEqualTo(new Couverture(null, 0, 0));
        assertThat(vide.servicesCouverts()).isEmpty();
        assertThat(vide.modeleTypeProjet()).isNull();
        assertThat(vide.attestationsExigees()).isNull();
        assertThat(a.contexte(LIAISON).orElseThrow().couvertureCertification()).isEqualTo(new Couverture(50, 0, 0));
        assertThat(a.contexte(LIAISON)).isEmpty();
        indisponible(() -> a.contexte(LIAISON), "CER_HOMOLOGATION_UNAVAILABLE");
        serveur.verify();
    }

    @Test
    @DisplayName("Réauthentification réelle : verdict de MS-IAM, refus 4xx, panne 503, assertion vide sans appel")
    void reauthentification() {
        IamReauthentificationAdapter iam = new IamReauthentificationAdapter(builder, jeton, "http://iam");
        String uri = "http://iam/api/v1/interne/reauthentifications/verification";
        serveur
            .expect(requestTo(uri))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.assertion").value("A"))
            .andExpect(jsonPath("$.sujet").value("u1"))
            .andExpect(jsonPath("$.finalite").value("QUITUS_SIGN"))
            .andRespond(withSuccess("{\"valide\":true}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withSuccess("{\"valide\":false}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        serveur.expect(requestTo(uri)).andRespond(withServerError());
        assertThat(iam.verifier("A", "u1", "QUITUS_SIGN")).isTrue();
        assertThat(iam.verifier("A", "u1", "QUITUS_SIGN")).isFalse();
        assertThat(iam.verifier("A", "u1", "QUITUS_SIGN")).isFalse();
        indisponible(() -> iam.verifier("A", "u1", "QUITUS_SIGN"), "CER_IAM_UNAVAILABLE");
        assertThat(iam.verifier(" ", "u1", "QUITUS_SIGN")).isFalse();
        assertThat(iam.verifier(null, "u1", "QUITUS_SIGN")).isFalse();
        serveur.verify();
    }

    @Test
    @DisplayName("MS-DOCUMENT réel : empreinte conforme, divergente, inconnue ; dépôt multipart ; pannes 503 ; refus 422")
    void document() {
        DocumentRestAdapter d = new DocumentRestAdapter(builder, jeton, "http://document");
        String empreinte = "http://document/api/v1/interne/documents/" + DOC + "/empreinte";
        serveur.expect(requestTo(empreinte)).andRespond(withSuccess("{\"sha256\":\"" + SHA.toUpperCase() + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(empreinte)).andRespond(withSuccess("{\"sha256\":\"" + "c".repeat(64) + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(empreinte)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(empreinte)).andRespond(withResourceNotFound());
        serveur.expect(requestTo(empreinte)).andRespond(withServerError());
        String depot = "http://document/api/v1/interne/documents";
        serveur
            .expect(requestTo(depot))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer jeton-service"))
            .andRespond(withSuccess("{\"id\":\"" + DOC + "\",\"sha256\":\"" + SHA + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(depot)).andRespond(withStatus(HttpStatus.CONFLICT));
        serveur.expect(requestTo(depot)).andRespond(withServerError());
        serveur.expect(requestTo(depot)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThat(d.empreinteConforme(DOC, SHA)).isTrue();
        assertThat(d.empreinteConforme(DOC, SHA)).isFalse();
        assertThat(d.empreinteConforme(DOC, SHA)).isFalse();
        assertThat(d.empreinteConforme(DOC, SHA)).isFalse();
        indisponible(() -> d.empreinteConforme(DOC, SHA), "CER_DOCUMENT_UNAVAILABLE");
        Fichier f = new Fichier("pci.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));
        assertThat(d.deposer(f, "ATTESTATION_SECURITE", "ref", "u1")).isEqualTo(new DocumentDepose(DOC.toString(), SHA));
        assertThatThrownBy(() -> d.deposer(f, "ATTESTATION_SECURITE", "ref", "u1"))
            .isInstanceOfSatisfying(CertificationException.class, e -> assertThat(e.code()).isEqualTo("CER_ATTESTATION_FORMAT"));
        indisponible(() -> d.deposer(f, "ATTESTATION_SECURITE", "ref", "u1"), "CER_DOCUMENT_UNAVAILABLE");
        indisponible(() -> d.deposer(f, "ATTESTATION_SECURITE", "ref", "u1"), "CER_DOCUMENT_UNAVAILABLE");
        serveur.verify();
    }

    @Test
    @DisplayName("Substituts : contexte enregistré, document déposé et vérifié, fiche approuvée par défaut, réauthentification")
    void substituts() {
        ContexteCertificationStubAdapter contextes = new ContexteCertificationStubAdapter();
        ContexteCertification c = new ContexteCertification(LIAISON, "R", "I", "N", "CARTE_INTERBANCAIRE", false, "EN_TEST", null, true, null, null, null);
        contextes.enregistrer(c);
        assertThat(contextes.contexte(LIAISON)).contains(c);
        contextes.oublier(LIAISON);
        assertThat(contextes.contexte(LIAISON)).isEmpty();
        DocumentStubAdapter docs = new DocumentStubAdapter();
        DocumentDepose depot = docs.deposer(new Fichier("a.pdf", "application/pdf", new byte[0]), "F", "R", "u");
        assertThat(depot.sha256()).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(docs.empreinteConforme(UUID.fromString(depot.id()), depot.sha256())).isTrue();
        assertThat(docs.empreinteConforme(UUID.fromString(depot.id()), SHA)).isFalse();
        assertThat(docs.empreinteConforme(UUID.randomUUID(), SHA)).isTrue();
        FicheChangementStubAdapter fiches = new FicheChangementStubAdapter();
        assertThat(fiches.lire("CHG-2026-0001")).isEqualTo(EtatFicheChangement.APPROUVEE);
        fiches.enregistrer("CHG-2026-0001", EtatFicheChangement.EN_EXAMEN);
        assertThat(fiches.lire("CHG-2026-0001")).isEqualTo(EtatFicheChangement.EN_EXAMEN);
        ReauthentificationStubAdapter reauth = new ReauthentificationStubAdapter();
        assertThat(reauth.verifier("x", "u", "F")).isTrue();
        assertThat(reauth.verifier(" ", "u", "F")).isFalse();
        assertThat(reauth.verifier(null, "u", "F")).isFalse();
        assertThat(reauth.verifier(ReauthentificationStubAdapter.ASSERTION_REFUSEE, "u", "F")).isFalse();
    }

    @Test
    @DisplayName("Cachet de substitution : signature déterministe pour une clé, nonce unique, horodatage de l'horloge")
    void cachet() {
        Instant t = Instant.parse("2026-09-30T08:00:00Z");
        CachetPlateformeStubAdapter a = new CachetPlateformeStubAdapter("cle", Clock.fixed(t, ZoneOffset.UTC));
        Cachet c1 = a.sceller(SHA);
        Cachet c2 = a.sceller(SHA);
        assertThat(c1.signature()).isEqualTo(c2.signature());
        assertThat(c1.nonce()).isNotEqualTo(c2.nonce()).hasSize(32);
        assertThat(c1.horodatage()).isEqualTo(t);
        assertThat(c1.autorite()).startsWith("SUBSTITUTION");
        assertThat(new String(java.util.Base64.getDecoder().decode(c1.jetonHorodatage()), StandardCharsets.UTF_8)).startsWith(SHA + "|" + c1.nonce());
        assertThat(new CachetPlateformeStubAdapter("autre", Clock.fixed(t, ZoneOffset.UTC)).sceller(SHA).signature()).isNotEqualTo(c1.signature());
        assertThat(List.of(c1.signature())).allMatch(s -> !s.isBlank());
    }
}
