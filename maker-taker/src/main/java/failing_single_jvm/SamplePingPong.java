package failing_single_jvm;

import io.aeron.driver.MediaDriver;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.SessionConfiguration;

public class SamplePingPong {
    static final int PORT_TO_LIBRARY_MAKER = 11113;
    static final int PORT_EXTERNAL = 11160;

    public static void main(final String[] args) throws InterruptedException {
        final MediaDriver driver = DummyUtils.startDefaultMediaDriver();

        final EngineConfiguration engineConfiguration = DummyUtils.getConfiguration(PORT_TO_LIBRARY_MAKER).bindTo("localhost", PORT_EXTERNAL);
        DummyUtils.cleanupOldLogFileDir(engineConfiguration);

        FixEngine.launch(engineConfiguration);

        final SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
                .address("localhost", PORT_EXTERNAL)
                .targetCompId("MAKER")
                .senderCompId("TAKER")
                .resetSeqNum(true)
                .build();

        final DummyTaker taker = new DummyTaker(sessionConfiguration);
        final DummyMaker maker = new DummyMaker();

        Thread.sleep(Integer.MAX_VALUE);
    }
}
