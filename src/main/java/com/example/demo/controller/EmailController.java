package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Email;
import com.example.demo.service.EmailService;

@RestController
@RequestMapping("/api/emails")
public class EmailController {
    
    @Autowired
    private EmailService emailService;

    @GetMapping
    public ResponseEntity<List<Email>> getAllEmails() {
        return ResponseEntity.ok(emailService.getAllEmails());
    }
    
    @PostMapping
    public ResponseEntity<Email> create(@RequestBody Email email) {
        emailService.saveEmail(email);
        return ResponseEntity.ok(email);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        emailService.deleteEmail(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/etat")
    public ResponseEntity<Void> updateEtat(@PathVariable Long id, @RequestBody String newEtat) {
        emailService.updateEtat(id, newEtat);
        return ResponseEntity.ok().build();
    }

}
