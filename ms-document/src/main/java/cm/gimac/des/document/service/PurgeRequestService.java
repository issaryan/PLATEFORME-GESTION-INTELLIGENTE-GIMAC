package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.PurgeRequest;
import cm.gimac.des.document.repository.PurgeRequestRepository;
import cm.gimac.des.document.service.dto.PurgeRequestDTO;
import cm.gimac.des.document.service.mapper.PurgeRequestMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.document.domain.PurgeRequest}.
 */
@Service
@Transactional
public class PurgeRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(PurgeRequestService.class);

    private final PurgeRequestRepository purgeRequestRepository;

    private final PurgeRequestMapper purgeRequestMapper;

    public PurgeRequestService(PurgeRequestRepository purgeRequestRepository, PurgeRequestMapper purgeRequestMapper) {
        this.purgeRequestRepository = purgeRequestRepository;
        this.purgeRequestMapper = purgeRequestMapper;
    }

    /**
     * Save a purgeRequest.
     *
     * @param purgeRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public PurgeRequestDTO save(PurgeRequestDTO purgeRequestDTO) {
        LOG.debug("Request to save PurgeRequest : {}", purgeRequestDTO);
        PurgeRequest purgeRequest = purgeRequestMapper.toEntity(purgeRequestDTO);
        purgeRequest = purgeRequestRepository.save(purgeRequest);
        return purgeRequestMapper.toDto(purgeRequest);
    }

    /**
     * Update a purgeRequest.
     *
     * @param purgeRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public PurgeRequestDTO update(PurgeRequestDTO purgeRequestDTO) {
        LOG.debug("Request to update PurgeRequest : {}", purgeRequestDTO);
        PurgeRequest purgeRequest = purgeRequestMapper.toEntity(purgeRequestDTO);
        purgeRequest = purgeRequestRepository.save(purgeRequest);
        return purgeRequestMapper.toDto(purgeRequest);
    }

    /**
     * Partially update a purgeRequest.
     *
     * @param purgeRequestDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PurgeRequestDTO> partialUpdate(PurgeRequestDTO purgeRequestDTO) {
        LOG.debug("Request to partially update PurgeRequest : {}", purgeRequestDTO);

        return purgeRequestRepository
            .findById(purgeRequestDTO.getId())
            .map(existingPurgeRequest -> {
                purgeRequestMapper.partialUpdate(existingPurgeRequest, purgeRequestDTO);

                return existingPurgeRequest;
            })
            .map(purgeRequestRepository::save)
            .map(purgeRequestMapper::toDto);
    }

    /**
     * Get all the purgeRequests with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<PurgeRequestDTO> findAllWithEagerRelationships(Pageable pageable) {
        return purgeRequestRepository.findAllWithEagerRelationships(pageable).map(purgeRequestMapper::toDto);
    }

    /**
     * Get one purgeRequest by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PurgeRequestDTO> findOne(UUID id) {
        LOG.debug("Request to get PurgeRequest : {}", id);
        return purgeRequestRepository.findOneWithEagerRelationships(id).map(purgeRequestMapper::toDto);
    }

    /**
     * Delete the purgeRequest by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete PurgeRequest : {}", id);
        purgeRequestRepository.deleteById(id);
    }
}
