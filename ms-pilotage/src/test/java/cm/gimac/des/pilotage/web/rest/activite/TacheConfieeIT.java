package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Unite;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tâches confiées (D-81, docs/lot1/pouvoirs-hierarchiques.md §6) avec l'adaptateur d'organisation simulé : à
 * plusieurs personnes (groupe), à une unité (à prendre, prise, attribution), relance, et contrôles par la
 * matrice des pouvoirs (TACHE_CONFIER, TACHE_SUIVRE, ACTIVITE_LIRE, RAPPORT_VISER).
 */
class TacheConfieeIT extends AbstractPilotageIT {

    /** Pôle réseau (sous PROD_INFO, sous DES) : un chef de pôle et deux agents ; un agent du pôle système. */
    static final String CHEF_POLE = "00000000-0000-4000-8000-0000000e0001";
    static final String RESEAU_1 = "00000000-0000-4000-8000-0000000e0002";
    static final String RESEAU_2 = "00000000-0000-4000-8000-0000000e0003";
    static final String SYSTEME_1 = "00000000-0000-4000-8000-0000000e0004";

    @Autowired
    private TransactionTemplate transactions;

    @BeforeEach
    void organisationDesPoles() {
        organisation.enregistrer(new Compte(CHEF_POLE, "Brice Onana", "Chef du pôle réseau", "POLE_RESEAU", CHEF_DES, true, true));
        organisation.enregistrer(new Compte(RESEAU_1, "Ingrid Mballa", "Ingénieure réseau", "POLE_RESEAU", CHEF_POLE, true, true));
        organisation.enregistrer(new Compte(RESEAU_2, "Yves Ateba", "Technicien réseau", "POLE_RESEAU", CHEF_POLE, true, true));
        organisation.enregistrer(new Compte(SYSTEME_1, "Carine Fouda", "Administratrice système", "POLE_SYSTEME", CHEF_DES, true, true));
        organisation.enregistrerUnite(new Unite("POLE_RESEAU", "PROD_INFO", CHEF_POLE));
        organisation.definirPouvoirs(CHEF_POLE, MATRICE_RESPONSABLE);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, Object corps) throws Exception {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps(corps));
    }

    private static Map<String, Object> tache(String titre) {
        Map<String, Object> t = new HashMap<>();
        t.put("titre", titre);
        t.put("echeance", "2026-01-05");
        t.put("priorite", "HAUTE");
        return t;
    }

    private String pourUnite(String createur, String unite) throws Exception {
        Map<String, Object> t = tache("Renouveler les certificats des routeurs");
        t.put("uniteCible", unite);
        return lire(mvc.perform(json(post("/api/v1/taches").with(responsable(createur)), t)).andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    private String confier(String createur, String responsable) throws Exception {
        Map<String, Object> t = tache("Préparer le comité");
        t.put("responsableId", responsable);
        return lire(mvc.perform(json(post("/api/v1/taches").with(responsable(createur)), t)).andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    private JsonNode charge(String type, String tacheId) throws Exception {
        return json.readTree(jdbc.queryForObject("select payload::text from outbox_event where event_type = ? and aggregate_id = ?", String.class, type, tacheId));
    }

    private static List<String> textes(JsonNode tableau) {
        List<String> l = new ArrayList<>();
        tableau.forEach(n -> l.add(n.asText()));
        return l;
    }

    /** Dernière relance reculée de 25 h, validée (le pool ne valide pas de lui-même). */
    private void vieillirRelance(String id) {
        transactions.executeWithoutResult(s -> assertThat(jdbc.update("update tache set relancee_le = now() - interval '25 hours' where id = ?::uuid", id)).isEqualTo(1));
    }

    private void restreindre(String compte, Pouvoir pouvoir, Portee portee) {
        Map<Pouvoir, Portee> m = new EnumMap<>(MATRICE_RESPONSABLE);
        m.put(pouvoir, portee);
        organisation.definirPouvoirs(compte, m);
    }

    @Test
    @DisplayName("À plusieurs personnes : une tâche par personne, liées par un groupe ; suivi groupé et avancement ; lecture limitée")
    void plusieursPersonnes() throws Exception {
        Map<String, Object> t = tache("Relire la procédure d'exploitation");
        t.put("responsables", List.of(AGENT, AGENT_BIS, AGENT));
        JsonNode g = lire(
            mvc
                .perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.startsWith("/api/v1/taches/groupes/")))
                .andExpect(jsonPath("$.titre").value("Relire la procédure d'exploitation"))
                .andExpect(jsonPath("$.createur.id").value(CHEF_SERVICE))
                .andExpect(jsonPath("$.createur.nomAffiche").value("Aminata Ngono"))
                .andExpect(jsonPath("$.taches.length()").value(2))
                .andExpect(jsonPath("$.taches[0].etat").value("OUVERTE"))
                .andExpect(jsonPath("$.avancement.total").value(2))
                .andExpect(jsonPath("$.avancement.terminees").value(0))
                .andExpect(jsonPath("$.avancement.enRetard").value(2))
                .andReturn()
        );
        String groupe = g.get("groupeId").asText();
        assertThat(g.at("/taches/0/groupeId").asText()).isEqualTo(groupe);
        assertThat(List.of(g.at("/taches/0/responsable/id").asText(), g.at("/taches/1/responsable/id").asText())).containsExactlyInAnyOrder(AGENT, AGENT_BIS);
        assertThat(compter("select count(*) from tache where groupe_id = ?::uuid", groupe)).isEqualTo(2);
        assertThat(compter("select count(*) from outbox_event where event_type = 'pilotage.task-created.v1'")).isEqualTo(2);
        assertThat(compter("select count(*) from outbox_event where event_type = 'pilotage.task-assigned.v1'")).isEqualTo(2);
        String tacheAgent = g.at("/taches/0/responsable/id").asText().equals(AGENT) ? g.at("/taches/0/id").asText() : g.at("/taches/1/id").asText();
        JsonNode affectee = charge("pilotage.task-assigned.v1", tacheAgent);
        assertThat(affectee.get("titre").asText()).isEqualTo("Relire la procédure d'exploitation");
        assertThat(affectee.get("echeance").asText()).isEqualTo("2026-01-05");
        assertThat(affectee.get("responsableId").asText()).isEqualTo(AGENT);

        // Avancement : une terminée.
        mvc.perform(json(post("/api/v1/taches/" + tacheAgent + "/transition").with(agent(AGENT)), Map.of("etat", "EN_COURS"))).andExpect(status().isOk());
        mvc.perform(json(post("/api/v1/taches/" + tacheAgent + "/transition").with(agent(AGENT)), Map.of("etat", "TERMINEE"))).andExpect(status().isOk());
        mvc
            .perform(get("/api/v1/taches/groupes/" + groupe).with(responsable(CHEF_SERVICE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groupeId").value(groupe))
            .andExpect(jsonPath("$.taches.length()").value(2))
            .andExpect(jsonPath("$.avancement.total").value(2))
            .andExpect(jsonPath("$.avancement.terminees").value(1))
            .andExpect(jsonPath("$.avancement.enRetard").value(1));
        // Un responsable du groupe voit sa tâche ; le chef du DES (TACHE_SUIVRE) les deux ; hors portée : 403 ; inconnu : 404.
        mvc.perform(get("/api/v1/taches/groupes/" + groupe).with(agent(AGENT))).andExpect(status().isOk()).andExpect(jsonPath("$.taches.length()").value(1));
        mvc.perform(get("/api/v1/taches/groupes/" + groupe).with(responsable(CHEF_DES))).andExpect(jsonPath("$.taches.length()").value(2));
        mvc.perform(get("/api/v1/taches/groupes/" + groupe).with(responsable(CHEF_DDRM))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(get("/api/v1/taches/groupes/00000000-0000-4000-8000-000000000000").with(responsable(CHEF_SERVICE)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PIL_GROUPE_INTROUVABLE"));
        mvc
            .perform(get("/api/v1/taches").param("vue", "creees").with(responsable(CHEF_SERVICE)))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.contenu[0].groupeId").value(groupe));

        // 2 à 50 personnes distinctes ; une personne hors portée : 403 et rien n'est créé ; exclusif de uniteCible.
        t.put("responsables", List.of(AGENT));
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("responsables"));
        t.put("responsables", IntStream.range(0, 51).mapToObj(i -> "compte-" + i).toList());
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t)).andExpect(status().isBadRequest());
        t.put("responsables", List.of(AGENT, CHEF_DDRM));
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        t.put("responsables", List.of(AGENT, AGENT_BIS));
        mvc.perform(json(post("/api/v1/taches").with(agent(AGENT)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        t.put("uniteCible", "SNA");
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t)).andExpect(status().isBadRequest());
        assertThat(compter("select count(*) from tache")).isEqualTo(2);
    }

    @Test
    @DisplayName("À une unité : à prendre, sans responsable ; task-offered aux membres de l'unité et des sous-unités ; vue unite ; prise unique")
    void uniteEtPrise() throws Exception {
        String id = pourUnite(CHEF_DES, "PROD_INFO");
        mvc
            .perform(get("/api/v1/taches/" + id).with(agent(RESEAU_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("A_PRENDRE"))
            .andExpect(jsonPath("$.responsable").doesNotExist())
            .andExpect(jsonPath("$.uniteCible").value("PROD_INFO"))
            .andExpect(jsonPath("$.groupeId").doesNotExist())
            .andExpect(jsonPath("$.createur.id").value(CHEF_DES))
            .andExpect(jsonPath("$.historique[0].detail").value("Confiée à l'unité PROD_INFO, à prendre"));
        JsonNode offerte = charge("pilotage.task-offered.v1", id);
        assertThat(offerte.get("tacheId").asText()).isEqualTo(id);
        assertThat(offerte.get("titre").asText()).isEqualTo("Renouveler les certificats des routeurs");
        assertThat(offerte.get("uniteCible").asText()).isEqualTo("PROD_INFO");
        assertThat(offerte.get("acteurId").asText()).isEqualTo(CHEF_DES);
        assertThat(offerte.get("le").asText()).isNotBlank();
        assertThat(textes(offerte.get("destinataires"))).containsExactlyInAnyOrder(CHEF_POLE, RESEAU_1, RESEAU_2, SYSTEME_1);
        assertThat(charge("pilotage.task-created.v1", id).get("uniteCible").asText()).isEqualTo("PROD_INFO");
        assertThat(compter("select count(*) from outbox_event where event_type = 'pilotage.task-assigned.v1'")).isZero();

        // Vue unite : membres de l'unité et des sous-unités, et responsables qui la dirigent ; autres : rien.
        mvc.perform(get("/api/v1/taches").param("vue", "unite").with(agent(RESEAU_1))).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.contenu[0].id").value(id));
        mvc.perform(get("/api/v1/taches").param("vue", "unite").with(agent(SYSTEME_1))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/taches").param("vue", "unite").with(responsable(CHEF_DES))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/taches").param("vue", "unite").with(agent(AGENT))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/taches").param("vue", "creees").with(responsable(CHEF_DES))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/taches/" + id).with(agent(AGENT))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));

        // Prise : hors unité 403 ; transition OUVERTE refusée (la prise ouvre la tâche) ; un membre la prend, une seule fois.
        mvc.perform(post("/api/v1/taches/" + id + "/prise").with(agent(AGENT))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/transition").with(agent(RESEAU_1)), Map.of("etat", "OUVERTE")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        mvc
            .perform(post("/api/v1/taches/" + id + "/prise").with(agent(RESEAU_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("OUVERTE"))
            .andExpect(jsonPath("$.responsable.id").value(RESEAU_1))
            .andExpect(jsonPath("$.responsable.nomAffiche").value("Ingrid Mballa"))
            .andExpect(jsonPath("$.uniteCible").value("PROD_INFO"))
            .andExpect(jsonPath("$.historique[1].action").value("PRISE"))
            .andExpect(jsonPath("$.historique[1].acteur.id").value(RESEAU_1));
        mvc
            .perform(post("/api/v1/taches/" + id + "/prise").with(agent(RESEAU_2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_TRANSITION_INTERDITE"));
        JsonNode affectee = charge("pilotage.task-assigned.v1", id);
        assertThat(affectee.get("responsableId").asText()).isEqualTo(RESEAU_1);
        assertThat(affectee.get("acteurId").asText()).isEqualTo(RESEAU_1);
        assertThat(affectee.get("titre").asText()).isEqualTo("Renouveler les certificats des routeurs");
        assertThat(affectee.get("echeance").asText()).isEqualTo("2026-01-05");

        mvc.perform(get("/api/v1/taches").param("vue", "unite").with(agent(RESEAU_2))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/taches").param("vue", "unite").param("etat", "OUVERTE").with(agent(RESEAU_2))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/taches").param("vue", "moi").with(agent(RESEAU_1))).andExpect(jsonPath("$.total").value(1));
        // Le membre de l'unité lit la tâche prise, mais seul le responsable la fait avancer.
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/transition").with(agent(RESEAU_2)), Map.of("etat", "EN_COURS")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        mvc.perform(json(post("/api/v1/taches/" + id + "/transition").with(agent(RESEAU_1)), Map.of("etat", "EN_COURS"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Attribution : responsable de l'unité ou créateur, TACHE_CONFIER couvrant la personne, membre de l'unité ; annulation par le créateur")
    void attribution() throws Exception {
        String id = pourUnite(CHEF_DES, "POLE_RESEAU");
        // Simple membre : 403 réservé ; hors unité et hors portée : 403 hors équipe.
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/attribution").with(agent(RESEAU_1)), Map.of("responsableId", RESEAU_2)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/attribution").with(responsable(CHEF_DDRM)), Map.of("responsableId", RESEAU_2)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        // Le chef de pôle n'a aucun pouvoir sur le pôle système ; le créateur, oui, mais la personne n'est pas de l'unité.
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/attribution").with(responsable(CHEF_POLE)), Map.of("responsableId", SYSTEME_1)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/attribution").with(responsable(CHEF_DES)), Map.of("responsableId", SYSTEME_1)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_RESPONSABLE_INVALIDE"));
        mvc.perform(json(post("/api/v1/taches/" + id + "/attribution").with(responsable(CHEF_POLE)), Map.of())).andExpect(status().isBadRequest());
        // Sans PIL_TASK_ASSIGN dans le jeton : 403 réservé, même avec la matrice.
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/attribution").with(utilisateur(CHEF_POLE, "PIL_ACTIVITY_OWN", "PIL_TEAM_READ")), Map.of("responsableId", RESEAU_2)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/attribution").with(responsable(CHEF_POLE)), Map.of("responsableId", RESEAU_2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("OUVERTE"))
            .andExpect(jsonPath("$.responsable.id").value(RESEAU_2))
            .andExpect(jsonPath("$.historique[1].action").value("ATTRIBUEE"))
            .andExpect(jsonPath("$.historique[1].acteur.id").value(CHEF_POLE))
            .andExpect(jsonPath("$.historique[1].detail").value("Attribuée à Yves Ateba"));
        mvc
            .perform(json(post("/api/v1/taches/" + id + "/attribution").with(responsable(CHEF_DES)), Map.of("responsableId", RESEAU_1)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_TRANSITION_INTERDITE"));
        assertThat(compter("select count(*) from outbox_event where aggregate_id = ? and event_type = 'pilotage.task-assigned.v1'", id)).isEqualTo(1);
        assertThat(charge("pilotage.task-assigned.v1", id).get("acteurId").asText()).isEqualTo(CHEF_POLE);

        // Le créateur annule une tâche à prendre (motif exigé) ; elle ne se prend plus ; la base garde l'invariant.
        String autre = pourUnite(CHEF_DES, "POLE_RESEAU");
        mvc.perform(json(post("/api/v1/taches/" + autre + "/transition").with(responsable(CHEF_DES)), Map.of("etat", "ANNULEE"))).andExpect(status().isUnprocessableEntity());
        mvc
            .perform(json(post("/api/v1/taches/" + autre + "/transition").with(responsable(CHEF_DES)), Map.of("etat", "ANNULEE", "motif", "Fait par le prestataire")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("ANNULEE"))
            .andExpect(jsonPath("$.responsable").doesNotExist());
        mvc.perform(post("/api/v1/taches/" + autre + "/prise").with(agent(RESEAU_1))).andExpect(status().isConflict());
        assertThatThrownBy(() -> jdbc.update("update tache set etat = 'OUVERTE' where id = ?::uuid", autre)).hasMessageContaining("ck_tache_responsable");

        // Réaffectation (PUT) d'une tâche à prendre par le créateur : c'est une attribution.
        String troisieme = pourUnite(CHEF_DES, "POLE_RESEAU");
        Map<String, Object> m = tache("Renouveler les certificats des routeurs");
        m.put("responsableId", RESEAU_1);
        mvc
            .perform(json(put("/api/v1/taches/" + troisieme).with(responsable(CHEF_DES)), m))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("OUVERTE"))
            .andExpect(jsonPath("$.responsable.id").value(RESEAU_1));
    }

    @Test
    @DisplayName("Confier à une unité : PIL_TASK_ASSIGN et TACHE_CONFIER de portée UNITES couvrant l'unité ; code d'unité contrôlé")
    void uniteHorsPortee() throws Exception {
        Map<String, Object> t = tache("T");
        t.put("uniteCible", "POLE_RESEAU");
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_SERVICE)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_DDRM)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc.perform(json(post("/api/v1/taches").with(agent(RESEAU_1)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        t.put("uniteCible", "pole réseau");
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_DES)), t)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.champ").value("uniteCible"));
        t.put("uniteCible", "POLE_RESEAU");
        t.put("responsableId", RESEAU_1);
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_DES)), t)).andExpect(status().isBadRequest());
        // Portée EQUIPE : l'unité n'est plus couverte, même dirigée.
        t.remove("responsableId");
        restreindre(CHEF_DES, Pouvoir.TACHE_CONFIER, Portee.EQUIPE);
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_DES)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        assertThat(compter("select count(*) from tache")).isZero();
    }

    @Test
    @DisplayName("Matrice : TACHE_CONFIER règle la portée de confier ; UNITES couvre un membre d'unité dirigée hors de l'équipe")
    void confierSelonLaMatrice() throws Exception {
        // Un agent du pôle réseau sans lien hiérarchique avec le chef de pôle, couvert par UNITES seulement.
        String isole = "00000000-0000-4000-8000-0000000e0009";
        organisation.enregistrer(new Compte(isole, "Agent isolé", "Technicien", "POLE_RESEAU", SANS_RESPONSABLE, true, true));
        confier(CHEF_POLE, isole);
        restreindre(CHEF_POLE, Pouvoir.TACHE_CONFIER, Portee.EQUIPE);
        Map<String, Object> t = tache("T");
        t.put("responsableId", isole);
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_POLE)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        // DIRECTS : le n+1 seulement.
        restreindre(CHEF_DES, Pouvoir.TACHE_CONFIER, Portee.DIRECTS);
        confier(CHEF_DES, CHEF_SERVICE);
        t.put("responsableId", AGENT);
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_DES)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        // AUCUNE : même avec PIL_TASK_ASSIGN, rien ne se confie ; pour soi, toujours permis.
        restreindre(CHEF_DES, Pouvoir.TACHE_CONFIER, Portee.AUCUNE);
        t.put("responsableId", CHEF_SERVICE);
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_DES)), t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        t.remove("responsableId");
        mvc.perform(json(post("/api/v1/taches").with(responsable(CHEF_DES)), t)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Relance : créateur ou TACHE_SUIVRE ; une par 24 h (429 PIL_RELANCE_RECENTE) ; task-reminded ; tâche à prendre ou pour soi : 409")
    void relance() throws Exception {
        String id = confier(CHEF_SERVICE, AGENT);
        mvc
            .perform(post("/api/v1/taches/" + id + "/relance").with(responsable(CHEF_SERVICE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relanceeLe").isNotEmpty())
            .andExpect(jsonPath("$.historique[1].action").value("RELANCEE"))
            .andExpect(jsonPath("$.historique[1].detail").value("Relance adressée à Serge Ndzana"));
        JsonNode relancee = charge("pilotage.task-reminded.v1", id);
        assertThat(relancee.get("tacheId").asText()).isEqualTo(id);
        assertThat(relancee.get("titre").asText()).isEqualTo("Préparer le comité");
        assertThat(relancee.get("responsableId").asText()).isEqualTo(AGENT);
        assertThat(relancee.get("acteurId").asText()).isEqualTo(CHEF_SERVICE);
        assertThat(relancee.get("le").asText()).isNotBlank();
        mvc
            .perform(post("/api/v1/taches/" + id + "/relance").with(responsable(CHEF_SERVICE)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("PIL_RELANCE_RECENTE"))
            .andExpect(jsonPath("$.details.prochaineRelance").isNotEmpty());
        // Le chef du DES suit l'agent (TACHE_SUIVRE) : la limite vaut pour la tâche, quel que soit l'auteur de la relance.
        mvc.perform(post("/api/v1/taches/" + id + "/relance").with(responsable(CHEF_DES))).andExpect(status().isTooManyRequests());
        vieillirRelance(id);
        mvc.perform(post("/api/v1/taches/" + id + "/relance").with(responsable(CHEF_DES))).andExpect(status().isOk());
        assertThat(compter("select count(*) from outbox_event where aggregate_id = ? and event_type = 'pilotage.task-reminded.v1'", id)).isEqualTo(2);

        // Le responsable ne relance pas (403 réservé) ; hors portée : 403 ; sans PIL_TEAM_READ ni création : 403.
        mvc.perform(post("/api/v1/taches/" + id + "/relance").with(agent(AGENT))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        mvc.perform(post("/api/v1/taches/" + id + "/relance").with(responsable(CHEF_DDRM))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc.perform(post("/api/v1/taches/" + id + "/relance").with(utilisateur(CHEF_DES, "PIL_ACTIVITY_OWN"))).andExpect(status().isForbidden());
        // TACHE_SUIVRE réduite à DIRECTS : l'agent (n+2) n'est plus suivi par le chef du DES.
        restreindre(CHEF_DES, Pouvoir.TACHE_SUIVRE, Portee.DIRECTS);
        vieillirRelance(id);
        mvc.perform(post("/api/v1/taches/" + id + "/relance").with(responsable(CHEF_DES))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));

        String aPrendre = pourUnite(CHEF_POLE, "POLE_RESEAU");
        mvc.perform(post("/api/v1/taches/" + aPrendre + "/relance").with(responsable(CHEF_POLE))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PIL_RELANCE_IMPOSSIBLE"));
        String pourSoi = lire(mvc.perform(json(post("/api/v1/taches").with(agent(AGENT)), tache("Pour moi"))).andExpect(status().isCreated()).andReturn()).get("id").asText();
        mvc.perform(post("/api/v1/taches/" + pourSoi + "/relance").with(agent(AGENT))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PIL_RELANCE_IMPOSSIBLE"));
    }

    @Test
    @DisplayName("Vues et lectures selon la matrice : equipe (TACHE_SUIVRE), /equipe et agenda (ACTIVITE_LIRE), rapports reçus et visa (RAPPORT_VISER)")
    void vuesSelonLaMatrice() throws Exception {
        confier(CHEF_SERVICE, AGENT);
        confier(CHEF_DES, CHEF_SERVICE);
        mvc.perform(get("/api/v1/taches").param("vue", "equipe").with(responsable(CHEF_DES))).andExpect(jsonPath("$.total").value(2));
        // UNITES : le chef de pôle suit aussi un membre du pôle sans lien hiérarchique.
        String isole = "00000000-0000-4000-8000-0000000e0009";
        organisation.enregistrer(new Compte(isole, "Agent isolé", "Technicien", "POLE_RESEAU", SANS_RESPONSABLE, true, true));
        mvc.perform(json(post("/api/v1/taches").with(agent(isole)), tache("La sienne"))).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/taches").param("vue", "equipe").with(responsable(CHEF_POLE))).andExpect(jsonPath("$.total").value(1));
        restreindre(CHEF_DES, Pouvoir.TACHE_SUIVRE, Portee.DIRECTS);
        mvc
            .perform(get("/api/v1/taches").param("vue", "equipe").with(responsable(CHEF_DES)))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].responsable.id").value(CHEF_SERVICE));

        // ACTIVITE_LIRE : /equipe et l'agenda d'un collaborateur.
        mvc
            .perform(get("/api/v1/agenda").param("du", aujourdhui().toString()).param("au", aujourdhui().toString()).param("collaborateur", AGENT).with(responsable(CHEF_DES)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/equipe").with(responsable(CHEF_DES))).andExpect(jsonPath("$[*].collaborateur.id").value(Matchers.hasItem(AGENT)));
        restreindre(CHEF_DES, Pouvoir.ACTIVITE_LIRE, Portee.DIRECTS);
        mvc
            .perform(get("/api/v1/agenda").param("du", aujourdhui().toString()).param("au", aujourdhui().toString()).param("collaborateur", AGENT).with(responsable(CHEF_DES)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(get("/api/v1/activites").param("du", aujourdhui().toString()).param("au", aujourdhui().toString()).param("collaborateur", AGENT).with(responsable(CHEF_DES)))
            .andExpect(status().isForbidden());
        mvc
            .perform(get("/api/v1/equipe").with(responsable(CHEF_DES)))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[*].collaborateur.id").value(Matchers.containsInAnyOrder(CHEF_SERVICE, CHEF_POLE, SYSTEME_1)));

        // RAPPORT_VISER : rapport de l'agent soumis à sa cheffe ; le chef du DES le lit et le vise tant que la portée le couvre.
        String rapport = lire(
            mvc
                .perform(json(post("/api/v1/rapports").with(agent(AGENT)), Map.of("type", "QUOTIDIEN", "periode", aujourdhui().toString(), "mode", "REDIGE", "contenu", "x")))
                .andExpect(status().isCreated())
                .andReturn()
        )
            .get("id")
            .asText();
        mvc.perform(post("/api/v1/rapports/" + rapport + "/soumission").with(agent(AGENT))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rapports").param("vue", "recus").with(responsable(CHEF_DES))).andExpect(jsonPath("$.total").value(1));
        restreindre(CHEF_DES, Pouvoir.RAPPORT_VISER, Portee.AUCUNE);
        mvc.perform(get("/api/v1/rapports").param("vue", "recus").with(responsable(CHEF_DES))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/rapports/" + rapport).with(responsable(CHEF_DES))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(json(post("/api/v1/rapports/" + rapport + "/visa").with(responsable(CHEF_DES)), Map.of("commentaire", "Vu")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        // Le destinataire (n+1 à la soumission) vise toujours.
        mvc.perform(json(post("/api/v1/rapports/" + rapport + "/visa").with(responsable(CHEF_SERVICE)), Map.of("commentaire", "Vu"))).andExpect(status().isOk());
    }
}
