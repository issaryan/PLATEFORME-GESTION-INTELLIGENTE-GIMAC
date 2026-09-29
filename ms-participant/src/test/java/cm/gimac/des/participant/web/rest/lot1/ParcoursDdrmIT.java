package cm.gimac.des.participant.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.AbstractParticipantIT;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Parcours de la DDRM de bout en bout, sur PostgreSQL et Kafka réels (Testcontainers) : prise de
 * contact, dossier d'adhésion, conditions, proposition, prononcé en maker-checker, événement
 * participant.status-changed.v1 relayé sur le sujet participant.institution.
 */
class ParcoursDdrmIT extends AbstractParticipantIT {

    static final UUID CHARGE = UUID.randomUUID();
    static final UUID CHEF = UUID.randomUUID();

    static RequestPostProcessor charge() {
        return utilisateur(
            CHARGE,
            "PAR_INSTITUTION_READ",
            "PAR_INSTITUTION_CREATE",
            "PAR_INSTITUTION_EDIT",
            "PAR_ADHESION_INSTRUCT",
            "PAR_TRANSITION_REQUEST"
        );
    }

    static RequestPostProcessor chef() {
        return utilisateur(CHEF, "PAR_INSTITUTION_READ", "PAR_ADHESION_INSTRUCT", "PAR_TRANSITION_APPROVE");
    }

    static String candidature(String nom, String bic) {
        return """
            {"nom":"%s","pays":"CM","categorie":"BANQUE","codeIdentification":%s,"originePriseDeContact":"Courrier"}"""
            .formatted(nom, bic == null ? "null" : "\"" + bic + "\"");
    }

