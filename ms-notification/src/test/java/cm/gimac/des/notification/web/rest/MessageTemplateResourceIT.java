package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.MessageTemplateAsserts.*;
import static cm.gimac.des.notification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.MessageTemplate;
import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.repository.MessageTemplateRepository;
import cm.gimac.des.notification.service.dto.MessageTemplateDTO;
import cm.gimac.des.notification.service.mapper.MessageTemplateMapper;
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
 * Integration tests for the {@link MessageTemplateResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class MessageTemplateResourceIT {

    private static final String DEFAULT_NOTIFICATION_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_NOTIFICATION_TYPE = "BBBBBBBBBB";

    private static final Channel DEFAULT_CHANNEL = Channel.PLATEFORME;
    private static final Channel UPDATED_CHANNEL = Channel.POUSSE;

    private static final String DEFAULT_LANGUAGE = "fr";
    private static final String UPDATED_LANGUAGE = "en";

    private static final String DEFAULT_SUBJECT_PATTERN = "AAAAAAAAAA";
    private static final String UPDATED_SUBJECT_PATTERN = "BBBBBBBBBB";

    private static final String DEFAULT_BODY_PATTERN = "AAAAAAAAAA";
    private static final String UPDATED_BODY_PATTERN = "BBBBBBBBBB";

    private static final String DEFAULT_VARIABLES = "AAAAAAAAAA";
    private static final String UPDATED_VARIABLES = "BBBBBBBBBB";

    private static final Boolean DEFAULT_MINIMAL = false;
    private static final Boolean UPDATED_MINIMAL = true;

    private static final String ENTITY_API_URL = "/api/message-templates";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MessageTemplateRepository messageTemplateRepository;

    @Autowired
    private MessageTemplateMapper messageTemplateMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMessageTemplateMockMvc;

    private MessageTemplate messageTemplate;

    private MessageTemplate insertedMessageTemplate;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MessageTemplate createEntity() {
        return new MessageTemplate()
            .notificationType(DEFAULT_NOTIFICATION_TYPE)
            .channel(DEFAULT_CHANNEL)
            .language(DEFAULT_LANGUAGE)
            .subjectPattern(DEFAULT_SUBJECT_PATTERN)
            .bodyPattern(DEFAULT_BODY_PATTERN)
            .variables(DEFAULT_VARIABLES)
            .minimal(DEFAULT_MINIMAL);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MessageTemplate createUpdatedEntity() {
        return new MessageTemplate()
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .channel(UPDATED_CHANNEL)
            .language(UPDATED_LANGUAGE)
            .subjectPattern(UPDATED_SUBJECT_PATTERN)
            .bodyPattern(UPDATED_BODY_PATTERN)
            .variables(UPDATED_VARIABLES)
            .minimal(UPDATED_MINIMAL);
    }

    @BeforeEach
    void initTest() {
        messageTemplate = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedMessageTemplate != null) {
            messageTemplateRepository.delete(insertedMessageTemplate);
            insertedMessageTemplate = null;
        }
    }

    @Test
    @Transactional
    void createMessageTemplate() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the MessageTemplate
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);
        var returnedMessageTemplateDTO = om.readValue(
            restMessageTemplateMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(messageTemplateDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MessageTemplateDTO.class
        );

        // Validate the MessageTemplate in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMessageTemplate = messageTemplateMapper.toEntity(returnedMessageTemplateDTO);
        assertMessageTemplateUpdatableFieldsEquals(returnedMessageTemplate, getPersistedMessageTemplate(returnedMessageTemplate));

        insertedMessageTemplate = returnedMessageTemplate;
    }

    @Test
    @Transactional
    void createMessageTemplateWithExistingId() throws Exception {
        // Create the MessageTemplate with an existing ID
        insertedMessageTemplate = messageTemplateRepository.saveAndFlush(messageTemplate);
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMessageTemplateMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MessageTemplate in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNotificationTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        messageTemplate.setNotificationType(null);

        // Create the MessageTemplate, which fails.
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        restMessageTemplateMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkChannelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        messageTemplate.setChannel(null);

        // Create the MessageTemplate, which fails.
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        restMessageTemplateMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLanguageIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        messageTemplate.setLanguage(null);

        // Create the MessageTemplate, which fails.
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        restMessageTemplateMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSubjectPatternIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        messageTemplate.setSubjectPattern(null);

        // Create the MessageTemplate, which fails.
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        restMessageTemplateMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMinimalIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        messageTemplate.setMinimal(null);

        // Create the MessageTemplate, which fails.
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        restMessageTemplateMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMessageTemplates() throws Exception {
        // Initialize the database
        insertedMessageTemplate = messageTemplateRepository.saveAndFlush(messageTemplate);

        // Get all the messageTemplateList
        restMessageTemplateMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(messageTemplate.getId().toString())))
            .andExpect(jsonPath("$.[*].notificationType").value(hasItem(DEFAULT_NOTIFICATION_TYPE)))
            .andExpect(jsonPath("$.[*].channel").value(hasItem(DEFAULT_CHANNEL.toString())))
            .andExpect(jsonPath("$.[*].language").value(hasItem(DEFAULT_LANGUAGE)))
            .andExpect(jsonPath("$.[*].subjectPattern").value(hasItem(DEFAULT_SUBJECT_PATTERN)))
            .andExpect(jsonPath("$.[*].bodyPattern").value(hasItem(DEFAULT_BODY_PATTERN)))
            .andExpect(jsonPath("$.[*].variables").value(hasItem(DEFAULT_VARIABLES)))
            .andExpect(jsonPath("$.[*].minimal").value(hasItem(DEFAULT_MINIMAL)));
    }

    @Test
    @Transactional
    void getMessageTemplate() throws Exception {
        // Initialize the database
        insertedMessageTemplate = messageTemplateRepository.saveAndFlush(messageTemplate);

        // Get the messageTemplate
        restMessageTemplateMockMvc
            .perform(get(ENTITY_API_URL_ID, messageTemplate.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(messageTemplate.getId().toString()))
            .andExpect(jsonPath("$.notificationType").value(DEFAULT_NOTIFICATION_TYPE))
            .andExpect(jsonPath("$.channel").value(DEFAULT_CHANNEL.toString()))
            .andExpect(jsonPath("$.language").value(DEFAULT_LANGUAGE))
            .andExpect(jsonPath("$.subjectPattern").value(DEFAULT_SUBJECT_PATTERN))
            .andExpect(jsonPath("$.bodyPattern").value(DEFAULT_BODY_PATTERN))
            .andExpect(jsonPath("$.variables").value(DEFAULT_VARIABLES))
            .andExpect(jsonPath("$.minimal").value(DEFAULT_MINIMAL));
    }

    @Test
    @Transactional
    void getNonExistingMessageTemplate() throws Exception {
        // Get the messageTemplate
        restMessageTemplateMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMessageTemplate() throws Exception {
        // Initialize the database
        insertedMessageTemplate = messageTemplateRepository.saveAndFlush(messageTemplate);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the messageTemplate
        MessageTemplate updatedMessageTemplate = messageTemplateRepository.findById(messageTemplate.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMessageTemplate are not directly saved in db
        em.detach(updatedMessageTemplate);
        updatedMessageTemplate
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .channel(UPDATED_CHANNEL)
            .language(UPDATED_LANGUAGE)
            .subjectPattern(UPDATED_SUBJECT_PATTERN)
            .bodyPattern(UPDATED_BODY_PATTERN)
            .variables(UPDATED_VARIABLES)
            .minimal(UPDATED_MINIMAL);
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(updatedMessageTemplate);

        restMessageTemplateMockMvc
            .perform(
                put(ENTITY_API_URL_ID, messageTemplateDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isOk());

        // Validate the MessageTemplate in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMessageTemplateToMatchAllProperties(updatedMessageTemplate);
    }

    @Test
    @Transactional
    void putNonExistingMessageTemplate() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        messageTemplate.setId(UUID.randomUUID());

        // Create the MessageTemplate
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMessageTemplateMockMvc
            .perform(
                put(ENTITY_API_URL_ID, messageTemplateDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MessageTemplate in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMessageTemplate() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        messageTemplate.setId(UUID.randomUUID());

        // Create the MessageTemplate
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMessageTemplateMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MessageTemplate in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMessageTemplate() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        messageTemplate.setId(UUID.randomUUID());

        // Create the MessageTemplate
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMessageTemplateMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the MessageTemplate in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMessageTemplateWithPatch() throws Exception {
        // Initialize the database
        insertedMessageTemplate = messageTemplateRepository.saveAndFlush(messageTemplate);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the messageTemplate using partial update
        MessageTemplate partialUpdatedMessageTemplate = new MessageTemplate();
        partialUpdatedMessageTemplate.setId(messageTemplate.getId());

        partialUpdatedMessageTemplate
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .language(UPDATED_LANGUAGE)
            .bodyPattern(UPDATED_BODY_PATTERN)
            .variables(UPDATED_VARIABLES);

        restMessageTemplateMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMessageTemplate.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMessageTemplate))
            )
            .andExpect(status().isOk());

        // Validate the MessageTemplate in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMessageTemplateUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedMessageTemplate, messageTemplate),
            getPersistedMessageTemplate(messageTemplate)
        );
    }

    @Test
    @Transactional
    void fullUpdateMessageTemplateWithPatch() throws Exception {
        // Initialize the database
        insertedMessageTemplate = messageTemplateRepository.saveAndFlush(messageTemplate);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the messageTemplate using partial update
        MessageTemplate partialUpdatedMessageTemplate = new MessageTemplate();
        partialUpdatedMessageTemplate.setId(messageTemplate.getId());

        partialUpdatedMessageTemplate
            .notificationType(UPDATED_NOTIFICATION_TYPE)
            .channel(UPDATED_CHANNEL)
            .language(UPDATED_LANGUAGE)
            .subjectPattern(UPDATED_SUBJECT_PATTERN)
            .bodyPattern(UPDATED_BODY_PATTERN)
            .variables(UPDATED_VARIABLES)
            .minimal(UPDATED_MINIMAL);

        restMessageTemplateMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMessageTemplate.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMessageTemplate))
            )
            .andExpect(status().isOk());

        // Validate the MessageTemplate in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMessageTemplateUpdatableFieldsEquals(
            partialUpdatedMessageTemplate,
            getPersistedMessageTemplate(partialUpdatedMessageTemplate)
        );
    }

    @Test
    @Transactional
    void patchNonExistingMessageTemplate() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        messageTemplate.setId(UUID.randomUUID());

        // Create the MessageTemplate
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMessageTemplateMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, messageTemplateDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MessageTemplate in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMessageTemplate() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        messageTemplate.setId(UUID.randomUUID());

        // Create the MessageTemplate
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMessageTemplateMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MessageTemplate in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMessageTemplate() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        messageTemplate.setId(UUID.randomUUID());

        // Create the MessageTemplate
        MessageTemplateDTO messageTemplateDTO = messageTemplateMapper.toDto(messageTemplate);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMessageTemplateMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(messageTemplateDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the MessageTemplate in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMessageTemplate() throws Exception {
        // Initialize the database
        insertedMessageTemplate = messageTemplateRepository.saveAndFlush(messageTemplate);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the messageTemplate
        restMessageTemplateMockMvc
            .perform(delete(ENTITY_API_URL_ID, messageTemplate.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return messageTemplateRepository.count();
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

    protected MessageTemplate getPersistedMessageTemplate(MessageTemplate messageTemplate) {
        return messageTemplateRepository.findById(messageTemplate.getId()).orElseThrow();
    }

    protected void assertPersistedMessageTemplateToMatchAllProperties(MessageTemplate expectedMessageTemplate) {
        assertMessageTemplateAllPropertiesEquals(expectedMessageTemplate, getPersistedMessageTemplate(expectedMessageTemplate));
    }

    protected void assertPersistedMessageTemplateToMatchUpdatableProperties(MessageTemplate expectedMessageTemplate) {
        assertMessageTemplateAllUpdatablePropertiesEquals(expectedMessageTemplate, getPersistedMessageTemplate(expectedMessageTemplate));
    }
}
