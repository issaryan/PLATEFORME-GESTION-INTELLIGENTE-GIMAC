package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.Escalation;
import cm.gimac.des.notification.repository.EscalationRepository;
import cm.gimac.des.notification.service.dto.EscalationDTO;
import cm.gimac.des.notification.service.mapper.EscalationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.notification.domain.Escalation}.
 */
@Service
@Transactional
public class EscalationService {

    private static final Logger LOG = LoggerFactory.getLogger(EscalationService.class);

    private final EscalationRepository escalationRepository;

    private final EscalationMapper escalationMapper;

    public EscalationService(EscalationRepository escalationRepository, EscalationMapper escalationMapper) {
        this.escalationRepository = escalationRepository;
        this.escalationMapper = escalationMapper;
    }

    /**
     * Save a escalation.
     *
     * @param escalationDTO the entity to save.
     * @return the persisted entity.
     */
    public EscalationDTO save(EscalationDTO escalationDTO) {
        LOG.debug("Request to save Escalation : {}", escalationDTO);
        Escalation escalation = escalationMapper.toEntity(escalationDTO);
        escalation = escalationRepository.save(escalation);
        return escalationMapper.toDto(escalation);
    }

    /**
     * Update a escalation.
     *
     * @param escalationDTO the entity to save.
     * @return the persisted entity.
     */
    public EscalationDTO update(EscalationDTO escalationDTO) {
        LOG.debug("Request to update Escalation : {}", escalationDTO);
        Escalation escalation = escalationMapper.toEntity(escalationDTO);
        escalation = escalationRepository.save(escalation);
        return escalationMapper.toDto(escalation);
    }

    /**
     * Partially update a escalation.
     *
     * @param escalationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<EscalationDTO> partialUpdate(EscalationDTO escalationDTO) {
        LOG.debug("Request to partially update Escalation : {}", escalationDTO);

        return escalationRepository
            .findById(escalationDTO.getId())
            .map(existingEscalation -> {
                escalationMapper.partialUpdate(existingEscalation, escalationDTO);

                return existingEscalation;
            })
            .map(escalationRepository::save)
            .map(escalationMapper::toDto);
    }

    /**
     * Get one escalation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EscalationDTO> findOne(UUID id) {
        LOG.debug("Request to get Escalation : {}", id);
        return escalationRepository.findById(id).map(escalationMapper::toDto);
    }

    /**
     * Delete the escalation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Escalation : {}", id);
        escalationRepository.deleteById(id);
    }
}
