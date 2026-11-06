package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT_BIS;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INACTIF;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.SUPER_ADMIN;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Public modifiable sur une version publiée (D-103, docs/lot1/lancement-processus.md §11) : PUT /processus/{id}/public,
 * sans nouvelle version, par qui définit les processus de l'unité ; effet immédiat sur « Lancer une demande » et le
 * lancement, instances déjà lancées inchangées ; refus sans droit (403), version retirée (409), saisie (400), anomalie
 * bloquante (422) ; historique PUBLIC_MODIFIE.
 */
class ProcessusPublicModifiableIT extends AbstractProcessusIT {

    private Map<String, Object> lancement() {
        Map<String, Object> l = new HashMap<>();
        l.put("intitule", "Flux de supervision");
        l.put("champs", Map.of("SOURCE", "10.0.0.1", "PORT", 443));
        l.put("designations", Map.of("MISE_EN_OEUVRE", INGENIEUR_RESEAU));
        return l;
    }

    private ObjectNode avecPublic(String code, Map<String, Object> initiateurs) {
        ObjectNode d = definition(code);
        Map<String, Object> i = new HashMap<>(initiateurs);
        i.put("initiateurIntervenant", true);
        d.set("initiateurs", json.valueToTree(i));
        return d;
    }

    private static Map<String, Object> corps(List<String> personnes, String motif) {
        Map<String, Object> c = new HashMap<>();
        c.put("initiateurs", Map.of("regles", List.of("PERSONNES"), "personnes", personnes));
        if (motif != null) {
            c.put("motif", motif);
        }
        return c;
    }

    private List<String> lancables(RequestPostProcessor qui) throws Exception {
        JsonNode l = lire(mvc.perform(get(BASE + "/lancables").with(qui)).andExpect(status().isOk()).andReturn());
        List<String> codes = new ArrayList<>();
        l.get("groupes").forEach(g -> codes.addAll(g.get("processus").findValuesAsText("code")));
        return codes;
    }

