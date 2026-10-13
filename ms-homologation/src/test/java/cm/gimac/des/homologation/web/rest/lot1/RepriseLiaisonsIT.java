package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reprise des projets de certification existants (D-107) sur PostgreSQL réel : projet à qualifier, détaillé opération
 * par opération, repris en production, bloqué ; idempotence par référence d'origine ; opération traitée sur la
 * plateforme jamais réécrite ; simulation sans écriture ; refus par ligne. Données fictives.
 */
class RepriseLiaisonsIT extends AbstractHomologationIT {

    static final ObjectMapper JSON = JsonMapper.builder().build();
    static final UUID COMPTE_REPRISE = UUID.randomUUID();

    @Autowired
    ParticipantStubAdapter registre;

    UUID active;

    static RequestPostProcessor compteReprise() {
        return jwt()
            .jwt(j -> j.subject(COMPTE_REPRISE.toString()).claim("preferred_username", "service-account-reprise-service"))
            .authorities(new SimpleGrantedAuthority("REF_REPRISE_IMPORT"));
    }

    @BeforeEach
    void preparer() {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        active = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(active, "Banque active fictive", "ACTIF", "BANQUE", List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2024-01-01"))), "BQ-ACT-01");
        registre.enregistrer(new InstitutionView(UUID.randomUUID(), "Candidat fictif", "CANDIDAT", "BANQUE", List.of()), "BQ-CAND-01");
    }

    static Map<String, Object> liaison(String reference, Object... cleValeur) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("referenceOrigine", reference);
        l.put("codeMembre", "BQ-ACT-01");
        l.put("typeProjet", "PAIEMENT_MOBILE");
        l.put("intitule", "Projet Mobile fictif " + reference);
        l.put("origineEtat", "Suivi Excel");
        l.put("dateOuverture", "2026-04-01");
        for (int k = 0; k < cleValeur.length; k += 2) {
            l.put((String) cleValeur[k], cleValeur[k + 1]);
        }
        return l;
    }

