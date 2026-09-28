package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.repository.TaskRepository;
import cm.gimac.des.pilotage.service.criteria.TaskCriteria;
import cm.gimac.des.pilotage.service.dto.TaskDTO;
import cm.gimac.des.pilotage.service.mapper.TaskMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Task} entities in the database.
 * The main input is a {@link TaskCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link TaskDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class TaskQueryService extends QueryService<Task> {

    private static final Logger LOG = LoggerFactory.getLogger(TaskQueryService.class);

    private final TaskRepository taskRepository;

    private final TaskMapper taskMapper;

    public TaskQueryService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    /**
     * Return a {@link Page} of {@link TaskDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<TaskDTO> findByCriteria(TaskCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Task> specification = createSpecification(criteria);
        return taskRepository.findAll(specification, page).map(taskMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(TaskCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Task> specification = createSpecification(criteria);
        return taskRepository.count(specification);
    }

    /**
     * Function to convert {@link TaskCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Task> createSpecification(TaskCriteria criteria) {
        Specification<Task> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Task_.id),
                buildStringSpecification(criteria.getTitle(), Task_.title),
                buildSpecification(criteria.getAssigneeId(), Task_.assigneeId),
                buildRangeSpecification(criteria.getDueDate(), Task_.dueDate),
                buildSpecification(criteria.getOriginKind(), Task_.originKind),
                buildStringSpecification(criteria.getOriginReference(), Task_.originReference),
                buildStringSpecification(criteria.getOriginService(), Task_.originService),
                buildRangeSpecification(criteria.getStartedAt(), Task_.startedAt),
                buildRangeSpecification(criteria.getCompletedAt(), Task_.completedAt),
                buildSpecification(criteria.getState(), Task_.state),
                buildRangeSpecification(criteria.getReminderLevel(), Task_.reminderLevel),
                buildRangeSpecification(criteria.getClosureProposedAt(), Task_.closureProposedAt),
                buildSpecification(criteria.getBoardId(), root -> root.join(Task_.board, JoinType.LEFT).get(Board_.id)),
                buildSpecification(criteria.getBoardColumnId(), root -> root.join(Task_.boardColumn, JoinType.LEFT).get(BoardColumn_.id)),
                buildSpecification(criteria.getActionSheetId(), root -> root.join(Task_.actionSheet, JoinType.LEFT).get(ActionSheet_.id)),
                buildSpecification(criteria.getDecisionId(), root -> root.join(Task_.decision, JoinType.LEFT).get(Decision_.id))
            );
        }
        return specification;
    }
}
