package dk.prototype


import dk.prototype.lookup.OutgoingLookup
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException

import javax.inject.Inject
import javax.inject.Singleton

import static io.micronaut.http.HttpStatus.*

@Singleton
@Slf4j
class AdresseService {

    String adressevaskPath = "/datavask/adresser"

    @Inject
    @Client('${dawa.host}')
    HttpClient httpClient

    OutgoingLookup<Retskreds> findRetskreds(String adresseString) {
        try {
            String adresseId = findAdresseId(adresseString)
            Retskreds retskreds = findRetskredsFraAdresseId(adresseId)
            return new OutgoingLookup<Retskreds>(data: retskreds, httpStatus: OK)
        } catch (HttpClientResponseException e) {
            return new OutgoingLookup<Retskreds>(httpStatus: e.status)
        }
    }

    private String findAdresseId(String adresseString) {
        HttpRequest request = HttpRequest.GET(adressevaskPath)
        request.parameters.add('betegnelse', adresseString)
        String payload = httpClient.toBlocking().retrieve(request)
        def json = new JsonSlurper().parseText(payload)
        return json.resultater[0].aktueladresse.id
    }

    private Retskreds findRetskredsFraAdresseId(String adresseId) {
        String payload = httpClient.toBlocking().retrieve("/adresser/${adresseId}")
        def json = new JsonSlurper().parseText(payload)
        json.adgangsadresse.retskreds as Retskreds
    }
}
