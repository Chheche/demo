package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "emails")
@Data
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String job;
    private String company;
    private String date;
    private String etat;

    private String userEmail;


    public Email() {}

    public Email(String job, String company, String date, String etat) {
        this.job = job;
        this.company = company;
        this.date = date;
        this.etat = etat;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }
}
