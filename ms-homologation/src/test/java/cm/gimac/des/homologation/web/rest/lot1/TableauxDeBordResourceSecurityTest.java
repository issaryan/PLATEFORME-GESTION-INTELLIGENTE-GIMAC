package cm.gimac.des.homologation.web.rest.lot1;

import static cm.gimac.des.homologation.web.rest.lot1.ParcoursResourceSecurityTest.jeton;
import static cm.gimac.des.homologation.web.rest.lot1.ParcoursResourceSecurityTest.req;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.application.usecase.ParcoursLectures;
import cm.gimac.des.homologation.config.SecurityConfiguration;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.domain.service.TableauDeBordCalculateur;
import cm.gimac.des.homologation.web.rest.errors.HomologationExceptionHandler;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.jhipster.config.JHipsterProperties;

/** Sécurité des tableaux de bord, du pilotage et des intervenants (D-58, D-61). */
@WebMvcTest(controllers = TableauxDeBordResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, HomologationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class TableauxDeBordResourceSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ParcoursLectures lectures;

    @BeforeEach
    void init() {
        var calcul = new TableauDeBordCalculateur(new ParcoursProjection());
        var p = ParcoursSamples.parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        when(lectures.tableauActeur(any(), any())).thenReturn(calcul.tableau(List.of(p), List.of(Fonction.CHEF_DES), new Acteur(null, null, Set.of(), null), null, Map.of(), LocalDate.now()));
        when(lectures.pilotage()).thenReturn(calcul.pilotage(List.of(p), LocalDate.now()));
        when(lectures.intervenants(Fonction.RSSI)).thenReturn(List.of(new Personne("U-RSSI", "RSSI", Fonction.RSSI)));
        // Conducteur de la liaison d'exemple : il suit ses opérations engagées (forme complète servie).
        var conducteur = new Acteur(ParcoursSamples.CONDUCTEUR, "Conducteur", Set.of("ROLE_CONDUCTEUR_PROJET", "HOM_OPERATION_VALIDATE"), null);
        when(lectures.mesOperations(any())).thenReturn(calcul.mesOperations(List.of(p), conducteur, LocalDate.now()));
    }

    /** D-98 : permissions qui ouvrent « Mes actions de certification » (catalogue des fonctionnalités, OPA, @PreAuthorize). */
    static final List<String> ACTEURS_CERTIFICATION = List.of(
        "HOM_LIAISON_READ", "HOM_DASHBOARD_READ", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_STEP_MANAGE", "HOM_OPERATION_ASSIGN",
        "HOM_PROJECT_CONDUCT", "HOM_FUNCTION_DDRM", "HOM_FUNCTION_PRODUCTION", "HOM_FUNCTION_NETWORK", "HOM_FUNCTION_SYSTEM", "HOM_FUNCTION_OPERATIONS",
        "CER_PV_ESTABLISH", "CER_QUITUS_TECHNIQUE_SIGN", "CER_DOSSIER_VERIFY", "CER_SECURITY_VERIFY", "CER_CHANGE_RECORD_LINK", "CER_QUITUS_CONFORMITE_SIGN",
        "CER_QUITUS_DG_SIGN"
    );

    static Stream<Arguments> routes() {
        return Stream.of(
            Arguments.of("/api/v1/tableaux-de-bord/acteur?fonctions=CHEF_DES", List.of("HOM_DASHBOARD_READ")),
            Arguments.of("/api/v1/tableaux-de-bord/pilotage", List.of("HOM_DASHBOARD_READ")),
            Arguments.of("/api/v1/intervenants?fonction=RSSI", List.of("HOM_LIAISON_READ", "HOM_OPERATION_ASSIGN", "HOM_STEP_MANAGE")),
            Arguments.of("/api/v1/parcours/mes-operations", ACTEURS_CERTIFICATION)
        );
    }

    @ParameterizedTest(name = "GET {0}")
    @MethodSource("routes")
    @DisplayName("401 sans jeton, 403 sans second facteur, 403 sans la permission, 200 avec chacune des permissions admises")
    void matrice(String uri, List<String> admises) throws Exception {
        mvc.perform(req(HttpMethod.GET, uri, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, uri, null).with(jeton(null, admises.get(0)))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, uri, null).with(jeton("1", admises.get(0)))).andExpect(status().isForbidden());
        String[] autres = ParcoursResourceSecurityTest.TOUTES.stream().filter(x -> !admises.contains(x)).toArray(String[]::new);
        mvc.perform(req(HttpMethod.GET, uri, null).with(jeton("2", autres))).andExpect(status().isForbidden());
        for (String admise : admises) {
            mvc.perform(req(HttpMethod.GET, uri, null).with(jeton("2", admise))).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Formes : compteurs et liens, pilotage et six questions, intervenants ; fonctions lues dans le paramètre")
    void formes() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/tableaux-de-bord/acteur?fonctions=CHEF_DES,SNA", null).with(jeton("2", "HOM_DASHBOARD_READ", "ROLE_CHEF_DES")))
            .andExpect(jsonPath("$.fonction").value("CHEF_DES"))
            .andExpect(jsonPath("$.compteurs[?(@.cle == 'pvASigner')].ton").value("ok"))
            .andExpect(jsonPath("$.liaisons[0].reference").value("LIA-2026-001"));
        verify(lectures).tableauActeur(eq(List.of(Fonction.CHEF_DES, Fonction.SNA)), any());
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/tableaux-de-bord/pilotage", null).with(jeton("2", "HOM_DASHBOARD_READ")))
            .andExpect(jsonPath("$.projetsActifs").value(1))
            .andExpect(jsonPath("$.repartitionParEtape.length()").value(9))
            .andExpect(jsonPath("$.questions.servicesPlusDemandes[0].serviceCode").value("RETRAIT"));
        mvc.perform(req(HttpMethod.GET, "/api/v1/intervenants?fonction=RSSI", null).with(jeton("2", "HOM_LIAISON_READ"))).andExpect(jsonPath("$[0].id").value("U-RSSI"));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/tableaux-de-bord/acteur?fonctions=INCONNUE", null).with(jeton("2", "HOM_DASHBOARD_READ")))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_FUNCTION_UNKNOWN"));
        mvc.perform(req(HttpMethod.GET, "/api/v1/intervenants?fonction=X", null).with(jeton("2", "HOM_LIAISON_READ"))).andExpect(status().isBadRequest());
        assertThat(TableauxDeBordResource.fonctions(null)).isEmpty();
        assertThat(TableauxDeBordResource.fonctions(" , SNA ,")).containsExactly(Fonction.SNA);
        assertThatThrownBy(() -> TableauxDeBordResource.fonctions("x")).isInstanceOf(HomologationException.class);
    }

    @Test
    @DisplayName("D-98 : mes opérations de l'appelant, sans fonction exigée ; refus sans permission d'acteur du parcours")
    void mesOperations() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/parcours/mes-operations", null).with(jeton("2", "CER_SECURITY_VERIFY")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aAgir").isArray())
            .andExpect(jsonPath("$.aSuivre").isArray())
            .andExpect(jsonPath("$.liaisons[0].reference").value("LIA-2026-001"))
            .andExpect(jsonPath("$.liaisons[0].conducteur").value(true))
            .andExpect(jsonPath("$.liaisons[0].jalons[*].jalon").value(org.hamcrest.Matchers.hasItem("EN_PRODUCTION")))
            .andExpect(jsonPath("$.aSuivre[0].lien").value(org.hamcrest.Matchers.startsWith("/des/parcours/")))
            .andExpect(jsonPath("$.aSuivre[0].responsable.fonction").exists());
        verify(lectures).mesOperations(any());
        mvc.perform(req(HttpMethod.GET, "/api/v1/parcours/mes-operations", null).with(jeton("2", "HOM_LIAISON_LIST", "PAR_INSTITUTION_READ"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D-94 : fonctions de l'appelant déduites du catalogue et des profils du jeton ; sans second facteur : 403")
    void mesFonctions() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/v1/moi/fonctions", null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, "/api/v1/moi/fonctions", null).with(jeton("1", "ROLE_CHEF_DES"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/moi/fonctions", null).with(jeton("2", "ROLE_CHEF_DES", "ROLE_SNA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code").value(org.hamcrest.Matchers.contains("CHEF_DES", "CHEFS_DEPARTEMENT", "SNA")))
            .andExpect(jsonPath("$[0].libelle").value("Chef du département DES"))
            .andExpect(jsonPath("$[0].profils[0]").value("ROLE_CHEF_DES"))
            .andExpect(jsonPath("$[2].externe").value(false));
        mvc.perform(req(HttpMethod.GET, "/api/v1/moi/fonctions", null).with(jeton("2", "HOM_LIAISON_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
