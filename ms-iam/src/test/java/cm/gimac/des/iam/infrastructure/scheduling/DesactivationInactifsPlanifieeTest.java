package cm.gimac.des.iam.infrastructure.scheduling;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.usecase.sandbox.DesactivationInactifsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Passage planifié de la désactivation des comptes inactifs de la sandbox (D-119, US-EXI-23). */
@ExtendWith(MockitoExtension.class)
class DesactivationInactifsPlanifieeTest {

    @Mock
    DesactivationInactifsService service;

    @Test
    @DisplayName("Le passage planifié exécute la désactivation et journalise son bilan")
    void passage() {
        when(service.executer()).thenReturn(new DesactivationInactifsService.Bilan(2, 1, true));
        new DesactivationInactifsPlanifiee(service).executer();
        verify(service).executer();
    }
}
