package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import cm.gimac.des.homologation.service.ScopeItemService;
import cm.gimac.des.homologation.service.dto.ScopeItemDTO;
import cm.gimac.des.homologation.service.mapper.ScopeItemMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ScopeItem}.
 */
@Service
@Transactional
public class ScopeItemServiceImpl implements ScopeItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ScopeItemServiceImpl.class);

    private final ScopeItemRepository scopeItemRepository;

    private final ScopeItemMapper scopeItemMapper;

    public ScopeItemServiceImpl(ScopeItemRepository scopeItemRepository, ScopeItemMapper scopeItemMapper) {
        this.scopeItemRepository = scopeItemRepository;
        this.scopeItemMapper = scopeItemMapper;
    }

    @Override
    public ScopeItemDTO save(ScopeItemDTO scopeItemDTO) {
        LOG.debug("Request to save ScopeItem : {}", scopeItemDTO);
        ScopeItem scopeItem = scopeItemMapper.toEntity(scopeItemDTO);
        scopeItem = scopeItemRepository.save(scopeItem);
        return scopeItemMapper.toDto(scopeItem);
    }

    @Override
    public ScopeItemDTO update(ScopeItemDTO scopeItemDTO) {
        LOG.debug("Request to update ScopeItem : {}", scopeItemDTO);
        ScopeItem scopeItem = scopeItemMapper.toEntity(scopeItemDTO);
        scopeItem = scopeItemRepository.save(scopeItem);
        return scopeItemMapper.toDto(scopeItem);
    }

    @Override
    public Optional<ScopeItemDTO> partialUpdate(ScopeItemDTO scopeItemDTO) {
        LOG.debug("Request to partially update ScopeItem : {}", scopeItemDTO);

        return scopeItemRepository
            .findById(scopeItemDTO.getId())
            .map(existingScopeItem -> {
                scopeItemMapper.partialUpdate(existingScopeItem, scopeItemDTO);

                return existingScopeItem;
            })
            .map(scopeItemRepository::save)
            .map(scopeItemMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScopeItemDTO> findAll() {
        LOG.debug("Request to get all ScopeItems");
        return scopeItemRepository.findAll().stream().map(scopeItemMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScopeItemDTO> findOne(UUID id) {
        LOG.debug("Request to get ScopeItem : {}", id);
        return scopeItemRepository.findById(id).map(scopeItemMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete ScopeItem : {}", id);
        scopeItemRepository.deleteById(id);
    }
}
