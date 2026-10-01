package cm.gimac.des.collaboration.application.port.out;

import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Annuaire des comptes internes actifs (MS-IAM, D-62) : destinataires de la messagerie (D-64).
 * Un annuaire injoignable lève COL_DIRECTORY_UNAVAILABLE (503), jamais un résultat supposé.
 */
public interface CollaboratorDirectoryPort {
    /** Page de l'annuaire filtrée par nom, courriel ou fonction. */
    record PageAnnuaire(List<Collaborateur> contenu, long total, int page, int taille) {
        public PageAnnuaire {
            contenu = List.copyOf(contenu);
        }
    }

    PageAnnuaire rechercher(String recherche, int page, int taille);

    Optional<Collaborateur> trouver(String id);

    /**
     * Versions des photos de profil d'un lot de comptes (docs/lot1/mon-compte.md §1) : membres et
     * auteurs affichés ; les comptes sans photo sont absents. Annuaire injoignable :
     * COL_DIRECTORY_UNAVAILABLE.
     */
    Map<String, String> versionsAvatar(Collection<String> ids);
}
