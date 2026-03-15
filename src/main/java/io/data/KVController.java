package io.data;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@RestController
@RequestMapping("/v1/kvinfo")
public class KVController {

  private final KVService kvService;
  private final Retry retrySpec;

  public KVController(KVService kvService, Retry retrySpec) {
    this.kvService = kvService;
    this.retrySpec = retrySpec;
  }

  @PostMapping
  public Mono<KeyValue> saveKey(@RequestBody KeyValue info) {
    return kvService.save(info).retryWhen(retrySpec);
  }

  @PutMapping
  public Mono<KeyValue> updateKey(@RequestBody KeyValue info) {
    return kvService.save(info).retryWhen(retrySpec);
  }

  @GetMapping("/{key}")
  public Mono<KeyValue> getKey(@PathVariable String key) {
    return kvService.getKey(key).retryWhen(retrySpec);
  }

  @GetMapping
  public Flux<KeyValue> getAllKeys() throws Throwable {
    return kvService.getAllKeys().retryWhen(retrySpec);
  }

  @DeleteMapping("/{key}")
  public Mono<Void> deleteKey(@PathVariable String key) {
    return kvService.deleteKey(key).retryWhen(retrySpec);
  }
}
