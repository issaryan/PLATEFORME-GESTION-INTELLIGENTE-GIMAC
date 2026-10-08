package cm.gimac.des.homologation.web.rest.lot1;

import static cm.gimac.des.homologation.web.rest.lot1.LiaisonsCadrageResourceSecurityTest.jeton;
import static cm.gimac.des.homologation.web.rest.lot1.LiaisonsCadrageResourceSecurityTest.req;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.application.usecase.ReferentialMigrationService;
import cm.gimac.des.homologation.config.SecurityConfiguration;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import cm.gimac.des.homologation.domain.ReferentialMigration;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.ReferentialVersionSamples;
import cm.gimac.des.homologation.domain.enumeration.EtatMigration;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxEventRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.web.rest.errors.HomologationExceptionHandler;
import java.util.List;
import java.util.Optional;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.jhipster.config.JHipsterProperties;

/** Sécurité (D-61) des routes de DES-12 appelées par l'interface : lecture, contrôle, chronologie, migration. */
@WebMvcTest(controllers = ReferentialResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, HomologationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class ReferentialResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final UUID LIAISON = UUID.randomUUID();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManageReferentialUseCase useCase;

    @MockitoBean
    ReferentialVersionRepository versions;

    @MockitoBean
    ReferentialMigrationService migrations;

    @MockitoBean
    OutboxEventRepository evenements;

    @MockitoBean
    ReferentialGraphValidator validateur;

    @MockitoBean
    ReferentialCompletenessChecker completude;

    @BeforeEach
    void init() {
        ReferentialVersion v = ReferentialVersionSamples.publiee(TypeProjet.CARTE_INTERBANCAIRE, 1);
        v.setId(ID);
        when(versions.findById(ID)).thenReturn(Optional.of(v));
        when(versions.findByTypeProjetOrderByDateCreationDesc(any())).thenReturn(List.of(v));
        when(versions.findFirstByTypeProjetAndEtat(TypeProjet.CARTE_INTERBANCAIRE, EtatVersionReferentiel.PUBLIEE)).thenReturn(Optional.of(v));
        when(validateur.controler(anyList(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(new ReferentialGraphValidator.Rapport(List.of(), List.of("A"), java.util.Map.of("A", 0)));
        when(completude.check(any())).thenReturn(List.of());
        when(evenements.findByAggregateIdOrderByCreatedAtAsc(ID.toString())).thenReturn(List.of());
        when(migrations.simuler(ID)).thenReturn(List.of(new ReferentialMigrationService.Impact(LIAISON, "EP A", 3, 0, 0, 0, false, null)));
        ReferentialMigration m = new ReferentialMigration().id(UUID.randomUUID()).versionSourceId(UUID.randomUUID()).versionCibleId(ID).etat(EtatMigration.APPROUVEE).liaisonsRetenues("[\"" + LIAISON + "\"]");
        when(migrations.migrer(eq(ID), anyList(), any())).thenReturn(m);
    }

    static Stream<Arguments> routes() {
        String migration = "{\"liaisonsRetenues\":[\"" + LIAISON + "\"]}";
        return Stream.of(
            Arguments.of(HttpMethod.GET, "/api/v1/referentiels", null, "HOM_REFERENTIEL_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/referentiels/" + ID, null, "HOM_REFERENTIEL_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/referentiels/" + ID + "/historique", null, "HOM_REFERENTIEL_READ", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/referentiels/" + ID + "/controle", null, "HOM_REFERENTIEL_EDIT", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/referentiels/" + ID + "/migrations/simulation", null, "HOM_REFERENTIEL_MIGRATE", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/referentiels/" + ID + "/migrations", migration, "HOM_REFERENTIEL_MIGRATE", 202)
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("401 sans jeton, 403 sans second facteur, 403 sans la permission, accès avec elle")
    void matrice(HttpMethod m, String uri, String body, String permission, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
        mvc.perform(req(m, uri, body).with(jeton("1", permission))).andExpect(status().isForbidden());
        String[] autres = Stream.of("HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_MIGRATE", "HOM_REFERENTIEL_PUBLISH").filter(p -> !p.equals(permission)).toArray(String[]::new);
        mvc.perform(req(m, uri, body).with(jeton("2", autres))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("2", permission))).andExpect(status().is(ok));
    }

    @Test
    @DisplayName("Formes : version en vigueur, contrôle restitué pour DES-12, impact et migration")
    void formes() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/v1/referentiels/" + ID, null).with(jeton("2", "HOM_REFERENTIEL_READ"))).andExpect(jsonPath("$.versionActiveId").value(ID.toString()));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/referentiels/" + ID + "/controle", null).with(jeton("2", "HOM_REFERENTIEL_EDIT")))
            .andExpect(jsonPath("$.acyclique").value(true))
            .andExpect(jsonPath("$.ordreTopologique[0]").value("A"))
            .andExpect(jsonPath("$.conforme").value(true));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/referentiels/" + ID + "/migrations/simulation", null).with(jeton("2", "HOM_REFERENTIEL_MIGRATE")))
            .andExpect(jsonPath("$[0].liaisonId").value(LIAISON.toString()))
            .andExpect(jsonPath("$[0].institutionNom").value("EP A"));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/referentiels/" + ID + "/migrations", "{\"liaisonsRetenues\":[\"" + LIAISON + "\"]}").with(jeton("2", "HOM_REFERENTIEL_MIGRATE")))
            .andExpect(jsonPath("$.liaisonsRetenues[0]").value(LIAISON.toString()))
            .andExpect(jsonPath("$.etat").value("APPROUVEE"));
    }
}
