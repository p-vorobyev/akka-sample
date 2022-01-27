package iot

import AbstractTestKit
import akka.actor.ActorRef
import akka.actor.PoisonPill
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

internal class DeviceGroupQueryTest: AbstractTestKit() {

    @Test
    fun temperaturesForDevices() {
        val requestActor: ActorRef = probe.testActor()

        val device1Actor: ActorRef = probe1.testActor()
        val device2Actor: ActorRef = probe2.testActor()

        val actorToDeviceId: MutableMap<ActorRef, String> = mutableMapOf(
            device1Actor to "device1",
            device2Actor to "device2"
        )

        val queryActor: ActorRef = system.actorOf(
            DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requestActor,
                FiniteDuration(3L, TimeUnit.SECONDS)
            )
        )
        assertEquals(0L, probe1.expectMsgClass(ReadTemperature::class.java).requestId)
        assertEquals(0L, probe2.expectMsgClass(ReadTemperature::class.java).requestId)

        queryActor.tell(RespondTemperature(0L, 1.0), device1Actor)
        queryActor.tell(RespondTemperature(0L, 2.0), device2Actor)

        val respondAllTemperatures: RespondAllTemperatures = probe.expectMsgClass(RespondAllTemperatures::class.java)
        assertEquals(1L, respondAllTemperatures.requestId)

        val expectedTemperatures: Map<String, TemperatureReading> = mapOf(
            "device1" to Temperature(1.0),
            "device2" to Temperature(2.0)
        )
        assertEquals(expectedTemperatures, respondAllTemperatures.temperatures)
    }

    @Test
    fun temperaturesForDevicesWithUnavailable() {
        val requestActor: ActorRef = probe.testActor()

        val device1Actor: ActorRef = probe1.testActor()
        val device2Actor: ActorRef = probe2.testActor()

        val actorToDeviceId: MutableMap<ActorRef, String> = mutableMapOf(
            device1Actor to "device1",
            device2Actor to "device2"
        )

        val queryActor: ActorRef = system.actorOf(
            DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requestActor,
                FiniteDuration(3L, TimeUnit.SECONDS)
            )
        )
        assertEquals(0L, probe1.expectMsgClass(ReadTemperature::class.java).requestId)
        assertEquals(0L, probe2.expectMsgClass(ReadTemperature::class.java).requestId)

        queryActor.tell(RespondTemperature(0L, 1.0), device1Actor)
        queryActor.tell(RespondTemperature(0L, null), device2Actor) // !!!!

        val respondAllTemperatures: RespondAllTemperatures = probe.expectMsgClass(RespondAllTemperatures::class.java)
        assertEquals(1L, respondAllTemperatures.requestId)

        val expectedTemperatures: Map<String, TemperatureReading> = mapOf(
            "device1" to Temperature(1.0),
            "device2" to TemperatureNotAvailable() // !!!!
        )
        assertEquals(expectedTemperatures, respondAllTemperatures.temperatures)
    }

    @Test
    fun temperaturesForDevicesWhenDeviceStoppedBeforeAnswer() {
        val requestActor: ActorRef = probe.testActor()

        val device1Actor: ActorRef = probe1.testActor()
        val device2Actor: ActorRef = probe2.testActor()

        val actorToDeviceId: MutableMap<ActorRef, String> = mutableMapOf(
            device1Actor to "device1",
            device2Actor to "device2"
        )

        val queryActor: ActorRef = system.actorOf(
            DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requestActor,
                FiniteDuration(3L, TimeUnit.SECONDS)
            )
        )
        assertEquals(0L, probe1.expectMsgClass(ReadTemperature::class.java).requestId)
        assertEquals(0L, probe2.expectMsgClass(ReadTemperature::class.java).requestId)

        queryActor.tell(RespondTemperature(0L, 1.0), device1Actor)
        device2Actor.tell(PoisonPill.getInstance(), ActorRef.noSender()) // !!!!

        val respondAllTemperatures: RespondAllTemperatures = probe.expectMsgClass(RespondAllTemperatures::class.java)
        assertEquals(1L, respondAllTemperatures.requestId)

        val expectedTemperatures: Map<String, TemperatureReading> = mapOf(
            "device1" to Temperature(1.0),
            "device2" to DeviceNotAvailable() // !!!!
        )
        assertEquals(expectedTemperatures, respondAllTemperatures.temperatures)
    }

    @Test
    fun temperaturesForDevicesWhenDeviceStopsAfterAnswer() {
        val requestActor: ActorRef = probe.testActor()

        val device1Actor: ActorRef = probe1.testActor()
        val device2Actor: ActorRef = probe2.testActor()

        val actorToDeviceId: MutableMap<ActorRef, String> = mutableMapOf(
            device1Actor to "device1",
            device2Actor to "device2"
        )

        val queryActor: ActorRef = system.actorOf(
            DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requestActor,
                FiniteDuration(3L, TimeUnit.SECONDS)
            )
        )
        assertEquals(0L, probe1.expectMsgClass(ReadTemperature::class.java).requestId)
        assertEquals(0L, probe2.expectMsgClass(ReadTemperature::class.java).requestId)

        queryActor.tell(RespondTemperature(0L, 1.0), device1Actor)
        queryActor.tell(RespondTemperature(0L, 2.0), device2Actor)
        device1Actor.tell(PoisonPill.getInstance(), ActorRef.noSender()) // !!!! actor already unwatched after received message

        val respondAllTemperatures: RespondAllTemperatures = probe.expectMsgClass(RespondAllTemperatures::class.java)
        assertEquals(1L, respondAllTemperatures.requestId)

        val expectedTemperatures: Map<String, TemperatureReading> = mapOf(
            "device1" to Temperature(1.0),
            "device2" to Temperature(2.0)
        )
        assertEquals(expectedTemperatures, respondAllTemperatures.temperatures)
    }

    @Test
    fun temperaturesForDevicesWhenDeviceNotAnswerInTime() {
        val requestActor: ActorRef = probe.testActor()

        val device1Actor: ActorRef = probe1.testActor()
        val device2Actor: ActorRef = probe2.testActor()

        val actorToDeviceId: MutableMap<ActorRef, String> = mutableMapOf(
            device1Actor to "device1",
            device2Actor to "device2"
        )

        val queryActor: ActorRef = system.actorOf(
            DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requestActor,
                FiniteDuration(2L, TimeUnit.SECONDS)
            )
        )
        assertEquals(0L, probe1.expectMsgClass(ReadTemperature::class.java).requestId)
        assertEquals(0L, probe2.expectMsgClass(ReadTemperature::class.java).requestId)

        queryActor.tell(RespondTemperature(0L, 1.0), device1Actor)
        // second actor will not answer in time

        val respondAllTemperatures: RespondAllTemperatures = probe.expectMsgClass(RespondAllTemperatures::class.java)
        assertEquals(1L, respondAllTemperatures.requestId)

        val expectedTemperatures: Map<String, TemperatureReading> = mapOf(
            "device1" to Temperature(1.0),
            "device2" to DeviceTimedOut() // !!!!
        )
        assertEquals(expectedTemperatures, respondAllTemperatures.temperatures)
    }

}
