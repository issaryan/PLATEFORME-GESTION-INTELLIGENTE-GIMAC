package cm.gimac.des.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.notification.domain.boite.NotificationUtilisateur;
import cm.gimac.des.notification.domain.boite.PreferencesNotification;
import cm.gimac.des.notification.domain.boite.Reference;
import cm.gimac.des.notification.domain.boite.TypeNotification;
import cm.gimac.des.notification.domain.exception.NotificationException;
import cm.gimac.des.notification.domain.service.ReglesNotifications.Etat;
import cm.gimac.des.notification.domain.service.ReglesNotifications.FaitsProcessus;
import cm.gimac.des.notification.domain.service.ReglesNotifications.NatureProcessus;
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
    @DisplayName("Demandes et tâches à prendre (D-81) : liens du contrat, titres et sous-titres")
    void demandesEtTachesAPrendre() {
        assertThat(regles.lienDemandeATraiter("d-1")).isEqualTo("/des/administration/demandes?demande=d-1");
        assertThat(regles.lienDemandeDecidee("d-1")).isEqualTo("/des/equipe/demandes?demande=d-1");
        assertThat(regles.lienTachesAPrendre()).isEqualTo("/des/taches?vue=unite");
        assertThat(regles.titreDemandeSoumise("Paul")).isEqualTo("Demande de Paul");
        assertThat(regles.titreDemandeSoumise(null)).isEqualTo("Nouvelle demande à traiter");
        assertThat(regles.titreDemandeDecidee("ACCEPTEE")).isEqualTo("Demande acceptée");
        assertThat(regles.titreDemandeDecidee("REFUSEE")).isEqualTo("Demande refusée");
        assertThat(regles.libelleDemande("CREATION_COMPTE")).isEqualTo("Création de compte");
        assertThat(regles.libelleDemande("AUTRE")).isNull();
        assertThat(regles.titreTacheOfferte(null)).isEqualTo("Tâche à prendre");
        assertThat(regles.sousTitreTacheOfferte("POLE_RESEAU")).isEqualTo("À prendre : POLE_RESEAU");
        assertThat(regles.titreRelance("Sauvegardes")).isEqualTo("Relance : Sauvegardes");
        assertThat(regles.sousTitreRelance("Paul")).isEqualTo("Relancée par Paul");
        assertThat(regles.type("demande", "type")).isEqualTo(TypeNotification.DEMANDE);
    }

    @Test
    @DisplayName("Mentions et réponse (D-82) : membres seulement, jamais l'auteur, mention prioritaire sur la réponse")
    void mentionsEtReponse() {
        List<String> membres = List.of("aminata", "serge", "rachel");
        // Mention et réponse distinctes ; ordre : mentions puis auteur cité.
        assertThat(regles.mentionsEtReponse(membres, List.of("serge"), "aminata", "paul")).containsExactly(
            Map.entry("serge", TypeNotification.MENTION),
            Map.entry("aminata", TypeNotification.REPONSE)
        );
        // Mentionné et auteur cité : une seule notification, MENTION.
        assertThat(regles.mentionsEtReponse(membres, List.of("aminata"), "aminata", "paul")).containsExactly(Map.entry("aminata", TypeNotification.MENTION));
        // L'auteur n'est jamais retenu (réponse à soi, mention de soi) ; non-membre écarté ; doublons et vides ignorés.
        assertThat(regles.mentionsEtReponse(membres, Arrays.asList("paul", "inconnu", "serge", "serge", " ", null), "paul", "paul")).containsExactly(
            Map.entry("serge", TypeNotification.MENTION)
        );
        // Anciens événements : ni réponse ni mention.
        assertThat(regles.mentionsEtReponse(membres, List.of(), null, "paul")).isEmpty();
        assertThat(regles.mentionsEtReponse(membres, null, " ", "paul")).isEmpty();
        assertThat(regles.mentionsEtReponse(List.of("aminata"), null, "rachel", "paul")).isEmpty();
    }

    @Test
    @DisplayName("Mentions et réponses (D-82) : titres, libellés neutres, lien vers le message ; types nommés")
    void titresMentionsEtReponses() {
        assertThat(regles.titreMention("Paul Mbarga Atangana")).isEqualTo("Paul Mbarga Atangana vous a mentionné");
        assertThat(regles.titreMention(null)).isEqualTo("Vous avez été mentionné");
        assertThat(regles.titreReponse("Paul Mbarga Atangana")).isEqualTo("Paul Mbarga Atangana a répondu à votre message");
        assertThat(regles.titreReponse(" ")).isEqualTo("Réponse à votre message");
        assertThat(regles.lienMessage("c-1", "m-1")).isEqualTo("/des/messagerie?conversation=c-1&message=m-1");
        assertThat(regles.type("mention", "type")).isEqualTo(TypeNotification.MENTION);
        assertThat(regles.type("REPONSE", "type")).isEqualTo(TypeNotification.REPONSE);
        assertThat(Reference.message("m-1")).isEqualTo(new Reference("collaboration", "MESSAGE", "m-1"));
        assertThat(Reference.conversation("c-1").estConversation()).isTrue();
        assertThat(Reference.message("m-1").estConversation()).isFalse();
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
        assertThat(p.types()).containsOnly(
            Map.entry(TypeNotification.MESSAGE, true),
            Map.entry(TypeNotification.TACHE, true),
            Map.entry(TypeNotification.RAPPORT, true),
            Map.entry(TypeNotification.DEMANDE, true),
            Map.entry(TypeNotification.MENTION, true),
            Map.entry(TypeNotification.REPONSE, true),
            Map.entry(TypeNotification.PROCESSUS, true)
        );
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

    // ------------------------------------------------------------------ processus d'unité (D-87)

    static FaitsProcessus faits(NatureProcessus nature, String precision, boolean renvoi, boolean aPrendre, String echeance) {
        return new FaitsProcessus(nature, "OUVERTURE_FLUX_RESEAU", "Flux vers la banque X", "ANALYSE", "Analyse de la demande", precision, renvoi, aPrendre, echeance);
    }

    @Test
    @DisplayName("D-87 : titres des notifications PROCESSUS selon la nature, l'attente, l'état et la cause ; repli sur les codes")
    void titresProcessus() {
        assertThat(regles.titreProcessus(faits(NatureProcessus.INSTANCE_LANCEE, null, false, false, null))).isEqualTo("Processus lancé : Flux vers la banque X");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_ACTIVEE, null, false, false, null))).isEqualTo("Étape à réaliser : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_ACTIVEE, null, false, true, null))).isEqualTo("Étape à prendre : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_ACTIVEE, null, true, false, null))).isEqualTo("Étape renvoyée : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_SOUMISE, "SIGNATURE", false, false, null))).isEqualTo("Signature attendue : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_SOUMISE, "VALIDATION", false, false, null))).isEqualTo("Validation attendue : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_SIGNEE, "A_SIGNER", false, false, null))).isEqualTo("Signature attendue : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_SIGNEE, "A_VALIDER", false, false, null))).isEqualTo("Validation attendue : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_SIGNEE, "TERMINEE", false, false, null))).isEqualTo("Étape signée : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_SIGNEE, null, false, false, null))).isEqualTo("Étape signée : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_REAFFECTEE, "REAFFECTATION", false, false, null))).isEqualTo("Étape réaffectée : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.ETAPE_REAFFECTEE, "DEPART", false, false, null))).isEqualTo("Étape à reprendre : Analyse de la demande");
        assertThat(regles.titreProcessus(faits(NatureProcessus.INSTANCE_CLOSE, "CLOSE", false, false, null))).isEqualTo("Processus terminé : Flux vers la banque X");
        assertThat(regles.titreProcessus(faits(NatureProcessus.INSTANCE_CLOSE, "ANNULEE", false, false, null))).isEqualTo("Processus annulé : Flux vers la banque X");
        // Sans libellé d'étape (événement de signature) : le code de l'étape ; sans intitulé : le code du processus ; rien : libellé seul.
        assertThat(regles.titreProcessus(new FaitsProcessus(NatureProcessus.ETAPE_SIGNEE, "P", "I", "RECETTE", null, "TERMINEE", false, false, null))).isEqualTo("Étape signée : RECETTE");
        assertThat(regles.titreProcessus(new FaitsProcessus(NatureProcessus.INSTANCE_LANCEE, "OUVERTURE", " ", null, null, null, false, false, null))).isEqualTo("Processus lancé : OUVERTURE");
        assertThat(regles.titreProcessus(new FaitsProcessus(NatureProcessus.ETAPE_ACTIVEE, null, null, null, null, null, false, false, null))).isEqualTo("Étape à réaliser");
        assertThat(regles.titreProcessus(new FaitsProcessus(NatureProcessus.INSTANCE_CLOSE, null, null, null, null, "ANNULEE", false, false, null))).isEqualTo("Processus annulé");
    }

    @Test
    @DisplayName("D-87 : sous-titres PROCESSUS (instance, échéance, acteur, départ) ; liens rendus tels quels sous /des/processus/, sinon page de l'instance")
    void sousTitresEtLiensProcessus() {
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.INSTANCE_LANCEE, null, false, false, null), "Awa Mballa", null)).isEqualTo("OUVERTURE_FLUX_RESEAU, lancé par Awa Mballa");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.ETAPE_ACTIVEE, null, false, false, "2026-10-09"), null, null)).isEqualTo("Flux vers la banque X, échéance 2026-10-09");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.ETAPE_ACTIVEE, null, false, false, null), null, null)).isEqualTo("Flux vers la banque X");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.ETAPE_SOUMISE, "SIGNATURE", false, false, null), "Bob Ndi", null)).isEqualTo("Flux vers la banque X, soumise par Bob Ndi");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.ETAPE_SIGNEE, "A_VALIDER", false, false, null), "Brice Onana", null)).isEqualTo("Flux vers la banque X, signée par Brice Onana");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.ETAPE_REAFFECTEE, "REAFFECTATION", false, false, null), "Brice Onana", null)).isEqualTo("Flux vers la banque X, réaffectée par Brice Onana");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.ETAPE_REAFFECTEE, "DEPART", false, false, null), null, "Jean Parti")).isEqualTo("Flux vers la banque X, après le départ de Jean Parti");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.ETAPE_REAFFECTEE, "DEPART", false, false, null), null, null)).isEqualTo("Flux vers la banque X, après un départ");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.INSTANCE_CLOSE, "ANNULEE", false, false, null), "Brice Onana", null)).isEqualTo("OUVERTURE_FLUX_RESEAU, annulé par Brice Onana");
        assertThat(regles.sousTitreProcessus(faits(NatureProcessus.INSTANCE_CLOSE, "CLOSE", false, false, null), "Bob Ndi", null)).isEqualTo("OUVERTURE_FLUX_RESEAU");
        // Instance sans intitulé : le code nomme déjà l'instance dans le titre.
        assertThat(regles.sousTitreProcessus(new FaitsProcessus(NatureProcessus.INSTANCE_LANCEE, "OUVERTURE", null, null, null, null, false, false, null), "Awa Mballa", null)).isEqualTo("Lancé par Awa Mballa");
        assertThat(regles.sousTitreProcessus(new FaitsProcessus(NatureProcessus.INSTANCE_CLOSE, "OUVERTURE", null, null, null, "CLOSE", false, false, null), null, null)).isNull();
        assertThat(regles.sousTitreProcessus(new FaitsProcessus(NatureProcessus.ETAPE_SOUMISE, null, null, "E", null, "SIGNATURE", false, false, null), null, null)).isNull();

        assertThat(regles.lienProcessus("/des/processus/instances/i-1?etape=ANALYSE", "i-1")).isEqualTo("/des/processus/instances/i-1?etape=ANALYSE");
        assertThat(regles.lienProcessus(null, "i-1")).isEqualTo("/des/processus/instances/i-1");
        assertThat(regles.lienProcessus("https://ailleurs.example/x", "i-1")).isEqualTo("/des/processus/instances/i-1");
        assertThat(regles.lienProcessus("/des/processus/../administration", "i-1")).isEqualTo("/des/processus/instances/i-1");
        assertThat(regles.lienProcessus("/des/processus/instances/i-1 x", "i-1")).isEqualTo("/des/processus/instances/i-1");
        assertThat(regles.lienProcessus("/des/processus/" + "a".repeat(500), "i-1")).isEqualTo("/des/processus/instances/i-1");
        assertThat(regles.type("processus", "type")).isEqualTo(TypeNotification.PROCESSUS);
    }
}
