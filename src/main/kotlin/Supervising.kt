import akka.actor.typed.*
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class SupervisingActor: AbstractBehavior<String> {

    private var child: ActorRef<String>? = null

    private constructor(context: ActorContext<String>?) : super(context) {
        child = context?.spawn(
            Behaviors.supervise(SupervisedActor.create()).onFailure(SupervisorStrategy.restart()),
            "supervised-actor"
        )
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup { context -> SupervisingActor(context) }
    }

    override fun createReceive(): Receive<String> =
        newReceiveBuilder()
            .onMessageEquals("failChild", this::onFailChild)
            .build()

    private fun onFailChild(): Behavior<String> {
        child?.tell("fail")
        return this
    }
}

class SupervisedActor: AbstractBehavior<String> {

    companion object {
        fun create(): Behavior<String> = Behaviors.setup { context -> SupervisedActor(context) }
    }

    private constructor(context: ActorContext<String>?) : super(context) {
        println("Supervised  actor started")
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder()
            .onMessageEquals("fail", this::fail)
            .onSignal(PreRestart::class.java) { preStart() }
            .onSignal(PostStop::class.java) { postStop() }
            .build()
    }

    private fun fail(): Behavior<String> {
        println("supervised actor fails now")
        throw RuntimeException("I failed!")
    }

    private fun preStart(): Behavior<String> {
        println("supervised will be restarted")
        return this
    }

    private fun postStop(): Behavior<String> {
        println("supervised stopped")
        return this
    }
}
