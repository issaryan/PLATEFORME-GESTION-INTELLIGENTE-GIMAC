package cm.gimac.des.homologation.domain.parcours;

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

    public static String libelle(String code) {
        return LIBELLES.getOrDefault(code, code);
    }
}
