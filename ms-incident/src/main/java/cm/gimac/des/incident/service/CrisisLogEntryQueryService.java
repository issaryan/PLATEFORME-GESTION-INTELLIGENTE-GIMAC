package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.*; // for static metamodels
import cm.gimac.des.incident.domain.CrisisLogEntry;
import cm.gimac.des.incident.repository.CrisisLogEntryRepository;
import cm.gimac.des.incident.service.criteria.CrisisLogEntryCriteria;
import cm.gimac.des.incident.service.dto.CrisisLogEntryDTO;
import cm.gimac.des.incident.service.mapper.CrisisLogEntryMapper;
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
 * Service for executing complex queries for {@link CrisisLogEntry} entities in the database.
 * The main input is a {@link CrisisLogEntryCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CrisisLogEntryDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CrisisLogEntryQueryService extends QueryService<CrisisLogEntry> {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisLogEntryQueryService.class);

    private final CrisisLogEntryRepository crisisLogEntryRepository;

    private final CrisisLogEntryMapper crisisLogEntryMapper;

    public CrisisLogEntryQueryService(CrisisLogEntryRepository crisisLogEntryRepository, CrisisLogEntryMapper crisisLogEntryMapper) {
        this.crisisLogEntryRepository = crisisLogEntryRepository;
        this.crisisLogEntryMapper = crisisLogEntryMapper;
    }

    /**
     * Return a {@link Page} of {@link CrisisLogEntryDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CrisisLogEntryDTO> findByCriteria(CrisisLogEntryCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<CrisisLogEntry> specification = createSpecification(criteria);
        return crisisLogEntryRepository.findAll(specification, page).map(crisisLogEntryMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CrisisLogEntryCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<CrisisLogEntry> specification = createSpecification(criteria);
        return crisisLogEntryRepository.count(specification);
    }

    /**
     * Function to convert {@link CrisisLogEntryCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<CrisisLogEntry> createSpecification(CrisisLogEntryCriteria criteria) {
        Specification<CrisisLogEntry> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), CrisisLogEntry_.id),
                buildRangeSpecification(criteria.getChainIndex(), CrisisLogEntry_.chainIndex),
                buildRangeSpecification(criteria.getOccurredAt(), CrisisLogEntry_.occurredAt),
                buildSpecification(criteria.getAuthorId(), CrisisLogEntry_.authorId),
                buildSpecification(criteria.getNature(), CrisisLogEntry_.nature),
                buildStringSpecification(criteria.getText(), CrisisLogEntry_.text),
                buildStringSpecification(criteria.getReference(), CrisisLogEntry_.reference),
                buildStringSpecification(criteria.getPreviousHash(), CrisisLogEntry_.previousHash),
                buildStringSpecification(criteria.getHash(), CrisisLogEntry_.hash),
                buildSpecification(criteria.getCellId(), root -> root.join(CrisisLogEntry_.cell, JoinType.LEFT).get(CrisisCell_.id))
            );
        }
        return specification;
    }
}
