package com.banking.system.model.entities;
import jakarta.persistence.*;

@Entity
@Table(name = "admins")
public class Admin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    @Column(nullable = false)
    private String staffCode;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    public Admin() {}

    public Admin(AppUser appUser, String staffCode, String firstName, String lastName) {
        this.appUser = appUser;
        this.staffCode = staffCode;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void setAppUser(AppUser appUser) { this.appUser = appUser; }
    public void setStaffCode(String staffCode) { this.staffCode = staffCode; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Long getId() { return id; }
    public AppUser getAppUser() { return appUser; }
    public String getStaffCode() { return staffCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; } 
    
}
