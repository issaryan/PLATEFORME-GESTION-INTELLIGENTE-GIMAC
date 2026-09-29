package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.TimestampToken;
import cm.gimac.des.certification.repository.TimestampTokenRepository;
import cm.gimac.des.certification.service.TimestampTokenService;
import cm.gimac.des.certification.service.dto.TimestampTokenDTO;
import cm.gimac.des.certification.service.mapper.TimestampTokenMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.TimestampToken}.
 */
@Service
@Transactional
public class TimestampTokenServiceImpl implements TimestampTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(TimestampTokenServiceImpl.class);

    private final TimestampTokenRepository timestampTokenRepository;

    private final TimestampTokenMapper timestampTokenMapper;

    public TimestampTokenServiceImpl(TimestampTokenRepository timestampTokenRepository, TimestampTokenMapper timestampTokenMapper) {
        this.timestampTokenRepository = timestampTokenRepository;
        this.timestampTokenMapper = timestampTokenMapper;
    }

    @Override
    public TimestampTokenDTO save(TimestampTokenDTO timestampTokenDTO) {
        LOG.debug("Request to save TimestampToken : {}", timestampTokenDTO);
        TimestampToken timestampToken = timestampTokenMapper.toEntity(timestampTokenDTO);
        timestampToken = timestampTokenRepository.save(timestampToken);
        return timestampTokenMapper.toDto(timestampToken);
    }

    @Override
    public TimestampTokenDTO update(TimestampTokenDTO timestampTokenDTO) {
        LOG.debug("Request to update TimestampToken : {}", timestampTokenDTO);
        TimestampToken timestampToken = timestampTokenMapper.toEntity(timestampTokenDTO);
        timestampToken = timestampTokenRepository.save(timestampToken);
        return timestampTokenMapper.toDto(timestampToken);
    }

    @Override
    public Optional<TimestampTokenDTO> partialUpdate(TimestampTokenDTO timestampTokenDTO) {
        LOG.debug("Request to partially update TimestampToken : {}", timestampTokenDTO);

        return timestampTokenRepository
            .findById(timestampTokenDTO.getId())
            .map(existingTimestampToken -> {
                timestampTokenMapper.partialUpdate(existingTimestampToken, timestampTokenDTO);

                return existingTimestampToken;
            })
            .map(timestampTokenRepository::save)
            .map(timestampTokenMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimestampTokenDTO> findAll() {
        LOG.debug("Request to get all TimestampTokens");
        return timestampTokenRepository
            .findAll()
            .stream()
            .map(timestampTokenMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TimestampTokenDTO> findOne(UUID id) {
        LOG.debug("Request to get TimestampToken : {}", id);
        return timestampTokenRepository.findById(id).map(timestampTokenMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete TimestampToken : {}", id);
        timestampTokenRepository.deleteById(id);
    }
}
