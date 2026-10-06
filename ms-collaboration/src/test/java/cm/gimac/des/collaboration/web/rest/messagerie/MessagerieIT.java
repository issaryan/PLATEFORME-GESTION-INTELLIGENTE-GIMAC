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
    @DisplayName("Route interne (D-80) : conversation lue par le jeton de service INTERNAL_COLLABORATION_READ, nom du groupe et membres")
    void conversationInterne() throws Exception {
        String groupe = JsonPath.read(
            creer(PAUL, "{\"groupe\":true,\"destinataires\":[\"" + AMINATA + "\"],\"titre\":\"Groupe Cadrage L-12\",\"contenu\":\"\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        var service = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
            .jwt(j -> j.subject("sa").claim("azp", "notification-service").claim("preferred_username", "service-account-notification-service"))
            .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("INTERNAL_COLLABORATION_READ"));
        mvc
            .perform(get("/api/v1/interne/conversations/" + groupe).with(service))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(groupe))
            .andExpect(jsonPath("$.type").value("GROUPE"))
            .andExpect(jsonPath("$.nom").value("Groupe Cadrage L-12"))
            .andExpect(jsonPath("$.membres", org.hamcrest.Matchers.containsInAnyOrder(PAUL, AMINATA)));
        String directe = directe(PAUL, SERGE, "Bonjour");
        mvc
            .perform(get("/api/v1/interne/conversations/" + directe).with(service))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("DIRECTE"))
            .andExpect(jsonPath("$.nom").doesNotExist());
        mvc.perform(get("/api/v1/interne/conversations/" + UUID.randomUUID()).with(service)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/interne/conversations/" + groupe).with(utilisateur(PAUL, MESSAGERIE))).andExpect(status().isForbidden());
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

    ResultActions envoyerJson(String qui, String conversation, String corps, MockMultipartFile... fichiers) throws Exception {
        MockMultipartHttpServletRequestBuilder r = multipart(BASE + "/conversations/" + conversation + "/messages").file(json(corps));
        for (MockMultipartFile f : fichiers) {
            r.file(f);
        }
        return mvc.perform(r.with(utilisateur(qui, MESSAGERIE)));
    }

    @Test
    @DisplayName("D-82 : réponse à un message précis et mentions ; fil, mentionneMoi, refus 422, citation purgée, événement de l'outbox")
    void reponseEtMentions() throws Exception {
        String groupe = JsonPath.read(
            creer(PAUL, "{\"groupe\":true,\"destinataires\":[\"" + AMINATA + "\",\"" + SERGE + "\"],\"titre\":\"Recette\",\"contenu\":\"\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        String evenementCree = JsonPath.read(
            mvc.perform(get(BASE + "/conversations/" + groupe + "/messages").with(utilisateur(PAUL, MESSAGERIE))).andReturn().getResponse().getContentAsString(),
            "$.contenu[0].id"
        );
        String long_ = "Le procès-verbal de la recette est prêt ; ".repeat(5);
        String cite = JsonPath.read(
            envoyerJson(AMINATA, groupe, "{\"contenu\":\"" + long_ + "\"}", new MockMultipartFile("fichiers", "pv.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reponseA").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.mentions.length()").value(0))
                .andExpect(jsonPath("$.mentionneMoi").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );

        // Réponse de Paul citant Aminata et mentionnant Serge (deux fois : dédoublonné).
        String reponse = JsonPath.read(
            envoyerJson(
                PAUL,
                groupe,
                "{\"contenu\":\"@Serge Ndzana peux-tu relire ?\",\"reponseA\":\"" + cite + "\",\"mentions\":[\"" + SERGE + "\",\"" + SERGE + "\"]}"
            )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reponseA.id").value(cite))
                .andExpect(jsonPath("$.reponseA.auteur.id").value(AMINATA))
                .andExpect(jsonPath("$.reponseA.auteur.nom").value("Aminata Ngono"))
                .andExpect(jsonPath("$.reponseA.extrait").value(org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.startsWith("Le procès-verbal"), org.hamcrest.Matchers.endsWith("…"))))
                .andExpect(jsonPath("$.reponseA.piecesJointes").value(1))
                .andExpect(jsonPath("$.reponseA.envoyeLe").isNotEmpty())
                .andExpect(jsonPath("$.reponseA.supprime").value(false))
                .andExpect(jsonPath("$.mentions.length()").value(1))
                .andExpect(jsonPath("$.mentions[0].id").value(SERGE))
                .andExpect(jsonPath("$.mentions[0].nom").isNotEmpty())
                .andExpect(jsonPath("$.mentionneMoi").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        String extrait = JsonPath.read(
            mvc.perform(get(BASE + "/conversations/" + groupe + "/messages").with(utilisateur(PAUL, MESSAGERIE))).andReturn().getResponse().getContentAsString(),
            "$.contenu[2].reponseA.extrait"
        );
        assertThat(extrait.length()).isLessThanOrEqualTo(120);

        // Fil : Aminata (citée) et Serge (mentionné) sont concernés ; Paul, auteur, ne l'est pas.
        String fil = BASE + "/conversations/" + groupe + "/messages";
        mvc
            .perform(get(fil).with(utilisateur(AMINATA, MESSAGERIE)))
            .andExpect(jsonPath("$.contenu[2].id").value(reponse))
            .andExpect(jsonPath("$.contenu[2].reponseA.id").value(cite))
            .andExpect(jsonPath("$.contenu[2].mentionneMoi").value(true))
            .andExpect(jsonPath("$.contenu[1].mentionneMoi").value(false));
        mvc.perform(get(fil).with(utilisateur(SERGE, MESSAGERIE))).andExpect(jsonPath("$.contenu[2].mentionneMoi").value(true));
        mvc.perform(get(fil).with(utilisateur(PAUL, MESSAGERIE))).andExpect(jsonPath("$.contenu[2].mentionneMoi").value(false));

        // Refus : message d'une autre conversation, événement du groupe, inconnu ; mention hors membres ou de soi.
        String directe = directe(PAUL, CLARISSE, "Bonjour");
        String ailleurs = JsonPath.read(
            mvc.perform(get(BASE + "/conversations/" + directe + "/messages").with(utilisateur(PAUL, MESSAGERIE))).andReturn().getResponse().getContentAsString(),
            "$.contenu[0].id"
        );
        for (String invalide : List.of(ailleurs, evenementCree, UUID.randomUUID().toString())) {
            envoyerJson(PAUL, groupe, "{\"contenu\":\"Oui\",\"reponseA\":\"" + invalide + "\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COL_REPONSE_INVALIDE"));
        }
        for (String mention : List.of(CLARISSE, PAUL)) {
            envoyerJson(PAUL, groupe, "{\"contenu\":\"@x\",\"mentions\":[\"" + mention + "\"]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COL_MENTION_INVALIDE"));
        }
        assertThat(jdbc.queryForObject("select count(*) from message where conversation_id = ?::uuid", Integer.class, groupe)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from message_mention", Integer.class)).isEqualTo(1);

        // Événement de l'outbox : auteur cité et mentionnés, toujours sans contenu.
        String charge = jdbc.queryForObject(
            "select payload::text from outbox_event where event_type = 'collaboration.message-sent.v1' and payload ->> 'messageId' = ?",
            String.class,
            reponse
        );
        assertThat((String) JsonPath.read(charge, "$.reponseAAuteurId")).isEqualTo(AMINATA);
        assertThat((List<String>) JsonPath.read(charge, "$.mentionnes")).containsExactly(SERGE);
        assertThat((List<String>) JsonPath.read(charge, "$.destinataires")).containsExactlyInAnyOrder(AMINATA, SERGE);
        assertThat(charge).doesNotContain("relire", "procès-verbal");
        String chargeCite = jdbc.queryForObject(
            "select payload::text from outbox_event where event_type = 'collaboration.message-sent.v1' and payload ->> 'messageId' = ?",
            String.class,
            cite
        );
        assertThat((Object) JsonPath.read(chargeCite, "$.reponseAAuteurId")).isNull();
        assertThat((List<String>) JsonPath.read(chargeCite, "$.mentionnes")).isEmpty();
        relais.relayerJusquaEnvoi(groupe);
        assertThat(relais.lire("collaboration.message", groupe))
            .extracting(r -> r.value())
            .anySatisfy(v -> {
                assertThat((String) JsonPath.read(v, "$.data.messageId")).isEqualTo(reponse);
                assertThat((String) JsonPath.read(v, "$.data.reponseAAuteurId")).isEqualTo(AMINATA);
                assertThat((List<String>) JsonPath.read(v, "$.data.mentionnes")).containsExactly(SERGE);
            });

        // Purge du message cité (RG-53) : la réponse, immuable, reste ; la citation est marquée supprimée.
        // Réserve de connexions sans validation automatique : suppression validée explicitement.
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) cx -> {
            try (var st = cx.createStatement()) {
                st.execute("delete from piece_jointe where message_id = '" + cite + "'");
                st.execute("delete from message where id = '" + cite + "'");
            }
            if (!cx.getAutoCommit()) {
                cx.commit();
            }
            return null;
        });
        mvc
            .perform(get(fil).with(utilisateur(SERGE, MESSAGERIE)))
            .andExpect(jsonPath("$.contenu[1].id").value(reponse))
            .andExpect(jsonPath("$.contenu[1].reponseA.id").value(cite))
            .andExpect(jsonPath("$.contenu[1].reponseA.supprime").value(true))
            .andExpect(jsonPath("$.contenu[1].reponseA.extrait").value(""))
            .andExpect(jsonPath("$.contenu[1].reponseA.auteur").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.contenu[1].mentionneMoi").value(true));
    }

    // D-88 : accusés de lecture (docs/lot1/accuses-lecture.md).

    ResultActions fil(String qui, String conversation) throws Exception {
        return mvc.perform(get(BASE + "/conversations/" + conversation + "/messages").with(utilisateur(qui, MESSAGERIE))).andExpect(status().isOk());
    }

    void lu(String qui, String conversation) throws Exception {
        mvc.perform(post(BASE + "/conversations/" + conversation + "/lu").with(utilisateur(qui, MESSAGERIE))).andExpect(status().isNoContent());
    }

    ResultActions lectures(String qui, String conversation, String message) throws Exception {
        return mvc.perform(get(BASE + "/conversations/" + conversation + "/messages/" + message + "/lectures").with(utilisateur(qui, MESSAGERIE)));
    }

    int accuses(String conversation) {
        return jdbc.queryForObject(
            "select count(*) from outbox_event where aggregate_id = ? and event_type = 'collaboration.conversation-read.v1'",
            Integer.class,
            conversation
        );
    }

    @Test
    @DisplayName("D-88 direct : ENVOYE puis LU, sur le fil et le dernier message ; accusé publié une fois ; « Lu par » ; 404 indistinct")
    void accusesDeLectureDirects() throws Exception {
        String id = directe(PAUL, AMINATA, "Bonjour");
        String messageId = JsonPath.read(fil(PAUL, id).andReturn().getResponse().getContentAsString(), "$.contenu[0].id");
        fil(PAUL, id)
            .andExpect(jsonPath("$.contenu[0].lecture.etat").value("ENVOYE"))
            .andExpect(jsonPath("$.contenu[0].lecture.lus").value(0))
            .andExpect(jsonPath("$.contenu[0].lecture.destinataires").value(1));
        fil(AMINATA, id).andExpect(jsonPath("$.contenu[0].lecture").doesNotExist());
        mvc.perform(get(BASE + "/conversations").with(utilisateur(PAUL, MESSAGERIE))).andExpect(jsonPath("$.contenu[0].dernierMessage.lecture.etat").value("ENVOYE"));
        mvc.perform(get(BASE + "/conversations").with(utilisateur(AMINATA, MESSAGERIE))).andExpect(jsonPath("$.contenu[0].dernierMessage.lecture").doesNotExist());
        lectures(PAUL, id, messageId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].membre.id").value(AMINATA))
            .andExpect(jsonPath("$[0].lu").value(false))
            .andExpect(jsonPath("$[0].luLe").value(org.hamcrest.Matchers.nullValue()));

        lu(AMINATA, id);
        lu(AMINATA, id);
        assertThat(accuses(id)).isEqualTo(1);

        fil(PAUL, id)
            .andExpect(jsonPath("$.contenu[0].lecture.etat").value("LU"))
            .andExpect(jsonPath("$.contenu[0].lecture.lus").value(1));
        mvc
            .perform(get(BASE + "/conversations/" + id).with(utilisateur(PAUL, MESSAGERIE)))
            .andExpect(jsonPath("$.dernierMessage.lecture.etat").value("LU"))
            .andExpect(jsonPath("$.dernierMessage.lecture.destinataires").value(1));
        lectures(PAUL, id, messageId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].membre.nom").value("Aminata Ngono"))
            .andExpect(jsonPath("$[0].lu").value(true))
            .andExpect(jsonPath("$[0].luLe").isNotEmpty());

        // Auteur seulement ; message inconnu, conversation d'un autre : même 404.
        lectures(AMINATA, id, messageId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COL_MESSAGE_NOT_FOUND"));
        lectures(CLARISSE, id, messageId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COL_MESSAGE_NOT_FOUND"));
        lectures(PAUL, id, UUID.randomUUID().toString()).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COL_MESSAGE_NOT_FOUND"));
        String autre = directe(PAUL, SERGE, "Salut");
        lectures(PAUL, autre, messageId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COL_MESSAGE_NOT_FOUND"));

        // Événement relayé sur collaboration.message, clé = conversation, sans contenu.
        relais.relayerJusquaEnvoi(id);
        for (int i = 0; i < 5 && jdbc.queryForObject("select count(*) from outbox_event where status = 'PENDING'", Integer.class) > 0; i++) {
            relais.relayer();
        }
        String evenement = relais
            .lire("collaboration.message", id)
            .stream()
            .map(r -> r.value())
            .filter(v -> v.contains("collaboration.conversation-read.v1"))
            .findFirst()
            .orElseThrow();
        assertThat((String) JsonPath.read(evenement, "$.type")).isEqualTo("collaboration.conversation-read.v1");
        assertThat((String) JsonPath.read(evenement, "$.data.conversationId")).isEqualTo(id);
        assertThat((String) JsonPath.read(evenement, "$.data.lecteurId")).isEqualTo(AMINATA);
        assertThat((String) JsonPath.read(evenement, "$.data.luJusquA")).isNotBlank();
        assertThat((List<String>) JsonPath.read(evenement, "$.data.destinataires")).containsExactly(PAUL);
        assertThat(evenement).doesNotContain("Bonjour");
    }

    @Test
    @DisplayName("D-88 groupe : LU_PAR_CERTAINS puis LU ; membre parti ne compte plus, membre ajouté après ignoré ; événements sans état")
    void accusesDeLectureEnGroupe() throws Exception {
        String id = JsonPath.read(
            creer(PAUL, "{\"groupe\":true,\"destinataires\":[\"" + AMINATA + "\",\"" + SERGE + "\",\"" + CLARISSE + "\"],\"titre\":\"Comité\",\"contenu\":\"Ordre du jour\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        String messageId = JsonPath.read(fil(PAUL, id).andReturn().getResponse().getContentAsString(), "$.contenu[1].id");
        fil(PAUL, id)
            .andExpect(jsonPath("$.contenu[0].evenement.type").value("GROUPE_CREE"))
            .andExpect(jsonPath("$.contenu[0].lecture").doesNotExist())
            .andExpect(jsonPath("$.contenu[1].lecture.etat").value("ENVOYE"))
            .andExpect(jsonPath("$.contenu[1].lecture.destinataires").value(3));

        lu(AMINATA, id);
        fil(PAUL, id)
            .andExpect(jsonPath("$.contenu[1].lecture.etat").value("LU_PAR_CERTAINS"))
            .andExpect(jsonPath("$.contenu[1].lecture.lus").value(1))
            .andExpect(jsonPath("$.contenu[1].lecture.destinataires").value(3));
        lu(SERGE, id);
        fil(PAUL, id).andExpect(jsonPath("$.contenu[1].lecture.lus").value(2));
        lectures(PAUL, id, messageId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].lu").value(true))
            .andExpect(jsonPath("$[1].lu").value(true))
            .andExpect(jsonPath("$[2].membre.id").value(CLARISSE))
            .andExpect(jsonPath("$[2].lu").value(false));

        // Clarisse retirée sans avoir lu : le message est lu par tous les membres restants.
        mvc.perform(delete(BASE + "/conversations/" + id + "/membres/" + CLARISSE).with(utilisateur(PAUL, MESSAGERIE))).andExpect(status().isNoContent());
        fil(PAUL, id)
            .andExpect(jsonPath("$.contenu[1].lecture.etat").value("LU"))
            .andExpect(jsonPath("$.contenu[1].lecture.lus").value(2))
            .andExpect(jsonPath("$.contenu[1].lecture.destinataires").value(2))
            .andExpect(jsonPath("$.contenu[2].evenement.type").value("MEMBRE_RETIRE"))
            .andExpect(jsonPath("$.contenu[2].lecture").doesNotExist());
        // Réintégrée après l'envoi : elle ne compte pas pour ce message.
        mvc
            .perform(post(BASE + "/conversations/" + id + "/membres").contentType(MediaType.APPLICATION_JSON).content("{\"membres\":[\"" + CLARISSE + "\"]}").with(utilisateur(PAUL, MESSAGERIE)))
            .andExpect(status().isOk());
        fil(PAUL, id).andExpect(jsonPath("$.contenu[1].lecture.etat").value("LU")).andExpect(jsonPath("$.contenu[1].lecture.destinataires").value(2));
        lectures(PAUL, id, messageId).andExpect(jsonPath("$.length()").value(2));
        // Un nouveau message la compte : envoyé, lu par personne.
        envoyer(PAUL, id, "Suite")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.lecture.etat").value("ENVOYE"))
            .andExpect(jsonPath("$.lecture.destinataires").value(3));
        // Les accusés vont aux autres membres, jamais au lecteur.
        assertThat(accuses(id)).isEqualTo(2);
        String donnee = jdbc.queryForObject(
            "select payload from outbox_event where aggregate_id = ? and event_type = 'collaboration.conversation-read.v1' order by created_at, id limit 1",
            String.class,
            id
        );
        assertThat((String) JsonPath.read(donnee, "$.lecteurId")).isEqualTo(AMINATA);
        assertThat((List<String>) JsonPath.read(donnee, "$.destinataires")).containsExactlyInAnyOrder(PAUL, SERGE, CLARISSE);
    }
}

