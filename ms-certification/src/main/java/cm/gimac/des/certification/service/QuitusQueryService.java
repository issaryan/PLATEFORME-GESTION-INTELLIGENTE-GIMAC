package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.*; // for static metamodels
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.service.criteria.QuitusCriteria;
import cm.gimac.des.certification.service.dto.QuitusDTO;
import cm.gimac.des.certification.service.mapper.QuitusMapper;
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
 * Service for executing complex queries for {@link Quitus} entities in the database.
 * The main input is a {@link QuitusCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link QuitusDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class QuitusQueryService extends QueryService<Quitus> {

    private static final Logger LOG = LoggerFactory.getLogger(QuitusQueryService.class);

    private final QuitusRepository quitusRepository;

    private final QuitusMapper quitusMapper;

    public QuitusQueryService(QuitusRepository quitusRepository, QuitusMapper quitusMapper) {
        this.quitusRepository = quitusRepository;
        this.quitusMapper = quitusMapper;
    }

    /**
     * Return a {@link Page} of {@link QuitusDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<QuitusDTO> findByCriteria(QuitusCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Quitus> specification = createSpecification(criteria);
        return quitusRepository.findAll(specification, page).map(quitusMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(QuitusCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Quitus> specification = createSpecification(criteria);
        return quitusRepository.count(specification);
    }

    /**
     * Function to convert {@link QuitusCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Quitus> createSpecification(QuitusCriteria criteria) {
        Specification<Quitus> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Quitus_.id),
                buildSpecification(criteria.getType(), Quitus_.type),
                buildSpecification(criteria.getStatus(), Quitus_.status),
                buildSpecification(criteria.getDecision(), Quitus_.decision),
                buildSpecification(criteria.getSignerId(), Quitus_.signerId),
                buildStringSpecification(criteria.getReason(), Quitus_.reason),
                buildStringSpecification(criteria.getBasisDigest(), Quitus_.basisDigest),
                buildStringSpecification(criteria.getDecisionDigest(), Quitus_.decisionDigest),
                buildStringSpecification(criteria.getCredentialId(), Quitus_.credentialId),
                buildRangeSpecification(criteria.getSignCount(), Quitus_.signCount),
                buildRangeSpecification(criteria.getSignedAt(), Quitus_.signedAt),
                buildRangeSpecification(criteria.getDueAt(), Quitus_.dueAt),
                buildRangeSpecification(criteria.getInvalidatedAt(), Quitus_.invalidatedAt),
                buildSpecification(criteria.getFileId(), root -> root.join(Quitus_.file, JoinType.LEFT).get(CommissioningFile_.id))
            );
        }
        return specification;
    }
}
