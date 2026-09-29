package cm.gimac.des.iam.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.ConfigurationAuthentificationService;
import cm.gimac.des.iam.application.usecase.MonCompteService;
import cm.gimac.des.iam.application.usecase.ReauthentificationService;
import cm.gimac.des.iam.config.SecurityConfiguration;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import cm.gimac.des.iam.web.rest.errors.IamExceptionHandler;
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

/**
 * Sécurité des routes d'IAM (D-61, D-62) avec la vraie chaîne de filtres : 401 sans jeton ; 403 sans
 * second facteur ; 403 sans la permission de la matrice des rôles ; 428 sans réauthentification forte
 * pour les actes sensibles ; routes internes réservées au jeton de service.
 */
@WebMvcTest(controllers = { ComptesResource.class, MonCompteResource.class, ReauthentificationResource.class, ConfigurationAuthentificationResource.class })
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, IamExceptionHandler.class, ReauthAdministration.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class IamRoutesSecurityTest {

    static final String ID = "3f1c2d4e-0000-4000-8000-000000000001";
    static final String CLE = UUID.randomUUID().toString();
    static final String MOTIF = "{\"motif\":\"Départ de l'agent\"}";
    static final String CREATION = """
        {"identifiant":"agent.ddrm","prenom":"Awa","nom":"Mballa","email":"awa@gimac.cm","profils":["ROLE_CHARGE_DDRM"],"envoyerInvitation":true}""";
    static final String FICHE = "{\"telephone\":\"+237600000000\"}";
    static final String PROFILS = "{\"profils\":[\"ROLE_CHARGE_DDRM\"],\"motif\":\"Mutation\"}";
    static final String ACTIONS = "{\"actions\":[\"UPDATE_PASSWORD\",\"CONFIGURE_TOTP\"],\"parCourriel\":true,\"motif\":\"Audit\"}";
    static final String POLITIQUE =
        "{\"politique\":{\"longueurMinimale\":14,\"majusculesMin\":1,\"minusculesMin\":1,\"chiffresMin\":1,\"speciauxMin\":1,\"historique\":5,\"expirationJours\":90,\"interditIdentifiant\":true,\"otpChiffres\":6,\"otpPeriodeSecondes\":30,\"otpAlgorithme\":\"HmacSHA256\",\"sessionInactiviteMinutes\":30,\"sessionDureeMaxHeures\":10,\"forceBruteActive\":true,\"forceBruteEchecsMax\":5,\"forceBruteAttenteMinutes\":15},\"motif\":\"Revue\"}";
    static final String MESSAGERIE =
        "{\"messagerie\":{\"hote\":\"smtp.gimac.cm\",\"port\":587,\"expediteur\":\"des@gimac.cm\",\"starttls\":true,\"authentification\":false},\"motif\":\"Mise en service\"}";
    static final String PERMISSIONS = "{\"permissions\":[\"PAR_INSTITUTION_READ\"],\"motif\":\"Revue\"}";

    static final List<String> PERMISSIONS_IAM = List.of(
        "IAM_ACCOUNT_READ",
        "IAM_ACCOUNT_CREATE",
        "IAM_ACCOUNT_EDIT",
        "IAM_ACCOUNT_ROLES",
        "IAM_ACCOUNT_STATUS",
        "IAM_ACCOUNT_DELETE",
        "IAM_ACCOUNT_SECURITY",
        "IAM_JOURNAL_READ",
        "IAM_PROFILE_EDIT",
        "PLATFORM_SECURITY_POLICY_READ",
        "PLATFORM_SECURITY_POLICY_EDIT",
        "PLATFORM_MESSAGING_EDIT"
    );

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdministrationComptesService comptes;

    @MockitoBean
    MonCompteService moi;

    @MockitoBean
    ConfigurationAuthentificationService configuration;

    @MockitoBean
    ReauthentificationService reauth;

    @MockitoBean
    JournalAdministrationRepository journal;

    Compte compte;

    @BeforeEach
    void init() {
        compte = new Compte(ID, "agent.ddrm", "Awa", "Mballa", "awa@gimac.cm", true, true, Instant.EPOCH, null, null, "fr", null, false, Set.of("ROLE_CHARGE_DDRM"), List.of(ActionRequise.UPDATE_PASSWORD), false);
        var mc = new MonCompteService.MonCompte(compte, 1, 2, List.of("FICHE"));
        var politique = new PolitiqueSecurite(14, 1, 1, 1, 1, 5, 90, true, 6, 30, "HmacSHA256", 30, 10, true, 5, 15);
        var messagerie = new ConfigurationMessagerie("smtp.gimac.cm", 587, "des@gimac.cm", null, true, false, false, null, null, false);
        var profil = new Profil("ROLE_CHARGE_DDRM", "Chargé DDRM", Set.of("PAR_INSTITUTION_READ"), false, 3);
        var session = new SessionCompte("s1", "10.0.0.1", Instant.EPOCH, Instant.EPOCH, List.of("web_app"));
        var cle = new ReauthentificationService.Cle(UUID.fromString(CLE), "Clé", Instant.EPOCH, null);
        when(comptes.lister(any(), anyInt(), anyInt())).thenReturn(new AdministrationComptesService.PageComptes(List.of(compte), 1));
        when(comptes.indicateurs()).thenReturn(new AdministrationComptesService.Indicateurs(1, 1, 0, 1, 1, 0, 1, 2, Map.of("ROLE_CHARGE_DDRM", 1L)));
        when(comptes.lire(ID)).thenReturn(compte);
        when(comptes.sessions(ID)).thenReturn(List.of(session));
        when(comptes.creer(any(), any(), anyBoolean(), anyBoolean())).thenReturn(new AdministrationComptesService.Creation(compte, true, null));
        when(comptes.modifier(any(), eq(ID), any())).thenReturn(compte);
        when(comptes.definirProfils(any(), eq(ID), any(), anyString())).thenReturn(compte);
        when(comptes.definirActif(any(), eq(ID), anyBoolean(), anyString())).thenReturn(compte);
        when(comptes.exigerActions(any(), eq(ID), any(), anyBoolean(), anyString())).thenReturn(compte);
        when(comptes.reinitialiserSecondFacteur(any(), eq(ID), anyString())).thenReturn(compte);
        when(comptes.motDePasseProvisoire(any(), eq(ID), anyString())).thenReturn("Provisoire-123!");
        when(journal.findAllByOrderByHorodatageDesc(any())).thenReturn(new PageImpl<>(List.of()));
        when(journal.findByCibleIdOrderByHorodatageDesc(anyString(), any())).thenReturn(new PageImpl<>(List.<JournalAdministration>of()));
        when(moi.moi(any())).thenReturn(mc);
        when(moi.premiereConfiguration(any(), anyString(), anyString(), any(), anyBoolean())).thenReturn(mc);
        when(moi.modifier(any(), any(), any(), any(), any(), any())).thenReturn(mc);
        when(moi.demander(any(), any())).thenReturn(mc);
        when(moi.sessions(any())).thenReturn(List.of(session));
        when(reauth.cles(any())).thenReturn(List.of(cle));
        when(reauth.enroler(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(cle);
        when(reauth.defiEnrolement(any())).thenReturn(
            new ReauthentificationService.OptionsEnrolement("c", "d", new ReauthentificationService.Rp("localhost", "DES"), new ReauthentificationService.Utilisateur("u", "u", "U"), List.of(), 120000, List.of())
        );
        when(reauth.defi(any(), anyString())).thenReturn(new ReauthentificationService.Defi("c", "d", "localhost", List.of("k"), 120000));
        when(reauth.verifier(eq("valide"), anyString(), anyString())).thenReturn(true);
        when(configuration.politique()).thenReturn(politique);
        when(configuration.appliquerPolitique(any(), any(), anyString())).thenReturn(politique);
        when(configuration.messagerie()).thenReturn(messagerie);
        when(configuration.appliquerMessagerie(any(), any(), anyString())).thenReturn(messagerie);
        when(configuration.profils()).thenReturn(List.of(profil));
        when(configuration.permissions()).thenReturn(List.of("PAR_INSTITUTION_READ"));
        when(configuration.definirPermissions(any(), eq("ROLE_CHARGE_DDRM"), any(), anyString())).thenReturn(profil);
    }

    /** Routes d'administration : méthode, URI, corps, permission exigée, statut en cas d'accès. */
    static Stream<Arguments> administration() {
        String c = "/api/v1/comptes";
        String k = "/api/v1/configuration";
        return Stream.of(
            Arguments.of(HttpMethod.GET, c, null, "IAM_ACCOUNT_READ", 200),
            Arguments.of(HttpMethod.GET, c + "/indicateurs", null, "IAM_ACCOUNT_READ", 200),
            Arguments.of(HttpMethod.GET, c + "/journal", null, "IAM_JOURNAL_READ", 200),
            Arguments.of(HttpMethod.GET, c + "/" + ID, null, "IAM_ACCOUNT_READ", 200),
            Arguments.of(HttpMethod.GET, c + "/" + ID + "/sessions", null, "IAM_ACCOUNT_READ", 200),
            Arguments.of(HttpMethod.POST, c, CREATION, "IAM_ACCOUNT_CREATE", 201),
            Arguments.of(HttpMethod.PUT, c + "/" + ID, FICHE, "IAM_ACCOUNT_EDIT", 200),
            Arguments.of(HttpMethod.PUT, c + "/" + ID + "/profils", PROFILS, "IAM_ACCOUNT_ROLES", 200),
            Arguments.of(HttpMethod.POST, c + "/" + ID + "/activation", MOTIF, "IAM_ACCOUNT_STATUS", 200),
            Arguments.of(HttpMethod.POST, c + "/" + ID + "/desactivation", MOTIF, "IAM_ACCOUNT_STATUS", 200),
            Arguments.of(HttpMethod.DELETE, c + "/" + ID + "?motif=Depart", null, "IAM_ACCOUNT_DELETE", 204),
            Arguments.of(HttpMethod.POST, c + "/" + ID + "/actions-requises", ACTIONS, "IAM_ACCOUNT_SECURITY", 200),
            Arguments.of(HttpMethod.POST, c + "/" + ID + "/second-facteur/reinitialisation", MOTIF, "IAM_ACCOUNT_SECURITY", 200),
            Arguments.of(HttpMethod.POST, c + "/" + ID + "/mot-de-passe-provisoire", MOTIF, "IAM_ACCOUNT_SECURITY", 200),
            Arguments.of(HttpMethod.POST, c + "/" + ID + "/sessions/fermeture", MOTIF, "IAM_ACCOUNT_SECURITY", 204),
            Arguments.of(HttpMethod.GET, k + "/politique-securite", null, "PLATFORM_SECURITY_POLICY_READ", 200),
            Arguments.of(HttpMethod.PUT, k + "/politique-securite", POLITIQUE, "PLATFORM_SECURITY_POLICY_EDIT", 200),
            Arguments.of(HttpMethod.GET, k + "/messagerie", null, "PLATFORM_MESSAGING_EDIT", 200),
            Arguments.of(HttpMethod.PUT, k + "/messagerie", MESSAGERIE, "PLATFORM_MESSAGING_EDIT", 200),
            Arguments.of(HttpMethod.POST, k + "/messagerie/test", "{\"destinataire\":\"rssi@gimac.cm\"}", "PLATFORM_MESSAGING_EDIT", 204),
            Arguments.of(HttpMethod.GET, k + "/profils", null, "IAM_ACCOUNT_READ", 200),
            Arguments.of(HttpMethod.GET, k + "/permissions", null, "IAM_ACCOUNT_READ", 200),
            Arguments.of(HttpMethod.PUT, k + "/profils/ROLE_CHARGE_DDRM/permissions", PERMISSIONS, "IAM_PROFILE_EDIT", 200)
        );
    }

    /** Routes « Mon compte » et défi de réauthentification : tout utilisateur à deux facteurs. */
    static Stream<Arguments> libreService() {
        String m = "/api/v1/moi";
        return Stream.of(
            Arguments.of(HttpMethod.GET, m, null, 200),
            Arguments.of(HttpMethod.POST, m + "/premiere-configuration", "{\"telephone\":\"+237600000000\",\"langue\":\"fr\",\"charteAcceptee\":true}", 200),
            Arguments.of(HttpMethod.PUT, m, "{\"prenom\":\"Awa\"}", 200),
            Arguments.of(HttpMethod.POST, m + "/actions", "{\"action\":\"UPDATE_PASSWORD\"}", 200),
            Arguments.of(HttpMethod.GET, m + "/sessions", null, 200),
            Arguments.of(HttpMethod.DELETE, m + "/sessions/s1", null, 204),
            Arguments.of(HttpMethod.DELETE, m + "/sessions", null, 204),
            Arguments.of(HttpMethod.GET, m + "/cles-securite", null, 200),
            Arguments.of(HttpMethod.POST, m + "/cles-securite/defi", null, 200),
            Arguments.of(HttpMethod.POST, m + "/cles-securite", "{\"challengeId\":\"c\",\"nom\":\"Clé\",\"clientDataJSON\":\"e30\",\"attestationObject\":\"e30\"}", 200),
            Arguments.of(HttpMethod.DELETE, m + "/cles-securite/" + CLE, null, 204),
            Arguments.of(HttpMethod.POST, "/api/v1/reauthentification/defi", "{\"purpose\":\"STATUS_APPROVE\"}", 200)
        );
    }

    static MockHttpServletRequestBuilder req(HttpMethod m, String uri, String body) {
        MockHttpServletRequestBuilder b = request(m, uri).header("X-Reauth-Assertion", "valide");
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return b;
    }

    static RequestPostProcessor jeton(String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject("demo-super-admin").claim("name", "Super Admin");
                if (acr != null) {
                    j.claim("acr", acr);
                }
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("administration")
    @DisplayName("Administration : 401 sans jeton, 403 sans second facteur, 403 sans la permission, accès avec")
    void administrationMatrice(HttpMethod m, String uri, String body, String permission, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
        mvc.perform(req(m, uri, body).with(jeton(null, permission))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("1", permission))).andExpect(status().isForbidden());
        String[] autres = PERMISSIONS_IAM.stream().filter(p -> !p.equals(permission)).toArray(String[]::new);
        mvc.perform(req(m, uri, body).with(jeton("2", autres))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("2", permission))).andExpect(status().is(ok));
        mvc.perform(req(m, uri, body).with(jeton("3", permission))).andExpect(status().is(ok));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("libreService")
    @DisplayName("Libre-service : 401 sans jeton, 403 sans second facteur, accès pour tout utilisateur à deux facteurs")
    void libreServiceMatrice(HttpMethod m, String uri, String body, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
        mvc.perform(req(m, uri, body).with(jeton("1"))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("2"))).andExpect(status().is(ok));
    }

    static Stream<Arguments> sensibles() {
        return Stream.of(
            Arguments.of(HttpMethod.PUT, "/api/v1/comptes/" + ID + "/profils", PROFILS, "IAM_ACCOUNT_ROLES"),
            Arguments.of(HttpMethod.DELETE, "/api/v1/comptes/" + ID + "?motif=Depart", null, "IAM_ACCOUNT_DELETE"),
            Arguments.of(HttpMethod.PUT, "/api/v1/configuration/politique-securite", POLITIQUE, "PLATFORM_SECURITY_POLICY_EDIT"),
            Arguments.of(HttpMethod.PUT, "/api/v1/configuration/profils/ROLE_CHARGE_DDRM/permissions", PERMISSIONS, "IAM_PROFILE_EDIT")
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("sensibles")
    @DisplayName("IAM-07 : actes sensibles sans assertion → 428 ; assertion refusée → 403")
    void reauthentificationForte(HttpMethod m, String uri, String body, String permission) throws Exception {
        MockHttpServletRequestBuilder sans = request(m, uri);
        MockHttpServletRequestBuilder fausse = request(m, uri).header("X-Reauth-Assertion", "rejouee");
        if (body != null) {
            sans.contentType(MediaType.APPLICATION_JSON).content(body);
            fausse.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        mvc.perform(sans.with(jeton("2", permission))).andExpect(status().is(428)).andExpect(jsonPath("$.code").value("IAM_REAUTH_REQUIRED"));
        mvc.perform(fausse.with(jeton("2", permission))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("IAM_REAUTH_INVALID"));
        verify(reauth, never()).verifier(eq("valide"), anyString(), eq("AUTRE"));
    }

    @Test
    @DisplayName("Vérification d'assertion : route interne, jeton de service exigé")
    void routeInterne() throws Exception {
        String corps = "{\"assertion\":\"valide\",\"sujet\":\"chef\",\"finalite\":\"STATUS_APPROVE\"}";
        mvc.perform(req(HttpMethod.POST, "/api/v1/interne/reauthentifications/verification", corps).with(jeton("2", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/interne/reauthentifications/verification", corps).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valide").value(true));
    }

    @Test
    @DisplayName("Jeton de service : aucune route d'administration ; CRUD générés réservés à l'administration technique")
    void cloisonnement() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/v1/comptes", null).with(jwt().authorities(new SimpleGrantedAuthority("IAM_ACCOUNT_READ")))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, "/api/roles", null).with(jeton("2", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Formes : pagination, Location, fiche sans secret, erreurs métier en ProblemDetail")
    void formes() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/comptes?q=awa&profil=ROLE_CHARGE_DDRM&etat=ACTIF", null).with(jeton("2", "IAM_ACCOUNT_READ")))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].nomAffiche").value("Awa Mballa"))
            .andExpect(jsonPath("$.contenu[0].premierAccesEnAttente").value(true))
            .andExpect(jsonPath("$.contenu[0].actionsRequises[0]").value("UPDATE_PASSWORD"));
        mvc.perform(req(HttpMethod.POST, "/api/v1/comptes", CREATION).with(jeton("2", "IAM_ACCOUNT_CREATE"))).andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/comptes/" + ID));
        mvc.perform(req(HttpMethod.GET, "/api/v1/comptes/journal?compte=" + ID, null).with(jeton("2", "IAM_JOURNAL_READ"))).andExpect(jsonPath("$.total").value(0));
        when(journal.findAllByOrderByHorodatageDesc(any())).thenReturn(
            new PageImpl<>(List.of(new JournalAdministration(Instant.EPOCH, "demo-rssi", "RSSI", "DESACTIVATION", ID, "agent.ddrm", "Départ", "actif", "corr-1")))
        );
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/comptes/journal", null).with(jeton("2", "IAM_JOURNAL_READ")))
            .andExpect(jsonPath("$.contenu[0].action").value("DESACTIVATION"))
            .andExpect(jsonPath("$.contenu[0].acteurNom").value("RSSI"))
            .andExpect(jsonPath("$.contenu[0].correlationId").value("corr-1"));
        mvc.perform(req(HttpMethod.GET, "/api/v1/moi", null).with(jeton("2"))).andExpect(jsonPath("$.etapesRestantes[0]").value("FICHE"));
        when(comptes.lire("absent")).thenThrow(IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", "absent"));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/comptes/absent", null).with(jeton("2", "IAM_ACCOUNT_READ")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("IAM_ACCOUNT_NOT_FOUND"))
            .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("Acteur : nom repris de preferred_username à défaut de name ; jeton sans sujet refusé")
    void acteur() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/moi", null).with(jwt().jwt(j -> j.subject("u1").claim("acr", "2").claim("preferred_username", "agent"))))
            .andExpect(status().isOk());
        verify(moi).moi(org.mockito.ArgumentMatchers.argThat(a -> a.nom().equals("agent") && a.id().equals("u1")));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/moi", null).with(jwt().jwt(j -> j.subject(null).claim("sub", "").claim("acr", "2").claim("name", " "))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Validation d'entrée : courriel invalide, profils vides, motif absent refusés (400)")
    void validation() throws Exception {
        mvc.perform(req(HttpMethod.POST, "/api/v1/comptes", CREATION.replace("awa@gimac.cm", "pas-un-courriel")).with(jeton("2", "IAM_ACCOUNT_CREATE"))).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.POST, "/api/v1/comptes", CREATION.replace("[\"ROLE_CHARGE_DDRM\"]", "[]")).with(jeton("2", "IAM_ACCOUNT_CREATE"))).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.POST, "/api/v1/comptes/" + ID + "/desactivation", "{}").with(jeton("2", "IAM_ACCOUNT_STATUS"))).andExpect(status().isBadRequest());
        verify(comptes, never()).definirActif(any(), anyString(), eq(false), any());
    }
}
