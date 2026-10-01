package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.List;
import java.util.Map;

/** Libellés des services du périmètre (CR, annexe A) ; un code inconnu est affiché tel quel. */
public final class CatalogueServices {

    /** Service toujours présent au périmètre contractualisé (HOM-05), sans objet pour la certification. */
    public static final String TRANSVERSE = "TRANSVERSE";

    private static final Map<String, String> LIBELLES = Map.ofEntries(
        Map.entry("RETRAIT", "Retrait"),
        Map.entry("PAIEMENT", "Paiement marchand"),
        Map.entry("TRANSFERT", "Transfert"),
        Map.entry("COMMERCE_ELECTRONIQUE", "Commerce électronique"),
        Map.entry("PAIEMENT_MARCHAND", "Paiement marchand mobile"),
        Map.entry("REMITTANCE", "Rémittence entrante"),
        Map.entry("FACTURE", "Paiement de facture"),
        Map.entry("CASH_IN", "Cash-in agent"),
        Map.entry("CARTE_INTERBANCAIRE", "Carte interbancaire"),
        Map.entry("PAIEMENT_MOBILE", "Paiement mobile"),
        Map.entry("DELEGATAIRE", "Délégataire")
    );

    /** Services offerts par type de projet (CR, annexe A ; délégataire : services carte), dans l'ordre d'affichage. */
    private static final List<String> CARTE = List.of("RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE");
    private static final List<String> MOBILE = List.of("PAIEMENT_MARCHAND", "REMITTANCE", "FACTURE", "CASH_IN");

    private CatalogueServices() {}

    /** Catalogue du type de projet : périmètre par défaut au cadrage quand aucun service souscrit n'est connu. */
    public static List<String> parType(TypeProjet type) {
        return type == TypeProjet.PAIEMENT_MOBILE ? MOBILE : CARTE;
    }

    public static String libelle(String code) {
        return LIBELLES.getOrDefault(code, code);
    }
}
