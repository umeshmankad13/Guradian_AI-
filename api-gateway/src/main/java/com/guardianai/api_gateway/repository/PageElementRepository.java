package com.guardianai.api_gateway.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guardianai.api_gateway.entity.PageElement;

public interface PageElementRepository  extends JpaRepository<PageElement, UUID>{
    List<PageElement> findByScanId(UUID scanId);
    
}
