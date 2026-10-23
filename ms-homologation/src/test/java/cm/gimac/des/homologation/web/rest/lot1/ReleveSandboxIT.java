package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.application.usecase.sandbox.ReleveSandbox;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.DocumentStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.sandbox.LectureSandboxStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * US-EXI-14 sur PostgreSQL, SBX-LECTURE de substitution au contrat exact : relève d'une campagne de l'institution 99001
 * par pages, plus haute position retenue par exécution sans omission ni doublon, reprise après 503 depuis la dernière
 * position retenue, pièce déjà conservée non redéposée, empreinte divergente écartée, scénario inconnu isolé, taux de
 * réussite d'une campagne terminée repris comme mesure de certification, route de lecture de l'application du DES.
 */
class ReleveSandboxIT extends AbstractHomologationIT {

    static final String C = "0192f0aa-0000-7000-8000-0000000c1401";
    static final String BILAN = "{\"bilan\":\"CMP-99001/MOB\",\"version\":1}";

    @Autowired
    ParticipantStubAdapter registre;

    @Autowired
    LectureSandboxStubAdapter sandbox;

    @Autowired
    DocumentStubAdapter documents;

    @Autowired
    ReleveSandbox releve;

    @Autowired
    cm.gimac.des.homologation.infrastructure.messaging.consumer.FinCampagneSandboxConsumer finCampagne;

    @Autowired
    org.springframework.transaction.support.TransactionTemplate transaction;

    /** Écriture validée (la réserve de connexions n'est pas en validation automatique). */
    void ecrire(String sql, Object... args) {
        transaction.executeWithoutResult(s -> jdbc.update(sql, args));
    }

    UUID liaison;

