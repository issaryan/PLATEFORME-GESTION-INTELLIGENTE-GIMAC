package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.ReauthentificationService;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import org.springframework.stereotype.Component;

/**
 * Actes d'administration sensibles (suppression d'un compte, profils, politique de sécurité,
 * composition d'un profil) : réauthentification forte FIDO2 exigée (IAM-07), finalité ADMIN_SENSITIVE.
 */
@Component
class ReauthAdministration {

    static final String FINALITE = "ADMIN_SENSITIVE";

    private final ReauthentificationService reauth;

    ReauthAdministration(ReauthentificationService reauth) {
        this.reauth = reauth;
    }

    void exiger(Acteur acteur, String assertion) {
        if (assertion == null || assertion.isBlank()) {
            throw IamException.reauthRequise();
        }
        if (!reauth.verifier(assertion, acteur.id(), FINALITE)) {
            throw IamException.interdit("IAM_REAUTH_INVALID", "Réauthentification forte invalide");
        }
    }
}
