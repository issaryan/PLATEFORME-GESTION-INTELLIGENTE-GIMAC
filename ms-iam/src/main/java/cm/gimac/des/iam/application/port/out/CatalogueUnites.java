package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.organisation.Unite;
import java.util.List;

/**
 * Catalogue des unités organisationnelles du GIMAC (D-76, docs/lot1/mon-activite.md §1.1), tenu par
 * MS-IAM (table unite_organisationnelle) et modifiable par l'administration.
 */
public interface CatalogueUnites {
    /** Toutes les unités, actives ou non, triées par code. */
    List<Unite> toutes();

    /** Crée ou remplace l'unité de ce code. */
    Unite enregistrer(Unite unite);
}
