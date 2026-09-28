package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.Escalation;
import cm.gimac.des.incident.repository.EscalationRepository;
import cm.gimac.des.incident.service.dto.EscalationDTO;
import cm.gimac.des.incident.service.mapper.EscalationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.Escalation}.
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
     * Get all the escalations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<EscalationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return escalationRepository.findAllWithEagerRelationships(pageable).map(escalationMapper::toDto);
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
        return escalationRepository.findOneWithEagerRelationships(id).map(escalationMapper::toDto);
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
