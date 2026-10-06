package cm.gimac.des.iam.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.application.usecase.ConfigurationAuthentificationService;
import cm.gimac.des.iam.application.usecase.DemandesService;
import cm.gimac.des.iam.application.usecase.OrganisationService;
import cm.gimac.des.iam.application.usecase.PouvoirsService;
import cm.gimac.des.iam.application.usecase.ProfilsService;
import cm.gimac.des.iam.application.usecase.ReauthentificationService;
import cm.gimac.des.iam.config.SecurityConfiguration;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.demande.ContenuDemande;
import cm.gimac.des.iam.domain.demande.Demande;
import cm.gimac.des.iam.domain.demande.EtatDemande;
import cm.gimac.des.iam.domain.demande.TypeDemande;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.web.rest.errors.IamExceptionHandler;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
 * Routes D-81 (docs/lot1/pouvoirs-hierarchiques.md §3 à §5) avec la vraie chaîne de filtres : 401, 403
 * selon la permission de chaque route, 428 sans réauthentification FIDO2 pour le réglage de la matrice,
 * l'acceptation et les profils créés, route interne réservée au jeton de service, formes JSON du contrat ;
 * D-86 : libellé et description des profils, libellés dans la matrice et les pouvoirs, actions des processus.
 */
@WebMvcTest(controllers = { PouvoirsResource.class, PouvoirsInterneResource.class, DemandesResource.class, ConfigurationAuthentificationResource.class })
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, IamExceptionHandler.class, ReauthAdministration.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class PouvoirsDemandesRoutesTest {

    static final String ID = "3f1c2d4e-0000-4000-8000-000000000001";
    static final UUID DEMANDE = UUID.fromString("3f1c2d4e-0000-4000-8000-0000000000d1");
    static final String LIGNE = "{\"actions\":{\"TACHE_CONFIER\":\"UNITES\"},\"profilsDemandables\":[\"ROLE_SNA\"],\"motif\":\"Revue\"}";
    static final String NOUVELLE = "{\"type\":\"STATUT\",\"cibleId\":\"c1\",\"contenu\":{\"actif\":false},\"motif\":\"Fin de mission\"}";
    static final String PROFIL = "{\"code\":\"ROLE_QUALITE\",\"libelle\":\"Cellule qualité\",\"description\":\"Qualité\",\"permissions\":[\"HOM_DASHBOARD_READ\"]}";
    static final String PRESENTATION = "{\"libelle\":\"Pôle réseau et télécoms\",\"description\":\"Exploite le réseau\",\"motif\":\"Réorganisation\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PouvoirsService pouvoirs;

    @MockitoBean
    DemandesService demandes;

    @MockitoBean
    ProfilsService profils;

    @MockitoBean
    ConfigurationAuthentificationService configuration;

    @MockitoBean
    ReauthentificationService reauth;

    @MockitoBean
    AvatarService avatars;

    Demande demande;

    @BeforeEach
    void init() {
        var ligne = new PouvoirsService.LigneMatrice("ROLE_CHEF_DES", "Chef du DES", false, false, Map.of(ActionPouvoir.TACHE_CONFIER, Portee.UNITES, ActionPouvoir.PROCESSUS_DEFINIR, Portee.UNITES), Set.of("ROLE_SNA"));
        when(pouvoirs.matrice()).thenReturn(new PouvoirsService.Matrice(Arrays.asList(ActionPouvoir.values()), Arrays.asList(Portee.values()), List.of(ligne)));
        when(pouvoirs.definir(any(), eq("ROLE_CHEF_DES"), any(), any(), any())).thenReturn(ligne);
        var vue = new PouvoirsService.VuePouvoirs(
            Map.of(ActionPouvoir.TACHE_CONFIER, Portee.UNITES, ActionPouvoir.PROCESSUS_LANCER, Portee.UNITES),
            Set.of("ROLE_SNA"),
            List.of(new OrganisationService.MembreEquipe("m1", "Bob Ndi", "Technicien", "POLE_RESEAU", ID, 0, true)),
            List.of("POLE_RESEAU"),
            Map.of("ROLE_SNA", "Normes et Agréments")
        );
        when(pouvoirs.vue(ID)).thenReturn(vue);
        demande = new Demande(
            DEMANDE,
            TypeDemande.STATUT,
            new Demande.Personne(ID, "Chef Mballa"),
            new Demande.Personne("c1", "Awa Mballa"),
            ContenuDemande.statut(false),
            "Fin de mission",
            EtatDemande.ACCEPTEE,
            new Demande.Decision(new Demande.Personne("admin", "Admin"), null, Instant.EPOCH),
            Instant.EPOCH
        );
        when(demandes.soumettre(any(), eq(TypeDemande.STATUT), eq("c1"), any(), eq("Fin de mission"))).thenReturn(demande);
        when(demandes.lister(any(), any(), any(), any(), any())).thenReturn(new DemandesService.PageDemandes(List.of(demande), 1, 0, 25));
        when(demandes.lire(any(), eq(DEMANDE))).thenReturn(demande);
        when(demandes.annuler(any(), eq(DEMANDE))).thenReturn(demande);
        when(demandes.accepter(any(), eq(DEMANDE), any())).thenReturn(demande);
        when(demandes.refuser(any(), eq(DEMANDE), anyString())).thenReturn(demande);
        when(profils.creer(any(), eq("ROLE_QUALITE"), any(), any(), any(), any())).thenReturn(new Profil("ROLE_QUALITE", "Cellule qualité", "Qualité", Set.of("HOM_DASHBOARD_READ"), false, 0, true));
        var modifie = new Profil(
            "ROLE_POLE_RESEAU",
            "Pôle réseau et télécoms",
            "Exploite le réseau",
            Set.of("HOM_EVIDENCE_SUBMIT"),
            false,
            2,
            false,
            Instant.parse("2026-10-06T09:00:00Z"),
            new Profil.Auteur(ID, "Chef")
        );
        when(profils.modifier(any(), eq("ROLE_POLE_RESEAU"), any(), any(), any())).thenReturn(modifie);
        when(configuration.profils()).thenReturn(List.of(modifie, new Profil("ROLE_SNA", null, Set.of(), false, 0)));
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
    @DisplayName("Matrice : lecture IAM_ACCOUNT_READ ; réglage IAM_PROFILE_EDIT et FIDO2 (428 sans assertion) ; pouvoirs/moi PIL_ACTIVITY_OWN ; interne : jeton de service")
    void pouvoirs() throws Exception {
        String m = "/api/v1/pouvoirs/matrice";
        mvc.perform(req(HttpMethod.GET, m, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, m, null).with(jeton("PIL_ACTIVITY_OWN"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.GET, m, null).with(jeton("IAM_ACCOUNT_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions[0]").value("TACHE_CONFIER"))
            .andExpect(jsonPath("$.portees[3]").value("UNITES"))
            .andExpect(jsonPath("$.actions[8]").value("PROCESSUS_DEFINIR"))
            .andExpect(jsonPath("$.actions[9]").value("PROCESSUS_LANCER"))
            .andExpect(jsonPath("$.profils[0].profil").value("ROLE_CHEF_DES"))
            .andExpect(jsonPath("$.profils[0].libelle").value("Chef du DES"))
            .andExpect(jsonPath("$.profils[0].actions.PROCESSUS_DEFINIR").value("UNITES"))
            .andExpect(jsonPath("$.profils[0].privilegie").value(false))
            .andExpect(jsonPath("$.profils[0].cree").value(false))
            .andExpect(jsonPath("$.profils[0].actions.TACHE_CONFIER").value("UNITES"))
            .andExpect(jsonPath("$.profils[0].profilsDemandables[0]").value("ROLE_SNA"));
        mvc.perform(req(HttpMethod.PUT, m + "/ROLE_CHEF_DES", LIGNE).with(jeton("IAM_ACCOUNT_READ")).header("X-Reauth-Assertion", "valide")).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.PUT, m + "/ROLE_CHEF_DES", LIGNE).with(jeton("IAM_PROFILE_EDIT"))).andExpect(status().is(428));
        mvc.perform(req(HttpMethod.PUT, m + "/ROLE_CHEF_DES", LIGNE).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "autre")).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.PUT, m + "/ROLE_CHEF_DES", LIGNE).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide")).andExpect(status().isOk());
        verify(pouvoirs).definir(any(), eq("ROLE_CHEF_DES"), eq(Map.of(ActionPouvoir.TACHE_CONFIER, Portee.UNITES)), eq(Set.of("ROLE_SNA")), eq("Revue"));
        mvc.perform(req(HttpMethod.PUT, m + "/ROLE_CHEF_DES", "{\"actions\":{\"INCONNUE\":\"UNITES\"},\"profilsDemandables\":[]}").with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide")).andExpect(status().isBadRequest());

        mvc.perform(req(HttpMethod.GET, "/api/v1/pouvoirs/moi", null).with(jeton("IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/pouvoirs/moi", null).with(jeton("PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actions.TACHE_CONFIER").value("UNITES"))
            .andExpect(jsonPath("$.profilsDemandables[0]").value("ROLE_SNA"))
            .andExpect(jsonPath("$.libellesProfils.ROLE_SNA").value("Normes et Agréments"))
            .andExpect(jsonPath("$.actions.PROCESSUS_LANCER").value("UNITES"))
            .andExpect(jsonPath("$.subordonnes[0].id").value("m1"))
            .andExpect(jsonPath("$.subordonnes[0].niveau").value(0))
            .andExpect(jsonPath("$.unitesDirigees[0]").value("POLE_RESEAU"));
        String interne = "/api/v1/interne/pouvoirs/" + ID;
        mvc.perform(req(HttpMethod.GET, interne, null).with(jeton("PIL_ACTIVITY_OWN", "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.GET, interne, null).with(jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unitesDirigees[0]").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.actions.PROCESSUS_LANCER").value("UNITES"))
            .andExpect(jsonPath("$.libellesProfils.ROLE_SNA").value("Normes et Agréments"));
    }

    @Test
    @DisplayName("Demandes : soumission PIL_ACTIVITY_OWN (201), lecture, annulation, acceptation (permission de l'acte et FIDO2), refus IAM_ACCOUNT_READ ; forme du contrat")
    void demandes() throws Exception {
        String u = "/api/v1/demandes";
        mvc.perform(req(HttpMethod.POST, u, NOUVELLE)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.POST, u, NOUVELLE).with(jeton("IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.POST, u, NOUVELLE).with(jeton("PIL_ACTIVITY_OWN")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/demandes/" + DEMANDE))
            .andExpect(jsonPath("$.id").value(DEMANDE.toString()))
            .andExpect(jsonPath("$.type").value("STATUT"))
            .andExpect(jsonPath("$.demandeur.nomAffiche").value("Chef Mballa"))
            .andExpect(jsonPath("$.cible.id").value("c1"))
            .andExpect(jsonPath("$.contenu.actif").value(false))
            .andExpect(jsonPath("$.contenu.ajouter").doesNotExist())
            .andExpect(jsonPath("$.etat").value("ACCEPTEE"))
            .andExpect(jsonPath("$.decision.par.id").value("admin"))
            .andExpect(jsonPath("$.creeeLe").exists());
        mvc.perform(req(HttpMethod.POST, u, "{\"type\":\"AUTRE\",\"contenu\":{},\"motif\":\"Fin de mission\"}").with(jeton("PIL_ACTIVITY_OWN"))).andExpect(status().isBadRequest());

        mvc.perform(req(HttpMethod.GET, u + "?vue=miennes", null).with(jeton("PIL_TEAM_READ"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, u + "?vue=miennes&etat=EN_ATTENTE&page=0&taille=10", null).with(jeton("PIL_ACTIVITY_OWN"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        verify(demandes).lister(any(), eq("miennes"), eq("EN_ATTENTE"), eq(0), eq(10));
        mvc.perform(req(HttpMethod.GET, u + "/" + DEMANDE, null).with(jeton("IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.motif").value("Fin de mission"));

        mvc.perform(req(HttpMethod.POST, u + "/" + DEMANDE + "/annulation", null).with(jeton("IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.POST, u + "/" + DEMANDE + "/annulation", null).with(jeton("PIL_ACTIVITY_OWN"))).andExpect(status().isOk());

        String a = u + "/" + DEMANDE + "/acceptation";
        mvc.perform(req(HttpMethod.POST, a, null).with(jeton("IAM_ACCOUNT_READ")).header("X-Reauth-Assertion", "valide")).andExpect(status().isForbidden());
        for (String permission : List.of("IAM_ACCOUNT_CREATE", "IAM_ACCOUNT_ROLES", "IAM_ACCOUNT_STATUS")) {
            mvc.perform(req(HttpMethod.POST, a, null).with(jeton(permission))).andExpect(status().is(428));
        }
        mvc.perform(req(HttpMethod.POST, a, null).with(jeton("IAM_ACCOUNT_STATUS")).header("X-Reauth-Assertion", "valide")).andExpect(status().isOk());
        verify(demandes).accepter(any(), eq(DEMANDE), isNull());
        mvc.perform(req(HttpMethod.POST, a, "{\"motif\":\"Validé par la DG\"}").with(jeton("IAM_ACCOUNT_STATUS")).header("X-Reauth-Assertion", "valide")).andExpect(status().isOk());
        verify(demandes).accepter(any(), eq(DEMANDE), eq("Validé par la DG"));

        String r = u + "/" + DEMANDE + "/refus";
        mvc.perform(req(HttpMethod.POST, r, "{\"motif\":\"Non justifié\"}").with(jeton("PIL_ACTIVITY_OWN"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.POST, r, "{\"motif\":\"Non justifié\"}").with(jeton("IAM_ACCOUNT_READ"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Profils créés : IAM_PROFILE_EDIT et FIDO2 pour la création (201) et la suppression (204)")
    void profilsCrees() throws Exception {
        String u = "/api/v1/configuration/profils";
        mvc.perform(req(HttpMethod.POST, u, PROFIL).with(jeton("IAM_ACCOUNT_READ")).header("X-Reauth-Assertion", "valide")).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.POST, u, PROFIL).with(jeton("IAM_PROFILE_EDIT"))).andExpect(status().is(428));
        mvc
            .perform(req(HttpMethod.POST, u, PROFIL).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/configuration/profils/ROLE_QUALITE"))
            .andExpect(jsonPath("$.code").value("ROLE_QUALITE"))
            .andExpect(jsonPath("$.libelle").value("Cellule qualité"))
            .andExpect(jsonPath("$.cree").value(true))
            .andExpect(jsonPath("$.privilegie").value(false))
            .andExpect(jsonPath("$.titulaires").value(0));
        mvc.perform(req(HttpMethod.POST, u, "{\"code\":\"ROLE_QUALITE\",\"permissions\":[]}").with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide")).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.DELETE, u + "/ROLE_QUALITE", null).with(jeton("IAM_ACCOUNT_READ")).header("X-Reauth-Assertion", "valide")).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.DELETE, u + "/ROLE_QUALITE", null).with(jeton("IAM_PROFILE_EDIT"))).andExpect(status().is(428));
        mvc.perform(req(HttpMethod.DELETE, u + "/ROLE_QUALITE?motif=Abandon", null).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide")).andExpect(status().isNoContent());
        verify(profils).supprimer(any(), eq("ROLE_QUALITE"), eq("Abandon"));
        verify(profils).creer(any(), eq("ROLE_QUALITE"), eq("Cellule qualité"), eq("Qualité"), eq(Set.of("HOM_DASHBOARD_READ")), isNull());
    }

    @Test
    @DisplayName("D-86 : libellé et description d'un profil, IAM_PROFILE_EDIT et FIDO2 ; lecture des profils avec libellé, modifieLe, modifiePar")
    void presentationProfil() throws Exception {
        String u = "/api/v1/configuration/profils/ROLE_POLE_RESEAU";
        mvc.perform(req(HttpMethod.PUT, u, PRESENTATION)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.PUT, u, PRESENTATION).with(jeton("IAM_ACCOUNT_READ")).header("X-Reauth-Assertion", "valide")).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.PUT, u, PRESENTATION).with(jeton("IAM_PROFILE_EDIT"))).andExpect(status().is(428));
        mvc.perform(req(HttpMethod.PUT, u, PRESENTATION).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "autre")).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.PUT, u, PRESENTATION).with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("ROLE_POLE_RESEAU"))
            .andExpect(jsonPath("$.libelle").value("Pôle réseau et télécoms"))
            .andExpect(jsonPath("$.description").value("Exploite le réseau"))
            .andExpect(jsonPath("$.permissions[0]").value("HOM_EVIDENCE_SUBMIT"))
            .andExpect(jsonPath("$.privilegie").value(false))
            .andExpect(jsonPath("$.cree").value(false))
            .andExpect(jsonPath("$.titulaires").value(2))
            .andExpect(jsonPath("$.modifieLe").value("2026-10-06T09:00:00Z"))
            .andExpect(jsonPath("$.modifiePar.id").value(ID))
            .andExpect(jsonPath("$.modifiePar.nomAffiche").value("Chef"));
        verify(profils).modifier(any(), eq("ROLE_POLE_RESEAU"), eq("Pôle réseau et télécoms"), eq("Exploite le réseau"), eq("Réorganisation"));
        // Motif obligatoire ; libellé démesuré refusé dès la requête.
        mvc.perform(req(HttpMethod.PUT, u, "{\"libelle\":\"Pôle\",\"description\":\"\"}").with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide")).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.PUT, u, "{\"libelle\":\"" + "x".repeat(201) + "\",\"motif\":\"m\"}").with(jeton("IAM_PROFILE_EDIT")).header("X-Reauth-Assertion", "valide")).andExpect(status().isBadRequest());

        mvc
            .perform(req(HttpMethod.GET, "/api/v1/configuration/profils", null).with(jeton("IAM_ACCOUNT_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].libelle").value("Pôle réseau et télécoms"))
            .andExpect(jsonPath("$[0].modifiePar.nomAffiche").value("Chef"))
            .andExpect(jsonPath("$[1].code").value("ROLE_SNA"))
            .andExpect(jsonPath("$[1].libelle").value("ROLE_SNA"))
            .andExpect(jsonPath("$[1].modifieLe").isEmpty())
            .andExpect(jsonPath("$[1].modifiePar").isEmpty());
    }

    @Test
    @DisplayName("Rattachement d'un subordonné : PIL_ACTIVITY_OWN (401, 403 sans) ; corps de la route d'administration et motif ; réponse : la fiche du compte")
    void rattachementSubordonne() throws Exception {
        String u = "/api/v1/pouvoirs/subordonnes/c1/rattachement";
        String corps = "{\"unite\":\"POLE_RESEAU\",\"responsableId\":\"" + ID + "\",\"motif\":\"Réorganisation\"}";
        Compte c = new Compte("c1", "awa.mballa", "Awa", "Mballa", "awa@gimac.cm", true, true, Instant.EPOCH, null, null, "fr", null, true, Set.of("ROLE_SNA"), List.of(), false)
            .avecRattachement("POLE_RESEAU", ID);
        when(pouvoirs.rattacherSubordonne(any(), eq("c1"), eq("POLE_RESEAU"), eq(ID), eq("Réorganisation"))).thenReturn(c);
        when(avatars.version("c1")).thenReturn(Optional.of("v1"));
        mvc.perform(req(HttpMethod.PUT, u, corps)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.PUT, u, corps).with(jeton("IAM_ACCOUNT_EDIT"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.PUT, u, corps).with(jeton("PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("c1"))
            .andExpect(jsonPath("$.nomAffiche").exists())
            .andExpect(jsonPath("$.unite").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.responsableId").value(ID))
            .andExpect(jsonPath("$.avatarVersion").value("v1"));
        verify(pouvoirs).rattacherSubordonne(any(), eq("c1"), eq("POLE_RESEAU"), eq(ID), eq("Réorganisation"));
        mvc.perform(req(HttpMethod.PUT, u, "{\"unite\":\"" + "X".repeat(40) + "\"}").with(jeton("PIL_ACTIVITY_OWN"))).andExpect(status().isBadRequest());
    }
}
