package cm.gimac.des.homologation.config.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tâche Liquibase de la reprise D-109 : contrat CustomTaskChange, échec explicite, versions publiables. */
class RepriseReferentielPreuvesCrV17Test {

    @Test
    @DisplayName("Échec SQL : la reprise s'arrête par une CustomChangeException qui nomme les preuves et signatures")
    void echec() throws Exception {
        Connection c = mock(Connection.class);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("base indisponible"));
        JdbcConnection jdbc = mock(JdbcConnection.class);
        when(jdbc.getUnderlyingConnection()).thenReturn(c);
        Database db = mock(Database.class);
        when(db.getConnection()).thenReturn(jdbc);
        RepriseReferentielPreuvesCrV17 r = new RepriseReferentielPreuvesCrV17();
        assertThatThrownBy(() -> r.execute(db)).isInstanceOf(CustomChangeException.class).hasMessageContaining("preuves et signatures").hasRootCauseMessage("base indisponible");
    }

    @Test
    @DisplayName("Sans paramètre ni ressource ; validation sans erreur ; message de confirmation")
    void contrat() throws Exception {
        RepriseReferentielPreuvesCrV17 r = new RepriseReferentielPreuvesCrV17();
        r.setUp();
        r.setFileOpener(null);
        assertThat(r.validate(mock(Database.class)).hasErrors()).isFalse();
        assertThat(r.getConfirmationMessage()).contains("Preuves").contains("0 type(s)");
    }

    @Test
    @DisplayName("Versions des trois types publiables (complétude et graphe), libellé de la reprise")
    void versions() {
        for (TypeProjet t : new TypeProjet[] { TypeProjet.CARTE_INTERBANCAIRE, TypeProjet.PAIEMENT_MOBILE, TypeProjet.DELEGATAIRE }) {
            var v = RepriseReferentielPreuvesCrV17.version(t, "Essai", Instant.parse("2026-10-15T00:00:00Z"));
            assertThat(v.getOperations()).allSatisfy(o -> assertThat(o.getLivrables()).isNotEmpty());
        }
    }
}
