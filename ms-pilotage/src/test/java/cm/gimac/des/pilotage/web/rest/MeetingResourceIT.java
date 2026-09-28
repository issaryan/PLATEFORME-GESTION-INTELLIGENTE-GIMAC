package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.MeetingAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Meeting;
import cm.gimac.des.pilotage.domain.enumeration.MeetingState;
import cm.gimac.des.pilotage.repository.MeetingRepository;
import cm.gimac.des.pilotage.service.dto.MeetingDTO;
import cm.gimac.des.pilotage.service.mapper.MeetingMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link MeetingResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class MeetingResourceIT {

    private static final String DEFAULT_INSTANCE = "AAAAAAAAAA";
    private static final String UPDATED_INSTANCE = "BBBBBBBBBB";

    private static final Instant DEFAULT_SCHEDULED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SCHEDULED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_HELD_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_HELD_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_AGENDA = "AAAAAAAAAA";
    private static final String UPDATED_AGENDA = "BBBBBBBBBB";

    private static final UUID DEFAULT_MINUTES_REF = UUID.randomUUID();
    private static final UUID UPDATED_MINUTES_REF = UUID.randomUUID();

    private static final String DEFAULT_MINUTES_DIGEST = "6c98f21748be11d0279dab63ba83540a41f5b47498a199c93ee572b316457c17";
    private static final String UPDATED_MINUTES_DIGEST = "316bcd75abee333811d69dd934d4b5ac93ce3f6a6048f55665a1abc15402a990";

    private static final MeetingState DEFAULT_STATE = MeetingState.PLANIFIEE;
    private static final MeetingState UPDATED_STATE = MeetingState.TENUE;

    private static final String ENTITY_API_URL = "/api/meetings";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingMapper meetingMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMeetingMockMvc;

    private Meeting meeting;

    private Meeting insertedMeeting;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Meeting createEntity() {
        return new Meeting()
            .instance(DEFAULT_INSTANCE)
            .scheduledAt(DEFAULT_SCHEDULED_AT)
            .heldAt(DEFAULT_HELD_AT)
            .agenda(DEFAULT_AGENDA)
            .minutesRef(DEFAULT_MINUTES_REF)
            .minutesDigest(DEFAULT_MINUTES_DIGEST)
            .state(DEFAULT_STATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Meeting createUpdatedEntity() {
        return new Meeting()
            .instance(UPDATED_INSTANCE)
            .scheduledAt(UPDATED_SCHEDULED_AT)
            .heldAt(UPDATED_HELD_AT)
            .agenda(UPDATED_AGENDA)
            .minutesRef(UPDATED_MINUTES_REF)
            .minutesDigest(UPDATED_MINUTES_DIGEST)
            .state(UPDATED_STATE);
    }

    @BeforeEach
    void initTest() {
        meeting = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedMeeting != null) {
            meetingRepository.delete(insertedMeeting);
            insertedMeeting = null;
        }
    }

    @Test
    @Transactional
    void createMeeting() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Meeting
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);
        var returnedMeetingDTO = om.readValue(
            restMeetingMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(meetingDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MeetingDTO.class
        );

        // Validate the Meeting in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMeeting = meetingMapper.toEntity(returnedMeetingDTO);
        assertMeetingUpdatableFieldsEquals(returnedMeeting, getPersistedMeeting(returnedMeeting));

        insertedMeeting = returnedMeeting;
    }

    @Test
    @Transactional
    void createMeetingWithExistingId() throws Exception {
        // Create the Meeting with an existing ID
        insertedMeeting = meetingRepository.saveAndFlush(meeting);
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMeetingMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(meetingDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Meeting in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkInstanceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        meeting.setInstance(null);

        // Create the Meeting, which fails.
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        restMeetingMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(meetingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkScheduledAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        meeting.setScheduledAt(null);

        // Create the Meeting, which fails.
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        restMeetingMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(meetingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        meeting.setState(null);

        // Create the Meeting, which fails.
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        restMeetingMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(meetingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMeetings() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList
        restMeetingMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(meeting.getId().toString())))
            .andExpect(jsonPath("$.[*].instance").value(hasItem(DEFAULT_INSTANCE)))
            .andExpect(jsonPath("$.[*].scheduledAt").value(hasItem(DEFAULT_SCHEDULED_AT.toString())))
            .andExpect(jsonPath("$.[*].heldAt").value(hasItem(DEFAULT_HELD_AT.toString())))
            .andExpect(jsonPath("$.[*].agenda").value(hasItem(DEFAULT_AGENDA)))
            .andExpect(jsonPath("$.[*].minutesRef").value(hasItem(DEFAULT_MINUTES_REF.toString())))
            .andExpect(jsonPath("$.[*].minutesDigest").value(hasItem(DEFAULT_MINUTES_DIGEST)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));
    }

    @Test
    @Transactional
    void getMeeting() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get the meeting
        restMeetingMockMvc
            .perform(get(ENTITY_API_URL_ID, meeting.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(meeting.getId().toString()))
            .andExpect(jsonPath("$.instance").value(DEFAULT_INSTANCE))
            .andExpect(jsonPath("$.scheduledAt").value(DEFAULT_SCHEDULED_AT.toString()))
            .andExpect(jsonPath("$.heldAt").value(DEFAULT_HELD_AT.toString()))
            .andExpect(jsonPath("$.agenda").value(DEFAULT_AGENDA))
            .andExpect(jsonPath("$.minutesRef").value(DEFAULT_MINUTES_REF.toString()))
            .andExpect(jsonPath("$.minutesDigest").value(DEFAULT_MINUTES_DIGEST))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()));
    }

    @Test
    @Transactional
    void getMeetingsByIdFiltering() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        UUID id = meeting.getId();

        defaultMeetingFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllMeetingsByInstanceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where instance equals to
        defaultMeetingFiltering("instance.equals=" + DEFAULT_INSTANCE, "instance.equals=" + UPDATED_INSTANCE);
    }

    @Test
    @Transactional
    void getAllMeetingsByInstanceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where instance in
        defaultMeetingFiltering("instance.in=" + DEFAULT_INSTANCE + "," + UPDATED_INSTANCE, "instance.in=" + UPDATED_INSTANCE);
    }

    @Test
    @Transactional
    void getAllMeetingsByInstanceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where instance is not null
        defaultMeetingFiltering("instance.specified=true", "instance.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingsByInstanceContainsSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where instance contains
        defaultMeetingFiltering("instance.contains=" + DEFAULT_INSTANCE, "instance.contains=" + UPDATED_INSTANCE);
    }

    @Test
    @Transactional
    void getAllMeetingsByInstanceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where instance does not contain
        defaultMeetingFiltering("instance.doesNotContain=" + UPDATED_INSTANCE, "instance.doesNotContain=" + DEFAULT_INSTANCE);
    }

    @Test
    @Transactional
    void getAllMeetingsByScheduledAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where scheduledAt equals to
        defaultMeetingFiltering("scheduledAt.equals=" + DEFAULT_SCHEDULED_AT, "scheduledAt.equals=" + UPDATED_SCHEDULED_AT);
    }

    @Test
    @Transactional
    void getAllMeetingsByScheduledAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where scheduledAt in
        defaultMeetingFiltering(
            "scheduledAt.in=" + DEFAULT_SCHEDULED_AT + "," + UPDATED_SCHEDULED_AT,
            "scheduledAt.in=" + UPDATED_SCHEDULED_AT
        );
    }

    @Test
    @Transactional
    void getAllMeetingsByScheduledAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where scheduledAt is not null
        defaultMeetingFiltering("scheduledAt.specified=true", "scheduledAt.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingsByHeldAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where heldAt equals to
        defaultMeetingFiltering("heldAt.equals=" + DEFAULT_HELD_AT, "heldAt.equals=" + UPDATED_HELD_AT);
    }

    @Test
    @Transactional
    void getAllMeetingsByHeldAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where heldAt in
        defaultMeetingFiltering("heldAt.in=" + DEFAULT_HELD_AT + "," + UPDATED_HELD_AT, "heldAt.in=" + UPDATED_HELD_AT);
    }

    @Test
    @Transactional
    void getAllMeetingsByHeldAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where heldAt is not null
        defaultMeetingFiltering("heldAt.specified=true", "heldAt.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingsByMinutesRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where minutesRef equals to
        defaultMeetingFiltering("minutesRef.equals=" + DEFAULT_MINUTES_REF, "minutesRef.equals=" + UPDATED_MINUTES_REF);
    }

    @Test
    @Transactional
    void getAllMeetingsByMinutesRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where minutesRef in
        defaultMeetingFiltering("minutesRef.in=" + DEFAULT_MINUTES_REF + "," + UPDATED_MINUTES_REF, "minutesRef.in=" + UPDATED_MINUTES_REF);
    }

    @Test
    @Transactional
    void getAllMeetingsByMinutesRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where minutesRef is not null
        defaultMeetingFiltering("minutesRef.specified=true", "minutesRef.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingsByMinutesDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where minutesDigest equals to
        defaultMeetingFiltering("minutesDigest.equals=" + DEFAULT_MINUTES_DIGEST, "minutesDigest.equals=" + UPDATED_MINUTES_DIGEST);
    }

    @Test
    @Transactional
    void getAllMeetingsByMinutesDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where minutesDigest in
        defaultMeetingFiltering(
            "minutesDigest.in=" + DEFAULT_MINUTES_DIGEST + "," + UPDATED_MINUTES_DIGEST,
            "minutesDigest.in=" + UPDATED_MINUTES_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllMeetingsByMinutesDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where minutesDigest is not null
        defaultMeetingFiltering("minutesDigest.specified=true", "minutesDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingsByMinutesDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where minutesDigest contains
        defaultMeetingFiltering("minutesDigest.contains=" + DEFAULT_MINUTES_DIGEST, "minutesDigest.contains=" + UPDATED_MINUTES_DIGEST);
    }

    @Test
    @Transactional
    void getAllMeetingsByMinutesDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where minutesDigest does not contain
        defaultMeetingFiltering(
            "minutesDigest.doesNotContain=" + UPDATED_MINUTES_DIGEST,
            "minutesDigest.doesNotContain=" + DEFAULT_MINUTES_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllMeetingsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where state equals to
        defaultMeetingFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllMeetingsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where state in
        defaultMeetingFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllMeetingsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        // Get all the meetingList where state is not null
        defaultMeetingFiltering("state.specified=true", "state.specified=false");
    }

    private void defaultMeetingFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultMeetingShouldBeFound(shouldBeFound);
        defaultMeetingShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultMeetingShouldBeFound(String filter) throws Exception {
        restMeetingMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(meeting.getId().toString())))
            .andExpect(jsonPath("$.[*].instance").value(hasItem(DEFAULT_INSTANCE)))
            .andExpect(jsonPath("$.[*].scheduledAt").value(hasItem(DEFAULT_SCHEDULED_AT.toString())))
            .andExpect(jsonPath("$.[*].heldAt").value(hasItem(DEFAULT_HELD_AT.toString())))
            .andExpect(jsonPath("$.[*].agenda").value(hasItem(DEFAULT_AGENDA)))
            .andExpect(jsonPath("$.[*].minutesRef").value(hasItem(DEFAULT_MINUTES_REF.toString())))
            .andExpect(jsonPath("$.[*].minutesDigest").value(hasItem(DEFAULT_MINUTES_DIGEST)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())));

        // Check, that the count call also returns 1
        restMeetingMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultMeetingShouldNotBeFound(String filter) throws Exception {
        restMeetingMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restMeetingMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingMeeting() throws Exception {
        // Get the meeting
        restMeetingMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMeeting() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meeting
        Meeting updatedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMeeting are not directly saved in db
        em.detach(updatedMeeting);
        updatedMeeting
            .instance(UPDATED_INSTANCE)
            .scheduledAt(UPDATED_SCHEDULED_AT)
            .heldAt(UPDATED_HELD_AT)
            .agenda(UPDATED_AGENDA)
            .minutesRef(UPDATED_MINUTES_REF)
            .minutesDigest(UPDATED_MINUTES_DIGEST)
            .state(UPDATED_STATE);
        MeetingDTO meetingDTO = meetingMapper.toDto(updatedMeeting);

        restMeetingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, meetingDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingDTO))
            )
            .andExpect(status().isOk());

        // Validate the Meeting in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMeetingToMatchAllProperties(updatedMeeting);
    }

    @Test
    @Transactional
    void putNonExistingMeeting() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meeting.setId(UUID.randomUUID());

        // Create the Meeting
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMeetingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, meetingDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Meeting in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMeeting() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meeting.setId(UUID.randomUUID());

        // Create the Meeting
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Meeting in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMeeting() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meeting.setId(UUID.randomUUID());

        // Create the Meeting
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(meetingDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Meeting in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMeetingWithPatch() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meeting using partial update
        Meeting partialUpdatedMeeting = new Meeting();
        partialUpdatedMeeting.setId(meeting.getId());

        partialUpdatedMeeting.scheduledAt(UPDATED_SCHEDULED_AT);

        restMeetingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMeeting.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMeeting))
            )
            .andExpect(status().isOk());

        // Validate the Meeting in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMeetingUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedMeeting, meeting), getPersistedMeeting(meeting));
    }

    @Test
    @Transactional
    void fullUpdateMeetingWithPatch() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meeting using partial update
        Meeting partialUpdatedMeeting = new Meeting();
        partialUpdatedMeeting.setId(meeting.getId());

        partialUpdatedMeeting
            .instance(UPDATED_INSTANCE)
            .scheduledAt(UPDATED_SCHEDULED_AT)
            .heldAt(UPDATED_HELD_AT)
            .agenda(UPDATED_AGENDA)
            .minutesRef(UPDATED_MINUTES_REF)
            .minutesDigest(UPDATED_MINUTES_DIGEST)
            .state(UPDATED_STATE);

        restMeetingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMeeting.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMeeting))
            )
            .andExpect(status().isOk());

        // Validate the Meeting in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMeetingUpdatableFieldsEquals(partialUpdatedMeeting, getPersistedMeeting(partialUpdatedMeeting));
    }

    @Test
    @Transactional
    void patchNonExistingMeeting() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meeting.setId(UUID.randomUUID());

        // Create the Meeting
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMeetingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, meetingDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(meetingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Meeting in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMeeting() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meeting.setId(UUID.randomUUID());

        // Create the Meeting
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(meetingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Meeting in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMeeting() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meeting.setId(UUID.randomUUID());

        // Create the Meeting
        MeetingDTO meetingDTO = meetingMapper.toDto(meeting);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(meetingDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Meeting in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMeeting() throws Exception {
        // Initialize the database
        insertedMeeting = meetingRepository.saveAndFlush(meeting);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the meeting
        restMeetingMockMvc
            .perform(delete(ENTITY_API_URL_ID, meeting.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return meetingRepository.count();
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

    protected Meeting getPersistedMeeting(Meeting meeting) {
        return meetingRepository.findById(meeting.getId()).orElseThrow();
    }

    protected void assertPersistedMeetingToMatchAllProperties(Meeting expectedMeeting) {
        assertMeetingAllPropertiesEquals(expectedMeeting, getPersistedMeeting(expectedMeeting));
    }

    protected void assertPersistedMeetingToMatchUpdatableProperties(Meeting expectedMeeting) {
        assertMeetingAllUpdatablePropertiesEquals(expectedMeeting, getPersistedMeeting(expectedMeeting));
    }
}
