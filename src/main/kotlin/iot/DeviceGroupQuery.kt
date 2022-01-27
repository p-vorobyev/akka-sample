package iot

import Log
import akka.actor.*
import scala.concurrent.duration.FiniteDuration


class CollectionTimeout


class DeviceGroupQuery(
    val actorToDeviceId: MutableMap<ActorRef, String>,

    val requestId: Long,

    val requester: ActorRef,

    val timeout: FiniteDuration,
): AbstractActor() {

    val queryTimeoutTimer: Cancellable = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout(), context.dispatcher, self)

    companion object : Log() {
        fun props(actorToDeviceId: MutableMap<ActorRef, String>,
                  requestId: Long,
                  requester: ActorRef,
                  timeout: FiniteDuration,): Props {
            return Props.create(DeviceGroupQuery::class.java) {
                DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout)
            }
        }
    }

    override fun preStart() {
        actorToDeviceId.keys.forEach { deviceActorRef ->
            context.watch(deviceActorRef)
            deviceActorRef.tell(ReadTemperature(0), self)
        }
    }

    override fun postStop() {
        queryTimeoutTimer.cancel()
    }

    override fun createReceive(): Receive {
        return waitingForReplies(HashMap(), actorToDeviceId.keys)
    }

    private fun waitingForReplies(repliesSoFar: MutableMap<String, TemperatureReading>,
                                  stillWaiting: Set<ActorRef>): Receive {
        return receiveBuilder()
            .match(RespondTemperature::class.java) { resp ->
                val deviceActor: ActorRef = sender
                val reading: TemperatureReading = resp.value?.let { Temperature(it) } ?: TemperatureNotAvailable()
                receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)
            }
            .match(Terminated::class.java) { t ->
                receivedResponse(t.actor, DeviceNotAvailable(), stillWaiting, repliesSoFar)
            }
            .match(CollectionTimeout::class.java) {
                val replies: MutableMap<String, TemperatureReading> = HashMap(repliesSoFar)
                stillWaiting.forEach { actorRef ->
                    val deviceId: String? = actorToDeviceId[actorRef]
                    if (deviceId != null) {
                        replies[deviceId] = DeviceTimedOut()
                    }
                }
                requester.tell(RespondAllTemperatures(requestId, replies), self)
                context.stop(self)
            }
            .build()
    }

    private fun receivedResponse(deviceActorRef: ActorRef,
                                 reading: TemperatureReading,
                                 stillWaiting: Set<ActorRef>,
                                 repliesSoFar: MutableMap<String, TemperatureReading>) {
        context.unwatch(deviceActorRef)
        val deviceId = actorToDeviceId[deviceActorRef]

        val newStillWaiting: MutableSet<ActorRef> = HashSet(stillWaiting)
        newStillWaiting.remove(deviceActorRef)

        val newRepliesSoFar: MutableMap<String, TemperatureReading> = HashMap(repliesSoFar)
        deviceId?.let {
            newRepliesSoFar[it] = reading
        }
        if (newStillWaiting.isEmpty()) {
            requester.tell(RespondAllTemperatures(requestId, newRepliesSoFar), self)
            context.stop(self)
        } else {
            context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
        }
    }

}
