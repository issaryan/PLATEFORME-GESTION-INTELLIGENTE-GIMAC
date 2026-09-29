package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.security.AuthoritiesConstants;
import static cm.gimac.des.participant.domain.CorrespondentAsserts.*;
import static cm.gimac.des.participant.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.enumeration.CorrespondentFunction;
import cm.gimac.des.participant.domain.enumeration.Reachability;
import cm.gimac.des.participant.repository.CorrespondentRepository;
import cm.gimac.des.participant.service.CorrespondentService;
import cm.gimac.des.participant.service.dto.CorrespondentDTO;
import cm.gimac.des.participant.service.mapper.CorrespondentMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
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
 * Integration tests for the {@link CorrespondentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class CorrespondentResourceIT {

    private static final String DEFAULT_FULL_NAME = "AAAAAAAAAA";
    private static final String UPDATED_FULL_NAME = "BBBBBBBBBB";

    private static final CorrespondentFunction DEFAULT_DUTY = CorrespondentFunction.DIRECTION;
    private static final CorrespondentFunction UPDATED_DUTY = CorrespondentFunction.MONETIQUE;

    private static final String DEFAULT_CHANNELS = "AAAAAAAAAA";
    private static final String UPDATED_CHANNELS = "BBBBBBBBBB";

    private static final String DEFAULT_EMAIL = "AAAAAAAAAA";
    private static final String UPDATED_EMAIL = "BBBBBBBBBB";

    private static final String DEFAULT_PHONE = "AAAAAAAAAA";
    private static final String UPDATED_PHONE = "BBBBBBBBBB";

    private static final String DEFAULT_PUBLIC_KEY_REF = "AAAAAAAAAA";
    private static final String UPDATED_PUBLIC_KEY_REF = "BBBBBBBBBB";

    private static final Reachability DEFAULT_REACHABILITY = Reachability.NON_VERIFIE;
    private static final Reachability UPDATED_REACHABILITY = Reachability.JOIGNABLE;

    private static final Instant DEFAULT_LAST_REACHABILITY_CHECK_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_REACHABILITY_CHECK_AT = Instant.ofEpochMilli(1790694080285L);

    private static final Boolean DEFAULT_ON_CALL_247 = false;
    private static final Boolean UPDATED_ON_CALL_247 = true;

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final UUID DEFAULT_PROPOSED_BY = UUID.randomUUID();
    private static final UUID UPDATED_PROPOSED_BY = UUID.randomUUID();

    private static final UUID DEFAULT_VALIDATED_BY = UUID.randomUUID();
    private static final UUID UPDATED_VALIDATED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_VALIDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VALIDATED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final String ENTITY_API_URL = "/api/correspondents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CorrespondentRepository correspondentRepository;

    @Mock
    private CorrespondentRepository correspondentRepositoryMock;

    @Autowired
    private CorrespondentMapper correspondentMapper;

    @Mock
    private CorrespondentService correspondentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCorrespondentMockMvc;

    private Correspondent correspondent;

    private Correspondent insertedCorrespondent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Correspondent createEntity(EntityManager em) {
        Correspondent correspondent = new Correspondent()
            .fullName(DEFAULT_FULL_NAME)
            .duty(DEFAULT_DUTY)
            .channels(DEFAULT_CHANNELS)
            .email(DEFAULT_EMAIL)
            .phone(DEFAULT_PHONE)
            .publicKeyRef(DEFAULT_PUBLIC_KEY_REF)
            .reachability(DEFAULT_REACHABILITY)
            .lastReachabilityCheckAt(DEFAULT_LAST_REACHABILITY_CHECK_AT)
            .onCall247(DEFAULT_ON_CALL_247)
            .userId(DEFAULT_USER_ID)
            .active(DEFAULT_ACTIVE)
            .proposedBy(DEFAULT_PROPOSED_BY)
            .validatedBy(DEFAULT_VALIDATED_BY)
            .validatedAt(DEFAULT_VALIDATED_AT);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        correspondent.setInstitution(institution);
        return correspondent;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Correspondent createUpdatedEntity(EntityManager em) {
        Correspondent updatedCorrespondent = new Correspondent()
            .fullName(UPDATED_FULL_NAME)
            .duty(UPDATED_DUTY)
            .channels(UPDATED_CHANNELS)
            .email(UPDATED_EMAIL)
            .phone(UPDATED_PHONE)
            .publicKeyRef(UPDATED_PUBLIC_KEY_REF)
            .reachability(UPDATED_REACHABILITY)
            .lastReachabilityCheckAt(UPDATED_LAST_REACHABILITY_CHECK_AT)
            .onCall247(UPDATED_ON_CALL_247)
            .userId(UPDATED_USER_ID)
            .active(UPDATED_ACTIVE)
            .proposedBy(UPDATED_PROPOSED_BY)
            .validatedBy(UPDATED_VALIDATED_BY)
            .validatedAt(UPDATED_VALIDATED_AT);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createUpdatedEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        updatedCorrespondent.setInstitution(institution);
        return updatedCorrespondent;
    }

    @BeforeEach
    void initTest() {
        correspondent = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCorrespondent != null) {
            correspondentRepository.delete(insertedCorrespondent);
            insertedCorrespondent = null;
        }
    }

    @Test
    @Transactional
    void createCorrespondent() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Correspondent
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);
        var returnedCorrespondentDTO = om.readValue(
            restCorrespondentMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(correspondentDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CorrespondentDTO.class
        );

        // Validate the Correspondent in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCorrespondent = correspondentMapper.toEntity(returnedCorrespondentDTO);
        assertCorrespondentUpdatableFieldsEquals(returnedCorrespondent, getPersistedCorrespondent(returnedCorrespondent));

        insertedCorrespondent = returnedCorrespondent;
    }

    @Test
    @Transactional
    void createCorrespondentWithExistingId() throws Exception {
        // Create the Correspondent with an existing ID
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCorrespondentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Correspondent in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkFullNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        correspondent.setFullName(null);

        // Create the Correspondent, which fails.
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        restCorrespondentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDutyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        correspondent.setDuty(null);

        // Create the Correspondent, which fails.
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        restCorrespondentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkChannelsIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        correspondent.setChannels(null);

        // Create the Correspondent, which fails.
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        restCorrespondentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReachabilityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        correspondent.setReachability(null);

        // Create the Correspondent, which fails.
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        restCorrespondentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOnCall247IsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        correspondent.setOnCall247(null);

        // Create the Correspondent, which fails.
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        restCorrespondentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        correspondent.setActive(null);

        // Create the Correspondent, which fails.
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        restCorrespondentMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCorrespondents() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList
        restCorrespondentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(correspondent.getId().toString())))
            .andExpect(jsonPath("$.[*].fullName").value(hasItem(DEFAULT_FULL_NAME)))
            .andExpect(jsonPath("$.[*].duty").value(hasItem(DEFAULT_DUTY.toString())))
            .andExpect(jsonPath("$.[*].channels").value(hasItem(DEFAULT_CHANNELS)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE)))
            .andExpect(jsonPath("$.[*].publicKeyRef").value(hasItem(DEFAULT_PUBLIC_KEY_REF)))
            .andExpect(jsonPath("$.[*].reachability").value(hasItem(DEFAULT_REACHABILITY.toString())))
            .andExpect(jsonPath("$.[*].lastReachabilityCheckAt").value(hasItem(DEFAULT_LAST_REACHABILITY_CHECK_AT.toString())))
            .andExpect(jsonPath("$.[*].onCall247").value(hasItem(DEFAULT_ON_CALL_247)))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].proposedBy").value(hasItem(DEFAULT_PROPOSED_BY.toString())))
            .andExpect(jsonPath("$.[*].validatedBy").value(hasItem(DEFAULT_VALIDATED_BY.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCorrespondentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(correspondentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCorrespondentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(correspondentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCorrespondentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(correspondentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCorrespondentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(correspondentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCorrespondent() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get the correspondent
        restCorrespondentMockMvc
            .perform(get(ENTITY_API_URL_ID, correspondent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(correspondent.getId().toString()))
            .andExpect(jsonPath("$.fullName").value(DEFAULT_FULL_NAME))
            .andExpect(jsonPath("$.duty").value(DEFAULT_DUTY.toString()))
            .andExpect(jsonPath("$.channels").value(DEFAULT_CHANNELS))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
            .andExpect(jsonPath("$.phone").value(DEFAULT_PHONE))
            .andExpect(jsonPath("$.publicKeyRef").value(DEFAULT_PUBLIC_KEY_REF))
            .andExpect(jsonPath("$.reachability").value(DEFAULT_REACHABILITY.toString()))
            .andExpect(jsonPath("$.lastReachabilityCheckAt").value(DEFAULT_LAST_REACHABILITY_CHECK_AT.toString()))
            .andExpect(jsonPath("$.onCall247").value(DEFAULT_ON_CALL_247))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE))
            .andExpect(jsonPath("$.proposedBy").value(DEFAULT_PROPOSED_BY.toString()))
            .andExpect(jsonPath("$.validatedBy").value(DEFAULT_VALIDATED_BY.toString()))
            .andExpect(jsonPath("$.validatedAt").value(DEFAULT_VALIDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getCorrespondentsByIdFiltering() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        UUID id = correspondent.getId();

        defaultCorrespondentFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByFullNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where fullName equals to
        defaultCorrespondentFiltering("fullName.equals=" + DEFAULT_FULL_NAME, "fullName.equals=" + UPDATED_FULL_NAME);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByFullNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where fullName in
        defaultCorrespondentFiltering("fullName.in=" + DEFAULT_FULL_NAME + "," + UPDATED_FULL_NAME, "fullName.in=" + UPDATED_FULL_NAME);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByFullNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where fullName is not null
        defaultCorrespondentFiltering("fullName.specified=true", "fullName.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByFullNameContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where fullName contains
        defaultCorrespondentFiltering("fullName.contains=" + DEFAULT_FULL_NAME, "fullName.contains=" + UPDATED_FULL_NAME);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByFullNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where fullName does not contain
        defaultCorrespondentFiltering("fullName.doesNotContain=" + UPDATED_FULL_NAME, "fullName.doesNotContain=" + DEFAULT_FULL_NAME);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByDutyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where duty equals to
        defaultCorrespondentFiltering("duty.equals=" + DEFAULT_DUTY, "duty.equals=" + UPDATED_DUTY);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByDutyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where duty in
        defaultCorrespondentFiltering("duty.in=" + DEFAULT_DUTY + "," + UPDATED_DUTY, "duty.in=" + UPDATED_DUTY);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByDutyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where duty is not null
        defaultCorrespondentFiltering("duty.specified=true", "duty.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByChannelsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where channels equals to
        defaultCorrespondentFiltering("channels.equals=" + DEFAULT_CHANNELS, "channels.equals=" + UPDATED_CHANNELS);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByChannelsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where channels in
        defaultCorrespondentFiltering("channels.in=" + DEFAULT_CHANNELS + "," + UPDATED_CHANNELS, "channels.in=" + UPDATED_CHANNELS);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByChannelsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where channels is not null
        defaultCorrespondentFiltering("channels.specified=true", "channels.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByChannelsContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where channels contains
        defaultCorrespondentFiltering("channels.contains=" + DEFAULT_CHANNELS, "channels.contains=" + UPDATED_CHANNELS);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByChannelsNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where channels does not contain
        defaultCorrespondentFiltering("channels.doesNotContain=" + UPDATED_CHANNELS, "channels.doesNotContain=" + DEFAULT_CHANNELS);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByEmailIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where email equals to
        defaultCorrespondentFiltering("email.equals=" + DEFAULT_EMAIL, "email.equals=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByEmailIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where email in
        defaultCorrespondentFiltering("email.in=" + DEFAULT_EMAIL + "," + UPDATED_EMAIL, "email.in=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByEmailIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where email is not null
        defaultCorrespondentFiltering("email.specified=true", "email.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByEmailContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where email contains
        defaultCorrespondentFiltering("email.contains=" + DEFAULT_EMAIL, "email.contains=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByEmailNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where email does not contain
        defaultCorrespondentFiltering("email.doesNotContain=" + UPDATED_EMAIL, "email.doesNotContain=" + DEFAULT_EMAIL);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPhoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where phone equals to
        defaultCorrespondentFiltering("phone.equals=" + DEFAULT_PHONE, "phone.equals=" + UPDATED_PHONE);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPhoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where phone in
        defaultCorrespondentFiltering("phone.in=" + DEFAULT_PHONE + "," + UPDATED_PHONE, "phone.in=" + UPDATED_PHONE);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPhoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where phone is not null
        defaultCorrespondentFiltering("phone.specified=true", "phone.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPhoneContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where phone contains
        defaultCorrespondentFiltering("phone.contains=" + DEFAULT_PHONE, "phone.contains=" + UPDATED_PHONE);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPhoneNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where phone does not contain
        defaultCorrespondentFiltering("phone.doesNotContain=" + UPDATED_PHONE, "phone.doesNotContain=" + DEFAULT_PHONE);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPublicKeyRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where publicKeyRef equals to
        defaultCorrespondentFiltering("publicKeyRef.equals=" + DEFAULT_PUBLIC_KEY_REF, "publicKeyRef.equals=" + UPDATED_PUBLIC_KEY_REF);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPublicKeyRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where publicKeyRef in
        defaultCorrespondentFiltering(
            "publicKeyRef.in=" + DEFAULT_PUBLIC_KEY_REF + "," + UPDATED_PUBLIC_KEY_REF,
            "publicKeyRef.in=" + UPDATED_PUBLIC_KEY_REF
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPublicKeyRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where publicKeyRef is not null
        defaultCorrespondentFiltering("publicKeyRef.specified=true", "publicKeyRef.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPublicKeyRefContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where publicKeyRef contains
        defaultCorrespondentFiltering("publicKeyRef.contains=" + DEFAULT_PUBLIC_KEY_REF, "publicKeyRef.contains=" + UPDATED_PUBLIC_KEY_REF);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByPublicKeyRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where publicKeyRef does not contain
        defaultCorrespondentFiltering(
            "publicKeyRef.doesNotContain=" + UPDATED_PUBLIC_KEY_REF,
            "publicKeyRef.doesNotContain=" + DEFAULT_PUBLIC_KEY_REF
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByReachabilityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where reachability equals to
        defaultCorrespondentFiltering("reachability.equals=" + DEFAULT_REACHABILITY, "reachability.equals=" + UPDATED_REACHABILITY);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByReachabilityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where reachability in
        defaultCorrespondentFiltering(
            "reachability.in=" + DEFAULT_REACHABILITY + "," + UPDATED_REACHABILITY,
            "reachability.in=" + UPDATED_REACHABILITY
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByReachabilityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where reachability is not null
        defaultCorrespondentFiltering("reachability.specified=true", "reachability.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByLastReachabilityCheckAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where lastReachabilityCheckAt equals to
        defaultCorrespondentFiltering(
            "lastReachabilityCheckAt.equals=" + DEFAULT_LAST_REACHABILITY_CHECK_AT,
            "lastReachabilityCheckAt.equals=" + UPDATED_LAST_REACHABILITY_CHECK_AT
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByLastReachabilityCheckAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where lastReachabilityCheckAt in
        defaultCorrespondentFiltering(
            "lastReachabilityCheckAt.in=" + DEFAULT_LAST_REACHABILITY_CHECK_AT + "," + UPDATED_LAST_REACHABILITY_CHECK_AT,
            "lastReachabilityCheckAt.in=" + UPDATED_LAST_REACHABILITY_CHECK_AT
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByLastReachabilityCheckAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where lastReachabilityCheckAt is not null
        defaultCorrespondentFiltering("lastReachabilityCheckAt.specified=true", "lastReachabilityCheckAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByOnCall247IsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where onCall247 equals to
        defaultCorrespondentFiltering("onCall247.equals=" + DEFAULT_ON_CALL_247, "onCall247.equals=" + UPDATED_ON_CALL_247);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByOnCall247IsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where onCall247 in
        defaultCorrespondentFiltering(
            "onCall247.in=" + DEFAULT_ON_CALL_247 + "," + UPDATED_ON_CALL_247,
            "onCall247.in=" + UPDATED_ON_CALL_247
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByOnCall247IsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where onCall247 is not null
        defaultCorrespondentFiltering("onCall247.specified=true", "onCall247.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where userId equals to
        defaultCorrespondentFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where userId in
        defaultCorrespondentFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where userId is not null
        defaultCorrespondentFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where active equals to
        defaultCorrespondentFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where active in
        defaultCorrespondentFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where active is not null
        defaultCorrespondentFiltering("active.specified=true", "active.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByProposedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where proposedBy equals to
        defaultCorrespondentFiltering("proposedBy.equals=" + DEFAULT_PROPOSED_BY, "proposedBy.equals=" + UPDATED_PROPOSED_BY);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByProposedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where proposedBy in
        defaultCorrespondentFiltering(
            "proposedBy.in=" + DEFAULT_PROPOSED_BY + "," + UPDATED_PROPOSED_BY,
            "proposedBy.in=" + UPDATED_PROPOSED_BY
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByProposedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where proposedBy is not null
        defaultCorrespondentFiltering("proposedBy.specified=true", "proposedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByValidatedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where validatedBy equals to
        defaultCorrespondentFiltering("validatedBy.equals=" + DEFAULT_VALIDATED_BY, "validatedBy.equals=" + UPDATED_VALIDATED_BY);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByValidatedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where validatedBy in
        defaultCorrespondentFiltering(
            "validatedBy.in=" + DEFAULT_VALIDATED_BY + "," + UPDATED_VALIDATED_BY,
            "validatedBy.in=" + UPDATED_VALIDATED_BY
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByValidatedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where validatedBy is not null
        defaultCorrespondentFiltering("validatedBy.specified=true", "validatedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByValidatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where validatedAt equals to
        defaultCorrespondentFiltering("validatedAt.equals=" + DEFAULT_VALIDATED_AT, "validatedAt.equals=" + UPDATED_VALIDATED_AT);
    }

    @Test
    @Transactional
    void getAllCorrespondentsByValidatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where validatedAt in
        defaultCorrespondentFiltering(
            "validatedAt.in=" + DEFAULT_VALIDATED_AT + "," + UPDATED_VALIDATED_AT,
            "validatedAt.in=" + UPDATED_VALIDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllCorrespondentsByValidatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        // Get all the correspondentList where validatedAt is not null
        defaultCorrespondentFiltering("validatedAt.specified=true", "validatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCorrespondentsByInstitutionIsEqualToSomething() throws Exception {
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            correspondentRepository.saveAndFlush(correspondent);
            institution = InstitutionResourceIT.createEntity();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        em.persist(institution);
        em.flush();
        correspondent.setInstitution(institution);
        correspondentRepository.saveAndFlush(correspondent);
        UUID institutionId = institution.getId();
        // Get all the correspondentList where institution equals to institutionId
        defaultCorrespondentShouldBeFound("institutionId.equals=" + institutionId);

        // Get all the correspondentList where institution equals to UUID.randomUUID()
        defaultCorrespondentShouldNotBeFound("institutionId.equals=" + UUID.randomUUID());
    }

    private void defaultCorrespondentFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCorrespondentShouldBeFound(shouldBeFound);
        defaultCorrespondentShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCorrespondentShouldBeFound(String filter) throws Exception {
        restCorrespondentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(correspondent.getId().toString())))
            .andExpect(jsonPath("$.[*].fullName").value(hasItem(DEFAULT_FULL_NAME)))
            .andExpect(jsonPath("$.[*].duty").value(hasItem(DEFAULT_DUTY.toString())))
            .andExpect(jsonPath("$.[*].channels").value(hasItem(DEFAULT_CHANNELS)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE)))
            .andExpect(jsonPath("$.[*].publicKeyRef").value(hasItem(DEFAULT_PUBLIC_KEY_REF)))
            .andExpect(jsonPath("$.[*].reachability").value(hasItem(DEFAULT_REACHABILITY.toString())))
            .andExpect(jsonPath("$.[*].lastReachabilityCheckAt").value(hasItem(DEFAULT_LAST_REACHABILITY_CHECK_AT.toString())))
            .andExpect(jsonPath("$.[*].onCall247").value(hasItem(DEFAULT_ON_CALL_247)))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].proposedBy").value(hasItem(DEFAULT_PROPOSED_BY.toString())))
            .andExpect(jsonPath("$.[*].validatedBy").value(hasItem(DEFAULT_VALIDATED_BY.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())));

        // Check, that the count call also returns 1
        restCorrespondentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCorrespondentShouldNotBeFound(String filter) throws Exception {
        restCorrespondentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCorrespondentMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCorrespondent() throws Exception {
        // Get the correspondent
        restCorrespondentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCorrespondent() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the correspondent
        Correspondent updatedCorrespondent = correspondentRepository.findById(correspondent.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCorrespondent are not directly saved in db
        em.detach(updatedCorrespondent);
        updatedCorrespondent
            .fullName(UPDATED_FULL_NAME)
            .duty(UPDATED_DUTY)
            .channels(UPDATED_CHANNELS)
            .email(UPDATED_EMAIL)
            .phone(UPDATED_PHONE)
            .publicKeyRef(UPDATED_PUBLIC_KEY_REF)
            .reachability(UPDATED_REACHABILITY)
            .lastReachabilityCheckAt(UPDATED_LAST_REACHABILITY_CHECK_AT)
            .onCall247(UPDATED_ON_CALL_247)
            .userId(UPDATED_USER_ID)
            .active(UPDATED_ACTIVE)
            .proposedBy(UPDATED_PROPOSED_BY)
            .validatedBy(UPDATED_VALIDATED_BY)
            .validatedAt(UPDATED_VALIDATED_AT);
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(updatedCorrespondent);

        restCorrespondentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, correspondentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isOk());

        // Validate the Correspondent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCorrespondentToMatchAllProperties(updatedCorrespondent);
    }

    @Test
    @Transactional
    void putNonExistingCorrespondent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        correspondent.setId(UUID.randomUUID());

        // Create the Correspondent
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCorrespondentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, correspondentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Correspondent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCorrespondent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        correspondent.setId(UUID.randomUUID());

        // Create the Correspondent
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCorrespondentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Correspondent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCorrespondent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        correspondent.setId(UUID.randomUUID());

        // Create the Correspondent
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCorrespondentMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Correspondent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCorrespondentWithPatch() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the correspondent using partial update
        Correspondent partialUpdatedCorrespondent = new Correspondent();
        partialUpdatedCorrespondent.setId(correspondent.getId());

        partialUpdatedCorrespondent
            .duty(UPDATED_DUTY)
            .phone(UPDATED_PHONE)
            .publicKeyRef(UPDATED_PUBLIC_KEY_REF)
            .lastReachabilityCheckAt(UPDATED_LAST_REACHABILITY_CHECK_AT)
            .active(UPDATED_ACTIVE)
            .validatedBy(UPDATED_VALIDATED_BY)
            .validatedAt(UPDATED_VALIDATED_AT);

        restCorrespondentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCorrespondent.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCorrespondent))
            )
            .andExpect(status().isOk());

        // Validate the Correspondent in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCorrespondentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCorrespondent, correspondent),
            getPersistedCorrespondent(correspondent)
        );
    }

    @Test
    @Transactional
    void fullUpdateCorrespondentWithPatch() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the correspondent using partial update
        Correspondent partialUpdatedCorrespondent = new Correspondent();
        partialUpdatedCorrespondent.setId(correspondent.getId());

        partialUpdatedCorrespondent
            .fullName(UPDATED_FULL_NAME)
            .duty(UPDATED_DUTY)
            .channels(UPDATED_CHANNELS)
            .email(UPDATED_EMAIL)
            .phone(UPDATED_PHONE)
            .publicKeyRef(UPDATED_PUBLIC_KEY_REF)
            .reachability(UPDATED_REACHABILITY)
            .lastReachabilityCheckAt(UPDATED_LAST_REACHABILITY_CHECK_AT)
            .onCall247(UPDATED_ON_CALL_247)
            .userId(UPDATED_USER_ID)
            .active(UPDATED_ACTIVE)
            .proposedBy(UPDATED_PROPOSED_BY)
            .validatedBy(UPDATED_VALIDATED_BY)
            .validatedAt(UPDATED_VALIDATED_AT);

        restCorrespondentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCorrespondent.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCorrespondent))
            )
            .andExpect(status().isOk());

        // Validate the Correspondent in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCorrespondentUpdatableFieldsEquals(partialUpdatedCorrespondent, getPersistedCorrespondent(partialUpdatedCorrespondent));
    }

    @Test
    @Transactional
    void patchNonExistingCorrespondent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        correspondent.setId(UUID.randomUUID());

        // Create the Correspondent
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCorrespondentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, correspondentDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Correspondent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCorrespondent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        correspondent.setId(UUID.randomUUID());

        // Create the Correspondent
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCorrespondentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Correspondent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCorrespondent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        correspondent.setId(UUID.randomUUID());

        // Create the Correspondent
        CorrespondentDTO correspondentDTO = correspondentMapper.toDto(correspondent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCorrespondentMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(correspondentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Correspondent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCorrespondent() throws Exception {
        // Initialize the database
        insertedCorrespondent = correspondentRepository.saveAndFlush(correspondent);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the correspondent
        restCorrespondentMockMvc
            .perform(delete(ENTITY_API_URL_ID, correspondent.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return correspondentRepository.count();
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

    protected Correspondent getPersistedCorrespondent(Correspondent correspondent) {
        return correspondentRepository.findById(correspondent.getId()).orElseThrow();
    }

    protected void assertPersistedCorrespondentToMatchAllProperties(Correspondent expectedCorrespondent) {
        assertCorrespondentAllPropertiesEquals(expectedCorrespondent, getPersistedCorrespondent(expectedCorrespondent));
    }

    protected void assertPersistedCorrespondentToMatchUpdatableProperties(Correspondent expectedCorrespondent) {
        assertCorrespondentAllUpdatablePropertiesEquals(expectedCorrespondent, getPersistedCorrespondent(expectedCorrespondent));
    }
}
