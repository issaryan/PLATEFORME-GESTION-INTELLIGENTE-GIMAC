package cm.gimac.des.homologation.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.application.usecase.LiaisonQueries;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes;
import cm.gimac.des.homologation.application.usecase.ParcoursLectures;
import cm.gimac.des.homologation.config.SecurityConfiguration;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.domain.service.TableauDeBordCalculateur;
import cm.gimac.des.homologation.web.rest.errors.HomologationExceptionHandler;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Sécurité du portail du participant (RG-59, D-61) : 401 sans jeton, 403 sans institution, sans second
 * facteur ou sans la permission ; une liaison d'une autre institution est introuvable (404).
 */
@WebMvcTest(controllers = PortailParcoursResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, HomologationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class PortailParcoursResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final String INSTITUTION = UUID.randomUUID().toString();
    static final List<String> TOUTES = List.of("HOM_PORTAL_READ", "HOM_PORTAL_SUBMIT", "HOM_LIAISON_READ", "HOM_STEP_MANAGE", "HOM_EVIDENCE_SUBMIT", "CER_PORTAL_READ", "CER_PORTAL_SIGN");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ParcoursLectures lectures;

    @MockitoBean
    ParcoursCommandes commandes;

    @MockitoBean
    LiaisonQueries liaisons;

    Parcours p;

    @BeforeEach
    void init() {
        ParcoursProjection projection = new ParcoursProjection();
        p = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "REMITTANCE");
        var ctx = new ParcoursProjection.Contexte(new Personne("c", "Conducteur de projet 1", Fonction.CONDUCTEUR), null, 2);
        when(lectures.dossierPortail(eq(ID), argThat((Acteur a) -> a != null && INSTITUTION.equals(a.institutionId())))).thenReturn(projection.dossierPortail(p, ctx, LocalDate.now()));
        when(lectures.dossierPortail(eq(ID), argThat((Acteur a) -> a != null && !INSTITUTION.equals(a.institutionId())))).thenThrow(
            HomologationException.notFound("HOM_LIAISON_NOT_FOUND", "Ce parcours est introuvable.", ID)
        );
        when(lectures.mesParcours(any())).thenReturn(List.of(p));
        when(lectures.resumePortail(p)).thenReturn(projection.resume(p, "Conducteur de projet GIMAC", LocalDate.now()));
        when(liaisons.fichePortail(eq(p.liaison()), any())).thenAnswer(i -> new LiaisonQueries.Fiche(p.liaison(), "Conducteur de projet GIMAC", null, List.of("REMITTANCE"), List.of(), List.of(), i.getArgument(1)));
        when(lectures.tachesPortail(any())).thenReturn(
            new TableauDeBordCalculateur(projection).tableau(List.of(p), List.of(Fonction.PARTICIPANT), new Acteur(null, null, Set.of(), INSTITUTION), p.liaison().getInstitutionId().toString(), Map.of(), LocalDate.now())
        );
        when(lectures.fichePortail(eq(ID), any())).thenReturn(projection.fiche(p, new Acteur(null, null, Set.of(), INSTITUTION), true));
    }

    static RequestPostProcessor correspondant(String institution, String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject(UUID.randomUUID().toString());
                if (acr != null) j.claim("acr", acr);
                if (institution != null) j.claim("institution_id", institution);
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static AbstractMockHttpServletRequestBuilder<?> req(HttpMethod m, String uri, String body) {
        if (body == null) {
            return request(m, uri);
        }
        if (body.equals("multipart")) {
            return multipart(uri).file(new MockMultipartFile("fichier", "cr.pdf", "application/pdf", "contenu".getBytes())).param("commentaire", "cr");
        }
        return request(m, uri).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    static Stream<Arguments> routes() {
        String l = "/api/v1/portail/liaisons";
        return Stream.of(
            Arguments.of(HttpMethod.GET, l, null, "HOM_PORTAL_READ", 200),
            Arguments.of(HttpMethod.GET, l + "/" + ID + "/dossier", null, "HOM_PORTAL_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/portail/taches", null, "HOM_PORTAL_READ", 200),
            Arguments.of(HttpMethod.PUT, l + "/" + ID + "/coordonnees-techniques", "{\"adresse\":\"essai.exemple\",\"port\":\"8443\"}", "HOM_PORTAL_SUBMIT", 204),
            Arguments.of(HttpMethod.POST, "/api/v1/portail/operations/" + ID + "/livrable", "multipart", "HOM_PORTAL_SUBMIT", 204),
            Arguments.of(HttpMethod.GET, l + "/" + ID + "/fiche-suivi", null, "HOM_PORTAL_READ", 200),
            Arguments.of(HttpMethod.PUT, l + "/" + ID + "/fiche-suivi", "{\"lignes\":[{\"serviceCode\":\"REMITTANCE\",\"ouvert\":true,\"commentaire\":null}]}", "HOM_PORTAL_SUBMIT", 204)
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("401 sans jeton, 403 sans institution, 403 sans second facteur, 403 sans la permission, accès avec elle")
    void matrice(HttpMethod m, String uri, String body, String permission, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
        mvc.perform(req(m, uri, body).with(correspondant(null, "2", permission))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(correspondant(INSTITUTION, null, permission))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(correspondant(INSTITUTION, "1", permission))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(correspondant(INSTITUTION, "2", TOUTES.stream().filter(x -> !x.equals(permission)).toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(correspondant(INSTITUTION, "2", permission))).andExpect(status().is(ok));
    }

    @Test
    @DisplayName("Cloisonnement : dossier d'une autre institution introuvable (404) ; projection sans nom interne")
    void cloisonnement() throws Exception {
        String uri = "/api/v1/portail/liaisons/" + ID + "/dossier";
        mvc
            .perform(req(HttpMethod.GET, uri, null).with(correspondant(UUID.randomUUID().toString(), "2", "HOM_PORTAL_READ")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_NOT_FOUND"));
        mvc
            .perform(req(HttpMethod.GET, uri, null).with(correspondant(INSTITUTION, "2", "HOM_PORTAL_READ")))
            .andExpect(jsonPath("$.conducteur.nom").value("Conducteur de projet GIMAC"))
            .andExpect(jsonPath("$.actionsPermises").isEmpty());
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/portail/liaisons", null).with(correspondant(INSTITUTION, "2", "HOM_PORTAL_READ")))
            .andExpect(jsonPath("$[0].chefDeProjetNom").value("Conducteur de projet GIMAC"))
            .andExpect(jsonPath("$[0].chefDeProjetId").isEmpty())
            .andExpect(jsonPath("$[0].statut").value("CADREE"));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/portail/taches", null).with(correspondant(INSTITUTION, "2", "HOM_PORTAL_READ")))
            .andExpect(jsonPath("$.fonction").value("PARTICIPANT"))
            .andExpect(jsonPath("$.compteurs[?(@.cle == 'tachesARealiser')].valeur").exists());
    }

    @Test
    @DisplayName("Livrable multipart transmis au cas d'utilisation ; sans fichier ; erreurs métier en ProblemDetail")
    void livrable() throws Exception {
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/portail/operations/" + ID + "/livrable", "multipart").with(correspondant(INSTITUTION, "2", "HOM_PORTAL_SUBMIT")))
            .andExpect(status().isNoContent());
        verify(commandes).deposerLivrablePortail(eq(ID), isNull(), isNull(), eq("contenu".getBytes()), eq("cr.pdf"), eq("application/pdf"), eq("cr"), any());
        mvc
            .perform(multipart("/api/v1/portail/operations/" + ID + "/livrable").with(correspondant(INSTITUTION, "2", "HOM_PORTAL_SUBMIT")))
            .andExpect(status().isNoContent());
        verify(commandes).deposerLivrablePortail(eq(ID), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
        doThrow(HomologationException.unprocessable("HOM_TECHNICAL_DATA_INVALID", "Adresse ou port invalide.")).when(commandes).communiquerCoordonnees(eq(ID), eq("hôte invalide!"), any(), any(), any());
        mvc
            .perform(req(HttpMethod.PUT, "/api/v1/portail/liaisons/" + ID + "/coordonnees-techniques", "{\"adresse\":\"hôte invalide!\",\"port\":\"8443\"}").with(correspondant(INSTITUTION, "2", "HOM_PORTAL_SUBMIT")))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_TECHNICAL_DATA_INVALID"));
        mvc.perform(req(HttpMethod.PUT, "/api/v1/portail/liaisons/" + ID + "/fiche-suivi", "{}").with(correspondant(INSTITUTION, "2", "HOM_PORTAL_SUBMIT"))).andExpect(status().isBadRequest());
    }
}
