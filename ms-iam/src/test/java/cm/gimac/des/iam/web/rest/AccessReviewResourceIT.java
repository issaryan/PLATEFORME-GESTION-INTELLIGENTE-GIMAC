package cm.gimac.des.iam.web.rest;

import static cm.gimac.des.iam.domain.AccessReviewAsserts.*;
import static cm.gimac.des.iam.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.iam.IntegrationTest;
import cm.gimac.des.iam.domain.AccessReview;
import cm.gimac.des.iam.domain.enumeration.ReviewState;
import cm.gimac.des.iam.repository.AccessReviewRepository;
import cm.gimac.des.iam.service.dto.AccessReviewDTO;
import cm.gimac.des.iam.service.mapper.AccessReviewMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AccessReviewResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AccessReviewResourceIT {

    private static final String DEFAULT_PERIOD = "AAAAAAAAAA";
    private static final String UPDATED_PERIOD = "BBBBBBBBBB";

    private static final ReviewState DEFAULT_STATE = ReviewState.OUVERTE;
    private static final ReviewState UPDATED_STATE = ReviewState.CLOSE;

    private static final Instant DEFAULT_OPENED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OPENED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final UUID DEFAULT_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROVER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final String DEFAULT_DIGEST = "AAAAAAAAAA";
    private static final String UPDATED_DIGEST = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/access-reviews";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AccessReviewRepository accessReviewRepository;

    @Autowired
    private AccessReviewMapper accessReviewMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAccessReviewMockMvc;

    private AccessReview accessReview;

    private AccessReview insertedAccessReview;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AccessReview createEntity() {
        return new AccessReview()
            .period(DEFAULT_PERIOD)
            .state(DEFAULT_STATE)
            .openedAt(DEFAULT_OPENED_AT)
            .approverId(DEFAULT_APPROVER_ID)
            .closedAt(DEFAULT_CLOSED_AT)
            .digest(DEFAULT_DIGEST);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AccessReview createUpdatedEntity() {
        return new AccessReview()
            .period(UPDATED_PERIOD)
            .state(UPDATED_STATE)
            .openedAt(UPDATED_OPENED_AT)
            .approverId(UPDATED_APPROVER_ID)
            .closedAt(UPDATED_CLOSED_AT)
            .digest(UPDATED_DIGEST);
    }

    @BeforeEach
    void initTest() {
        accessReview = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAccessReview != null) {
            accessReviewRepository.delete(insertedAccessReview);
            insertedAccessReview = null;
        }
    }

    @Test
    @Transactional
    void createAccessReview() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AccessReview
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);
        var returnedAccessReviewDTO = om.readValue(
            restAccessReviewMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(accessReviewDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AccessReviewDTO.class
        );

        // Validate the AccessReview in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAccessReview = accessReviewMapper.toEntity(returnedAccessReviewDTO);
        assertAccessReviewUpdatableFieldsEquals(returnedAccessReview, getPersistedAccessReview(returnedAccessReview));

        insertedAccessReview = returnedAccessReview;
    }

    @Test
    @Transactional
    void createAccessReviewWithExistingId() throws Exception {
        // Create the AccessReview with an existing ID
        insertedAccessReview = accessReviewRepository.saveAndFlush(accessReview);
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAccessReviewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReview in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkPeriodIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        accessReview.setPeriod(null);

        // Create the AccessReview, which fails.
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        restAccessReviewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        accessReview.setState(null);

        // Create the AccessReview, which fails.
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        restAccessReviewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOpenedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        accessReview.setOpenedAt(null);

        // Create the AccessReview, which fails.
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        restAccessReviewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAccessReviews() throws Exception {
        // Initialize the database
        insertedAccessReview = accessReviewRepository.saveAndFlush(accessReview);

        // Get all the accessReviewList
        restAccessReviewMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(accessReview.getId().toString())))
            .andExpect(jsonPath("$.[*].period").value(hasItem(DEFAULT_PERIOD)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].openedAt").value(hasItem(DEFAULT_OPENED_AT.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())))
            .andExpect(jsonPath("$.[*].digest").value(hasItem(DEFAULT_DIGEST)));
    }

    @Test
    @Transactional
    void getAccessReview() throws Exception {
        // Initialize the database
        insertedAccessReview = accessReviewRepository.saveAndFlush(accessReview);

        // Get the accessReview
        restAccessReviewMockMvc
            .perform(get(ENTITY_API_URL_ID, accessReview.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(accessReview.getId().toString()))
            .andExpect(jsonPath("$.period").value(DEFAULT_PERIOD))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.openedAt").value(DEFAULT_OPENED_AT.toString()))
            .andExpect(jsonPath("$.approverId").value(DEFAULT_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()))
            .andExpect(jsonPath("$.digest").value(DEFAULT_DIGEST));
    }

    @Test
    @Transactional
    void getNonExistingAccessReview() throws Exception {
        // Get the accessReview
        restAccessReviewMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAccessReview() throws Exception {
        // Initialize the database
        insertedAccessReview = accessReviewRepository.saveAndFlush(accessReview);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the accessReview
        AccessReview updatedAccessReview = accessReviewRepository.findById(accessReview.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAccessReview are not directly saved in db
        em.detach(updatedAccessReview);
        updatedAccessReview
            .period(UPDATED_PERIOD)
            .state(UPDATED_STATE)
            .openedAt(UPDATED_OPENED_AT)
            .approverId(UPDATED_APPROVER_ID)
            .closedAt(UPDATED_CLOSED_AT)
            .digest(UPDATED_DIGEST);
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(updatedAccessReview);

        restAccessReviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, accessReviewDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isOk());

        // Validate the AccessReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAccessReviewToMatchAllProperties(updatedAccessReview);
    }

    @Test
    @Transactional
    void putNonExistingAccessReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReview.setId(UUID.randomUUID());

        // Create the AccessReview
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAccessReviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, accessReviewDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAccessReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReview.setId(UUID.randomUUID());

        // Create the AccessReview
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAccessReviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAccessReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReview.setId(UUID.randomUUID());

        // Create the AccessReview
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAccessReviewMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AccessReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAccessReviewWithPatch() throws Exception {
        // Initialize the database
        insertedAccessReview = accessReviewRepository.saveAndFlush(accessReview);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the accessReview using partial update
        AccessReview partialUpdatedAccessReview = new AccessReview();
        partialUpdatedAccessReview.setId(accessReview.getId());

        partialUpdatedAccessReview.period(UPDATED_PERIOD).openedAt(UPDATED_OPENED_AT).closedAt(UPDATED_CLOSED_AT);

        restAccessReviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAccessReview.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAccessReview))
            )
            .andExpect(status().isOk());

        // Validate the AccessReview in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAccessReviewUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAccessReview, accessReview),
            getPersistedAccessReview(accessReview)
        );
    }

    @Test
    @Transactional
    void fullUpdateAccessReviewWithPatch() throws Exception {
        // Initialize the database
        insertedAccessReview = accessReviewRepository.saveAndFlush(accessReview);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the accessReview using partial update
        AccessReview partialUpdatedAccessReview = new AccessReview();
        partialUpdatedAccessReview.setId(accessReview.getId());

        partialUpdatedAccessReview
            .period(UPDATED_PERIOD)
            .state(UPDATED_STATE)
            .openedAt(UPDATED_OPENED_AT)
            .approverId(UPDATED_APPROVER_ID)
            .closedAt(UPDATED_CLOSED_AT)
            .digest(UPDATED_DIGEST);

        restAccessReviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAccessReview.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAccessReview))
            )
            .andExpect(status().isOk());

        // Validate the AccessReview in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAccessReviewUpdatableFieldsEquals(partialUpdatedAccessReview, getPersistedAccessReview(partialUpdatedAccessReview));
    }

    @Test
    @Transactional
    void patchNonExistingAccessReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReview.setId(UUID.randomUUID());

        // Create the AccessReview
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAccessReviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, accessReviewDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAccessReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReview.setId(UUID.randomUUID());

        // Create the AccessReview
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAccessReviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAccessReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReview.setId(UUID.randomUUID());

        // Create the AccessReview
        AccessReviewDTO accessReviewDTO = accessReviewMapper.toDto(accessReview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAccessReviewMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(accessReviewDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AccessReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAccessReview() throws Exception {
        // Initialize the database
        insertedAccessReview = accessReviewRepository.saveAndFlush(accessReview);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the accessReview
        restAccessReviewMockMvc
            .perform(delete(ENTITY_API_URL_ID, accessReview.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return accessReviewRepository.count();
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

    protected AccessReview getPersistedAccessReview(AccessReview accessReview) {
        return accessReviewRepository.findById(accessReview.getId()).orElseThrow();
    }

    protected void assertPersistedAccessReviewToMatchAllProperties(AccessReview expectedAccessReview) {
        assertAccessReviewAllPropertiesEquals(expectedAccessReview, getPersistedAccessReview(expectedAccessReview));
    }

    protected void assertPersistedAccessReviewToMatchUpdatableProperties(AccessReview expectedAccessReview) {
        assertAccessReviewAllUpdatablePropertiesEquals(expectedAccessReview, getPersistedAccessReview(expectedAccessReview));
    }
}
