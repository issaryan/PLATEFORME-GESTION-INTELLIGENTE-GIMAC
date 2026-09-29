package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.EvidenceReference;
import cm.gimac.des.homologation.repository.EvidenceReferenceRepository;
import cm.gimac.des.homologation.service.EvidenceReferenceService;
import cm.gimac.des.homologation.service.dto.EvidenceReferenceDTO;
import cm.gimac.des.homologation.service.mapper.EvidenceReferenceMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.EvidenceReference}.
 */
@Service
@Transactional
public class EvidenceReferenceServiceImpl implements EvidenceReferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceReferenceServiceImpl.class);

    private final EvidenceReferenceRepository evidenceReferenceRepository;

    private final EvidenceReferenceMapper evidenceReferenceMapper;

    public EvidenceReferenceServiceImpl(
        EvidenceReferenceRepository evidenceReferenceRepository,
        EvidenceReferenceMapper evidenceReferenceMapper
    ) {
        this.evidenceReferenceRepository = evidenceReferenceRepository;
        this.evidenceReferenceMapper = evidenceReferenceMapper;
    }

    @Override
    public EvidenceReferenceDTO save(EvidenceReferenceDTO evidenceReferenceDTO) {
        LOG.debug("Request to save EvidenceReference : {}", evidenceReferenceDTO);
        EvidenceReference evidenceReference = evidenceReferenceMapper.toEntity(evidenceReferenceDTO);
        evidenceReference = evidenceReferenceRepository.save(evidenceReference);
        return evidenceReferenceMapper.toDto(evidenceReference);
    }

    @Override
    public EvidenceReferenceDTO update(EvidenceReferenceDTO evidenceReferenceDTO) {
        LOG.debug("Request to update EvidenceReference : {}", evidenceReferenceDTO);
        EvidenceReference evidenceReference = evidenceReferenceMapper.toEntity(evidenceReferenceDTO);
        evidenceReference = evidenceReferenceRepository.save(evidenceReference);
        return evidenceReferenceMapper.toDto(evidenceReference);
    }

    @Override
    public Optional<EvidenceReferenceDTO> partialUpdate(EvidenceReferenceDTO evidenceReferenceDTO) {
        LOG.debug("Request to partially update EvidenceReference : {}", evidenceReferenceDTO);

        return evidenceReferenceRepository
            .findById(evidenceReferenceDTO.getId())
            .map(existingEvidenceReference -> {
                evidenceReferenceMapper.partialUpdate(existingEvidenceReference, evidenceReferenceDTO);

                return existingEvidenceReference;
            })
            .map(evidenceReferenceRepository::save)
            .map(evidenceReferenceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceReferenceDTO> findAll() {
        LOG.debug("Request to get all EvidenceReferences");
        return evidenceReferenceRepository
            .findAll()
            .stream()
            .map(evidenceReferenceMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvidenceReferenceDTO> findOne(UUID id) {
        LOG.debug("Request to get EvidenceReference : {}", id);
        return evidenceReferenceRepository.findById(id).map(evidenceReferenceMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete EvidenceReference : {}", id);
        evidenceReferenceRepository.deleteById(id);
    }
}
