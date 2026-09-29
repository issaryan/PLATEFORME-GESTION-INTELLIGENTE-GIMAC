package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.SignedDecision;
import cm.gimac.des.certification.repository.SignedDecisionRepository;
import cm.gimac.des.certification.service.SignedDecisionService;
import cm.gimac.des.certification.service.dto.SignedDecisionDTO;
import cm.gimac.des.certification.service.mapper.SignedDecisionMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.SignedDecision}.
 */
@Service
@Transactional
public class SignedDecisionServiceImpl implements SignedDecisionService {

    private static final Logger LOG = LoggerFactory.getLogger(SignedDecisionServiceImpl.class);

    private final SignedDecisionRepository signedDecisionRepository;

    private final SignedDecisionMapper signedDecisionMapper;

    public SignedDecisionServiceImpl(SignedDecisionRepository signedDecisionRepository, SignedDecisionMapper signedDecisionMapper) {
        this.signedDecisionRepository = signedDecisionRepository;
        this.signedDecisionMapper = signedDecisionMapper;
    }

    @Override
    public SignedDecisionDTO save(SignedDecisionDTO signedDecisionDTO) {
        LOG.debug("Request to save SignedDecision : {}", signedDecisionDTO);
        SignedDecision signedDecision = signedDecisionMapper.toEntity(signedDecisionDTO);
        signedDecision = signedDecisionRepository.save(signedDecision);
        return signedDecisionMapper.toDto(signedDecision);
    }

    @Override
    public SignedDecisionDTO update(SignedDecisionDTO signedDecisionDTO) {
        // Une décision signée est immuable (PI-GIMAC-01 §0.3, déclencheur trg_signed_decision_immuable ; D-35).
        throw new ResponseStatusException(HttpStatus.CONFLICT, "CER_SIGNED_DECISION_IMMUTABLE : une décision signée ne se modifie pas");
    }

    @Override
    public Optional<SignedDecisionDTO> partialUpdate(SignedDecisionDTO signedDecisionDTO) {
        // Une décision signée est immuable (PI-GIMAC-01 §0.3, déclencheur trg_signed_decision_immuable ; D-35).
        throw new ResponseStatusException(HttpStatus.CONFLICT, "CER_SIGNED_DECISION_IMMUTABLE : une décision signée ne se modifie pas");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignedDecisionDTO> findAll() {
        LOG.debug("Request to get all SignedDecisions");
        return signedDecisionRepository
            .findAll()
            .stream()
            .map(signedDecisionMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SignedDecisionDTO> findOne(UUID id) {
        LOG.debug("Request to get SignedDecision : {}", id);
        return signedDecisionRepository.findById(id).map(signedDecisionMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete SignedDecision : {}", id);
        signedDecisionRepository.deleteById(id);
    }
}
