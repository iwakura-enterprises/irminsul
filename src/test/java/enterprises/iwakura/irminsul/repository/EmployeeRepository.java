package enterprises.iwakura.irminsul.repository;

import enterprises.iwakura.irminsul.IrminsulDatabaseService;
import enterprises.iwakura.irminsul.entity.Employee;

public class EmployeeRepository extends BaseRepository<Employee, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public EmployeeRepository(IrminsulDatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    protected Class<Employee> getEntityClass() {
        return Employee.class;
    }

    @Override
    protected boolean hasId(Employee employee) {
        return employee.getId() != null;
    }
}
