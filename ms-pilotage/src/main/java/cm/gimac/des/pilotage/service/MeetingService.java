package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.Meeting;
import cm.gimac.des.pilotage.repository.MeetingRepository;
import cm.gimac.des.pilotage.service.dto.MeetingDTO;
import cm.gimac.des.pilotage.service.mapper.MeetingMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.Meeting}.
 */
@Service
@Transactional
public class MeetingService {

    private static final Logger LOG = LoggerFactory.getLogger(MeetingService.class);

    private final MeetingRepository meetingRepository;

    private final MeetingMapper meetingMapper;

    public MeetingService(MeetingRepository meetingRepository, MeetingMapper meetingMapper) {
        this.meetingRepository = meetingRepository;
        this.meetingMapper = meetingMapper;
    }

    /**
     * Save a meeting.
     *
     * @param meetingDTO the entity to save.
     * @return the persisted entity.
     */
    public MeetingDTO save(MeetingDTO meetingDTO) {
        LOG.debug("Request to save Meeting : {}", meetingDTO);
        Meeting meeting = meetingMapper.toEntity(meetingDTO);
        meeting = meetingRepository.save(meeting);
        return meetingMapper.toDto(meeting);
    }

    /**
     * Update a meeting.
     *
     * @param meetingDTO the entity to save.
     * @return the persisted entity.
     */
    public MeetingDTO update(MeetingDTO meetingDTO) {
        LOG.debug("Request to update Meeting : {}", meetingDTO);
        Meeting meeting = meetingMapper.toEntity(meetingDTO);
        meeting = meetingRepository.save(meeting);
        return meetingMapper.toDto(meeting);
    }

    /**
     * Partially update a meeting.
     *
     * @param meetingDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MeetingDTO> partialUpdate(MeetingDTO meetingDTO) {
        LOG.debug("Request to partially update Meeting : {}", meetingDTO);

        return meetingRepository
            .findById(meetingDTO.getId())
            .map(existingMeeting -> {
                meetingMapper.partialUpdate(existingMeeting, meetingDTO);

                return existingMeeting;
            })
            .map(meetingRepository::save)
            .map(meetingMapper::toDto);
    }

    /**
     * Get one meeting by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MeetingDTO> findOne(UUID id) {
        LOG.debug("Request to get Meeting : {}", id);
        return meetingRepository.findById(id).map(meetingMapper::toDto);
    }

    /**
     * Delete the meeting by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Meeting : {}", id);
        meetingRepository.deleteById(id);
    }
}
