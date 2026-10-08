package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modèle d'un type de projet (D-91, docs/lot1/types-projet.md §1) : porte les comportements jusqu'ici codés par
 * type (attestations exigées, catalogue des services, plateforme des essais de l'annexe A, avancement repris du
 * simulateur). Un nouveau type hérite d'un des trois modèles repris, ou de {@link #GENERIQUE} : aucune
 * attestation imposée par le type, plateforme laissée à chaque opération, services sans catalogue.
 */
public enum ModeleTypeProjet {
    CARTE_INTERBANCAIRE("Carte interbancaire"),
    PAIEMENT_MOBILE("Paiement mobile"),
    DELEGATAIRE("Délégataire"),
    GENERIQUE("Générique");

    private static final List<String> SERVICES_CARTE = List.of("RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE");
    private static final List<String> SERVICES_MOBILE = List.of("PAIEMENT_MARCHAND", "REMITTANCE", "FACTURE", "CASH_IN");

    private final String libelle;

    ModeleTypeProjet(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /**
     * Attestations exigées du participant (rangs 21 et 22, D-56) : PCI DSS pour la carte et le délégataire, ISO 27001
     * et GSMA pour le mobile, aucune pour le modèle générique ; la conformité COBAC s'ajoute pour une fintech.
     */
    public List<TypeAttestation> attestationsExigees(boolean fintech) {
        List<TypeAttestation> l = new ArrayList<>(
            switch (this) {
                case PAIEMENT_MOBILE -> List.of(TypeAttestation.ISO_27001, TypeAttestation.GSMA);
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
            case GENERIQUE -> List.of();
            default -> SERVICES_CARTE;
        };
    }

    /** Plateforme des essais de l'annexe A (rangs 15 et 16) : le simulateur pour le mobile, sinon laissée à l'opération. */
    public PlateformeExecution plateformeEssais() {
        return this == PAIEMENT_MOBILE ? PlateformeExecution.SIMULATEUR : null;
    }

    /** Le périmètre affiche l'avancement repris du simulateur (paiement mobile). */
    public boolean avancementSimulateur() {
        return this == PAIEMENT_MOBILE;
    }

    /** Colonne de l'annexe A du CR dont le modèle reprend les opérations ; aucune pour le générique. */
    public Optional<TypeProjet> annexeA() {
        return this == GENERIQUE ? Optional.empty() : Optional.of(TypeProjet.valueOf(name()));
    }

    /** Modèle d'un type dont la définition n'est pas chargée : celui de même nom pour un type repris, sinon générique. */
    public static ModeleTypeProjet parDefaut(TypeProjet type) {
        if (type == null || !type.repris()) {
            return GENERIQUE;
        }
        return valueOf(type.name());
    }
}
