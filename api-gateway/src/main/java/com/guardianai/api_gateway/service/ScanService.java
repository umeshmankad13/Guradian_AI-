package com.guardianai.api_gateway.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.guardianai.api_gateway.dto.ScanRequest;
import com.guardianai.api_gateway.dto.ScanResponse;
import com.guardianai.api_gateway.entity.Domain;
import com.guardianai.api_gateway.entity.Scan;
import com.guardianai.api_gateway.entity.Url;
import com.guardianai.api_gateway.repository.DomainRepository;
import com.guardianai.api_gateway.repository.ScanRepository;
import com.guardianai.api_gateway.repository.UrlRepository;

import jakarta.transaction.Transactional;

@Service 
public class ScanService {

    private final DomainRepository domainRepository;
    private final UrlRepository urlRepository;
    private final ScanRepository scanRepository;

    public ScanService(DomainRepository domainRepository, UrlRepository urlRepository,
        ScanRepository scanRepository
    ){
        this.domainRepository = domainRepository;
        this.urlRepository = urlRepository;
        this.scanRepository = scanRepository;
    }

    @Transactional 
    public ScanResponse analyze(ScanRequest request)
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

        // TODO: replace these mock values with the Analysis Orchestrator call
        Scan scan = new Scan();
        scan.setUrl(url);
        scan.setDomain(domain);
        scan.setStatus("COMPLETED");
        scan.setRiskScore(82);
        scan.setRiskLevel("LOW");
        scan.setConfidence(new BigDecimal("0.90"));
        scan.setSummary("This is a placeholder verdict for " + domainName + ".");
        scan.setCompletedAt(LocalDateTime.now());
        scan = scanRepository.save(scan);


        //TODO: replace with the Analysis Orchestrator call
        return new ScanResponse(scan.getId(),
                domainName,
                scan.getRiskLevel(),
                scan.getRiskScore(),
                scan.getConfidence().doubleValue(),
                List.of("Mock result: real analysis not connected yet"),
                scan.getSummary(),
                List.of("No action needed"),
                scan.getCompletedAt().toInstant(ZoneOffset.UTC));
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
