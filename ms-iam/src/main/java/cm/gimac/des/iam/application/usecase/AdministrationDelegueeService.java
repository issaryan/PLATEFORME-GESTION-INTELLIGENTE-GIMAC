package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.EtapesIdempotentes;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.domain.service.ReglesAdministrationDeleguee;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration déléguée des comptes d'une institution dans la sandbox (D-115, docs/lot1/sandbox-existant-adapte.md §4
 * US-EXI-03) : l'administrateur de l'institution crée les comptes de ses collaborateurs, leur attribue les rôles de
 * la sandbox, les suspend et les rétablit, par les écritures existantes de MS-IAM (aucune seconde administration) et
 * dans les seules bornes de son institution. Une étape rejouée avec la même clé d'idempotence rend son résultat
 * d'origine. Chaque acte est journalisé et publié (iam.account-administered.v1), comme toute administration.
 */
@Service
public class AdministrationDelegueeService {

    /** Page de comptes de l'institution. */
    public record PageComptes(List<Compte> contenu, long total, int page, int taille) {}

    /** Création : le compte, l'invitation envoyée ou non, et si l'étape a été rejouée. */
    public record Creation(Compte compte, boolean invitationEnvoyee, boolean rejouee) {}

    /** Fiche d'un collaborateur à créer ; l'institution est celle de l'administrateur. */
    public record NouveauCollaborateur(String identifiant, String prenom, String nom, String email, String telephone, String fonction, Set<String> roles) {}

    static final String CREATION = "CREATION";
    static final String ROLES = "ROLES";
    static final String ETAT = "ETAT";

    private final AdministrationComptesService comptes;
    private final CatalogueProfils catalogue;
    private final Annuaire annuaire;
    private final JournalActes journal;
    private final EtapesIdempotentes etapes;

    public AdministrationDelegueeService(
        AdministrationComptesService comptes,
        CatalogueProfils catalogue,
        Annuaire annuaire,
        JournalActes journal,
        EtapesIdempotentes etapes
    ) {
        this.comptes = comptes;
        this.catalogue = catalogue;
        this.annuaire = annuaire;
        this.journal = journal;
        this.etapes = etapes;
    }

    private Set<String> delegables() {
        return ReglesAdministrationDeleguee.rolesDelegables(catalogue.externes(), catalogue.compositions());
    }

    /** Rôles de la sandbox que l'administrateur peut attribuer. */
    @Transactional(readOnly = true)
    public Set<String> roles() {
        return delegables();
    }

    /** Comptes de l'institution, actifs et suspendus, profils complets, triés par nom ; page à partir de 0. */
    @Transactional(readOnly = true)
    public PageComptes lister(String institutionActeur, int page, int taille) {
        String inst = ReglesAdministrationDeleguee.institution(institutionActeur);
        List<Compte> tous = tous(inst);
        int p = Math.max(0, page);
        int t = Math.min(Math.max(1, taille), 100);
        List<Compte> contenu = tous.stream().skip((long) p * t).limit(t).toList();
        return new PageComptes(contenu, tous.size(), p, t);
    }

    private List<Compte> tous(String inst) {
        Map<String, Set<String>> parCompte = annuaire.profilsParCompte();
        return annuaire
            .parInstitution(inst)
            .stream()
            .filter(c -> inst.equals(c.institutionId()))
            .map(c -> c.avecProfils(parCompte.getOrDefault(c.id(), c.profils())))
            .sorted(Comparator.comparing((Compte c) -> c.nomAffiche().toLowerCase(Locale.ROOT)).thenComparing(Compte::id))
            .toList();
    }

    /** Compte de l'institution ; hors périmètre ou inexistant : 404 indistinguable. */
    @Transactional(readOnly = true)
    public Compte lire(String institutionActeur, String id) {
        String inst = ReglesAdministrationDeleguee.institution(institutionActeur);
        return ReglesAdministrationDeleguee.dansPerimetre(annuaire.lire(id).map(c -> c.avecProfils(annuaire.profilsParCompte().getOrDefault(c.id(), c.profils()))).orElse(null), inst, id);
    }

    private Optional<String> rejouee(String cle, String operation) {
        return cle == null || cle.isBlank() ? Optional.empty() : etapes.resultat(cle.strip(), operation);
    }

