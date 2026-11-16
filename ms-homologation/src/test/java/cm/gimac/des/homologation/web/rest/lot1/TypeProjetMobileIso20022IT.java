package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.config.liquibase.ReferentielMobileIso20022;
import cm.gimac.des.homologation.contrat.SchemaContrat;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import tools.jackson.databind.json.JsonMapper;

/**
 * D-148 sur PostgreSQL réel : type de projet MOBILE_ISO20022 créé par Liquibase, référentiel copié et publié depuis le
 * paiement mobile (ReferentielMobileIso20022), proposé au cadrage, périmètre restreint aux services de préfixe ISO.,
 * et référence d'origine portée par homologation.liaison-opened.v1.
 */
class TypeProjetMobileIso20022IT extends AbstractHomologationIT {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final UUID GESTIONNAIRE = UUID.randomUUID();
    static final String[] OUVREUR = { "HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "HOM_REFERENTIEL_READ" };

    @Autowired
    ParticipantStubAdapter registre;

    UUID souscriptions;
    UUID sansSouscription;

    @BeforeEach
    void preparer() {
        fixtures.referentielPublie(TypeProjet.PAIEMENT_MOBILE);
        souscriptions = UUID.randomUUID();
        sansSouscription = UUID.randomUUID();
        registre.enregistrer(
            new InstitutionView(
                souscriptions,
                "Institution 99003",
                "ADHERENT",
                "ETABLISSEMENT_PAIEMENT",
                List.of(new InstitutionView.ServiceSouscrit("W2W", "2026-09-01"), new InstitutionView.ServiceSouscrit("ISO.W2W", "2026-09-01"), new InstitutionView.ServiceSouscrit("CRT.NET", "2026-09-01"))
            ),
            "99003"
        );
        registre.enregistrer(new InstitutionView(sansSouscription, "Institution 99004", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of()), "99004");
    }

    /** Copie sur une connexion validée explicitement (réserve sans validation automatique), après les ordres donnés. */
    Optional<UUID> copier(String... avant) {
        return jdbc.execute(
            (ConnectionCallback<Optional<UUID>>) c -> {
                try (var st = c.createStatement()) {
                    for (String q : avant) {
                        st.executeUpdate(q);
                    }
                }
                Optional<UUID> r = ReferentielMobileIso20022.executer(c, Instant.parse("2026-11-16T00:00:00Z"));
                if (!c.getAutoCommit()) {
                    c.commit();
                }
                return r;
            }
        );
    }

    @Test
    @DisplayName("Changeset : type actif de modèle MOBILE_ISO20022 ; sans référentiel publié du paiement mobile, aucune copie")
    void typeSansSource() {
        assertThat(jdbc.queryForMap("select libelle, modele, actif from type_projet where code = 'MOBILE_ISO20022'"))
            .containsEntry("libelle", "Paiement mobile ISO 20022")
            .containsEntry("modele", "MOBILE_ISO20022")
            .containsEntry("actif", true);
        assertThat(copier("update referential_version set etat = 'RETIREE' where type_projet = 'PAIEMENT_MOBILE'")).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from referential_version where type_projet = 'MOBILE_ISO20022'", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Référentiel copié et publié en version 1, idempotent ; type proposable ; périmètre ISO ; ouverture avec référence d'origine")
    void typeProposableEtOuverture() throws Exception {
        UUID source = jdbc.queryForObject("select id from referential_version where type_projet = 'PAIEMENT_MOBILE' and etat = 'PUBLIEE'", UUID.class);
        UUID v = copier().orElseThrow();
        assertThat(jdbc.queryForMap("select numero, etat, libelle, version_source_id, pilote_id from referential_version where id = ?", v))
            .containsEntry("numero", 1)
            .containsEntry("etat", "PUBLIEE")
            .containsEntry("libelle", "Référentiel Paiement mobile ISO 20022 (version 1)")
            .containsEntry("version_source_id", null)
            .containsEntry("pilote_id", null);
        String compte = "select count(*) from %s where version_id = ?";
        for (String table : List.of("reference_operation", "referential_etape")) {
            assertThat(jdbc.queryForObject(compte.formatted(table), Integer.class, v)).isPositive().isEqualTo(jdbc.queryForObject(compte.formatted(table), Integer.class, source));
        }
        assertThat(copier()).isEmpty();

        mvc
            .perform(get("/api/v1/types-projet").param("actif", "true").with(utilisateur(GESTIONNAIRE, OUVREUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.code == 'MOBILE_ISO20022')].proposable").value(Matchers.contains(true)))
            .andExpect(jsonPath("$[?(@.code == 'MOBILE_ISO20022')].modele").value(Matchers.contains("MOBILE_ISO20022")));
        mvc
            .perform(get("/api/v1/liaisons/perimetre-proposable").param("institutionId", souscriptions.toString()).param("typeProjet", "MOBILE_ISO20022").with(utilisateur(GESTIONNAIRE, OUVREUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.souscrits").value(true))
            .andExpect(jsonPath("$.services[*].code").value(Matchers.contains("ISO.W2W")))
            .andExpect(jsonPath("$.services[*].interfaceSandbox").value(Matchers.contains("ISO20022")));
        mvc
            .perform(get("/api/v1/liaisons/perimetre-proposable").param("institutionId", sansSouscription.toString()).param("typeProjet", "MOBILE_ISO20022").with(utilisateur(GESTIONNAIRE, OUVREUR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.souscrits").value(false))
            .andExpect(jsonPath("$.services[*].code").value(Matchers.contains("ISO.W2W", "ISO.R2P")));

        String corps = "{\"institutionId\":\"%s\",\"typeProjet\":\"MOBILE_ISO20022\",\"chefDeProjetId\":\"%s\",\"referenceOrigine\":\"SBX-ISO-0001\"}".formatted(
            souscriptions,
            CollaboratorStubAdapter.CONDUCTEUR_1
        );
        String r = mvc
            .perform(post("/api/v1/liaisons").with(utilisateur(GESTIONNAIRE, OUVREUR)).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String liaison = JsonPath.read(r, "$.id");
        String charge = jdbc.queryForObject("select payload::text from outbox_event where aggregate_id = ? and event_type = 'homologation.liaison-opened.v1'", String.class, liaison);
        assertThat(SchemaContrat.charger("sandbox/homologation.liaison-opened.v1.schema.json").ecarts(JSON.readTree(charge))).isEmpty();
        assertThat(JsonPath.<String>read(charge, "$.typeProjet")).isEqualTo("MOBILE_ISO20022");
        assertThat(JsonPath.<String>read(charge, "$.modeleTypeProjet")).isEqualTo("MOBILE_ISO20022");
        assertThat(JsonPath.<String>read(charge, "$.versionId")).isEqualTo(v.toString());
        assertThat(JsonPath.<List<String>>read(charge, "$.perimetre")).containsExactly("TRANSVERSE", "ISO.W2W");
        assertThat(JsonPath.<String>read(charge, "$.referenceOrigine")).isEqualTo("SBX-ISO-0001");
    }
}
