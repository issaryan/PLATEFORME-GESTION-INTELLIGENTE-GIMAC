package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.config.liquibase.RepriseReferentielCr;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ConversionAnnexeA;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;

/**
 * Moteur unique (D-77, docs/lot1/modelisateur-processus.md §3) : reprise des données en version 1 publiée,
 * migration d'une liaison vers une nouvelle version avec application du différentiel au parcours.
 */
class MoteurUniqueIT extends ParcoursITSupport {

    static final UUID INGENIEUR = UUID.fromString("00000000-0000-4000-8000-0000000e0011");

    @SuppressWarnings("unchecked")
    Map<cm.gimac.des.homologation.domain.enumeration.TypeProjet, UUID> reprendre() {
        return jdbc.execute(
            (ConnectionCallback<Map<TypeProjet, UUID>>) c -> {
                Map<TypeProjet, UUID> r = RepriseReferentielCr.executer(c, Instant.parse("2026-10-08T00:00:00Z"));
                if (!c.getAutoCommit()) {
                    c.commit();
                }
                return r;
            }
        );
    }

    /** Mise à jour validée (la réserve de connexions n'est pas en validation automatique). */
    void modifier(String... sql) {
        jdbc.execute(
            (ConnectionCallback<Void>) c -> {
                try (var st = c.createStatement()) {
                    for (String q : sql) {
                        st.executeUpdate(q);
                    }
                }
                if (!c.getAutoCommit()) {
                    c.commit();
                }
                return null;
            }
        );
    }

