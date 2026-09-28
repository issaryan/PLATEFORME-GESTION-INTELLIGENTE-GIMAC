package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.CollaboratorProjectionAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.CollaboratorProjection;
import cm.gimac.des.pilotage.repository.CollaboratorProjectionRepository;
import cm.gimac.des.pilotage.service.mapper.CollaboratorProjectionMapper;
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
 * Integration tests for the {@link CollaboratorProjectionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CollaboratorProjectionResourceIT {

    private static final UUID DEFAULT_COLLABORATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_COLLABORATOR_ID = UUID.randomUUID();

    private static final String DEFAULT_DISPLAY_NAME = "AAAAAAAAAA";
    private static final String UPDATED_DISPLAY_NAME = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/collaborator-projections";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CollaboratorProjectionRepository collaboratorProjectionRepository;

    @Autowired
    private CollaboratorProjectionMapper collaboratorProjectionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCollaboratorProjectionMockMvc;

    private CollaboratorProjection collaboratorProjection;

    private CollaboratorProjection insertedCollaboratorProjection;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CollaboratorProjection createEntity() {
        return new CollaboratorProjection()
            .collaboratorId(DEFAULT_COLLABORATOR_ID)
            .displayName(DEFAULT_DISPLAY_NAME)
            .active(DEFAULT_ACTIVE)
            .serviceCode(DEFAULT_SERVICE_CODE)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CollaboratorProjection createUpdatedEntity() {
        return new CollaboratorProjection()
            .collaboratorId(UPDATED_COLLABORATOR_ID)
            .displayName(UPDATED_DISPLAY_NAME)
            .active(UPDATED_ACTIVE)
            .serviceCode(UPDATED_SERVICE_CODE)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        collaboratorProjection = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCollaboratorProjection != null) {
            collaboratorProjectionRepository.delete(insertedCollaboratorProjection);
            insertedCollaboratorProjection = null;
        }
    }

    @Test
    @Transactional
    void getAllCollaboratorProjections() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList
        restCollaboratorProjectionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(collaboratorProjection.getId().toString())))
            .andExpect(jsonPath("$.[*].collaboratorId").value(hasItem(DEFAULT_COLLABORATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].displayName").value(hasItem(DEFAULT_DISPLAY_NAME)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getCollaboratorProjection() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get the collaboratorProjection
        restCollaboratorProjectionMockMvc
            .perform(get(ENTITY_API_URL_ID, collaboratorProjection.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(collaboratorProjection.getId().toString()))
            .andExpect(jsonPath("$.collaboratorId").value(DEFAULT_COLLABORATOR_ID.toString()))
            .andExpect(jsonPath("$.displayName").value(DEFAULT_DISPLAY_NAME))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getCollaboratorProjectionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        UUID id = collaboratorProjection.getId();

        defaultCollaboratorProjectionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByCollaboratorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where collaboratorId equals to
        defaultCollaboratorProjectionFiltering(
            "collaboratorId.equals=" + DEFAULT_COLLABORATOR_ID,
            "collaboratorId.equals=" + UPDATED_COLLABORATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByCollaboratorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where collaboratorId in
        defaultCollaboratorProjectionFiltering(
            "collaboratorId.in=" + DEFAULT_COLLABORATOR_ID + "," + UPDATED_COLLABORATOR_ID,
            "collaboratorId.in=" + UPDATED_COLLABORATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByCollaboratorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where collaboratorId is not null
        defaultCollaboratorProjectionFiltering("collaboratorId.specified=true", "collaboratorId.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByDisplayNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where displayName equals to
        defaultCollaboratorProjectionFiltering("displayName.equals=" + DEFAULT_DISPLAY_NAME, "displayName.equals=" + UPDATED_DISPLAY_NAME);
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByDisplayNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where displayName in
        defaultCollaboratorProjectionFiltering(
            "displayName.in=" + DEFAULT_DISPLAY_NAME + "," + UPDATED_DISPLAY_NAME,
            "displayName.in=" + UPDATED_DISPLAY_NAME
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByDisplayNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where displayName is not null
        defaultCollaboratorProjectionFiltering("displayName.specified=true", "displayName.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByDisplayNameContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where displayName contains
        defaultCollaboratorProjectionFiltering(
            "displayName.contains=" + DEFAULT_DISPLAY_NAME,
            "displayName.contains=" + UPDATED_DISPLAY_NAME
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByDisplayNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where displayName does not contain
        defaultCollaboratorProjectionFiltering(
            "displayName.doesNotContain=" + UPDATED_DISPLAY_NAME,
            "displayName.doesNotContain=" + DEFAULT_DISPLAY_NAME
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where active equals to
        defaultCollaboratorProjectionFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where active in
        defaultCollaboratorProjectionFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where active is not null
        defaultCollaboratorProjectionFiltering("active.specified=true", "active.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where serviceCode equals to
        defaultCollaboratorProjectionFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where serviceCode in
        defaultCollaboratorProjectionFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where serviceCode is not null
        defaultCollaboratorProjectionFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where serviceCode contains
        defaultCollaboratorProjectionFiltering(
            "serviceCode.contains=" + DEFAULT_SERVICE_CODE,
            "serviceCode.contains=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where serviceCode does not contain
        defaultCollaboratorProjectionFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where updatedAt equals to
        defaultCollaboratorProjectionFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where updatedAt in
        defaultCollaboratorProjectionFiltering(
            "updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT,
            "updatedAt.in=" + UPDATED_UPDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllCollaboratorProjectionsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCollaboratorProjection = collaboratorProjectionRepository.saveAndFlush(collaboratorProjection);

        // Get all the collaboratorProjectionList where updatedAt is not null
        defaultCollaboratorProjectionFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    private void defaultCollaboratorProjectionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCollaboratorProjectionShouldBeFound(shouldBeFound);
        defaultCollaboratorProjectionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCollaboratorProjectionShouldBeFound(String filter) throws Exception {
        restCollaboratorProjectionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(collaboratorProjection.getId().toString())))
            .andExpect(jsonPath("$.[*].collaboratorId").value(hasItem(DEFAULT_COLLABORATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].displayName").value(hasItem(DEFAULT_DISPLAY_NAME)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restCollaboratorProjectionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCollaboratorProjectionShouldNotBeFound(String filter) throws Exception {
        restCollaboratorProjectionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCollaboratorProjectionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCollaboratorProjection() throws Exception {
        // Get the collaboratorProjection
        restCollaboratorProjectionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return collaboratorProjectionRepository.count();
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

    protected CollaboratorProjection getPersistedCollaboratorProjection(CollaboratorProjection collaboratorProjection) {
        return collaboratorProjectionRepository.findById(collaboratorProjection.getId()).orElseThrow();
    }

    protected void assertPersistedCollaboratorProjectionToMatchAllProperties(CollaboratorProjection expectedCollaboratorProjection) {
        assertCollaboratorProjectionAllPropertiesEquals(
            expectedCollaboratorProjection,
            getPersistedCollaboratorProjection(expectedCollaboratorProjection)
        );
    }

    protected void assertPersistedCollaboratorProjectionToMatchUpdatableProperties(CollaboratorProjection expectedCollaboratorProjection) {
        assertCollaboratorProjectionAllUpdatablePropertiesEquals(
            expectedCollaboratorProjection,
            getPersistedCollaboratorProjection(expectedCollaboratorProjection)
        );
    }
}
