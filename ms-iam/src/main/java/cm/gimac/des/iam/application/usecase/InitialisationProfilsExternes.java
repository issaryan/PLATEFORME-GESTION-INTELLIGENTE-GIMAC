package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.PresentationsProfils;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Attribut externe des profils (gimac_externe, D-94, docs/lot1/ecrans-par-configuration.md) au démarrage, en deux
 * temps idempotents :
 *
 * <ol>
 *   <li>réapplication des valeurs fixées depuis la console (table profil_presentation, colonne externe) quand le
 *       fournisseur d'identité en diffère, par exemple après un réimport du domaine ;</li>
 *   <li>valeur de départ : chaque profil de gimac.iam.profils.externes-initiaux qui n'a pas encore l'attribut le
 *       reçoit à vrai (ROLE_CORRESPONDANT dans la configuration livrée). Un attribut déjà posé, à vrai ou à faux,
 *       n'est jamais modifié.</li>
 * </ol>
 *
 * <p>Rien n'est écrit quand tout concorde ; un profil absent du fournisseur est ignoré ; un échec n'empêche pas le
 * démarrage (la règle de repli, permission *_PORTAL_*, s'applique alors).
 */
@Component
public class InitialisationProfilsExternes {

    private static final Logger LOG = LoggerFactory.getLogger(InitialisationProfilsExternes.class);

    private final Annuaire annuaire;
    private final PresentationsProfils presentations;
    private final IamProprietes proprietes;

    public InitialisationProfilsExternes(Annuaire annuaire, PresentationsProfils presentations, IamProprietes proprietes) {
        this.annuaire = annuaire;
        this.presentations = presentations;
        this.proprietes = proprietes;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void auDemarrage() {
        try {
            int n = initialiser();
            if (n > 0) {
                LOG.info("Profils : attribut externe posé ou réappliqué sur {} profil(s)", n);
            }
        } catch (RuntimeException e) {
            LOG.warn("Profils : attribut externe non initialisé au démarrage ({})", e.getMessage());
        }
    }

    /** Réapplique les valeurs fixées, puis pose les valeurs de départ absentes ; renvoie le nombre d'écritures. */
    public int initialiser() {
        Map<String, Boolean> poses = annuaire.attributsExternes();
        Set<String> profils = annuaire.presentationsProfils().keySet();
        Map<String, Boolean> fixes = presentations == null ? Map.of() : presentations.externes();
        int n = 0;
        for (Map.Entry<String, Boolean> f : fixes.entrySet()) {
            if (profils.contains(f.getKey()) && !f.getValue().equals(poses.get(f.getKey()))) {
                annuaire.definirExterne(f.getKey(), f.getValue());
                n++;
            }
        }
        for (String code : proprietes.profils().externesInitiaux()) {
            if (profils.contains(code) && !poses.containsKey(code) && !fixes.containsKey(code)) {
                annuaire.definirExterne(code, true);
                n++;
            }
        }
        return n;
    }
}
