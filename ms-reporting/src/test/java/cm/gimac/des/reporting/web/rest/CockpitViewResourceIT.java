package cm.gimac.des.reporting.web.rest;

import static cm.gimac.des.reporting.domain.CockpitViewAsserts.*;
import static cm.gimac.des.reporting.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.reporting.IntegrationTest;
import cm.gimac.des.reporting.domain.CockpitView;
import cm.gimac.des.reporting.repository.CockpitViewRepository;
import cm.gimac.des.reporting.service.CockpitViewService;
import cm.gimac.des.reporting.service.dto.CockpitViewDTO;
import cm.gimac.des.reporting.service.mapper.CockpitViewMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
 * Integration tests for the {@link CockpitViewResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CockpitViewResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final String DEFAULT_AUDIENCE = "AAAAAAAAAA";
    private static final String UPDATED_AUDIENCE = "BBBBBBBBBB";

    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofHours(6);
    private static final Duration UPDATED_REFRESH_INTERVAL = Duration.ofHours(12);

    private static final String ENTITY_API_URL = "/api/cockpit-views";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CockpitViewRepository cockpitViewRepository;

    @Mock
    private CockpitViewRepository cockpitViewRepositoryMock;

    @Autowired
    private CockpitViewMapper cockpitViewMapper;

    @Mock
    private CockpitViewService cockpitViewServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCockpitViewMockMvc;

    private CockpitView cockpitView;

    private CockpitView insertedCockpitView;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CockpitView createEntity() {
        return new CockpitView()
            .code(DEFAULT_CODE)
            .label(DEFAULT_LABEL)
            .audience(DEFAULT_AUDIENCE)
            .refreshInterval(DEFAULT_REFRESH_INTERVAL);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CockpitView createUpdatedEntity() {
        return new CockpitView()
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .audience(UPDATED_AUDIENCE)
            .refreshInterval(UPDATED_REFRESH_INTERVAL);
    }

    @BeforeEach
    void initTest() {
        cockpitView = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCockpitView != null) {
            cockpitViewRepository.delete(insertedCockpitView);
            insertedCockpitView = null;
        }
    }

    @Test
    @Transactional
    void createCockpitView() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the CockpitView
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);
        var returnedCockpitViewDTO = om.readValue(
            restCockpitViewMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cockpitViewDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CockpitViewDTO.class
        );

        // Validate the CockpitView in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCockpitView = cockpitViewMapper.toEntity(returnedCockpitViewDTO);
        assertCockpitViewUpdatableFieldsEquals(returnedCockpitView, getPersistedCockpitView(returnedCockpitView));

        insertedCockpitView = returnedCockpitView;
    }

    @Test
    @Transactional
    void createCockpitViewWithExistingId() throws Exception {
        // Create the CockpitView with an existing ID
        insertedCockpitView = cockpitViewRepository.saveAndFlush(cockpitView);
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCockpitViewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CockpitView in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        cockpitView.setCode(null);

        // Create the CockpitView, which fails.
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        restCockpitViewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        cockpitView.setLabel(null);

        // Create the CockpitView, which fails.
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        restCockpitViewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAudienceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        cockpitView.setAudience(null);

        // Create the CockpitView, which fails.
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        restCockpitViewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRefreshIntervalIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        cockpitView.setRefreshInterval(null);

        // Create the CockpitView, which fails.
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        restCockpitViewMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCockpitViews() throws Exception {
        // Initialize the database
        insertedCockpitView = cockpitViewRepository.saveAndFlush(cockpitView);

        // Get all the cockpitViewList
        restCockpitViewMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(cockpitView.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].audience").value(hasItem(DEFAULT_AUDIENCE)))
            .andExpect(jsonPath("$.[*].refreshInterval").value(hasItem(DEFAULT_REFRESH_INTERVAL.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCockpitViewsWithEagerRelationshipsIsEnabled() throws Exception {
        when(cockpitViewServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCockpitViewMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(cockpitViewServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCockpitViewsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(cockpitViewServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCockpitViewMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(cockpitViewRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCockpitView() throws Exception {
        // Initialize the database
        insertedCockpitView = cockpitViewRepository.saveAndFlush(cockpitView);

        // Get the cockpitView
        restCockpitViewMockMvc
            .perform(get(ENTITY_API_URL_ID, cockpitView.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(cockpitView.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.audience").value(DEFAULT_AUDIENCE))
            .andExpect(jsonPath("$.refreshInterval").value(DEFAULT_REFRESH_INTERVAL.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCockpitView() throws Exception {
        // Get the cockpitView
        restCockpitViewMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCockpitView() throws Exception {
        // Initialize the database
        insertedCockpitView = cockpitViewRepository.saveAndFlush(cockpitView);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the cockpitView
        CockpitView updatedCockpitView = cockpitViewRepository.findById(cockpitView.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCockpitView are not directly saved in db
        em.detach(updatedCockpitView);
        updatedCockpitView.code(UPDATED_CODE).label(UPDATED_LABEL).audience(UPDATED_AUDIENCE).refreshInterval(UPDATED_REFRESH_INTERVAL);
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(updatedCockpitView);

        restCockpitViewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, cockpitViewDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isOk());

        // Validate the CockpitView in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCockpitViewToMatchAllProperties(updatedCockpitView);
    }

    @Test
    @Transactional
    void putNonExistingCockpitView() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        cockpitView.setId(UUID.randomUUID());

        // Create the CockpitView
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCockpitViewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, cockpitViewDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CockpitView in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCockpitView() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        cockpitView.setId(UUID.randomUUID());

        // Create the CockpitView
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCockpitViewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CockpitView in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCockpitView() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        cockpitView.setId(UUID.randomUUID());

        // Create the CockpitView
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCockpitViewMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cockpitViewDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the CockpitView in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCockpitViewWithPatch() throws Exception {
        // Initialize the database
        insertedCockpitView = cockpitViewRepository.saveAndFlush(cockpitView);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the cockpitView using partial update
        CockpitView partialUpdatedCockpitView = new CockpitView();
        partialUpdatedCockpitView.setId(cockpitView.getId());

        partialUpdatedCockpitView.label(UPDATED_LABEL).audience(UPDATED_AUDIENCE);

        restCockpitViewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCockpitView.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCockpitView))
            )
            .andExpect(status().isOk());

        // Validate the CockpitView in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCockpitViewUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCockpitView, cockpitView),
            getPersistedCockpitView(cockpitView)
        );
    }

    @Test
    @Transactional
    void fullUpdateCockpitViewWithPatch() throws Exception {
        // Initialize the database
        insertedCockpitView = cockpitViewRepository.saveAndFlush(cockpitView);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the cockpitView using partial update
        CockpitView partialUpdatedCockpitView = new CockpitView();
        partialUpdatedCockpitView.setId(cockpitView.getId());

        partialUpdatedCockpitView
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .audience(UPDATED_AUDIENCE)
            .refreshInterval(UPDATED_REFRESH_INTERVAL);

        restCockpitViewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCockpitView.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCockpitView))
            )
            .andExpect(status().isOk());

        // Validate the CockpitView in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCockpitViewUpdatableFieldsEquals(partialUpdatedCockpitView, getPersistedCockpitView(partialUpdatedCockpitView));
    }

    @Test
    @Transactional
    void patchNonExistingCockpitView() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        cockpitView.setId(UUID.randomUUID());

        // Create the CockpitView
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCockpitViewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, cockpitViewDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CockpitView in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCockpitView() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        cockpitView.setId(UUID.randomUUID());

        // Create the CockpitView
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCockpitViewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CockpitView in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCockpitView() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        cockpitView.setId(UUID.randomUUID());

        // Create the CockpitView
        CockpitViewDTO cockpitViewDTO = cockpitViewMapper.toDto(cockpitView);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCockpitViewMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(cockpitViewDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CockpitView in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCockpitView() throws Exception {
        // Initialize the database
        insertedCockpitView = cockpitViewRepository.saveAndFlush(cockpitView);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the cockpitView
        restCockpitViewMockMvc
            .perform(delete(ENTITY_API_URL_ID, cockpitView.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return cockpitViewRepository.count();
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

    protected CockpitView getPersistedCockpitView(CockpitView cockpitView) {
        return cockpitViewRepository.findById(cockpitView.getId()).orElseThrow();
    }

    protected void assertPersistedCockpitViewToMatchAllProperties(CockpitView expectedCockpitView) {
        assertCockpitViewAllPropertiesEquals(expectedCockpitView, getPersistedCockpitView(expectedCockpitView));
    }

    protected void assertPersistedCockpitViewToMatchUpdatableProperties(CockpitView expectedCockpitView) {
        assertCockpitViewAllUpdatablePropertiesEquals(expectedCockpitView, getPersistedCockpitView(expectedCockpitView));
    }
}
