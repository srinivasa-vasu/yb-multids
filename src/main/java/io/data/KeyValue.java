package io.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "kvinfo")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeyValue {
  @Id @UuidGenerator UUID key;
  String value;
}
