package io.data;

import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/kvinfo")
public class KVController {

  private final KVService kvService;

  public KVController(KVService kvService) {
    this.kvService = kvService;
  }

  @PostMapping
  public KeyValue saveKey(@RequestBody KeyValue info) {
    return kvService.save(info);
  }

  @PutMapping
  public KeyValue updateKey(@RequestBody KeyValue info) {
    return kvService.save(info);
  }

  @GetMapping("/{key}")
  public Optional<KeyValue> getKey(@PathVariable String key) {
    return kvService.getKey(key);
  }

  @GetMapping
  public Iterable<KeyValue> getAllKeys() {
    return kvService.getAllKeys();
  }

  @DeleteMapping("/{key}")
  public void deleteKey(@PathVariable String key) {
    kvService.deleteKey(key);
  }
}
