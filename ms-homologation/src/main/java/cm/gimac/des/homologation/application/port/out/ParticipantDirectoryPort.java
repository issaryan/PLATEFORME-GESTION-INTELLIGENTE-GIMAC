package cm.gimac.des.homologation.application.port.out;

import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import java.util.UUID;

/** Port sortant : registre des institutions (MS-PARTICIPANT), lecture seule. */
public interface ParticipantDirectoryPort {
    InstitutionView getInstitution(UUID institutionId);
}
