package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.ProjectionRuleAsserts.*;
import static cm.gimac.des.reporting.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.ProjectionRule;
import cm.gimac.des.reporting.domain.enumeration.FactDomain;
import cm.gimac.des.reporting.repository.ProjectionRuleRepository;
import cm.gimac.des.reporting.service.dto.ProjectionRuleDTO;
import cm.gimac.des.reporting.service.mapper.ProjectionRuleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link ProjectionRuleResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProjectionRuleResourceIT {

    private static final String DEFAULT_SUBJECT = "AAAAAAAAAA";
    private static final String UPDATED_SUBJECT = "BBBBBBBBBB";

    private static final FactDomain DEFAULT_DOMAIN = FactDomain.INCIDENT;
    private static final FactDomain UPDATED_DOMAIN = FactDomain.MONITORING;

    private static final String DEFAULT_MEASURE_MAPPING = "AAAAAAAAAA";
    private static final String UPDATED_MEASURE_MAPPING = "BBBBBBBBBB";

    private static final String DEFAULT_DIMENSION_MAPPING = "AAAAAAAAAA";
    private static final String UPDATED_DIMENSION_MAPPING = "BBBBBBBBBB";

    private static final String DEFAULT_INDICATOR_CODES = "AAAAAAAAAA";
    private static final String UPDATED_INDICATOR_CODES = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/projection-rules";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ProjectionRuleRepository projectionRuleRepository;

    @Autowired
    private ProjectionRuleMapper projectionRuleMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProjectionRuleMockMvc;

    private ProjectionRule projectionRule;

    private ProjectionRule insertedProjectionRule;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProjectionRule createEntity() {
        return new ProjectionRule()
            .subject(DEFAULT_SUBJECT)
            .domain(DEFAULT_DOMAIN)
            .measureMapping(DEFAULT_MEASURE_MAPPING)
            .dimensionMapping(DEFAULT_DIMENSION_MAPPING)
            .indicatorCodes(DEFAULT_INDICATOR_CODES)
            .active(DEFAULT_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProjectionRule createUpdatedEntity() {
        return new ProjectionRule()
            .subject(UPDATED_SUBJECT)
            .domain(UPDATED_DOMAIN)
            .measureMapping(UPDATED_MEASURE_MAPPING)
            .dimensionMapping(UPDATED_DIMENSION_MAPPING)
            .indicatorCodes(UPDATED_INDICATOR_CODES)
            .active(UPDATED_ACTIVE);
    }

    @BeforeEach
    void initTest() {
        projectionRule = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedProjectionRule != null) {
            projectionRuleRepository.delete(insertedProjectionRule);
            insertedProjectionRule = null;
        }
    }

    @Test
    @Transactional
    void createProjectionRule() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ProjectionRule
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);
        var returnedProjectionRuleDTO = om.readValue(
            restProjectionRuleMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(projectionRuleDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ProjectionRuleDTO.class
        );

        // Validate the ProjectionRule in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedProjectionRule = projectionRuleMapper.toEntity(returnedProjectionRuleDTO);
        assertProjectionRuleUpdatableFieldsEquals(returnedProjectionRule, getPersistedProjectionRule(returnedProjectionRule));

        insertedProjectionRule = returnedProjectionRule;
    }

    @Test
    @Transactional
    void createProjectionRuleWithExistingId() throws Exception {
        // Create the ProjectionRule with an existing ID
        insertedProjectionRule = projectionRuleRepository.saveAndFlush(projectionRule);
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restProjectionRuleMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProjectionRule in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSubjectIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        projectionRule.setSubject(null);

        // Create the ProjectionRule, which fails.
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        restProjectionRuleMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDomainIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        projectionRule.setDomain(null);

        // Create the ProjectionRule, which fails.
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        restProjectionRuleMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        projectionRule.setActive(null);

        // Create the ProjectionRule, which fails.
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        restProjectionRuleMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllProjectionRules() throws Exception {
        // Initialize the database
        insertedProjectionRule = projectionRuleRepository.saveAndFlush(projectionRule);

        // Get all the projectionRuleList
        restProjectionRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(projectionRule.getId().toString())))
            .andExpect(jsonPath("$.[*].subject").value(hasItem(DEFAULT_SUBJECT)))
            .andExpect(jsonPath("$.[*].domain").value(hasItem(DEFAULT_DOMAIN.toString())))
            .andExpect(jsonPath("$.[*].measureMapping").value(hasItem(DEFAULT_MEASURE_MAPPING)))
            .andExpect(jsonPath("$.[*].dimensionMapping").value(hasItem(DEFAULT_DIMENSION_MAPPING)))
            .andExpect(jsonPath("$.[*].indicatorCodes").value(hasItem(DEFAULT_INDICATOR_CODES)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)));
    }

    @Test
    @Transactional
    void getProjectionRule() throws Exception {
        // Initialize the database
        insertedProjectionRule = projectionRuleRepository.saveAndFlush(projectionRule);

        // Get the projectionRule
        restProjectionRuleMockMvc
            .perform(get(ENTITY_API_URL_ID, projectionRule.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(projectionRule.getId().toString()))
            .andExpect(jsonPath("$.subject").value(DEFAULT_SUBJECT))
            .andExpect(jsonPath("$.domain").value(DEFAULT_DOMAIN.toString()))
            .andExpect(jsonPath("$.measureMapping").value(DEFAULT_MEASURE_MAPPING))
            .andExpect(jsonPath("$.dimensionMapping").value(DEFAULT_DIMENSION_MAPPING))
            .andExpect(jsonPath("$.indicatorCodes").value(DEFAULT_INDICATOR_CODES))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingProjectionRule() throws Exception {
        // Get the projectionRule
        restProjectionRuleMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingProjectionRule() throws Exception {
        // Initialize the database
        insertedProjectionRule = projectionRuleRepository.saveAndFlush(projectionRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the projectionRule
        ProjectionRule updatedProjectionRule = projectionRuleRepository.findById(projectionRule.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedProjectionRule are not directly saved in db
        em.detach(updatedProjectionRule);
        updatedProjectionRule
            .subject(UPDATED_SUBJECT)
            .domain(UPDATED_DOMAIN)
            .measureMapping(UPDATED_MEASURE_MAPPING)
            .dimensionMapping(UPDATED_DIMENSION_MAPPING)
            .indicatorCodes(UPDATED_INDICATOR_CODES)
            .active(UPDATED_ACTIVE);
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(updatedProjectionRule);

        restProjectionRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, projectionRuleDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isOk());

        // Validate the ProjectionRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedProjectionRuleToMatchAllProperties(updatedProjectionRule);
    }

    @Test
    @Transactional
    void putNonExistingProjectionRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        projectionRule.setId(UUID.randomUUID());

        // Create the ProjectionRule
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProjectionRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, projectionRuleDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProjectionRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProjectionRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        projectionRule.setId(UUID.randomUUID());

        // Create the ProjectionRule
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProjectionRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProjectionRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProjectionRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        projectionRule.setId(UUID.randomUUID());

        // Create the ProjectionRule
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProjectionRuleMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ProjectionRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateProjectionRuleWithPatch() throws Exception {
        // Initialize the database
        insertedProjectionRule = projectionRuleRepository.saveAndFlush(projectionRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the projectionRule using partial update
        ProjectionRule partialUpdatedProjectionRule = new ProjectionRule();
        partialUpdatedProjectionRule.setId(projectionRule.getId());

        partialUpdatedProjectionRule.measureMapping(UPDATED_MEASURE_MAPPING).dimensionMapping(UPDATED_DIMENSION_MAPPING);

        restProjectionRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProjectionRule.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProjectionRule))
            )
            .andExpect(status().isOk());

        // Validate the ProjectionRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertProjectionRuleUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedProjectionRule, projectionRule),
            getPersistedProjectionRule(projectionRule)
        );
    }

    @Test
    @Transactional
    void fullUpdateProjectionRuleWithPatch() throws Exception {
        // Initialize the database
        insertedProjectionRule = projectionRuleRepository.saveAndFlush(projectionRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the projectionRule using partial update
        ProjectionRule partialUpdatedProjectionRule = new ProjectionRule();
        partialUpdatedProjectionRule.setId(projectionRule.getId());

        partialUpdatedProjectionRule
            .subject(UPDATED_SUBJECT)
            .domain(UPDATED_DOMAIN)
            .measureMapping(UPDATED_MEASURE_MAPPING)
            .dimensionMapping(UPDATED_DIMENSION_MAPPING)
            .indicatorCodes(UPDATED_INDICATOR_CODES)
            .active(UPDATED_ACTIVE);

        restProjectionRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProjectionRule.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProjectionRule))
            )
            .andExpect(status().isOk());

        // Validate the ProjectionRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertProjectionRuleUpdatableFieldsEquals(partialUpdatedProjectionRule, getPersistedProjectionRule(partialUpdatedProjectionRule));
    }

    @Test
    @Transactional
    void patchNonExistingProjectionRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        projectionRule.setId(UUID.randomUUID());

        // Create the ProjectionRule
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProjectionRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, projectionRuleDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProjectionRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchProjectionRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        projectionRule.setId(UUID.randomUUID());

        // Create the ProjectionRule
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProjectionRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProjectionRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamProjectionRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        projectionRule.setId(UUID.randomUUID());

        // Create the ProjectionRule
        ProjectionRuleDTO projectionRuleDTO = projectionRuleMapper.toDto(projectionRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProjectionRuleMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(projectionRuleDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ProjectionRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteProjectionRule() throws Exception {
        // Initialize the database
        insertedProjectionRule = projectionRuleRepository.saveAndFlush(projectionRule);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the projectionRule
        restProjectionRuleMockMvc
            .perform(delete(ENTITY_API_URL_ID, projectionRule.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return projectionRuleRepository.count();
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

    protected ProjectionRule getPersistedProjectionRule(ProjectionRule projectionRule) {
        return projectionRuleRepository.findById(projectionRule.getId()).orElseThrow();
    }

    protected void assertPersistedProjectionRuleToMatchAllProperties(ProjectionRule expectedProjectionRule) {
        assertProjectionRuleAllPropertiesEquals(expectedProjectionRule, getPersistedProjectionRule(expectedProjectionRule));
    }

    protected void assertPersistedProjectionRuleToMatchUpdatableProperties(ProjectionRule expectedProjectionRule) {
        assertProjectionRuleAllUpdatablePropertiesEquals(expectedProjectionRule, getPersistedProjectionRule(expectedProjectionRule));
    }
}
