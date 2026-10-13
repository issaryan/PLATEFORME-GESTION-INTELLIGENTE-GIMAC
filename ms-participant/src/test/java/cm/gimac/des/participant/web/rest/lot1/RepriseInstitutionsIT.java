package cm.gimac.des.participant.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.AbstractParticipantIT;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reprise des institutions (D-107) : jeton du compte de service reprise-service sur la route interne, super
 * administrateur sur la route métier ; simulation sans écriture, création avec l'UUID fourni, idempotence par code
 * membre, transition de reprise tracée, refus par ligne. Données fictives.
 */
class RepriseInstitutionsIT extends AbstractParticipantIT {

    static final ObjectMapper JSON = JsonMapper.builder().build();
    static final UUID UUID_COMPTES = UUID.fromString("0f0e0d0c-0b0a-4908-8706-050403020100");

    /** Jeton client_credentials du compte de service reprise-service : sans acr ni session. */
    static RequestPostProcessor compteReprise() {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("preferred_username", "service-account-reprise-service"))
            .authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT"));
    }

    static Map<String, Object> institution(String code, String statut) {
        Map<String, Object> i = new LinkedHashMap<>();
        i.put("codeMembre", code);
        i.put("nom", "Institution fictive " + code);
        i.put("pays", "CM");
        i.put("categorie", "BANQUE");
        i.put("statut", statut);
        return i;
    }

    static Map<String, Object> avec(Map<String, Object> i, Object... cleValeur) {
        for (int k = 0; k < cleValeur.length; k += 2) {
            i.put((String) cleValeur[k], cleValeur[k + 1]);
        }
        return i;
    }

    ResultActions reprendre(String chemin, RequestPostProcessor jeton, Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        Map<String, Object> corps = new LinkedHashMap<>();
        if (simulation != null) {
            corps.put("simulation", simulation);
        }
        corps.put("motif", "Reprise de l'existant (recette)");
        corps.put("institutions", lignes);
        return mvc.perform(post(chemin).with(jeton).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(corps)));
    }

    ResultActions interne(Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return reprendre("/api/v1/interne/reprise/institutions", compteReprise(), simulation, lignes);
    }

    long institutions() {
        return jdbc.queryForObject("select count(*) from institution", Long.class);
    }

    @Test
    @DisplayName("Simulation (par défaut) : rapport complet, rien n'est écrit, ni institution ni événement")
    void simulation() throws Exception {
        interne(null, List.of(institution("BQ-SIM-01", "ACTIF"), institution("bad code!", "ACTIF")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(true))
            .andExpect(jsonPath("$.lignes[0].resultat").value("CREEE"))
            .andExpect(jsonPath("$.lignes[0].statut").value("ACTIF"))
            .andExpect(jsonPath("$.lignes[1].resultat").value("REFUSEE"))
            .andExpect(jsonPath("$.lignes[1].code").value("PAR_REPRISE_LIGNE_INVALIDE"))
            .andExpect(jsonPath("$.lignes[1].details.champs.codeMembre").exists())
            .andExpect(jsonPath("$.totaux.CREEE").value(1))
            .andExpect(jsonPath("$.totaux.REFUSEE").value(1));
        assertThat(institutions()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from outbox_event", Long.class)).isZero();
    }

    @Test
    @DisplayName("Création réelle avec l'UUID fourni, statut cible par transition de reprise, puis réimport inchangé")
    void creationPuisIdempotence() throws Exception {
        Map<String, Object> l = avec(
            institution("BQ-REP-01", "ACTIF"),
            "id", UUID_COMPTES.toString(),
            "groupe", "  Groupe   Fictif ",
            "dateAdhesion", "2021-03-15",
            "referenceDecision", "PV-DDRM-2021-007",
            "dateDecision", "2022-01-10",
            "modeAdhesion", "DIRECT",
            "compteReglement", "CR-0001",
            "servicesSouscrits", List.of(Map.of("code", "INC_ACCOUNT_INQUIRY", "depuis", "2021-04-01"), Map.of("code", "PAIEMENT_MOBILE"))
        );
        interne(false, List.of(l))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(false))
            .andExpect(jsonPath("$.lignes[0].resultat").value("CREEE"))
            .andExpect(jsonPath("$.lignes[0].id").value(UUID_COMPTES.toString()));
        Map<String, Object> ligne = jdbc.queryForMap("select id, status, group_name, joined_on, compliance_state, participant_code from institution");
        assertThat(ligne.get("id")).isEqualTo(UUID_COMPTES);
        assertThat(ligne).containsEntry("status", "ACTIF").containsEntry("group_name", "Groupe Fictif").containsEntry("compliance_state", "CONFORME");
        assertThat(ligne.get("joined_on").toString()).isEqualTo("2021-03-15");
        Map<String, Object> t = jdbc.queryForMap("select from_status, to_status, automatic_transition, reason from status_transition");
        assertThat(t).containsEntry("from_status", "CANDIDAT").containsEntry("to_status", "ACTIF").containsEntry("automatic_transition", true);
        assertThat((String) t.get("reason")).contains("Reprise de l'existant", "PV-DDRM-2021-007", "2022-01-10");
        assertThat(jdbc.queryForList("select event_type from outbox_event", String.class)).contains(
            "participant.institution-registered.v1",
            "participant.status-changed.v1"
        );
        assertThat(jdbc.queryForObject("select count(*) from subscribed_service", Long.class)).isEqualTo(2);
        long evenements = jdbc.queryForObject("select count(*) from outbox_event", Long.class);

        interne(false, List.of(l)).andExpect(jsonPath("$.lignes[0].resultat").value("INCHANGEE"));
        assertThat(jdbc.queryForObject("select count(*) from outbox_event", Long.class)).isEqualTo(evenements);

        // Lecture interne par code membre (MS-HOMOLOGATION, reprise des liaisons).
        mvc
            .perform(get("/api/v1/interne/institutions").param("codeParticipant", "BQ-REP-01").with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(UUID_COMPTES.toString()))
            .andExpect(jsonPath("$.statut").value("ACTIF"));
        mvc
            .perform(get("/api/v1/interne/institutions").param("codeParticipant", "INCONNU").with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Mise à jour : fiche, statut atteignable et service ajouté ; statut en arrière et UUID divergent refusés")
    void miseAJour() throws Exception {
        interne(false, List.of(institution("BQ-MAJ-01", "ADHERENT"))).andExpect(jsonPath("$.lignes[0].resultat").value("CREEE"));
        interne(false, List.of(avec(institution("BQ-MAJ-01", "ACTIF"), "sigle", "BMF", "servicesSouscrits", List.of(Map.of("code", "CARTE_INTERBANCAIRE")))))
            .andExpect(jsonPath("$.lignes[0].resultat").value("MISE_A_JOUR"))
            .andExpect(jsonPath("$.lignes[0].changements", hasItem("statut")))
            .andExpect(jsonPath("$.lignes[0].changements", hasItem("sigle")))
            .andExpect(jsonPath("$.lignes[0].changements", hasItem("servicesSouscrits")));
        assertThat(jdbc.queryForObject("select status from institution", String.class)).isEqualTo("ACTIF");
        assertThat(jdbc.queryForList("select event_type from outbox_event", String.class)).contains("participant.institution-updated.v1");
        interne(false, List.of(institution("BQ-MAJ-01", "ADHERENT")))
            .andExpect(jsonPath("$.lignes[0].resultat").value("REFUSEE"))
            .andExpect(jsonPath("$.lignes[0].code").value("PAR_REPRISE_STATUT_INATTEIGNABLE"));
        interne(false, List.of(avec(institution("BQ-MAJ-01", "ACTIF"), "id", UUID.randomUUID().toString())))
            .andExpect(jsonPath("$.lignes[0].code").value("PAR_REPRISE_ID_DIVERGENT"));
        UUID pris = UUID.fromString(jdbc.queryForObject("select id::text from institution", String.class));
        interne(false, List.of(avec(institution("BQ-MAJ-02", "ACTIF"), "id", pris.toString())))
            .andExpect(jsonPath("$.lignes[0].code").value("PAR_REPRISE_ID_DEJA_PRIS"));
    }

    @Test
    @DisplayName("Lot : banque marraine reprise plus haut, doublon de fichier, banque inconnue, partenaire hors CEMAC du groupe MFSA")
    void lot() throws Exception {
        List<Map<String, Object>> lignes = new ArrayList<>();
        lignes.add(institution("BQ-MAR-01", "ACTIF"));
        lignes.add(avec(institution("EP-IND-01", "EN_HOMOLOGATION"), "categorie", "ETABLISSEMENT_PAIEMENT", "modeAdhesion", "INDIRECT", "banqueMarraine", "BQ-MAR-01", "compteCantonnement", "CC-01"));
        lignes.add(institution("BQ-MAR-01", "ACTIF"));
        lignes.add(avec(institution("EP-IND-02", "ADHERENT"), "banqueMarraine", "BQ-ABSENTE"));
        lignes.add(avec(institution("REM-FR-01", "ACTIF"), "pays", "FR", "categorie", "PARTENAIRE_REMITTANCE", "groupe", "MFSA"));
        lignes.add(avec(institution("FACT-01", "ACTIF"), "categorie", "FACTURIER"));
        interne(true, lignes)
            .andExpect(jsonPath("$.lignes[1].resultat").value("CREEE"))
            .andExpect(jsonPath("$.lignes[1].avertissements", hasItem(org.hamcrest.Matchers.containsString("BQ-MAR-01"))))
            .andExpect(jsonPath("$.lignes[2].code").value("PAR_REPRISE_DOUBLON_FICHIER"))
            .andExpect(jsonPath("$.lignes[3].code").value("PAR_REPRISE_BANQUE_INCONNUE"))
            .andExpect(jsonPath("$.lignes[4].resultat").value("CREEE"))
            .andExpect(jsonPath("$.lignes[4].avertissements", hasItem(org.hamcrest.Matchers.containsString("hors CEMAC"))))
            .andExpect(jsonPath("$.lignes[5].resultat").value("CREEE"));
        assertThat(institutions()).isZero();
        interne(false, lignes).andExpect(jsonPath("$.totaux.CREEE").value(4)).andExpect(jsonPath("$.totaux.REFUSEE").value(2));
        assertThat(jdbc.queryForObject("select m.participant_code from institution i join institution m on m.id = i.sponsor_bank_id where i.participant_code = 'EP-IND-01'", String.class)).isEqualTo("BQ-MAR-01");
        assertThat(jdbc.queryForObject("select group_name from institution where participant_code = 'REM-FR-01'", String.class)).isEqualTo("MFSA");
    }

    @Test
    @DisplayName("Accès : REF_REPRISE_IMPORT exigée ; super administrateur à deux facteurs sur la route métier ; lot vide refusé")
    void acces() throws Exception {
        reprendre("/api/v1/interne/reprise/institutions", jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ")), true, List.of(institution("X-1", "ACTIF")))
            .andExpect(status().isForbidden());
        reprendre("/api/v1/reprise/institutions", utilisateur(UUID.randomUUID(), "PAR_INSTITUTION_CREATE"), true, List.of(institution("X-1", "ACTIF")))
            .andExpect(status().isForbidden());
        reprendre("/api/v1/reprise/institutions", utilisateur(UUID.randomUUID(), "REF_REPRISE_IMPORT"), true, List.of(institution("X-1", "ACTIF")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lignes[0].resultat").value("CREEE"));
        reprendre("/api/v1/reprise/institutions", compteReprise(), true, List.of(institution("X-1", "ACTIF"))).andExpect(status().isForbidden());
        interne(true, List.of()).andExpect(status().isBadRequest());
    }
}
