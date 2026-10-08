package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.domain.event.AvatarModifiePayload;
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

    @Test
    @DisplayName("Photo de profil : journal AVATAR_* et iam.avatar-changed.v1, sans l'image")
    void avatar() {
        JournalActes j = new JournalActes(journal, outbox, Clock.fixed(T, ZoneOffset.UTC));
        j.avatar(RSSI, "demo-rssi", "rssi", "REMPLACEE", "0123456789abcdef");
        j.avatar(RSSI, "demo-rssi", "rssi", "RETIREE", null);
        ArgumentCaptor<JournalAdministration> e = ArgumentCaptor.forClass(JournalAdministration.class);
        verify(journal, org.mockito.Mockito.times(2)).save(e.capture());
        assertThat(e.getAllValues().get(0).getAction()).isEqualTo("AVATAR_REMPLACEE");
        assertThat(e.getAllValues().get(0).getDetails()).isEqualTo("version 0123456789abcdef");
        assertThat(e.getAllValues().get(1).getAction()).isEqualTo("AVATAR_RETIREE");
        assertThat(e.getAllValues().get(1).getDetails()).isEmpty();
        verify(outbox).publish(
            eq("avatar"),
            eq("demo-rssi"),
            eq(AvatarModifiePayload.TYPE),
            eq(new AvatarModifiePayload("demo-rssi", "rssi", "REMPLACEE", "0123456789abcdef", "demo-rssi"))
        );
        assertThat(AvatarModifiePayload.TYPE).isEqualTo("iam.avatar-changed.v1");
        assertThat(new cm.gimac.des.iam.infrastructure.messaging.TopicResolver().topicFor(AvatarModifiePayload.TYPE)).isEqualTo("iam.avatar");
    }

    @Test
    @DisplayName("D-81 : MATRICE_POUVOIRS_MODIFIEE (avant, après) et événement de configuration ; demandes au journal (cible : la demande), sans événement d'audit")
    void matriceEtDemandes() {
        JournalActes j = new JournalActes(journal, outbox, Clock.fixed(T, ZoneOffset.UTC));
        j.matrice(RSSI, "ROLE_CHEF_DES", "Revue", "AUCUNE demandables=", "TACHE_CONFIER=UNITES demandables=ROLE_SNA");
        j.demande(RSSI, "DEMANDE_REFUSEE", "d-1", "PROFILS", "x".repeat(1200), List.of("demandeur=c1"));
        ArgumentCaptor<JournalAdministration> e = ArgumentCaptor.forClass(JournalAdministration.class);
        verify(journal, org.mockito.Mockito.times(2)).save(e.capture());
        JournalAdministration m = e.getAllValues().get(0);
        assertThat(m.getAction()).isEqualTo("MATRICE_POUVOIRS_MODIFIEE");
        assertThat(m.getCibleLibelle()).isEqualTo("ROLE_CHEF_DES");
        assertThat(m.getDetails()).isEqualTo("avant: AUCUNE demandables=, après: TACHE_CONFIER=UNITES demandables=ROLE_SNA");
        JournalAdministration d = e.getAllValues().get(1);
        assertThat(d.getAction()).isEqualTo("DEMANDE_REFUSEE");
        assertThat(d.getCibleId()).isEqualTo("d-1");
        assertThat(d.getMotif()).hasSize(1000);
        verify(outbox).publish(eq("configuration"), eq("MATRICE_POUVOIRS"), eq(ConfigurationModifieePayload.TYPE), eq(new ConfigurationModifieePayload("MATRICE_POUVOIRS", "demo-rssi", "Revue", List.of("ROLE_CHEF_DES"))));
        org.mockito.Mockito.verifyNoMoreInteractions(outbox);
        assertThat(new cm.gimac.des.iam.infrastructure.messaging.TopicResolver().topicFor("iam.request-submitted.v1")).isEqualTo("iam.request");
    }

    @Test
    @DisplayName("D-86 et D-94 : CONFIGURATION_PROFIL_MODIFIE et CONFIGURATION_FONCTIONNALITE_MODIFIEE (cible : le code ou la clé ; avant, après), événement de configuration")
    void profilEtFonctionnalite() {
        JournalActes j = new JournalActes(journal, outbox, Clock.fixed(T, ZoneOffset.UTC));
        j.profil(RSSI, "ROLE_SNA", "Renommage", "libelle=A ; description=", "libelle=B ; description=");
        j.fonctionnalite(RSSI, "liaisons", "x".repeat(1200), "ordre=110", "a".repeat(2100));
        j.fonctionnalite(RSSI, "agenda", null, "visible=true", "visible=false");
        ArgumentCaptor<JournalAdministration> e = ArgumentCaptor.forClass(JournalAdministration.class);
        verify(journal, org.mockito.Mockito.times(3)).save(e.capture());
        assertThat(e.getAllValues().get(0).getAction()).isEqualTo("CONFIGURATION_PROFIL_MODIFIE");
        JournalAdministration f = e.getAllValues().get(1);
        assertThat(f.getAction()).isEqualTo("CONFIGURATION_FONCTIONNALITE_MODIFIEE");
        assertThat(f.getCibleLibelle()).isEqualTo("liaisons");
        assertThat(f.getCibleId()).isNull();
        assertThat(f.getMotif()).hasSize(1000);
        assertThat(f.getDetails()).hasSize(2000).startsWith("avant: ordre=110, après: aaa").endsWith("a…");
        // Valeur avant longue (droits d'une fonctionnalité) : la valeur après reste entière ; deux valeurs longues :
        // chacune raccourcie de moitié, les deux restent lisibles.
        String apres = "libelle=Projets d'homologation ; ordre=5";
        assertThat(JournalActes.avantApres("d".repeat(2500), apres)).hasSize(2000).startsWith("avant: ddd").endsWith("d…, après: " + apres);
        assertThat(JournalActes.avantApres("d".repeat(1500), "e".repeat(1500))).hasSize(2000).contains("d…, après: eee").endsWith("e…");
        assertThat(JournalActes.avantApres("d".repeat(992), "e".repeat(992))).hasSize(2000).doesNotContain("…");
        assertThat(JournalActes.avantApres(null, "x")).isEqualTo("avant: null, après: x");
        assertThat(e.getAllValues().get(2).getMotif()).isNull();
        assertThat(e.getAllValues().get(2).getDetails()).isEqualTo("avant: visible=true, après: visible=false");
        verify(outbox).publish(
            eq("configuration"),
            eq("FONCTIONNALITE_MODIFIEE"),
            eq(ConfigurationModifieePayload.TYPE),
            eq(new ConfigurationModifieePayload("FONCTIONNALITE_MODIFIEE", "demo-rssi", null, List.of("agenda")))
        );
    }
}
