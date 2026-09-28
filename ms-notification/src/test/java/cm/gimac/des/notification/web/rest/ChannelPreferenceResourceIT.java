package cm.gimac.des.notification.web.rest;

import static cm.gimac.des.notification.domain.ChannelPreferenceAsserts.*;
import static cm.gimac.des.notification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.notification.IntegrationTest;
import cm.gimac.des.notification.domain.ChannelPreference;
import cm.gimac.des.notification.domain.enumeration.Channel;
import cm.gimac.des.notification.domain.enumeration.UrgencyLevel;
import cm.gimac.des.notification.repository.ChannelPreferenceRepository;
import cm.gimac.des.notification.service.dto.ChannelPreferenceDTO;
import cm.gimac.des.notification.service.mapper.ChannelPreferenceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
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
 * Integration tests for the {@link ChannelPreferenceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ChannelPreferenceResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final Channel DEFAULT_CHANNEL = Channel.PLATEFORME;
    private static final Channel UPDATED_CHANNEL = Channel.POUSSE;

    private static final UrgencyLevel DEFAULT_MINIMUM_URGENCY = UrgencyLevel.INFORMATION;
    private static final UrgencyLevel UPDATED_MINIMUM_URGENCY = UrgencyLevel.AVERTISSEMENT;

    private static final LocalTime DEFAULT_WINDOW_START = LocalTime.NOON;
    private static final LocalTime UPDATED_WINDOW_START = LocalTime.MAX.withNano(0);

    private static final LocalTime DEFAULT_WINDOW_END = LocalTime.NOON;
    private static final LocalTime UPDATED_WINDOW_END = LocalTime.MAX.withNano(0);

    private static final Boolean DEFAULT_ENABLED = false;
    private static final Boolean UPDATED_ENABLED = true;

    private static final String ENTITY_API_URL = "/api/channel-preferences";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ChannelPreferenceRepository channelPreferenceRepository;

    @Autowired
    private ChannelPreferenceMapper channelPreferenceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restChannelPreferenceMockMvc;

    private ChannelPreference channelPreference;

    private ChannelPreference insertedChannelPreference;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChannelPreference createEntity() {
        return new ChannelPreference()
            .userId(DEFAULT_USER_ID)
            .channel(DEFAULT_CHANNEL)
            .minimumUrgency(DEFAULT_MINIMUM_URGENCY)
            .windowStart(DEFAULT_WINDOW_START)
            .windowEnd(DEFAULT_WINDOW_END)
            .enabled(DEFAULT_ENABLED);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ChannelPreference createUpdatedEntity() {
        return new ChannelPreference()
            .userId(UPDATED_USER_ID)
            .channel(UPDATED_CHANNEL)
            .minimumUrgency(UPDATED_MINIMUM_URGENCY)
            .windowStart(UPDATED_WINDOW_START)
            .windowEnd(UPDATED_WINDOW_END)
            .enabled(UPDATED_ENABLED);
    }

    @BeforeEach
    void initTest() {
        channelPreference = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedChannelPreference != null) {
            channelPreferenceRepository.delete(insertedChannelPreference);
            insertedChannelPreference = null;
        }
    }

    @Test
    @Transactional
    void createChannelPreference() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ChannelPreference
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);
        var returnedChannelPreferenceDTO = om.readValue(
            restChannelPreferenceMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(channelPreferenceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ChannelPreferenceDTO.class
        );

        // Validate the ChannelPreference in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedChannelPreference = channelPreferenceMapper.toEntity(returnedChannelPreferenceDTO);
        assertChannelPreferenceUpdatableFieldsEquals(returnedChannelPreference, getPersistedChannelPreference(returnedChannelPreference));

        insertedChannelPreference = returnedChannelPreference;
    }

    @Test
    @Transactional
    void createChannelPreferenceWithExistingId() throws Exception {
        // Create the ChannelPreference with an existing ID
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restChannelPreferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChannelPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUserIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        channelPreference.setUserId(null);

        // Create the ChannelPreference, which fails.
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        restChannelPreferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkChannelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        channelPreference.setChannel(null);

        // Create the ChannelPreference, which fails.
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        restChannelPreferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMinimumUrgencyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        channelPreference.setMinimumUrgency(null);

        // Create the ChannelPreference, which fails.
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        restChannelPreferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEnabledIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        channelPreference.setEnabled(null);

        // Create the ChannelPreference, which fails.
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        restChannelPreferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllChannelPreferences() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList
        restChannelPreferenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(channelPreference.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].channel").value(hasItem(DEFAULT_CHANNEL.toString())))
            .andExpect(jsonPath("$.[*].minimumUrgency").value(hasItem(DEFAULT_MINIMUM_URGENCY.toString())))
            .andExpect(jsonPath("$.[*].windowStart").value(hasItem(DEFAULT_WINDOW_START.toString())))
            .andExpect(jsonPath("$.[*].windowEnd").value(hasItem(DEFAULT_WINDOW_END.toString())))
            .andExpect(jsonPath("$.[*].enabled").value(hasItem(DEFAULT_ENABLED)));
    }

    @Test
    @Transactional
    void getChannelPreference() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get the channelPreference
        restChannelPreferenceMockMvc
            .perform(get(ENTITY_API_URL_ID, channelPreference.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(channelPreference.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.channel").value(DEFAULT_CHANNEL.toString()))
            .andExpect(jsonPath("$.minimumUrgency").value(DEFAULT_MINIMUM_URGENCY.toString()))
            .andExpect(jsonPath("$.windowStart").value(DEFAULT_WINDOW_START.toString()))
            .andExpect(jsonPath("$.windowEnd").value(DEFAULT_WINDOW_END.toString()))
            .andExpect(jsonPath("$.enabled").value(DEFAULT_ENABLED));
    }

    @Test
    @Transactional
    void getChannelPreferencesByIdFiltering() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        UUID id = channelPreference.getId();

        defaultChannelPreferenceFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where userId equals to
        defaultChannelPreferenceFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where userId in
        defaultChannelPreferenceFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where userId is not null
        defaultChannelPreferenceFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByChannelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where channel equals to
        defaultChannelPreferenceFiltering("channel.equals=" + DEFAULT_CHANNEL, "channel.equals=" + UPDATED_CHANNEL);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByChannelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where channel in
        defaultChannelPreferenceFiltering("channel.in=" + DEFAULT_CHANNEL + "," + UPDATED_CHANNEL, "channel.in=" + UPDATED_CHANNEL);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByChannelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where channel is not null
        defaultChannelPreferenceFiltering("channel.specified=true", "channel.specified=false");
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByMinimumUrgencyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where minimumUrgency equals to
        defaultChannelPreferenceFiltering(
            "minimumUrgency.equals=" + DEFAULT_MINIMUM_URGENCY,
            "minimumUrgency.equals=" + UPDATED_MINIMUM_URGENCY
        );
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByMinimumUrgencyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where minimumUrgency in
        defaultChannelPreferenceFiltering(
            "minimumUrgency.in=" + DEFAULT_MINIMUM_URGENCY + "," + UPDATED_MINIMUM_URGENCY,
            "minimumUrgency.in=" + UPDATED_MINIMUM_URGENCY
        );
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByMinimumUrgencyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where minimumUrgency is not null
        defaultChannelPreferenceFiltering("minimumUrgency.specified=true", "minimumUrgency.specified=false");
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByWindowStartIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where windowStart equals to
        defaultChannelPreferenceFiltering("windowStart.equals=" + DEFAULT_WINDOW_START, "windowStart.equals=" + UPDATED_WINDOW_START);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByWindowStartIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where windowStart in
        defaultChannelPreferenceFiltering(
            "windowStart.in=" + DEFAULT_WINDOW_START + "," + UPDATED_WINDOW_START,
            "windowStart.in=" + UPDATED_WINDOW_START
        );
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByWindowStartIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where windowStart is not null
        defaultChannelPreferenceFiltering("windowStart.specified=true", "windowStart.specified=false");
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByWindowEndIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where windowEnd equals to
        defaultChannelPreferenceFiltering("windowEnd.equals=" + DEFAULT_WINDOW_END, "windowEnd.equals=" + UPDATED_WINDOW_END);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByWindowEndIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where windowEnd in
        defaultChannelPreferenceFiltering(
            "windowEnd.in=" + DEFAULT_WINDOW_END + "," + UPDATED_WINDOW_END,
            "windowEnd.in=" + UPDATED_WINDOW_END
        );
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByWindowEndIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where windowEnd is not null
        defaultChannelPreferenceFiltering("windowEnd.specified=true", "windowEnd.specified=false");
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByEnabledIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where enabled equals to
        defaultChannelPreferenceFiltering("enabled.equals=" + DEFAULT_ENABLED, "enabled.equals=" + UPDATED_ENABLED);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByEnabledIsInShouldWork() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where enabled in
        defaultChannelPreferenceFiltering("enabled.in=" + DEFAULT_ENABLED + "," + UPDATED_ENABLED, "enabled.in=" + UPDATED_ENABLED);
    }

    @Test
    @Transactional
    void getAllChannelPreferencesByEnabledIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        // Get all the channelPreferenceList where enabled is not null
        defaultChannelPreferenceFiltering("enabled.specified=true", "enabled.specified=false");
    }

    private void defaultChannelPreferenceFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultChannelPreferenceShouldBeFound(shouldBeFound);
        defaultChannelPreferenceShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultChannelPreferenceShouldBeFound(String filter) throws Exception {
        restChannelPreferenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(channelPreference.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].channel").value(hasItem(DEFAULT_CHANNEL.toString())))
            .andExpect(jsonPath("$.[*].minimumUrgency").value(hasItem(DEFAULT_MINIMUM_URGENCY.toString())))
            .andExpect(jsonPath("$.[*].windowStart").value(hasItem(DEFAULT_WINDOW_START.toString())))
            .andExpect(jsonPath("$.[*].windowEnd").value(hasItem(DEFAULT_WINDOW_END.toString())))
            .andExpect(jsonPath("$.[*].enabled").value(hasItem(DEFAULT_ENABLED)));

        // Check, that the count call also returns 1
        restChannelPreferenceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultChannelPreferenceShouldNotBeFound(String filter) throws Exception {
        restChannelPreferenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restChannelPreferenceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingChannelPreference() throws Exception {
        // Get the channelPreference
        restChannelPreferenceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingChannelPreference() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the channelPreference
        ChannelPreference updatedChannelPreference = channelPreferenceRepository.findById(channelPreference.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedChannelPreference are not directly saved in db
        em.detach(updatedChannelPreference);
        updatedChannelPreference
            .userId(UPDATED_USER_ID)
            .channel(UPDATED_CHANNEL)
            .minimumUrgency(UPDATED_MINIMUM_URGENCY)
            .windowStart(UPDATED_WINDOW_START)
            .windowEnd(UPDATED_WINDOW_END)
            .enabled(UPDATED_ENABLED);
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(updatedChannelPreference);

        restChannelPreferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, channelPreferenceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isOk());

        // Validate the ChannelPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedChannelPreferenceToMatchAllProperties(updatedChannelPreference);
    }

    @Test
    @Transactional
    void putNonExistingChannelPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        channelPreference.setId(UUID.randomUUID());

        // Create the ChannelPreference
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChannelPreferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, channelPreferenceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChannelPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchChannelPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        channelPreference.setId(UUID.randomUUID());

        // Create the ChannelPreference
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChannelPreferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChannelPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamChannelPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        channelPreference.setId(UUID.randomUUID());

        // Create the ChannelPreference
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChannelPreferenceMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChannelPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateChannelPreferenceWithPatch() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the channelPreference using partial update
        ChannelPreference partialUpdatedChannelPreference = new ChannelPreference();
        partialUpdatedChannelPreference.setId(channelPreference.getId());

        partialUpdatedChannelPreference.windowStart(UPDATED_WINDOW_START).enabled(UPDATED_ENABLED);

        restChannelPreferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChannelPreference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChannelPreference))
            )
            .andExpect(status().isOk());

        // Validate the ChannelPreference in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChannelPreferenceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedChannelPreference, channelPreference),
            getPersistedChannelPreference(channelPreference)
        );
    }

    @Test
    @Transactional
    void fullUpdateChannelPreferenceWithPatch() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the channelPreference using partial update
        ChannelPreference partialUpdatedChannelPreference = new ChannelPreference();
        partialUpdatedChannelPreference.setId(channelPreference.getId());

        partialUpdatedChannelPreference
            .userId(UPDATED_USER_ID)
            .channel(UPDATED_CHANNEL)
            .minimumUrgency(UPDATED_MINIMUM_URGENCY)
            .windowStart(UPDATED_WINDOW_START)
            .windowEnd(UPDATED_WINDOW_END)
            .enabled(UPDATED_ENABLED);

        restChannelPreferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedChannelPreference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedChannelPreference))
            )
            .andExpect(status().isOk());

        // Validate the ChannelPreference in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertChannelPreferenceUpdatableFieldsEquals(
            partialUpdatedChannelPreference,
            getPersistedChannelPreference(partialUpdatedChannelPreference)
        );
    }

    @Test
    @Transactional
    void patchNonExistingChannelPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        channelPreference.setId(UUID.randomUUID());

        // Create the ChannelPreference
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restChannelPreferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, channelPreferenceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChannelPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchChannelPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        channelPreference.setId(UUID.randomUUID());

        // Create the ChannelPreference
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChannelPreferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ChannelPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamChannelPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        channelPreference.setId(UUID.randomUUID());

        // Create the ChannelPreference
        ChannelPreferenceDTO channelPreferenceDTO = channelPreferenceMapper.toDto(channelPreference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restChannelPreferenceMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(channelPreferenceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ChannelPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteChannelPreference() throws Exception {
        // Initialize the database
        insertedChannelPreference = channelPreferenceRepository.saveAndFlush(channelPreference);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the channelPreference
        restChannelPreferenceMockMvc
            .perform(delete(ENTITY_API_URL_ID, channelPreference.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return channelPreferenceRepository.count();
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

    protected ChannelPreference getPersistedChannelPreference(ChannelPreference channelPreference) {
        return channelPreferenceRepository.findById(channelPreference.getId()).orElseThrow();
    }

    protected void assertPersistedChannelPreferenceToMatchAllProperties(ChannelPreference expectedChannelPreference) {
        assertChannelPreferenceAllPropertiesEquals(expectedChannelPreference, getPersistedChannelPreference(expectedChannelPreference));
    }

    protected void assertPersistedChannelPreferenceToMatchUpdatableProperties(ChannelPreference expectedChannelPreference) {
        assertChannelPreferenceAllUpdatablePropertiesEquals(
            expectedChannelPreference,
            getPersistedChannelPreference(expectedChannelPreference)
        );
    }
}
