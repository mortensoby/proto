package dk.prototype.cache

import dk.prototype.CacheEntry
import dk.prototype.Retskreds
import groovy.util.logging.Slf4j
import io.micronaut.cache.CacheManager
import io.micronaut.cache.ehcache.EhcacheSyncCache

import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant

@Singleton
@Slf4j
class RetskredsCache {

    @Inject
    CacheManager cacheManager

    @Lazy
    EhcacheSyncCache cache = { (EhcacheSyncCache)cacheManager.getCache('retskredse') }()

    void addToCache(String adresse, Retskreds retskreds, Instant lookupTime) {
        log.info("Tilføjer til cachen")
        CacheEntry entry = new CacheEntry(data: retskreds, lookupTime: lookupTime)
        cache.put(adresse, entry)
    }

    CacheEntry<Retskreds> getEntry(String adresse) {
        Optional cachedElement = cache.get(adresse, CacheEntry<Retskreds>)
        if (cachedElement.isPresent()) {
            return cachedElement.get()
        } else {
            return null
        }
    }
}
