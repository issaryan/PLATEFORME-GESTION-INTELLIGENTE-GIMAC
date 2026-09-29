package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.Attestation;
import cm.gimac.des.certification.repository.AttestationRepository;
import cm.gimac.des.certification.service.AttestationService;
import cm.gimac.des.certification.service.dto.AttestationDTO;
import cm.gimac.des.certification.service.mapper.AttestationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.Attestation}.
 */
@Service
@Transactional
public class AttestationServiceImpl implements AttestationService {

    private static final Logger LOG = LoggerFactory.getLogger(AttestationServiceImpl.class);

    private final AttestationRepository attestationRepository;

    private final AttestationMapper attestationMapper;

    public AttestationServiceImpl(AttestationRepository attestationRepository, AttestationMapper attestationMapper) {
        this.attestationRepository = attestationRepository;
        this.attestationMapper = attestationMapper;
    }

    @Override
    public AttestationDTO save(AttestationDTO attestationDTO) {
        LOG.debug("Request to save Attestation : {}", attestationDTO);
        Attestation attestation = attestationMapper.toEntity(attestationDTO);
        attestation = attestationRepository.save(attestation);
        return attestationMapper.toDto(attestation);
    }

    @Override
    public AttestationDTO update(AttestationDTO attestationDTO) {
        LOG.debug("Request to update Attestation : {}", attestationDTO);
        Attestation attestation = attestationMapper.toEntity(attestationDTO);
        attestation = attestationRepository.save(attestation);
        return attestationMapper.toDto(attestation);
    }

    @Override
    public Optional<AttestationDTO> partialUpdate(AttestationDTO attestationDTO) {
        LOG.debug("Request to partially update Attestation : {}", attestationDTO);

        return attestationRepository
            .findById(attestationDTO.getId())
            .map(existingAttestation -> {
                attestationMapper.partialUpdate(existingAttestation, attestationDTO);

                return existingAttestation;
            })
            .map(attestationRepository::save)
            .map(attestationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttestationDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Attestations");
        return attestationRepository.findAll(pageable).map(attestationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttestationDTO> findOne(UUID id) {
        LOG.debug("Request to get Attestation : {}", id);
        return attestationRepository.findById(id).map(attestationMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete Attestation : {}", id);
        attestationRepository.deleteById(id);
    }
}
