package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.RoomArchive;
import cm.gimac.des.collaboration.repository.RoomArchiveRepository;
import cm.gimac.des.collaboration.service.dto.RoomArchiveDTO;
import cm.gimac.des.collaboration.service.mapper.RoomArchiveMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.RoomArchive}.
 */
@Service
@Transactional
public class RoomArchiveService {

    private static final Logger LOG = LoggerFactory.getLogger(RoomArchiveService.class);

    private final RoomArchiveRepository roomArchiveRepository;

    private final RoomArchiveMapper roomArchiveMapper;

    public RoomArchiveService(RoomArchiveRepository roomArchiveRepository, RoomArchiveMapper roomArchiveMapper) {
        this.roomArchiveRepository = roomArchiveRepository;
        this.roomArchiveMapper = roomArchiveMapper;
    }

    /**
     * Save a roomArchive.
     *
     * @param roomArchiveDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomArchiveDTO save(RoomArchiveDTO roomArchiveDTO) {
        LOG.debug("Request to save RoomArchive : {}", roomArchiveDTO);
        RoomArchive roomArchive = roomArchiveMapper.toEntity(roomArchiveDTO);
        roomArchive = roomArchiveRepository.save(roomArchive);
        return roomArchiveMapper.toDto(roomArchive);
    }

    /**
     * Update a roomArchive.
     *
     * @param roomArchiveDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomArchiveDTO update(RoomArchiveDTO roomArchiveDTO) {
        LOG.debug("Request to update RoomArchive : {}", roomArchiveDTO);
        RoomArchive roomArchive = roomArchiveMapper.toEntity(roomArchiveDTO);
        roomArchive = roomArchiveRepository.save(roomArchive);
        return roomArchiveMapper.toDto(roomArchive);
    }

    /**
     * Partially update a roomArchive.
     *
     * @param roomArchiveDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RoomArchiveDTO> partialUpdate(RoomArchiveDTO roomArchiveDTO) {
        LOG.debug("Request to partially update RoomArchive : {}", roomArchiveDTO);

        return roomArchiveRepository
            .findById(roomArchiveDTO.getId())
            .map(existingRoomArchive -> {
                roomArchiveMapper.partialUpdate(existingRoomArchive, roomArchiveDTO);

                return existingRoomArchive;
            })
            .map(roomArchiveRepository::save)
            .map(roomArchiveMapper::toDto);
    }

    /**
     * Get all the roomArchives with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RoomArchiveDTO> findAllWithEagerRelationships(Pageable pageable) {
        return roomArchiveRepository.findAllWithEagerRelationships(pageable).map(roomArchiveMapper::toDto);
    }

    /**
     * Get one roomArchive by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RoomArchiveDTO> findOne(UUID id) {
        LOG.debug("Request to get RoomArchive : {}", id);
        return roomArchiveRepository.findOneWithEagerRelationships(id).map(roomArchiveMapper::toDto);
    }

    /**
     * Delete the roomArchive by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RoomArchive : {}", id);
        roomArchiveRepository.deleteById(id);
    }
}
