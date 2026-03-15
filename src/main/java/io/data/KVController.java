package io.data;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/kvinfo")
public class KVController {

  private final KVService kvService;
  private final RetryTemplate retryTemplate;

  public KVController(KVService kvService, RetryTemplate retryTemplate) {
    this.kvService = kvService;
	  this.retryTemplate = retryTemplate;
  }

  @PostMapping
  public Mono<KeyValue> saveKey(@RequestBody KeyValue info) {
    return kvService.save(info);
  }

  @PutMapping
  public Mono<KeyValue> updateKey(@RequestBody KeyValue info) {
    return kvService.save(info);
  }

  @GetMapping("/{key}")
  public Mono<KeyValue> getKey(@PathVariable String key) {
    return kvService.getKey(key);
  }

  @GetMapping
  public Flux<KeyValue> getAllKeys() throws Throwable {
    return retryTemplate.execute(kvService::getAllKeys);
  }

  @DeleteMapping("/{key}")
  public Mono<Void> deleteKey(@PathVariable String key) {
    return kvService.deleteKey(key);
  }
}
