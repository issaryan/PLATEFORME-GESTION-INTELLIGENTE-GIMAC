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
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService;
import cm.gimac.des.iam.application.usecase.sandbox.DelegationsSandboxService.VueDelegation;
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
@WebMvcTest(controllers = { SandboxSagaInterneResource.class, SandboxIntrospectionResource.class, SandboxConsoleResource.class })
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
        when(introspection.introspecter("j1", "99001")).thenReturn(Introspection.refusee("s", "PERIMETRE_SUSPENDU"));
        when(introspection.introspecter("j2", null)).thenReturn(new Introspection(true, "s", "99001", Set.of("SBX_CER_EXECUTION"), "COMPTE", null, T, Set.of(), null));
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
}
