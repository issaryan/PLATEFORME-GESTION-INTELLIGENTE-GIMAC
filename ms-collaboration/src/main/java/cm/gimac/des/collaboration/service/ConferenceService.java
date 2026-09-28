package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.repository.ConferenceRepository;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.service.mapper.ConferenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.Conference}.
 */
@Service
@Transactional
public class ConferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenceService.class);

    private final ConferenceRepository conferenceRepository;

    private final ConferenceMapper conferenceMapper;

    public ConferenceService(ConferenceRepository conferenceRepository, ConferenceMapper conferenceMapper) {
        this.conferenceRepository = conferenceRepository;
        this.conferenceMapper = conferenceMapper;
    }

    /**
     * Save a conference.
     *
     * @param conferenceDTO the entity to save.
     * @return the persisted entity.
     */
    public ConferenceDTO save(ConferenceDTO conferenceDTO) {
        LOG.debug("Request to save Conference : {}", conferenceDTO);
        Conference conference = conferenceMapper.toEntity(conferenceDTO);
        conference = conferenceRepository.save(conference);
        return conferenceMapper.toDto(conference);
    }

    /**
     * Update a conference.
     *
     * @param conferenceDTO the entity to save.
     * @return the persisted entity.
     */
    public ConferenceDTO update(ConferenceDTO conferenceDTO) {
        LOG.debug("Request to update Conference : {}", conferenceDTO);
        Conference conference = conferenceMapper.toEntity(conferenceDTO);
        conference = conferenceRepository.save(conference);
        return conferenceMapper.toDto(conference);
    }

    /**
     * Partially update a conference.
     *
     * @param conferenceDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ConferenceDTO> partialUpdate(ConferenceDTO conferenceDTO) {
        LOG.debug("Request to partially update Conference : {}", conferenceDTO);

        return conferenceRepository
            .findById(conferenceDTO.getId())
            .map(existingConference -> {
                conferenceMapper.partialUpdate(existingConference, conferenceDTO);

                return existingConference;
            })
            .map(conferenceRepository::save)
            .map(conferenceMapper::toDto);
    }

    /**
     * Get all the conferences with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ConferenceDTO> findAllWithEagerRelationships(Pageable pageable) {
        return conferenceRepository.findAllWithEagerRelationships(pageable).map(conferenceMapper::toDto);
    }

    /**
     * Get one conference by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ConferenceDTO> findOne(UUID id) {
        LOG.debug("Request to get Conference : {}", id);
        return conferenceRepository.findOneWithEagerRelationships(id).map(conferenceMapper::toDto);
    }

    /**
     * Delete the conference by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Conference : {}", id);
        conferenceRepository.deleteById(id);
    }
}
