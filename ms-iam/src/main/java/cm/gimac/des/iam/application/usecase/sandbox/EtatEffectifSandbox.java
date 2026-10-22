package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.sandbox.ActiviteComptes;
import cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.EtatConnexion;
import cm.gimac.des.iam.domain.sandbox.ReglesPerimetre;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * État de connexion effectif d'un compte (D-119, US-EXI-06) lu par la passerelle après le mot de passe (D-65) : un
 * compte d'un périmètre suspendu est vu inactif, sans que son propre état change, et la connexion est refusée. Une
 * connexion admise est relevée comme dernière connexion du compte (désactivation des comptes inactifs).
 */
@Component
public class EtatEffectifSandbox {

    private final PerimetresSuspendus perimetres;
    private final ActiviteComptes activite;
    private final Clock clock;

    public EtatEffectifSandbox(PerimetresSuspendus perimetres, ActiviteComptes activite, Clock clock) {
        this.perimetres = perimetres;
        this.activite = activite;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public EtatConnexion etat(Compte c) {
        boolean actif = ReglesPerimetre.actifEffectif(c.actif(), !c.interne() && perimetres.suspendu(c.institutionId()));
        if (actif) {
            activite.connexion(c.id(), clock.instant());
        }
        return new EtatConnexion(actif, c.secondFacteur(), c.etapesRestantes());
    }
}
