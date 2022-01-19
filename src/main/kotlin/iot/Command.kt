package iot

import akka.actor.typed.ActorRef

interface Command

class ReadTemperature(
    val requestId: Long,
    val replyTo: ActorRef<RespondTemperature>
) : Command


class RecordTemperature(
    val requestId: Long,
    val value: Double,
    val replyTo: ActorRef<TemperatureRecorded>? = null
): Command


class RequestTrackDevice(val groupId: String, val deviceId: String): Command


class RequestDeviceList(val requestId: Long): Command

class RequestAllTemperatures(val requestId: Long): Command
