package it.raffaele.esposito.requestapp.request.ports.out.persistence;

import it.raffaele.esposito.requestapp.request.domain.Request;

public interface RequestRepo {

    Request findRequestById(String requestId, RequestLookupScope scope);

    void save(Request request);

    void update(Request request);
}
