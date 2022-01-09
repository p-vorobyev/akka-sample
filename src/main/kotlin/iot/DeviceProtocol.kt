package iot

import Log
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive


interface Command


class ReadTemperature(
    val requestId: Long,
    val replyTo: ActorRef<RespondTemperature>
) : Command


class RespondTemperature(
    val requestId: Long,
    val value: Double?
)


class RecordTemperature(
    val requestId: Long,
    val value: Double,
    val replyTo: ActorRef<TemperatureRecorded>
): Command


class TemperatureRecorded(val requestId: Long)


class Device: AbstractBehavior<Command> {

    private val groupId: String

    private val deviceId: String

    private var lastTemperatureReading: Double? = null

    private constructor(context: ActorContext<Command>?,
                        groupId: String,
                        deviceId: String) : super(context) {
        this.groupId = groupId
        this.deviceId = deviceId
        log.info("Device actor $groupId-$deviceId started")
        //context?.log?.info("Device actor $groupId-$deviceId started")
    }

    companion object: Log() {
        fun create(groupId: String, deviceId: String): Behavior<Command> {
            return Behaviors.setup { context -> Device(context, groupId, deviceId) }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(RecordTemperature::class.java, this::onRecordTemperature)
            .onMessage(ReadTemperature::class.java, this::onReadTemperature)
            .onSignal(PostStop::class.java) { signal -> onPostStop() }
            .build()
    }

    private fun onRecordTemperature(r: RecordTemperature): Behavior<Command> {
        log.info("Recorded temperature reading ${r.value} with ${r.requestId}")
        lastTemperatureReading = r.value
        r.replyTo.tell(TemperatureRecorded(r.requestId))
        return this
    }

    private fun onReadTemperature(r: ReadTemperature): Behavior<Command> {
        r.replyTo.tell(RespondTemperature(r.requestId, lastTemperatureReading))
        return this
    }

    private fun onPostStop(): Device {
        //context?.log?.info("Device actor $groupId-$deviceId stopped")
        log.info("Device actor $groupId-$deviceId stopped")
        return this
    }
}
