package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT_BIS;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DES;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CORRESPONDANT;
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

import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Public d'un processus (D-100, docs/lot1/lancement-processus.md §10) : défini par son créateur, il fait seul foi pour
 * « Lancer une demande », le lancement, la lecture d'une version publiée hors de l'unité et {@code droits.lancer}. Un
 * administrateur (REF_METIER_ADMIN, PROCESSUS_LANCER à TOUTES) ne voit plus un processus réservé ; une personne désignée
 * le voit et le lance ; MEMBRES_UNITE couvre les responsables de l'unité ; anomalies de publication de la règle PERSONNES.
 */
class ProcessusPublicIT extends AbstractProcessusIT {

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

    private List<String> lancables(RequestPostProcessor qui) throws Exception {
        JsonNode l = lire(mvc.perform(get(BASE + "/lancables").with(qui)).andExpect(status().isOk()).andReturn());
        List<String> codes = new ArrayList<>();
        l.get("groupes").forEach(g -> codes.addAll(g.get("processus").findValuesAsText("code")));
        return codes;
    }

    @Test
    @DisplayName("Réservé à une personne désignée : l'administration ne le voit ni ne le lance, la personne désignée le voit et le lance")
    void personneDesignee() throws Exception {
        String reserve = creer(chef(CHEF_RESEAU), avecPublic("FLUX_RESERVE", Map.of("regles", List.of("PERSONNES"), "personnes", List.of(AGENT_BIS))));
        publier(chef(CHEF_RESEAU), reserve);
        String ouvert = publie("FLUX_OUVERT", CHEF_RESEAU);

        // La vue de la définition nomme les personnes désignées.
        mvc
            .perform(get(BASE + "/" + reserve).with(chef(CHEF_RESEAU)))
            .andExpect(jsonPath("$.initiateurs.regles", Matchers.contains("PERSONNES")))
            .andExpect(jsonPath("$.initiateurs.personnes", Matchers.contains(AGENT_BIS)))
            .andExpect(jsonPath("$.initiateurs.personnesDetail[0].id").value(AGENT_BIS))
            .andExpect(jsonPath("$.initiateurs.personnesDetail[0].nomAffiche").value("Clarisse Bilong"))
            .andExpect(jsonPath("$.droits.lancer").value(false))
            .andExpect(jsonPath("$.droits.retirer").value(true));
        mvc
            .perform(get(BASE).param("etat", "PUBLIEE").with(chef(CHEF_RESEAU)))
            .andExpect(jsonPath("$[?(@.code == \'" + code("FLUX_RESERVE") + "\')].initiateurs.personnesDetail[0].nomAffiche", Matchers.contains("Clarisse Bilong")));

        // Super administrateur (REF_METIER_ADMIN) : ni dans « Lancer une demande », ni lançable ; il lit et retire toujours.
        RequestPostProcessor admin = administration(SUPER_ADMIN);
        assertThat(lancables(admin)).doesNotContain(code("FLUX_RESERVE"), code("FLUX_OUVERT"));
        mvc.perform(get(BASE + "/" + reserve).with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.droits.lancer").value(false)).andExpect(jsonPath("$.droits.retirer").value(true));
        mvc
            .perform(json(post(BASE + "/" + reserve + "/instances").with(admin), lancement()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_NON_INITIATEUR"))
            .andExpect(jsonPath("$.message").value("Ce processus ne peut être lancé que par la personne désignée par son créateur"))
            .andExpect(jsonPath("$.details.personnes", Matchers.contains(AGENT_BIS)));

        // PROCESSUS_LANCER à TOUTES : pilotage partout, lancement nulle part hors du public.
        organisation.definirPouvoirs(AGENT, Map.of(Pouvoir.PROCESSUS_LANCER, Portee.TOUTES));
        assertThat(lancables(agent(AGENT))).isEmpty();
        mvc.perform(json(post(BASE + "/" + reserve + "/instances").with(agent(AGENT)), lancement())).andExpect(status().isForbidden());
        mvc.perform(json(post(BASE + "/" + ouvert + "/instances").with(agent(AGENT)), lancement())).andExpect(status().isForbidden());

        // Membres et chef du pôle : hors du public de ce processus (le leur reste ouvert).
        assertThat(lancables(agent(TECHNICIEN_RESEAU))).contains(code("FLUX_OUVERT")).doesNotContain(code("FLUX_RESERVE"));
        mvc.perform(json(post(BASE + "/" + reserve + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement())).andExpect(status().isForbidden());
        mvc.perform(json(post(BASE + "/" + reserve + "/instances").with(chef(CHEF_RESEAU)), lancement())).andExpect(status().isForbidden());

        // La personne désignée, d'un autre service : le voit, lit la définition publiée et le lance.
        assertThat(lancables(agent(AGENT_BIS))).containsExactly(code("FLUX_RESERVE"));
        // D-106 : au seul titre du public, la version publiée seulement, sans historique de la définition ni contrôle.
        mvc
            .perform(get(BASE + "/" + reserve).with(agent(AGENT_BIS)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.droits.lancer").value(true))
            .andExpect(jsonPath("$.droits.modifierPublic").value(false))
            .andExpect(jsonPath("$.versions.length()").value(1))
            .andExpect(jsonPath("$.historique").isEmpty());
        mvc.perform(post(BASE + "/" + reserve + "/controle").with(agent(AGENT_BIS))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_PROCESSUS_INTERDIT"));
        mvc.perform(get(BASE + "/" + reserve).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.historique").isNotEmpty());
        mvc.perform(get(BASE + "/" + ouvert).with(agent(AGENT_BIS))).andExpect(status().isForbidden());
        JsonNode i = lire(mvc.perform(json(post(BASE + "/" + reserve + "/instances").with(agent(AGENT_BIS)), lancement())).andExpect(status().isCreated()).andReturn());
        assertThat(i.at("/lanceur/id").asText()).isEqualTo(AGENT_BIS);
        assertThat(i.get("historique").findValuesAsText("detail").get(0)).doesNotContain("au titre de");

        // Le pilotage reste à l'administration : elle suspend l'instance lancée par la personne désignée.
        mvc
            .perform(json(post(INSTANCES + "/" + i.get("id").asText() + "/suspension").with(admin), Map.of("motif", "Gel")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("SUSPENDUE"));
    }

    @Test
    @DisplayName("Réservé à des profils : seuls leurs porteurs actifs le voient ; un administrateur sans le profil, non")
    void profils() throws Exception {
        String id = creer(chef(CHEF_RESEAU), avecPublic("FLUX_CONFORMITE", Map.of("regles", List.of("PROFILS"), "profils", List.of("ROLE_CONFORMITE"))));
        publier(chef(CHEF_RESEAU), id);
        assertThat(lancables(administration(SUPER_ADMIN))).doesNotContain(code("FLUX_CONFORMITE"));
        assertThat(lancables(agent(AGENT_BIS))).doesNotContain(code("FLUX_CONFORMITE"));
        organisation.attribuerProfils(AGENT_BIS, "ROLE_CONFORMITE");
        assertThat(lancables(agent(AGENT_BIS))).containsExactly(code("FLUX_CONFORMITE"));
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(AGENT_BIS)), lancement())).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("MEMBRES_UNITE : membres et responsables de l'unité (chef qui la dirige ou dirige une unité parente), pas les autres chefs")
    void membresEtResponsables() throws Exception {
        String id = publie("FLUX_UNITE", CHEF_RESEAU);
        mvc.perform(get(BASE + "/" + id).with(chef(CHEF_RESEAU))).andExpect(jsonPath("$.initiateurs.regles", Matchers.contains("MEMBRES_UNITE"))).andExpect(jsonPath("$.initiateurs.personnes.length()").value(0));
        // Le chef du DES dirige le pôle réseau (sous-unité) sans en être membre.
        assertThat(lancables(chef(CHEF_DES))).contains(code("FLUX_UNITE"));
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(chef(CHEF_DES)), lancement())).andExpect(status().isCreated());
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement())).andExpect(status().isCreated());
        assertThat(lancables(chef(CHEF_DDRM))).doesNotContain(code("FLUX_UNITE"));
        mvc
            .perform(json(post(BASE + "/" + id + "/instances").with(chef(CHEF_DDRM)), lancement()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Ce processus ne peut être lancé que par les membres et les responsables de l'unité POLE_RESEAU"));

        // Définition antérieure à D-100 (JSON sans « personnes ») : relue et lancée sans changement.
        jdbc.update(
            "update processus set initiateurs = '{\"regles\":[\"MEMBRES_UNITE\"],\"unites\":[],\"profils\":[],\"permissions\":[],\"initiateurIntervenant\":true}'::jsonb where id = ?::uuid",
            id
        );
        mvc.perform(get(BASE + "/" + id).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk()).andExpect(jsonPath("$.initiateurs.personnes.length()").value(0));
        mvc.perform(json(post(BASE + "/" + id + "/instances").with(agent(TECHNICIEN_RESEAU)), lancement())).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Règle PERSONNES : saisie contrôlée (400), anomalies de publication INITIATEURS_INCOMPLETS et PERSONNE_INCONNUE")
    void saisieEtControle() throws Exception {
        String id = creer(chef(CHEF_RESEAU), avecPublic("FLUX_PERSONNES", Map.of("regles", List.of("PERSONNES"))));
        // Règle sans personne : bloquante.
        JsonNode c = lire(mvc.perform(post(BASE + "/" + id + "/controle").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        assertThat(c.get("publiable").asBoolean()).isFalse();
        assertThat(c.get("anomalies").findValuesAsText("code")).contains("INITIATEURS_INCOMPLETS");
        assertThat(c.get("anomalies").findValuesAsText("element")).contains("initiateurs.personnes");
        mvc.perform(json(post(BASE + "/" + id + "/publication").with(chef(CHEF_RESEAU)), Map.of("motif", "x"))).andExpect(status().isUnprocessableEntity());

        // Identifiant vide, liste trop longue : 400 avec le chemin du champ.
        ObjectNode vide = avecPublic("FLUX_PERSONNES", Map.of("regles", List.of("PERSONNES"), "personnes", List.of(AGENT_BIS, " ")));
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), vide)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PIL_REQUETE_INVALIDE")).andExpect(jsonPath("$.details.champ").value("initiateurs.personnes[1]"));
        List<String> trop = IntStream.rangeClosed(1, 51).mapToObj(n -> "compte-" + n).toList();
        mvc
            .perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), avecPublic("FLUX_PERSONNES", Map.of("regles", List.of("PERSONNES"), "personnes", trop))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("initiateurs.personnes"));

        // Personne inconnue, inactive, externe : PERSONNE_INCONNUE (bloquante) ; une personne active : rien. Doublons retirés.
        ObjectNode mixte = avecPublic("FLUX_PERSONNES", Map.of("regles", List.of("PERSONNES"), "personnes", List.of(AGENT_BIS, AGENT_BIS, INACTIF, CORRESPONDANT, "compte-inconnu")));
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), mixte)).andExpect(status().isOk()).andExpect(jsonPath("$.initiateurs.personnes.length()").value(4));
        c = lire(mvc.perform(post(BASE + "/" + id + "/controle").with(chef(CHEF_RESEAU))).andExpect(status().isOk()).andReturn());
        List<String> inconnues = new ArrayList<>();
        c.get("anomalies").forEach(a -> {
            if ("PERSONNE_INCONNUE".equals(a.get("code").asText())) {
                assertThat(a.get("element").asText()).isEqualTo("initiateurs.personnes");
                assertThat(a.get("gravite").asText()).isEqualTo("BLOQUANTE");
                inconnues.add(a.get("message").asText());
            }
        });
        assertThat(inconnues).hasSize(3).anyMatch(m -> m.endsWith("compte-inconnu")).anyMatch(m -> m.endsWith("Compte Désactivé")).anyMatch(m -> m.endsWith("Jeanne Owona"));

        // Corrigée : publiable.
        mvc.perform(json(put(BASE + "/" + id).with(chef(CHEF_RESEAU)), avecPublic("FLUX_PERSONNES", Map.of("regles", List.of("PERSONNES"), "personnes", List.of(AGENT_BIS))))).andExpect(status().isOk());
        publier(chef(CHEF_RESEAU), id);
    }
}
