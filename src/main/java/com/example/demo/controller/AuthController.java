package com.example.demo.controller;

import com.example.demo.model.Cv;
import com.example.demo.service.CvService;
import com.example.demo.service.EmailService;

import com.example.demo.model.Email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

@Controller
public class AuthController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private CvService cvService;

    @GetMapping("/")
    public String home() {
        return "accueil";
    }

    @GetMapping("/accueil")
    public String accueil() {
        return "accueil";
    }

    @GetMapping("/candidatures")
    public String candidatures(Model model) {
        String userEmail = getCurrentUserEmail();
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("data", emailService.getAllEmails());
        return "candidatures";
    }

    @GetMapping("/CVs")
    public String cvs(Model model) {
        String userEmail = getCurrentUserEmail();
        List<Cv> cvs = cvService.getCvsByUserEmail(userEmail);

        model.addAttribute("data", cvs);
        return "CVs";
    }

    @PostMapping("/uploadCv")
    public String uploadCv(@RequestParam("file") MultipartFile file, Model model) {
        String userEmail = getCurrentUserEmail();
        try {
            cvService.saveCv(file, userEmail);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Cv> cvs = cvService.getCvsByUserEmail(userEmail);
        model.addAttribute("data", cvs);
        return "CVs";
    }

    @GetMapping("/viewCv/{id}")
    public ResponseEntity<byte[]> viewCv(@PathVariable Long id) {
        Cv cv = cvService.findById(id)
                .orElseThrow(() -> new RuntimeException("CV introuvable"));

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"" + cv.getFileName() + "\"")
                .header("X-Frame-Options", "SAMEORIGIN")
                .body(cv.getData());
    }

    @DeleteMapping("/deleteCv/{id}")
    public ResponseEntity<Void> deleteCv(@PathVariable Long id) {
        try {
            cvService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getCurrentUserEmail() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof OAuth2User user) {
        return user.getAttribute("email");
    }
    return null;
    }

    @GetMapping("/login")
        public String login() {
        return "accueil";
    }

    @Transactional
    @GetMapping("/auth")
    public String authenticate(Model model) {
        try {
            emailService.deleteEmailsBySource("auto");

            // Lance le script Python
            ProcessBuilder pb = new ProcessBuilder("python", "src/main/python/scriptMail.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Récupère la sortie du script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            process.waitFor();

            // Parse le JSON
            JSONObject jsonResponse = new JSONObject(output.toString());
            String userEmail = jsonResponse.getString("userEmail");
            JSONArray jsonArray = jsonResponse.getJSONArray("mails");

            // Sauvegarde les mails avec l'email de l'utilisateur
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Email email = new Email();
                email.setJob(json.getString("job"));
                email.setCompany(json.getString("company"));
                email.setDate(json.getString("date"));
                email.setEtat(json.getString("etat"));
                email.setUserEmail(userEmail);  // Utilise l'email récupéré de Google
                email.setSource("auto");

                emailService.saveEmail(email);
            }

            // Recharge les données pour la vue
            model.addAttribute("userEmail", userEmail);
            model.addAttribute("data", emailService.getAllEmails());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "Erreur lors de l'authentification : " + e.getMessage());
        }
        return "redirect:/candidatures";
    }

    @GetMapping("/logout")
    public String logout() {
        emailService.deleteEmailsBySource("auto");
        
        String tokenPath = "src/main/python/token.json";
        try {
            File tokenFile = new File(tokenPath);
            if (tokenFile.exists()) tokenFile.delete();
        } catch (Exception e) {
            System.out.println("Erreur lors de la suppression du token : " + e.getMessage());
        }
        return "redirect:/accueil";
    }
}
