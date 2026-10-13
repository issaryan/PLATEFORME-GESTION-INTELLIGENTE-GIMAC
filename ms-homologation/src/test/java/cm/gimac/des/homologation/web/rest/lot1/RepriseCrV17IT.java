package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.config.liquibase.RepriseReferentielCrV17;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.ConversionAnnexeA;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;

/**
 * Reprise de l'annexe A du CR-CERT-GIMAC-01 v1.7 (RepriseReferentielCrV17) : version publiée par type, étapes et vue
 * renseignées, liaisons rattachées sans changement d'état d'exécution, responsable des opérations mobiles M6, M11 à
 * M14, M16 et M17 passé du pôle réseau à la Production monétique sur les seules opérations à faire ; idempotente.
 */
class RepriseCrV17IT extends ParcoursITSupport {

    static final String AGENT_PM = "00000000-0000-4000-8000-0000000f0017";

    Map<TypeProjet, UUID> reprendre() {
        return jdbc.execute(
            (ConnectionCallback<Map<TypeProjet, UUID>>) c -> {
                Map<TypeProjet, UUID> r = RepriseReferentielCrV17.executer(c, Instant.parse("2026-10-13T00:00:00Z"), Set.of(TypeProjet.PAIEMENT_MOBILE));
                if (!c.getAutoCommit()) {
                    c.commit();
                }
                return r;
            }
        );
    }

    /** Une version v1.7 reprise par un autre essai (base partagée) est renommée pour rejouer la reprise. */
    static String ecarterRepriseAnterieure() {
        return "update referential_version set libelle = 'Reprise v1.7 antérieure' where type_projet = 'PAIEMENT_MOBILE' and libelle = '" +
            ConversionAnnexeA.LIBELLE_VERSION_V17.replace("'", "''") + "'";
    }

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
    @DisplayName("Reprise v1.7 : version publiée, étapes, vue, M6 et suivantes à la Production monétique si non faites, états inchangés, idempotente")
    void reprise() throws Exception {
        UUID ep = adherent("Établissement Reprise v1.7", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.PAIEMENT_MOBILE);
        livrable(opId(liaison, "3"), conducteur());
        UUID ancienne = jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison);
        // État d'une liaison rattachée à la version 1 (CR v1.4) : branche mobile en partie au pôle réseau, intitulés abrégés,
        // jalon d'approbation sur le rang 25 ; M12 déjà faite ; agent de la Production monétique affecté au rang 6.
        String raciReseau =
            "jsonb_set(raci, '{R}', '{\"id\": \"A_AFFECTER:POLE_RESEAU\", \"nom\": \"Non affecté (POLE_RESEAU)\", \"fonction\": \"POLE_RESEAU\"}'::jsonb)";
        modifier(
            "update parcours_operation set responsable = 'POLE_RESEAU', raci = " + raciReseau + " where liaison_id = '" + liaison + "' and rang in ('M6','M11','M12','M13','M14','M16','M17')",
            "update parcours_operation set libelle = 'Communication des points de terminaison de production du GIMAC' where liaison_id = '" + liaison + "' and rang = 'M1'",
            "update parcours_operation set jalon = null where liaison_id = '" + liaison + "' and rang = '24'",
            "update parcours_operation set jalon = 'APPROUVEE_SECURITE' where liaison_id = '" + liaison + "' and rang = '25'",
            "update parcours_operation set etat = 'FAITE' where liaison_id = '" + liaison + "' and rang = 'M12'",
            "update parcours_operation set vue = null where liaison_id = '" + liaison + "' and rang = 'M6'",
            "update parcours_liaison set ressources = '[{\"id\": \"" + AGENT_PM + "\", \"nom\": \"Agent Production monétique\", \"fonction\": \"PRODUCTION_MONETIQUE\"}]'::jsonb where liaison_id = '" + liaison + "'",
            ecarterRepriseAnterieure()
        );
        var etats = jdbc.queryForList("select rang, etat, echeance, preuve::text, depots::text from parcours_operation where liaison_id = ?::uuid order by rang", liaison);
        int numeroAvant = jdbc.queryForObject("select max(numero) from referential_version where type_projet = 'PAIEMENT_MOBILE'", Integer.class);

