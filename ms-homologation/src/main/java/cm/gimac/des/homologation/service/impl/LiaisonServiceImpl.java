package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.service.LiaisonService;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.mapper.LiaisonMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.Liaison}.
 */
@Service
@Transactional
public class LiaisonServiceImpl implements LiaisonService {

    private static final Logger LOG = LoggerFactory.getLogger(LiaisonServiceImpl.class);

    private final LiaisonRepository liaisonRepository;

    private final LiaisonMapper liaisonMapper;

    public LiaisonServiceImpl(LiaisonRepository liaisonRepository, LiaisonMapper liaisonMapper) {
        this.liaisonRepository = liaisonRepository;
        this.liaisonMapper = liaisonMapper;
    }

    @Override
    public LiaisonDTO save(LiaisonDTO liaisonDTO) {
        LOG.debug("Request to save Liaison : {}", liaisonDTO);
        Liaison liaison = liaisonMapper.toEntity(liaisonDTO);
        liaison = liaisonRepository.save(liaison);
        return liaisonMapper.toDto(liaison);
    }

    @Override
    public LiaisonDTO update(LiaisonDTO liaisonDTO) {
        LOG.debug("Request to update Liaison : {}", liaisonDTO);
        Liaison liaison = liaisonMapper.toEntity(liaisonDTO);
        liaison = liaisonRepository.save(liaison);
        return liaisonMapper.toDto(liaison);
    }

    @Override
    public Optional<LiaisonDTO> partialUpdate(LiaisonDTO liaisonDTO) {
        LOG.debug("Request to partially update Liaison : {}", liaisonDTO);

        return liaisonRepository
            .findById(liaisonDTO.getId())
            .map(existingLiaison -> {
                liaisonMapper.partialUpdate(existingLiaison, liaisonDTO);

                return existingLiaison;
            })
            .map(liaisonRepository::save)
            .map(liaisonMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LiaisonDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Liaisons");
        return liaisonRepository.findAll(pageable).map(liaisonMapper::toDto);
    }

    /**
     *  Get all the liaisons where ContractualScope is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<LiaisonDTO> findAllWhereContractualScopeIsNull() {
        LOG.debug("Request to get all liaisons where ContractualScope is null");
        return StreamSupport.stream(liaisonRepository.findAll().spliterator(), false)
            .filter(liaison -> liaison.getContractualScope() == null)
            .map(liaisonMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LiaisonDTO> findOne(UUID id) {
        LOG.debug("Request to get Liaison : {}", id);
        return liaisonRepository.findById(id).map(liaisonMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete Liaison : {}", id);
        liaisonRepository.deleteById(id);
    }
}
