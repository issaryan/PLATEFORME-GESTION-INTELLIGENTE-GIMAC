package cm.gimac.des.iam.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.application.usecase.sandbox.ComptesSandboxSagaService;
import cm.gimac.des.iam.application.usecase.sandbox.ComptesSandboxSagaService.CompteSaga;
import cm.gimac.des.iam.application.usecase.sandbox.MatriceSandboxSagaService;
import cm.gimac.des.iam.application.usecase.sandbox.MatriceSandboxSagaService.Publication;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService.VueDelegation;
import cm.gimac.des.iam.application.usecase.sandbox.ReexamensAccesService;
import cm.gimac.des.iam.application.usecase.sandbox.ReexamensAccesService.Campagne;
import cm.gimac.des.iam.application.usecase.sandbox.ReexamensAccesService.VueCampagne;
import cm.gimac.des.iam.application.usecase.sandbox.IntrospectionSandboxService;
import cm.gimac.des.iam.domain.sandbox.Delegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatEffectif;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.CompteProtege;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatPerimetre;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.EtatProtection;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.PagePerimetres;
import cm.gimac.des.iam.application.usecase.sandbox.PerimetresSandboxService.PageProtections;
import cm.gimac.des.iam.config.SecurityConfiguration;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import cm.gimac.des.iam.domain.sandbox.Introspection;
import cm.gimac.des.iam.domain.sandbox.PerimetreSuspendu;
import cm.gimac.des.iam.web.rest.errors.IamExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Routes de la sandbox dans MS-IAM (D-119, US-EXI-06) avec la vraie chaîne de filtres : étapes de saga réservées au
 * jeton de service portant INTERNAL_IAM_SBX_SAGA, introspection à INTERNAL_IAM_SBX_INTROSPECT, lectures de la console à
 * IAM_ACCOUNT_READ sous deux facteurs ; INTERNAL_IAM_READ seul n'ouvre aucune route de la sandbox.
 */
