package NoMathExpectation.NMEBoot.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.json.Json
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource

internal val ktorClient = HttpClient(CIO) {
    install(Resources)
    install(ContentNegotiation) {
        json(Json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 10000
    }
}

internal fun ByteReadChannel.toExternalResource() = toInputStream().toExternalResource()