package io.data;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KVRepository extends R2dbcRepository<KeyValue, String> {}
