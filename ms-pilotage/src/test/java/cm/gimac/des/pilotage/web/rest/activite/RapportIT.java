package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import cm.gimac.des.pilotage.domain.activite.Periode;
import cm.gimac.des.pilotage.domain.activite.TypeRapport;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class RapportIT extends AbstractPilotageIT {

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, Object corps) throws Exception {
        return r.contentType(MediaType.APPLICATION_JSON).content(corps(corps));
    }

    private Map<String, Object> rapport(String type, String periode, String mode, String contenu) {
        Map<String, Object> r = new HashMap<>();
        r.put("type", type);
        r.put("periode", periode);
        r.put("mode", mode);
        r.put("contenu", contenu);
        return r;
    }

    private String creer(String auteur, Map<String, Object> r) throws Exception {
        return lire(mvc.perform(json(post("/api/v1/rapports").with(agent(auteur)), r)).andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    private void activite(String auteur, LocalDate jour, String titre, String statut, Integer duree) throws Exception {
        Map<String, Object> a = new HashMap<>();
        a.put("jour", jour.toString());
        a.put("titre", titre);
        a.put("dureeMinutes", duree);
        String id = lire(mvc.perform(json(post("/api/v1/activites").with(agent(auteur)), a)).andExpect(status().isCreated()).andReturn()).get("id").asText();
        if (statut != null) {
            mvc.perform(json(post("/api/v1/activites/" + id + "/statut").with(agent(auteur)), Map.of("statut", statut))).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Préremplissage quotidien et hebdomadaire à partir des activités et tâches ; rien n'est enregistré")
    void preremplissage() throws Exception {
        LocalDate jour = aujourdhui();
        activite(AGENT, jour, "Revue du dossier L-12", "FAITE", 90);
        activite(AGENT, jour, "Rédaction de la note", "EN_COURS", null);
        activite(AGENT, cm.gimac.des.pilotage.domain.service.ReglesActivite.jourOuvreSuivant(jour), "Visite du site", null, null);
        String tache = lire(mvc.perform(json(post("/api/v1/taches").with(agent(AGENT)), Map.of("titre", "Clore la campagne"))).andReturn()).get("id").asText();
        mvc.perform(json(post("/api/v1/taches/" + tache + "/transition").with(agent(AGENT)), Map.of("etat", "EN_COURS"))).andExpect(status().isOk());
        mvc.perform(json(post("/api/v1/taches/" + tache + "/transition").with(agent(AGENT)), Map.of("etat", "TERMINEE"))).andExpect(status().isOk());

        String md = lire(
            mvc.perform(json(post("/api/v1/rapports/preremplissage").with(agent(AGENT)), Map.of("type", "QUOTIDIEN", "periode", jour.toString()))).andExpect(status().isOk()).andReturn()
        )
            .get("contenu")
            .asText();
        assertThat(md).startsWith("# Rapport quotidien du ");
        assertThat(md).contains("## Réalisé\n\n- Revue du dossier L-12 (1 h 30)\n");
        assertThat(md).contains("## En cours\n\n- Rédaction de la note\n");
        assertThat(md).contains("## Tâches terminées\n\n- Clore la campagne\n");
        assertThat(md).contains("## Prévu demain (").contains("- Visite du site\n");

        String semaine = Periode.contenant(TypeRapport.HEBDOMADAIRE, jour).texte();
        String hebdo = lire(
            mvc.perform(json(post("/api/v1/rapports/preremplissage").with(agent(AGENT)), Map.of("type", "HEBDOMADAIRE", "periode", semaine))).andExpect(status().isOk()).andReturn()
        )
            .get("contenu")
            .asText();
        assertThat(hebdo).startsWith("# Rapport hebdomadaire, semaine ").contains("**Total des heures : 1 h 30**").contains("### Réalisé");
        assertThat(compter("select count(*) from rapport")).isZero();

        mvc
            .perform(json(post("/api/v1/rapports/preremplissage").with(agent(AGENT)), Map.of("type", "QUOTIDIEN", "periode", jour.plusDays(1).toString())))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PERIODE_FUTURE"));
        mvc
            .perform(json(post("/api/v1/rapports/preremplissage").with(agent(AGENT)), Map.of("type", "QUOTIDIEN", "periode", semaine)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PIL_PERIODE_INVALIDE"));
    }

    @Test
    @DisplayName("Cycle : brouillon (201), unicité (409), soumission au n+1, non modifiable (409), reçus, visa ; événements sans contenu")
    void cycle() throws Exception {
        String jour = aujourdhui().toString();
        activite(AGENT, aujourdhui(), "Revue", "FAITE", 60);
        String id = creer(AGENT, rapport("QUOTIDIEN", jour, "REDIGE", "Réalisé : revue du dossier confidentiel"));
        mvc
            .perform(json(post("/api/v1/rapports").with(agent(AGENT)), rapport("QUOTIDIEN", jour, "PREREMPLI", "x")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_RAPPORT_EXISTANT"));
        mvc
            .perform(json(post("/api/v1/rapports").with(agent(AGENT)), rapport("QUOTIDIEN", aujourdhui().plusDays(2).toString(), "REDIGE", "x")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PERIODE_FUTURE"));
        mvc
            .perform(json(post("/api/v1/rapports").with(agent(AGENT)), rapport("QUOTIDIEN", "2026-09-01", "REDIGE", "x".repeat(20_001))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.champ").value("contenu"));

        mvc
            .perform(get("/api/v1/rapports/" + id).with(agent(AGENT)))
            .andExpect(jsonPath("$.etat").value("BROUILLON"))
            .andExpect(jsonPath("$.destinataire").doesNotExist())
            .andExpect(jsonPath("$.indicateurs.activitesFaites").value(1))
            .andExpect(jsonPath("$.indicateurs.minutes").value(60));
        // Brouillon d'un autre : illisible, même pour le responsable.
        mvc.perform(get("/api/v1/rapports/" + id).with(responsable(CHEF_SERVICE))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));

        mvc
            .perform(post("/api/v1/rapports/" + id + "/soumission").with(agent(AGENT)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("SOUMIS"))
            .andExpect(jsonPath("$.destinataire.id").value(CHEF_SERVICE))
            .andExpect(jsonPath("$.destinataire.nomAffiche").value("Aminata Ngono"))
            .andExpect(jsonPath("$.soumisLe").isNotEmpty());
        mvc
            .perform(json(put("/api/v1/rapports/" + id).with(agent(AGENT)), rapport("QUOTIDIEN", jour, "REDIGE", "autre")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_RAPPORT_NON_MODIFIABLE"));
        mvc.perform(post("/api/v1/rapports/" + id + "/soumission").with(agent(AGENT))).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/rapports").param("vue", "recus").with(agent(CHEF_SERVICE))).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.contenu[0].id").value(id));
        mvc.perform(get("/api/v1/rapports").param("vue", "recus").with(responsable(CHEF_DES))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/rapports").param("vue", "recus").with(agent(CHEF_DES))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/rapports").param("vue", "recus").with(responsable(CHEF_DDRM))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/rapports").param("vue", "moi").param("etat", "SOUMIS").with(agent(AGENT))).andExpect(jsonPath("$.total").value(1));

        // Visa : ni l'auteur, ni un compte hors équipe.
        mvc.perform(json(post("/api/v1/rapports/" + id + "/visa").with(responsable(AGENT)), Map.of("commentaire", "x"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        mvc.perform(json(post("/api/v1/rapports/" + id + "/visa").with(responsable(CHEF_DDRM)), Map.of("commentaire", "x"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(json(post("/api/v1/rapports/" + id + "/visa").with(agent(CHEF_SERVICE)), Map.of("commentaire", "Bien reçu")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("VISE"))
            .andExpect(jsonPath("$.visa.par.id").value(CHEF_SERVICE))
            .andExpect(jsonPath("$.visa.commentaire").value("Bien reçu"))
            .andExpect(jsonPath("$.viseLe").isNotEmpty());
        mvc
            .perform(json(post("/api/v1/rapports/" + id + "/visa").with(responsable(CHEF_DES)), Map.of()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PIL_TRANSITION_INTERDITE"));

        List<String> charges = jdbc.queryForList("select payload::text from outbox_event where aggregate_id = ? order by created_at", String.class, id);
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ? order by created_at", String.class, id)).containsExactly(
            "pilotage.report-submitted.v1",
            "pilotage.report-visaed.v1"
        );
        assertThat(charges).allSatisfy(c -> assertThat(c).doesNotContain("confidentiel").doesNotContain("Bien reçu").contains(id));
    }

    @Test
    @DisplayName("Soumission sans responsable nulle part : rapport déposé (plus de 422) ; rapport vide : 422 PIL_RAPPORT_INCOMPLET")
    void sansResponsable() throws Exception {
        String id = creer(SANS_RESPONSABLE, rapport("QUOTIDIEN", aujourdhui().toString(), "REDIGE", "Texte"));
        mvc.perform(post("/api/v1/rapports/" + id + "/soumission").with(agent(SANS_RESPONSABLE))).andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("DEPOSE"));
        String vide = creer(AGENT, rapport("HEBDOMADAIRE", Periode.contenant(TypeRapport.HEBDOMADAIRE, aujourdhui()).texte(), "PREREMPLI", null));
        mvc.perform(post("/api/v1/rapports/" + vide + "/soumission").with(agent(AGENT))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PIL_RAPPORT_INCOMPLET"));
        mvc.perform(post("/api/v1/rapports/" + vide + "/soumission").with(agent(AGENT_BIS))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Mode DOCUMENT : ticket vérifié (finalité RAPPORT_ACTIVITE, référence = rapport, déposant = auteur), flux relayé")
    void document() throws Exception {
        String id = creer(AGENT, rapport("QUOTIDIEN", aujourdhui().toString(), "DOCUMENT", "Voir le document joint"));
        byte[] pdf = "%PDF-1.7 rapport".getBytes(StandardCharsets.US_ASCII);
        UUID autreReference = documents.deposer("r.pdf", "application/pdf", pdf, "RAPPORT_ACTIVITE", UUID.randomUUID().toString(), AGENT);
        UUID autreFinalite = documents.deposer("r.pdf", "application/pdf", pdf, "MESSAGERIE", id, AGENT);
        UUID autreDeposant = documents.deposer("r.pdf", "application/pdf", pdf, "RAPPORT_ACTIVITE", id, AGENT_BIS);
        for (UUID ticket : List.of(autreReference, autreFinalite, autreDeposant, UUID.randomUUID())) {
            Map<String, Object> r = rapport("QUOTIDIEN", aujourdhui().toString(), "DOCUMENT", null);
            r.put("document", Map.of("ticket", ticket.toString()));
            mvc.perform(json(put("/api/v1/rapports/" + id).with(agent(AGENT)), r)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PIL_DOCUMENT_INVALIDE"));
        }
        UUID ticket = documents.deposer("rapport.pdf", "application/pdf", pdf, "RAPPORT_ACTIVITE", id, AGENT);
        Map<String, Object> r = rapport("QUOTIDIEN", aujourdhui().toString(), "DOCUMENT", "Voir le document joint");
        r.put("document", Map.of("ticket", ticket.toString()));
        mvc
            .perform(json(put("/api/v1/rapports/" + id).with(agent(AGENT)), r))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.document.ticket").value(ticket.toString()))
            .andExpect(jsonPath("$.document.nom").value("rapport.pdf"))
            .andExpect(jsonPath("$.document.typeMime").value("application/pdf"))
            .andExpect(jsonPath("$.document.taille").value(pdf.length));
        // Sans ticket en mode DOCUMENT : le document est conservé.
        mvc
            .perform(json(put("/api/v1/rapports/" + id).with(agent(AGENT)), rapport("QUOTIDIEN", aujourdhui().toString(), "DOCUMENT", "Texte revu")))
            .andExpect(jsonPath("$.document.ticket").value(ticket.toString()));
        mvc.perform(post("/api/v1/rapports/" + id + "/soumission").with(agent(AGENT))).andExpect(status().isOk());

        byte[] lu = mvc
            .perform(get("/api/v1/rapports/" + id + "/document").with(agent(CHEF_SERVICE)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("rapport.pdf")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        assertThat(lu).isEqualTo(pdf);
        mvc.perform(get("/api/v1/rapports/" + id + "/document").with(responsable(CHEF_DES))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rapports/" + id + "/document").with(agent(AGENT_BIS))).andExpect(status().isForbidden());

        String sans = creer(AGENT, rapport("HEBDOMADAIRE", Periode.contenant(TypeRapport.HEBDOMADAIRE, aujourdhui()).texte(), "REDIGE", "x"));
        mvc.perform(get("/api/v1/rapports/" + sans + "/document").with(agent(AGENT))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PIL_DOCUMENT_ABSENT"));
    }
}
