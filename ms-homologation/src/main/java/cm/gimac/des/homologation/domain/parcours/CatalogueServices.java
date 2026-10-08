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

    private CatalogueServices() {}

    /**
     * Catalogue d'un type repris : périmètre par défaut au cadrage quand aucun service souscrit n'est connu (porté
     * par le modèle du type depuis D-91, {@link ModeleTypeProjet#catalogueServices()}).
     */
    public static List<String> parType(TypeProjet type) {
        return ModeleTypeProjet.parDefaut(type).catalogueServices();
    }

    public static String libelle(String code) {
        return LIBELLES.getOrDefault(code, code);
    }
}
