import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class PrintMyActorRefActor private constructor(context: ActorContext<String>?) :
    AbstractBehavior<String>(context) {

    override fun createReceive(): Receive<String> =
        newReceiveBuilder()
            .onMessageEquals("printit", this::printIt)
            .build()

    private fun printIt(): Behavior<String> {
        val secondRef: ActorRef<String> = context.spawn(Behaviors.empty(), "second-actor")
        println("Second: $secondRef")
        return this
    }

    companion object {
        fun create(): Behavior<String> =
            Behaviors.setup { context -> PrintMyActorRefActor(context) }
    }

}

class StartRefActor private constructor(context: ActorContext<String>?) :
    AbstractBehavior<String>(context) {

    override fun createReceive(): Receive<String> =
        newReceiveBuilder()
            .onMessageEquals("start", this::start)
            .build()

    private fun start(): Behavior<String> {
        val firstRef: ActorRef<String> = context.spawn(PrintMyActorRefActor.create(), "first-actor")
        println("First: $firstRef")
        firstRef.tell("printit")
        return Behaviors.same()
    }

    companion object {
        fun create(): Behavior<String> =
            Behaviors.setup { context -> StartRefActor(context) }
    }

}

