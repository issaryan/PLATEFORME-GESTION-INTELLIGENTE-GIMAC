package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.enumeration.Segment;
import java.util.List;

/** Opération du référentiel vue comme nœud du graphe des prédécesseurs (HOM-02). */
public record NoeudReferentiel(String code, String libelle, Segment segment, List<String> predecesseurs) {
    public NoeudReferentiel {
        predecesseurs = predecesseurs == null ? List.of() : List.copyOf(predecesseurs);
    }
}