    String enregistrer(String nom, String bic) throws Exception {
        String corps = mvc
            .perform(post("/api/v1/institutions").with(charge()).contentType(MediaType.APPLICATION_JSON).content(candidature(nom, bic)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(corps, "$.id");
    }

    void deposerEtValider(String id, String code) throws Exception {
        String depot = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"documentSha256\":\"" + "d".repeat(64) + "\",\"nomFichier\":\"" + code + ".pdf\"}";
        mvc
            .perform(post("/api/v1/institutions/{id}/pieces/{code}/depot", id, code).with(charge()).contentType(MediaType.APPLICATION_JSON).content(depot))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("RECUE"));
        mvc
            .perform(post("/api/v1/institutions/{id}/pieces/{code}/validation", id, code).with(chef()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("VALIDEE"));
    }

    @Test
    @DisplayName("Étapes 1 et 2 : du candidat à l'adhérent, en maker-checker, avec l'événement relayé")
    void parcoursComplet() throws Exception {
        String id = enregistrer("Banque Parcours", "BPARCMCX");
        mvc
            .perform(get("/api/v1/institutions/{id}", id).with(charge()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.institution.statut").value("CANDIDAT"))
            .andExpect(jsonPath("$.pieces.length()").value(3))
            .andExpect(jsonPath("$.transitionsPermises[0]").value("ADHERENT"));

        // Dossier incomplet : proposition refusée avec la liste des manquants.
        mvc
            .perform(
                post("/api/v1/institutions/{id}/transitions", id)
                    .with(charge())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"vers\":\"ADHERENT\",\"motif\":\"Dossier complet\"}")
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PAR_PREREQUISITES_MISSING"));

        deposerEtValider(id, "DEMANDE_ADHESION");
        deposerEtValider(id, "STATUTS");
        mvc
            .perform(
                put("/api/v1/institutions/{id}/conditions-adhesion", id)
                    .with(charge())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"codeParticipant\":\"BQ-PAR-001\",\"mode\":\"DIRECT\",\"compteReglement\":\"BEAC-PAR-001\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codeParticipant").value("BQ-PAR-001"));

        String proposition = mvc
            .perform(
                post("/api/v1/institutions/{id}/transitions", id)
                    .with(charge())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"vers\":\"ADHERENT\",\"motif\":\"Dossier complet\"}")
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String t = JsonPath.read(proposition, "$.id");

        // Le chargé ne peut approuver sa propre proposition.
        mvc
            .perform(
                post("/api/v1/institutions/{id}/transitions/{t}/approbation", id, t)
                    .with(utilisateur(CHARGE, "PAR_TRANSITION_APPROVE"))
                    .header("X-Reauth-Assertion", "assertion")
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PAR_SEPARATION_OF_DUTIES"));

        // Sans réauthentification : 428.
        mvc.perform(post("/api/v1/institutions/{id}/transitions/{t}/approbation", id, t).with(chef())).andExpect(status().is(428));

        mvc
            .perform(post("/api/v1/institutions/{id}/transitions/{t}/approbation", id, t).with(chef()).header("X-Reauth-Assertion", "assertion"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("APPROUVEE"));

        mvc
            .perform(get("/api/v1/institutions/{id}", id).with(chef()))
            .andExpect(jsonPath("$.institution.statut").value("ADHERENT"))
            .andExpect(jsonPath("$.adhesion.prononceePar").value(CHEF.toString()))
            .andExpect(jsonPath("$.institution.conformite").value("CONFORME"));

        assertThat(
            jdbc.queryForList("select event_type from outbox_event where aggregate_id = ? order by created_at", String.class, id)
        ).contains(
            "participant.institution-registered.v1",
            "participant.adhesion-piece-recorded.v1",
            "participant.institution-updated.v1",
            "participant.transition-requested.v1",
            "participant.status-changed.v1"
        );
        assertThat(jdbc.queryForObject("select count(*) from interaction where institution_id = ?::uuid", Integer.class, id)).isEqualTo(2);

        relais.relayerJusquaEnvoi(id);
        assertThat(relais.lire("participant.institution", id)).anySatisfy(r -> {
            assertThat(JsonPath.<String>read(r.value(), "$.type")).isIn("participant.institution-registered.v1", "participant.status-changed.v1");
            assertThat(JsonPath.<String>read(r.value(), "$.specversion")).isEqualTo("1.0");
        });
    }

    @Test
    @DisplayName("PAR-01 (intégration) : unicité des codes sous créations concurrentes")
    void uniciteSousConcurrence() throws Exception {
        int n = 6;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch depart = new CountDownLatch(1);
        List<Future<Integer>> resultats = new ArrayList<>();
        try {
            for (int i = 0; i < n; i++) {
                int k = i;
                Callable<Integer> tache = () -> {
                    depart.await();
                    return mvc
                        .perform(post("/api/v1/institutions").with(charge()).contentType(MediaType.APPLICATION_JSON).content(candidature("Concurrente " + k, "BCONCMCX")))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                };
                resultats.add(pool.submit(tache));
            }
            depart.countDown();
            List<Integer> statuts = new ArrayList<>();
            for (Future<Integer> f : resultats) {
                statuts.add(f.get());
            }
            assertThat(statuts).containsOnly(201, 409);
            assertThat(statuts).filteredOn(s -> s == 201).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select count(*) from institution where identification_code = 'BCONCMCX'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Liste paginée filtrée et compteurs de la DDRM")
    void listeEtIndicateurs() throws Exception {
        enregistrer("Microfinance Liste", null);
        mvc
            .perform(get("/api/v1/institutions").param("statut", "CANDIDAT").param("q", "liste").param("mesDossiers", "true").with(charge()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].nom").value("Microfinance Liste"));
        mvc.perform(get("/api/v1/institutions/indicateurs").with(charge())).andExpect(status().isOk()).andExpect(jsonPath("$.candidats").isNumber());
    }

    @Test
    @DisplayName("Clôture sans suite d'un candidat, motif consigné")
    void clotureSansSuite() throws Exception {
        String id = enregistrer("Candidat Abandon", null);
        String t = JsonPath.read(
            mvc
                .perform(
                    post("/api/v1/institutions/{id}/transitions", id)
                        .with(charge())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vers\":\"CLOS_SANS_SUITE\",\"motif\":\"Projet abandonné par le candidat\"}")
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        mvc
            .perform(post("/api/v1/institutions/{id}/transitions/{t}/approbation", id, t).with(chef()).header("X-Reauth-Assertion", "a"))
            .andExpect(status().isOk());
        mvc
            .perform(get("/api/v1/institutions/{id}", id).with(chef()))
            .andExpect(jsonPath("$.institution.statut").value("CLOS_SANS_SUITE"))
            .andExpect(jsonPath("$.adhesion.motifCloture").value("Projet abandonné par le candidat"))
            .andExpect(jsonPath("$.transitionsPermises.length()").value(0));
        mvc
            .perform(put("/api/v1/institutions/{id}", id).with(charge()).contentType(MediaType.APPLICATION_JSON).content(candidature("Candidat Abandon", null)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PAR_INSTITUTION_CLOSED"));
    }

    @Autowired
    KafkaListenerEndpointRegistry consommateurs;

    @Autowired
    KafkaConnectionDetails kafka;

    /** Mène une candidature jusqu'à l'adhésion prononcée (étapes 1 et 2). */
    String adherent(String nom, String bic, String code) throws Exception {
        String id = enregistrer(nom, bic);
        deposerEtValider(id, "DEMANDE_ADHESION");
        deposerEtValider(id, "STATUTS");
        mvc
            .perform(
                put("/api/v1/institutions/{id}/conditions-adhesion", id)
                    .with(charge())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"codeParticipant\":\"" + code + "\",\"mode\":\"DIRECT\",\"compteReglement\":\"BEAC-" + code + "\"}")
            )
            .andExpect(status().isOk());
        String t = JsonPath.read(
            mvc
                .perform(post("/api/v1/institutions/{id}/transitions", id).with(charge()).contentType(MediaType.APPLICATION_JSON).content("{\"vers\":\"ADHERENT\",\"motif\":\"Dossier complet\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        mvc.perform(post("/api/v1/institutions/{id}/transitions/{t}/approbation", id, t).with(chef()).header("X-Reauth-Assertion", "a")).andExpect(status().isOk());
        return id;
    }

    @Test
    @org.junit.jupiter.api.Timeout(value = 2, unit = java.util.concurrent.TimeUnit.MINUTES)
    @DisplayName("Étape 3 : l'ouverture d'une liaison (Kafka) fait passer l'adhérent à EN_HOMOLOGATION, une seule fois")
    void ouvertureDeLiaison() throws Exception {
        String id = adherent("Banque Cadrage", "BCADRCMX", "BQ-CAD-001");
        mvc
            .perform(get("/api/v1/interne/institutions/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("INTERNAL_PARTICIPANT_READ"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("ADHERENT"))
            .andExpect(jsonPath("$.codeParticipant").value("BQ-CAD-001"));

        String liaison = UUID.randomUUID().toString();
        String evenement = """
            {"specversion":"1.0","id":"%s","source":"/ms-homologation","type":"homologation.liaison-opened.v1","subject":"%s",\
            "datacontenttype":"application/json","data":{"liaisonId":"%s","institutionId":"%s","typeProjet":"CARTE_INTERBANCAIRE"}}"""
            .formatted(UUID.randomUUID(), liaison, liaison, id);
        var props = new java.util.HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafka.getBootstrapServers()));
        try (var producteur = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            // Livré deux fois : le consommateur idempotent n'applique la transition qu'une fois.
            producteur.send(new ProducerRecord<>("homologation.liaison", liaison, evenement)).get();
            producteur.send(new ProducerRecord<>("homologation.liaison", liaison, evenement)).get();
        }
        consommateurs.getListenerContainer("participant-liaison-ouverte").start();
        try {
            String statut = null;
            for (int essai = 0; essai < 60 && !"EN_HOMOLOGATION".equals(statut); essai++) {
                Thread.sleep(500);
                statut = jdbc.queryForObject("select status from institution where id = ?::uuid", String.class, id);
            }
            assertThat(statut).isEqualTo("EN_HOMOLOGATION");
            Thread.sleep(1500);
        } finally {
            consommateurs.getListenerContainer("participant-liaison-ouverte").stop();
        }
        assertThat(
            jdbc.queryForObject("select count(*) from status_transition where institution_id = ?::uuid and to_status = 'EN_HOMOLOGATION' and automatic_transition", Integer.class, id)
        ).isEqualTo(1);
        mvc
            .perform(get("/api/v1/institutions/{id}", id).with(chef()))
            .andExpect(jsonPath("$.institution.statut").value("EN_HOMOLOGATION"))
            .andExpect(jsonPath("$.transitions[0].vers").value("EN_HOMOLOGATION"))
            .andExpect(jsonPath("$.transitions[0].automatique").value(true))
            .andExpect(jsonPath("$.transitions[0].institutionId").value(id));
        assertThat(jdbc.queryForList("select event_type from outbox_event where aggregate_id = ? order by created_at", String.class, id)).last().isEqualTo("participant.status-changed.v1");
    }
}
