package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.service.QuitusService;
import cm.gimac.des.certification.service.dto.QuitusDTO;
import cm.gimac.des.certification.service.mapper.QuitusMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.Quitus}.
 */
@Service
@Transactional
public class QuitusServiceImpl implements QuitusService {

    private static final Logger LOG = LoggerFactory.getLogger(QuitusServiceImpl.class);

    private final QuitusRepository quitusRepository;

    private final QuitusMapper quitusMapper;

    public QuitusServiceImpl(QuitusRepository quitusRepository, QuitusMapper quitusMapper) {
        this.quitusRepository = quitusRepository;
        this.quitusMapper = quitusMapper;
    }

    @Override
    public QuitusDTO save(QuitusDTO quitusDTO) {
        LOG.debug("Request to save Quitus : {}", quitusDTO);
        Quitus quitus = quitusMapper.toEntity(quitusDTO);
        quitus = quitusRepository.save(quitus);
        return quitusMapper.toDto(quitus);
    }

    @Override
    public QuitusDTO update(QuitusDTO quitusDTO) {
        LOG.debug("Request to update Quitus : {}", quitusDTO);
        Quitus quitus = quitusMapper.toEntity(quitusDTO);
        quitus = quitusRepository.save(quitus);
        return quitusMapper.toDto(quitus);
    }

    @Override
    public Optional<QuitusDTO> partialUpdate(QuitusDTO quitusDTO) {
        LOG.debug("Request to partially update Quitus : {}", quitusDTO);

        return quitusRepository
            .findById(quitusDTO.getId())
            .map(existingQuitus -> {
                quitusMapper.partialUpdate(existingQuitus, quitusDTO);

                return existingQuitus;
            })
            .map(quitusRepository::save)
            .map(quitusMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuitusDTO> findAll() {
        LOG.debug("Request to get all Quituses");
        return quitusRepository.findAll().stream().map(quitusMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the quituses where SignedDecision is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<QuitusDTO> findAllWhereSignedDecisionIsNull() {
        LOG.debug("Request to get all quituses where SignedDecision is null");
        return StreamSupport.stream(quitusRepository.findAll().spliterator(), false)
            .filter(quitus -> quitus.getSignedDecision() == null)
            .map(quitusMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuitusDTO> findOne(UUID id) {
        LOG.debug("Request to get Quitus : {}", id);
        return quitusRepository.findById(id).map(quitusMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete Quitus : {}", id);
        quitusRepository.deleteById(id);
    }
}
