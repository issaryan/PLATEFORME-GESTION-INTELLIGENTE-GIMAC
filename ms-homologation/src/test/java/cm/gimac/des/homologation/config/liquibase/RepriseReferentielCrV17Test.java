package cm.gimac.des.homologation.config.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tâche Liquibase de la reprise de l'annexe A v1.7 : contrat CustomTaskChange et échec explicite. */
class RepriseReferentielCrV17Test {

    @Test
    @DisplayName("Échec SQL : la reprise s'arrête par une CustomChangeException qui nomme l'annexe A v1.7")
    void echec() throws Exception {
        Connection c = mock(Connection.class);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("base indisponible"));
        JdbcConnection jdbc = mock(JdbcConnection.class);
        when(jdbc.getUnderlyingConnection()).thenReturn(c);
        Database db = mock(Database.class);
        when(db.getConnection()).thenReturn(jdbc);
        RepriseReferentielCrV17 r = new RepriseReferentielCrV17();
        assertThatThrownBy(() -> r.execute(db)).isInstanceOf(CustomChangeException.class).hasMessageContaining("v1.7").hasRootCauseMessage("base indisponible");
    }

    @Test
    @DisplayName("Sans paramètre ni ressource ; validation sans erreur ; message de confirmation")
    void contrat() throws Exception {
        RepriseReferentielCrV17 r = new RepriseReferentielCrV17();
        r.setUp();
        r.setFileOpener(null);
        assertThat(r.validate(mock(Database.class)).hasErrors()).isFalse();
        assertThat(r.getConfirmationMessage()).contains("v1.7").contains("0 type(s)");
    }
}
