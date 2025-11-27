package com.sensei.app.repository;

import com.sensei.app.model.Material;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MaterialRepository extends MongoRepository<Material, String> {}
