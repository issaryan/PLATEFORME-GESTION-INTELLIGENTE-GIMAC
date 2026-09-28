package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.*; // for static metamodels
import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import cm.gimac.des.monitoring.repository.ShiftLogEntryRepository;
import cm.gimac.des.monitoring.service.criteria.ShiftLogEntryCriteria;
import cm.gimac.des.monitoring.service.dto.ShiftLogEntryDTO;
import cm.gimac.des.monitoring.service.mapper.ShiftLogEntryMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link ShiftLogEntry} entities in the database.
 * The main input is a {@link ShiftLogEntryCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ShiftLogEntryDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ShiftLogEntryQueryService extends QueryService<ShiftLogEntry> {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftLogEntryQueryService.class);

    private final ShiftLogEntryRepository shiftLogEntryRepository;

    private final ShiftLogEntryMapper shiftLogEntryMapper;

    public ShiftLogEntryQueryService(ShiftLogEntryRepository shiftLogEntryRepository, ShiftLogEntryMapper shiftLogEntryMapper) {
        this.shiftLogEntryRepository = shiftLogEntryRepository;
        this.shiftLogEntryMapper = shiftLogEntryMapper;
    }

    /**
     * Return a {@link Page} of {@link ShiftLogEntryDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ShiftLogEntryDTO> findByCriteria(ShiftLogEntryCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ShiftLogEntry> specification = createSpecification(criteria);
        return shiftLogEntryRepository.findAll(specification, page).map(shiftLogEntryMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ShiftLogEntryCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ShiftLogEntry> specification = createSpecification(criteria);
        return shiftLogEntryRepository.count(specification);
    }

    /**
     * Function to convert {@link ShiftLogEntryCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ShiftLogEntry> createSpecification(ShiftLogEntryCriteria criteria) {
        Specification<ShiftLogEntry> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ShiftLogEntry_.id),
                buildRangeSpecification(criteria.getChainIndex(), ShiftLogEntry_.chainIndex),
                buildRangeSpecification(criteria.getOccurredAt(), ShiftLogEntry_.occurredAt),
                buildSpecification(criteria.getAuthorId(), ShiftLogEntry_.authorId),
                buildSpecification(criteria.getNature(), ShiftLogEntry_.nature),
                buildStringSpecification(criteria.getText(), ShiftLogEntry_.text),
                buildStringSpecification(criteria.getReference(), ShiftLogEntry_.reference),
                buildStringSpecification(criteria.getPreviousHash(), ShiftLogEntry_.previousHash),
                buildStringSpecification(criteria.getHash(), ShiftLogEntry_.hash),
                buildSpecification(criteria.getShiftId(), root -> root.join(ShiftLogEntry_.shift, JoinType.LEFT).get(Shift_.id)),
                buildSpecification(criteria.getCorrectsId(), root ->
                    root.join(ShiftLogEntry_.corrects, JoinType.LEFT).get(ShiftLogEntry_.id)
                )
            );
        }
        return specification;
    }
}
