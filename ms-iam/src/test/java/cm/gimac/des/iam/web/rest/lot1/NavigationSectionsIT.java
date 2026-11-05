package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.AbstractIamIT;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sections repliables de la barre du DES (05/11/2026) sur PostgreSQL réel : la mise à jour des groupes déplace une
 * fonctionnalité dont le groupe vaut encore l'ancienne valeur de départ, garde le reste de sa définition, et
 * n'écrase jamais un groupe changé par un administrateur. Exécutée dans une transaction annulée.
 */
class NavigationSectionsIT extends AbstractIamIT {

    @Autowired
    PlatformTransactionManager transactions;

    String groupe(String cle) {
        return jdbc.queryForObject("select definition::jsonb ->> 'groupe' from fonctionnalite where cle = ?", String.class, cle);
    }

    @Test
    void valeursDeDepartDejaRangeesAuDemarrage() {
        assertThat(groupe("processus")).isEqualTo("processus");
        assertThat(groupe("lancerDemande")).isEqualTo("processus");
        assertThat(groupe("mesDemandes")).isEqualTo("processus");
        assertThat(groupe("institutions")).isEqualTo("homologation");
    }

    @Test
    void deplaceLAncienGroupeSansEcraserCeluiDUnAdministrateur() throws Exception {
        String sql = new String(new ClassPathResource("config/liquibase/sql/iam_navigation_sections.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        TransactionTemplate t = new TransactionTemplate(transactions);
        t.executeWithoutResult(s -> {
            // Ancien regroupement, sauf lancerDemande rangée ailleurs par un administrateur.
            jdbc.update("update fonctionnalite set definition = jsonb_set(definition::jsonb, '{groupe}', '\"monActivite\"')::text where cle in ('processus', 'mesDemandes')");
            jdbc.update("update fonctionnalite set definition = jsonb_set(definition::jsonb, '{groupe}', '\"monEspace\"')::text, modifie_le = now() where cle = 'lancerDemande'");
            jdbc.update("update fonctionnalite set definition = jsonb_set(definition::jsonb, '{groupe}', '\"registre\"')::text where cle = 'institutions'");
            String libelle = jdbc.queryForObject("select definition::jsonb ->> 'libelle' from fonctionnalite where cle = 'processus'", String.class);

            for (String instruction : sql.split(";")) {
                if (!instruction.replaceAll("(?m)^--.*$", "").isBlank()) {
                    jdbc.update(instruction);
                }
            }

            assertThat(groupe("processus")).isEqualTo("processus");
            assertThat(groupe("mesDemandes")).isEqualTo("processus");
            assertThat(groupe("institutions")).isEqualTo("homologation");
            assertThat(groupe("lancerDemande")).isEqualTo("monEspace");
            assertThat(jdbc.queryForObject("select definition::jsonb ->> 'libelle' from fonctionnalite where cle = 'processus'", String.class)).isEqualTo(libelle);
            s.setRollbackOnly();
        });
    }
}
