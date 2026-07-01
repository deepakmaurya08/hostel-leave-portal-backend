package com.akgec.hostel.controller;


import com.akgec.hostel.model.entity.LeaveDocuments;
import com.akgec.hostel.repository.LeaveDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final LeaveDocumentRepository repository;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> view(@PathVariable Long id) {

        LeaveDocuments doc = repository.findById(id)
                .orElseThrow();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + doc.getFileName() + "\"")
                .body(doc.getData());
    }
}