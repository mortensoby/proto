package dk.prototype


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client

import javax.inject.Inject
import java.time.Duration

import static io.micronaut.http.HttpResponse.status

@CompileStatic
@Controller("/retskredse")
@Slf4j
class RetskredsController {

    @Inject
    RetskredsService retskredsService

    @Inject
    @Client("https://dawa.aws.dk")
    HttpClient httpClient

    @Post()
    @Produces(MediaType.APPLICATION_JSON)
    HttpResponse index(@Body Map body) {
        Long seconds = ((String) body.freshness)?.toLong()
        String adresse = body.adresse
        log.info("Henter adressen '${adresse}' må højest være ${seconds} sekunder gammel")
        RetskredsLookup retskredsLookup = retskredsService.findRetskreds(adresse, Duration.ofSeconds(seconds))
        return status(retskredsLookup.httpStatus).body(retskredsLookup)
    }
}
