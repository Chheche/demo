package com.example.demo.service;

import com.example.demo.model.Email;
import com.example.demo.repository.EmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private EmailRepository EmailRepository;

    public void saveEmail(Email email) {
        EmailRepository.save(email);
    }

    public List<Email> getAllEmails() {
        return EmailRepository.findAll();
    }

    public void deleteEmail(Long id) {
        EmailRepository.deleteById(id);
    }

    public void updateEtat(Long id, String newEtat) {
        Email email = EmailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email non trouvé"));
        email.setEtat(newEtat);
        EmailRepository.save(email);
    }

    public void deleteEmailsBySource(String source) {
        EmailRepository.deleteBySource(source);
    }
}
