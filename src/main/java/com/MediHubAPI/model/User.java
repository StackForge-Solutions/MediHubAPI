package com.MediHubAPI.model;


import com.MediHubAPI.model.enums.BloodGroup;
import com.MediHubAPI.model.enums.ReferrerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


import com.MediHubAPI.model.enums.Sex;
import com.MediHubAPI.model.enums.MaritalStatus;
import com.MediHubAPI.model.enums.GovtIdType;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
//@Table(name = "users", uniqueConstraints = {
//        @UniqueConstraint(columnNames = "username"),
//        @UniqueConstraint(columnNames = "email")
//})
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true),
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_hospital_id", columnList = "hospital_id", unique = true),
        @Index(name = "idx_users_mobile_number", columnList = "mobile_number"),
        @Index(name = "idx_users_file_no", columnList = "file_no")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "title", length = 20)
    private String title;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;


    // ============================================================


    @Enumerated(EnumType.STRING)
    @Column(name = "referrer_type")
    private ReferrerType referrerType; // existing enum (if you use one)


    @Column(name = "referrer_name")
    private String referrerName;


    @Column(name = "referrer_number")
    private String referrerNumber;


    @Column(name = "referrer_email")
    private String referrerEmail;


    // Clinical
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulting_doctor_id")
    private User consultingDoctor; // self reference to a DOCTOR user




    @Column(name = "main_complaint", length = 2000)
    private String mainComplaint;


    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group")
    private BloodGroup bloodGroup; // existing enum


    @Column(name = "fathers_name")
    private String fathersName;


    @Column(name = "mothers_name")
    private String mothersName;


    @Column(name = "spouses_name")
    private String spousesName;


    @Column(name = "education")
    private String education;


    @Column(name = "occupation")
    private String occupation;


    @Column(name = "religion")
    private String religion;


    @Column(name = "birth_weight_value")
    private Double birthWeightValue;


    @Column(name = "birth_weight_unit")
    private String birthWeightUnit; // normalized to kg/g/lb


    // Photo
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "photo", columnDefinition = "MEDIUMBLOB") // or BLOB/LONGBLOB
    private byte[] photo;


    @Column(name = "photo_content_type", length = 128)
    private String photoContentType;


    // --- Patient profile kept in the same `users` table ---
    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "file_no")
    private String fileNo;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "alternate_contact")
    private String alternateContact;

    @Column(name = "landline_number")
    private String landlineNumber;

    @Column(name = "dob")
    private LocalDate dateOfBirth;

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex")
    private Sex sex;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "govt_id_type")
    private GovtIdType govtIdType;

    @Column(name = "govt_id_number")
    private String govtIdNumber;

    @Column(name = "other_hospital_ids")
    private String otherHospitalIds;

    @Column(name = "mother_tongue")
    private String motherTongue;

    // Address fields
    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_area")
    private String addressArea;

    @Column(name = "address_city")
    private String addressCity;

    @Column(name = "address_pin", length = 15)
    private String addressPin;

    @Column(name = "address_state")
    private String addressState;

    @Column(name = "address_country")
    private String addressCountry;

    @Column(name = "is_international")
    private Boolean international;

    @Column(name = "needs_attention")
    private Boolean needsAttention;

    @Column(name = "notes", length = 2000)
    private String notes;


}
