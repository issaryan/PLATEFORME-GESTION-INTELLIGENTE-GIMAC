package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.ServiceCommitmentAsserts.*;
import static cm.gimac.des.incident.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.ServiceCommitment;
import cm.gimac.des.incident.domain.enumeration.CrisisActivation;
import cm.gimac.des.incident.domain.enumeration.Severity;
import cm.gimac.des.incident.repository.ServiceCommitmentRepository;
import cm.gimac.des.incident.service.dto.ServiceCommitmentDTO;
import cm.gimac.des.incident.service.mapper.ServiceCommitmentMapper;
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
 * Integration tests for the {@link ServiceCommitmentResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ServiceCommitmentResourceIT {

    private static final Severity DEFAULT_SEVERITY = Severity.S1;
    private static final Severity UPDATED_SEVERITY = Severity.S2;

    private static final Duration DEFAULT_TAKE_CHARGE = Duration.ofHours(6);
    private static final Duration UPDATED_TAKE_CHARGE = Duration.ofHours(12);

    private static final Duration DEFAULT_RESTORATION = Duration.ofHours(6);
    private static final Duration UPDATED_RESTORATION = Duration.ofHours(12);

    private static final String DEFAULT_ESCALATION_STEPS = "AAAAAAAAAA";
    private static final String UPDATED_ESCALATION_STEPS = "BBBBBBBBBB";

    private static final CrisisActivation DEFAULT_CRISIS_ACTIVATION = CrisisActivation.AUTOMATIQUE;
    private static final CrisisActivation UPDATED_CRISIS_ACTIVATION = CrisisActivation.SUR_DECISION;

    private static final Duration DEFAULT_OBSERVATION_PERIOD = Duration.ofHours(6);
    private static final Duration UPDATED_OBSERVATION_PERIOD = Duration.ofHours(12);

    private static final String ENTITY_API_URL = "/api/service-commitments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ServiceCommitmentRepository serviceCommitmentRepository;

    @Autowired
    private ServiceCommitmentMapper serviceCommitmentMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restServiceCommitmentMockMvc;

    private ServiceCommitment serviceCommitment;

    private ServiceCommitment insertedServiceCommitment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ServiceCommitment createEntity() {
        return new ServiceCommitment()
            .severity(DEFAULT_SEVERITY)
            .takeCharge(DEFAULT_TAKE_CHARGE)
            .restoration(DEFAULT_RESTORATION)
            .escalationSteps(DEFAULT_ESCALATION_STEPS)
            .crisisActivation(DEFAULT_CRISIS_ACTIVATION)
            .observationPeriod(DEFAULT_OBSERVATION_PERIOD);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ServiceCommitment createUpdatedEntity() {
        return new ServiceCommitment()
            .severity(UPDATED_SEVERITY)
            .takeCharge(UPDATED_TAKE_CHARGE)
            .restoration(UPDATED_RESTORATION)
            .escalationSteps(UPDATED_ESCALATION_STEPS)
            .crisisActivation(UPDATED_CRISIS_ACTIVATION)
            .observationPeriod(UPDATED_OBSERVATION_PERIOD);
    }

    @BeforeEach
    void initTest() {
        serviceCommitment = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedServiceCommitment != null) {
            serviceCommitmentRepository.delete(insertedServiceCommitment);
            insertedServiceCommitment = null;
        }
    }

    @Test
    @Transactional
    void createServiceCommitment() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ServiceCommitment
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);
        var returnedServiceCommitmentDTO = om.readValue(
            restServiceCommitmentMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(serviceCommitmentDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ServiceCommitmentDTO.class
        );

        // Validate the ServiceCommitment in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedServiceCommitment = serviceCommitmentMapper.toEntity(returnedServiceCommitmentDTO);
        assertServiceCommitmentUpdatableFieldsEquals(returnedServiceCommitment, getPersistedServiceCommitment(returnedServiceCommitment));

        insertedServiceCommitment = returnedServiceCommitment;
    }

    @Test
    @Transactional
    void createServiceCommitmentWithExistingId() throws Exception {
        // Create the ServiceCommitment with an existing ID
        insertedServiceCommitment = serviceCommitmentRepository.saveAndFlush(serviceCommitment);
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restServiceCommitmentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ServiceCommitment in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSeverityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        serviceCommitment.setSeverity(null);

        // Create the ServiceCommitment, which fails.
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        restServiceCommitmentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTakeChargeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        serviceCommitment.setTakeCharge(null);

        // Create the ServiceCommitment, which fails.
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        restServiceCommitmentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRestorationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        serviceCommitment.setRestoration(null);

        // Create the ServiceCommitment, which fails.
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        restServiceCommitmentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCrisisActivationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        serviceCommitment.setCrisisActivation(null);

        // Create the ServiceCommitment, which fails.
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        restServiceCommitmentMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllServiceCommitments() throws Exception {
        // Initialize the database
        insertedServiceCommitment = serviceCommitmentRepository.saveAndFlush(serviceCommitment);

        // Get all the serviceCommitmentList
        restServiceCommitmentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(serviceCommitment.getId().toString())))
            .andExpect(jsonPath("$.[*].severity").value(hasItem(DEFAULT_SEVERITY.toString())))
            .andExpect(jsonPath("$.[*].takeCharge").value(hasItem(DEFAULT_TAKE_CHARGE.toString())))
            .andExpect(jsonPath("$.[*].restoration").value(hasItem(DEFAULT_RESTORATION.toString())))
            .andExpect(jsonPath("$.[*].escalationSteps").value(hasItem(DEFAULT_ESCALATION_STEPS)))
            .andExpect(jsonPath("$.[*].crisisActivation").value(hasItem(DEFAULT_CRISIS_ACTIVATION.toString())))
            .andExpect(jsonPath("$.[*].observationPeriod").value(hasItem(DEFAULT_OBSERVATION_PERIOD.toString())));
    }

    @Test
    @Transactional
    void getServiceCommitment() throws Exception {
        // Initialize the database
        insertedServiceCommitment = serviceCommitmentRepository.saveAndFlush(serviceCommitment);

        // Get the serviceCommitment
        restServiceCommitmentMockMvc
            .perform(get(ENTITY_API_URL_ID, serviceCommitment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(serviceCommitment.getId().toString()))
            .andExpect(jsonPath("$.severity").value(DEFAULT_SEVERITY.toString()))
            .andExpect(jsonPath("$.takeCharge").value(DEFAULT_TAKE_CHARGE.toString()))
            .andExpect(jsonPath("$.restoration").value(DEFAULT_RESTORATION.toString()))
            .andExpect(jsonPath("$.escalationSteps").value(DEFAULT_ESCALATION_STEPS))
            .andExpect(jsonPath("$.crisisActivation").value(DEFAULT_CRISIS_ACTIVATION.toString()))
            .andExpect(jsonPath("$.observationPeriod").value(DEFAULT_OBSERVATION_PERIOD.toString()));
    }

    @Test
    @Transactional
    void getNonExistingServiceCommitment() throws Exception {
        // Get the serviceCommitment
        restServiceCommitmentMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingServiceCommitment() throws Exception {
        // Initialize the database
        insertedServiceCommitment = serviceCommitmentRepository.saveAndFlush(serviceCommitment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the serviceCommitment
        ServiceCommitment updatedServiceCommitment = serviceCommitmentRepository.findById(serviceCommitment.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedServiceCommitment are not directly saved in db
        em.detach(updatedServiceCommitment);
        updatedServiceCommitment
            .severity(UPDATED_SEVERITY)
            .takeCharge(UPDATED_TAKE_CHARGE)
            .restoration(UPDATED_RESTORATION)
            .escalationSteps(UPDATED_ESCALATION_STEPS)
            .crisisActivation(UPDATED_CRISIS_ACTIVATION)
            .observationPeriod(UPDATED_OBSERVATION_PERIOD);
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(updatedServiceCommitment);

        restServiceCommitmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, serviceCommitmentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isOk());

        // Validate the ServiceCommitment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedServiceCommitmentToMatchAllProperties(updatedServiceCommitment);
    }

    @Test
    @Transactional
    void putNonExistingServiceCommitment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        serviceCommitment.setId(UUID.randomUUID());

        // Create the ServiceCommitment
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restServiceCommitmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, serviceCommitmentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ServiceCommitment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchServiceCommitment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        serviceCommitment.setId(UUID.randomUUID());

        // Create the ServiceCommitment
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restServiceCommitmentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ServiceCommitment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamServiceCommitment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        serviceCommitment.setId(UUID.randomUUID());

        // Create the ServiceCommitment
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restServiceCommitmentMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ServiceCommitment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateServiceCommitmentWithPatch() throws Exception {
        // Initialize the database
        insertedServiceCommitment = serviceCommitmentRepository.saveAndFlush(serviceCommitment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the serviceCommitment using partial update
        ServiceCommitment partialUpdatedServiceCommitment = new ServiceCommitment();
        partialUpdatedServiceCommitment.setId(serviceCommitment.getId());

        partialUpdatedServiceCommitment.severity(UPDATED_SEVERITY).takeCharge(UPDATED_TAKE_CHARGE).restoration(UPDATED_RESTORATION);

        restServiceCommitmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedServiceCommitment.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedServiceCommitment))
            )
            .andExpect(status().isOk());

        // Validate the ServiceCommitment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertServiceCommitmentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedServiceCommitment, serviceCommitment),
            getPersistedServiceCommitment(serviceCommitment)
        );
    }

    @Test
    @Transactional
    void fullUpdateServiceCommitmentWithPatch() throws Exception {
        // Initialize the database
        insertedServiceCommitment = serviceCommitmentRepository.saveAndFlush(serviceCommitment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the serviceCommitment using partial update
        ServiceCommitment partialUpdatedServiceCommitment = new ServiceCommitment();
        partialUpdatedServiceCommitment.setId(serviceCommitment.getId());

        partialUpdatedServiceCommitment
            .severity(UPDATED_SEVERITY)
            .takeCharge(UPDATED_TAKE_CHARGE)
            .restoration(UPDATED_RESTORATION)
            .escalationSteps(UPDATED_ESCALATION_STEPS)
            .crisisActivation(UPDATED_CRISIS_ACTIVATION)
            .observationPeriod(UPDATED_OBSERVATION_PERIOD);

        restServiceCommitmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedServiceCommitment.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedServiceCommitment))
            )
            .andExpect(status().isOk());

        // Validate the ServiceCommitment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertServiceCommitmentUpdatableFieldsEquals(
            partialUpdatedServiceCommitment,
            getPersistedServiceCommitment(partialUpdatedServiceCommitment)
        );
    }

    @Test
    @Transactional
    void patchNonExistingServiceCommitment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        serviceCommitment.setId(UUID.randomUUID());

        // Create the ServiceCommitment
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restServiceCommitmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, serviceCommitmentDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ServiceCommitment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchServiceCommitment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        serviceCommitment.setId(UUID.randomUUID());

        // Create the ServiceCommitment
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restServiceCommitmentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ServiceCommitment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamServiceCommitment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        serviceCommitment.setId(UUID.randomUUID());

        // Create the ServiceCommitment
        ServiceCommitmentDTO serviceCommitmentDTO = serviceCommitmentMapper.toDto(serviceCommitment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restServiceCommitmentMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(serviceCommitmentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ServiceCommitment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteServiceCommitment() throws Exception {
        // Initialize the database
        insertedServiceCommitment = serviceCommitmentRepository.saveAndFlush(serviceCommitment);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the serviceCommitment
        restServiceCommitmentMockMvc
            .perform(delete(ENTITY_API_URL_ID, serviceCommitment.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return serviceCommitmentRepository.count();
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

    protected ServiceCommitment getPersistedServiceCommitment(ServiceCommitment serviceCommitment) {
        return serviceCommitmentRepository.findById(serviceCommitment.getId()).orElseThrow();
    }

    protected void assertPersistedServiceCommitmentToMatchAllProperties(ServiceCommitment expectedServiceCommitment) {
        assertServiceCommitmentAllPropertiesEquals(expectedServiceCommitment, getPersistedServiceCommitment(expectedServiceCommitment));
    }

    protected void assertPersistedServiceCommitmentToMatchUpdatableProperties(ServiceCommitment expectedServiceCommitment) {
        assertServiceCommitmentAllUpdatablePropertiesEquals(
            expectedServiceCommitment,
            getPersistedServiceCommitment(expectedServiceCommitment)
        );
    }
}
