package cm.gimac.des.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import cm.gimac.des.notification.domain.boite.Reference;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.exception.NotificationException;
import cm.gimac.des.notification.domain.service.ReglesNotifications.Etat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReglesNotificationsTest {

    static final Instant T = Instant.parse("2026-10-05T08:00:00Z");
    final ReglesNotifications regles = new ReglesNotifications();

    @Test
    @DisplayName("Destinataires : sans doublon ni vide, jamais l'auteur")
    void destinataires() {
        assertThat(regles.destinataires(Arrays.asList("a", "b", "a", " ", null, "auteur", " c "), "auteur")).containsExactly("a", "b", "c");
        assertThat(regles.destinataires(null, "auteur")).isEmpty();
        assertThat(regles.destinataires(List.of("auteur"), "auteur")).isEmpty();
    }

    @Test
    @DisplayName("État, page et taille : défauts et refus en 400 NOT_REQUETE_INVALIDE")
    void pagination() {
        assertThat(regles.etat(null)).isEqualTo(Etat.TOUTES);
        assertThat(regles.etat("non-lues")).isEqualTo(Etat.NON_LUES);
        assertThat(regles.etat("TOUTES")).isEqualTo(Etat.TOUTES);
        assertThatThrownBy(() -> regles.etat("lues")).isInstanceOf(NotificationException.class).extracting("code").isEqualTo("NOT_REQUETE_INVALIDE");
        assertThat(regles.page(null)).isZero();
        assertThatThrownBy(() -> regles.page(-1)).isInstanceOf(NotificationException.class);
        assertThat(regles.taille(null)).isEqualTo(20);
        assertThat(regles.taille(100)).isEqualTo(100);
        assertThatThrownBy(() -> regles.taille(0)).isInstanceOf(NotificationException.class);
        assertThatThrownBy(() -> regles.taille(101)).isInstanceOf(NotificationException.class);
    }

    @Test
    @DisplayName("Titres, sous-titres et liens du contrat §2 ; libellés neutres sans nom")
    void titresEtLiens() {
        assertThat(regles.titreMessage("Carine Ebongue Ndjock")).isEqualTo("Carine Ebongue Ndjock");
        assertThat(regles.titreMessage(null)).isEqualTo("Nouveau message");
        assertThat(regles.titreTache(null, "Paul")).isEqualTo("Tâche confiée par Paul");
        assertThat(regles.titreTache("Préparer le dossier", "Paul")).isEqualTo("Préparer le dossier");
        assertThat(regles.titreTache(null, null)).isEqualTo("Nouvelle tâche confiée");
        assertThat(regles.sousTitreTache("2026-10-09")).isEqualTo("Échéance : 2026-10-09");
        assertThat(regles.sousTitreTache(null)).isNull();
        assertThat(regles.titreRapport("Serge")).isEqualTo("Rapport de Serge");
        assertThat(regles.sousTitreRapport("QUOTIDIEN", "2026-10-05")).isEqualTo("Rapport quotidien 2026-10-05");
        assertThat(regles.sousTitreRapport("HEBDOMADAIRE", "2026-W40")).isEqualTo("Rapport hebdomadaire 2026-W40");
        assertThat(regles.lienConversation("c-1")).isEqualTo("/des/messagerie?conversation=c-1");
        assertThat(regles.lienTache("t-1")).isEqualTo("/des/taches?tache=t-1");
        assertThat(regles.lienRapport("r-1")).isEqualTo("/des/rapports?vue=recus&rapport=r-1");
    }

    @Test
    @DisplayName("Types : noms admis, clé inconnue refusée ; conservation de 90 jours")
    void typesEtConservation() {
        assertThat(regles.types(Map.of("message", false))).containsEntry(TypeNotification.MESSAGE, false);
        assertThatThrownBy(() -> regles.types(Map.of("COURRIEL", true))).isInstanceOf(NotificationException.class).extracting("details").asString().contains("types");
        assertThat(regles.limiteConservation(T)).isEqualTo(Instant.parse("2026-07-07T08:00:00Z"));
    }

    @Test
    @DisplayName("Préférences : défauts systeme=true, son=false, tous les types ; modification partielle")
    void preferences() {
        PreferencesNotification p = PreferencesNotification.defauts("u1", T);
        assertThat(p.isSysteme()).isTrue();
        assertThat(p.isSon()).isFalse();
        assertThat(p.types()).containsOnly(Map.entry(TypeNotification.MESSAGE, true), Map.entry(TypeNotification.TACHE, true), Map.entry(TypeNotification.RAPPORT, true));
        p.modifier(null, true, Map.of(TypeNotification.TACHE, false), T.plusSeconds(1));
        assertThat(p.isSysteme()).isTrue();
        assertThat(p.isSon()).isTrue();
        assertThat(p.accepte(TypeNotification.TACHE)).isFalse();
        assertThat(p.accepte(TypeNotification.MESSAGE)).isTrue();
    }

    @Test
    @DisplayName("Regroupement : nombre incrémenté, remontée en tête, titre du dernier expéditeur ; lecture unique")
    void regroupement() {
        NotificationUtilisateur n = new NotificationUtilisateur("u1", TypeNotification.MESSAGE, "Paul", "Groupe", "/des/messagerie?conversation=c", Reference.conversation("c"), "p", "Paul", T);
        n.regrouper("Serge", "Groupe", "s", "Serge", T.plusSeconds(5));
        n.regrouper("Paul", "Groupe", "p", "Paul", T.plusSeconds(3));
        assertThat(n.getNombre()).isEqualTo(3);
        assertThat(n.getTitre()).isEqualTo("Paul");
        assertThat(n.getMisAJourLe()).isEqualTo(T.plusSeconds(5));
        assertThat(n.getCreeLe()).isEqualTo(T);
        assertThat(n.marquerLue(T.plusSeconds(10))).isTrue();
        assertThat(n.marquerLue(T.plusSeconds(20))).isFalse();
        assertThat(n.getLueLe()).isEqualTo(T.plusSeconds(10));
        assertThat(new NotificationUtilisateur("u", TypeNotification.TACHE, "x".repeat(300), null, "/", Reference.tache("t"), null, null, T).getTitre()).hasSize(200);
    }
}
