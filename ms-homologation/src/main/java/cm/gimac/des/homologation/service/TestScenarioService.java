package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.TestScenario;
import cm.gimac.des.homologation.repository.TestScenarioRepository;
import cm.gimac.des.homologation.service.dto.TestScenarioDTO;
import cm.gimac.des.homologation.service.mapper.TestScenarioMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.TestScenario}.
 */
@Service
@Transactional
public class TestScenarioService {

    private static final Logger LOG = LoggerFactory.getLogger(TestScenarioService.class);

    private final TestScenarioRepository testScenarioRepository;

    private final TestScenarioMapper testScenarioMapper;

    public TestScenarioService(TestScenarioRepository testScenarioRepository, TestScenarioMapper testScenarioMapper) {
        this.testScenarioRepository = testScenarioRepository;
        this.testScenarioMapper = testScenarioMapper;
    }

    /**
     * Save a testScenario.
     *
     * @param testScenarioDTO the entity to save.
     * @return the persisted entity.
     */
    public TestScenarioDTO save(TestScenarioDTO testScenarioDTO) {
        LOG.debug("Request to save TestScenario : {}", testScenarioDTO);
        TestScenario testScenario = testScenarioMapper.toEntity(testScenarioDTO);
        testScenario = testScenarioRepository.save(testScenario);
        return testScenarioMapper.toDto(testScenario);
    }

    /**
     * Update a testScenario.
     *
     * @param testScenarioDTO the entity to save.
     * @return the persisted entity.
     */
    public TestScenarioDTO update(TestScenarioDTO testScenarioDTO) {
        LOG.debug("Request to update TestScenario : {}", testScenarioDTO);
        TestScenario testScenario = testScenarioMapper.toEntity(testScenarioDTO);
        testScenario = testScenarioRepository.save(testScenario);
        return testScenarioMapper.toDto(testScenario);
    }

    /**
     * Partially update a testScenario.
     *
     * @param testScenarioDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<TestScenarioDTO> partialUpdate(TestScenarioDTO testScenarioDTO) {
        LOG.debug("Request to partially update TestScenario : {}", testScenarioDTO);

        return testScenarioRepository
            .findById(testScenarioDTO.getId())
            .map(existingTestScenario -> {
                testScenarioMapper.partialUpdate(existingTestScenario, testScenarioDTO);

                return existingTestScenario;
            })
            .map(testScenarioRepository::save)
            .map(testScenarioMapper::toDto);
    }

    /**
     * Get all the testScenarios with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<TestScenarioDTO> findAllWithEagerRelationships(Pageable pageable) {
        return testScenarioRepository.findAllWithEagerRelationships(pageable).map(testScenarioMapper::toDto);
    }

    /**
     * Get one testScenario by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<TestScenarioDTO> findOne(UUID id) {
        LOG.debug("Request to get TestScenario : {}", id);
        return testScenarioRepository.findOneWithEagerRelationships(id).map(testScenarioMapper::toDto);
    }

    /**
     * Delete the testScenario by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete TestScenario : {}", id);
        testScenarioRepository.deleteById(id);
    }
}
