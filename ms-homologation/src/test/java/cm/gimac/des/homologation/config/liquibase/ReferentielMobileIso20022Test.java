package cm.gimac.des.homologation.config.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tâche Liquibase du référentiel MOBILE_ISO20022 (D-148) : contrat CustomTaskChange, idempotence et échec explicite. */
class ReferentielMobileIso20022Test {

    static Database base(Connection c) throws Exception {
        JdbcConnection jdbc = mock(JdbcConnection.class);
        when(jdbc.getUnderlyingConnection()).thenReturn(c);
        Database db = mock(Database.class);
        when(db.getConnection()).thenReturn(jdbc);
        return db;
    }

    @Test
    @DisplayName("Échec SQL : CustomChangeException qui nomme le type MOBILE_ISO20022")
    void echec() throws Exception {
        Connection c = mock(Connection.class);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("base indisponible"));
        ReferentielMobileIso20022 r = new ReferentielMobileIso20022();
        assertThatThrownBy(() -> r.execute(base(c))).isInstanceOf(CustomChangeException.class).hasMessageContaining("MOBILE_ISO20022").hasRootCauseMessage("base indisponible");
    }

    @Test
    @DisplayName("Type déjà doté d'une version : rien n'est copié ; message de confirmation en conséquence")
    void dejaFait() throws Exception {
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(1);
        ReferentielMobileIso20022 r = new ReferentielMobileIso20022();
        r.execute(base(c));
        assertThat(r.getConfirmationMessage()).contains("non copié");
    }

    @Test
    @DisplayName("Sans paramètre ni ressource ; validation sans erreur")
    void contrat() throws Exception {
        ReferentielMobileIso20022 r = new ReferentielMobileIso20022();
        r.setUp();
        r.setFileOpener(null);
        assertThat(r.validate(mock(Database.class)).hasErrors()).isFalse();
    }
}
