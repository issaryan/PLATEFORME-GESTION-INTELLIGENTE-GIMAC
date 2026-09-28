package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.ChannelPreference;
import cm.gimac.des.notification.service.dto.ChannelPreferenceDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ChannelPreference} and its DTO {@link ChannelPreferenceDTO}.
 */
@Mapper(componentModel = "spring")
public interface ChannelPreferenceMapper extends EntityMapper<ChannelPreferenceDTO, ChannelPreference> {}
