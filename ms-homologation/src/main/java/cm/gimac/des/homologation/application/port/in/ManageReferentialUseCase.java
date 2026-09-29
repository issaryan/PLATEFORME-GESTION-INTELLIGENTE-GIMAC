package cm.gimac.des.homologation.application.port.in;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.UUID;

/** Cycle de vie des versions du référentiel d'opérations (HOM-01). */
public interface ManageReferentialUseCase {
    ReferentialVersion creerBrouillon(TypeProjet type, String libelle, UUID versionSourceId);

    ReferenceOperation enregistrerOperation(UUID versionId, String code, ReferenceOperationData data);

    ReferentialVersion publier(UUID versionId);
}
