import akka.actor.typed.ActorRef
import org.junit.jupiter.api.Test

internal class MainKtTest: AbstractTestKit() {

    @Test
    fun main() {
        val actorRef: ActorRef<String> = testKit.spawn(StartRefActor.create(), "testSystem")
        actorRef.tell("start")
        testKit.shutdownTestKit()
    }

}
