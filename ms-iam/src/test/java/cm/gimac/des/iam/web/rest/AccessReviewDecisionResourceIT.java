package cm.gimac.des.iam.web.rest;

import static cm.gimac.des.iam.domain.AccessReviewDecisionAsserts.*;
import static cm.gimac.des.iam.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.iam.IntegrationTest;
import cm.gimac.des.iam.domain.AccessReview;
import cm.gimac.des.iam.domain.AccessReviewDecision;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import cm.gimac.des.iam.repository.AccessReviewDecisionRepository;
import cm.gimac.des.iam.service.AccessReviewDecisionService;
import cm.gimac.des.iam.service.dto.AccessReviewDecisionDTO;
import cm.gimac.des.iam.service.mapper.AccessReviewDecisionMapper;
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
 * Integration tests for the {@link AccessReviewDecisionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class AccessReviewDecisionResourceIT {

    private static final ReviewDecision DEFAULT_DECISION = ReviewDecision.MAINTENU;
    private static final ReviewDecision UPDATED_DECISION = ReviewDecision.REDUIT;

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

    private static final UUID DEFAULT_DECIDED_BY = UUID.randomUUID();
    private static final UUID UPDATED_DECIDED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_DECIDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DECIDED_AT = Instant.ofEpochMilli(1702849447300L);

    private static final String ENTITY_API_URL = "/api/access-review-decisions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AccessReviewDecisionRepository accessReviewDecisionRepository;

    @Mock
    private AccessReviewDecisionRepository accessReviewDecisionRepositoryMock;

    @Autowired
    private AccessReviewDecisionMapper accessReviewDecisionMapper;

    @Mock
    private AccessReviewDecisionService accessReviewDecisionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAccessReviewDecisionMockMvc;

    private AccessReviewDecision accessReviewDecision;

    private AccessReviewDecision insertedAccessReviewDecision;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AccessReviewDecision createEntity(EntityManager em) {
        AccessReviewDecision accessReviewDecision = new AccessReviewDecision()
            .decision(DEFAULT_DECISION)
            .comment(DEFAULT_COMMENT)
            .decidedBy(DEFAULT_DECIDED_BY)
            .decidedAt(DEFAULT_DECIDED_AT);
        // Add required entity
        AccessReview accessReview;
        if (TestUtil.findAll(em, AccessReview.class).isEmpty()) {
            accessReview = AccessReviewResourceIT.createEntity();
            em.persist(accessReview);
            em.flush();
        } else {
            accessReview = TestUtil.findAll(em, AccessReview.class).getFirst();
        }
        accessReviewDecision.setReview(accessReview);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        accessReviewDecision.setCollaborator(collaborator);
        return accessReviewDecision;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AccessReviewDecision createUpdatedEntity(EntityManager em) {
        AccessReviewDecision updatedAccessReviewDecision = new AccessReviewDecision()
            .decision(UPDATED_DECISION)
            .comment(UPDATED_COMMENT)
            .decidedBy(UPDATED_DECIDED_BY)
            .decidedAt(UPDATED_DECIDED_AT);
        // Add required entity
        AccessReview accessReview;
        if (TestUtil.findAll(em, AccessReview.class).isEmpty()) {
            accessReview = AccessReviewResourceIT.createUpdatedEntity();
            em.persist(accessReview);
            em.flush();
        } else {
            accessReview = TestUtil.findAll(em, AccessReview.class).getFirst();
        }
        updatedAccessReviewDecision.setReview(accessReview);
        // Add required entity
        Collaborator collaborator;
        if (TestUtil.findAll(em, Collaborator.class).isEmpty()) {
            collaborator = CollaboratorResourceIT.createUpdatedEntity();
            em.persist(collaborator);
            em.flush();
        } else {
            collaborator = TestUtil.findAll(em, Collaborator.class).getFirst();
        }
        updatedAccessReviewDecision.setCollaborator(collaborator);
        return updatedAccessReviewDecision;
    }

    @BeforeEach
    void initTest() {
        accessReviewDecision = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAccessReviewDecision != null) {
            accessReviewDecisionRepository.delete(insertedAccessReviewDecision);
            insertedAccessReviewDecision = null;
        }
    }

    @Test
    @Transactional
    void createAccessReviewDecision() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AccessReviewDecision
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);
        var returnedAccessReviewDecisionDTO = om.readValue(
            restAccessReviewDecisionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(accessReviewDecisionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AccessReviewDecisionDTO.class
        );

        // Validate the AccessReviewDecision in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAccessReviewDecision = accessReviewDecisionMapper.toEntity(returnedAccessReviewDecisionDTO);
        assertAccessReviewDecisionUpdatableFieldsEquals(
            returnedAccessReviewDecision,
            getPersistedAccessReviewDecision(returnedAccessReviewDecision)
        );

        insertedAccessReviewDecision = returnedAccessReviewDecision;
    }

    @Test
    @Transactional
    void createAccessReviewDecisionWithExistingId() throws Exception {
        // Create the AccessReviewDecision with an existing ID
        insertedAccessReviewDecision = accessReviewDecisionRepository.saveAndFlush(accessReviewDecision);
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAccessReviewDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReviewDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDecisionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        accessReviewDecision.setDecision(null);

        // Create the AccessReviewDecision, which fails.
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        restAccessReviewDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDecidedByIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        accessReviewDecision.setDecidedBy(null);

        // Create the AccessReviewDecision, which fails.
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        restAccessReviewDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDecidedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        accessReviewDecision.setDecidedAt(null);

        // Create the AccessReviewDecision, which fails.
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        restAccessReviewDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAccessReviewDecisions() throws Exception {
        // Initialize the database
        insertedAccessReviewDecision = accessReviewDecisionRepository.saveAndFlush(accessReviewDecision);

        // Get all the accessReviewDecisionList
        restAccessReviewDecisionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(accessReviewDecision.getId().toString())))
            .andExpect(jsonPath("$.[*].decision").value(hasItem(DEFAULT_DECISION.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)))
            .andExpect(jsonPath("$.[*].decidedBy").value(hasItem(DEFAULT_DECIDED_BY.toString())))
            .andExpect(jsonPath("$.[*].decidedAt").value(hasItem(DEFAULT_DECIDED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAccessReviewDecisionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(accessReviewDecisionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAccessReviewDecisionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(accessReviewDecisionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAccessReviewDecisionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(accessReviewDecisionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAccessReviewDecisionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(accessReviewDecisionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAccessReviewDecision() throws Exception {
        // Initialize the database
        insertedAccessReviewDecision = accessReviewDecisionRepository.saveAndFlush(accessReviewDecision);

        // Get the accessReviewDecision
        restAccessReviewDecisionMockMvc
            .perform(get(ENTITY_API_URL_ID, accessReviewDecision.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(accessReviewDecision.getId().toString()))
            .andExpect(jsonPath("$.decision").value(DEFAULT_DECISION.toString()))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT))
            .andExpect(jsonPath("$.decidedBy").value(DEFAULT_DECIDED_BY.toString()))
            .andExpect(jsonPath("$.decidedAt").value(DEFAULT_DECIDED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingAccessReviewDecision() throws Exception {
        // Get the accessReviewDecision
        restAccessReviewDecisionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAccessReviewDecision() throws Exception {
        // Initialize the database
        insertedAccessReviewDecision = accessReviewDecisionRepository.saveAndFlush(accessReviewDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the accessReviewDecision
        AccessReviewDecision updatedAccessReviewDecision = accessReviewDecisionRepository
            .findById(accessReviewDecision.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedAccessReviewDecision are not directly saved in db
        em.detach(updatedAccessReviewDecision);
        updatedAccessReviewDecision
            .decision(UPDATED_DECISION)
            .comment(UPDATED_COMMENT)
            .decidedBy(UPDATED_DECIDED_BY)
            .decidedAt(UPDATED_DECIDED_AT);
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(updatedAccessReviewDecision);

        restAccessReviewDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, accessReviewDecisionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isOk());

        // Validate the AccessReviewDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAccessReviewDecisionToMatchAllProperties(updatedAccessReviewDecision);
    }

    @Test
    @Transactional
    void putNonExistingAccessReviewDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReviewDecision.setId(UUID.randomUUID());

        // Create the AccessReviewDecision
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAccessReviewDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, accessReviewDecisionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReviewDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAccessReviewDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReviewDecision.setId(UUID.randomUUID());

        // Create the AccessReviewDecision
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAccessReviewDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReviewDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAccessReviewDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReviewDecision.setId(UUID.randomUUID());

        // Create the AccessReviewDecision
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAccessReviewDecisionMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AccessReviewDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAccessReviewDecisionWithPatch() throws Exception {
        // Initialize the database
        insertedAccessReviewDecision = accessReviewDecisionRepository.saveAndFlush(accessReviewDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the accessReviewDecision using partial update
        AccessReviewDecision partialUpdatedAccessReviewDecision = new AccessReviewDecision();
        partialUpdatedAccessReviewDecision.setId(accessReviewDecision.getId());

        partialUpdatedAccessReviewDecision.decision(UPDATED_DECISION).decidedAt(UPDATED_DECIDED_AT);

        restAccessReviewDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAccessReviewDecision.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAccessReviewDecision))
            )
            .andExpect(status().isOk());

        // Validate the AccessReviewDecision in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAccessReviewDecisionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAccessReviewDecision, accessReviewDecision),
            getPersistedAccessReviewDecision(accessReviewDecision)
        );
    }

    @Test
    @Transactional
    void fullUpdateAccessReviewDecisionWithPatch() throws Exception {
        // Initialize the database
        insertedAccessReviewDecision = accessReviewDecisionRepository.saveAndFlush(accessReviewDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the accessReviewDecision using partial update
        AccessReviewDecision partialUpdatedAccessReviewDecision = new AccessReviewDecision();
        partialUpdatedAccessReviewDecision.setId(accessReviewDecision.getId());

        partialUpdatedAccessReviewDecision
            .decision(UPDATED_DECISION)
            .comment(UPDATED_COMMENT)
            .decidedBy(UPDATED_DECIDED_BY)
            .decidedAt(UPDATED_DECIDED_AT);

        restAccessReviewDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAccessReviewDecision.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAccessReviewDecision))
            )
            .andExpect(status().isOk());

        // Validate the AccessReviewDecision in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAccessReviewDecisionUpdatableFieldsEquals(
            partialUpdatedAccessReviewDecision,
            getPersistedAccessReviewDecision(partialUpdatedAccessReviewDecision)
        );
    }

    @Test
    @Transactional
    void patchNonExistingAccessReviewDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReviewDecision.setId(UUID.randomUUID());

        // Create the AccessReviewDecision
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAccessReviewDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, accessReviewDecisionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReviewDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAccessReviewDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReviewDecision.setId(UUID.randomUUID());

        // Create the AccessReviewDecision
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAccessReviewDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AccessReviewDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAccessReviewDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        accessReviewDecision.setId(UUID.randomUUID());

        // Create the AccessReviewDecision
        AccessReviewDecisionDTO accessReviewDecisionDTO = accessReviewDecisionMapper.toDto(accessReviewDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAccessReviewDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(accessReviewDecisionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AccessReviewDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAccessReviewDecision() throws Exception {
        // Initialize the database
        insertedAccessReviewDecision = accessReviewDecisionRepository.saveAndFlush(accessReviewDecision);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the accessReviewDecision
        restAccessReviewDecisionMockMvc
            .perform(delete(ENTITY_API_URL_ID, accessReviewDecision.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return accessReviewDecisionRepository.count();
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

    protected AccessReviewDecision getPersistedAccessReviewDecision(AccessReviewDecision accessReviewDecision) {
        return accessReviewDecisionRepository.findById(accessReviewDecision.getId()).orElseThrow();
    }

    protected void assertPersistedAccessReviewDecisionToMatchAllProperties(AccessReviewDecision expectedAccessReviewDecision) {
        assertAccessReviewDecisionAllPropertiesEquals(
            expectedAccessReviewDecision,
            getPersistedAccessReviewDecision(expectedAccessReviewDecision)
        );
    }

    protected void assertPersistedAccessReviewDecisionToMatchUpdatableProperties(AccessReviewDecision expectedAccessReviewDecision) {
        assertAccessReviewDecisionAllUpdatablePropertiesEquals(
            expectedAccessReviewDecision,
            getPersistedAccessReviewDecision(expectedAccessReviewDecision)
        );
    }
}
