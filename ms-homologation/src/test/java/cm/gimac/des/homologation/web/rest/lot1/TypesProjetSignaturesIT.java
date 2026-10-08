package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.PilotageTachesStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ReauthStubAdapter;
import cm.gimac.des.homologation.infrastructure.messaging.taches.RelaisTachesPilotage;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Types de projet, catalogue des fonctions et signatures d'une opération (D-91, docs/lot1/types-projet.md) par l'API :
 * création vierge et copiée, type GENERIQUE de bout en bout jusqu'à une liaison, fonction créée puis utilisée (rôles,
 * signataire, lecture filtrée), signatures dans l'ordre avec le vérificateur FIDO2 de substitution, refus hors ordre,
 * publication refusée sur une signature invalide ; REF_METIER_ADMIN (D-92).
 */
class TypesProjetSignaturesIT extends ParcoursITSupport {

    static final UUID INGENIEUR = UUID.fromString("00000000-0000-4000-8000-0000000f0001");
    static final UUID JURISTE = UUID.fromString("00000000-0000-4000-8000-0000000f0002");
    static final UUID ADMIN = UUID.fromString("00000000-0000-4000-8000-0000000f0003");

    @Autowired
    PilotageTachesStubAdapter pilotage;

    @Autowired
    RelaisTachesPilotage relaisTaches;

    @BeforeEach
    void init() {
        pilotage.vider();
        annuaire.enregistrer(new CollaboratorView(JURISTE, "Juriste du GIMAC", Set.of("ROLE_JURISTE"), null, true));
    }

    static RequestPostProcessor editeur() {
        return utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_PUBLISH", "ROLE_INGENIEUR_HOMOLOGATION");
    }

