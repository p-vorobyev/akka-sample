package iot

import AbstractTestKit
import akka.actor.ActorRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class DeviceGroupActorTest: AbstractTestKit() {

    @Test
    fun testRegisterDeviceActorForGroup() {
        val groupId = "kitchen"
        val deviceGroupActor: ActorRef = system.actorOf(DeviceGroupActor.props(groupId), "group-actor")

        deviceGroupActor.tell(RequestTrackDevice(groupId, "TeaPot"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)
        val teaPotActorRef: ActorRef = probe.lastSender()

        deviceGroupActor.tell(RequestTrackDevice(groupId, "Oven"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)
        val ovenActorRef: ActorRef = probe.lastSender()

        assertNotEquals(teaPotActorRef, ovenActorRef)

        teaPotActorRef.tell(RecordTemperature(1L, 60.0), probe.testActor())
        var temperatureRecorded: TemperatureRecorded = probe.expectMsgClass(TemperatureRecorded::class.java)
        assertEquals(1L, temperatureRecorded.requestId)

        ovenActorRef.tell(RecordTemperature(2L, 200.0), probe.testActor())
        temperatureRecorded = probe.expectMsgClass(TemperatureRecorded::class.java)
        assertEquals(2L, temperatureRecorded.requestId)
    }

    @Test
    fun testSameActorForSameDeviceId() {
        val groupId = "kitchen"
        val deviceGroupActor: ActorRef = system.actorOf(DeviceGroupActor.props(groupId), "group-actor")

        deviceGroupActor.tell(RequestTrackDevice(groupId, "TeaPot"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)
        val teaPotActorRef: ActorRef = probe.lastSender()

        deviceGroupActor.tell(RequestTrackDevice(groupId, "TeaPot"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)
        val teaPotActorRef1: ActorRef = probe.lastSender()

        assertEquals(teaPotActorRef, teaPotActorRef1)
    }

    @Test
    fun testIgnoreRegisterForWrongGroup() {
        val groupId = "kitchen"
        val deviceGroupActor: ActorRef = system.actorOf(DeviceGroupActor.props(groupId), "group-actor")

        deviceGroupActor.tell(RequestTrackDevice("livingRoom", "CoffeeMAchine"), probe.testActor())
        probe.expectNoMessage()
    }

}

