package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.service.PolitiqueMotDePasse;
import java.util.List;
import java.util.Map;

/** Contrôle d'un nouveau mot de passe au regard de la politique en vigueur, avant de le soumettre au fournisseur d'identité. */
final class MotsDePasse {

    private MotsDePasse() {}

    static void verifier(Annuaire annuaire, Compte compte, String motDePasse) {
        List<String> violations = PolitiqueMotDePasse.violations(annuaire.politique(), motDePasse, compte.identifiant());
        if (!violations.isEmpty()) {
            throw politique(violations);
        }
    }

    static IamException politique(List<String> violations) {
        return IamException.invalide("IAM_PASSWORD_POLICY", "Mot de passe non conforme à la politique de sécurité", Map.of("violations", List.copyOf(violations)));
    }
}
