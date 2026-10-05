package cm.gimac.des.iam.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.application.usecase.OrganisationService;
import cm.gimac.des.iam.config.SecurityConfiguration;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.web.rest.errors.IamExceptionHandler;
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
 * Routes de l'organisation (D-76, docs/lot1/mon-activite.md §1.3) avec la vraie chaîne de filtres :
 * permissions exactes, jeton à deux facteurs, routes internes réservées au jeton de service
 * INTERNAL_IAM_READ, formes JSON du contrat et erreurs problem+json.
 */
@WebMvcTest(controllers = { OrganisationResource.class, OrganisationInterneResource.class })
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, IamExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class OrganisationRoutesTest {

    static final String ID = "3f1c2d4e-0000-4000-8000-000000000001";
    static final String CHEF = "3f1c2d4e-0000-4000-8000-000000000002";
    static final Unite DG = new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true);
    static final Unite DES = new Unite("DES", "Département Exploitation et Supervision", TypeUnite.DEPARTEMENT, "DG", true);
    static final Unite RESEAU = new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true);
    static final String UNITE = "{\"code\":\"POLE_RESEAU\",\"libelle\":\"Pôle réseau\",\"type\":\"POLE\",\"parent\":\"PROD_INFO\"}";
    static final String MODIFICATION = "{\"libelle\":\"Pôle réseau\",\"type\":\"POLE\",\"parent\":\"PROD_INFO\",\"active\":false}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrganisationService service;

    @BeforeEach
    void init() {
        when(service.unites(anyBoolean())).thenReturn(List.of(DG, DES, RESEAU));
        when(service.creerUnite(any(), any())).thenReturn(RESEAU);
        when(service.modifierUnite(any(), eq("POLE_RESEAU"), any())).thenReturn(RESEAU);
        var rattachement = new OrganisationService.Rattachement(
            ID,
            "Awa Mballa",
            RESEAU,
            List.of(DG, DES, RESEAU),
            new OrganisationService.Responsable(CHEF, "Chef Mballa", "Chef du pôle"),
            List.of(CHEF, "dg"),
            3
        );
        when(service.rattachement(ID)).thenReturn(rattachement);
        when(service.equipe(ID)).thenReturn(List.of(new OrganisationService.MembreEquipe("m1", "Bob Ndi", "Technicien", "POLE_RESEAU", ID, 1, true)));
    }

    static RequestPostProcessor jeton(String sujet, String... permissions) {
        return jwt().jwt(j -> j.subject(sujet).claim("acr", "2").claim("name", "Agent")).authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static MockHttpServletRequestBuilder req(HttpMethod m, String uri, String body) {
        MockHttpServletRequestBuilder b = request(m, uri);
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return b;
    }

    @Test
    @DisplayName("Catalogue : lecture avec PIL_ACTIVITY_OWN ou IAM_ACCOUNT_READ, édition avec IAM_ACCOUNT_EDIT seulement")
    void permissionsCatalogue() throws Exception {
        String u = "/api/v1/organisation/unites";
        mvc.perform(req(HttpMethod.GET, u, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, u, null).with(jwt().authorities(new SimpleGrantedAuthority("PIL_ACTIVITY_OWN")))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "IAM_ACCOUNT_READ"))).andExpect(status().isOk());
        mvc
            .perform(req(HttpMethod.GET, u + "?actives=true", null).with(jeton(ID, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[2].code").value("POLE_RESEAU"))
            .andExpect(jsonPath("$[2].libelle").value("Pôle réseau"))
            .andExpect(jsonPath("$[2].type").value("POLE"))
            .andExpect(jsonPath("$[2].parent").value("PROD_INFO"))
            .andExpect(jsonPath("$[2].active").value(true))
            .andExpect(jsonPath("$[0].parent").isEmpty());
        verify(service).unites(true);

        for (String perm : List.of("IAM_ACCOUNT_READ", "PIL_ACTIVITY_OWN")) {
            mvc.perform(req(HttpMethod.POST, u, UNITE).with(jeton(ID, perm))).andExpect(status().isForbidden());
            mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", MODIFICATION).with(jeton(ID, perm))).andExpect(status().isForbidden());
        }
        mvc
            .perform(req(HttpMethod.POST, u, UNITE).with(jeton(ID, "IAM_ACCOUNT_EDIT")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/organisation/unites/POLE_RESEAU"))
            .andExpect(jsonPath("$.code").value("POLE_RESEAU"));
        verify(service).creerUnite(any(), eq(RESEAU));
        mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", MODIFICATION).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isOk());
        verify(service).modifierUnite(any(), eq("POLE_RESEAU"), eq(new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", false)));
        mvc.perform(req(HttpMethod.POST, u, "{\"code\":\"X\",\"type\":\"POLE\"}").with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", "{\"libelle\":\"x\",\"type\":\"POLE\"}").with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Erreurs du contrat en problem+json : ORG_CYCLE 409, ORG_UNITE_INCONNUE 422")
    void erreurs() throws Exception {
        when(service.modifierUnite(any(), eq("DES"), any())).thenThrow(IamException.conflit("ORG_CYCLE", "Cycle", Map.of()));
        when(service.creerUnite(any(), eq(new Unite("POLE_X", "x", TypeUnite.POLE, "ANCIEN", true)))).thenThrow(IamException.nonTraitable("ORG_UNITE_INCONNUE", "Inconnue", Map.of()));
        mvc
            .perform(req(HttpMethod.PUT, "/api/v1/organisation/unites/DES", MODIFICATION).with(jeton(ID, "IAM_ACCOUNT_EDIT")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORG_CYCLE"));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/organisation/unites", "{\"code\":\"POLE_X\",\"libelle\":\"x\",\"type\":\"POLE\",\"parent\":\"ANCIEN\",\"active\":true}").with(jeton(ID, "IAM_ACCOUNT_EDIT")))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("ORG_UNITE_INCONNUE"));
    }

    @Test
    @DisplayName("GET organisation/moi : PIL_ACTIVITY_OWN, rattachement du sujet du jeton, forme du contrat")
    void moi() throws Exception {
        String u = "/api/v1/organisation/moi";
        mvc.perform(req(HttpMethod.GET, u, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.GET, u, null).with(jeton(ID, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ID))
            .andExpect(jsonPath("$.nomAffiche").value("Awa Mballa"))
            .andExpect(jsonPath("$.unite.code").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.chemin[0].code").value("DG"))
            .andExpect(jsonPath("$.chemin[2].code").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.responsable.id").value(CHEF))
            .andExpect(jsonPath("$.responsable.nomAffiche").value("Chef Mballa"))
            .andExpect(jsonPath("$.responsable.fonction").value("Chef du pôle"))
            .andExpect(jsonPath("$.chaine[0]").value(CHEF))
            .andExpect(jsonPath("$.equipeDirecte").value(3));
    }

    @Test
    @DisplayName("Routes internes : jeton de service INTERNAL_IAM_READ seulement ; rattachement et équipe avec niveau")
    void interne() throws Exception {
        String u = "/api/v1/interne/organisation/" + ID;
        for (String uri : List.of(u, u + "/equipe")) {
            mvc.perform(req(HttpMethod.GET, uri, null)).andExpect(status().isUnauthorized());
            mvc.perform(req(HttpMethod.GET, uri, null).with(jeton(ID, "PIL_ACTIVITY_OWN", "IAM_ACCOUNT_READ", "PIL_TEAM_READ"))).andExpect(status().isForbidden());
        }
        var service = jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
        mvc.perform(req(HttpMethod.GET, u, null).with(service)).andExpect(status().isOk()).andExpect(jsonPath("$.chaine.length()").value(2));
        mvc
            .perform(req(HttpMethod.GET, u + "/equipe", null).with(service))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("m1"))
            .andExpect(jsonPath("$[0].nomAffiche").value("Bob Ndi"))
            .andExpect(jsonPath("$[0].fonction").value("Technicien"))
            .andExpect(jsonPath("$[0].unite").value("POLE_RESEAU"))
            .andExpect(jsonPath("$[0].responsableId").value(ID))
            .andExpect(jsonPath("$[0].niveau").value(1))
            .andExpect(jsonPath("$[0].actif").value(true));
    }
}
