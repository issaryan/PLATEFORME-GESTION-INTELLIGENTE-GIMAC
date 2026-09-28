package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.AccessReview;
import cm.gimac.des.iam.repository.AccessReviewRepository;
import cm.gimac.des.iam.service.dto.AccessReviewDTO;
import cm.gimac.des.iam.service.mapper.AccessReviewMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.iam.domain.AccessReview}.
 */
@Service
@Transactional
public class AccessReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(AccessReviewService.class);

    private final AccessReviewRepository accessReviewRepository;

    private final AccessReviewMapper accessReviewMapper;

    public AccessReviewService(AccessReviewRepository accessReviewRepository, AccessReviewMapper accessReviewMapper) {
        this.accessReviewRepository = accessReviewRepository;
        this.accessReviewMapper = accessReviewMapper;
    }

    /**
     * Save a accessReview.
     *
     * @param accessReviewDTO the entity to save.
     * @return the persisted entity.
     */
    public AccessReviewDTO save(AccessReviewDTO accessReviewDTO) {
        LOG.debug("Request to save AccessReview : {}", accessReviewDTO);
        AccessReview accessReview = accessReviewMapper.toEntity(accessReviewDTO);
        accessReview = accessReviewRepository.save(accessReview);
        return accessReviewMapper.toDto(accessReview);
    }

    /**
     * Update a accessReview.
     *
     * @param accessReviewDTO the entity to save.
     * @return the persisted entity.
     */
    public AccessReviewDTO update(AccessReviewDTO accessReviewDTO) {
        LOG.debug("Request to update AccessReview : {}", accessReviewDTO);
        AccessReview accessReview = accessReviewMapper.toEntity(accessReviewDTO);
        accessReview = accessReviewRepository.save(accessReview);
        return accessReviewMapper.toDto(accessReview);
    }

    /**
     * Partially update a accessReview.
     *
     * @param accessReviewDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AccessReviewDTO> partialUpdate(AccessReviewDTO accessReviewDTO) {
        LOG.debug("Request to partially update AccessReview : {}", accessReviewDTO);

        return accessReviewRepository
            .findById(accessReviewDTO.getId())
            .map(existingAccessReview -> {
                accessReviewMapper.partialUpdate(existingAccessReview, accessReviewDTO);

                return existingAccessReview;
            })
            .map(accessReviewRepository::save)
            .map(accessReviewMapper::toDto);
    }

    /**
     * Get all the accessReviews.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<AccessReviewDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all AccessReviews");
        return accessReviewRepository.findAll(pageable).map(accessReviewMapper::toDto);
    }

    /**
     * Get one accessReview by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AccessReviewDTO> findOne(UUID id) {
        LOG.debug("Request to get AccessReview : {}", id);
        return accessReviewRepository.findById(id).map(accessReviewMapper::toDto);
    }

    /**
     * Delete the accessReview by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AccessReview : {}", id);
        accessReviewRepository.deleteById(id);
    }
}
