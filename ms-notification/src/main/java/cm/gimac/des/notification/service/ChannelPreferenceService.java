package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.ChannelPreference;
import cm.gimac.des.notification.repository.ChannelPreferenceRepository;
import cm.gimac.des.notification.service.dto.ChannelPreferenceDTO;
import cm.gimac.des.notification.service.mapper.ChannelPreferenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.notification.domain.ChannelPreference}.
 */
@Service
@Transactional
public class ChannelPreferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelPreferenceService.class);

    private final ChannelPreferenceRepository channelPreferenceRepository;

    private final ChannelPreferenceMapper channelPreferenceMapper;

    public ChannelPreferenceService(
        ChannelPreferenceRepository channelPreferenceRepository,
        ChannelPreferenceMapper channelPreferenceMapper
    ) {
        this.channelPreferenceRepository = channelPreferenceRepository;
        this.channelPreferenceMapper = channelPreferenceMapper;
    }

    /**
     * Save a channelPreference.
     *
     * @param channelPreferenceDTO the entity to save.
     * @return the persisted entity.
     */
    public ChannelPreferenceDTO save(ChannelPreferenceDTO channelPreferenceDTO) {
        LOG.debug("Request to save ChannelPreference : {}", channelPreferenceDTO);
        ChannelPreference channelPreference = channelPreferenceMapper.toEntity(channelPreferenceDTO);
        channelPreference = channelPreferenceRepository.save(channelPreference);
        return channelPreferenceMapper.toDto(channelPreference);
    }

    /**
     * Update a channelPreference.
     *
     * @param channelPreferenceDTO the entity to save.
     * @return the persisted entity.
     */
    public ChannelPreferenceDTO update(ChannelPreferenceDTO channelPreferenceDTO) {
        LOG.debug("Request to update ChannelPreference : {}", channelPreferenceDTO);
        ChannelPreference channelPreference = channelPreferenceMapper.toEntity(channelPreferenceDTO);
        channelPreference = channelPreferenceRepository.save(channelPreference);
        return channelPreferenceMapper.toDto(channelPreference);
    }

    /**
     * Partially update a channelPreference.
     *
     * @param channelPreferenceDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ChannelPreferenceDTO> partialUpdate(ChannelPreferenceDTO channelPreferenceDTO) {
        LOG.debug("Request to partially update ChannelPreference : {}", channelPreferenceDTO);

        return channelPreferenceRepository
            .findById(channelPreferenceDTO.getId())
            .map(existingChannelPreference -> {
                channelPreferenceMapper.partialUpdate(existingChannelPreference, channelPreferenceDTO);

                return existingChannelPreference;
            })
            .map(channelPreferenceRepository::save)
            .map(channelPreferenceMapper::toDto);
    }

    /**
     * Get one channelPreference by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ChannelPreferenceDTO> findOne(UUID id) {
        LOG.debug("Request to get ChannelPreference : {}", id);
        return channelPreferenceRepository.findById(id).map(channelPreferenceMapper::toDto);
    }

    /**
     * Delete the channelPreference by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ChannelPreference : {}", id);
        channelPreferenceRepository.deleteById(id);
    }
}
