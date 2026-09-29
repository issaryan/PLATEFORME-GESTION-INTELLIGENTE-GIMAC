package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.CompletenessCondition;
import cm.gimac.des.certification.repository.CompletenessConditionRepository;
import cm.gimac.des.certification.service.CompletenessConditionService;
import cm.gimac.des.certification.service.dto.CompletenessConditionDTO;
import cm.gimac.des.certification.service.mapper.CompletenessConditionMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.CompletenessCondition}.
 */
@Service
@Transactional
public class CompletenessConditionServiceImpl implements CompletenessConditionService {

    private static final Logger LOG = LoggerFactory.getLogger(CompletenessConditionServiceImpl.class);

    private final CompletenessConditionRepository completenessConditionRepository;

    private final CompletenessConditionMapper completenessConditionMapper;

    public CompletenessConditionServiceImpl(
        CompletenessConditionRepository completenessConditionRepository,
        CompletenessConditionMapper completenessConditionMapper
    ) {
        this.completenessConditionRepository = completenessConditionRepository;
        this.completenessConditionMapper = completenessConditionMapper;
    }

    @Override
    public CompletenessConditionDTO save(CompletenessConditionDTO completenessConditionDTO) {
        LOG.debug("Request to save CompletenessCondition : {}", completenessConditionDTO);
        CompletenessCondition completenessCondition = completenessConditionMapper.toEntity(completenessConditionDTO);
        completenessCondition = completenessConditionRepository.save(completenessCondition);
        return completenessConditionMapper.toDto(completenessCondition);
    }

    @Override
    public CompletenessConditionDTO update(CompletenessConditionDTO completenessConditionDTO) {
        LOG.debug("Request to update CompletenessCondition : {}", completenessConditionDTO);
        CompletenessCondition completenessCondition = completenessConditionMapper.toEntity(completenessConditionDTO);
        completenessCondition = completenessConditionRepository.save(completenessCondition);
        return completenessConditionMapper.toDto(completenessCondition);
    }

    @Override
    public Optional<CompletenessConditionDTO> partialUpdate(CompletenessConditionDTO completenessConditionDTO) {
        LOG.debug("Request to partially update CompletenessCondition : {}", completenessConditionDTO);

        return completenessConditionRepository
            .findById(completenessConditionDTO.getId())
            .map(existingCompletenessCondition -> {
                completenessConditionMapper.partialUpdate(existingCompletenessCondition, completenessConditionDTO);

                return existingCompletenessCondition;
            })
            .map(completenessConditionRepository::save)
            .map(completenessConditionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompletenessConditionDTO> findAll() {
        LOG.debug("Request to get all CompletenessConditions");
        return completenessConditionRepository
            .findAll()
            .stream()
            .map(completenessConditionMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompletenessConditionDTO> findOne(UUID id) {
        LOG.debug("Request to get CompletenessCondition : {}", id);
        return completenessConditionRepository.findById(id).map(completenessConditionMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete CompletenessCondition : {}", id);
        completenessConditionRepository.deleteById(id);
    }
}