@WebMvcTest(controllers = { SandboxSagaInterneResource.class, SandboxIntrospectionResource.class, SandboxConsoleResource.class, SandboxAccessReviewResource.class })
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, IamExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class SandboxPerimetresRoutesTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PerimetresSandboxService perimetres;

    @MockitoBean
    IntrospectionSandboxService introspection;

    @MockitoBean
    AvatarService avatars;

    @MockitoBean
    DelegationsSandboxService delegations;

    @MockitoBean
    ComptesSandboxSagaService comptesSaga;

    @MockitoBean
    MatriceSandboxSagaService matriceGlobale;

    @MockitoBean
    ReexamensAccesService reexamens;

    static VueDelegation vue(boolean rejouee) {
        return new VueDelegation(
            new Delegation("0f0e0d0c-0000-4000-8000-000000000001", TypeDelegation.MANDAT, "p1", "99001", Set.of("SBX_CER_EXECUTION"), Set.of(), T, T.plusSeconds(86400), EtatDelegation.ACCORDEE, "sa", null, null, T, null),
            EtatEffectif.ACTIVE, "Paul Ebode", rejouee);
    }

    static RequestPostProcessor service(String... permissions) {
        return jwt().jwt(j -> j.subject("sa-sbx-espace").claim("preferred_username", "service-account-sbx-espace")).authorities(java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(org.springframework.security.core.GrantedAuthority[]::new));
    }

    static RequestPostProcessor console(String acr, String... permissions) {
        return jwt().jwt(j -> j.subject("demo-super-admin").claim("acr", acr)).authorities(java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(org.springframework.security.core.GrantedAuthority[]::new));
    }

    @BeforeEach
    void init() {
        when(perimetres.suspendre(any(), eq("99001"), eq(true), eq("Statut SUSPENDU"), eq("saga-1/3"))).thenReturn(new EtatPerimetre("99001", true, T, 4, false));
        when(perimetres.suspendre(any(), eq("99001"), eq(false), isNull(), isNull())).thenReturn(new EtatPerimetre("99001", false, null, 0, false));
        when(perimetres.lire("99001")).thenReturn(new EtatPerimetre("99001", true, T, 0, false));
        when(perimetres.proteger(any(), eq("c1"), eq(true), eq("saga-2/1"))).thenReturn(new EtatProtection("c1", "99001", true, false));
        when(perimetres.suspendus(anyInt(), anyInt())).thenReturn(new PagePerimetres(List.of(new PerimetreSuspendu("99001", T, "sa", "m")), 1, 0, 20));
        when(perimetres.protections(anyInt(), anyInt())).thenReturn(new PageProtections(List.of(new CompteProtege("c1", "Awa N", "99001", T)), 1, 0, 20));
        when(delegations.accorderMandat(any(), eq("p1"), eq("99001"), eq(Set.of("SBX_CER_EXECUTION")), eq(T), eq(T.plusSeconds(86400)), eq("saga-m/4"))).thenReturn(vue(false));
        when(delegations.accorderMandat(any(), eq("p1"), eq("99001"), eq(Set.of("SBX_CER_EXECUTION")), eq(T), eq(T.plusSeconds(86400)), eq("saga-m/5"))).thenReturn(vue(true));
        when(delegations.lire("d1")).thenReturn(vue(false));
        when(delegations.revoquer(any(), eq("d1"), eq("Fin"), eq("saga-r/1"))).thenReturn(vue(false));
        when(delegations.prolonger(any(), eq("d1"), eq(T.plusSeconds(172800)), isNull())).thenReturn(vue(false));
        when(delegations.lister(eq(TypeDelegation.MANDAT), anyInt(), anyInt())).thenReturn(new DelegationsSandboxService.PageDelegations(List.of(vue(false)), 1, 0, 20));
        when(introspection.introspecter("j1", "99001", null)).thenReturn(Introspection.refusee("s", "PERIMETRE_SUSPENDU"));
        when(introspection.introspecter("j2", null, null)).thenReturn(new Introspection(true, "s", "99001", Set.of("SBX_CER_EXECUTION"), "COMPTE", null, T, Set.of(), null));
        when(introspection.introspecter("j3", null, "SBX-TRACE")).thenReturn(new Introspection(true, "pr", null, Set.of(), "PRESTATAIRE", "a1", T, Set.of("SBX-TRACE"), null));
        VueDelegation acces = new VueDelegation(
            new Delegation("a1", TypeDelegation.PRESTATAIRE, "pr", null, Set.of(), Set.of("SBX-TRACE"), T, T.plusSeconds(14400), EtatDelegation.DEMANDEE, "demo-super-admin", null, "Correctif", T, null),
            EtatEffectif.DEMANDEE, "Jean Mbarga", false);
        when(delegations.demanderAccesPrestataire(any(), eq("pr"), eq(List.of("SBX-TRACE")), eq(T), eq(T.plusSeconds(14400)), eq("Correctif"))).thenReturn(acces);
        when(delegations.approuverAccesPrestataire(any(), eq("a1"))).thenReturn(acces);
        when(delegations.refuserAccesPrestataire(any(), eq("a1"), eq("Non"))).thenReturn(acces);
        when(delegations.revoquerAccesPrestataire(any(), eq("a1"), eq("Fin"))).thenReturn(acces);
        CompteSaga compte = new CompteSaga("c1", "awa.test", "Awa Test", "awa@test.cm", "99001", "99001", true, T, List.of("ROLE_SBX_TESTEUR"), List.of("SBX_CER_EXECUTION"), false);
        when(comptesSaga.ouvrir(any(), any(), eq("saga-o/1"))).thenReturn(compte);
        when(comptesSaga.ouvrir(any(), any(), eq("saga-o/replay"))).thenReturn(new CompteSaga("c1", "awa.test", "Awa Test", "awa@test.cm", "99001", "99001", true, T, List.of("ROLE_SBX_TESTEUR"), List.of("SBX_CER_EXECUTION"), true));
        when(comptesSaga.lire("c1")).thenReturn(compte);
        when(comptesSaga.matrice(any(), eq("c1"), eq(Set.of("ROLE_SBX_TESTEUR")), eq("Revue"), eq("saga-m/2"))).thenReturn(compte);
        when(matriceGlobale.publier(any(), eq(7L), eq(java.util.Map.of("ROLE_SBX_LECTEUR", Set.of("SBX_ISO_SIMULATION"))), eq("saga-m/7")))
            .thenReturn(new Publication(7, false));
        java.util.UUID rid = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101");
        Campagne campagne = new Campagne(rid, "2026-T4", cm.gimac.des.iam.domain.enumeration.ReviewState.OUVERTE, T, null, null, null, 1);
        VueCampagne vueCampagne = new VueCampagne(campagne, List.of());
        when(reexamens.lister()).thenReturn(List.of(campagne));
        when(reexamens.ouvrirCourante(any())).thenReturn(campagne);
        when(reexamens.lire(rid)).thenReturn(vueCampagne);
        when(reexamens.decider(any(), eq(rid), eq("c1"), eq(cm.gimac.des.iam.domain.enumeration.ReviewDecision.MAINTENU), isNull(), isNull())).thenReturn(vueCampagne);
        when(reexamens.cloturer(any(), eq(rid))).thenReturn(campagne);
    }

    @Test
    @DisplayName("Saga SUSPENSION et RETABLISSEMENT, protection : jeton de service sbx-espace, clé d'idempotence relayée")
    void saga() throws Exception {
        mvc
            .perform(put("/api/v1/interne/sandbox/perimetres/99001/suspension").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-1/3").contentType(MediaType.APPLICATION_JSON).content("{\"suspendu\":true,\"motif\":\"Statut SUSPENDU\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suspendu").value(true))
            .andExpect(jsonPath("$.sessionsRevoquees").value(4));
        mvc
            .perform(put("/api/v1/interne/sandbox/perimetres/99001/suspension").with(service("INTERNAL_IAM_SBX_SAGA")).contentType(MediaType.APPLICATION_JSON).content("{\"suspendu\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suspendu").value(false));
        mvc.perform(get("/api/v1/interne/sandbox/perimetres/99001").with(service("INTERNAL_IAM_SBX_SAGA"))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("99001"));
        mvc
            .perform(put("/api/v1/interne/sandbox/comptes/c1/protection-dernier-administrateur").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-2/1").contentType(MediaType.APPLICATION_JSON).content("{\"protege\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.protege").value(true));
        mvc.perform(put("/api/v1/interne/sandbox/perimetres/99001/suspension").with(service("INTERNAL_IAM_SBX_SAGA")).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-INS-07 : matrice globale interne, jamais ouverte à l'administration ordinaire")
    void matriceGlobaleInterne() throws Exception {
        String corps = "{\"version\":7,\"profils\":{\"ROLE_SBX_LECTEUR\":[\"SBX_ISO_SIMULATION\"]}}";
        mvc.perform(put("/api/v1/interne/sandbox/matrice").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-m/7").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(7)).andExpect(jsonPath("$.rejouee").value(false));
        mvc.perform(put("/api/v1/interne/sandbox/matrice").with(service("IAM_PROFILE_EDIT")).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/interne/sandbox/matrice").with(service("INTERNAL_IAM_SBX_SAGA")).contentType(MediaType.APPLICATION_JSON).content("{\"version\":7,\"profils\":{}}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Moindre privilège : INTERNAL_IAM_READ, l'introspection ou un utilisateur n'ouvrent pas les étapes de saga")
    void sagaRefusee() throws Exception {
        String corps = "{\"suspendu\":true}";
        mvc.perform(put("/api/v1/interne/sandbox/perimetres/99001/suspension").with(service("INTERNAL_IAM_READ")).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/interne/sandbox/perimetres/99001/suspension").with(service("INTERNAL_IAM_SBX_INTROSPECT")).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/interne/sandbox/perimetres/99001/suspension").with(console("2", "IAM_ACCOUNT_READ", "IAM_ACCOUNT_STATUS")).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/interne/sandbox/perimetres/99001/suspension").contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Introspection : refus réduit à active=false, portée admise ; réservée à INTERNAL_IAM_SBX_INTROSPECT")
    void introspection() throws Exception {
        mvc
            .perform(post("/api/v1/interne/sandbox/introspection").with(service("INTERNAL_IAM_SBX_INTROSPECT")).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"j1\",\"institution\":\"99001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.sub").doesNotExist());
        mvc
            .perform(post("/api/v1/interne/sandbox/introspection").with(service("INTERNAL_IAM_SBX_INTROSPECT")).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"j2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.institution_id").value("99001"))
            .andExpect(jsonPath("$.permissions[0]").value("SBX_CER_EXECUTION"));
        mvc.perform(post("/api/v1/interne/sandbox/introspection").with(service("INTERNAL_IAM_SBX_SAGA")).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"j2\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/interne/sandbox/introspection").with(service("INTERNAL_IAM_SBX_INTROSPECT")).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"\"}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Console : périmètres suspendus et comptes protégés en lecture, IAM_ACCOUNT_READ sous deux facteurs")
    void console() throws Exception {
        mvc.perform(get("/api/v1/sandbox/perimetres?page=0&taille=20").with(console("2", "IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.contenu[0].code").value("99001"));
        mvc.perform(get("/api/v1/sandbox/perimetres/99001").with(console("2", "IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.suspendu").value(true));
        mvc.perform(get("/api/v1/sandbox/protections").with(console("2", "IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.contenu[0].nom").value("Awa N"));
        mvc.perform(get("/api/v1/sandbox/perimetres").with(console("1", "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/sandbox/protections").with(console("2", "SBX_HAB_CONSULTATION"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-EXI-04 : saga MANDAT (201, 200 rejouée), lecture, révocation, prolongation ; liste de la console")
    void delegations() throws Exception {
        String corps = "{\"compteId\":\"p1\",\"institution\":\"99001\",\"permissions\":[\"SBX_CER_EXECUTION\"],\"debut\":\"" + T + "\",\"fin\":\"" + T.plusSeconds(86400) + "\"}";
        mvc
            .perform(post("/api/v1/interne/sandbox/delegations").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-m/4").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.delegation.institutionId").value("99001"))
            .andExpect(jsonPath("$.etat").value("ACTIVE"));
        mvc
            .perform(post("/api/v1/interne/sandbox/delegations").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-m/5").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rejouee").value(true));
        mvc.perform(post("/api/v1/interne/sandbox/delegations").with(service("INTERNAL_IAM_SBX_SAGA")).contentType(MediaType.APPLICATION_JSON).content("{\"compteId\":\"p1\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/interne/sandbox/delegations").with(service("INTERNAL_IAM_READ")).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/interne/sandbox/delegations/d1").with(service("INTERNAL_IAM_SBX_SAGA"))).andExpect(status().isOk()).andExpect(jsonPath("$.nomCompte").value("Paul Ebode"));
        mvc.perform(delete("/api/v1/interne/sandbox/delegations/d1?motif=Fin").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-r/1")).andExpect(status().isOk());
        mvc
            .perform(put("/api/v1/interne/sandbox/delegations/d1/fin").with(service("INTERNAL_IAM_SBX_SAGA")).contentType(MediaType.APPLICATION_JSON).content("{\"fin\":\"" + T.plusSeconds(172800) + "\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/sandbox/delegations?type=MANDAT").with(console("2", "IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.contenu[0].delegation.type").value("MANDAT"));
        mvc.perform(get("/api/v1/sandbox/delegations").with(console("2", "SBX_HAB_ADMINISTRATION"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-EXI-24 : accès de prestataire demandé, approuvé, refusé, révoqué depuis la console (IAM_ACCOUNT_SECURITY, deux facteurs) ; introspection par composant")
    void prestataire() throws Exception {
        String corps = "{\"compteId\":\"pr\",\"composants\":[\"SBX-TRACE\"],\"debut\":\"" + T + "\",\"fin\":\"" + T.plusSeconds(14400) + "\",\"motif\":\"Correctif\"}";
        mvc
            .perform(post("/api/v1/sandbox/delegations/prestataire").with(console("2", "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.delegation.composants[0]").value("SBX-TRACE"));
        mvc.perform(post("/api/v1/sandbox/delegations/a1/approbation").with(console("2", "IAM_ACCOUNT_SECURITY"))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/sandbox/delegations/a1/refus").with(console("2", "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Non\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/sandbox/delegations/a1/revocation").with(console("2", "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Fin\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/sandbox/delegations/a1/refus").with(console("2", "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/sandbox/delegations/prestataire").with(console("2", "IAM_ACCOUNT_READ")).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/sandbox/delegations/a1/approbation").with(console("1", "IAM_ACCOUNT_SECURITY"))).andExpect(status().isForbidden());
        mvc
            .perform(post("/api/v1/interne/sandbox/introspection").with(service("INTERNAL_IAM_SBX_INTROSPECT")).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"j3\",\"composant\":\"SBX-TRACE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("PRESTATAIRE"))
            .andExpect(jsonPath("$.composants[0]").value("SBX-TRACE"))
            .andExpect(jsonPath("$.permissions").isEmpty());
    }

    @Test
    @DisplayName("Sagas OUVERTURE et MATRICE : compte partagé et profils SBX, clé relayée")
    void ouvertureMatrice() throws Exception {
        String corps = "{\"identifiant\":\"awa.test\",\"prenom\":\"Awa\",\"nom\":\"Test\",\"email\":\"awa@test.cm\",\"institutionId\":\"99001\",\"profils\":[\"ROLE_SBX_TESTEUR\"]}";
        mvc.perform(post("/api/v1/interne/sandbox/comptes").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-o/1").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.permissionsSbx[0]").value("SBX_CER_EXECUTION"));
        mvc.perform(post("/api/v1/interne/sandbox/comptes").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-o/replay").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rejouee").value(true));
        mvc.perform(get("/api/v1/interne/sandbox/comptes/c1").with(service("INTERNAL_IAM_SBX_SAGA"))).andExpect(status().isOk()).andExpect(jsonPath("$.identifiant").value("awa.test"));
        mvc.perform(put("/api/v1/interne/sandbox/comptes/c1/profils-sandbox").with(service("INTERNAL_IAM_SBX_SAGA")).header("Idempotency-Key", "saga-m/2").contentType(MediaType.APPLICATION_JSON).content("{\"profils\":[\"ROLE_SBX_TESTEUR\"],\"motif\":\"Revue\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/interne/sandbox/comptes").with(service("INTERNAL_IAM_READ")).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-EXI-23 : lecture, ouverture, décision et clôture des campagnes de réexamen")
    void reexamensRoutes() throws Exception {
        String id = "00000000-0000-0000-0000-000000000101";
        mvc.perform(get("/api/v1/comptes/reexamens").with(console("2", "IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].period").value("2026-T4"));
        mvc.perform(post("/api/v1/comptes/reexamens/ouverture").with(console("2", "IAM_ACCOUNT_CREATE"))).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/comptes/reexamens/" + id).with(console("2", "IAM_ACCOUNT_READ"))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/comptes/reexamens/" + id + "/comptes/c1/decision").with(console("2", "IAM_ACCOUNT_ROLES")).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"MAINTENU\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/comptes/reexamens/" + id + "/cloture").with(console("2", "IAM_ACCOUNT_ROLES"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/comptes/reexamens").with(console("1", "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/comptes/reexamens/" + id + "/cloture").with(console("2", "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
    }
}
