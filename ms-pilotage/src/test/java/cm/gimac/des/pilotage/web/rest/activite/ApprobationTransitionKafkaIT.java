package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT_BIS;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DDRM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Tâche de l'approbateur d'une transition de statut (D-98, docs/lot1/lancement-processus.md §8.8) sur Kafka réel : la
 * proposition de MS-PARTICIPANT (participant.transition-requested.v1, sujet participant.transition) devient une tâche à
 * prendre par les porteurs de PAR_TRANSITION_APPROVE (composition des profils : données de MS-IAM), notifiée avec le lien
 * de l'écran où l'on prononce la transition ; elle est remise à prendre après une première approbation sur deux, terminée
 * quand la transition est prononcée (participant.transition-approved.v1 définitive) ou refusée
 * (participant.transition-rejected.v1) ; chaque événement n'est traité qu'une fois.
 */
class ApprobationTransitionKafkaIT extends AbstractPilotageIT {

    private static final String SUJET = "participant.transition";

    @Autowired
    private KafkaTemplate<String, String> kafka;

    private void envoyer(String eventId, String type, String institution, Map<String, Object> data) throws Exception {
        Map<String, Object> e = new HashMap<>();
        e.put("specversion", "1.0");
        e.put("id", eventId);
        e.put("source", "/gimac/des/participant");
        e.put("type", type);
        e.put("subject", institution);
        e.put("data", data);
        kafka.send(SUJET, institution, json.writeValueAsString(e)).get(10, TimeUnit.SECONDS);
    }

    private static Map<String, Object> proposition(String institution, String transition, String de, String vers, int requises) {
        Map<String, Object> d = new HashMap<>();
        d.put("institutionId", institution);
        d.put("transitionId", transition);
        d.put("de", de);
        d.put("vers", vers);
        d.put("motif", "Dossier complet");
        d.put("demandeurId", AGENT);
        d.put("approbationsRequises", requises);
        d.put("institutionNom", "Banque B");
        d.put("lien", "/des/institutions/" + institution + "?onglet=transitions");
        return d;
    }

    private static Map<String, Object> approbation(String institution, String transition, boolean definitive) {
        return Map.of("institutionId", institution, "transitionId", transition, "de", "SUSPENDU", "vers", "ACTIF", "approbateurId", CHEF_DDRM, "definitive", definitive);
    }

