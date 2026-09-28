package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.CampaignSync;
import cm.gimac.des.homologation.repository.CampaignSyncRepository;
import cm.gimac.des.homologation.service.dto.CampaignSyncDTO;
import cm.gimac.des.homologation.service.mapper.CampaignSyncMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.CampaignSync}.
 */
@Service
@Transactional
public class CampaignSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignSyncService.class);

    private final CampaignSyncRepository campaignSyncRepository;

    private final CampaignSyncMapper campaignSyncMapper;

    public CampaignSyncService(CampaignSyncRepository campaignSyncRepository, CampaignSyncMapper campaignSyncMapper) {
        this.campaignSyncRepository = campaignSyncRepository;
        this.campaignSyncMapper = campaignSyncMapper;
    }

    /**
     * Save a campaignSync.
     *
     * @param campaignSyncDTO the entity to save.
     * @return the persisted entity.
     */
    public CampaignSyncDTO save(CampaignSyncDTO campaignSyncDTO) {
        LOG.debug("Request to save CampaignSync : {}", campaignSyncDTO);
        CampaignSync campaignSync = campaignSyncMapper.toEntity(campaignSyncDTO);
        campaignSync = campaignSyncRepository.save(campaignSync);
        return campaignSyncMapper.toDto(campaignSync);
    }

    /**
     * Update a campaignSync.
     *
     * @param campaignSyncDTO the entity to save.
     * @return the persisted entity.
     */
    public CampaignSyncDTO update(CampaignSyncDTO campaignSyncDTO) {
        LOG.debug("Request to update CampaignSync : {}", campaignSyncDTO);
        CampaignSync campaignSync = campaignSyncMapper.toEntity(campaignSyncDTO);
        campaignSync = campaignSyncRepository.save(campaignSync);
        return campaignSyncMapper.toDto(campaignSync);
    }

    /**
     * Partially update a campaignSync.
     *
     * @param campaignSyncDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CampaignSyncDTO> partialUpdate(CampaignSyncDTO campaignSyncDTO) {
        LOG.debug("Request to partially update CampaignSync : {}", campaignSyncDTO);

        return campaignSyncRepository
            .findById(campaignSyncDTO.getId())
            .map(existingCampaignSync -> {
                campaignSyncMapper.partialUpdate(existingCampaignSync, campaignSyncDTO);

                return existingCampaignSync;
            })
            .map(campaignSyncRepository::save)
            .map(campaignSyncMapper::toDto);
    }

    /**
     * Get all the campaignSyncs.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CampaignSyncDTO> findAll() {
        LOG.debug("Request to get all CampaignSyncs");
        return campaignSyncRepository.findAll().stream().map(campaignSyncMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the campaignSyncs with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CampaignSyncDTO> findAllWithEagerRelationships(Pageable pageable) {
        return campaignSyncRepository.findAllWithEagerRelationships(pageable).map(campaignSyncMapper::toDto);
    }

    /**
     * Get one campaignSync by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CampaignSyncDTO> findOne(UUID id) {
        LOG.debug("Request to get CampaignSync : {}", id);
        return campaignSyncRepository.findOneWithEagerRelationships(id).map(campaignSyncMapper::toDto);
    }

    /**
     * Delete the campaignSync by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CampaignSync : {}", id);
        campaignSyncRepository.deleteById(id);
    }
}
