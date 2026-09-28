package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.RoomRetentionPolicy;
import cm.gimac.des.collaboration.repository.RoomRetentionPolicyRepository;
import cm.gimac.des.collaboration.service.dto.RoomRetentionPolicyDTO;
import cm.gimac.des.collaboration.service.mapper.RoomRetentionPolicyMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.RoomRetentionPolicy}.
 */
@Service
@Transactional
public class RoomRetentionPolicyService {

    private static final Logger LOG = LoggerFactory.getLogger(RoomRetentionPolicyService.class);

    private final RoomRetentionPolicyRepository roomRetentionPolicyRepository;

    private final RoomRetentionPolicyMapper roomRetentionPolicyMapper;

    public RoomRetentionPolicyService(
        RoomRetentionPolicyRepository roomRetentionPolicyRepository,
        RoomRetentionPolicyMapper roomRetentionPolicyMapper
    ) {
        this.roomRetentionPolicyRepository = roomRetentionPolicyRepository;
        this.roomRetentionPolicyMapper = roomRetentionPolicyMapper;
    }

    /**
     * Save a roomRetentionPolicy.
     *
     * @param roomRetentionPolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomRetentionPolicyDTO save(RoomRetentionPolicyDTO roomRetentionPolicyDTO) {
        LOG.debug("Request to save RoomRetentionPolicy : {}", roomRetentionPolicyDTO);
        RoomRetentionPolicy roomRetentionPolicy = roomRetentionPolicyMapper.toEntity(roomRetentionPolicyDTO);
        roomRetentionPolicy = roomRetentionPolicyRepository.save(roomRetentionPolicy);
        return roomRetentionPolicyMapper.toDto(roomRetentionPolicy);
    }

    /**
     * Update a roomRetentionPolicy.
     *
     * @param roomRetentionPolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomRetentionPolicyDTO update(RoomRetentionPolicyDTO roomRetentionPolicyDTO) {
        LOG.debug("Request to update RoomRetentionPolicy : {}", roomRetentionPolicyDTO);
        RoomRetentionPolicy roomRetentionPolicy = roomRetentionPolicyMapper.toEntity(roomRetentionPolicyDTO);
        roomRetentionPolicy = roomRetentionPolicyRepository.save(roomRetentionPolicy);
        return roomRetentionPolicyMapper.toDto(roomRetentionPolicy);
    }

    /**
     * Partially update a roomRetentionPolicy.
     *
     * @param roomRetentionPolicyDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RoomRetentionPolicyDTO> partialUpdate(RoomRetentionPolicyDTO roomRetentionPolicyDTO) {
        LOG.debug("Request to partially update RoomRetentionPolicy : {}", roomRetentionPolicyDTO);

        return roomRetentionPolicyRepository
            .findById(roomRetentionPolicyDTO.getId())
            .map(existingRoomRetentionPolicy -> {
                roomRetentionPolicyMapper.partialUpdate(existingRoomRetentionPolicy, roomRetentionPolicyDTO);

                return existingRoomRetentionPolicy;
            })
            .map(roomRetentionPolicyRepository::save)
            .map(roomRetentionPolicyMapper::toDto);
    }

    /**
     * Get all the roomRetentionPolicies.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RoomRetentionPolicyDTO> findAll() {
        LOG.debug("Request to get all RoomRetentionPolicies");
        return roomRetentionPolicyRepository
            .findAll()
            .stream()
            .map(roomRetentionPolicyMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one roomRetentionPolicy by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RoomRetentionPolicyDTO> findOne(UUID id) {
        LOG.debug("Request to get RoomRetentionPolicy : {}", id);
        return roomRetentionPolicyRepository.findById(id).map(roomRetentionPolicyMapper::toDto);
    }

    /**
     * Delete the roomRetentionPolicy by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RoomRetentionPolicy : {}", id);
        roomRetentionPolicyRepository.deleteById(id);
    }
}
