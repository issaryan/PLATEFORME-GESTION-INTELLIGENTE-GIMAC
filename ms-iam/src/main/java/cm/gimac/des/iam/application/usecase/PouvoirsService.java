package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.MatricePouvoirs;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.Pouvoirs;
import cm.gimac.des.iam.domain.pouvoir.PouvoirsProfil;
import cm.gimac.des.iam.domain.service.ReglesPouvoirs;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Matrice des pouvoirs des chefs sur leurs subordonnés (D-81, docs/lot1/pouvoirs-hierarchiques.md §3) :
 * lecture et réglage par l'administration (journal MATRICE_POUVOIRS_MODIFIEE), pouvoirs d'un compte
 * (portée la plus large de ses profils), vue « mes pouvoirs » avec les subordonnés couverts et les unités
 * dirigées, contrôle de couverture d'un compte par une action.
 */
@Service
@Transactional
public class PouvoirsService {

    /** Ligne de la matrice pour un profil attribuable, avec son libellé (D-86 §2 ; absent : le code). */
    public record LigneMatrice(String profil, String libelle, boolean privilegie, boolean cree, Map<ActionPouvoir, Portee> actions, Set<String> profilsDemandables) {}

    /** Matrice complète : actions et portées dans leur ordre, une ligne par profil attribuable. */
    public record Matrice(List<ActionPouvoir> actions, List<Portee> portees, List<LigneMatrice> profils) {}

    /**
     * Pouvoirs d'un compte (GET /pouvoirs/moi, /interne/pouvoirs/{id}) : portée de chaque action, profils
     * demandables, comptes couverts par sa portée la plus large, unités dirigées ; libellé de chaque profil
     * demandable (D-86 §2, par code).
     */
    public record VuePouvoirs(
        Map<ActionPouvoir, Portee> actions,
        Set<String> profilsDemandables,
        List<OrganisationService.MembreEquipe> subordonnes,
        List<String> unitesDirigees,
        Map<String, String> libellesProfils
    ) {}

    private final MatricePouvoirs matrice;
    private final CatalogueProfils catalogue;
    private final Annuaire annuaire;
    private final OrganisationService organisation;
    private final JournalActes journal;

    public PouvoirsService(MatricePouvoirs matrice, CatalogueProfils catalogue, Annuaire annuaire, OrganisationService organisation, JournalActes journal) {
        this.matrice = matrice;
        this.catalogue = catalogue;
        this.annuaire = annuaire;
        this.organisation = organisation;
        this.journal = journal;
    }

    // ------------------------------------------------------------------ matrice

    @Transactional(readOnly = true)
    public Matrice matrice() {
        Map<String, PouvoirsProfil> lignes = matrice.toutes();
        Set<String> privilegies = catalogue.privilegies();
        Set<String> livres = catalogue.livres();
        Map<String, String> libelles = libelles();
        List<LigneMatrice> l = catalogue.attribuables().stream()
            .map(p -> ligne(lignes.getOrDefault(p, PouvoirsProfil.aucun(p)), privilegies, livres, libelles))
            .toList();
        return new Matrice(Arrays.asList(ActionPouvoir.values()), Arrays.asList(Portee.values()), l);
    }

    /**
     * Règle la ligne d'un profil : portée de chaque action (absente : AUCUNE) et profils demandables
     * (attribuables, jamais privilégiés). Journal MATRICE_POUVOIRS_MODIFIEE (avant, après).
     */
    public LigneMatrice definir(Acteur acteur, String profil, Map<ActionPouvoir, Portee> actions, Set<String> profilsDemandables, String motif) {
        Set<String> attribuables = catalogue.attribuables();
        if (!attribuables.contains(profil)) {
            throw IamException.introuvable("IAM_PROFILE_NOT_FOUND", profil);
        }
        Set<String> privilegies = catalogue.privilegies();
        PouvoirsProfil nouvelle = new PouvoirsProfil(profil, actions, profilsDemandables);
        ReglesPouvoirs.verifierLigne(nouvelle, attribuables, privilegies);
        PouvoirsProfil avant = matrice.lire(profil);
        matrice.enregistrer(nouvelle);
        journal.matrice(acteur, profil, motif == null || motif.isBlank() ? null : motif.trim(), avant.resume(), nouvelle.resume());
        return ligne(nouvelle, privilegies, catalogue.livres(), libelles());
    }

