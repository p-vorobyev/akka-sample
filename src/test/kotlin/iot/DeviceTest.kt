package iot

import AbstractTestKit
import akka.actor.testkit.typed.javadsl.TestProbe
import akka.actor.typed.ActorRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DeviceTest: AbstractTestKit() {

    @Test
    fun testReplyWithEmptyTemperature() {
        val recordProbe: TestProbe<TemperatureRecorded> = testKit.createTestProbe(TemperatureRecorded::class.java)
        val respProbe: TestProbe<RespondTemperature> = testKit.createTestProbe(RespondTemperature::class.java)

        val deviceActor: ActorRef<Command> = testKit.spawn(Device.create(groupId = "group", deviceId = "device"))

        /*******************************************************/

        deviceActor.tell(RecordTemperature(1L, 23.5, recordProbe.ref))
        assertEquals(1L, recordProbe.receiveMessage().requestId)

        deviceActor.tell(ReadTemperature(2L, respProbe.ref))
        val response: RespondTemperature = respProbe.receiveMessage()
        assertEquals(2L, response.requestId)
        assertEquals(23.5, response.value)

        /*******************************************************/

        deviceActor.tell(RecordTemperature(3L, 17.0, recordProbe.ref))
        assertEquals(3L, recordProbe.receiveMessage().requestId)

        deviceActor.tell(ReadTemperature(3L, respProbe.ref))
        val response1: RespondTemperature = respProbe.receiveMessage()
        assertEquals(3L, response1.requestId)
        assertEquals(17.0, response1.value)
    }
}
