package cm.gimac.des.pilotage.web.rest.activite;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.AbstractPilotageIT;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AgendaIT extends AbstractPilotageIT {

    private static final LocalDate JOUR = LocalDate.of(2026, 10, 6);

    private Map<String, Object> evenement(String titre, String debut, String fin, List<String> participants) {
        Map<String, Object> e = new HashMap<>();
        e.put("titre", titre);
        e.put("description", "Ordre du jour");
        e.put("lieu", "Salle 2");
        e.put("debut", debut);
        e.put("fin", fin);
        e.put("categorie", "REUNION");
        e.put("participants", participants);
        return e;
    }

    private String creer(String proprietaire, Map<String, Object> e) throws Exception {
        return lire(
            mvc
                .perform(post("/api/v1/agenda").with(agent(proprietaire)).contentType(MediaType.APPLICATION_JSON).content(corps(e)))
                .andExpect(status().isCreated())
                .andReturn()
        )
            .get("id")
            .asText();
    }

    @Test
    @DisplayName("Création (201), horaires en +01:00, participant invité qui voit l'événement et répond")
    void creationEtReponse() throws Exception {
        String id = creer(AGENT, evenement("Revue hebdomadaire", "2026-10-06T09:00:00+01:00", "2026-10-06T10:00:00+01:00", List.of(AGENT_BIS, AGENT)));

        mvc
            .perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06").with(agent(AGENT)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].debut").value("2026-10-06T09:00:00+01:00"))
            .andExpect(jsonPath("$[0].fin").value("2026-10-06T10:00:00+01:00"))
            .andExpect(jsonPath("$[0].journeeEntiere").value(false))
            .andExpect(jsonPath("$[0].categorie").value("REUNION"))
            .andExpect(jsonPath("$[0].proprietaire.id").value(AGENT))
            .andExpect(jsonPath("$[0].proprietaire.nomAffiche").value("Serge Ndzana"))
            .andExpect(jsonPath("$[0].participants.length()").value(1))
            .andExpect(jsonPath("$[0].participants[0].id").value(AGENT_BIS))
            .andExpect(jsonPath("$[0].participants[0].reponse").value("EN_ATTENTE"))
            .andExpect(jsonPath("$[0].moi").value("PROPRIETAIRE"));

        mvc
            .perform(get("/api/v1/agenda").param("du", "2026-10-01").param("au", "2026-10-31").with(agent(AGENT_BIS)))
            .andExpect(jsonPath("$[0].moi").value("PARTICIPANT"))
            .andExpect(jsonPath("$[0].description").value("Ordre du jour"));

        mvc
            .perform(post("/api/v1/agenda/" + id + "/reponse").with(agent(AGENT_BIS)).contentType(MediaType.APPLICATION_JSON).content("{\"reponse\":\"ACCEPTE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participants[0].reponse").value("ACCEPTE"));

        // Un participant ne modifie ni ne supprime : 403 PIL_ACTION_RESERVEE.
        mvc
            .perform(put("/api/v1/agenda/" + id).with(agent(AGENT_BIS)).contentType(MediaType.APPLICATION_JSON).content(corps(evenement("x", "2026-10-06T09:00:00+01:00", "2026-10-06T10:00:00+01:00", List.of()))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        mvc.perform(delete("/api/v1/agenda/" + id).with(agent(AGENT_BIS))).andExpect(status().isForbidden());
        // Le propriétaire n'est pas invité à son propre événement.
        mvc
            .perform(post("/api/v1/agenda/" + id + "/reponse").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content("{\"reponse\":\"DECLINE\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_ACTION_RESERVEE"));
        // Hors équipe : 403 PIL_HORS_EQUIPE.
        mvc.perform(delete("/api/v1/agenda/" + id).with(agent(CHEF_DDRM))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));

        mvc
            .perform(put("/api/v1/agenda/" + id).with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps(evenement("Revue déplacée", "2026-10-06T11:00:00+01:00", "2026-10-06T12:00:00+01:00", List.of(AGENT_BIS)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titre").value("Revue déplacée"))
            .andExpect(jsonPath("$.participants[0].reponse").value("ACCEPTE"));

        mvc.perform(delete("/api/v1/agenda/" + id).with(agent(AGENT))).andExpect(status().isNoContent());
        assertThat(compter("select count(*) from evenement_agenda")).isZero();
        assertThat(compter("select count(*) from evenement_participant")).isZero();
    }

    @Test
    @DisplayName("Agenda d'un membre de l'équipe : lecture seule (catégorie, titre, horaires) avec PIL_TEAM_READ ; hors équipe : 403")
    void equipe() throws Exception {
        creer(AGENT, evenement("Entretien", "2026-10-06T09:00:00+01:00", "2026-10-06T10:00:00+01:00", List.of(AGENT_BIS)));

        mvc
            .perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06").param("collaborateur", AGENT).with(responsable(CHEF_SERVICE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].titre").value("Entretien"))
            .andExpect(jsonPath("$[0].moi").value("EQUIPE"))
            .andExpect(jsonPath("$[0].description").doesNotExist())
            .andExpect(jsonPath("$[0].lieu").doesNotExist())
            .andExpect(jsonPath("$[0].participants.length()").value(0));
        // Chaîne hiérarchique (n+2) : lecture permise.
        mvc
            .perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06").param("collaborateur", AGENT).with(responsable(CHEF_DES)))
            .andExpect(status().isOk());
        // Sans PIL_TEAM_READ, ou hors équipe : 403 PIL_HORS_EQUIPE.
        mvc
            .perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06").param("collaborateur", AGENT).with(agent(CHEF_SERVICE)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06").param("collaborateur", AGENT).with(responsable(CHEF_DDRM)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PIL_HORS_EQUIPE"));
        mvc
            .perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06").param("collaborateur", AGENT).with(responsable(AGENT_BIS)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Règles : fin après début, journée entière à minuit, participants internes actifs, 62 jours au plus")
    void regles() throws Exception {
        mvc
            .perform(post("/api/v1/agenda").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps(evenement("R", "2026-10-06T10:00:00+01:00", "2026-10-06T10:00:00+01:00", List.of()))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PIL_REQUETE_INVALIDE"))
            .andExpect(jsonPath("$.details.champ").value("fin"));
        mvc
            .perform(post("/api/v1/agenda").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps(evenement("R", "2026-10-06T09:00:00+01:00", "2026-10-06T10:00:00+01:00", List.of(CORRESPONDANT, INACTIF)))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PARTICIPANT_INVALIDE"))
            .andExpect(jsonPath("$.details.participants.length()").value(2));
        Map<String, Object> journee = evenement("Congé", "2026-10-06T00:00:00+01:00", "2026-10-08T00:00:00+01:00", List.of());
        journee.put("journeeEntiere", true);
        journee.put("categorie", "ABSENCE");
        mvc.perform(post("/api/v1/agenda").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps(journee))).andExpect(status().isCreated());
        journee.put("debut", "2026-10-06T08:00:00+01:00");
        mvc.perform(post("/api/v1/agenda").with(agent(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps(journee))).andExpect(status().isBadRequest());
        mvc
            .perform(get("/api/v1/agenda").param("du", "2026-10-01").param("au", "2026-12-02").with(agent(AGENT)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PIL_INTERVALLE_INVALIDE"));
        mvc.perform(get("/api/v1/agenda").param("du", "2026-10-01").param("au", "2026-12-01").with(agent(AGENT))).andExpect(status().isOk());
        assertThat(ReglesActivite.debutJour(JOUR)).isNotNull();
    }

    @Test
    @DisplayName("Sécurité : PIL_ACTIVITY_OWN exigée ; jeton sans second facteur ou jeton de service refusés")
    void securite() throws Exception {
        mvc.perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06").with(utilisateur(AGENT, "COL_MESSAGE_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(
                get("/api/v1/agenda")
                    .param("du", "2026-10-06")
                    .param("au", "2026-10-06")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(AGENT)).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PIL_ACTIVITY_OWN")))
            )
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06").with(service("homologation-service", "PIL_ACTIVITY_OWN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/agenda").param("du", "2026-10-06").param("au", "2026-10-06")).andExpect(status().isUnauthorized());
    }
}
