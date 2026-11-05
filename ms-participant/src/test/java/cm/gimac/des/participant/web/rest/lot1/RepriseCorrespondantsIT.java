package cm.gimac.des.participant.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.AbstractParticipantIT;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reprise des correspondants (D-132) : simulation sans écriture, déclaration du correspondant actif lu par la fiche
 * interne de la Sandbox GIMAC, idempotence par institution et compte, mise à jour et réactivation, refus par ligne.
 * Données fictives (codes 99xxx).
 */
class RepriseCorrespondantsIT extends AbstractParticipantIT {

    static final ObjectMapper JSON = JsonMapper.builder().build();
    static final UUID COMPTE = UUID.fromString("0a0b0c0d-0e0f-4a1b-8c2d-3e4f5a6b7c8d");

    static RequestPostProcessor compteReprise() {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("preferred_username", "service-account-reprise-service"))
            .authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT"));
    }

    static Map<String, Object> correspondant(String code, UUID compte, Object... cleValeur) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("codeMembre", code);
        c.put("compteId", compte == null ? null : compte.toString());
        c.put("nom", "Awa Fictive");
        for (int k = 0; k < cleValeur.length; k += 2) {
            c.put((String) cleValeur[k], cleValeur[k + 1]);
        }
        return c;
    }

    ResultActions envoyer(String chemin, String nature, Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        Map<String, Object> corps = new LinkedHashMap<>();
        if (simulation != null) {
            corps.put("simulation", simulation);
        }
        corps.put(nature, lignes);
        return mvc.perform(post(chemin).with(compteReprise()).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(corps)));
    }

    ResultActions reprendre(Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return envoyer("/api/v1/interne/reprise/correspondants", "correspondants", simulation, lignes);
    }

    @BeforeEach
    void institutions() throws Exception {
        envoyer(
            "/api/v1/interne/reprise/institutions",
            "institutions",
            false,
            List.of(
                Map.of("codeMembre", "99001", "nom", "Banque fictive 99001", "pays", "CM", "categorie", "BANQUE", "statut", "EN_HOMOLOGATION"),
                Map.of("codeMembre", "99002", "nom", "Microfinance fictive 99002", "pays", "CM", "categorie", "MICROFINANCE", "statut", "CLOS_SANS_SUITE")
            )
        ).andExpect(status().isOk());
    }

    long correspondants() {
        return jdbc.queryForObject("select count(*) from correspondent", Long.class);
    }

    @Test
    @DisplayName("Simulation (par défaut) : rapport complet, aucun correspondant écrit")
    void simulation() throws Exception {
        reprendre(null, List.of(correspondant("99001", COMPTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(true))
            .andExpect(jsonPath("$.lignes[0].resultat").value("CREE"))
            .andExpect(jsonPath("$.totaux.CREE").value(1));
        assertThat(correspondants()).isZero();
    }

    @Test
    @DisplayName("Déclaration réelle : correspondant actif lu par la fiche interne, puis réimport inchangé, puis mise à jour (réactivation : test unitaire)")
    void declarationIdempotenceMiseAJour() throws Exception {
        reprendre(false, List.of(correspondant("99001", COMPTE, "email", "Admin@Banque-99001.invalid")))
            .andExpect(jsonPath("$.lignes[0].resultat").value("CREE"));
        Map<String, Object> c = jdbc.queryForMap("select full_name, duty, channels, reachability, active, user_id, email, on_call_247 from correspondent");
        assertThat(c)
            .containsEntry("full_name", "Awa Fictive")
            .containsEntry("duty", "MONETIQUE")
            .containsEntry("channels", "messagerie")
            .containsEntry("reachability", "NON_VERIFIE")
            .containsEntry("active", true)
            .containsEntry("user_id", COMPTE)
            .containsEntry("email", "admin@banque-99001.invalid")
            .containsEntry("on_call_247", false);
        assertThat(jdbc.queryForObject("select count(*) from interaction where summary like 'Correspondant repris%'", Long.class)).isEqualTo(1);
        mvc
            .perform(get("/api/v1/interne/institutions/code/99001/fiche").with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correspondants[0].compteId").value(COMPTE.toString()));

        reprendre(false, List.of(correspondant("99001", COMPTE))).andExpect(jsonPath("$.lignes[0].resultat").value("INCHANGE"));

        reprendre(false, List.of(correspondant("99001", COMPTE, "fonction", "direction", "telephone", "+237 000 000 001", "astreinte247", true, "canaux", "messagerie, téléphone")))
            .andExpect(jsonPath("$.lignes[0].resultat").value("MIS_A_JOUR"))
            .andExpect(jsonPath("$.lignes[0].changements.length()").value(4));
        assertThat(correspondants()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select active from correspondent", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("Refus par ligne : institution inconnue ou close, champs manquants, fonction et courriel invalides, doublon")
    void refus() throws Exception {
        UUID autre = UUID.randomUUID();
        reprendre(
            false,
            List.of(
                correspondant("99009", COMPTE),
                correspondant("99002", COMPTE),
                correspondant(" ", null, "nom", " "),
                correspondant("99001", autre, "fonction", "PRESIDENT"),
                correspondant("99001", UUID.randomUUID(), "email", "pas-un-courriel"),
                correspondant("99001", COMPTE),
                correspondant("99001", COMPTE)
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lignes[0].code").value("PAR_REPRISE_INSTITUTION_INCONNUE"))
            .andExpect(jsonPath("$.lignes[1].code").value("PAR_REPRISE_INSTITUTION_CLOSE"))
            .andExpect(jsonPath("$.lignes[2].code").value("PAR_REPRISE_CHAMP_OBLIGATOIRE"))
            .andExpect(jsonPath("$.lignes[2].details.champs.length()").value(3))
            .andExpect(jsonPath("$.lignes[3].code").value("PAR_REPRISE_FONCTION_INCONNUE"))
            .andExpect(jsonPath("$.lignes[4].code").value("PAR_REPRISE_COURRIEL_INVALIDE"))
            .andExpect(jsonPath("$.lignes[5].resultat").value("CREE"))
            .andExpect(jsonPath("$.lignes[6].code").value("PAR_REPRISE_DOUBLON_FICHIER"))
            .andExpect(jsonPath("$.totaux.REFUSE").value(6));
        assertThat(correspondants()).isEqualTo(1);
    }

    @Test
    @DisplayName("Lot vide refusé ; route métier : second facteur exigé")
    void lotVideEtRouteMetier() throws Exception {
        reprendre(false, List.of()).andExpect(status().isBadRequest());
        String corps = JSON.writeValueAsString(Map.of("simulation", false, "correspondants", List.of(correspondant("99001", COMPTE))));
        mvc
            .perform(post("/api/v1/reprise/correspondants").with(jwt().authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT"))).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                post("/api/v1/reprise/correspondants")
                    .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2")).authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corps)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lignes[0].resultat").value("CREE"));
        assertThat(correspondants()).isEqualTo(1);
    }
}
