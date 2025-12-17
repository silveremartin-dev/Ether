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
 * Repository for H3Cell CRUD operations.
 */
public class H3CellRepository {
    private static final Logger logger = LoggerFactory.getLogger(H3CellRepository.class);
    private final EntityManagerFactory emf;

    public H3CellRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Save a single H3Cell.
     */
    public void save(H3Cell cell) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            if (cell.getId() == null) {
                em.persist(cell);
            } else {
                em.merge(cell);
            }
            em.getTransaction().commit();
            logger.debug("Saved H3Cell: {}", cell.getH3Index());
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            logger.error("Failed to save H3Cell", e);
            throw new RuntimeException("Failed to save H3Cell", e);
        } finally {
            em.close();
        }
    }

    /**
     * Save multiple H3Cells in batch.
     */
    public void saveAll(List<H3Cell> cells) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            int batchSize = 50;
            for (int i = 0; i < cells.size(); i++) {
                H3Cell cell = cells.get(i);
                em.persist(cell);

                if (i > 0 && i % batchSize == 0) {
                    em.flush();
                    em.clear();
                }
            }

            em.getTransaction().commit();
            logger.info("Saved {} H3Cells", cells.size());
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            logger.error("Failed to save H3Cells batch", e);
            throw new RuntimeException("Failed to save H3Cells", e);
        } finally {
            em.close();
        }
    }

    /**
     * Find H3Cell by ID.
     */
    public Optional<H3Cell> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            H3Cell cell = em.find(H3Cell.class, id);
            return Optional.ofNullable(cell);
        } finally {
            em.close();
        }
    }

    /**
     * Find H3Cell by H3 index.
     */
    public Optional<H3Cell> findByH3Index(long h3Index) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<H3Cell> query = em.createQuery(
                    "SELECT c FROM H3Cell c WHERE c.h3Index = :h3Index", H3Cell.class);
            query.setParameter("h3Index", h3Index);
            List<H3Cell> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    /**
     * Find all H3Cells within lat/lng bounds.
     */
    public List<H3Cell> findByBounds(double minLat, double maxLat, double minLng, double maxLng) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<H3Cell> query = em.createQuery(
                    "SELECT c FROM H3Cell c WHERE c.latitude BETWEEN :minLat AND :maxLat " +
                            "AND c.longitude BETWEEN :minLng AND :maxLng",
                    H3Cell.class);
            query.setParameter("minLat", minLat);
            query.setParameter("maxLat", maxLat);
            query.setParameter("minLng", minLng);
            query.setParameter("maxLng", maxLng);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Find all H3Cells (use with caution - can be large!).
     */
    public List<H3Cell> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<H3Cell> query = em.createQuery("SELECT c FROM H3Cell c", H3Cell.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Count total H3Cells.
     */
    public long count() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(c) FROM H3Cell c", Long.class);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    /**
     * Delete all H3Cells (use with caution!).
     */
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM H3Cell").executeUpdate();
            em.getTransaction().commit();
            logger.info("Deleted all H3Cells");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            logger.error("Failed to delete all H3Cells", e);
            throw new RuntimeException("Failed to delete H3Cells", e);
        } finally {
            em.close();
        }
    }
}

