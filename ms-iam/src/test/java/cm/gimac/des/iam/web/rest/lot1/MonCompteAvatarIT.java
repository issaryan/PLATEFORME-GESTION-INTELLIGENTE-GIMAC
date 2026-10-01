package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.domain.compte.Courriel;
import cm.gimac.des.iam.domain.service.TraitementAvatarTest;
import cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * « Mon compte » (docs/lot1/mon-compte.md) de bout en bout, PostgreSQL et Kafka par Testcontainers :
 * photo de profil (contrôle, recadrage, réencodage sans EXIF ni GPS, table avatar_compte, version dans
 * /moi, la console et l'annuaire, journal et iam.avatar-changed.v1 sans l'image, retrait), changement
 * de mot de passe fermant les autres sessions avec courriel d'information, alerte « nouvel appareil »
 * demandée par la passerelle.
 */
class MonCompteAvatarIT extends AbstractIamIT {

    @Autowired
    CourrielsEnMemoire courriels;

    @Autowired
    cm.gimac.des.iam.application.port.out.Annuaire annuaireBean;

    AnnuaireEnMemoire annuaire() {
        return (AnnuaireEnMemoire) annuaireBean;
    }

    static RequestPostProcessor passerelle() {
        return jwt().jwt(j -> j.subject("service-account-gateway-service").claim("azp", "gateway-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
    }

    String creer(String identifiant) throws Exception {
        String corps = """
            {"identifiant":"%s","prenom":"Awa","nom":"Mballa","email":"%s@gimac.test","telephone":"+237600000009","fonction":"Chargée","profils":["ROLE_CHARGE_DDRM"],"envoyerInvitation":true}"""
            .formatted(identifiant, identifiant);
        String reponse = mvc
            .perform(post("/api/v1/comptes").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(reponse, "$.compte.id");
    }

    String envoyer(String sujet, String fichier, int statut) throws Exception {
        byte[] octets = TraitementAvatarTest.ressource(fichier);
        return mvc
            .perform(multipart(HttpMethod.PUT, "/api/v1/moi/avatar").file(new MockMultipartFile("image", "photo.bin", "application/octet-stream", octets)).with(utilisateur(sujet)))
            .andExpect(status().is(statut))
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    @Test
    @DisplayName("Photo : JPEG avec GPS réencodé 256 × 256 sans métadonnées, version partout, audit sans l'image, retrait")
    void photoDeProfil() throws Exception {
        String identifiant = "avatar." + UUID.randomUUID().toString().substring(0, 6);
        String id = creer(identifiant);
        mvc.perform(get("/api/v1/moi").with(utilisateur(id))).andExpect(jsonPath("$.avatarVersion").doesNotExist());

        String version = JsonPath.read(envoyer(id, "gps.jpg", 200), "$.version");
        assertThat(version).matches("[0-9a-f]{16}");
        byte[] stocke = jdbc.queryForObject("select image from avatar_compte where compte_id = ?", byte[].class, id);
        assertThat(TraitementAvatarTest.contient(stocke, "Exif")).isFalse();
        assertThat(TraitementAvatarTest.contient(stocke, "FabricantSecret")).isFalse();
        assertThat(jdbc.queryForObject("select type_contenu from avatar_compte where compte_id = ?", String.class, id)).isEqualTo("image/png");

        mvc.perform(get("/api/v1/moi").with(utilisateur(id))).andExpect(jsonPath("$.avatarVersion").value(version));
        byte[] lue = mvc
            .perform(get("/api/v1/avatars/{id}?v={v}", id, version).with(utilisateur(RSSI)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("immutable")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        BufferedImage image = TraitementAvatarTest.lire(lue);
        assertThat(image.getWidth()).isEqualTo(256);
        assertThat(image.getHeight()).isEqualTo(256);
        assertThat(TraitementAvatarTest.contient(lue, "GPS")).isFalse();
        mvc.perform(get("/api/v1/moi/avatar").with(utilisateur(id))).andExpect(status().isOk());
        mvc
            .perform(get("/api/v1/comptes/{id}", id).with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)))
            .andExpect(jsonPath("$.avatarVersion").value(version));
        mvc.perform(get("/api/v1/interne/collaborateurs/{id}", id).with(jetonDeService("INTERNAL_IAM_READ"))).andExpect(jsonPath("$.avatarVersion").value(version));
        mvc
            .perform(get("/api/v1/interne/collaborateurs?recherche=" + identifiant).with(jetonDeService("INTERNAL_IAM_READ")))
            .andExpect(jsonPath("$.contenu[?(@.id == '" + id + "')].avatarVersion").value(org.hamcrest.Matchers.hasItem(version)));

        mvc
            .perform(get("/api/v1/interne/collaborateurs/avatars?ids=" + id + "&ids=inconnu").with(jetonDeService("INTERNAL_IAM_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['" + id + "']").value(version))
            .andExpect(jsonPath("$.inconnu").doesNotExist());

        // WebP accepté, nouvelle version ; GIF refusé par ses octets, la photo reste.
        String webp = JsonPath.read(envoyer(id, "paysage.webp", 200), "$.version");
        assertThat(webp).isNotEqualTo(version);
        String refus = mvc
            .perform(
                multipart(HttpMethod.PUT, "/api/v1/moi/avatar")
                    .file(new MockMultipartFile("image", "photo.png", "image/png", "GIF89a-pas-une-image".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                    .with(utilisateur(id))
            )
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat((String) JsonPath.read(refus, "$.code")).isEqualTo("IAM_AVATAR_TYPE");
        mvc.perform(get("/api/v1/moi").with(utilisateur(id))).andExpect(jsonPath("$.avatarVersion").value(webp));

        // Journal et événements d'audit, sans l'image.
        assertThat(jdbc.queryForList("select action from journal_administration where cible_id = ? and action like 'AVATAR_%'", String.class, id)).containsExactly(
            "AVATAR_REMPLACEE",
            "AVATAR_REMPLACEE"
        );
        List<String> evenements = jdbc.queryForList("select payload::text from outbox_event where aggregate_id = ? and event_type = 'iam.avatar-changed.v1'", String.class, id);
        assertThat(evenements).hasSize(2).allMatch(p -> p.length() < 400 && p.contains(id) && !p.contains("image\":") && !p.contains("iVBOR"));

        // Retrait : 204, image introuvable, version nulle, trace.
        mvc.perform(delete("/api/v1/moi/avatar").with(utilisateur(id))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/avatars/{id}", id).with(utilisateur(RSSI))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IAM_AVATAR_NOT_FOUND"));
        mvc.perform(get("/api/v1/moi").with(utilisateur(id))).andExpect(jsonPath("$.avatarVersion").doesNotExist());
        mvc.perform(delete("/api/v1/moi/avatar").with(utilisateur(id))).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and event_type = 'iam.avatar-changed.v1'", Integer.class, id)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from avatar_compte where compte_id = ?", Integer.class, id)).isZero();
    }

    @Test
    @DisplayName("Mot de passe : fermerAutresSessions ferme les autres sessions du fournisseur d'identité ; courriel d'information")
    void motDePasseFermerAutresSessions() throws Exception {
        String identifiant = "mdp." + UUID.randomUUID().toString().substring(0, 6);
        String id = creer(identifiant);
        String provisoire = annuaire().definirMotDePasseProvisoire(id);
        String courante = annuaire().ouvrirSession(id, "10.0.0.1");
        annuaire().ouvrirSession(id, "10.0.0.2");
        annuaire().ouvrirSession(id, "10.0.0.3");
        RequestPostProcessor titulaire = jwt().jwt(j -> j.subject(id).claim("acr", "2").claim("name", "Awa Mballa").claim("sid", courante));
        mvc
            .perform(
                post("/api/v1/moi/mot-de-passe")
                    .with(titulaire)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"actuel\":\"" + provisoire + "\",\"nouveau\":\"Nouveau-Mot-2026!\",\"fermerAutresSessions\":true}")
            )
            .andExpect(status().isNoContent())
            .andExpect(header().string(MonCompteResource.EN_TETE_FERMER_AUTRES_SESSIONS, "1"));
        assertThat(annuaire().sessions(id)).extracting(cm.gimac.des.iam.domain.compte.SessionCompte::id).containsExactly(courante);
        Courriel info = courriels.dernierPour(identifiant + "@gimac.test").orElseThrow();
        assertThat(info.objet()).isEqualTo("Votre mot de passe a été modifié");
        assertThat(info.texte()).contains("/des/mon-compte").doesNotContain("Nouveau-Mot-2026!");
        assertThat(jdbc.queryForList("select details from journal_administration where cible_id = ? and action = 'LIBRE_SERVICE_MOT_DE_PASSE'", String.class, id)).containsExactly(
            "autres sessions fermées : 2"
        );
    }

    @Test
    @DisplayName("Alerte « nouvel appareil » : jeton de service de la passerelle seulement, courriel au titulaire, trace")
    void alerteNouvelAppareil() throws Exception {
        String identifiant = "alerte." + UUID.randomUUID().toString().substring(0, 6);
        String id = creer(identifiant);
        String corps = "{\"navigateur\":\"Firefox 131\",\"systeme\":\"Windows 10\",\"type\":\"ORDINATEUR\",\"adresseIp\":\"41.202.10.20\",\"couche\":\"des\",\"le\":\"2026-10-01T08:30:00Z\"}";
        mvc.perform(post("/api/v1/interne/comptes/{id}/alerte-connexion", id).with(jetonDeService("INTERNAL_IAM_READ")).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/interne/comptes/{id}/alerte-connexion", id).with(passerelle()).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isNoContent());
        Courriel alerte = courriels.dernierPour(identifiant + "@gimac.test").orElseThrow();
        assertThat(alerte.objet()).isEqualTo("Nouvelle connexion à votre compte");
        assertThat(alerte.texte()).contains("Firefox 131 sur Windows 10, adresse IP 41.202.10.20, le 01/10/2026 08:30 (UTC)");
        assertThat(jdbc.queryForObject("select count(*) from journal_administration where cible_id = ? and action = 'ALERTE_NOUVEL_APPAREIL'", Integer.class, id)).isEqualTo(1);
        mvc
            .perform(post("/api/v1/interne/comptes/{id}/alerte-connexion", "inconnu").with(passerelle()).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isNotFound());
    }
}
