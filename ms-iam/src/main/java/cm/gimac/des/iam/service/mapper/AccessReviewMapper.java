package cm.gimac.des.iam.service.mapper;

import cm.gimac.des.iam.domain.AccessReview;
import cm.gimac.des.iam.service.dto.AccessReviewDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AccessReview} and its DTO {@link AccessReviewDTO}.
 */
@Mapper(componentModel = "spring")
public interface AccessReviewMapper extends EntityMapper<AccessReviewDTO, AccessReview> {}
