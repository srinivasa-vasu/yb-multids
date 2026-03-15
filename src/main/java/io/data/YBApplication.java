package io.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class YBApplication {

  static void main(String[] args) {
    SpringApplication.run(YBApplication.class, args);
  }
}
