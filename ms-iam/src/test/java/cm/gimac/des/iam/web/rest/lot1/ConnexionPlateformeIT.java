package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.domain.compte.Courriel;
import cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Connexion et configuration des comptes sur la plateforme (D-65) de bout en bout, sur PostgreSQL et
 * Kafka réels (Testcontainers), annuaire et messagerie de substitution : invitation par courriel de la
 * plateforme, nouveau mot de passe par lien à usage unique, état de connexion lu par la passerelle,
 * assistant de premier accès en acr 1 (mot de passe, code TOTP, fiche et charte), mot de passe oublié.
 * Jetons conservés hachés, secrets TOTP chiffrés, journal et événements d'audit à chaque acte.
 */
class ConnexionPlateformeIT extends AbstractIamIT {

    @Autowired
    CourrielsEnMemoire courriels;

    static RequestPostProcessor passerelle() {
        return jwt().jwt(j -> j.subject("service-account-gateway-service").claim("azp", "gateway-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
    }

    /** Jeton de la session restreinte du premier accès (octroi direct à un facteur). */
    static RequestPostProcessor premierAcces(String sujet) {
        return jwt().jwt(j -> j.subject(sujet).claim("acr", "1").claim("name", "Titulaire"));
    }

    String jeton(String adresse) {
        Courriel c = courriels.dernierPour(adresse).orElseThrow();
        int i = c.texte().indexOf("jeton=");
        return c.texte().substring(i + 6, i + 6 + 43);
    }

    String etat(String id) throws Exception {
        return mvc.perform(get("/api/v1/interne/comptes/{id}/etat-connexion", id).with(passerelle())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Invitation, nouveau mot de passe, premier accès en acr 1 jusqu'à l'état « configuré », puis mot de passe oublié")
    void parcoursComplet() throws Exception {
        String identifiant = "connexion." + UUID.randomUUID().toString().substring(0, 6);
        String adresse = identifiant + "@gimac.test";
        String corps = """
            {"identifiant":"%s","prenom":"Awa","nom":"Mballa","email":"%s","telephone":"+237600000009","profils":["ROLE_CHARGE_DDRM"],"envoyerInvitation":true}"""
            .formatted(identifiant, adresse);
        String cree = mvc
            .perform(post("/api/v1/comptes").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invitationEnvoyee").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String id = JsonPath.read(cree, "$.compte.id");

        // Courriel de la plateforme : lien vers le département, jeton conservé haché seulement.
        String invitation = jeton(adresse);
        assertThat(courriels.dernierPour(adresse).orElseThrow().texte()).contains("https://des.gimac.test/connexion/nouveau-mot-de-passe?jeton=" + invitation);
        assertThat(jdbc.queryForObject("select count(*) from jeton_lien where compte_id = ? and finalite = 'INVITATION' and empreinte <> ?", Integer.class, id, invitation)).isEqualTo(1);
        assertThat(JsonPath.<java.util.List<String>>read(etat(id), "$.actionsRequises")).containsExactly("MOT_DE_PASSE", "SECOND_FACTEUR", "FICHE", "CHARTE");

        // Politique refusée : le lien reste utilisable ; puis premier mot de passe, lien consommé.
        String nouveau = "/api/v1/interne/connexion/nouveau-mot-de-passe";
        mvc
            .perform(post(nouveau).with(passerelle()).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"" + invitation + "\",\"motDePasse\":\"court\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_PASSWORD_POLICY"))
            .andExpect(jsonPath("$.violations").isArray());
        mvc.perform(post(nouveau).with(passerelle()).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"" + invitation + "\",\"motDePasse\":\"Premier-Mot-2026!\"}")).andExpect(status().isNoContent());
        mvc
            .perform(post(nouveau).with(passerelle()).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"" + invitation + "\",\"motDePasse\":\"Autre-Mot-2026!\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CNX_JETON_INVALIDE"));
        String apres = etat(id);
        assertThat(JsonPath.<Boolean>read(apres, "$.actif")).isTrue();
        assertThat(JsonPath.<Boolean>read(apres, "$.secondFacteur")).isFalse();
        assertThat(JsonPath.<java.util.List<String>>read(apres, "$.actionsRequises")).containsExactly("SECOND_FACTEUR", "FICHE", "CHARTE");

        // Session restreinte (acr 1) : l'assistant seulement, aucune route métier.
        mvc.perform(get("/api/v1/moi").with(premierAcces(id))).andExpect(status().isOk()).andExpect(jsonPath("$.etapesRestantes[0]").value("SECOND_FACTEUR"));
        mvc.perform(get("/api/v1/moi/sessions").with(premierAcces(id))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/comptes").with(jwt().jwt(j -> j.subject(id).claim("acr", "1")).authorities(new SimpleGrantedAuthority("IAM_ACCOUNT_READ")))).andExpect(status().isForbidden());

        // Changement de mot de passe : actuel vérifié.
        mvc
            .perform(post("/api/v1/moi/mot-de-passe").with(premierAcces(id)).contentType(MediaType.APPLICATION_JSON).content("{\"actuel\":\"faux\",\"nouveau\":\"Deuxieme-Mot-2026!\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_PASSWORD_INVALID"));
        mvc
            .perform(post("/api/v1/moi/mot-de-passe").with(premierAcces(id)).contentType(MediaType.APPLICATION_JSON).content("{\"actuel\":\"Premier-Mot-2026!\",\"nouveau\":\"Deuxieme-Mot-2026!\"}"))
            .andExpect(status().isNoContent());

        // Code TOTP : secret gardé chiffré, code faux refusé, code juste enregistré.
        String enrolement = mvc
            .perform(post("/api/v1/moi/second-facteur/totp/enrolement").with(premierAcces(id)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/GIMAC%20DES:" + identifiant + "?secret=")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        String secret = JsonPath.read(enrolement, "$.secret");
        String enrolementId = JsonPath.read(enrolement, "$.enrolementId");
        assertThat(jdbc.queryForObject("select secret_chiffre from enrolement_totp where id = ?::uuid", String.class, enrolementId)).doesNotContain(secret);
        String code = codeTotp(JsonPath.read(enrolement, "$.uri"));
        String faux = code.equals("000000") ? "111111" : "000000";
        String totp = "{\"enrolementId\":\"%s\",\"code\":\"%s\",\"libelle\":\"Téléphone\"}";
        mvc
            .perform(post("/api/v1/moi/second-facteur/totp").with(premierAcces(id)).contentType(MediaType.APPLICATION_JSON).content(totp.formatted(enrolementId, faux)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_TOTP_CODE_INVALID"));
        mvc.perform(post("/api/v1/moi/second-facteur/totp").with(premierAcces(id)).contentType(MediaType.APPLICATION_JSON).content(totp.formatted(enrolementId, code))).andExpect(status().isNoContent());
        mvc
            .perform(post("/api/v1/moi/second-facteur/totp").with(premierAcces(id)).contentType(MediaType.APPLICATION_JSON).content(totp.formatted(enrolementId, code)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_TOTP_ENROLMENT_EXPIRED"));

        // Fiche et charte : plus aucune étape, la passerelle ouvrira une session complète.
        mvc
            .perform(
                post("/api/v1/moi/premiere-configuration")
                    .with(premierAcces(id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"telephone\":\"+237677000000\",\"langue\":\"es\",\"charteAcceptee\":true}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etapesRestantes").isEmpty());
        String configure = etat(id);
        assertThat(JsonPath.<Boolean>read(configure, "$.secondFacteur")).isTrue();
        assertThat(JsonPath.<java.util.List<String>>read(configure, "$.actionsRequises")).isEmpty();

        // Mot de passe oublié : 202 dans tous les cas, courriel en espagnol (langue du compte), 30 min.
        String oublie = "/api/v1/interne/connexion/mot-de-passe-oublie";
        mvc.perform(post(oublie).with(passerelle()).contentType(MediaType.APPLICATION_JSON).content("{\"identifiant\":\"inconnu\",\"couche\":\"des\"}")).andExpect(status().isAccepted());
        mvc.perform(post(oublie).with(passerelle()).contentType(MediaType.APPLICATION_JSON).content("{\"identifiant\":\"" + adresse + "\",\"couche\":\"console\"}")).andExpect(status().isAccepted());
        Courriel reinitialisation = courriels.dernierPour(adresse).orElseThrow();
        assertThat(reinitialisation.objet()).isEqualTo("Restablecimiento de su contraseña");
        assertThat(reinitialisation.texte()).contains("https://console.gimac.test/connexion/nouveau-mot-de-passe?jeton=").contains("30 minutos");
        mvc
            .perform(post(nouveau).with(passerelle()).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"" + jeton(adresse) + "\",\"motDePasse\":\"Troisieme-Mot-2026!\"}"))
            .andExpect(status().isNoContent());

        mvc
            .perform(get("/api/v1/comptes/journal").param("compte", id).param("taille", "50").with(utilisateur(RSSI, PERMISSIONS_RSSI)))
            .andExpect(
                jsonPath("$.contenu[*].action").value(
                    org.hamcrest.Matchers.hasItems(
                        "CREATION",
                        "INVITATION",
                        "NOUVEAU_MOT_DE_PASSE",
                        "LIBRE_SERVICE_MOT_DE_PASSE",
                        "LIBRE_SERVICE_SECOND_FACTEUR",
                        "PREMIERE_CONFIGURATION",
                        "MOT_DE_PASSE_OUBLIE"
                    )
                )
            );
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and event_type = 'iam.account-administered.v1'", Integer.class, id)).isGreaterThanOrEqualTo(7);
        // Aucun secret dans le journal ni dans les événements.
        assertThat(jdbc.queryForList("select coalesce(details, '') from journal_administration where cible_id = ?", String.class, id)).noneMatch(d -> d.contains("Mot-2026") || d.contains(secret));
        assertThat(jdbc.queryForList("select payload::text from outbox_event where aggregate_id = ?", String.class, id)).noneMatch(p -> p.contains("Mot-2026") || p.contains(secret) || p.contains(invitation));
    }

    @Test
    @DisplayName("Routes internes de la connexion : un autre service, même porteur de INTERNAL_IAM_READ, est refusé")
    void cloisonnement() throws Exception {
        mvc
            .perform(get("/api/v1/interne/comptes/{id}/etat-connexion", RSSI).with(jetonDeService("INTERNAL_IAM_READ")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/interne/comptes/{id}/etat-connexion", "inconnu").with(passerelle())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IAM_ACCOUNT_NOT_FOUND"));
    }
}
