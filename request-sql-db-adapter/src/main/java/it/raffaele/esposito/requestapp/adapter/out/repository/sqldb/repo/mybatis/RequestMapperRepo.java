package it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.repo.mybatis;

import it.raffaele.esposito.requestapp.adapter.out.repository.sqldb.entity.RequestEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RequestMapperRepo {

    RequestEntity findRequestById(@Param("requestId") String requestId, @Param("excludeDeleted") boolean excludeDeleted);

    void save(RequestEntity requestEntity);

    int update(RequestEntity requestEntity);
}
