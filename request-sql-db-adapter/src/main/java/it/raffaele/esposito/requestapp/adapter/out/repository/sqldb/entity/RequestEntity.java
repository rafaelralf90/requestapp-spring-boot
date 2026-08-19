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
public class RequestEntity {

    private String uuid;

    private String name;

    private String body;

    private String status;

    private String publishedRequestUuid;

    private Instant createdAt;

    private Instant disabledDate;

    private String reason;

    private long version;
}
