package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.MeetingAttendance;
import cm.gimac.des.pilotage.repository.MeetingAttendanceRepository;
import cm.gimac.des.pilotage.service.dto.MeetingAttendanceDTO;
import cm.gimac.des.pilotage.service.mapper.MeetingAttendanceMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.MeetingAttendance}.
 */
@Service
@Transactional
public class MeetingAttendanceService {

    private static final Logger LOG = LoggerFactory.getLogger(MeetingAttendanceService.class);

    private final MeetingAttendanceRepository meetingAttendanceRepository;

    private final MeetingAttendanceMapper meetingAttendanceMapper;

    public MeetingAttendanceService(
        MeetingAttendanceRepository meetingAttendanceRepository,
        MeetingAttendanceMapper meetingAttendanceMapper
    ) {
        this.meetingAttendanceRepository = meetingAttendanceRepository;
        this.meetingAttendanceMapper = meetingAttendanceMapper;
    }

    /**
     * Save a meetingAttendance.
     *
     * @param meetingAttendanceDTO the entity to save.
     * @return the persisted entity.
     */
    public MeetingAttendanceDTO save(MeetingAttendanceDTO meetingAttendanceDTO) {
        LOG.debug("Request to save MeetingAttendance : {}", meetingAttendanceDTO);
        MeetingAttendance meetingAttendance = meetingAttendanceMapper.toEntity(meetingAttendanceDTO);
        meetingAttendance = meetingAttendanceRepository.save(meetingAttendance);
        return meetingAttendanceMapper.toDto(meetingAttendance);
    }

    /**
     * Update a meetingAttendance.
     *
     * @param meetingAttendanceDTO the entity to save.
     * @return the persisted entity.
     */
    public MeetingAttendanceDTO update(MeetingAttendanceDTO meetingAttendanceDTO) {
        LOG.debug("Request to update MeetingAttendance : {}", meetingAttendanceDTO);
        MeetingAttendance meetingAttendance = meetingAttendanceMapper.toEntity(meetingAttendanceDTO);
        meetingAttendance = meetingAttendanceRepository.save(meetingAttendance);
        return meetingAttendanceMapper.toDto(meetingAttendance);
    }

    /**
     * Partially update a meetingAttendance.
     *
     * @param meetingAttendanceDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MeetingAttendanceDTO> partialUpdate(MeetingAttendanceDTO meetingAttendanceDTO) {
        LOG.debug("Request to partially update MeetingAttendance : {}", meetingAttendanceDTO);

        return meetingAttendanceRepository
            .findById(meetingAttendanceDTO.getId())
            .map(existingMeetingAttendance -> {
                meetingAttendanceMapper.partialUpdate(existingMeetingAttendance, meetingAttendanceDTO);

                return existingMeetingAttendance;
            })
            .map(meetingAttendanceRepository::save)
            .map(meetingAttendanceMapper::toDto);
    }

    /**
     * Get all the meetingAttendances.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<MeetingAttendanceDTO> findAll() {
        LOG.debug("Request to get all MeetingAttendances");
        return meetingAttendanceRepository
            .findAll()
            .stream()
            .map(meetingAttendanceMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the meetingAttendances with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MeetingAttendanceDTO> findAllWithEagerRelationships(Pageable pageable) {
        return meetingAttendanceRepository.findAllWithEagerRelationships(pageable).map(meetingAttendanceMapper::toDto);
    }

    /**
     * Get one meetingAttendance by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MeetingAttendanceDTO> findOne(UUID id) {
        LOG.debug("Request to get MeetingAttendance : {}", id);
        return meetingAttendanceRepository.findOneWithEagerRelationships(id).map(meetingAttendanceMapper::toDto);
    }

    /**
     * Delete the meetingAttendance by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete MeetingAttendance : {}", id);
        meetingAttendanceRepository.deleteById(id);
    }
}
