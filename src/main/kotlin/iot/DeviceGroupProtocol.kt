package iot

import Log
import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated

class RequestDeviceList(val requestId: Long): Command

class ReplyDeviceList(val requestId: Long, val ids: Set<String>)

class DeviceGroupActor(private val groupId: String): AbstractActor() {

    companion object: Log() {
        fun props(groupId: String): Props {
            return Props.create(DeviceGroupActor::class.java) { DeviceGroupActor(groupId) }
        }
    }

    private val deviceIdToActor: MutableMap<String, ActorRef> = hashMapOf()

    private val actorToDeviceId: MutableMap<ActorRef, String> = hashMapOf()

    override fun preStart() {
        log.info("DeviceGroupActor $groupId started")
    }

    override fun postStop() {
        log.info("DeviceGroupActor $groupId stopped")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(RequestTrackDevice::class.java, this::onTrackDevice)
            .match(Terminated::class.java, this::onTerminated)
            .match(RequestDeviceList::class.java, this::onRequestDeviceList)
            .build()
    }

    private fun onRequestDeviceList(rdl: RequestDeviceList) {
        log.info("List of deviceIds requested: [requestId: ${rdl.requestId}]")
        sender.tell(ReplyDeviceList(rdl.requestId, deviceIdToActor.keys), self)
    }

    private fun onTrackDevice(reqMsg: RequestTrackDevice) {
        if (this.groupId == reqMsg.groupId) {
            var deviceActorRef: ActorRef? = deviceIdToActor[reqMsg.deviceId]
            if (deviceActorRef != null) {
                deviceActorRef.forward(reqMsg, context)
            } else {
                log.info("Creating device actor for ${reqMsg.deviceId}")
                deviceActorRef = context.actorOf(DeviceActor.props(groupId, reqMsg.deviceId), "device-${reqMsg.deviceId}")
                context.watch(deviceActorRef)
                deviceIdToActor[reqMsg.deviceId] = deviceActorRef
                actorToDeviceId[deviceActorRef] = reqMsg.deviceId
                deviceActorRef.forward(reqMsg, context)
            }
        } else {
            log.warn("Ignoring TrackDevice request for ${reqMsg.groupId}. This actor is responsible for ${this.groupId}.")
        }
    }

    private fun onTerminated(t: Terminated) {
        val actorRef: ActorRef = t.actor
        val deviceId: String? = actorToDeviceId[actorRef]
        if (deviceId != null) {
            deviceIdToActor.remove(deviceId)
            actorToDeviceId.remove(actorRef)
            log.info("DeviceActor for $deviceId has been terminated")
        } else {
            log.warn("No deviceId found for terminated Actor: [$actorRef]")
        }
    }
}
