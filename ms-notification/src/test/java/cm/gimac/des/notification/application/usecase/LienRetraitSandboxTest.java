package cm.gimac.des.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase.Demande;
import cm.gimac.des.notification.application.port.out.DestinatairesSandboxPort;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.service.ReglesSandbox;
import cm.gimac.des.notification.infrastructure.catalogue.CatalogueNotificationsSandbox;
import cm.gimac.des.notification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.notification.repository.NotificationUtilisateurRepository;
import cm.gimac.des.notification.repository.PreferencesNotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Identifiant d'accès de test délivré (constat du 18/11/2026) : le lien de la notification était toujours « /portail »,
 * le lien de retrait de la demande ignoré ; il mène désormais à la page de retrait du portail de la sandbox.
 */
@ExtendWith(MockitoExtension.class)
class LienRetraitSandboxTest {

    static final Instant T = Instant.parse("2026-11-18T08:34:24Z");
    static final String JETON = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde";
    static final String CHEMIN = "/portail/sbx-raccordement/retrait/" + JETON;

    @Mock
    NotificationUtilisateurRepository notifications;

    @Mock
    PreferencesNotificationRepository preferences;

    @Mock
    DestinatairesSandboxPort annuaire;

    @Mock
    OutboxPublisher outbox;

    @Mock
    JdbcTemplate jdbc;

    private NotificationsSandboxService service(String adresse) {
        lenient().when(notifications.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(preferences.findById(anyString())).thenReturn(Optional.empty());
        lenient().when(annuaire.comptesInstitution("14024")).thenReturn(Map.of("titulaire", Set.of("ROLE_SBX_DEVELOPPEUR")));
        return new NotificationsSandboxService(notifications, preferences, annuaire, outbox, CatalogueNotificationsSandbox.regles(), jdbc, Clock.fixed(T, ZoneOffset.UTC), adresse);
    }

    private List<String> liens(NotificationsSandboxService s, String modele, String lienRetrait, int n) {
        s.demander(new Demande("e-" + modele, "sbx-raccordement", modele, List.of("titulaire"), "14024", null, T, lienRetrait));
        ArgumentCaptor<NotificationUtilisateur> c = ArgumentCaptor.forClass(NotificationUtilisateur.class);
        verify(notifications, times(n)).save(c.capture());
        return c.getAllValues().stream().map(NotificationUtilisateur::getLien).toList();
    }

    @Test
    @DisplayName("Adresse du portail de la sandbox configurée : lien absolu vers la page de retrait")
    void lienAbsolu() {
        assertThat(liens(service("https://portail-sandbox.example/"), ReglesSandbox.IDENTIFIANT_DELIVRE, CHEMIN, 1)).containsExactly("https://portail-sandbox.example" + CHEMIN);
    }

    @Test
    @DisplayName("Sans adresse : chemin relatif, ouvert depuis la cloche du portail de la sandbox")
    void lienRelatif() {
        assertThat(liens(service(""), ReglesSandbox.IDENTIFIANT_DELIVRE, CHEMIN, 1)).containsExactly(CHEMIN);
    }

    @Test
    @DisplayName("Lien absent ou hors de la forme attendue, autre modèle : lien par défaut du portail")
    void lienParDefaut() {
        NotificationsSandboxService s = service("https://portail-sandbox.example");
        s.demander(new Demande("e1", "sbx-raccordement", ReglesSandbox.IDENTIFIANT_DELIVRE, List.of("titulaire"), "14024", null, T));
        s.demander(new Demande("e2", "sbx-raccordement", ReglesSandbox.IDENTIFIANT_DELIVRE, List.of("titulaire"), "14024", null, T, "https://ailleurs.example/x"));
        assertThat(liens(s, "SBX_RAC_IDENTIFIANT_REVOQUE", CHEMIN, 3)).containsExactly("/portail", "/portail", "/portail");
        assertThat(CatalogueNotificationsSandbox.regles().lienPortailSandbox(CatalogueNotificationsSandbox.regles().modele(ReglesSandbox.IDENTIFIANT_DELIVRE).orElseThrow(), CHEMIN, null)).contains(CHEMIN);
    }
}
