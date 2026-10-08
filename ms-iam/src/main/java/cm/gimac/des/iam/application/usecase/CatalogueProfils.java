package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.ProfilsCrees;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Couche;
import cm.gimac.des.iam.domain.compte.ProfilCree;
import cm.gimac.des.iam.domain.service.ReglesProfils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Catalogue des profils attribuables (D-62, D-81 §5) : profils livrés de la configuration
 * (gimac.iam.profils.attribuables) et profils créés par l'administration (table profil_cree).
 *
 * <p>D-94 (docs/lot1/ecrans-par-configuration.md) : aucune qualité n'est décrite par un nom de profil. Un profil,
 * livré ou créé, est privilégié dès que sa composition porte une permission IAM_* ou PLATFORM_* ; externe selon son
 * attribut gimac_externe (à défaut, une permission *_PORTAL_*). Un compte porte l'octroi privilégié quand l'un de
 * ses profils compose IAM_ROLE_PRIVILEGED_GRANT ; ses liens de courriel mènent à la console quand ses profils
 * composent l'une des permissions de la console (gimac.iam.liens.permissions-console).
 */
@Component
public class CatalogueProfils {

    private final IamProprietes proprietes;
    private final ProfilsCrees crees;
    private final Annuaire annuaire;

    public CatalogueProfils(IamProprietes proprietes, ProfilsCrees crees, Annuaire annuaire) {
        this.proprietes = proprietes;
        this.crees = crees;
        this.annuaire = annuaire;
    }

    /** Catalogue de la seule configuration, sans profil créé (tests et outils). */
    public static CatalogueProfils configuration(IamProprietes proprietes, Annuaire annuaire) {
        return new CatalogueProfils(proprietes, null, annuaire);
    }

    private List<ProfilCree> crees() {
        return crees == null ? List.of() : crees.tous();
    }

    /** Profils livrés avec la plateforme : jamais supprimables. */
    public Set<String> livres() {
        return proprietes.profils().attribuables();
    }

    /** Profils attribuables : configuration ∪ profils créés. */
    public Set<String> attribuables() {
        Set<String> s = new TreeSet<>(livres());
        crees().forEach(p -> s.add(p.code()));
        return s;
    }

    /**
     * Profils attribuables privilégiés : composition portant une permission IAM_* ou PLATFORM_* (lue dans le
     * fournisseur d'identité) ; un profil créé absent du fournisseur garde la qualité retenue à sa création.
     */
    public Set<String> privilegies() {
        Map<String, Set<String>> compositions = compositions();
        Set<String> s = new TreeSet<>();
        attribuables().forEach(code -> {
            Set<String> c = compositions.get(code);
            if (c != null && ReglesProfils.privilegie(c)) {
                s.add(code);
            }
        });
        crees().stream().filter(p -> !compositions.containsKey(p.code()) && p.privilegie()).forEach(p -> s.add(p.code()));
        return s;
    }

    /** Profils externes (attribut gimac_externe, ou repli *_PORTAL_*). */
    public Set<String> externes() {
        return annuaire == null ? Set.of() : annuaire.profilsExternes();
    }

    /** Permissions de chaque profil, par code. */
    public Map<String, Set<String>> compositions() {
        return annuaire == null ? Map.of() : annuaire.compositions();
    }

    /** Le compte porte IAM_ROLE_PRIVILEGED_GRANT par la composition de l'un de ses profils. */
    public boolean porteOctroiPrivilegie(Set<String> profils, Map<String, Set<String>> compositions) {
        return ReglesProfils.porteOctroiPrivilegie(profils, compositions);
    }

    /** Nombre de comptes actifs portant IAM_ROLE_PRIVILEGED_GRANT par la composition de leurs profils. */
    public long porteursActifsOctroiPrivilegie(Map<String, Set<String>> compositions) {
        return annuaire.lister().stream().filter(c -> c.actif() && ReglesProfils.porteOctroiPrivilegie(c.profils(), compositions)).count();
    }

    /** Couche des liens envoyés au compte (invitation, rappels, informations de sécurité). */
    public Couche couche(Compte c) {
        return couche(c, compositions(), proprietes);
    }

    /**
     * Couche des liens d'un compte (D-94) : portail pour un compte d'institution, console si ses profils composent
     * l'une des permissions de la console, département sinon.
     */
    public static Couche couche(Compte c, Map<String, Set<String>> compositions, IamProprietes proprietes) {
        return Couche.duLien(c, ReglesProfils.permissionsDe(c.profils(), compositions), proprietes.liens().permissionsConsole());
    }

    public boolean cree(String code) {
        return crees != null && !livres().contains(code) && crees.lire(code).isPresent();
    }
}
