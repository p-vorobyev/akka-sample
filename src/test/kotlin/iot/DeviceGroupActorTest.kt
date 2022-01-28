package iot

import AbstractTestKit
import akka.actor.ActorRef
import akka.actor.PoisonPill
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

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

    @Test
    fun testListActiveDevices() {
        val groupId = "kitchen"
        val deviceGroupActor: ActorRef = system.actorOf(DeviceGroupActor.props(groupId), "group-actor")

        deviceGroupActor.tell(RequestTrackDevice(groupId, "TeaPot"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)

        deviceGroupActor.tell(RequestTrackDevice(groupId, "TeaPot"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)

        deviceGroupActor.tell(RequestTrackDevice(groupId, "Oven"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)

        deviceGroupActor.tell(RequestDeviceList(0L), probe.testActor())
        val deviceList: ReplyDeviceList = probe.expectMsgClass(ReplyDeviceList::class.java)
        assertEquals(0L, deviceList.requestId)
        assertEquals(setOf("TeaPot", "Oven"), deviceList.ids)
    }
    
    @Test
    fun testListActiveDevicesAfterOneShutsDown() {
        val groupId = "kitchen"
        val deviceGroupActor: ActorRef = system.actorOf(DeviceGroupActor.props(groupId), "group-actor")

        deviceGroupActor.tell(RequestTrackDevice(groupId, "TeaPot"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)
        val toShutDown: ActorRef = probe.lastSender()

        deviceGroupActor.tell(RequestTrackDevice(groupId, "Oven"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)

        deviceGroupActor.tell(RequestDeviceList(1L), probe.testActor())
        val deviceList: ReplyDeviceList = probe.expectMsgClass(ReplyDeviceList::class.java)
        assertEquals(1L, deviceList.requestId)
        assertEquals(setOf("TeaPot", "Oven"), deviceList.ids)

        probe.watch(toShutDown)
        toShutDown.tell(PoisonPill.getInstance(), ActorRef.noSender())
        probe.expectTerminated(toShutDown, Duration.create(5L, TimeUnit.SECONDS))

        probe.awaitAssert({
            deviceGroupActor.tell(RequestDeviceList(2L), probe.testActor())
            val deviceList1: ReplyDeviceList = probe.expectMsgClass(ReplyDeviceList::class.java)
            assertEquals(2L, deviceList1.requestId)
            assertEquals(setOf("Oven"), deviceList1.ids)
        },
            Duration.create(3L, TimeUnit.SECONDS),
            Duration.create(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testCollectTemperaturesFromAllActiveDevices() {
        val group = "group-actor-1"
        val groupActor: ActorRef = system.actorOf(DeviceGroupActor.props(group))

        groupActor.tell(RequestTrackDevice(group, "device1"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)
        val device1Actor: ActorRef = probe.lastSender()

        groupActor.tell(RequestTrackDevice(group, "device2"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)
        val device2Actor: ActorRef = probe.lastSender()

        groupActor.tell(RequestTrackDevice(group, "device3"), probe.testActor())
        probe.expectMsgClass(DeviceRegistered::class.java)
        val device3Actor: ActorRef = probe.lastSender()

        // Check that the device actors are working
        device1Actor.tell(RecordTemperature(0L, 10.0), probe.testActor())
        assertEquals(0L, probe.expectMsgClass(TemperatureRecorded::class.java).requestId)
        device2Actor.tell(RecordTemperature(1L, 18.3), probe.testActor())
        assertEquals(1L, probe.expectMsgClass(TemperatureRecorded::class.java).requestId)
        // No temperature for device 3

        groupActor.tell(RequestAllTemperatures(0L), probe.testActor())
        val allTemperatures: RespondAllTemperatures = probe.expectMsgClass(RespondAllTemperatures::class.java)
        assertEquals(0L, allTemperatures.requestId)

        val expectedTemperatures = mapOf(
            "device1" to Temperature(10.0),
            "device2" to Temperature(18.3),
            "device3" to TemperatureNotAvailable()
        )

        assertEquals(expectedTemperatures, allTemperatures.temperatures)
    }
}

