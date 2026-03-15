package io.data;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "kvinfo")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeyValue {
  @Id UUID key;
  String value;
}
