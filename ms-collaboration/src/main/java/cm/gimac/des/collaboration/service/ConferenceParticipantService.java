package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.ConferenceParticipant;
import cm.gimac.des.collaboration.repository.ConferenceParticipantRepository;
import cm.gimac.des.collaboration.service.dto.ConferenceParticipantDTO;
import cm.gimac.des.collaboration.service.mapper.ConferenceParticipantMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.ConferenceParticipant}.
 */
@Service
@Transactional
public class ConferenceParticipantService {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenceParticipantService.class);

    private final ConferenceParticipantRepository conferenceParticipantRepository;

    private final ConferenceParticipantMapper conferenceParticipantMapper;

    public ConferenceParticipantService(
        ConferenceParticipantRepository conferenceParticipantRepository,
        ConferenceParticipantMapper conferenceParticipantMapper
    ) {
        this.conferenceParticipantRepository = conferenceParticipantRepository;
        this.conferenceParticipantMapper = conferenceParticipantMapper;
    }

    /**
     * Save a conferenceParticipant.
     *
     * @param conferenceParticipantDTO the entity to save.
     * @return the persisted entity.
     */
    public ConferenceParticipantDTO save(ConferenceParticipantDTO conferenceParticipantDTO) {
        LOG.debug("Request to save ConferenceParticipant : {}", conferenceParticipantDTO);
        ConferenceParticipant conferenceParticipant = conferenceParticipantMapper.toEntity(conferenceParticipantDTO);
        conferenceParticipant = conferenceParticipantRepository.save(conferenceParticipant);
        return conferenceParticipantMapper.toDto(conferenceParticipant);
    }

    /**
     * Update a conferenceParticipant.
     *
     * @param conferenceParticipantDTO the entity to save.
     * @return the persisted entity.
     */
    public ConferenceParticipantDTO update(ConferenceParticipantDTO conferenceParticipantDTO) {
        LOG.debug("Request to update ConferenceParticipant : {}", conferenceParticipantDTO);
        ConferenceParticipant conferenceParticipant = conferenceParticipantMapper.toEntity(conferenceParticipantDTO);
        conferenceParticipant = conferenceParticipantRepository.save(conferenceParticipant);
        return conferenceParticipantMapper.toDto(conferenceParticipant);
    }

    /**
     * Partially update a conferenceParticipant.
     *
     * @param conferenceParticipantDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ConferenceParticipantDTO> partialUpdate(ConferenceParticipantDTO conferenceParticipantDTO) {
        LOG.debug("Request to partially update ConferenceParticipant : {}", conferenceParticipantDTO);

        return conferenceParticipantRepository
            .findById(conferenceParticipantDTO.getId())
            .map(existingConferenceParticipant -> {
                conferenceParticipantMapper.partialUpdate(existingConferenceParticipant, conferenceParticipantDTO);

                return existingConferenceParticipant;
            })
            .map(conferenceParticipantRepository::save)
            .map(conferenceParticipantMapper::toDto);
    }

    /**
     * Get all the conferenceParticipants.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ConferenceParticipantDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all ConferenceParticipants");
        return conferenceParticipantRepository.findAll(pageable).map(conferenceParticipantMapper::toDto);
    }

    /**
     * Get all the conferenceParticipants with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ConferenceParticipantDTO> findAllWithEagerRelationships(Pageable pageable) {
        return conferenceParticipantRepository.findAllWithEagerRelationships(pageable).map(conferenceParticipantMapper::toDto);
    }

    /**
     * Get one conferenceParticipant by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ConferenceParticipantDTO> findOne(UUID id) {
        LOG.debug("Request to get ConferenceParticipant : {}", id);
        return conferenceParticipantRepository.findOneWithEagerRelationships(id).map(conferenceParticipantMapper::toDto);
    }

    /**
     * Delete the conferenceParticipant by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ConferenceParticipant : {}", id);
        conferenceParticipantRepository.deleteById(id);
    }
}
