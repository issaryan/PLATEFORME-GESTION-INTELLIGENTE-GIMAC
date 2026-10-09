package cm.gimac.des.iam.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.application.usecase.AffectationsService;
import cm.gimac.des.iam.application.usecase.OrganigrammePdfService;
import cm.gimac.des.iam.application.usecase.OrganisationService;
import cm.gimac.des.iam.config.SecurityConfiguration;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.web.rest.errors.IamExceptionHandler;
import java.util.List;
import java.util.Map;
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
 * Routes de l'organisation (D-76, docs/lot1/mon-activite.md §1.3) avec la vraie chaîne de filtres :
 * permissions exactes, jeton à deux facteurs, routes internes réservées au jeton de service
 * INTERNAL_IAM_READ, formes JSON du contrat et erreurs problem+json.
 */
@WebMvcTest(controllers = { OrganisationResource.class, OrganisationInterneResource.class })
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, IamExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class OrganisationRoutesTest {

    static final String ID = "3f1c2d4e-0000-4000-8000-000000000001";
    static final String CHEF = "3f1c2d4e-0000-4000-8000-000000000002";
    static final Unite DG = new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true);
    static final Unite DES = new Unite("DES", "Département Exploitation et Supervision", TypeUnite.DEPARTEMENT, "DG", true);
    static final Unite RESEAU = new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", true);
    static final String UNITE = "{\"code\":\"POLE_RESEAU\",\"libelle\":\"Pôle réseau\",\"type\":\"POLE\",\"parent\":\"PROD_INFO\"}";
    static final String MODIFICATION = "{\"libelle\":\"Pôle réseau\",\"type\":\"POLE\",\"parent\":\"PROD_INFO\",\"active\":false}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrganisationService service;

    @MockitoBean
    AffectationsService affectations;

    @MockitoBean
    OrganigrammePdfService organigramme;

    @BeforeEach
    void init() {
        when(service.unites(anyBoolean())).thenReturn(List.of(DG, DES, RESEAU));
        when(service.creerUnite(any(), any(), any())).thenReturn(RESEAU);
        when(service.modifierUnite(any(), eq("POLE_RESEAU"), any(), anyBoolean(), any())).thenReturn(RESEAU);
        var rattachement = new OrganisationService.Rattachement(
            ID,
            "Awa Mballa",
            RESEAU,
            List.of(DG, DES, RESEAU),
            new OrganisationService.Responsable(CHEF, "Chef Mballa", "Chef du pôle"),
            List.of(CHEF, "dg"),
            3,
            List.of("POLE_RESEAU")
        );
        when(service.perimetre(ID, Portee.UNITES)).thenReturn(
            new OrganisationService.Perimetre(
                Portee.UNITES,
                List.of(new OrganisationService.MembreEquipe("m1", "Bob Ndi", "Technicien", "POLE_RESEAU", ID, 1, true), new OrganisationService.MembreEquipe("m2", "Eva Ngo", "Technicienne", "POLE_RESEAU", "autre", 0, true)),
                List.of("POLE_RESEAU")
            )
        );
        when(service.rattachement(ID)).thenReturn(rattachement);
        when(service.equipe(ID)).thenReturn(List.of(new OrganisationService.MembreEquipe("m1", "Bob Ndi", "Technicien", "POLE_RESEAU", ID, 1, true)));
    }

    static RequestPostProcessor jeton(String sujet, String... permissions) {
        return jwt().jwt(j -> j.subject(sujet).claim("acr", "2").claim("name", "Agent")).authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static MockHttpServletRequestBuilder req(HttpMethod m, String uri, String body) {
        MockHttpServletRequestBuilder b = request(m, uri);
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return b;
    }

    @Test
    @DisplayName("Catalogue : lecture avec PIL_ACTIVITY_OWN ou IAM_ACCOUNT_READ, édition avec IAM_ACCOUNT_EDIT seulement")
    void permissionsCatalogue() throws Exception {
        String u = "/api/v1/organisation/unites";
        mvc.perform(req(HttpMethod.GET, u, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, u, null).with(jwt().authorities(new SimpleGrantedAuthority("PIL_ACTIVITY_OWN")))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "IAM_ACCOUNT_READ"))).andExpect(status().isOk());
        mvc
            .perform(req(HttpMethod.GET, u + "?actives=true", null).with(jeton(ID, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[2].code").value("POLE_RESEAU"))
            .andExpect(jsonPath("$[2].libelle").value("Pôle réseau"))
            .andExpect(jsonPath("$[2].type").value("POLE"))
            .andExpect(jsonPath("$[2].parent").value("PROD_INFO"))
            .andExpect(jsonPath("$[2].active").value(true))
            .andExpect(jsonPath("$[0].parent").isEmpty());
        verify(service).unites(true);

        for (String perm : List.of("IAM_ACCOUNT_READ", "PIL_ACTIVITY_OWN")) {
            mvc.perform(req(HttpMethod.POST, u, UNITE).with(jeton(ID, perm))).andExpect(status().isForbidden());
            mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", MODIFICATION).with(jeton(ID, perm))).andExpect(status().isForbidden());
        }
        mvc
            .perform(req(HttpMethod.POST, u, UNITE).with(jeton(ID, "IAM_ACCOUNT_EDIT")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/organisation/unites/POLE_RESEAU"))
            .andExpect(jsonPath("$.code").value("POLE_RESEAU"));
        verify(service).creerUnite(any(), eq(RESEAU), isNull());
        mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", MODIFICATION).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isOk());
        verify(service).modifierUnite(any(), eq("POLE_RESEAU"), eq(new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", false)), eq(true), isNull());
        // D-81 §2 : responsable fourni (valeur ou null) : appliqué ; absent : conservé.
        String avecResponsable = MODIFICATION.replace("}", ",\"responsableId\":\"" + CHEF + "\"}");
        mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", avecResponsable).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isOk());
        verify(service).modifierUnite(any(), eq("POLE_RESEAU"), eq(new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", false, CHEF)), eq(false), isNull());
        String sansResponsable = MODIFICATION.replace("}", ",\"responsableId\":null}");
        mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", sansResponsable).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isOk());
        verify(service).modifierUnite(any(), eq("POLE_RESEAU"), eq(new Unite("POLE_RESEAU", "Pôle réseau", TypeUnite.POLE, "PROD_INFO", false)), eq(false), isNull());
        mvc.perform(req(HttpMethod.POST, u, "{\"code\":\"X\",\"type\":\"POLE\"}").with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", "{\"libelle\":\"x\",\"type\":\"POLE\"}").with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("D-102 : motif des gestes d'unité transmis au service")
    void motifDesGestes() throws Exception {
        String u = "/api/v1/organisation/unites";
        mvc.perform(req(HttpMethod.POST, u, UNITE.replace("}", ",\"motif\":\"Nouvelle unité\"}")).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isCreated());
        verify(service).creerUnite(any(), eq(RESEAU), eq("Nouvelle unité"));
        mvc.perform(req(HttpMethod.PUT, u + "/POLE_RESEAU", MODIFICATION.replace("}", ",\"motif\":\"Déplacement\"}")).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isOk());
        verify(service).modifierUnite(any(), eq("POLE_RESEAU"), any(), eq(true), eq("Déplacement"));
    }

    @Test
    @DisplayName("D-102 : affectations (IAM_ACCOUNT_EDIT ou PIL_ACTIVITY_OWN), n+1 absent ou null distingués, retrait (IAM_ACCOUNT_EDIT), refus détaillé")
    void affectations() throws Exception {
        String u = "/api/v1/organisation/unites/POLE_RESEAU/affectations";
        var resultat = new AffectationsService.Resultat(ID, "Awa Mballa", new AffectationsService.Etat(null, null), new AffectationsService.Etat("POLE_RESEAU", CHEF));
        when(affectations.affecter(any(), eq("POLE_RESEAU"), any(), anyBoolean(), any(), any())).thenReturn(List.of(resultat));
        String corps = "{\"comptes\":[\"" + ID + "\"],\"motif\":\"Renfort\"}";
        mvc.perform(req(HttpMethod.POST, u, corps)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.POST, u, corps).with(jeton(ID, "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.POST, u, corps).with(jeton(ID, "IAM_ACCOUNT_EDIT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ID))
            .andExpect(jsonPath("$[0].avant.unite").isEmpty())
            .andExpect(jsonPath("$[0].apres.unite").value("POLE_RESEAU"))
            .andExpect(jsonPath("$[0].apres.responsableId").value(CHEF));
        verify(affectations).affecter(any(), eq("POLE_RESEAU"), eq(List.of(ID)), eq(false), isNull(), eq("Renfort"));
        mvc.perform(req(HttpMethod.POST, u, corps.replace("}", ",\"responsableId\":null}")).with(jeton(ID, "PIL_ACTIVITY_OWN"))).andExpect(status().isOk());
        verify(affectations).affecter(any(), eq("POLE_RESEAU"), eq(List.of(ID)), eq(true), isNull(), eq("Renfort"));
        mvc.perform(req(HttpMethod.POST, u, "{\"comptes\":[],\"motif\":\"x\"}").with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isBadRequest());
        when(affectations.affecter(any(), eq("DES"), any(), anyBoolean(), any(), any())).thenThrow(
            IamException.nonTraitable("ORG_AFFECTATION_REFUSEE", "Refus", Map.of("unite", "DES", "refus", List.of(Map.of("id", ID, "code", "ORG_COMPTE_INACTIF", "message", "Inactif"))))
        );
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/organisation/unites/DES/affectations", corps).with(jeton(ID, "IAM_ACCOUNT_EDIT")))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("ORG_AFFECTATION_REFUSEE"))
            .andExpect(jsonPath("$.details.refus[0].code").value("ORG_COMPTE_INACTIF"));

        when(affectations.retirer(any(), eq("POLE_RESEAU"), eq(ID), eq("Départ"))).thenReturn(resultat);
        mvc.perform(req(HttpMethod.DELETE, u + "/" + ID + "?motif=Départ", null).with(jeton(ID, "PIL_ACTIVITY_OWN"))).andExpect(status().isForbidden());
        mvc.perform(req(HttpMethod.DELETE, u + "/" + ID + "?motif=Départ", null).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    @DisplayName("D-102 : organigramme.pdf, mêmes lecteurs que le catalogue, pièce jointe datée ; moteur indisponible : 503 ORG_PDF_INDISPONIBLE")
    void organigrammePdf() throws Exception {
        String u = "/api/v1/organisation/organigramme.pdf";
        byte[] pdf = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(organigramme.produire(any())).thenReturn(new OrganigrammePdfService.Fichier("organigramme-moabi-2026-10-09.pdf", pdf));
        mvc.perform(req(HttpMethod.GET, u, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "IAM_ACCOUNT_EDIT"))).andExpect(status().isForbidden());
        for (String perm : List.of("IAM_ACCOUNT_READ", "PIL_ACTIVITY_OWN")) {
            mvc
                .perform(req(HttpMethod.GET, u + "?racine=DES&format=A4&orientation=portrait&membres=true", null).with(jeton(ID, perm)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"organigramme-moabi-2026-10-09.pdf\""))
                .andExpect(content().bytes(pdf));
        }
        verify(organigramme, org.mockito.Mockito.times(2)).produire(eq(new OrganigrammePdfService.Demande("DES", RenduPdf.Format.A4, false, true)));
        mvc.perform(req(HttpMethod.GET, u + "?format=A5", null).with(jeton(ID, "IAM_ACCOUNT_READ"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ORG_PDF_PARAMETRE"));
        when(organigramme.produire(eq(new OrganigrammePdfService.Demande(null, RenduPdf.Format.A3, true, false)))).thenThrow(IamException.indisponible("ORG_PDF_INDISPONIBLE", "Moteur indisponible"));
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "IAM_ACCOUNT_READ"))).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ORG_PDF_INDISPONIBLE"));
    }

    @Test
    @DisplayName("Erreurs du contrat en problem+json : ORG_CYCLE 409, ORG_UNITE_INCONNUE 422")
    void erreurs() throws Exception {
        when(service.modifierUnite(any(), eq("DES"), any(), anyBoolean(), any())).thenThrow(IamException.conflit("ORG_CYCLE", "Cycle", Map.of()));
        when(service.creerUnite(any(), eq(new Unite("POLE_X", "x", TypeUnite.POLE, "ANCIEN", true)), any())).thenThrow(IamException.nonTraitable("ORG_UNITE_INCONNUE", "Inconnue", Map.of()));
        mvc
            .perform(req(HttpMethod.PUT, "/api/v1/organisation/unites/DES", MODIFICATION).with(jeton(ID, "IAM_ACCOUNT_EDIT")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORG_CYCLE"));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/organisation/unites", "{\"code\":\"POLE_X\",\"libelle\":\"x\",\"type\":\"POLE\",\"parent\":\"ANCIEN\",\"active\":true}").with(jeton(ID, "IAM_ACCOUNT_EDIT")))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("ORG_UNITE_INCONNUE"));
    }

    @Test
    @DisplayName("GET organisation/moi : PIL_ACTIVITY_OWN, rattachement du sujet du jeton, forme du contrat")
    void moi() throws Exception {
        String u = "/api/v1/organisation/moi";
        mvc.perform(req(HttpMethod.GET, u, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(req(HttpMethod.GET, u, null).with(jeton(ID, "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ID))
            .andExpect(jsonPath("$.nomAffiche").value("Awa Mballa"))
            .andExpect(jsonPath("$.unite.code").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.chemin[0].code").value("DG"))
            .andExpect(jsonPath("$.chemin[2].code").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.responsable.id").value(CHEF))
            .andExpect(jsonPath("$.responsable.nomAffiche").value("Chef Mballa"))
            .andExpect(jsonPath("$.responsable.fonction").value("Chef du pôle"))
            .andExpect(jsonPath("$.chaine[0]").value(CHEF))
            .andExpect(jsonPath("$.equipeDirecte").value(3))
            .andExpect(jsonPath("$.unitesDirigees[0]").value("POLE_RESEAU"));
    }

    @Test
    @DisplayName("Routes internes : jeton de service INTERNAL_IAM_READ seulement ; rattachement et équipe avec niveau")
    void interne() throws Exception {
        String u = "/api/v1/interne/organisation/" + ID;
        for (String uri : List.of(u, u + "/equipe", u + "/perimetre?portee=UNITES")) {
            mvc.perform(req(HttpMethod.GET, uri, null)).andExpect(status().isUnauthorized());
            mvc.perform(req(HttpMethod.GET, uri, null).with(jeton(ID, "PIL_ACTIVITY_OWN", "IAM_ACCOUNT_READ", "PIL_TEAM_READ"))).andExpect(status().isForbidden());
        }
        var service = jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
        mvc.perform(req(HttpMethod.GET, u, null).with(service)).andExpect(status().isOk()).andExpect(jsonPath("$.chaine.length()").value(2));
        mvc
            .perform(req(HttpMethod.GET, u + "/equipe", null).with(service))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("m1"))
            .andExpect(jsonPath("$[0].nomAffiche").value("Bob Ndi"))
            .andExpect(jsonPath("$[0].fonction").value("Technicien"))
            .andExpect(jsonPath("$[0].unite").value("POLE_RESEAU"))
            .andExpect(jsonPath("$[0].responsableId").value(ID))
            .andExpect(jsonPath("$[0].niveau").value(1))
            .andExpect(jsonPath("$[0].actif").value(true));
        // D-81 §2 : périmètre pour une portée.
        mvc
            .perform(req(HttpMethod.GET, u + "/perimetre?portee=UNITES", null).with(service))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portee").value("UNITES"))
            .andExpect(jsonPath("$.comptes.length()").value(2))
            .andExpect(jsonPath("$.comptes[1].id").value("m2"))
            .andExpect(jsonPath("$.comptes[1].niveau").value(0))
            .andExpect(jsonPath("$.comptes[1].nomAffiche").value("Eva Ngo"))
            .andExpect(jsonPath("$.comptes[1].fonction").value("Technicienne"))
            .andExpect(jsonPath("$.comptes[1].unite").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.comptes[1].responsableId").value("autre"))
            .andExpect(jsonPath("$.comptes[1].actif").value(true))
            .andExpect(jsonPath("$.unites[0]").value("POLE_RESEAU"));
        mvc.perform(req(HttpMethod.GET, u + "/perimetre?portee=TOUT", null).with(service)).andExpect(status().isBadRequest());
        // D-92 : portée TOUTES, toutes les unités (actives ou non) et les comptes internes actifs.
        when(this.service.perimetre(ID, Portee.TOUTES)).thenReturn(
            new OrganisationService.Perimetre(
                Portee.TOUTES,
                List.of(new OrganisationService.MembreEquipe("m3", "Ali Oumarou", "Analyste", "SNA", null, 0, true)),
                List.of("DES", "DG", "POLE_RESEAU", "SNA")
            )
        );
        mvc
            .perform(req(HttpMethod.GET, u + "/perimetre?portee=TOUTES", null).with(service))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portee").value("TOUTES"))
            .andExpect(jsonPath("$.comptes[0].id").value("m3"))
            .andExpect(jsonPath("$.comptes[0].niveau").value(0))
            .andExpect(jsonPath("$.unites.length()").value(4));
    }

    @Test
    @DisplayName("D-86 : membres actifs d'une unité pour le moteur de processus ; jeton de service ; profil et sous-unités transmis ; unité inconnue 404")
    void membresUnite() throws Exception {
        String u = "/api/v1/interne/organisation/unites/POLE_RESEAU/membres";
        var membre = new OrganisationService.MembreUnite("m1", "Bob Ndi", "Technicien", "POLE_RESEAU", CHEF, true, java.util.Set.of("ROLE_POLE_RESEAU"));
        when(service.membresUnite("POLE_RESEAU", "ROLE_POLE_RESEAU", true)).thenReturn(List.of(membre));
        when(service.membresUnite("POLE_RESEAU", null, false)).thenReturn(List.of());
        when(service.membresUnite(eq("INCONNUE"), any(), anyBoolean())).thenThrow(cm.gimac.des.iam.domain.IamException.introuvable("ORG_UNITE_INTROUVABLE", "INCONNUE"));
        mvc.perform(req(HttpMethod.GET, u, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "PIL_ACTIVITY_OWN", "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        var jetonService = jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
        mvc
            .perform(req(HttpMethod.GET, u + "?profil=ROLE_POLE_RESEAU&sousUnites=true", null).with(jetonService))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("m1"))
            .andExpect(jsonPath("$[0].nomAffiche").value("Bob Ndi"))
            .andExpect(jsonPath("$[0].fonction").value("Technicien"))
            .andExpect(jsonPath("$[0].unite").value("POLE_RESEAU"))
            .andExpect(jsonPath("$[0].responsableId").value(CHEF))
            .andExpect(jsonPath("$[0].actif").value(true))
            .andExpect(jsonPath("$[0].profils[0]").value("ROLE_POLE_RESEAU"));
        // Sans profil : tous les membres ; sous-unités comprises par défaut.
        mvc.perform(req(HttpMethod.GET, u + "?profil=ROLE_POLE_RESEAU", null).with(jetonService)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(req(HttpMethod.GET, u + "?sousUnites=false", null).with(jetonService)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        verify(service).membresUnite("POLE_RESEAU", null, false);
        mvc.perform(req(HttpMethod.GET, "/api/v1/interne/organisation/unites/INCONNUE/membres", null).with(jetonService)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D-87 : une unité par son code (responsable compris) ; jeton de service INTERNAL_IAM_READ seulement ; unité inconnue 404")
    void uniteParCode() throws Exception {
        String u = "/api/v1/interne/organisation/unites/POLE_RESEAU";
        when(service.unite("POLE_RESEAU")).thenReturn(RESEAU.avecResponsable(CHEF));
        when(service.unite("INCONNUE")).thenThrow(cm.gimac.des.iam.domain.IamException.introuvable("ORG_UNITE_INTROUVABLE", "INCONNUE"));
        mvc.perform(req(HttpMethod.GET, u, null)).andExpect(status().isUnauthorized());
        mvc.perform(req(HttpMethod.GET, u, null).with(jeton(ID, "PIL_ACTIVITY_OWN", "IAM_ACCOUNT_READ"))).andExpect(status().isForbidden());
        var jetonService = jwt().jwt(j -> j.subject("service-account-pilotage-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
        mvc
            .perform(req(HttpMethod.GET, u, null).with(jetonService))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("POLE_RESEAU"))
            .andExpect(jsonPath("$.libelle").value("Pôle réseau"))
            .andExpect(jsonPath("$.type").value("POLE"))
            .andExpect(jsonPath("$.parent").value("PROD_INFO"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.responsableId").value(CHEF));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/interne/organisation/unites/INCONNUE", null).with(jetonService))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORG_UNITE_INTROUVABLE"));
    }
}
