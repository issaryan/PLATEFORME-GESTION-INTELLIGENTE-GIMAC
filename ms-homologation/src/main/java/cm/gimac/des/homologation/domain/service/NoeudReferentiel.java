package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import java.util.List;

/**
 * Opération du référentiel vue comme nœud du graphe des prédécesseurs (HOM-02). {@code modele} porte les
 * attributs du modèle contrôlés à la publication (D-77, §2) ; il est absent d'un nœud de graphe pur.
 */
public record NoeudReferentiel(String code, String libelle, Segment segment, List<String> predecesseurs, Modele modele) {
    /** Attributs du modèle contrôlés à la publication : qualités R et V, geste, jalon, déclencheur, secret, livrables, durée. */
    public record Modele(
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
        public Modele {
            livrables = livrables == null ? List.of() : List.copyOf(livrables);
        }
    }

    public NoeudReferentiel {
        predecesseurs = predecesseurs == null ? List.of() : List.copyOf(predecesseurs);
    }

    public NoeudReferentiel(String code, String libelle, Segment segment, List<String> predecesseurs) {
        this(code, libelle, segment, predecesseurs, null);
    }
}
