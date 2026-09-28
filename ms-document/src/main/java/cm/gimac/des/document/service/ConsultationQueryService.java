package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.*; // for static metamodels
import cm.gimac.des.document.domain.Consultation;
import cm.gimac.des.document.repository.ConsultationRepository;
import cm.gimac.des.document.service.criteria.ConsultationCriteria;
import cm.gimac.des.document.service.dto.ConsultationDTO;
import cm.gimac.des.document.service.mapper.ConsultationMapper;
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
 * Service for executing complex queries for {@link Consultation} entities in the database.
 * The main input is a {@link ConsultationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ConsultationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ConsultationQueryService extends QueryService<Consultation> {

    private static final Logger LOG = LoggerFactory.getLogger(ConsultationQueryService.class);

    private final ConsultationRepository consultationRepository;

    private final ConsultationMapper consultationMapper;

    public ConsultationQueryService(ConsultationRepository consultationRepository, ConsultationMapper consultationMapper) {
        this.consultationRepository = consultationRepository;
        this.consultationMapper = consultationMapper;
    }

    /**
     * Return a {@link Page} of {@link ConsultationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ConsultationDTO> findByCriteria(ConsultationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Consultation> specification = createSpecification(criteria);
        return consultationRepository.findAll(specification, page).map(consultationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ConsultationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Consultation> specification = createSpecification(criteria);
        return consultationRepository.count(specification);
    }

    /**
     * Function to convert {@link ConsultationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Consultation> createSpecification(ConsultationCriteria criteria) {
        Specification<Consultation> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Consultation_.id),
                buildSpecification(criteria.getUserId(), Consultation_.userId),
                buildSpecification(criteria.getInstitutionId(), Consultation_.institutionId),
                buildSpecification(criteria.getAction(), Consultation_.action),
                buildSpecification(criteria.getChannel(), Consultation_.channel),
                buildRangeSpecification(criteria.getConsultedAt(), Consultation_.consultedAt),
                buildSpecification(criteria.getCorrelationId(), Consultation_.correlationId),
                buildSpecification(criteria.getGranted(), Consultation_.granted),
                buildStringSpecification(criteria.getRefusalReason(), Consultation_.refusalReason),
                buildSpecification(criteria.getDocumentId(), root -> root.join(Consultation_.document, JoinType.LEFT).get(Document_.id))
            );
        }
        return specification;
    }
}
