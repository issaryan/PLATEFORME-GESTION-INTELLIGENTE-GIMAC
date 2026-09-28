package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.MilestoneApproval;
import cm.gimac.des.pilotage.repository.MilestoneApprovalRepository;
import cm.gimac.des.pilotage.service.dto.MilestoneApprovalDTO;
import cm.gimac.des.pilotage.service.mapper.MilestoneApprovalMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.MilestoneApproval}.
 */
@Service
@Transactional
public class MilestoneApprovalService {

    private static final Logger LOG = LoggerFactory.getLogger(MilestoneApprovalService.class);

    private final MilestoneApprovalRepository milestoneApprovalRepository;

    private final MilestoneApprovalMapper milestoneApprovalMapper;

    public MilestoneApprovalService(
        MilestoneApprovalRepository milestoneApprovalRepository,
        MilestoneApprovalMapper milestoneApprovalMapper
    ) {
        this.milestoneApprovalRepository = milestoneApprovalRepository;
        this.milestoneApprovalMapper = milestoneApprovalMapper;
    }

    /**
     * Save a milestoneApproval.
     *
     * @param milestoneApprovalDTO the entity to save.
     * @return the persisted entity.
     */
    public MilestoneApprovalDTO save(MilestoneApprovalDTO milestoneApprovalDTO) {
        LOG.debug("Request to save MilestoneApproval : {}", milestoneApprovalDTO);
        MilestoneApproval milestoneApproval = milestoneApprovalMapper.toEntity(milestoneApprovalDTO);
        milestoneApproval = milestoneApprovalRepository.save(milestoneApproval);
        return milestoneApprovalMapper.toDto(milestoneApproval);
    }

    /**
     * Update a milestoneApproval.
     *
     * @param milestoneApprovalDTO the entity to save.
     * @return the persisted entity.
     */
    public MilestoneApprovalDTO update(MilestoneApprovalDTO milestoneApprovalDTO) {
        LOG.debug("Request to update MilestoneApproval : {}", milestoneApprovalDTO);
        MilestoneApproval milestoneApproval = milestoneApprovalMapper.toEntity(milestoneApprovalDTO);
        milestoneApproval = milestoneApprovalRepository.save(milestoneApproval);
        return milestoneApprovalMapper.toDto(milestoneApproval);
    }

    /**
     * Partially update a milestoneApproval.
     *
     * @param milestoneApprovalDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MilestoneApprovalDTO> partialUpdate(MilestoneApprovalDTO milestoneApprovalDTO) {
        LOG.debug("Request to partially update MilestoneApproval : {}", milestoneApprovalDTO);

        return milestoneApprovalRepository
            .findById(milestoneApprovalDTO.getId())
            .map(existingMilestoneApproval -> {
                milestoneApprovalMapper.partialUpdate(existingMilestoneApproval, milestoneApprovalDTO);

                return existingMilestoneApproval;
            })
            .map(milestoneApprovalRepository::save)
            .map(milestoneApprovalMapper::toDto);
    }

    /**
     * Get all the milestoneApprovals.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<MilestoneApprovalDTO> findAll() {
        LOG.debug("Request to get all MilestoneApprovals");
        return milestoneApprovalRepository
            .findAll()
            .stream()
            .map(milestoneApprovalMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the milestoneApprovals with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MilestoneApprovalDTO> findAllWithEagerRelationships(Pageable pageable) {
        return milestoneApprovalRepository.findAllWithEagerRelationships(pageable).map(milestoneApprovalMapper::toDto);
    }

    /**
     * Get one milestoneApproval by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MilestoneApprovalDTO> findOne(UUID id) {
        LOG.debug("Request to get MilestoneApproval : {}", id);
        return milestoneApprovalRepository.findOneWithEagerRelationships(id).map(milestoneApprovalMapper::toDto);
    }

    /**
     * Delete the milestoneApproval by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete MilestoneApproval : {}", id);
        milestoneApprovalRepository.deleteById(id);
    }
}
