package com.akgec.hostel.service.impl;

import com.akgec.hostel.model.entity.LeaveDocuments;
import com.akgec.hostel.model.entity.LeaveRequest;
import com.akgec.hostel.repository.LeaveDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final LeaveDocumentRepository repository;

    @Transactional
    public void saveDocuments(LeaveRequest leave,
                              List<MultipartFile> files) {

        if (files == null || files.isEmpty()) return;

        for (MultipartFile file : files) {

            if (file.isEmpty()) continue;

            try {

                LeaveDocuments doc = LeaveDocuments.builder()
                        .leaveRequest(leave)
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .data(file.getBytes())
                        .build();

                repository.save(doc);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
