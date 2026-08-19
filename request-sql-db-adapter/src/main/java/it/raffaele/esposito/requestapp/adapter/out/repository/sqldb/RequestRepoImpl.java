package it.raffaele.esposito.requestapp.adapter.out.repository.sqldb;

import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity.RequestEntity;
import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity.RequestEntityMapper;
import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.repo.mybatis.RequestMapperRepo;
import it.raffaele.esposito.requestapp.request.domain.Request;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestLookupScope;
import it.raffaele.esposito.requestapp.request.ports.out.persistence.RequestRepo;
import it.raffaele.esposito.requestapp.request.application.exceptions.StaleRequestVersionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RequestRepoImpl implements RequestRepo {

    private final RequestMapperRepo requestMapperRepo;

    private final Clock clock;

    @Override
    public Request findRequestById(String requestId, RequestLookupScope scope) {
        final RequestEntity requestEntity =
                requestMapperRepo.findRequestById(requestId, scope == RequestLookupScope.EXCLUDE_DELETED);
        return requestEntity == null ? null : RequestEntityMapper.toDomain(requestEntity, this.clock);
    }

    @Transactional
    @Override
    public void save(Request request) {
      requestMapperRepo.save(RequestEntityMapper.toEntity(request));
    }

    @Transactional
    @Override
    public void update(Request request) {

        final long expectedVersion = request.getVersion();
        final int updatedRows = requestMapperRepo.update(RequestEntityMapper.toEntity(request));

        if (updatedRows == 0) {
            log.warn("refused to update request {}: it is no longer at version {}", request.getUuid(), expectedVersion);
            throw new StaleRequestVersionException(request.getUuid(), expectedVersion);
        }

        request.writtenAsVersion(expectedVersion + 1);
    }
}
