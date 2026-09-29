package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.event.CompteAdministrePayload;
import cm.gimac.des.iam.domain.event.ConfigurationModifieePayload;
import cm.gimac.des.iam.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Chaque acte d'administration laisse une entrée de journal et un événement d'audit (D-62). */
@ExtendWith(MockitoExtension.class)
class JournalActesTest {

    static final Instant T = Instant.parse("2026-09-29T08:00:00Z");
    static final Acteur RSSI = new Acteur("demo-rssi", "RSSI", Set.of());

    @Mock
    JournalAdministrationRepository journal;

    @Mock
    OutboxPublisher outbox;

    @Test
    @DisplayName("Acte sur un compte : journal horodaté et iam.account-administered.v1")
    void compte() {
        new JournalActes(journal, outbox, Clock.fixed(T, ZoneOffset.UTC)).compte(RSSI, "DESACTIVATION", "c1", "agent", "Départ", List.of("actif"));
        ArgumentCaptor<JournalAdministration> e = ArgumentCaptor.forClass(JournalAdministration.class);
        verify(journal).save(e.capture());
        assertThat(e.getValue().getHorodatage()).isEqualTo(T);
        assertThat(e.getValue().getActeurNom()).isEqualTo("RSSI");
        assertThat(e.getValue().getDetails()).isEqualTo("actif");
        verify(outbox).publish(eq("account"), eq("c1"), eq(CompteAdministrePayload.TYPE), eq(new CompteAdministrePayload("DESACTIVATION", "c1", "agent", "demo-rssi", "Départ", List.of("actif"))));
    }

    @Test
    @DisplayName("Changement de configuration : journal CONFIGURATION_* et iam.configuration-changed.v1")
    void configuration() {
        new JournalActes(journal, outbox, Clock.fixed(T, ZoneOffset.UTC)).configuration(RSSI, "POLITIQUE_SECURITE", "Revue", List.of("longueurMinimale", "historique"));
        ArgumentCaptor<JournalAdministration> e = ArgumentCaptor.forClass(JournalAdministration.class);
        verify(journal).save(e.capture());
        assertThat(e.getValue().getAction()).isEqualTo("CONFIGURATION_POLITIQUE_SECURITE");
        assertThat(e.getValue().getCibleId()).isNull();
        assertThat(e.getValue().getDetails()).isEqualTo("longueurMinimale, historique");
        verify(outbox).publish(
            eq("configuration"),
            eq("POLITIQUE_SECURITE"),
            eq(ConfigurationModifieePayload.TYPE),
            eq(new ConfigurationModifieePayload("POLITIQUE_SECURITE", "demo-rssi", "Revue", List.of("longueurMinimale", "historique")))
        );
    }
}
