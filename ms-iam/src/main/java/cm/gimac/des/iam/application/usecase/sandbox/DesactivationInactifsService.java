package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.sandbox.ActiviteComptes;
import cm.gimac.des.iam.application.port.out.sandbox.ProtectionsAdministrateur;
import cm.gimac.des.iam.application.port.out.sandbox.VerrouTaches;
import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.CatalogueProfils;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.sandbox.ReglesPerimetre;
import cm.gimac.des.iam.domain.service.ReglesAdministrationDeleguee;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Désactivation des comptes inactifs de la sandbox (D-119, US-EXI-06 scénario 3 et US-EXI-23, IAM-15) : un compte
 * d'institution portant un rôle de la sandbox, actif, sans connexion depuis le nombre de jours paramétré
 * (gimac.sandbox.inactivite-jours) est désactivé par l'administration des comptes de MS-IAM (journal et événement).
 * Le compte protégé (dernier administrateur actif) est écarté, et l'écart journalisé.
 */
@Service
public class DesactivationInactifsService {

    /** Bilan d'un passage. */
    public record Bilan(int desactives, int ecartes, boolean execute) {}

    public static final String TACHE = "desactivation-comptes-inactifs-sandbox";
    public static final Acteur SYSTEME = new Acteur("systeme-iam", "MS-IAM, désactivation pour inactivité", Set.of());

    private final Annuaire annuaire;
    private final CatalogueProfils catalogue;
    private final ActiviteComptes activite;
    private final ProtectionsAdministrateur protections;
    private final AdministrationComptesService comptes;
    private final JournalSandbox journal;
    private final VerrouTaches verrou;
    private final ProprietesSandbox proprietes;
    private final Clock clock;

    public DesactivationInactifsService(
        Annuaire annuaire,
        CatalogueProfils catalogue,
        ActiviteComptes activite,
        ProtectionsAdministrateur protections,
        AdministrationComptesService comptes,
        JournalSandbox journal,
        VerrouTaches verrou,
        ProprietesSandbox proprietes,
        Clock clock
    ) {
        this.annuaire = annuaire;
        this.catalogue = catalogue;
        this.activite = activite;
        this.protections = protections;
        this.comptes = comptes;
        this.journal = journal;
        this.verrou = verrou;
        this.proprietes = proprietes;
        this.clock = clock;
    }

    @Transactional
    public Bilan executer() {
        if (!verrou.prendre(TACHE)) {
            return new Bilan(0, 0, false);
        }
        Instant maintenant = clock.instant();
        int jours = proprietes.inactiviteJours();
        Set<String> rolesSandbox = ReglesAdministrationDeleguee.rolesDelegables(catalogue.externes(), catalogue.compositions());
        Map<String, Set<String>> profils = annuaire.profilsParCompte();
        Map<String, Instant> dernieres = activite.dernieres();
        String motif = "Aucune connexion depuis " + jours + " jours";
        int desactives = 0;
        int ecartes = 0;
        for (Compte c : annuaire.actifs()) {
            boolean sandbox = !c.interne() && profils.getOrDefault(c.id(), Set.of()).stream().anyMatch(rolesSandbox::contains);
            if (!sandbox || !ReglesPerimetre.inactif(dernieres.get(c.id()), c.creeLe(), maintenant, jours)) {
                continue;
            }
            if (protections.protege(c.id())) {
                journal.compte(SYSTEME, "DESACTIVATION_INACTIVITE_ECARTEE", c.id(), c.identifiant(), ReglesPerimetre.MOTIF_PROTECTION, List.of(c.institutionId()));
                ecartes++;
            } else {
                comptes.definirActif(SYSTEME, c.id(), false, motif);
                desactives++;
            }
        }
        return new Bilan(desactives, ecartes, true);
    }
}
