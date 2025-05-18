package enterprises.iwakura.irminsul.repository;

import enterprises.iwakura.irminsul.DatabaseService;
import enterprises.iwakura.irminsul.entity.Company;

public class CompanyRepository extends BaseRepository<Company, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public CompanyRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    protected Class<Company> getEntityClass() {
        return Company.class;
    }

    @Override
    protected boolean hasId(Company company) {
        return company.getId() != null;
    }
}
