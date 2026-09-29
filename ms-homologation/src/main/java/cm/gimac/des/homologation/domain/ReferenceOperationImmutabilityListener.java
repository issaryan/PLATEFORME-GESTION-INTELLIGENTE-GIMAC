package cm.gimac.des.homologation.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * Défense en profondeur de la garde d'immuabilité (HOM-01 A2) : aucune écriture sur une opération
 * d'une version publiée ou retirée, quel que soit le chemin de code.
 */
public class ReferenceOperationImmutabilityListener {

    @PrePersist
    @PreUpdate
    @PreRemove
    void verifier(ReferenceOperation operation) {
        if (operation.getVersion() != null) {
            operation.getVersion().assertModifiable();
        }
    }
}
