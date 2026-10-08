package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DES;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.EnumMap;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Définitions des processus d'unité (D-87 §1) : droits (PROCESSUS_DEFINIR couvrant l'unité, administration,
 * lecture par l'unité), brouillon, contrôle (Kahn, prédécesseurs, intervenants, couverture des profils),
 * publication (anomalies, avertissements levés par motif), versions, retrait, suppression ; événement publié.
 */
class ProcessusDefinitionIT extends AbstractProcessusIT {

    @Test
    @DisplayName("Droits : définir (PROCESSUS_DEFINIR ou administration), lire (unité), brouillon caché aux membres")
    void droits() throws Exception {
        ObjectNode d = definition("FLUX_DROITS");
        mvc.perform(json(post(BASE).with(agent(TECHNICIEN_RESEAU)), d)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTERDIT"));
        mvc.perform(json(post(BASE).with(chef(CHEF_DDRM)), d)).andExpect(status().isForbidden());
        mvc.perform(json(post(BASE).with(utilisateur(CHEF_RESEAU)), d)).andExpect(status().isForbidden());
        // PROCESSUS_DEFINIR absent de la matrice (réponse de MS-IAM sans l'action) : AUCUNE.
        Map<Pouvoir, Portee> sans = new EnumMap<>(OrganisationStubAdapter.MATRICE_RESPONSABLE);
        sans.remove(Pouvoir.PROCESSUS_DEFINIR);
        organisation.definirPouvoirs(CHEF_RESEAU, sans);
        mvc.perform(json(post(BASE).with(chef(CHEF_RESEAU)), d)).andExpect(status().isForbidden());
        organisation.definirPouvoirs(CHEF_RESEAU, OrganisationStubAdapter.MATRICE_RESPONSABLE);

        String id = creer(chef(CHEF_RESEAU), d);
        mvc.perform(json(post(BASE).with(chef(CHEF_RESEAU)), d)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_CODE_PRIS"));
        // Le chef du DES couvre le pôle réseau (sous-unité) ; l'administration aussi.
        mvc.perform(get(BASE + "/" + id).with(chef(CHEF_DES))).andExpect(status().isOk()).andExpect(jsonPath("$.droits.modifier").value(true));
        mvc.perform(get(BASE + "/" + id).with(administration(AGENT))).andExpect(status().isOk());
        // D-94 : l'administration des comptes (IAM_PROFILE_EDIT) seule n'ouvre pas les processus des autres unités.
        mvc.perform(get(BASE + "/" + id).with(administrationDesComptes(AGENT))).andExpect(status().isForbidden());
        mvc.perform(get(BASE).with(administrationDesComptes(AGENT))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(json(post(BASE).with(administrationDesComptes(AGENT)), definition("PAR_LE_RSSI"))).andExpect(status().isForbidden());
        mvc
            .perform(get(BASE + "/portee").with(administrationDesComptes(AGENT)))
            .andExpect(jsonPath("$.portee").value("MES_UNITES"))
            .andExpect(jsonPath("$.auTitreDe.length()").value(0));
        // Brouillon : invisible des simples membres.
        mvc.perform(get(BASE + "/" + id).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isForbidden());
        mvc.perform(get(BASE).with(agent(TECHNICIEN_RESEAU))).andExpect(jsonPath("$.length()").value(0));
        publier(chef(CHEF_RESEAU), id);
        mvc
            .perform(get(BASE + "/" + id).with(agent(TECHNICIEN_RESEAU)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.droits.lancer").value(true))
            .andExpect(jsonPath("$.droits.modifier").value(false));
        mvc.perform(get(BASE).with(agent(TECHNICIEN_RESEAU))).andExpect(jsonPath("$[0].code").value("FLUX_DROITS")).andExpect(jsonPath("$[0].nombreEtapes").value(4));
        mvc.perform(get(BASE + "/" + id).with(agent(AGENT))).andExpect(status().isForbidden());
        mvc.perform(get(BASE).with(agent(AGENT))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(BASE).param("q", "flux").param("etat", "PUBLIEE").with(administration(AGENT))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(BASE).param("unite", "POLE_RESEAU").with(chef(CHEF_DES))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(BASE).param("etat", "INCONNU").with(chef(CHEF_DES))).andExpect(status().isBadRequest());
        // Administration : toute unité.
        ObjectNode ddrm = definition("FLUX_DDRM");
        ddrm.put("unite", "DDRM");
        creer(administration(AGENT), ddrm);
        mvc.perform(json(post(BASE).with(utilisateur(AGENT, "PIL_ACTIVITY_OWN")), definition("SANS_DROIT"))).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/" + java.util.UUID.randomUUID()).with(agent(AGENT))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTROUVABLE"));
    }

    @Test
    @DisplayName("Contrôle : cycle (Kahn), prédécesseur inconnu, intervenant incomplet, remplacement, signature, profil inconnu, couverture")
    void controle() throws Exception {
        ObjectNode d = definition("FLUX_CONTROLE");
        ArrayNode etapes = (ArrayNode) d.get("etapes");
        ((ArrayNode) etapes.get(0).get("predecesseurs")).add("RECETTE");
        ((ObjectNode) etapes.get(1)).putNull("intervenant");
        ((ObjectNode) etapes.get(2).get("intervenant")).putNull("remplacement");
        ((ArrayNode) etapes.get(3).get("predecesseurs")).add("FANTOME");
        ((ArrayNode) etapes.get(3).at("/signatures/0/porte")).add("INEXISTANT");
        String id = creer(chef(CHEF_RESEAU), d);
        JsonNode c = lire(mvc.perform(post(BASE + "/" + id + "/controle").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(c.get("publiable").asBoolean()).isFalse();
        assertThat(c.get("anomalies").findValuesAsText("code")).contains(
            "CYCLE",
            "AUCUNE_ETAPE_INITIALE",
            "INTERVENANT_ABSENT",
            "REMPLACEMENT_ABSENT",
            "PREDECESSEUR_INCONNU",
            "SIGNATURE_PORTEE_INVALIDE"
        );
        mvc
            .perform(json(post(BASE + "/" + id + "/publication").with(chef(CHEF_RESEAU)), Map.of("motif", "Essai")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_ANOMALIES"))
            .andExpect(jsonPath("$.details.anomalies[*].code", Matchers.hasItem("CYCLE")));

        // Profil inconnu (bloquant) ; profil sans porteur actif (avertissement levable par motif).
        ObjectNode e = definition("FLUX_CONTROLE");
        ((ObjectNode) e.get("etapes").get(1).get("intervenant")).put("profil", "ROLE_INEXISTANT");
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), e)).andExpect(status().isOk());
        c = lire(mvc.perform(post(BASE + "/" + id + "/controle").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(c.get("anomalies").findValuesAsText("code")).containsExactly("PROFIL_INCONNU");
        ((ObjectNode) e.get("etapes").get(1).get("intervenant")).put("profil", "ROLE_POLE_SYSTEME");
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), e)).andExpect(status().isOk());
        c = lire(mvc.perform(post(BASE + "/" + id + "/controle").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(c.get("publiable").asBoolean()).isTrue();
        assertThat(c.at("/anomalies/0/code").asText()).isEqualTo("COUVERTURE_PROFIL");
        assertThat(c.at("/anomalies/0/gravite").asText()).isEqualTo("LEVABLE");
        mvc
            .perform(json(post(BASE + "/" + id + "/publication").with(chef(CHEF_RESEAU)), Map.of("motif", "Essai")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_AVERTISSEMENTS"));
        mvc
            .perform(json(post(BASE + "/" + id + "/publication").with(chef(CHEF_RESEAU)), Map.of("leverAvertissements", true)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("motif"));
        mvc
            .perform(json(post(BASE + "/" + id + "/publication").with(chef(CHEF_RESEAU)), Map.of("leverAvertissements", true, "motif", "Recrutement en cours au pôle système")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("PUBLIEE"))
            .andExpect(jsonPath("$.avertissementsLeves").value(true));
        JsonNode publie = charge("pilotage.process-published.v1", id);
        assertThat(publie.get("avertissementsLeves").asBoolean()).isTrue();
        assertThat(publie.get("unite").asText()).isEqualTo("POLE_RESEAU");

        // Saisie mal formée : 400 avec le chemin du champ.
        ObjectNode f = definition("FLUX_FORMAT");
        ((ObjectNode) f.get("etapes").get(0)).put("code", "minuscules");
        mvc.perform(json(post(BASE).with(chef(CHEF_RESEAU)), f)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("etapes[0].code"));
        ObjectNode g = definition("FLUX_FORMAT");
        ((ObjectNode) g.get("etapes").get(1).get("intervenant")).put("mode", "INCONNU");
        mvc.perform(json(post(BASE).with(chef(CHEF_RESEAU)), g)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("etapes[1].intervenant.mode"));
        ObjectNode h = definition("flux");
        mvc.perform(json(post(BASE).with(chef(CHEF_RESEAU)), h)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("code"));
        ObjectNode u = definition("FLUX_UNITE");
        u.put("unite", "UNITE_FANTOME");
        mvc.perform(json(post(BASE).with(administration(AGENT)), u)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_UNITE_INCONNUE"));
    }

    @Test
    @DisplayName("Versions : brouillon v+1, une seule en brouillon, publication qui retire la précédente, retrait, suppression")
    void versions() throws Exception {
        String v1 = publie("FLUX_VERSIONS", CHEF_RESEAU);
        mvc.perform(json(put(BASE + "/" + v1).with(chef(CHEF_RESEAU)), definition("FLUX_VERSIONS"))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_ETAT"));
        mvc.perform(delete(BASE + "/" + v1).with(chef(CHEF_RESEAU))).andExpect(status().isConflict());
        mvc.perform(post(BASE + "/" + v1 + "/nouvelle-version").with(agent(TECHNICIEN_RESEAU))).andExpect(status().isForbidden());
        JsonNode v2 = lire(mvc.perform(post(BASE + "/" + v1 + "/nouvelle-version").with(chef(CHEF_RESEAU))).andExpect(status().isCreated()).andReturn());
        assertThat(v2.get("version").asInt()).isEqualTo(2);
        assertThat(v2.get("etat").asText()).isEqualTo("BROUILLON");
        assertThat(v2.get("versions").findValuesAsText("version")).containsExactly("2", "1");
        mvc.perform(post(BASE + "/" + v1 + "/nouvelle-version").with(chef(CHEF_RESEAU))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_BROUILLON_EXISTANT"));
        String id2 = v2.get("id").asText();
        ObjectNode d = definition("FLUX_VERSIONS");
        d.put("libelle", "Ouverture d'un flux réseau (v2)");
        d.put("unite", "POLE_SYSTEME");
        mvc.perform(json(put(BASE + "/" + id2).with(chef(CHEF_RESEAU)), d)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("unite"));
        d.put("unite", "POLE_RESEAU");
        mvc.perform(json(put(BASE + "/" + id2).with(chef(CHEF_RESEAU)), d)).andExpect(status().isOk()).andExpect(jsonPath("$.libelle").value("Ouverture d'un flux réseau (v2)"));
        publier(chef(CHEF_RESEAU), id2);
        mvc.perform(get(BASE + "/" + v1).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.etat").value("RETIREE")).andExpect(jsonPath("$.motifRetrait").value("Remplacée par la version 2"));
        assertThat(charge("pilotage.process-published.v1", id2).get("versionRetiree").asText()).isEqualTo(v1);
        // Une version retirée ne se lance plus.
        mvc
            .perform(json(post(BASE + "/" + v1 + "/instances").with(agent(TECHNICIEN_RESEAU)), Map.of("intitule", "x", "designations", Map.of("MISE_EN_OEUVRE", INGENIEUR_RESEAU))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_PUBLIE"));
        mvc.perform(json(post(BASE + "/" + id2 + "/retrait").with(chef(CHEF_RESEAU)), Map.of())).andExpect(status().isBadRequest());
        mvc
            .perform(json(post(BASE + "/" + id2 + "/retrait").with(chef(CHEF_RESEAU)), Map.of("motif", "Procédure abandonnée")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("RETIREE"))
            .andExpect(jsonPath("$.historique[*].action", Matchers.hasItems("VERSION_CREEE", "MODIFIE", "PUBLIE", "RETIRE")));
        mvc.perform(json(post(BASE + "/" + id2 + "/retrait").with(chef(CHEF_RESEAU)), Map.of("motif", "Encore"))).andExpect(status().isConflict());
        JsonNode v3 = lire(mvc.perform(post(BASE + "/" + id2 + "/nouvelle-version").with(chef(CHEF_RESEAU))).andExpect(status().isCreated()).andReturn());
        assertThat(v3.get("version").asInt()).isEqualTo(3);
        mvc.perform(delete(BASE + "/" + v3.get("id").asText()).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/" + v3.get("id").asText()).with(chef(CHEF_RESEAU))).andExpect(status().isNoContent());
        assertThat(compter("select count(*) from processus where code = 'FLUX_VERSIONS'")).isEqualTo(2);
        // Brouillon lancé : refusé.
        String brouillon = creer(chef(CHEF_RESEAU), definition("FLUX_BROUILLON"));
        mvc
            .perform(json(post(BASE + "/" + brouillon + "/instances").with(agent(TECHNICIEN_RESEAU)), Map.of("intitule", "x")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_PUBLIE"));
    }
}
