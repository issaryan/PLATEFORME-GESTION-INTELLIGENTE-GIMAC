package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.repository.CertificationCircuitRepository;
import cm.gimac.des.certification.service.CertificationCircuitService;
import cm.gimac.des.certification.service.dto.CertificationCircuitDTO;
import cm.gimac.des.certification.service.mapper.CertificationCircuitMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.CertificationCircuit}.
 */
@Service
@Transactional
public class CertificationCircuitServiceImpl implements CertificationCircuitService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificationCircuitServiceImpl.class);

    private final CertificationCircuitRepository certificationCircuitRepository;

    private final CertificationCircuitMapper certificationCircuitMapper;

    public CertificationCircuitServiceImpl(
        CertificationCircuitRepository certificationCircuitRepository,
        CertificationCircuitMapper certificationCircuitMapper
    ) {
        this.certificationCircuitRepository = certificationCircuitRepository;
        this.certificationCircuitMapper = certificationCircuitMapper;
    }

    @Override
    public CertificationCircuitDTO save(CertificationCircuitDTO certificationCircuitDTO) {
        LOG.debug("Request to save CertificationCircuit : {}", certificationCircuitDTO);
        CertificationCircuit certificationCircuit = certificationCircuitMapper.toEntity(certificationCircuitDTO);
        certificationCircuit = certificationCircuitRepository.save(certificationCircuit);
        return certificationCircuitMapper.toDto(certificationCircuit);
    }

    @Override
    public CertificationCircuitDTO update(CertificationCircuitDTO certificationCircuitDTO) {
        LOG.debug("Request to update CertificationCircuit : {}", certificationCircuitDTO);
        CertificationCircuit certificationCircuit = certificationCircuitMapper.toEntity(certificationCircuitDTO);
        certificationCircuit = certificationCircuitRepository.save(certificationCircuit);
        return certificationCircuitMapper.toDto(certificationCircuit);
    }

    @Override
    public Optional<CertificationCircuitDTO> partialUpdate(CertificationCircuitDTO certificationCircuitDTO) {
        LOG.debug("Request to partially update CertificationCircuit : {}", certificationCircuitDTO);

        return certificationCircuitRepository
            .findById(certificationCircuitDTO.getId())
            .map(existingCertificationCircuit -> {
                certificationCircuitMapper.partialUpdate(existingCertificationCircuit, certificationCircuitDTO);

                return existingCertificationCircuit;
            })
            .map(certificationCircuitRepository::save)
            .map(certificationCircuitMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificationCircuitDTO> findAll() {
        LOG.debug("Request to get all CertificationCircuits");
        return certificationCircuitRepository
            .findAll()
            .stream()
            .map(certificationCircuitMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the certificationCircuits where GoLiveDossier is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CertificationCircuitDTO> findAllWhereGoLiveDossierIsNull() {
        LOG.debug("Request to get all certificationCircuits where GoLiveDossier is null");
        return StreamSupport.stream(certificationCircuitRepository.findAll().spliterator(), false)
            .filter(certificationCircuit -> certificationCircuit.getGoLiveDossier() == null)
            .map(certificationCircuitMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CertificationCircuitDTO> findOne(UUID id) {
        LOG.debug("Request to get CertificationCircuit : {}", id);
        return certificationCircuitRepository.findById(id).map(certificationCircuitMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete CertificationCircuit : {}", id);
        certificationCircuitRepository.deleteById(id);
    }
}
