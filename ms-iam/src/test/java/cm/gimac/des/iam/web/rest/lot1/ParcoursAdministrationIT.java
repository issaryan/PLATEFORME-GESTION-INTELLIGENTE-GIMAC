package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.CleDeSecuriteEmulee;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Administration des comptes et premier accès de bout en bout (D-62), sur PostgreSQL et Kafka réels :
 * création par le super administrateur, première configuration par le titulaire, enrôlement d'une clé
 * de sécurité, réauthentification forte FIDO2 (IAM-07) exigée pour la suppression, journal et
 * événements d'audit relayés.
 */
class ParcoursAdministrationIT extends AbstractIamIT {

    static RequestPostProcessor superAdmin() {
        return utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN);
    }

    static RequestPostProcessor rssi() {
        return utilisateur(RSSI, PERMISSIONS_RSSI);
    }

    String creer(String identifiant, String profil) throws Exception {
        String corps = """
            {"identifiant":"%s","prenom":"Awa","nom":"Mballa","email":"%s@gimac.test","telephone":"+237600000009","profils":["%s"],"envoyerInvitation":true}"""
            .formatted(identifiant, identifiant, profil);
        String reponse = mvc
            .perform(post("/api/v1/comptes").with(superAdmin()).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invitationEnvoyee").value(true))
            .andExpect(jsonPath("$.compte.premierAccesEnAttente").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(reponse, "$.compte.id");
    }

    /** Enrôle une clé émulée pour le sujet, par les routes « Mon compte ». */
    CleDeSecuriteEmulee enroler(String sujet) throws Exception {
        CleDeSecuriteEmulee cle = new CleDeSecuriteEmulee();
        String options = mvc
            .perform(post("/api/v1/moi/cles-securite/defi").with(utilisateur(sujet)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        mvc
            .perform(post("/api/v1/moi/cles-securite").with(utilisateur(sujet)).contentType(MediaType.APPLICATION_JSON).content(cle.enrolement(options, "Clé du bureau")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nom").value("Clé du bureau"));
        return cle;
    }

    String defi(String sujet, String finalite) throws Exception {
        return mvc
            .perform(post("/api/v1/reauthentification/defi").with(utilisateur(sujet)).contentType(MediaType.APPLICATION_JSON).content("{\"purpose\":\"" + finalite + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    @Test
    @DisplayName("Création par le super administrateur, première configuration par le titulaire, journal et événement relayé")
    void creationEtPremierAcces() throws Exception {
        String identifiant = "agent." + UUID.randomUUID().toString().substring(0, 6);
        String id = creer(identifiant, "ROLE_CHARGE_DDRM");

        mvc
            .perform(get("/api/v1/moi").with(utilisateur(id)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.compte.identifiant").value(identifiant))
            .andExpect(jsonPath("$.etapesRestantes").isNotEmpty());
        mvc
            .perform(
                post("/api/v1/moi/premiere-configuration")
                    .with(utilisateur(id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"telephone\":\"+237677000000\",\"langue\":\"en\",\"fonction\":\"Chargée DDRM\",\"charteAcceptee\":true}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.compte.profilComplet").value(true))
            .andExpect(jsonPath("$.compte.langue").value("en"));

        mvc
            .perform(get("/api/v1/comptes/journal").param("compte", id).with(rssi()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu[*].action").value(org.hamcrest.Matchers.hasItems("CREATION", "PREMIERE_CONFIGURATION")));
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ?", String.class, id)).contains("iam.account-administered.v1");

        relais.relayerJusquaEnvoi(id);
        assertThat(relais.lire("iam.account", id)).anySatisfy(r -> {
            assertThat(JsonPath.<String>read(r.value(), "$.type")).isEqualTo("iam.account-administered.v1");
            assertThat(JsonPath.<String>read(r.value(), "$.specversion")).isEqualTo("1.0");
        });
    }

    @Test
    @DisplayName("IAM-07 : suppression refusée sans réauthentification (428), acceptée avec l'assertion FIDO2, jamais rejouable")
    void suppressionAvecReauthentification() throws Exception {
        String id = creer("depart." + UUID.randomUUID().toString().substring(0, 6), "ROLE_SNA");
        mvc.perform(delete("/api/v1/comptes/{id}", id).param("motif", "Départ de l'agent").with(superAdmin())).andExpect(status().is(428));

        CleDeSecuriteEmulee cle = enroler(SUPER_ADMIN);
        String assertion = cle.assertion(defi(SUPER_ADMIN, "ADMIN_SENSITIVE"));
        mvc
            .perform(delete("/api/v1/comptes/{id}", id).param("motif", "Départ de l'agent").header("X-Reauth-Assertion", assertion).with(superAdmin()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/comptes/{id}", id).with(superAdmin())).andExpect(status().isNotFound());

        // L'assertion déjà consommée est refusée.
        String autre = creer("rejeu." + UUID.randomUUID().toString().substring(0, 6), "ROLE_SNA");
        mvc
            .perform(delete("/api/v1/comptes/{id}", autre).param("motif", "Rejeu").header("X-Reauth-Assertion", assertion).with(superAdmin()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_REAUTH_INVALID"));
        // Une assertion émise pour une autre finalité ne vaut pas pour un acte d'administration.
        String autreFinalite = cle.assertion(defi(SUPER_ADMIN, "STATUS_APPROVE"));
        mvc
            .perform(delete("/api/v1/comptes/{id}", autre).param("motif", "Rejeu").header("X-Reauth-Assertion", autreFinalite).with(superAdmin()))
            .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from journal_administration where cible_id = ? and action = 'SUPPRESSION'", Integer.class, id)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vérification pour un service tiers : route interne, assertion liée au sujet et à la finalité")
    void verificationInterne() throws Exception {
        CleDeSecuriteEmulee cle = enroler(RSSI);
        String assertion = cle.assertion(defi(RSSI, "QUITUS_SECURITE_SIGN"));
        String corps = "{\"assertion\":\"%s\",\"sujet\":\"%s\",\"finalite\":\"QUITUS_SECURITE_SIGN\"}";
        mvc
            .perform(post("/api/v1/interne/reauthentifications/verification").with(jetonDeService("INTERNAL_IAM_READ")).contentType(MediaType.APPLICATION_JSON).content(corps.formatted(assertion, RSSI)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valide").value(true));
        mvc
            .perform(post("/api/v1/interne/reauthentifications/verification").with(jetonDeService("INTERNAL_IAM_READ")).contentType(MediaType.APPLICATION_JSON).content(corps.formatted(assertion, RSSI)))
            .andExpect(jsonPath("$.valide").value(false));
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ?", String.class, RSSI)).contains("iam.reauthentication-verified.v1");
    }

    @Test
    @DisplayName("Garde-fous : le RSSI n'attribue pas de profil privilégié, nul ne s'administre soi-même")
    void gardeFous() throws Exception {
        String id = creer("promu." + UUID.randomUUID().toString().substring(0, 6), "ROLE_SNA");
        CleDeSecuriteEmulee cle = enroler(RSSI);
        mvc
            .perform(
                put("/api/v1/comptes/{id}/profils", id)
                    .with(rssi())
                    .header("X-Reauth-Assertion", cle.assertion(defi(RSSI, "ADMIN_SENSITIVE")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"profils\":[\"ROLE_SUPER_ADMIN\"],\"motif\":\"Promotion\"}")
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_PRIVILEGED_PROFILE"));
        mvc
            .perform(post("/api/v1/comptes/{id}/desactivation", RSSI).with(rssi()).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Essai\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_SELF_ADMINISTRATION"));
        mvc
            .perform(
                post("/api/v1/comptes/{id}/actions-requises", id)
                    .with(rssi())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"actions\":[\"MOT_DE_PASSE\",\"SECOND_FACTEUR\"],\"parCourriel\":true,\"motif\":\"Revue des accès\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actionsRequises").value(org.hamcrest.Matchers.hasItems("MOT_DE_PASSE", "SECOND_FACTEUR")));
    }

    @Test
    @DisplayName("Politique de sécurité : appliquée avec réauthentification et motif, planchers imposés")
    void politique() throws Exception {
        CleDeSecuriteEmulee cle = enroler(SUPER_ADMIN);
        String politique =
            "{\"longueurMinimale\":14,\"majusculesMin\":1,\"minusculesMin\":1,\"chiffresMin\":1,\"speciauxMin\":1,\"historique\":5,\"expirationJours\":90,\"interditIdentifiant\":true,\"otpChiffres\":6,\"otpPeriodeSecondes\":30,\"otpAlgorithme\":\"HmacSHA256\",\"sessionInactiviteMinutes\":30,\"sessionDureeMaxHeures\":10,\"forceBruteActive\":true,\"forceBruteEchecsMax\":5,\"forceBruteAttenteMinutes\":15}";
        mvc
            .perform(
                put("/api/v1/configuration/politique-securite")
                    .with(superAdmin())
                    .header("X-Reauth-Assertion", cle.assertion(defi(SUPER_ADMIN, "ADMIN_SENSITIVE")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"politique\":" + politique + ",\"motif\":\"Revue annuelle\"}")
            )
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/configuration/politique-securite").with(rssi())).andExpect(jsonPath("$.longueurMinimale").value(14));
        mvc
            .perform(
                put("/api/v1/configuration/politique-securite")
                    .with(superAdmin())
                    .header("X-Reauth-Assertion", cle.assertion(defi(SUPER_ADMIN, "ADMIN_SENSITIVE")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"politique\":" + politique.replace("\"longueurMinimale\":14", "\"longueurMinimale\":8") + ",\"motif\":\"Affaiblir\"}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_POLICY_INVALID"));
    }
}
