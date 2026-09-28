package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ReferenceOperationAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ReferenceCatalog;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.enumeration.OperationPhase;
import cm.gimac.des.homologation.domain.enumeration.ResponsibleParty;
import cm.gimac.des.homologation.repository.ReferenceOperationRepository;
import cm.gimac.des.homologation.service.ReferenceOperationService;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import cm.gimac.des.homologation.service.mapper.ReferenceOperationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ReferenceOperationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ReferenceOperationResourceIT {

    private static final String DEFAULT_CODE = "SHD4R-47";
    private static final String UPDATED_CODE = "Z9QRZ-237";

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final String DEFAULT_BRANCH = "AAAAAAAAAA";
    private static final String UPDATED_BRANCH = "BBBBBBBBBB";

    private static final OperationPhase DEFAULT_PHASE = OperationPhase.PARAMETRAGE;
    private static final OperationPhase UPDATED_PHASE = OperationPhase.ECHANGE_CERTIFICATS;

    private static final ResponsibleParty DEFAULT_OWNER = ResponsibleParty.GROUPEMENT;
    private static final ResponsibleParty UPDATED_OWNER = ResponsibleParty.PARTICIPANT;

    private static final String DEFAULT_EXPECTED_EVIDENCE = "AAAAAAAAAA";
    private static final String UPDATED_EXPECTED_EVIDENCE = "BBBBBBBBBB";

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_SCENARIO_REF = "AAAAAAAAAA";
    private static final String UPDATED_SCENARIO_REF = "BBBBBBBBBB";

    private static final Boolean DEFAULT_CRITICAL_PATH = false;
    private static final Boolean UPDATED_CRITICAL_PATH = true;

    private static final String ENTITY_API_URL = "/api/reference-operations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReferenceOperationRepository referenceOperationRepository;

    @Mock
    private ReferenceOperationRepository referenceOperationRepositoryMock;

    @Autowired
    private ReferenceOperationMapper referenceOperationMapper;

    @Mock
    private ReferenceOperationService referenceOperationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReferenceOperationMockMvc;

    private ReferenceOperation referenceOperation;

    private ReferenceOperation insertedReferenceOperation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferenceOperation createEntity(EntityManager em) {
        ReferenceOperation referenceOperation = new ReferenceOperation()
            .code(DEFAULT_CODE)
            .label(DEFAULT_LABEL)
            .branch(DEFAULT_BRANCH)
            .phase(DEFAULT_PHASE)
            .owner(DEFAULT_OWNER)
            .expectedEvidence(DEFAULT_EXPECTED_EVIDENCE)
            .serviceCode(DEFAULT_SERVICE_CODE)
            .scenarioRef(DEFAULT_SCENARIO_REF)
            .criticalPath(DEFAULT_CRITICAL_PATH);
        // Add required entity
        ReferenceCatalog referenceCatalog;
        if (TestUtil.findAll(em, ReferenceCatalog.class).isEmpty()) {
            referenceCatalog = ReferenceCatalogResourceIT.createEntity();
            em.persist(referenceCatalog);
            em.flush();
        } else {
            referenceCatalog = TestUtil.findAll(em, ReferenceCatalog.class).get(0);
        }
        referenceOperation.setCatalog(referenceCatalog);
        return referenceOperation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferenceOperation createUpdatedEntity(EntityManager em) {
        ReferenceOperation updatedReferenceOperation = new ReferenceOperation()
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .branch(UPDATED_BRANCH)
            .phase(UPDATED_PHASE)
            .owner(UPDATED_OWNER)
            .expectedEvidence(UPDATED_EXPECTED_EVIDENCE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .scenarioRef(UPDATED_SCENARIO_REF)
            .criticalPath(UPDATED_CRITICAL_PATH);
        // Add required entity
        ReferenceCatalog referenceCatalog;
        if (TestUtil.findAll(em, ReferenceCatalog.class).isEmpty()) {
            referenceCatalog = ReferenceCatalogResourceIT.createUpdatedEntity();
            em.persist(referenceCatalog);
            em.flush();
        } else {
            referenceCatalog = TestUtil.findAll(em, ReferenceCatalog.class).get(0);
        }
        updatedReferenceOperation.setCatalog(referenceCatalog);
        return updatedReferenceOperation;
    }

    @BeforeEach
    void initTest() {
        referenceOperation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedReferenceOperation != null) {
            referenceOperationRepository.delete(insertedReferenceOperation);
            insertedReferenceOperation = null;
        }
    }

    @Test
    @Transactional
    void createReferenceOperation() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);
        var returnedReferenceOperationDTO = om.readValue(
            restReferenceOperationMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(referenceOperationDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReferenceOperationDTO.class
        );

        // Validate the ReferenceOperation in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReferenceOperation = referenceOperationMapper.toEntity(returnedReferenceOperationDTO);
        assertReferenceOperationUpdatableFieldsEquals(
            returnedReferenceOperation,
            getPersistedReferenceOperation(returnedReferenceOperation)
        );

        insertedReferenceOperation = returnedReferenceOperation;
    }

    @Test
    @Transactional
    void createReferenceOperationWithExistingId() throws Exception {
        // Create the ReferenceOperation with an existing ID
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setCode(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setLabel(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkBranchIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setBranch(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPhaseIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setPhase(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOwnerIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setOwner(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkExpectedEvidenceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setExpectedEvidence(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCriticalPathIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceOperation.setCriticalPath(null);

        // Create the ReferenceOperation, which fails.
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        restReferenceOperationMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReferenceOperations() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList
        restReferenceOperationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(referenceOperation.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].branch").value(hasItem(DEFAULT_BRANCH)))
            .andExpect(jsonPath("$.[*].phase").value(hasItem(DEFAULT_PHASE.toString())))
            .andExpect(jsonPath("$.[*].owner").value(hasItem(DEFAULT_OWNER.toString())))
            .andExpect(jsonPath("$.[*].expectedEvidence").value(hasItem(DEFAULT_EXPECTED_EVIDENCE)))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].scenarioRef").value(hasItem(DEFAULT_SCENARIO_REF)))
            .andExpect(jsonPath("$.[*].criticalPath").value(hasItem(DEFAULT_CRITICAL_PATH)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReferenceOperationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(referenceOperationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReferenceOperationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(referenceOperationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllReferenceOperationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(referenceOperationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restReferenceOperationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(referenceOperationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getReferenceOperation() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get the referenceOperation
        restReferenceOperationMockMvc
            .perform(get(ENTITY_API_URL_ID, referenceOperation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(referenceOperation.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.branch").value(DEFAULT_BRANCH))
            .andExpect(jsonPath("$.phase").value(DEFAULT_PHASE.toString()))
            .andExpect(jsonPath("$.owner").value(DEFAULT_OWNER.toString()))
            .andExpect(jsonPath("$.expectedEvidence").value(DEFAULT_EXPECTED_EVIDENCE))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.scenarioRef").value(DEFAULT_SCENARIO_REF))
            .andExpect(jsonPath("$.criticalPath").value(DEFAULT_CRITICAL_PATH));
    }

    @Test
    @Transactional
    void getReferenceOperationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        UUID id = referenceOperation.getId();

        defaultReferenceOperationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where code equals to
        defaultReferenceOperationFiltering("code.equals=" + DEFAULT_CODE, "code.equals=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where code in
        defaultReferenceOperationFiltering("code.in=" + DEFAULT_CODE + "," + UPDATED_CODE, "code.in=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where code is not null
        defaultReferenceOperationFiltering("code.specified=true", "code.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where code contains
        defaultReferenceOperationFiltering("code.contains=" + DEFAULT_CODE, "code.contains=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where code does not contain
        defaultReferenceOperationFiltering("code.doesNotContain=" + UPDATED_CODE, "code.doesNotContain=" + DEFAULT_CODE);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByLabelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where label equals to
        defaultReferenceOperationFiltering("label.equals=" + DEFAULT_LABEL, "label.equals=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByLabelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where label in
        defaultReferenceOperationFiltering("label.in=" + DEFAULT_LABEL + "," + UPDATED_LABEL, "label.in=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByLabelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where label is not null
        defaultReferenceOperationFiltering("label.specified=true", "label.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByLabelContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where label contains
        defaultReferenceOperationFiltering("label.contains=" + DEFAULT_LABEL, "label.contains=" + UPDATED_LABEL);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByLabelNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where label does not contain
        defaultReferenceOperationFiltering("label.doesNotContain=" + UPDATED_LABEL, "label.doesNotContain=" + DEFAULT_LABEL);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByBranchIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where branch equals to
        defaultReferenceOperationFiltering("branch.equals=" + DEFAULT_BRANCH, "branch.equals=" + UPDATED_BRANCH);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByBranchIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where branch in
        defaultReferenceOperationFiltering("branch.in=" + DEFAULT_BRANCH + "," + UPDATED_BRANCH, "branch.in=" + UPDATED_BRANCH);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByBranchIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where branch is not null
        defaultReferenceOperationFiltering("branch.specified=true", "branch.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByBranchContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where branch contains
        defaultReferenceOperationFiltering("branch.contains=" + DEFAULT_BRANCH, "branch.contains=" + UPDATED_BRANCH);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByBranchNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where branch does not contain
        defaultReferenceOperationFiltering("branch.doesNotContain=" + UPDATED_BRANCH, "branch.doesNotContain=" + DEFAULT_BRANCH);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByPhaseIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where phase equals to
        defaultReferenceOperationFiltering("phase.equals=" + DEFAULT_PHASE, "phase.equals=" + UPDATED_PHASE);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByPhaseIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where phase in
        defaultReferenceOperationFiltering("phase.in=" + DEFAULT_PHASE + "," + UPDATED_PHASE, "phase.in=" + UPDATED_PHASE);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByPhaseIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where phase is not null
        defaultReferenceOperationFiltering("phase.specified=true", "phase.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByOwnerIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where owner equals to
        defaultReferenceOperationFiltering("owner.equals=" + DEFAULT_OWNER, "owner.equals=" + UPDATED_OWNER);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByOwnerIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where owner in
        defaultReferenceOperationFiltering("owner.in=" + DEFAULT_OWNER + "," + UPDATED_OWNER, "owner.in=" + UPDATED_OWNER);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByOwnerIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where owner is not null
        defaultReferenceOperationFiltering("owner.specified=true", "owner.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByExpectedEvidenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where expectedEvidence equals to
        defaultReferenceOperationFiltering(
            "expectedEvidence.equals=" + DEFAULT_EXPECTED_EVIDENCE,
            "expectedEvidence.equals=" + UPDATED_EXPECTED_EVIDENCE
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByExpectedEvidenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where expectedEvidence in
        defaultReferenceOperationFiltering(
            "expectedEvidence.in=" + DEFAULT_EXPECTED_EVIDENCE + "," + UPDATED_EXPECTED_EVIDENCE,
            "expectedEvidence.in=" + UPDATED_EXPECTED_EVIDENCE
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByExpectedEvidenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where expectedEvidence is not null
        defaultReferenceOperationFiltering("expectedEvidence.specified=true", "expectedEvidence.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByExpectedEvidenceContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where expectedEvidence contains
        defaultReferenceOperationFiltering(
            "expectedEvidence.contains=" + DEFAULT_EXPECTED_EVIDENCE,
            "expectedEvidence.contains=" + UPDATED_EXPECTED_EVIDENCE
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByExpectedEvidenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where expectedEvidence does not contain
        defaultReferenceOperationFiltering(
            "expectedEvidence.doesNotContain=" + UPDATED_EXPECTED_EVIDENCE,
            "expectedEvidence.doesNotContain=" + DEFAULT_EXPECTED_EVIDENCE
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where serviceCode equals to
        defaultReferenceOperationFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where serviceCode in
        defaultReferenceOperationFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where serviceCode is not null
        defaultReferenceOperationFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where serviceCode contains
        defaultReferenceOperationFiltering("serviceCode.contains=" + DEFAULT_SERVICE_CODE, "serviceCode.contains=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where serviceCode does not contain
        defaultReferenceOperationFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByScenarioRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where scenarioRef equals to
        defaultReferenceOperationFiltering("scenarioRef.equals=" + DEFAULT_SCENARIO_REF, "scenarioRef.equals=" + UPDATED_SCENARIO_REF);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByScenarioRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where scenarioRef in
        defaultReferenceOperationFiltering(
            "scenarioRef.in=" + DEFAULT_SCENARIO_REF + "," + UPDATED_SCENARIO_REF,
            "scenarioRef.in=" + UPDATED_SCENARIO_REF
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByScenarioRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where scenarioRef is not null
        defaultReferenceOperationFiltering("scenarioRef.specified=true", "scenarioRef.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByScenarioRefContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where scenarioRef contains
        defaultReferenceOperationFiltering("scenarioRef.contains=" + DEFAULT_SCENARIO_REF, "scenarioRef.contains=" + UPDATED_SCENARIO_REF);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByScenarioRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where scenarioRef does not contain
        defaultReferenceOperationFiltering(
            "scenarioRef.doesNotContain=" + UPDATED_SCENARIO_REF,
            "scenarioRef.doesNotContain=" + DEFAULT_SCENARIO_REF
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCriticalPathIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where criticalPath equals to
        defaultReferenceOperationFiltering("criticalPath.equals=" + DEFAULT_CRITICAL_PATH, "criticalPath.equals=" + UPDATED_CRITICAL_PATH);
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCriticalPathIsInShouldWork() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where criticalPath in
        defaultReferenceOperationFiltering(
            "criticalPath.in=" + DEFAULT_CRITICAL_PATH + "," + UPDATED_CRITICAL_PATH,
            "criticalPath.in=" + UPDATED_CRITICAL_PATH
        );
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCriticalPathIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        // Get all the referenceOperationList where criticalPath is not null
        defaultReferenceOperationFiltering("criticalPath.specified=true", "criticalPath.specified=false");
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByCatalogIsEqualToSomething() throws Exception {
        ReferenceCatalog catalog;
        if (TestUtil.findAll(em, ReferenceCatalog.class).isEmpty()) {
            referenceOperationRepository.saveAndFlush(referenceOperation);
            catalog = ReferenceCatalogResourceIT.createEntity();
        } else {
            catalog = TestUtil.findAll(em, ReferenceCatalog.class).get(0);
        }
        em.persist(catalog);
        em.flush();
        referenceOperation.setCatalog(catalog);
        referenceOperationRepository.saveAndFlush(referenceOperation);
        UUID catalogId = catalog.getId();
        // Get all the referenceOperationList where catalog equals to catalogId
        defaultReferenceOperationShouldBeFound("catalogId.equals=" + catalogId);

        // Get all the referenceOperationList where catalog equals to UUID.randomUUID()
        defaultReferenceOperationShouldNotBeFound("catalogId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllReferenceOperationsByPredecessorsIsEqualToSomething() throws Exception {
        ReferenceOperation predecessors;
        if (TestUtil.findAll(em, ReferenceOperation.class).isEmpty()) {
            referenceOperationRepository.saveAndFlush(referenceOperation);
            predecessors = ReferenceOperationResourceIT.createEntity(em);
        } else {
            predecessors = TestUtil.findAll(em, ReferenceOperation.class).get(0);
        }
        em.persist(predecessors);
        em.flush();
        referenceOperation.addPredecessors(predecessors);
        referenceOperationRepository.saveAndFlush(referenceOperation);
        UUID predecessorsId = predecessors.getId();
        // Get all the referenceOperationList where predecessors equals to predecessorsId
        defaultReferenceOperationShouldBeFound("predecessorsId.equals=" + predecessorsId);

        // Get all the referenceOperationList where predecessors equals to UUID.randomUUID()
        defaultReferenceOperationShouldNotBeFound("predecessorsId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllReferenceOperationsBySuccessorsIsEqualToSomething() throws Exception {
        ReferenceOperation successors;
        if (TestUtil.findAll(em, ReferenceOperation.class).isEmpty()) {
            referenceOperationRepository.saveAndFlush(referenceOperation);
            successors = ReferenceOperationResourceIT.createEntity(em);
        } else {
            successors = TestUtil.findAll(em, ReferenceOperation.class).get(0);
        }
        em.persist(successors);
        em.flush();
        referenceOperation.addSuccessors(successors);
        referenceOperationRepository.saveAndFlush(referenceOperation);
        UUID successorsId = successors.getId();
        // Get all the referenceOperationList where successors equals to successorsId
        defaultReferenceOperationShouldBeFound("successorsId.equals=" + successorsId);

        // Get all the referenceOperationList where successors equals to UUID.randomUUID()
        defaultReferenceOperationShouldNotBeFound("successorsId.equals=" + UUID.randomUUID());
    }

    private void defaultReferenceOperationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultReferenceOperationShouldBeFound(shouldBeFound);
        defaultReferenceOperationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultReferenceOperationShouldBeFound(String filter) throws Exception {
        restReferenceOperationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(referenceOperation.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].branch").value(hasItem(DEFAULT_BRANCH)))
            .andExpect(jsonPath("$.[*].phase").value(hasItem(DEFAULT_PHASE.toString())))
            .andExpect(jsonPath("$.[*].owner").value(hasItem(DEFAULT_OWNER.toString())))
            .andExpect(jsonPath("$.[*].expectedEvidence").value(hasItem(DEFAULT_EXPECTED_EVIDENCE)))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].scenarioRef").value(hasItem(DEFAULT_SCENARIO_REF)))
            .andExpect(jsonPath("$.[*].criticalPath").value(hasItem(DEFAULT_CRITICAL_PATH)));

        // Check, that the count call also returns 1
        restReferenceOperationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultReferenceOperationShouldNotBeFound(String filter) throws Exception {
        restReferenceOperationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restReferenceOperationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingReferenceOperation() throws Exception {
        // Get the referenceOperation
        restReferenceOperationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReferenceOperation() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceOperation
        ReferenceOperation updatedReferenceOperation = referenceOperationRepository.findById(referenceOperation.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReferenceOperation are not directly saved in db
        em.detach(updatedReferenceOperation);
        updatedReferenceOperation
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .branch(UPDATED_BRANCH)
            .phase(UPDATED_PHASE)
            .owner(UPDATED_OWNER)
            .expectedEvidence(UPDATED_EXPECTED_EVIDENCE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .scenarioRef(UPDATED_SCENARIO_REF)
            .criticalPath(UPDATED_CRITICAL_PATH);
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(updatedReferenceOperation);

        restReferenceOperationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referenceOperationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReferenceOperationToMatchAllProperties(updatedReferenceOperation);
    }

    @Test
    @Transactional
    void putNonExistingReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referenceOperationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReferenceOperationWithPatch() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceOperation using partial update
        ReferenceOperation partialUpdatedReferenceOperation = new ReferenceOperation();
        partialUpdatedReferenceOperation.setId(referenceOperation.getId());

        partialUpdatedReferenceOperation
            .label(UPDATED_LABEL)
            .branch(UPDATED_BRANCH)
            .phase(UPDATED_PHASE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .criticalPath(UPDATED_CRITICAL_PATH);

        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferenceOperation.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferenceOperation))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceOperation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferenceOperationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReferenceOperation, referenceOperation),
            getPersistedReferenceOperation(referenceOperation)
        );
    }

    @Test
    @Transactional
    void fullUpdateReferenceOperationWithPatch() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceOperation using partial update
        ReferenceOperation partialUpdatedReferenceOperation = new ReferenceOperation();
        partialUpdatedReferenceOperation.setId(referenceOperation.getId());

        partialUpdatedReferenceOperation
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .branch(UPDATED_BRANCH)
            .phase(UPDATED_PHASE)
            .owner(UPDATED_OWNER)
            .expectedEvidence(UPDATED_EXPECTED_EVIDENCE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .scenarioRef(UPDATED_SCENARIO_REF)
            .criticalPath(UPDATED_CRITICAL_PATH);

        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferenceOperation.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferenceOperation))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceOperation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferenceOperationUpdatableFieldsEquals(
            partialUpdatedReferenceOperation,
            getPersistedReferenceOperation(partialUpdatedReferenceOperation)
        );
    }

    @Test
    @Transactional
    void patchNonExistingReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, referenceOperationDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReferenceOperation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceOperation.setId(UUID.randomUUID());

        // Create the ReferenceOperation
        ReferenceOperationDTO referenceOperationDTO = referenceOperationMapper.toDto(referenceOperation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceOperationMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceOperationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferenceOperation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReferenceOperation() throws Exception {
        // Initialize the database
        insertedReferenceOperation = referenceOperationRepository.saveAndFlush(referenceOperation);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the referenceOperation
        restReferenceOperationMockMvc
            .perform(delete(ENTITY_API_URL_ID, referenceOperation.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return referenceOperationRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected ReferenceOperation getPersistedReferenceOperation(ReferenceOperation referenceOperation) {
        return referenceOperationRepository.findById(referenceOperation.getId()).orElseThrow();
    }

    protected void assertPersistedReferenceOperationToMatchAllProperties(ReferenceOperation expectedReferenceOperation) {
        assertReferenceOperationAllPropertiesEquals(expectedReferenceOperation, getPersistedReferenceOperation(expectedReferenceOperation));
    }

    protected void assertPersistedReferenceOperationToMatchUpdatableProperties(ReferenceOperation expectedReferenceOperation) {
        assertReferenceOperationAllUpdatablePropertiesEquals(
            expectedReferenceOperation,
            getPersistedReferenceOperation(expectedReferenceOperation)
        );
    }
}
