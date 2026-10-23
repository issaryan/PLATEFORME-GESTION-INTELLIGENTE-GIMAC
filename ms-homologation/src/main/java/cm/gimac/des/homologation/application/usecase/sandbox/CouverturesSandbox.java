package cm.gimac.des.homologation.application.usecase.sandbox;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Campagne;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Taux;
import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.repository.CoverageRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Aptitude déduite des résultats de la sandbox (US-EXI-14, HOM-12, D-122) : pour une campagne TERMINEE, le taux de
 * réussite restitué par service devient la mesure de certification du service (table coverage, nature CERTIFICATION),
 * sans recalcul. Campagne de référence d'un service : la campagne terminée dont le taux est le plus récent (horodatage
 * restitué) ; une mesure plus ancienne ne remplace jamais une plus récente. Le parcours ne certifie alors un service
 * mesuré qu'à 100 % (Parcours, règle des mesures reprises).
 */
@Component
public class CouverturesSandbox {

    static final String TERMINEE = "TERMINEE";
    static final String REUSSITE = "REUSSITE";

    private final CoverageRepository couvertures;

    public CouverturesSandbox(CoverageRepository couvertures) {
        this.couvertures = couvertures;
    }

    /** À appeler dans la transaction de la relève ; rend le nombre de services mesurés. */
    public int appliquer(Liaison liaison, Campagne c) {
        if (!TERMINEE.equals(c.etat())) {
            return 0;
        }
        int n = 0;
        for (Taux t : c.taux()) {
            if (t.service() == null || !REUSSITE.equals(t.nature())) {
                continue;
            }
            Coverage cov = couvertures
                .findFirstByLiaison_IdAndNatureAndServiceCode(liaison.getId(), NatureCouverture.CERTIFICATION, t.service())
                .orElseGet(() -> new Coverage().serviceCode(t.service()).nature(NatureCouverture.CERTIFICATION).liaison(liaison));
            if (cov.getDateCalcul() != null && !t.horodatage().isAfter(cov.getDateCalcul())) {
                continue;
            }
            couvertures.save(cov.numerateur(t.numerateur()).denominateur(t.denominateur()).taux(new BigDecimal(t.valeur())).calculable(true).motifNonCalculable(null).dateCalcul(t.horodatage()));
            n++;
        }
        return n;
    }
}
