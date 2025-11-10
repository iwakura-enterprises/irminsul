package enterprises.iwakura.irminsul.repository;

import enterprises.iwakura.irminsul.IrminsulDatabaseService;
import enterprises.iwakura.irminsul.util.TriFunction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;

/**
 * Base repository class for handling database operations within one entity.
 *
 * @param <TEntity> the entity type
 * @param <TId>     the ID type
 */
@Getter
public abstract class BaseRepository<TEntity, TId> {

    /**
     * The database service used for database operations.
     */
    protected final IrminsulDatabaseService databaseService;

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public BaseRepository(IrminsulDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Gets the entity class type.
     *
     * @return the entity class type
     */
    protected abstract Class<TEntity> getEntityClass();

    /**
     * Determines if the entity has an ID. Used when using {@link #save(TEntity)} method to determine if should be inserted or updated.
     *
     * @param entity the entity to check
     *
     * @return true if the entity has an ID, false otherwise
     */
    protected abstract boolean hasId(TEntity entity);

    /**
     * Finds all entities in the database. Calls {@link #findByCriteria(TriFunction)} with a conjunction predicate, which matches all entities.
     *
     * @return the list of found entities
     */
    public List<TEntity> findAll() {
        return findByCriteria((root, query, cb) -> cb.conjunction());
    }

    /**
     * Finds an entity by its ID.
     *
     * @param id the ID of the entity to find
     *
     * @return the found entity, or null if not found
     */
    public Optional<TEntity> findById(TId id) {
        return Optional.ofNullable(databaseService.runInThreadTransaction(session -> {
            return session.find(getEntityClass(), id, LockModeType.NONE);
        }));
    }

    /**
     * Finds all entities by criteria.
     *
     * @param criteriaBuilderConsumer the criteria builder consumer
     *
     * @return the list of found entities
     */
    public List<TEntity> findByCriteria(TriFunction<Root<TEntity>, CriteriaQuery<TEntity>, CriteriaBuilder, Predicate> criteriaBuilderConsumer) {
        return databaseService.runInThreadTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(getEntityClass());
            var root = query.from(getEntityClass());
            query.select(root);
            var predicate = criteriaBuilderConsumer.apply(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
            return session.createQuery(query).getResultList();
        });
    }

    /**
     * Saves the entity to the database. If the entity has an ID, it will be updated. Otherwise, it will be inserted.
     * This uses the {@link #hasId(TEntity)} method to determine if the entity has an ID.
     *
     * @param entity the entity to save
     *
     * @return the saved entity
     */
    public TEntity save(TEntity entity) {
        if (hasId(entity)) {
            return update(entity);
        } else {
            return insert(entity);
        }
    }

    /**
     * Saves a list of entities to the database. Each entity will be either inserted or updated based on its ID.
     * This uses the {@link #hasId(TEntity)} method to determine if the entity has an ID.
     *
     * @param entities the list of entities to save
     *
     * @return the list of saved entities
     */
    public List<TEntity> saveAll(List<TEntity> entities) {
        if (entities.isEmpty()) {
            return entities;
        }

        return databaseService.runInThreadTransaction(session -> {
            return entities.stream().map(this::save).toList();
        });
    }

    /**
     * Inserts a new entity to the database.
     *
     * @param entity the entity to save
     *
     * @return the saved entity
     */
    public TEntity insert(TEntity entity) {
        return databaseService.runInThreadTransaction(session -> {
            session.persist(entity);
            return entity;
        });
    }

    /**
     * Inserts a list of entities to the database.
     *
     * @param entities the list of entities to save
     *
     * @return the list of saved entities
     */
    public List<TEntity> insertAll(List<TEntity> entities) {
        return databaseService.runInThreadTransaction(session -> {
            for (TEntity entity : entities) {
                session.persist(entity);
            }
            return entities;
        });
    }

    /**
     * Updates an existing entity in the database.
     *
     * @param entity the entity to update
     *
     * @return the updated entity
     */
    public TEntity update(TEntity entity) {
        return databaseService.runInThreadTransaction(session -> {
            session.merge(entity);
            return entity;
        });
    }

    /**
     * Updates a list of entities in the database.
     *
     * @param entities the list of entities to update
     *
     * @return the list of updated entities
     */
    public List<TEntity> updateAll(List<TEntity> entities) {
        return databaseService.runInThreadTransaction(session -> {
            for (TEntity entity : entities) {
                session.merge(entity);
            }
            return entities;
        });
    }

    /**
     * Deletes an entity from the database.
     *
     * @param entity the entity to delete
     */
    public void delete(TEntity entity) {
        databaseService.runInThreadTransaction(session -> {
            session.remove(entity);
            return null;
        });
    }

    /**
     * Deletes a list of entities from the database.
     *
     * @param entities the list of entities to delete
     */
    public void deleteAll(List<TEntity> entities) {
        databaseService.runInThreadTransaction(session -> {
            for (TEntity entity : entities) {
                session.remove(entity);
            }
            return null;
        });
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id the ID of the entity to delete
     */
    public void deleteById(TId id) {
        databaseService.runInThreadTransaction(session -> {
            TEntity entity = session.find(getEntityClass(), id);
            if (entity != null) {
                session.remove(entity);
            }
            return null;
        });
    }
}
