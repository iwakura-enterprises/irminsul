package enterprises.iwakura.irminsul;

import enterprises.iwakura.irminsul.entity.Company;
import enterprises.iwakura.irminsul.entity.Employee;
import enterprises.iwakura.irminsul.repository.CompanyRepository;
import enterprises.iwakura.irminsul.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

public class IrminsulDatabaseServiceCrudTest extends DatabaseTest {

    @Test
    public void createUpdateRollbackAndDeleteTest() {
        final var databaseService = new IrminsulDatabaseService(applyTestcontainersConfig(new DatabaseServiceConfiguration()));
        databaseService.initialize(
                Company.class,
                Employee.class
        );

        final var companyRepository = new CompanyRepository(databaseService);
        final var employeeRepository = new EmployeeRepository(databaseService);

        AtomicReference<Long> employeeId = new AtomicReference<>();
        AtomicReference<Long> companyId = new AtomicReference<>();

        // Create
        databaseService.runInThreadTransaction(session -> {
            final var companyToCreate = Company.create("Test Company");
            final var savedCompany = companyRepository.save(companyToCreate);
            final var employeeToCreate = Employee.create("Test Employee", savedCompany);
            final var savedEmployee = employeeRepository.save(employeeToCreate);

            final var fetchedCompany = companyRepository.findById(savedCompany.getId());
            final var fetchedEmployee = employeeRepository.findById(savedEmployee.getId());

            assert fetchedCompany != null;
            assert fetchedCompany.getId().equals(savedCompany.getId());
            assert fetchedCompany.getName().equals(savedCompany.getName());
            assert fetchedEmployee != null;
            assert fetchedEmployee.getId().equals(savedEmployee.getId());
            assert fetchedEmployee.getName().equals(savedEmployee.getName());
            assert fetchedEmployee.getCompany().getId().equals(savedCompany.getId());
            assert fetchedEmployee.getCompany().getName().equals(savedCompany.getName());

            employeeId.set(savedEmployee.getId());
            companyId.set(savedCompany.getId());
            return null;
        });

        // Update
        databaseService.runInThreadTransaction(session -> {
            final var employeeToUpdate = employeeRepository.findById(employeeId.get());
            final var companyToUpdate = companyRepository.findById(companyId.get());

            assert employeeToUpdate != null;
            assert companyToUpdate != null;

            employeeToUpdate.setName("Updated Employee");
            companyToUpdate.setName("Updated Company");

            employeeRepository.save(employeeToUpdate);
            companyRepository.save(companyToUpdate);

            final var updatedEmployee = employeeRepository.findById(employeeId.get());
            final var updatedCompany = companyRepository.findById(companyId.get());

            assert updatedEmployee != null;
            assert updatedCompany != null;

            assert updatedEmployee.getName().equals("Updated Employee");
            assert updatedCompany.getName().equals("Updated Company");

            return null;
        });

        // Update with rollback
        try {
            databaseService.runInThreadTransaction(session -> {
                final var employeeToUpdate = employeeRepository.findById(employeeId.get());
                final var companyToUpdate = companyRepository.findById(companyId.get());

                assert employeeToUpdate != null;
                assert companyToUpdate != null;

                employeeToUpdate.setName("Rollback Employee");
                companyToUpdate.setName("Rollback Company");

                employeeRepository.save(employeeToUpdate);
                companyRepository.save(companyToUpdate);

                throw new RuntimeException("Simulated exception to trigger rollback");
            });
        } catch (Exception exception) {
            // Expected exception, do nothing
        }

        // Check if rollback occurred
        databaseService.runInThreadTransaction(session -> {
            final var employeeAfterRollback = employeeRepository.findById(employeeId.get());
            final var companyAfterRollback = companyRepository.findById(companyId.get());

            assert employeeAfterRollback != null;
            assert companyAfterRollback != null;

            assert !employeeAfterRollback.getName().equals("Rollback Employee");
            assert !companyAfterRollback.getName().equals("Rollback Company");

            return null;
        });

        // Delete
        databaseService.runInThreadTransaction(session -> {
            final var employeeToDelete = employeeRepository.findById(employeeId.get());
            final var companyToDelete = companyRepository.findById(companyId.get());

            assert employeeToDelete != null;
            assert companyToDelete != null;

            employeeRepository.delete(employeeToDelete);
            companyRepository.delete(companyToDelete);

            final var deletedEmployee = employeeRepository.findById(employeeId.get());
            final var deletedCompany = companyRepository.findById(companyId.get());

            assert deletedEmployee == null;
            assert deletedCompany == null;

            return null;
        });

        databaseService.shutdown();
    }
}
