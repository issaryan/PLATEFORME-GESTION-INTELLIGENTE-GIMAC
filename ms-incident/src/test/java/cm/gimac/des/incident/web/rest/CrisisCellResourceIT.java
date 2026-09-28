package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.CrisisCellAsserts.*;
import static cm.gimac.des.incident.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.domain.Incident;
import cm.gimac.des.incident.domain.enumeration.CrisisState;
import cm.gimac.des.incident.domain.enumeration.Severity;
import cm.gimac.des.incident.repository.CrisisCellRepository;
import cm.gimac.des.incident.service.dto.CrisisCellDTO;
import cm.gimac.des.incident.service.mapper.CrisisCellMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link CrisisCellResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CrisisCellResourceIT {

    private static final String DEFAULT_REFERENCE = "CRI-7982-011328";
    private static final String UPDATED_REFERENCE = "CRI-8978-99037";

    private static final Severity DEFAULT_LEVEL = Severity.S1;
    private static final Severity UPDATED_LEVEL = Severity.S2;

    private static final CrisisState DEFAULT_STATE = CrisisState.CONVOQUEE;
    private static final CrisisState UPDATED_STATE = CrisisState.CONSTITUEE;

    private static final Boolean DEFAULT_AUTOMATIC_ACTIVATION = false;
    private static final Boolean UPDATED_AUTOMATIC_ACTIVATION = true;

    private static final String DEFAULT_ACTIVATION_REASON = "AAAAAAAAAA";
    private static final String UPDATED_ACTIVATION_REASON = "BBBBBBBBBB";

    private static final UUID DEFAULT_CONVENED_BY = UUID.randomUUID();
    private static final UUID UPDATED_CONVENED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_CONVENED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CONVENED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_QUORUM_DEADLINE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_QUORUM_DEADLINE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_QUORUM_THRESHOLD = 1;
    private static final Integer UPDATED_QUORUM_THRESHOLD = 2;
    private static final Integer SMALLER_QUORUM_THRESHOLD = 1 - 1;

    private static final Instant DEFAULT_CONSTITUTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CONSTITUTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_ROOM_REF = "AAAAAAAAAA";
    private static final String UPDATED_ROOM_REF = "BBBBBBBBBB";

    private static final Instant DEFAULT_POST_INCIDENT_REPORT_DUE_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_POST_INCIDENT_REPORT_DUE_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_POST_INCIDENT_REPORT_REF = "AAAAAAAAAA";
    private static final String UPDATED_POST_INCIDENT_REPORT_REF = "BBBBBBBBBB";

    private static final String DEFAULT_POST_INCIDENT_REPORT_DIGEST = "9489ff743124b8cdb6a4598d93b42e7fc49695a6bfd9389a9eca1e15c977b3b9";
    private static final String UPDATED_POST_INCIDENT_REPORT_DIGEST = "7a845f74752bcd08a99fe2a82f18bf7969dc71f0358f560a7eb910a9e8216a7d";

    private static final UUID DEFAULT_POST_INCIDENT_REPORT_SIGNED_BY = UUID.randomUUID();
    private static final UUID UPDATED_POST_INCIDENT_REPORT_SIGNED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_POST_INCIDENT_REPORT_SIGNED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_POST_INCIDENT_REPORT_SIGNED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_CLOSED_BY = UUID.randomUUID();
    private static final UUID UPDATED_CLOSED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_ARCHIVE_REF = "AAAAAAAAAA";
    private static final String UPDATED_ARCHIVE_REF = "BBBBBBBBBB";

    private static final Instant DEFAULT_ARCHIVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ARCHIVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/crisis-cells";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CrisisCellRepository crisisCellRepository;

    @Autowired
    private CrisisCellMapper crisisCellMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCrisisCellMockMvc;

    private CrisisCell crisisCell;

    private CrisisCell insertedCrisisCell;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CrisisCell createEntity(EntityManager em) {
        CrisisCell crisisCell = new CrisisCell()
            .reference(DEFAULT_REFERENCE)
            .level(DEFAULT_LEVEL)
            .state(DEFAULT_STATE)
            .automaticActivation(DEFAULT_AUTOMATIC_ACTIVATION)
            .activationReason(DEFAULT_ACTIVATION_REASON)
            .convenedBy(DEFAULT_CONVENED_BY)
            .convenedAt(DEFAULT_CONVENED_AT)
            .quorumDeadline(DEFAULT_QUORUM_DEADLINE)
            .quorumThreshold(DEFAULT_QUORUM_THRESHOLD)
            .constitutedAt(DEFAULT_CONSTITUTED_AT)
            .roomRef(DEFAULT_ROOM_REF)
            .postIncidentReportDueAt(DEFAULT_POST_INCIDENT_REPORT_DUE_AT)
            .postIncidentReportRef(DEFAULT_POST_INCIDENT_REPORT_REF)
            .postIncidentReportDigest(DEFAULT_POST_INCIDENT_REPORT_DIGEST)
            .postIncidentReportSignedBy(DEFAULT_POST_INCIDENT_REPORT_SIGNED_BY)
            .postIncidentReportSignedAt(DEFAULT_POST_INCIDENT_REPORT_SIGNED_AT)
            .closedBy(DEFAULT_CLOSED_BY)
            .closedAt(DEFAULT_CLOSED_AT)
            .archiveRef(DEFAULT_ARCHIVE_REF)
            .archivedAt(DEFAULT_ARCHIVED_AT);
        // Add required entity
        Incident incident;
        if (TestUtil.findAll(em, Incident.class).isEmpty()) {
            incident = IncidentResourceIT.createEntity(em);
            em.persist(incident);
            em.flush();
        } else {
            incident = TestUtil.findAll(em, Incident.class).get(0);
        }
        crisisCell.setIncident(incident);
        return crisisCell;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CrisisCell createUpdatedEntity(EntityManager em) {
        CrisisCell updatedCrisisCell = new CrisisCell()
            .reference(UPDATED_REFERENCE)
            .level(UPDATED_LEVEL)
            .state(UPDATED_STATE)
            .automaticActivation(UPDATED_AUTOMATIC_ACTIVATION)
            .activationReason(UPDATED_ACTIVATION_REASON)
            .convenedBy(UPDATED_CONVENED_BY)
            .convenedAt(UPDATED_CONVENED_AT)
            .quorumDeadline(UPDATED_QUORUM_DEADLINE)
            .quorumThreshold(UPDATED_QUORUM_THRESHOLD)
            .constitutedAt(UPDATED_CONSTITUTED_AT)
            .roomRef(UPDATED_ROOM_REF)
            .postIncidentReportDueAt(UPDATED_POST_INCIDENT_REPORT_DUE_AT)
            .postIncidentReportRef(UPDATED_POST_INCIDENT_REPORT_REF)
            .postIncidentReportDigest(UPDATED_POST_INCIDENT_REPORT_DIGEST)
            .postIncidentReportSignedBy(UPDATED_POST_INCIDENT_REPORT_SIGNED_BY)
            .postIncidentReportSignedAt(UPDATED_POST_INCIDENT_REPORT_SIGNED_AT)
            .closedBy(UPDATED_CLOSED_BY)
            .closedAt(UPDATED_CLOSED_AT)
            .archiveRef(UPDATED_ARCHIVE_REF)
            .archivedAt(UPDATED_ARCHIVED_AT);
        // Add required entity
        Incident incident;
        if (TestUtil.findAll(em, Incident.class).isEmpty()) {
            incident = IncidentResourceIT.createUpdatedEntity(em);
            em.persist(incident);
            em.flush();
        } else {
            incident = TestUtil.findAll(em, Incident.class).get(0);
        }
        updatedCrisisCell.setIncident(incident);
        return updatedCrisisCell;
    }

    @BeforeEach
    void initTest() {
        crisisCell = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCrisisCell != null) {
            crisisCellRepository.delete(insertedCrisisCell);
            insertedCrisisCell = null;
        }
    }

    @Test
    @Transactional
    void createCrisisCell() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the CrisisCell
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);
        var returnedCrisisCellDTO = om.readValue(
            restCrisisCellMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CrisisCellDTO.class
        );

        // Validate the CrisisCell in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCrisisCell = crisisCellMapper.toEntity(returnedCrisisCellDTO);
        assertCrisisCellUpdatableFieldsEquals(returnedCrisisCell, getPersistedCrisisCell(returnedCrisisCell));

        insertedCrisisCell = returnedCrisisCell;
    }

    @Test
    @Transactional
    void createCrisisCellWithExistingId() throws Exception {
        // Create the CrisisCell with an existing ID
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCrisisCellMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isBadRequest());

        // Validate the CrisisCell in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkReferenceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        crisisCell.setReference(null);

        // Create the CrisisCell, which fails.
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        restCrisisCellMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLevelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        crisisCell.setLevel(null);

        // Create the CrisisCell, which fails.
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        restCrisisCellMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        crisisCell.setState(null);

        // Create the CrisisCell, which fails.
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        restCrisisCellMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAutomaticActivationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        crisisCell.setAutomaticActivation(null);

        // Create the CrisisCell, which fails.
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        restCrisisCellMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkConvenedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        crisisCell.setConvenedAt(null);

        // Create the CrisisCell, which fails.
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        restCrisisCellMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkQuorumDeadlineIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        crisisCell.setQuorumDeadline(null);

        // Create the CrisisCell, which fails.
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        restCrisisCellMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkQuorumThresholdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        crisisCell.setQuorumThreshold(null);

        // Create the CrisisCell, which fails.
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        restCrisisCellMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCrisisCells() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList
        restCrisisCellMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(crisisCell.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].automaticActivation").value(hasItem(DEFAULT_AUTOMATIC_ACTIVATION)))
            .andExpect(jsonPath("$.[*].activationReason").value(hasItem(DEFAULT_ACTIVATION_REASON)))
            .andExpect(jsonPath("$.[*].convenedBy").value(hasItem(DEFAULT_CONVENED_BY.toString())))
            .andExpect(jsonPath("$.[*].convenedAt").value(hasItem(DEFAULT_CONVENED_AT.toString())))
            .andExpect(jsonPath("$.[*].quorumDeadline").value(hasItem(DEFAULT_QUORUM_DEADLINE.toString())))
            .andExpect(jsonPath("$.[*].quorumThreshold").value(hasItem(DEFAULT_QUORUM_THRESHOLD)))
            .andExpect(jsonPath("$.[*].constitutedAt").value(hasItem(DEFAULT_CONSTITUTED_AT.toString())))
            .andExpect(jsonPath("$.[*].roomRef").value(hasItem(DEFAULT_ROOM_REF)))
            .andExpect(jsonPath("$.[*].postIncidentReportDueAt").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_DUE_AT.toString())))
            .andExpect(jsonPath("$.[*].postIncidentReportRef").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_REF)))
            .andExpect(jsonPath("$.[*].postIncidentReportDigest").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_DIGEST)))
            .andExpect(jsonPath("$.[*].postIncidentReportSignedBy").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_SIGNED_BY.toString())))
            .andExpect(jsonPath("$.[*].postIncidentReportSignedAt").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_SIGNED_AT.toString())))
            .andExpect(jsonPath("$.[*].closedBy").value(hasItem(DEFAULT_CLOSED_BY.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].archiveRef").value(hasItem(DEFAULT_ARCHIVE_REF)))
            .andExpect(jsonPath("$.[*].archivedAt").value(hasItem(DEFAULT_ARCHIVED_AT.toString())));
    }

    @Test
    @Transactional
    void getCrisisCell() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get the crisisCell
        restCrisisCellMockMvc
            .perform(get(ENTITY_API_URL_ID, crisisCell.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(crisisCell.getId().toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.level").value(DEFAULT_LEVEL.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.automaticActivation").value(DEFAULT_AUTOMATIC_ACTIVATION))
            .andExpect(jsonPath("$.activationReason").value(DEFAULT_ACTIVATION_REASON))
            .andExpect(jsonPath("$.convenedBy").value(DEFAULT_CONVENED_BY.toString()))
            .andExpect(jsonPath("$.convenedAt").value(DEFAULT_CONVENED_AT.toString()))
            .andExpect(jsonPath("$.quorumDeadline").value(DEFAULT_QUORUM_DEADLINE.toString()))
            .andExpect(jsonPath("$.quorumThreshold").value(DEFAULT_QUORUM_THRESHOLD))
            .andExpect(jsonPath("$.constitutedAt").value(DEFAULT_CONSTITUTED_AT.toString()))
            .andExpect(jsonPath("$.roomRef").value(DEFAULT_ROOM_REF))
            .andExpect(jsonPath("$.postIncidentReportDueAt").value(DEFAULT_POST_INCIDENT_REPORT_DUE_AT.toString()))
            .andExpect(jsonPath("$.postIncidentReportRef").value(DEFAULT_POST_INCIDENT_REPORT_REF))
            .andExpect(jsonPath("$.postIncidentReportDigest").value(DEFAULT_POST_INCIDENT_REPORT_DIGEST))
            .andExpect(jsonPath("$.postIncidentReportSignedBy").value(DEFAULT_POST_INCIDENT_REPORT_SIGNED_BY.toString()))
            .andExpect(jsonPath("$.postIncidentReportSignedAt").value(DEFAULT_POST_INCIDENT_REPORT_SIGNED_AT.toString()))
            .andExpect(jsonPath("$.closedBy").value(DEFAULT_CLOSED_BY.toString()))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()))
            .andExpect(jsonPath("$.archiveRef").value(DEFAULT_ARCHIVE_REF))
            .andExpect(jsonPath("$.archivedAt").value(DEFAULT_ARCHIVED_AT.toString()));
    }

    @Test
    @Transactional
    void getCrisisCellsByIdFiltering() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        UUID id = crisisCell.getId();

        defaultCrisisCellFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where reference equals to
        defaultCrisisCellFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where reference in
        defaultCrisisCellFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where reference is not null
        defaultCrisisCellFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where reference contains
        defaultCrisisCellFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where reference does not contain
        defaultCrisisCellFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByLevelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where level equals to
        defaultCrisisCellFiltering("level.equals=" + DEFAULT_LEVEL, "level.equals=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByLevelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where level in
        defaultCrisisCellFiltering("level.in=" + DEFAULT_LEVEL + "," + UPDATED_LEVEL, "level.in=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByLevelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where level is not null
        defaultCrisisCellFiltering("level.specified=true", "level.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where state equals to
        defaultCrisisCellFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where state in
        defaultCrisisCellFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where state is not null
        defaultCrisisCellFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByAutomaticActivationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where automaticActivation equals to
        defaultCrisisCellFiltering(
            "automaticActivation.equals=" + DEFAULT_AUTOMATIC_ACTIVATION,
            "automaticActivation.equals=" + UPDATED_AUTOMATIC_ACTIVATION
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByAutomaticActivationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where automaticActivation in
        defaultCrisisCellFiltering(
            "automaticActivation.in=" + DEFAULT_AUTOMATIC_ACTIVATION + "," + UPDATED_AUTOMATIC_ACTIVATION,
            "automaticActivation.in=" + UPDATED_AUTOMATIC_ACTIVATION
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByAutomaticActivationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where automaticActivation is not null
        defaultCrisisCellFiltering("automaticActivation.specified=true", "automaticActivation.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByActivationReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where activationReason equals to
        defaultCrisisCellFiltering(
            "activationReason.equals=" + DEFAULT_ACTIVATION_REASON,
            "activationReason.equals=" + UPDATED_ACTIVATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByActivationReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where activationReason in
        defaultCrisisCellFiltering(
            "activationReason.in=" + DEFAULT_ACTIVATION_REASON + "," + UPDATED_ACTIVATION_REASON,
            "activationReason.in=" + UPDATED_ACTIVATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByActivationReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where activationReason is not null
        defaultCrisisCellFiltering("activationReason.specified=true", "activationReason.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByActivationReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where activationReason contains
        defaultCrisisCellFiltering(
            "activationReason.contains=" + DEFAULT_ACTIVATION_REASON,
            "activationReason.contains=" + UPDATED_ACTIVATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByActivationReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where activationReason does not contain
        defaultCrisisCellFiltering(
            "activationReason.doesNotContain=" + UPDATED_ACTIVATION_REASON,
            "activationReason.doesNotContain=" + DEFAULT_ACTIVATION_REASON
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConvenedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where convenedBy equals to
        defaultCrisisCellFiltering("convenedBy.equals=" + DEFAULT_CONVENED_BY, "convenedBy.equals=" + UPDATED_CONVENED_BY);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConvenedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where convenedBy in
        defaultCrisisCellFiltering(
            "convenedBy.in=" + DEFAULT_CONVENED_BY + "," + UPDATED_CONVENED_BY,
            "convenedBy.in=" + UPDATED_CONVENED_BY
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConvenedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where convenedBy is not null
        defaultCrisisCellFiltering("convenedBy.specified=true", "convenedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConvenedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where convenedAt equals to
        defaultCrisisCellFiltering("convenedAt.equals=" + DEFAULT_CONVENED_AT, "convenedAt.equals=" + UPDATED_CONVENED_AT);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConvenedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where convenedAt in
        defaultCrisisCellFiltering(
            "convenedAt.in=" + DEFAULT_CONVENED_AT + "," + UPDATED_CONVENED_AT,
            "convenedAt.in=" + UPDATED_CONVENED_AT
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConvenedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where convenedAt is not null
        defaultCrisisCellFiltering("convenedAt.specified=true", "convenedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumDeadlineIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumDeadline equals to
        defaultCrisisCellFiltering("quorumDeadline.equals=" + DEFAULT_QUORUM_DEADLINE, "quorumDeadline.equals=" + UPDATED_QUORUM_DEADLINE);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumDeadlineIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumDeadline in
        defaultCrisisCellFiltering(
            "quorumDeadline.in=" + DEFAULT_QUORUM_DEADLINE + "," + UPDATED_QUORUM_DEADLINE,
            "quorumDeadline.in=" + UPDATED_QUORUM_DEADLINE
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumDeadlineIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumDeadline is not null
        defaultCrisisCellFiltering("quorumDeadline.specified=true", "quorumDeadline.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumThresholdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumThreshold equals to
        defaultCrisisCellFiltering(
            "quorumThreshold.equals=" + DEFAULT_QUORUM_THRESHOLD,
            "quorumThreshold.equals=" + UPDATED_QUORUM_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumThresholdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumThreshold in
        defaultCrisisCellFiltering(
            "quorumThreshold.in=" + DEFAULT_QUORUM_THRESHOLD + "," + UPDATED_QUORUM_THRESHOLD,
            "quorumThreshold.in=" + UPDATED_QUORUM_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumThresholdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumThreshold is not null
        defaultCrisisCellFiltering("quorumThreshold.specified=true", "quorumThreshold.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumThresholdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumThreshold is greater than or equal to
        defaultCrisisCellFiltering(
            "quorumThreshold.greaterThanOrEqual=" + DEFAULT_QUORUM_THRESHOLD,
            "quorumThreshold.greaterThanOrEqual=" + UPDATED_QUORUM_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumThresholdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumThreshold is less than or equal to
        defaultCrisisCellFiltering(
            "quorumThreshold.lessThanOrEqual=" + DEFAULT_QUORUM_THRESHOLD,
            "quorumThreshold.lessThanOrEqual=" + SMALLER_QUORUM_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumThresholdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumThreshold is less than
        defaultCrisisCellFiltering(
            "quorumThreshold.lessThan=" + UPDATED_QUORUM_THRESHOLD,
            "quorumThreshold.lessThan=" + DEFAULT_QUORUM_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByQuorumThresholdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where quorumThreshold is greater than
        defaultCrisisCellFiltering(
            "quorumThreshold.greaterThan=" + SMALLER_QUORUM_THRESHOLD,
            "quorumThreshold.greaterThan=" + DEFAULT_QUORUM_THRESHOLD
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConstitutedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where constitutedAt equals to
        defaultCrisisCellFiltering("constitutedAt.equals=" + DEFAULT_CONSTITUTED_AT, "constitutedAt.equals=" + UPDATED_CONSTITUTED_AT);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConstitutedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where constitutedAt in
        defaultCrisisCellFiltering(
            "constitutedAt.in=" + DEFAULT_CONSTITUTED_AT + "," + UPDATED_CONSTITUTED_AT,
            "constitutedAt.in=" + UPDATED_CONSTITUTED_AT
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByConstitutedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where constitutedAt is not null
        defaultCrisisCellFiltering("constitutedAt.specified=true", "constitutedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByRoomRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where roomRef equals to
        defaultCrisisCellFiltering("roomRef.equals=" + DEFAULT_ROOM_REF, "roomRef.equals=" + UPDATED_ROOM_REF);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByRoomRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where roomRef in
        defaultCrisisCellFiltering("roomRef.in=" + DEFAULT_ROOM_REF + "," + UPDATED_ROOM_REF, "roomRef.in=" + UPDATED_ROOM_REF);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByRoomRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where roomRef is not null
        defaultCrisisCellFiltering("roomRef.specified=true", "roomRef.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByRoomRefContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where roomRef contains
        defaultCrisisCellFiltering("roomRef.contains=" + DEFAULT_ROOM_REF, "roomRef.contains=" + UPDATED_ROOM_REF);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByRoomRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where roomRef does not contain
        defaultCrisisCellFiltering("roomRef.doesNotContain=" + UPDATED_ROOM_REF, "roomRef.doesNotContain=" + DEFAULT_ROOM_REF);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportDueAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportDueAt equals to
        defaultCrisisCellFiltering(
            "postIncidentReportDueAt.equals=" + DEFAULT_POST_INCIDENT_REPORT_DUE_AT,
            "postIncidentReportDueAt.equals=" + UPDATED_POST_INCIDENT_REPORT_DUE_AT
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportDueAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportDueAt in
        defaultCrisisCellFiltering(
            "postIncidentReportDueAt.in=" + DEFAULT_POST_INCIDENT_REPORT_DUE_AT + "," + UPDATED_POST_INCIDENT_REPORT_DUE_AT,
            "postIncidentReportDueAt.in=" + UPDATED_POST_INCIDENT_REPORT_DUE_AT
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportDueAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportDueAt is not null
        defaultCrisisCellFiltering("postIncidentReportDueAt.specified=true", "postIncidentReportDueAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportRef equals to
        defaultCrisisCellFiltering(
            "postIncidentReportRef.equals=" + DEFAULT_POST_INCIDENT_REPORT_REF,
            "postIncidentReportRef.equals=" + UPDATED_POST_INCIDENT_REPORT_REF
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportRef in
        defaultCrisisCellFiltering(
            "postIncidentReportRef.in=" + DEFAULT_POST_INCIDENT_REPORT_REF + "," + UPDATED_POST_INCIDENT_REPORT_REF,
            "postIncidentReportRef.in=" + UPDATED_POST_INCIDENT_REPORT_REF
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportRef is not null
        defaultCrisisCellFiltering("postIncidentReportRef.specified=true", "postIncidentReportRef.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportRefContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportRef contains
        defaultCrisisCellFiltering(
            "postIncidentReportRef.contains=" + DEFAULT_POST_INCIDENT_REPORT_REF,
            "postIncidentReportRef.contains=" + UPDATED_POST_INCIDENT_REPORT_REF
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportRef does not contain
        defaultCrisisCellFiltering(
            "postIncidentReportRef.doesNotContain=" + UPDATED_POST_INCIDENT_REPORT_REF,
            "postIncidentReportRef.doesNotContain=" + DEFAULT_POST_INCIDENT_REPORT_REF
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportDigest equals to
        defaultCrisisCellFiltering(
            "postIncidentReportDigest.equals=" + DEFAULT_POST_INCIDENT_REPORT_DIGEST,
            "postIncidentReportDigest.equals=" + UPDATED_POST_INCIDENT_REPORT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportDigest in
        defaultCrisisCellFiltering(
            "postIncidentReportDigest.in=" + DEFAULT_POST_INCIDENT_REPORT_DIGEST + "," + UPDATED_POST_INCIDENT_REPORT_DIGEST,
            "postIncidentReportDigest.in=" + UPDATED_POST_INCIDENT_REPORT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportDigest is not null
        defaultCrisisCellFiltering("postIncidentReportDigest.specified=true", "postIncidentReportDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportDigest contains
        defaultCrisisCellFiltering(
            "postIncidentReportDigest.contains=" + DEFAULT_POST_INCIDENT_REPORT_DIGEST,
            "postIncidentReportDigest.contains=" + UPDATED_POST_INCIDENT_REPORT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportDigest does not contain
        defaultCrisisCellFiltering(
            "postIncidentReportDigest.doesNotContain=" + UPDATED_POST_INCIDENT_REPORT_DIGEST,
            "postIncidentReportDigest.doesNotContain=" + DEFAULT_POST_INCIDENT_REPORT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportSignedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportSignedBy equals to
        defaultCrisisCellFiltering(
            "postIncidentReportSignedBy.equals=" + DEFAULT_POST_INCIDENT_REPORT_SIGNED_BY,
            "postIncidentReportSignedBy.equals=" + UPDATED_POST_INCIDENT_REPORT_SIGNED_BY
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportSignedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportSignedBy in
        defaultCrisisCellFiltering(
            "postIncidentReportSignedBy.in=" + DEFAULT_POST_INCIDENT_REPORT_SIGNED_BY + "," + UPDATED_POST_INCIDENT_REPORT_SIGNED_BY,
            "postIncidentReportSignedBy.in=" + UPDATED_POST_INCIDENT_REPORT_SIGNED_BY
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportSignedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportSignedBy is not null
        defaultCrisisCellFiltering("postIncidentReportSignedBy.specified=true", "postIncidentReportSignedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportSignedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportSignedAt equals to
        defaultCrisisCellFiltering(
            "postIncidentReportSignedAt.equals=" + DEFAULT_POST_INCIDENT_REPORT_SIGNED_AT,
            "postIncidentReportSignedAt.equals=" + UPDATED_POST_INCIDENT_REPORT_SIGNED_AT
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportSignedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportSignedAt in
        defaultCrisisCellFiltering(
            "postIncidentReportSignedAt.in=" + DEFAULT_POST_INCIDENT_REPORT_SIGNED_AT + "," + UPDATED_POST_INCIDENT_REPORT_SIGNED_AT,
            "postIncidentReportSignedAt.in=" + UPDATED_POST_INCIDENT_REPORT_SIGNED_AT
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByPostIncidentReportSignedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where postIncidentReportSignedAt is not null
        defaultCrisisCellFiltering("postIncidentReportSignedAt.specified=true", "postIncidentReportSignedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByClosedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where closedBy equals to
        defaultCrisisCellFiltering("closedBy.equals=" + DEFAULT_CLOSED_BY, "closedBy.equals=" + UPDATED_CLOSED_BY);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByClosedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where closedBy in
        defaultCrisisCellFiltering("closedBy.in=" + DEFAULT_CLOSED_BY + "," + UPDATED_CLOSED_BY, "closedBy.in=" + UPDATED_CLOSED_BY);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByClosedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where closedBy is not null
        defaultCrisisCellFiltering("closedBy.specified=true", "closedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByClosedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where closedAt equals to
        defaultCrisisCellFiltering("closedAt.equals=" + DEFAULT_CLOSED_AT, "closedAt.equals=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByClosedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where closedAt in
        defaultCrisisCellFiltering("closedAt.in=" + DEFAULT_CLOSED_AT + "," + UPDATED_CLOSED_AT, "closedAt.in=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByClosedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where closedAt is not null
        defaultCrisisCellFiltering("closedAt.specified=true", "closedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByArchiveRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where archiveRef equals to
        defaultCrisisCellFiltering("archiveRef.equals=" + DEFAULT_ARCHIVE_REF, "archiveRef.equals=" + UPDATED_ARCHIVE_REF);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByArchiveRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where archiveRef in
        defaultCrisisCellFiltering(
            "archiveRef.in=" + DEFAULT_ARCHIVE_REF + "," + UPDATED_ARCHIVE_REF,
            "archiveRef.in=" + UPDATED_ARCHIVE_REF
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByArchiveRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where archiveRef is not null
        defaultCrisisCellFiltering("archiveRef.specified=true", "archiveRef.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByArchiveRefContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where archiveRef contains
        defaultCrisisCellFiltering("archiveRef.contains=" + DEFAULT_ARCHIVE_REF, "archiveRef.contains=" + UPDATED_ARCHIVE_REF);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByArchiveRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where archiveRef does not contain
        defaultCrisisCellFiltering("archiveRef.doesNotContain=" + UPDATED_ARCHIVE_REF, "archiveRef.doesNotContain=" + DEFAULT_ARCHIVE_REF);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByArchivedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where archivedAt equals to
        defaultCrisisCellFiltering("archivedAt.equals=" + DEFAULT_ARCHIVED_AT, "archivedAt.equals=" + UPDATED_ARCHIVED_AT);
    }

    @Test
    @Transactional
    void getAllCrisisCellsByArchivedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where archivedAt in
        defaultCrisisCellFiltering(
            "archivedAt.in=" + DEFAULT_ARCHIVED_AT + "," + UPDATED_ARCHIVED_AT,
            "archivedAt.in=" + UPDATED_ARCHIVED_AT
        );
    }

    @Test
    @Transactional
    void getAllCrisisCellsByArchivedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        // Get all the crisisCellList where archivedAt is not null
        defaultCrisisCellFiltering("archivedAt.specified=true", "archivedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCrisisCellsByIncidentIsEqualToSomething() throws Exception {
        Incident incident;
        if (TestUtil.findAll(em, Incident.class).isEmpty()) {
            crisisCellRepository.saveAndFlush(crisisCell);
            incident = IncidentResourceIT.createEntity(em);
        } else {
            incident = TestUtil.findAll(em, Incident.class).get(0);
        }
        em.persist(incident);
        em.flush();
        crisisCell.setIncident(incident);
        crisisCellRepository.saveAndFlush(crisisCell);
        UUID incidentId = incident.getId();
        // Get all the crisisCellList where incident equals to incidentId
        defaultCrisisCellShouldBeFound("incidentId.equals=" + incidentId);

        // Get all the crisisCellList where incident equals to UUID.randomUUID()
        defaultCrisisCellShouldNotBeFound("incidentId.equals=" + UUID.randomUUID());
    }

    private void defaultCrisisCellFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCrisisCellShouldBeFound(shouldBeFound);
        defaultCrisisCellShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCrisisCellShouldBeFound(String filter) throws Exception {
        restCrisisCellMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(crisisCell.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].automaticActivation").value(hasItem(DEFAULT_AUTOMATIC_ACTIVATION)))
            .andExpect(jsonPath("$.[*].activationReason").value(hasItem(DEFAULT_ACTIVATION_REASON)))
            .andExpect(jsonPath("$.[*].convenedBy").value(hasItem(DEFAULT_CONVENED_BY.toString())))
            .andExpect(jsonPath("$.[*].convenedAt").value(hasItem(DEFAULT_CONVENED_AT.toString())))
            .andExpect(jsonPath("$.[*].quorumDeadline").value(hasItem(DEFAULT_QUORUM_DEADLINE.toString())))
            .andExpect(jsonPath("$.[*].quorumThreshold").value(hasItem(DEFAULT_QUORUM_THRESHOLD)))
            .andExpect(jsonPath("$.[*].constitutedAt").value(hasItem(DEFAULT_CONSTITUTED_AT.toString())))
            .andExpect(jsonPath("$.[*].roomRef").value(hasItem(DEFAULT_ROOM_REF)))
            .andExpect(jsonPath("$.[*].postIncidentReportDueAt").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_DUE_AT.toString())))
            .andExpect(jsonPath("$.[*].postIncidentReportRef").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_REF)))
            .andExpect(jsonPath("$.[*].postIncidentReportDigest").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_DIGEST)))
            .andExpect(jsonPath("$.[*].postIncidentReportSignedBy").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_SIGNED_BY.toString())))
            .andExpect(jsonPath("$.[*].postIncidentReportSignedAt").value(hasItem(DEFAULT_POST_INCIDENT_REPORT_SIGNED_AT.toString())))
            .andExpect(jsonPath("$.[*].closedBy").value(hasItem(DEFAULT_CLOSED_BY.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].archiveRef").value(hasItem(DEFAULT_ARCHIVE_REF)))
            .andExpect(jsonPath("$.[*].archivedAt").value(hasItem(DEFAULT_ARCHIVED_AT.toString())));

        // Check, that the count call also returns 1
        restCrisisCellMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCrisisCellShouldNotBeFound(String filter) throws Exception {
        restCrisisCellMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCrisisCellMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCrisisCell() throws Exception {
        // Get the crisisCell
        restCrisisCellMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCrisisCell() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the crisisCell
        CrisisCell updatedCrisisCell = crisisCellRepository.findById(crisisCell.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCrisisCell are not directly saved in db
        em.detach(updatedCrisisCell);
        updatedCrisisCell
            .reference(UPDATED_REFERENCE)
            .level(UPDATED_LEVEL)
            .state(UPDATED_STATE)
            .automaticActivation(UPDATED_AUTOMATIC_ACTIVATION)
            .activationReason(UPDATED_ACTIVATION_REASON)
            .convenedBy(UPDATED_CONVENED_BY)
            .convenedAt(UPDATED_CONVENED_AT)
            .quorumDeadline(UPDATED_QUORUM_DEADLINE)
            .quorumThreshold(UPDATED_QUORUM_THRESHOLD)
            .constitutedAt(UPDATED_CONSTITUTED_AT)
            .roomRef(UPDATED_ROOM_REF)
            .postIncidentReportDueAt(UPDATED_POST_INCIDENT_REPORT_DUE_AT)
            .postIncidentReportRef(UPDATED_POST_INCIDENT_REPORT_REF)
            .postIncidentReportDigest(UPDATED_POST_INCIDENT_REPORT_DIGEST)
            .postIncidentReportSignedBy(UPDATED_POST_INCIDENT_REPORT_SIGNED_BY)
            .postIncidentReportSignedAt(UPDATED_POST_INCIDENT_REPORT_SIGNED_AT)
            .closedBy(UPDATED_CLOSED_BY)
            .closedAt(UPDATED_CLOSED_AT)
            .archiveRef(UPDATED_ARCHIVE_REF)
            .archivedAt(UPDATED_ARCHIVED_AT);
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(updatedCrisisCell);

        restCrisisCellMockMvc
            .perform(
                put(ENTITY_API_URL_ID, crisisCellDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(crisisCellDTO))
            )
            .andExpect(status().isOk());

        // Validate the CrisisCell in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCrisisCellToMatchAllProperties(updatedCrisisCell);
    }

    @Test
    @Transactional
    void putNonExistingCrisisCell() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        crisisCell.setId(UUID.randomUUID());

        // Create the CrisisCell
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCrisisCellMockMvc
            .perform(
                put(ENTITY_API_URL_ID, crisisCellDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(crisisCellDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CrisisCell in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCrisisCell() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        crisisCell.setId(UUID.randomUUID());

        // Create the CrisisCell
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCrisisCellMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(crisisCellDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CrisisCell in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCrisisCell() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        crisisCell.setId(UUID.randomUUID());

        // Create the CrisisCell
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCrisisCellMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(crisisCellDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the CrisisCell in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCrisisCellWithPatch() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the crisisCell using partial update
        CrisisCell partialUpdatedCrisisCell = new CrisisCell();
        partialUpdatedCrisisCell.setId(crisisCell.getId());

        partialUpdatedCrisisCell
            .reference(UPDATED_REFERENCE)
            .state(UPDATED_STATE)
            .activationReason(UPDATED_ACTIVATION_REASON)
            .convenedBy(UPDATED_CONVENED_BY)
            .quorumDeadline(UPDATED_QUORUM_DEADLINE)
            .postIncidentReportRef(UPDATED_POST_INCIDENT_REPORT_REF)
            .postIncidentReportDigest(UPDATED_POST_INCIDENT_REPORT_DIGEST)
            .postIncidentReportSignedBy(UPDATED_POST_INCIDENT_REPORT_SIGNED_BY)
            .closedBy(UPDATED_CLOSED_BY)
            .closedAt(UPDATED_CLOSED_AT)
            .archiveRef(UPDATED_ARCHIVE_REF);

        restCrisisCellMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCrisisCell.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCrisisCell))
            )
            .andExpect(status().isOk());

        // Validate the CrisisCell in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCrisisCellUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCrisisCell, crisisCell),
            getPersistedCrisisCell(crisisCell)
        );
    }

    @Test
    @Transactional
    void fullUpdateCrisisCellWithPatch() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the crisisCell using partial update
        CrisisCell partialUpdatedCrisisCell = new CrisisCell();
        partialUpdatedCrisisCell.setId(crisisCell.getId());

        partialUpdatedCrisisCell
            .reference(UPDATED_REFERENCE)
            .level(UPDATED_LEVEL)
            .state(UPDATED_STATE)
            .automaticActivation(UPDATED_AUTOMATIC_ACTIVATION)
            .activationReason(UPDATED_ACTIVATION_REASON)
            .convenedBy(UPDATED_CONVENED_BY)
            .convenedAt(UPDATED_CONVENED_AT)
            .quorumDeadline(UPDATED_QUORUM_DEADLINE)
            .quorumThreshold(UPDATED_QUORUM_THRESHOLD)
            .constitutedAt(UPDATED_CONSTITUTED_AT)
            .roomRef(UPDATED_ROOM_REF)
            .postIncidentReportDueAt(UPDATED_POST_INCIDENT_REPORT_DUE_AT)
            .postIncidentReportRef(UPDATED_POST_INCIDENT_REPORT_REF)
            .postIncidentReportDigest(UPDATED_POST_INCIDENT_REPORT_DIGEST)
            .postIncidentReportSignedBy(UPDATED_POST_INCIDENT_REPORT_SIGNED_BY)
            .postIncidentReportSignedAt(UPDATED_POST_INCIDENT_REPORT_SIGNED_AT)
            .closedBy(UPDATED_CLOSED_BY)
            .closedAt(UPDATED_CLOSED_AT)
            .archiveRef(UPDATED_ARCHIVE_REF)
            .archivedAt(UPDATED_ARCHIVED_AT);

        restCrisisCellMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCrisisCell.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCrisisCell))
            )
            .andExpect(status().isOk());

        // Validate the CrisisCell in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCrisisCellUpdatableFieldsEquals(partialUpdatedCrisisCell, getPersistedCrisisCell(partialUpdatedCrisisCell));
    }

    @Test
    @Transactional
    void patchNonExistingCrisisCell() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        crisisCell.setId(UUID.randomUUID());

        // Create the CrisisCell
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCrisisCellMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, crisisCellDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(crisisCellDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CrisisCell in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCrisisCell() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        crisisCell.setId(UUID.randomUUID());

        // Create the CrisisCell
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCrisisCellMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(crisisCellDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CrisisCell in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCrisisCell() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        crisisCell.setId(UUID.randomUUID());

        // Create the CrisisCell
        CrisisCellDTO crisisCellDTO = crisisCellMapper.toDto(crisisCell);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCrisisCellMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(crisisCellDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CrisisCell in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCrisisCell() throws Exception {
        // Initialize the database
        insertedCrisisCell = crisisCellRepository.saveAndFlush(crisisCell);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the crisisCell
        restCrisisCellMockMvc
            .perform(delete(ENTITY_API_URL_ID, crisisCell.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return crisisCellRepository.count();
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

    protected CrisisCell getPersistedCrisisCell(CrisisCell crisisCell) {
        return crisisCellRepository.findById(crisisCell.getId()).orElseThrow();
    }

    protected void assertPersistedCrisisCellToMatchAllProperties(CrisisCell expectedCrisisCell) {
        assertCrisisCellAllPropertiesEquals(expectedCrisisCell, getPersistedCrisisCell(expectedCrisisCell));
    }

    protected void assertPersistedCrisisCellToMatchUpdatableProperties(CrisisCell expectedCrisisCell) {
        assertCrisisCellAllUpdatablePropertiesEquals(expectedCrisisCell, getPersistedCrisisCell(expectedCrisisCell));
    }
}
