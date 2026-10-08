package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.MatricePouvoirs;
import cm.gimac.des.iam.application.port.out.PresentationsProfils;
import cm.gimac.des.iam.application.port.out.ProfilsCrees;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.PresentationProfil;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.ProfilCree;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.domain.service.ReglesProfils;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profils créés par l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §5) : rôle composite du
 * realm avec ses permissions et les permissions communes (messagerie, Mon activité, notifications),
 * inscrit dans la table profil_cree, aussitôt attribuable et présent dans la matrice des pouvoirs (AUCUNE
 * partout) ; suppression d'un profil créé sans titulaire. Les profils livrés ne sont pas supprimables.
 *
 * <p>La table est écrite avant le fournisseur d'identité, dans la transaction : un refus de Keycloak
 * annule l'inscription.
 *
 * <p>Libellé et description de tout profil, livré ou créé (D-86, docs/lot1/profils-organisation.md §2) :
 * attribut gimac_libelle et description du rôle, trace dans la table profil_presentation (date, auteur,
 * valeurs fixées par l'administration, réappliquées après un réimport du domaine).
 */
@Service
@Transactional
public class ProfilsService {

    private final Annuaire annuaire;
    private final ProfilsCrees crees;
    private final CatalogueProfils catalogue;
    private final MatricePouvoirs matrice;
    private final JournalActes journal;
    private final Clock clock;
    private final PresentationsProfils presentations;

    @Autowired
    public ProfilsService(
        Annuaire annuaire,
        ProfilsCrees crees,
        CatalogueProfils catalogue,
        MatricePouvoirs matrice,
        JournalActes journal,
        Clock clock,
        PresentationsProfils presentations
    ) {
        this.annuaire = annuaire;
        this.crees = crees;
        this.catalogue = catalogue;
        this.matrice = matrice;
        this.journal = journal;
        this.clock = clock;
        this.presentations = presentations;
    }

    /** Service sans trace des modifications (tests). */
    public ProfilsService(Annuaire annuaire, ProfilsCrees crees, CatalogueProfils catalogue, MatricePouvoirs matrice, JournalActes journal, Clock clock) {
        this(annuaire, crees, catalogue, matrice, journal, clock, null);
    }

    public Profil creer(Acteur acteur, String code, String description, Set<String> permissions, String motif) {
        return creer(acteur, code, null, description, permissions, motif);
    }

    /**
     * Crée un profil (D-81 §5) avec son libellé (D-86 §2 ; absent : le code en tient lieu) et sa
     * description obligatoire (500 caractères au plus).
     */
    public Profil creer(Acteur acteur, String code, String libelle, String description, Set<String> permissions, String motif) {
        ReglesProfils.verifierCode(code);
        String l = libelle == null || libelle.isBlank() ? null : ReglesProfils.libelle(libelle);
        String d = ReglesProfils.description(description, true);
        if (catalogue.attribuables().contains(code)) {
            throw IamException.conflit("IAM_PROFIL_EXISTANT", "Un profil porte déjà ce code", Map.of("code", code));
        }
        Set<String> composition = ReglesProfils.avecCommunes(permissions == null ? Set.of() : permissions);
        ReglesProfils.verifierComposition(acteur, composition, annuaire.permissions());
        boolean privilegie = ReglesProfils.privilegie(composition);
        Instant maintenant = clock.instant();
        crees.enregistrer(new ProfilCree(code, d, privilegie, acteur.id(), maintenant));
        if (presentations != null) {
            presentations.toucher(code, maintenant, acteur.id(), acteur.nom());
        }
        annuaire.creerProfil(code, l, d, composition);
        List<String> champs = new ArrayList<>(List.of("CREATION", "code=" + code, "privilegie=" + privilegie));
        if (l != null) {
            champs.add("libelle=" + l);
        }
        champs.addAll(composition);
        journal.configuration(acteur, "PROFIL_CREE", vide(motif), champs);
        Profil p = new Profil(code, l, d, composition, privilegie, 0, true);
        return presentations == null ? p : p.modifie(new PresentationProfil(code, l, d, maintenant, acteur.id(), acteur.nom()));
    }

    /** Libellé et description, attribut externe inchangé (D-86 §2). */
    public Profil modifier(Acteur acteur, String code, String libelle, String description, String motif) {
        return modifier(acteur, code, libelle, description, null, motif);
    }

    /**
     * Libellé, description et attribut externe d'un profil livré ou créé (D-86 §2, D-94) ; le code est immuable.
     * Motif obligatoire (400 IAM_REASON_REQUIRED) ; profil inconnu : 404 IAM_PROFILE_NOT_FOUND ; profil privilégié
     * sans le droit d'octroi privilégié : 403 IAM_PRIVILEGED_PROFILE ; libellé (1..80) ou description (0..500)
     * invalide : 400 IAM_PROFIL_INVALIDE. {@code externe} nul : inchangé ; changé sur un profil qui a des
     * titulaires : 409 IAM_PROFIL_EXTERNE_TITULAIRES (leurs comptes perdraient la cohérence entre profil et
     * institution). Journal CONFIGURATION_PROFIL_MODIFIE (avant, après) ; les valeurs fixées sont gardées par MS-IAM
     * pour survivre à un réimport du domaine.
     */
    public Profil modifier(Acteur acteur, String code, String libelle, String description, Boolean externe, String motif) {
        ReglesAdministration.exigerMotif(motif);
        if (!catalogue.attribuables().contains(code)) {
            throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", code);
        }
        if (catalogue.privilegies().contains(code) && !acteur.peut(ReglesAdministration.OCTROI_PRIVILEGIE)) {
            throw IamException.interdit("IAM_PRIVILEGED_PROFILE", "La modification d'un profil privilégié est réservée au super administrateur");
        }
        String l = ReglesProfils.libelle(libelle);
        String d = ReglesProfils.description(description, false);
        PresentationProfil avant = annuaire.presentationsProfils().get(code);
        if (avant == null) {
            throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", code);
        }
        Profil actuel = annuaire.profils().stream().filter(p -> p.code().equals(code)).findFirst().orElseThrow(() -> IamException.introuvable("IAM_PROFILE_NOT_FOUND", code));
        boolean changeExterne = externe != null && externe != actuel.externe();
        if (changeExterne && actuel.titulaires() > 0) {
            throw IamException.conflit(
                "IAM_PROFIL_EXTERNE_TITULAIRES",
                "Retirer d'abord le profil de ses titulaires avant de changer sa qualité externe",
                Map.of("code", code, "titulaires", actuel.titulaires())
            );
        }
        if (catalogue.cree(code)) {
            ProfilCree cree = crees.lire(code).orElseThrow(() -> IamException.introuvable("IAM_PROFILE_NOT_FOUND", code));
            crees.enregistrer(new ProfilCree(code, d, cree.privilegie(), cree.creePar(), cree.creeLe()));
        }
        Instant maintenant = clock.instant();
        if (presentations != null) {
            presentations.fixer(code, l, d, maintenant, acteur.id(), acteur.nom());
            if (changeExterne) {
                presentations.fixerExterne(code, externe, maintenant, acteur.id(), acteur.nom());
            }
        }
        annuaire.definirPresentationProfil(code, l, d);
        if (changeExterne) {
            annuaire.definirExterne(code, externe);
        }
        String resumeAvant = resume(avant.libelle() == null ? code : avant.libelle(), avant.description());
        String resumeApres = resume(l, d);
        if (changeExterne) {
            resumeAvant += " ; externe=" + actuel.externe();
            resumeApres += " ; externe=" + externe;
        }
        journal.profil(acteur, code, motif.strip(), resumeAvant, resumeApres);
        Set<String> privilegies = catalogue.privilegies();
        return annuaire.profils().stream()
            .filter(p -> p.code().equals(code))
            .findFirst()
            .map(p -> p.qualifie(privilegies.contains(code), !catalogue.livres().contains(code)))
            .map(p -> presentations == null ? p : p.modifie(new PresentationProfil(code, l, d, maintenant, acteur.id(), acteur.nom())))
            .orElseThrow(() -> IamException.introuvable("IAM_PROFILE_NOT_FOUND", code));
    }

    private static String resume(String libelle, String description) {
        return "libelle=" + libelle + " ; description=" + (description == null ? "" : description);
    }

    /**
     * Supprime un profil créé : 409 IAM_PROFIL_SYSTEME pour un profil livré, 404 s'il est inconnu, 409
     * IAM_PROFIL_TITULAIRES s'il a encore des titulaires, 403 pour un profil privilégié sans le droit
     * d'octroi privilégié. Il quitte la matrice des pouvoirs.
     */
    public void supprimer(Acteur acteur, String code, String motif) {
        if (catalogue.livres().contains(code)) {
            throw IamException.conflit("IAM_PROFIL_SYSTEME", "Un profil livré avec la plateforme n'est pas supprimable", Map.of("code", code));
        }
        ProfilCree p = crees.lire(code).orElseThrow(() -> IamException.introuvable("IAM_PROFILE_NOT_FOUND", code));
        if (p.privilegie() && !acteur.peut(ReglesAdministration.OCTROI_PRIVILEGIE)) {
            throw IamException.interdit("IAM_PRIVILEGED_PROFILE", "Un profil privilégié est réservé au super administrateur");
        }
        long titulaires = annuaire.profils().stream().filter(x -> x.code().equals(code)).mapToLong(Profil::titulaires).sum();
        if (titulaires > 0) {
            throw IamException.conflit("IAM_PROFIL_TITULAIRES", "Retirer d'abord le profil de ses titulaires", Map.of("code", code, "titulaires", titulaires));
        }
        matrice.supprimer(code);
        crees.supprimer(code);
        if (presentations != null) {
            presentations.supprimer(code);
        }
        annuaire.supprimerProfil(code);
        journal.configuration(acteur, "PROFIL_SUPPRIME", vide(motif), List.of("SUPPRESSION", "code=" + code));
    }

    private static String vide(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
