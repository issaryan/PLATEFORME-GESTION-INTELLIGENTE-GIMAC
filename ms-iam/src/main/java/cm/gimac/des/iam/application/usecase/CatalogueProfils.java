package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.ProfilsCrees;
import cm.gimac.des.iam.domain.compte.ProfilCree;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Catalogue des profils attribuables (D-62, D-81 §5) : profils livrés de la configuration
 * (gimac.iam.profils.attribuables) et profils créés par l'administration (table profil_cree). Privilégiés :
 * ceux de la configuration (gimac.iam.profils.privilegies) et les profils créés portant une permission
 * IAM_* ou PLATFORM_*.
 */
@Component
public class CatalogueProfils {

    private final IamProprietes proprietes;
    private final ProfilsCrees crees;

    public CatalogueProfils(IamProprietes proprietes, ProfilsCrees crees) {
        this.proprietes = proprietes;
        this.crees = crees;
    }

    /** Catalogue de la seule configuration, sans profil créé (tests et outils). */
    public static CatalogueProfils configuration(IamProprietes proprietes) {
        return new CatalogueProfils(proprietes, null);
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

    public Set<String> privilegies() {
        Set<String> s = new TreeSet<>(proprietes.profils().privilegies());
        crees().stream().filter(ProfilCree::privilegie).forEach(p -> s.add(p.code()));
        return s;
    }

    public boolean cree(String code) {
        return crees != null && !livres().contains(code) && crees.lire(code).isPresent();
    }
}
