package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.AlertObservationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.AlertObservation;
import cm.gimac.des.incident.repository.AlertObservationRepository;
import cm.gimac.des.incident.service.mapper.AlertObservationMapper;
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
 * Integration tests for the {@link AlertObservationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AlertObservationResourceIT {

    private static final UUID DEFAULT_ALERT_ID = UUID.randomUUID();
    private static final UUID UPDATED_ALERT_ID = UUID.randomUUID();

    private static final String DEFAULT_LINK_ID = "AAAAAAAAAA";
    private static final String UPDATED_LINK_ID = "BBBBBBBBBB";

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_ALERT_SEVERITY = "AAAAAAAAAA";
    private static final String UPDATED_ALERT_SEVERITY = "BBBBBBBBBB";

    private static final Instant DEFAULT_RAISED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RAISED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/alert-observations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AlertObservationRepository alertObservationRepository;

    @Autowired
    private AlertObservationMapper alertObservationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAlertObservationMockMvc;

    private AlertObservation alertObservation;

    private AlertObservation insertedAlertObservation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AlertObservation createEntity() {
        return new AlertObservation()
            .alertId(DEFAULT_ALERT_ID)
            .linkId(DEFAULT_LINK_ID)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .alertSeverity(DEFAULT_ALERT_SEVERITY)
            .raisedAt(DEFAULT_RAISED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AlertObservation createUpdatedEntity() {
        return new AlertObservation()
            .alertId(UPDATED_ALERT_ID)
            .linkId(UPDATED_LINK_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .alertSeverity(UPDATED_ALERT_SEVERITY)
            .raisedAt(UPDATED_RAISED_AT);
    }

    @BeforeEach
    void initTest() {
        alertObservation = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAlertObservation != null) {
            alertObservationRepository.delete(insertedAlertObservation);
            insertedAlertObservation = null;
        }
    }

    @Test
    @Transactional
    void getAllAlertObservations() throws Exception {
        // Initialize the database
        insertedAlertObservation = alertObservationRepository.saveAndFlush(alertObservation);

        // Get all the alertObservationList
        restAlertObservationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(alertObservation.getId().toString())))
            .andExpect(jsonPath("$.[*].alertId").value(hasItem(DEFAULT_ALERT_ID.toString())))
            .andExpect(jsonPath("$.[*].linkId").value(hasItem(DEFAULT_LINK_ID)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].alertSeverity").value(hasItem(DEFAULT_ALERT_SEVERITY)))
            .andExpect(jsonPath("$.[*].raisedAt").value(hasItem(DEFAULT_RAISED_AT.toString())));
    }

    @Test
    @Transactional
    void getAlertObservation() throws Exception {
        // Initialize the database
        insertedAlertObservation = alertObservationRepository.saveAndFlush(alertObservation);

        // Get the alertObservation
        restAlertObservationMockMvc
            .perform(get(ENTITY_API_URL_ID, alertObservation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(alertObservation.getId().toString()))
            .andExpect(jsonPath("$.alertId").value(DEFAULT_ALERT_ID.toString()))
            .andExpect(jsonPath("$.linkId").value(DEFAULT_LINK_ID))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.alertSeverity").value(DEFAULT_ALERT_SEVERITY))
            .andExpect(jsonPath("$.raisedAt").value(DEFAULT_RAISED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingAlertObservation() throws Exception {
        // Get the alertObservation
        restAlertObservationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return alertObservationRepository.count();
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

    protected AlertObservation getPersistedAlertObservation(AlertObservation alertObservation) {
        return alertObservationRepository.findById(alertObservation.getId()).orElseThrow();
    }

    protected void assertPersistedAlertObservationToMatchAllProperties(AlertObservation expectedAlertObservation) {
        assertAlertObservationAllPropertiesEquals(expectedAlertObservation, getPersistedAlertObservation(expectedAlertObservation));
    }

    protected void assertPersistedAlertObservationToMatchUpdatableProperties(AlertObservation expectedAlertObservation) {
        assertAlertObservationAllUpdatablePropertiesEquals(
            expectedAlertObservation,
            getPersistedAlertObservation(expectedAlertObservation)
        );
    }
}
