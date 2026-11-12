package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.web.rest.errors.IamExceptionHandler;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Routes internes de l'administration déléguée appelées par SBX-ESPACE avec son compte de service (D-144), sur la vraie
 * chaîne de filtres : INTERNAL_IAM_SBX_SAGA seule, institution et auteur en paramètres, réponses égales au contrat
 * commun contrats/iam-administration-deleguee-interne.json (copie de docs/contrats de la sandbox, relu par
 * IamComptesHttpTest de SBX-ESPACE).
 */
@WebMvcTest(controllers = SandboxAdministrationInterneResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, IamExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class SandboxAdministrationInterneRoutesTest {

    static final String U = "/api/v1/interne/sandbox/administration-deleguee";
    static final String ID = "947697ad-5b60-4e9d-a595-b281a4b7e80b";
    static final String ADMIN = "3f1c2d4e-0000-4000-8000-000000000042";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdministrationDelegueeService service;

    @MockitoBean
    AvatarService avatars;

    static JsonNode contrat() throws Exception {
        try (InputStream in = SandboxAdministrationInterneRoutesTest.class.getResourceAsStream("/contrats/iam-administration-deleguee-interne.json")) {
            return JsonMapper.builder().build().readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    static Compte compte(boolean actif) {
        return new Compte(ID, "demo.dev.99001", "Développeur", "Fictif", "demo.dev@institution-99001.example", true, actif, Instant.parse("2026-11-12T08:00:00Z"), null, null, "fr",
            "0b6f6c1e-0000-4000-8000-000000099001", true, Set.of("ROLE_SBX_DEVELOPPEUR"), List.of(ActionRequise.SECOND_FACTEUR), true, null, null);
    }

    static RequestPostProcessor espace() {
        return jwt().jwt(j -> j.subject("service-account-sbx-espace")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_SBX_SAGA"));
    }

    @Test
    @DisplayName("Lectures du compte de service : rôles, page et compte de l'institution désignée, égaux au contrat")
    void lectures() throws Exception {
        when(service.roles()).thenReturn(new java.util.TreeSet<>(Set.of("ROLE_SBX_DEVELOPPEUR", "ROLE_SBX_TESTEUR")));
        when(service.lister("99001", 0, 20)).thenReturn(new PageComptes(List.of(compte(true)), 1, 0, 20));
        when(service.lire("99001", ID)).thenReturn(compte(true));
        when(service.lire("99001", "c9")).thenThrow(IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", "c9"));
        JsonNode c = contrat();
        mvc.perform(get(U + "/roles").with(espace())).andExpect(status().isOk()).andExpect(content().json(c.get("roles").toString(), JsonCompareMode.STRICT));
        mvc.perform(get(U + "/comptes").param("codeParticipant", "99001").with(espace())).andExpect(status().isOk()).andExpect(content().json(c.get("comptes").toString(), JsonCompareMode.STRICT));
        mvc.perform(get(U + "/comptes/" + ID).param("codeParticipant", " 99001 ").with(espace())).andExpect(status().isOk()).andExpect(content().json(c.get("compte").toString(), JsonCompareMode.STRICT));
        mvc.perform(get(U + "/comptes/c9").param("codeParticipant", "99001").with(espace())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IAM_ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Écritures : journalisées au nom de l'auteur transmis, clé d'idempotence relayée, 201 puis 200 rejouée")
    void ecritures() throws Exception {
        when(service.creer(any(), eq("99001"), any(), eq("saga:1"))).thenReturn(new Creation(compte(true), true, false));
        when(service.creer(any(), eq("99001"), any(), eq("saga:rejouee"))).thenReturn(new Creation(compte(true), false, true));
        when(service.definirRoles(any(), eq("99001"), eq(ID), eq(Set.of("ROLE_SBX_DEVELOPPEUR")), eq("Affectation"), isNull())).thenReturn(compte(true));
        when(service.definirActif(any(), eq("99001"), eq(ID), eq(false), eq("Départ"), eq("saga:3"))).thenReturn(compte(false));
        String corps = "{\"identifiant\":\" demo.dev.99001 \",\"prenom\":\"Développeur\",\"nom\":\"Fictif\",\"email\":\"demo.dev@institution-99001.example\",\"roles\":[\"ROLE_SBX_DEVELOPPEUR\"]}";
        mvc
            .perform(post(U + "/comptes").param("codeParticipant", "99001").param("acteur", ADMIN).param("acteurNom", "Administrateur fictif").with(espace()).header("Idempotency-Key", "saga:1").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", U + "/comptes/" + ID))
            .andExpect(content().json(contrat().get("creation").toString(), JsonCompareMode.STRICT));
        ArgumentCaptor<Acteur> acteur = ArgumentCaptor.forClass(Acteur.class);
        verify(service).creer(acteur.capture(), eq("99001"), eq(new AdministrationDelegueeService.NouveauCollaborateur("demo.dev.99001", "Développeur", "Fictif", "demo.dev@institution-99001.example", null, null, Set.of("ROLE_SBX_DEVELOPPEUR"))), eq("saga:1"));
        assertThat(acteur.getValue()).isEqualTo(new Acteur(ADMIN, "Administrateur fictif", Set.of("SBX_HAB_ADMINISTRATION")));
        mvc
            .perform(post(U + "/comptes").param("codeParticipant", "99001").param("acteur", ADMIN).with(espace()).header("Idempotency-Key", "saga:rejouee").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rejouee").value(true));
        mvc
            .perform(put(U + "/comptes/" + ID + "/roles").param("codeParticipant", "99001").param("acteur", ADMIN).with(espace()).contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ROLE_SBX_DEVELOPPEUR\"],\"motif\":\"Affectation\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codeParticipant").value("99001"));
        ArgumentCaptor<Acteur> sansNom = ArgumentCaptor.forClass(Acteur.class);
        verify(service).definirRoles(sansNom.capture(), eq("99001"), eq(ID), any(), any(), isNull());
        assertThat(sansNom.getValue().nom()).isEqualTo(ADMIN);
        mvc
            .perform(put(U + "/comptes/" + ID + "/etat").param("codeParticipant", "99001").param("acteur", ADMIN).with(espace()).header("Idempotency-Key", "saga:3").contentType(MediaType.APPLICATION_JSON).content("{\"actif\":false,\"motif\":\"Départ\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actif").value(false));
    }

    @Test
    @DisplayName("Refus : jeton d'utilisateur ou autre compte de service 403, institution hors forme ou auteur absent 400")
    void refus() throws Exception {
        var administrateur = jwt().jwt(j -> j.subject(ADMIN).claim("institution_id", "99001")).authorities(new SimpleGrantedAuthority("SBX_HAB_ADMINISTRATION"));
        mvc.perform(get(U + "/comptes").param("codeParticipant", "99001").with(administrateur)).andExpect(status().isForbidden());
        var lecture = jwt().jwt(j -> j.subject("service-account-homologation-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
        mvc.perform(get(U + "/roles").with(lecture)).andExpect(status().isForbidden());
        mvc.perform(get(U + "/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get(U + "/comptes").param("codeParticipant", "0b6f6c1e-0000-4000-8000-000000099001").with(espace())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IAM_SBX_INSTITUTION_INVALIDE"));
        mvc.perform(get(U + "/comptes/" + ID).param("codeParticipant", "99 001").with(espace())).andExpect(status().isBadRequest());
        mvc.perform(get(U + "/comptes").with(espace())).andExpect(status().isBadRequest());
        String roles = "{\"roles\":[],\"motif\":\"m\"}";
        mvc.perform(put(U + "/comptes/" + ID + "/roles").param("codeParticipant", "99001").param("acteur", " ").with(espace()).contentType(MediaType.APPLICATION_JSON).content(roles)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IAM_SBX_ACTEUR_REQUIS"));
        mvc.perform(put(U + "/comptes/" + ID + "/etat").param("codeParticipant", "99001").with(espace()).contentType(MediaType.APPLICATION_JSON).content("{\"actif\":true,\"motif\":\"m\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
