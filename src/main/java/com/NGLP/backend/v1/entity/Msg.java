package com.NGLP.backend.v1.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * كيان الرسالة (Message Entity).
 * يمثل الرسائل الفردية المتبادلة في كل محادثة بين الطالب والـ AI.
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Msg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore // تجنب التكرار الدائري في تسلسل JSON
    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    private String senderType; // "USER" , "AI"
    private Integer videoTimestamp;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime sentAt;
}
