package cm.gimac.des.collaboration.web.rest.messagerie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.collaboration.AbstractCollaborationIT;
import cm.gimac.des.collaboration.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.collaboration.infrastructure.client.DocumentStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** Messagerie de travail (D-64) de bout en bout sur PostgreSQL et Kafka : contrat docs/lot1/messagerie.md. */
class MessagerieIT extends AbstractCollaborationIT {

    static final String PAUL = CollaboratorStubAdapter.CHEF_DES;
    static final String AMINATA = CollaboratorStubAdapter.CONDUCTEUR;
    static final String SERGE = CollaboratorStubAdapter.INGENIEUR;
    static final String CLARISSE = CollaboratorStubAdapter.RSSI;
    static final String BASE = "/api/v1/messagerie";

    @Autowired
    DocumentStubAdapter depots;

    static MockMultipartFile json(String contenu) {
        return new MockMultipartFile("message", "", MediaType.APPLICATION_JSON_VALUE, contenu.getBytes(StandardCharsets.UTF_8));
    }

    ResultActions creer(String qui, String corps, MockMultipartFile... fichiers) throws Exception {
        MockMultipartHttpServletRequestBuilder r = multipart(BASE + "/conversations").file(json(corps));
        for (MockMultipartFile f : fichiers) {
            r.file(f);
        }
        return mvc.perform(r.with(utilisateur(qui, MESSAGERIE)));
    }

    ResultActions envoyer(String qui, String conversation, String contenu, MockMultipartFile... fichiers) throws Exception {
        MockMultipartHttpServletRequestBuilder r = multipart(BASE + "/conversations/" + conversation + "/messages").file(
            json("{\"contenu\":\"" + contenu + "\",\"etiquettes\":[]}")
        );
        for (MockMultipartFile f : fichiers) {
            r.file(f);
        }
        return mvc.perform(r.with(utilisateur(qui, MESSAGERIE)));
    }

    String directe(String de, String a, String contenu) throws Exception {
        String corps = creer(de, "{\"destinataires\":[\"" + a + "\"],\"contenu\":\"" + contenu + "\"}").andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.id");
    }

