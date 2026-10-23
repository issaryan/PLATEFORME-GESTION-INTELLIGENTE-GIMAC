package cm.gimac.des.iam.application.port.out.sandbox;

import java.time.Instant;
import java.util.Map;

/**
 * Dernière connexion de chaque compte (D-119, US-EXI-06 et US-EXI-23), table activite_compte : relevée quand la
 * passerelle lit l'état de connexion du compte après son mot de passe (D-65) ; base de la désactivation des comptes
 * inactifs.
 */
public interface ActiviteComptes {
    void connexion(String compteId, Instant le);

    /** Dernière connexion de chaque compte connu, par identifiant. */
    Map<String, Instant> dernieres();
}
