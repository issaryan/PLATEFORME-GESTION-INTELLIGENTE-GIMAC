package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.*; // for static metamodels
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.service.criteria.LiaisonCriteria;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.mapper.LiaisonMapper;
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
 * Service for executing complex queries for {@link Liaison} entities in the database.
 * The main input is a {@link LiaisonCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link LiaisonDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class LiaisonQueryService extends QueryService<Liaison> {

    private static final Logger LOG = LoggerFactory.getLogger(LiaisonQueryService.class);

    private final LiaisonRepository liaisonRepository;

    private final LiaisonMapper liaisonMapper;

    public LiaisonQueryService(LiaisonRepository liaisonRepository, LiaisonMapper liaisonMapper) {
        this.liaisonRepository = liaisonRepository;
        this.liaisonMapper = liaisonMapper;
    }

    /**
     * Return a {@link Page} of {@link LiaisonDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<LiaisonDTO> findByCriteria(LiaisonCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Liaison> specification = createSpecification(criteria);
        return liaisonRepository.findAll(specification, page).map(liaisonMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(LiaisonCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Liaison> specification = createSpecification(criteria);
        return liaisonRepository.count(specification);
    }

    /**
     * Function to convert {@link LiaisonCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Liaison> createSpecification(LiaisonCriteria criteria) {
        Specification<Liaison> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Liaison_.id),
                buildStringSpecification(criteria.getLiaisonCode(), Liaison_.liaisonCode),
                buildSpecification(criteria.getInstitutionId(), Liaison_.institutionId),
                buildSpecification(criteria.getProjectType(), Liaison_.projectType),
                buildRangeSpecification(criteria.getOpenedOn(), Liaison_.openedOn),
                buildSpecification(criteria.getProjectLeadId(), Liaison_.projectLeadId),
                buildSpecification(criteria.getState(), Liaison_.state),
                buildStringSpecification(criteria.getCampaignRef(), Liaison_.campaignRef),
                buildRangeSpecification(criteria.getReadyForCertificationAt(), Liaison_.readyForCertificationAt),
                buildRangeSpecification(criteria.getCommissionedOn(), Liaison_.commissionedOn),
                buildStringSpecification(criteria.getStateReason(), Liaison_.stateReason),
                buildSpecification(criteria.getCatalogId(), root -> root.join(Liaison_.catalog, JoinType.LEFT).get(ReferenceCatalog_.id)),
                buildSpecification(criteria.getCampaignSyncId(), root ->
                    root.join(Liaison_.campaignSync, JoinType.LEFT).get(CampaignSync_.id)
                )
            );
        }
        return specification;
    }
}