    static RequestPostProcessor juriste() {
        return utilisateur(JURISTE, avec(COMMUN, "ROLE_JURISTE"));
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, String corps) {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    static String sha256(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    static String operation(String etape, String r, String v, String consultes, String preds, String signatures) {
        return (
            "{\"libelle\":\"Opération\",\"etape\":\"" + etape + "\",\"segment\":\"AMONT\",\"ordre\":1,\"responsable\":\"" + r + "\",\"validateur\":\"" + v + "\"," +
            "\"consultes\":[" + consultes + "],\"informes\":[],\"externes\":[],\"predecesseurs\":[" + preds + "],\"delaiJours\":2,\"grain\":\"OPERATION\"," +
            "\"livrables\":[{\"code\":\"L1\",\"libelle\":\"Compte rendu\",\"nature\":\"LIVRABLE\",\"deposant\":\"" + r + "\",\"typesPieces\":[\"application/pdf\"],\"obligatoire\":true}]," +
            "\"geste\":\"DEPOSER_LIVRABLE\"" + (signatures == null ? "" : ",\"signatures\":" + signatures) + "}"
        );
    }

    String creerType(String corps, RequestPostProcessor qui) throws Exception {
        return mvc.perform(json(post("/api/v1/types-projet").with(qui), corps)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Création VIERGE (onze étapes, aucune opération) et COPIE (version active d'un autre type) ; règles et refus")
    void creationViergeEtCopie() throws Exception {
        String vierge = creerType(
            "{\"code\":\"MONNAIE_ELECTRONIQUE\",\"libelle\":\"Monnaie électronique\",\"description\":\"Émetteurs\",\"modele\":\"GENERIQUE\",\"depart\":{\"mode\":\"VIERGE\"}}",
            editeur()
        );
        assertThat((String) JsonPath.read(vierge, "$.modele")).isEqualTo("GENERIQUE");
        assertThat((Boolean) JsonPath.read(vierge, "$.actif")).isTrue();
        assertThat((Boolean) JsonPath.read(vierge, "$.proposable")).isFalse();
        assertThat((Boolean) JsonPath.read(vierge, "$.fige")).isFalse();
        assertThat((Object) JsonPath.read(vierge, "$.referentielActif")).isNull();
        String brouillon = JsonPath.read(vierge, "$.brouillonId");
        String graphe = lire("/api/v1/referentiels/" + brouillon + "/graphe", editeur());
        assertThat((List<?>) JsonPath.read(graphe, "$.noeuds")).isEmpty();
        assertThat((List<?>) JsonPath.read(graphe, "$.etapes")).hasSize(11);
        assertThat((String) JsonPath.read(graphe, "$.typeProjet")).isEqualTo("MONNAIE_ELECTRONIQUE");
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where event_type = 'homologation.project-type-created.v1'", Integer.class)).isEqualTo(1);
        // Tâche du pilote (le créateur) : compléter et publier le brouillon.
        relaisTaches.relayer();
        assertThat(pilotage.taches().values()).anyMatch(t -> t.tache().titre().contains("Monnaie électronique v1"));

        // Copie de la version active de la carte interbancaire : étapes, opérations, rôles, livrables.
        UUID source = fixtures.referentielPublie(TypeProjet.CARTE_INTERBANCAIRE);
        int nombreSource = jdbc.queryForObject("select count(*) from reference_operation where version_id = ?", Integer.class, source);
        String copie = creerType(
            "{\"code\":\"CARTE_PRIVATIVE\",\"libelle\":\"Carte privative\",\"modele\":\"CARTE_INTERBANCAIRE\",\"depart\":{\"mode\":\"COPIE\",\"typeSource\":\"CARTE_INTERBANCAIRE\"}}",
            editeur()
        );
        String brouillonCopie = JsonPath.read(copie, "$.brouillonId");
        assertThat(jdbc.queryForObject("select count(*) from reference_operation where version_id = ?::uuid", Integer.class, brouillonCopie)).isEqualTo(nombreSource);
        assertThat(jdbc.queryForObject("select type_projet from referential_version where id = ?::uuid", String.class, brouillonCopie)).isEqualTo("CARTE_PRIVATIVE");

        // Refus : code pris, code invalide, départ absent, copie d'un type sans version publiée, source inconnue, droit.
        mvc
            .perform(json(post("/api/v1/types-projet").with(editeur()), "{\"code\":\"CARTE_PRIVATIVE\",\"libelle\":\"X\",\"modele\":\"GENERIQUE\",\"depart\":{\"mode\":\"VIERGE\"}}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_TYPE_PROJET_CODE_PRIS"));
        mvc
            .perform(json(post("/api/v1/types-projet").with(editeur()), "{\"code\":\"ab\",\"libelle\":\"X\",\"modele\":\"GENERIQUE\",\"depart\":{\"mode\":\"VIERGE\"}}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_TYPE_PROJET_CODE_INVALIDE"));
        mvc
            .perform(json(post("/api/v1/types-projet").with(editeur()), "{\"code\":\"SANS_DEPART\",\"libelle\":\"X\",\"modele\":\"GENERIQUE\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_TYPE_PROJET_DEPART_INVALIDE"));
        mvc
            .perform(json(post("/api/v1/types-projet").with(editeur()), "{\"code\":\"COPIE_VIDE\",\"libelle\":\"X\",\"modele\":\"GENERIQUE\",\"depart\":{\"mode\":\"COPIE\",\"typeSource\":\"MONNAIE_ELECTRONIQUE\"}}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_TYPE_PROJET_SOURCE_NON_PUBLIE"));
        mvc
            .perform(json(post("/api/v1/types-projet").with(editeur()), "{\"code\":\"COPIE_INCONNUE\",\"libelle\":\"X\",\"modele\":\"GENERIQUE\",\"depart\":{\"mode\":\"COPIE\",\"typeSource\":\"INCONNU\"}}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOM_TYPE_PROJET_INTROUVABLE"));
        mvc
            .perform(json(post("/api/v1/types-projet").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")), "{\"code\":\"REFUSE\",\"libelle\":\"X\",\"modele\":\"GENERIQUE\",\"depart\":{\"mode\":\"VIERGE\"}}"))
            .andExpect(status().isForbidden());

        // Modification : motif exigé ; modèle modifiable avant la première publication ; liste filtrée par état.
        mvc
            .perform(json(put("/api/v1/types-projet/MONNAIE_ELECTRONIQUE").with(editeur()), "{\"libelle\":\"Monnaie électronique (ME)\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_MOTIF_REQUIS"));
        mvc
            .perform(json(put("/api/v1/types-projet/MONNAIE_ELECTRONIQUE").with(editeur()), "{\"libelle\":\"Monnaie électronique (ME)\",\"modele\":\"PAIEMENT_MOBILE\",\"actif\":false,\"motif\":\"Cadrage\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.libelle").value("Monnaie électronique (ME)"))
            .andExpect(jsonPath("$.modele").value("PAIEMENT_MOBILE"))
            .andExpect(jsonPath("$.actif").value(false));
        mvc.perform(get("/api/v1/types-projet?actif=true").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ"))).andExpect(jsonPath("$[*].code", not(hasItem("MONNAIE_ELECTRONIQUE"))));
        mvc.perform(get("/api/v1/types-projet").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(jsonPath("$[0].code").value("CARTE_INTERBANCAIRE"))
            .andExpect(jsonPath("$[0].fige").value(true))
            .andExpect(jsonPath("$[*].code", hasItems("PAIEMENT_MOBILE", "DELEGATAIRE", "MONNAIE_ELECTRONIQUE", "CARTE_PRIVATIVE")));
        mvc
            .perform(json(put("/api/v1/types-projet/CARTE_INTERBANCAIRE").with(editeur()), "{\"modele\":\"GENERIQUE\",\"motif\":\"Essai\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_TYPE_PROJET_FIGE"));
        mvc.perform(get("/api/v1/types-projet/INCONNU").with(editeur())).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where event_type = 'homologation.project-type-updated.v1'", Integer.class)).isEqualTo(1);

        // Suppression d'un brouillon (D-92) par l'administration des référentiels métier ; une version publiée reste.
        mvc.perform(delete("/api/v1/referentiels/" + brouillonCopie).with(utilisateur(ADMIN, "REF_METIER_ADMIN"))).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from referential_version where id = ?::uuid", Integer.class, brouillonCopie)).isZero();
        mvc
            .perform(delete("/api/v1/referentiels/" + source).with(utilisateur(ADMIN, "REF_METIER_ADMIN")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_FROZEN"));
    }

    @Test
    @DisplayName("Fonction créée puis utilisée ; type GENERIQUE de bout en bout ; signatures dans l'ordre (FIDO2), refus hors ordre, publication refusée")
    void generiqueFonctionsSignatures() throws Exception {
        // Fonction du catalogue reliée à un profil créé par l'administration.
        mvc
            .perform(json(post("/api/v1/referentiels/fonctions").with(editeur()), "{\"code\":\"JURISTE\",\"libelle\":\"Juriste\",\"profils\":[\"ROLE_JURISTE\"],\"externe\":false}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reprise").value(false));
        mvc
            .perform(json(post("/api/v1/referentiels/fonctions").with(editeur()), "{\"code\":\"JURISTE\",\"libelle\":\"Juriste\",\"profils\":[\"ROLE_JURISTE\"]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_FONCTION_CODE_PRIS"));
        mvc
            .perform(json(post("/api/v1/referentiels/fonctions").with(editeur()), "{\"code\":\"SANS_PROFIL\",\"libelle\":\"Sans profil\",\"profils\":[]}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_FONCTION_PROFIL_REQUIS"));
        mvc.perform(get("/api/v1/referentiels/fonctions").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ"))).andExpect(jsonPath("$[0].code").value("DDRM")).andExpect(jsonPath("$[16].code").value("JURISTE"));
        mvc.perform(get("/api/v1/referentiels/catalogue").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(jsonPath("$.fonctions[*].code", hasItem("JURISTE")))
            .andExpect(jsonPath("$.modelesTypeProjet[*].code", hasItems("CARTE_INTERBANCAIRE", "GENERIQUE")));
        mvc.perform(get("/api/v1/intervenants?fonction=JURISTE").with(utilisateur(INGENIEUR, "HOM_LIAISON_READ"))).andExpect(jsonPath("$[0].id").value(JURISTE.toString()));
        assertThat(Fonction.valueOf("JURISTE").profils()).containsExactly("ROLE_JURISTE");

        // Type générique vierge, deux opérations ; la seconde exige deux signatures ordonnées.
        String type = creerType("{\"code\":\"AGREGATEUR\",\"libelle\":\"Agrégateur de paiement\",\"modele\":\"GENERIQUE\",\"depart\":{\"mode\":\"VIERGE\"}}", editeur());
        String version = JsonPath.read(type, "$.brouillonId");
        String base = "/api/v1/referentiels/" + version;
        mvc.perform(json(put(base + "/operations/OUV").with(editeur()), operation("LANCEMENT", "CONDUCTEUR", "CHEF_DES", "\"JURISTE\"", "", null))).andExpect(status().isOk());
        String invalides = "[{\"code\":\"VISA_JURIDIQUE\",\"libelle\":\"Visa juridique\",\"signataire\":\"INCONNUE_X\",\"ordre\":1,\"porte\":[\"L9\"]}]";
        mvc.perform(json(put(base + "/operations/SIG").with(editeur()), operation("LANCEMENT", "CONDUCTEUR", "CHEF_DES", "", "\"OUV\"", invalides))).andExpect(status().isOk());
        mvc
            .perform(json(put(base + "/operations/SIG").with(editeur()), operation("LANCEMENT", "CONDUCTEUR", "CHEF_DES", "", "\"OUV\"", "[{\"code\":\"x\",\"libelle\":\"V\",\"signataire\":\"JURISTE\"}]")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_SIGNATURE_INVALIDE"))
            .andExpect(jsonPath("$.details.champ").value("signatures[0].code"));
        mvc
            .perform(post(base + "/publication").with(editeur()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_REFERENTIAL_GRAPH_INVALID"))
            .andExpect(jsonPath("$.details.anomalies[*].type", hasItems("SIGNATAIRE_INCONNU", "SIGNATURE_PORTEE_INVALIDE", "FONCTION_INCONNUE")));
        String signatures =
            "[{\"code\":\"APPROBATION\",\"libelle\":\"Approbation du conducteur\",\"signataire\":\"CONDUCTEUR\",\"ordre\":2,\"porte\":[\"L1\"]}," +
            "{\"code\":\"VISA_JURIDIQUE\",\"libelle\":\"Visa juridique\",\"signataire\":\"JURISTE\",\"ordre\":1,\"porte\":[\"L1\",\"OUV/L1\"]}]";
        mvc
            .perform(json(put(base + "/operations/SIG").with(editeur()), operation("LANCEMENT", "CONDUCTEUR", "CHEF_DES", "", "\"OUV\"", signatures)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signatures[*].code", hasItems("APPROBATION", "VISA_JURIDIQUE")));
        mvc.perform(post(base + "/publication").with(editeur())).andExpect(status().isOk()).andExpect(jsonPath("$.numero").value(1));
        mvc.perform(get("/api/v1/types-projet/AGREGATEUR").with(editeur())).andExpect(jsonPath("$.proposable").value(true)).andExpect(jsonPath("$.fige").value(true)).andExpect(jsonPath("$.referentielActif.version").value(1));
        String csv = mvc.perform(get(base + "/export").with(editeur())).andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("APPROBATION:CONDUCTEUR:2:L1").contains("VISA_JURIDIQUE:JURISTE:1:L1,OUV/L1");

        // Lecture filtrée (D-89) : le juriste voit l'opération où il est consulté et celle qu'il signe.
        mvc
            .perform(get(base + "/operations").with(juriste()))
            .andExpect(header().string("X-Gimac-Filtre", "CONCERNE"))
            .andExpect(jsonPath("$[?(@.fantome == false)].code", hasItems("OUV", "SIG")));

        // Liaison sur le type générique : périmètre sans catalogue, contexte de certification sans attestation.
        UUID institution = adherent("Agrégateur Alpha", "ETABLISSEMENT_PAIEMENT", "PAIEMENT_MARCHAND");
        String corps = mvc
            .perform(json(post("/api/v1/liaisons").with(chefDes()), "{\"institutionId\":\"" + institution + "\",\"typeProjet\":\"AGREGATEUR\",\"chefDeProjetId\":\"" + CollaboratorStubAdapter.CONDUCTEUR_1 + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.typeProjet").value("AGREGATEUR"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String liaison = JsonPath.read(corps, "$.id");
        assertThat((String) JsonPath.read(jdbc.queryForObject("select payload::text from outbox_event where event_type = 'homologation.liaison-opened.v1'", String.class), "$.modeleTypeProjet")).isEqualTo("GENERIQUE");
        mvc
            .perform(get("/api/v1/interne/liaisons/" + liaison + "/contexte-certification").with(service()))
            .andExpect(jsonPath("$.typeProjet").value("AGREGATEUR"))
            .andExpect(jsonPath("$.modeleTypeProjet").value("GENERIQUE"))
            .andExpect(jsonPath("$.attestationsExigees").isEmpty());

        // Exécution : OUV close par son livrable, SIG attend ses signatures dans l'ordre.
        livrable(opId(liaison, "OUV"), conducteur());
        String sig = opId(liaison, "SIG");
        livrable(sig, conducteur());
        assertThat(etat(liaison, "SIG")).isEqualTo("EN_SIGNATURE");
        String signer = "/api/v1/liaisons/" + liaison + "/operations/SIG/signatures/";
        mvc.perform(post(signer + "VISA_JURIDIQUE").with(juriste())).andExpect(status().is(428));
        mvc
            .perform(post(signer + "APPROBATION").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(conducteur()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_SIGNATURE_ORDRE"));
        mvc
            .perform(post(signer + "VISA_JURIDIQUE").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(conducteur()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_SIGNATURE_NON_SIGNATAIRE"));
        mvc
            .perform(post(signer + "VISA_JURIDIQUE").header("X-Reauth-Assertion", ReauthStubAdapter.ASSERTION_REFUSEE).with(juriste()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_REAUTH_INVALID"));
        mvc.perform(post(signer + "INCONNUE").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(juriste())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("HOM_SIGNATURE_INCONNUE"));
        mvc.perform(post(signer + "VISA_JURIDIQUE").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(juriste())).andExpect(status().isNoContent());
        mvc
            .perform(post(signer + "VISA_JURIDIQUE").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(juriste()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_SIGNATURE_DEJA_SIGNEE"));
        Map<String, Object> vue = operation(liaison, "SIG");
        assertThat(vue.get("etat")).isEqualTo("EN_SIGNATURE");
        assertThat((List<String>) vue.get("actionsPermises")).contains("SIGNER");
        assertThat((List<String>) JsonPath.read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations", juriste()), "$[?(@.rang == 'SIG')].actionsPermises[*]")).doesNotContain("SIGNER");
        // Empreinte : lignes REFERENCE:documentId:sha256 des livrables portés, dans l'ordre de « porte ».
        String docSig = JsonPath.read(vue, "$.preuve.documentRef");
        String docOuv = JsonPath.read(operation(liaison, "OUV"), "$.preuve.documentRef");
        String attendue = sha256("L1:" + docSig + ":" + SHA + "\nOUV/L1:" + docOuv + ":" + SHA + "\n");
        assertThat((List<Object>) JsonPath.read(vue, "$.signatures[?(@.code == 'VISA_JURIDIQUE')].empreinte")).containsExactly(attendue);
        assertThat((List<Object>) JsonPath.read(vue, "$.signatures[?(@.code == 'VISA_JURIDIQUE')].etat")).containsExactly("SIGNEE");
        assertThat((List<Object>) JsonPath.read(vue, "$.signatures[?(@.code == 'APPROBATION')].suivante")).containsExactly(true);
        // Tâche au signataire attendu (conducteur désigné), terminée par sa signature.
        relaisTaches.relayer();
        String cle = "/SIG/S/APPROBATION|" + CollaboratorStubAdapter.CONDUCTEUR_1;
        assertThat(pilotage.taches()).anySatisfy((k, t) -> {
            assertThat(k).endsWith(cle);
            assertThat(t.etat()).isEqualTo("OUVERTE");
            assertThat(t.tache().titre()).startsWith("Signer : ");
        });
        mvc.perform(post(signer + "APPROBATION").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(validateur())).andExpect(status().isForbidden());
        mvc.perform(post(signer + "APPROBATION").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(conducteur())).andExpect(status().isNoContent());
        assertThat(etat(liaison, "SIG")).isEqualTo("FAITE");
        relaisTaches.relayer();
        assertThat(pilotage.taches()).anySatisfy((k, t) -> {
            assertThat(k).endsWith(cle);
            assertThat(t.etat()).isEqualTo("TERMINEE");
        });
        List<String> charges = jdbc.queryForList("select payload::text from outbox_event where event_type = 'homologation.operation-signed.v1' order by created_at", String.class);
        assertThat(charges).hasSize(2);
        assertThat((String) JsonPath.read(charges.get(0), "$.signature")).isEqualTo("VISA_JURIDIQUE");
        assertThat((String) JsonPath.read(charges.get(0), "$.empreinte")).isEqualTo(attendue);
        assertThat((Boolean) JsonPath.read(charges.get(0), "$.operationClose")).isFalse();
        assertThat((List<?>) JsonPath.read(charges.get(0), "$.documents")).hasSize(2);
        assertThat((String) JsonPath.read(charges.get(1), "$.signature")).isEqualTo("APPROBATION");
        assertThat((Boolean) JsonPath.read(charges.get(1), "$.operationClose")).isTrue();

        // Désactivation : le type n'est plus proposé aux liaisons.
        mvc.perform(json(put("/api/v1/types-projet/AGREGATEUR").with(editeur()), "{\"actif\":false,\"motif\":\"Fin de la campagne\"}")).andExpect(jsonPath("$.proposable").value(false));
        UUID autre = adherent("Agrégateur Bêta", "ETABLISSEMENT_PAIEMENT");
        mvc
            .perform(json(post("/api/v1/liaisons").with(chefDes()), "{\"institutionId\":\"" + autre + "\",\"typeProjet\":\"AGREGATEUR\",\"chefDeProjetId\":\"" + CollaboratorStubAdapter.CONDUCTEUR_1 + "\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_TYPE_PROJET_INACTIF"));
        mvc
            .perform(json(post("/api/v1/liaisons").with(chefDes()), "{\"institutionId\":\"" + autre + "\",\"typeProjet\":\"INCONNU\",\"chefDeProjetId\":\"" + CollaboratorStubAdapter.CONDUCTEUR_1 + "\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOM_TYPE_PROJET_INTROUVABLE"));

        // Fonction désactivée : absente du catalogue actif, signalée au contrôle d'une version qui la cite.
        mvc
            .perform(json(put("/api/v1/referentiels/fonctions/JURISTE").with(utilisateur(ADMIN, "REF_METIER_ADMIN")), "{\"actif\":false,\"motif\":\"Réorganisation\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actif").value(false));
        assertThat(CatalogueFonctions.active(Fonction.valueOf("JURISTE"))).isFalse();
        mvc
            .perform(post(base + "/controle").with(editeur()))
            .andExpect(jsonPath("$.conforme").value(false))
            .andExpect(jsonPath("$.anomalies[*].type", hasItem("SIGNATAIRE_INCONNU")));
        assertThat(jdbc.queryForList("select event_type from outbox_event where event_type like 'homologation.function-%' order by created_at", String.class))
            .containsExactly("homologation.function-created.v1", "homologation.function-updated.v1");
    }
}
