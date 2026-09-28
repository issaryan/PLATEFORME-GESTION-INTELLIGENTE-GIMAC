package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.ActionSheetAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static cm.gimac.des.pilotage.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.ActionSheet;
import cm.gimac.des.pilotage.domain.Risk;
import cm.gimac.des.pilotage.domain.enumeration.OriginKind;
import cm.gimac.des.pilotage.domain.enumeration.PdcaPhase;
import cm.gimac.des.pilotage.repository.ActionSheetRepository;
import cm.gimac.des.pilotage.service.ActionSheetService;
import cm.gimac.des.pilotage.service.dto.ActionSheetDTO;
import cm.gimac.des.pilotage.service.mapper.ActionSheetMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
 * Integration tests for the {@link ActionSheetResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ActionSheetResourceIT {

    private static final OriginKind DEFAULT_ORIGIN_KIND = OriginKind.TICKET;
    private static final OriginKind UPDATED_ORIGIN_KIND = OriginKind.OPERATION;

    private static final String DEFAULT_ORIGIN_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_ORIGIN_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_ORIGIN_SERVICE = "AAAAAAAAAA";
    private static final String UPDATED_ORIGIN_SERVICE = "BBBBBBBBBB";

    private static final PdcaPhase DEFAULT_PHASE = PdcaPhase.PLANIFIER;
    private static final PdcaPhase UPDATED_PHASE = PdcaPhase.REALISER;

    private static final String DEFAULT_OBJECTIVE = "AAAAAAAAAA";
    private static final String UPDATED_OBJECTIVE = "BBBBBBBBBB";

    private static final String DEFAULT_CAUSE_ANALYSIS = "AAAAAAAAAA";
    private static final String UPDATED_CAUSE_ANALYSIS = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_TARGET_SCORE = new BigDecimal(0);
    private static final BigDecimal UPDATED_TARGET_SCORE = new BigDecimal(1);
    private static final BigDecimal SMALLER_TARGET_SCORE = new BigDecimal(0 - 1);

    private static final UUID DEFAULT_OWNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_OWNER_ID = UUID.randomUUID();

    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_DUE_DATE = LocalDate.ofEpochDay(-1L);

    private static final LocalDate DEFAULT_OBSERVATION_END_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_OBSERVATION_END_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_OBSERVATION_END_DATE = LocalDate.ofEpochDay(-1L);

    private static final String DEFAULT_RESULT = "AAAAAAAAAA";
    private static final String UPDATED_RESULT = "BBBBBBBBBB";

    private static final Integer DEFAULT_CYCLE = 1;
    private static final Integer UPDATED_CYCLE = 2;
    private static final Integer SMALLER_CYCLE = 1 - 1;

    private static final UUID DEFAULT_PROCEDURE_DOCUMENT_REF = UUID.randomUUID();
    private static final UUID UPDATED_PROCEDURE_DOCUMENT_REF = UUID.randomUUID();

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/action-sheets";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ActionSheetRepository actionSheetRepository;

    @Mock
    private ActionSheetRepository actionSheetRepositoryMock;

    @Autowired
    private ActionSheetMapper actionSheetMapper;

    @Mock
    private ActionSheetService actionSheetServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restActionSheetMockMvc;

    private ActionSheet actionSheet;

    private ActionSheet insertedActionSheet;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ActionSheet createEntity() {
        return new ActionSheet()
            .originKind(DEFAULT_ORIGIN_KIND)
            .originReference(DEFAULT_ORIGIN_REFERENCE)
            .originService(DEFAULT_ORIGIN_SERVICE)
            .phase(DEFAULT_PHASE)
            .objective(DEFAULT_OBJECTIVE)
            .causeAnalysis(DEFAULT_CAUSE_ANALYSIS)
            .targetScore(DEFAULT_TARGET_SCORE)
            .ownerId(DEFAULT_OWNER_ID)
            .dueDate(DEFAULT_DUE_DATE)
            .observationEndDate(DEFAULT_OBSERVATION_END_DATE)
            .result(DEFAULT_RESULT)
            .cycle(DEFAULT_CYCLE)
            .procedureDocumentRef(DEFAULT_PROCEDURE_DOCUMENT_REF)
            .closedAt(DEFAULT_CLOSED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ActionSheet createUpdatedEntity() {
        return new ActionSheet()
            .originKind(UPDATED_ORIGIN_KIND)
            .originReference(UPDATED_ORIGIN_REFERENCE)
            .originService(UPDATED_ORIGIN_SERVICE)
            .phase(UPDATED_PHASE)
            .objective(UPDATED_OBJECTIVE)
            .causeAnalysis(UPDATED_CAUSE_ANALYSIS)
            .targetScore(UPDATED_TARGET_SCORE)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .observationEndDate(UPDATED_OBSERVATION_END_DATE)
            .result(UPDATED_RESULT)
            .cycle(UPDATED_CYCLE)
            .procedureDocumentRef(UPDATED_PROCEDURE_DOCUMENT_REF)
            .closedAt(UPDATED_CLOSED_AT);
    }

    @BeforeEach
    void initTest() {
        actionSheet = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedActionSheet != null) {
            actionSheetRepository.delete(insertedActionSheet);
            insertedActionSheet = null;
        }
    }

    @Test
    @Transactional
    void createActionSheet() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ActionSheet
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);
        var returnedActionSheetDTO = om.readValue(
            restActionSheetMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ActionSheetDTO.class
        );

        // Validate the ActionSheet in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedActionSheet = actionSheetMapper.toEntity(returnedActionSheetDTO);
        assertActionSheetUpdatableFieldsEquals(returnedActionSheet, getPersistedActionSheet(returnedActionSheet));

        insertedActionSheet = returnedActionSheet;
    }

    @Test
    @Transactional
    void createActionSheetWithExistingId() throws Exception {
        // Create the ActionSheet with an existing ID
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restActionSheetMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ActionSheet in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkOriginKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actionSheet.setOriginKind(null);

        // Create the ActionSheet, which fails.
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        restActionSheetMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPhaseIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actionSheet.setPhase(null);

        // Create the ActionSheet, which fails.
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        restActionSheetMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkObjectiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actionSheet.setObjective(null);

        // Create the ActionSheet, which fails.
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        restActionSheetMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOwnerIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actionSheet.setOwnerId(null);

        // Create the ActionSheet, which fails.
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        restActionSheetMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDueDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actionSheet.setDueDate(null);

        // Create the ActionSheet, which fails.
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        restActionSheetMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCycleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actionSheet.setCycle(null);

        // Create the ActionSheet, which fails.
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        restActionSheetMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllActionSheets() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList
        restActionSheetMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(actionSheet.getId().toString())))
            .andExpect(jsonPath("$.[*].originKind").value(hasItem(DEFAULT_ORIGIN_KIND.toString())))
            .andExpect(jsonPath("$.[*].originReference").value(hasItem(DEFAULT_ORIGIN_REFERENCE)))
            .andExpect(jsonPath("$.[*].originService").value(hasItem(DEFAULT_ORIGIN_SERVICE)))
            .andExpect(jsonPath("$.[*].phase").value(hasItem(DEFAULT_PHASE.toString())))
            .andExpect(jsonPath("$.[*].objective").value(hasItem(DEFAULT_OBJECTIVE)))
            .andExpect(jsonPath("$.[*].causeAnalysis").value(hasItem(DEFAULT_CAUSE_ANALYSIS)))
            .andExpect(jsonPath("$.[*].targetScore").value(hasItem(sameNumber(DEFAULT_TARGET_SCORE))))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].observationEndDate").value(hasItem(DEFAULT_OBSERVATION_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].result").value(hasItem(DEFAULT_RESULT)))
            .andExpect(jsonPath("$.[*].cycle").value(hasItem(DEFAULT_CYCLE)))
            .andExpect(jsonPath("$.[*].procedureDocumentRef").value(hasItem(DEFAULT_PROCEDURE_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllActionSheetsWithEagerRelationshipsIsEnabled() throws Exception {
        when(actionSheetServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restActionSheetMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(actionSheetServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllActionSheetsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(actionSheetServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restActionSheetMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(actionSheetRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getActionSheet() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get the actionSheet
        restActionSheetMockMvc
            .perform(get(ENTITY_API_URL_ID, actionSheet.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(actionSheet.getId().toString()))
            .andExpect(jsonPath("$.originKind").value(DEFAULT_ORIGIN_KIND.toString()))
            .andExpect(jsonPath("$.originReference").value(DEFAULT_ORIGIN_REFERENCE))
            .andExpect(jsonPath("$.originService").value(DEFAULT_ORIGIN_SERVICE))
            .andExpect(jsonPath("$.phase").value(DEFAULT_PHASE.toString()))
            .andExpect(jsonPath("$.objective").value(DEFAULT_OBJECTIVE))
            .andExpect(jsonPath("$.causeAnalysis").value(DEFAULT_CAUSE_ANALYSIS))
            .andExpect(jsonPath("$.targetScore").value(sameNumber(DEFAULT_TARGET_SCORE)))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.dueDate").value(DEFAULT_DUE_DATE.toString()))
            .andExpect(jsonPath("$.observationEndDate").value(DEFAULT_OBSERVATION_END_DATE.toString()))
            .andExpect(jsonPath("$.result").value(DEFAULT_RESULT))
            .andExpect(jsonPath("$.cycle").value(DEFAULT_CYCLE))
            .andExpect(jsonPath("$.procedureDocumentRef").value(DEFAULT_PROCEDURE_DOCUMENT_REF.toString()))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()));
    }

    @Test
    @Transactional
    void getActionSheetsByIdFiltering() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        UUID id = actionSheet.getId();

        defaultActionSheetFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginKindIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originKind equals to
        defaultActionSheetFiltering("originKind.equals=" + DEFAULT_ORIGIN_KIND, "originKind.equals=" + UPDATED_ORIGIN_KIND);
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginKindIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originKind in
        defaultActionSheetFiltering(
            "originKind.in=" + DEFAULT_ORIGIN_KIND + "," + UPDATED_ORIGIN_KIND,
            "originKind.in=" + UPDATED_ORIGIN_KIND
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginKindIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originKind is not null
        defaultActionSheetFiltering("originKind.specified=true", "originKind.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originReference equals to
        defaultActionSheetFiltering(
            "originReference.equals=" + DEFAULT_ORIGIN_REFERENCE,
            "originReference.equals=" + UPDATED_ORIGIN_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originReference in
        defaultActionSheetFiltering(
            "originReference.in=" + DEFAULT_ORIGIN_REFERENCE + "," + UPDATED_ORIGIN_REFERENCE,
            "originReference.in=" + UPDATED_ORIGIN_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originReference is not null
        defaultActionSheetFiltering("originReference.specified=true", "originReference.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originReference contains
        defaultActionSheetFiltering(
            "originReference.contains=" + DEFAULT_ORIGIN_REFERENCE,
            "originReference.contains=" + UPDATED_ORIGIN_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originReference does not contain
        defaultActionSheetFiltering(
            "originReference.doesNotContain=" + UPDATED_ORIGIN_REFERENCE,
            "originReference.doesNotContain=" + DEFAULT_ORIGIN_REFERENCE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginServiceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originService equals to
        defaultActionSheetFiltering("originService.equals=" + DEFAULT_ORIGIN_SERVICE, "originService.equals=" + UPDATED_ORIGIN_SERVICE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginServiceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originService in
        defaultActionSheetFiltering(
            "originService.in=" + DEFAULT_ORIGIN_SERVICE + "," + UPDATED_ORIGIN_SERVICE,
            "originService.in=" + UPDATED_ORIGIN_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginServiceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originService is not null
        defaultActionSheetFiltering("originService.specified=true", "originService.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginServiceContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originService contains
        defaultActionSheetFiltering("originService.contains=" + DEFAULT_ORIGIN_SERVICE, "originService.contains=" + UPDATED_ORIGIN_SERVICE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByOriginServiceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where originService does not contain
        defaultActionSheetFiltering(
            "originService.doesNotContain=" + UPDATED_ORIGIN_SERVICE,
            "originService.doesNotContain=" + DEFAULT_ORIGIN_SERVICE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByPhaseIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where phase equals to
        defaultActionSheetFiltering("phase.equals=" + DEFAULT_PHASE, "phase.equals=" + UPDATED_PHASE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByPhaseIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where phase in
        defaultActionSheetFiltering("phase.in=" + DEFAULT_PHASE + "," + UPDATED_PHASE, "phase.in=" + UPDATED_PHASE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByPhaseIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where phase is not null
        defaultActionSheetFiltering("phase.specified=true", "phase.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByObjectiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where objective equals to
        defaultActionSheetFiltering("objective.equals=" + DEFAULT_OBJECTIVE, "objective.equals=" + UPDATED_OBJECTIVE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByObjectiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where objective in
        defaultActionSheetFiltering("objective.in=" + DEFAULT_OBJECTIVE + "," + UPDATED_OBJECTIVE, "objective.in=" + UPDATED_OBJECTIVE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByObjectiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where objective is not null
        defaultActionSheetFiltering("objective.specified=true", "objective.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByObjectiveContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where objective contains
        defaultActionSheetFiltering("objective.contains=" + DEFAULT_OBJECTIVE, "objective.contains=" + UPDATED_OBJECTIVE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByObjectiveNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where objective does not contain
        defaultActionSheetFiltering("objective.doesNotContain=" + UPDATED_OBJECTIVE, "objective.doesNotContain=" + DEFAULT_OBJECTIVE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCauseAnalysisIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where causeAnalysis equals to
        defaultActionSheetFiltering("causeAnalysis.equals=" + DEFAULT_CAUSE_ANALYSIS, "causeAnalysis.equals=" + UPDATED_CAUSE_ANALYSIS);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCauseAnalysisIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where causeAnalysis in
        defaultActionSheetFiltering(
            "causeAnalysis.in=" + DEFAULT_CAUSE_ANALYSIS + "," + UPDATED_CAUSE_ANALYSIS,
            "causeAnalysis.in=" + UPDATED_CAUSE_ANALYSIS
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByCauseAnalysisIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where causeAnalysis is not null
        defaultActionSheetFiltering("causeAnalysis.specified=true", "causeAnalysis.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByCauseAnalysisContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where causeAnalysis contains
        defaultActionSheetFiltering("causeAnalysis.contains=" + DEFAULT_CAUSE_ANALYSIS, "causeAnalysis.contains=" + UPDATED_CAUSE_ANALYSIS);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCauseAnalysisNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where causeAnalysis does not contain
        defaultActionSheetFiltering(
            "causeAnalysis.doesNotContain=" + UPDATED_CAUSE_ANALYSIS,
            "causeAnalysis.doesNotContain=" + DEFAULT_CAUSE_ANALYSIS
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByTargetScoreIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where targetScore equals to
        defaultActionSheetFiltering("targetScore.equals=" + DEFAULT_TARGET_SCORE, "targetScore.equals=" + UPDATED_TARGET_SCORE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByTargetScoreIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where targetScore in
        defaultActionSheetFiltering(
            "targetScore.in=" + DEFAULT_TARGET_SCORE + "," + UPDATED_TARGET_SCORE,
            "targetScore.in=" + UPDATED_TARGET_SCORE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByTargetScoreIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where targetScore is not null
        defaultActionSheetFiltering("targetScore.specified=true", "targetScore.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByTargetScoreIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where targetScore is greater than or equal to
        defaultActionSheetFiltering(
            "targetScore.greaterThanOrEqual=" + DEFAULT_TARGET_SCORE,
            "targetScore.greaterThanOrEqual=" + (DEFAULT_TARGET_SCORE.add(BigDecimal.ONE))
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByTargetScoreIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where targetScore is less than or equal to
        defaultActionSheetFiltering(
            "targetScore.lessThanOrEqual=" + DEFAULT_TARGET_SCORE,
            "targetScore.lessThanOrEqual=" + SMALLER_TARGET_SCORE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByTargetScoreIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where targetScore is less than
        defaultActionSheetFiltering(
            "targetScore.lessThan=" + (DEFAULT_TARGET_SCORE.add(BigDecimal.ONE)),
            "targetScore.lessThan=" + DEFAULT_TARGET_SCORE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByTargetScoreIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where targetScore is greater than
        defaultActionSheetFiltering("targetScore.greaterThan=" + SMALLER_TARGET_SCORE, "targetScore.greaterThan=" + DEFAULT_TARGET_SCORE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByOwnerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where ownerId equals to
        defaultActionSheetFiltering("ownerId.equals=" + DEFAULT_OWNER_ID, "ownerId.equals=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllActionSheetsByOwnerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where ownerId in
        defaultActionSheetFiltering("ownerId.in=" + DEFAULT_OWNER_ID + "," + UPDATED_OWNER_ID, "ownerId.in=" + UPDATED_OWNER_ID);
    }

    @Test
    @Transactional
    void getAllActionSheetsByOwnerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where ownerId is not null
        defaultActionSheetFiltering("ownerId.specified=true", "ownerId.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByDueDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where dueDate equals to
        defaultActionSheetFiltering("dueDate.equals=" + DEFAULT_DUE_DATE, "dueDate.equals=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByDueDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where dueDate in
        defaultActionSheetFiltering("dueDate.in=" + DEFAULT_DUE_DATE + "," + UPDATED_DUE_DATE, "dueDate.in=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByDueDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where dueDate is not null
        defaultActionSheetFiltering("dueDate.specified=true", "dueDate.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByDueDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where dueDate is greater than or equal to
        defaultActionSheetFiltering("dueDate.greaterThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.greaterThanOrEqual=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByDueDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where dueDate is less than or equal to
        defaultActionSheetFiltering("dueDate.lessThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.lessThanOrEqual=" + SMALLER_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByDueDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where dueDate is less than
        defaultActionSheetFiltering("dueDate.lessThan=" + UPDATED_DUE_DATE, "dueDate.lessThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByDueDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where dueDate is greater than
        defaultActionSheetFiltering("dueDate.greaterThan=" + SMALLER_DUE_DATE, "dueDate.greaterThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByObservationEndDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where observationEndDate equals to
        defaultActionSheetFiltering(
            "observationEndDate.equals=" + DEFAULT_OBSERVATION_END_DATE,
            "observationEndDate.equals=" + UPDATED_OBSERVATION_END_DATE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByObservationEndDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where observationEndDate in
        defaultActionSheetFiltering(
            "observationEndDate.in=" + DEFAULT_OBSERVATION_END_DATE + "," + UPDATED_OBSERVATION_END_DATE,
            "observationEndDate.in=" + UPDATED_OBSERVATION_END_DATE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByObservationEndDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where observationEndDate is not null
        defaultActionSheetFiltering("observationEndDate.specified=true", "observationEndDate.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByObservationEndDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where observationEndDate is greater than or equal to
        defaultActionSheetFiltering(
            "observationEndDate.greaterThanOrEqual=" + DEFAULT_OBSERVATION_END_DATE,
            "observationEndDate.greaterThanOrEqual=" + UPDATED_OBSERVATION_END_DATE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByObservationEndDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where observationEndDate is less than or equal to
        defaultActionSheetFiltering(
            "observationEndDate.lessThanOrEqual=" + DEFAULT_OBSERVATION_END_DATE,
            "observationEndDate.lessThanOrEqual=" + SMALLER_OBSERVATION_END_DATE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByObservationEndDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where observationEndDate is less than
        defaultActionSheetFiltering(
            "observationEndDate.lessThan=" + UPDATED_OBSERVATION_END_DATE,
            "observationEndDate.lessThan=" + DEFAULT_OBSERVATION_END_DATE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByObservationEndDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where observationEndDate is greater than
        defaultActionSheetFiltering(
            "observationEndDate.greaterThan=" + SMALLER_OBSERVATION_END_DATE,
            "observationEndDate.greaterThan=" + DEFAULT_OBSERVATION_END_DATE
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByResultIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where result equals to
        defaultActionSheetFiltering("result.equals=" + DEFAULT_RESULT, "result.equals=" + UPDATED_RESULT);
    }

    @Test
    @Transactional
    void getAllActionSheetsByResultIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where result in
        defaultActionSheetFiltering("result.in=" + DEFAULT_RESULT + "," + UPDATED_RESULT, "result.in=" + UPDATED_RESULT);
    }

    @Test
    @Transactional
    void getAllActionSheetsByResultIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where result is not null
        defaultActionSheetFiltering("result.specified=true", "result.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByResultContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where result contains
        defaultActionSheetFiltering("result.contains=" + DEFAULT_RESULT, "result.contains=" + UPDATED_RESULT);
    }

    @Test
    @Transactional
    void getAllActionSheetsByResultNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where result does not contain
        defaultActionSheetFiltering("result.doesNotContain=" + UPDATED_RESULT, "result.doesNotContain=" + DEFAULT_RESULT);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCycleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where cycle equals to
        defaultActionSheetFiltering("cycle.equals=" + DEFAULT_CYCLE, "cycle.equals=" + UPDATED_CYCLE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCycleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where cycle in
        defaultActionSheetFiltering("cycle.in=" + DEFAULT_CYCLE + "," + UPDATED_CYCLE, "cycle.in=" + UPDATED_CYCLE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCycleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where cycle is not null
        defaultActionSheetFiltering("cycle.specified=true", "cycle.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByCycleIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where cycle is greater than or equal to
        defaultActionSheetFiltering("cycle.greaterThanOrEqual=" + DEFAULT_CYCLE, "cycle.greaterThanOrEqual=" + UPDATED_CYCLE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCycleIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where cycle is less than or equal to
        defaultActionSheetFiltering("cycle.lessThanOrEqual=" + DEFAULT_CYCLE, "cycle.lessThanOrEqual=" + SMALLER_CYCLE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCycleIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where cycle is less than
        defaultActionSheetFiltering("cycle.lessThan=" + UPDATED_CYCLE, "cycle.lessThan=" + DEFAULT_CYCLE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByCycleIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where cycle is greater than
        defaultActionSheetFiltering("cycle.greaterThan=" + SMALLER_CYCLE, "cycle.greaterThan=" + DEFAULT_CYCLE);
    }

    @Test
    @Transactional
    void getAllActionSheetsByProcedureDocumentRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where procedureDocumentRef equals to
        defaultActionSheetFiltering(
            "procedureDocumentRef.equals=" + DEFAULT_PROCEDURE_DOCUMENT_REF,
            "procedureDocumentRef.equals=" + UPDATED_PROCEDURE_DOCUMENT_REF
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByProcedureDocumentRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where procedureDocumentRef in
        defaultActionSheetFiltering(
            "procedureDocumentRef.in=" + DEFAULT_PROCEDURE_DOCUMENT_REF + "," + UPDATED_PROCEDURE_DOCUMENT_REF,
            "procedureDocumentRef.in=" + UPDATED_PROCEDURE_DOCUMENT_REF
        );
    }

    @Test
    @Transactional
    void getAllActionSheetsByProcedureDocumentRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where procedureDocumentRef is not null
        defaultActionSheetFiltering("procedureDocumentRef.specified=true", "procedureDocumentRef.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByClosedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where closedAt equals to
        defaultActionSheetFiltering("closedAt.equals=" + DEFAULT_CLOSED_AT, "closedAt.equals=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllActionSheetsByClosedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where closedAt in
        defaultActionSheetFiltering("closedAt.in=" + DEFAULT_CLOSED_AT + "," + UPDATED_CLOSED_AT, "closedAt.in=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllActionSheetsByClosedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        // Get all the actionSheetList where closedAt is not null
        defaultActionSheetFiltering("closedAt.specified=true", "closedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllActionSheetsByRiskIsEqualToSomething() throws Exception {
        Risk risk;
        if (TestUtil.findAll(em, Risk.class).isEmpty()) {
            actionSheetRepository.saveAndFlush(actionSheet);
            risk = RiskResourceIT.createEntity();
        } else {
            risk = TestUtil.findAll(em, Risk.class).get(0);
        }
        em.persist(risk);
        em.flush();
        actionSheet.setRisk(risk);
        actionSheetRepository.saveAndFlush(actionSheet);
        UUID riskId = risk.getId();
        // Get all the actionSheetList where risk equals to riskId
        defaultActionSheetShouldBeFound("riskId.equals=" + riskId);

        // Get all the actionSheetList where risk equals to UUID.randomUUID()
        defaultActionSheetShouldNotBeFound("riskId.equals=" + UUID.randomUUID());
    }

    private void defaultActionSheetFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultActionSheetShouldBeFound(shouldBeFound);
        defaultActionSheetShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultActionSheetShouldBeFound(String filter) throws Exception {
        restActionSheetMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(actionSheet.getId().toString())))
            .andExpect(jsonPath("$.[*].originKind").value(hasItem(DEFAULT_ORIGIN_KIND.toString())))
            .andExpect(jsonPath("$.[*].originReference").value(hasItem(DEFAULT_ORIGIN_REFERENCE)))
            .andExpect(jsonPath("$.[*].originService").value(hasItem(DEFAULT_ORIGIN_SERVICE)))
            .andExpect(jsonPath("$.[*].phase").value(hasItem(DEFAULT_PHASE.toString())))
            .andExpect(jsonPath("$.[*].objective").value(hasItem(DEFAULT_OBJECTIVE)))
            .andExpect(jsonPath("$.[*].causeAnalysis").value(hasItem(DEFAULT_CAUSE_ANALYSIS)))
            .andExpect(jsonPath("$.[*].targetScore").value(hasItem(sameNumber(DEFAULT_TARGET_SCORE))))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].observationEndDate").value(hasItem(DEFAULT_OBSERVATION_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].result").value(hasItem(DEFAULT_RESULT)))
            .andExpect(jsonPath("$.[*].cycle").value(hasItem(DEFAULT_CYCLE)))
            .andExpect(jsonPath("$.[*].procedureDocumentRef").value(hasItem(DEFAULT_PROCEDURE_DOCUMENT_REF.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())));

        // Check, that the count call also returns 1
        restActionSheetMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultActionSheetShouldNotBeFound(String filter) throws Exception {
        restActionSheetMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restActionSheetMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingActionSheet() throws Exception {
        // Get the actionSheet
        restActionSheetMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingActionSheet() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the actionSheet
        ActionSheet updatedActionSheet = actionSheetRepository.findById(actionSheet.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedActionSheet are not directly saved in db
        em.detach(updatedActionSheet);
        updatedActionSheet
            .originKind(UPDATED_ORIGIN_KIND)
            .originReference(UPDATED_ORIGIN_REFERENCE)
            .originService(UPDATED_ORIGIN_SERVICE)
            .phase(UPDATED_PHASE)
            .objective(UPDATED_OBJECTIVE)
            .causeAnalysis(UPDATED_CAUSE_ANALYSIS)
            .targetScore(UPDATED_TARGET_SCORE)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .observationEndDate(UPDATED_OBSERVATION_END_DATE)
            .result(UPDATED_RESULT)
            .cycle(UPDATED_CYCLE)
            .procedureDocumentRef(UPDATED_PROCEDURE_DOCUMENT_REF)
            .closedAt(UPDATED_CLOSED_AT);
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(updatedActionSheet);

        restActionSheetMockMvc
            .perform(
                put(ENTITY_API_URL_ID, actionSheetDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isOk());

        // Validate the ActionSheet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedActionSheetToMatchAllProperties(updatedActionSheet);
    }

    @Test
    @Transactional
    void putNonExistingActionSheet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actionSheet.setId(UUID.randomUUID());

        // Create the ActionSheet
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restActionSheetMockMvc
            .perform(
                put(ENTITY_API_URL_ID, actionSheetDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ActionSheet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchActionSheet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actionSheet.setId(UUID.randomUUID());

        // Create the ActionSheet
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restActionSheetMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ActionSheet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamActionSheet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actionSheet.setId(UUID.randomUUID());

        // Create the ActionSheet
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restActionSheetMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actionSheetDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ActionSheet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateActionSheetWithPatch() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the actionSheet using partial update
        ActionSheet partialUpdatedActionSheet = new ActionSheet();
        partialUpdatedActionSheet.setId(actionSheet.getId());

        partialUpdatedActionSheet
            .originKind(UPDATED_ORIGIN_KIND)
            .originReference(UPDATED_ORIGIN_REFERENCE)
            .originService(UPDATED_ORIGIN_SERVICE)
            .objective(UPDATED_OBJECTIVE)
            .targetScore(UPDATED_TARGET_SCORE)
            .dueDate(UPDATED_DUE_DATE)
            .observationEndDate(UPDATED_OBSERVATION_END_DATE)
            .result(UPDATED_RESULT)
            .cycle(UPDATED_CYCLE)
            .closedAt(UPDATED_CLOSED_AT);

        restActionSheetMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedActionSheet.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedActionSheet))
            )
            .andExpect(status().isOk());

        // Validate the ActionSheet in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertActionSheetUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedActionSheet, actionSheet),
            getPersistedActionSheet(actionSheet)
        );
    }

    @Test
    @Transactional
    void fullUpdateActionSheetWithPatch() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the actionSheet using partial update
        ActionSheet partialUpdatedActionSheet = new ActionSheet();
        partialUpdatedActionSheet.setId(actionSheet.getId());

        partialUpdatedActionSheet
            .originKind(UPDATED_ORIGIN_KIND)
            .originReference(UPDATED_ORIGIN_REFERENCE)
            .originService(UPDATED_ORIGIN_SERVICE)
            .phase(UPDATED_PHASE)
            .objective(UPDATED_OBJECTIVE)
            .causeAnalysis(UPDATED_CAUSE_ANALYSIS)
            .targetScore(UPDATED_TARGET_SCORE)
            .ownerId(UPDATED_OWNER_ID)
            .dueDate(UPDATED_DUE_DATE)
            .observationEndDate(UPDATED_OBSERVATION_END_DATE)
            .result(UPDATED_RESULT)
            .cycle(UPDATED_CYCLE)
            .procedureDocumentRef(UPDATED_PROCEDURE_DOCUMENT_REF)
            .closedAt(UPDATED_CLOSED_AT);

        restActionSheetMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedActionSheet.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedActionSheet))
            )
            .andExpect(status().isOk());

        // Validate the ActionSheet in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertActionSheetUpdatableFieldsEquals(partialUpdatedActionSheet, getPersistedActionSheet(partialUpdatedActionSheet));
    }

    @Test
    @Transactional
    void patchNonExistingActionSheet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actionSheet.setId(UUID.randomUUID());

        // Create the ActionSheet
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restActionSheetMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, actionSheetDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ActionSheet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchActionSheet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actionSheet.setId(UUID.randomUUID());

        // Create the ActionSheet
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restActionSheetMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ActionSheet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamActionSheet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actionSheet.setId(UUID.randomUUID());

        // Create the ActionSheet
        ActionSheetDTO actionSheetDTO = actionSheetMapper.toDto(actionSheet);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restActionSheetMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(actionSheetDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ActionSheet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteActionSheet() throws Exception {
        // Initialize the database
        insertedActionSheet = actionSheetRepository.saveAndFlush(actionSheet);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the actionSheet
        restActionSheetMockMvc
            .perform(delete(ENTITY_API_URL_ID, actionSheet.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return actionSheetRepository.count();
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

    protected ActionSheet getPersistedActionSheet(ActionSheet actionSheet) {
        return actionSheetRepository.findById(actionSheet.getId()).orElseThrow();
    }

    protected void assertPersistedActionSheetToMatchAllProperties(ActionSheet expectedActionSheet) {
        assertActionSheetAllPropertiesEquals(expectedActionSheet, getPersistedActionSheet(expectedActionSheet));
    }

    protected void assertPersistedActionSheetToMatchUpdatableProperties(ActionSheet expectedActionSheet) {
        assertActionSheetAllUpdatablePropertiesEquals(expectedActionSheet, getPersistedActionSheet(expectedActionSheet));
    }
}
