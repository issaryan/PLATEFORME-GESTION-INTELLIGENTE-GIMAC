package cm.gimac.des.certification.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.port.out.DocumentPort.Fichier;
import cm.gimac.des.certification.application.usecase.ApprobationSecuriteService;
import cm.gimac.des.certification.application.usecase.ProcesVerbalService;
import cm.gimac.des.certification.config.SecurityConfiguration;
import cm.gimac.des.certification.config.TestSecurityConfiguration;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.web.rest.errors.CertificationExceptionHandler;
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
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Tests de sécurité du portail (D-61, RG-59) : 401 sans jeton ; 403 sans second facteur ou sans institution
 * dans le jeton ; 403 sans la permission ; accès avec elle ; 404 pour une liaison d'une autre institution.
 */
@WebMvcTest(controllers = PortailDecisionResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, CertificationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class PortailDecisionResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final String P = "/api/v1/portail/liaisons/" + ID;
    static final String INSTITUTION = "INST-0042";
    static final List<String> PERMISSIONS = List.of("CER_PORTAL_READ", "CER_PORTAL_SIGN", "HOM_PORTAL_SUBMIT", "CER_DOSSIER_READ", "HOM_PORTAL_READ");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProcesVerbalService procesVerbaux;

    @MockitoBean
    ApprobationSecuriteService approbations;

    static Stream<Arguments> routes() {
        return Stream.of(
            Arguments.of(HttpMethod.GET, P + "/proces-verbal", "CER_PORTAL_READ"),
            Arguments.of(HttpMethod.POST, P + "/proces-verbal/signature", "CER_PORTAL_SIGN"),
            Arguments.of(HttpMethod.GET, P + "/approbation-securite", "CER_PORTAL_READ"),
            Arguments.of(HttpMethod.POST, P + "/attestations/PCI_DSS", "HOM_PORTAL_SUBMIT")
        );
    }

    static AbstractMockHttpServletRequestBuilder<?> req(HttpMethod m, String uri) {
        if (uri.contains("/attestations/")) {
            return multipart(uri).file(new MockMultipartFile("fichier", "pci.pdf", "application/pdf", "%PDF-1.7".getBytes())).header("X-Reauth-Assertion", "a");
        }
        return request(m, uri).header("X-Reauth-Assertion", "a");
    }

    static RequestPostProcessor jeton(String acr, String institution, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject("u-corr").claim("name", "Représentant");
                if (acr != null) {
                    j.claim("acr", acr);
                }
                if (institution != null) {
                    j.claim("institution_id", institution);
                }
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Sans jeton : 401")
    void sansJeton(HttpMethod m, String uri, String permission) throws Exception {
        mvc.perform(req(m, uri)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Sans second facteur ou sans institution dans le jeton : 403")
    void sansDoubleFacteurNiInstitution(HttpMethod m, String uri, String permission) throws Exception {
        mvc.perform(req(m, uri).with(jeton(null, INSTITUTION, permission))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri).with(jeton("1", INSTITUTION, permission))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri).with(jeton("2", null, permission))).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Sans la permission de la route : 403")
    void sansPermission(HttpMethod m, String uri, String permission) throws Exception {
        String[] autres = PERMISSIONS.stream().filter(p -> !p.equals(permission)).toArray(String[]::new);
        mvc.perform(req(m, uri).with(jeton("2", INSTITUTION, autres))).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Avec second facteur, institution et permission : accès accordé")
    void avecPermission(HttpMethod m, String uri, String permission) throws Exception {
        mvc.perform(req(m, uri).with(jeton("2", INSTITUTION, permission))).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Liaison d'une autre institution : 404 HOM_LIAISON_NOT_FOUND")
    void autreInstitution(HttpMethod m, String uri, String permission) throws Exception {
        CertificationException introuvable = CertificationException.introuvable("HOM_LIAISON_NOT_FOUND", "Ce parcours est introuvable.");
        when(procesVerbaux.consulterPortail(eq(ID), any())).thenThrow(introuvable);
        when(procesVerbaux.signerParticipant(eq(ID), any(), any())).thenThrow(introuvable);
        when(approbations.consulterPortail(eq(ID), any())).thenThrow(introuvable);
        when(approbations.deposerAttestation(eq(ID), any(), any(), any())).thenThrow(introuvable);
        mvc
            .perform(req(m, uri).with(jeton("2", "INST-AUTRE", permission)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_NOT_FOUND"));
    }

    @Test
    @DisplayName("Dépôt : pièce multipart relayée avec son nom, son type et son contenu ; institution transmise")
    void depot() throws Exception {
        mvc.perform(req(HttpMethod.POST, P + "/attestations/GSMA").with(jeton("2", INSTITUTION, "HOM_PORTAL_SUBMIT"))).andExpect(status().isOk());
        ArgumentCaptor<Fichier> f = ArgumentCaptor.forClass(Fichier.class);
        ArgumentCaptor<Acteur> a = ArgumentCaptor.forClass(Acteur.class);
        verify(approbations).deposerAttestation(eq(ID), eq("GSMA"), f.capture(), a.capture());
        assertThat(f.getValue().nom()).isEqualTo("pci.pdf");
        assertThat(f.getValue().typeMime()).isEqualTo("application/pdf");
        assertThat(new String(f.getValue().octets())).isEqualTo("%PDF-1.7");
        assertThat(a.getValue().institutionId()).isEqualTo(INSTITUTION);
        mvc
            .perform(multipart(P + "/attestations/GSMA").with(jeton("2", INSTITUTION, "HOM_PORTAL_SUBMIT")))
            .andExpect(status().isOk());
        verify(approbations).deposerAttestation(eq(ID), eq("GSMA"), isNull(), any());
        mvc
            .perform(req(HttpMethod.POST, P + "/proces-verbal/signature").with(jeton("2", INSTITUTION, "CER_PORTAL_SIGN")))
            .andExpect(status().isOk());
        verify(procesVerbaux).signerParticipant(eq(ID), any(), eq("a"));
    }
}
