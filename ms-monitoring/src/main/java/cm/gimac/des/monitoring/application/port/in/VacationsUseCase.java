package cm.gimac.des.monitoring.application.port.in;

import cm.gimac.des.monitoring.application.port.in.Vues.EntreeVue;
import cm.gimac.des.monitoring.application.port.in.Vues.VacationEnCours;
import cm.gimac.des.monitoring.application.port.in.Vues.VacationVue;
import cm.gimac.des.monitoring.application.port.in.Vues.VerificationVue;
import java.util.UUID;

/** Vacations et main courante chaînée (MON-13, MON-14). */
public interface VacationsUseCase {
    VacationEnCours enCours(Appelant appelant);

    VacationVue vacation(Appelant appelant, UUID id);

    Page<EntreeVue> mainCourante(Appelant appelant, UUID vacation, Integer page, Integer taille);

    EntreeVue consigner(Appelant appelant, UUID vacation, String nature, String texte, String reference, UUID rectifie);

    VerificationVue verifier(Appelant appelant, UUID vacation);
}
