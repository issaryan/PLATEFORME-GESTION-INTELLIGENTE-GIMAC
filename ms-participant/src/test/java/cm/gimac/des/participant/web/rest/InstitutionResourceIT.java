package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.security.AuthoritiesConstants;
import static cm.gimac.des.participant.domain.InstitutionAsserts.*;
import static cm.gimac.des.participant.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.service.InstitutionService;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import cm.gimac.des.participant.service.mapper.InstitutionMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link InstitutionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class InstitutionResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_ACRONYM = "AAAAAAAAAA";
    private static final String UPDATED_ACRONYM = "BBBBBBBBBB";

    private static final String DEFAULT_PARTICIPANT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_PARTICIPANT_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_IDENTIFICATION_CODE = "FYTVIZXT";
    private static final String UPDATED_IDENTIFICATION_CODE = "LILRLYSE";

    private static final String DEFAULT_BEAC_BANK_CODE = "AAAAAAAAAA";
    private static final String UPDATED_BEAC_BANK_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_COUNTRY = "CF";
    private static final String UPDATED_COUNTRY = "CFB";

    private static final InstitutionCategory DEFAULT_CATEGORY = InstitutionCategory.BANQUE;
    private static final InstitutionCategory UPDATED_CATEGORY = InstitutionCategory.MICROFINANCE;

    private static final MembershipMode DEFAULT_MEMBERSHIP_MODE = MembershipMode.DIRECT;
    private static final MembershipMode UPDATED_MEMBERSHIP_MODE = MembershipMode.INDIRECT;

    private static final String DEFAULT_SETTLEMENT_ACCOUNT_REF = "AAAAAAAAAA";
    private static final String UPDATED_SETTLEMENT_ACCOUNT_REF = "BBBBBBBBBB";

    private static final String DEFAULT_RING_FENCED_ACCOUNT_REF = "AAAAAAAAAA";
    private static final String UPDATED_RING_FENCED_ACCOUNT_REF = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_JOINED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_JOINED_ON = LocalDate.parse("2026-09-29");
    private static final LocalDate SMALLER_JOINED_ON = LocalDate.ofEpochDay(-1L);

    private static final InstitutionStatus DEFAULT_STATUS = InstitutionStatus.CANDIDAT;
    private static final InstitutionStatus UPDATED_STATUS = InstitutionStatus.ADHERENT;

    private static final ComplianceState DEFAULT_COMPLIANCE_STATE = ComplianceState.CONFORME;
    private static final ComplianceState UPDATED_COMPLIANCE_STATE = ComplianceState.A_REGULARISER;

    private static final Instant DEFAULT_ATTESTATION_ISSUED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ATTESTATION_ISSUED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final Instant DEFAULT_GO_LIVE_AUTHORIZED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_GO_LIVE_AUTHORIZED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final Integer DEFAULT_OPEN_LINK_COUNT = 0;
    private static final Integer UPDATED_OPEN_LINK_COUNT = 1;
    private static final Integer SMALLER_OPEN_LINK_COUNT = 0 - 1;

    private static final Integer DEFAULT_OPEN_SUSPENSE_COUNT = 0;
    private static final Integer UPDATED_OPEN_SUSPENSE_COUNT = 1;
    private static final Integer SMALLER_OPEN_SUSPENSE_COUNT = 0 - 1;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final LocalDate DEFAULT_CONTACTED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_CONTACTED_ON = LocalDate.parse("2026-09-29");
    private static final LocalDate SMALLER_CONTACTED_ON = LocalDate.ofEpochDay(-1L);

    private static final String DEFAULT_CONTACT_ORIGIN = "AAAAAAAAAA";
    private static final String UPDATED_CONTACT_ORIGIN = "BBBBBBBBBB";

    private static final String DEFAULT_INTENDED_PROJECT = "AAAAAAAAAA";
    private static final String UPDATED_INTENDED_PROJECT = "BBBBBBBBBB";

    private static final UUID DEFAULT_DDRM_OFFICER_ID = UUID.randomUUID();
    private static final UUID UPDATED_DDRM_OFFICER_ID = UUID.randomUUID();

    private static final String DEFAULT_DDRM_OFFICER_NAME = "AAAAAAAAAA";
    private static final String UPDATED_DDRM_OFFICER_NAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_ADMITTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ADMITTED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final UUID DEFAULT_ADMITTED_BY_ID = UUID.randomUUID();
    private static final UUID UPDATED_ADMITTED_BY_ID = UUID.randomUUID();

    private static final String DEFAULT_CLOSURE_REASON = "AAAAAAAAAA";
    private static final String UPDATED_CLOSURE_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final String ENTITY_API_URL = "/api/institutions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Mock
    private InstitutionRepository institutionRepositoryMock;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Mock
    private InstitutionService institutionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restInstitutionMockMvc;

    private Institution institution;

    private Institution insertedInstitution;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Institution createEntity() {
        return new Institution()
            .name(DEFAULT_NAME)
            .acronym(DEFAULT_ACRONYM)
            .participantCode(DEFAULT_PARTICIPANT_CODE)
            .identificationCode(DEFAULT_IDENTIFICATION_CODE)
            .beacBankCode(DEFAULT_BEAC_BANK_CODE)
            .country(DEFAULT_COUNTRY)
            .category(DEFAULT_CATEGORY)
            .membershipMode(DEFAULT_MEMBERSHIP_MODE)
            .settlementAccountRef(DEFAULT_SETTLEMENT_ACCOUNT_REF)
            .ringFencedAccountRef(DEFAULT_RING_FENCED_ACCOUNT_REF)
            .joinedOn(DEFAULT_JOINED_ON)
            .status(DEFAULT_STATUS)
            .complianceState(DEFAULT_COMPLIANCE_STATE)
            .attestationIssuedAt(DEFAULT_ATTESTATION_ISSUED_AT)
            .goLiveAuthorizedAt(DEFAULT_GO_LIVE_AUTHORIZED_AT)
            .openLinkCount(DEFAULT_OPEN_LINK_COUNT)
            .openSuspenseCount(DEFAULT_OPEN_SUSPENSE_COUNT)
            .createdAt(DEFAULT_CREATED_AT)
            .contactedOn(DEFAULT_CONTACTED_ON)
            .contactOrigin(DEFAULT_CONTACT_ORIGIN)
            .intendedProject(DEFAULT_INTENDED_PROJECT)
            .ddrmOfficerId(DEFAULT_DDRM_OFFICER_ID)
            .ddrmOfficerName(DEFAULT_DDRM_OFFICER_NAME)
            .admittedAt(DEFAULT_ADMITTED_AT)
            .admittedById(DEFAULT_ADMITTED_BY_ID)
            .closureReason(DEFAULT_CLOSURE_REASON)
            .closedAt(DEFAULT_CLOSED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Institution createUpdatedEntity() {
        return new Institution()
            .name(UPDATED_NAME)
            .acronym(UPDATED_ACRONYM)
            .participantCode(UPDATED_PARTICIPANT_CODE)
            .identificationCode(UPDATED_IDENTIFICATION_CODE)
            .beacBankCode(UPDATED_BEAC_BANK_CODE)
            .country(UPDATED_COUNTRY)
            .category(UPDATED_CATEGORY)
            .membershipMode(UPDATED_MEMBERSHIP_MODE)
            .settlementAccountRef(UPDATED_SETTLEMENT_ACCOUNT_REF)
            .ringFencedAccountRef(UPDATED_RING_FENCED_ACCOUNT_REF)
            .joinedOn(UPDATED_JOINED_ON)
            .status(UPDATED_STATUS)
            .complianceState(UPDATED_COMPLIANCE_STATE)
            .attestationIssuedAt(UPDATED_ATTESTATION_ISSUED_AT)
            .goLiveAuthorizedAt(UPDATED_GO_LIVE_AUTHORIZED_AT)
            .openLinkCount(UPDATED_OPEN_LINK_COUNT)
            .openSuspenseCount(UPDATED_OPEN_SUSPENSE_COUNT)
            .createdAt(UPDATED_CREATED_AT)
            .contactedOn(UPDATED_CONTACTED_ON)
            .contactOrigin(UPDATED_CONTACT_ORIGIN)
            .intendedProject(UPDATED_INTENDED_PROJECT)
            .ddrmOfficerId(UPDATED_DDRM_OFFICER_ID)
            .ddrmOfficerName(UPDATED_DDRM_OFFICER_NAME)
            .admittedAt(UPDATED_ADMITTED_AT)
            .admittedById(UPDATED_ADMITTED_BY_ID)
            .closureReason(UPDATED_CLOSURE_REASON)
            .closedAt(UPDATED_CLOSED_AT);
    }

    @BeforeEach
    void initTest() {
        institution = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedInstitution != null) {
            institutionRepository.delete(insertedInstitution);
            insertedInstitution = null;
        }
    }

    @Test
    @Transactional
    void createInstitution() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Institution
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);
        var returnedInstitutionDTO = om.readValue(
            restInstitutionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            InstitutionDTO.class
        );

        // Validate the Institution in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedInstitution = institutionMapper.toEntity(returnedInstitutionDTO);
        assertInstitutionUpdatableFieldsEquals(returnedInstitution, getPersistedInstitution(returnedInstitution));

        insertedInstitution = returnedInstitution;
    }

    @Test
    @Transactional
    void createInstitutionWithExistingId() throws Exception {
        // Create the Institution with an existing ID
        insertedInstitution = institutionRepository.saveAndFlush(institution);
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Institution in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        institution.setName(null);

        // Create the Institution, which fails.
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCountryIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        institution.setCountry(null);

        // Create the Institution, which fails.
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCategoryIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        institution.setCategory(null);

        // Create the Institution, which fails.
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        institution.setStatus(null);

        // Create the Institution, which fails.
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkComplianceStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        institution.setComplianceState(null);

        // Create the Institution, which fails.
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOpenLinkCountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        institution.setOpenLinkCount(null);

        // Create the Institution, which fails.
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOpenSuspenseCountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        institution.setOpenSuspenseCount(null);

        // Create the Institution, which fails.
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        institution.setCreatedAt(null);

        // Create the Institution, which fails.
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        restInstitutionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllInstitutions() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList
        restInstitutionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(institution.getId().toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].acronym").value(hasItem(DEFAULT_ACRONYM)))
            .andExpect(jsonPath("$.[*].participantCode").value(hasItem(DEFAULT_PARTICIPANT_CODE)))
            .andExpect(jsonPath("$.[*].identificationCode").value(hasItem(DEFAULT_IDENTIFICATION_CODE)))
            .andExpect(jsonPath("$.[*].beacBankCode").value(hasItem(DEFAULT_BEAC_BANK_CODE)))
            .andExpect(jsonPath("$.[*].country").value(hasItem(DEFAULT_COUNTRY)))
            .andExpect(jsonPath("$.[*].category").value(hasItem(DEFAULT_CATEGORY.toString())))
            .andExpect(jsonPath("$.[*].membershipMode").value(hasItem(DEFAULT_MEMBERSHIP_MODE.toString())))
            .andExpect(jsonPath("$.[*].settlementAccountRef").value(hasItem(DEFAULT_SETTLEMENT_ACCOUNT_REF)))
            .andExpect(jsonPath("$.[*].ringFencedAccountRef").value(hasItem(DEFAULT_RING_FENCED_ACCOUNT_REF)))
            .andExpect(jsonPath("$.[*].joinedOn").value(hasItem(DEFAULT_JOINED_ON.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].complianceState").value(hasItem(DEFAULT_COMPLIANCE_STATE.toString())))
            .andExpect(jsonPath("$.[*].attestationIssuedAt").value(hasItem(DEFAULT_ATTESTATION_ISSUED_AT.toString())))
            .andExpect(jsonPath("$.[*].goLiveAuthorizedAt").value(hasItem(DEFAULT_GO_LIVE_AUTHORIZED_AT.toString())))
            .andExpect(jsonPath("$.[*].openLinkCount").value(hasItem(DEFAULT_OPEN_LINK_COUNT)))
            .andExpect(jsonPath("$.[*].openSuspenseCount").value(hasItem(DEFAULT_OPEN_SUSPENSE_COUNT)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].contactedOn").value(hasItem(DEFAULT_CONTACTED_ON.toString())))
            .andExpect(jsonPath("$.[*].contactOrigin").value(hasItem(DEFAULT_CONTACT_ORIGIN)))
            .andExpect(jsonPath("$.[*].intendedProject").value(hasItem(DEFAULT_INTENDED_PROJECT)))
            .andExpect(jsonPath("$.[*].ddrmOfficerId").value(hasItem(DEFAULT_DDRM_OFFICER_ID.toString())))
            .andExpect(jsonPath("$.[*].ddrmOfficerName").value(hasItem(DEFAULT_DDRM_OFFICER_NAME)))
            .andExpect(jsonPath("$.[*].admittedAt").value(hasItem(DEFAULT_ADMITTED_AT.toString())))
            .andExpect(jsonPath("$.[*].admittedById").value(hasItem(DEFAULT_ADMITTED_BY_ID.toString())))
            .andExpect(jsonPath("$.[*].closureReason").value(hasItem(DEFAULT_CLOSURE_REASON)))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInstitutionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(institutionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restInstitutionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(institutionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInstitutionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(institutionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restInstitutionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(institutionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getInstitution() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get the institution
        restInstitutionMockMvc
            .perform(get(ENTITY_API_URL_ID, institution.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(institution.getId().toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.acronym").value(DEFAULT_ACRONYM))
            .andExpect(jsonPath("$.participantCode").value(DEFAULT_PARTICIPANT_CODE))
            .andExpect(jsonPath("$.identificationCode").value(DEFAULT_IDENTIFICATION_CODE))
            .andExpect(jsonPath("$.beacBankCode").value(DEFAULT_BEAC_BANK_CODE))
            .andExpect(jsonPath("$.country").value(DEFAULT_COUNTRY))
            .andExpect(jsonPath("$.category").value(DEFAULT_CATEGORY.toString()))
            .andExpect(jsonPath("$.membershipMode").value(DEFAULT_MEMBERSHIP_MODE.toString()))
            .andExpect(jsonPath("$.settlementAccountRef").value(DEFAULT_SETTLEMENT_ACCOUNT_REF))
            .andExpect(jsonPath("$.ringFencedAccountRef").value(DEFAULT_RING_FENCED_ACCOUNT_REF))
            .andExpect(jsonPath("$.joinedOn").value(DEFAULT_JOINED_ON.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.complianceState").value(DEFAULT_COMPLIANCE_STATE.toString()))
            .andExpect(jsonPath("$.attestationIssuedAt").value(DEFAULT_ATTESTATION_ISSUED_AT.toString()))
            .andExpect(jsonPath("$.goLiveAuthorizedAt").value(DEFAULT_GO_LIVE_AUTHORIZED_AT.toString()))
            .andExpect(jsonPath("$.openLinkCount").value(DEFAULT_OPEN_LINK_COUNT))
            .andExpect(jsonPath("$.openSuspenseCount").value(DEFAULT_OPEN_SUSPENSE_COUNT))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.contactedOn").value(DEFAULT_CONTACTED_ON.toString()))
            .andExpect(jsonPath("$.contactOrigin").value(DEFAULT_CONTACT_ORIGIN))
            .andExpect(jsonPath("$.intendedProject").value(DEFAULT_INTENDED_PROJECT))
            .andExpect(jsonPath("$.ddrmOfficerId").value(DEFAULT_DDRM_OFFICER_ID.toString()))
            .andExpect(jsonPath("$.ddrmOfficerName").value(DEFAULT_DDRM_OFFICER_NAME))
            .andExpect(jsonPath("$.admittedAt").value(DEFAULT_ADMITTED_AT.toString()))
            .andExpect(jsonPath("$.admittedById").value(DEFAULT_ADMITTED_BY_ID.toString()))
            .andExpect(jsonPath("$.closureReason").value(DEFAULT_CLOSURE_REASON))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()));
    }

    @Test
    @Transactional
    void getInstitutionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        UUID id = institution.getId();

        defaultInstitutionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllInstitutionsByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where name equals to
        defaultInstitutionFiltering("name.equals=" + DEFAULT_NAME, "name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllInstitutionsByNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where name in
        defaultInstitutionFiltering("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME, "name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllInstitutionsByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where name is not null
        defaultInstitutionFiltering("name.specified=true", "name.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByNameContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where name contains
        defaultInstitutionFiltering("name.contains=" + DEFAULT_NAME, "name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllInstitutionsByNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where name does not contain
        defaultInstitutionFiltering("name.doesNotContain=" + UPDATED_NAME, "name.doesNotContain=" + DEFAULT_NAME);
    }

    @Test
    @Transactional
    void getAllInstitutionsByAcronymIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where acronym equals to
        defaultInstitutionFiltering("acronym.equals=" + DEFAULT_ACRONYM, "acronym.equals=" + UPDATED_ACRONYM);
    }

    @Test
    @Transactional
    void getAllInstitutionsByAcronymIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where acronym in
        defaultInstitutionFiltering("acronym.in=" + DEFAULT_ACRONYM + "," + UPDATED_ACRONYM, "acronym.in=" + UPDATED_ACRONYM);
    }

    @Test
    @Transactional
    void getAllInstitutionsByAcronymIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where acronym is not null
        defaultInstitutionFiltering("acronym.specified=true", "acronym.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByAcronymContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where acronym contains
        defaultInstitutionFiltering("acronym.contains=" + DEFAULT_ACRONYM, "acronym.contains=" + UPDATED_ACRONYM);
    }

    @Test
    @Transactional
    void getAllInstitutionsByAcronymNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where acronym does not contain
        defaultInstitutionFiltering("acronym.doesNotContain=" + UPDATED_ACRONYM, "acronym.doesNotContain=" + DEFAULT_ACRONYM);
    }

    @Test
    @Transactional
    void getAllInstitutionsByParticipantCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where participantCode equals to
        defaultInstitutionFiltering(
            "participantCode.equals=" + DEFAULT_PARTICIPANT_CODE,
            "participantCode.equals=" + UPDATED_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByParticipantCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where participantCode in
        defaultInstitutionFiltering(
            "participantCode.in=" + DEFAULT_PARTICIPANT_CODE + "," + UPDATED_PARTICIPANT_CODE,
            "participantCode.in=" + UPDATED_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByParticipantCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where participantCode is not null
        defaultInstitutionFiltering("participantCode.specified=true", "participantCode.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByParticipantCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where participantCode contains
        defaultInstitutionFiltering(
            "participantCode.contains=" + DEFAULT_PARTICIPANT_CODE,
            "participantCode.contains=" + UPDATED_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByParticipantCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where participantCode does not contain
        defaultInstitutionFiltering(
            "participantCode.doesNotContain=" + UPDATED_PARTICIPANT_CODE,
            "participantCode.doesNotContain=" + DEFAULT_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByIdentificationCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where identificationCode equals to
        defaultInstitutionFiltering(
            "identificationCode.equals=" + DEFAULT_IDENTIFICATION_CODE,
            "identificationCode.equals=" + UPDATED_IDENTIFICATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByIdentificationCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where identificationCode in
        defaultInstitutionFiltering(
            "identificationCode.in=" + DEFAULT_IDENTIFICATION_CODE + "," + UPDATED_IDENTIFICATION_CODE,
            "identificationCode.in=" + UPDATED_IDENTIFICATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByIdentificationCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where identificationCode is not null
        defaultInstitutionFiltering("identificationCode.specified=true", "identificationCode.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByIdentificationCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where identificationCode contains
        defaultInstitutionFiltering(
            "identificationCode.contains=" + DEFAULT_IDENTIFICATION_CODE,
            "identificationCode.contains=" + UPDATED_IDENTIFICATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByIdentificationCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where identificationCode does not contain
        defaultInstitutionFiltering(
            "identificationCode.doesNotContain=" + UPDATED_IDENTIFICATION_CODE,
            "identificationCode.doesNotContain=" + DEFAULT_IDENTIFICATION_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByBeacBankCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where beacBankCode equals to
        defaultInstitutionFiltering("beacBankCode.equals=" + DEFAULT_BEAC_BANK_CODE, "beacBankCode.equals=" + UPDATED_BEAC_BANK_CODE);
    }

    @Test
    @Transactional
    void getAllInstitutionsByBeacBankCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where beacBankCode in
        defaultInstitutionFiltering(
            "beacBankCode.in=" + DEFAULT_BEAC_BANK_CODE + "," + UPDATED_BEAC_BANK_CODE,
            "beacBankCode.in=" + UPDATED_BEAC_BANK_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByBeacBankCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where beacBankCode is not null
        defaultInstitutionFiltering("beacBankCode.specified=true", "beacBankCode.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByBeacBankCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where beacBankCode contains
        defaultInstitutionFiltering("beacBankCode.contains=" + DEFAULT_BEAC_BANK_CODE, "beacBankCode.contains=" + UPDATED_BEAC_BANK_CODE);
    }

    @Test
    @Transactional
    void getAllInstitutionsByBeacBankCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where beacBankCode does not contain
        defaultInstitutionFiltering(
            "beacBankCode.doesNotContain=" + UPDATED_BEAC_BANK_CODE,
            "beacBankCode.doesNotContain=" + DEFAULT_BEAC_BANK_CODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByCountryIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where country equals to
        defaultInstitutionFiltering("country.equals=" + DEFAULT_COUNTRY, "country.equals=" + UPDATED_COUNTRY);
    }

    @Test
    @Transactional
    void getAllInstitutionsByCountryIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where country in
        defaultInstitutionFiltering("country.in=" + DEFAULT_COUNTRY + "," + UPDATED_COUNTRY, "country.in=" + UPDATED_COUNTRY);
    }

    @Test
    @Transactional
    void getAllInstitutionsByCountryIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where country is not null
        defaultInstitutionFiltering("country.specified=true", "country.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByCountryContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where country contains
        defaultInstitutionFiltering("country.contains=" + DEFAULT_COUNTRY, "country.contains=" + UPDATED_COUNTRY);
    }

    @Test
    @Transactional
    void getAllInstitutionsByCountryNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where country does not contain
        defaultInstitutionFiltering("country.doesNotContain=" + UPDATED_COUNTRY, "country.doesNotContain=" + DEFAULT_COUNTRY);
    }

    @Test
    @Transactional
    void getAllInstitutionsByCategoryIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where category equals to
        defaultInstitutionFiltering("category.equals=" + DEFAULT_CATEGORY, "category.equals=" + UPDATED_CATEGORY);
    }

    @Test
    @Transactional
    void getAllInstitutionsByCategoryIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where category in
        defaultInstitutionFiltering("category.in=" + DEFAULT_CATEGORY + "," + UPDATED_CATEGORY, "category.in=" + UPDATED_CATEGORY);
    }

    @Test
    @Transactional
    void getAllInstitutionsByCategoryIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where category is not null
        defaultInstitutionFiltering("category.specified=true", "category.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByMembershipModeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where membershipMode equals to
        defaultInstitutionFiltering("membershipMode.equals=" + DEFAULT_MEMBERSHIP_MODE, "membershipMode.equals=" + UPDATED_MEMBERSHIP_MODE);
    }

    @Test
    @Transactional
    void getAllInstitutionsByMembershipModeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where membershipMode in
        defaultInstitutionFiltering(
            "membershipMode.in=" + DEFAULT_MEMBERSHIP_MODE + "," + UPDATED_MEMBERSHIP_MODE,
            "membershipMode.in=" + UPDATED_MEMBERSHIP_MODE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByMembershipModeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where membershipMode is not null
        defaultInstitutionFiltering("membershipMode.specified=true", "membershipMode.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsBySettlementAccountRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where settlementAccountRef equals to
        defaultInstitutionFiltering(
            "settlementAccountRef.equals=" + DEFAULT_SETTLEMENT_ACCOUNT_REF,
            "settlementAccountRef.equals=" + UPDATED_SETTLEMENT_ACCOUNT_REF
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsBySettlementAccountRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where settlementAccountRef in
        defaultInstitutionFiltering(
            "settlementAccountRef.in=" + DEFAULT_SETTLEMENT_ACCOUNT_REF + "," + UPDATED_SETTLEMENT_ACCOUNT_REF,
            "settlementAccountRef.in=" + UPDATED_SETTLEMENT_ACCOUNT_REF
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsBySettlementAccountRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where settlementAccountRef is not null
        defaultInstitutionFiltering("settlementAccountRef.specified=true", "settlementAccountRef.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsBySettlementAccountRefContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where settlementAccountRef contains
        defaultInstitutionFiltering(
            "settlementAccountRef.contains=" + DEFAULT_SETTLEMENT_ACCOUNT_REF,
            "settlementAccountRef.contains=" + UPDATED_SETTLEMENT_ACCOUNT_REF
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsBySettlementAccountRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where settlementAccountRef does not contain
        defaultInstitutionFiltering(
            "settlementAccountRef.doesNotContain=" + UPDATED_SETTLEMENT_ACCOUNT_REF,
            "settlementAccountRef.doesNotContain=" + DEFAULT_SETTLEMENT_ACCOUNT_REF
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByRingFencedAccountRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ringFencedAccountRef equals to
        defaultInstitutionFiltering(
            "ringFencedAccountRef.equals=" + DEFAULT_RING_FENCED_ACCOUNT_REF,
            "ringFencedAccountRef.equals=" + UPDATED_RING_FENCED_ACCOUNT_REF
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByRingFencedAccountRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ringFencedAccountRef in
        defaultInstitutionFiltering(
            "ringFencedAccountRef.in=" + DEFAULT_RING_FENCED_ACCOUNT_REF + "," + UPDATED_RING_FENCED_ACCOUNT_REF,
            "ringFencedAccountRef.in=" + UPDATED_RING_FENCED_ACCOUNT_REF
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByRingFencedAccountRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ringFencedAccountRef is not null
        defaultInstitutionFiltering("ringFencedAccountRef.specified=true", "ringFencedAccountRef.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByRingFencedAccountRefContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ringFencedAccountRef contains
        defaultInstitutionFiltering(
            "ringFencedAccountRef.contains=" + DEFAULT_RING_FENCED_ACCOUNT_REF,
            "ringFencedAccountRef.contains=" + UPDATED_RING_FENCED_ACCOUNT_REF
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByRingFencedAccountRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ringFencedAccountRef does not contain
        defaultInstitutionFiltering(
            "ringFencedAccountRef.doesNotContain=" + UPDATED_RING_FENCED_ACCOUNT_REF,
            "ringFencedAccountRef.doesNotContain=" + DEFAULT_RING_FENCED_ACCOUNT_REF
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByJoinedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where joinedOn equals to
        defaultInstitutionFiltering("joinedOn.equals=" + DEFAULT_JOINED_ON, "joinedOn.equals=" + UPDATED_JOINED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByJoinedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where joinedOn in
        defaultInstitutionFiltering("joinedOn.in=" + DEFAULT_JOINED_ON + "," + UPDATED_JOINED_ON, "joinedOn.in=" + UPDATED_JOINED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByJoinedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where joinedOn is not null
        defaultInstitutionFiltering("joinedOn.specified=true", "joinedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByJoinedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where joinedOn is greater than or equal to
        defaultInstitutionFiltering("joinedOn.greaterThanOrEqual=" + DEFAULT_JOINED_ON, "joinedOn.greaterThanOrEqual=" + UPDATED_JOINED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByJoinedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where joinedOn is less than or equal to
        defaultInstitutionFiltering("joinedOn.lessThanOrEqual=" + DEFAULT_JOINED_ON, "joinedOn.lessThanOrEqual=" + SMALLER_JOINED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByJoinedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where joinedOn is less than
        defaultInstitutionFiltering("joinedOn.lessThan=" + UPDATED_JOINED_ON, "joinedOn.lessThan=" + DEFAULT_JOINED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByJoinedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where joinedOn is greater than
        defaultInstitutionFiltering("joinedOn.greaterThan=" + SMALLER_JOINED_ON, "joinedOn.greaterThan=" + DEFAULT_JOINED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where status equals to
        defaultInstitutionFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllInstitutionsByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where status in
        defaultInstitutionFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllInstitutionsByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where status is not null
        defaultInstitutionFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByComplianceStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where complianceState equals to
        defaultInstitutionFiltering(
            "complianceState.equals=" + DEFAULT_COMPLIANCE_STATE,
            "complianceState.equals=" + UPDATED_COMPLIANCE_STATE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByComplianceStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where complianceState in
        defaultInstitutionFiltering(
            "complianceState.in=" + DEFAULT_COMPLIANCE_STATE + "," + UPDATED_COMPLIANCE_STATE,
            "complianceState.in=" + UPDATED_COMPLIANCE_STATE
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByComplianceStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where complianceState is not null
        defaultInstitutionFiltering("complianceState.specified=true", "complianceState.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByAttestationIssuedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where attestationIssuedAt equals to
        defaultInstitutionFiltering(
            "attestationIssuedAt.equals=" + DEFAULT_ATTESTATION_ISSUED_AT,
            "attestationIssuedAt.equals=" + UPDATED_ATTESTATION_ISSUED_AT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByAttestationIssuedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where attestationIssuedAt in
        defaultInstitutionFiltering(
            "attestationIssuedAt.in=" + DEFAULT_ATTESTATION_ISSUED_AT + "," + UPDATED_ATTESTATION_ISSUED_AT,
            "attestationIssuedAt.in=" + UPDATED_ATTESTATION_ISSUED_AT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByAttestationIssuedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where attestationIssuedAt is not null
        defaultInstitutionFiltering("attestationIssuedAt.specified=true", "attestationIssuedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByGoLiveAuthorizedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where goLiveAuthorizedAt equals to
        defaultInstitutionFiltering(
            "goLiveAuthorizedAt.equals=" + DEFAULT_GO_LIVE_AUTHORIZED_AT,
            "goLiveAuthorizedAt.equals=" + UPDATED_GO_LIVE_AUTHORIZED_AT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByGoLiveAuthorizedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where goLiveAuthorizedAt in
        defaultInstitutionFiltering(
            "goLiveAuthorizedAt.in=" + DEFAULT_GO_LIVE_AUTHORIZED_AT + "," + UPDATED_GO_LIVE_AUTHORIZED_AT,
            "goLiveAuthorizedAt.in=" + UPDATED_GO_LIVE_AUTHORIZED_AT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByGoLiveAuthorizedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where goLiveAuthorizedAt is not null
        defaultInstitutionFiltering("goLiveAuthorizedAt.specified=true", "goLiveAuthorizedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenLinkCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openLinkCount equals to
        defaultInstitutionFiltering("openLinkCount.equals=" + DEFAULT_OPEN_LINK_COUNT, "openLinkCount.equals=" + UPDATED_OPEN_LINK_COUNT);
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenLinkCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openLinkCount in
        defaultInstitutionFiltering(
            "openLinkCount.in=" + DEFAULT_OPEN_LINK_COUNT + "," + UPDATED_OPEN_LINK_COUNT,
            "openLinkCount.in=" + UPDATED_OPEN_LINK_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenLinkCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openLinkCount is not null
        defaultInstitutionFiltering("openLinkCount.specified=true", "openLinkCount.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenLinkCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openLinkCount is greater than or equal to
        defaultInstitutionFiltering(
            "openLinkCount.greaterThanOrEqual=" + DEFAULT_OPEN_LINK_COUNT,
            "openLinkCount.greaterThanOrEqual=" + UPDATED_OPEN_LINK_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenLinkCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openLinkCount is less than or equal to
        defaultInstitutionFiltering(
            "openLinkCount.lessThanOrEqual=" + DEFAULT_OPEN_LINK_COUNT,
            "openLinkCount.lessThanOrEqual=" + SMALLER_OPEN_LINK_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenLinkCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openLinkCount is less than
        defaultInstitutionFiltering(
            "openLinkCount.lessThan=" + UPDATED_OPEN_LINK_COUNT,
            "openLinkCount.lessThan=" + DEFAULT_OPEN_LINK_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenLinkCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openLinkCount is greater than
        defaultInstitutionFiltering(
            "openLinkCount.greaterThan=" + SMALLER_OPEN_LINK_COUNT,
            "openLinkCount.greaterThan=" + DEFAULT_OPEN_LINK_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenSuspenseCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openSuspenseCount equals to
        defaultInstitutionFiltering(
            "openSuspenseCount.equals=" + DEFAULT_OPEN_SUSPENSE_COUNT,
            "openSuspenseCount.equals=" + UPDATED_OPEN_SUSPENSE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenSuspenseCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openSuspenseCount in
        defaultInstitutionFiltering(
            "openSuspenseCount.in=" + DEFAULT_OPEN_SUSPENSE_COUNT + "," + UPDATED_OPEN_SUSPENSE_COUNT,
            "openSuspenseCount.in=" + UPDATED_OPEN_SUSPENSE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenSuspenseCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openSuspenseCount is not null
        defaultInstitutionFiltering("openSuspenseCount.specified=true", "openSuspenseCount.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenSuspenseCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openSuspenseCount is greater than or equal to
        defaultInstitutionFiltering(
            "openSuspenseCount.greaterThanOrEqual=" + DEFAULT_OPEN_SUSPENSE_COUNT,
            "openSuspenseCount.greaterThanOrEqual=" + UPDATED_OPEN_SUSPENSE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenSuspenseCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openSuspenseCount is less than or equal to
        defaultInstitutionFiltering(
            "openSuspenseCount.lessThanOrEqual=" + DEFAULT_OPEN_SUSPENSE_COUNT,
            "openSuspenseCount.lessThanOrEqual=" + SMALLER_OPEN_SUSPENSE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenSuspenseCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openSuspenseCount is less than
        defaultInstitutionFiltering(
            "openSuspenseCount.lessThan=" + UPDATED_OPEN_SUSPENSE_COUNT,
            "openSuspenseCount.lessThan=" + DEFAULT_OPEN_SUSPENSE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByOpenSuspenseCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where openSuspenseCount is greater than
        defaultInstitutionFiltering(
            "openSuspenseCount.greaterThan=" + SMALLER_OPEN_SUSPENSE_COUNT,
            "openSuspenseCount.greaterThan=" + DEFAULT_OPEN_SUSPENSE_COUNT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where createdAt equals to
        defaultInstitutionFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllInstitutionsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where createdAt in
        defaultInstitutionFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllInstitutionsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where createdAt is not null
        defaultInstitutionFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactedOn equals to
        defaultInstitutionFiltering("contactedOn.equals=" + DEFAULT_CONTACTED_ON, "contactedOn.equals=" + UPDATED_CONTACTED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactedOn in
        defaultInstitutionFiltering(
            "contactedOn.in=" + DEFAULT_CONTACTED_ON + "," + UPDATED_CONTACTED_ON,
            "contactedOn.in=" + UPDATED_CONTACTED_ON
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactedOn is not null
        defaultInstitutionFiltering("contactedOn.specified=true", "contactedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactedOn is greater than or equal to
        defaultInstitutionFiltering(
            "contactedOn.greaterThanOrEqual=" + DEFAULT_CONTACTED_ON,
            "contactedOn.greaterThanOrEqual=" + UPDATED_CONTACTED_ON
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactedOn is less than or equal to
        defaultInstitutionFiltering(
            "contactedOn.lessThanOrEqual=" + DEFAULT_CONTACTED_ON,
            "contactedOn.lessThanOrEqual=" + SMALLER_CONTACTED_ON
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactedOn is less than
        defaultInstitutionFiltering("contactedOn.lessThan=" + UPDATED_CONTACTED_ON, "contactedOn.lessThan=" + DEFAULT_CONTACTED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactedOn is greater than
        defaultInstitutionFiltering("contactedOn.greaterThan=" + SMALLER_CONTACTED_ON, "contactedOn.greaterThan=" + DEFAULT_CONTACTED_ON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactOriginIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactOrigin equals to
        defaultInstitutionFiltering("contactOrigin.equals=" + DEFAULT_CONTACT_ORIGIN, "contactOrigin.equals=" + UPDATED_CONTACT_ORIGIN);
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactOriginIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactOrigin in
        defaultInstitutionFiltering(
            "contactOrigin.in=" + DEFAULT_CONTACT_ORIGIN + "," + UPDATED_CONTACT_ORIGIN,
            "contactOrigin.in=" + UPDATED_CONTACT_ORIGIN
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactOriginIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactOrigin is not null
        defaultInstitutionFiltering("contactOrigin.specified=true", "contactOrigin.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactOriginContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactOrigin contains
        defaultInstitutionFiltering("contactOrigin.contains=" + DEFAULT_CONTACT_ORIGIN, "contactOrigin.contains=" + UPDATED_CONTACT_ORIGIN);
    }

    @Test
    @Transactional
    void getAllInstitutionsByContactOriginNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where contactOrigin does not contain
        defaultInstitutionFiltering(
            "contactOrigin.doesNotContain=" + UPDATED_CONTACT_ORIGIN,
            "contactOrigin.doesNotContain=" + DEFAULT_CONTACT_ORIGIN
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByIntendedProjectIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where intendedProject equals to
        defaultInstitutionFiltering(
            "intendedProject.equals=" + DEFAULT_INTENDED_PROJECT,
            "intendedProject.equals=" + UPDATED_INTENDED_PROJECT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByIntendedProjectIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where intendedProject in
        defaultInstitutionFiltering(
            "intendedProject.in=" + DEFAULT_INTENDED_PROJECT + "," + UPDATED_INTENDED_PROJECT,
            "intendedProject.in=" + UPDATED_INTENDED_PROJECT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByIntendedProjectIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where intendedProject is not null
        defaultInstitutionFiltering("intendedProject.specified=true", "intendedProject.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByIntendedProjectContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where intendedProject contains
        defaultInstitutionFiltering(
            "intendedProject.contains=" + DEFAULT_INTENDED_PROJECT,
            "intendedProject.contains=" + UPDATED_INTENDED_PROJECT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByIntendedProjectNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where intendedProject does not contain
        defaultInstitutionFiltering(
            "intendedProject.doesNotContain=" + UPDATED_INTENDED_PROJECT,
            "intendedProject.doesNotContain=" + DEFAULT_INTENDED_PROJECT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByDdrmOfficerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ddrmOfficerId equals to
        defaultInstitutionFiltering("ddrmOfficerId.equals=" + DEFAULT_DDRM_OFFICER_ID, "ddrmOfficerId.equals=" + UPDATED_DDRM_OFFICER_ID);
    }

    @Test
    @Transactional
    void getAllInstitutionsByDdrmOfficerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ddrmOfficerId in
        defaultInstitutionFiltering(
            "ddrmOfficerId.in=" + DEFAULT_DDRM_OFFICER_ID + "," + UPDATED_DDRM_OFFICER_ID,
            "ddrmOfficerId.in=" + UPDATED_DDRM_OFFICER_ID
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByDdrmOfficerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ddrmOfficerId is not null
        defaultInstitutionFiltering("ddrmOfficerId.specified=true", "ddrmOfficerId.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByDdrmOfficerNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ddrmOfficerName equals to
        defaultInstitutionFiltering(
            "ddrmOfficerName.equals=" + DEFAULT_DDRM_OFFICER_NAME,
            "ddrmOfficerName.equals=" + UPDATED_DDRM_OFFICER_NAME
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByDdrmOfficerNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ddrmOfficerName in
        defaultInstitutionFiltering(
            "ddrmOfficerName.in=" + DEFAULT_DDRM_OFFICER_NAME + "," + UPDATED_DDRM_OFFICER_NAME,
            "ddrmOfficerName.in=" + UPDATED_DDRM_OFFICER_NAME
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByDdrmOfficerNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ddrmOfficerName is not null
        defaultInstitutionFiltering("ddrmOfficerName.specified=true", "ddrmOfficerName.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByDdrmOfficerNameContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ddrmOfficerName contains
        defaultInstitutionFiltering(
            "ddrmOfficerName.contains=" + DEFAULT_DDRM_OFFICER_NAME,
            "ddrmOfficerName.contains=" + UPDATED_DDRM_OFFICER_NAME
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByDdrmOfficerNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where ddrmOfficerName does not contain
        defaultInstitutionFiltering(
            "ddrmOfficerName.doesNotContain=" + UPDATED_DDRM_OFFICER_NAME,
            "ddrmOfficerName.doesNotContain=" + DEFAULT_DDRM_OFFICER_NAME
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByAdmittedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where admittedAt equals to
        defaultInstitutionFiltering("admittedAt.equals=" + DEFAULT_ADMITTED_AT, "admittedAt.equals=" + UPDATED_ADMITTED_AT);
    }

    @Test
    @Transactional
    void getAllInstitutionsByAdmittedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where admittedAt in
        defaultInstitutionFiltering(
            "admittedAt.in=" + DEFAULT_ADMITTED_AT + "," + UPDATED_ADMITTED_AT,
            "admittedAt.in=" + UPDATED_ADMITTED_AT
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByAdmittedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where admittedAt is not null
        defaultInstitutionFiltering("admittedAt.specified=true", "admittedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByAdmittedByIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where admittedById equals to
        defaultInstitutionFiltering("admittedById.equals=" + DEFAULT_ADMITTED_BY_ID, "admittedById.equals=" + UPDATED_ADMITTED_BY_ID);
    }

    @Test
    @Transactional
    void getAllInstitutionsByAdmittedByIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where admittedById in
        defaultInstitutionFiltering(
            "admittedById.in=" + DEFAULT_ADMITTED_BY_ID + "," + UPDATED_ADMITTED_BY_ID,
            "admittedById.in=" + UPDATED_ADMITTED_BY_ID
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByAdmittedByIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where admittedById is not null
        defaultInstitutionFiltering("admittedById.specified=true", "admittedById.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByClosureReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where closureReason equals to
        defaultInstitutionFiltering("closureReason.equals=" + DEFAULT_CLOSURE_REASON, "closureReason.equals=" + UPDATED_CLOSURE_REASON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByClosureReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where closureReason in
        defaultInstitutionFiltering(
            "closureReason.in=" + DEFAULT_CLOSURE_REASON + "," + UPDATED_CLOSURE_REASON,
            "closureReason.in=" + UPDATED_CLOSURE_REASON
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByClosureReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where closureReason is not null
        defaultInstitutionFiltering("closureReason.specified=true", "closureReason.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsByClosureReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where closureReason contains
        defaultInstitutionFiltering("closureReason.contains=" + DEFAULT_CLOSURE_REASON, "closureReason.contains=" + UPDATED_CLOSURE_REASON);
    }

    @Test
    @Transactional
    void getAllInstitutionsByClosureReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where closureReason does not contain
        defaultInstitutionFiltering(
            "closureReason.doesNotContain=" + UPDATED_CLOSURE_REASON,
            "closureReason.doesNotContain=" + DEFAULT_CLOSURE_REASON
        );
    }

    @Test
    @Transactional
    void getAllInstitutionsByClosedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where closedAt equals to
        defaultInstitutionFiltering("closedAt.equals=" + DEFAULT_CLOSED_AT, "closedAt.equals=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllInstitutionsByClosedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where closedAt in
        defaultInstitutionFiltering("closedAt.in=" + DEFAULT_CLOSED_AT + "," + UPDATED_CLOSED_AT, "closedAt.in=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllInstitutionsByClosedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        // Get all the institutionList where closedAt is not null
        defaultInstitutionFiltering("closedAt.specified=true", "closedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllInstitutionsBySettlementBankIsEqualToSomething() throws Exception {
        Institution settlementBank;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institutionRepository.saveAndFlush(institution);
            settlementBank = InstitutionResourceIT.createEntity();
        } else {
            settlementBank = TestUtil.findAll(em, Institution.class).getFirst();
        }
        em.persist(settlementBank);
        em.flush();
        institution.setSettlementBank(settlementBank);
        institutionRepository.saveAndFlush(institution);
        UUID settlementBankId = settlementBank.getId();
        // Get all the institutionList where settlementBank equals to settlementBankId
        defaultInstitutionShouldBeFound("settlementBankId.equals=" + settlementBankId);

        // Get all the institutionList where settlementBank equals to UUID.randomUUID()
        defaultInstitutionShouldNotBeFound("settlementBankId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllInstitutionsBySponsorBankIsEqualToSomething() throws Exception {
        Institution sponsorBank;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institutionRepository.saveAndFlush(institution);
            sponsorBank = InstitutionResourceIT.createEntity();
        } else {
            sponsorBank = TestUtil.findAll(em, Institution.class).getFirst();
        }
        em.persist(sponsorBank);
        em.flush();
        institution.setSponsorBank(sponsorBank);
        institutionRepository.saveAndFlush(institution);
        UUID sponsorBankId = sponsorBank.getId();
        // Get all the institutionList where sponsorBank equals to sponsorBankId
        defaultInstitutionShouldBeFound("sponsorBankId.equals=" + sponsorBankId);

        // Get all the institutionList where sponsorBank equals to UUID.randomUUID()
        defaultInstitutionShouldNotBeFound("sponsorBankId.equals=" + UUID.randomUUID());
    }

    private void defaultInstitutionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultInstitutionShouldBeFound(shouldBeFound);
        defaultInstitutionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultInstitutionShouldBeFound(String filter) throws Exception {
        restInstitutionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(institution.getId().toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].acronym").value(hasItem(DEFAULT_ACRONYM)))
            .andExpect(jsonPath("$.[*].participantCode").value(hasItem(DEFAULT_PARTICIPANT_CODE)))
            .andExpect(jsonPath("$.[*].identificationCode").value(hasItem(DEFAULT_IDENTIFICATION_CODE)))
            .andExpect(jsonPath("$.[*].beacBankCode").value(hasItem(DEFAULT_BEAC_BANK_CODE)))
            .andExpect(jsonPath("$.[*].country").value(hasItem(DEFAULT_COUNTRY)))
            .andExpect(jsonPath("$.[*].category").value(hasItem(DEFAULT_CATEGORY.toString())))
            .andExpect(jsonPath("$.[*].membershipMode").value(hasItem(DEFAULT_MEMBERSHIP_MODE.toString())))
            .andExpect(jsonPath("$.[*].settlementAccountRef").value(hasItem(DEFAULT_SETTLEMENT_ACCOUNT_REF)))
            .andExpect(jsonPath("$.[*].ringFencedAccountRef").value(hasItem(DEFAULT_RING_FENCED_ACCOUNT_REF)))
            .andExpect(jsonPath("$.[*].joinedOn").value(hasItem(DEFAULT_JOINED_ON.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].complianceState").value(hasItem(DEFAULT_COMPLIANCE_STATE.toString())))
            .andExpect(jsonPath("$.[*].attestationIssuedAt").value(hasItem(DEFAULT_ATTESTATION_ISSUED_AT.toString())))
            .andExpect(jsonPath("$.[*].goLiveAuthorizedAt").value(hasItem(DEFAULT_GO_LIVE_AUTHORIZED_AT.toString())))
            .andExpect(jsonPath("$.[*].openLinkCount").value(hasItem(DEFAULT_OPEN_LINK_COUNT)))
            .andExpect(jsonPath("$.[*].openSuspenseCount").value(hasItem(DEFAULT_OPEN_SUSPENSE_COUNT)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].contactedOn").value(hasItem(DEFAULT_CONTACTED_ON.toString())))
            .andExpect(jsonPath("$.[*].contactOrigin").value(hasItem(DEFAULT_CONTACT_ORIGIN)))
            .andExpect(jsonPath("$.[*].intendedProject").value(hasItem(DEFAULT_INTENDED_PROJECT)))
            .andExpect(jsonPath("$.[*].ddrmOfficerId").value(hasItem(DEFAULT_DDRM_OFFICER_ID.toString())))
            .andExpect(jsonPath("$.[*].ddrmOfficerName").value(hasItem(DEFAULT_DDRM_OFFICER_NAME)))
            .andExpect(jsonPath("$.[*].admittedAt").value(hasItem(DEFAULT_ADMITTED_AT.toString())))
            .andExpect(jsonPath("$.[*].admittedById").value(hasItem(DEFAULT_ADMITTED_BY_ID.toString())))
            .andExpect(jsonPath("$.[*].closureReason").value(hasItem(DEFAULT_CLOSURE_REASON)))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())));

        // Check, that the count call also returns 1
        restInstitutionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultInstitutionShouldNotBeFound(String filter) throws Exception {
        restInstitutionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restInstitutionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingInstitution() throws Exception {
        // Get the institution
        restInstitutionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingInstitution() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the institution
        Institution updatedInstitution = institutionRepository.findById(institution.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedInstitution are not directly saved in db
        em.detach(updatedInstitution);
        updatedInstitution
            .name(UPDATED_NAME)
            .acronym(UPDATED_ACRONYM)
            .participantCode(UPDATED_PARTICIPANT_CODE)
            .identificationCode(UPDATED_IDENTIFICATION_CODE)
            .beacBankCode(UPDATED_BEAC_BANK_CODE)
            .country(UPDATED_COUNTRY)
            .category(UPDATED_CATEGORY)
            .membershipMode(UPDATED_MEMBERSHIP_MODE)
            .settlementAccountRef(UPDATED_SETTLEMENT_ACCOUNT_REF)
            .ringFencedAccountRef(UPDATED_RING_FENCED_ACCOUNT_REF)
            .joinedOn(UPDATED_JOINED_ON)
            .status(UPDATED_STATUS)
            .complianceState(UPDATED_COMPLIANCE_STATE)
            .attestationIssuedAt(UPDATED_ATTESTATION_ISSUED_AT)
            .goLiveAuthorizedAt(UPDATED_GO_LIVE_AUTHORIZED_AT)
            .openLinkCount(UPDATED_OPEN_LINK_COUNT)
            .openSuspenseCount(UPDATED_OPEN_SUSPENSE_COUNT)
            .createdAt(UPDATED_CREATED_AT)
            .contactedOn(UPDATED_CONTACTED_ON)
            .contactOrigin(UPDATED_CONTACT_ORIGIN)
            .intendedProject(UPDATED_INTENDED_PROJECT)
            .ddrmOfficerId(UPDATED_DDRM_OFFICER_ID)
            .ddrmOfficerName(UPDATED_DDRM_OFFICER_NAME)
            .admittedAt(UPDATED_ADMITTED_AT)
            .admittedById(UPDATED_ADMITTED_BY_ID)
            .closureReason(UPDATED_CLOSURE_REASON)
            .closedAt(UPDATED_CLOSED_AT);
        InstitutionDTO institutionDTO = institutionMapper.toDto(updatedInstitution);

        restInstitutionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, institutionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isOk());

        // Validate the Institution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedInstitutionToMatchAllProperties(updatedInstitution);
    }

    @Test
    @Transactional
    void putNonExistingInstitution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        institution.setId(UUID.randomUUID());

        // Create the Institution
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInstitutionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, institutionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Institution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchInstitution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        institution.setId(UUID.randomUUID());

        // Create the Institution
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInstitutionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Institution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamInstitution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        institution.setId(UUID.randomUUID());

        // Create the Institution
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInstitutionMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(institutionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Institution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateInstitutionWithPatch() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the institution using partial update
        Institution partialUpdatedInstitution = new Institution();
        partialUpdatedInstitution.setId(institution.getId());

        partialUpdatedInstitution
            .name(UPDATED_NAME)
            .participantCode(UPDATED_PARTICIPANT_CODE)
            .country(UPDATED_COUNTRY)
            .membershipMode(UPDATED_MEMBERSHIP_MODE)
            .settlementAccountRef(UPDATED_SETTLEMENT_ACCOUNT_REF)
            .joinedOn(UPDATED_JOINED_ON)
            .complianceState(UPDATED_COMPLIANCE_STATE)
            .createdAt(UPDATED_CREATED_AT)
            .contactOrigin(UPDATED_CONTACT_ORIGIN)
            .ddrmOfficerId(UPDATED_DDRM_OFFICER_ID);

        restInstitutionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedInstitution.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedInstitution))
            )
            .andExpect(status().isOk());

        // Validate the Institution in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInstitutionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedInstitution, institution),
            getPersistedInstitution(institution)
        );
    }

    @Test
    @Transactional
    void fullUpdateInstitutionWithPatch() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the institution using partial update
        Institution partialUpdatedInstitution = new Institution();
        partialUpdatedInstitution.setId(institution.getId());

        partialUpdatedInstitution
            .name(UPDATED_NAME)
            .acronym(UPDATED_ACRONYM)
            .participantCode(UPDATED_PARTICIPANT_CODE)
            .identificationCode(UPDATED_IDENTIFICATION_CODE)
            .beacBankCode(UPDATED_BEAC_BANK_CODE)
            .country(UPDATED_COUNTRY)
            .category(UPDATED_CATEGORY)
            .membershipMode(UPDATED_MEMBERSHIP_MODE)
            .settlementAccountRef(UPDATED_SETTLEMENT_ACCOUNT_REF)
            .ringFencedAccountRef(UPDATED_RING_FENCED_ACCOUNT_REF)
            .joinedOn(UPDATED_JOINED_ON)
            .status(UPDATED_STATUS)
            .complianceState(UPDATED_COMPLIANCE_STATE)
            .attestationIssuedAt(UPDATED_ATTESTATION_ISSUED_AT)
            .goLiveAuthorizedAt(UPDATED_GO_LIVE_AUTHORIZED_AT)
            .openLinkCount(UPDATED_OPEN_LINK_COUNT)
            .openSuspenseCount(UPDATED_OPEN_SUSPENSE_COUNT)
            .createdAt(UPDATED_CREATED_AT)
            .contactedOn(UPDATED_CONTACTED_ON)
            .contactOrigin(UPDATED_CONTACT_ORIGIN)
            .intendedProject(UPDATED_INTENDED_PROJECT)
            .ddrmOfficerId(UPDATED_DDRM_OFFICER_ID)
            .ddrmOfficerName(UPDATED_DDRM_OFFICER_NAME)
            .admittedAt(UPDATED_ADMITTED_AT)
            .admittedById(UPDATED_ADMITTED_BY_ID)
            .closureReason(UPDATED_CLOSURE_REASON)
            .closedAt(UPDATED_CLOSED_AT);

        restInstitutionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedInstitution.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedInstitution))
            )
            .andExpect(status().isOk());

        // Validate the Institution in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInstitutionUpdatableFieldsEquals(partialUpdatedInstitution, getPersistedInstitution(partialUpdatedInstitution));
    }

    @Test
    @Transactional
    void patchNonExistingInstitution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        institution.setId(UUID.randomUUID());

        // Create the Institution
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInstitutionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, institutionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Institution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchInstitution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        institution.setId(UUID.randomUUID());

        // Create the Institution
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInstitutionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Institution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamInstitution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        institution.setId(UUID.randomUUID());

        // Create the Institution
        InstitutionDTO institutionDTO = institutionMapper.toDto(institution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInstitutionMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(institutionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Institution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteInstitution() throws Exception {
        // Initialize the database
        insertedInstitution = institutionRepository.saveAndFlush(institution);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the institution
        restInstitutionMockMvc
            .perform(delete(ENTITY_API_URL_ID, institution.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return institutionRepository.count();
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

    protected Institution getPersistedInstitution(Institution institution) {
        return institutionRepository.findById(institution.getId()).orElseThrow();
    }

    protected void assertPersistedInstitutionToMatchAllProperties(Institution expectedInstitution) {
        assertInstitutionAllPropertiesEquals(expectedInstitution, getPersistedInstitution(expectedInstitution));
    }

    protected void assertPersistedInstitutionToMatchUpdatableProperties(Institution expectedInstitution) {
        assertInstitutionAllUpdatablePropertiesEquals(expectedInstitution, getPersistedInstitution(expectedInstitution));
    }
}
