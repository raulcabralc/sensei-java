package com.sensei.app.repository;

import com.sensei.app.model.Material;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MaterialRepository extends MongoRepository<Material, String> {
    List<Material> findByClassId(String classId);
}
