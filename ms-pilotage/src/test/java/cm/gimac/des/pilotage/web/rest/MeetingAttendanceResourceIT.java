package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.MeetingAttendanceAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Meeting;
import cm.gimac.des.pilotage.domain.MeetingAttendance;
import cm.gimac.des.pilotage.repository.MeetingAttendanceRepository;
import cm.gimac.des.pilotage.service.MeetingAttendanceService;
import cm.gimac.des.pilotage.service.dto.MeetingAttendanceDTO;
import cm.gimac.des.pilotage.service.mapper.MeetingAttendanceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link MeetingAttendanceResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class MeetingAttendanceResourceIT {

    private static final UUID DEFAULT_PARTICIPANT_ID = UUID.randomUUID();
    private static final UUID UPDATED_PARTICIPANT_ID = UUID.randomUUID();

    private static final Boolean DEFAULT_PRESENT = false;
    private static final Boolean UPDATED_PRESENT = true;

    private static final Instant DEFAULT_RECORDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RECORDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/meeting-attendances";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MeetingAttendanceRepository meetingAttendanceRepository;

    @Mock
    private MeetingAttendanceRepository meetingAttendanceRepositoryMock;

    @Autowired
    private MeetingAttendanceMapper meetingAttendanceMapper;

    @Mock
    private MeetingAttendanceService meetingAttendanceServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMeetingAttendanceMockMvc;

    private MeetingAttendance meetingAttendance;

    private MeetingAttendance insertedMeetingAttendance;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MeetingAttendance createEntity(EntityManager em) {
        MeetingAttendance meetingAttendance = new MeetingAttendance()
            .participantId(DEFAULT_PARTICIPANT_ID)
            .present(DEFAULT_PRESENT)
            .recordedAt(DEFAULT_RECORDED_AT);
        // Add required entity
        Meeting meeting;
        if (TestUtil.findAll(em, Meeting.class).isEmpty()) {
            meeting = MeetingResourceIT.createEntity();
            em.persist(meeting);
            em.flush();
        } else {
            meeting = TestUtil.findAll(em, Meeting.class).get(0);
        }
        meetingAttendance.setMeeting(meeting);
        return meetingAttendance;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MeetingAttendance createUpdatedEntity(EntityManager em) {
        MeetingAttendance updatedMeetingAttendance = new MeetingAttendance()
            .participantId(UPDATED_PARTICIPANT_ID)
            .present(UPDATED_PRESENT)
            .recordedAt(UPDATED_RECORDED_AT);
        // Add required entity
        Meeting meeting;
        if (TestUtil.findAll(em, Meeting.class).isEmpty()) {
            meeting = MeetingResourceIT.createUpdatedEntity();
            em.persist(meeting);
            em.flush();
        } else {
            meeting = TestUtil.findAll(em, Meeting.class).get(0);
        }
        updatedMeetingAttendance.setMeeting(meeting);
        return updatedMeetingAttendance;
    }

    @BeforeEach
    void initTest() {
        meetingAttendance = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedMeetingAttendance != null) {
            meetingAttendanceRepository.delete(insertedMeetingAttendance);
            insertedMeetingAttendance = null;
        }
    }

    @Test
    @Transactional
    void createMeetingAttendance() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the MeetingAttendance
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);
        var returnedMeetingAttendanceDTO = om.readValue(
            restMeetingAttendanceMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(meetingAttendanceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MeetingAttendanceDTO.class
        );

        // Validate the MeetingAttendance in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMeetingAttendance = meetingAttendanceMapper.toEntity(returnedMeetingAttendanceDTO);
        assertMeetingAttendanceUpdatableFieldsEquals(returnedMeetingAttendance, getPersistedMeetingAttendance(returnedMeetingAttendance));

        insertedMeetingAttendance = returnedMeetingAttendance;
    }

    @Test
    @Transactional
    void createMeetingAttendanceWithExistingId() throws Exception {
        // Create the MeetingAttendance with an existing ID
        insertedMeetingAttendance = meetingAttendanceRepository.saveAndFlush(meetingAttendance);
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMeetingAttendanceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingAttendance in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkParticipantIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        meetingAttendance.setParticipantId(null);

        // Create the MeetingAttendance, which fails.
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);

        restMeetingAttendanceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMeetingAttendances() throws Exception {
        // Initialize the database
        insertedMeetingAttendance = meetingAttendanceRepository.saveAndFlush(meetingAttendance);

        // Get all the meetingAttendanceList
        restMeetingAttendanceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(meetingAttendance.getId().toString())))
            .andExpect(jsonPath("$.[*].participantId").value(hasItem(DEFAULT_PARTICIPANT_ID.toString())))
            .andExpect(jsonPath("$.[*].present").value(hasItem(DEFAULT_PRESENT)))
            .andExpect(jsonPath("$.[*].recordedAt").value(hasItem(DEFAULT_RECORDED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMeetingAttendancesWithEagerRelationshipsIsEnabled() throws Exception {
        when(meetingAttendanceServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMeetingAttendanceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(meetingAttendanceServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMeetingAttendancesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(meetingAttendanceServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMeetingAttendanceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(meetingAttendanceRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMeetingAttendance() throws Exception {
        // Initialize the database
        insertedMeetingAttendance = meetingAttendanceRepository.saveAndFlush(meetingAttendance);

        // Get the meetingAttendance
        restMeetingAttendanceMockMvc
            .perform(get(ENTITY_API_URL_ID, meetingAttendance.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(meetingAttendance.getId().toString()))
            .andExpect(jsonPath("$.participantId").value(DEFAULT_PARTICIPANT_ID.toString()))
            .andExpect(jsonPath("$.present").value(DEFAULT_PRESENT))
            .andExpect(jsonPath("$.recordedAt").value(DEFAULT_RECORDED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingMeetingAttendance() throws Exception {
        // Get the meetingAttendance
        restMeetingAttendanceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMeetingAttendance() throws Exception {
        // Initialize the database
        insertedMeetingAttendance = meetingAttendanceRepository.saveAndFlush(meetingAttendance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meetingAttendance
        MeetingAttendance updatedMeetingAttendance = meetingAttendanceRepository.findById(meetingAttendance.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMeetingAttendance are not directly saved in db
        em.detach(updatedMeetingAttendance);
        updatedMeetingAttendance.participantId(UPDATED_PARTICIPANT_ID).present(UPDATED_PRESENT).recordedAt(UPDATED_RECORDED_AT);
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(updatedMeetingAttendance);

        restMeetingAttendanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, meetingAttendanceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isOk());

        // Validate the MeetingAttendance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMeetingAttendanceToMatchAllProperties(updatedMeetingAttendance);
    }

    @Test
    @Transactional
    void putNonExistingMeetingAttendance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingAttendance.setId(UUID.randomUUID());

        // Create the MeetingAttendance
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMeetingAttendanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, meetingAttendanceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingAttendance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMeetingAttendance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingAttendance.setId(UUID.randomUUID());

        // Create the MeetingAttendance
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingAttendanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingAttendance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMeetingAttendance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingAttendance.setId(UUID.randomUUID());

        // Create the MeetingAttendance
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingAttendanceMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the MeetingAttendance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMeetingAttendanceWithPatch() throws Exception {
        // Initialize the database
        insertedMeetingAttendance = meetingAttendanceRepository.saveAndFlush(meetingAttendance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meetingAttendance using partial update
        MeetingAttendance partialUpdatedMeetingAttendance = new MeetingAttendance();
        partialUpdatedMeetingAttendance.setId(meetingAttendance.getId());

        partialUpdatedMeetingAttendance.present(UPDATED_PRESENT);

        restMeetingAttendanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMeetingAttendance.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMeetingAttendance))
            )
            .andExpect(status().isOk());

        // Validate the MeetingAttendance in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMeetingAttendanceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedMeetingAttendance, meetingAttendance),
            getPersistedMeetingAttendance(meetingAttendance)
        );
    }

    @Test
    @Transactional
    void fullUpdateMeetingAttendanceWithPatch() throws Exception {
        // Initialize the database
        insertedMeetingAttendance = meetingAttendanceRepository.saveAndFlush(meetingAttendance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meetingAttendance using partial update
        MeetingAttendance partialUpdatedMeetingAttendance = new MeetingAttendance();
        partialUpdatedMeetingAttendance.setId(meetingAttendance.getId());

        partialUpdatedMeetingAttendance.participantId(UPDATED_PARTICIPANT_ID).present(UPDATED_PRESENT).recordedAt(UPDATED_RECORDED_AT);

        restMeetingAttendanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMeetingAttendance.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMeetingAttendance))
            )
            .andExpect(status().isOk());

        // Validate the MeetingAttendance in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMeetingAttendanceUpdatableFieldsEquals(
            partialUpdatedMeetingAttendance,
            getPersistedMeetingAttendance(partialUpdatedMeetingAttendance)
        );
    }

    @Test
    @Transactional
    void patchNonExistingMeetingAttendance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingAttendance.setId(UUID.randomUUID());

        // Create the MeetingAttendance
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMeetingAttendanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, meetingAttendanceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingAttendance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMeetingAttendance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingAttendance.setId(UUID.randomUUID());

        // Create the MeetingAttendance
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingAttendanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingAttendance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMeetingAttendance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingAttendance.setId(UUID.randomUUID());

        // Create the MeetingAttendance
        MeetingAttendanceDTO meetingAttendanceDTO = meetingAttendanceMapper.toDto(meetingAttendance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingAttendanceMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(meetingAttendanceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the MeetingAttendance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMeetingAttendance() throws Exception {
        // Initialize the database
        insertedMeetingAttendance = meetingAttendanceRepository.saveAndFlush(meetingAttendance);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the meetingAttendance
        restMeetingAttendanceMockMvc
            .perform(delete(ENTITY_API_URL_ID, meetingAttendance.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return meetingAttendanceRepository.count();
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

    protected MeetingAttendance getPersistedMeetingAttendance(MeetingAttendance meetingAttendance) {
        return meetingAttendanceRepository.findById(meetingAttendance.getId()).orElseThrow();
    }

    protected void assertPersistedMeetingAttendanceToMatchAllProperties(MeetingAttendance expectedMeetingAttendance) {
        assertMeetingAttendanceAllPropertiesEquals(expectedMeetingAttendance, getPersistedMeetingAttendance(expectedMeetingAttendance));
    }

    protected void assertPersistedMeetingAttendanceToMatchUpdatableProperties(MeetingAttendance expectedMeetingAttendance) {
        assertMeetingAttendanceAllUpdatablePropertiesEquals(
            expectedMeetingAttendance,
            getPersistedMeetingAttendance(expectedMeetingAttendance)
        );
    }
}
