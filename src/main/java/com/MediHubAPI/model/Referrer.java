//package com.MediHubAPI.model;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import lombok.Data;
//import jakarta.persistence.*;
//
//@Entity
//@Table(name = "referrers")
//@Data
//public class Referrer {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String referrerType;
//    private String referrerName;
//    private String referrerNumber;
//    private String referrerEmail;
//
//    private String consultingDept;
//    private String consultingDoctor;
//
//    private String mainComplaint;
//
//    @OneToOne
//    @JoinColumn(name = "patient_id")
//    private Patient patient;
//}
