package dk.prototype

import dk.prototype.cache.RetskredsCache
import groovy.json.JsonOutput
import groovy.transform.NamedParam
import groovy.transform.NamedVariant
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

import static io.micronaut.http.HttpStatus.NON_AUTHORITATIVE_INFORMATION
import static io.micronaut.http.HttpStatus.SERVICE_UNAVAILABLE

@MicronautTest
class RetskredsRestSpec extends Specification {

    @Inject
    @Client("/")
    RxHttpClient client

    @Inject
    AdresseService adresseService

    @Inject
    RetskredsService retskredsService

    @Inject
    RetskredsCache retskredsCache

    String adresse = 'Vestervej 17, 2630 Taastrup'

    void "test cache"() {
        when: 'slår op for første gang'
            RetskredsLookup opriginalLookup = kaldApi(freshness: 1000)
        then: 'så får vi den forventede retskreds tilbage'
            opriginalLookup.retskreds.navn == "Retten i Glostrup"
        when: 'vi henter igen, og det må være op til 1000 sekunder gammelt'
            RetskredsLookup expectCachedLookup = kaldApi(freshness: 1000)
        then: 'så er opslagstidspunktet det samme som i den oprindelige'
            expectCachedLookup.lookupTime == opriginalLookup.lookupTime
        when: 'Når vi kalder med en freshness på 0'
            RetskredsLookup freshLookup = kaldApi(freshness: 0)
        then: 'er opslagstidspunktet nyere'
            freshLookup.lookupTime > opriginalLookup.lookupTime
        when: 'vi ødelægger pathen så vi får en http-fejl'
            String originalPath = adresseService.adressevaskPath
            adresseService.adressevaskPath = '/nogetHeltGalt'
        and: 'og prøver at hente helt nye data'
            HttpResponse forGammelCachetDataResponse = kaldApiExchange(freshness: 0)
        then: 'får vi et gammel resultat med en tilhørende statuskode'
            forGammelCachetDataResponse.status == NON_AUTHORITATIVE_INFORMATION
        and: 'så får vi det nyeste data som ligger i cachen'
            RetskredsLookup lookupWithTooOldData = forGammelCachetDataResponse.body()
            lookupWithTooOldData.lookupTime == freshLookup.lookupTime
        when: 'vi ryder cachen'
            clearCachen()
        then: 'så får vi en 503, da vi hverken har cachede data eller nye.'
            try {
                kaldApiExchange(freshness: 0)
            } catch (HttpClientResponseException e) {
                assert e.status == SERVICE_UNAVAILABLE
            }
    }

    @NamedVariant
    RetskredsLookup kaldApi(@NamedParam long freshness = 0) {
        HttpRequest request = HttpRequest.POST('/retskredse', JsonOutput.toJson(adresse: adresse, freshness: freshness))
        client.toBlocking().retrieve(request, RetskredsLookup)
    }

    @NamedVariant
    HttpResponse<RetskredsLookup> kaldApiExchange(@NamedParam long freshness = 0) {
        HttpRequest request = HttpRequest.POST('/retskredse', JsonOutput.toJson(adresse: adresse, freshness: freshness))
        client.toBlocking().exchange(request, RetskredsLookup)
    }

    void clearCachen() {
        retskredsCache.cache.invalidateAll()
    }
}
