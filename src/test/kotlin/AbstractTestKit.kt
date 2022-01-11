import akka.actor.ActorSystem
import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.testkit.TestKit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractTestKit {

    lateinit var testKit: ActorTestKit

    lateinit var system: ActorSystem

    lateinit var probe: TestKit

    @BeforeEach
    fun setUp() {
        testKit = ActorTestKit.create()
        system = ActorSystem.create("actor-system")
        probe = TestKit(system)
    }

    @AfterEach
    fun clean() {
        testKit.shutdownTestKit()
    }

}
