package cm.gimac.des.homologation.application.port.in;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Position;
import java.util.Map;
import java.util.UUID;

/** Cycle de vie des versions du référentiel d'opérations (HOM-01) et modélisateur (D-77). */
public interface ManageReferentialUseCase {
    ReferentialVersion creerBrouillon(TypeProjet type, String libelle, UUID versionSourceId);

    /** Brouillon créé ou dérivé, confié à un pilote (par défaut le créateur), destinataire d'une tâche (D-77, §5). */
    default ReferentialVersion creerBrouillon(TypeProjet type, String libelle, UUID versionSourceId, UUID createurId, UUID piloteId) {
        return creerBrouillon(type, libelle, versionSourceId);
    }

    /**
     * Brouillon v1 d'un nouveau type de projet copié de la version active d'un autre type (D-91) : étapes,
     * opérations, rôles, livrables et signatures.
     */
    ReferentialVersion copierVers(UUID versionSourceId, TypeProjet cible, String libelle, UUID createurId);

    /** Suppression d'un brouillon (D-92) ; une version publiée ou retirée ne se supprime jamais (409 HOM_REFERENTIAL_FROZEN). */
    void supprimerBrouillon(UUID versionId);

    ReferenceOperation enregistrerOperation(UUID versionId, String code, ReferenceOperationData data);

    ReferentialVersion publier(UUID versionId);

    /** Création d'une opération au code fourni ; 409 HOM_OPERATION_CODE_DUPLICATE si le code est pris. */
    ReferenceOperation creerOperation(UUID versionId, String code, ReferenceOperationData data);

    /** Retrait d'une opération et de son code des prédécesseurs des autres. */
    void supprimerOperation(UUID versionId, String code);

    /** Placement des opérations dans le canevas. */
    void placer(UUID versionId, Map<String, Position> positions);

    /** Ajout ou retrait d'un arc ; un arc qui fermerait un cycle est refusé (409 HOM_REFERENTIAL_CYCLE). */
    ReferenceOperation arc(UUID versionId, String de, String vers, boolean present);

    /** Pilote du brouillon : compte portant HOM_REFERENTIEL_EDIT ; sa tâche suit le changement. */
    ReferentialVersion designerPilote(UUID versionId, UUID compteId, UUID demandeurId);
}
