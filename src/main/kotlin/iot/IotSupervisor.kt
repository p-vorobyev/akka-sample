package iot

import Log
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class IotSupervisor: AbstractBehavior<Unit> {

    private constructor(context: ActorContext<Unit>?) : super(context) {
        log.info("IoT Application started")
    }

    companion object: Log() {
        fun create(): Behavior<Unit> {
            return Behaviors.setup { IotSupervisor(it) }
        }
    }

    override fun createReceive(): Receive<Unit> {
        return newReceiveBuilder()
            .onSignal(PostStop::class.java) { signal -> onPostStop() }
            .build()
    }

    private fun onPostStop(): IotSupervisor {
        log.info("IoT Application stopped")
        return this
    }
}
