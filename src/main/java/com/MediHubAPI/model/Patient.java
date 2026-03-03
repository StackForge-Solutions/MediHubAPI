package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.MediHubAPI.model.enums.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Link to generic User (keeps auth, roles, username/email, etc.)
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Auto-generated hospital-specific ID like PAT-2025-0043
     */
    @Column(name = "hospital_id", unique = true, nullable = false, length = 32)
    private String hospitalId;

    /**
     * Demographics
     */
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false, length = 15)
    private String mobileNumber;
    private String alternateContact;
    private String landlineNumber;

    @Column(nullable = false)
    private LocalDate dateOfBirth;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    private MaritalStatus maritalStatus;

    private String email;

    @Enumerated(EnumType.STRING)
    private GovtIdType govtIdType;

    private String govtIdNumber;

    private String otherHospitalIds;
    private String motherTongue;

    /**
     * Referrer Info
     */
    @Enumerated(EnumType.STRING)
    private ReferrerType referrerType;

    private String referrerName;
    private String referrerNumber;
    private String referrerEmail;

    /**
     * Consulting doctor (FK to User with DOCTOR role)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulting_doctor_id")
    private User consultingDoctor;

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private Department department;

    //  Instead of Department, use Specialization
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;

    @Column(nullable = false, length = 2000)
    private String mainComplaint;

    /**
     * Additional Details
     */
    @Enumerated(EnumType.STRING)
    private BloodGroup bloodGroup;

    private String fathersName;
    private String mothersName;
    private String spousesName;

    @Enumerated(EnumType.STRING)
    private Education education;

    @Enumerated(EnumType.STRING)
    private Occupation occupation;

    @Enumerated(EnumType.STRING)
    private Religion religion;

    /**
     * Birth weight with unit
     */
    private Double birthWeightValue;      // e.g., 3.2
    private String birthWeightUnit;       // "kg", "g", "lb"

    /**
     * Photo (store as blob)
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] photo;

    private String photoContentType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

