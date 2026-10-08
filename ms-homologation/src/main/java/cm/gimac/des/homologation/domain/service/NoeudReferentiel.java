package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import java.util.List;

/**
 * Opération du référentiel vue comme nœud du graphe des prédécesseurs (HOM-02). {@code modele} porte les
 * attributs du modèle contrôlés à la publication (D-77, §2) ; il est absent d'un nœud de graphe pur.
 */
public record NoeudReferentiel(String code, String libelle, Segment segment, List<String> predecesseurs, Modele modele) {
    /**
     * Attributs du modèle contrôlés à la publication : qualités R et V, geste, jalon, déclencheur, secret, livrables,
     * durée, signatures et fonctions citées (catalogue, D-91).
     */
    public record Modele(
        Fonction responsable,
        Fonction validateur,
        Geste geste,
        Jalon jalon,
        String declencheur,
        boolean secret,
        String valeurReference,
        List<String> livrables,
        int delaiJours,
        List<SignatureOperation> signatures,
        List<Fonction> fonctions
    ) {
        public Modele {
            livrables = livrables == null ? List.of() : List.copyOf(livrables);
            signatures = signatures == null ? List.of() : List.copyOf(signatures);
            fonctions = fonctions == null ? List.of() : List.copyOf(fonctions);
        }

        /** Modèle sans signatures ni contrôle du catalogue des fonctions (D-77). */
        public Modele(
            Fonction responsable,
            Fonction validateur,
            Geste geste,
            Jalon jalon,
            String declencheur,
            boolean secret,
            String valeurReference,
            List<String> livrables,
            int delaiJours
        ) {
            this(responsable, validateur, geste, jalon, declencheur, secret, valeurReference, livrables, delaiJours, List.of(), List.of());
        }
    }

    public NoeudReferentiel {
        predecesseurs = predecesseurs == null ? List.of() : List.copyOf(predecesseurs);
    }

    public NoeudReferentiel(String code, String libelle, Segment segment, List<String> predecesseurs) {
        this(code, libelle, segment, predecesseurs, null);
    }
}
