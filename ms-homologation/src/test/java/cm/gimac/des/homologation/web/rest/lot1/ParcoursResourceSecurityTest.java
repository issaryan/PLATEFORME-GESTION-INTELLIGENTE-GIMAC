package cm.gimac.des.homologation.web.rest.lot1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.application.usecase.LiaisonQueries;
import cm.gimac.des.homologation.application.usecase.ParcoursCommandes;
import cm.gimac.des.homologation.application.usecase.ParcoursLectures;
import cm.gimac.des.homologation.application.usecase.PiecesParcours;
import cm.gimac.des.homologation.config.SecurityConfiguration;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.web.rest.errors.HomologationExceptionHandler;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/** Sécurité des routes du parcours, étapes 4 à 10 (D-61), avec la vraie chaîne de filtres. */
@WebMvcTest(controllers = ParcoursResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, HomologationExceptionHandler.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class ParcoursResourceSecurityTest {

    static final UUID ID = UUID.randomUUID();
    static final String SHA = "a".repeat(64);
    static final String PIECE = "{\"documentId\":\"" + ID + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"cr.pdf\",\"commentaire\":\"relu\"}";
    static final String PREUVE = "{\"documentId\":\"" + ID + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"c.png\",\"codeControle\":\"A1B2C3\"}";
    static final String BLOCAGE = "{\"motif\":\"Pièce manquante\",\"prochaineAction\":\"Relancer\",\"responsableId\":\"" + ID + "\",\"echeanceLevee\":\"2026-10-10\"}";
    static final List<String> TOUTES = List.of(
        "HOM_LIAISON_LIST",
        "HOM_LIAISON_READ",
        "HOM_LIAISON_OPEN",
        "HOM_PROJECT_DESIGNATE",
        "HOM_DASHBOARD_READ",
        "HOM_OPERATION_ASSIGN",
        "HOM_EVIDENCE_SUBMIT",
        "HOM_OPERATION_VALIDATE",
        "HOM_STEP_MANAGE",
        "CER_DOSSIER_VERIFY",
        "HOM_PORTAL_READ",
        "HOM_PORTAL_SUBMIT"
    );

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ParcoursLectures lectures;

    @MockitoBean
    ParcoursCommandes commandes;

    @MockitoBean
    LiaisonQueries liaisons;

    @MockitoBean
    ParcoursProjection projection;

    @MockitoBean
    PiecesParcours pieces;

    Parcours p;

    @BeforeEach
    void init() {
        ParcoursProjection vraie = new ParcoursProjection();
        p = ParcoursSamples.parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        var ctx = new ParcoursProjection.Contexte(new Personne("c", "Conducteur", Fonction.CONDUCTEUR), null, 2);
        var a = new cm.gimac.des.homologation.domain.parcours.Acteur(null, null, Set.of(), null);
        when(lectures.dossier(eq(ID), any())).thenReturn(vraie.dossier(p, ctx, a, LocalDate.now()));
        when(lectures.operations(eq(ID), any(), any(), any())).thenReturn(p.operations().stream().map(o -> vraie.operation(p, o, a, LocalDate.now())).toList());
        when(lectures.fiche(eq(ID), any())).thenReturn(vraie.fiche(p, a, false));
        var b = p.declarerBlocage("Pièce manquante", "Relancer", new Personne("u", "n", Fonction.DDRM), LocalDate.now(), null, LocalDate.now());
        when(commandes.declarerBlocage(eq(ID), any(), any(), any(), any(), any())).thenReturn(b);
        when(projection.blocage(any(), any())).thenReturn(vraie.blocage(b, new cm.gimac.des.homologation.domain.parcours.Acteur(null, null, Set.of("HOM_STEP_MANAGE"), null)));
        when(commandes.ouvrirServiceSupplementaire(eq(ID), any(), any())).thenReturn(p.liaison());
        when(pieces.piece(eq(ID), eq("DOC-1"))).thenReturn(new PiecesParcours.PieceServie("capture".getBytes(), "ping é.png", "image/png", SHA));
        when(pieces.chronologie(ID)).thenReturn(List.of());
        when(liaisons.fiche(eq(p.id()), anySet())).thenReturn(new LiaisonQueries.Fiche(p.liaison(), "Conducteur", null, List.of("RETRAIT"), List.of(), List.of(), vraie.resume(p, "Conducteur", LocalDate.now())));
    }

    static Stream<Arguments> routes() {
        String l = "/api/v1/liaisons/" + ID;
        String o = "/api/v1/parcours/operations/" + ID;
        return Stream.of(
            Arguments.of(HttpMethod.GET, l + "/dossier", null, List.of("HOM_LIAISON_READ"), 200),
            Arguments.of(HttpMethod.GET, l + "/parcours/operations?etape=CADRAGE&fonction=DDRM", null, List.of("HOM_LIAISON_READ"), 200),
            Arguments.of(HttpMethod.PUT, l + "/ressources", "{\"affectations\":[{\"fonction\":\"SNA\",\"personneId\":\"u\"}]}", List.of("HOM_STEP_MANAGE"), 204),
            Arguments.of(HttpMethod.POST, o + "/livrable", PIECE, List.of("HOM_STEP_MANAGE", "HOM_EVIDENCE_SUBMIT"), 204),
            Arguments.of(HttpMethod.POST, o + "/preuves", PREUVE, List.of("HOM_STEP_MANAGE", "HOM_EVIDENCE_SUBMIT"), 204),
            Arguments.of(HttpMethod.POST, o + "/validation", "{}", List.of("HOM_OPERATION_VALIDATE"), 204),
            // D-93 §2 : renvoi au responsable avec motif ; §1 : affectation d'une fonction à tout moment.
            Arguments.of(HttpMethod.POST, o + "/renvoi", "{\"motif\":\"Pièce illisible\"}", List.of("HOM_OPERATION_VALIDATE"), 204),
            Arguments.of(HttpMethod.PUT, l + "/fonctions/SNA", "{\"personneId\":\"u\",\"motif\":\"m\"}", List.of("HOM_STEP_MANAGE", "HOM_OPERATION_ASSIGN"), 204),
            Arguments.of(HttpMethod.PUT, o + "/intervenants", "{\"qualite\":\"V\",\"personneId\":\"u\",\"motif\":\"m\"}", List.of("HOM_OPERATION_ASSIGN", "HOM_STEP_MANAGE"), 204),
            Arguments.of(HttpMethod.POST, l + "/blocages", BLOCAGE, List.of("HOM_STEP_MANAGE"), 201),
            Arguments.of(HttpMethod.POST, "/api/v1/blocages/" + ID + "/levee", "{\"commentaire\":\"levé\"}", List.of("HOM_STEP_MANAGE"), 204),
            Arguments.of(HttpMethod.POST, l + "/couverture/constat", "{}", List.of("CER_DOSSIER_VERIFY"), 204),
            Arguments.of(HttpMethod.POST, l + "/services-supplementaires", "{\"services\":[\"CASH_IN\"]}", List.of("HOM_LIAISON_OPEN", "HOM_STEP_MANAGE"), 201),
            Arguments.of(HttpMethod.GET, l + "/fiche-suivi", null, List.of("HOM_LIAISON_READ"), 200),
            Arguments.of(HttpMethod.POST, l + "/fiche-suivi/remise", "{}", List.of("HOM_STEP_MANAGE", "CER_DOSSIER_VERIFY"), 204),
            Arguments.of(HttpMethod.POST, l + "/fiche-suivi/constat", "{}", List.of("CER_DOSSIER_VERIFY"), 204),
            // D-109 : chronologie, pièce téléchargée (empreinte revérifiée), livrable sans objet motivé.
            Arguments.of(HttpMethod.GET, l + "/chronologie", null, List.of("HOM_LIAISON_READ"), 200),
            Arguments.of(HttpMethod.GET, o + "/pieces/DOC-1", null, List.of("HOM_LIAISON_READ"), 200),
            Arguments.of(HttpMethod.POST, o + "/livrables/L1/sans-objet", "{\"motif\":\"Participant non porté\"}", List.of("HOM_STEP_MANAGE", "HOM_EVIDENCE_SUBMIT"), 204)
        );
    }

    static MockHttpServletRequestBuilder req(HttpMethod m, String uri, String body) {
        MockHttpServletRequestBuilder b = request(m, uri);
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(body);
        return b;
    }

    static RequestPostProcessor jeton(String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject(UUID.randomUUID().toString());
                if (acr != null) j.claim("acr", acr);
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("routes")
    @DisplayName("401 sans jeton, 403 sans second facteur, 403 sans la permission, accès avec chacune des permissions admises")
    void matrice(HttpMethod m, String uri, String body, List<String> admises, int ok) throws Exception {
        mvc.perform(req(m, uri, body)).andExpect(status().isUnauthorized());
        mvc.perform(req(m, uri, body).with(jeton(null, admises.get(0)))).andExpect(status().isForbidden());
        mvc.perform(req(m, uri, body).with(jeton("1", admises.get(0)))).andExpect(status().isForbidden());
        String[] autres = TOUTES.stream().filter(x -> !admises.contains(x)).toArray(String[]::new);
        mvc.perform(req(m, uri, body).with(jeton("2", autres))).andExpect(status().isForbidden());
        for (String admise : admises) {
            mvc.perform(req(m, uri, body).header(ParcoursResource.REAUTH_HEADER, "assertion").with(jeton("2", admise))).andExpect(status().is(ok));
        }
    }

    @Test
    @DisplayName("Formes : dossier et opérations au format de l'interface, Location du blocage et de la nouvelle liaison, assertion transmise")
    void formes() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/liaisons/" + ID + "/dossier", null).with(jeton("2", "HOM_LIAISON_READ")))
            .andExpect(jsonPath("$.reference").value("LIA-2026-001"))
            .andExpect(jsonPath("$.etapes.length()").value(11))
            .andExpect(jsonPath("$.etapes[2].elements.RESPONSABLE").value(true))
            .andExpect(jsonPath("$.couvertureCertification.calculable").value(true))
            .andExpect(jsonPath("$.dateDemarrage").value("2026-09-01"))
            .andExpect(jsonPath("$.coordonneesTechniques").isEmpty());
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/liaisons/" + ID + "/parcours/operations", null).with(jeton("2", "HOM_LIAISON_READ")))
            .andExpect(jsonPath("$[0].rang").value("3"))
            .andExpect(jsonPath("$[0].raci.R.fonction").value("DDRM"))
            .andExpect(jsonPath("$[0].raci.E[0].fonction").value("PARTICIPANT"))
            .andExpect(jsonPath("$[0].preuve").isEmpty());
        verify(lectures).operations(eq(ID), isNull(), isNull(), any());
        mvc.perform(req(HttpMethod.GET, "/api/v1/liaisons/" + ID + "/parcours/operations?etape=TESTS&fonction=SNA", null).with(jeton("2", "HOM_LIAISON_READ"))).andExpect(status().isOk());
        verify(lectures).operations(eq(ID), eq(EtapeCode.TESTS), eq(Fonction.SNA), any());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/liaisons/" + ID + "/blocages", BLOCAGE).with(jeton("2", "HOM_STEP_MANAGE")))
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/blocages/")))
            .andExpect(jsonPath("$.actionsPermises[0]").value("LEVER"));
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/liaisons/" + ID + "/services-supplementaires", "{\"services\":[\"CASH_IN\"]}").with(jeton("2", "HOM_LIAISON_OPEN")))
            .andExpect(header().string("Location", "/api/v1/liaisons/" + p.id()))
            .andExpect(jsonPath("$.statut").value("CADREE"))
            .andExpect(jsonPath("$.reference").value("LIA-2026-001"));
        mvc.perform(req(HttpMethod.POST, "/api/v1/parcours/operations/" + ID + "/validation", "{}").header("X-Reauth-Assertion", "YXNzZXJ0").with(jeton("2", "HOM_OPERATION_VALIDATE"))).andExpect(status().isNoContent());
        verify(commandes).valider(eq(ID), eq("YXNzZXJ0"), any());
        mvc.perform(req(HttpMethod.POST, "/api/v1/blocages/" + ID + "/levee", null).with(jeton("2", "HOM_STEP_MANAGE"))).andExpect(status().isNoContent());
        verify(commandes).leverBlocage(eq(ID), isNull(), any());
    }

    @Test
    @DisplayName("D-109 : pièce servie avec son nom, son type et son empreinte ; service d'un dépôt et motif d'un sans objet transmis")
    void preuvesD109() throws Exception {
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/parcours/operations/" + ID + "/pieces/DOC-1", null).with(jeton("2", "HOM_LIAISON_READ")))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Empreinte-Sha256", SHA))
            .andExpect(header().string("Repr-Digest", org.hamcrest.Matchers.startsWith("sha-256=:")))
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=UTF-8''ping%20%C3%A9.png")));
        when(pieces.piece(eq(ID), eq("DOC-2"))).thenReturn(new PiecesParcours.PieceServie("x".getBytes(), " ", "pas un type", SHA));
        mvc
            .perform(req(HttpMethod.GET, "/api/v1/parcours/operations/" + ID + "/pieces/DOC-2", null).with(jeton("2", "HOM_LIAISON_READ")))
            .andExpect(header().string("Content-Type", "application/octet-stream"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("piece")));
        when(pieces.piece(eq(ID), eq("DOC-3"))).thenThrow(HomologationException.notFound("HOM_PIECE_INCONNUE", "x", "DOC-3"));
        mvc.perform(req(HttpMethod.GET, "/api/v1/parcours/operations/" + ID + "/pieces/DOC-3", null).with(jeton("2", "HOM_LIAISON_READ"))).andExpect(status().isNotFound());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/parcours/operations/" + ID + "/preuves", "{\"documentId\":\"" + ID + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"r.csv\",\"livrable\":\"L1\",\"service\":\"RETRAIT\"}").with(jeton("2", "HOM_EVIDENCE_SUBMIT")))
            .andExpect(status().isNoContent());
        verify(commandes).deposerPreuve(eq(ID), eq("L1"), eq("RETRAIT"), any(), isNull(), isNull(), isNull(), any());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/parcours/operations/" + ID + "/livrable", "{\"documentId\":\"" + ID + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"r.csv\",\"livrable\":\"L1\",\"service\":\"RETRAIT\"}").with(jeton("2", "HOM_STEP_MANAGE")))
            .andExpect(status().isNoContent());
        verify(commandes).deposerLivrable(eq(ID), eq("L1"), eq("RETRAIT"), any(), any());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/parcours/operations/" + ID + "/livrables/L1/sans-objet", "{\"motif\":\"Service non ouvert\",\"service\":\"RETRAIT\"}").with(jeton("2", "HOM_STEP_MANAGE")))
            .andExpect(status().isNoContent());
        verify(commandes).declarerSansObjet(eq(ID), eq("L1"), eq("RETRAIT"), eq("Service non ouvert"), any());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/parcours/operations/" + ID + "/livrables/L1/sans-objet", "{\"motif\":\"x\"}").with(jeton("2", "HOM_STEP_MANAGE")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Preuve : toute propriété hors contrat (valeur de clé, secret) est refusée ; erreurs métier en ProblemDetail")
    void refus() throws Exception {
        String avecCle = "{\"documentId\":\"" + ID + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"c.png\",\"valeurCle\":\"0123456789ABCDEF\"}";
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/parcours/operations/" + ID + "/preuves", avecCle).with(jeton("2", "HOM_EVIDENCE_SUBMIT")))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_EVIDENCE_FIELD_REFUSED"));
        org.mockito.Mockito.doThrow(HomologationException.reauthRequired()).when(commandes).valider(eq(ID), isNull(), any());
        mvc
            .perform(req(HttpMethod.POST, "/api/v1/parcours/operations/" + ID + "/validation", "{}").with(jeton("2", "HOM_OPERATION_VALIDATE")))
            .andExpect(status().is(428))
            .andExpect(jsonPath("$.code").value("IAM_REAUTH_REQUIRED"));
        mvc.perform(req(HttpMethod.POST, "/api/v1/liaisons/" + ID + "/blocages", "{\"motif\":\" \"}").with(jeton("2", "HOM_STEP_MANAGE"))).andExpect(status().isBadRequest());
        mvc.perform(req(HttpMethod.GET, "/api/v1/liaisons/" + ID + "/parcours/operations?etape=INCONNUE", null).with(jeton("2", "HOM_LIAISON_READ"))).andExpect(status().isBadRequest());
    }
}
