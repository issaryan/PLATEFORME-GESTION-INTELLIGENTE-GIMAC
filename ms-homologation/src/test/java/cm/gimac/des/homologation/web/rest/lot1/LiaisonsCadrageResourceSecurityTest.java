package cm.gimac.des.homologation.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.application.port.in.OpenLiaisonUseCase;
import cm.gimac.des.homologation.application.usecase.LiaisonQueries;
import cm.gimac.des.homologation.config.SecurityConfiguration;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferentialVersionSamples;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.web.rest.errors.HomologationExceptionHandler;
import java.time.Instant;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/** Sécurité des routes du cadrage (D-61) avec la vraie chaîne de filtres. */
@WebMvcTest(controllers = LiaisonsCadrageResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, HomologationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class LiaisonsCadrageResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final UUID INSTITUTION = UUID.randomUUID();
    static final UUID CONDUCTEUR = UUID.randomUUID();
    static final String OUVERTURE = "{\"institutionId\":\"" + INSTITUTION + "\",\"typeProjet\":\"PAIEMENT_MOBILE\",\"chefDeProjetId\":\"" + CONDUCTEUR + "\"}";
    static final String DESIGNATION = "{\"chefDeProjetId\":\"" + CONDUCTEUR + "\",\"motif\":\"Réaffectation\"}";
    static final List<String> TOUTES = List.of("HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "PAR_INSTITUTION_READ");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OpenLiaisonUseCase useCase;

    @MockitoBean
    LiaisonQueries queries;

    Liaison liaison;

    @BeforeEach
    void init() {
        var v = ReferentialVersionSamples.publiee(TypeProjet.PAIEMENT_MOBILE, 1);
        v.setId(UUID.randomUUID());
        liaison = new Liaison().etat(EtatLiaison.OUVERTE).typeProjet(TypeProjet.PAIEMENT_MOBILE).institutionId(INSTITUTION).institutionNom("EP A").chefDeProjetId(CONDUCTEUR).dateOuverture(Instant.EPOCH).version(v);
        liaison.setId(ID);
        var fiche = new LiaisonQueries.Fiche(liaison, "Aminata Ngono", null, List.of("TRANSVERSE"), List.of(new LiaisonQueries.Intervenant("INSTITUTION", INSTITUTION.toString(), "EP A")), List.of());
        when(queries.lister(any(), anyInt(), anyInt(), anySet())).thenReturn(new PageImpl<>(List.of(fiche)));
        when(queries.fiche(eq(ID), anySet())).thenReturn(fiche);
        when(queries.conducteurs()).thenReturn(List.of(new CollaboratorView(CONDUCTEUR, "Aminata Ngono", Set.of(), null, true)));
        when(useCase.ouvrir(any(), any())).thenReturn(liaison);
        when(useCase.designerChefDeProjet(eq(ID), eq(CONDUCTEUR), any(), any())).thenReturn(liaison);
    }

    static Stream<Arguments> routes() {
        return Stream.of(
            Arguments.of(HttpMethod.GET, "/api/v1/liaisons", null, "HOM_LIAISON_LIST", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/liaisons/" + ID, null, "HOM_LIAISON_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/liaisons/conducteurs", null, "HOM_PROJECT_DESIGNATE", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/liaisons", OUVERTURE, "HOM_LIAISON_OPEN", 201),
            Arguments.of(HttpMethod.PUT, "/api/v1/liaisons/" + ID + "/chef-de-projet", DESIGNATION, "HOM_PROJECT_DESIGNATE", 200)
        );
    }

    static MockHttpServletRequestBuilder req(HttpMethod m, String uri, String body) {
        MockHttpServletRequestBuilder b = request(m, uri);
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(body);
        return b;
    }

    static RequestPostProcessor jeton(String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject(UUID.randomUUID().toString());
                if (acr != null) j.claim("acr", acr);
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("401 sans jeton, 403 sans second facteur, 403 sans la permission, accès avec elle")
    void matrice(HttpMethod m, String uri, String body, String permission, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
        mvc.perform(req(m, uri, body).with(jeton(null, permission))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("1", permission))).andExpect(status().isForbidden());
        String[] autres = TOUTES.stream().filter(p -> !p.equals(permission) && !(uri.equals("/api/v1/liaisons") && m == HttpMethod.GET && p.equals("HOM_LIAISON_READ")) && !(uri.endsWith("/conducteurs") && p.equals("HOM_LIAISON_OPEN"))).toArray(String[]::new);
        mvc.perform(req(m, uri, body).with(jeton("2", autres))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("2", permission))).andExpect(status().is(ok));
    }

    @Test
    @DisplayName("Formes : pagination, Location, fiche au format de l'interface ; erreurs métier en ProblemDetail")
    void formes() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/liaisons?mesProjets=true&etat=OUVERTE&typeProjet=PAIEMENT_MOBILE", null).with(jeton("2", "HOM_LIAISON_LIST")))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].chefDeProjetNom").value("Aminata Ngono"))
            .andExpect(jsonPath("$.contenu[0].phaseCourante").value("CADRAGE"))
            .andExpect(jsonPath("$.contenu[0].couvertureCertification.calculable").value(false))
            .andExpect(jsonPath("$.contenu[0].perimetre[0]").value("TRANSVERSE"));
        mvc.perform(req(HttpMethod.POST, "/api/v1/liaisons", OUVERTURE).with(jeton("2", "HOM_LIAISON_OPEN"))).andExpect(header().string("Location", "/api/v1/liaisons/" + ID));
        mvc.perform(req(HttpMethod.GET, "/api/v1/liaisons/conducteurs", null).with(jeton("2", "HOM_LIAISON_OPEN"))).andExpect(jsonPath("$[0].nom").value("Aminata Ngono"));
        when(useCase.ouvrir(any(), any())).thenThrow(HomologationException.conflict("HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE", "x", Map.of("statut", "CANDIDAT")));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/liaisons", OUVERTURE).with(jeton("2", "HOM_LIAISON_OPEN")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE"))
            .andExpect(jsonPath("$.details.statut").value("CANDIDAT"));
        mvc.perform(req(HttpMethod.POST, "/api/v1/liaisons", "{\"typeProjet\":\"PAIEMENT_MOBILE\"}").with(jeton("2", "HOM_LIAISON_OPEN"))).andExpect(status().isBadRequest());
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/liaisons?mesProjets=true", null).with(jwt().jwt(j -> j.subject("pas-un-uuid").claim("acr", "2")).authorities(new SimpleGrantedAuthority("HOM_LIAISON_LIST"))))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                req(HttpMethod.GET, "/api/v1/liaisons?mesProjets=true", null).with(
                    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("x").authorities(new SimpleGrantedAuthority("HOM_LIAISON_LIST"))
                )
            )
            .andExpect(status().isForbidden());
        liaison.setEtat(EtatLiaison.GELEE);
        mvc.perform(req(HttpMethod.GET, "/api/v1/liaisons/" + ID, null).with(jeton("2", "HOM_LIAISON_READ"))).andExpect(jsonPath("$.phaseCourante").value("GELEE"));
    }

    @Test
    @DisplayName("CRUD généré réservé à l'administration technique ; route interne au jeton de service")
    void cloisonnement() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/liaisons", null).with(jeton("2", TOUTES.toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, "/api/v1/liaisons/" + ID + "/intervenants", null).with(jeton("2", TOUTES.toArray(String[]::new)))).andExpect(status().isForbidden());
    }
}
