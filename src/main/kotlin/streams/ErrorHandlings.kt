package streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.javadsl.Source
import java.io.Serializable

fun main() {
    val system = ActorSystem.create("system")
    source()
        .recoverWithRetries(1, RuntimeException::class.java) { Source.from(listOf("one", "two", "three")) }
        .runForeach({ println(it)}, system)
        .thenRun { system.terminate() }
}

private fun source(): Source<Serializable, NotUsed> =
    Source
    .from(listOf(1, 2, 3, 4, 5))
    .map {
        if (it % 3 == 0) {
            throw RuntimeException("Wrong value!")
        } else it
    }
