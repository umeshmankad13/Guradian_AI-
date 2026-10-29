package com.guardianai.api_gateway.service;

import com.guardianai.api_gateway.dto.HistoryItem;
import com.guardianai.api_gateway.dto.PageResponse;
import com.guardianai.api_gateway.dto.ScanDetailResponse;
import com.guardianai.api_gateway.dto.ScanRequest;
import com.guardianai.api_gateway.dto.ScanResponse;
import com.guardianai.api_gateway.entity.Device;
import com.guardianai.api_gateway.entity.Domain;
import com.guardianai.api_gateway.entity.PageElement;
import com.guardianai.api_gateway.entity.Scan;
import com.guardianai.api_gateway.entity.ScanIndicator;
import com.guardianai.api_gateway.entity.ScanResult;
import com.guardianai.api_gateway.entity.Url;
import com.guardianai.api_gateway.repository.DeviceRepository;
import com.guardianai.api_gateway.repository.DomainRepository;
import com.guardianai.api_gateway.repository.PageElementRepository;
import com.guardianai.api_gateway.repository.ScanIndicatorRepository;
import com.guardianai.api_gateway.repository.ScanRepository;
import com.guardianai.api_gateway.repository.ScanResultRepository;
import com.guardianai.api_gateway.repository.UrlRepository;
import com.guardianai.api_gateway.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ScanService {

    private final DomainRepository domainRepository;
    private final UrlRepository urlRepository;
    private final ScanRepository scanRepository;
    private final ScanIndicatorRepository indicatorRepository;
    private final PageElementRepository elementRepository;
    private final ScanResultRepository resultRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final AnalysisService analysisService;
    private final TransactionTemplate tx;

    public ScanService(DomainRepository domainRepository,
                       UrlRepository urlRepository,
                       ScanRepository scanRepository,
                       ScanIndicatorRepository indicatorRepository,
                       PageElementRepository elementRepository,
                       ScanResultRepository resultRepository,
                       UserRepository userRepository,
                       DeviceRepository deviceRepository,
                       AnalysisService analysisService,
                       TransactionTemplate tx) {
        this.domainRepository = domainRepository;
        this.urlRepository = urlRepository;
        this.scanRepository = scanRepository;
        this.indicatorRepository = indicatorRepository;
        this.elementRepository = elementRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.analysisService = analysisService;
        this.tx = tx;
    }

    public ScanResponse analyze(UUID userId, UUID deviceId, ScanRequest request) {
        if (deviceId != null) {
            boolean allowed = deviceRepository.findByIdAndUserId(deviceId, userId)
                    .filter(d -> !d.isRevoked())
                    .isPresent();
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device not allowed");
            }
        }

        // the slow part (cache lookup or analysis) runs outside any database transaction
        AnalysisService.Result analysis = analysisService.analyze(request);

        return tx.execute(status -> save(userId, deviceId, request, analysis.outcome(), analysis.cached()));
    }

    private ScanResponse save(UUID userId, UUID deviceId, ScanRequest request,
                              AnalysisOutcome outcome, boolean cached) {
        String domainName = UrlUtils.extractDomain(request.url());

        Domain domain = domainRepository.findByDomain(domainName).orElseGet(() -> {
            Domain d = new Domain();
            d.setDomain(domainName);
            return domainRepository.save(d);
        });

        Url url = urlRepository.findByUrl(request.url()).orElseGet(() -> {
            Url u = new Url();
            u.setUrl(request.url());
            u.setDomain(domain);
            return urlRepository.save(u);
        });

        Device device = null;
        if (deviceId != null) {
            device = deviceRepository.findById(deviceId).orElse(null);
            if (device != null) {
                device.setLastSeenAt(LocalDateTime.now());
            }
        }

        Scan draft = new Scan();
        draft.setUser(userRepository.getReferenceById(userId));
        draft.setDevice(device);
        draft.setUrl(url);
        draft.setDomain(domain);
        draft.setStatus("COMPLETED");
        draft.setRiskScore(outcome.score());
        draft.setRiskLevel(outcome.level());
        draft.setConfidence(BigDecimal.valueOf(outcome.confidence()).setScale(2, RoundingMode.HALF_UP));
        draft.setSummary(outcome.explanation());
        draft.setCompletedAt(LocalDateTime.now());
        Scan scan = scanRepository.save(draft);

        indicatorRepository.saveAll(outcome.indicators().stream().map(i -> toEntity(scan, i)).toList());
        elementRepository.saveAll(outcome.elements().stream().map(e -> toEntity(scan, e)).toList());

        ScanResult result = new ScanResult();
        result.setScan(scan);
        result.setAnalysisType("RULE_BASED");
        result.setRiskScore(outcome.score());
        result.setRiskLevel(outcome.level());
        result.setExplanation(outcome.explanation());
        result.setRecommendation(String.join("; ", outcome.recommendations()));
        result.setModelUsed("placeholder-rules");
        resultRepository.save(result);

        return new ScanResponse(
                scan.getId(),
                domainName,
                outcome.level(),
                outcome.score(),
                outcome.confidence(),
                outcome.findings(),
                outcome.explanation(),
                outcome.recommendations(),
                toInstant(scan.getCompletedAt()),
                cached
        );
    }

    @Transactional(readOnly = true)
    public ScanDetailResponse getScan(UUID userId, UUID id) {
        Scan scan = scanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scan not found"));

        // other users' scans look exactly like missing ones
        if (scan.getUser() == null || !scan.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scan not found");
        }

        List<ScanDetailResponse.IndicatorDto> indicators = indicatorRepository.findByScanId(id).stream()
                .map(i -> new ScanDetailResponse.IndicatorDto(
                        i.getIndicatorType(), i.getIndicatorName(), i.getValue(), i.isMalicious()))
                .toList();

        List<ScanDetailResponse.ElementDto> elements = elementRepository.findByScanId(id).stream()
                .map(e -> new ScanDetailResponse.ElementDto(
                        e.getElementType(), e.getElementValue(), e.isSuspicious(), e.getReason()))
                .toList();

        List<String> recommendations = resultRepository.findFirstByScanId(id)
                .map(r -> r.getRecommendation() == null
                        ? List.<String>of()
                        : List.of(r.getRecommendation().split("; ")))
                .orElse(List.of());

        return new ScanDetailResponse(
                scan.getId(),
                scan.getUrl().getUrl(),
                scan.getDomain().getDomain(),
                scan.getStatus(),
                scan.getRiskScore(),
                scan.getRiskLevel(),
                scan.getConfidence(),
                scan.getSummary(),
                recommendations,
                indicators,
                elements,
                toInstant(scan.getCreatedAt()),
                toInstant(scan.getCompletedAt())
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoryItem> getHistory(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<Scan> result = scanRepository.findByUserId(
                userId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<HistoryItem> items = result.getContent().stream()
                .map(s -> new HistoryItem(
                        s.getId(),
                        s.getUrl().getUrl(),
                        s.getDomain().getDomain(),
                        s.getStatus(),
                        s.getRiskScore(),
                        s.getRiskLevel(),
                        toInstant(s.getCreatedAt())))
                .toList();

        return new PageResponse<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private ScanIndicator toEntity(Scan scan, AnalysisOutcome.IndicatorData d) {
        ScanIndicator i = new ScanIndicator();
        i.setScan(scan);
        i.setIndicatorType(d.type());
        i.setIndicatorName(d.name());
        i.setValue(d.value());
        i.setMalicious(d.malicious());
        return i;
    }

    private PageElement toEntity(Scan scan, AnalysisOutcome.ElementData d) {
        PageElement e = new PageElement();
        e.setScan(scan);
        e.setElementType(d.type());
        e.setElementValue(d.value());
        e.setSuspicious(d.suspicious());
        e.setReason(d.reason());
        return e;
    }

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}