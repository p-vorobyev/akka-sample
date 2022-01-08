import akka.actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractTestKit {

    lateinit var testKit: ActorTestKit

    @BeforeEach
    fun setUp() {
        testKit = ActorTestKit.create()
    }

    @AfterEach
    fun clean() {
        testKit.shutdownTestKit()
    }

}