    long nonLus(String qui) throws Exception {
        String corps = mvc.perform(get(BASE + "/non-lus").with(utilisateur(qui, MESSAGERIE))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, "$.total")).longValue();
    }

    @Test
    @DisplayName("Message direct : conversation créée (201), réutilisée ensuite (200), événement sans contenu relayé sur collaboration.message")
    void messageDirect() throws Exception {
        String corps = creer(PAUL, "{\"destinataires\":[\"" + AMINATA + "\"],\"titre\":null,\"liaisonId\":null,\"contenu\":\"Bonjour Aminata\",\"etiquettes\":[]}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("DIRECTE"))
            .andExpect(jsonPath("$.membres.length()").value(2))
            .andExpect(jsonPath("$.membres[*].nom", org.hamcrest.Matchers.containsInAnyOrder("Paul Mbarga Atangana", "Aminata Ngono")))
            .andExpect(jsonPath("$.membres[*].fonction", org.hamcrest.Matchers.hasItem("Conductrice de projet")))
            .andExpect(jsonPath("$.dernierMessage.extrait").value("Bonjour Aminata"))
            .andExpect(jsonPath("$.nonLus").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String id = JsonPath.read(corps, "$.id");

        creer(AMINATA, "{\"destinataires\":[\"" + PAUL + "\"],\"contenu\":\"Bonjour Paul\"}").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertThat(jdbc.queryForObject("select count(*) from conversation", Integer.class)).isEqualTo(1);

        relais.relayerJusquaEnvoi(id);
        var enregistrements = relais.lire("collaboration.message", id);
        assertThat(enregistrements).isNotEmpty();
        String evenement = enregistrements.get(0).value();
        assertThat((String) JsonPath.read(evenement, "$.type")).isEqualTo("collaboration.message-sent.v1");
        assertThat((String) JsonPath.read(evenement, "$.data.conversationId")).isEqualTo(id);
        assertThat((List<String>) JsonPath.read(evenement, "$.data.destinataires")).containsExactly(AMINATA);
        assertThat(evenement).doesNotContain("Bonjour");
    }

    @Test
    @DisplayName("Groupe de discussion (D-68) : créé nommé sans message, renommé, membres ajoutés et retirés, départ ; événements dans le fil")
    void groupeDeDiscussion() throws Exception {
        String id = JsonPath.read(
            creer(PAUL, "{\"groupe\":true,\"destinataires\":[\"" + AMINATA + "\"],\"titre\":\"Comité DES\",\"contenu\":\"\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GROUPE"))
                .andExpect(jsonPath("$.titre").value("Comité DES"))
                .andExpect(jsonPath("$.membres[?(@.id=='" + PAUL + "')].administrateur").value(org.hamcrest.Matchers.contains(true)))
                .andExpect(jsonPath("$.membres[?(@.id=='" + AMINATA + "')].administrateur").value(org.hamcrest.Matchers.contains(false)))
                .andExpect(jsonPath("$.dernierMessage.evenement.type").value("GROUPE_CREE"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        String c = BASE + "/conversations/" + id;
        mvc
            .perform(patch(c).contentType(MediaType.APPLICATION_JSON).content("{\"titre\":\"Comité de pilotage\"}").with(utilisateur(PAUL, MESSAGERIE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titre").value("Comité de pilotage"));
        mvc
            .perform(post(c + "/membres").contentType(MediaType.APPLICATION_JSON).content("{\"membres\":[\"" + SERGE + "\",\"" + CLARISSE + "\"]}").with(utilisateur(PAUL, MESSAGERIE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membres.length()").value(4));
        // Un membre ordinaire ne gère pas le groupe.
        mvc
            .perform(patch(c).contentType(MediaType.APPLICATION_JSON).content("{\"titre\":\"X\"}").with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("COL_GROUP_ADMIN_REQUIRED"));
        mvc.perform(delete(c + "/membres/" + CLARISSE).with(utilisateur(PAUL, MESSAGERIE))).andExpect(status().isNoContent());
        mvc.perform(get(c).with(utilisateur(CLARISSE, MESSAGERIE))).andExpect(status().isNotFound());
        mvc.perform(delete(c + "/membres/" + SERGE).with(utilisateur(SERGE, MESSAGERIE))).andExpect(status().isNoContent());
        mvc
            .perform(get(c + "/messages").with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(jsonPath("$.contenu[*].evenement.type").value(org.hamcrest.Matchers.contains("GROUPE_CREE", "GROUPE_RENOMME", "MEMBRES_AJOUTES", "MEMBRE_RETIRE", "MEMBRE_PARTI")))
            .andExpect(jsonPath("$.contenu[2].evenement.cibles.length()").value(2))
            .andExpect(jsonPath("$.contenu[3].evenement.cibles[0]").value("Clarisse Bilong"))
            .andExpect(jsonPath("$.contenu[4].auteur.id").value(SERGE))
            .andExpect(jsonPath("$.contenu[0].contenu").value(""));
        // Le départ de l'administrateur confie la gestion au plus ancien membre restant.
        mvc.perform(delete(c + "/membres/" + PAUL).with(utilisateur(PAUL, MESSAGERIE))).andExpect(status().isNoContent());
        mvc
            .perform(get(c).with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(jsonPath("$.membres.length()").value(1))
            .andExpect(jsonPath("$.membres[0].administrateur").value(true));
        // Une conversation directe ne se quitte pas.
        String d = directe(PAUL, AMINATA, "Bonjour");
        mvc
            .perform(delete(BASE + "/conversations/" + d + "/membres/" + PAUL).with(utilisateur(PAUL, MESSAGERIE)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COL_NOT_A_GROUP"));
    }

    @Test
    @DisplayName("Chiffrement au repos et RG-52 : aucun clair en base, numéro de carte masqué avant l'enregistrement")
    void chiffrementEtMasquage() throws Exception {
        String id = directe(PAUL, AMINATA, "Carte 4111 1111 1111 1111 refusée au guichet");
        byte[] chiffre = jdbc.queryForObject("select contenu_chiffre from message", byte[].class);
        String brut = new String(chiffre, StandardCharsets.ISO_8859_1);
        assertThat(brut).doesNotContain("Carte", "4111", "refusée");
        mvc
            .perform(get(BASE + "/conversations/" + id + "/messages").with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu[0].contenu").value("Carte 411111******1111 refusée au guichet"))
            .andExpect(jsonPath("$.contenu[0].moi").value(false));
        String messageId = jdbc.queryForObject("select id::text from message", String.class);
        assertThatThrownBy(() -> jdbc.update("update message set auteur_nom = 'X' where id = ?::uuid", messageId)).hasMessageContaining("COL_MESSAGE_IMMUTABLE");
    }

    @Test
    @DisplayName("Fil : du plus ancien au plus récent, pagination vers le passé ; non-lus comptés puis remis à zéro")
    void filEtNonLus() throws Exception {
        String id = directe(PAUL, AMINATA, "m1");
        for (int i = 2; i <= 5; i++) {
            envoyer(PAUL, id, "m" + i).andExpect(status().isCreated()).andExpect(jsonPath("$.moi").value(true));
        }
        assertThat(nonLus(AMINATA)).isEqualTo(5);
        assertThat(nonLus(PAUL)).isZero();

        String page = mvc
            .perform(get(BASE + "/conversations/" + id + "/messages?taille=2").with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(jsonPath("$.contenu[0].contenu").value("m4"))
            .andExpect(jsonPath("$.contenu[1].contenu").value("m5"))
            .andExpect(jsonPath("$.plusAnciens").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String plusAncien = JsonPath.read(page, "$.contenu[0].id");
        mvc
            .perform(get(BASE + "/conversations/" + id + "/messages?taille=10&avant=" + plusAncien).with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(jsonPath("$.contenu.length()").value(3))
            .andExpect(jsonPath("$.contenu[0].contenu").value("m1"))
            .andExpect(jsonPath("$.plusAnciens").value(false));

        mvc.perform(get(BASE + "/conversations").with(utilisateur(AMINATA, MESSAGERIE))).andExpect(jsonPath("$.contenu[0].nonLus").value(5));
        mvc.perform(post(BASE + "/conversations/" + id + "/lu").with(utilisateur(AMINATA, MESSAGERIE))).andExpect(status().isNoContent());
        assertThat(nonLus(AMINATA)).isZero();
        envoyer(PAUL, id, "m6");
        assertThat(nonLus(AMINATA)).isEqualTo(1);
        mvc.perform(get(BASE + "/conversations/" + id).with(utilisateur(AMINATA, MESSAGERIE))).andExpect(jsonPath("$.nonLus").value(1));
    }

    @Test
    @DisplayName("Groupe rattaché à une liaison, étiquettes ; liste filtrée par liaison et par texte")
    void groupeEtEtiquettes() throws Exception {
        UUID liaison = UUID.randomUUID();
        String corps =
            "{\"destinataires\":[\"" + AMINATA + "\",\"" + SERGE + "\"],\"titre\":\"Recette EP A\",\"liaisonId\":\"" + liaison + "\"," +
            "\"contenu\":\"Point sur LIA-2026-027\",\"etiquettes\":[{\"liaisonId\":\"" + liaison + "\",\"reference\":\"LIA-2026-027\"}]}";
        String id = JsonPath.read(
            creer(PAUL, corps)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GROUPE"))
                .andExpect(jsonPath("$.titre").value("Recette EP A"))
                .andExpect(jsonPath("$.liaison.reference").value("LIA-2026-027"))
                .andExpect(jsonPath("$.membres.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        directe(PAUL, CLARISSE, "Autre sujet");

        mvc
            .perform(get(BASE + "/conversations/" + id + "/messages").with(utilisateur(SERGE, MESSAGERIE)))
            .andExpect(jsonPath("$.contenu[0].etiquettes[0].reference").value("LIA-2026-027"))
            .andExpect(jsonPath("$.contenu[0].etiquettes[0].liaisonId").value(liaison.toString()));
        mvc
            .perform(get(BASE + "/conversations?liaisonId=" + liaison).with(utilisateur(PAUL, MESSAGERIE)))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].id").value(id));
        mvc.perform(get(BASE + "/conversations").with(utilisateur(PAUL, MESSAGERIE))).andExpect(jsonPath("$.total").value(2));
        mvc.perform(get(BASE + "/conversations?recherche=bilong").with(utilisateur(PAUL, MESSAGERIE))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(BASE + "/conversations?recherche=lia-2026").with(utilisateur(PAUL, MESSAGERIE))).andExpect(jsonPath("$.contenu[0].id").value(id));
        mvc.perform(get(BASE + "/conversations?recherche=recette&page=0&taille=1").with(utilisateur(SERGE, MESSAGERIE))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(BASE + "/conversations").with(utilisateur(CLARISSE, MESSAGERIE))).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("Pièce jointe : déposée, empreinte SHA-256, téléchargée par un membre en pièce jointe, 404 sinon")
    void pieceJointe() throws Exception {
        byte[] pdf = "%PDF-1.7 procès-verbal".getBytes(StandardCharsets.UTF_8);
        String corps = creer(
            PAUL,
            "{\"destinataires\":[\"" + AMINATA + "\"],\"contenu\":\"\"}",
            new MockMultipartFile("fichiers", "pv recette.pdf", "application/pdf", pdf)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dernierMessage.piecesJointes").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String id = JsonPath.read(corps, "$.id");
        String fil = mvc
            .perform(get(BASE + "/conversations/" + id + "/messages").with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(jsonPath("$.contenu[0].piecesJointes[0].nom").value("pv recette.pdf"))
            .andExpect(jsonPath("$.contenu[0].piecesJointes[0].taille").value(pdf.length))
            .andExpect(jsonPath("$.contenu[0].piecesJointes[0].typeMime").value("application/pdf"))
            .andExpect(jsonPath("$.contenu[0].piecesJointes[0].empreinte").value(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf))))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String piece = JsonPath.read(fil, "$.contenu[0].piecesJointes[0].id");
        mvc
            .perform(get(BASE + "/pieces/" + piece).with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(pdf));
        mvc
            .perform(get(BASE + "/pieces/" + piece).with(utilisateur(SERGE, MESSAGERIE)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COL_ATTACHMENT_NOT_FOUND"));
        creer(PAUL, "{\"destinataires\":[\"" + AMINATA + "\"],\"contenu\":\"x\"}", new MockMultipartFile("fichiers", "outil.exe", "application/octet-stream", new byte[] { 1 }))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COL_ATTACHMENT_TYPE"));
    }

    @Test
    @DisplayName("D-74 : ticket d'un dépôt fragmenté joint sans fichier, puis transféré vers une autre conversation (même document) ; ticket d'un autre : 400")
    void ticketEtTransfert() throws Exception {
        byte[] video = "atelier du 01/10".getBytes(StandardCharsets.UTF_8);
        UUID ticket = depots.deposerFragmente("atelier.mp4", "video/mp4", video, "MESSAGERIE", PAUL);
        String corps = creer(PAUL, "{\"destinataires\":[\"" + AMINATA + "\"],\"contenu\":\"\",\"documents\":[\"" + ticket + "\"]}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dernierMessage.piecesJointes").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String id = JsonPath.read(corps, "$.id");
        String fil = mvc
            .perform(get(BASE + "/conversations/" + id + "/messages").with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(jsonPath("$.contenu[0].piecesJointes[0].nom").value("atelier.mp4"))
            .andExpect(jsonPath("$.contenu[0].piecesJointes[0].typeMime").value("video/mp4"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String piece = JsonPath.read(fil, "$.contenu[0].piecesJointes[0].id");

        // Aminata transfère la pièce à Serge : nouvelle pièce, même document.
        String vers = directe(AMINATA, SERGE, "Enregistrement de l'atelier");
        mvc
            .perform(
                multipart(BASE + "/conversations/" + vers + "/messages")
                    .file(json("{\"contenu\":\"\",\"transferts\":[\"" + piece + "\"]}"))
                    .with(utilisateur(AMINATA, MESSAGERIE))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.piecesJointes[0].nom").value("atelier.mp4"));
        assertThat(jdbc.queryForObject("select count(distinct document_ref) from piece_jointe", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from piece_jointe", Integer.class)).isEqualTo(2);
        String filSerge = mvc.perform(get(BASE + "/conversations/" + vers + "/messages").with(utilisateur(SERGE, MESSAGERIE))).andReturn().getResponse().getContentAsString();
        String transferee = JsonPath.read(filSerge, "$.contenu[1].piecesJointes[0].id");
        mvc
            .perform(get(BASE + "/pieces/" + transferee).with(utilisateur(SERGE, MESSAGERIE)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Length", String.valueOf(video.length)))
            .andExpect(content().bytes(video));

        // Serge ne peut ni réutiliser le ticket de Paul, ni transférer une pièce d'une conversation dont il n'est pas membre.
        envoyerJson(SERGE, vers, "{\"contenu\":\"\",\"documents\":[\"" + ticket + "\"]}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COL_DOCUMENT_INVALID"));
        envoyerJson(SERGE, vers, "{\"contenu\":\"\",\"transferts\":[\"" + piece + "\"]}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COL_ATTACHMENT_NOT_FOUND"));
    }

    ResultActions envoyerJson(String qui, String conversation, String message) throws Exception {
        return mvc.perform(multipart(BASE + "/conversations/" + conversation + "/messages").file(json(message)).with(utilisateur(qui, MESSAGERIE)));
    }

    @Test
    @DisplayName("Non-membre : 404 sur la conversation, le fil, l'envoi et la lecture ; son existence n'est pas révélée")
    void nonMembre() throws Exception {
        String id = directe(PAUL, AMINATA, "Confidentiel");
        mvc.perform(get(BASE + "/conversations/" + id).with(utilisateur(SERGE, MESSAGERIE))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COL_CONVERSATION_NOT_FOUND"));
        mvc.perform(get(BASE + "/conversations/" + id + "/messages").with(utilisateur(SERGE, MESSAGERIE))).andExpect(status().isNotFound());
        envoyer(SERGE, id, "Intrusion").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COL_CONVERSATION_NOT_FOUND"));
        mvc.perform(post(BASE + "/conversations/" + id + "/lu").with(utilisateur(SERGE, MESSAGERIE))).andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/conversations").with(utilisateur(SERGE, MESSAGERIE))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(BASE + "/conversations/" + UUID.randomUUID()).with(utilisateur(PAUL, MESSAGERIE))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from message", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Annuaire et règles : appelant exclu, destinataires inconnus, soi-même, contenu vide ou trop long, étiquette invalide")
    void annuaireEtRegles() throws Exception {
        mvc
            .perform(get(BASE + "/annuaire?taille=50").with(utilisateur(PAUL, MESSAGERIE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(4))
            .andExpect(jsonPath("$.contenu[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(PAUL))));
        mvc.perform(get(BASE + "/annuaire?recherche=sécurité").with(utilisateur(PAUL, MESSAGERIE))).andExpect(jsonPath("$.contenu[0].id").value(CLARISSE));
        String[][] cas = {
            { "{\"destinataires\":[],\"contenu\":\"x\"}", "COL_RECIPIENT_REQUIRED" },
            { "{\"destinataires\":[\"" + CollaboratorStubAdapter.CORRESPONDANT + "\"],\"contenu\":\"x\"}", "COL_RECIPIENT_UNKNOWN" },
            { "{\"destinataires\":[\"" + CollaboratorStubAdapter.INACTIF + "\"],\"contenu\":\"x\"}", "COL_RECIPIENT_UNKNOWN" },
            { "{\"destinataires\":[\"" + PAUL + "\"],\"contenu\":\"x\"}", "COL_RECIPIENT_SELF" },
            { "{\"destinataires\":[\"" + AMINATA + "\"],\"contenu\":\"   \"}", "COL_MESSAGE_EMPTY" },
            { "{\"destinataires\":[\"" + AMINATA + "\"],\"contenu\":\"" + "x".repeat(4001) + "\"}", "COL_MESSAGE_TOO_LONG" },
            {
                "{\"destinataires\":[\"" + AMINATA + "\"],\"contenu\":\"x\",\"etiquettes\":[{\"liaisonId\":\"" + UUID.randomUUID() + "\",\"reference\":\"LIA-27\"}]}",
                "COL_TAG_INVALID",
            },
        };
        for (String[] c : cas) {
            creer(PAUL, c[0]).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(c[1]));
        }
        assertThat(jdbc.queryForObject("select count(*) from conversation", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from outbox_event", Integer.class)).isZero();
    }

    @org.springframework.beans.factory.annotation.Autowired
    cm.gimac.des.collaboration.application.port.out.CollaboratorDirectoryPort annuaireBean;

    @Test
    @DisplayName("mon-compte.md §1 : version de la photo de profil relayée dans l'annuaire, les membres et les auteurs")
    void avatarVersion() throws Exception {
        CollaboratorStubAdapter annuaire = (CollaboratorStubAdapter) annuaireBean;
        var serge = annuaire.trouver(SERGE).orElseThrow();
        annuaire.enregistrer(new cm.gimac.des.collaboration.domain.messagerie.Collaborateur(serge.id(), serge.nom(), serge.fonction(), serge.profils(), null, true, "0123456789abcdef"));
        try {
            mvc
                .perform(get(BASE + "/annuaire?recherche=Ndzana").with(utilisateur(CLARISSE, MESSAGERIE)))
                .andExpect(jsonPath("$.contenu[0].avatarVersion").value("0123456789abcdef"));
            String id = directe(SERGE, CLARISSE, "Photo");
            mvc
                .perform(get(BASE + "/conversations/" + id).with(utilisateur(CLARISSE, MESSAGERIE)))
                .andExpect(jsonPath("$.membres[?(@.id == '" + SERGE + "')].avatarVersion").value(org.hamcrest.Matchers.hasItem("0123456789abcdef")))
                .andExpect(jsonPath("$.membres[?(@.id == '" + CLARISSE + "')].avatarVersion").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));
            mvc
                .perform(get(BASE + "/conversations/" + id + "/messages").with(utilisateur(CLARISSE, MESSAGERIE)))
                .andExpect(jsonPath("$.contenu[0].auteur.avatarVersion").value("0123456789abcdef"));
            mvc
                .perform(get(BASE + "/conversations").with(utilisateur(CLARISSE, MESSAGERIE)))
                .andExpect(jsonPath("$.contenu[*].membres[?(@.id == '" + SERGE + "')].avatarVersion").value(org.hamcrest.Matchers.hasItem("0123456789abcdef")));
        } finally {
            annuaire.enregistrer(serge);
        }
    }
}
