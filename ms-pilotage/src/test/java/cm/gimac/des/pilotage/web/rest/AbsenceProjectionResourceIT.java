package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.AbsenceProjectionAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.AbsenceProjection;
import cm.gimac.des.pilotage.repository.AbsenceProjectionRepository;
import cm.gimac.des.pilotage.service.mapper.AbsenceProjectionMapper;
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
 * Integration tests for the {@link AbsenceProjectionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AbsenceProjectionResourceIT {

    private static final UUID DEFAULT_AVAILABILITY_ID = UUID.randomUUID();
    private static final UUID UPDATED_AVAILABILITY_ID = UUID.randomUUID();

    private static final UUID DEFAULT_COLLABORATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_COLLABORATOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_STARTS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTS_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ENDS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENDS_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/absence-projections";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AbsenceProjectionRepository absenceProjectionRepository;

    @Autowired
    private AbsenceProjectionMapper absenceProjectionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAbsenceProjectionMockMvc;

    private AbsenceProjection absenceProjection;

    private AbsenceProjection insertedAbsenceProjection;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AbsenceProjection createEntity() {
        return new AbsenceProjection()
            .availabilityId(DEFAULT_AVAILABILITY_ID)
            .collaboratorId(DEFAULT_COLLABORATOR_ID)
            .startsAt(DEFAULT_STARTS_AT)
            .endsAt(DEFAULT_ENDS_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AbsenceProjection createUpdatedEntity() {
        return new AbsenceProjection()
            .availabilityId(UPDATED_AVAILABILITY_ID)
            .collaboratorId(UPDATED_COLLABORATOR_ID)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        absenceProjection = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAbsenceProjection != null) {
            absenceProjectionRepository.delete(insertedAbsenceProjection);
            insertedAbsenceProjection = null;
        }
    }

    @Test
    @Transactional
    void getAllAbsenceProjections() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList
        restAbsenceProjectionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(absenceProjection.getId().toString())))
            .andExpect(jsonPath("$.[*].availabilityId").value(hasItem(DEFAULT_AVAILABILITY_ID.toString())))
            .andExpect(jsonPath("$.[*].collaboratorId").value(hasItem(DEFAULT_COLLABORATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getAbsenceProjection() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get the absenceProjection
        restAbsenceProjectionMockMvc
            .perform(get(ENTITY_API_URL_ID, absenceProjection.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(absenceProjection.getId().toString()))
            .andExpect(jsonPath("$.availabilityId").value(DEFAULT_AVAILABILITY_ID.toString()))
            .andExpect(jsonPath("$.collaboratorId").value(DEFAULT_COLLABORATOR_ID.toString()))
            .andExpect(jsonPath("$.startsAt").value(DEFAULT_STARTS_AT.toString()))
            .andExpect(jsonPath("$.endsAt").value(DEFAULT_ENDS_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getAbsenceProjectionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        UUID id = absenceProjection.getId();

        defaultAbsenceProjectionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByAvailabilityIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where availabilityId equals to
        defaultAbsenceProjectionFiltering(
            "availabilityId.equals=" + DEFAULT_AVAILABILITY_ID,
            "availabilityId.equals=" + UPDATED_AVAILABILITY_ID
        );
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByAvailabilityIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where availabilityId in
        defaultAbsenceProjectionFiltering(
            "availabilityId.in=" + DEFAULT_AVAILABILITY_ID + "," + UPDATED_AVAILABILITY_ID,
            "availabilityId.in=" + UPDATED_AVAILABILITY_ID
        );
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByAvailabilityIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where availabilityId is not null
        defaultAbsenceProjectionFiltering("availabilityId.specified=true", "availabilityId.specified=false");
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByCollaboratorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where collaboratorId equals to
        defaultAbsenceProjectionFiltering(
            "collaboratorId.equals=" + DEFAULT_COLLABORATOR_ID,
            "collaboratorId.equals=" + UPDATED_COLLABORATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByCollaboratorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where collaboratorId in
        defaultAbsenceProjectionFiltering(
            "collaboratorId.in=" + DEFAULT_COLLABORATOR_ID + "," + UPDATED_COLLABORATOR_ID,
            "collaboratorId.in=" + UPDATED_COLLABORATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByCollaboratorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where collaboratorId is not null
        defaultAbsenceProjectionFiltering("collaboratorId.specified=true", "collaboratorId.specified=false");
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByStartsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where startsAt equals to
        defaultAbsenceProjectionFiltering("startsAt.equals=" + DEFAULT_STARTS_AT, "startsAt.equals=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByStartsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where startsAt in
        defaultAbsenceProjectionFiltering("startsAt.in=" + DEFAULT_STARTS_AT + "," + UPDATED_STARTS_AT, "startsAt.in=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByStartsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where startsAt is not null
        defaultAbsenceProjectionFiltering("startsAt.specified=true", "startsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByEndsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where endsAt equals to
        defaultAbsenceProjectionFiltering("endsAt.equals=" + DEFAULT_ENDS_AT, "endsAt.equals=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByEndsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where endsAt in
        defaultAbsenceProjectionFiltering("endsAt.in=" + DEFAULT_ENDS_AT + "," + UPDATED_ENDS_AT, "endsAt.in=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByEndsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where endsAt is not null
        defaultAbsenceProjectionFiltering("endsAt.specified=true", "endsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where updatedAt equals to
        defaultAbsenceProjectionFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where updatedAt in
        defaultAbsenceProjectionFiltering(
            "updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT,
            "updatedAt.in=" + UPDATED_UPDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllAbsenceProjectionsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAbsenceProjection = absenceProjectionRepository.saveAndFlush(absenceProjection);

        // Get all the absenceProjectionList where updatedAt is not null
        defaultAbsenceProjectionFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    private void defaultAbsenceProjectionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultAbsenceProjectionShouldBeFound(shouldBeFound);
        defaultAbsenceProjectionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultAbsenceProjectionShouldBeFound(String filter) throws Exception {
        restAbsenceProjectionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(absenceProjection.getId().toString())))
            .andExpect(jsonPath("$.[*].availabilityId").value(hasItem(DEFAULT_AVAILABILITY_ID.toString())))
            .andExpect(jsonPath("$.[*].collaboratorId").value(hasItem(DEFAULT_COLLABORATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restAbsenceProjectionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultAbsenceProjectionShouldNotBeFound(String filter) throws Exception {
        restAbsenceProjectionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restAbsenceProjectionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingAbsenceProjection() throws Exception {
        // Get the absenceProjection
        restAbsenceProjectionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return absenceProjectionRepository.count();
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

    protected AbsenceProjection getPersistedAbsenceProjection(AbsenceProjection absenceProjection) {
        return absenceProjectionRepository.findById(absenceProjection.getId()).orElseThrow();
    }

    protected void assertPersistedAbsenceProjectionToMatchAllProperties(AbsenceProjection expectedAbsenceProjection) {
        assertAbsenceProjectionAllPropertiesEquals(expectedAbsenceProjection, getPersistedAbsenceProjection(expectedAbsenceProjection));
    }

    protected void assertPersistedAbsenceProjectionToMatchUpdatableProperties(AbsenceProjection expectedAbsenceProjection) {
        assertAbsenceProjectionAllUpdatablePropertiesEquals(
            expectedAbsenceProjection,
            getPersistedAbsenceProjection(expectedAbsenceProjection)
        );
    }
}
