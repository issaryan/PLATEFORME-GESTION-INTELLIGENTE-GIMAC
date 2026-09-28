package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.EscalationContact;
import cm.gimac.des.participant.repository.EscalationContactRepository;
import cm.gimac.des.participant.service.dto.EscalationContactDTO;
import cm.gimac.des.participant.service.mapper.EscalationContactMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.EscalationContact}.
 */
@Service
@Transactional
public class EscalationContactService {

    private static final Logger LOG = LoggerFactory.getLogger(EscalationContactService.class);

    private final EscalationContactRepository escalationContactRepository;

    private final EscalationContactMapper escalationContactMapper;

    public EscalationContactService(
        EscalationContactRepository escalationContactRepository,
        EscalationContactMapper escalationContactMapper
    ) {
        this.escalationContactRepository = escalationContactRepository;
        this.escalationContactMapper = escalationContactMapper;
    }

    /**
     * Save a escalationContact.
     *
     * @param escalationContactDTO the entity to save.
     * @return the persisted entity.
     */
    public EscalationContactDTO save(EscalationContactDTO escalationContactDTO) {
        LOG.debug("Request to save EscalationContact : {}", escalationContactDTO);
        EscalationContact escalationContact = escalationContactMapper.toEntity(escalationContactDTO);
        escalationContact = escalationContactRepository.save(escalationContact);
        return escalationContactMapper.toDto(escalationContact);
    }

    /**
     * Update a escalationContact.
     *
     * @param escalationContactDTO the entity to save.
     * @return the persisted entity.
     */
    public EscalationContactDTO update(EscalationContactDTO escalationContactDTO) {
        LOG.debug("Request to update EscalationContact : {}", escalationContactDTO);
        EscalationContact escalationContact = escalationContactMapper.toEntity(escalationContactDTO);
        escalationContact = escalationContactRepository.save(escalationContact);
        return escalationContactMapper.toDto(escalationContact);
    }

    /**
     * Partially update a escalationContact.
     *
     * @param escalationContactDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<EscalationContactDTO> partialUpdate(EscalationContactDTO escalationContactDTO) {
        LOG.debug("Request to partially update EscalationContact : {}", escalationContactDTO);

        return escalationContactRepository
            .findById(escalationContactDTO.getId())
            .map(existingEscalationContact -> {
                escalationContactMapper.partialUpdate(existingEscalationContact, escalationContactDTO);

                return existingEscalationContact;
            })
            .map(escalationContactRepository::save)
            .map(escalationContactMapper::toDto);
    }

    /**
     * Get all the escalationContacts with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<EscalationContactDTO> findAllWithEagerRelationships(Pageable pageable) {
        return escalationContactRepository.findAllWithEagerRelationships(pageable).map(escalationContactMapper::toDto);
    }

    /**
     * Get one escalationContact by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EscalationContactDTO> findOne(UUID id) {
        LOG.debug("Request to get EscalationContact : {}", id);
        return escalationContactRepository.findOneWithEagerRelationships(id).map(escalationContactMapper::toDto);
    }

    /**
     * Delete the escalationContact by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete EscalationContact : {}", id);
        escalationContactRepository.deleteById(id);
    }
}
