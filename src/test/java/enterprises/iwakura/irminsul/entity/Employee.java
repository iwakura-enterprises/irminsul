package enterprises.iwakura.irminsul.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @SequenceGenerator(name = "employee_seq_id", sequenceName = "employee_seq_id", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq_id")
    @Access(AccessType.PROPERTY)
    private Long id;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @Size(max = 512)
    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "companyId")
    private Company company;

    /**
     * Creates a new Employee with the given name.
     *
     * @param name    the name of the employee
     * @param company the company to which the employee belongs
     *
     * @return a new Employee instance
     */
    public static Employee create(String name, Company company) {
        Employee employee = new Employee();
        employee.setName(name);
        employee.setCompany(company);
        return employee;
    }
}
