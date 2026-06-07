package com.NGLP.backend.v1.entity;
import jakarta.persistence.*;
import lombok.*;



@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // ROLE_STUDENT, ROLE_ADMIN
    private String description;
}
