package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.EvaluationBase;
import cm.gimac.des.homologation.repository.EvaluationBaseRepository;
import cm.gimac.des.homologation.service.EvaluationBaseService;
import cm.gimac.des.homologation.service.dto.EvaluationBaseDTO;
import cm.gimac.des.homologation.service.mapper.EvaluationBaseMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.EvaluationBase}.
 */
@Service
@Transactional
public class EvaluationBaseServiceImpl implements EvaluationBaseService {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationBaseServiceImpl.class);

    private final EvaluationBaseRepository evaluationBaseRepository;

    private final EvaluationBaseMapper evaluationBaseMapper;

    public EvaluationBaseServiceImpl(EvaluationBaseRepository evaluationBaseRepository, EvaluationBaseMapper evaluationBaseMapper) {
        this.evaluationBaseRepository = evaluationBaseRepository;
        this.evaluationBaseMapper = evaluationBaseMapper;
    }

    @Override
    public EvaluationBaseDTO save(EvaluationBaseDTO evaluationBaseDTO) {
        LOG.debug("Request to save EvaluationBase : {}", evaluationBaseDTO);
        EvaluationBase evaluationBase = evaluationBaseMapper.toEntity(evaluationBaseDTO);
        evaluationBase = evaluationBaseRepository.save(evaluationBase);
        return evaluationBaseMapper.toDto(evaluationBase);
    }

    @Override
    public EvaluationBaseDTO update(EvaluationBaseDTO evaluationBaseDTO) {
        LOG.debug("Request to update EvaluationBase : {}", evaluationBaseDTO);
        EvaluationBase evaluationBase = evaluationBaseMapper.toEntity(evaluationBaseDTO);
        evaluationBase = evaluationBaseRepository.save(evaluationBase);
        return evaluationBaseMapper.toDto(evaluationBase);
    }

    @Override
    public Optional<EvaluationBaseDTO> partialUpdate(EvaluationBaseDTO evaluationBaseDTO) {
        LOG.debug("Request to partially update EvaluationBase : {}", evaluationBaseDTO);

        return evaluationBaseRepository
            .findById(evaluationBaseDTO.getId())
            .map(existingEvaluationBase -> {
                evaluationBaseMapper.partialUpdate(existingEvaluationBase, evaluationBaseDTO);

                return existingEvaluationBase;
            })
            .map(evaluationBaseRepository::save)
            .map(evaluationBaseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationBaseDTO> findAll() {
        LOG.debug("Request to get all EvaluationBases");
        return evaluationBaseRepository
            .findAll()
            .stream()
            .map(evaluationBaseMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvaluationBaseDTO> findOne(UUID id) {
        LOG.debug("Request to get EvaluationBase : {}", id);
        return evaluationBaseRepository.findById(id).map(evaluationBaseMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete EvaluationBase : {}", id);
        evaluationBaseRepository.deleteById(id);
    }
}
