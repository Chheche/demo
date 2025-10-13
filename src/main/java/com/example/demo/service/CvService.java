package com.example.demo.service;

import com.example.demo.model.Cv;
import com.example.demo.repository.CvRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class CvService {

    private final CvRepository cvRepository;

    public CvService(CvRepository cvRepository) {
        this.cvRepository = cvRepository;
    }

    public Cv saveCv(MultipartFile file, String userEmail) throws IOException {
        Cv cv = new Cv();
        cv.setFileName(file.getOriginalFilename());
        cv.setFileType(file.getContentType());
        cv.setData(file.getBytes());
        cv.setUserEmail(userEmail);
        return cvRepository.save(cv);
    }

    public List<Cv> getAllCvs() {
        return cvRepository.findAll();
    }

    public List<Cv> getCvsByUserEmail(String email) {
        return cvRepository.findByUserEmail(email);
    }

    public Optional<Cv> getCv(Long id) {
        return cvRepository.findById(id);
    }

    public void deleteCv(Long id) {
        cvRepository.deleteById(id);
    }
}