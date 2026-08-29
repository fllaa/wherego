package app.wherego.core.common

import io.azam.ulidj.ULID

class UlidGenerator {
    fun next(): String = ULID.random()
}
