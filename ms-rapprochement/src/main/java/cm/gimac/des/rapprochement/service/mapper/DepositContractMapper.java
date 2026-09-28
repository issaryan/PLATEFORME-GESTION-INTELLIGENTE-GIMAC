package cm.gimac.des.rapprochement.service.mapper;

import cm.gimac.des.rapprochement.domain.DepositContract;
import cm.gimac.des.rapprochement.service.dto.DepositContractDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DepositContract} and its DTO {@link DepositContractDTO}.
 */
@Mapper(componentModel = "spring")
public interface DepositContractMapper extends EntityMapper<DepositContractDTO, DepositContract> {}
