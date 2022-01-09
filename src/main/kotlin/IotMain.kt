import akka.actor.typed.ActorSystem
import iot.IotSupervisor

fun main() {
    // Create ActorSystem and top level supervisor
    ActorSystem.create(IotSupervisor.create(), "iot-system")
}
