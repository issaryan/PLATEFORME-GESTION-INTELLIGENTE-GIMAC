package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.SubscribedServiceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecture d'une institution par les autres services (PI §0.7, jeton de service) : MS-HOMOLOGATION
 * y vérifie l'éligibilité à l'ouverture d'une liaison (étape 3, D-57) et y lit les services
 * souscrits dont il dérive le périmètre contractualisé (HOM-05).
 */
@Service
@Transactional(readOnly = true)
public class InstitutionInterneQueries {

    public record Vue(Institution institution, List<SubscribedService> servicesSouscrits) {}

    private final InstitutionRepository institutions;
    private final SubscribedServiceRepository services;

    public InstitutionInterneQueries(InstitutionRepository institutions, SubscribedServiceRepository services) {
        this.institutions = institutions;
        this.services = services;
    }

    /** Institution d'un code membre (reprise des liaisons par MS-HOMOLOGATION, D-107) ; 404 si aucune. */
    public Vue parCode(String codeParticipant) {
        Institution i = institutions
            .findByParticipantCode(codeParticipant == null ? "" : codeParticipant.trim())
            .orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", codeParticipant));
        return lire(i.getId());
    }

    public Vue lire(UUID id) {
        Institution i = institutions.findById(id).orElseThrow(() -> ParticipantException.notFound("PAR_INSTITUTION_NOT_FOUND", id));
        return new Vue(i, services.findByInstitution_IdAndStateOrderByServiceCode(id, SubscriptionState.ACTIF));
    }
}
