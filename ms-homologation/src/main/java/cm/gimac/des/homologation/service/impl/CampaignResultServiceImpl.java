package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.CampaignResult;
import cm.gimac.des.homologation.repository.CampaignResultRepository;
import cm.gimac.des.homologation.service.CampaignResultService;
import cm.gimac.des.homologation.service.dto.CampaignResultDTO;
import cm.gimac.des.homologation.service.mapper.CampaignResultMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.CampaignResult}.
 */
@Service
@Transactional
public class CampaignResultServiceImpl implements CampaignResultService {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignResultServiceImpl.class);

    private final CampaignResultRepository campaignResultRepository;

    private final CampaignResultMapper campaignResultMapper;

    public CampaignResultServiceImpl(CampaignResultRepository campaignResultRepository, CampaignResultMapper campaignResultMapper) {
        this.campaignResultRepository = campaignResultRepository;
        this.campaignResultMapper = campaignResultMapper;
    }

    @Override
    public CampaignResultDTO save(CampaignResultDTO campaignResultDTO) {
        LOG.debug("Request to save CampaignResult : {}", campaignResultDTO);
        CampaignResult campaignResult = campaignResultMapper.toEntity(campaignResultDTO);
        campaignResult = campaignResultRepository.save(campaignResult);
        return campaignResultMapper.toDto(campaignResult);
    }

    @Override
    public CampaignResultDTO update(CampaignResultDTO campaignResultDTO) {
        LOG.debug("Request to update CampaignResult : {}", campaignResultDTO);
        CampaignResult campaignResult = campaignResultMapper.toEntity(campaignResultDTO);
        campaignResult = campaignResultRepository.save(campaignResult);
        return campaignResultMapper.toDto(campaignResult);
    }

    @Override
    public Optional<CampaignResultDTO> partialUpdate(CampaignResultDTO campaignResultDTO) {
        LOG.debug("Request to partially update CampaignResult : {}", campaignResultDTO);

        return campaignResultRepository
            .findById(campaignResultDTO.getId())
            .map(existingCampaignResult -> {
                campaignResultMapper.partialUpdate(existingCampaignResult, campaignResultDTO);

                return existingCampaignResult;
            })
            .map(campaignResultRepository::save)
            .map(campaignResultMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CampaignResultDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all CampaignResults");
        return campaignResultRepository.findAll(pageable).map(campaignResultMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CampaignResultDTO> findOne(UUID id) {
        LOG.debug("Request to get CampaignResult : {}", id);
        return campaignResultRepository.findById(id).map(campaignResultMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete CampaignResult : {}", id);
        campaignResultRepository.deleteById(id);
    }
}
