package cm.gimac.des.collaboration.domain.messagerie;

import java.time.Instant;
import java.util.Collection;

/**
 * État de lecture d'un message pour son auteur (D-88, docs/lot1/accuses-lecture.md §1), calculé à partir
 * des positions de lecture des membres, sans requête : destinataires = membres actuels, hors auteur,
 * entrés au plus tard à l'envoi (un membre parti ne compte plus, un membre ajouté après ne compte pas) ;
 * un membre a lu le message quand sa position de lecture est postérieure ou égale à l'envoi.
 *
 * @param lus destinataires qui ont lu le message
 * @param destinataires membres présents à l'envoi, hors auteur, encore membres
 */
public record Lecture(EtatLecture etat, int lus, int destinataires) {
    /** ENVOYE : aucun autre membre ne l'a lu ; LU : tous ; LU_PAR_CERTAINS : groupe, au moins un mais pas tous. */
    public enum EtatLecture {
        ENVOYE,
        LU_PAR_CERTAINS,
        LU,
    }

    /** État d'un message de {@code auteurId} envoyé à {@code envoyeLe}, d'après les membres actuels. */
    public static Lecture de(String auteurId, Instant envoyeLe, Collection<ConversationMembre> membres) {
        int destinataires = 0;
        int lus = 0;
        for (ConversationMembre m : membres) {
            if (destinataire(m, auteurId, envoyeLe)) {
                destinataires++;
                if (aLu(m, envoyeLe)) {
                    lus++;
                }
            }
        }
        EtatLecture etat;
        if (lus == 0) {
            etat = EtatLecture.ENVOYE;
        } else if (lus == destinataires) {
            etat = EtatLecture.LU;
        } else {
            etat = EtatLecture.LU_PAR_CERTAINS;
        }
        return new Lecture(etat, lus, destinataires);
    }

    /** État d'un message pour son auteur ; null pour un événement de groupe (D-68), sans accusé de lecture. */
    public static Lecture de(Message message, Collection<ConversationMembre> membres) {
        return message.getEvenement() != null ? null : de(message.getAuteurId(), message.getEnvoyeLe(), membres);
    }

    /** Membre présent à l'envoi, hors auteur. */
    public static boolean destinataire(ConversationMembre m, String auteurId, Instant envoyeLe) {
        return !m.getMembreId().equals(auteurId) && !m.getAjouteLe().isAfter(envoyeLe);
    }

    /** Position de lecture du membre postérieure ou égale à l'envoi. */
    public static boolean aLu(ConversationMembre m, Instant envoyeLe) {
        return m.getDernierLuLe() != null && !m.getDernierLuLe().isBefore(envoyeLe);
    }
}
