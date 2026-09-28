package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.AgingNotice;
import cm.gimac.des.rapprochement.repository.AgingNoticeRepository;
import cm.gimac.des.rapprochement.service.dto.AgingNoticeDTO;
import cm.gimac.des.rapprochement.service.mapper.AgingNoticeMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.AgingNotice}.
 */
@Service
@Transactional
public class AgingNoticeService {

    private static final Logger LOG = LoggerFactory.getLogger(AgingNoticeService.class);

    private final AgingNoticeRepository agingNoticeRepository;

    private final AgingNoticeMapper agingNoticeMapper;

    public AgingNoticeService(AgingNoticeRepository agingNoticeRepository, AgingNoticeMapper agingNoticeMapper) {
        this.agingNoticeRepository = agingNoticeRepository;
        this.agingNoticeMapper = agingNoticeMapper;
    }

    /**
     * Save a agingNotice.
     *
     * @param agingNoticeDTO the entity to save.
     * @return the persisted entity.
     */
    public AgingNoticeDTO save(AgingNoticeDTO agingNoticeDTO) {
        LOG.debug("Request to save AgingNotice : {}", agingNoticeDTO);
        AgingNotice agingNotice = agingNoticeMapper.toEntity(agingNoticeDTO);
        agingNotice = agingNoticeRepository.save(agingNotice);
        return agingNoticeMapper.toDto(agingNotice);
    }

    /**
     * Update a agingNotice.
     *
     * @param agingNoticeDTO the entity to save.
     * @return the persisted entity.
     */
    public AgingNoticeDTO update(AgingNoticeDTO agingNoticeDTO) {
        LOG.debug("Request to update AgingNotice : {}", agingNoticeDTO);
        AgingNotice agingNotice = agingNoticeMapper.toEntity(agingNoticeDTO);
        agingNotice = agingNoticeRepository.save(agingNotice);
        return agingNoticeMapper.toDto(agingNotice);
    }

    /**
     * Partially update a agingNotice.
     *
     * @param agingNoticeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AgingNoticeDTO> partialUpdate(AgingNoticeDTO agingNoticeDTO) {
        LOG.debug("Request to partially update AgingNotice : {}", agingNoticeDTO);

        return agingNoticeRepository
            .findById(agingNoticeDTO.getId())
            .map(existingAgingNotice -> {
                agingNoticeMapper.partialUpdate(existingAgingNotice, agingNoticeDTO);

                return existingAgingNotice;
            })
            .map(agingNoticeRepository::save)
            .map(agingNoticeMapper::toDto);
    }

    /**
     * Get all the agingNotices.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<AgingNoticeDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all AgingNotices");
        return agingNoticeRepository.findAll(pageable).map(agingNoticeMapper::toDto);
    }

    /**
     * Get all the agingNotices with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<AgingNoticeDTO> findAllWithEagerRelationships(Pageable pageable) {
        return agingNoticeRepository.findAllWithEagerRelationships(pageable).map(agingNoticeMapper::toDto);
    }

    /**
     * Get one agingNotice by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AgingNoticeDTO> findOne(UUID id) {
        LOG.debug("Request to get AgingNotice : {}", id);
        return agingNoticeRepository.findOneWithEagerRelationships(id).map(agingNoticeMapper::toDto);
    }

    /**
     * Delete the agingNotice by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AgingNotice : {}", id);
        agingNoticeRepository.deleteById(id);
    }
}
