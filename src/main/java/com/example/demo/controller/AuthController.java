package com.example.demo.controller;

import com.example.demo.model.Cv;
import com.example.demo.service.CvService;
import com.example.demo.service.EmailService;
import com.example.demo.model.Email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

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

    private String getCurrentUserEmail() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {
            return oauthUser.getAttribute("email");
        }

        return null;
    }

    @GetMapping("/login")
        public String login() {
        return "accueil";
    }

    @GetMapping("/auth")
    public String authenticate(Model model) {
        try {
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

            // Parse le JSON renvoyé par le script
            JSONObject json = new JSONObject(output.toString());
            String job = json.getString("job");
            String company = json.getString("company");
            String date = json.getString("date");
            String etat = json.getString("etat");

            // Enregistre dans la base via le service
            Email email = new Email();
            email.setJob(job);
            email.setCompany(company);
            email.setDate(date);
            email.setEtat(etat);
            emailService.saveEmail(email);

            // Recharge les données pour la vue
            model.addAttribute("data", emailService.getAllEmails());
            model.addAttribute("message", "Mail récupéré et enregistré !");

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "Erreur lors de l'authentification : " + e.getMessage());
        }

        return "candidatures";
    }

    @GetMapping("/logout")
    public String logout() {
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
