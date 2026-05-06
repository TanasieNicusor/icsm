package com.example.icsm.service;

import com.example.icsm.model.Document;
import com.example.icsm.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    public List<Document> getDocumentsByPolicy(Long policyId) {
        return documentRepository.findByPolicyId(policyId);
    }

    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }
}
