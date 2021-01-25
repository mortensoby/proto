package dk.prototype

import java.time.Instant

class CacheEntry<T> implements Serializable{
    Instant lookupTime
    T data
}
