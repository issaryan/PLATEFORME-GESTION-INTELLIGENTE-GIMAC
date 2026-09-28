package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.VerifiedTerminal;
import cm.gimac.des.collaboration.service.dto.VerifiedTerminalDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link VerifiedTerminal} and its DTO {@link VerifiedTerminalDTO}.
 */
@Mapper(componentModel = "spring")
public interface VerifiedTerminalMapper extends EntityMapper<VerifiedTerminalDTO, VerifiedTerminal> {}