    private static LigneMatrice ligne(PouvoirsProfil p, Set<String> privilegies, Set<String> livres, Map<String, String> libelles) {
        return new LigneMatrice(p.profil(), libelles.getOrDefault(p.profil(), p.profil()), privilegies.contains(p.profil()), !livres.contains(p.profil()), p.actions(), p.profilsDemandables());
    }

    /** Libellé de chaque profil du fournisseur d'identité (D-86 §2), le code quand il n'est pas renseigné. */
    private Map<String, String> libelles() {
        Map<String, String> m = new TreeMap<>();
        annuaire.presentationsProfils().forEach((code, p) -> m.put(code, p.libelle() == null || p.libelle().isBlank() ? code : p.libelle()));
        return m;
    }

    // ------------------------------------------------------------------ pouvoirs d'un compte

    /** Pouvoirs d'un compte d'après ses profils actuels ; un compte inactif n'en a aucun. */
    @Transactional(readOnly = true)
    public Pouvoirs pouvoirs(String compteId) {
        Compte c = annuaire.lire(compteId).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", compteId));
        return pouvoirs(c);
    }

    Pouvoirs pouvoirs(Compte c) {
        if (!c.actif() || !c.interne()) {
            return new Pouvoirs(Map.of(), Set.of());
        }
        return ReglesPouvoirs.calculer(c.profils(), matrice.toutes(), catalogue.attribuables(), catalogue.privilegies());
    }

    @Transactional(readOnly = true)
    public VuePouvoirs vue(String compteId) {
        Pouvoirs p = pouvoirs(compteId);
        Map<String, String> libellesDemandables = new TreeMap<>();
        if (!p.profilsDemandables().isEmpty()) {
            Map<String, String> libelles = libelles();
            p.profilsDemandables().forEach(code -> libellesDemandables.put(code, libelles.getOrDefault(code, code)));
        }
        return new VuePouvoirs(p.actions(), p.profilsDemandables(), organisation.perimetre(compteId, p.porteeMax()).comptes(), organisation.unitesDirigees(compteId), libellesDemandables);
    }

    // ------------------------------------------------------------------ rattachement d'un subordonné

    /**
     * Rattachement d'un subordonné par son chef (D-81 §3, action RATTACHEMENT_MODIFIER) : cible dans le
     * périmètre de la portée de l'action, unité actuelle ou dirigée, n+1 actuel, le chef ou un compte de son
     * périmètre (sinon 403 ORG_HORS_PERIMETRE), jamais soi-même (403 IAM_SELF_ADMINISTRATION) ; puis règles
     * habituelles, journal COMPTE_RATTACHEMENT_MODIFIE (acteur : le chef) et événement de la route
     * d'administration.
     */
    public Compte rattacherSubordonne(Acteur acteur, String cibleId, String unite, String responsableId, String motif) {
        Compte chef = annuaire.lire(acteur.id()).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", acteur.id()));
        if (chef.id().equals(cibleId)) {
            throw IamException.interdit("IAM_SELF_ADMINISTRATION", "Un compte ne modifie pas son propre rattachement");
        }
        Portee portee = pouvoirs(chef).portee(ActionPouvoir.RATTACHEMENT_MODIFIER);
        ReglesPouvoirs.exigerCouverture(portee != Portee.AUCUNE, ActionPouvoir.RATTACHEMENT_MODIFIER);
        List<OrganisationService.MembreEquipe> perimetre = organisation.perimetre(chef.id(), portee).comptes();
        Set<String> couverts = new HashSet<>();
        perimetre.forEach(m -> couverts.add(m.id()));
        OrganisationService.MembreEquipe cible = perimetre.stream().filter(m -> m.id().equals(cibleId)).findFirst().orElse(null);
        ReglesPouvoirs.exigerCouverture(cible != null, ActionPouvoir.RATTACHEMENT_MODIFIER);
        String u = OrganisationService.vide(unite);
        String r = OrganisationService.vide(responsableId);
        ReglesPouvoirs.verifierRattachementSubordonne(chef.id(), cible.id(), cible.unite(), cible.responsableId(), u, r, couverts, organisation.unitesDirigees(chef.id()));
        return organisation.rattacher(acteur, cible.id(), u, r, motif, "pouvoir=" + ActionPouvoir.RATTACHEMENT_MODIFIER.name() + " portee=" + portee.name());
    }

    /** Périmètre d'un compte pour une action : celui de la portée que lui donne sa matrice. */
    @Transactional(readOnly = true)
    public OrganisationService.Perimetre perimetre(String compteId, ActionPouvoir action) {
        return organisation.perimetre(compteId, pouvoirs(compteId).portee(action));
    }
}
