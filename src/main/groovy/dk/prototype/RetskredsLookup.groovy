package dk.prototype

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus

import java.time.Instant

@CompileStatic
class RetskredsLookup {
    Retskreds retskreds
    boolean isFreshEnough
    Instant lookupTime
    HttpStatus httpStatus = HttpStatus.OK
}
