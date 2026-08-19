package it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.repo.mybatis;

import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity.RequestEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RequestEventMapperRepo {

    void append(RequestEventEntity requestEventEntity);

    List<RequestEventEntity> findEventsByRequestId(@Param("requestId") String requestId);
}
