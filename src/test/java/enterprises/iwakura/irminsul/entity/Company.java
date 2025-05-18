package enterprises.iwakura.irminsul.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "company")
public class Company {

    @Id
    @SequenceGenerator(name = "company_seq_id", sequenceName = "company_seq_id", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_seq_id")
    @Access(AccessType.PROPERTY)
    private Long id;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @Size(max = 512)
    @Column(nullable = false)
    private String name;

    @OneToMany
    private List<Employee> employees = new ArrayList<>();

    /**
     * Creates a new Company with the given name.
     * @param testCompany the name of the company
     * @return a new Company instance
     */
    public static Company create(String testCompany) {
        Company company = new Company();
        company.setName(testCompany);
        return company;
    }
}
