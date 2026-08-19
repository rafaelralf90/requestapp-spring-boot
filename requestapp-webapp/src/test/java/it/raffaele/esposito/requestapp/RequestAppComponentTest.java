package it.raffaele.esposito.requestapp;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import it.raffaele.esposito.requestapp.request.domain.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestAppComponentTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/requestapp";
    }

    @Test
    void when_createNewRequest_then_requestCanBeCorrectlyRetrieved() {
        final String uuid = addRequest("first request", "the body of the request");

        given()
                .when().get("/api/requests/" + uuid)
                .then().statusCode(200)
                .body("uuid", equalTo(uuid))
                .body("name", equalTo("first request"))
                .body("body", equalTo("the body of the request"))
                .body("status", equalTo(RequestStatus.CREATED.name()))
                .body("version", equalTo(0))
                .body("createdAt", notNullValue());
    }

    @Test
    void when_requestDoesNotExist_then_notFoundIsReturned() {
        given()
                .when().get("/api/requests/does-not-exist")
                .then().statusCode(404)
                .body("[0].engMessage", equalTo("request not found"));
    }

    @Test
    void when_aRequestIsVerifiedAndAccepted_then_itCanBePublished() {
        final String uuid = addRequest("first request", "the body of the request");

        transition(uuid, "verify", versionOf(uuid))
                .then().statusCode(200)
                .body(emptyString());

        given()
                .when().get("/api/requests/" + uuid)
                .then().statusCode(200)
                .body("status", equalTo(RequestStatus.VERIFIED.name()))
                .body("version", equalTo(1));

        transition(uuid, "accept", versionOf(uuid))
                .then().statusCode(200)
                .body(emptyString());

        final Response published = transition(uuid, "publish", versionOf(uuid));
        published.then().statusCode(200);
        final String publishedRequestUuid = published.asString();
        assertFalse(publishedRequestUuid.isBlank());

        given()
                .when().get("/api/requests/" + uuid)
                .then().statusCode(200)
                .body("status", equalTo(RequestStatus.PUBLISHED.name()))
                .body("publishedRequestUuid", equalTo(publishedRequestUuid))
                .body("version", equalTo(3));
    }

    @Test
    void when_aVerifiedRequestIsRejected_then_theReasonIsKept() {
        final String uuid = addRequest("first request", "the body of the request");
        transition(uuid, "verify", 0L).then().statusCode(200);

        withReason(uuid, "reject", "does not meet the guidelines", 1L)
                .then().statusCode(200)
                .body(emptyString());

        given()
                .when().get("/api/requests/" + uuid)
                .then().statusCode(200)
                .body("status", equalTo(RequestStatus.REJECTED.name()))
                .body("reason", equalTo("does not meet the guidelines"));
    }

    @Test
    void when_aRequestIsDeleted_then_itIsNoLongerRetrievable() {
        final String uuid = addRequest("first request", "the body of the request");

        withReason(uuid, "delete", "no longer needed", 0L)
                .then().statusCode(200)
                .body(emptyString());

        given()
                .when().get("/api/requests/" + uuid)
                .then().statusCode(404);
    }

    @Test
    void when_theBodyIsUpdated_then_theNewBodyIsStored() {
        final String uuid = addRequest("first request", "the original body");

        given()
                .contentType("application/json")
                .body("{\"body\":\"a rewritten body\"}")
                .when().put("/api/requests/" + uuid + "/body?version=0")
                .then().statusCode(200)
                .body(emptyString());

        given()
                .when().get("/api/requests/" + uuid)
                .then().statusCode(200)
                .body("body", equalTo("a rewritten body"))
                .body("version", equalTo(1));
    }

    @Test
    void when_aTransitionIsNotAllowedFromTheCurrentStatus_then_conflictIsReturned() {
        final String uuid = addRequest("first request", "the body of the request");

        transition(uuid, "publish", 0L)
                .then().statusCode(409)
                .body("[0].code", equalTo(3))
                .body("[0].fieldName", equalTo("status"));
    }

    @Test
    void when_aClosingTransitionCarriesNoReason_then_badRequestIsReturned() {
        final String uuid = addRequest("first request", "the body of the request");

        given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/requests/" + uuid + "/delete?version=0")
                .then().statusCode(400)
                .body("[0].code", equalTo(5))
                .body("[0].fieldName", equalTo("reason"));
    }

    @Test
    void when_aClosingTransitionCarriesNoBodyAtAll_then_unsupportedMediaTypeIsReturned() {
        final String uuid = addRequest("first request", "the body of the request");

        given()
                .when().post("/api/requests/" + uuid + "/delete?version=0")
                .then().statusCode(415)
                .body("[0].code", equalTo(8));
    }

    @Test
    void when_theNamedVersionHasMovedOn_then_conflictIsReturned() {
        final String uuid = addRequest("first request", "the body of the request");
        final long versionEveryoneRead = versionOf(uuid);

        transition(uuid, "verify", versionEveryoneRead).then().statusCode(200);

        transition(uuid, "accept", versionEveryoneRead)
                .then().statusCode(409)
                .body("[0].code", equalTo(7))
                .body("[0].fieldName", equalTo("version"));

        given()
                .contentType("application/json")
                .body("{\"body\":\"a rewritten body\"}")
                .when().put("/api/requests/" + uuid + "/body?version=" + versionEveryoneRead)
                .then().statusCode(409)
                .body("[0].code", equalTo(7));

        given()
                .when().get("/api/requests/" + uuid)
                .then().statusCode(200)
                .body("status", equalTo(RequestStatus.VERIFIED.name()))
                .body("body", equalTo("the body of the request"))
                .body("version", equalTo(1));
    }

    @Test
    void when_noVersionIsNamed_then_badRequestIsReturned() {
        final String uuid = addRequest("first request", "the body of the request");

        given()
                .when().post("/api/requests/" + uuid + "/verify")
                .then().statusCode(400)
                .body("[0].code", equalTo(2))
                .body("[0].fieldName", equalTo("version"));
    }

    @Test
    void when_theNamedVersionIsNotANumber_then_badRequestIsReturned() {
        final String uuid = addRequest("first request", "the body of the request");

        given()
                .when().post("/api/requests/" + uuid + "/verify?version=not-a-number")
                .then().statusCode(400)
                .body("[0].code", equalTo(2))
                .body("[0].fieldName", equalTo("version"));
    }

    @ParameterizedTest
    @CsvSource({
            "'{}', name",
            "'{\"body\":\"the body of the request\"}', name",
            "'{\"name\":\"   \",\"body\":\"the body of the request\"}', name",
            "'{\"name\":\"first request\"}', body",
            "'{\"name\":\"first request\",\"body\":\"   \"}', body"
    })
    void when_aRequestIsCreatedWithoutEverythingItNeeds_then_theMissingFieldIsNamed(String payload,
                                                                                   String missingField) {
        given()
                .contentType("application/json")
                .body(payload)
                .when().post("/api/requests")
                .then().statusCode(400)
                .body("[0].code", equalTo(5))
                .body("[0].fieldName", equalTo(missingField));
    }

    @Test
    void when_aPayloadCannotBeRead_then_badRequestIsReturned() {
        given()
                .contentType("application/json")
                .body("{ this is not json")
                .when().post("/api/requests")
                .then().statusCode(400)
                .body("[0].code", equalTo(2));
    }

    @Test
    void when_aRequestIsStillOpen_then_itCarriesNoReasonAndNoPublishedId() {
        final String uuid = addRequest("first request", "the body of the request");

        given()
                .when().get("/api/requests/" + uuid)
                .then().statusCode(200)
                .body("reason", nullValue())
                .body("publishedRequestUuid", nullValue());
    }

    @Test
    void when_aRequestIsDrivenThroughItsLifecycle_then_theWholeHistoryIsStored() {
        final String uuid = addRequest("first request", "the original body");

        given().contentType("application/json").body("{\"body\":\"a rewritten body\"}")
                .when().put("/api/requests/" + uuid + "/body?version=0").then().statusCode(200);
        transition(uuid, "verify", 1L).then().statusCode(200);
        transition(uuid, "accept", 2L).then().statusCode(200);
        final String publishedRequestUuid = transition(uuid, "publish", 3L).asString();

        final List<Map<String, Object>> history = historyOf(uuid);

        assertEquals(List.of("CREATED", "BODY_UPDATED", "VERIFIED", "ACCEPTED", "PUBLISHED"),
                history.stream().map(row -> row.get("EVENT_TYPE")).toList());
        assertEquals(List.of(0L, 0L, 1L, 2L, 3L),
                history.stream().map(row -> ((Number) row.get("DECIDED_ON_VERSION")).longValue()).toList());
        assertNull(history.get(0).get("FROM_STATUS"), "a request does not come from a status into being");
        assertEquals("ACCEPTED", history.get(4).get("FROM_STATUS"));
        assertEquals("PUBLISHED", history.get(4).get("TO_STATUS"));
        assertEquals(publishedRequestUuid, history.get(4).get("PUBLISHED_REQUEST_UUID"));
        assertNull(history.get(4).get("PUBLISHED_AT"), "nothing has forwarded these anywhere yet");
    }

    @Test
    void when_aRequestIsDeleted_then_theReasonSurvivesInTheHistory() {
        final String uuid = addRequest("first request", "the body of the request");

        withReason(uuid, "delete", "no longer needed", 0L).then().statusCode(200);

        final List<Map<String, Object>> history = historyOf(uuid);
        assertEquals(List.of("CREATED", "DELETED"), history.stream().map(row -> row.get("EVENT_TYPE")).toList());
        assertEquals("no longer needed", history.get(1).get("REASON"));
    }

    @Test
    void when_aCallIsRefused_then_nothingIsAddedToTheHistory() {
        final String uuid = addRequest("first request", "the body of the request");

        transition(uuid, "publish", 0L).then().statusCode(409);
        transition(uuid, "verify", 99L).then().statusCode(409);

        assertEquals(List.of("CREATED"), historyOf(uuid).stream().map(row -> row.get("EVENT_TYPE")).toList());
    }

    private List<Map<String, Object>> historyOf(String uuid) {
        return jdbcTemplate.queryForList(
                "SELECT event_type, from_status, to_status, reason, published_request_uuid,"
                        + " decided_on_version, published_at FROM request_event"
                        + " WHERE request_uuid = ? ORDER BY id", uuid);
    }

    private Response transition(String uuid, String transition, long version) {
        return given().when().post("/api/requests/" + uuid + "/" + transition + "?version=" + version);
    }

    private Response withReason(String uuid, String transition, String reason, long version) {
        return given()
                .contentType("application/json")
                .body("{\"reason\":\"" + reason + "\"}")
                .when().post("/api/requests/" + uuid + "/" + transition + "?version=" + version);
    }

    private long versionOf(String uuid) {
        final Response response = given().when().get("/api/requests/" + uuid);
        response.then().statusCode(200);
        return response.jsonPath().getLong("version");
    }

    private String addRequest(String name, String body) {
        final Response response = given()
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\",\"body\":\"" + body + "\"}")
                .when().post("/api/requests");

        response.then().statusCode(200);
        return response.asString();
    }

    @Test
    void when_aFieldIsLongerThanTheRequestAllows_then_badRequestNamesIt() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"first request\",\"body\":\"" + "x".repeat(4001) + "\"}")
                .when().post("/api/requests")
                .then().statusCode(400)
                .body("[0].code", equalTo(11))
                .body("[0].fieldName", equalTo("body"));
    }

    @Test
    void when_aFieldIsExactlyAtTheLimit_then_theRequestIsCreated() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"first request\",\"body\":\"" + "x".repeat(4000) + "\"}")
                .when().post("/api/requests")
                .then().statusCode(200);
    }

    @Test
    void when_theHttpMethodIsNotSupported_then_methodNotAllowedIsReturned() {
        given()
                .when().delete("/api/requests/whatever")
                .then().statusCode(405)
                .header("Allow", notNullValue())
                .body("[0].code", equalTo(9));
    }

    @Test
    void when_theEndpointDoesNotExist_then_notFoundIsReturned() {
        given()
                .when().get("/api/nope")
                .then().statusCode(404)
                .body("[0].code", equalTo(10));
    }

    @Test
    void when_aRequestIsCreated_then_itStartsAtTheFirstVersion() {
        final String uuid = addRequest("first request", "the body of the request");

        assertEquals(0L, versionOf(uuid));
    }
}
