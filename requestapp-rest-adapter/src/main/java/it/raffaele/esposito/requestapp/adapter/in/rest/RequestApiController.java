package it.raffaele.esposito.requestapp.adapter.in.rest;

import it.raffaele.esposito.requestapp.adapter.in.rest.model.BodyPayload;
import it.raffaele.esposito.requestapp.adapter.in.rest.model.NewRequestPayload;
import it.raffaele.esposito.requestapp.adapter.in.rest.model.ReasonPayload;
import it.raffaele.esposito.requestapp.adapter.in.rest.model.RequestPayload;
import it.raffaele.esposito.requestapp.request.ports.in.RequestServicePort;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.NewRequestCommand;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.RequestUpdateStatusWithReason;
import it.raffaele.esposito.requestapp.request.ports.in.dto.in.RequestUpdateBodyCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/")
public class RequestApiController {

    private static final String VERSION = "version";

    private final RequestServicePort requestServicePort;

    @GetMapping(value = "requests/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RequestPayload getRequest(@PathVariable("id") String requestId) {
        return RequestPayload.from(requestServicePort.getRequestById(requestId));
    }

    @PostMapping(value = "requests", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String addRequest(@RequestBody(required = false) NewRequestPayload newRequestPayload) {
        final NewRequestCommand command = newRequestPayload == null
                ? new NewRequestCommand()
                : new NewRequestCommand(newRequestPayload.getName(), newRequestPayload.getBody());
        return requestServicePort.createRequest(command);
    }

    @PutMapping(value = "requests/{id}/body", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateBody(@PathVariable("id") String requestId,
                           @RequestBody(required = false) BodyPayload bodyPayload,
                           @RequestParam(value = VERSION) long version) {
        final RequestUpdateBodyCommand command =
                new RequestUpdateBodyCommand(requestId, bodyPayload == null ? null : bodyPayload.getBody());
        requestServicePort.updateRequestBody(command, version);
    }

    @PostMapping(value = "requests/{id}/verify")
    public void verify(@PathVariable("id") String requestId,
                       @RequestParam(value = VERSION) long version) {
        requestServicePort.verify(requestId, version);
    }

    @PostMapping(value = "requests/{id}/accept")
    public void accept(@PathVariable("id") String requestId,
                       @RequestParam(value = VERSION) long version) {
        requestServicePort.accept(requestId, version);
    }

    @PostMapping(value = "requests/{id}/publish", produces = MediaType.TEXT_PLAIN_VALUE)
    public String publish(@PathVariable("id") String requestId,
                          @RequestParam(value = VERSION) long version) {
        return requestServicePort.publish(requestId, version);
    }

    @PostMapping(value = "requests/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void reject(@PathVariable("id") String requestId,
                       @RequestBody(required = false) ReasonPayload reasonPayload,
                       @RequestParam(value = VERSION) long version) {
        final RequestUpdateStatusWithReason command = new RequestUpdateStatusWithReason(requestId, reasonOf(reasonPayload));
        requestServicePort.reject(command, version);
    }

    @PostMapping(value = "requests/{id}/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void delete(@PathVariable("id") String requestId,
                       @RequestBody(required = false) ReasonPayload reasonPayload,
                       @RequestParam(value = VERSION) long version) {
        final RequestUpdateStatusWithReason command = new RequestUpdateStatusWithReason(requestId, reasonOf(reasonPayload));
        requestServicePort.delete(command, version);
    }

    private static String reasonOf(ReasonPayload reasonPayload) {
        return reasonPayload == null ? null : reasonPayload.getReason();
    }
}