    @Test
    @DisplayName("Version publiée : la personne ajoutée le voit et le lance aussitôt, la personne retirée non ; instance déjà lancée inchangée")
    void versionPubliee() throws Exception {
        String id = creer(chef(CHEF_RESEAU), avecPublic("FLUX_PUBLIC", Map.of("regles", List.of("PERSONNES"), "personnes", List.of(AGENT_BIS))));
        publier(chef(CHEF_RESEAU), id);
        assertThat(lancables(agent(AGENT_BIS))).containsExactly(code("FLUX_PUBLIC"));
        assertThat(lancables(agent(AGENT))).doesNotContain(code("FLUX_PUBLIC"));
        JsonNode instance = lire(mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(AGENT_BIS)), lancement())).andExpect(status().isCreated()).andReturn());

        // Droits : modifierPublic pour qui définit, sur une version publiée ; rien d'autre ne change de sens.
        mvc
            .perform(get(BASE + "/" + id).with(chef(CHEF_RESEAU)))
            .andExpect(jsonPath("$.droits.modifierPublic").value(true))
            .andExpect(jsonPath("$.droits.modifier").value(false))
            .andExpect(jsonPath("$.droits.retirer").value(true));
        mvc.perform(get(BASE + "/" + id).with(agent(AGENT_BIS))).andExpect(jsonPath("$.droits.modifierPublic").value(false)).andExpect(jsonPath("$.droits.lancer").value(true));

        // Changement motivé par le chef du pôle : l'agent remplace la chargée d'études.
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), "Ouverture à l'ingénieur d'homologation")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.etat").value("PUBLIEE"))
            .andExpect(jsonPath("$.initiateurs.regles", Matchers.contains("PERSONNES")))
            .andExpect(jsonPath("$.initiateurs.personnes", Matchers.contains(AGENT)))
            .andExpect(jsonPath("$.initiateurs.personnesDetail[0].nomAffiche").value("Serge Ndzana"))
            .andExpect(jsonPath("$.initiateurs.initiateurIntervenant").value(true))
            .andExpect(jsonPath("$.droits.modifierPublic").value(true))
            .andExpect(jsonPath("$.historique[-1].action").value("PUBLIC_MODIFIE"))
            .andExpect(jsonPath("$.historique[-1].acteur.id").value(CHEF_RESEAU))
            .andExpect(
                jsonPath("$.historique[-1].detail").value(
                    "Public de la version 1 modifié : Ouverture à l'ingénieur d'homologation ; avant : PERSONNES (Clarisse Bilong) ; après : PERSONNES (Serge Ndzana)"
                )
            );
        assertThat(jdbc.queryForObject("select count(*) from processus where code = ?", Integer.class, code("FLUX_PUBLIC"))).isEqualTo(1);

        // Effet immédiat : l'agent ajouté le voit et le lance ; la personne retirée ne le voit plus et ne le lance plus.
        assertThat(lancables(agent(AGENT))).containsExactly(code("FLUX_PUBLIC"));
        assertThat(lancables(agent(AGENT_BIS))).doesNotContain(code("FLUX_PUBLIC"));
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(AGENT)), lancement())).andExpect(status().isCreated());
        mvc
            .perform(json(post(BASE + "/" + id + "/instances").with(agent(AGENT_BIS)), lancement()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_INITIATEUR"));
        // L'instance qu'elle a lancée auparavant continue, et elle la suit toujours.
        mvc
            .perform(get(INSTANCES + "/" + instance.get("id").asText()).with(agent(AGENT_BIS)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("EN_COURS"))
            .andExpect(jsonPath("$.lanceur.id").value(AGENT_BIS));

        // Même public renvoyé : rien n'est écrit.
        int lignes = jdbc.queryForObject("select count(*) from processus_historique where processus_id = ?::uuid and instance_id is null", Integer.class, id);
        mvc.perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), "Sans changement"))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from processus_historique where processus_id = ?::uuid and instance_id is null", Integer.class, id)).isEqualTo(lignes);

        // L'administration des référentiels métier (REF_METIER_ADMIN) le règle aussi ; aucun événement n'est émis.
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(administration(SUPER_ADMIN)), Map.of("initiateurs", Map.of("regles", List.of("MEMBRES_UNITE")), "motif", "Retour à l'unité")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.initiateurs.regles", Matchers.contains("MEMBRES_UNITE")))
            .andExpect(jsonPath("$.initiateurs.personnes.length()").value(0))
            .andExpect(jsonPath("$.historique[-1].detail", Matchers.containsString("(au titre de")));
        assertThat(lancables(agent(TECHNICIEN_RESEAU))).contains(code("FLUX_PUBLIC"));
        assertThat(lancables(agent(AGENT))).doesNotContain(code("FLUX_PUBLIC"));
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and event_type <> 'pilotage.process-published.v1' and aggregate_type = 'process'", Integer.class, id)).isZero();
    }

    @Test
    @DisplayName("Refus : sans droit (403), motif absent ou trop court (400), anomalie bloquante (422), version retirée (409)")
    void refus() throws Exception {
        String id = creer(chef(CHEF_RESEAU), avecPublic("FLUX_REFUS", Map.of("regles", List.of("PERSONNES"), "personnes", List.of(AGENT_BIS))));
        publier(chef(CHEF_RESEAU), id);

        // Membre du public, membre de l'unité : ne définissent pas les processus de l'unité.
        for (RequestPostProcessor qui : List.of(agent(AGENT_BIS), agent(TECHNICIEN_RESEAU))) {
            mvc
                .perform(json(put(BASE + "/" + id + "/public").with(qui), corps(List.of(AGENT), "Ouverture")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTERDIT"));
        }
        // Motif obligatoire sur une version publiée, 3 à 500 caractères ; public absent ; saisie au format.
        mvc.perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), null))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("motif"));
        mvc.perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), "ab"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PIL_REQUETE_INVALIDE"));
        mvc.perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), "x".repeat(501)))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("motif"));
        mvc.perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), Map.of("motif", "Ouverture"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("initiateurs"));
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT, " "), "Ouverture")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("initiateurs.personnes[1]"));
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), Map.of("initiateurs", Map.of("regles", List.of("INCONNUE")), "motif", "Ouverture")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("initiateurs.regles[0]"));

        // Anomalies bloquantes du contrôle de publication : refusées sur une version publiée.
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT, INACTIF), "Ouverture")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_ANOMALIES"))
            .andExpect(jsonPath("$.details.anomalies[0].code").value("PERSONNE_INCONNUE"))
            .andExpect(jsonPath("$.details.anomalies[0].element").value("initiateurs.personnes"))
            .andExpect(jsonPath("$.details.anomalies[0].gravite").value("BLOQUANTE"));
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(), "Ouverture")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.details.anomalies[0].code").value("INITIATEURS_INCOMPLETS"));
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), Map.of("initiateurs", Map.of("regles", List.of("UNITES"), "unites", List.of("UNITE_FANTOME")), "motif", "Ouverture")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.details.anomalies[0].code").value("UNITE_INCONNUE"));
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), Map.of("initiateurs", Map.of("regles", List.of("PROFILS"), "profils", List.of("ROLE_FANTOME")), "motif", "Ouverture")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.details.anomalies[0].code").value("PROFIL_INCONNU"));
        // Rien n'a changé.
        mvc.perform(get(BASE + "/" + id).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.initiateurs.personnes", Matchers.contains(AGENT_BIS)));

        // Nouvelle version publiée : la version 1, retirée, a un public figé (409) et plus de droit modifierPublic.
        String v2 = lire(mvc.perform(post(BASE + "/" + id + "/nouvelle-version").with(chef(CHEF_RESEAU))).andExpect(status().isCreated()).andReturn()).get("id").asText();
        publier(chef(CHEF_RESEAU), v2);
        mvc.perform(get(BASE + "/" + id).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.etat").value("RETIREE")).andExpect(jsonPath("$.droits.modifierPublic").value(false));
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), "Ouverture")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_MODIFIABLE"));
        mvc.perform(json(put(BASE + "/" + v2 + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), "Ouverture"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Brouillon : public réglable sans motif, incomplet admis (contrôlé à la publication)")
    void brouillon() throws Exception {
        String id = creer(chef(CHEF_RESEAU), definition("FLUX_BROUILLON"));
        mvc
            .perform(get(BASE + "/" + id).with(chef(CHEF_RESEAU)))
            .andExpect(jsonPath("$.droits.modifier").value(true))
            .andExpect(jsonPath("$.droits.modifierPublic").value(true));
        mvc
            .perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(), null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("BROUILLON"))
            .andExpect(jsonPath("$.initiateurs.regles", Matchers.contains("PERSONNES")))
            .andExpect(jsonPath("$.initiateurs.initiateurIntervenant").value(true))
            .andExpect(jsonPath("$.historique[-1].action").value("PUBLIC_MODIFIE"))
            .andExpect(jsonPath("$.historique[-1].detail").value("Public de la version 1 modifié ; avant : MEMBRES_UNITE ; après : PERSONNES"));
        mvc
            .perform(json(post(BASE + "/" + id + "/publication").with(chef(CHEF_RESEAU)), Map.of("motif", "x")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.details.anomalies[*].code", Matchers.hasItem("INITIATEURS_INCOMPLETS")));
        mvc.perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), "ab"))).andExpect(status().isBadRequest());
        mvc.perform(json(put(BASE + "/" + id + "/public").with(chef(CHEF_RESEAU)), corps(List.of(AGENT), "Réservé à l'ingénieur"))).andExpect(status().isOk());
        publier(chef(CHEF_RESEAU), id);
        assertThat(lancables(agent(AGENT))).containsExactly(code("FLUX_BROUILLON"));
    }
}
