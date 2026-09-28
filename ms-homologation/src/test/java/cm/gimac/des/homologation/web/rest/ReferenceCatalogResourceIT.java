package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ReferenceCatalogAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ReferenceCatalog;
import cm.gimac.des.homologation.domain.enumeration.ProjectType;
import cm.gimac.des.homologation.repository.ReferenceCatalogRepository;
import cm.gimac.des.homologation.service.dto.ReferenceCatalogDTO;
import cm.gimac.des.homologation.service.mapper.ReferenceCatalogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link ReferenceCatalogResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ReferenceCatalogResourceIT {

    private static final ProjectType DEFAULT_PROJECT_TYPE = ProjectType.CARTE_INTERBANCAIRE;
    private static final ProjectType UPDATED_PROJECT_TYPE = ProjectType.PAIEMENT_MOBILE;

    private static final String DEFAULT_VERSION = "PK-94358.296";
    private static final String UPDATED_VERSION = "SHJX-7.3084";

    private static final LocalDate DEFAULT_EFFECTIVE_FROM = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_EFFECTIVE_FROM = LocalDate.now(ZoneId.systemDefault());

    private static final Boolean DEFAULT_PUBLISHED = false;
    private static final Boolean UPDATED_PUBLISHED = true;

    private static final UUID DEFAULT_AUTHOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_AUTHOR_ID = UUID.randomUUID();

    private static final UUID DEFAULT_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROVER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_PUBLISHED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PUBLISHED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_DIFF_SUMMARY = "AAAAAAAAAA";
    private static final String UPDATED_DIFF_SUMMARY = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/reference-catalogs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ReferenceCatalogRepository referenceCatalogRepository;

    @Autowired
    private ReferenceCatalogMapper referenceCatalogMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restReferenceCatalogMockMvc;

    private ReferenceCatalog referenceCatalog;

    private ReferenceCatalog insertedReferenceCatalog;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferenceCatalog createEntity() {
        return new ReferenceCatalog()
            .projectType(DEFAULT_PROJECT_TYPE)
            .version(DEFAULT_VERSION)
            .effectiveFrom(DEFAULT_EFFECTIVE_FROM)
            .published(DEFAULT_PUBLISHED)
            .authorId(DEFAULT_AUTHOR_ID)
            .approverId(DEFAULT_APPROVER_ID)
            .publishedAt(DEFAULT_PUBLISHED_AT)
            .diffSummary(DEFAULT_DIFF_SUMMARY);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ReferenceCatalog createUpdatedEntity() {
        return new ReferenceCatalog()
            .projectType(UPDATED_PROJECT_TYPE)
            .version(UPDATED_VERSION)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .published(UPDATED_PUBLISHED)
            .authorId(UPDATED_AUTHOR_ID)
            .approverId(UPDATED_APPROVER_ID)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .diffSummary(UPDATED_DIFF_SUMMARY);
    }

    @BeforeEach
    void initTest() {
        referenceCatalog = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedReferenceCatalog != null) {
            referenceCatalogRepository.delete(insertedReferenceCatalog);
            insertedReferenceCatalog = null;
        }
    }

    @Test
    @Transactional
    void createReferenceCatalog() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ReferenceCatalog
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);
        var returnedReferenceCatalogDTO = om.readValue(
            restReferenceCatalogMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(referenceCatalogDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ReferenceCatalogDTO.class
        );

        // Validate the ReferenceCatalog in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedReferenceCatalog = referenceCatalogMapper.toEntity(returnedReferenceCatalogDTO);
        assertReferenceCatalogUpdatableFieldsEquals(returnedReferenceCatalog, getPersistedReferenceCatalog(returnedReferenceCatalog));

        insertedReferenceCatalog = returnedReferenceCatalog;
    }

    @Test
    @Transactional
    void createReferenceCatalogWithExistingId() throws Exception {
        // Create the ReferenceCatalog with an existing ID
        insertedReferenceCatalog = referenceCatalogRepository.saveAndFlush(referenceCatalog);
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restReferenceCatalogMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceCatalog in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkProjectTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceCatalog.setProjectType(null);

        // Create the ReferenceCatalog, which fails.
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        restReferenceCatalogMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVersionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceCatalog.setVersion(null);

        // Create the ReferenceCatalog, which fails.
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        restReferenceCatalogMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEffectiveFromIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceCatalog.setEffectiveFrom(null);

        // Create the ReferenceCatalog, which fails.
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        restReferenceCatalogMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPublishedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceCatalog.setPublished(null);

        // Create the ReferenceCatalog, which fails.
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        restReferenceCatalogMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAuthorIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        referenceCatalog.setAuthorId(null);

        // Create the ReferenceCatalog, which fails.
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        restReferenceCatalogMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllReferenceCatalogs() throws Exception {
        // Initialize the database
        insertedReferenceCatalog = referenceCatalogRepository.saveAndFlush(referenceCatalog);

        // Get all the referenceCatalogList
        restReferenceCatalogMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(referenceCatalog.getId().toString())))
            .andExpect(jsonPath("$.[*].projectType").value(hasItem(DEFAULT_PROJECT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)))
            .andExpect(jsonPath("$.[*].effectiveFrom").value(hasItem(DEFAULT_EFFECTIVE_FROM.toString())))
            .andExpect(jsonPath("$.[*].published").value(hasItem(DEFAULT_PUBLISHED)))
            .andExpect(jsonPath("$.[*].authorId").value(hasItem(DEFAULT_AUTHOR_ID.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())))
            .andExpect(jsonPath("$.[*].diffSummary").value(hasItem(DEFAULT_DIFF_SUMMARY)));
    }

    @Test
    @Transactional
    void getReferenceCatalog() throws Exception {
        // Initialize the database
        insertedReferenceCatalog = referenceCatalogRepository.saveAndFlush(referenceCatalog);

        // Get the referenceCatalog
        restReferenceCatalogMockMvc
            .perform(get(ENTITY_API_URL_ID, referenceCatalog.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(referenceCatalog.getId().toString()))
            .andExpect(jsonPath("$.projectType").value(DEFAULT_PROJECT_TYPE.toString()))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION))
            .andExpect(jsonPath("$.effectiveFrom").value(DEFAULT_EFFECTIVE_FROM.toString()))
            .andExpect(jsonPath("$.published").value(DEFAULT_PUBLISHED))
            .andExpect(jsonPath("$.authorId").value(DEFAULT_AUTHOR_ID.toString()))
            .andExpect(jsonPath("$.approverId").value(DEFAULT_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.publishedAt").value(DEFAULT_PUBLISHED_AT.toString()))
            .andExpect(jsonPath("$.diffSummary").value(DEFAULT_DIFF_SUMMARY));
    }

    @Test
    @Transactional
    void getNonExistingReferenceCatalog() throws Exception {
        // Get the referenceCatalog
        restReferenceCatalogMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingReferenceCatalog() throws Exception {
        // Initialize the database
        insertedReferenceCatalog = referenceCatalogRepository.saveAndFlush(referenceCatalog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceCatalog
        ReferenceCatalog updatedReferenceCatalog = referenceCatalogRepository.findById(referenceCatalog.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedReferenceCatalog are not directly saved in db
        em.detach(updatedReferenceCatalog);
        updatedReferenceCatalog
            .projectType(UPDATED_PROJECT_TYPE)
            .version(UPDATED_VERSION)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .published(UPDATED_PUBLISHED)
            .authorId(UPDATED_AUTHOR_ID)
            .approverId(UPDATED_APPROVER_ID)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .diffSummary(UPDATED_DIFF_SUMMARY);
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(updatedReferenceCatalog);

        restReferenceCatalogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referenceCatalogDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceCatalog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedReferenceCatalogToMatchAllProperties(updatedReferenceCatalog);
    }

    @Test
    @Transactional
    void putNonExistingReferenceCatalog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceCatalog.setId(UUID.randomUUID());

        // Create the ReferenceCatalog
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferenceCatalogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, referenceCatalogDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceCatalog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchReferenceCatalog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceCatalog.setId(UUID.randomUUID());

        // Create the ReferenceCatalog
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceCatalogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceCatalog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamReferenceCatalog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceCatalog.setId(UUID.randomUUID());

        // Create the ReferenceCatalog
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceCatalogMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferenceCatalog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateReferenceCatalogWithPatch() throws Exception {
        // Initialize the database
        insertedReferenceCatalog = referenceCatalogRepository.saveAndFlush(referenceCatalog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceCatalog using partial update
        ReferenceCatalog partialUpdatedReferenceCatalog = new ReferenceCatalog();
        partialUpdatedReferenceCatalog.setId(referenceCatalog.getId());

        partialUpdatedReferenceCatalog
            .projectType(UPDATED_PROJECT_TYPE)
            .version(UPDATED_VERSION)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .published(UPDATED_PUBLISHED)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .diffSummary(UPDATED_DIFF_SUMMARY);

        restReferenceCatalogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferenceCatalog.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferenceCatalog))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceCatalog in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferenceCatalogUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedReferenceCatalog, referenceCatalog),
            getPersistedReferenceCatalog(referenceCatalog)
        );
    }

    @Test
    @Transactional
    void fullUpdateReferenceCatalogWithPatch() throws Exception {
        // Initialize the database
        insertedReferenceCatalog = referenceCatalogRepository.saveAndFlush(referenceCatalog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the referenceCatalog using partial update
        ReferenceCatalog partialUpdatedReferenceCatalog = new ReferenceCatalog();
        partialUpdatedReferenceCatalog.setId(referenceCatalog.getId());

        partialUpdatedReferenceCatalog
            .projectType(UPDATED_PROJECT_TYPE)
            .version(UPDATED_VERSION)
            .effectiveFrom(UPDATED_EFFECTIVE_FROM)
            .published(UPDATED_PUBLISHED)
            .authorId(UPDATED_AUTHOR_ID)
            .approverId(UPDATED_APPROVER_ID)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .diffSummary(UPDATED_DIFF_SUMMARY);

        restReferenceCatalogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedReferenceCatalog.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedReferenceCatalog))
            )
            .andExpect(status().isOk());

        // Validate the ReferenceCatalog in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertReferenceCatalogUpdatableFieldsEquals(
            partialUpdatedReferenceCatalog,
            getPersistedReferenceCatalog(partialUpdatedReferenceCatalog)
        );
    }

    @Test
    @Transactional
    void patchNonExistingReferenceCatalog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceCatalog.setId(UUID.randomUUID());

        // Create the ReferenceCatalog
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restReferenceCatalogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, referenceCatalogDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceCatalog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchReferenceCatalog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceCatalog.setId(UUID.randomUUID());

        // Create the ReferenceCatalog
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceCatalogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ReferenceCatalog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamReferenceCatalog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        referenceCatalog.setId(UUID.randomUUID());

        // Create the ReferenceCatalog
        ReferenceCatalogDTO referenceCatalogDTO = referenceCatalogMapper.toDto(referenceCatalog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restReferenceCatalogMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(referenceCatalogDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ReferenceCatalog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteReferenceCatalog() throws Exception {
        // Initialize the database
        insertedReferenceCatalog = referenceCatalogRepository.saveAndFlush(referenceCatalog);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the referenceCatalog
        restReferenceCatalogMockMvc
            .perform(delete(ENTITY_API_URL_ID, referenceCatalog.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return referenceCatalogRepository.count();
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

    protected ReferenceCatalog getPersistedReferenceCatalog(ReferenceCatalog referenceCatalog) {
        return referenceCatalogRepository.findById(referenceCatalog.getId()).orElseThrow();
    }

    protected void assertPersistedReferenceCatalogToMatchAllProperties(ReferenceCatalog expectedReferenceCatalog) {
        assertReferenceCatalogAllPropertiesEquals(expectedReferenceCatalog, getPersistedReferenceCatalog(expectedReferenceCatalog));
    }

    protected void assertPersistedReferenceCatalogToMatchUpdatableProperties(ReferenceCatalog expectedReferenceCatalog) {
        assertReferenceCatalogAllUpdatablePropertiesEquals(
            expectedReferenceCatalog,
            getPersistedReferenceCatalog(expectedReferenceCatalog)
        );
    }
}
