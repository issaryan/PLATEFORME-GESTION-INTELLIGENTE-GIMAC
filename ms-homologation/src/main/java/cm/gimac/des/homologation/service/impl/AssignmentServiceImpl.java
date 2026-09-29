package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.Assignment;
import cm.gimac.des.homologation.repository.AssignmentRepository;
import cm.gimac.des.homologation.service.AssignmentService;
import cm.gimac.des.homologation.service.dto.AssignmentDTO;
import cm.gimac.des.homologation.service.mapper.AssignmentMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.Assignment}.
 */
@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private static final Logger LOG = LoggerFactory.getLogger(AssignmentServiceImpl.class);

    private final AssignmentRepository assignmentRepository;

    private final AssignmentMapper assignmentMapper;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository, AssignmentMapper assignmentMapper) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentMapper = assignmentMapper;
    }

    @Override
    public AssignmentDTO save(AssignmentDTO assignmentDTO) {
        LOG.debug("Request to save Assignment : {}", assignmentDTO);
        Assignment assignment = assignmentMapper.toEntity(assignmentDTO);
        assignment = assignmentRepository.save(assignment);
        return assignmentMapper.toDto(assignment);
    }

    @Override
    public AssignmentDTO update(AssignmentDTO assignmentDTO) {
        LOG.debug("Request to update Assignment : {}", assignmentDTO);
        Assignment assignment = assignmentMapper.toEntity(assignmentDTO);
        assignment = assignmentRepository.save(assignment);
        return assignmentMapper.toDto(assignment);
    }

    @Override
    public Optional<AssignmentDTO> partialUpdate(AssignmentDTO assignmentDTO) {
        LOG.debug("Request to partially update Assignment : {}", assignmentDTO);

        return assignmentRepository
            .findById(assignmentDTO.getId())
            .map(existingAssignment -> {
                assignmentMapper.partialUpdate(existingAssignment, assignmentDTO);

                return existingAssignment;
            })
            .map(assignmentRepository::save)
            .map(assignmentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentDTO> findAll() {
        LOG.debug("Request to get all Assignments");
        return assignmentRepository.findAll().stream().map(assignmentMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentDTO> findOne(UUID id) {
        LOG.debug("Request to get Assignment : {}", id);
        return assignmentRepository.findById(id).map(assignmentMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete Assignment : {}", id);
        assignmentRepository.deleteById(id);
    }
}
