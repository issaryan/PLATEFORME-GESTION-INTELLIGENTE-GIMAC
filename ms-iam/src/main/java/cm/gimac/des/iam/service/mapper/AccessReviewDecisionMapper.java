package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.AccessReview;
import cm.gimac.des.iam.domain.AccessReviewDecision;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.service.dto.AccessReviewDTO;
import cm.gimac.des.iam.service.dto.AccessReviewDecisionDTO;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AccessReviewDecision} and its DTO {@link AccessReviewDecisionDTO}.
 */
@Mapper(componentModel = "spring")
public interface AccessReviewDecisionMapper extends EntityMapper<AccessReviewDecisionDTO, AccessReviewDecision> {
    @Mapping(target = "review", source = "review", qualifiedByName = "accessReviewPeriod")
    @Mapping(target = "collaborator", source = "collaborator", qualifiedByName = "collaboratorFederatedId")
    AccessReviewDecisionDTO toDto(AccessReviewDecision s);

    @Named("accessReviewPeriod")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "period", source = "period")
    AccessReviewDTO toDtoAccessReviewPeriod(AccessReview accessReview);

    @Named("collaboratorFederatedId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "federatedId", source = "federatedId")
    CollaboratorDTO toDtoCollaboratorFederatedId(Collaborator collaborator);
}
