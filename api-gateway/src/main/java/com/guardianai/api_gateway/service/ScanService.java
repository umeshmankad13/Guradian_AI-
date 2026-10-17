package com.guardianai.api_gateway.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.guardianai.api_gateway.dto.ScanDetailResponse;
import com.guardianai.api_gateway.dto.ScanRequest;
import com.guardianai.api_gateway.dto.ScanResponse;
import com.guardianai.api_gateway.entity.Domain;
import com.guardianai.api_gateway.entity.PageElement;
import com.guardianai.api_gateway.entity.Scan;
import com.guardianai.api_gateway.entity.ScanIndicator;
import com.guardianai.api_gateway.entity.ScanResult;
import com.guardianai.api_gateway.entity.Url;
import com.guardianai.api_gateway.repository.DomainRepository;
import com.guardianai.api_gateway.repository.PageElementRepository;
import com.guardianai.api_gateway.repository.ScanIndicatorRepository;
import com.guardianai.api_gateway.repository.ScanRepository;
import com.guardianai.api_gateway.repository.ScanResultRepository;
import com.guardianai.api_gateway.repository.UrlRepository;
import com.guardianai.api_gateway.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

@Service 
public class ScanService {

    private final DomainRepository domainRepository;
    private final UrlRepository urlRepository;
    private final ScanRepository scanRepository;
    private final ScanIndicatorRepository indicatorRepository;
    private final PageElementRepository elementRepository;
    private final ScanResultRepository resultRepository;
    private final UserRepository userRepository;

    public ScanService(DomainRepository domainRepository, UrlRepository urlRepository,
        ScanRepository scanRepository,ScanIndicatorRepository indicatorRepository,PageElementRepository elementRepository,ScanResultRepository resultRepository,UserRepository userRepository
    ){
        this.domainRepository = domainRepository;
        this.urlRepository = urlRepository;
        this.scanRepository = scanRepository;
        this.indicatorRepository = indicatorRepository;
        this.elementRepository = elementRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
    }

    @Transactional 
    public ScanResponse analyze(UUID userId, ScanRequest request)
    {
        String domainName = extractDomain(request.url());

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

        //Create the scan first so the child rows can point to it
        Scan scan = new Scan();
        scan.setUser(userRepository.getReferenceById(userId));
        scan.setUrl(url);
        scan.setDomain(domain);
        scan = scanRepository.save(scan);

        // TODO: replace these mock values with the Analysis Orchestrator call
        int score = 100;
        List<String> findings = new ArrayList<>();
        List<ScanIndicator> indicators = new ArrayList<>();
        List<PageElement> elements = new ArrayList<>();

        boolean https = request.url().startsWith("https://");

        indicators.add(indicator(scan, "URL", "uses_https", String.valueOf(https), !https));
        if(https){
            findings.add("Connection uses HTTPS");
        }
        else{
            score -= 20;
            findings.add("Connections does not use HTTPS");
        }

        if(request.tls() !=null){
            boolean valid = request.tls().valid();
            indicators.add(indicator(scan, "TLS", "tls_valid", String.valueOf(valid),!valid));

            if(valid){
                findings.add("TLS certificate is valid");
            } else {
                score -=30;
                findings.add("TLS certificate is invalid");
            }
        }

        if(request.redirects()!=null){
            int count = request.redirects().size();
            boolean tooMany = count > 3;
            indicators.add(indicator(scan,"REDIRECT", "redirect_count", String.valueOf(count),tooMany));
             if (tooMany) {
                score -= 10;
                findings.add("Many redirects detected (" + count + ")");
            } else {
                findings.add("No suspicious redirects");
            }
        }
        if(request.page() != null)
        {
            int passwordFields = request.page().passwordFields();
            boolean riskyPassword = passwordFields > 0 && !https;
            elements.add(element(scan, "PASSWORD_FIELD", String.valueOf(passwordFields), riskyPassword,
                    riskyPassword ? "Password field on a page without HTTPS" : null));
            if (riskyPassword) {
                score -= 20;
                findings.add("Password field on an unencrypted page");
            }

             int scripts = request.page().externalScripts();
            boolean manyScripts = scripts > 10;
            elements.add(element(scan, "EXTERNAL_SCRIPT", String.valueOf(scripts), manyScripts,
                    manyScripts ? "Unusually many external scripts" : null));
            if (manyScripts) {
                score -= 10;
                findings.add("Many external scripts (" + scripts + ")");
            }
        }

        score = Math.max(score, 0);
        String level = score >= 80 ? "LOW" : score>=50 ? "MEDIUM" : "HIGH";
        List<String> recommendations = switch(level) {
            case "LOW" -> List.of("No actions  needed");

            case "MEDIUM" -> List.of("Avoid entering personal information on this site");

            default -> List.of("Do not enter credentials or personal data ", "Leave the site");
        };
        String explanation = "Placeholder rule-based analysis of " + domainName + "gave a security score of"
         + score + "/100";

        
        scan.setStatus("COMPLETED");
        scan.setRiskScore(82);
        scan.setRiskLevel("LOW");
        scan.setConfidence(new BigDecimal("0.90"));
        scan.setSummary("This is a placeholder verdict for " + domainName + ".");
        scan.setCompletedAt(LocalDateTime.now());
        
        indicatorRepository.saveAll(indicators);
        elementRepository.saveAll(elements);

        ScanResult result = new ScanResult();
        result.setScan(scan);
        result.setAnalysisType("RULE_BASED");
        result.setRiskScore(score);
        result.setRiskLevel(level);
        result.setExplanation(explanation);
        result.setRecommendation(String.join("; ", recommendations));
        result.setModelUsed("placeholder-rules");
        resultRepository.save(result);

          // finalize the scan (it is managed, so the changes are saved automatically)
        scan.setStatus("COMPLETED");
        scan.setRiskScore(score);
        scan.setRiskLevel(level);
        scan.setConfidence(new BigDecimal("0.60"));
        scan.setSummary(explanation);
        scan.setCompletedAt(LocalDateTime.now());

        //TODO: replace with the Analysis Orchestrator call
        return new ScanResponse(scan.getId(),
                domainName,
                level,
                score,
                0.60,
                findings,
                explanation,
                recommendations,
                scan.getCompletedAt().toInstant(ZoneOffset.UTC));
    }

    private ScanIndicator indicator(Scan scan, String type, String name, String value, boolean malicious) {
        ScanIndicator i = new ScanIndicator();
        i.setScan(scan);
        i.setIndicatorType(type);
        i.setIndicatorName(name);
        i.setValue(value);
        i.setMalicious(malicious);
        return i;
    }

    private PageElement element(Scan scan, String type, String value, boolean suspicious, String reason) {
        PageElement e = new PageElement();
        e.setScan(scan);
        e.setElementType(type);
        e.setElementValue(value);
        e.setSuspicious(suspicious);
        e.setReason(reason);
        return e;
    }

    @Transactional(readOnly = true)
    public ScanDetailResponse getScan(UUID userId,UUID id) {
        Scan scan = scanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scan not found"));

        //Other user's scans look the same as missing ones, so IDs can't be probed
        if(scan.getUser() == null || !scan.getUser().getId().equals(userId)){
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

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }

    private String extractDomain(String url){
        try {
            String host = URI.create(url).getHost();
            return host != null ? host : "unknown";
        } catch (IllegalArgumentException e) {
            return "unknown";
        }
    }
}
