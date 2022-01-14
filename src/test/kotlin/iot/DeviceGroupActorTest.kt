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
}

