package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.*; // for static metamodels
import cm.gimac.des.collaboration.domain.VerifiedTerminal;
import cm.gimac.des.collaboration.repository.VerifiedTerminalRepository;
import cm.gimac.des.collaboration.service.criteria.VerifiedTerminalCriteria;
import cm.gimac.des.collaboration.service.dto.VerifiedTerminalDTO;
import cm.gimac.des.collaboration.service.mapper.VerifiedTerminalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link VerifiedTerminal} entities in the database.
 * The main input is a {@link VerifiedTerminalCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link VerifiedTerminalDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class VerifiedTerminalQueryService extends QueryService<VerifiedTerminal> {

    private static final Logger LOG = LoggerFactory.getLogger(VerifiedTerminalQueryService.class);

    private final VerifiedTerminalRepository verifiedTerminalRepository;

    private final VerifiedTerminalMapper verifiedTerminalMapper;

    public VerifiedTerminalQueryService(
        VerifiedTerminalRepository verifiedTerminalRepository,
        VerifiedTerminalMapper verifiedTerminalMapper
    ) {
        this.verifiedTerminalRepository = verifiedTerminalRepository;
        this.verifiedTerminalMapper = verifiedTerminalMapper;
    }

    /**
     * Return a {@link Page} of {@link VerifiedTerminalDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<VerifiedTerminalDTO> findByCriteria(VerifiedTerminalCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<VerifiedTerminal> specification = createSpecification(criteria);
        return verifiedTerminalRepository.findAll(specification, page).map(verifiedTerminalMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(VerifiedTerminalCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<VerifiedTerminal> specification = createSpecification(criteria);
        return verifiedTerminalRepository.count(specification);
    }

    /**
     * Function to convert {@link VerifiedTerminalCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<VerifiedTerminal> createSpecification(VerifiedTerminalCriteria criteria) {
        Specification<VerifiedTerminal> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), VerifiedTerminal_.id),
                buildSpecification(criteria.getUserId(), VerifiedTerminal_.userId),
                buildStringSpecification(criteria.getTerminalId(), VerifiedTerminal_.terminalId),
                buildStringSpecification(criteria.getIdentityKey(), VerifiedTerminal_.identityKey),
                buildStringSpecification(criteria.getAgreementKey(), VerifiedTerminal_.agreementKey),
                buildStringSpecification(criteria.getSignedPreKey(), VerifiedTerminal_.signedPreKey),
                buildStringSpecification(criteria.getSignedPreKeySignature(), VerifiedTerminal_.signedPreKeySignature),
                buildRangeSpecification(criteria.getOneTimePreKeys(), VerifiedTerminal_.oneTimePreKeys),
                buildStringSpecification(criteria.getCrossSignature(), VerifiedTerminal_.crossSignature),
                buildSpecification(criteria.getState(), VerifiedTerminal_.state),
                buildSpecification(criteria.getArchiveTerminal(), VerifiedTerminal_.archiveTerminal),
                buildRangeSpecification(criteria.getRegisteredAt(), VerifiedTerminal_.registeredAt),
                buildRangeSpecification(criteria.getVerifiedAt(), VerifiedTerminal_.verifiedAt),
                buildRangeSpecification(criteria.getRevokedAt(), VerifiedTerminal_.revokedAt)
            );
        }
        return specification;
    }
}
