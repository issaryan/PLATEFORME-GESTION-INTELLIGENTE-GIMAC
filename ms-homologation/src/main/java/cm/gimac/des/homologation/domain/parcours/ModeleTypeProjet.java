package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modèle d'un type de projet (D-91, docs/lot1/types-projet.md §1) : porte les comportements jusqu'ici codés par
 * type (attestations exigées, catalogue des services, plateforme des essais de l'annexe A, avancement repris du
 * simulateur). Un nouveau type hérite d'un des trois modèles repris, ou de {@link #GENERIQUE} : aucune
 * attestation imposée par le type, plateforme laissée à chaque opération, services sans catalogue. Le modèle
 * {@link #MOBILE_ISO20022} (D-148) est celui du paiement mobile restreint à l'interface ISO 20022 : ses services sont
 * ceux de préfixe {@code ISO.} (D-143), ses opérations celles de la colonne paiement mobile de l'annexe A.
 */
public enum ModeleTypeProjet {
    CARTE_INTERBANCAIRE("Carte interbancaire"),
    PAIEMENT_MOBILE("Paiement mobile"),
    DELEGATAIRE("Délégataire"),
    GENERIQUE("Générique"),
    MOBILE_ISO20022("Paiement mobile ISO 20022");

    /** Préfixe des codes de service de l'interface ISO 20022 (TDR-SBX-GIMAC-02 D.1, D-143). */
    public static final String PREFIXE_ISO = "ISO.";

    private static final List<String> SERVICES_CARTE = List.of("RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE");
    private static final List<String> SERVICES_MOBILE = List.of("PAIEMENT_MARCHAND", "REMITTANCE", "FACTURE", "CASH_IN");
    /** Services ISO 20022 du catalogue amorcé de SBX-REFERENTIEL (référentiel MOBILE_ISO20022). */
    private static final List<String> SERVICES_ISO = List.of("ISO.W2W", "ISO.R2P");

    private final String libelle;

    ModeleTypeProjet(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /**
     * Attestations exigées du participant (rangs 21 et 22, D-56) : PCI DSS pour la carte et le délégataire, ISO 27001
     * et GSMA pour le mobile (ISO 20022 compris), aucune pour le modèle générique ; la conformité COBAC s'ajoute pour
     * une fintech.
     */
    public List<TypeAttestation> attestationsExigees(boolean fintech) {
        List<TypeAttestation> l = new ArrayList<>(
            switch (this) {
                case PAIEMENT_MOBILE, MOBILE_ISO20022 -> List.of(TypeAttestation.ISO_27001, TypeAttestation.GSMA);
                case GENERIQUE -> List.<TypeAttestation>of();
                default -> List.of(TypeAttestation.PCI_DSS);
            }
        );
        if (fintech) {
            l.add(TypeAttestation.COBAC);
        }
        return List.copyOf(l);
    }

    /** Services proposés au cadrage quand aucun service souscrit n'est connu (CR annexe A) ; vide pour le générique. */
    public List<String> catalogueServices() {
        return switch (this) {
            case PAIEMENT_MOBILE -> SERVICES_MOBILE;
            case MOBILE_ISO20022 -> SERVICES_ISO;
            case GENERIQUE -> List.of();
            default -> SERVICES_CARTE;
        };
    }

    /**
     * Service retenu au périmètre dérivé des services souscrits : pour le modèle ISO 20022, les seuls services de
     * préfixe {@code ISO.} ; pour les autres modèles, tout service souscrit (comportement inchangé).
     */
    public boolean retient(String codeService) {
        return this != MOBILE_ISO20022 || (codeService != null && codeService.startsWith(PREFIXE_ISO));
    }

    /** Modèle du paiement mobile, propriétaire ou ISO 20022. */
    private boolean mobile() {
        return this == PAIEMENT_MOBILE || this == MOBILE_ISO20022;
    }

    /** Plateforme des essais de l'annexe A (rangs 15 et 16) : le simulateur pour le mobile, sinon laissée à l'opération. */
    public PlateformeExecution plateformeEssais() {
        return mobile() ? PlateformeExecution.SIMULATEUR : null;
    }

    /** Le périmètre affiche l'avancement repris du simulateur (paiement mobile, ISO 20022 compris). */
    public boolean avancementSimulateur() {
        return mobile();
    }

    /**
     * Colonne de l'annexe A du CR dont le modèle reprend les opérations ; aucune pour le générique, celle du paiement
     * mobile pour le modèle ISO 20022.
     */
    public Optional<TypeProjet> annexeA() {
        return switch (this) {
            case GENERIQUE -> Optional.empty();
            case MOBILE_ISO20022 -> Optional.of(TypeProjet.PAIEMENT_MOBILE);
            default -> Optional.of(TypeProjet.valueOf(name()));
        };
    }

    /** Modèle d'un type dont la définition n'est pas chargée : celui de même nom pour un type repris, sinon générique. */
    public static ModeleTypeProjet parDefaut(TypeProjet type) {
        if (type == null || !type.repris()) {
            return GENERIQUE;
        }
        return valueOf(type.name());
    }
}