    static Map<String, Object> op(String rang, String etat, String commentaire) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("rang", rang);
        o.put("etat", etat);
        o.put("faiteLe", "2026-05-02");
        o.put("commentaire", commentaire);
        return o;
    }

    ResultActions reprendre(String chemin, RequestPostProcessor jeton, Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        Map<String, Object> corps = new LinkedHashMap<>();
        if (simulation != null) {
            corps.put("simulation", simulation);
        }
        corps.put("liaisons", lignes);
        return mvc.perform(post(chemin).with(jeton).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(corps)));
    }

    ResultActions interne(Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return reprendre("/api/v1/interne/reprise/liaisons", compteReprise(), simulation, lignes);
    }

    String liaisonId(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.lignes[0].liaisonId");
    }

    List<String> rangs(String liaisonId) {
        return jdbc.queryForList("select rang from parcours_operation where liaison_id = ?::uuid order by ordre", String.class, liaisonId);
    }

    @Test
    @DisplayName("Simulation par défaut : rapport complet, aucune liaison écrite")
    void simulation() throws Exception {
        interne(null, List.of(liaison("SIM-01"), liaison("SIM-02", "codeMembre", "BQ-INCONNUE"), liaison("SIM-01")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation").value(true))
            .andExpect(jsonPath("$.lignes[0].resultat").value("CREEE"))
            .andExpect(jsonPath("$.lignes[0].statut").value("CADREE"))
            .andExpect(jsonPath("$.lignes[1].code").value("HOM_LIAISON_INSTITUTION_UNKNOWN"))
            .andExpect(jsonPath("$.lignes[2].code").value("HOM_REPRISE_DOUBLON_FICHIER"));
        assertThat(jdbc.queryForObject("select count(*) from liaison where reference_origine like 'SIM-%'", Long.class)).isZero();
    }

    @Test
    @DisplayName("À qualifier, sans conducteur : liaison cadrée, conducteur à désigner, origine conservée, réimport inchangé")
    void aQualifier() throws Exception {
        String id = liaisonId(interne(false, List.of(liaison("QUAL-01"))).andExpect(jsonPath("$.lignes[0].resultat").value("CREEE")));
        assertThat(jdbc.queryForObject("select origine_etat from liaison where id = ?::uuid", String.class, id)).isEqualTo("Suivi Excel");
        assertThat(jdbc.queryForObject("select conducteur_a_designer from parcours_liaison where liaison_id = ?::uuid", Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForObject("select date_demarrage from parcours_liaison where liaison_id = ?::uuid", LocalDate.class, id)).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ?", String.class, id)).contains("homologation.liaison-opened.v1");
        interne(false, List.of(liaison("QUAL-01"))).andExpect(jsonPath("$.lignes[0].resultat").value("INCHANGEE"));
    }

    @Test
    @DisplayName("Détaillé : opérations faites avec preuve « reprise », en cours, ressource et conducteur ; mise à jour ; opération traitée sur la plateforme protégée")
    void detaille() throws Exception {
        String id = liaisonId(interne(false, List.of(liaison("DET-01", "conducteurId", CollaboratorStubAdapter.CONDUCTEUR_1.toString()))));
        List<String> rangs = rangs(id);
        List<Map<String, Object>> ops = new ArrayList<>();
        ops.add(op(rangs.get(0), "FAITE", "Compte rendu de lancement du 02/05"));
        ops.add(op(rangs.get(1), "EN_COURS", null));
        Map<String, Object> l = liaison(
            "DET-01",
            "conducteurId", CollaboratorStubAdapter.CONDUCTEUR_1.toString(),
            "ressources", List.of(Map.of("fonction", "SNA", "compteId", CollaboratorStubAdapter.CONDUCTEUR_2.toString())),
            "services", List.of(Map.of("code", "PAIEMENT_MOBILE", "commentaire", "12 cas réussis, 1 en attente")),
            "operations", ops
        );
        interne(false, List.of(l))
            .andExpect(jsonPath("$.lignes[0].resultat").value("MISE_A_JOUR"))
            .andExpect(jsonPath("$.lignes[0].operationsModifiees").value(2));
        String preuve = jdbc.queryForObject("select preuve::text from parcours_operation where liaison_id = ?::uuid and rang = ?", String.class, id, rangs.get(0));
        assertThat(preuve).contains("\"reprise\"", "Compte rendu de lancement");
        assertThat(jdbc.queryForObject("select etat from parcours_operation where liaison_id = ?::uuid and rang = ?", String.class, id, rangs.get(1))).isEqualTo("EN_COURS");
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ?", String.class, id)).contains("homologation.operation-closed.v1");
        interne(false, List.of(l)).andExpect(jsonPath("$.lignes[0].resultat").value("INCHANGEE"));

        // Une opération faite sur la plateforme (preuve non reprise) n'est jamais réécrite par la reprise.
        // La réserve de connexions n'est pas en validation automatique : validation explicite.
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
            try (var st = c.prepareStatement("update parcours_operation set preuve = null where liaison_id = ?::uuid and rang = ?")) {
                st.setString(1, id);
                st.setString(2, rangs.get(0));
                st.executeUpdate();
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
            return null;
        });
        ops.set(0, op(rangs.get(0), "A_FAIRE", null));
        interne(false, List.of(l))
            .andExpect(jsonPath("$.lignes[0].resultat").value("REFUSEE"))
            .andExpect(jsonPath("$.lignes[0].code").value("HOM_REPRISE_OPERATION_TRAITEE"));
        // Même référence, autre type de projet : refus.
        interne(false, List.of(liaison("DET-01", "typeProjet", "CARTE_INTERBANCAIRE"))).andExpect(jsonPath("$.lignes[0].code").value("HOM_REPRISE_LIAISON_DIVERGENTE"));
    }

    @Test
    @DisplayName("En production : opérations jusqu'au jalon faites, statut EN_PRODUCTION, liaison close, jalon publié")
    void enProduction() throws Exception {
        String id = liaisonId(
            interne(false, List.of(liaison("PROD-01", "avancement", "EN_PRODUCTION", "dateMiseEnProduction", "2025-11-30", "origineEtat", "Payway production")))
                .andExpect(jsonPath("$.lignes[0].resultat").value("CREEE"))
                .andExpect(jsonPath("$.lignes[0].statut").value("EN_PRODUCTION"))
                .andExpect(jsonPath("$.lignes[0].etat").value("CLOSE"))
        );
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ?", String.class, id)).contains("homologation.liaison-milestone-reached.v1");
    }

    @Test
    @DisplayName("Bloqué : blocage daté déclaré, opérations ouvertes bloquées")
    void bloque() throws Exception {
        String id = liaisonId(interne(false, List.of(liaison("BLQ-01"))));
        String premier = rangs(id).get(0);
        interne(false, List.of(liaison("BLQ-01", "operations", List.of(op(premier, "BLOQUEE", "En attente du certificat de la BEAC")))))
            .andExpect(jsonPath("$.lignes[0].resultat").value("MISE_A_JOUR"));
        assertThat(jdbc.queryForObject("select bloquee from parcours_liaison where liaison_id = ?::uuid", Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForObject("select motif from parcours_blocage where liaison_id = ?::uuid", String.class, id)).contains("certificat de la BEAC");
    }

    @Test
    @DisplayName("Refus : institution candidate, opération inconnue, ligne invalide ; accès réservé à REF_REPRISE_IMPORT")
    void refus() throws Exception {
        interne(false, List.of(
            liaison("REF-01", "codeMembre", "BQ-CAND-01"),
            liaison("REF-02", "operations", List.of(op("ZZ99", "FAITE", null))),
            liaison("réf ?", "typeProjet", "x")
        ))
            .andExpect(jsonPath("$.lignes[0].code").value("HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE"))
            .andExpect(jsonPath("$.lignes[1].code").value("HOM_REPRISE_OPERATION_INCONNUE"))
            .andExpect(jsonPath("$.lignes[2].code").value("HOM_REPRISE_LIGNE_INVALIDE"))
            .andExpect(jsonPath("$.totaux.REFUSEE").value(3));
        reprendre("/api/v1/interne/reprise/liaisons", jwt().authorities(new SimpleGrantedAuthority("INTERNAL_HOMOLOGATION_READ")), true, List.of(liaison("X-1")))
            .andExpect(status().isForbidden());
        reprendre("/api/v1/reprise/liaisons", compteReprise(), true, List.of(liaison("X-1"))).andExpect(status().isForbidden());
        reprendre("/api/v1/reprise/liaisons", utilisateur(UUID.randomUUID(), "REF_REPRISE_IMPORT"), true, List.of(liaison("X-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lignes[*].resultat", hasItem("CREEE")));
    }
}
