package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.IncidentAsserts.*;
import static cm.gimac.des.incident.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.Incident;
import cm.gimac.des.incident.domain.QualificationMatrix;
import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.domain.enumeration.Severity;
import cm.gimac.des.incident.repository.IncidentRepository;
import cm.gimac.des.incident.service.IncidentService;
import cm.gimac.des.incident.service.dto.IncidentDTO;
import cm.gimac.des.incident.service.mapper.IncidentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
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
 * Integration tests for the {@link IncidentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class IncidentResourceIT {

    private static final Integer DEFAULT_IMPACT = 1;
    private static final Integer UPDATED_IMPACT = 2;
    private static final Integer SMALLER_IMPACT = 1 - 1;

    private static final Integer DEFAULT_URGENCY = 1;
    private static final Integer UPDATED_URGENCY = 2;
    private static final Integer SMALLER_URGENCY = 1 - 1;

    private static final Integer DEFAULT_SCORE = 1;
    private static final Integer UPDATED_SCORE = 2;
    private static final Integer SMALLER_SCORE = 1 - 1;

    private static final Severity DEFAULT_SEVERITY = Severity.S1;
    private static final Severity UPDATED_SEVERITY = Severity.S2;

    private static final Boolean DEFAULT_SECURITY_INCIDENT = false;
    private static final Boolean UPDATED_SECURITY_INCIDENT = true;

    private static final Boolean DEFAULT_DATA_COMPROMISE_SUSPECTED = false;
    private static final Boolean UPDATED_DATA_COMPROMISE_SUSPECTED = true;

    private static final Boolean DEFAULT_REGULATORY_FLAG = false;
    private static final Boolean UPDATED_REGULATORY_FLAG = true;

    private static final Instant DEFAULT_QUALIFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_QUALIFIED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_QUALIFIED_BY = UUID.randomUUID();
    private static final UUID UPDATED_QUALIFIED_BY = UUID.randomUUID();

    private static final String DEFAULT_ROOT_CAUSE = "AAAAAAAAAA";
    private static final String UPDATED_ROOT_CAUSE = "BBBBBBBBBB";

    private static final String DEFAULT_ROOT_CAUSE_CATEGORY = "AAAAAAAAAA";
    private static final String UPDATED_ROOT_CAUSE_CATEGORY = "BBBBBBBBBB";

    private static final String DEFAULT_ROOT_CAUSE_PROOF_REF = "AAAAAAAAAA";
    private static final String UPDATED_ROOT_CAUSE_PROOF_REF = "BBBBBBBBBB";

    private static final String DEFAULT_REMEDIATION_PLAN = "AAAAAAAAAA";
    private static final String UPDATED_REMEDIATION_PLAN = "BBBBBBBBBB";

    private static final String DEFAULT_RESOLUTION_TEST = "AAAAAAAAAA";
    private static final String UPDATED_RESOLUTION_TEST = "BBBBBBBBBB";

    private static final Boolean DEFAULT_RESOLUTION_TEST_PASSED = false;
    private static final Boolean UPDATED_RESOLUTION_TEST_PASSED = true;

    private static final UUID DEFAULT_RESOLUTION_TEST_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_RESOLUTION_TEST_AUTHOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_RESOLUTION_TEST_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RESOLUTION_TEST_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_RESOLUTION_TEST_PROOF_REF = "AAAAAAAAAA";
    private static final String UPDATED_RESOLUTION_TEST_PROOF_REF = "BBBBBBBBBB";

    private static final String DEFAULT_LESSONS_LEARNED_REF = "AAAAAAAAAA";
    private static final String UPDATED_LESSONS_LEARNED_REF = "BBBBBBBBBB";

    private static final String DEFAULT_LESSONS_LEARNED_WAIVER = "AAAAAAAAAA";
    private static final String UPDATED_LESSONS_LEARNED_WAIVER = "BBBBBBBBBB";

    private static final Instant DEFAULT_REPORT_DUE_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REPORT_DUE_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_REPORT_GENERATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REPORT_GENERATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_REPORT_REF = "AAAAAAAAAA";
    private static final String UPDATED_REPORT_REF = "BBBBBBBBBB";

    private static final String DEFAULT_REPORT_DIGEST = "38e4563b66eeda17a0ca1997d8132fb997e47b4e2e2a27969c74e5f728e6336d";
    private static final String UPDATED_REPORT_DIGEST = "4b3d2a3688300f553bba0cc1193533f2decfeeb25b180d3a857fc71747f2518f";

    private static final UUID DEFAULT_COMPLIANCE_VISA_BY = UUID.randomUUID();
    private static final UUID UPDATED_COMPLIANCE_VISA_BY = UUID.randomUUID();

    private static final Instant DEFAULT_COMPLIANCE_VISA_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPLIANCE_VISA_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_CRITERIA_VALIDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CRITERIA_VALIDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_VALIDATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_VALIDATOR_ID = UUID.randomUUID();

    private static final Duration DEFAULT_RAW_DURATION = Duration.ofHours(6);
    private static final Duration UPDATED_RAW_DURATION = Duration.ofHours(12);
    private static final Duration SMALLER_RAW_DURATION = Duration.ofHours(5);

    private static final Duration DEFAULT_FROZEN_DURATION = Duration.ofHours(6);
    private static final Duration UPDATED_FROZEN_DURATION = Duration.ofHours(12);
    private static final Duration SMALLER_FROZEN_DURATION = Duration.ofHours(5);

    private static final Duration DEFAULT_IMPUTABLE_DELAY = Duration.ofHours(6);
    private static final Duration UPDATED_IMPUTABLE_DELAY = Duration.ofHours(12);
    private static final Duration SMALLER_IMPUTABLE_DELAY = Duration.ofHours(5);

    private static final Duration DEFAULT_MTTR = Duration.ofHours(6);
    private static final Duration UPDATED_MTTR = Duration.ofHours(12);
    private static final Duration SMALLER_MTTR = Duration.ofHours(5);

    private static final Boolean DEFAULT_TAKE_CHARGE_MET = false;
    private static final Boolean UPDATED_TAKE_CHARGE_MET = true;

    private static final Boolean DEFAULT_RESTORATION_MET = false;
    private static final Boolean UPDATED_RESTORATION_MET = true;

    private static final String ENTITY_API_URL = "/api/incidents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentRepository incidentRepositoryMock;

    @Autowired
    private IncidentMapper incidentMapper;

    @Mock
    private IncidentService incidentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restIncidentMockMvc;

    private Incident incident;

    private Incident insertedIncident;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Incident createEntity(EntityManager em) {
        Incident incident = new Incident()
            .impact(DEFAULT_IMPACT)
            .urgency(DEFAULT_URGENCY)
            .score(DEFAULT_SCORE)
            .severity(DEFAULT_SEVERITY)
            .securityIncident(DEFAULT_SECURITY_INCIDENT)
            .dataCompromiseSuspected(DEFAULT_DATA_COMPROMISE_SUSPECTED)
            .regulatoryFlag(DEFAULT_REGULATORY_FLAG)
            .qualifiedAt(DEFAULT_QUALIFIED_AT)
            .qualifiedBy(DEFAULT_QUALIFIED_BY)
            .rootCause(DEFAULT_ROOT_CAUSE)
            .rootCauseCategory(DEFAULT_ROOT_CAUSE_CATEGORY)
            .rootCauseProofRef(DEFAULT_ROOT_CAUSE_PROOF_REF)
            .remediationPlan(DEFAULT_REMEDIATION_PLAN)
            .resolutionTest(DEFAULT_RESOLUTION_TEST)
            .resolutionTestPassed(DEFAULT_RESOLUTION_TEST_PASSED)
            .resolutionTestAuthorId(DEFAULT_RESOLUTION_TEST_AUTHOR_ID)
            .resolutionTestAt(DEFAULT_RESOLUTION_TEST_AT)
            .resolutionTestProofRef(DEFAULT_RESOLUTION_TEST_PROOF_REF)
            .lessonsLearnedRef(DEFAULT_LESSONS_LEARNED_REF)
            .lessonsLearnedWaiver(DEFAULT_LESSONS_LEARNED_WAIVER)
            .reportDueAt(DEFAULT_REPORT_DUE_AT)
            .reportGeneratedAt(DEFAULT_REPORT_GENERATED_AT)
            .reportRef(DEFAULT_REPORT_REF)
            .reportDigest(DEFAULT_REPORT_DIGEST)
            .complianceVisaBy(DEFAULT_COMPLIANCE_VISA_BY)
            .complianceVisaAt(DEFAULT_COMPLIANCE_VISA_AT)
            .criteriaValidatedAt(DEFAULT_CRITERIA_VALIDATED_AT)
            .validatorId(DEFAULT_VALIDATOR_ID)
            .rawDuration(DEFAULT_RAW_DURATION)
            .frozenDuration(DEFAULT_FROZEN_DURATION)
            .imputableDelay(DEFAULT_IMPUTABLE_DELAY)
            .mttr(DEFAULT_MTTR)
            .takeChargeMet(DEFAULT_TAKE_CHARGE_MET)
            .restorationMet(DEFAULT_RESTORATION_MET);
        // Add required entity
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            ticket = TicketResourceIT.createEntity();
            em.persist(ticket);
            em.flush();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        incident.setTicket(ticket);
        // Add required entity
        QualificationMatrix qualificationMatrix;
        if (TestUtil.findAll(em, QualificationMatrix.class).isEmpty()) {
            qualificationMatrix = QualificationMatrixResourceIT.createEntity();
            em.persist(qualificationMatrix);
            em.flush();
        } else {
            qualificationMatrix = TestUtil.findAll(em, QualificationMatrix.class).get(0);
        }
        incident.setMatrix(qualificationMatrix);
        return incident;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Incident createUpdatedEntity(EntityManager em) {
        Incident updatedIncident = new Incident()
            .impact(UPDATED_IMPACT)
            .urgency(UPDATED_URGENCY)
            .score(UPDATED_SCORE)
            .severity(UPDATED_SEVERITY)
            .securityIncident(UPDATED_SECURITY_INCIDENT)
            .dataCompromiseSuspected(UPDATED_DATA_COMPROMISE_SUSPECTED)
            .regulatoryFlag(UPDATED_REGULATORY_FLAG)
            .qualifiedAt(UPDATED_QUALIFIED_AT)
            .qualifiedBy(UPDATED_QUALIFIED_BY)
            .rootCause(UPDATED_ROOT_CAUSE)
            .rootCauseCategory(UPDATED_ROOT_CAUSE_CATEGORY)
            .rootCauseProofRef(UPDATED_ROOT_CAUSE_PROOF_REF)
            .remediationPlan(UPDATED_REMEDIATION_PLAN)
            .resolutionTest(UPDATED_RESOLUTION_TEST)
            .resolutionTestPassed(UPDATED_RESOLUTION_TEST_PASSED)
            .resolutionTestAuthorId(UPDATED_RESOLUTION_TEST_AUTHOR_ID)
            .resolutionTestAt(UPDATED_RESOLUTION_TEST_AT)
            .resolutionTestProofRef(UPDATED_RESOLUTION_TEST_PROOF_REF)
            .lessonsLearnedRef(UPDATED_LESSONS_LEARNED_REF)
            .lessonsLearnedWaiver(UPDATED_LESSONS_LEARNED_WAIVER)
            .reportDueAt(UPDATED_REPORT_DUE_AT)
            .reportGeneratedAt(UPDATED_REPORT_GENERATED_AT)
            .reportRef(UPDATED_REPORT_REF)
            .reportDigest(UPDATED_REPORT_DIGEST)
            .complianceVisaBy(UPDATED_COMPLIANCE_VISA_BY)
            .complianceVisaAt(UPDATED_COMPLIANCE_VISA_AT)
            .criteriaValidatedAt(UPDATED_CRITERIA_VALIDATED_AT)
            .validatorId(UPDATED_VALIDATOR_ID)
            .rawDuration(UPDATED_RAW_DURATION)
            .frozenDuration(UPDATED_FROZEN_DURATION)
            .imputableDelay(UPDATED_IMPUTABLE_DELAY)
            .mttr(UPDATED_MTTR)
            .takeChargeMet(UPDATED_TAKE_CHARGE_MET)
            .restorationMet(UPDATED_RESTORATION_MET);
        // Add required entity
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            ticket = TicketResourceIT.createUpdatedEntity();
            em.persist(ticket);
            em.flush();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        updatedIncident.setTicket(ticket);
        // Add required entity
        QualificationMatrix qualificationMatrix;
        if (TestUtil.findAll(em, QualificationMatrix.class).isEmpty()) {
            qualificationMatrix = QualificationMatrixResourceIT.createUpdatedEntity();
            em.persist(qualificationMatrix);
            em.flush();
        } else {
            qualificationMatrix = TestUtil.findAll(em, QualificationMatrix.class).get(0);
        }
        updatedIncident.setMatrix(qualificationMatrix);
        return updatedIncident;
    }

    @BeforeEach
    void initTest() {
        incident = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedIncident != null) {
            incidentRepository.delete(insertedIncident);
            insertedIncident = null;
        }
    }

    @Test
    @Transactional
    void createIncident() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Incident
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);
        var returnedIncidentDTO = om.readValue(
            restIncidentMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            IncidentDTO.class
        );

        // Validate the Incident in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedIncident = incidentMapper.toEntity(returnedIncidentDTO);
        assertIncidentUpdatableFieldsEquals(returnedIncident, getPersistedIncident(returnedIncident));

        insertedIncident = returnedIncident;
    }

    @Test
    @Transactional
    void createIncidentWithExistingId() throws Exception {
        // Create the Incident with an existing ID
        insertedIncident = incidentRepository.saveAndFlush(incident);
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Incident in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkImpactIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        incident.setImpact(null);

        // Create the Incident, which fails.
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUrgencyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        incident.setUrgency(null);

        // Create the Incident, which fails.
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkScoreIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        incident.setScore(null);

        // Create the Incident, which fails.
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSeverityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        incident.setSeverity(null);

        // Create the Incident, which fails.
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSecurityIncidentIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        incident.setSecurityIncident(null);

        // Create the Incident, which fails.
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRegulatoryFlagIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        incident.setRegulatoryFlag(null);

        // Create the Incident, which fails.
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkQualifiedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        incident.setQualifiedAt(null);

        // Create the Incident, which fails.
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkQualifiedByIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        incident.setQualifiedBy(null);

        // Create the Incident, which fails.
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        restIncidentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllIncidents() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList
        restIncidentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(incident.getId().toString())))
            .andExpect(jsonPath("$.[*].impact").value(hasItem(DEFAULT_IMPACT)))
            .andExpect(jsonPath("$.[*].urgency").value(hasItem(DEFAULT_URGENCY)))
            .andExpect(jsonPath("$.[*].score").value(hasItem(DEFAULT_SCORE)))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY.toString())))
            .andExpect(jsonPath("$.[*].securityIncident").value(hasItem(DEFAULT_SECURITY_INCIDENT)))
            .andExpect(jsonPath("$.[*].dataCompromiseSuspected").value(hasItem(DEFAULT_DATA_COMPROMISE_SUSPECTED)))
            .andExpect(jsonPath("$.[*].regulatoryFlag").value(hasItem(DEFAULT_REGULATORY_FLAG)))
            .andExpect(jsonPath("$.[*].qualifiedAt").value(hasItem(DEFAULT_QUALIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].qualifiedBy").value(hasItem(DEFAULT_QUALIFIED_BY.toString())))
            .andExpect(jsonPath("$.[*].rootCause").value(hasItem(DEFAULT_ROOT_CAUSE)))
            .andExpect(jsonPath("$.[*].rootCauseCategory").value(hasItem(DEFAULT_ROOT_CAUSE_CATEGORY)))
            .andExpect(jsonPath("$.[*].rootCauseProofRef").value(hasItem(DEFAULT_ROOT_CAUSE_PROOF_REF)))
            .andExpect(jsonPath("$.[*].remediationPlan").value(hasItem(DEFAULT_REMEDIATION_PLAN)))
            .andExpect(jsonPath("$.[*].resolutionTest").value(hasItem(DEFAULT_RESOLUTION_TEST)))
            .andExpect(jsonPath("$.[*].resolutionTestPassed").value(hasItem(DEFAULT_RESOLUTION_TEST_PASSED)))
            .andExpect(jsonPath("$.[*].resolutionTestAuthorId").value(hasItem(DEFAULT_RESOLUTION_TEST_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].resolutionTestAt").value(hasItem(DEFAULT_RESOLUTION_TEST_AT.toString())))
            .andExpect(jsonPath("$.[*].resolutionTestProofRef").value(hasItem(DEFAULT_RESOLUTION_TEST_PROOF_REF)))
            .andExpect(jsonPath("$.[*].lessonsLearnedRef").value(hasItem(DEFAULT_LESSONS_LEARNED_REF)))
            .andExpect(jsonPath("$.[*].lessonsLearnedWaiver").value(hasItem(DEFAULT_LESSONS_LEARNED_WAIVER)))
            .andExpect(jsonPath("$.[*].reportDueAt").value(hasItem(DEFAULT_REPORT_DUE_AT.toString())))
            .andExpect(jsonPath("$.[*].reportGeneratedAt").value(hasItem(DEFAULT_REPORT_GENERATED_AT.toString())))
            .andExpect(jsonPath("$.[*].reportRef").value(hasItem(DEFAULT_REPORT_REF)))
            .andExpect(jsonPath("$.[*].reportDigest").value(hasItem(DEFAULT_REPORT_DIGEST)))
            .andExpect(jsonPath("$.[*].complianceVisaBy").value(hasItem(DEFAULT_COMPLIANCE_VISA_BY.toString())))
            .andExpect(jsonPath("$.[*].complianceVisaAt").value(hasItem(DEFAULT_COMPLIANCE_VISA_AT.toString())))
            .andExpect(jsonPath("$.[*].criteriaValidatedAt").value(hasItem(DEFAULT_CRITERIA_VALIDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].rawDuration").value(hasItem(DEFAULT_RAW_DURATION.toString())))
            .andExpect(jsonPath("$.[*].frozenDuration").value(hasItem(DEFAULT_FROZEN_DURATION.toString())))
            .andExpect(jsonPath("$.[*].imputableDelay").value(hasItem(DEFAULT_IMPUTABLE_DELAY.toString())))
            .andExpect(jsonPath("$.[*].mttr").value(hasItem(DEFAULT_MTTR.toString())))
            .andExpect(jsonPath("$.[*].takeChargeMet").value(hasItem(DEFAULT_TAKE_CHARGE_MET)))
            .andExpect(jsonPath("$.[*].restorationMet").value(hasItem(DEFAULT_RESTORATION_MET)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllIncidentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(incidentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restIncidentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(incidentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllIncidentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(incidentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restIncidentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(incidentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getIncident() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get the incident
        restIncidentMockMvc
            .perform(get(ENTITY_API_URL_ID, incident.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(incident.getId().toString()))
            .andExpect(jsonPath("$.impact").value(DEFAULT_IMPACT))
            .andExpect(jsonPath("$.urgency").value(DEFAULT_URGENCY))
            .andExpect(jsonPath("$.score").value(DEFAULT_SCORE))
            .andExpect(jsonPath("$.severity").value(DEFAULT_SEVERITY.toString()))
            .andExpect(jsonPath("$.securityIncident").value(DEFAULT_SECURITY_INCIDENT))
            .andExpect(jsonPath("$.dataCompromiseSuspected").value(DEFAULT_DATA_COMPROMISE_SUSPECTED))
            .andExpect(jsonPath("$.regulatoryFlag").value(DEFAULT_REGULATORY_FLAG))
            .andExpect(jsonPath("$.qualifiedAt").value(DEFAULT_QUALIFIED_AT.toString()))
            .andExpect(jsonPath("$.qualifiedBy").value(DEFAULT_QUALIFIED_BY.toString()))
            .andExpect(jsonPath("$.rootCause").value(DEFAULT_ROOT_CAUSE))
            .andExpect(jsonPath("$.rootCauseCategory").value(DEFAULT_ROOT_CAUSE_CATEGORY))
            .andExpect(jsonPath("$.rootCauseProofRef").value(DEFAULT_ROOT_CAUSE_PROOF_REF))
            .andExpect(jsonPath("$.remediationPlan").value(DEFAULT_REMEDIATION_PLAN))
            .andExpect(jsonPath("$.resolutionTest").value(DEFAULT_RESOLUTION_TEST))
            .andExpect(jsonPath("$.resolutionTestPassed").value(DEFAULT_RESOLUTION_TEST_PASSED))
            .andExpect(jsonPath("$.resolutionTestAuthorId").value(DEFAULT_RESOLUTION_TEST_AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.resolutionTestAt").value(DEFAULT_RESOLUTION_TEST_AT.toString()))
            .andExpect(jsonPath("$.resolutionTestProofRef").value(DEFAULT_RESOLUTION_TEST_PROOF_REF))
            .andExpect(jsonPath("$.lessonsLearnedRef").value(DEFAULT_LESSONS_LEARNED_REF))
            .andExpect(jsonPath("$.lessonsLearnedWaiver").value(DEFAULT_LESSONS_LEARNED_WAIVER))
            .andExpect(jsonPath("$.reportDueAt").value(DEFAULT_REPORT_DUE_AT.toString()))
            .andExpect(jsonPath("$.reportGeneratedAt").value(DEFAULT_REPORT_GENERATED_AT.toString()))
            .andExpect(jsonPath("$.reportRef").value(DEFAULT_REPORT_REF))
            .andExpect(jsonPath("$.reportDigest").value(DEFAULT_REPORT_DIGEST))
            .andExpect(jsonPath("$.complianceVisaBy").value(DEFAULT_COMPLIANCE_VISA_BY.toString()))
            .andExpect(jsonPath("$.complianceVisaAt").value(DEFAULT_COMPLIANCE_VISA_AT.toString()))
            .andExpect(jsonPath("$.criteriaValidatedAt").value(DEFAULT_CRITERIA_VALIDATED_AT.toString()))
            .andExpect(jsonPath("$.validatorId").value(DEFAULT_VALIDATOR_ID.toString()))
            .andExpect(jsonPath("$.rawDuration").value(DEFAULT_RAW_DURATION.toString()))
            .andExpect(jsonPath("$.frozenDuration").value(DEFAULT_FROZEN_DURATION.toString()))
            .andExpect(jsonPath("$.imputableDelay").value(DEFAULT_IMPUTABLE_DELAY.toString()))
            .andExpect(jsonPath("$.mttr").value(DEFAULT_MTTR.toString()))
            .andExpect(jsonPath("$.takeChargeMet").value(DEFAULT_TAKE_CHARGE_MET))
            .andExpect(jsonPath("$.restorationMet").value(DEFAULT_RESTORATION_MET));
    }

    @Test
    @Transactional
    void getIncidentsByIdFiltering() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        UUID id = incident.getId();

        defaultIncidentFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllIncidentsByImpactIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where impact equals to
        defaultIncidentFiltering("impact.equals=" + DEFAULT_IMPACT, "impact.equals=" + UPDATED_IMPACT);
    }

    @Test
    @Transactional
    void getAllIncidentsByImpactIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where impact in
        defaultIncidentFiltering("impact.in=" + DEFAULT_IMPACT + "," + UPDATED_IMPACT, "impact.in=" + UPDATED_IMPACT);
    }

    @Test
    @Transactional
    void getAllIncidentsByImpactIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where impact is not null
        defaultIncidentFiltering("impact.specified=true", "impact.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByImpactIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where impact is greater than or equal to
        defaultIncidentFiltering("impact.greaterThanOrEqual=" + DEFAULT_IMPACT, "impact.greaterThanOrEqual=" + (DEFAULT_IMPACT + 1));
    }

    @Test
    @Transactional
    void getAllIncidentsByImpactIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where impact is less than or equal to
        defaultIncidentFiltering("impact.lessThanOrEqual=" + DEFAULT_IMPACT, "impact.lessThanOrEqual=" + SMALLER_IMPACT);
    }

    @Test
    @Transactional
    void getAllIncidentsByImpactIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where impact is less than
        defaultIncidentFiltering("impact.lessThan=" + (DEFAULT_IMPACT + 1), "impact.lessThan=" + DEFAULT_IMPACT);
    }

    @Test
    @Transactional
    void getAllIncidentsByImpactIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where impact is greater than
        defaultIncidentFiltering("impact.greaterThan=" + SMALLER_IMPACT, "impact.greaterThan=" + DEFAULT_IMPACT);
    }

    @Test
    @Transactional
    void getAllIncidentsByUrgencyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where urgency equals to
        defaultIncidentFiltering("urgency.equals=" + DEFAULT_URGENCY, "urgency.equals=" + UPDATED_URGENCY);
    }

    @Test
    @Transactional
    void getAllIncidentsByUrgencyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where urgency in
        defaultIncidentFiltering("urgency.in=" + DEFAULT_URGENCY + "," + UPDATED_URGENCY, "urgency.in=" + UPDATED_URGENCY);
    }

    @Test
    @Transactional
    void getAllIncidentsByUrgencyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where urgency is not null
        defaultIncidentFiltering("urgency.specified=true", "urgency.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByUrgencyIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where urgency is greater than or equal to
        defaultIncidentFiltering("urgency.greaterThanOrEqual=" + DEFAULT_URGENCY, "urgency.greaterThanOrEqual=" + (DEFAULT_URGENCY + 1));
    }

    @Test
    @Transactional
    void getAllIncidentsByUrgencyIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where urgency is less than or equal to
        defaultIncidentFiltering("urgency.lessThanOrEqual=" + DEFAULT_URGENCY, "urgency.lessThanOrEqual=" + SMALLER_URGENCY);
    }

    @Test
    @Transactional
    void getAllIncidentsByUrgencyIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where urgency is less than
        defaultIncidentFiltering("urgency.lessThan=" + (DEFAULT_URGENCY + 1), "urgency.lessThan=" + DEFAULT_URGENCY);
    }

    @Test
    @Transactional
    void getAllIncidentsByUrgencyIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where urgency is greater than
        defaultIncidentFiltering("urgency.greaterThan=" + SMALLER_URGENCY, "urgency.greaterThan=" + DEFAULT_URGENCY);
    }

    @Test
    @Transactional
    void getAllIncidentsByScoreIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where score equals to
        defaultIncidentFiltering("score.equals=" + DEFAULT_SCORE, "score.equals=" + UPDATED_SCORE);
    }

    @Test
    @Transactional
    void getAllIncidentsByScoreIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where score in
        defaultIncidentFiltering("score.in=" + DEFAULT_SCORE + "," + UPDATED_SCORE, "score.in=" + UPDATED_SCORE);
    }

    @Test
    @Transactional
    void getAllIncidentsByScoreIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where score is not null
        defaultIncidentFiltering("score.specified=true", "score.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByScoreIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where score is greater than or equal to
        defaultIncidentFiltering("score.greaterThanOrEqual=" + DEFAULT_SCORE, "score.greaterThanOrEqual=" + (DEFAULT_SCORE + 1));
    }

    @Test
    @Transactional
    void getAllIncidentsByScoreIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where score is less than or equal to
        defaultIncidentFiltering("score.lessThanOrEqual=" + DEFAULT_SCORE, "score.lessThanOrEqual=" + SMALLER_SCORE);
    }

    @Test
    @Transactional
    void getAllIncidentsByScoreIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where score is less than
        defaultIncidentFiltering("score.lessThan=" + (DEFAULT_SCORE + 1), "score.lessThan=" + DEFAULT_SCORE);
    }

    @Test
    @Transactional
    void getAllIncidentsByScoreIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where score is greater than
        defaultIncidentFiltering("score.greaterThan=" + SMALLER_SCORE, "score.greaterThan=" + DEFAULT_SCORE);
    }

    @Test
    @Transactional
    void getAllIncidentsBySeverityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where severity equals to
        defaultIncidentFiltering("severity.equals=" + DEFAULT_SEVERITY, "severity.equals=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllIncidentsBySeverityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where severity in
        defaultIncidentFiltering("severity.in=" + DEFAULT_SEVERITY + "," + UPDATED_SEVERITY, "severity.in=" + UPDATED_SEVERITY);
    }

    @Test
    @Transactional
    void getAllIncidentsBySeverityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where severity is not null
        defaultIncidentFiltering("severity.specified=true", "severity.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsBySecurityIncidentIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where securityIncident equals to
        defaultIncidentFiltering(
            "securityIncident.equals=" + DEFAULT_SECURITY_INCIDENT,
            "securityIncident.equals=" + UPDATED_SECURITY_INCIDENT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsBySecurityIncidentIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where securityIncident in
        defaultIncidentFiltering(
            "securityIncident.in=" + DEFAULT_SECURITY_INCIDENT + "," + UPDATED_SECURITY_INCIDENT,
            "securityIncident.in=" + UPDATED_SECURITY_INCIDENT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsBySecurityIncidentIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where securityIncident is not null
        defaultIncidentFiltering("securityIncident.specified=true", "securityIncident.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByDataCompromiseSuspectedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where dataCompromiseSuspected equals to
        defaultIncidentFiltering(
            "dataCompromiseSuspected.equals=" + DEFAULT_DATA_COMPROMISE_SUSPECTED,
            "dataCompromiseSuspected.equals=" + UPDATED_DATA_COMPROMISE_SUSPECTED
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByDataCompromiseSuspectedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where dataCompromiseSuspected in
        defaultIncidentFiltering(
            "dataCompromiseSuspected.in=" + DEFAULT_DATA_COMPROMISE_SUSPECTED + "," + UPDATED_DATA_COMPROMISE_SUSPECTED,
            "dataCompromiseSuspected.in=" + UPDATED_DATA_COMPROMISE_SUSPECTED
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByDataCompromiseSuspectedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where dataCompromiseSuspected is not null
        defaultIncidentFiltering("dataCompromiseSuspected.specified=true", "dataCompromiseSuspected.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByRegulatoryFlagIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where regulatoryFlag equals to
        defaultIncidentFiltering("regulatoryFlag.equals=" + DEFAULT_REGULATORY_FLAG, "regulatoryFlag.equals=" + UPDATED_REGULATORY_FLAG);
    }

    @Test
    @Transactional
    void getAllIncidentsByRegulatoryFlagIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where regulatoryFlag in
        defaultIncidentFiltering(
            "regulatoryFlag.in=" + DEFAULT_REGULATORY_FLAG + "," + UPDATED_REGULATORY_FLAG,
            "regulatoryFlag.in=" + UPDATED_REGULATORY_FLAG
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRegulatoryFlagIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where regulatoryFlag is not null
        defaultIncidentFiltering("regulatoryFlag.specified=true", "regulatoryFlag.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByQualifiedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where qualifiedAt equals to
        defaultIncidentFiltering("qualifiedAt.equals=" + DEFAULT_QUALIFIED_AT, "qualifiedAt.equals=" + UPDATED_QUALIFIED_AT);
    }

    @Test
    @Transactional
    void getAllIncidentsByQualifiedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where qualifiedAt in
        defaultIncidentFiltering(
            "qualifiedAt.in=" + DEFAULT_QUALIFIED_AT + "," + UPDATED_QUALIFIED_AT,
            "qualifiedAt.in=" + UPDATED_QUALIFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByQualifiedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where qualifiedAt is not null
        defaultIncidentFiltering("qualifiedAt.specified=true", "qualifiedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByQualifiedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where qualifiedBy equals to
        defaultIncidentFiltering("qualifiedBy.equals=" + DEFAULT_QUALIFIED_BY, "qualifiedBy.equals=" + UPDATED_QUALIFIED_BY);
    }

    @Test
    @Transactional
    void getAllIncidentsByQualifiedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where qualifiedBy in
        defaultIncidentFiltering(
            "qualifiedBy.in=" + DEFAULT_QUALIFIED_BY + "," + UPDATED_QUALIFIED_BY,
            "qualifiedBy.in=" + UPDATED_QUALIFIED_BY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByQualifiedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where qualifiedBy is not null
        defaultIncidentFiltering("qualifiedBy.specified=true", "qualifiedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCause equals to
        defaultIncidentFiltering("rootCause.equals=" + DEFAULT_ROOT_CAUSE, "rootCause.equals=" + UPDATED_ROOT_CAUSE);
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCause in
        defaultIncidentFiltering("rootCause.in=" + DEFAULT_ROOT_CAUSE + "," + UPDATED_ROOT_CAUSE, "rootCause.in=" + UPDATED_ROOT_CAUSE);
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCause is not null
        defaultIncidentFiltering("rootCause.specified=true", "rootCause.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCause contains
        defaultIncidentFiltering("rootCause.contains=" + DEFAULT_ROOT_CAUSE, "rootCause.contains=" + UPDATED_ROOT_CAUSE);
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCause does not contain
        defaultIncidentFiltering("rootCause.doesNotContain=" + UPDATED_ROOT_CAUSE, "rootCause.doesNotContain=" + DEFAULT_ROOT_CAUSE);
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseCategoryIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseCategory equals to
        defaultIncidentFiltering(
            "rootCauseCategory.equals=" + DEFAULT_ROOT_CAUSE_CATEGORY,
            "rootCauseCategory.equals=" + UPDATED_ROOT_CAUSE_CATEGORY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseCategoryIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseCategory in
        defaultIncidentFiltering(
            "rootCauseCategory.in=" + DEFAULT_ROOT_CAUSE_CATEGORY + "," + UPDATED_ROOT_CAUSE_CATEGORY,
            "rootCauseCategory.in=" + UPDATED_ROOT_CAUSE_CATEGORY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseCategoryIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseCategory is not null
        defaultIncidentFiltering("rootCauseCategory.specified=true", "rootCauseCategory.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseCategoryContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseCategory contains
        defaultIncidentFiltering(
            "rootCauseCategory.contains=" + DEFAULT_ROOT_CAUSE_CATEGORY,
            "rootCauseCategory.contains=" + UPDATED_ROOT_CAUSE_CATEGORY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseCategoryNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseCategory does not contain
        defaultIncidentFiltering(
            "rootCauseCategory.doesNotContain=" + UPDATED_ROOT_CAUSE_CATEGORY,
            "rootCauseCategory.doesNotContain=" + DEFAULT_ROOT_CAUSE_CATEGORY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseProofRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseProofRef equals to
        defaultIncidentFiltering(
            "rootCauseProofRef.equals=" + DEFAULT_ROOT_CAUSE_PROOF_REF,
            "rootCauseProofRef.equals=" + UPDATED_ROOT_CAUSE_PROOF_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseProofRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseProofRef in
        defaultIncidentFiltering(
            "rootCauseProofRef.in=" + DEFAULT_ROOT_CAUSE_PROOF_REF + "," + UPDATED_ROOT_CAUSE_PROOF_REF,
            "rootCauseProofRef.in=" + UPDATED_ROOT_CAUSE_PROOF_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseProofRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseProofRef is not null
        defaultIncidentFiltering("rootCauseProofRef.specified=true", "rootCauseProofRef.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseProofRefContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseProofRef contains
        defaultIncidentFiltering(
            "rootCauseProofRef.contains=" + DEFAULT_ROOT_CAUSE_PROOF_REF,
            "rootCauseProofRef.contains=" + UPDATED_ROOT_CAUSE_PROOF_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRootCauseProofRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rootCauseProofRef does not contain
        defaultIncidentFiltering(
            "rootCauseProofRef.doesNotContain=" + UPDATED_ROOT_CAUSE_PROOF_REF,
            "rootCauseProofRef.doesNotContain=" + DEFAULT_ROOT_CAUSE_PROOF_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTest equals to
        defaultIncidentFiltering("resolutionTest.equals=" + DEFAULT_RESOLUTION_TEST, "resolutionTest.equals=" + UPDATED_RESOLUTION_TEST);
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTest in
        defaultIncidentFiltering(
            "resolutionTest.in=" + DEFAULT_RESOLUTION_TEST + "," + UPDATED_RESOLUTION_TEST,
            "resolutionTest.in=" + UPDATED_RESOLUTION_TEST
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTest is not null
        defaultIncidentFiltering("resolutionTest.specified=true", "resolutionTest.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTest contains
        defaultIncidentFiltering(
            "resolutionTest.contains=" + DEFAULT_RESOLUTION_TEST,
            "resolutionTest.contains=" + UPDATED_RESOLUTION_TEST
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTest does not contain
        defaultIncidentFiltering(
            "resolutionTest.doesNotContain=" + UPDATED_RESOLUTION_TEST,
            "resolutionTest.doesNotContain=" + DEFAULT_RESOLUTION_TEST
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestPassedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestPassed equals to
        defaultIncidentFiltering(
            "resolutionTestPassed.equals=" + DEFAULT_RESOLUTION_TEST_PASSED,
            "resolutionTestPassed.equals=" + UPDATED_RESOLUTION_TEST_PASSED
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestPassedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestPassed in
        defaultIncidentFiltering(
            "resolutionTestPassed.in=" + DEFAULT_RESOLUTION_TEST_PASSED + "," + UPDATED_RESOLUTION_TEST_PASSED,
            "resolutionTestPassed.in=" + UPDATED_RESOLUTION_TEST_PASSED
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestPassedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestPassed is not null
        defaultIncidentFiltering("resolutionTestPassed.specified=true", "resolutionTestPassed.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestAuthorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestAuthorId equals to
        defaultIncidentFiltering(
            "resolutionTestAuthorId.equals=" + DEFAULT_RESOLUTION_TEST_AUTHOR_ID,
            "resolutionTestAuthorId.equals=" + UPDATED_RESOLUTION_TEST_AUTHOR_ID
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestAuthorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestAuthorId in
        defaultIncidentFiltering(
            "resolutionTestAuthorId.in=" + DEFAULT_RESOLUTION_TEST_AUTHOR_ID + "," + UPDATED_RESOLUTION_TEST_AUTHOR_ID,
            "resolutionTestAuthorId.in=" + UPDATED_RESOLUTION_TEST_AUTHOR_ID
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestAuthorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestAuthorId is not null
        defaultIncidentFiltering("resolutionTestAuthorId.specified=true", "resolutionTestAuthorId.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestAt equals to
        defaultIncidentFiltering(
            "resolutionTestAt.equals=" + DEFAULT_RESOLUTION_TEST_AT,
            "resolutionTestAt.equals=" + UPDATED_RESOLUTION_TEST_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestAt in
        defaultIncidentFiltering(
            "resolutionTestAt.in=" + DEFAULT_RESOLUTION_TEST_AT + "," + UPDATED_RESOLUTION_TEST_AT,
            "resolutionTestAt.in=" + UPDATED_RESOLUTION_TEST_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestAt is not null
        defaultIncidentFiltering("resolutionTestAt.specified=true", "resolutionTestAt.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestProofRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestProofRef equals to
        defaultIncidentFiltering(
            "resolutionTestProofRef.equals=" + DEFAULT_RESOLUTION_TEST_PROOF_REF,
            "resolutionTestProofRef.equals=" + UPDATED_RESOLUTION_TEST_PROOF_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestProofRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestProofRef in
        defaultIncidentFiltering(
            "resolutionTestProofRef.in=" + DEFAULT_RESOLUTION_TEST_PROOF_REF + "," + UPDATED_RESOLUTION_TEST_PROOF_REF,
            "resolutionTestProofRef.in=" + UPDATED_RESOLUTION_TEST_PROOF_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestProofRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestProofRef is not null
        defaultIncidentFiltering("resolutionTestProofRef.specified=true", "resolutionTestProofRef.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestProofRefContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestProofRef contains
        defaultIncidentFiltering(
            "resolutionTestProofRef.contains=" + DEFAULT_RESOLUTION_TEST_PROOF_REF,
            "resolutionTestProofRef.contains=" + UPDATED_RESOLUTION_TEST_PROOF_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByResolutionTestProofRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where resolutionTestProofRef does not contain
        defaultIncidentFiltering(
            "resolutionTestProofRef.doesNotContain=" + UPDATED_RESOLUTION_TEST_PROOF_REF,
            "resolutionTestProofRef.doesNotContain=" + DEFAULT_RESOLUTION_TEST_PROOF_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedRef equals to
        defaultIncidentFiltering(
            "lessonsLearnedRef.equals=" + DEFAULT_LESSONS_LEARNED_REF,
            "lessonsLearnedRef.equals=" + UPDATED_LESSONS_LEARNED_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedRef in
        defaultIncidentFiltering(
            "lessonsLearnedRef.in=" + DEFAULT_LESSONS_LEARNED_REF + "," + UPDATED_LESSONS_LEARNED_REF,
            "lessonsLearnedRef.in=" + UPDATED_LESSONS_LEARNED_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedRef is not null
        defaultIncidentFiltering("lessonsLearnedRef.specified=true", "lessonsLearnedRef.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedRefContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedRef contains
        defaultIncidentFiltering(
            "lessonsLearnedRef.contains=" + DEFAULT_LESSONS_LEARNED_REF,
            "lessonsLearnedRef.contains=" + UPDATED_LESSONS_LEARNED_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedRef does not contain
        defaultIncidentFiltering(
            "lessonsLearnedRef.doesNotContain=" + UPDATED_LESSONS_LEARNED_REF,
            "lessonsLearnedRef.doesNotContain=" + DEFAULT_LESSONS_LEARNED_REF
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedWaiverIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedWaiver equals to
        defaultIncidentFiltering(
            "lessonsLearnedWaiver.equals=" + DEFAULT_LESSONS_LEARNED_WAIVER,
            "lessonsLearnedWaiver.equals=" + UPDATED_LESSONS_LEARNED_WAIVER
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedWaiverIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedWaiver in
        defaultIncidentFiltering(
            "lessonsLearnedWaiver.in=" + DEFAULT_LESSONS_LEARNED_WAIVER + "," + UPDATED_LESSONS_LEARNED_WAIVER,
            "lessonsLearnedWaiver.in=" + UPDATED_LESSONS_LEARNED_WAIVER
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedWaiverIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedWaiver is not null
        defaultIncidentFiltering("lessonsLearnedWaiver.specified=true", "lessonsLearnedWaiver.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedWaiverContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedWaiver contains
        defaultIncidentFiltering(
            "lessonsLearnedWaiver.contains=" + DEFAULT_LESSONS_LEARNED_WAIVER,
            "lessonsLearnedWaiver.contains=" + UPDATED_LESSONS_LEARNED_WAIVER
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByLessonsLearnedWaiverNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where lessonsLearnedWaiver does not contain
        defaultIncidentFiltering(
            "lessonsLearnedWaiver.doesNotContain=" + UPDATED_LESSONS_LEARNED_WAIVER,
            "lessonsLearnedWaiver.doesNotContain=" + DEFAULT_LESSONS_LEARNED_WAIVER
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByReportDueAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportDueAt equals to
        defaultIncidentFiltering("reportDueAt.equals=" + DEFAULT_REPORT_DUE_AT, "reportDueAt.equals=" + UPDATED_REPORT_DUE_AT);
    }

    @Test
    @Transactional
    void getAllIncidentsByReportDueAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportDueAt in
        defaultIncidentFiltering(
            "reportDueAt.in=" + DEFAULT_REPORT_DUE_AT + "," + UPDATED_REPORT_DUE_AT,
            "reportDueAt.in=" + UPDATED_REPORT_DUE_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByReportDueAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportDueAt is not null
        defaultIncidentFiltering("reportDueAt.specified=true", "reportDueAt.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByReportGeneratedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportGeneratedAt equals to
        defaultIncidentFiltering(
            "reportGeneratedAt.equals=" + DEFAULT_REPORT_GENERATED_AT,
            "reportGeneratedAt.equals=" + UPDATED_REPORT_GENERATED_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByReportGeneratedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportGeneratedAt in
        defaultIncidentFiltering(
            "reportGeneratedAt.in=" + DEFAULT_REPORT_GENERATED_AT + "," + UPDATED_REPORT_GENERATED_AT,
            "reportGeneratedAt.in=" + UPDATED_REPORT_GENERATED_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByReportGeneratedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportGeneratedAt is not null
        defaultIncidentFiltering("reportGeneratedAt.specified=true", "reportGeneratedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByReportRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportRef equals to
        defaultIncidentFiltering("reportRef.equals=" + DEFAULT_REPORT_REF, "reportRef.equals=" + UPDATED_REPORT_REF);
    }

    @Test
    @Transactional
    void getAllIncidentsByReportRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportRef in
        defaultIncidentFiltering("reportRef.in=" + DEFAULT_REPORT_REF + "," + UPDATED_REPORT_REF, "reportRef.in=" + UPDATED_REPORT_REF);
    }

    @Test
    @Transactional
    void getAllIncidentsByReportRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportRef is not null
        defaultIncidentFiltering("reportRef.specified=true", "reportRef.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByReportRefContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportRef contains
        defaultIncidentFiltering("reportRef.contains=" + DEFAULT_REPORT_REF, "reportRef.contains=" + UPDATED_REPORT_REF);
    }

    @Test
    @Transactional
    void getAllIncidentsByReportRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportRef does not contain
        defaultIncidentFiltering("reportRef.doesNotContain=" + UPDATED_REPORT_REF, "reportRef.doesNotContain=" + DEFAULT_REPORT_REF);
    }

    @Test
    @Transactional
    void getAllIncidentsByReportDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportDigest equals to
        defaultIncidentFiltering("reportDigest.equals=" + DEFAULT_REPORT_DIGEST, "reportDigest.equals=" + UPDATED_REPORT_DIGEST);
    }

    @Test
    @Transactional
    void getAllIncidentsByReportDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportDigest in
        defaultIncidentFiltering(
            "reportDigest.in=" + DEFAULT_REPORT_DIGEST + "," + UPDATED_REPORT_DIGEST,
            "reportDigest.in=" + UPDATED_REPORT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByReportDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportDigest is not null
        defaultIncidentFiltering("reportDigest.specified=true", "reportDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByReportDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportDigest contains
        defaultIncidentFiltering("reportDigest.contains=" + DEFAULT_REPORT_DIGEST, "reportDigest.contains=" + UPDATED_REPORT_DIGEST);
    }

    @Test
    @Transactional
    void getAllIncidentsByReportDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where reportDigest does not contain
        defaultIncidentFiltering(
            "reportDigest.doesNotContain=" + UPDATED_REPORT_DIGEST,
            "reportDigest.doesNotContain=" + DEFAULT_REPORT_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByComplianceVisaByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where complianceVisaBy equals to
        defaultIncidentFiltering(
            "complianceVisaBy.equals=" + DEFAULT_COMPLIANCE_VISA_BY,
            "complianceVisaBy.equals=" + UPDATED_COMPLIANCE_VISA_BY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByComplianceVisaByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where complianceVisaBy in
        defaultIncidentFiltering(
            "complianceVisaBy.in=" + DEFAULT_COMPLIANCE_VISA_BY + "," + UPDATED_COMPLIANCE_VISA_BY,
            "complianceVisaBy.in=" + UPDATED_COMPLIANCE_VISA_BY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByComplianceVisaByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where complianceVisaBy is not null
        defaultIncidentFiltering("complianceVisaBy.specified=true", "complianceVisaBy.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByComplianceVisaAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where complianceVisaAt equals to
        defaultIncidentFiltering(
            "complianceVisaAt.equals=" + DEFAULT_COMPLIANCE_VISA_AT,
            "complianceVisaAt.equals=" + UPDATED_COMPLIANCE_VISA_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByComplianceVisaAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where complianceVisaAt in
        defaultIncidentFiltering(
            "complianceVisaAt.in=" + DEFAULT_COMPLIANCE_VISA_AT + "," + UPDATED_COMPLIANCE_VISA_AT,
            "complianceVisaAt.in=" + UPDATED_COMPLIANCE_VISA_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByComplianceVisaAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where complianceVisaAt is not null
        defaultIncidentFiltering("complianceVisaAt.specified=true", "complianceVisaAt.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByCriteriaValidatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where criteriaValidatedAt equals to
        defaultIncidentFiltering(
            "criteriaValidatedAt.equals=" + DEFAULT_CRITERIA_VALIDATED_AT,
            "criteriaValidatedAt.equals=" + UPDATED_CRITERIA_VALIDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByCriteriaValidatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where criteriaValidatedAt in
        defaultIncidentFiltering(
            "criteriaValidatedAt.in=" + DEFAULT_CRITERIA_VALIDATED_AT + "," + UPDATED_CRITERIA_VALIDATED_AT,
            "criteriaValidatedAt.in=" + UPDATED_CRITERIA_VALIDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByCriteriaValidatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where criteriaValidatedAt is not null
        defaultIncidentFiltering("criteriaValidatedAt.specified=true", "criteriaValidatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByValidatorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where validatorId equals to
        defaultIncidentFiltering("validatorId.equals=" + DEFAULT_VALIDATOR_ID, "validatorId.equals=" + UPDATED_VALIDATOR_ID);
    }

    @Test
    @Transactional
    void getAllIncidentsByValidatorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where validatorId in
        defaultIncidentFiltering(
            "validatorId.in=" + DEFAULT_VALIDATOR_ID + "," + UPDATED_VALIDATOR_ID,
            "validatorId.in=" + UPDATED_VALIDATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByValidatorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where validatorId is not null
        defaultIncidentFiltering("validatorId.specified=true", "validatorId.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByRawDurationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rawDuration equals to
        defaultIncidentFiltering("rawDuration.equals=" + DEFAULT_RAW_DURATION, "rawDuration.equals=" + UPDATED_RAW_DURATION);
    }

    @Test
    @Transactional
    void getAllIncidentsByRawDurationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rawDuration in
        defaultIncidentFiltering(
            "rawDuration.in=" + DEFAULT_RAW_DURATION + "," + UPDATED_RAW_DURATION,
            "rawDuration.in=" + UPDATED_RAW_DURATION
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRawDurationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rawDuration is not null
        defaultIncidentFiltering("rawDuration.specified=true", "rawDuration.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByRawDurationIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rawDuration is greater than or equal to
        defaultIncidentFiltering(
            "rawDuration.greaterThanOrEqual=" + DEFAULT_RAW_DURATION,
            "rawDuration.greaterThanOrEqual=" + UPDATED_RAW_DURATION
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRawDurationIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rawDuration is less than or equal to
        defaultIncidentFiltering(
            "rawDuration.lessThanOrEqual=" + DEFAULT_RAW_DURATION,
            "rawDuration.lessThanOrEqual=" + SMALLER_RAW_DURATION
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRawDurationIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rawDuration is less than
        defaultIncidentFiltering("rawDuration.lessThan=" + UPDATED_RAW_DURATION, "rawDuration.lessThan=" + DEFAULT_RAW_DURATION);
    }

    @Test
    @Transactional
    void getAllIncidentsByRawDurationIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where rawDuration is greater than
        defaultIncidentFiltering("rawDuration.greaterThan=" + SMALLER_RAW_DURATION, "rawDuration.greaterThan=" + DEFAULT_RAW_DURATION);
    }

    @Test
    @Transactional
    void getAllIncidentsByFrozenDurationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where frozenDuration equals to
        defaultIncidentFiltering("frozenDuration.equals=" + DEFAULT_FROZEN_DURATION, "frozenDuration.equals=" + UPDATED_FROZEN_DURATION);
    }

    @Test
    @Transactional
    void getAllIncidentsByFrozenDurationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where frozenDuration in
        defaultIncidentFiltering(
            "frozenDuration.in=" + DEFAULT_FROZEN_DURATION + "," + UPDATED_FROZEN_DURATION,
            "frozenDuration.in=" + UPDATED_FROZEN_DURATION
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByFrozenDurationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where frozenDuration is not null
        defaultIncidentFiltering("frozenDuration.specified=true", "frozenDuration.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByFrozenDurationIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where frozenDuration is greater than or equal to
        defaultIncidentFiltering(
            "frozenDuration.greaterThanOrEqual=" + DEFAULT_FROZEN_DURATION,
            "frozenDuration.greaterThanOrEqual=" + UPDATED_FROZEN_DURATION
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByFrozenDurationIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where frozenDuration is less than or equal to
        defaultIncidentFiltering(
            "frozenDuration.lessThanOrEqual=" + DEFAULT_FROZEN_DURATION,
            "frozenDuration.lessThanOrEqual=" + SMALLER_FROZEN_DURATION
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByFrozenDurationIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where frozenDuration is less than
        defaultIncidentFiltering(
            "frozenDuration.lessThan=" + UPDATED_FROZEN_DURATION,
            "frozenDuration.lessThan=" + DEFAULT_FROZEN_DURATION
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByFrozenDurationIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where frozenDuration is greater than
        defaultIncidentFiltering(
            "frozenDuration.greaterThan=" + SMALLER_FROZEN_DURATION,
            "frozenDuration.greaterThan=" + DEFAULT_FROZEN_DURATION
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByImputableDelayIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where imputableDelay equals to
        defaultIncidentFiltering("imputableDelay.equals=" + DEFAULT_IMPUTABLE_DELAY, "imputableDelay.equals=" + UPDATED_IMPUTABLE_DELAY);
    }

    @Test
    @Transactional
    void getAllIncidentsByImputableDelayIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where imputableDelay in
        defaultIncidentFiltering(
            "imputableDelay.in=" + DEFAULT_IMPUTABLE_DELAY + "," + UPDATED_IMPUTABLE_DELAY,
            "imputableDelay.in=" + UPDATED_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByImputableDelayIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where imputableDelay is not null
        defaultIncidentFiltering("imputableDelay.specified=true", "imputableDelay.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByImputableDelayIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where imputableDelay is greater than or equal to
        defaultIncidentFiltering(
            "imputableDelay.greaterThanOrEqual=" + DEFAULT_IMPUTABLE_DELAY,
            "imputableDelay.greaterThanOrEqual=" + UPDATED_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByImputableDelayIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where imputableDelay is less than or equal to
        defaultIncidentFiltering(
            "imputableDelay.lessThanOrEqual=" + DEFAULT_IMPUTABLE_DELAY,
            "imputableDelay.lessThanOrEqual=" + SMALLER_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByImputableDelayIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where imputableDelay is less than
        defaultIncidentFiltering(
            "imputableDelay.lessThan=" + UPDATED_IMPUTABLE_DELAY,
            "imputableDelay.lessThan=" + DEFAULT_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByImputableDelayIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where imputableDelay is greater than
        defaultIncidentFiltering(
            "imputableDelay.greaterThan=" + SMALLER_IMPUTABLE_DELAY,
            "imputableDelay.greaterThan=" + DEFAULT_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByMttrIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where mttr equals to
        defaultIncidentFiltering("mttr.equals=" + DEFAULT_MTTR, "mttr.equals=" + UPDATED_MTTR);
    }

    @Test
    @Transactional
    void getAllIncidentsByMttrIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where mttr in
        defaultIncidentFiltering("mttr.in=" + DEFAULT_MTTR + "," + UPDATED_MTTR, "mttr.in=" + UPDATED_MTTR);
    }

    @Test
    @Transactional
    void getAllIncidentsByMttrIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where mttr is not null
        defaultIncidentFiltering("mttr.specified=true", "mttr.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByMttrIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where mttr is greater than or equal to
        defaultIncidentFiltering("mttr.greaterThanOrEqual=" + DEFAULT_MTTR, "mttr.greaterThanOrEqual=" + UPDATED_MTTR);
    }

    @Test
    @Transactional
    void getAllIncidentsByMttrIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where mttr is less than or equal to
        defaultIncidentFiltering("mttr.lessThanOrEqual=" + DEFAULT_MTTR, "mttr.lessThanOrEqual=" + SMALLER_MTTR);
    }

    @Test
    @Transactional
    void getAllIncidentsByMttrIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where mttr is less than
        defaultIncidentFiltering("mttr.lessThan=" + UPDATED_MTTR, "mttr.lessThan=" + DEFAULT_MTTR);
    }

    @Test
    @Transactional
    void getAllIncidentsByMttrIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where mttr is greater than
        defaultIncidentFiltering("mttr.greaterThan=" + SMALLER_MTTR, "mttr.greaterThan=" + DEFAULT_MTTR);
    }

    @Test
    @Transactional
    void getAllIncidentsByTakeChargeMetIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where takeChargeMet equals to
        defaultIncidentFiltering("takeChargeMet.equals=" + DEFAULT_TAKE_CHARGE_MET, "takeChargeMet.equals=" + UPDATED_TAKE_CHARGE_MET);
    }

    @Test
    @Transactional
    void getAllIncidentsByTakeChargeMetIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where takeChargeMet in
        defaultIncidentFiltering(
            "takeChargeMet.in=" + DEFAULT_TAKE_CHARGE_MET + "," + UPDATED_TAKE_CHARGE_MET,
            "takeChargeMet.in=" + UPDATED_TAKE_CHARGE_MET
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByTakeChargeMetIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where takeChargeMet is not null
        defaultIncidentFiltering("takeChargeMet.specified=true", "takeChargeMet.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByRestorationMetIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where restorationMet equals to
        defaultIncidentFiltering("restorationMet.equals=" + DEFAULT_RESTORATION_MET, "restorationMet.equals=" + UPDATED_RESTORATION_MET);
    }

    @Test
    @Transactional
    void getAllIncidentsByRestorationMetIsInShouldWork() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where restorationMet in
        defaultIncidentFiltering(
            "restorationMet.in=" + DEFAULT_RESTORATION_MET + "," + UPDATED_RESTORATION_MET,
            "restorationMet.in=" + UPDATED_RESTORATION_MET
        );
    }

    @Test
    @Transactional
    void getAllIncidentsByRestorationMetIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        // Get all the incidentList where restorationMet is not null
        defaultIncidentFiltering("restorationMet.specified=true", "restorationMet.specified=false");
    }

    @Test
    @Transactional
    void getAllIncidentsByTicketIsEqualToSomething() throws Exception {
        // Get already existing entity
        Ticket ticket = incident.getTicket();
        incidentRepository.saveAndFlush(incident);
        UUID ticketId = ticket.getId();
        // Get all the incidentList where ticket equals to ticketId
        defaultIncidentShouldBeFound("ticketId.equals=" + ticketId);

        // Get all the incidentList where ticket equals to UUID.randomUUID()
        defaultIncidentShouldNotBeFound("ticketId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllIncidentsByMatrixIsEqualToSomething() throws Exception {
        QualificationMatrix matrix;
        if (TestUtil.findAll(em, QualificationMatrix.class).isEmpty()) {
            incidentRepository.saveAndFlush(incident);
            matrix = QualificationMatrixResourceIT.createEntity();
        } else {
            matrix = TestUtil.findAll(em, QualificationMatrix.class).get(0);
        }
        em.persist(matrix);
        em.flush();
        incident.setMatrix(matrix);
        incidentRepository.saveAndFlush(incident);
        UUID matrixId = matrix.getId();
        // Get all the incidentList where matrix equals to matrixId
        defaultIncidentShouldBeFound("matrixId.equals=" + matrixId);

        // Get all the incidentList where matrix equals to UUID.randomUUID()
        defaultIncidentShouldNotBeFound("matrixId.equals=" + UUID.randomUUID());
    }

    private void defaultIncidentFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultIncidentShouldBeFound(shouldBeFound);
        defaultIncidentShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultIncidentShouldBeFound(String filter) throws Exception {
        restIncidentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(incident.getId().toString())))
            .andExpect(jsonPath("$.[*].impact").value(hasItem(DEFAULT_IMPACT)))
            .andExpect(jsonPath("$.[*].urgency").value(hasItem(DEFAULT_URGENCY)))
            .andExpect(jsonPath("$.[*].score").value(hasItem(DEFAULT_SCORE)))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY.toString())))
            .andExpect(jsonPath("$.[*].securityIncident").value(hasItem(DEFAULT_SECURITY_INCIDENT)))
            .andExpect(jsonPath("$.[*].dataCompromiseSuspected").value(hasItem(DEFAULT_DATA_COMPROMISE_SUSPECTED)))
            .andExpect(jsonPath("$.[*].regulatoryFlag").value(hasItem(DEFAULT_REGULATORY_FLAG)))
            .andExpect(jsonPath("$.[*].qualifiedAt").value(hasItem(DEFAULT_QUALIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].qualifiedBy").value(hasItem(DEFAULT_QUALIFIED_BY.toString())))
            .andExpect(jsonPath("$.[*].rootCause").value(hasItem(DEFAULT_ROOT_CAUSE)))
            .andExpect(jsonPath("$.[*].rootCauseCategory").value(hasItem(DEFAULT_ROOT_CAUSE_CATEGORY)))
            .andExpect(jsonPath("$.[*].rootCauseProofRef").value(hasItem(DEFAULT_ROOT_CAUSE_PROOF_REF)))
            .andExpect(jsonPath("$.[*].remediationPlan").value(hasItem(DEFAULT_REMEDIATION_PLAN)))
            .andExpect(jsonPath("$.[*].resolutionTest").value(hasItem(DEFAULT_RESOLUTION_TEST)))
            .andExpect(jsonPath("$.[*].resolutionTestPassed").value(hasItem(DEFAULT_RESOLUTION_TEST_PASSED)))
            .andExpect(jsonPath("$.[*].resolutionTestAuthorId").value(hasItem(DEFAULT_RESOLUTION_TEST_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].resolutionTestAt").value(hasItem(DEFAULT_RESOLUTION_TEST_AT.toString())))
            .andExpect(jsonPath("$.[*].resolutionTestProofRef").value(hasItem(DEFAULT_RESOLUTION_TEST_PROOF_REF)))
            .andExpect(jsonPath("$.[*].lessonsLearnedRef").value(hasItem(DEFAULT_LESSONS_LEARNED_REF)))
            .andExpect(jsonPath("$.[*].lessonsLearnedWaiver").value(hasItem(DEFAULT_LESSONS_LEARNED_WAIVER)))
            .andExpect(jsonPath("$.[*].reportDueAt").value(hasItem(DEFAULT_REPORT_DUE_AT.toString())))
            .andExpect(jsonPath("$.[*].reportGeneratedAt").value(hasItem(DEFAULT_REPORT_GENERATED_AT.toString())))
            .andExpect(jsonPath("$.[*].reportRef").value(hasItem(DEFAULT_REPORT_REF)))
            .andExpect(jsonPath("$.[*].reportDigest").value(hasItem(DEFAULT_REPORT_DIGEST)))
            .andExpect(jsonPath("$.[*].complianceVisaBy").value(hasItem(DEFAULT_COMPLIANCE_VISA_BY.toString())))
            .andExpect(jsonPath("$.[*].complianceVisaAt").value(hasItem(DEFAULT_COMPLIANCE_VISA_AT.toString())))
            .andExpect(jsonPath("$.[*].criteriaValidatedAt").value(hasItem(DEFAULT_CRITERIA_VALIDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].validatorId").value(hasItem(DEFAULT_VALIDATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].rawDuration").value(hasItem(DEFAULT_RAW_DURATION.toString())))
            .andExpect(jsonPath("$.[*].frozenDuration").value(hasItem(DEFAULT_FROZEN_DURATION.toString())))
            .andExpect(jsonPath("$.[*].imputableDelay").value(hasItem(DEFAULT_IMPUTABLE_DELAY.toString())))
            .andExpect(jsonPath("$.[*].mttr").value(hasItem(DEFAULT_MTTR.toString())))
            .andExpect(jsonPath("$.[*].takeChargeMet").value(hasItem(DEFAULT_TAKE_CHARGE_MET)))
            .andExpect(jsonPath("$.[*].restorationMet").value(hasItem(DEFAULT_RESTORATION_MET)));

        // Check, that the count call also returns 1
        restIncidentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultIncidentShouldNotBeFound(String filter) throws Exception {
        restIncidentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restIncidentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingIncident() throws Exception {
        // Get the incident
        restIncidentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingIncident() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the incident
        Incident updatedIncident = incidentRepository.findById(incident.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedIncident are not directly saved in db
        em.detach(updatedIncident);
        updatedIncident
            .impact(UPDATED_IMPACT)
            .urgency(UPDATED_URGENCY)
            .score(UPDATED_SCORE)
            .severity(UPDATED_SEVERITY)
            .securityIncident(UPDATED_SECURITY_INCIDENT)
            .dataCompromiseSuspected(UPDATED_DATA_COMPROMISE_SUSPECTED)
            .regulatoryFlag(UPDATED_REGULATORY_FLAG)
            .qualifiedAt(UPDATED_QUALIFIED_AT)
            .qualifiedBy(UPDATED_QUALIFIED_BY)
            .rootCause(UPDATED_ROOT_CAUSE)
            .rootCauseCategory(UPDATED_ROOT_CAUSE_CATEGORY)
            .rootCauseProofRef(UPDATED_ROOT_CAUSE_PROOF_REF)
            .remediationPlan(UPDATED_REMEDIATION_PLAN)
            .resolutionTest(UPDATED_RESOLUTION_TEST)
            .resolutionTestPassed(UPDATED_RESOLUTION_TEST_PASSED)
            .resolutionTestAuthorId(UPDATED_RESOLUTION_TEST_AUTHOR_ID)
            .resolutionTestAt(UPDATED_RESOLUTION_TEST_AT)
            .resolutionTestProofRef(UPDATED_RESOLUTION_TEST_PROOF_REF)
            .lessonsLearnedRef(UPDATED_LESSONS_LEARNED_REF)
            .lessonsLearnedWaiver(UPDATED_LESSONS_LEARNED_WAIVER)
            .reportDueAt(UPDATED_REPORT_DUE_AT)
            .reportGeneratedAt(UPDATED_REPORT_GENERATED_AT)
            .reportRef(UPDATED_REPORT_REF)
            .reportDigest(UPDATED_REPORT_DIGEST)
            .complianceVisaBy(UPDATED_COMPLIANCE_VISA_BY)
            .complianceVisaAt(UPDATED_COMPLIANCE_VISA_AT)
            .criteriaValidatedAt(UPDATED_CRITERIA_VALIDATED_AT)
            .validatorId(UPDATED_VALIDATOR_ID)
            .rawDuration(UPDATED_RAW_DURATION)
            .frozenDuration(UPDATED_FROZEN_DURATION)
            .imputableDelay(UPDATED_IMPUTABLE_DELAY)
            .mttr(UPDATED_MTTR)
            .takeChargeMet(UPDATED_TAKE_CHARGE_MET)
            .restorationMet(UPDATED_RESTORATION_MET);
        IncidentDTO incidentDTO = incidentMapper.toDto(updatedIncident);

        restIncidentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, incidentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(incidentDTO))
            )
            .andExpect(status().isOk());

        // Validate the Incident in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedIncidentToMatchAllProperties(updatedIncident);
    }

    @Test
    @Transactional
    void putNonExistingIncident() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        incident.setId(UUID.randomUUID());

        // Create the Incident
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restIncidentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, incidentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(incidentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Incident in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchIncident() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        incident.setId(UUID.randomUUID());

        // Create the Incident
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIncidentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(incidentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Incident in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamIncident() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        incident.setId(UUID.randomUUID());

        // Create the Incident
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIncidentMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(incidentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Incident in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateIncidentWithPatch() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the incident using partial update
        Incident partialUpdatedIncident = new Incident();
        partialUpdatedIncident.setId(incident.getId());

        partialUpdatedIncident
            .impact(UPDATED_IMPACT)
            .urgency(UPDATED_URGENCY)
            .score(UPDATED_SCORE)
            .securityIncident(UPDATED_SECURITY_INCIDENT)
            .qualifiedAt(UPDATED_QUALIFIED_AT)
            .rootCause(UPDATED_ROOT_CAUSE)
            .rootCauseCategory(UPDATED_ROOT_CAUSE_CATEGORY)
            .resolutionTestPassed(UPDATED_RESOLUTION_TEST_PASSED)
            .resolutionTestAuthorId(UPDATED_RESOLUTION_TEST_AUTHOR_ID)
            .lessonsLearnedRef(UPDATED_LESSONS_LEARNED_REF)
            .reportDueAt(UPDATED_REPORT_DUE_AT)
            .reportRef(UPDATED_REPORT_REF)
            .complianceVisaBy(UPDATED_COMPLIANCE_VISA_BY)
            .validatorId(UPDATED_VALIDATOR_ID)
            .rawDuration(UPDATED_RAW_DURATION)
            .frozenDuration(UPDATED_FROZEN_DURATION)
            .mttr(UPDATED_MTTR)
            .takeChargeMet(UPDATED_TAKE_CHARGE_MET);

        restIncidentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedIncident.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedIncident))
            )
            .andExpect(status().isOk());

        // Validate the Incident in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertIncidentUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedIncident, incident), getPersistedIncident(incident));
    }

    @Test
    @Transactional
    void fullUpdateIncidentWithPatch() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the incident using partial update
        Incident partialUpdatedIncident = new Incident();
        partialUpdatedIncident.setId(incident.getId());

        partialUpdatedIncident
            .impact(UPDATED_IMPACT)
            .urgency(UPDATED_URGENCY)
            .score(UPDATED_SCORE)
            .severity(UPDATED_SEVERITY)
            .securityIncident(UPDATED_SECURITY_INCIDENT)
            .dataCompromiseSuspected(UPDATED_DATA_COMPROMISE_SUSPECTED)
            .regulatoryFlag(UPDATED_REGULATORY_FLAG)
            .qualifiedAt(UPDATED_QUALIFIED_AT)
            .qualifiedBy(UPDATED_QUALIFIED_BY)
            .rootCause(UPDATED_ROOT_CAUSE)
            .rootCauseCategory(UPDATED_ROOT_CAUSE_CATEGORY)
            .rootCauseProofRef(UPDATED_ROOT_CAUSE_PROOF_REF)
            .remediationPlan(UPDATED_REMEDIATION_PLAN)
            .resolutionTest(UPDATED_RESOLUTION_TEST)
            .resolutionTestPassed(UPDATED_RESOLUTION_TEST_PASSED)
            .resolutionTestAuthorId(UPDATED_RESOLUTION_TEST_AUTHOR_ID)
            .resolutionTestAt(UPDATED_RESOLUTION_TEST_AT)
            .resolutionTestProofRef(UPDATED_RESOLUTION_TEST_PROOF_REF)
            .lessonsLearnedRef(UPDATED_LESSONS_LEARNED_REF)
            .lessonsLearnedWaiver(UPDATED_LESSONS_LEARNED_WAIVER)
            .reportDueAt(UPDATED_REPORT_DUE_AT)
            .reportGeneratedAt(UPDATED_REPORT_GENERATED_AT)
            .reportRef(UPDATED_REPORT_REF)
            .reportDigest(UPDATED_REPORT_DIGEST)
            .complianceVisaBy(UPDATED_COMPLIANCE_VISA_BY)
            .complianceVisaAt(UPDATED_COMPLIANCE_VISA_AT)
            .criteriaValidatedAt(UPDATED_CRITERIA_VALIDATED_AT)
            .validatorId(UPDATED_VALIDATOR_ID)
            .rawDuration(UPDATED_RAW_DURATION)
            .frozenDuration(UPDATED_FROZEN_DURATION)
            .imputableDelay(UPDATED_IMPUTABLE_DELAY)
            .mttr(UPDATED_MTTR)
            .takeChargeMet(UPDATED_TAKE_CHARGE_MET)
            .restorationMet(UPDATED_RESTORATION_MET);

        restIncidentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedIncident.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedIncident))
            )
            .andExpect(status().isOk());

        // Validate the Incident in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertIncidentUpdatableFieldsEquals(partialUpdatedIncident, getPersistedIncident(partialUpdatedIncident));
    }

    @Test
    @Transactional
    void patchNonExistingIncident() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        incident.setId(UUID.randomUUID());

        // Create the Incident
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restIncidentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, incidentDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(incidentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Incident in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchIncident() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        incident.setId(UUID.randomUUID());

        // Create the Incident
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIncidentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(incidentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Incident in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamIncident() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        incident.setId(UUID.randomUUID());

        // Create the Incident
        IncidentDTO incidentDTO = incidentMapper.toDto(incident);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restIncidentMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(incidentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Incident in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteIncident() throws Exception {
        // Initialize the database
        insertedIncident = incidentRepository.saveAndFlush(incident);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the incident
        restIncidentMockMvc
            .perform(delete(ENTITY_API_URL_ID, incident.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return incidentRepository.count();
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

    protected Incident getPersistedIncident(Incident incident) {
        return incidentRepository.findById(incident.getId()).orElseThrow();
    }

    protected void assertPersistedIncidentToMatchAllProperties(Incident expectedIncident) {
        assertIncidentAllPropertiesEquals(expectedIncident, getPersistedIncident(expectedIncident));
    }

    protected void assertPersistedIncidentToMatchUpdatableProperties(Incident expectedIncident) {
        assertIncidentAllUpdatablePropertiesEquals(expectedIncident, getPersistedIncident(expectedIncident));
    }
}
