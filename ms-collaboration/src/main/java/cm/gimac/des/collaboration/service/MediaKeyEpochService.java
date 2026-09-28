package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.MediaKeyEpoch;
import cm.gimac.des.collaboration.repository.MediaKeyEpochRepository;
import cm.gimac.des.collaboration.service.dto.MediaKeyEpochDTO;
import cm.gimac.des.collaboration.service.mapper.MediaKeyEpochMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.MediaKeyEpoch}.
 */
@Service
@Transactional
public class MediaKeyEpochService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaKeyEpochService.class);

    private final MediaKeyEpochRepository mediaKeyEpochRepository;

    private final MediaKeyEpochMapper mediaKeyEpochMapper;

    public MediaKeyEpochService(MediaKeyEpochRepository mediaKeyEpochRepository, MediaKeyEpochMapper mediaKeyEpochMapper) {
        this.mediaKeyEpochRepository = mediaKeyEpochRepository;
        this.mediaKeyEpochMapper = mediaKeyEpochMapper;
    }

    /**
     * Save a mediaKeyEpoch.
     *
     * @param mediaKeyEpochDTO the entity to save.
     * @return the persisted entity.
     */
    public MediaKeyEpochDTO save(MediaKeyEpochDTO mediaKeyEpochDTO) {
        LOG.debug("Request to save MediaKeyEpoch : {}", mediaKeyEpochDTO);
        MediaKeyEpoch mediaKeyEpoch = mediaKeyEpochMapper.toEntity(mediaKeyEpochDTO);
        mediaKeyEpoch = mediaKeyEpochRepository.save(mediaKeyEpoch);
        return mediaKeyEpochMapper.toDto(mediaKeyEpoch);
    }

    /**
     * Update a mediaKeyEpoch.
     *
     * @param mediaKeyEpochDTO the entity to save.
     * @return the persisted entity.
     */
    public MediaKeyEpochDTO update(MediaKeyEpochDTO mediaKeyEpochDTO) {
        LOG.debug("Request to update MediaKeyEpoch : {}", mediaKeyEpochDTO);
        MediaKeyEpoch mediaKeyEpoch = mediaKeyEpochMapper.toEntity(mediaKeyEpochDTO);
        mediaKeyEpoch = mediaKeyEpochRepository.save(mediaKeyEpoch);
        return mediaKeyEpochMapper.toDto(mediaKeyEpoch);
    }

    /**
     * Partially update a mediaKeyEpoch.
     *
     * @param mediaKeyEpochDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MediaKeyEpochDTO> partialUpdate(MediaKeyEpochDTO mediaKeyEpochDTO) {
        LOG.debug("Request to partially update MediaKeyEpoch : {}", mediaKeyEpochDTO);

        return mediaKeyEpochRepository
            .findById(mediaKeyEpochDTO.getId())
            .map(existingMediaKeyEpoch -> {
                mediaKeyEpochMapper.partialUpdate(existingMediaKeyEpoch, mediaKeyEpochDTO);

                return existingMediaKeyEpoch;
            })
            .map(mediaKeyEpochRepository::save)
            .map(mediaKeyEpochMapper::toDto);
    }

    /**
     * Get all the mediaKeyEpoches.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<MediaKeyEpochDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all MediaKeyEpoches");
        return mediaKeyEpochRepository.findAll(pageable).map(mediaKeyEpochMapper::toDto);
    }

    /**
     * Get all the mediaKeyEpoches with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MediaKeyEpochDTO> findAllWithEagerRelationships(Pageable pageable) {
        return mediaKeyEpochRepository.findAllWithEagerRelationships(pageable).map(mediaKeyEpochMapper::toDto);
    }

    /**
     * Get one mediaKeyEpoch by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MediaKeyEpochDTO> findOne(UUID id) {
        LOG.debug("Request to get MediaKeyEpoch : {}", id);
        return mediaKeyEpochRepository.findOneWithEagerRelationships(id).map(mediaKeyEpochMapper::toDto);
    }

    /**
     * Delete the mediaKeyEpoch by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete MediaKeyEpoch : {}", id);
        mediaKeyEpochRepository.deleteById(id);
    }
}
