import akka.actor.typed.ActorSystem

fun main() {
    val system: ActorSystem<String> = ActorSystem.create(StartRefActor.create(), "testSystem")
    system.tell("start")
    system.terminate()
}
