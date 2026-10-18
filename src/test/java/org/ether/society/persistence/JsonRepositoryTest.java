/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link JsonRepository}.
 * Verifies generic JSON entity persistence, querying, serialization, and deletion.
 */
class JsonRepositoryTest {

    public static class SampleEntity {
        private String id;
        private int value;

        public SampleEntity() {
        }

        public SampleEntity(String id, int value) {
            this.id = id;
            this.value = value;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SampleEntity that = (SampleEntity) o;
            return value == that.value && Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }
    }

    private static class SampleEntityRepository extends JsonRepository<SampleEntity> {
        public SampleEntityRepository(Path targetFile) {
            super(targetFile, SampleEntity.class);
        }
    }

    @Test
    @DisplayName("JsonRepository initializes empty storage and supports CRUD operations")
    void testJsonRepositoryCRUD(@TempDir Path tempDir) {
        Path repoFile = tempDir.resolve("sample_entities.json");
        SampleEntityRepository repo = new SampleEntityRepository(repoFile);

        assertTrue(repo.findAll().isEmpty(), "Initial repository should be empty");

        SampleEntity e1 = new SampleEntity("A", 100);
        SampleEntity e2 = new SampleEntity("B", 200);

        repo.save(e1);
        repo.save(e2);

        List<SampleEntity> all = repo.findAll();
        assertEquals(2, all.size());
        assertEquals("A", all.get(0).getId());
        assertEquals(100, all.get(0).getValue());
        assertEquals("B", all.get(1).getId());
        assertEquals(200, all.get(1).getValue());

        repo.delete(e1);
        List<SampleEntity> afterDelete = repo.findAll();
        assertEquals(1, afterDelete.size());
        assertEquals("B", afterDelete.get(0).getId());
    }
}
