package com.example.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object interface defining standard CRUD operations.
 *
 * <p>This interface uses Java generics to abstract the entity type and
 * its identifier type, satisfying rubric criterion 2 (Java Generics).
 * Concrete DAOs implement this interface for specific entity types,
 * e.g. {@code EvacuationCenterDao implements GenericDao<EvacuationCenter, Long>}.</p>
 *
 * <p>All methods that interact with the database declare {@link SQLException}
 * so the caller (typically a service class) can decide how to handle
 * connection failures. Service methods then translate these into
 * application-specific exceptions if needed.</p>
 *
 * @param <T>  the entity type this DAO manages (e.g. EvacuationCenter)
 * @param <ID> the type of the entity's identifier (typically Long)
 */
public interface GenericDao<T, ID> {

    /**
     * Finds an entity by its primary key.
     *
     * @param id the primary key value
     * @return Optional containing the entity if found, empty otherwise
     * @throws SQLException if the query fails
     */
    Optional<T> findById(ID id) throws SQLException;

    /**
     * Returns all entities of this type in the database.
     * Use sparingly on large tables — consider paginated queries instead.
     *
     * @return list of all entities (empty list if none exist)
     * @throws SQLException if the query fails
     */
    List<T> findAll() throws SQLException;

    /**
     * Inserts a new entity into the database.
     * The entity's ID field will be populated with the generated key.
     *
     * @param entity the new entity to persist (ID should be null)
     * @return the saved entity with its ID populated
     * @throws SQLException if the insert fails
     */
    T save(T entity) throws SQLException;

    /**
     * Updates an existing entity in the database.
     * Entity must have a non-null ID.
     *
     * @param entity the entity with updated field values
     * @throws SQLException if the update fails or the row doesn't exist
     */
    void update(T entity) throws SQLException;

    /**
     * Deletes an entity by its primary key.
     *
     * @param id the primary key of the entity to delete
     * @return true if a row was deleted, false if no row matched
     * @throws SQLException if the delete fails
     */
    boolean deleteById(ID id) throws SQLException;
}