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

            final var fetchedCompany = companyRepository.findById(savedCompany.getId()).orElseThrow();
            final var fetchedEmployee = employeeRepository.findById(savedEmployee.getId()).orElseThrow();

            assert fetchedCompany.getId().equals(savedCompany.getId());
            assert fetchedCompany.getName().equals(savedCompany.getName());

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
            final var employeeToUpdate = employeeRepository.findById(employeeId.get()).orElseThrow();
            final var companyToUpdate = companyRepository.findById(companyId.get()).orElseThrow();

            employeeToUpdate.setName("Updated Employee");
            companyToUpdate.setName("Updated Company");

            employeeRepository.save(employeeToUpdate);
            companyRepository.save(companyToUpdate);

            final var updatedEmployee = employeeRepository.findById(employeeId.get()).orElseThrow();
            final var updatedCompany = companyRepository.findById(companyId.get()).orElseThrow();

            assert updatedEmployee.getName().equals("Updated Employee");
            assert updatedCompany.getName().equals("Updated Company");

            return null;
        });

        // Update with rollback
        try {
            databaseService.runInThreadTransaction(session -> {
                final var employeeToUpdate = employeeRepository.findById(employeeId.get()).orElseThrow();
                final var companyToUpdate = companyRepository.findById(companyId.get()).orElseThrow();

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
            final var employeeAfterRollback = employeeRepository.findById(employeeId.get()).orElseThrow();
            final var companyAfterRollback = companyRepository.findById(companyId.get()).orElseThrow();

            assert !employeeAfterRollback.getName().equals("Rollback Employee");
            assert !companyAfterRollback.getName().equals("Rollback Company");

            return null;
        });

        // Delete
        databaseService.runInThreadTransaction(session -> {
            final var employeeToDelete = employeeRepository.findById(employeeId.get()).orElseThrow();
            final var companyToDelete = companyRepository.findById(companyId.get()).orElseThrow();

            employeeRepository.delete(employeeToDelete);
            companyRepository.delete(companyToDelete);

            final var deletedEmployee = employeeRepository.findById(employeeId.get()).isEmpty();
            final var deletedCompany = companyRepository.findById(companyId.get()).isEmpty();

            assert deletedEmployee;
            assert deletedCompany;

            return null;
        });

        // Create multiple companies
        for (int i = 0; i < 10; i++) {
            final int finalIndex = i;
            databaseService.runInThreadTransaction(session -> {
                final var companyToCreate = Company.create("Company " + finalIndex);
                companyRepository.save(companyToCreate);
                return null;
            });
        }

        // Find all companies
        databaseService.runInThreadTransaction(session -> {
            final var companies = companyRepository.findAll();
            assert companies.size() == 10 : "Expected 10 companies, found: " + companies.size();
            return null;
        });

        databaseService.shutdown();
    }
}
