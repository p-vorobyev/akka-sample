package streams

import akka.Done
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.javadsl.FileIO
import akka.stream.javadsl.Source
import akka.util.ByteString
import java.math.BigInteger
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.CompletionStage

fun main() {
    val actorSystem: ActorSystem = ActorSystem.create("QuickStart")
    val source: Source<Int, NotUsed> = Source.range(1, 10)

    val done: CompletionStage<Done> = source.runForeach({ println("$it") }, actorSystem)

    done.thenRun {
        println("""
        *
        *
        *
    """.trimIndent())
    }

    val factorials: Source<BigInteger, NotUsed> =
        source.scan(BigInteger.ONE) { acc: BigInteger, next: Int ->
            acc.multiply(BigInteger.valueOf(next.toLong()))
        }

    val factorialsWithIndex: CompletionStage<Done> = factorials
        .zipWith(Source.range(0, 9)) { num: BigInteger, idx: Int -> "index: $idx, element: $num" }
        .throttle(1, Duration.ofSeconds(1))
        .runForeach({ println(it) }, actorSystem)

    factorialsWithIndex.toCompletableFuture().get()

    val doneFactorials: CompletionStage<IOResult> = factorials
        .map { bigInt -> ByteString.fromString("$bigInt\n") }
        .runWith(FileIO.toPath(Paths.get("numbers.txt")), actorSystem)

    doneFactorials.thenRun {
        println("Goodbye!")
        actorSystem.terminate()
    }
}
