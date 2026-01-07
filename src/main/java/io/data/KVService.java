package io.data;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KVService {

  private final KVRepository repository;

  public KVService(KVRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public KeyValue save(KeyValue resource) {
    return repository.save(resource);
  }

  @Transactional(readOnly = true)
  public Optional<KeyValue> getKey(String id) {
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  @ReadOnly
  public Iterable<KeyValue> getAllKeys() {
    return repository.findAll();
  }

  @Transactional
  public void deleteKey(String key) {
    repository.deleteById(key);
  }
}
