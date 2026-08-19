package it.raffaele.esposito.requestapp.adapter.out.repository.sqldb;

import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity.RequestEventEntityMapper;
import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.repo.mybatis.RequestEventMapperRepo;
import it.raffaele.esposito.requestapp.request.domain.event.RequestEvent;
import it.raffaele.esposito.requestapp.request.ports.out.outbox.RequestEventOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RequestEventOutboxRepoImpl implements RequestEventOutbox {

    private final RequestEventMapperRepo requestEventMapperRepo;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void append(List<RequestEvent> events) {

        events.forEach(event -> requestEventMapperRepo.append(RequestEventEntityMapper.toEntity(event)));
    }
}
