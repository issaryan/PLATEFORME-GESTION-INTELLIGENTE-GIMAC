package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.RoomRetentionPolicyAsserts.*;
import static cm.gimac.des.collaboration.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.RoomRetentionPolicy;
import cm.gimac.des.collaboration.domain.enumeration.RetentionAnchor;
import cm.gimac.des.collaboration.domain.enumeration.RoomType;
import cm.gimac.des.collaboration.repository.RoomRetentionPolicyRepository;
import cm.gimac.des.collaboration.service.dto.RoomRetentionPolicyDTO;
import cm.gimac.des.collaboration.service.mapper.RoomRetentionPolicyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
 * Integration tests for the {@link RoomRetentionPolicyResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class RoomRetentionPolicyResourceIT {

    private static final RoomType DEFAULT_ROOM_TYPE = RoomType.QUART;
    private static final RoomType UPDATED_ROOM_TYPE = RoomType.THEME;

    private static final Duration DEFAULT_MESSAGE_RETENTION = Duration.ofHours(6);
    private static final Duration UPDATED_MESSAGE_RETENTION = Duration.ofHours(12);

    private static final RetentionAnchor DEFAULT_ANCHOR = RetentionAnchor.ENVOI_MESSAGE;
    private static final RetentionAnchor UPDATED_ANCHOR = RetentionAnchor.CLOTURE_RATTACHEMENT;

    private static final Boolean DEFAULT_ARCHIVED = false;
    private static final Boolean UPDATED_ARCHIVED = true;

    private static final Integer DEFAULT_ARCHIVE_RETENTION_YEARS = 1;
    private static final Integer UPDATED_ARCHIVE_RETENTION_YEARS = 2;

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/room-retention-policies";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RoomRetentionPolicyRepository roomRetentionPolicyRepository;

    @Autowired
    private RoomRetentionPolicyMapper roomRetentionPolicyMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRoomRetentionPolicyMockMvc;

    private RoomRetentionPolicy roomRetentionPolicy;

    private RoomRetentionPolicy insertedRoomRetentionPolicy;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomRetentionPolicy createEntity() {
        return new RoomRetentionPolicy()
            .roomType(DEFAULT_ROOM_TYPE)
            .messageRetention(DEFAULT_MESSAGE_RETENTION)
            .anchor(DEFAULT_ANCHOR)
            .archived(DEFAULT_ARCHIVED)
            .archiveRetentionYears(DEFAULT_ARCHIVE_RETENTION_YEARS)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomRetentionPolicy createUpdatedEntity() {
        return new RoomRetentionPolicy()
            .roomType(UPDATED_ROOM_TYPE)
            .messageRetention(UPDATED_MESSAGE_RETENTION)
            .anchor(UPDATED_ANCHOR)
            .archived(UPDATED_ARCHIVED)
            .archiveRetentionYears(UPDATED_ARCHIVE_RETENTION_YEARS)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        roomRetentionPolicy = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRoomRetentionPolicy != null) {
            roomRetentionPolicyRepository.delete(insertedRoomRetentionPolicy);
            insertedRoomRetentionPolicy = null;
        }
    }

    @Test
    @Transactional
    void createRoomRetentionPolicy() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RoomRetentionPolicy
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);
        var returnedRoomRetentionPolicyDTO = om.readValue(
            restRoomRetentionPolicyMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RoomRetentionPolicyDTO.class
        );

        // Validate the RoomRetentionPolicy in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoomRetentionPolicy = roomRetentionPolicyMapper.toEntity(returnedRoomRetentionPolicyDTO);
        assertRoomRetentionPolicyUpdatableFieldsEquals(
            returnedRoomRetentionPolicy,
            getPersistedRoomRetentionPolicy(returnedRoomRetentionPolicy)
        );

        insertedRoomRetentionPolicy = returnedRoomRetentionPolicy;
    }

    @Test
    @Transactional
    void createRoomRetentionPolicyWithExistingId() throws Exception {
        // Create the RoomRetentionPolicy with an existing ID
        insertedRoomRetentionPolicy = roomRetentionPolicyRepository.saveAndFlush(roomRetentionPolicy);
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRoomRetentionPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomRetentionPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkRoomTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomRetentionPolicy.setRoomType(null);

        // Create the RoomRetentionPolicy, which fails.
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        restRoomRetentionPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMessageRetentionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomRetentionPolicy.setMessageRetention(null);

        // Create the RoomRetentionPolicy, which fails.
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        restRoomRetentionPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAnchorIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomRetentionPolicy.setAnchor(null);

        // Create the RoomRetentionPolicy, which fails.
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        restRoomRetentionPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkArchivedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomRetentionPolicy.setArchived(null);

        // Create the RoomRetentionPolicy, which fails.
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        restRoomRetentionPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUpdatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomRetentionPolicy.setUpdatedAt(null);

        // Create the RoomRetentionPolicy, which fails.
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        restRoomRetentionPolicyMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRoomRetentionPolicies() throws Exception {
        // Initialize the database
        insertedRoomRetentionPolicy = roomRetentionPolicyRepository.saveAndFlush(roomRetentionPolicy);

        // Get all the roomRetentionPolicyList
        restRoomRetentionPolicyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roomRetentionPolicy.getId().toString())))
            .andExpect(jsonPath("$.[*].roomType").value(hasItem(DEFAULT_ROOM_TYPE.toString())))
            .andExpect(jsonPath("$.[*].messageRetention").value(hasItem(DEFAULT_MESSAGE_RETENTION.toString())))
            .andExpect(jsonPath("$.[*].anchor").value(hasItem(DEFAULT_ANCHOR.toString())))
            .andExpect(jsonPath("$.[*].archived").value(hasItem(DEFAULT_ARCHIVED)))
            .andExpect(jsonPath("$.[*].archiveRetentionYears").value(hasItem(DEFAULT_ARCHIVE_RETENTION_YEARS)))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getRoomRetentionPolicy() throws Exception {
        // Initialize the database
        insertedRoomRetentionPolicy = roomRetentionPolicyRepository.saveAndFlush(roomRetentionPolicy);

        // Get the roomRetentionPolicy
        restRoomRetentionPolicyMockMvc
            .perform(get(ENTITY_API_URL_ID, roomRetentionPolicy.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(roomRetentionPolicy.getId().toString()))
            .andExpect(jsonPath("$.roomType").value(DEFAULT_ROOM_TYPE.toString()))
            .andExpect(jsonPath("$.messageRetention").value(DEFAULT_MESSAGE_RETENTION.toString()))
            .andExpect(jsonPath("$.anchor").value(DEFAULT_ANCHOR.toString()))
            .andExpect(jsonPath("$.archived").value(DEFAULT_ARCHIVED))
            .andExpect(jsonPath("$.archiveRetentionYears").value(DEFAULT_ARCHIVE_RETENTION_YEARS))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingRoomRetentionPolicy() throws Exception {
        // Get the roomRetentionPolicy
        restRoomRetentionPolicyMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRoomRetentionPolicy() throws Exception {
        // Initialize the database
        insertedRoomRetentionPolicy = roomRetentionPolicyRepository.saveAndFlush(roomRetentionPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomRetentionPolicy
        RoomRetentionPolicy updatedRoomRetentionPolicy = roomRetentionPolicyRepository.findById(roomRetentionPolicy.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRoomRetentionPolicy are not directly saved in db
        em.detach(updatedRoomRetentionPolicy);
        updatedRoomRetentionPolicy
            .roomType(UPDATED_ROOM_TYPE)
            .messageRetention(UPDATED_MESSAGE_RETENTION)
            .anchor(UPDATED_ANCHOR)
            .archived(UPDATED_ARCHIVED)
            .archiveRetentionYears(UPDATED_ARCHIVE_RETENTION_YEARS)
            .updatedAt(UPDATED_UPDATED_AT);
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(updatedRoomRetentionPolicy);

        restRoomRetentionPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomRetentionPolicyDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isOk());

        // Validate the RoomRetentionPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRoomRetentionPolicyToMatchAllProperties(updatedRoomRetentionPolicy);
    }

    @Test
    @Transactional
    void putNonExistingRoomRetentionPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomRetentionPolicy.setId(UUID.randomUUID());

        // Create the RoomRetentionPolicy
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomRetentionPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomRetentionPolicyDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomRetentionPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRoomRetentionPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomRetentionPolicy.setId(UUID.randomUUID());

        // Create the RoomRetentionPolicy
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomRetentionPolicyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomRetentionPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRoomRetentionPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomRetentionPolicy.setId(UUID.randomUUID());

        // Create the RoomRetentionPolicy
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomRetentionPolicyMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomRetentionPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRoomRetentionPolicyWithPatch() throws Exception {
        // Initialize the database
        insertedRoomRetentionPolicy = roomRetentionPolicyRepository.saveAndFlush(roomRetentionPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomRetentionPolicy using partial update
        RoomRetentionPolicy partialUpdatedRoomRetentionPolicy = new RoomRetentionPolicy();
        partialUpdatedRoomRetentionPolicy.setId(roomRetentionPolicy.getId());

        partialUpdatedRoomRetentionPolicy
            .roomType(UPDATED_ROOM_TYPE)
            .anchor(UPDATED_ANCHOR)
            .archiveRetentionYears(UPDATED_ARCHIVE_RETENTION_YEARS)
            .updatedAt(UPDATED_UPDATED_AT);

        restRoomRetentionPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomRetentionPolicy.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomRetentionPolicy))
            )
            .andExpect(status().isOk());

        // Validate the RoomRetentionPolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomRetentionPolicyUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRoomRetentionPolicy, roomRetentionPolicy),
            getPersistedRoomRetentionPolicy(roomRetentionPolicy)
        );
    }

    @Test
    @Transactional
    void fullUpdateRoomRetentionPolicyWithPatch() throws Exception {
        // Initialize the database
        insertedRoomRetentionPolicy = roomRetentionPolicyRepository.saveAndFlush(roomRetentionPolicy);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomRetentionPolicy using partial update
        RoomRetentionPolicy partialUpdatedRoomRetentionPolicy = new RoomRetentionPolicy();
        partialUpdatedRoomRetentionPolicy.setId(roomRetentionPolicy.getId());

        partialUpdatedRoomRetentionPolicy
            .roomType(UPDATED_ROOM_TYPE)
            .messageRetention(UPDATED_MESSAGE_RETENTION)
            .anchor(UPDATED_ANCHOR)
            .archived(UPDATED_ARCHIVED)
            .archiveRetentionYears(UPDATED_ARCHIVE_RETENTION_YEARS)
            .updatedAt(UPDATED_UPDATED_AT);

        restRoomRetentionPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomRetentionPolicy.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomRetentionPolicy))
            )
            .andExpect(status().isOk());

        // Validate the RoomRetentionPolicy in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomRetentionPolicyUpdatableFieldsEquals(
            partialUpdatedRoomRetentionPolicy,
            getPersistedRoomRetentionPolicy(partialUpdatedRoomRetentionPolicy)
        );
    }

    @Test
    @Transactional
    void patchNonExistingRoomRetentionPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomRetentionPolicy.setId(UUID.randomUUID());

        // Create the RoomRetentionPolicy
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomRetentionPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, roomRetentionPolicyDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomRetentionPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRoomRetentionPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomRetentionPolicy.setId(UUID.randomUUID());

        // Create the RoomRetentionPolicy
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomRetentionPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomRetentionPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRoomRetentionPolicy() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomRetentionPolicy.setId(UUID.randomUUID());

        // Create the RoomRetentionPolicy
        RoomRetentionPolicyDTO roomRetentionPolicyDTO = roomRetentionPolicyMapper.toDto(roomRetentionPolicy);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomRetentionPolicyMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomRetentionPolicyDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomRetentionPolicy in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRoomRetentionPolicy() throws Exception {
        // Initialize the database
        insertedRoomRetentionPolicy = roomRetentionPolicyRepository.saveAndFlush(roomRetentionPolicy);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the roomRetentionPolicy
        restRoomRetentionPolicyMockMvc
            .perform(delete(ENTITY_API_URL_ID, roomRetentionPolicy.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return roomRetentionPolicyRepository.count();
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

    protected RoomRetentionPolicy getPersistedRoomRetentionPolicy(RoomRetentionPolicy roomRetentionPolicy) {
        return roomRetentionPolicyRepository.findById(roomRetentionPolicy.getId()).orElseThrow();
    }

    protected void assertPersistedRoomRetentionPolicyToMatchAllProperties(RoomRetentionPolicy expectedRoomRetentionPolicy) {
        assertRoomRetentionPolicyAllPropertiesEquals(
            expectedRoomRetentionPolicy,
            getPersistedRoomRetentionPolicy(expectedRoomRetentionPolicy)
        );
    }

    protected void assertPersistedRoomRetentionPolicyToMatchUpdatableProperties(RoomRetentionPolicy expectedRoomRetentionPolicy) {
        assertRoomRetentionPolicyAllUpdatablePropertiesEquals(
            expectedRoomRetentionPolicy,
            getPersistedRoomRetentionPolicy(expectedRoomRetentionPolicy)
        );
    }
}
