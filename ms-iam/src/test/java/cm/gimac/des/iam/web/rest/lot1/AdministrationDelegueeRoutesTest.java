package cm.gimac.des.iam.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService;
import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService.Creation;
import cm.gimac.des.iam.application.usecase.AdministrationDelegueeService.PageComptes;
import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.config.SecurityConfiguration;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Compte;
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
 * Routes de l'administration déléguée de la sandbox (D-115, US-EXI-03) avec la vraie chaîne de filtres : permission
 * SBX_HAB_ADMINISTRATION, lecture de la liste par ROLE_SBX_CHEF_PROJET, institution du jeton transmise au service, clé d'idempotence relayée, 201 pour une création et
 * 200 pour une étape rejouée, corps validé, refus normalisés.
 */
@WebMvcTest(controllers = AdministrationDelegueeResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, IamExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class AdministrationDelegueeRoutesTest {

    static final String U = "/api/v1/administration-deleguee";
    static final String ADMIN = "3f1c2d4e-0000-4000-8000-000000000042";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdministrationDelegueeService service;

    @MockitoBean
    AvatarService avatars;

    static Compte compte(String id, boolean actif, String... profils) {
        return new Compte(id, "awa.ndiaye", "Awa", "Ndiaye", "awa@institution.test", true, actif, Instant.parse("2026-10-19T08:00:00Z"), null, "Testeuse", "fr", "99001", true, Set.of(profils), List.of(), true, null, null);
    }

    static RequestPostProcessor administrateur() {
        return jwt().jwt(j -> j.subject(ADMIN).claim("acr", "2").claim("name", "Administrateur").claim("institution_id", "99001")).authorities(new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION"));
    }

    @BeforeEach
    void init() {
        when(service.roles()).thenReturn(Set.of("ROLE_SBX_TESTEUR"));
        when(service.lister(eq("99001"), anyInt(), anyInt())).thenReturn(new PageComptes(List.of(compte("c1", true, "ROLE_SBX_TESTEUR")), 1, 0, 20));
        when(service.lire("99001", "c1")).thenReturn(compte("c1", true, "ROLE_SBX_TESTEUR"));
        when(service.lire("99001", "c9")).thenThrow(IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", "c9"));
        when(service.creer(any(), eq("99001"), any(), eq("saga-1"))).thenReturn(new Creation(compte("c2", true, "ROLE_SBX_TESTEUR"), true, false));
        when(service.creer(any(), eq("99001"), any(), eq("saga-rejouee"))).thenReturn(new Creation(compte("c2", true, "ROLE_SBX_TESTEUR"), false, true));
        when(service.definirRoles(any(), eq("99001"), eq("c1"), eq(Set.of("ROLE_SBX_LECTEUR")), eq("Lecture seule"), isNull())).thenReturn(compte("c1", true, "ROLE_SBX_LECTEUR"));
        when(service.definirActif(any(), eq("99001"), eq("c1"), eq(false), eq("Départ"), eq("saga-2"))).thenReturn(compte("c1", false, "ROLE_SBX_TESTEUR"));
    }

    @Test
    @DisplayName("Lectures : rôles délégables, page de comptes de l'institution du jeton, compte ; hors périmètre : 404")
    void lectures() throws Exception {
        mvc.perform(get(U + "/roles").with(administrateur())).andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("ROLE_SBX_TESTEUR"));
        mvc.perform(get(U + "/comptes?page=0&taille=20").with(administrateur())).andExpect(status().isOk()).andExpect(jsonPath("$.contenu[0].id").value("c1")).andExpect(jsonPath("$.total").value(1));
        var chef = jwt().jwt(j -> j.subject(ADMIN).claim("acr", "2").claim("institution_id", "99001")).authorities(new SimpleGrantedAuthority("ROLE_SBX_CHEF_PROJET"));
        mvc.perform(get(U + "/comptes").with(chef)).andExpect(status().isOk()).andExpect(jsonPath("$.contenu[0].id").value("c1"));
        mvc.perform(get(U + "/comptes/c1").with(chef)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("c1"));
        mvc
            .perform(post(U + "/comptes").with(chef).contentType(MediaType.APPLICATION_JSON).content("{\"identifiant\":\"x\",\"prenom\":\"X\",\"nom\":\"Y\",\"email\":\"x@y.test\",\"roles\":[]}"))
            .andExpect(status().isForbidden());
        mvc.perform(get(U + "/comptes/c1").with(administrateur())).andExpect(status().isOk()).andExpect(jsonPath("$.institutionId").value("99001"));
        mvc.perform(get(U + "/comptes/c9").with(administrateur())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IAM_ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Création : 201 avec l'adresse du compte ; étape rejouée : 200 ; clé d'idempotence transmise")
    void creation() throws Exception {
        String corps = "{\"identifiant\":\" awa.ndiaye \",\"prenom\":\"Awa\",\"nom\":\"Ndiaye\",\"email\":\"awa@institution.test\",\"roles\":[\"ROLE_SBX_TESTEUR\"]}";
        mvc
            .perform(post(U + "/comptes").with(administrateur()).header("Idempotency-Key", "saga-1").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", U + "/comptes/c2"))
            .andExpect(jsonPath("$.compte.id").value("c2"))
            .andExpect(jsonPath("$.invitationEnvoyee").value(true));
        verify(service).creer(any(), eq("99001"), eq(new AdministrationDelegueeService.NouveauCollaborateur("awa.ndiaye", "Awa", "Ndiaye", "awa@institution.test", null, null, Set.of("ROLE_SBX_TESTEUR"))), eq("saga-1"));
        mvc
            .perform(post(U + "/comptes").with(administrateur()).header("Idempotency-Key", "saga-rejouee").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rejouee").value(true));
        mvc.perform(post(U + "/comptes").with(administrateur()).contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[]}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rôles et état : délégués au service avec motif et clé ; motif obligatoire")
    void ecritures() throws Exception {
        mvc
            .perform(put(U + "/comptes/c1/roles").with(administrateur()).contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ROLE_SBX_LECTEUR\"],\"motif\":\"Lecture seule\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profils[0]").value("ROLE_SBX_LECTEUR"));
        mvc
            .perform(put(U + "/comptes/c1/etat").with(administrateur()).header("Idempotency-Key", "saga-2").contentType(MediaType.APPLICATION_JSON).content("{\"actif\":false,\"motif\":\"Départ\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actif").value(false));
        mvc.perform(put(U + "/comptes/c1/etat").with(administrateur()).contentType(MediaType.APPLICATION_JSON).content("{\"actif\":false}")).andExpect(status().isBadRequest());
        mvc.perform(put(U + "/comptes/c1/roles").with(administrateur()).contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[],\"motif\":\" \"}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-EXI-23 : demande de déblocage de l'institution transmise au service avec son motif, ou sans corps")
    void deblocage() throws Exception {
        when(service.refuserDeblocage(any(), eq("99001"), eq("c1"), any())).thenThrow(IamException.interdit("IAM_SBX_DEBLOCAGE_RESERVE", "Réservé au GIMAC", java.util.Map.of()));
        mvc
            .perform(post(U + "/comptes/c1/deblocage").with(administrateur()).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Testeur bloqué\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_SBX_DEBLOCAGE_RESERVE"));
        mvc.perform(post(U + "/comptes/c1/deblocage").with(administrateur())).andExpect(status().isForbidden());
        verify(service).refuserDeblocage(any(), eq("99001"), eq("c1"), eq("Testeur bloqué"));
        verify(service).refuserDeblocage(any(), eq("99001"), eq("c1"), isNull());
    }

    @Test
    @DisplayName("Accès : 401 sans jeton, 403 sans la permission ou sans second facteur")
    void acces() throws Exception {
        mvc.perform(get(U + "/comptes")).andExpect(status().isUnauthorized());
        mvc.perform(get(U + "/comptes").with(jwt().jwt(j -> j.subject(ADMIN).claim("acr", "2").claim("institution_id", "99001")).authorities(new SimpleGrantedAuthority("SBX_HAB_CONSULTATION")))).andExpect(status().isForbidden());
        mvc.perform(get(U + "/comptes").with(jwt().jwt(j -> j.subject(ADMIN).claim("acr", "1").claim("institution_id", "99001")).authorities(new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION")))).andExpect(status().isForbidden());
        // Jeton sans institution : la route la refuse (403), le service n'est pas joint.
        mvc.perform(get(U + "/roles").with(jwt().jwt(j -> j.subject(ADMIN).claim("acr", "2")).authorities(new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION")))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("IAM_SBX_SANS_INSTITUTION"));
    }
}
