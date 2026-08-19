package it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestEventEntity {

    private Long id;

    private String requestUuid;

    private String eventType;

    private String fromStatus;

    private String toStatus;

    private String reason;

    private String publishedRequestUuid;

    private long decidedOnVersion;

    private Instant occurredAt;

    private Instant publishedAt;
}
