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

import cm.gimac.des.notification.application.port.out.ComptesInstitutionPort;
import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.event.NotificationCreeePayload;
import cm.gimac.des.notification.domain.exception.NotificationException;
import cm.gimac.des.notification.domain.service.ReglesHomologation;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsJalon;
import cm.gimac.des.notification.domain.service.ReglesHomologation.FaitsOperation;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Intervenant;
import cm.gimac.des.notification.domain.service.ReglesHomologation.Nature;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Notifications du parcours d'homologation (D-93 §11) : résolution des institutions, rôle le plus fort, préférences. */
@ExtendWith(MockitoExtension.class)
class NotificationsHomologationServiceTest {

    static final Instant T = Instant.parse("2026-10-07T08:00:00Z");
    static final String LIAISON = "11111111-1111-4111-8111-111111111111";
    static final String OPERATION = "22222222-2222-4222-8222-222222222222";
    static final String BANQUE = "33333333-3333-4333-8333-333333333333";

    @Mock
    NotificationUtilisateurRepository notifications;

    @Mock
    PreferencesNotificationRepository preferences;

    @Mock
    ComptesInstitutionPort institutions;

    @Mock
    OutboxPublisher outbox;

    NotificationsHomologationService service;

    @BeforeEach
    void init() {
        service = new NotificationsHomologationService(
            notifications,
            preferences,
            institutions,
            outbox,
            new ReglesHomologation(),
            Clock.fixed(T, ZoneOffset.UTC),
            Set.of("EN_PRODUCTION")
        );
        lenient().when(notifications.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(preferences.findById(anyString())).thenReturn(Optional.empty());
        lenient().when(institutions.comptes(BANQUE)).thenReturn(List.of("corr-1", "corr-2"));
    }

    Map<String, NotificationUtilisateur> enregistrees(int n) {
        ArgumentCaptor<NotificationUtilisateur> c = ArgumentCaptor.forClass(NotificationUtilisateur.class);
        verify(notifications, times(n)).save(c.capture());
        return c.getAllValues().stream().collect(Collectors.toMap(NotificationUtilisateur::getDestinataireId, Function.identity()));
    }

    static FaitsOperation operation(Nature nature, Intervenant r, List<Intervenant> dest, List<String> institutions) {
        return new FaitsOperation(
            nature,
            LIAISON,
            "LIA-2026-001",
            "Banque A",
            OPERATION,
            "3",
            "Cadrage",
            "DEPOSER_LIVRABLE",
            "2026-10-09",
            "OUVERTURE",
            r,
            new Intervenant("V", "SNA", "valideur", false, null),
            dest,
            institutions,
            null
        );
    }

    @Test
    @DisplayName("Opération externe ouverte : comptes de l'institution R (lien de dépôt du portail), C et I internes, à affecter ignoré ; HOMOLOGATION publiée")
    void operationExterne() {
        FaitsOperation f = operation(
            Nature.OPERATION_ACTIVEE,
            new Intervenant("R", "BANQUE_PARTENAIRE", null, false, BANQUE),
            List.of(new Intervenant("C", "DDRM", "consulte", false, null), new Intervenant("I", "SNA", "informe", false, null), new Intervenant("I", "RSSI", null, true, null)),
            List.of(BANQUE)
        );
        assertThat(service.operationHomologation(f, T)).isEqualTo(4);
        Map<String, NotificationUtilisateur> n = enregistrees(4);
        assertThat(n).containsOnlyKeys("corr-1", "corr-2", "consulte", "informe");
        NotificationUtilisateur corr = n.get("corr-1");
        assertThat(corr.getType()).isEqualTo(TypeNotification.HOMOLOGATION);
        assertThat(corr.getTitre()).isEqualTo("Action attendue : Cadrage");
        assertThat(corr.getSousTitre()).isEqualTo("LIA-2026-001 (Banque A), échéance 2026-10-09");
        assertThat(corr.getLien()).isEqualTo("/portail/liaisons/" + LIAISON + "/livrables/" + OPERATION);
        assertThat(corr.getReference().getService()).isEqualTo("homologation");
        assertThat(corr.getReference().getType()).isEqualTo("OPERATION");
        assertThat(corr.getReference().getId()).isEqualTo(OPERATION);
        assertThat(corr.getNombre()).isEqualTo(1);
        assertThat(n.get("consulte").getTitre()).isEqualTo("Avis demandé : Cadrage");
        assertThat(n.get("consulte").getLien()).isEqualTo("/des/parcours/" + LIAISON + "?onglet=operations&operation=" + OPERATION);
        assertThat(n.get("informe").getTitre()).isEqualTo("Opération ouverte : Cadrage");
        ArgumentCaptor<Object> p = ArgumentCaptor.forClass(Object.class);
        verify(outbox, times(4)).publish(eq("notification"), any(UUID.class), eq(NotificationCreeePayload.TYPE), p.capture());
        assertThat(p.getAllValues()).allSatisfy(x -> assertThat(((NotificationCreeePayload) x).type()).isEqualTo("HOMOLOGATION"));
    }

    @Test
    @DisplayName("Un compte visé deux fois reçoit une notification (rôle le plus fort) ; préférence HOMOLOGATION désactivée respectée")
    void dedoublonnageEtPreferences() {
        PreferencesNotification sans = PreferencesNotification.defauts("informe", T);
        sans.modifier(null, null, Map.of(TypeNotification.HOMOLOGATION, false), T);
        when(preferences.findById("informe")).thenReturn(Optional.of(sans));
        FaitsOperation f = operation(
            Nature.OPERATION_ACTIVEE,
            new Intervenant("R", "DDRM", "responsable", false, null),
            List.of(new Intervenant("I", "DDRM", "responsable", false, null), new Intervenant("I", "SNA", "informe", false, null)),
            null
        );
        assertThat(service.operationHomologation(f, null)).isEqualTo(1);
        NotificationUtilisateur r = enregistrees(1).get("responsable");
        assertThat(r.getTitre()).isEqualTo("Action attendue : Cadrage");
        assertThat(r.getCreeLe()).isEqualTo(T);
    }

    @Test
    @DisplayName("Validation demandée : V action, comptes de l'institution R informés (lien de la liaison)")
    void validation() {
        FaitsOperation f = operation(Nature.VALIDATION_DEMANDEE, new Intervenant("R", "BANQUE_PARTENAIRE", null, false, BANQUE), List.of(), List.of(BANQUE));
        assertThat(service.operationHomologation(f, T)).isEqualTo(3);
        Map<String, NotificationUtilisateur> n = enregistrees(3);
        assertThat(n.get("valideur").getTitre()).isEqualTo("Validation attendue : Cadrage");
        assertThat(n.get("corr-2").getTitre()).isEqualTo("Opération en validation : Cadrage");
        assertThat(n.get("corr-2").getLien()).isEqualTo("/portail/liaisons/" + LIAISON);
    }

    @Test
    @DisplayName("Jalon : conducteur et C/I informés ; institution seulement pour les jalons configurés")
    void jalon() {
        FaitsJalon prod = new FaitsJalon(LIAISON, "LIA-2026-001", BANQUE, "Banque A", "EN_PRODUCTION", "En production", "conducteur", List.of(new Intervenant("C", "DDRM", "consulte", false, null)), null);
        assertThat(service.jalonHomologation(prod, T)).isEqualTo(4);
        Map<String, NotificationUtilisateur> n = enregistrees(4);
        assertThat(n.get("conducteur").getTitre()).isEqualTo("Jalon atteint : En production");
        assertThat(n.get("conducteur").getLien()).isEqualTo("/des/parcours/" + LIAISON);
        assertThat(n.get("corr-1").getLien()).isEqualTo("/portail/liaisons/" + LIAISON);
        assertThat(n.get("corr-1").getReference().getType()).isEqualTo("LIAISON");
        FaitsJalon certifiee = new FaitsJalon(LIAISON, "LIA-2026-001", BANQUE, "Banque A", "CERTIFIEE", "Certifiée", "conducteur", List.of(), null);
        assertThat(service.jalonHomologation(certifiee, T)).isEqualTo(1);
    }

    @Test
    @DisplayName("MS-IAM injoignable : l'erreur remonte (rejeu puis rebut), rien n'est perdu en silence ; événement incomplet : rien")
    void indisponibleEtIncomplet() {
        when(institutions.comptes(BANQUE)).thenThrow(NotificationException.indisponible("NOT_ANNUAIRE_INDISPONIBLE", "x"));
        FaitsOperation f = operation(Nature.OPERATION_ACTIVEE, new Intervenant("R", "BANQUE_PARTENAIRE", null, false, BANQUE), List.of(), List.of(BANQUE));
        assertThatThrownBy(() -> service.operationHomologation(f, T)).isInstanceOf(NotificationException.class);
        verify(notifications, never()).save(any());
        assertThat(service.operationHomologation(null, T)).isZero();
        assertThat(service.jalonHomologation(new FaitsJalon(null, null, null, null, null, null, "c", null, null), T)).isZero();
    }
}
