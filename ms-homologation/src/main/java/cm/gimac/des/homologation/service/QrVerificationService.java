package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.QrVerification;
import cm.gimac.des.homologation.repository.QrVerificationRepository;
import cm.gimac.des.homologation.service.dto.QrVerificationDTO;
import cm.gimac.des.homologation.service.mapper.QrVerificationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.QrVerification}.
 */
@Service
@Transactional
public class QrVerificationService {

    private static final Logger LOG = LoggerFactory.getLogger(QrVerificationService.class);

    private final QrVerificationRepository qrVerificationRepository;

    private final QrVerificationMapper qrVerificationMapper;

    public QrVerificationService(QrVerificationRepository qrVerificationRepository, QrVerificationMapper qrVerificationMapper) {
        this.qrVerificationRepository = qrVerificationRepository;
        this.qrVerificationMapper = qrVerificationMapper;
    }

    /**
     * Save a qrVerification.
     *
     * @param qrVerificationDTO the entity to save.
     * @return the persisted entity.
     */
    public QrVerificationDTO save(QrVerificationDTO qrVerificationDTO) {
        LOG.debug("Request to save QrVerification : {}", qrVerificationDTO);
        QrVerification qrVerification = qrVerificationMapper.toEntity(qrVerificationDTO);
        qrVerification = qrVerificationRepository.save(qrVerification);
        return qrVerificationMapper.toDto(qrVerification);
    }

    /**
     * Update a qrVerification.
     *
     * @param qrVerificationDTO the entity to save.
     * @return the persisted entity.
     */
    public QrVerificationDTO update(QrVerificationDTO qrVerificationDTO) {
        LOG.debug("Request to update QrVerification : {}", qrVerificationDTO);
        QrVerification qrVerification = qrVerificationMapper.toEntity(qrVerificationDTO);
        qrVerification = qrVerificationRepository.save(qrVerification);
        return qrVerificationMapper.toDto(qrVerification);
    }

    /**
     * Partially update a qrVerification.
     *
     * @param qrVerificationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<QrVerificationDTO> partialUpdate(QrVerificationDTO qrVerificationDTO) {
        LOG.debug("Request to partially update QrVerification : {}", qrVerificationDTO);

        return qrVerificationRepository
            .findById(qrVerificationDTO.getId())
            .map(existingQrVerification -> {
                qrVerificationMapper.partialUpdate(existingQrVerification, qrVerificationDTO);

                return existingQrVerification;
            })
            .map(qrVerificationRepository::save)
            .map(qrVerificationMapper::toDto);
    }

    /**
     * Get all the qrVerifications with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<QrVerificationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return qrVerificationRepository.findAllWithEagerRelationships(pageable).map(qrVerificationMapper::toDto);
    }

    /**
     * Get one qrVerification by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<QrVerificationDTO> findOne(UUID id) {
        LOG.debug("Request to get QrVerification : {}", id);
        return qrVerificationRepository.findOneWithEagerRelationships(id).map(qrVerificationMapper::toDto);
    }

    /**
     * Delete the qrVerification by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete QrVerification : {}", id);
        qrVerificationRepository.deleteById(id);
    }
}
