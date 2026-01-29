/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WorldMap CRUD operations.
 */
public class WorldMapRepository {
    private static final Logger logger = LoggerFactory.getLogger(WorldMapRepository.class);
    private final EntityManagerFactory emf;

    public WorldMapRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Save a WorldMap.
     */
    public void save(WorldMap map) {
        if (emf == null) {
            logger.warn("Database unavailable - skipping save for map {}", map.getName());
            return;
        }
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            if (map.getId() == null) {
                em.persist(map);
            } else {
                em.merge(map);
            }
            em.getTransaction().commit();
            logger.info("Saved WorldMap: {}", map.getName());
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            logger.error("Failed to save WorldMap", e);
            throw new RuntimeException("Failed to save WorldMap", e);
        } finally {
            em.close();
        }
    }

    /**
     * Find WorldMap by ID.
     */
    public Optional<WorldMap> findById(Long id) {
        if (emf == null) return Optional.empty();
        EntityManager em = emf.createEntityManager();
        try {
            WorldMap map = em.find(WorldMap.class, id);
            return Optional.ofNullable(map);
        } finally {
            em.close();
        }
    }

    /**
     * Find WorldMap by name.
     */
    public Optional<WorldMap> findByName(String name) {
        if (emf == null) return Optional.empty();
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<WorldMap> query = em.createQuery(
                    "SELECT m FROM WorldMap m WHERE m.name = :name", WorldMap.class);
            query.setParameter("name", name);
            List<WorldMap> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    /**
     * Find all WorldMaps.
     */
    public List<WorldMap> findAll() {
        if (emf == null) return java.util.Collections.emptyList();
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<WorldMap> query = em.createQuery("SELECT m FROM WorldMap m", WorldMap.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Delete a WorldMap.
     */
    public void delete(Long id) {
        if (emf == null) return;
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            WorldMap map = em.find(WorldMap.class, id);
            if (map != null) {
                em.remove(map);
            }
            em.getTransaction().commit();
            logger.info("Deleted WorldMap with ID: {}", id);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            logger.error("Failed to delete WorldMap", e);
            throw new RuntimeException("Failed to delete WorldMap", e);
        } finally {
            em.close();
        }
    }
}

