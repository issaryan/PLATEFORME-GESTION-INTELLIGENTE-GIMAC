package cm.gimac.des.iam.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.application.usecase.FonctionnalitesService;
import cm.gimac.des.iam.application.usecase.ReauthentificationService;
import cm.gimac.des.iam.config.SecurityConfiguration;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite.Droit;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite.Predicat;
import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite.Widget;
import cm.gimac.des.iam.domain.fonctionnalite.NatureDroit;
import cm.gimac.des.iam.web.rest.errors.IamExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Routes du catalogue des fonctionnalités (D-94, docs/lot1/ecrans-par-configuration.md §2) avec la vraie chaîne de
 * filtres : lecture par tout compte interne authentifié (401 sans jeton, 403 pour un compte d'institution),
 * modification réservée à IAM_PROFILE_EDIT avec motif et clé de sécurité (428 sans réauthentification), forme JSON.
 */
@WebMvcTest(controllers = FonctionnalitesResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, IamExceptionHandler.class, ReauthAdministration.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class FonctionnalitesRoutesTest {

    static final String ID = "3f1c2d4e-0000-4000-8000-000000000001";
    static final String U = "/api/v1/configuration/fonctionnalites";
    static final String CORPS =
        "{\"application\":\"des\",\"module\":\"homologation\",\"libelle\":\"Projets\",\"groupe\":\"pilotage\",\"ordre\":5,\"icone\":\"route\",\"visible\":false," +
        "\"permissions\":{\"tous\":[\"HOM_DASHBOARD_READ\"],\"unDe\":[]},\"widgets\":[{\"cle\":\"cockpit\",\"permissions\":{\"tous\":[],\"unDe\":[\"HOM_PROJECT_DESIGNATE\"]}}]," +
        "\"droits\":[{\"cle\":\"HOM_LIAISON_LIST\",\"nature\":\"CONSULTER\",\"permission\":\"HOM_LIAISON_LIST\",\"libelle\":\"droits.permission.HOM_LIAISON_LIST\"}]," +
        "\"motif\":\"Revue de la navigation\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FonctionnalitesService service;

    @MockitoBean
    ReauthentificationService reauth;

    @MockitoBean
    AvatarService avatars;

    static Fonctionnalite liaisons(Instant le) {
        return new Fonctionnalite(
            "liaisons",
            "des",
            "homologation",
            "Projets",
            "pilotage",
            5,
            "route",
            false,
            new Predicat(List.of("HOM_DASHBOARD_READ"), List.of()),
            List.of(new Widget("cockpit", new Predicat(List.of(), List.of("HOM_PROJECT_DESIGNATE")))),
            List.of(new Droit("HOM_LIAISON_LIST", NatureDroit.CONSULTER, "HOM_LIAISON_LIST", "droits.permission.HOM_LIAISON_LIST")),
            le,
            le == null ? null : new Fonctionnalite.Auteur(ID, "Chef")
        );
    }

    @BeforeEach
    void init() {
        when(service.lister(any())).thenReturn(List.of(liaisons(null)));
        when(service.lister("intranet")).thenThrow(IamException.invalide("IAM_FONCTIONNALITE_INVALIDE", "Application inconnue", Map.of("champ", "application")));
        when(service.modifier(any(), eq("liaisons"), any(), eq("Revue de la navigation"))).thenReturn(liaisons(Instant.parse("2026-10-07T09:00:00Z")));
        when(service.modifier(any(), eq("inconnue"), any(), anyString())).thenThrow(IamException.introuvable("IAM_FONCTIONNALITE_INTROUVABLE", "inconnue"));
        when(reauth.verifier(eq("valide"), anyString(), anyString())).thenReturn(true);
    }

    static RequestPostProcessor jeton(String... permissions) {
        return jwt().jwt(j -> j.subject(ID).claim("acr", "2").claim("name", "Chef")).authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static MockHttpServletRequestBuilder req(HttpMethod m, String uri, String body) {
        MockHttpServletRequestBuilder b = request(m, uri);
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return b;
    }

    @Test
    @DisplayName("GET : tout compte interne authentifié, forme du contrat ; compte d'institution refusé ; application inconnue 400")
    void lecture() throws Exception {
        mvc.perform(req(HttpMethod.GET, U, null)).andExpect(status().isUnauthorized());
        mvc
            .perform(req(HttpMethod.GET, U + "?application=des", null).with(jeton("PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].cle").value("liaisons"))
            .andExpect(jsonPath("$[0].application").value("des"))
            .andExpect(jsonPath("$[0].module").value("homologation"))
            .andExpect(jsonPath("$[0].libelle").value("Projets"))
            .andExpect(jsonPath("$[0].groupe").value("pilotage"))
            .andExpect(jsonPath("$[0].ordre").value(5))
            .andExpect(jsonPath("$[0].icone").value("route"))
            .andExpect(jsonPath("$[0].visible").value(false))
            .andExpect(jsonPath("$[0].permissions.tous[0]").value("HOM_DASHBOARD_READ"))
            .andExpect(jsonPath("$[0].permissions.unDe.length()").value(0))
            .andExpect(jsonPath("$[0].widgets[0].cle").value("cockpit"))
            .andExpect(jsonPath("$[0].widgets[0].permissions.unDe[0]").value("HOM_PROJECT_DESIGNATE"))
            .andExpect(jsonPath("$[0].droits[0].cle").value("HOM_LIAISON_LIST"))
            .andExpect(jsonPath("$[0].droits[0].nature").value("CONSULTER"))
            .andExpect(jsonPath("$[0].droits[0].permission").value("HOM_LIAISON_LIST"))
            .andExpect(jsonPath("$[0].droits[0].libelle").value("droits.permission.HOM_LIAISON_LIST"))
            .andExpect(jsonPath("$[0].modifieLe").doesNotExist());
        verify(service).lister("des");
        // Sans aucune permission : la navigation se construit quand même (compte interne).
        mvc.perform(req(HttpMethod.GET, U, null).with(jeton())).andExpect(status().isOk());
        verify(service).lister(isNull());
        // Compte d'institution (portail) : refusé.
        mvc
            .perform(req(HttpMethod.GET, U, null).with(jwt().jwt(j -> j.subject(ID).claim("acr", "2").claim("institution_id", "INST-1")).authorities(new SimpleGrantedAuthority("HOM_PORTAL_READ"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_COMPTE_INTERNE_EXIGE"));
        mvc
            .perform(req(HttpMethod.GET, U + "?application=intranet", null).with(jeton("PIL_ACTIVITY_OWN")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_FONCTIONNALITE_INVALIDE"));
    }

    @Test
    @DisplayName("PUT : IAM_PROFILE_EDIT, motif, clé de sécurité FIDO2 (428), réponse avec la trace ; clé inconnue 404 ; corps invalide 400")
    void modification() throws Exception {
        String u = U + "/liaisons";
        mvc.perform(req(HttpMethod.PUT, u, CORPS)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.PUT, u, CORPS).with(jeton("IAM_ACCOUNT_READ")).header("X-Reauth-Assertion", "valide")).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.PUT, u, CORPS).with(jeton("IAM_PROFILE_EDIT"))).andExpect(status().is(428));
        mvc.perform(req(HttpMethod.PUT, u, CORPS).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "autre")).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.PUT, u, CORPS).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cle").value("liaisons"))
            .andExpect(jsonPath("$.libelle").value("Projets"))
            .andExpect(jsonPath("$.modifieLe").value("2026-10-07T09:00:00Z"))
            .andExpect(jsonPath("$.modifiePar.id").value(ID))
            .andExpect(jsonPath("$.modifiePar.nomAffiche").value("Chef"));
        Fonctionnalite l = liaisons(null);
        Fonctionnalite demandee = new Fonctionnalite(null, l.application(), l.module(), l.libelle(), l.groupe(), l.ordre(), l.icone(), l.visible(), l.permissions(), l.widgets(), l.droits(), null, null);
        verify(service).modifier(any(), eq("liaisons"), eq(demandee), eq("Revue de la navigation"));
        mvc
            .perform(req(HttpMethod.PUT, U + "/inconnue", CORPS).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("IAM_FONCTIONNALITE_INTROUVABLE"));
        // Motif absent ou permissions absentes : refus dès la requête.
        mvc.perform(req(HttpMethod.PUT, u, CORPS.replace(",\"motif\":\"Revue de la navigation\"", "")).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide")).andExpect(status().isBadRequest());
        mvc
            .perform(req(HttpMethod.PUT, u, "{\"application\":\"des\",\"module\":\"m\",\"libelle\":\"x\",\"icone\":\"route\",\"motif\":\"m\"}").with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide"))
            .andExpect(status().isBadRequest());
    }
}
