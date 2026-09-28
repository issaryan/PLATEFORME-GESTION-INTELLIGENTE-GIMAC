package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.mapper.LiaisonMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.Liaison}.
 */
@Service
@Transactional
public class LiaisonService {

    private static final Logger LOG = LoggerFactory.getLogger(LiaisonService.class);

    private final LiaisonRepository liaisonRepository;

    private final LiaisonMapper liaisonMapper;

    public LiaisonService(LiaisonRepository liaisonRepository, LiaisonMapper liaisonMapper) {
        this.liaisonRepository = liaisonRepository;
        this.liaisonMapper = liaisonMapper;
    }

    /**
     * Save a liaison.
     *
     * @param liaisonDTO the entity to save.
     * @return the persisted entity.
     */
    public LiaisonDTO save(LiaisonDTO liaisonDTO) {
        LOG.debug("Request to save Liaison : {}", liaisonDTO);
        Liaison liaison = liaisonMapper.toEntity(liaisonDTO);
        liaison = liaisonRepository.save(liaison);
        return liaisonMapper.toDto(liaison);
    }

    /**
     * Update a liaison.
     *
     * @param liaisonDTO the entity to save.
     * @return the persisted entity.
     */
    public LiaisonDTO update(LiaisonDTO liaisonDTO) {
        LOG.debug("Request to update Liaison : {}", liaisonDTO);
        Liaison liaison = liaisonMapper.toEntity(liaisonDTO);
        liaison = liaisonRepository.save(liaison);
        return liaisonMapper.toDto(liaison);
    }

    /**
     * Partially update a liaison.
     *
     * @param liaisonDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<LiaisonDTO> partialUpdate(LiaisonDTO liaisonDTO) {
        LOG.debug("Request to partially update Liaison : {}", liaisonDTO);

        return liaisonRepository
            .findById(liaisonDTO.getId())
            .map(existingLiaison -> {
                liaisonMapper.partialUpdate(existingLiaison, liaisonDTO);

                return existingLiaison;
            })
            .map(liaisonRepository::save)
            .map(liaisonMapper::toDto);
    }

    /**
     * Get all the liaisons with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<LiaisonDTO> findAllWithEagerRelationships(Pageable pageable) {
        return liaisonRepository.findAllWithEagerRelationships(pageable).map(liaisonMapper::toDto);
    }

    /**
     *  Get all the liaisons where CampaignSync is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<LiaisonDTO> findAllWhereCampaignSyncIsNull() {
        LOG.debug("Request to get all liaisons where CampaignSync is null");
        return StreamSupport.stream(liaisonRepository.findAll().spliterator(), false)
            .filter(liaison -> liaison.getCampaignSync() == null)
            .map(liaisonMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one liaison by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<LiaisonDTO> findOne(UUID id) {
        LOG.debug("Request to get Liaison : {}", id);
        return liaisonRepository.findOneWithEagerRelationships(id).map(liaisonMapper::toDto);
    }

    /**
     * Delete the liaison by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Liaison : {}", id);
        liaisonRepository.deleteById(id);
    }
}
