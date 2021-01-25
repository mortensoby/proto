package dk.prototype

import dk.prototype.cache.RetskredsCache
import dk.prototype.lookup.OutgoingLookup
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.inject.Inject
import javax.inject.Singleton
import java.time.Duration
import java.time.Instant

import static io.micronaut.http.HttpStatus.*

@CompileStatic
@Singleton
@Slf4j
class RetskredsService {


    @Inject
    AdresseService adresseService

    @Inject
    RetskredsCache retskredsCache

    RetskredsLookup findRetskreds(String adresse, Duration freshness) {
        CacheEntry<Retskreds> cachedResult = findInCache(adresse)
        if (cachedResult) {
            return handleWhenThereIsACacheHit(adresse, cachedResult, freshness)
        } else {
            return handleWithoutCacheHit(adresse)
        }
    }

    private RetskredsLookup handleWithoutCacheHit(String adresse) {
        OutgoingLookup<Retskreds> retskredsLookup = lookupRetskreds(adresse)
        if (retskredsLookup.valid) {
            Instant lookupTime = Instant.now()
            addToCache(adresse, retskredsLookup.data, lookupTime)
            return new RetskredsLookup(retskreds: retskredsLookup.data, isFreshEnough: true, lookupTime: lookupTime)
        } else {
            return new RetskredsLookup(httpStatus: SERVICE_UNAVAILABLE)
        }
    }

    RetskredsLookup handleWhenThereIsACacheHit(String adresse, CacheEntry<Retskreds> cachedResult, Duration freshness) {
        boolean isFreshEnough = isFreshEnough((CacheEntry) cachedResult, freshness)
        if (isFreshEnough) {
            log.info("Findes i cachen. Returnerer uden at kalde ud")
            return new RetskredsLookup(retskreds: cachedResult.data, isFreshEnough: true, lookupTime: cachedResult.lookupTime)
        } else {
            OutgoingLookup<Retskreds> retskredsLookup = lookupRetskreds(adresse)
            if (retskredsLookup.valid) {
                log.info("Har hentet data som nu gemmes i cachen og returneres til brugeren")
                Instant lookupTime = Instant.now()
                addToCache(adresse, retskredsLookup.data, lookupTime)
                return new RetskredsLookup(retskreds: retskredsLookup.data, isFreshEnough: true, lookupTime: lookupTime)
            }
            log.info("Returnerer for gamle data, da nye data ikke kunne slås op")
            return new RetskredsLookup(retskreds: cachedResult.data, isFreshEnough: false, lookupTime: cachedResult.lookupTime, httpStatus: NON_AUTHORITATIVE_INFORMATION)
        }
    }

    private CacheEntry<Retskreds> findInCache(String adresse) {
        retskredsCache.getEntry(adresse)
    }

    private OutgoingLookup<Retskreds> lookupRetskreds(String adresse) {
        log.info("Slår retskreds op for '${adresse}'")
        adresseService.findRetskreds(adresse)
    }

    private boolean isFreshEnough(CacheEntry cacheEntry, Duration freshness) {
        cacheEntry.lookupTime.toEpochMilli() > Instant.now().minusMillis(freshness.toMillis()).toEpochMilli()
    }

    void addToCache(String adresse, Retskreds retskreds, Instant lookupTime) {
        retskredsCache.addToCache(adresse, retskreds, lookupTime)
    }
}
