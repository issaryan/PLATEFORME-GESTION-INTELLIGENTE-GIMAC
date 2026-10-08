package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite;
import java.util.List;
import java.util.Optional;

/**
 * Catalogue des fonctionnalités tenu par MS-IAM (D-94, table fonctionnalite) : valeurs de départ chargées au
 * démarrage depuis le registre de l'interface, modifications faites depuis la plateforme (avec leur date et leur
 * auteur), qui survivent aux déploiements.
 */
public interface CatalogueFonctionnalites {
    /** Toutes les fonctionnalités, triées par application, ordre et clé. */
    List<Fonctionnalite> toutes();

    Optional<Fonctionnalite> lire(String cle);

    /** Ajoute une valeur de départ (sans trace de modification) ; faux si la clé existe déjà. */
    boolean ajouterInitiale(Fonctionnalite f);

    /** Remplace une valeur de départ jamais modifiée depuis la plateforme ; faux sinon (rien n'est écrit). */
    boolean remplacerInitiale(Fonctionnalite f);

    /** Retire une valeur de départ jamais modifiée depuis la plateforme ; faux sinon (rien n'est écrit). */
    boolean retirerInitiale(String cle);

    /** Enregistre une modification faite depuis la plateforme (trace comprise). */
    void modifier(Fonctionnalite f);
}
