package cm.gimac.des.participant.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries;
import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries.Fiche;
import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries.PageServices;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Routes internes du registre pour la sandbox (US-EXI-21) : délégation au cas d'utilisation, sans base. */
class RegistreSandboxInterneResourceTest {

    @Test
    @DisplayName("Fiche et nomenclature rendues telles que lues par le cas d'utilisation")
    void delegation() {
        RegistreSandboxQueries queries = mock(RegistreSandboxQueries.class);
        Fiche fiche = new Fiche("99001", "Banque fictive 99001", "ACTIF", 2, List.of());
        PageServices page = new PageServices(List.of(), 0, 0, 50);
        when(queries.fiche("99001")).thenReturn(fiche);
        when(queries.nomenclature(0, 50)).thenReturn(page);
        RegistreSandboxInterneResource r = new RegistreSandboxInterneResource(queries);
        assertThat(r.fiche("99001")).isSameAs(fiche);
        assertThat(r.services(0, 50)).isSameAs(page);
    }
}
