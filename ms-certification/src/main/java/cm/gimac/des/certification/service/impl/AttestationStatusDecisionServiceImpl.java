package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.AttestationStatusDecision;
import cm.gimac.des.certification.repository.AttestationStatusDecisionRepository;
import cm.gimac.des.certification.service.AttestationStatusDecisionService;
import cm.gimac.des.certification.service.dto.AttestationStatusDecisionDTO;
import cm.gimac.des.certification.service.mapper.AttestationStatusDecisionMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.AttestationStatusDecision}.
 */
@Service
@Transactional
public class AttestationStatusDecisionServiceImpl implements AttestationStatusDecisionService {

    private static final Logger LOG = LoggerFactory.getLogger(AttestationStatusDecisionServiceImpl.class);

    private final AttestationStatusDecisionRepository attestationStatusDecisionRepository;

    private final AttestationStatusDecisionMapper attestationStatusDecisionMapper;

    public AttestationStatusDecisionServiceImpl(
        AttestationStatusDecisionRepository attestationStatusDecisionRepository,
        AttestationStatusDecisionMapper attestationStatusDecisionMapper
    ) {
        this.attestationStatusDecisionRepository = attestationStatusDecisionRepository;
        this.attestationStatusDecisionMapper = attestationStatusDecisionMapper;
    }

    @Override
    public AttestationStatusDecisionDTO save(AttestationStatusDecisionDTO attestationStatusDecisionDTO) {
        LOG.debug("Request to save AttestationStatusDecision : {}", attestationStatusDecisionDTO);
        AttestationStatusDecision attestationStatusDecision = attestationStatusDecisionMapper.toEntity(attestationStatusDecisionDTO);
        attestationStatusDecision = attestationStatusDecisionRepository.save(attestationStatusDecision);
        return attestationStatusDecisionMapper.toDto(attestationStatusDecision);
    }

    @Override
    public AttestationStatusDecisionDTO update(AttestationStatusDecisionDTO attestationStatusDecisionDTO) {
        LOG.debug("Request to update AttestationStatusDecision : {}", attestationStatusDecisionDTO);
        AttestationStatusDecision attestationStatusDecision = attestationStatusDecisionMapper.toEntity(attestationStatusDecisionDTO);
        attestationStatusDecision = attestationStatusDecisionRepository.save(attestationStatusDecision);
        return attestationStatusDecisionMapper.toDto(attestationStatusDecision);
    }

    @Override
    public Optional<AttestationStatusDecisionDTO> partialUpdate(AttestationStatusDecisionDTO attestationStatusDecisionDTO) {
        LOG.debug("Request to partially update AttestationStatusDecision : {}", attestationStatusDecisionDTO);

        return attestationStatusDecisionRepository
            .findById(attestationStatusDecisionDTO.getId())
            .map(existingAttestationStatusDecision -> {
                attestationStatusDecisionMapper.partialUpdate(existingAttestationStatusDecision, attestationStatusDecisionDTO);

                return existingAttestationStatusDecision;
            })
            .map(attestationStatusDecisionRepository::save)
            .map(attestationStatusDecisionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttestationStatusDecisionDTO> findAll() {
        LOG.debug("Request to get all AttestationStatusDecisions");
        return attestationStatusDecisionRepository
            .findAll()
            .stream()
            .map(attestationStatusDecisionMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttestationStatusDecisionDTO> findOne(UUID id) {
        LOG.debug("Request to get AttestationStatusDecision : {}", id);
        return attestationStatusDecisionRepository.findById(id).map(attestationStatusDecisionMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete AttestationStatusDecision : {}", id);
        attestationStatusDecisionRepository.deleteById(id);
    }
}
