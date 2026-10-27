package cm.gimac.des.iam.infrastructure.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.usecase.sandbox.ReexamensAccesService;
import org.junit.jupiter.api.Test;

class ReexamenAccesPlanifieTest {
    @Test
    void delegueAuServiceTrimestriel() {
        ReexamensAccesService service = mock(ReexamensAccesService.class);
        when(service.ouvrirTrimestre()).thenReturn(true);
        new ReexamenAccesPlanifie(service).executer();
        verify(service).ouvrirTrimestre();
    }
}
