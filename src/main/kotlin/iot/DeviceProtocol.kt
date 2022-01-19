package iot

import Log
import akka.actor.AbstractActor
import akka.actor.Props
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class RespondTemperature(
    val requestId: Long,
    val value: Double?
)


class TemperatureRecorded(val requestId: Long)


class DeviceRegistered


class DeviceBehavior: AbstractBehavior<Command> {

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
            return Behaviors.setup { context -> DeviceBehavior(context, groupId, deviceId) }
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
        r.replyTo?.tell(TemperatureRecorded(r.requestId))
        return this
    }

    private fun onReadTemperature(r: ReadTemperature): Behavior<Command> {
        r.replyTo.tell(RespondTemperature(r.requestId, lastTemperatureReading))
        return this
    }

    private fun onPostStop(): DeviceBehavior {
        //context?.log?.info("Device actor $groupId-$deviceId stopped")
        log.info("Device actor $groupId-$deviceId stopped")
        return this
    }
}


class DeviceActor: AbstractActor {

    private val groupId: String

    private val deviceId: String

    private var lastTemperatureReading: Double? = null

    constructor(groupId: String,
                deviceId: String) : super() {
        this.groupId = groupId
        this.deviceId = deviceId
    }

    companion object: Log() {
        fun props(groupId: String, deviceId: String): Props {
            return Props.create(DeviceActor::class.java) { DeviceActor(groupId, deviceId) }
        }
    }

    override fun preStart() {
        log.info("Device actor $groupId-$deviceId started")
    }

    override fun postStop() {
        log.info("Device actor $groupId-$deviceId stopped")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(RequestTrackDevice::class.java, this::onRequestTrackDevice)
            .match(RecordTemperature::class.java, this::onRecordTemperature)
            .match(ReadTemperature::class.java, this::onReadTemperature)
            .build()
    }

    private fun onRequestTrackDevice(r: RequestTrackDevice) {
        if (this.groupId == r.groupId && this.deviceId == r.deviceId) {
            sender.tell(DeviceRegistered(), self)
        } else {
            log.warn("Ignoring TrackDevice request for ${r.groupId}-${r.deviceId}. " +
                    "This actor is responsible for ${this.groupId}-${this.deviceId}.")
        }
    }

    private fun onRecordTemperature(r: RecordTemperature) {
        log.info("Recorded temperature reading ${r.value} with ${r.requestId}")
        lastTemperatureReading = r.value
        sender.tell(TemperatureRecorded(r.requestId), self)
    }

    private fun onReadTemperature(r: ReadTemperature) {
        sender.tell(RespondTemperature(r.requestId, lastTemperatureReading), self)
    }
}
