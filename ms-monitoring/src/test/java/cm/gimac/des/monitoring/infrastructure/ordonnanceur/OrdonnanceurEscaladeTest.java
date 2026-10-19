package cm.gimac.des.monitoring.infrastructure.ordonnanceur;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.monitoring.application.port.in.ReceptionAlertesUseCase;
import org.junit.jupiter.api.Test;

/** Ordonnanceur de l'escalade (MON-05) : chaque passage appelle le cas d'utilisation ; un échec n'arrête jamais l'ordonnanceur. */
class OrdonnanceurEscaladeTest {

    @Test
    void chaquePassageEscaladeEtSurvitAUnEchec() {
        ReceptionAlertesUseCase alertes = mock(ReceptionAlertesUseCase.class);
        when(alertes.escalader()).thenReturn(2).thenReturn(0).thenThrow(new IllegalStateException("base indisponible"));
        OrdonnanceurEscalade o = new OrdonnanceurEscalade(alertes);
        o.escalader();
        o.escalader();
        assertThatCode(o::escalader).doesNotThrowAnyException();
        verify(alertes, times(3)).escalader();
    }
}
