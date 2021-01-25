package dk.prototype.lookup

import io.micronaut.http.HttpStatus

class OutgoingLookup<T> {
    T data
    HttpStatus httpStatus = HttpStatus.OK

    boolean isValid(){
        return data as boolean
    }
}
