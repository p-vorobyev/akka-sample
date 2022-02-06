package streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.javadsl.Source
import java.time.Duration
import java.time.Instant

data class Author(val handle: String)

data class HashTag(val name: String)

data class Tweet(
    val author: Author,
    val timestamp: Long,
    val body: String
)

fun Tweet.hashTags(): Set<HashTag> {
    return body.split(" ")
        .filter { it.startsWith("#") }
        .map { HashTag(it.lowercase()) }
        .toSet()
}

private val AKKA = HashTag("#akka")

fun main() {
    val system: ActorSystem = ActorSystem.create("reactive-tweets")

    val tweets: Source<Tweet, NotUsed> = Source.range(0, 100)
        .throttle(20, Duration.ofSeconds(1))
        .map {
            if (it % 2 == 0) {
                Tweet(Author("author$it"), Instant.now().epochSecond, "body$it #akka")
            } else Tweet(Author("author$it"), Instant.now().epochSecond, "body$it #some_hashtag")
        }

    val authors: Source<Author, NotUsed> = tweets
        .filter { it.hashTags().contains(AKKA) }
        .map { it.author }

    authors.runForeach({ println(it) }, system)
        .thenRun {
            tweets.filter { it.hashTags().contains(AKKA) }
                .map { it.hashTags() }.flatMapConcat { Source.from(it) }
                .runForeach({ println(it) }, system)
                .thenRun {
                    system.terminate()
                }
        }

    /*tweets
        .buffer(2, OverflowStrategy.dropHead())
        .map { *//*some slow computation*//* }
        .runWith(Sink.ignore(), system).thenRun { system.terminate() }*/
}
