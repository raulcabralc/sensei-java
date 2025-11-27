package com.sensei.app.repository;

import com.sensei.app.model.Class;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClassRepository extends MongoRepository<Class, String> {}
