package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.LiaisonAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferenceCatalog;
import cm.gimac.des.homologation.domain.enumeration.LiaisonState;
import cm.gimac.des.homologation.domain.enumeration.ProjectType;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.service.LiaisonService;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.mapper.LiaisonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link LiaisonResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class LiaisonResourceIT {

    private static final String DEFAULT_LIAISON_CODE = "LIA-8147-212709";
    private static final String UPDATED_LIAISON_CODE = "LIA-8128-437635";

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final ProjectType DEFAULT_PROJECT_TYPE = ProjectType.CARTE_INTERBANCAIRE;
    private static final ProjectType UPDATED_PROJECT_TYPE = ProjectType.PAIEMENT_MOBILE;

    private static final LocalDate DEFAULT_OPENED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_OPENED_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_OPENED_ON = LocalDate.ofEpochDay(-1L);

    private static final UUID DEFAULT_PROJECT_LEAD_ID = UUID.randomUUID();
    private static final UUID UPDATED_PROJECT_LEAD_ID = UUID.randomUUID();

    private static final LiaisonState DEFAULT_STATE = LiaisonState.OUVERTE;
    private static final LiaisonState UPDATED_STATE = LiaisonState.EN_ESSAIS;

    private static final String DEFAULT_CAMPAIGN_REF = "AAAAAAAAAA";
    private static final String UPDATED_CAMPAIGN_REF = "BBBBBBBBBB";

    private static final Instant DEFAULT_READY_FOR_CERTIFICATION_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_READY_FOR_CERTIFICATION_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final LocalDate DEFAULT_COMMISSIONED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_COMMISSIONED_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_COMMISSIONED_ON = LocalDate.ofEpochDay(-1L);

    private static final String DEFAULT_STATE_REASON = "AAAAAAAAAA";
    private static final String UPDATED_STATE_REASON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/liaisons";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LiaisonRepository liaisonRepository;

    @Mock
    private LiaisonRepository liaisonRepositoryMock;

    @Autowired
    private LiaisonMapper liaisonMapper;

    @Mock
    private LiaisonService liaisonServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restLiaisonMockMvc;

    private Liaison liaison;

    private Liaison insertedLiaison;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Liaison createEntity(EntityManager em) {
        Liaison liaison = new Liaison()
            .liaisonCode(DEFAULT_LIAISON_CODE)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .projectType(DEFAULT_PROJECT_TYPE)
            .openedOn(DEFAULT_OPENED_ON)
            .projectLeadId(DEFAULT_PROJECT_LEAD_ID)
            .state(DEFAULT_STATE)
            .campaignRef(DEFAULT_CAMPAIGN_REF)
            .readyForCertificationAt(DEFAULT_READY_FOR_CERTIFICATION_AT)
            .commissionedOn(DEFAULT_COMMISSIONED_ON)
            .stateReason(DEFAULT_STATE_REASON);
        // Add required entity
        ReferenceCatalog referenceCatalog;
        if (TestUtil.findAll(em, ReferenceCatalog.class).isEmpty()) {
            referenceCatalog = ReferenceCatalogResourceIT.createEntity();
            em.persist(referenceCatalog);
            em.flush();
        } else {
            referenceCatalog = TestUtil.findAll(em, ReferenceCatalog.class).get(0);
        }
        liaison.setCatalog(referenceCatalog);
        return liaison;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Liaison createUpdatedEntity(EntityManager em) {
        Liaison updatedLiaison = new Liaison()
            .liaisonCode(UPDATED_LIAISON_CODE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .projectType(UPDATED_PROJECT_TYPE)
            .openedOn(UPDATED_OPENED_ON)
            .projectLeadId(UPDATED_PROJECT_LEAD_ID)
            .state(UPDATED_STATE)
            .campaignRef(UPDATED_CAMPAIGN_REF)
            .readyForCertificationAt(UPDATED_READY_FOR_CERTIFICATION_AT)
            .commissionedOn(UPDATED_COMMISSIONED_ON)
            .stateReason(UPDATED_STATE_REASON);
        // Add required entity
        ReferenceCatalog referenceCatalog;
        if (TestUtil.findAll(em, ReferenceCatalog.class).isEmpty()) {
            referenceCatalog = ReferenceCatalogResourceIT.createUpdatedEntity();
            em.persist(referenceCatalog);
            em.flush();
        } else {
            referenceCatalog = TestUtil.findAll(em, ReferenceCatalog.class).get(0);
        }
        updatedLiaison.setCatalog(referenceCatalog);
        return updatedLiaison;
    }

    @BeforeEach
    void initTest() {
        liaison = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedLiaison != null) {
            liaisonRepository.delete(insertedLiaison);
            insertedLiaison = null;
        }
    }

    @Test
    @Transactional
    void createLiaison() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);
        var returnedLiaisonDTO = om.readValue(
            restLiaisonMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            LiaisonDTO.class
        );

        // Validate the Liaison in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLiaison = liaisonMapper.toEntity(returnedLiaisonDTO);
        assertLiaisonUpdatableFieldsEquals(returnedLiaison, getPersistedLiaison(returnedLiaison));

        insertedLiaison = returnedLiaison;
    }

    @Test
    @Transactional
    void createLiaisonWithExistingId() throws Exception {
        // Create the Liaison with an existing ID
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkLiaisonCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setLiaisonCode(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInstitutionIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setInstitutionId(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkProjectTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setProjectType(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOpenedOnIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setOpenedOn(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkProjectLeadIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setProjectLeadId(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        liaison.setState(null);

        // Create the Liaison, which fails.
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        restLiaisonMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllLiaisons() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList
        restLiaisonMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(liaison.getId().toString())))
            .andExpect(jsonPath("$.[*].liaisonCode").value(hasItem(DEFAULT_LIAISON_CODE)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].projectType").value(hasItem(DEFAULT_PROJECT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].openedOn").value(hasItem(DEFAULT_OPENED_ON.toString())))
            .andExpect(jsonPath("$.[*].projectLeadId").value(hasItem(DEFAULT_PROJECT_LEAD_ID.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].campaignRef").value(hasItem(DEFAULT_CAMPAIGN_REF)))
            .andExpect(jsonPath("$.[*].readyForCertificationAt").value(hasItem(DEFAULT_READY_FOR_CERTIFICATION_AT.toString())))
            .andExpect(jsonPath("$.[*].commissionedOn").value(hasItem(DEFAULT_COMMISSIONED_ON.toString())))
            .andExpect(jsonPath("$.[*].stateReason").value(hasItem(DEFAULT_STATE_REASON)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLiaisonsWithEagerRelationshipsIsEnabled() throws Exception {
        when(liaisonServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restLiaisonMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(liaisonServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLiaisonsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(liaisonServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restLiaisonMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(liaisonRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getLiaison() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get the liaison
        restLiaisonMockMvc
            .perform(get(ENTITY_API_URL_ID, liaison.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(liaison.getId().toString()))
            .andExpect(jsonPath("$.liaisonCode").value(DEFAULT_LIAISON_CODE))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.projectType").value(DEFAULT_PROJECT_TYPE.toString()))
            .andExpect(jsonPath("$.openedOn").value(DEFAULT_OPENED_ON.toString()))
            .andExpect(jsonPath("$.projectLeadId").value(DEFAULT_PROJECT_LEAD_ID.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.campaignRef").value(DEFAULT_CAMPAIGN_REF))
            .andExpect(jsonPath("$.readyForCertificationAt").value(DEFAULT_READY_FOR_CERTIFICATION_AT.toString()))
            .andExpect(jsonPath("$.commissionedOn").value(DEFAULT_COMMISSIONED_ON.toString()))
            .andExpect(jsonPath("$.stateReason").value(DEFAULT_STATE_REASON));
    }

    @Test
    @Transactional
    void getLiaisonsByIdFiltering() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        UUID id = liaison.getId();

        defaultLiaisonFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllLiaisonsByLiaisonCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where liaisonCode equals to
        defaultLiaisonFiltering("liaisonCode.equals=" + DEFAULT_LIAISON_CODE, "liaisonCode.equals=" + UPDATED_LIAISON_CODE);
    }

    @Test
    @Transactional
    void getAllLiaisonsByLiaisonCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where liaisonCode in
        defaultLiaisonFiltering(
            "liaisonCode.in=" + DEFAULT_LIAISON_CODE + "," + UPDATED_LIAISON_CODE,
            "liaisonCode.in=" + UPDATED_LIAISON_CODE
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByLiaisonCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where liaisonCode is not null
        defaultLiaisonFiltering("liaisonCode.specified=true", "liaisonCode.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByLiaisonCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where liaisonCode contains
        defaultLiaisonFiltering("liaisonCode.contains=" + DEFAULT_LIAISON_CODE, "liaisonCode.contains=" + UPDATED_LIAISON_CODE);
    }

    @Test
    @Transactional
    void getAllLiaisonsByLiaisonCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where liaisonCode does not contain
        defaultLiaisonFiltering("liaisonCode.doesNotContain=" + UPDATED_LIAISON_CODE, "liaisonCode.doesNotContain=" + DEFAULT_LIAISON_CODE);
    }

    @Test
    @Transactional
    void getAllLiaisonsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where institutionId equals to
        defaultLiaisonFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllLiaisonsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where institutionId in
        defaultLiaisonFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where institutionId is not null
        defaultLiaisonFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByProjectTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where projectType equals to
        defaultLiaisonFiltering("projectType.equals=" + DEFAULT_PROJECT_TYPE, "projectType.equals=" + UPDATED_PROJECT_TYPE);
    }

    @Test
    @Transactional
    void getAllLiaisonsByProjectTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where projectType in
        defaultLiaisonFiltering(
            "projectType.in=" + DEFAULT_PROJECT_TYPE + "," + UPDATED_PROJECT_TYPE,
            "projectType.in=" + UPDATED_PROJECT_TYPE
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByProjectTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where projectType is not null
        defaultLiaisonFiltering("projectType.specified=true", "projectType.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByOpenedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where openedOn equals to
        defaultLiaisonFiltering("openedOn.equals=" + DEFAULT_OPENED_ON, "openedOn.equals=" + UPDATED_OPENED_ON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByOpenedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where openedOn in
        defaultLiaisonFiltering("openedOn.in=" + DEFAULT_OPENED_ON + "," + UPDATED_OPENED_ON, "openedOn.in=" + UPDATED_OPENED_ON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByOpenedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where openedOn is not null
        defaultLiaisonFiltering("openedOn.specified=true", "openedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByOpenedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where openedOn is greater than or equal to
        defaultLiaisonFiltering("openedOn.greaterThanOrEqual=" + DEFAULT_OPENED_ON, "openedOn.greaterThanOrEqual=" + UPDATED_OPENED_ON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByOpenedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where openedOn is less than or equal to
        defaultLiaisonFiltering("openedOn.lessThanOrEqual=" + DEFAULT_OPENED_ON, "openedOn.lessThanOrEqual=" + SMALLER_OPENED_ON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByOpenedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where openedOn is less than
        defaultLiaisonFiltering("openedOn.lessThan=" + UPDATED_OPENED_ON, "openedOn.lessThan=" + DEFAULT_OPENED_ON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByOpenedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where openedOn is greater than
        defaultLiaisonFiltering("openedOn.greaterThan=" + SMALLER_OPENED_ON, "openedOn.greaterThan=" + DEFAULT_OPENED_ON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByProjectLeadIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where projectLeadId equals to
        defaultLiaisonFiltering("projectLeadId.equals=" + DEFAULT_PROJECT_LEAD_ID, "projectLeadId.equals=" + UPDATED_PROJECT_LEAD_ID);
    }

    @Test
    @Transactional
    void getAllLiaisonsByProjectLeadIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where projectLeadId in
        defaultLiaisonFiltering(
            "projectLeadId.in=" + DEFAULT_PROJECT_LEAD_ID + "," + UPDATED_PROJECT_LEAD_ID,
            "projectLeadId.in=" + UPDATED_PROJECT_LEAD_ID
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByProjectLeadIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where projectLeadId is not null
        defaultLiaisonFiltering("projectLeadId.specified=true", "projectLeadId.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where state equals to
        defaultLiaisonFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllLiaisonsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where state in
        defaultLiaisonFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllLiaisonsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where state is not null
        defaultLiaisonFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByCampaignRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where campaignRef equals to
        defaultLiaisonFiltering("campaignRef.equals=" + DEFAULT_CAMPAIGN_REF, "campaignRef.equals=" + UPDATED_CAMPAIGN_REF);
    }

    @Test
    @Transactional
    void getAllLiaisonsByCampaignRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where campaignRef in
        defaultLiaisonFiltering(
            "campaignRef.in=" + DEFAULT_CAMPAIGN_REF + "," + UPDATED_CAMPAIGN_REF,
            "campaignRef.in=" + UPDATED_CAMPAIGN_REF
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByCampaignRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where campaignRef is not null
        defaultLiaisonFiltering("campaignRef.specified=true", "campaignRef.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByCampaignRefContainsSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where campaignRef contains
        defaultLiaisonFiltering("campaignRef.contains=" + DEFAULT_CAMPAIGN_REF, "campaignRef.contains=" + UPDATED_CAMPAIGN_REF);
    }

    @Test
    @Transactional
    void getAllLiaisonsByCampaignRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where campaignRef does not contain
        defaultLiaisonFiltering("campaignRef.doesNotContain=" + UPDATED_CAMPAIGN_REF, "campaignRef.doesNotContain=" + DEFAULT_CAMPAIGN_REF);
    }

    @Test
    @Transactional
    void getAllLiaisonsByReadyForCertificationAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where readyForCertificationAt equals to
        defaultLiaisonFiltering(
            "readyForCertificationAt.equals=" + DEFAULT_READY_FOR_CERTIFICATION_AT,
            "readyForCertificationAt.equals=" + UPDATED_READY_FOR_CERTIFICATION_AT
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByReadyForCertificationAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where readyForCertificationAt in
        defaultLiaisonFiltering(
            "readyForCertificationAt.in=" + DEFAULT_READY_FOR_CERTIFICATION_AT + "," + UPDATED_READY_FOR_CERTIFICATION_AT,
            "readyForCertificationAt.in=" + UPDATED_READY_FOR_CERTIFICATION_AT
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByReadyForCertificationAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where readyForCertificationAt is not null
        defaultLiaisonFiltering("readyForCertificationAt.specified=true", "readyForCertificationAt.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByCommissionedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where commissionedOn equals to
        defaultLiaisonFiltering("commissionedOn.equals=" + DEFAULT_COMMISSIONED_ON, "commissionedOn.equals=" + UPDATED_COMMISSIONED_ON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByCommissionedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where commissionedOn in
        defaultLiaisonFiltering(
            "commissionedOn.in=" + DEFAULT_COMMISSIONED_ON + "," + UPDATED_COMMISSIONED_ON,
            "commissionedOn.in=" + UPDATED_COMMISSIONED_ON
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByCommissionedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where commissionedOn is not null
        defaultLiaisonFiltering("commissionedOn.specified=true", "commissionedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByCommissionedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where commissionedOn is greater than or equal to
        defaultLiaisonFiltering(
            "commissionedOn.greaterThanOrEqual=" + DEFAULT_COMMISSIONED_ON,
            "commissionedOn.greaterThanOrEqual=" + UPDATED_COMMISSIONED_ON
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByCommissionedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where commissionedOn is less than or equal to
        defaultLiaisonFiltering(
            "commissionedOn.lessThanOrEqual=" + DEFAULT_COMMISSIONED_ON,
            "commissionedOn.lessThanOrEqual=" + SMALLER_COMMISSIONED_ON
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByCommissionedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where commissionedOn is less than
        defaultLiaisonFiltering("commissionedOn.lessThan=" + UPDATED_COMMISSIONED_ON, "commissionedOn.lessThan=" + DEFAULT_COMMISSIONED_ON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByCommissionedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where commissionedOn is greater than
        defaultLiaisonFiltering(
            "commissionedOn.greaterThan=" + SMALLER_COMMISSIONED_ON,
            "commissionedOn.greaterThan=" + DEFAULT_COMMISSIONED_ON
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByStateReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where stateReason equals to
        defaultLiaisonFiltering("stateReason.equals=" + DEFAULT_STATE_REASON, "stateReason.equals=" + UPDATED_STATE_REASON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByStateReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where stateReason in
        defaultLiaisonFiltering(
            "stateReason.in=" + DEFAULT_STATE_REASON + "," + UPDATED_STATE_REASON,
            "stateReason.in=" + UPDATED_STATE_REASON
        );
    }

    @Test
    @Transactional
    void getAllLiaisonsByStateReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where stateReason is not null
        defaultLiaisonFiltering("stateReason.specified=true", "stateReason.specified=false");
    }

    @Test
    @Transactional
    void getAllLiaisonsByStateReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where stateReason contains
        defaultLiaisonFiltering("stateReason.contains=" + DEFAULT_STATE_REASON, "stateReason.contains=" + UPDATED_STATE_REASON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByStateReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        // Get all the liaisonList where stateReason does not contain
        defaultLiaisonFiltering("stateReason.doesNotContain=" + UPDATED_STATE_REASON, "stateReason.doesNotContain=" + DEFAULT_STATE_REASON);
    }

    @Test
    @Transactional
    void getAllLiaisonsByCatalogIsEqualToSomething() throws Exception {
        ReferenceCatalog catalog;
        if (TestUtil.findAll(em, ReferenceCatalog.class).isEmpty()) {
            liaisonRepository.saveAndFlush(liaison);
            catalog = ReferenceCatalogResourceIT.createEntity();
        } else {
            catalog = TestUtil.findAll(em, ReferenceCatalog.class).get(0);
        }
        em.persist(catalog);
        em.flush();
        liaison.setCatalog(catalog);
        liaisonRepository.saveAndFlush(liaison);
        UUID catalogId = catalog.getId();
        // Get all the liaisonList where catalog equals to catalogId
        defaultLiaisonShouldBeFound("catalogId.equals=" + catalogId);

        // Get all the liaisonList where catalog equals to UUID.randomUUID()
        defaultLiaisonShouldNotBeFound("catalogId.equals=" + UUID.randomUUID());
    }

    private void defaultLiaisonFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultLiaisonShouldBeFound(shouldBeFound);
        defaultLiaisonShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultLiaisonShouldBeFound(String filter) throws Exception {
        restLiaisonMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(liaison.getId().toString())))
            .andExpect(jsonPath("$.[*].liaisonCode").value(hasItem(DEFAULT_LIAISON_CODE)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].projectType").value(hasItem(DEFAULT_PROJECT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].openedOn").value(hasItem(DEFAULT_OPENED_ON.toString())))
            .andExpect(jsonPath("$.[*].projectLeadId").value(hasItem(DEFAULT_PROJECT_LEAD_ID.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].campaignRef").value(hasItem(DEFAULT_CAMPAIGN_REF)))
            .andExpect(jsonPath("$.[*].readyForCertificationAt").value(hasItem(DEFAULT_READY_FOR_CERTIFICATION_AT.toString())))
            .andExpect(jsonPath("$.[*].commissionedOn").value(hasItem(DEFAULT_COMMISSIONED_ON.toString())))
            .andExpect(jsonPath("$.[*].stateReason").value(hasItem(DEFAULT_STATE_REASON)));

        // Check, that the count call also returns 1
        restLiaisonMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultLiaisonShouldNotBeFound(String filter) throws Exception {
        restLiaisonMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restLiaisonMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingLiaison() throws Exception {
        // Get the liaison
        restLiaisonMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingLiaison() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the liaison
        Liaison updatedLiaison = liaisonRepository.findById(liaison.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedLiaison are not directly saved in db
        em.detach(updatedLiaison);
        updatedLiaison
            .liaisonCode(UPDATED_LIAISON_CODE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .projectType(UPDATED_PROJECT_TYPE)
            .openedOn(UPDATED_OPENED_ON)
            .projectLeadId(UPDATED_PROJECT_LEAD_ID)
            .state(UPDATED_STATE)
            .campaignRef(UPDATED_CAMPAIGN_REF)
            .readyForCertificationAt(UPDATED_READY_FOR_CERTIFICATION_AT)
            .commissionedOn(UPDATED_COMMISSIONED_ON)
            .stateReason(UPDATED_STATE_REASON);
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(updatedLiaison);

        restLiaisonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, liaisonDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isOk());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLiaisonToMatchAllProperties(updatedLiaison);
    }

    @Test
    @Transactional
    void putNonExistingLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, liaisonDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(liaisonDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateLiaisonWithPatch() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the liaison using partial update
        Liaison partialUpdatedLiaison = new Liaison();
        partialUpdatedLiaison.setId(liaison.getId());

        partialUpdatedLiaison
            .institutionId(UPDATED_INSTITUTION_ID)
            .projectType(UPDATED_PROJECT_TYPE)
            .openedOn(UPDATED_OPENED_ON)
            .state(UPDATED_STATE)
            .campaignRef(UPDATED_CAMPAIGN_REF)
            .commissionedOn(UPDATED_COMMISSIONED_ON)
            .stateReason(UPDATED_STATE_REASON);

        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLiaison.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLiaison))
            )
            .andExpect(status().isOk());

        // Validate the Liaison in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLiaisonUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedLiaison, liaison), getPersistedLiaison(liaison));
    }

    @Test
    @Transactional
    void fullUpdateLiaisonWithPatch() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the liaison using partial update
        Liaison partialUpdatedLiaison = new Liaison();
        partialUpdatedLiaison.setId(liaison.getId());

        partialUpdatedLiaison
            .liaisonCode(UPDATED_LIAISON_CODE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .projectType(UPDATED_PROJECT_TYPE)
            .openedOn(UPDATED_OPENED_ON)
            .projectLeadId(UPDATED_PROJECT_LEAD_ID)
            .state(UPDATED_STATE)
            .campaignRef(UPDATED_CAMPAIGN_REF)
            .readyForCertificationAt(UPDATED_READY_FOR_CERTIFICATION_AT)
            .commissionedOn(UPDATED_COMMISSIONED_ON)
            .stateReason(UPDATED_STATE_REASON);

        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLiaison.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLiaison))
            )
            .andExpect(status().isOk());

        // Validate the Liaison in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLiaisonUpdatableFieldsEquals(partialUpdatedLiaison, getPersistedLiaison(partialUpdatedLiaison));
    }

    @Test
    @Transactional
    void patchNonExistingLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, liaisonDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamLiaison() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        liaison.setId(UUID.randomUUID());

        // Create the Liaison
        LiaisonDTO liaisonDTO = liaisonMapper.toDto(liaison);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLiaisonMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(liaisonDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Liaison in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteLiaison() throws Exception {
        // Initialize the database
        insertedLiaison = liaisonRepository.saveAndFlush(liaison);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the liaison
        restLiaisonMockMvc
            .perform(delete(ENTITY_API_URL_ID, liaison.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return liaisonRepository.count();
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

    protected Liaison getPersistedLiaison(Liaison liaison) {
        return liaisonRepository.findById(liaison.getId()).orElseThrow();
    }

    protected void assertPersistedLiaisonToMatchAllProperties(Liaison expectedLiaison) {
        assertLiaisonAllPropertiesEquals(expectedLiaison, getPersistedLiaison(expectedLiaison));
    }

    protected void assertPersistedLiaisonToMatchUpdatableProperties(Liaison expectedLiaison) {
        assertLiaisonAllUpdatablePropertiesEquals(expectedLiaison, getPersistedLiaison(expectedLiaison));
    }
}
