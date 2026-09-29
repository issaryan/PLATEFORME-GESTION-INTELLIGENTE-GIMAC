package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.security.AuthoritiesConstants;
import static cm.gimac.des.participant.domain.InteractionAsserts.*;
import static cm.gimac.des.participant.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.Interaction;
import cm.gimac.des.participant.domain.enumeration.InteractionNature;
import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.service.InteractionService;
import cm.gimac.des.participant.service.dto.InteractionDTO;
import cm.gimac.des.participant.service.mapper.InteractionMapper;
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
 * Integration tests for the {@link InteractionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class InteractionResourceIT {

    private static final InteractionNature DEFAULT_NATURE = InteractionNature.COURRIER;
    private static final InteractionNature UPDATED_NATURE = InteractionNature.REUNION;

    private static final String DEFAULT_SUMMARY = "AAAAAAAAAA";
    private static final String UPDATED_SUMMARY = "BBBBBBBBBB";

    private static final Instant DEFAULT_OCCURRED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OCCURRED_AT = Instant.ofEpochMilli(1790694080285L);

    private static final UUID DEFAULT_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_AUTHOR_ID = UUID.randomUUID();

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final UUID DEFAULT_NOTIFICATION_ID = UUID.randomUUID();
    private static final UUID UPDATED_NOTIFICATION_ID = UUID.randomUUID();

    private static final UUID DEFAULT_SOURCE_EVENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_SOURCE_EVENT_ID = UUID.randomUUID();

    private static final String ENTITY_API_URL = "/api/interactions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private InteractionRepository interactionRepository;

    @Mock
    private InteractionRepository interactionRepositoryMock;

    @Autowired
    private InteractionMapper interactionMapper;

    @Mock
    private InteractionService interactionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restInteractionMockMvc;

    private Interaction interaction;

    private Interaction insertedInteraction;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Interaction createEntity(EntityManager em) {
        Interaction interaction = new Interaction()
            .nature(DEFAULT_NATURE)
            .summary(DEFAULT_SUMMARY)
            .occurredAt(DEFAULT_OCCURRED_AT)
            .authorId(DEFAULT_AUTHOR_ID)
            .reference(DEFAULT_REFERENCE)
            .notificationId(DEFAULT_NOTIFICATION_ID)
            .sourceEventId(DEFAULT_SOURCE_EVENT_ID);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        interaction.setInstitution(institution);
        return interaction;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Interaction createUpdatedEntity(EntityManager em) {
        Interaction updatedInteraction = new Interaction()
            .nature(UPDATED_NATURE)
            .summary(UPDATED_SUMMARY)
            .occurredAt(UPDATED_OCCURRED_AT)
            .authorId(UPDATED_AUTHOR_ID)
            .reference(UPDATED_REFERENCE)
            .notificationId(UPDATED_NOTIFICATION_ID)
            .sourceEventId(UPDATED_SOURCE_EVENT_ID);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createUpdatedEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        updatedInteraction.setInstitution(institution);
        return updatedInteraction;
    }

    @BeforeEach
    void initTest() {
        interaction = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedInteraction != null) {
            interactionRepository.delete(insertedInteraction);
            insertedInteraction = null;
        }
    }

    @Test
    @Transactional
    void createInteraction() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Interaction
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);
        var returnedInteractionDTO = om.readValue(
            restInteractionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interactionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            InteractionDTO.class
        );

        // Validate the Interaction in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedInteraction = interactionMapper.toEntity(returnedInteractionDTO);
        assertInteractionUpdatableFieldsEquals(returnedInteraction, getPersistedInteraction(returnedInteraction));

        insertedInteraction = returnedInteraction;
    }

    @Test
    @Transactional
    void createInteractionWithExistingId() throws Exception {
        // Create the Interaction with an existing ID
        insertedInteraction = interactionRepository.saveAndFlush(interaction);
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restInteractionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interaction in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNatureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interaction.setNature(null);

        // Create the Interaction, which fails.
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        restInteractionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSummaryIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interaction.setSummary(null);

        // Create the Interaction, which fails.
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        restInteractionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOccurredAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interaction.setOccurredAt(null);

        // Create the Interaction, which fails.
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        restInteractionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAuthorIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interaction.setAuthorId(null);

        // Create the Interaction, which fails.
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        restInteractionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllInteractions() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList
        restInteractionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(interaction.getId().toString())))
            .andExpect(jsonPath("$.[*].nature").value(hasItem(DEFAULT_NATURE.toString())))
            .andExpect(jsonPath("$.[*].summary").value(hasItem(DEFAULT_SUMMARY)))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].notificationId").value(hasItem(DEFAULT_NOTIFICATION_ID.toString())))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInteractionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(interactionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restInteractionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(interactionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInteractionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(interactionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restInteractionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(interactionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getInteraction() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get the interaction
        restInteractionMockMvc
            .perform(get(ENTITY_API_URL_ID, interaction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(interaction.getId().toString()))
            .andExpect(jsonPath("$.nature").value(DEFAULT_NATURE.toString()))
            .andExpect(jsonPath("$.summary").value(DEFAULT_SUMMARY))
            .andExpect(jsonPath("$.occurredAt").value(DEFAULT_OCCURRED_AT.toString()))
            .andExpect(jsonPath("$.authorId").value(DEFAULT_AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.notificationId").value(DEFAULT_NOTIFICATION_ID.toString()))
            .andExpect(jsonPath("$.sourceEventId").value(DEFAULT_SOURCE_EVENT_ID.toString()));
    }

    @Test
    @Transactional
    void getInteractionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        UUID id = interaction.getId();

        defaultInteractionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllInteractionsByNatureIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where nature equals to
        defaultInteractionFiltering("nature.equals=" + DEFAULT_NATURE, "nature.equals=" + UPDATED_NATURE);
    }

    @Test
    @Transactional
    void getAllInteractionsByNatureIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where nature in
        defaultInteractionFiltering("nature.in=" + DEFAULT_NATURE + "," + UPDATED_NATURE, "nature.in=" + UPDATED_NATURE);
    }

    @Test
    @Transactional
    void getAllInteractionsByNatureIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where nature is not null
        defaultInteractionFiltering("nature.specified=true", "nature.specified=false");
    }

    @Test
    @Transactional
    void getAllInteractionsBySummaryIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where summary equals to
        defaultInteractionFiltering("summary.equals=" + DEFAULT_SUMMARY, "summary.equals=" + UPDATED_SUMMARY);
    }

    @Test
    @Transactional
    void getAllInteractionsBySummaryIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where summary in
        defaultInteractionFiltering("summary.in=" + DEFAULT_SUMMARY + "," + UPDATED_SUMMARY, "summary.in=" + UPDATED_SUMMARY);
    }

    @Test
    @Transactional
    void getAllInteractionsBySummaryIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where summary is not null
        defaultInteractionFiltering("summary.specified=true", "summary.specified=false");
    }

    @Test
    @Transactional
    void getAllInteractionsBySummaryContainsSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where summary contains
        defaultInteractionFiltering("summary.contains=" + DEFAULT_SUMMARY, "summary.contains=" + UPDATED_SUMMARY);
    }

    @Test
    @Transactional
    void getAllInteractionsBySummaryNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where summary does not contain
        defaultInteractionFiltering("summary.doesNotContain=" + UPDATED_SUMMARY, "summary.doesNotContain=" + DEFAULT_SUMMARY);
    }

    @Test
    @Transactional
    void getAllInteractionsByOccurredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where occurredAt equals to
        defaultInteractionFiltering("occurredAt.equals=" + DEFAULT_OCCURRED_AT, "occurredAt.equals=" + UPDATED_OCCURRED_AT);
    }

    @Test
    @Transactional
    void getAllInteractionsByOccurredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where occurredAt in
        defaultInteractionFiltering(
            "occurredAt.in=" + DEFAULT_OCCURRED_AT + "," + UPDATED_OCCURRED_AT,
            "occurredAt.in=" + UPDATED_OCCURRED_AT
        );
    }

    @Test
    @Transactional
    void getAllInteractionsByOccurredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where occurredAt is not null
        defaultInteractionFiltering("occurredAt.specified=true", "occurredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllInteractionsByAuthorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where authorId equals to
        defaultInteractionFiltering("authorId.equals=" + DEFAULT_AUTHOR_ID, "authorId.equals=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllInteractionsByAuthorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where authorId in
        defaultInteractionFiltering("authorId.in=" + DEFAULT_AUTHOR_ID + "," + UPDATED_AUTHOR_ID, "authorId.in=" + UPDATED_AUTHOR_ID);
    }

    @Test
    @Transactional
    void getAllInteractionsByAuthorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where authorId is not null
        defaultInteractionFiltering("authorId.specified=true", "authorId.specified=false");
    }

    @Test
    @Transactional
    void getAllInteractionsByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where reference equals to
        defaultInteractionFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllInteractionsByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where reference in
        defaultInteractionFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllInteractionsByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where reference is not null
        defaultInteractionFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllInteractionsByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where reference contains
        defaultInteractionFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllInteractionsByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where reference does not contain
        defaultInteractionFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllInteractionsByNotificationIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where notificationId equals to
        defaultInteractionFiltering("notificationId.equals=" + DEFAULT_NOTIFICATION_ID, "notificationId.equals=" + UPDATED_NOTIFICATION_ID);
    }

    @Test
    @Transactional
    void getAllInteractionsByNotificationIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where notificationId in
        defaultInteractionFiltering(
            "notificationId.in=" + DEFAULT_NOTIFICATION_ID + "," + UPDATED_NOTIFICATION_ID,
            "notificationId.in=" + UPDATED_NOTIFICATION_ID
        );
    }

    @Test
    @Transactional
    void getAllInteractionsByNotificationIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where notificationId is not null
        defaultInteractionFiltering("notificationId.specified=true", "notificationId.specified=false");
    }

    @Test
    @Transactional
    void getAllInteractionsBySourceEventIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where sourceEventId equals to
        defaultInteractionFiltering("sourceEventId.equals=" + DEFAULT_SOURCE_EVENT_ID, "sourceEventId.equals=" + UPDATED_SOURCE_EVENT_ID);
    }

    @Test
    @Transactional
    void getAllInteractionsBySourceEventIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where sourceEventId in
        defaultInteractionFiltering(
            "sourceEventId.in=" + DEFAULT_SOURCE_EVENT_ID + "," + UPDATED_SOURCE_EVENT_ID,
            "sourceEventId.in=" + UPDATED_SOURCE_EVENT_ID
        );
    }

    @Test
    @Transactional
    void getAllInteractionsBySourceEventIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        // Get all the interactionList where sourceEventId is not null
        defaultInteractionFiltering("sourceEventId.specified=true", "sourceEventId.specified=false");
    }

    @Test
    @Transactional
    void getAllInteractionsByInstitutionIsEqualToSomething() throws Exception {
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            interactionRepository.saveAndFlush(interaction);
            institution = InstitutionResourceIT.createEntity();
        } else {
            institution = TestUtil.findAll(em, Institution.class).getFirst();
        }
        em.persist(institution);
        em.flush();
        interaction.setInstitution(institution);
        interactionRepository.saveAndFlush(interaction);
        UUID institutionId = institution.getId();
        // Get all the interactionList where institution equals to institutionId
        defaultInteractionShouldBeFound("institutionId.equals=" + institutionId);

        // Get all the interactionList where institution equals to UUID.randomUUID()
        defaultInteractionShouldNotBeFound("institutionId.equals=" + UUID.randomUUID());
    }

    private void defaultInteractionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultInteractionShouldBeFound(shouldBeFound);
        defaultInteractionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultInteractionShouldBeFound(String filter) throws Exception {
        restInteractionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(interaction.getId().toString())))
            .andExpect(jsonPath("$.[*].nature").value(hasItem(DEFAULT_NATURE.toString())))
            .andExpect(jsonPath("$.[*].summary").value(hasItem(DEFAULT_SUMMARY)))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].notificationId").value(hasItem(DEFAULT_NOTIFICATION_ID.toString())))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID.toString())));

        // Check, that the count call also returns 1
        restInteractionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultInteractionShouldNotBeFound(String filter) throws Exception {
        restInteractionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restInteractionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingInteraction() throws Exception {
        // Get the interaction
        restInteractionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingInteraction() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the interaction
        Interaction updatedInteraction = interactionRepository.findById(interaction.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedInteraction are not directly saved in db
        em.detach(updatedInteraction);
        updatedInteraction
            .nature(UPDATED_NATURE)
            .summary(UPDATED_SUMMARY)
            .occurredAt(UPDATED_OCCURRED_AT)
            .authorId(UPDATED_AUTHOR_ID)
            .reference(UPDATED_REFERENCE)
            .notificationId(UPDATED_NOTIFICATION_ID)
            .sourceEventId(UPDATED_SOURCE_EVENT_ID);
        InteractionDTO interactionDTO = interactionMapper.toDto(updatedInteraction);

        restInteractionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, interactionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isOk());

        // Validate the Interaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedInteractionToMatchAllProperties(updatedInteraction);
    }

    @Test
    @Transactional
    void putNonExistingInteraction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interaction.setId(UUID.randomUUID());

        // Create the Interaction
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInteractionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, interactionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchInteraction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interaction.setId(UUID.randomUUID());

        // Create the Interaction
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInteractionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamInteraction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interaction.setId(UUID.randomUUID());

        // Create the Interaction
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInteractionMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interactionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Interaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateInteractionWithPatch() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the interaction using partial update
        Interaction partialUpdatedInteraction = new Interaction();
        partialUpdatedInteraction.setId(interaction.getId());

        partialUpdatedInteraction.nature(UPDATED_NATURE).reference(UPDATED_REFERENCE).sourceEventId(UPDATED_SOURCE_EVENT_ID);

        restInteractionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedInteraction.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedInteraction))
            )
            .andExpect(status().isOk());

        // Validate the Interaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInteractionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedInteraction, interaction),
            getPersistedInteraction(interaction)
        );
    }

    @Test
    @Transactional
    void fullUpdateInteractionWithPatch() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the interaction using partial update
        Interaction partialUpdatedInteraction = new Interaction();
        partialUpdatedInteraction.setId(interaction.getId());

        partialUpdatedInteraction
            .nature(UPDATED_NATURE)
            .summary(UPDATED_SUMMARY)
            .occurredAt(UPDATED_OCCURRED_AT)
            .authorId(UPDATED_AUTHOR_ID)
            .reference(UPDATED_REFERENCE)
            .notificationId(UPDATED_NOTIFICATION_ID)
            .sourceEventId(UPDATED_SOURCE_EVENT_ID);

        restInteractionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedInteraction.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedInteraction))
            )
            .andExpect(status().isOk());

        // Validate the Interaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInteractionUpdatableFieldsEquals(partialUpdatedInteraction, getPersistedInteraction(partialUpdatedInteraction));
    }

    @Test
    @Transactional
    void patchNonExistingInteraction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interaction.setId(UUID.randomUUID());

        // Create the Interaction
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInteractionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, interactionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchInteraction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interaction.setId(UUID.randomUUID());

        // Create the Interaction
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInteractionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamInteraction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interaction.setId(UUID.randomUUID());

        // Create the Interaction
        InteractionDTO interactionDTO = interactionMapper.toDto(interaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInteractionMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(interactionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Interaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteInteraction() throws Exception {
        // Initialize the database
        insertedInteraction = interactionRepository.saveAndFlush(interaction);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the interaction
        restInteractionMockMvc
            .perform(delete(ENTITY_API_URL_ID, interaction.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return interactionRepository.count();
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

    protected Interaction getPersistedInteraction(Interaction interaction) {
        return interactionRepository.findById(interaction.getId()).orElseThrow();
    }

    protected void assertPersistedInteractionToMatchAllProperties(Interaction expectedInteraction) {
        assertInteractionAllPropertiesEquals(expectedInteraction, getPersistedInteraction(expectedInteraction));
    }

    protected void assertPersistedInteractionToMatchUpdatableProperties(Interaction expectedInteraction) {
        assertInteractionAllUpdatablePropertiesEquals(expectedInteraction, getPersistedInteraction(expectedInteraction));
    }
}
