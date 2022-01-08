import akka.actor.typed.ActorRef
import org.junit.jupiter.api.Test

internal class SupervisedActorTest: AbstractTestKit() {

    @Test
    fun supervise() {
        val actorRef: ActorRef<String> = testKit.spawn(SupervisingActor.create(), "supervising-actor")
        actorRef.tell("failChild")
    }
}
