package cm.gimac.des.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase.Demande;
import cm.gimac.des.notification.application.port.in.RecevoirNotificationsSandboxUseCase.Resolution;
import cm.gimac.des.notification.application.port.out.DestinatairesSandboxPort;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.event.NotificationCreeePayload;
import cm.gimac.des.notification.domain.exception.NotificationException;
import cm.gimac.des.notification.domain.service.ReglesSandbox;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/** Notifications de la sandbox (US-EXI-09) : résolution, préférences, rebut d'un modèle sans règle, résolution à blanc. */
@ExtendWith(MockitoExtension.class)
class NotificationsSandboxServiceTest {

    static final Instant T = Instant.parse("2026-10-23T08:00:00Z");

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

    NotificationsSandboxService service;

    @BeforeEach
    void init() {
        service = new NotificationsSandboxService(notifications, preferences, annuaire, outbox, new ReglesSandbox(), jdbc, Clock.fixed(T, ZoneOffset.UTC));
        lenient().when(notifications.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(preferences.findById(anyString())).thenReturn(Optional.empty());
        lenient().when(annuaire.comptesInstitution("99001")).thenReturn(Map.of("adm", Set.of("ROLE_SBX_ADMIN_INSTITUTION"), "chef", Set.of("ROLE_SBX_CHEF_PROJET")));
        lenient().when(annuaire.porteurs("SBX_HAB_ADMINISTRATION")).thenReturn(List.of("g1"));
    }

    @Test
    @DisplayName("Suspension : administrateur et chef de projet sur le portail, administrateur du GIMAC sur le DES, préférence respectée")
    void suspension() {
        PreferencesNotification sansSandbox = PreferencesNotification.defauts("chef", T);
        sansSandbox.modifier(null, null, Map.of(TypeNotification.SANDBOX, false), T);
        when(preferences.findById("chef")).thenReturn(Optional.of(sansSandbox));
        Demande d = new Demande(
            "e1",
            "sbx-espace",
            "SBX_ESP_ESPACE_SUSPENDU",
            List.of("ROLE:ADMINISTRATEUR_INSTITUTION", "ROLE:CHEF_DE_PROJET", "ROLE:ADMINISTRATEUR_GIMAC"),
            "99001",
            "ESP-99001",
            null
        );
        assertThat(service.demander(d)).isEqualTo(2);
        ArgumentCaptor<NotificationUtilisateur> c = ArgumentCaptor.forClass(NotificationUtilisateur.class);
        verify(notifications, times(2)).save(c.capture());
        assertThat(c.getAllValues()).extracting(NotificationUtilisateur::getDestinataireId).containsExactly("adm", "g1");
        assertThat(c.getAllValues()).extracting(NotificationUtilisateur::getLien).containsExactly("/portail", "/des/sandbox/espaces/99001");
        NotificationUtilisateur n = c.getAllValues().get(0);
        assertThat(n.getType()).isEqualTo(TypeNotification.SANDBOX);
        assertThat(n.getTitre()).isEqualTo("Espace sandbox 99001 suspendu");
        assertThat(n.getReference().getService()).isEqualTo("sandbox");
        assertThat(n.getReference().getType()).isEqualTo("SBX_ESP_ESPACE_SUSPENDU");
        assertThat(n.getReference().getId()).isEqualTo("e1");
        assertThat(n.getMisAJourLe()).isEqualTo(T);
        verify(outbox, times(2)).publish(eq(BoiteNotificationsService.AGREGAT), any(UUID.class), eq(NotificationCreeePayload.TYPE), any());
    }

    @Test
    @DisplayName("Portée GIMAC : MS-IAM n'est pas lu pour l'institution ; aucun destinataire : rien n'est créé")
    void porteeGimac() {
        Demande d = new Demande("e2", "sbx-espace", "SBX_ESP_FERMETURE_A_PRONONCER", List.of("ROLE:RSSI", "ROLE:INCONNU"), "99001", null, Instant.parse("2026-10-22T10:00:00Z"));
        assertThat(service.demander(d)).isZero();
        verify(annuaire, never()).comptesInstitution(anyString());
        verify(annuaire).porteurs("SBX_LEC_ADMINISTRATION");
        verify(notifications, never()).save(any());
    }

    @Test
    @DisplayName("Portée institution : les porteurs d'une permission du GIMAC ne sont pas lus")
    void porteeInstitution() {
        Demande d = new Demande("e3", "sbx-espace", "SBX_ESP_MANDAT_ACCORDE", List.of("ROLE:ADMINISTRATEUR_GIMAC", "adm"), "99001", null, null);
        assertThat(service.demander(d)).isEqualTo(1);
        verify(annuaire, never()).porteurs(anyString());
    }

    @Test
    @DisplayName("Modèle sans règle : rebut avec son motif, valeurs bornées, rien n'est notifié")
    void sansRegle() {
        Demande d = new Demande("e4", "sbx-raccordement", "SBX_RAC_INCONNU\n" + "x".repeat(100), List.of("adm"), "99001", null, null);
        assertThat(service.demander(d)).isZero();
        verify(jdbc).update(anyString(), eq("e4"), eq("sbx-raccordement"), eq("SBX_RAC_INCONNU_" + "x".repeat(64)), eq("99001"), eq(NotificationsSandboxService.MODELE_SANS_REGLE), any());
        verify(notifications, never()).save(any());
        assertThat(NotificationsSandboxService.tronquer(null, 3)).isNull();
        assertThat(NotificationsSandboxService.tronquer("ab", 3)).isEqualTo("ab");
    }

    @Test
    @DisplayName("Résolution à blanc : destinataires, titres et liens sans rien créer ; modèle sans règle : 400 ; catalogue")
    void simulation() {
        List<Resolution> r = service.simuler("SBX_ESP_ESPACE_RETABLI", List.of("ROLE:ADMINISTRATEUR_INSTITUTION", "ROLE:ADMINISTRATEUR_GIMAC"), "99001");
        assertThat(r).containsExactly(
            new Resolution("adm", true, "Espace sandbox 99001 rétabli", "/portail"),
            new Resolution("g1", false, "Espace sandbox 99001 rétabli", "/des/sandbox/espaces/99001")
        );
        verify(notifications, never()).save(any());
        verify(outbox, never()).publish(anyString(), any(UUID.class), anyString(), any());
        assertThatThrownBy(() -> service.simuler("INCONNU", List.of(), "99001")).isInstanceOf(NotificationException.class).extracting("status").isEqualTo(400);
        assertThat(service.modeles()).isNotEmpty().allSatisfy(m -> assertThat(m.code()).startsWith("SBX_"));
        assertThat(service.modeles().get(0).portee()).isEqualTo("INSTITUTION");
    }
}
