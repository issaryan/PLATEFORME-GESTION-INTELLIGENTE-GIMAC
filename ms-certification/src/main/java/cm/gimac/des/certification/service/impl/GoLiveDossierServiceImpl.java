package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.repository.GoLiveDossierRepository;
import cm.gimac.des.certification.service.GoLiveDossierService;
import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
import cm.gimac.des.certification.service.mapper.GoLiveDossierMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.GoLiveDossier}.
 */
@Service
@Transactional
public class GoLiveDossierServiceImpl implements GoLiveDossierService {

    private static final Logger LOG = LoggerFactory.getLogger(GoLiveDossierServiceImpl.class);

    private final GoLiveDossierRepository goLiveDossierRepository;

    private final GoLiveDossierMapper goLiveDossierMapper;

    public GoLiveDossierServiceImpl(GoLiveDossierRepository goLiveDossierRepository, GoLiveDossierMapper goLiveDossierMapper) {
        this.goLiveDossierRepository = goLiveDossierRepository;
        this.goLiveDossierMapper = goLiveDossierMapper;
    }

    @Override
    public GoLiveDossierDTO save(GoLiveDossierDTO goLiveDossierDTO) {
        LOG.debug("Request to save GoLiveDossier : {}", goLiveDossierDTO);
        GoLiveDossier goLiveDossier = goLiveDossierMapper.toEntity(goLiveDossierDTO);
        goLiveDossier = goLiveDossierRepository.save(goLiveDossier);
        return goLiveDossierMapper.toDto(goLiveDossier);
    }

    @Override
    public GoLiveDossierDTO update(GoLiveDossierDTO goLiveDossierDTO) {
        LOG.debug("Request to update GoLiveDossier : {}", goLiveDossierDTO);
        GoLiveDossier goLiveDossier = goLiveDossierMapper.toEntity(goLiveDossierDTO);
        goLiveDossier = goLiveDossierRepository.save(goLiveDossier);
        return goLiveDossierMapper.toDto(goLiveDossier);
    }

    @Override
    public Optional<GoLiveDossierDTO> partialUpdate(GoLiveDossierDTO goLiveDossierDTO) {
        LOG.debug("Request to partially update GoLiveDossier : {}", goLiveDossierDTO);

        return goLiveDossierRepository
            .findById(goLiveDossierDTO.getId())
            .map(existingGoLiveDossier -> {
                goLiveDossierMapper.partialUpdate(existingGoLiveDossier, goLiveDossierDTO);

                return existingGoLiveDossier;
            })
            .map(goLiveDossierRepository::save)
            .map(goLiveDossierMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoLiveDossierDTO> findAll() {
        LOG.debug("Request to get all GoLiveDossiers");
        return goLiveDossierRepository.findAll().stream().map(goLiveDossierMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the goLiveDossiers where GoLiveReport is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GoLiveDossierDTO> findAllWhereGoLiveReportIsNull() {
        LOG.debug("Request to get all goLiveDossiers where GoLiveReport is null");
        return StreamSupport.stream(goLiveDossierRepository.findAll().spliterator(), false)
            .filter(goLiveDossier -> goLiveDossier.getGoLiveReport() == null)
            .map(goLiveDossierMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the goLiveDossiers where Attestation is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GoLiveDossierDTO> findAllWhereAttestationIsNull() {
        LOG.debug("Request to get all goLiveDossiers where Attestation is null");
        return StreamSupport.stream(goLiveDossierRepository.findAll().spliterator(), false)
            .filter(goLiveDossier -> goLiveDossier.getAttestation() == null)
            .map(goLiveDossierMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GoLiveDossierDTO> findOne(UUID id) {
        LOG.debug("Request to get GoLiveDossier : {}", id);
        return goLiveDossierRepository.findById(id).map(goLiveDossierMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete GoLiveDossier : {}", id);
        goLiveDossierRepository.deleteById(id);
    }
}
