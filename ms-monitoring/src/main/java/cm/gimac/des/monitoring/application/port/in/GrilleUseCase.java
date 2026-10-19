package cm.gimac.des.monitoring.application.port.in;

import cm.gimac.des.monitoring.application.port.in.Vues.EchangeVue;
import cm.gimac.des.monitoring.application.port.in.Vues.GrilleResume;
import cm.gimac.des.monitoring.application.port.in.Vues.GrilleVue;
import java.util.List;
import java.util.UUID;

/** Grille d'astreinte (algorithme 6.45, D-15, MON-09, MON-10, MON-12). */
public interface GrilleUseCase {
    GrilleVue generer(Appelant appelant, String semaine);

    List<GrilleResume> grilles(Appelant appelant, String semaine);

    GrilleVue grille(Appelant appelant, UUID id);

    GrilleVue publiee(Appelant appelant, String semaine);

    GrilleVue publier(Appelant appelant, UUID id, Boolean accepterNonCouvertes, String motif, String assertion);

    List<EchangeVue> echanges(Appelant appelant, UUID grille);

    EchangeVue proposer(Appelant appelant, UUID grille, UUID affectation, UUID affectationCible, String motif);

    EchangeVue repondre(Appelant appelant, UUID echange, boolean accord);

    EchangeVue decider(Appelant appelant, UUID echange, boolean approbation);
}
