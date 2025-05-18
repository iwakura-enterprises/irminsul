package enterprises.iwakura.irminsul.repository;

import enterprises.iwakura.irminsul.DatabaseService;
import enterprises.iwakura.irminsul.util.TriFunction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;

/**
 * Repository extension. This interface provides additional methods for repositories to interact with the database. These methods does not
 * really fit into the main {@link BaseRepository} class due to their specific nature.
 *
 * @param <TEntity> the entity type
 */
public interface RepositoryExtension<TEntity> {

    DatabaseService getDatabaseService();

    /**
     * Gets the entity class type.
     *
     * @return the entity class type
     */
    Class<TEntity> getEntityClass();

    /**
     * Finds all entities by criteria with pagination.
     *
     * @param pageIndex               the index of the page to retrieve
     * @param pageSize                the size of the page
     * @param criteriaBuilderConsumer the criteria builder consumer
     *
     * @return the list of found entities
     */
    default List<TEntity> findByCriteriaPaged(int pageIndex, int pageSize, TriFunction<Root<TEntity>, CriteriaQuery<TEntity>, CriteriaBuilder, Predicate> criteriaBuilderConsumer) {
        return getDatabaseService().runInThreadTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(getEntityClass());
            var root = query.from(getEntityClass());
            query.select(root);
            var predicate = criteriaBuilderConsumer.apply(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
            return session.createQuery(query)
                          .setFirstResult(pageIndex * pageSize)
                          .setMaxResults(pageSize)
                          .getResultList();
        });
    }

    /**
     * Counts the number of entities by criteria.
     *
     * @param criteriaBuilderConsumer the criteria builder consumer
     *
     * @return the count of entities
     */
    default long countByCriteria(TriFunction<Root<TEntity>, CriteriaQuery<Long>, CriteriaBuilder, Predicate> criteriaBuilderConsumer) {
        return getDatabaseService().runInThreadTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(Long.class);
            var root = query.from(getEntityClass());
            query.select(cb.count(root));
            var predicate = criteriaBuilderConsumer.apply(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
            return session.createQuery(query).getSingleResult();
        });
    }

    /**
     * Calculates the sum of a field by criteria.
     *
     * @param criteriaBuilderConsumer the criteria builder consumer
     * @param fieldName               the name of the field to sum
     *
     * @return the sum of the field
     */
    default double sumByCriteria(TriFunction<Root<TEntity>, CriteriaQuery<Long>, CriteriaBuilder, Predicate> criteriaBuilderConsumer, String fieldName) {
        return getDatabaseService().runInThreadTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(Long.class);
            var root = query.from(getEntityClass());
            query.select(cb.sum(root.get(fieldName)));
            var predicate = criteriaBuilderConsumer.apply(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
            return session.createQuery(query).getSingleResult().doubleValue();
        });
    }

    /**
     * Calculates the maximum value of a field by criteria.
     *
     * @param criteriaBuilderConsumer the criteria builder consumer
     * @param fieldName               the name of the field to find the maximum value
     *
     * @return the maximum value of the field
     */
    default double maxByCriteria(TriFunction<Root<TEntity>, CriteriaQuery<Long>, CriteriaBuilder, Predicate> criteriaBuilderConsumer, String fieldName) {
        return getDatabaseService().runInThreadTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(Long.class);
            var root = query.from(getEntityClass());
            query.select(cb.max(root.get(fieldName)));
            var predicate = criteriaBuilderConsumer.apply(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
            return Optional.ofNullable(session.createQuery(query).getSingleResult()).map(Long::doubleValue).orElse(0.0);
        });
    }

    /**
     * Gets all long values by criteria.
     *
     * @param criteriaBuilderConsumer the criteria builder consumer
     * @param fieldName               the name of the field to get values from
     *
     * @return the list of long values
     */
    default List<Long> getAllLongValuesByCriteria(TriFunction<Root<TEntity>, CriteriaQuery<Long>, CriteriaBuilder, Predicate> criteriaBuilderConsumer, String fieldName) {
        return getDatabaseService().runInThreadTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(Long.class);
            var root = query.from(getEntityClass());
            query.select(root.get(fieldName));
            var predicate = criteriaBuilderConsumer.apply(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
            return session.createQuery(query).getResultList();
        });
    }

}
