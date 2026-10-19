package cm.gimac.des.monitoring.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Annuaire de MS-IAM (lecture, jeton de service) : porteurs d'une permission (qualifications d'opérateur, chefs du
 * DES) et noms affichés des comptes. MS-IAM injoignable : 503 MON_ANNUAIRE_INDISPONIBLE, sans valeur supposée.
 */
public interface AnnuairePort {
    /** Comptes internes actifs dont un profil porte la permission, triés par nom affiché. */
    List<Personne> porteurs(String permission);

    /** Noms affichés des comptes connus ; un compte inconnu est absent de la réponse. */
    Map<String, String> noms(Collection<String> ids);

    record Personne(String id, String nom) {}
}
