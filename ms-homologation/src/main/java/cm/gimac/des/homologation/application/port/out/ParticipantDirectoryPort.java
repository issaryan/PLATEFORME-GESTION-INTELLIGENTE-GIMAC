package cm.gimac.des.homologation.application.port.out;

import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import java.util.UUID;

/**
 * Registre des institutions (MS-PARTICIPANT, interface interne) : éligibilité à l'ouverture d'une
 * liaison (étape 3, D-57) et services souscrits dont dérive le périmètre contractualisé (HOM-05).
 */
public interface ParticipantDirectoryPort {
    /** Institution lue ; 409 HOM_LIAISON_INSTITUTION_UNKNOWN si absente, 503 si le registre est injoignable. */
    InstitutionView getInstitution(UUID institutionId);

    /**
     * Institution d'un code membre (reprise de l'existant, D-107) ; vide si aucune, 503 si le registre est injoignable.
     */
    java.util.Optional<InstitutionView> parCodeMembre(String codeMembre);
}
