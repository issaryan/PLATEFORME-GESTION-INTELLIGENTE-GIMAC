package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.repository.CorrespondentRepository;
import cm.gimac.des.participant.service.dto.CorrespondentDTO;
import cm.gimac.des.participant.service.mapper.CorrespondentMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.Correspondent}.
 */
@Service
@Transactional
public class CorrespondentService {

    private static final Logger LOG = LoggerFactory.getLogger(CorrespondentService.class);

    private final CorrespondentRepository correspondentRepository;

    private final CorrespondentMapper correspondentMapper;

    public CorrespondentService(CorrespondentRepository correspondentRepository, CorrespondentMapper correspondentMapper) {
        this.correspondentRepository = correspondentRepository;
        this.correspondentMapper = correspondentMapper;
    }

    /**
     * Save a correspondent.
     *
     * @param correspondentDTO the entity to save.
     * @return the persisted entity.
     */
    public CorrespondentDTO save(CorrespondentDTO correspondentDTO) {
        LOG.debug("Request to save Correspondent : {}", correspondentDTO);
        Correspondent correspondent = correspondentMapper.toEntity(correspondentDTO);
        correspondent = correspondentRepository.save(correspondent);
        return correspondentMapper.toDto(correspondent);
    }

    /**
     * Update a correspondent.
     *
     * @param correspondentDTO the entity to save.
     * @return the persisted entity.
     */
    public CorrespondentDTO update(CorrespondentDTO correspondentDTO) {
        LOG.debug("Request to update Correspondent : {}", correspondentDTO);
        Correspondent correspondent = correspondentMapper.toEntity(correspondentDTO);
        correspondent = correspondentRepository.save(correspondent);
        return correspondentMapper.toDto(correspondent);
    }

    /**
     * Partially update a correspondent.
     *
     * @param correspondentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CorrespondentDTO> partialUpdate(CorrespondentDTO correspondentDTO) {
        LOG.debug("Request to partially update Correspondent : {}", correspondentDTO);

        return correspondentRepository
            .findById(correspondentDTO.getId())
            .map(existingCorrespondent -> {
                correspondentMapper.partialUpdate(existingCorrespondent, correspondentDTO);

                return existingCorrespondent;
            })
            .map(correspondentRepository::save)
            .map(correspondentMapper::toDto);
    }

    /**
     * Get all the correspondents with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CorrespondentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return correspondentRepository.findAllWithEagerRelationships(pageable).map(correspondentMapper::toDto);
    }

    /**
     * Get one correspondent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CorrespondentDTO> findOne(UUID id) {
        LOG.debug("Request to get Correspondent : {}", id);
        return correspondentRepository.findOneWithEagerRelationships(id).map(correspondentMapper::toDto);
    }

    /**
     * Delete the correspondent by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Correspondent : {}", id);
        correspondentRepository.deleteById(id);
    }
}