    private void retenir(String cle, String operation, String resultat, Acteur acteur) {
        if (cle != null && !cle.isBlank()) {
            etapes.enregistrer(cle.strip(), operation, resultat, acteur.id());
        }
    }

    /** Crée le compte nominatif d'un collaborateur, rattaché à l'institution, avec ses rôles de la sandbox. */
    @Transactional
    public Creation creer(Acteur acteur, String institutionActeur, NouveauCollaborateur n, String cle) {
        String inst = ReglesAdministrationDeleguee.institution(institutionActeur);
        Optional<String> deja = rejouee(cle, CREATION);
        if (deja.isPresent()) {
            return new Creation(lire(inst, deja.orElseThrow()), false, true);
        }
        ReglesAdministrationDeleguee.verifierRoles(n.roles(), delegables(), true);
        NouveauCompte nouveau = new NouveauCompte(n.identifiant(), n.prenom(), n.nom(), n.email(), n.telephone(), n.fonction(), inst, Set.copyOf(n.roles()));
        AdministrationComptesService.Creation c = comptes.creer(acteur, nouveau, true, false);
        retenir(cle, CREATION, c.compte().id(), acteur);
        return new Creation(c.compte(), c.invitationEnvoyee(), false);
    }

    /**
     * Remplace les rôles de la sandbox d'un compte de l'institution (ses autres profils restent). Sur son propre compte :
     * tentative journalisée puis refusée (403, rien n'est modifié).
     */
    @Transactional(noRollbackFor = IamException.class)
    public Compte definirRoles(Acteur acteur, String institutionActeur, String id, Set<String> roles, String motif, String cle) {
        String inst = ReglesAdministrationDeleguee.institution(institutionActeur);
        refuserSoiMeme(acteur, id, "TENTATIVE_AUTO_ATTRIBUTION");
        if (rejouee(cle, ROLES).isPresent()) {
            return lire(inst, id);
        }
        Compte c = lire(inst, id);
        Set<String> delegables = delegables();
        ReglesAdministrationDeleguee.verifierRoles(roles, delegables, false);
        Set<String> apres = ReglesAdministrationDeleguee.profilsApres(c.profils(), roles, delegables);
        Map<String, Set<String>> compositions = catalogue.compositions();
        boolean perd = c.actif() && ReglesAdministrationDeleguee.administrateur(c.profils(), compositions) && !ReglesAdministrationDeleguee.administrateur(apres, compositions);
        if (perd) {
            ReglesAdministrationDeleguee.verifierDernierAdministrateur(true, administrateursActifs(inst, compositions));
        }
        comptes.definirProfils(acteur, id, apres, motif);
        retenir(cle, ROLES, id, acteur);
        return lire(inst, id);
    }

    /** Suspend ou rétablit un compte de l'institution, sans le supprimer. */
    @Transactional(noRollbackFor = IamException.class)
    public Compte definirActif(Acteur acteur, String institutionActeur, String id, boolean actif, String motif, String cle) {
        String inst = ReglesAdministrationDeleguee.institution(institutionActeur);
        refuserSoiMeme(acteur, id, actif ? "TENTATIVE_AUTO_RETABLISSEMENT" : "TENTATIVE_AUTO_SUSPENSION");
        if (rejouee(cle, ETAT).isPresent()) {
            return lire(inst, id);
        }
        Compte c = lire(inst, id);
        Map<String, Set<String>> compositions = catalogue.compositions();
        if (!actif && c.actif() && ReglesAdministrationDeleguee.administrateur(c.profils(), compositions)) {
            ReglesAdministrationDeleguee.verifierDernierAdministrateur(true, administrateursActifs(inst, compositions));
        }
        comptes.definirActif(acteur, id, actif, motif);
        retenir(cle, ETAT, id, acteur);
        return lire(inst, id);
    }

    private long administrateursActifs(String inst, Map<String, Set<String>> compositions) {
        return tous(inst).stream().filter(x -> x.actif() && ReglesAdministrationDeleguee.administrateur(x.profils(), compositions)).count();
    }

    private void refuserSoiMeme(Acteur acteur, String id, String tentative) {
        try {
            ReglesAdministration.verifierPasSoiMeme(acteur, id);
        } catch (IamException e) {
            journal.compte(acteur, tentative, id, acteur.nom(), null, List.of(e.code()));
            throw e;
        }
    }
}
