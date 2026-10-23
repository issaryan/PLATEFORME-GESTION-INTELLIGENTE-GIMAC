package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.usecase.JournalActes;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Traces des actes de la sandbox (D-119) : acte sur un compte par le journal existant, acte sur un objet borné. */
@ExtendWith(MockitoExtension.class)
class JournalSandboxTest {

    @Mock
    JournalActes actes;

    @Mock
    JournalAdministrationRepository journal;

    @Test
    @DisplayName("Acte sur un compte : délégué au journal de l'administration ; acte sur un objet : entrée bornée (motif 1000, détails 2000)")
    void traces() {
        Instant t = Instant.parse("2026-10-22T08:00:00Z");
        JournalSandbox j = new JournalSandbox(actes, journal, new Memoires.Horloge(t));
        Acteur a = new Acteur("sa", "service-account-sbx-espace", Set.of());
        j.compte(a, "PERIMETRE_SUSPENDU", "c1", "testeur", "m", List.of("99001"));
        verify(actes).compte(a, "PERIMETRE_SUSPENDU", "c1", "testeur", "m", List.of("99001"));

        j.objet(a, "PERIMETRE_SUSPENDU", "99001", "99001", "x".repeat(1200), List.of("d".repeat(2500)));
        j.objet(a, "PERIMETRE_RETABLI", "99001", "99001", null, List.of());
        ArgumentCaptor<JournalAdministration> e = ArgumentCaptor.forClass(JournalAdministration.class);
        verify(journal, org.mockito.Mockito.times(2)).save(e.capture());
        assertThat(e.getAllValues().get(0).getMotif()).hasSize(1000);
        assertThat(e.getAllValues().get(0).getDetails()).hasSize(2000);
        assertThat(e.getAllValues().get(0).getHorodatage()).isEqualTo(t);
        assertThat(e.getAllValues().get(1).getMotif()).isNull();
        assertThat(JournalSandbox.tronquer("abc", 5)).isEqualTo("abc");
    }
}
