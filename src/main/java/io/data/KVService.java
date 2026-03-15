package io.data;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KVService {

  private final KVRepository repository;

  public KVService(KVRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Mono<KeyValue> save(KeyValue resource) {
    return repository.save(resource);
  }

  @Transactional(readOnly = true)
  public Mono<KeyValue> getKey(String id) {
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public Flux<KeyValue> getAllKeys() {
    repository.findAll();
    return repository.findAll();
  }

  @Transactional
  public Mono<Void> deleteKey(String key) {
    return repository.deleteById(key);
  }
}
