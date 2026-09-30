package cm.gimac.des.certification.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.usecase.ApprobationSecuriteService;
import cm.gimac.des.certification.application.usecase.DecisionVues.ApprobationVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.CouvertureVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.ProcesVerbalVue;
import cm.gimac.des.certification.application.usecase.DecisionVues.SignatureVue;
import cm.gimac.des.certification.application.usecase.ProcesVerbalService;
import cm.gimac.des.certification.config.SecurityConfiguration;
import cm.gimac.des.certification.config.TestSecurityConfiguration;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import cm.gimac.des.certification.web.rest.errors.CertificationExceptionHandler;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Tests de sécurité des routes des verrous côté DES (D-61) avec la vraie chaîne de filtres : 401 sans jeton ;
 * 403 sans second facteur (acr) ; 403 sans la permission de la route ; accès avec la permission. Formes JSON
 * du contrat de l'interface et erreurs en problem+json. Sans Docker (tranche web, cas d'utilisation simulés).
 */
@WebMvcTest(controllers = DecisionResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, CertificationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class DecisionResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final String L = "/api/v1/liaisons/" + ID;
    static final String DOSSIER = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + "a".repeat(64) + "\",\"nom\":\"dossier.pdf\"}";
    static final String FICHE = "{\"reference\":\"CHG-2026-0412\"}";
    static final String VERIFICATION = "{\"conforme\":true,\"motif\":\"\"}";
    static final String AVIS = "{\"favorable\":true,\"motif\":\"Conforme\"}";
    static final List<String> PERMISSIONS = List.of(
        "CER_DOSSIER_READ",
        "CER_PV_ESTABLISH",
        "CER_QUITUS_TECHNIQUE_SIGN",
        "CER_CHANGE_RECORD_LINK",
        "CER_SECURITY_VERIFY",
        "CER_QUITUS_CONFORMITE_SIGN",
        "CER_QUITUS_DG_SIGN",
        "CER_PORTAL_READ",
        "CER_PORTAL_SIGN",
        "HOM_PORTAL_SUBMIT"
    );

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProcesVerbalService procesVerbaux;

    @MockitoBean
    ApprobationSecuriteService approbations;

    /** Route, corps, permission exigée (une de celles-ci suffit). */
    static Stream<Arguments> routes() {
        return Stream.of(
            Arguments.of(HttpMethod.GET, L + "/proces-verbal", null, List.of("CER_DOSSIER_READ")),
            Arguments.of(HttpMethod.POST, L + "/proces-verbal", null, List.of("CER_PV_ESTABLISH")),
            Arguments.of(HttpMethod.POST, L + "/proces-verbal/signature-gimac", null, List.of("CER_QUITUS_TECHNIQUE_SIGN")),
            Arguments.of(HttpMethod.GET, L + "/approbation-securite", null, List.of("CER_DOSSIER_READ")),
            Arguments.of(HttpMethod.POST, L + "/approbation-securite/dossier", DOSSIER, List.of("CER_CHANGE_RECORD_LINK")),
            Arguments.of(HttpMethod.PUT, L + "/approbation-securite/fiche", FICHE, List.of("CER_CHANGE_RECORD_LINK")),
            Arguments.of(HttpMethod.POST, L + "/approbation-securite/fiche/lecture", null, List.of("CER_CHANGE_RECORD_LINK", "CER_SECURITY_VERIFY")),
            Arguments.of(HttpMethod.POST, L + "/approbation-securite/attestations/PCI_DSS/verification", VERIFICATION, List.of("CER_SECURITY_VERIFY")),
            Arguments.of(HttpMethod.POST, L + "/approbation-securite/archivage", null, List.of("CER_SECURITY_VERIFY")),
            Arguments.of(HttpMethod.POST, L + "/approbation-securite/visas/CONFORMITE", AVIS, List.of("CER_QUITUS_CONFORMITE_SIGN")),
            Arguments.of(HttpMethod.POST, L + "/approbation-securite/visas/DIRECTION_GENERALE", AVIS, List.of("CER_QUITUS_DG_SIGN"))
        );
    }

    static MockHttpServletRequestBuilder req(HttpMethod m, String uri, String body) {
        MockHttpServletRequestBuilder b = request(m, uri).header("X-Reauth-Assertion", "assertion");
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return b;
    }

    /** Jeton utilisateur : sujet, niveau d'assurance acr, permissions de la matrice des rôles. */
    static RequestPostProcessor jeton(String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject("u-" + UUID.randomUUID()).claim("name", "Agent du DES");
                if (acr != null) {
                    j.claim("acr", acr);
                }
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Sans jeton : 401")
    void sansJeton(HttpMethod m, String uri, String body, List<String> permissions) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("D-61 : jeton sans second facteur (acr absent ou 1) : 403, même avec la permission")
    void sansDoubleFacteur(HttpMethod m, String uri, String body, List<String> permissions) throws Exception {
        mvc.perform(req(m, uri, body).with(jeton(null, permissions.get(0)))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("1", permissions.get(0)))).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Matrice des rôles : sans la permission de la route, 403 (séparation des pouvoirs)")
    void sansPermission(HttpMethod m, String uri, String body, List<String> permissions) throws Exception {
        String[] autres = PERMISSIONS.stream().filter(p -> !permissions.contains(p)).toArray(String[]::new);
        mvc.perform(req(m, uri, body).with(jeton("2", autres))).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Avec le second facteur et la permission de la route : accès accordé")
    void avecPermission(HttpMethod m, String uri, String body, List<String> permissions) throws Exception {
        for (String p : permissions) {
            mvc.perform(req(m, uri, body).with(jeton("2", p))).andExpect(status().isOk());
            mvc.perform(req(m, uri, body).with(jeton("3", p))).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Ressources générées par JHipster : réservées à l'administration technique ; jeton de service refusé")
    void crudEtJetonDeService() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/quituses", null).with(jeton("2", "CER_DOSSIER_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.GET, L + "/proces-verbal", null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_HOMOLOGATION_READ"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Forme du procès-verbal servie à l'interface (ProcesVerbalCertification)")
    void formeProcesVerbal() throws Exception {
        Instant h = Instant.parse("2026-09-30T09:15:00Z");
        when(procesVerbaux.consulter(eq(ID), any())).thenReturn(
            new ProcesVerbalVue(
                ID,
                "SIGNE_GIMAC",
                CouvertureVue.de(100, 3, 3),
                List.of("RETRAIT"),
                "Aminata Ngono",
                LocalDate.parse("2026-09-29"),
                "e".repeat(64),
                new SignatureVue("Chef du DES", "CHEF_DES", LocalDate.parse("2026-09-30"), h),
                null,
                List.of(),
                List.of()
            )
        );
        mvc
            .perform(req(HttpMethod.GET, L + "/proces-verbal", null).with(jeton("2", "CER_DOSSIER_READ")))
            .andExpect(jsonPath("$.liaisonId").value(ID.toString()))
            .andExpect(jsonPath("$.couverture.taux").value(100))
            .andExpect(jsonPath("$.couverture.calculable").value(true))
            .andExpect(jsonPath("$.etabliLe").value("2026-09-29"))
            .andExpect(jsonPath("$.signatureGimac.signeLe").value("2026-09-30"))
            .andExpect(jsonPath("$.signatureGimac.horodatage").value("2026-09-30T09:15:00Z"))
            .andExpect(jsonPath("$.signatureParticipant").isEmpty())
            .andExpect(jsonPath("$.transitionsPermises").isArray())
            .andExpect(jsonPath("$.actionsPermises").isArray());
        when(approbations.consulter(eq(ID), any())).thenReturn(
            new ApprobationVue(ID, "EN_ATTENTE_DOSSIER", null, null, null, List.of(), false, List.of(), null, List.of(), List.of("CONSTITUER_DOSSIER"))
        );
        mvc
            .perform(req(HttpMethod.GET, L + "/approbation-securite", null).with(jeton("2", "CER_DOSSIER_READ")))
            .andExpect(jsonPath("$.etat").value("EN_ATTENTE_DOSSIER"))
            .andExpect(jsonPath("$.fintech").value(false))
            .andExpect(jsonPath("$.actionsPermises[0]").value("CONSTITUER_DOSSIER"));
    }

    @Test
    @DisplayName("Acteur transmis aux cas d'utilisation : sujet, nom, permissions ; assertion et corps relayés")
    void acteurEtCorps() throws Exception {
        mvc.perform(req(HttpMethod.POST, L + "/proces-verbal/signature-gimac", null).with(jeton("2", "CER_QUITUS_TECHNIQUE_SIGN")));
        ArgumentCaptor<Acteur> a = ArgumentCaptor.forClass(Acteur.class);
        verify(procesVerbaux).signerGimac(eq(ID), a.capture(), eq("assertion"));
        org.assertj.core.api.Assertions.assertThat(a.getValue().nom()).isEqualTo("Agent du DES");
        org.assertj.core.api.Assertions.assertThat(a.getValue().permissions()).containsExactly("CER_QUITUS_TECHNIQUE_SIGN");
        mvc.perform(req(HttpMethod.POST, L + "/approbation-securite/visas/DIRECTION_GENERALE", "{\"favorable\":false,\"motif\":\"Non\"}").with(jeton("2", "CER_QUITUS_DG_SIGN")));
        verify(approbations).signerVisa(eq(ID), eq(NatureVisa.DIRECTION_GENERALE), eq(false), eq("Non"), any(), eq("assertion"));
        mvc.perform(req(HttpMethod.POST, L + "/approbation-securite/attestations/GSMA/verification", "{\"conforme\":false,\"motif\":\"Expirée\"}").with(jeton("2", "CER_SECURITY_VERIFY")));
        verify(approbations).verifierAttestation(eq(ID), eq("GSMA"), eq(false), eq("Expirée"), any());
        // Nom absent du jeton : identifiant de connexion, puis sujet.
        mvc.perform(
            request(HttpMethod.POST, L + "/proces-verbal").with(
                jwt().jwt(j -> j.subject("u-sans-nom").claim("acr", "2").claim("preferred_username", "conducteur1")).authorities(new SimpleGrantedAuthority("CER_PV_ESTABLISH"))
            )
        );
        mvc.perform(request(HttpMethod.POST, L + "/proces-verbal").with(jwt().jwt(j -> j.subject("u-anonyme").claim("acr", "2")).authorities(new SimpleGrantedAuthority("CER_PV_ESTABLISH"))));
        ArgumentCaptor<Acteur> e = ArgumentCaptor.forClass(Acteur.class);
        verify(procesVerbaux, org.mockito.Mockito.times(2)).etablir(eq(ID), e.capture());
        org.assertj.core.api.Assertions.assertThat(e.getAllValues()).extracting(Acteur::nom).containsExactly("conducteur1", "u-anonyme");
    }

    @Test
    @DisplayName("Erreurs métier en problem+json avec code ; concurrence 409 ; validation d'entrée 400")
    void erreurs() throws Exception {
        when(procesVerbaux.etablir(eq(ID), any())).thenThrow(
            CertificationException.conflit("CER_COVERAGE_INCOMPLETE", "La couverture de certification est de 75 % : le procès-verbal exige 100 %.")
        );
        mvc
            .perform(req(HttpMethod.POST, L + "/proces-verbal", null).with(jeton("2", "CER_PV_ESTABLISH")))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("CER_COVERAGE_INCOMPLETE"))
            .andExpect(jsonPath("$.correlationId").exists());
        when(procesVerbaux.signerGimac(eq(ID), any(), any())).thenThrow(CertificationException.reauthentificationExigee());
        mvc
            .perform(req(HttpMethod.POST, L + "/proces-verbal/signature-gimac", null).with(jeton("2", "CER_QUITUS_TECHNIQUE_SIGN")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("IAM_REAUTH_REQUIRED"));
        when(approbations.archiver(eq(ID), any())).thenThrow(new ObjectOptimisticLockingFailureException("ApprobationSecurite", ID));
        mvc
            .perform(req(HttpMethod.POST, L + "/approbation-securite/archivage", null).with(jeton("2", "CER_SECURITY_VERIFY")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CER_CONCURRENT_UPDATE"));
        when(approbations.relireFiche(eq(ID), any())).thenThrow(new DataIntegrityViolationException("uk_signature_verrou_role"));
        mvc
            .perform(req(HttpMethod.POST, L + "/approbation-securite/fiche/lecture", null).with(jeton("2", "CER_SECURITY_VERIFY")))
            .andExpect(status().isConflict());
        mvc
            .perform(req(HttpMethod.POST, L + "/approbation-securite/dossier", DOSSIER.replace("a".repeat(64), "XYZ")).with(jeton("2", "CER_CHANGE_RECORD_LINK")))
            .andExpect(status().isBadRequest());
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/liaisons/pas-un-uuid/proces-verbal", null).with(jeton("2", "CER_DOSSIER_READ")))
            .andExpect(status().isBadRequest());
        mvc
            .perform(req(HttpMethod.POST, L + "/approbation-securite/visas/AUTRE", AVIS).with(jeton("2", "CER_QUITUS_DG_SIGN")))
            .andExpect(status().is4xxClientError());
        org.mockito.Mockito.verify(approbations, org.mockito.Mockito.never()).signerVisa(any(), any(), anyBoolean(), any(), any(), any());
    }
}