    private String attendreEtat(String reference, String etat) throws InterruptedException {
        long fin = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < fin) {
            List<Map<String, Object>> l = jdbc.queryForList("select id::text as id, etat from tache where origine_reference = ?", reference);
            if (!l.isEmpty() && etat.equals(l.get(0).get("etat"))) {
                return (String) l.get(0).get("id");
            }
            Thread.sleep(300);
        }
        throw new AssertionError("Tâche " + reference + " jamais à l'état " + etat);
    }

    private void attendreTraite(String eventId) throws InterruptedException {
        long fin = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < fin) {
            if (compter("select count(*) from processed_event where event_id = ?::uuid", eventId) == 1) {
                return;
            }
            Thread.sleep(300);
        }
        throw new AssertionError("Événement " + eventId + " jamais traité");
    }

    private List<String> mesTaches(String qui) throws Exception {
        JsonNode p = lire(mvc.perform(get("/api/v1/taches").param("vue", "moi").with(agent(qui))).andExpect(status().isOk()).andReturn());
        List<String> ids = new ArrayList<>();
        p.get("contenu").forEach(n -> ids.add(n.get("id").asText()));
        return ids;
    }

    private JsonNode offerte(String tache) throws Exception {
        return json.readTree(
            jdbc.queryForObject(
                "select payload::text from outbox_event where event_type = 'pilotage.task-offered.v1' and aggregate_id = ? order by created_at desc limit 1",
                String.class,
                tache
            )
        );
    }

    @Test
    @DisplayName("Proposition : tâche à prendre des porteurs de PAR_TRANSITION_APPROVE, notifiée vers l'écran de la transition, remise à prendre, close à l'approbation ; idempotente")
    void propositionApprouvee() throws Exception {
        organisation.composerProfil("ROLE_CHEF_DDRM", "PAR_TRANSITION_APPROVE");
        organisation.composerProfil("ROLE_SNA", "PAR_TRANSITION_APPROVE");
        String institution = UUID.randomUUID().toString();
        String transition = UUID.randomUUID().toString();
        String reference = institution + "/TRANSITION/" + transition;
        String demandee = UUID.randomUUID().toString();
        envoyer(demandee, "participant.transition-requested.v1", institution, proposition(institution, transition, "SUSPENDU", "ACTIF", 2));
        String id = attendreEtat(reference, "A_PRENDRE");

        mvc
            .perform(get("/api/v1/taches/" + id).with(agent(CHEF_DDRM)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aPrendre").value(true))
            .andExpect(jsonPath("$.titre").value("Statuer sur le passage de Suspendu à Actif : Banque B"))
            .andExpect(jsonPath("$.priorite").value("HAUTE"))
            .andExpect(jsonPath("$.permissionsRequises[0]").value("PAR_TRANSITION_APPROVE"))
            .andExpect(jsonPath("$.origine.type").value("DECISION"))
            .andExpect(jsonPath("$.origine.service").value("participant"))
            .andExpect(jsonPath("$.origine.lien").value("/des/institutions/" + institution + "?onglet=transitions"))
            .andExpect(jsonPath("$.createur.id").value(AGENT));
        JsonNode offre = offerte(id);
        assertThat(offre.get("lien").asText()).isEqualTo("/des/institutions/" + institution + "?onglet=transitions");
        List<String> destinataires = new ArrayList<>();
        offre.get("destinataires").forEach(n -> destinataires.add(n.asText()));
        assertThat(destinataires).containsExactlyInAnyOrder(CHEF_DDRM, AGENT_BIS);
        assertThat(mesTaches(CHEF_DDRM)).contains(id);
        assertThat(mesTaches(AGENT_BIS)).contains(id);
        assertThat(mesTaches(AGENT)).doesNotContain(id);
        // DDR-28 : ni le créateur ni le responsable n'annulent ou ne terminent à la main la tâche d'une décision.
        mvc
            .perform(
                post("/api/v1/taches/" + id + "/transition")
                    .with(agent(AGENT))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("etat", "ANNULEE", "motif", "Plus utile")))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_TACHE_DECISION"));

        // Rejeu du même événement : une seule tâche.
        envoyer(demandee, "participant.transition-requested.v1", institution, proposition(institution, transition, "SUSPENDU", "ACTIF", 2));
        attendreTraite(demandee);
        Thread.sleep(1500);
        assertThat(compter("select count(*) from tache where origine_reference = ?", reference)).isEqualTo(1);

        // Le chef de la DDRM la prend puis donne la première approbation : la tâche revient à prendre pour un second approbateur.
        mvc.perform(post("/api/v1/taches/" + id + "/prise").with(agent(CHEF_DDRM))).andExpect(status().isOk()).andExpect(jsonPath("$.responsable.id").value(CHEF_DDRM));
        envoyer(UUID.randomUUID().toString(), "participant.transition-approved.v1", institution, approbation(institution, transition, false));
        attendreEtat(reference, "A_PRENDRE");
        assertThat(jdbc.queryForObject("select responsable_id from tache where id = ?::uuid", String.class, id)).isNull();
        assertThat(mesTaches(AGENT_BIS)).contains(id);

        // Seconde approbation : transition prononcée, tâche terminée par le service.
        envoyer(UUID.randomUUID().toString(), "participant.transition-approved.v1", institution, approbation(institution, transition, true));
        attendreEtat(reference, "TERMINEE");
        assertThat(mesTaches(AGENT_BIS)).doesNotContain(id);
    }

    @Test
    @DisplayName("Proposition d'adhésion refusée : tâche terminée ; message sans transition ignoré")
    void propositionRefusee() throws Exception {
        organisation.composerProfil("ROLE_CHEF_DDRM", "PAR_TRANSITION_APPROVE");
        String institution = UUID.randomUUID().toString();
        String transition = UUID.randomUUID().toString();
        String reference = institution + "/TRANSITION/" + transition;
        envoyer(UUID.randomUUID().toString(), "participant.transition-requested.v1", institution, proposition(institution, transition, "CANDIDAT", "ADHERENT", 1));
        String id = attendreEtat(reference, "A_PRENDRE");
        mvc.perform(get("/api/v1/taches/" + id).with(agent(CHEF_DDRM))).andExpect(jsonPath("$.titre").value("Prononcer l'adhésion : Banque B"));

        String incomplet = UUID.randomUUID().toString();
        envoyer(incomplet, "participant.transition-rejected.v1", institution, Map.of("institutionId", institution));
        envoyer(
            UUID.randomUUID().toString(),
            "participant.transition-rejected.v1",
            institution,
            Map.of("institutionId", institution, "transitionId", transition, "de", "CANDIDAT", "vers", "ADHERENT", "motif", "Pièces à compléter", "approbateurId", CHEF_DDRM)
        );
        attendreEtat(reference, "TERMINEE");
        assertThat(compter("select count(*) from processed_event where event_id = ?::uuid", incomplet)).isZero();
        assertThat(mesTaches(CHEF_DDRM)).doesNotContain(id);
    }
}
