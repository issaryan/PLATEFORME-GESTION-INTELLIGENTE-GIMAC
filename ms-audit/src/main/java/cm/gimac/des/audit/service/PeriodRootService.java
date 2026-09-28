package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.PeriodRoot;
import cm.gimac.des.audit.repository.PeriodRootRepository;
import cm.gimac.des.audit.service.dto.PeriodRootDTO;
import cm.gimac.des.audit.service.mapper.PeriodRootMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.audit.domain.PeriodRoot}.
 */
@Service
@Transactional
public class PeriodRootService {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodRootService.class);

    private final PeriodRootRepository periodRootRepository;

    private final PeriodRootMapper periodRootMapper;

    public PeriodRootService(PeriodRootRepository periodRootRepository, PeriodRootMapper periodRootMapper) {
        this.periodRootRepository = periodRootRepository;
        this.periodRootMapper = periodRootMapper;
    }

    /**
     * Save a periodRoot.
     *
     * @param periodRootDTO the entity to save.
     * @return the persisted entity.
     */
    public PeriodRootDTO save(PeriodRootDTO periodRootDTO) {
        LOG.debug("Request to save PeriodRoot : {}", periodRootDTO);
        PeriodRoot periodRoot = periodRootMapper.toEntity(periodRootDTO);
        periodRoot = periodRootRepository.save(periodRoot);
        return periodRootMapper.toDto(periodRoot);
    }

    /**
     * Update a periodRoot.
     *
     * @param periodRootDTO the entity to save.
     * @return the persisted entity.
     */
    public PeriodRootDTO update(PeriodRootDTO periodRootDTO) {
        LOG.debug("Request to update PeriodRoot : {}", periodRootDTO);
        PeriodRoot periodRoot = periodRootMapper.toEntity(periodRootDTO);
        periodRoot = periodRootRepository.save(periodRoot);
        return periodRootMapper.toDto(periodRoot);
    }

    /**
     * Partially update a periodRoot.
     *
     * @param periodRootDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PeriodRootDTO> partialUpdate(PeriodRootDTO periodRootDTO) {
        LOG.debug("Request to partially update PeriodRoot : {}", periodRootDTO);

        return periodRootRepository
            .findById(periodRootDTO.getId())
            .map(existingPeriodRoot -> {
                periodRootMapper.partialUpdate(existingPeriodRoot, periodRootDTO);

                return existingPeriodRoot;
            })
            .map(periodRootRepository::save)
            .map(periodRootMapper::toDto);
    }

    /**
     * Get all the periodRoots with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<PeriodRootDTO> findAllWithEagerRelationships(Pageable pageable) {
        return periodRootRepository.findAllWithEagerRelationships(pageable).map(periodRootMapper::toDto);
    }

    /**
     *  Get all the periodRoots where PeriodRoot is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<PeriodRootDTO> findAllWherePeriodRootIsNull() {
        LOG.debug("Request to get all periodRoots where PeriodRoot is null");
        return StreamSupport.stream(periodRootRepository.findAll().spliterator(), false)
            .filter(periodRoot -> periodRoot.getPeriodRoot() == null)
            .map(periodRootMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one periodRoot by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PeriodRootDTO> findOne(UUID id) {
        LOG.debug("Request to get PeriodRoot : {}", id);
        return periodRootRepository.findOneWithEagerRelationships(id).map(periodRootMapper::toDto);
    }

    /**
     * Delete the periodRoot by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete PeriodRoot : {}", id);
        periodRootRepository.deleteById(id);
    }
}
