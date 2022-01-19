package iot

import Log
import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated

class DeviceManagerActor: AbstractActor() {

    private val groupIdToActor: MutableMap<String, ActorRef> = hashMapOf()

    private val actorToGroupId: MutableMap<ActorRef, String> = hashMapOf()

    companion object: Log() {
        fun props(): Props {
            return Props.create(DeviceManagerActor::class.java, DeviceManagerActor())
        }
    }

    override fun preStart() {
        log.info("${this.javaClass.simpleName} started")
    }

    override fun postStop() {
        log.info("${this.javaClass.simpleName} stopped")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(RequestTrackDevice::class.java, this::onTrackDevice)
            .match(Terminated::class.java, this::onTerminated)
            .build()
    }

    private fun onTrackDevice(reqTrack: RequestTrackDevice) {
        val groupId = reqTrack.groupId
        var groupActorRef: ActorRef? = groupIdToActor[groupId]
        if (groupActorRef != null) {
            groupActorRef.forward(reqTrack, context)
        } else {
            log.info("Create device group actor for $groupId")
            groupActorRef = context.actorOf(DeviceGroupActor.props(groupId), "group-$groupId")
            context.watch(groupActorRef)
            groupActorRef.forward(reqTrack, context)
            groupIdToActor[groupId] = groupActorRef
            actorToGroupId[groupActorRef] = groupId
        }
    }

    private fun onTerminated(t: Terminated) {
        val groupActorRef: ActorRef = t.actor
        val groupId: String? = actorToGroupId[groupActorRef]
        if (groupId != null) {
            log.info("Device group actor $groupId has benn terminated")
            groupIdToActor.remove(groupId)
            actorToGroupId.remove(groupActorRef)
        }
    }
}
