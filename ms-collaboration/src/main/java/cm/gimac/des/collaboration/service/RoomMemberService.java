package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.RoomMember;
import cm.gimac.des.collaboration.repository.RoomMemberRepository;
import cm.gimac.des.collaboration.service.dto.RoomMemberDTO;
import cm.gimac.des.collaboration.service.mapper.RoomMemberMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.RoomMember}.
 */
@Service
@Transactional
public class RoomMemberService {

    private static final Logger LOG = LoggerFactory.getLogger(RoomMemberService.class);

    private final RoomMemberRepository roomMemberRepository;

    private final RoomMemberMapper roomMemberMapper;

    public RoomMemberService(RoomMemberRepository roomMemberRepository, RoomMemberMapper roomMemberMapper) {
        this.roomMemberRepository = roomMemberRepository;
        this.roomMemberMapper = roomMemberMapper;
    }

    /**
     * Save a roomMember.
     *
     * @param roomMemberDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomMemberDTO save(RoomMemberDTO roomMemberDTO) {
        LOG.debug("Request to save RoomMember : {}", roomMemberDTO);
        RoomMember roomMember = roomMemberMapper.toEntity(roomMemberDTO);
        roomMember = roomMemberRepository.save(roomMember);
        return roomMemberMapper.toDto(roomMember);
    }

    /**
     * Update a roomMember.
     *
     * @param roomMemberDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomMemberDTO update(RoomMemberDTO roomMemberDTO) {
        LOG.debug("Request to update RoomMember : {}", roomMemberDTO);
        RoomMember roomMember = roomMemberMapper.toEntity(roomMemberDTO);
        roomMember = roomMemberRepository.save(roomMember);
        return roomMemberMapper.toDto(roomMember);
    }

    /**
     * Partially update a roomMember.
     *
     * @param roomMemberDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RoomMemberDTO> partialUpdate(RoomMemberDTO roomMemberDTO) {
        LOG.debug("Request to partially update RoomMember : {}", roomMemberDTO);

        return roomMemberRepository
            .findById(roomMemberDTO.getId())
            .map(existingRoomMember -> {
                roomMemberMapper.partialUpdate(existingRoomMember, roomMemberDTO);

                return existingRoomMember;
            })
            .map(roomMemberRepository::save)
            .map(roomMemberMapper::toDto);
    }

    /**
     * Get all the roomMembers with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RoomMemberDTO> findAllWithEagerRelationships(Pageable pageable) {
        return roomMemberRepository.findAllWithEagerRelationships(pageable).map(roomMemberMapper::toDto);
    }

    /**
     * Get one roomMember by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RoomMemberDTO> findOne(UUID id) {
        LOG.debug("Request to get RoomMember : {}", id);
        return roomMemberRepository.findOneWithEagerRelationships(id).map(roomMemberMapper::toDto);
    }

    /**
     * Delete the roomMember by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RoomMember : {}", id);
        roomMemberRepository.deleteById(id);
    }
}
