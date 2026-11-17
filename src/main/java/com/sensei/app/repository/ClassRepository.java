package com.sensei.app.repository;

import com.sensei.app.model.Class;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClassRepository extends MongoRepository<Class, String> {
    boolean existsByTeacherId(String teacherId);

    // studentIds é uma lista: a consulta corresponde a qualquer elemento igual ao ID.
    boolean existsByStudentIds(String studentId);
}
