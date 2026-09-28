package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.OperatorUnavailabilityAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import cm.gimac.des.monitoring.repository.OperatorUnavailabilityRepository;
import cm.gimac.des.monitoring.service.mapper.OperatorUnavailabilityMapper;
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
 * Integration tests for the {@link OperatorUnavailabilityResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class OperatorUnavailabilityResourceIT {

    private static final UUID DEFAULT_AVAILABILITY_ID = UUID.randomUUID();
    private static final UUID UPDATED_AVAILABILITY_ID = UUID.randomUUID();

    private static final UUID DEFAULT_OPERATOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_OPERATOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_STARTS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTS_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ENDS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENDS_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_VALIDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VALIDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_CANCELLED = false;
    private static final Boolean UPDATED_CANCELLED = true;

    private static final String ENTITY_API_URL = "/api/operator-unavailabilities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OperatorUnavailabilityRepository operatorUnavailabilityRepository;

    @Autowired
    private OperatorUnavailabilityMapper operatorUnavailabilityMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOperatorUnavailabilityMockMvc;

    private OperatorUnavailability operatorUnavailability;

    private OperatorUnavailability insertedOperatorUnavailability;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OperatorUnavailability createEntity() {
        return new OperatorUnavailability()
            .availabilityId(DEFAULT_AVAILABILITY_ID)
            .operatorId(DEFAULT_OPERATOR_ID)
            .startsAt(DEFAULT_STARTS_AT)
            .endsAt(DEFAULT_ENDS_AT)
            .validatedAt(DEFAULT_VALIDATED_AT)
            .cancelled(DEFAULT_CANCELLED);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OperatorUnavailability createUpdatedEntity() {
        return new OperatorUnavailability()
            .availabilityId(UPDATED_AVAILABILITY_ID)
            .operatorId(UPDATED_OPERATOR_ID)
            .startsAt(UPDATED_STARTS_AT)
            .endsAt(UPDATED_ENDS_AT)
            .validatedAt(UPDATED_VALIDATED_AT)
            .cancelled(UPDATED_CANCELLED);
    }

    @BeforeEach
    void initTest() {
        operatorUnavailability = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedOperatorUnavailability != null) {
            operatorUnavailabilityRepository.delete(insertedOperatorUnavailability);
            insertedOperatorUnavailability = null;
        }
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilities() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList
        restOperatorUnavailabilityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(operatorUnavailability.getId().toString())))
            .andExpect(jsonPath("$.[*].availabilityId").value(hasItem(DEFAULT_AVAILABILITY_ID.toString())))
            .andExpect(jsonPath("$.[*].operatorId").value(hasItem(DEFAULT_OPERATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].cancelled").value(hasItem(DEFAULT_CANCELLED)));
    }

    @Test
    @Transactional
    void getOperatorUnavailability() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get the operatorUnavailability
        restOperatorUnavailabilityMockMvc
            .perform(get(ENTITY_API_URL_ID, operatorUnavailability.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(operatorUnavailability.getId().toString()))
            .andExpect(jsonPath("$.availabilityId").value(DEFAULT_AVAILABILITY_ID.toString()))
            .andExpect(jsonPath("$.operatorId").value(DEFAULT_OPERATOR_ID.toString()))
            .andExpect(jsonPath("$.startsAt").value(DEFAULT_STARTS_AT.toString()))
            .andExpect(jsonPath("$.endsAt").value(DEFAULT_ENDS_AT.toString()))
            .andExpect(jsonPath("$.validatedAt").value(DEFAULT_VALIDATED_AT.toString()))
            .andExpect(jsonPath("$.cancelled").value(DEFAULT_CANCELLED));
    }

    @Test
    @Transactional
    void getOperatorUnavailabilitiesByIdFiltering() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        UUID id = operatorUnavailability.getId();

        defaultOperatorUnavailabilityFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByAvailabilityIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where availabilityId equals to
        defaultOperatorUnavailabilityFiltering(
            "availabilityId.equals=" + DEFAULT_AVAILABILITY_ID,
            "availabilityId.equals=" + UPDATED_AVAILABILITY_ID
        );
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByAvailabilityIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where availabilityId in
        defaultOperatorUnavailabilityFiltering(
            "availabilityId.in=" + DEFAULT_AVAILABILITY_ID + "," + UPDATED_AVAILABILITY_ID,
            "availabilityId.in=" + UPDATED_AVAILABILITY_ID
        );
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByAvailabilityIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where availabilityId is not null
        defaultOperatorUnavailabilityFiltering("availabilityId.specified=true", "availabilityId.specified=false");
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByOperatorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where operatorId equals to
        defaultOperatorUnavailabilityFiltering("operatorId.equals=" + DEFAULT_OPERATOR_ID, "operatorId.equals=" + UPDATED_OPERATOR_ID);
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByOperatorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where operatorId in
        defaultOperatorUnavailabilityFiltering(
            "operatorId.in=" + DEFAULT_OPERATOR_ID + "," + UPDATED_OPERATOR_ID,
            "operatorId.in=" + UPDATED_OPERATOR_ID
        );
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByOperatorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where operatorId is not null
        defaultOperatorUnavailabilityFiltering("operatorId.specified=true", "operatorId.specified=false");
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByStartsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where startsAt equals to
        defaultOperatorUnavailabilityFiltering("startsAt.equals=" + DEFAULT_STARTS_AT, "startsAt.equals=" + UPDATED_STARTS_AT);
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByStartsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where startsAt in
        defaultOperatorUnavailabilityFiltering(
            "startsAt.in=" + DEFAULT_STARTS_AT + "," + UPDATED_STARTS_AT,
            "startsAt.in=" + UPDATED_STARTS_AT
        );
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByStartsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where startsAt is not null
        defaultOperatorUnavailabilityFiltering("startsAt.specified=true", "startsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByEndsAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where endsAt equals to
        defaultOperatorUnavailabilityFiltering("endsAt.equals=" + DEFAULT_ENDS_AT, "endsAt.equals=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByEndsAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where endsAt in
        defaultOperatorUnavailabilityFiltering("endsAt.in=" + DEFAULT_ENDS_AT + "," + UPDATED_ENDS_AT, "endsAt.in=" + UPDATED_ENDS_AT);
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByEndsAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where endsAt is not null
        defaultOperatorUnavailabilityFiltering("endsAt.specified=true", "endsAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByValidatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where validatedAt equals to
        defaultOperatorUnavailabilityFiltering("validatedAt.equals=" + DEFAULT_VALIDATED_AT, "validatedAt.equals=" + UPDATED_VALIDATED_AT);
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByValidatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where validatedAt in
        defaultOperatorUnavailabilityFiltering(
            "validatedAt.in=" + DEFAULT_VALIDATED_AT + "," + UPDATED_VALIDATED_AT,
            "validatedAt.in=" + UPDATED_VALIDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByValidatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where validatedAt is not null
        defaultOperatorUnavailabilityFiltering("validatedAt.specified=true", "validatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByCancelledIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where cancelled equals to
        defaultOperatorUnavailabilityFiltering("cancelled.equals=" + DEFAULT_CANCELLED, "cancelled.equals=" + UPDATED_CANCELLED);
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByCancelledIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where cancelled in
        defaultOperatorUnavailabilityFiltering(
            "cancelled.in=" + DEFAULT_CANCELLED + "," + UPDATED_CANCELLED,
            "cancelled.in=" + UPDATED_CANCELLED
        );
    }

    @Test
    @Transactional
    void getAllOperatorUnavailabilitiesByCancelledIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOperatorUnavailability = operatorUnavailabilityRepository.saveAndFlush(operatorUnavailability);

        // Get all the operatorUnavailabilityList where cancelled is not null
        defaultOperatorUnavailabilityFiltering("cancelled.specified=true", "cancelled.specified=false");
    }

    private void defaultOperatorUnavailabilityFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultOperatorUnavailabilityShouldBeFound(shouldBeFound);
        defaultOperatorUnavailabilityShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultOperatorUnavailabilityShouldBeFound(String filter) throws Exception {
        restOperatorUnavailabilityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(operatorUnavailability.getId().toString())))
            .andExpect(jsonPath("$.[*].availabilityId").value(hasItem(DEFAULT_AVAILABILITY_ID.toString())))
            .andExpect(jsonPath("$.[*].operatorId").value(hasItem(DEFAULT_OPERATOR_ID.toString())))
            .andExpect(jsonPath("$.[*].startsAt").value(hasItem(DEFAULT_STARTS_AT.toString())))
            .andExpect(jsonPath("$.[*].endsAt").value(hasItem(DEFAULT_ENDS_AT.toString())))
            .andExpect(jsonPath("$.[*].validatedAt").value(hasItem(DEFAULT_VALIDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].cancelled").value(hasItem(DEFAULT_CANCELLED)));

        // Check, that the count call also returns 1
        restOperatorUnavailabilityMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultOperatorUnavailabilityShouldNotBeFound(String filter) throws Exception {
        restOperatorUnavailabilityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restOperatorUnavailabilityMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingOperatorUnavailability() throws Exception {
        // Get the operatorUnavailability
        restOperatorUnavailabilityMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return operatorUnavailabilityRepository.count();
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

    protected OperatorUnavailability getPersistedOperatorUnavailability(OperatorUnavailability operatorUnavailability) {
        return operatorUnavailabilityRepository.findById(operatorUnavailability.getId()).orElseThrow();
    }

    protected void assertPersistedOperatorUnavailabilityToMatchAllProperties(OperatorUnavailability expectedOperatorUnavailability) {
        assertOperatorUnavailabilityAllPropertiesEquals(
            expectedOperatorUnavailability,
            getPersistedOperatorUnavailability(expectedOperatorUnavailability)
        );
    }

    protected void assertPersistedOperatorUnavailabilityToMatchUpdatableProperties(OperatorUnavailability expectedOperatorUnavailability) {
        assertOperatorUnavailabilityAllUpdatablePropertiesEquals(
            expectedOperatorUnavailability,
            getPersistedOperatorUnavailability(expectedOperatorUnavailability)
        );
    }
}
