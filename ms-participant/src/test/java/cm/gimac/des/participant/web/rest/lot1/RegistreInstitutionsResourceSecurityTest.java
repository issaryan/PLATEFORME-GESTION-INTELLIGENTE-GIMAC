package cm.gimac.des.participant.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase;
import cm.gimac.des.participant.application.usecase.InstitutionRegistryQueries;
import cm.gimac.des.participant.config.SecurityConfiguration;
import cm.gimac.des.participant.config.TestSecurityConfiguration;
import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Prerequis;
import cm.gimac.des.participant.web.rest.errors.ParticipantExceptionHandler;
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
import org.springframework.dao.DataIntegrityViolationException;
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

/**
 * Tests de sécurité des routes de la DDRM (D-61, PAR-02 « Sécurité ») avec la vraie chaîne de
 * filtres : 401 sans jeton ; 403 sans second facteur (acr) ; 403 sans la permission de la route ;
 * accès avec la permission. Exécutés sans Docker (tranche web, cas d'utilisation simulés).
 */
@WebMvcTest(controllers = RegistreInstitutionsResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, ParticipantExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class RegistreInstitutionsResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final UUID T = UUID.randomUUID();
    static final String CANDIDATURE = """
        {"nom":"Banque B","sigle":"BB","pays":"CM","categorie":"BANQUE","codeIdentification":"BQBBCMCX","codeBanqueBeac":"B001"}""";
    static final String CONDITIONS = """
        {"codeParticipant":"BQ-B-001","mode":"DIRECT","compteReglement":"BEAC-001"}""";
    static final String DEPOT = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"documentSha256\":\"" + "a".repeat(64) + "\"}";
    static final String MOTIF = "{\"motif\":\"Pièce illisible\"}";
    static final String TRANSITION = "{\"vers\":\"ADHERENT\",\"motif\":\"Dossier complet\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManageInstitutionUseCase useCase;

    @MockitoBean
    InstitutionRegistryQueries queries;

    Institution institution;

    @BeforeEach
    void init() {
        institution = new Institution()
            .name("Banque B")
            .country("CM")
            .category(InstitutionCategory.BANQUE)
            .status(InstitutionStatus.CANDIDAT)
            .complianceState(ComplianceState.A_REGULARISER)
            .createdAt(Instant.EPOCH)
            .ddrmOfficerId(UUID.randomUUID())
            .ddrmOfficerName("Chargé DDRM");
        institution.setId(ID);
        AdhesionPiece piece = new AdhesionPiece().code("STATUTS").label("Statuts").mandatory(true).state(AdhesionPieceState.RECUE).institution(institution);
        StatusTransition t = new StatusTransition()
            .fromStatus(InstitutionStatus.CANDIDAT)
            .toStatus(InstitutionStatus.ADHERENT)
            .reason("Dossier complet")
            .requesterId(UUID.randomUUID())
            .requestedAt(Instant.EPOCH)
            .automaticTransition(false)
            .requiredApprovals(1)
            .state(TransitionState.PROPOSEE);
        t.setId(T);
        when(queries.lister(any(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of(institution)));
        when(queries.indicateurs()).thenReturn(new InstitutionRegistryQueries.Indicateurs(3, 2, 1, 4, 5, 0,
            List.of(new InstitutionRegistryQueries.FluxMensuel(java.time.YearMonth.of(2026, 10), 4, 1, 0))));
        when(queries.dossier(ID)).thenReturn(
            new InstitutionRegistryQueries.Dossier(
                institution,
                List.of(piece),
                List.of(t),
                List.of(new Prerequis("CODE_PARTICIPANT", "Code participant attribué", false, null)),
                Set.of(InstitutionStatus.ADHERENT, InstitutionStatus.CLOS_SANS_SUITE)
            )
        );
        when(useCase.prerequis(ID, InstitutionStatus.ADHERENT)).thenReturn(List.of(new Prerequis("MODE_ADHESION", "Mode", true, "DIRECT")));
        when(useCase.enregistrerCandidat(any(), any())).thenReturn(institution);
        when(useCase.modifierFiche(eq(ID), any(), any())).thenReturn(institution);
        when(useCase.definirConditionsAdhesion(eq(ID), any(), any())).thenReturn(institution);
        when(useCase.deposerPiece(eq(ID), eq("STATUTS"), any(), any())).thenReturn(piece);
        when(useCase.validerPiece(eq(ID), eq("STATUTS"), any())).thenReturn(piece);
        when(useCase.rejeterPiece(eq(ID), eq("STATUTS"), anyString(), any())).thenReturn(piece);
        when(useCase.proposerTransition(eq(ID), any(), anyString(), isNull(), any())).thenReturn(t);
        when(useCase.approuverTransition(eq(ID), eq(T), any(), any())).thenReturn(t);
        when(useCase.rejeterTransition(eq(ID), eq(T), anyString(), any())).thenReturn(t);
    }

    /** Route, permission exigée et statut attendu quand l'accès est accordé. */
    static Stream<Arguments> routes() {
        return Stream.of(
            Arguments.of(HttpMethod.GET, "/api/v1/institutions", null, "PAR_INSTITUTION_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/institutions/indicateurs", null, "PAR_INSTITUTION_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/institutions/" + ID, null, "PAR_INSTITUTION_READ", 200),
            Arguments.of(HttpMethod.GET, "/api/v1/institutions/" + ID + "/prerequis?etat=ADHERENT", null, "PAR_INSTITUTION_READ", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/institutions", CANDIDATURE, "PAR_INSTITUTION_CREATE", 201),
            Arguments.of(HttpMethod.PUT, "/api/v1/institutions/" + ID, CANDIDATURE, "PAR_INSTITUTION_EDIT", 200),
            Arguments.of(HttpMethod.PUT, "/api/v1/institutions/" + ID + "/conditions-adhesion", CONDITIONS, "PAR_INSTITUTION_EDIT", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/institutions/" + ID + "/pieces/STATUTS/depot", DEPOT, "PAR_ADHESION_INSTRUCT", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/institutions/" + ID + "/pieces/STATUTS/validation", null, "PAR_ADHESION_INSTRUCT", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/institutions/" + ID + "/pieces/STATUTS/rejet", MOTIF, "PAR_ADHESION_INSTRUCT", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/institutions/" + ID + "/transitions", TRANSITION, "PAR_TRANSITION_REQUEST", 201),
            Arguments.of(HttpMethod.POST, "/api/v1/institutions/" + ID + "/transitions/" + T + "/approbation", null, "PAR_TRANSITION_APPROVE", 200),
            Arguments.of(HttpMethod.POST, "/api/v1/institutions/" + ID + "/transitions/" + T + "/rejet", MOTIF, "PAR_TRANSITION_APPROVE", 200)
        );
    }

    private static MockHttpServletRequestBuilder req(HttpMethod m, String uri, String body) {
        MockHttpServletRequestBuilder b = request(m, uri);
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return b;
    }

    /** Jeton utilisateur : sujet UUID, niveau d'assurance acr, permissions de la matrice des rôles. */
    static RequestPostProcessor jeton(String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject(UUID.randomUUID().toString()).claim("name", "Agent DDRM");
                if (acr != null) {
                    j.claim("acr", acr);
                }
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Sans jeton : 401")
    void sansJeton(HttpMethod m, String uri, String body, String permission, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("D-61 : jeton sans second facteur (acr absent ou 1) : 403, même avec la permission")
    void sansDoubleFacteur(HttpMethod m, String uri, String body, String permission, int ok) throws Exception {
        mvc.perform(req(m, uri, body).with(jeton(null, permission))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("1", permission))).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Matrice des rôles : sans la permission de la route, 403")
    void sansPermission(HttpMethod m, String uri, String body, String permission, int ok) throws Exception {
        String[] autres = Stream.of(
            "PAR_INSTITUTION_READ",
            "PAR_INSTITUTION_CREATE",
            "PAR_INSTITUTION_EDIT",
            "PAR_ADHESION_INSTRUCT",
            "PAR_TRANSITION_REQUEST",
            "PAR_TRANSITION_APPROVE",
            "HOM_LIAISON_READ"
        )
            .filter(p -> !p.equals(permission))
            .toArray(String[]::new);
        mvc.perform(req(m, uri, body).with(jeton("2", autres))).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("Avec le second facteur et la permission de la route : accès accordé")
    void avecPermission(HttpMethod m, String uri, String body, String permission, int ok) throws Exception {
        mvc.perform(req(m, uri, body).with(jeton("2", permission))).andExpect(status().is(ok));
        mvc.perform(req(m, uri, body).with(jeton("3", permission))).andExpect(status().is(ok));
    }

    @Test
    @DisplayName("Ressources générées par JHipster : réservées à l'administration technique")
    void crudGenereReserveAdmin() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/institutions", null).with(jeton("2", "PAR_INSTITUTION_READ"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Jeton de service sans acr : aucune route métier")
    void jetonDeService() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/v1/institutions", null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Interfaces internes : jeton de service exigé")
    void interfacesInternes() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/v1/interne/institutions/" + ID, null).with(jeton("2", "PAR_INSTITUTION_READ"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, "/api/v1/portail/institution", null).with(jeton("2", "PAR_INSTITUTION_READ"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Réponses : pagination {contenu,total}, en-tête Location, dossier numérique")
    void formes() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/institutions?statut=CANDIDAT&q=banque&mesDossiers=true", null).with(jeton("2", "PAR_INSTITUTION_READ")))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].nom").value("Banque B"))
            .andExpect(jsonPath("$.contenu[0].statut").value("CANDIDAT"))
            .andExpect(jsonPath("$.contenu[0].chargeDdrm.nom").value("Chargé DDRM"));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/institutions", CANDIDATURE).with(jeton("2", "PAR_INSTITUTION_CREATE")))
            .andExpect(header().string("Location", "/api/v1/institutions/" + ID));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/institutions/" + ID, null).with(jeton("2", "PAR_INSTITUTION_READ")))
            .andExpect(jsonPath("$.pieces[0].code").value("STATUTS"))
            .andExpect(jsonPath("$.transitions[0].vers").value("ADHERENT"))
            .andExpect(jsonPath("$.prerequisAdhesion[0].satisfait").value(false))
            .andExpect(jsonPath("$.transitionsPermises[0]").value("ADHERENT"));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/institutions/indicateurs", null).with(jeton("2", "PAR_INSTITUTION_READ")))
            .andExpect(jsonPath("$.candidats").value(3))
            // Mois du flux en AAAA-MM, jamais en tableau : l'interface le découpe tel quel.
            .andExpect(jsonPath("$.fluxParMois[0].mois").value("2026-10"))
            .andExpect(jsonPath("$.fluxParMois[0].contacts").value(4));
    }

    @Test
    @DisplayName("Erreur métier : ProblemDetail avec code, détails et identifiant de corrélation")
    void erreurMetier() throws Exception {
        when(useCase.validerPiece(eq(ID), eq("STATUTS"), any())).thenThrow(
            ParticipantException.conflict("PAR_PIECE_NOT_RECEIVED", "Seule une pièce reçue peut être examinée", Map.of("code", "STATUTS"))
        );
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/institutions/" + ID + "/pieces/STATUTS/validation", null).with(jeton("2", "PAR_ADHESION_INSTRUCT")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PAR_PIECE_NOT_RECEIVED"))
            .andExpect(jsonPath("$.details.code").value("STATUTS"))
            .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("Course entre deux enregistrements : la contrainte d'unicité donne 409")
    void conflitConcurrent() throws Exception {
        when(useCase.enregistrerCandidat(any(), any())).thenThrow(new DataIntegrityViolationException("ux_institution"));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/institutions", CANDIDATURE).with(jeton("2", "PAR_INSTITUTION_CREATE")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PAR_CODE_ALREADY_USED"));
    }

    @Test
    @DisplayName("Validation d'entrée : pays hors CEMAC et empreinte mal formée refusés (400)")
    void validationEntree() throws Exception {
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/institutions", CANDIDATURE.replace("\"CM\"", "\"FR\"")).with(jeton("2", "PAR_INSTITUTION_CREATE")))
            .andExpect(status().isBadRequest());
        mvc
            .perform(
                req(HttpMethod.POST, "/api/v1/institutions/" + ID + "/pieces/STATUTS/depot", DEPOT.replace("a".repeat(64), "XYZ")).with(
                    jeton("2", "PAR_ADHESION_INSTRUCT")
                )
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sujet de jeton non UUID : refus")
    void sujetInvalide() throws Exception {
        mvc
            .perform(
                req(HttpMethod.POST, "/api/v1/institutions", CANDIDATURE).with(
                    jwt().jwt(j -> j.subject("pas-un-uuid").claim("acr", "2")).authorities(new SimpleGrantedAuthority("PAR_INSTITUTION_CREATE"))
                )
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PAR_USER_TOKEN_REQUIRED"));
    }
}
