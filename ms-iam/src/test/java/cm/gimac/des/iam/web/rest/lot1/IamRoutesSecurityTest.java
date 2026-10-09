package cm.gimac.des.iam.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.application.usecase.ConfigurationAuthentificationService;
import cm.gimac.des.iam.application.usecase.ConnexionService;
import cm.gimac.des.iam.application.usecase.MonCompteService;
import cm.gimac.des.iam.application.usecase.OrganisationService;
import cm.gimac.des.iam.application.usecase.ProfilsService;
import cm.gimac.des.iam.application.usecase.ReauthentificationService;
import cm.gimac.des.iam.config.SecurityConfiguration;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.EtatConnexion;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import cm.gimac.des.iam.web.rest.errors.IamExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Sécurité des routes d'IAM (D-61, D-62, D-65) avec la vraie chaîne de filtres : 401 sans jeton ; 403
 * sans second facteur ; 403 sans la permission de la matrice des rôles ; 428 sans réauthentification
 * forte pour les actes sensibles ; routes internes réservées au jeton de service ; acr 1 (premier
 * accès) admis sur les seules routes de l'assistant ; routes internes de la connexion réservées au
 * jeton de service de la passerelle.
 */
@WebMvcTest(
    controllers = {
        ComptesResource.class,
        MonCompteResource.class,
        ReauthentificationResource.class,
        ConfigurationAuthentificationResource.class,
        CollaborateursInternesResource.class,
        ConnexionInterneResource.class,
        AvatarResource.class,
    }
)
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
    static final String ACTIONS = "{\"actions\":[\"MOT_DE_PASSE\",\"SECOND_FACTEUR\"],\"parCourriel\":true,\"motif\":\"Audit\"}";
    static final String ENROLEMENT = "3f1c2d4e-0000-4000-8000-00000000e001";
    static final String TOTP = "{\"enrolementId\":\"" + ENROLEMENT + "\",\"code\":\"123456\",\"libelle\":\"Téléphone\"}";
    static final String MOT_DE_PASSE = "{\"actuel\":\"Ancien-Mot-2026!\",\"nouveau\":\"Nouveau-Mot-2026!\"}";
    static final String POLITIQUE =
        "{\"politique\":{\"longueurMinimale\":14,\"majusculesMin\":1,\"minusculesMin\":1,\"chiffresMin\":1,\"speciauxMin\":1,\"historique\":5,\"expirationJours\":90,\"interditIdentifiant\":true,\"otpChiffres\":6,\"otpPeriodeSecondes\":30,\"otpAlgorithme\":\"HmacSHA256\",\"sessionInactiviteMinutes\":30,\"sessionDureeMaxHeures\":10,\"forceBruteActive\":true,\"forceBruteEchecsMax\":5,\"forceBruteAttenteMinutes\":15},\"motif\":\"Revue\"}";
    static final String MESSAGERIE =
        "{\"messagerie\":{\"hote\":\"smtp.gimac.cm\",\"port\":587,\"expediteur\":\"des@gimac.cm\",\"starttls\":true,\"authentification\":false},\"motif\":\"Mise en service\"}";
    static final String ALERTE =
        "{\"navigateur\":\"Firefox 131\",\"systeme\":\"Windows 10\",\"type\":\"ORDINATEUR\",\"adresseIp\":\"41.202.10.20\",\"couche\":\"des\",\"le\":\"2026-10-01T08:30:00Z\"}";
    static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
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

    @MockitoBean
    ConnexionService connexion;

    @MockitoBean
    AvatarService avatars;

    @MockitoBean
    OrganisationService organisation;

    @MockitoBean
    ProfilsService profilsCrees;

    Compte compte;

    @BeforeEach
    void init() {
        compte = new Compte(ID, "agent.ddrm", "Awa", "Mballa", "awa@gimac.cm", true, true, Instant.EPOCH, null, null, "fr", null, false, Set.of("ROLE_CHARGE_DDRM"), List.of(ActionRequise.MOT_DE_PASSE), false);
        var mc = new MonCompteService.MonCompte(compte, 1, 2, List.of("FICHE"), null);
        var politique = new PolitiqueSecurite(14, 1, 1, 1, 1, 5, 90, true, 6, 30, "HmacSHA256", 30, 10, true, 5, 15);
        var messagerie = new ConfigurationMessagerie("smtp.gimac.cm", 587, "des@gimac.cm", null, true, false, false, null, null, false);
        var profil = new Profil("ROLE_CHARGE_DDRM", "Chargé DDRM", Set.of("PAR_INSTITUTION_READ"), false, 3);
        var session = new SessionCompte("s1", "10.0.0.1", Instant.EPOCH, Instant.EPOCH, List.of("web_app"));
        var cle = new ReauthentificationService.Cle(UUID.fromString(CLE), "Clé", Instant.EPOCH, null);
        when(comptes.lister(any(), anyInt(), anyInt())).thenReturn(new AdministrationComptesService.PageComptes(List.of(compte), 1));
        when(comptes.indicateurs()).thenReturn(new AdministrationComptesService.Indicateurs(1, 1, 0, 1, 1, 0, 1, 2, Map.of("ROLE_CHARGE_DDRM", 1L),
            List.of(new AdministrationComptesService.ParJour(java.time.LocalDate.parse("2026-09-29"), 2)), List.of()));
        when(comptes.lire(ID)).thenReturn(compte);
        when(comptes.sessions(ID)).thenReturn(List.of(session));
        when(comptes.creer(any(), any(), anyBoolean(), anyBoolean())).thenReturn(new AdministrationComptesService.Creation(compte, true, null));
        when(comptes.modifier(any(), eq(ID), any())).thenReturn(compte);
        when(organisation.rattacher(any(), eq(ID), any(), any(), any(), any())).thenReturn(compte);
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
        when(moi.enrolementTotp(any())).thenReturn(new MonCompteService.Enrolement(UUID.fromString(ENROLEMENT), "JBSWY3DPEHPK3PXP", "otpauth://totp/GIMAC%20DES:agent"));
        when(connexion.etat(ID)).thenReturn(new EtatConnexion(true, false, List.of(ActionRequise.SECOND_FACTEUR, ActionRequise.FICHE)));
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
            Arguments.of(HttpMethod.PUT, c + "/" + ID + "/rattachement", "{\"unite\":\"POLE_RESEAU\",\"responsableId\":null}", "IAM_ACCOUNT_EDIT", 200),
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

    /** Routes de l'assistant de premier accès (D-65) : acr 1 admis, comme acr 2 et 3. */
    static Stream<Arguments> premierAcces() {
        String m = "/api/v1/moi";
        return Stream.of(
            Arguments.of(HttpMethod.GET, m, null, 200),
            Arguments.of(HttpMethod.POST, m + "/premiere-configuration", "{\"telephone\":\"+237600000000\",\"langue\":\"fr\",\"charteAcceptee\":true}", 200),
            Arguments.of(HttpMethod.POST, m + "/mot-de-passe", MOT_DE_PASSE, 204),
            Arguments.of(HttpMethod.POST, m + "/second-facteur/totp/enrolement", null, 200),
            Arguments.of(HttpMethod.POST, m + "/second-facteur/totp", TOTP, 204)
        );
    }

    /** Routes « Mon compte » et défi de réauthentification : tout utilisateur à deux facteurs. */
    static Stream<Arguments> libreService() {
        String m = "/api/v1/moi";
        return Stream.of(
            Arguments.of(HttpMethod.PUT, m, "{\"prenom\":\"Awa\"}", 200),
            Arguments.of(HttpMethod.POST, m + "/actions", "{\"action\":\"MOT_DE_PASSE\"}", 200),
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
    @MethodSource("premierAcces")
    @DisplayName("D-65 : assistant de premier accès, 401 sans jeton, 403 sans acr, accès en acr 1, 2 et 3")
    void premierAccesMatrice(HttpMethod m, String uri, String body, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
        mvc.perform(req(m, uri, body).with(jeton(null))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("0"))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("1"))).andExpect(status().is(ok));
        mvc.perform(req(m, uri, body).with(jeton("2"))).andExpect(status().is(ok));
        mvc.perform(req(m, uri, body).with(jeton("3"))).andExpect(status().is(ok));
    }

    @Test
    @DisplayName("Indicateurs : jours des séries en date ISO (AAAA-MM-JJ), jamais en tableau")
    void seriesEnDateIso() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/comptes/indicateurs", null).with(jeton("2", "IAM_ACCOUNT_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creationsParJour[0].jour").value("2026-09-29"))
            .andExpect(jsonPath("$.creationsParJour[0].nombre").value(2))
            .andExpect(jsonPath("$.actesParJour").isArray());
    }

    @Test
    @DisplayName("D-65 : acr 1 limité aux routes listées (même chemin avec une autre méthode refusé)")
    void premierAccesBorne() throws Exception {
        mvc.perform(req(HttpMethod.PUT, "/api/v1/moi", "{\"prenom\":\"Awa\"}").with(jeton("1"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, "/api/v1/moi/sessions", null).with(jeton("1"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.POST, "/api/v1/moi/actions", "{\"action\":\"MOT_DE_PASSE\"}").with(jeton("1"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, "/api/v1/moi/second-facteur/totp", null).with(jeton("1"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.POST, "/api/v1/reauthentification/defi", "{\"purpose\":\"STATUS_APPROVE\"}").with(jeton("1"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, "/api/v1/comptes", null).with(jeton("1", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, "/api/v1/configuration/politique-securite", null).with(jeton("1", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
    }

    static RequestPostProcessor service(String client, String... permissions) {
        return jwt().jwt(j -> j.subject("service-account-" + client).claim("azp", client)).authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    /** Routes internes de la connexion (D-65) : méthode, URI, corps, statut attendu pour la passerelle. */
    static Stream<Arguments> connexionInterne() {
        return Stream.of(
            Arguments.of(HttpMethod.GET, "/api/v1/interne/comptes/" + ID + "/etat-connexion", null, 200),
            Arguments.of(HttpMethod.POST, "/api/v1/interne/connexion/mot-de-passe-oublie", "{\"identifiant\":\"agent\",\"couche\":\"des\"}", 202),
            Arguments.of(HttpMethod.POST, "/api/v1/interne/connexion/nouveau-mot-de-passe", "{\"jeton\":\"abc\",\"motDePasse\":\"Nouveau-Mot-2026!\"}", 204),
            Arguments.of(HttpMethod.POST, "/api/v1/interne/comptes/" + ID + "/alerte-connexion", ALERTE, 204)
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("connexionInterne")
    @DisplayName("D-65 : routes internes de la connexion, jeton de service de la passerelle (gateway-service, INTERNAL_IAM_READ) seulement")
    void connexionInterneMatrice(HttpMethod m, String uri, String body, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
        mvc.perform(req(m, uri, body).with(jeton("2", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(service("homologation-service", "INTERNAL_IAM_READ"))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(service("gateway-service", "INTERNAL_DOCUMENT_READ"))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jwt().jwt(j -> j.subject("s").claim("client_id", "autre")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ")))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(service("gateway-service", "INTERNAL_IAM_READ"))).andExpect(status().is(ok));
        mvc.perform(req(m, uri, body).with(jwt().jwt(j -> j.subject("s").claim("client_id", "gateway-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ")))).andExpect(status().is(ok));
    }

    @Test
    @DisplayName("D-65 : formes du contrat, état de connexion, erreurs problem+json (jeton, politique avec violations, code TOTP)")
    void formesConnexion() throws Exception {
        var passerelle = service("gateway-service", "INTERNAL_IAM_READ");
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/interne/comptes/" + ID + "/etat-connexion", null).with(passerelle))
            .andExpect(jsonPath("$.actif").value(true))
            .andExpect(jsonPath("$.secondFacteur").value(false))
            .andExpect(jsonPath("$.actionsRequises[0]").value("SECOND_FACTEUR"))
            .andExpect(jsonPath("$.actionsRequises[1]").value("FICHE"));
        org.mockito.Mockito.doThrow(IamException.invalide("CNX_JETON_INVALIDE", "Lien inconnu, expiré ou déjà utilisé", Map.of())).when(connexion).nouveauMotDePasse(eq("perime"), anyString());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/interne/connexion/nouveau-mot-de-passe", "{\"jeton\":\"perime\",\"motDePasse\":\"x\"}").with(passerelle))
            .andExpect(status().isBadRequest())
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
            .andExpect(jsonPath("$.code").value("CNX_JETON_INVALIDE"));
        org.mockito.Mockito.doThrow(IamException.invalide("IAM_PASSWORD_POLICY", "Politique", Map.of("violations", List.of("LONGUEUR")))).when(connexion).nouveauMotDePasse(eq("court"), anyString());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/interne/connexion/nouveau-mot-de-passe", "{\"jeton\":\"court\",\"motDePasse\":\"x\"}").with(passerelle))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_PASSWORD_POLICY"))
            .andExpect(jsonPath("$.violations[0]").value("LONGUEUR"))
            .andExpect(jsonPath("$.details.violations[0]").value("LONGUEUR"));
        mvc.perform(req(HttpMethod.POST, "/api/v1/interne/connexion/nouveau-mot-de-passe", "{\"motDePasse\":\"x\"}").with(passerelle)).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.POST, "/api/v1/interne/connexion/mot-de-passe-oublie", "{\"identifiant\":\" \"}").with(passerelle)).andExpect(status().isBadRequest());
        verify(connexion, never()).motDePasseOublie(anyString(), any());

        mvc
            .perform(req(HttpMethod.POST, "/api/v1/moi/second-facteur/totp/enrolement", null).with(jeton("1")))
            .andExpect(jsonPath("$.enrolementId").value(ENROLEMENT))
            .andExpect(jsonPath("$.secret").value("JBSWY3DPEHPK3PXP"))
            .andExpect(jsonPath("$.uri").value("otpauth://totp/GIMAC%20DES:agent"));
        org.mockito.Mockito.doThrow(IamException.invalide("IAM_TOTP_CODE_INVALID", "Code", Map.of())).when(moi).confirmerTotp(any(), any(), eq("000000"), any(), any());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/moi/second-facteur/totp", TOTP.replace("123456", "000000")).with(jeton("1")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_TOTP_CODE_INVALID"));
        mvc.perform(req(HttpMethod.POST, "/api/v1/moi/second-facteur/totp", TOTP).with(jeton("1"))).andExpect(status().isNoContent());
        verify(moi).confirmerTotp(any(), eq(UUID.fromString(ENROLEMENT)), eq("123456"), eq("Téléphone"), eq("valide"));
        mvc.perform(req(HttpMethod.POST, "/api/v1/moi/second-facteur/totp", "{\"code\":\"123456\"}").with(jeton("1"))).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.POST, "/api/v1/moi/mot-de-passe", MOT_DE_PASSE).with(jeton("1"))).andExpect(status().isNoContent());
        verify(moi).changerMotDePasse(any(), eq("Ancien-Mot-2026!"), eq("Nouveau-Mot-2026!"), eq(false), org.mockito.ArgumentMatchers.isNull());
        // Aucune trace des secrets dans la représentation des requêtes.
        assertThat(new MonCompteResource.MotDePasseRequest("a1", "b2").toString()).doesNotContain("a1").doesNotContain("b2");
        assertThat(new MonCompteResource.TotpRequest(UUID.fromString(ENROLEMENT), "654321", null).toString()).doesNotContain("654321");
        assertThat(new ConnexionInterneResource.NouveauMotDePasseRequest("jeton-secret", "mdp-secret").toString()).doesNotContain("secret");
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
    @DisplayName("D-87 : finalité SIGNATURE_PROCESSUS admise au défi (jeton à deux facteurs) et à la vérification interne (jeton du client pilotage-service)")
    void signatureProcessus() throws Exception {
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/reauthentification/defi", "{\"purpose\":\"SIGNATURE_PROCESSUS\"}").with(jeton("2")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.challengeId").value("c"));
        verify(reauth).defi(any(), eq("SIGNATURE_PROCESSUS"));
        String corps = "{\"assertion\":\"valide\",\"sujet\":\"chef\",\"finalite\":\"SIGNATURE_PROCESSUS\"}";
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/interne/reauthentifications/verification", corps).with(service("pilotage-service", "INTERNAL_IAM_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valide").value(true));
        verify(reauth).verifier("valide", "chef", "SIGNATURE_PROCESSUS");
        mvc.perform(req(HttpMethod.POST, "/api/v1/interne/reauthentifications/verification", corps).with(service("pilotage-service"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IAM-04 : annuaire des collaborateurs réservé au jeton de service ; actifs seuls, sans coordonnées")
    void collaborateursInternes() throws Exception {
        mvc.perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs/" + ID, null).with(jeton("2", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs/" + ID, null)).andExpect(status().isUnauthorized());
        var service = jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs/" + ID, null).with(service))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profils[0]").value("ROLE_CHARGE_DDRM"))
            .andExpect(jsonPath("$.email").doesNotExist());
        Compte inactif = new Compte("x", "inactif", "I", "N", "i@gimac.cm", true, false, Instant.EPOCH, null, null, "fr", null, true, Set.of("ROLE_CONDUCTEUR_PROJET"), List.of(), true);
        when(comptes.lister(any(), anyInt(), anyInt())).thenReturn(new AdministrationComptesService.PageComptes(List.of(compte, inactif), 2));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs?profil=ROLE_CONDUCTEUR_PROJET", null).with(service))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ID));
    }

    @Test
    @DisplayName("D-94 §3 : porteurs d'une permission, liste réservée au jeton de service INTERNAL_IAM_READ, sans coordonnées")
    void collaborateursParPermission() throws Exception {
        String uri = "/api/v1/interne/collaborateurs?permission=HOM_PROJECT_CONDUCT";
        mvc.perform(req(HttpMethod.GET, uri, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, uri, null).with(jeton("2", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
        when(comptes.porteursPermission("HOM_PROJECT_CONDUCT")).thenReturn(List.of(compte));
        mvc
            .perform(req(HttpMethod.GET, uri, null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ID))
            .andExpect(jsonPath("$[0].nomAffiche").value("Awa Mballa"))
            .andExpect(jsonPath("$[0].profils[0]").value("ROLE_CHARGE_DDRM"))
            .andExpect(jsonPath("$[0].actif").value(true))
            .andExpect(jsonPath("$[0].email").doesNotExist());
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs?permission=INCONNUE", null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("D-93 §11 : comptes d'une institution, liste réservée au jeton de service INTERNAL_IAM_READ, sans coordonnées")
    void collaborateursParInstitution() throws Exception {
        String uri = "/api/v1/interne/collaborateurs?institution=INST-0042";
        mvc.perform(req(HttpMethod.GET, uri, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, uri, null).with(jeton("2", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
        Compte correspondant = new Compte("c-1", "corr", "Carine", "Ngo", "c@gimac.cm", true, true, Instant.EPOCH, null, null, "fr", "INST-0042", true, Set.of("ROLE_CORRESPONDANT"), List.of(), true);
        when(comptes.comptesInstitution("INST-0042")).thenReturn(List.of(correspondant));
        mvc
            .perform(req(HttpMethod.GET, uri, null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("c-1"))
            .andExpect(jsonPath("$[0].institutionId").value("INST-0042"))
            .andExpect(jsonPath("$[0].actif").value(true))
            .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    @DisplayName("D-64 : annuaire interne paginé réservé au jeton de service INTERNAL_IAM_READ, sans coordonnées")
    void annuaireInterne() throws Exception {
        String uri = "/api/v1/interne/collaborateurs?recherche=mballa&page=1&taille=10";
        mvc.perform(req(HttpMethod.GET, uri, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, uri, null).with(jeton("2", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, uri, null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_READ")))).andExpect(status().isForbidden());
        when(comptes.annuaireInterne("mballa", 1, 10)).thenReturn(new AdministrationComptesService.PageAnnuaire(List.of(compte), 11, 1, 10));
        mvc
            .perform(req(HttpMethod.GET, uri, null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu[0].id").value(ID))
            .andExpect(jsonPath("$.contenu[0].nomAffiche").value("Awa Mballa"))
            .andExpect(jsonPath("$.contenu[0].email").doesNotExist())
            .andExpect(jsonPath("$.total").value(11))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.taille").value(10));
        when(comptes.annuaireInterne(null, 0, 20)).thenReturn(new AdministrationComptesService.PageAnnuaire(List.of(), 0, 0, 20));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs", null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"))))
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.taille").value(20));
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
            .andExpect(jsonPath("$.contenu[0].actionsRequises[0]").value("MOT_DE_PASSE"));
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
        mvc.perform(req(HttpMethod.GET, "/api/v1/comptes/" + ID, null).with(jeton("2", "IAM_ACCOUNT_READ"))).andExpect(jsonPath("$.actionsRequises[0]").value("MOT_DE_PASSE"));
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

    // ------------------------------------------------------------------ « Mon compte » : photo, mot de passe, alerte (mon-compte.md)

    static org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder<?> envoiAvatar(byte[] octets) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/v1/moi/avatar").file(
            new org.springframework.mock.web.MockMultipartFile("image", "photo.png", "image/png", octets)
        );
    }

    @Test
    @DisplayName("mon-compte.md §1 : routes de la photo, 401 sans jeton, 403 sans second facteur (acr 1 compris), accès en acr 2 et 3")
    void avatarMatrice() throws Exception {
        when(avatars.remplacer(any(), any())).thenReturn("0123456789abcdef");
        when(avatars.lire(anyString())).thenReturn(Optional.of(new cm.gimac.des.iam.domain.service.TraitementAvatar.ImageAvatar(PNG, "image/png", "0123456789abcdef")));
        List<org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder<?>> routes = List.of(
            envoiAvatar(PNG),
            request(HttpMethod.DELETE, "/api/v1/moi/avatar"),
            request(HttpMethod.GET, "/api/v1/moi/avatar"),
            request(HttpMethod.GET, "/api/v1/avatars/" + ID + "?v=0123456789abcdef")
        );
        List<Integer> attendus = List.of(200, 204, 200, 200);
        for (int i = 0; i < routes.size(); i++) {
            mvc.perform(routes.get(i)).andExpect(status().isUnauthorized());
            mvc.perform(routes.get(i).with(jeton(null))).andExpect(status().isForbidden());
            mvc.perform(routes.get(i).with(jeton("1"))).andExpect(status().isForbidden());
            mvc.perform(routes.get(i).with(jeton("2"))).andExpect(status().is(attendus.get(i)));
            mvc.perform(routes.get(i).with(jeton("3"))).andExpect(status().is(attendus.get(i)));
        }
    }

    @Test
    @DisplayName("mon-compte.md §1 : formes (version, image en cache privé immuable, 404 sans photo, portail limité à sa propre photo, refus)")
    void avatarFormes() throws Exception {
        when(avatars.remplacer(any(), any())).thenReturn("0123456789abcdef");
        mvc.perform(envoiAvatar(PNG).with(jeton("2"))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value("0123456789abcdef"));
        verify(avatars).remplacer(org.mockito.ArgumentMatchers.argThat(a -> a.id().equals("demo-super-admin")), eq(PNG));
        mvc
            .perform(envoiAvatar(new byte[0]).with(jeton("2")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_AVATAR_UNREADABLE"));
        mvc
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/v1/moi/avatar").with(jeton("2")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_AVATAR_UNREADABLE"));
        when(avatars.remplacer(any(), eq(new byte[] { 1, 2, 3 }))).thenThrow(new org.springframework.web.multipart.MaxUploadSizeExceededException(3L));
        mvc
            .perform(envoiAvatar(new byte[] { 1, 2, 3 }).with(jeton("2")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_AVATAR_TOO_LARGE"));
        when(avatars.remplacer(any(), eq(new byte[] { 4 }))).thenThrow(IamException.invalide("IAM_AVATAR_TYPE", "Format refusé", Map.of()));
        mvc.perform(envoiAvatar(new byte[] { 4 }).with(jeton("2"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IAM_AVATAR_TYPE"));

        var image = new cm.gimac.des.iam.domain.service.TraitementAvatar.ImageAvatar(PNG, "image/png", "0123456789abcdef");
        when(avatars.lire(ID)).thenReturn(Optional.of(image));
        when(avatars.lire("demo-super-admin")).thenReturn(Optional.of(image));
        when(avatars.lire("sans-photo")).thenReturn(Optional.empty());
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/avatars/" + ID).with(jeton("2")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("max-age=86400"), org.hamcrest.Matchers.containsString("private"), org.hamcrest.Matchers.containsString("immutable"))))
            .andExpect(header().string("ETag", "\"0123456789abcdef\""))
            .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(PNG));
        mvc
            .perform(request(HttpMethod.GET, "/api/v1/avatars/sans-photo").with(jeton("2")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("IAM_AVATAR_NOT_FOUND"));
        // Correspondant d'un participant (portail) : sa propre photo seulement.
        var correspondant = jwt().jwt(j -> j.subject("demo-super-admin").claim("acr", "2").claim("institution_id", "INST-0042"));
        mvc.perform(request(HttpMethod.GET, "/api/v1/avatars/" + ID).with(correspondant)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("IAM_AVATAR_FORBIDDEN"));
        mvc.perform(request(HttpMethod.GET, "/api/v1/avatars/demo-super-admin").with(correspondant)).andExpect(status().isOk());
        mvc.perform(request(HttpMethod.GET, "/api/v1/moi/avatar").with(correspondant)).andExpect(status().isOk());
        var institutionVide = jwt().jwt(j -> j.subject("demo-super-admin").claim("acr", "2").claim("institution_id", " "));
        mvc.perform(request(HttpMethod.GET, "/api/v1/avatars/" + ID).with(institutionVide)).andExpect(status().isOk());
        assertThat(AvatarResource.correspondant(null)).isFalse();
    }

    @Test
    @DisplayName("mon-compte.md §1 : avatarVersion dans GET /moi, la liste et la fiche des comptes, l'annuaire interne")
    void avatarVersions() throws Exception {
        var mc = new MonCompteService.MonCompte(compte, 1, 2, List.of(), "0123456789abcdef");
        when(moi.moi(any())).thenReturn(mc);
        when(avatars.version(ID)).thenReturn(Optional.of("0123456789abcdef"));
        when(avatars.versions(any())).thenReturn(Map.of(ID, "0123456789abcdef"));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/moi", null).with(jeton("2")))
            .andExpect(jsonPath("$.avatarVersion").value("0123456789abcdef"))
            .andExpect(jsonPath("$.compte.avatarVersion").value("0123456789abcdef"));
        mvc.perform(req(HttpMethod.GET, "/api/v1/comptes", null).with(jeton("2", "IAM_ACCOUNT_READ"))).andExpect(jsonPath("$.contenu[0].avatarVersion").value("0123456789abcdef"));
        mvc.perform(req(HttpMethod.GET, "/api/v1/comptes/" + ID, null).with(jeton("2", "IAM_ACCOUNT_READ"))).andExpect(jsonPath("$.avatarVersion").value("0123456789abcdef"));
        var service = jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
        mvc.perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs/" + ID, null).with(service)).andExpect(jsonPath("$.avatarVersion").value("0123456789abcdef"));
        when(comptes.annuaireInterne(null, 0, 20)).thenReturn(new AdministrationComptesService.PageAnnuaire(List.of(compte), 1, 0, 20));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs", null).with(service))
            .andExpect(jsonPath("$.contenu[0].avatarVersion").value("0123456789abcdef"));
        when(avatars.versions(any())).thenReturn(Map.of());
        mvc.perform(req(HttpMethod.GET, "/api/v1/interne/collaborateurs", null).with(service)).andExpect(jsonPath("$.contenu[0].avatarVersion").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("mon-compte.md §2 : fermerAutresSessions transmis avec la session courante (sid) ; en-tête pour la passerelle")
    void motDePasseFermerAutresSessions() throws Exception {
        String corps = MOT_DE_PASSE.replace("}", ",\"fermerAutresSessions\":true}");
        var avecSid = jwt().jwt(j -> j.subject("demo-super-admin").claim("acr", "2").claim("sid", "kc-session-1"));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/moi/mot-de-passe", corps).with(avecSid))
            .andExpect(status().isNoContent())
            .andExpect(header().string(MonCompteResource.EN_TETE_FERMER_AUTRES_SESSIONS, "1"));
        verify(moi).changerMotDePasse(any(), eq("Ancien-Mot-2026!"), eq("Nouveau-Mot-2026!"), eq(true), eq("kc-session-1"));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/moi/mot-de-passe", MOT_DE_PASSE).with(avecSid))
            .andExpect(status().isNoContent())
            .andExpect(header().doesNotExist(MonCompteResource.EN_TETE_FERMER_AUTRES_SESSIONS));
        assertThat(new MonCompteResource.MotDePasseRequest("a1", "b2", true).toString()).doesNotContain("a1").doesNotContain("b2").contains("fermerAutresSessions=true");
    }

    @Test
    @DisplayName("mon-compte.md §3 : alerte « nouvel appareil » transmise au service avec la connexion décrite")
    void alerteConnexion() throws Exception {
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/interne/comptes/" + ID + "/alerte-connexion", ALERTE).with(service("gateway-service", "INTERNAL_IAM_READ")))
            .andExpect(status().isNoContent());
        verify(connexion, org.mockito.Mockito.atLeastOnce()).alerterNouvelAppareil(
            eq(ID),
            eq(new ConnexionService.NouvelleConnexion("Firefox 131", "Windows 10", "ORDINATEUR", "41.202.10.20", "des", Instant.parse("2026-10-01T08:30:00Z")))
        );
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/interne/comptes/" + ID + "/alerte-connexion", "{\"navigateur\":\"" + "x".repeat(101) + "\"}").with(service("gateway-service", "INTERNAL_IAM_READ")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("mon-compte.md §1 : versions des photos par lot, jeton de service INTERNAL_IAM_READ seulement, 200 identifiants au plus")
    void versionsAvatarsInternes() throws Exception {
        String uri = "/api/v1/interne/collaborateurs/avatars?ids=u1&ids=u2&ids=u1&ids=";
        mvc.perform(req(HttpMethod.GET, uri, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, uri, null).with(jeton("2", PERMISSIONS_IAM.toArray(String[]::new)))).andExpect(status().isForbidden());
        when(avatars.versions(List.of("u1", "u2"))).thenReturn(Map.of("u1", "0123456789abcdef"));
        mvc
            .perform(req(HttpMethod.GET, uri, null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.u1").value("0123456789abcdef"))
            .andExpect(jsonPath("$.u2").doesNotExist());
        StringBuilder beaucoup = new StringBuilder("/api/v1/interne/collaborateurs/avatars?ids=x0");
        for (int i = 1; i < 250; i++) {
            beaucoup.append("&ids=x").append(i);
        }
        mvc.perform(req(HttpMethod.GET, beaucoup.toString(), null).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ")))).andExpect(status().isOk());
        verify(avatars).versions(org.mockito.ArgumentMatchers.argThat(l -> l.size() == 200));
    }
}
