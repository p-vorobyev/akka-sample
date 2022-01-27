package iot

import akka.actor.typed.ActorRef

interface Command

class ReadTemperature(
    val requestId: Long,
    val replyTo: ActorRef<RespondTemperature>? = null
) : Command


class RecordTemperature(
    val requestId: Long,
    val value: Double,
    val replyTo: ActorRef<TemperatureRecorded>? = null
): Command


class RequestTrackDevice(val groupId: String, val deviceId: String): Command


class RequestDeviceList(val requestId: Long): Command


class RequestAllTemperatures(val requestId: Long): Command

/*------------------------------------------------------------------------------------------------------------*/

interface TemperatureReading


class RespondAllTemperatures(val requestId: Long, val temperatures: Map<String, TemperatureReading>)


data class Temperature(val value: Double): TemperatureReading


class TemperatureNotAvailable: TemperatureReading {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}


class DeviceNotAvailable: TemperatureReading {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}


class DeviceTimedOut: TemperatureReading {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
