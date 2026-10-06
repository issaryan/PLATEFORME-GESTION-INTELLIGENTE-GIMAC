package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.PresentationsProfils;
import cm.gimac.des.iam.domain.compte.PresentationProfil;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Réapplication, au démarrage, des libellés et descriptions fixés par l'administration (D-86,
 * docs/lot1/profils-organisation.md §2).
 *
 * <p>Sur le serveur, une nouvelle version du domaine est réimportée en remplaçant les rôles par ceux du
 * dépôt (infra/ci/fusionner-domaine.py) : le jeu initial du dépôt (libellés, missions) s'applique, mais une
 * modification faite depuis la console serait écrasée. MS-IAM garde chaque modification (table
 * profil_presentation) et la réapplique ici quand le fournisseur d'identité en diffère. Idempotent : rien
 * n'est écrit quand tout concorde ; un profil disparu est ignoré ; un échec n'empêche pas le démarrage.
 */
@Component
public class RestaurationPresentationsProfils {

    private static final Logger LOG = LoggerFactory.getLogger(RestaurationPresentationsProfils.class);

    private final Annuaire annuaire;
    private final PresentationsProfils presentations;

    public RestaurationPresentationsProfils(Annuaire annuaire, PresentationsProfils presentations) {
        this.annuaire = annuaire;
        this.presentations = presentations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void auDemarrage() {
        try {
            int n = restaurer();
            if (n > 0) {
                LOG.info("Profils : {} libellé(s) et description(s) fixés par l'administration réappliqués au fournisseur d'identité", n);
            }
        } catch (RuntimeException e) {
            LOG.warn("Profils : réapplication des libellés fixés par l'administration impossible au démarrage ({})", e.getMessage());
        }
    }

    /** Réapplique les présentations fixées qui diffèrent du fournisseur d'identité ; renvoie leur nombre. */
    public int restaurer() {
        Map<String, PresentationProfil> fixees = presentations.toutes();
        if (fixees.values().stream().noneMatch(PresentationProfil::fixeeParAdministration)) {
            return 0;
        }
        Map<String, PresentationProfil> actuelles = annuaire.presentationsProfils();
        int n = 0;
        for (PresentationProfil f : fixees.values()) {
            PresentationProfil a = actuelles.get(f.code());
            if (!f.fixeeParAdministration() || a == null) {
                continue;
            }
            String description = f.description() == null ? "" : f.description();
            String actuelle = a.description() == null ? "" : a.description();
            if (!Objects.equals(f.libelle(), a.libelle()) || !description.equals(actuelle)) {
                annuaire.definirPresentationProfil(f.code(), f.libelle(), description);
                n++;
            }
        }
        return n;
    }
}
