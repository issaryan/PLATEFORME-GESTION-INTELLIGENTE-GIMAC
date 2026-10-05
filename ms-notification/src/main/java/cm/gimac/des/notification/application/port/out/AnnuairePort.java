package cm.gimac.des.notification.application.port.out;

import java.util.Optional;

/**
 * Annuaire des comptes internes (MS-IAM, GET /api/v1/interne/collaborateurs/{id}, jeton de service
 * INTERNAL_IAM_READ) : nom affiché d'un compte, mis en cache. Compte inconnu : vide ; annuaire
 * injoignable : NOT_ANNUAIRE_INDISPONIBLE (503).
 */
public interface AnnuairePort {
    Optional<String> nomAffiche(String compteId);
}
