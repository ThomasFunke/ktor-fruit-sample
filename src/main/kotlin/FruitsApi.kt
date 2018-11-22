import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    embeddedServer(
            factory = Netty,
            port = 8080,
            module = Application::module
    ).start(wait = true)
}

data class Fruit(val name: String)

fun Application.module() {
    installFeatures()

    val fruitStash = mutableListOf<Fruit>()
    routing {
        route("/fruits") {
            authenticate {
                post {
                    val fruit = call.receive<Fruit>()
                    fruitStash.add(fruit)
                    call.respond(Created, "Created ${fruit.name}")
                }
            }
            get {
                call.respond(fruitStash)
            }
            get("/{index}") {
                val index = call.parameters["index"]?.toIntOrNull()
                if (index != null) {
                    val fruit = fruitStash[index]
                    call.respond(fruit)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

    }
}

private fun Application.installFeatures() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(feature = StatusPages) {
        exception<IndexOutOfBoundsException> {
            call.respond(NotFound, "404 Not Found")
        }
        exception<Throwable> { e ->
            call.respondText { e.message ?: "Unknown Error" }
        }
    }

    install(Authentication) {
        basic {
            validate { (name, password) ->
                if (name == "lovis" && password == "cc hamburg") {
                    UserIdPrincipal(name)
                } else {
                    null
                }
            }
        }
    }
}