package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.Milestone;
import cm.gimac.des.pilotage.repository.MilestoneRepository;
import cm.gimac.des.pilotage.service.dto.MilestoneDTO;
import cm.gimac.des.pilotage.service.mapper.MilestoneMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.Milestone}.
 */
@Service
@Transactional
public class MilestoneService {

    private static final Logger LOG = LoggerFactory.getLogger(MilestoneService.class);

    private final MilestoneRepository milestoneRepository;

    private final MilestoneMapper milestoneMapper;

    public MilestoneService(MilestoneRepository milestoneRepository, MilestoneMapper milestoneMapper) {
        this.milestoneRepository = milestoneRepository;
        this.milestoneMapper = milestoneMapper;
    }

    /**
     * Save a milestone.
     *
     * @param milestoneDTO the entity to save.
     * @return the persisted entity.
     */
    public MilestoneDTO save(MilestoneDTO milestoneDTO) {
        LOG.debug("Request to save Milestone : {}", milestoneDTO);
        Milestone milestone = milestoneMapper.toEntity(milestoneDTO);
        milestone = milestoneRepository.save(milestone);
        return milestoneMapper.toDto(milestone);
    }

    /**
     * Update a milestone.
     *
     * @param milestoneDTO the entity to save.
     * @return the persisted entity.
     */
    public MilestoneDTO update(MilestoneDTO milestoneDTO) {
        LOG.debug("Request to update Milestone : {}", milestoneDTO);
        Milestone milestone = milestoneMapper.toEntity(milestoneDTO);
        milestone = milestoneRepository.save(milestone);
        return milestoneMapper.toDto(milestone);
    }

    /**
     * Partially update a milestone.
     *
     * @param milestoneDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MilestoneDTO> partialUpdate(MilestoneDTO milestoneDTO) {
        LOG.debug("Request to partially update Milestone : {}", milestoneDTO);

        return milestoneRepository
            .findById(milestoneDTO.getId())
            .map(existingMilestone -> {
                milestoneMapper.partialUpdate(existingMilestone, milestoneDTO);

                return existingMilestone;
            })
            .map(milestoneRepository::save)
            .map(milestoneMapper::toDto);
    }

    /**
     * Get all the milestones with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MilestoneDTO> findAllWithEagerRelationships(Pageable pageable) {
        return milestoneRepository.findAllWithEagerRelationships(pageable).map(milestoneMapper::toDto);
    }

    /**
     * Get one milestone by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MilestoneDTO> findOne(UUID id) {
        LOG.debug("Request to get Milestone : {}", id);
        return milestoneRepository.findOneWithEagerRelationships(id).map(milestoneMapper::toDto);
    }

    /**
     * Delete the milestone by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Milestone : {}", id);
        milestoneRepository.deleteById(id);
    }
}
