package fi.metropolia.erikroi.projekti.suppliers;

import fi.metropolia.erikroi.projekti.common.BasePerson;
import jakarta.persistence.*;

@Entity
@Table(name = "suppliers")
public class Supplier extends BasePerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "contact_name")
    private String contactName;

    @OneToOne(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SupplierAddress address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public SupplierAddress getAddress() {
        return address;
    }

    public void setAddress(SupplierAddress address) {
        this.address = address;
    }
}