        Map<TypeProjet, UUID> reprises = reprendre();

        assertThat(reprises).containsOnlyKeys(TypeProjet.PAIEMENT_MOBILE);
        UUID v = reprises.get(TypeProjet.PAIEMENT_MOBILE);
        assertThat(jdbc.queryForObject("select etat from referential_version where id = ?", String.class, ancienne)).isEqualTo("RETIREE");
        assertThat(jdbc.queryForObject("select count(*) from referential_version where type_projet = 'PAIEMENT_MOBILE' and etat = 'PUBLIEE'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForMap("select etat, numero, libelle from referential_version where id = ?", v))
            .containsEntry("etat", "PUBLIEE")
            .containsEntry("numero", numeroAvant + 1)
            .containsEntry("libelle", ConversionAnnexeA.LIBELLE_VERSION_V17);
        // Modèle complet : 49 opérations, onze étapes (libellés du CR v1.7), vue renseignée partout.
        assertThat(jdbc.queryForObject("select count(*) from reference_operation where version_id = ?", Integer.class, v)).isEqualTo(49);
        assertThat(jdbc.queryForObject("select count(*) from reference_operation where version_id = ? and vue is null", Integer.class, v)).isZero();
        assertThat(jdbc.queryForObject("select vue from reference_operation where version_id = ? and code = '24'", String.class, v)).isEqualTo("DECISION");
        assertThat(jdbc.queryForObject("select count(*) from referential_etape where version_id = ?", Integer.class, v)).isEqualTo(11);
        assertThat(jdbc.queryForObject("select libelle from referential_etape where version_id = ? and code = 'CADRAGE'", String.class, v)).isEqualTo("Cadrage et périmètre");
        assertThat(jdbc.queryForObject("select libelle from referential_etape where version_id = ? and code = 'LANCEMENT'", String.class, v)).isEqualTo("Réunion de lancement");
        assertThat(jdbc.queryForObject("select count(*) from reference_operation where version_id = ? and code like 'M%' and responsable <> 'PRODUCTION_MONETIQUE'", Integer.class, v)).isZero();
        assertThat(jdbc.queryForObject("select code from reference_operation where version_id = ? and jalon = 'APPROUVEE_SECURITE'", String.class, v)).isEqualTo("24");
        // Liaison et parcours rattachés.
        assertThat(jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison)).isEqualTo(v);
        assertThat(jdbc.queryForMap("select version_id, version_numero from parcours_liaison where liaison_id = ?::uuid", liaison))
            .containsEntry("version_id", v)
            .containsEntry("version_numero", numeroAvant + 1);
        assertThat(jdbc.queryForObject("select count(*) from parcours_operation where liaison_id = ?::uuid and version_id <> ?", Integer.class, liaison, v)).isZero();
        // Opérations à faire : modèle v1.7 ; R passé à l'agent affecté à la Production monétique.
        Map<String, Object> m6 = jdbc.queryForMap("select responsable, vue, raci -> 'R' ->> 'id' as r, raci -> 'R' ->> 'fonction' as f from parcours_operation where liaison_id = ?::uuid and rang = 'M6'", liaison);
        assertThat(m6).containsEntry("responsable", "PRODUCTION_MONETIQUE").containsEntry("r", AGENT_PM).containsEntry("f", "PRODUCTION_MONETIQUE");
        assertThat(m6.get("vue")).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from parcours_operation where liaison_id = ?::uuid and rang in ('M6','M11','M13','M14','M16','M17') and raci -> 'R' ->> 'fonction' = 'PRODUCTION_MONETIQUE'", Integer.class, liaison)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select libelle from parcours_operation where liaison_id = ?::uuid and rang = 'M1'", String.class, liaison))
            .isEqualTo("Communication des points de terminaison de production du GIMAC au participant");
        assertThat(jdbc.queryForObject("select jalon from parcours_operation where liaison_id = ?::uuid and rang = '24'", String.class, liaison)).isEqualTo("APPROUVEE_SECURITE");
        assertThat(jdbc.queryForObject("select jalon from parcours_operation where liaison_id = ?::uuid and rang = '25'", String.class, liaison)).isNull();
        // Opération faite : seule la version change.
        assertThat(jdbc.queryForMap("select responsable, raci -> 'R' ->> 'fonction' as f from parcours_operation where liaison_id = ?::uuid and rang = 'M12'", liaison))
            .containsEntry("responsable", "POLE_RESEAU")
            .containsEntry("f", "POLE_RESEAU");
        // Aucun état d'exécution ne change.
        assertThat(jdbc.queryForList("select rang, etat, echeance, preuve::text, depots::text from parcours_operation where liaison_id = ?::uuid order by rang", liaison)).isEqualTo(etats);

        assertThat(reprendre()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from referential_version where type_projet = 'PAIEMENT_MOBILE' and libelle = ?", Integer.class, ConversionAnnexeA.LIBELLE_VERSION_V17)).isEqualTo(1);
    }

    @Test
    @DisplayName("Reprise v1.7 sans affectation : R tenu par la fonction Production monétique à affecter ; liaison close non rattachée")
    void sansAffectation() throws Exception {
        UUID ep = adherent("Établissement Reprise v1.7 bis", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.PAIEMENT_MOBILE);
        String close = ouvrir(adherent("Établissement clos", "ETABLISSEMENT_PAIEMENT", "RETRAIT"), TypeProjet.PAIEMENT_MOBILE);
        modifier(
            "update parcours_operation set responsable = 'POLE_RESEAU', raci = jsonb_set(raci, '{R}', '{\"id\": \"A_AFFECTER:POLE_RESEAU\", \"nom\": \"Non affecté (POLE_RESEAU)\", \"fonction\": \"POLE_RESEAU\"}'::jsonb) where liaison_id = '" + liaison + "' and rang = 'M11'",
            "update parcours_liaison set ressources = null where liaison_id = '" + liaison + "'",
            // M13 réaffectée à la main (R d'une autre fonction que l'ancien responsable) et M14 sans responsable recopié :
            // leur qualité R n'est pas touchée.
            "update parcours_operation set responsable = 'POLE_RESEAU' where liaison_id = '" + liaison + "' and rang = 'M13'",
            "update parcours_operation set responsable = null, raci = jsonb_set(raci, '{R}', '{\"id\": \"A_AFFECTER:POLE_SYSTEME\", \"nom\": \"x\", \"fonction\": \"POLE_SYSTEME\"}'::jsonb) where liaison_id = '" + liaison + "' and rang = 'M14'",
            "update liaison set etat = 'CLOSE' where id = '" + close + "'",
            ecarterRepriseAnterieure()
        );
        UUID versionClose = jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, close);

        UUID v = reprendre().get(TypeProjet.PAIEMENT_MOBILE);

        assertThat(jdbc.queryForObject("select raci -> 'R' ->> 'id' from parcours_operation where liaison_id = ?::uuid and rang = 'M11'", String.class, liaison))
            .isEqualTo("A_AFFECTER:PRODUCTION_MONETIQUE");
        assertThat(jdbc.queryForObject("select raci -> 'R' ->> 'fonction' from parcours_operation where liaison_id = ?::uuid and rang = 'M13'", String.class, liaison))
            .isEqualTo("PRODUCTION_MONETIQUE");
        assertThat(jdbc.queryForObject("select raci -> 'R' ->> 'id' from parcours_operation where liaison_id = ?::uuid and rang = 'M14'", String.class, liaison))
            .isEqualTo("A_AFFECTER:POLE_SYSTEME");
        assertThat(jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison)).isEqualTo(v);
        assertThat(jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, close)).isEqualTo(versionClose);
    }
}
