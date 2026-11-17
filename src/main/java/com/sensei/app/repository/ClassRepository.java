package com.sensei.app.repository;

import com.sensei.app.model.Class;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ClassRepository extends MongoRepository<Class, String> {
    List<Class> findByTeacherId(String teacherId);

    // studentIds é uma lista: a consulta corresponde a qualquer elemento igual ao ID.
    List<Class> findByStudentIds(String studentId);

    boolean existsByTeacherId(String teacherId);

    boolean existsByStudentIds(String studentId);
}
