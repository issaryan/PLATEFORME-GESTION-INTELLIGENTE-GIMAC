package cm.gimac.des.monitoring.web.rest;

import static cm.gimac.des.monitoring.domain.AlertSeverityPolicyAsserts.*;
import static cm.gimac.des.monitoring.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.monitoring.IntegrationTest;
import cm.gimac.des.monitoring.domain.AlertSeverityPolicy;
import cm.gimac.des.monitoring.domain.enumeration.AlertSeverity;
import cm.gimac.des.monitoring.domain.enumeration.NotificationLevel;
import cm.gimac.des.monitoring.domain.enumeration.TicketPolicy;
import cm.gimac.des.monitoring.repository.AlertSeverityPolicyRepository;
import cm.gimac.des.monitoring.service.dto.AlertSeverityPolicyDTO;
import cm.gimac.des.monitoring.service.mapper.AlertSeverityPolicyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
 * Integration tests for the {@link AlertSeverityPolicyResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AlertSeverityPolicyResourceIT {

    private static final AlertSeverity DEFAULT_SEVERITY = AlertSeverity.MINEURE;
    private static final AlertSeverity UPDATED_SEVERITY = AlertSeverity.MAJEURE;

    private static final Duration DEFAULT_ACK_DELAY = Duration.ofHours(6);
    private static final Duration UPDATED_ACK_DELAY = Duration.ofHours(12);

    private static final NotificationLevel DEFAULT_NOTIFICATION_LEVEL = NotificationLevel.CRITIQUE;
    private static final NotificationLevel UPDATED_NOTIFICATION_LEVEL = NotificationLevel.URGENT;

    private static final Boolean DEFAULT_URGENT_CHANNEL = false;
    private static final Boolean UPDATED_URGENT_CHANNEL = true;

    private static final TicketPolicy DEFAULT_TICKET_POLICY = TicketPolicy.OBLIGATOIRE;
    private static final TicketPolicy UPDATED_TICKET_POLICY = TicketPolicy.A_APPRECIATION;

    private static final String ENTITY_API_URL = "/api/alert-severity-policies";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AlertSeverityPolicyRepository alertSeverityPolicyRepository;

    @Autowired
    private AlertSeverityPolicyMapper alertSeverityPolicyMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAlertSeverityPolicyMockMvc;

    private AlertSeverityPolicy alertSeverityPolicy;

    private AlertSeverityPolicy insertedAlertSeverityPolicy;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AlertSeverityPolicy createEntity() {
        return new AlertSeverityPolicy()
            .severity(DEFAULT_SEVERITY)
            .ackDelay(DEFAULT_ACK_DELAY)
            .notificationLevel(DEFAULT_NOTIFICATION_LEVEL)
            .urgentChannel(DEFAULT_URGENT_CHANNEL)
            .ticketPolicy(DEFAULT_TICKET_POLICY);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AlertSeverityPolicy createUpdatedEntity() {
        return new AlertSeverityPolicy()
            .severity(UPDATED_SEVERITY)
            .ackDelay(UPDATED_ACK_DELAY)
            .notificationLevel(UPDATED_NOTIFICATION_LEVEL)
            .urgentChannel(UPDATED_URGENT_CHANNEL)
            .ticketPolicy(UPDATED_TICKET_POLICY);
    }

    @BeforeEach
    void initTest() {
        alertSeverityPolicy = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAlertSeverityPolicy != null) {
            alertSeverityPolicyRepository.delete(insertedAlertSeverityPolicy);
            insertedAlertSeverityPolicy = null;
        }
    }

    @Test
    @Transactional
    void createAlertSeverityPolicy() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AlertSeverityPolicy
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);
        var returnedAlertSeverityPolicyDTO = om.readValue(
            restAlertSeverityPolicyMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AlertSeverityPolicyDTO.class
        );

        // Validate the AlertSeverityPolicy in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAlertSeverityPolicy = alertSeverityPolicyMapper.toEntity(returnedAlertSeverityPolicyDTO);
        assertAlertSeverityPolicyUpdatableFieldsEquals(
            returnedAlertSeverityPolicy,
            getPersistedAlertSeverityPolicy(returnedAlertSeverityPolicy)
        );

        insertedAlertSeverityPolicy = returnedAlertSeverityPolicy;
    }

    @Test
    @Transactional
    void createAlertSeverityPolicyWithExistingId() throws Exception {
        // Create the AlertSeverityPolicy with an existing ID
        insertedAlertSeverityPolicy = alertSeverityPolicyRepository.saveAndFlush(alertSeverityPolicy);
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAlertSeverityPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSeverityPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSeverityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSeverityPolicy.setSeverity(null);

        // Create the AlertSeverityPolicy, which fails.
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        restAlertSeverityPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAckDelayIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSeverityPolicy.setAckDelay(null);

        // Create the AlertSeverityPolicy, which fails.
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        restAlertSeverityPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNotificationLevelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSeverityPolicy.setNotificationLevel(null);

        // Create the AlertSeverityPolicy, which fails.
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        restAlertSeverityPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUrgentChannelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSeverityPolicy.setUrgentChannel(null);

        // Create the AlertSeverityPolicy, which fails.
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        restAlertSeverityPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTicketPolicyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        alertSeverityPolicy.setTicketPolicy(null);

        // Create the AlertSeverityPolicy, which fails.
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        restAlertSeverityPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAlertSeverityPolicies() throws Exception {
        // Initialize the database
        insertedAlertSeverityPolicy = alertSeverityPolicyRepository.saveAndFlush(alertSeverityPolicy);

        // Get all the alertSeverityPolicyList
        restAlertSeverityPolicyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(alertSeverityPolicy.getId().toString())))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY.toString())))
            .andExpect(jsonPath("$.[*].ackDelay").value(hasItem(DEFAULT_ACK_DELAY.toString())))
            .andExpect(jsonPath("$.[*].notificationLevel").value(hasItem(DEFAULT_NOTIFICATION_LEVEL.toString())))
            .andExpect(jsonPath("$.[*].urgentChannel").value(hasItem(DEFAULT_URGENT_CHANNEL)))
            .andExpect(jsonPath("$.[*].ticketPolicy").value(hasItem(DEFAULT_TICKET_POLICY.toString())));
    }

    @Test
    @Transactional
    void getAlertSeverityPolicy() throws Exception {
        // Initialize the database
        insertedAlertSeverityPolicy = alertSeverityPolicyRepository.saveAndFlush(alertSeverityPolicy);

        // Get the alertSeverityPolicy
        restAlertSeverityPolicyMockMvc
            .perform(get(ENTITY_API_URL_ID, alertSeverityPolicy.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(alertSeverityPolicy.getId().toString()))
            .andExpect(jsonPath("$.severity").value(DEFAULT_SEVERITY.toString()))
            .andExpect(jsonPath("$.ackDelay").value(DEFAULT_ACK_DELAY.toString()))
            .andExpect(jsonPath("$.notificationLevel").value(DEFAULT_NOTIFICATION_LEVEL.toString()))
            .andExpect(jsonPath("$.urgentChannel").value(DEFAULT_URGENT_CHANNEL))
            .andExpect(jsonPath("$.ticketPolicy").value(DEFAULT_TICKET_POLICY.toString()));
    }

    @Test
    @Transactional
    void getNonExistingAlertSeverityPolicy() throws Exception {
        // Get the alertSeverityPolicy
        restAlertSeverityPolicyMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAlertSeverityPolicy() throws Exception {
        // Initialize the database
        insertedAlertSeverityPolicy = alertSeverityPolicyRepository.saveAndFlush(alertSeverityPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the alertSeverityPolicy
        AlertSeverityPolicy updatedAlertSeverityPolicy = alertSeverityPolicyRepository.findById(alertSeverityPolicy.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAlertSeverityPolicy are not directly saved in db
        em.detach(updatedAlertSeverityPolicy);
        updatedAlertSeverityPolicy
            .severity(UPDATED_SEVERITY)
            .ackDelay(UPDATED_ACK_DELAY)
            .notificationLevel(UPDATED_NOTIFICATION_LEVEL)
            .urgentChannel(UPDATED_URGENT_CHANNEL)
            .ticketPolicy(UPDATED_TICKET_POLICY);
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(updatedAlertSeverityPolicy);

        restAlertSeverityPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, alertSeverityPolicyDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isOk());

        // Validate the AlertSeverityPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAlertSeverityPolicyToMatchAllProperties(updatedAlertSeverityPolicy);
    }

    @Test
    @Transactional
    void putNonExistingAlertSeverityPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSeverityPolicy.setId(UUID.randomUUID());

        // Create the AlertSeverityPolicy
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAlertSeverityPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, alertSeverityPolicyDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSeverityPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAlertSeverityPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSeverityPolicy.setId(UUID.randomUUID());

        // Create the AlertSeverityPolicy
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAlertSeverityPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSeverityPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAlertSeverityPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSeverityPolicy.setId(UUID.randomUUID());

        // Create the AlertSeverityPolicy
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAlertSeverityPolicyMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AlertSeverityPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAlertSeverityPolicyWithPatch() throws Exception {
        // Initialize the database
        insertedAlertSeverityPolicy = alertSeverityPolicyRepository.saveAndFlush(alertSeverityPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the alertSeverityPolicy using partial update
        AlertSeverityPolicy partialUpdatedAlertSeverityPolicy = new AlertSeverityPolicy();
        partialUpdatedAlertSeverityPolicy.setId(alertSeverityPolicy.getId());

        partialUpdatedAlertSeverityPolicy.ticketPolicy(UPDATED_TICKET_POLICY);

        restAlertSeverityPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAlertSeverityPolicy.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAlertSeverityPolicy))
            )
            .andExpect(status().isOk());

        // Validate the AlertSeverityPolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAlertSeverityPolicyUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAlertSeverityPolicy, alertSeverityPolicy),
            getPersistedAlertSeverityPolicy(alertSeverityPolicy)
        );
    }

    @Test
    @Transactional
    void fullUpdateAlertSeverityPolicyWithPatch() throws Exception {
        // Initialize the database
        insertedAlertSeverityPolicy = alertSeverityPolicyRepository.saveAndFlush(alertSeverityPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the alertSeverityPolicy using partial update
        AlertSeverityPolicy partialUpdatedAlertSeverityPolicy = new AlertSeverityPolicy();
        partialUpdatedAlertSeverityPolicy.setId(alertSeverityPolicy.getId());

        partialUpdatedAlertSeverityPolicy
            .severity(UPDATED_SEVERITY)
            .ackDelay(UPDATED_ACK_DELAY)
            .notificationLevel(UPDATED_NOTIFICATION_LEVEL)
            .urgentChannel(UPDATED_URGENT_CHANNEL)
            .ticketPolicy(UPDATED_TICKET_POLICY);

        restAlertSeverityPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAlertSeverityPolicy.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAlertSeverityPolicy))
            )
            .andExpect(status().isOk());

        // Validate the AlertSeverityPolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAlertSeverityPolicyUpdatableFieldsEquals(
            partialUpdatedAlertSeverityPolicy,
            getPersistedAlertSeverityPolicy(partialUpdatedAlertSeverityPolicy)
        );
    }

    @Test
    @Transactional
    void patchNonExistingAlertSeverityPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSeverityPolicy.setId(UUID.randomUUID());

        // Create the AlertSeverityPolicy
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAlertSeverityPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, alertSeverityPolicyDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSeverityPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAlertSeverityPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSeverityPolicy.setId(UUID.randomUUID());

        // Create the AlertSeverityPolicy
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAlertSeverityPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AlertSeverityPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAlertSeverityPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        alertSeverityPolicy.setId(UUID.randomUUID());

        // Create the AlertSeverityPolicy
        AlertSeverityPolicyDTO alertSeverityPolicyDTO = alertSeverityPolicyMapper.toDto(alertSeverityPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAlertSeverityPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(alertSeverityPolicyDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AlertSeverityPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAlertSeverityPolicy() throws Exception {
        // Initialize the database
        insertedAlertSeverityPolicy = alertSeverityPolicyRepository.saveAndFlush(alertSeverityPolicy);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the alertSeverityPolicy
        restAlertSeverityPolicyMockMvc
            .perform(delete(ENTITY_API_URL_ID, alertSeverityPolicy.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return alertSeverityPolicyRepository.count();
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

    protected AlertSeverityPolicy getPersistedAlertSeverityPolicy(AlertSeverityPolicy alertSeverityPolicy) {
        return alertSeverityPolicyRepository.findById(alertSeverityPolicy.getId()).orElseThrow();
    }

    protected void assertPersistedAlertSeverityPolicyToMatchAllProperties(AlertSeverityPolicy expectedAlertSeverityPolicy) {
        assertAlertSeverityPolicyAllPropertiesEquals(
            expectedAlertSeverityPolicy,
            getPersistedAlertSeverityPolicy(expectedAlertSeverityPolicy)
        );
    }

    protected void assertPersistedAlertSeverityPolicyToMatchUpdatableProperties(AlertSeverityPolicy expectedAlertSeverityPolicy) {
        assertAlertSeverityPolicyAllUpdatablePropertiesEquals(
            expectedAlertSeverityPolicy,
            getPersistedAlertSeverityPolicy(expectedAlertSeverityPolicy)
        );
    }
}
