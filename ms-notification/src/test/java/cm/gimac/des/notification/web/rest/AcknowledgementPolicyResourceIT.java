package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.AcknowledgementPolicyAsserts.*;
import static cm.gimac.des.notification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.AcknowledgementPolicy;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import cm.gimac.des.notification.repository.AcknowledgementPolicyRepository;
import cm.gimac.des.notification.service.dto.AcknowledgementPolicyDTO;
import cm.gimac.des.notification.service.mapper.AcknowledgementPolicyMapper;
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
 * Integration tests for the {@link AcknowledgementPolicyResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AcknowledgementPolicyResourceIT {

    private static final String DEFAULT_NOTIFICATION_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_NOTIFICATION_TYPE = "BBBBBBBBBB";

    private static final UrgencyLevel DEFAULT_URGENCY = UrgencyLevel.INFORMATION;
    private static final UrgencyLevel UPDATED_URGENCY = UrgencyLevel.AVERTISSEMENT;

    private static final Duration DEFAULT_ACK_DELAY = Duration.ofHours(6);
    private static final Duration UPDATED_ACK_DELAY = Duration.ofHours(12);

    private static final Integer DEFAULT_MAX_ESCALATION_LEVEL = 1;
    private static final Integer UPDATED_MAX_ESCALATION_LEVEL = 2;

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/acknowledgement-policies";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AcknowledgementPolicyRepository acknowledgementPolicyRepository;

    @Autowired
    private AcknowledgementPolicyMapper acknowledgementPolicyMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAcknowledgementPolicyMockMvc;

    private AcknowledgementPolicy acknowledgementPolicy;

    private AcknowledgementPolicy insertedAcknowledgementPolicy;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AcknowledgementPolicy createEntity() {
        return new AcknowledgementPolicy()
            .notificationType(DEFAULT_NOTIFICATION_TYPE)
            .urgency(DEFAULT_URGENCY)
            .ackDelay(DEFAULT_ACK_DELAY)
            .maxEscalationLevel(DEFAULT_MAX_ESCALATION_LEVEL)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AcknowledgementPolicy createUpdatedEntity() {
        return new AcknowledgementPolicy()
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .urgency(UPDATED_URGENCY)
            .ackDelay(UPDATED_ACK_DELAY)
            .maxEscalationLevel(UPDATED_MAX_ESCALATION_LEVEL)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    void initTest() {
        acknowledgementPolicy = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAcknowledgementPolicy != null) {
            acknowledgementPolicyRepository.delete(insertedAcknowledgementPolicy);
            insertedAcknowledgementPolicy = null;
        }
    }

    @Test
    @Transactional
    void createAcknowledgementPolicy() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AcknowledgementPolicy
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);
        var returnedAcknowledgementPolicyDTO = om.readValue(
            restAcknowledgementPolicyMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AcknowledgementPolicyDTO.class
        );

        // Validate the AcknowledgementPolicy in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAcknowledgementPolicy = acknowledgementPolicyMapper.toEntity(returnedAcknowledgementPolicyDTO);
        assertAcknowledgementPolicyUpdatableFieldsEquals(
            returnedAcknowledgementPolicy,
            getPersistedAcknowledgementPolicy(returnedAcknowledgementPolicy)
        );

        insertedAcknowledgementPolicy = returnedAcknowledgementPolicy;
    }

    @Test
    @Transactional
    void createAcknowledgementPolicyWithExistingId() throws Exception {
        // Create the AcknowledgementPolicy with an existing ID
        insertedAcknowledgementPolicy = acknowledgementPolicyRepository.saveAndFlush(acknowledgementPolicy);
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAcknowledgementPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcknowledgementPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUrgencyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        acknowledgementPolicy.setUrgency(null);

        // Create the AcknowledgementPolicy, which fails.
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        restAcknowledgementPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAckDelayIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        acknowledgementPolicy.setAckDelay(null);

        // Create the AcknowledgementPolicy, which fails.
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        restAcknowledgementPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMaxEscalationLevelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        acknowledgementPolicy.setMaxEscalationLevel(null);

        // Create the AcknowledgementPolicy, which fails.
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        restAcknowledgementPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        acknowledgementPolicy.setActive(null);

        // Create the AcknowledgementPolicy, which fails.
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        restAcknowledgementPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAcknowledgementPolicies() throws Exception {
        // Initialize the database
        insertedAcknowledgementPolicy = acknowledgementPolicyRepository.saveAndFlush(acknowledgementPolicy);

        // Get all the acknowledgementPolicyList
        restAcknowledgementPolicyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(acknowledgementPolicy.getId().toString())))
            .andExpect(jsonPath("$.[*].notificationType").value(hasItem(DEFAULT_NOTIFICATION_TYPE)))
            .andExpect(jsonPath("$.[*].urgency").value(hasItem(DEFAULT_URGENCY.toString())))
            .andExpect(jsonPath("$.[*].ackDelay").value(hasItem(DEFAULT_ACK_DELAY.toString())))
            .andExpect(jsonPath("$.[*].maxEscalationLevel").value(hasItem(DEFAULT_MAX_ESCALATION_LEVEL)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getAcknowledgementPolicy() throws Exception {
        // Initialize the database
        insertedAcknowledgementPolicy = acknowledgementPolicyRepository.saveAndFlush(acknowledgementPolicy);

        // Get the acknowledgementPolicy
        restAcknowledgementPolicyMockMvc
            .perform(get(ENTITY_API_URL_ID, acknowledgementPolicy.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(acknowledgementPolicy.getId().toString()))
            .andExpect(jsonPath("$.notificationType").value(DEFAULT_NOTIFICATION_TYPE))
            .andExpect(jsonPath("$.urgency").value(DEFAULT_URGENCY.toString()))
            .andExpect(jsonPath("$.ackDelay").value(DEFAULT_ACK_DELAY.toString()))
            .andExpect(jsonPath("$.maxEscalationLevel").value(DEFAULT_MAX_ESCALATION_LEVEL))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingAcknowledgementPolicy() throws Exception {
        // Get the acknowledgementPolicy
        restAcknowledgementPolicyMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAcknowledgementPolicy() throws Exception {
        // Initialize the database
        insertedAcknowledgementPolicy = acknowledgementPolicyRepository.saveAndFlush(acknowledgementPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the acknowledgementPolicy
        AcknowledgementPolicy updatedAcknowledgementPolicy = acknowledgementPolicyRepository
            .findById(acknowledgementPolicy.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedAcknowledgementPolicy are not directly saved in db
        em.detach(updatedAcknowledgementPolicy);
        updatedAcknowledgementPolicy
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .urgency(UPDATED_URGENCY)
            .ackDelay(UPDATED_ACK_DELAY)
            .maxEscalationLevel(UPDATED_MAX_ESCALATION_LEVEL)
            .active(UPDATED_ACTIVE);
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(updatedAcknowledgementPolicy);

        restAcknowledgementPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, acknowledgementPolicyDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isOk());

        // Validate the AcknowledgementPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAcknowledgementPolicyToMatchAllProperties(updatedAcknowledgementPolicy);
    }

    @Test
    @Transactional
    void putNonExistingAcknowledgementPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acknowledgementPolicy.setId(UUID.randomUUID());

        // Create the AcknowledgementPolicy
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAcknowledgementPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, acknowledgementPolicyDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcknowledgementPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAcknowledgementPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acknowledgementPolicy.setId(UUID.randomUUID());

        // Create the AcknowledgementPolicy
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAcknowledgementPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcknowledgementPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAcknowledgementPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acknowledgementPolicy.setId(UUID.randomUUID());

        // Create the AcknowledgementPolicy
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAcknowledgementPolicyMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AcknowledgementPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAcknowledgementPolicyWithPatch() throws Exception {
        // Initialize the database
        insertedAcknowledgementPolicy = acknowledgementPolicyRepository.saveAndFlush(acknowledgementPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the acknowledgementPolicy using partial update
        AcknowledgementPolicy partialUpdatedAcknowledgementPolicy = new AcknowledgementPolicy();
        partialUpdatedAcknowledgementPolicy.setId(acknowledgementPolicy.getId());

        partialUpdatedAcknowledgementPolicy.notificationType(UPDATED_NOTIFICATION_TYPE);

        restAcknowledgementPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAcknowledgementPolicy.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAcknowledgementPolicy))
            )
            .andExpect(status().isOk());

        // Validate the AcknowledgementPolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAcknowledgementPolicyUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAcknowledgementPolicy, acknowledgementPolicy),
            getPersistedAcknowledgementPolicy(acknowledgementPolicy)
        );
    }

    @Test
    @Transactional
    void fullUpdateAcknowledgementPolicyWithPatch() throws Exception {
        // Initialize the database
        insertedAcknowledgementPolicy = acknowledgementPolicyRepository.saveAndFlush(acknowledgementPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the acknowledgementPolicy using partial update
        AcknowledgementPolicy partialUpdatedAcknowledgementPolicy = new AcknowledgementPolicy();
        partialUpdatedAcknowledgementPolicy.setId(acknowledgementPolicy.getId());

        partialUpdatedAcknowledgementPolicy
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .urgency(UPDATED_URGENCY)
            .ackDelay(UPDATED_ACK_DELAY)
            .maxEscalationLevel(UPDATED_MAX_ESCALATION_LEVEL)
            .active(UPDATED_ACTIVE);

        restAcknowledgementPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAcknowledgementPolicy.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAcknowledgementPolicy))
            )
            .andExpect(status().isOk());

        // Validate the AcknowledgementPolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAcknowledgementPolicyUpdatableFieldsEquals(
            partialUpdatedAcknowledgementPolicy,
            getPersistedAcknowledgementPolicy(partialUpdatedAcknowledgementPolicy)
        );
    }

    @Test
    @Transactional
    void patchNonExistingAcknowledgementPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acknowledgementPolicy.setId(UUID.randomUUID());

        // Create the AcknowledgementPolicy
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAcknowledgementPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, acknowledgementPolicyDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcknowledgementPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAcknowledgementPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acknowledgementPolicy.setId(UUID.randomUUID());

        // Create the AcknowledgementPolicy
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAcknowledgementPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AcknowledgementPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAcknowledgementPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        acknowledgementPolicy.setId(UUID.randomUUID());

        // Create the AcknowledgementPolicy
        AcknowledgementPolicyDTO acknowledgementPolicyDTO = acknowledgementPolicyMapper.toDto(acknowledgementPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAcknowledgementPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(acknowledgementPolicyDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AcknowledgementPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAcknowledgementPolicy() throws Exception {
        // Initialize the database
        insertedAcknowledgementPolicy = acknowledgementPolicyRepository.saveAndFlush(acknowledgementPolicy);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the acknowledgementPolicy
        restAcknowledgementPolicyMockMvc
            .perform(delete(ENTITY_API_URL_ID, acknowledgementPolicy.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return acknowledgementPolicyRepository.count();
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

    protected AcknowledgementPolicy getPersistedAcknowledgementPolicy(AcknowledgementPolicy acknowledgementPolicy) {
        return acknowledgementPolicyRepository.findById(acknowledgementPolicy.getId()).orElseThrow();
    }

    protected void assertPersistedAcknowledgementPolicyToMatchAllProperties(AcknowledgementPolicy expectedAcknowledgementPolicy) {
        assertAcknowledgementPolicyAllPropertiesEquals(
            expectedAcknowledgementPolicy,
            getPersistedAcknowledgementPolicy(expectedAcknowledgementPolicy)
        );
    }

    protected void assertPersistedAcknowledgementPolicyToMatchUpdatableProperties(AcknowledgementPolicy expectedAcknowledgementPolicy) {
        assertAcknowledgementPolicyAllUpdatablePropertiesEquals(
            expectedAcknowledgementPolicy,
            getPersistedAcknowledgementPolicy(expectedAcknowledgementPolicy)
        );
    }
}
