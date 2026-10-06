package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.compte.ProfilCree;
import java.util.List;
import java.util.Optional;

/** Profils créés par l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §5), table profil_cree. */
public interface ProfilsCrees {
    List<ProfilCree> tous();

    Optional<ProfilCree> lire(String code);

    void enregistrer(ProfilCree profil);

    void supprimer(String code);
}
