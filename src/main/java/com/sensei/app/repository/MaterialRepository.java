package com.sensei.app.repository;

import com.sensei.app.enums.MaterialStatus;
import com.sensei.app.model.Material;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MaterialRepository extends MongoRepository<Material, String> {
    List<Material> findByClassId(String classId);

    List<Material> findByClassIdAndStatus(String classId, MaterialStatus status);

    boolean existsByClassId(String classId);

    boolean existsByTeacherId(String teacherId);
}