    @BeforeEach
    void ouvrir() throws Exception {
        sandbox.vider();
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        UUID institution = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(institution, "Institution 99001", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2026-09-01"))), "99001");
        String corps = mvc
            .perform(
                post("/api/v1/liaisons")
                    .with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"institutionId\":\"%s\",\"typeProjet\":\"PAIEMENT_MOBILE\",\"chefDeProjetId\":\"%s\"}".formatted(institution, CollaboratorStubAdapter.CONDUCTEUR_1))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        liaison = UUID.fromString(JsonPath.read(corps, "$.id"));
        // Deux scénarios du référentiel de la liaison : SCN-MOB-001 et SCN-MOB-002.
        List<String> codes = jdbc.queryForList("select o.code from reference_operation o join liaison l on l.version_id = o.version_id where l.id = ? order by o.code limit 2", String.class, liaison);
        ecrire("update reference_operation o set id_scenario = 'SCN-MOB-001' from liaison l where l.id = ? and o.version_id = l.version_id and o.code = ?", liaison, codes.get(0));
        ecrire("update reference_operation o set id_scenario = 'SCN-MOB-002' from liaison l where l.id = ? and o.version_id = l.version_id and o.code = ?", liaison, codes.get(1));
    }

    @AfterEach
    void fin() {
        sandbox.vider();
    }

    /** 45 exécutions (90 enregistrements : création puis validation), dont deux d'un scénario inconnu. */
    LectureSandboxStubAdapter.CampagneServie campagne() {
        var c = sandbox.alimenter(liaison, C, "99001", "MOB", List.of("RETRAIT", "TRANSFERT"));
        for (int i = 1; i <= 45; i++) {
            String scenario = i <= 2 ? "SCN-INCONNU-" + i : (i % 2 == 0 ? "SCN-MOB-001" : "SCN-MOB-002");
            c.executer("EXE-%06d".formatted(i), scenario, i % 7 == 0 ? "FAILED" : "PASSED", i % 7 == 0 ? "defavorable" : "favorable", "2026-10-13T16:%02d:07Z".formatted(i % 60));
        }
        return c;
    }

    long compter(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    @Test
    @DisplayName("CA1 nominal : relève des exécutions, plus haute position retenue sans omission ni doublon, campaign-result-imported publié")
    void ca1ReleveDesExecutions() {
        campagne();
        assertThat(releve.releverLiaison(liaison).issue()).isEqualTo(ReleveSandbox.Issue.REUSSIE);

        assertThat(compter("select count(*) from sbx_execution where campagne_id = ?", C)).isEqualTo(45);
        assertThat(compter("select count(*) from sbx_execution where campagne_id = ? and nature = 'VALIDATION' and verdict_natif <> 'PENDING'", C)).isEqualTo(45);
        assertThat(compter("select count(*) from sbx_execution where campagne_id = ? and operation_code is not null", C)).isEqualTo(43);
        String evenement = jdbc.queryForObject("select payload::text from outbox_event where event_type = 'homologation.campaign-result-imported.v1' and aggregate_id = ?", String.class, C);
        assertThat(JsonPath.<Integer>read(evenement, "$.enregistrementsLus")).isEqualTo(90);
        assertThat(JsonPath.<Integer>read(evenement, "$.executionsRetenues")).isEqualTo(45);
        assertThat(JsonPath.<Integer>read(evenement, "$.scenariosNonRapproches")).isEqualTo(2);
        assertThat(JsonPath.<String>read(evenement, "$.liaisonId")).isEqualTo(liaison.toString());

        // Lecteur à jour : relève suivante sans enregistrement ni événement.
        sandbox.alimenter(liaison, "0192f0aa-0000-7000-8000-0000000c1402", "99001", "CAR", List.of()).signaler("COLLISION_CODE_PARTICIPANT");
        assertThat(releve.releverLiaison(liaison).enregistrements()).isZero();
        assertThat(compter("select count(*) from outbox_event where event_type = 'homologation.campaign-result-imported.v1' and aggregate_id = ?", C)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select etat from sbx_campagne where campagne_id = '0192f0aa-0000-7000-8000-0000000c1402'", String.class)).isEqualTo("SIGNALEE");
    }

    @Test
    @DisplayName("CA2 alternatif : 503 avec délai au milieu de la relève, reprise depuis la dernière position retenue sans perte")
    void ca2IndisponibiliteDeLaSandbox() {
        campagne();
        // Liste, détail et première page servis, puis 503 sur la deuxième page.
        sandbox.indisponibleApres(3, 1, 503, Duration.ZERO);
        assertThat(releve.releverLiaison(liaison).issue()).isEqualTo(ReleveSandbox.Issue.ECHEC);
        assertThat(compter("select count(*) from sbx_execution where campagne_id = ?", C)).isEqualTo(20);
        assertThat(jdbc.queryForObject("select dernier_echec is not null and derniere_releve_reussie is null from sbx_releve_liaison where liaison_id = ?", Boolean.class, liaison)).isTrue();

        // Délai indicatif plus long : la relève est reportée tant qu'il court.
        sandbox.indisponible(1, 503, Duration.ofHours(1));
        assertThat(releve.releverLiaison(liaison).issue()).isEqualTo(ReleveSandbox.Issue.ECHEC);
        assertThat(releve.releverLiaison(liaison).issue()).isEqualTo(ReleveSandbox.Issue.REPORTEE);
        ecrire("update sbx_releve_liaison set reprendre_apres = null where liaison_id = ?", liaison);

        assertThat(releve.releverLiaison(liaison).issue()).isEqualTo(ReleveSandbox.Issue.REUSSIE);
        assertThat(compter("select count(*) from sbx_execution where campagne_id = ?", C)).isEqualTo(45);
        assertThat(compter("select count(*) from sbx_execution where campagne_id = ? and verdict_natif = 'PENDING'", C)).isZero();
    }

    @Test
    @DisplayName("CA3 alternatif : bilan déjà conservé dans MS-DOCUMENT, aucune pièce de même empreinte redéposée ; empreinte divergente écartée")
    void ca3PieceDejaConservee() throws Exception {
        campagne().piece("BILAN", 1, BILAN).piece("RESULTATS_DETAILLES", 1, "{\"r\":1}");
        UUID depose = UUID.randomUUID();
        documents.enregistrer(depose, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(BILAN.getBytes(StandardCharsets.UTF_8))));

        releve.releverLiaison(liaison);

        assertThat(jdbc.queryForObject("select statut from sbx_piece where campagne_id = ? and nature = 'BILAN'", String.class, C)).isEqualTo("DEJA_CONSERVEE");
        assertThat(jdbc.queryForObject("select document_id from sbx_piece where campagne_id = ? and nature = 'BILAN'", UUID.class, C)).isEqualTo(depose);
        assertThat(jdbc.queryForObject("select statut from sbx_piece where campagne_id = ? and nature = 'RESULTATS_DETAILLES'", String.class, C)).isEqualTo("DEPOSEE");

        // Nouvelle version dont l'empreinte annoncée diffère des octets : rien n'est déposé.
        var c2 = sandbox.alimenter(liaison, "0192f0aa-0000-7000-8000-0000000c1403", "99001", "MOB", List.of()).piece("BILAN", 1, "{\"autre\":1}");
        c2.falsifierEmpreinte("00".repeat(32));
        releve.releverLiaison(liaison);
        assertThat(jdbc.queryForObject("select statut from sbx_piece where campagne_id = '0192f0aa-0000-7000-8000-0000000c1403'", String.class)).isEqualTo("EMPREINTE_DIVERGENTE");
        assertThat(jdbc.queryForObject("select document_id is null from sbx_piece where campagne_id = '0192f0aa-0000-7000-8000-0000000c1403'", Boolean.class)).isTrue();
        // Pièces déjà inscrites : jamais relues ni redéposées.
        releve.releverLiaison(liaison);
        assertThat(compter("select count(*) from sbx_piece where campagne_id = ?", C)).isEqualTo(2);
    }

    @Test
    @DisplayName("CA4 erreur : scénario inconnu isolé parmi les scénarios non rapprochés, aucune écriture vers la sandbox")
    void ca4ScenarioInconnu() throws Exception {
        campagne();
        releve.releverLiaison(liaison);
        releve.releverLiaison(liaison);
        assertThat(jdbc.queryForList("select id_scenario from unmatched_scenario where liaison_id = ? order by id_scenario", String.class, liaison)).containsExactly("SCN-INCONNU-1", "SCN-INCONNU-2");
        assertThat(jdbc.queryForObject("select qualification from unmatched_scenario where liaison_id = ? limit 1", String.class, liaison)).isEqualTo("A_QUALIFIER");
        assertThat(compter("select count(*) from sbx_execution where campagne_id = ? and operation_code is null", C)).isEqualTo(2);

        // Écran DES-09 : résultats, scénarios non rapprochés, exécutions paginées ; lecture seule, droit exigé.
        mvc
            .perform(get("/api/v1/liaisons/{id}/sandbox", liaison).with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mention").value("REPRIS_NON_RECALCULE"))
            .andExpect(jsonPath("$.enEchec").value(false))
            .andExpect(jsonPath("$.campagnes[0].campagneId").value(C))
            .andExpect(jsonPath("$.campagnes[0].executions").value(45))
            .andExpect(jsonPath("$.campagnes[0].nonRapprochees").value(2))
            .andExpect(jsonPath("$.scenariosNonRapproches.length()").value(2));
        mvc
            .perform(get("/api/v1/liaisons/{id}/sandbox/campagnes/{c}/executions", liaison, C).param("page", "2").param("taille", "20").with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(45))
            .andExpect(jsonPath("$.contenu.length()").value(5))
            .andExpect(jsonPath("$.contenu[0].verdictNatif").exists())
            .andExpect(jsonPath("$.contenu[0].versionTable").value("1.0"));
        mvc.perform(get("/api/v1/liaisons/{id}/sandbox", liaison).with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_LIST"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/liaisons/{id}/sandbox", UUID.randomUUID()).with(utilisateur(UUID.randomUUID(), "HOM_LIAISON_READ"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Aptitude : taux de réussite par service d'une campagne terminée repris comme mesure de certification")
    void aptitudeDeduite() {
        campagne()
            .taux("RETRAIT", "REUSSITE", 20, 20, "100.00", "2026-10-13T17:00:00Z")
            .taux("TRANSFERT", "REUSSITE", 19, 20, "95.00", "2026-10-13T17:00:00Z")
            .taux(null, "AVANCEMENT", 45, 45, "100.00", "2026-10-13T17:00:00Z")
            .etat("TERMINEE", 1);
        releve.releverLiaison(liaison);
        assertThat(jdbc.queryForObject("select taux from coverage where liaison_id = ? and nature = 'CERTIFICATION' and service_code = 'RETRAIT'", java.math.BigDecimal.class, liaison)).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject("select numerateur from coverage where liaison_id = ? and nature = 'CERTIFICATION' and service_code = 'TRANSFERT'", Integer.class, liaison)).isEqualTo(19);
        assertThat(releve.releverTout()).isPositive();
    }

    static String notification(String id, String campagne) {
        return "{\"specversion\":\"1.0\",\"id\":\"" + id + "\",\"source\":\"/sbx-lecture\",\"type\":\"sbx-lecture.campaign-published.v1\",\"data\":{\"identifiantCampagne\":\"" + campagne + "\",\"motif\":\"FIN_DE_CAMPAGNE\"}}";
    }

    @Test
    @DisplayName("US-EXI-15 : relecture déclenchée par sbx-lecture.campaign-published.v1, accusé constaté, notification réémise sans second traitement")
    void exi15RelectureDeclenchee() {
        campagne().etat("TERMINEE", 1);
        String id = UUID.randomUUID().toString();

        finCampagne.recevoir(notification(id, C));

        assertThat(compter("select count(*) from sbx_execution where campagne_id = ?", C)).isEqualTo(45);
        assertThat(sandbox.lecturesDetail(C)).isEqualTo(1);
        assertThat(compter("select count(*) from processed_event where event_id = ?", id)).isEqualTo(1);
        assertThat(compter("select count(*) from outbox_event where event_type = 'homologation.campaign-result-imported.v1' and aggregate_id = ?", C)).isEqualTo(1);

        // Notification réémise à l'identique : aucune relecture.
        finCampagne.recevoir(notification(id, C));
        assertThat(sandbox.lecturesDetail(C)).isEqualTo(1);

        // Notification d'une campagne inconnue de la sandbox : retenue sans effet, la relève périodique reste la référence.
        String inconnue = UUID.randomUUID().toString();
        finCampagne.recevoir(notification(inconnue, "0192f0aa-0000-7000-8000-0000000c1499"));
        assertThat(compter("select count(*) from processed_event where event_id = ?", inconnue)).isEqualTo(1);
    }

    @Test
    @DisplayName("US-EXI-15 critère 3 : notification désactivée ou perdue, la relève périodique reprend la campagne sans perte")
    void exi15ReleveSansNotification() {
        campagne().etat("TERMINEE", 1);
        assertThat(releve.releverTout()).isPositive();
        assertThat(compter("select count(*) from sbx_execution where campagne_id = ?", C)).isEqualTo(45);
        assertThat(jdbc.queryForObject("select etat from sbx_campagne where campagne_id = ?", String.class, C)).isEqualTo("TERMINEE");
    }
}
