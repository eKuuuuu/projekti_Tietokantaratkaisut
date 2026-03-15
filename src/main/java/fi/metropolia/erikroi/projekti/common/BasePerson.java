package fi.metropolia.erikroi.projekti.common;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class BasePerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    // Getters and Setters
    public Long getId() { return id; } // Added Getter

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}