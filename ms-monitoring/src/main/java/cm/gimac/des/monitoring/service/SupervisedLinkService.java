package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.repository.SupervisedLinkRepository;
import cm.gimac.des.monitoring.service.dto.SupervisedLinkDTO;
import cm.gimac.des.monitoring.service.mapper.SupervisedLinkMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.SupervisedLink}.
 */
@Service
@Transactional
public class SupervisedLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisedLinkService.class);

    private final SupervisedLinkRepository supervisedLinkRepository;

    private final SupervisedLinkMapper supervisedLinkMapper;

    public SupervisedLinkService(SupervisedLinkRepository supervisedLinkRepository, SupervisedLinkMapper supervisedLinkMapper) {
        this.supervisedLinkRepository = supervisedLinkRepository;
        this.supervisedLinkMapper = supervisedLinkMapper;
    }

    /**
     * Save a supervisedLink.
     *
     * @param supervisedLinkDTO the entity to save.
     * @return the persisted entity.
     */
    public SupervisedLinkDTO save(SupervisedLinkDTO supervisedLinkDTO) {
        LOG.debug("Request to save SupervisedLink : {}", supervisedLinkDTO);
        SupervisedLink supervisedLink = supervisedLinkMapper.toEntity(supervisedLinkDTO);
        supervisedLink = supervisedLinkRepository.save(supervisedLink);
        return supervisedLinkMapper.toDto(supervisedLink);
    }

    /**
     * Update a supervisedLink.
     *
     * @param supervisedLinkDTO the entity to save.
     * @return the persisted entity.
     */
    public SupervisedLinkDTO update(SupervisedLinkDTO supervisedLinkDTO) {
        LOG.debug("Request to update SupervisedLink : {}", supervisedLinkDTO);
        SupervisedLink supervisedLink = supervisedLinkMapper.toEntity(supervisedLinkDTO);
        supervisedLink = supervisedLinkRepository.save(supervisedLink);
        return supervisedLinkMapper.toDto(supervisedLink);
    }

    /**
     * Partially update a supervisedLink.
     *
     * @param supervisedLinkDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<SupervisedLinkDTO> partialUpdate(SupervisedLinkDTO supervisedLinkDTO) {
        LOG.debug("Request to partially update SupervisedLink : {}", supervisedLinkDTO);

        return supervisedLinkRepository
            .findById(supervisedLinkDTO.getId())
            .map(existingSupervisedLink -> {
                supervisedLinkMapper.partialUpdate(existingSupervisedLink, supervisedLinkDTO);

                return existingSupervisedLink;
            })
            .map(supervisedLinkRepository::save)
            .map(supervisedLinkMapper::toDto);
    }

    /**
     * Get all the supervisedLinks with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<SupervisedLinkDTO> findAllWithEagerRelationships(Pageable pageable) {
        return supervisedLinkRepository.findAllWithEagerRelationships(pageable).map(supervisedLinkMapper::toDto);
    }

    /**
     * Get one supervisedLink by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<SupervisedLinkDTO> findOne(UUID id) {
        LOG.debug("Request to get SupervisedLink : {}", id);
        return supervisedLinkRepository.findOneWithEagerRelationships(id).map(supervisedLinkMapper::toDto);
    }

    /**
     * Delete the supervisedLink by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete SupervisedLink : {}", id);
        supervisedLinkRepository.deleteById(id);
    }
}