    @Test
    @DisplayName("Reprise : version 1 publiée par type (numéro suivant, l'ancienne retirée), liaisons et parcours rattachés, aucun état changé ; idempotente")
    void reprise() throws Exception {
        UUID ep = adherent("Établissement Reprise", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        livrable(opId(liaison, "3"), conducteur());
        UUID ancienne = jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison);
        // État d'avant D-77 : parcours instancié depuis le CSV, sans version ni modèle recopié.
        modifier(
            "update parcours_operation set version_id = null, libelle = null, geste = null, jalon = null, declencheur = null, particularite = null, livrables = null, delai_jours = null, responsable = null where liaison_id = '" + liaison + "'",
            "update parcours_liaison set version_id = null, version_numero = null, version_cr = 'CR-1.4' where liaison_id = '" + liaison + "'"
        );
        assertThat(jdbc.queryForObject("select count(*) from parcours_operation where liaison_id = ?::uuid and geste is null", Integer.class, liaison)).isEqualTo(43);
        var etats = jdbc.queryForList("select rang, etat, echeance from parcours_operation where liaison_id = ?::uuid order by rang", liaison);

        Map<TypeProjet, UUID> reprises = reprendre();

        assertThat(reprises).containsOnlyKeys(TypeProjet.values());
        UUID v1 = reprises.get(TypeProjet.CARTE_INTERBANCAIRE);
        assertThat(jdbc.queryForObject("select etat from referential_version where id = ?", String.class, ancienne)).isEqualTo("RETIREE");
        assertThat(jdbc.queryForMap("select etat, numero, libelle from referential_version where id = ?", v1)).containsEntry("etat", "PUBLIEE").containsEntry("numero", 2).containsEntry("libelle", ConversionAnnexeA.LIBELLE_VERSION);
        assertThat(jdbc.queryForObject("select numero from referential_version where id = ?", Integer.class, reprises.get(TypeProjet.DELEGATAIRE))).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from reference_operation where version_id = ?", Integer.class, v1)).isEqualTo(43);
        assertThat(jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison)).isEqualTo(v1);
        assertThat(jdbc.queryForMap("select version_id, version_numero from parcours_liaison where liaison_id = ?::uuid", liaison)).containsEntry("version_id", v1).containsEntry("version_numero", 2);
        assertThat(jdbc.queryForObject("select count(*) from parcours_operation where liaison_id = ?::uuid and version_id = ?", Integer.class, liaison, v1)).isEqualTo(43);
        assertThat(jdbc.queryForMap("select geste, libelle, delai_jours, responsable from parcours_operation where liaison_id = ?::uuid and rang = '6'", liaison))
            .containsEntry("geste", "AFFECTER_RESSOURCES")
            .containsEntry("libelle", "Affectation des ressources du projet")
            .containsEntry("delai_jours", 3)
            .containsEntry("responsable", "CHEFS_DEPARTEMENT");
        assertThat(jdbc.queryForObject("select declencheur from parcours_operation where liaison_id = ?::uuid and rang = '19'", String.class, liaison)).isEqualTo("certification.pv-signed.v1");
        assertThat(jdbc.queryForList("select rang, etat, echeance from parcours_operation where liaison_id = ?::uuid order by rang", liaison)).isEqualTo(etats);
        mvc
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/liaisons/" + liaison + "/dossier").with(conducteur()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionReferentiel").value(2));

        assertThat(reprendre()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from referential_version where libelle = ?", Integer.class, ConversionAnnexeA.LIBELLE_VERSION)).isEqualTo(3);
    }

    @Test
    @DisplayName("Migration : différentiel appliqué au parcours (opération ajoutée éligible, retirée supprimée, modifiée replanifiée)")
    void migration() throws Exception {
        UUID ep = adherent("Établissement Migration", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        livrable(opId(liaison, "3"), conducteur());
        UUID v1 = jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison);
        var edition = utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT");
        String v2 = JsonPath.read(
            mvc
                .perform(post("/api/v1/referentiels").with(edition).contentType(MediaType.APPLICATION_JSON).content("{\"typeProjet\":\"CARTE_INTERBANCAIRE\",\"libelle\":\"Carte v2\",\"versionSourceId\":\"" + v1 + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id"
        );
        mvc.perform(delete("/api/v1/referentiels/" + v2 + "/operations/5").with(edition)).andExpect(status().isNoContent());
        mvc
            .perform(
                post("/api/v1/referentiels/" + v2 + "/operations")
                    .with(edition)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"code\":\"X1\",\"libelle\":\"Revue de sécurité préalable\",\"etape\":\"LANCEMENT\",\"segment\":\"AMONT\",\"ordre\":6,\"responsable\":\"RSSI\",\"validateur\":\"CONDUCTEUR\"," +
                        "\"predecesseurs\":[\"3\"],\"delaiJours\":4,\"geste\":\"DEPOSER_PREUVE\"}"
                    )
            )
            .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/referentiels/" + v2 + "/arcs").with(edition).contentType(MediaType.APPLICATION_JSON).content("{\"de\":\"X1\",\"vers\":\"7\",\"present\":true}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/referentiels/" + v2 + "/publication").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_PUBLISH"))).andExpect(status().isOk());
        mvc
            .perform(post("/api/v1/referentiels/" + v2 + "/migrations/simulation").with(utilisateur(CHEF_DES, "HOM_REFERENTIEL_MIGRATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ajoutees").value(1))
            .andExpect(jsonPath("$[0].retirees").value(1))
            .andExpect(jsonPath("$[0].modifiees").value(1));
        mvc
            .perform(
                post("/api/v1/referentiels/" + v2 + "/migrations")
                    .with(utilisateur(CHEF_DES, "HOM_REFERENTIEL_MIGRATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"liaisonsRetenues\":[\"" + liaison + "\"]}")
            )
            .andExpect(status().isAccepted());

        assertThat(operations(liaison).stream().map(o -> o.get("rang"))).doesNotContain("5").contains("X1");
        assertThat(etat(liaison, "3")).isEqualTo("FAITE");
        assertThat(etat(liaison, "X1")).isEqualTo("A_FAIRE");
        assertThat(operation(liaison, "X1").get("libelle")).isEqualTo("Revue de sécurité préalable");
        assertThat(operation(liaison, "7").get("etat")).isEqualTo("VERROUILLEE");
        assertThat(jdbc.queryForObject("select version_id from parcours_liaison where liaison_id = ?::uuid", UUID.class, liaison)).hasToString(v2);
        assertThat(jdbc.queryForObject("select count(*) from parcours_operation where liaison_id = ?::uuid and version_id <> ?::uuid", Integer.class, liaison, v2)).isZero();
    }
}
