package iot

import Log
import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props

class DeviceGroupActor(private val groupId: String): AbstractActor() {

    companion object: Log() {
        fun props(groupId: String): Props {
            return Props.create(DeviceGroupActor::class.java) { DeviceGroupActor(groupId) }
        }
    }

    private val deviceIdToActor: MutableMap<String, ActorRef> = hashMapOf()

    override fun preStart() {
        log.info("DeviceGroupActor $groupId started")
    }

    override fun postStop() {
        log.info("DeviceGroupActor $groupId stopped")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(RequestTrackDevice::class.java, this::onTrackDevice)
            .build()
    }

    private fun onTrackDevice(reqMsg: RequestTrackDevice) {
        if (this.groupId == reqMsg.groupId) {
            var actorRef: ActorRef? = deviceIdToActor[reqMsg.deviceId]
            if (actorRef != null) {
                actorRef.forward(reqMsg, context)
            } else {
                log.info("Creating device actor for ${reqMsg.deviceId}")
                actorRef = context.actorOf(DeviceActor.props(groupId, reqMsg.deviceId), "device-${reqMsg.deviceId}")
                deviceIdToActor[reqMsg.deviceId] = actorRef
                actorRef.forward(reqMsg, context)
            }
        } else {
            log.warn("Ignoring TrackDevice request for ${reqMsg.groupId}. This actor is responsible for ${this.groupId}.")
        }
    }
}
