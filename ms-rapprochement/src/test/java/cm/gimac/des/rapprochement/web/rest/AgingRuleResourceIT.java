package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.AgingRuleAsserts.*;
import static cm.gimac.des.rapprochement.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.AgingRule;
import cm.gimac.des.rapprochement.domain.enumeration.AgingUrgency;
import cm.gimac.des.rapprochement.repository.AgingRuleRepository;
import cm.gimac.des.rapprochement.service.dto.AgingRuleDTO;
import cm.gimac.des.rapprochement.service.mapper.AgingRuleMapper;
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
 * Integration tests for the {@link AgingRuleResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AgingRuleResourceIT {

    private static final Integer DEFAULT_THRESHOLD_DAYS = 1;
    private static final Integer UPDATED_THRESHOLD_DAYS = 2;

    private static final AgingUrgency DEFAULT_URGENCY = AgingUrgency.NORMALE;
    private static final AgingUrgency UPDATED_URGENCY = AgingUrgency.ELEVEE;

    private static final String DEFAULT_RECIPIENT_ROLE = "AAAAAAAAAA";
    private static final String UPDATED_RECIPIENT_ROLE = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACKNOWLEDGEMENT_REQUIRED = false;
    private static final Boolean UPDATED_ACKNOWLEDGEMENT_REQUIRED = true;

    private static final Boolean DEFAULT_TRIGGERED_BY_DUE_DATE = false;
    private static final Boolean UPDATED_TRIGGERED_BY_DUE_DATE = true;

    private static final String ENTITY_API_URL = "/api/aging-rules";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AgingRuleRepository agingRuleRepository;

    @Autowired
    private AgingRuleMapper agingRuleMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAgingRuleMockMvc;

    private AgingRule agingRule;

    private AgingRule insertedAgingRule;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AgingRule createEntity() {
        return new AgingRule()
            .thresholdDays(DEFAULT_THRESHOLD_DAYS)
            .urgency(DEFAULT_URGENCY)
            .recipientRole(DEFAULT_RECIPIENT_ROLE)
            .acknowledgementRequired(DEFAULT_ACKNOWLEDGEMENT_REQUIRED)
            .triggeredByDueDate(DEFAULT_TRIGGERED_BY_DUE_DATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AgingRule createUpdatedEntity() {
        return new AgingRule()
            .thresholdDays(UPDATED_THRESHOLD_DAYS)
            .urgency(UPDATED_URGENCY)
            .recipientRole(UPDATED_RECIPIENT_ROLE)
            .acknowledgementRequired(UPDATED_ACKNOWLEDGEMENT_REQUIRED)
            .triggeredByDueDate(UPDATED_TRIGGERED_BY_DUE_DATE);
    }

    @BeforeEach
    void initTest() {
        agingRule = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAgingRule != null) {
            agingRuleRepository.delete(insertedAgingRule);
            insertedAgingRule = null;
        }
    }

    @Test
    @Transactional
    void createAgingRule() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AgingRule
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);
        var returnedAgingRuleDTO = om.readValue(
            restAgingRuleMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(agingRuleDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AgingRuleDTO.class
        );

        // Validate the AgingRule in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAgingRule = agingRuleMapper.toEntity(returnedAgingRuleDTO);
        assertAgingRuleUpdatableFieldsEquals(returnedAgingRule, getPersistedAgingRule(returnedAgingRule));

        insertedAgingRule = returnedAgingRule;
    }

    @Test
    @Transactional
    void createAgingRuleWithExistingId() throws Exception {
        // Create the AgingRule with an existing ID
        insertedAgingRule = agingRuleRepository.saveAndFlush(agingRule);
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAgingRuleMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(agingRuleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the AgingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkThresholdDaysIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        agingRule.setThresholdDays(null);

        // Create the AgingRule, which fails.
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        restAgingRuleMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(agingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUrgencyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        agingRule.setUrgency(null);

        // Create the AgingRule, which fails.
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        restAgingRuleMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(agingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRecipientRoleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        agingRule.setRecipientRole(null);

        // Create the AgingRule, which fails.
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        restAgingRuleMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(agingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAcknowledgementRequiredIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        agingRule.setAcknowledgementRequired(null);

        // Create the AgingRule, which fails.
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        restAgingRuleMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(agingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTriggeredByDueDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        agingRule.setTriggeredByDueDate(null);

        // Create the AgingRule, which fails.
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        restAgingRuleMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(agingRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAgingRules() throws Exception {
        // Initialize the database
        insertedAgingRule = agingRuleRepository.saveAndFlush(agingRule);

        // Get all the agingRuleList
        restAgingRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(agingRule.getId().toString())))
            .andExpect(jsonPath("$.[*].thresholdDays").value(hasItem(DEFAULT_THRESHOLD_DAYS)))
            .andExpect(jsonPath("$.[*].urgency").value(hasItem(DEFAULT_URGENCY.toString())))
            .andExpect(jsonPath("$.[*].recipientRole").value(hasItem(DEFAULT_RECIPIENT_ROLE)))
            .andExpect(jsonPath("$.[*].acknowledgementRequired").value(hasItem(DEFAULT_ACKNOWLEDGEMENT_REQUIRED)))
            .andExpect(jsonPath("$.[*].triggeredByDueDate").value(hasItem(DEFAULT_TRIGGERED_BY_DUE_DATE)));
    }

    @Test
    @Transactional
    void getAgingRule() throws Exception {
        // Initialize the database
        insertedAgingRule = agingRuleRepository.saveAndFlush(agingRule);

        // Get the agingRule
        restAgingRuleMockMvc
            .perform(get(ENTITY_API_URL_ID, agingRule.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(agingRule.getId().toString()))
            .andExpect(jsonPath("$.thresholdDays").value(DEFAULT_THRESHOLD_DAYS))
            .andExpect(jsonPath("$.urgency").value(DEFAULT_URGENCY.toString()))
            .andExpect(jsonPath("$.recipientRole").value(DEFAULT_RECIPIENT_ROLE))
            .andExpect(jsonPath("$.acknowledgementRequired").value(DEFAULT_ACKNOWLEDGEMENT_REQUIRED))
            .andExpect(jsonPath("$.triggeredByDueDate").value(DEFAULT_TRIGGERED_BY_DUE_DATE));
    }

    @Test
    @Transactional
    void getNonExistingAgingRule() throws Exception {
        // Get the agingRule
        restAgingRuleMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAgingRule() throws Exception {
        // Initialize the database
        insertedAgingRule = agingRuleRepository.saveAndFlush(agingRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the agingRule
        AgingRule updatedAgingRule = agingRuleRepository.findById(agingRule.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAgingRule are not directly saved in db
        em.detach(updatedAgingRule);
        updatedAgingRule
            .thresholdDays(UPDATED_THRESHOLD_DAYS)
            .urgency(UPDATED_URGENCY)
            .recipientRole(UPDATED_RECIPIENT_ROLE)
            .acknowledgementRequired(UPDATED_ACKNOWLEDGEMENT_REQUIRED)
            .triggeredByDueDate(UPDATED_TRIGGERED_BY_DUE_DATE);
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(updatedAgingRule);

        restAgingRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, agingRuleDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(agingRuleDTO))
            )
            .andExpect(status().isOk());

        // Validate the AgingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAgingRuleToMatchAllProperties(updatedAgingRule);
    }

    @Test
    @Transactional
    void putNonExistingAgingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        agingRule.setId(UUID.randomUUID());

        // Create the AgingRule
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAgingRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, agingRuleDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(agingRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AgingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAgingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        agingRule.setId(UUID.randomUUID());

        // Create the AgingRule
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAgingRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(agingRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AgingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAgingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        agingRule.setId(UUID.randomUUID());

        // Create the AgingRule
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAgingRuleMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(agingRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AgingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAgingRuleWithPatch() throws Exception {
        // Initialize the database
        insertedAgingRule = agingRuleRepository.saveAndFlush(agingRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the agingRule using partial update
        AgingRule partialUpdatedAgingRule = new AgingRule();
        partialUpdatedAgingRule.setId(agingRule.getId());

        partialUpdatedAgingRule
            .thresholdDays(UPDATED_THRESHOLD_DAYS)
            .urgency(UPDATED_URGENCY)
            .recipientRole(UPDATED_RECIPIENT_ROLE)
            .acknowledgementRequired(UPDATED_ACKNOWLEDGEMENT_REQUIRED)
            .triggeredByDueDate(UPDATED_TRIGGERED_BY_DUE_DATE);

        restAgingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAgingRule.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAgingRule))
            )
            .andExpect(status().isOk());

        // Validate the AgingRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAgingRuleUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAgingRule, agingRule),
            getPersistedAgingRule(agingRule)
        );
    }

    @Test
    @Transactional
    void fullUpdateAgingRuleWithPatch() throws Exception {
        // Initialize the database
        insertedAgingRule = agingRuleRepository.saveAndFlush(agingRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the agingRule using partial update
        AgingRule partialUpdatedAgingRule = new AgingRule();
        partialUpdatedAgingRule.setId(agingRule.getId());

        partialUpdatedAgingRule
            .thresholdDays(UPDATED_THRESHOLD_DAYS)
            .urgency(UPDATED_URGENCY)
            .recipientRole(UPDATED_RECIPIENT_ROLE)
            .acknowledgementRequired(UPDATED_ACKNOWLEDGEMENT_REQUIRED)
            .triggeredByDueDate(UPDATED_TRIGGERED_BY_DUE_DATE);

        restAgingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAgingRule.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAgingRule))
            )
            .andExpect(status().isOk());

        // Validate the AgingRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAgingRuleUpdatableFieldsEquals(partialUpdatedAgingRule, getPersistedAgingRule(partialUpdatedAgingRule));
    }

    @Test
    @Transactional
    void patchNonExistingAgingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        agingRule.setId(UUID.randomUUID());

        // Create the AgingRule
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAgingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, agingRuleDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(agingRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AgingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAgingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        agingRule.setId(UUID.randomUUID());

        // Create the AgingRule
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAgingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(agingRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AgingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAgingRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        agingRule.setId(UUID.randomUUID());

        // Create the AgingRule
        AgingRuleDTO agingRuleDTO = agingRuleMapper.toDto(agingRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAgingRuleMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(agingRuleDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AgingRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAgingRule() throws Exception {
        // Initialize the database
        insertedAgingRule = agingRuleRepository.saveAndFlush(agingRule);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the agingRule
        restAgingRuleMockMvc
            .perform(delete(ENTITY_API_URL_ID, agingRule.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return agingRuleRepository.count();
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

    protected AgingRule getPersistedAgingRule(AgingRule agingRule) {
        return agingRuleRepository.findById(agingRule.getId()).orElseThrow();
    }

    protected void assertPersistedAgingRuleToMatchAllProperties(AgingRule expectedAgingRule) {
        assertAgingRuleAllPropertiesEquals(expectedAgingRule, getPersistedAgingRule(expectedAgingRule));
    }

    protected void assertPersistedAgingRuleToMatchUpdatableProperties(AgingRule expectedAgingRule) {
        assertAgingRuleAllUpdatablePropertiesEquals(expectedAgingRule, getPersistedAgingRule(expectedAgingRule));
    }
}
