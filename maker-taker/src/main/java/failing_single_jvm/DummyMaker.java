package failing_single_jvm;

import org.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.artio.builder.ExampleMessageEncoder;
import uk.co.real_logic.artio.decoder.*;
import uk.co.real_logic.artio.library.AcquiringSessionExistsHandler;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.library.SessionAcquireHandler;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.session.SessionIdStrategy;

import java.util.Random;

import static failing_single_jvm.SamplePingPong.PORT_TO_LIBRARY_MAKER;
import static java.util.Collections.singletonList;

public class DummyMaker
        implements DictionaryAcceptor {
    private Session session;
    private FixLibrary library;

    public DummyMaker() {
        new Thread(this::run).start();
    }

    private void run() {
        Thread.currentThread().setName("DummyMaker");

        final SessionAcquireHandler sessionAcquireHandler = (session, isSlow) ->
        {
            System.out.println("Session acceptor started");
            this.session = session;
            return new MessageHandler(this);
        };
        LibraryConfiguration libraryConfiguration = new LibraryConfiguration()
                .sessionExistsHandler(new AcquiringSessionExistsHandler())
                .sessionAcquireHandler(sessionAcquireHandler)
                .libraryAeronChannels(singletonList(DummyUtils.buildChannelString(PORT_TO_LIBRARY_MAKER)))
                .sessionIdStrategy(SessionIdStrategy.senderAndTarget());
        library = DummyUtils.blockingConnect(libraryConfiguration);

        final SleepingIdleStrategy sleepingIdleStrategy = new SleepingIdleStrategy(10);

        final ExampleMessageEncoder exampleMessageEncoder = new ExampleMessageEncoder();

        while (true) {
            // rcv
            sleepingIdleStrategy.idle((tryRead() ? 1 : 0));

            // send
            if (session == null) continue;
            exampleMessageEncoder.testReqID(getRandomChars());
            long sendResult = session.send(exampleMessageEncoder);
            if (sendResult < 0) {
                throw new RuntimeException("Producer can't send, error status = " + sendResult);
            } else {
                System.out.println("Maker: send position = " + sendResult);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final char[] randomChars = (
                    "abcdefghijklmnopqrstuvwxyz" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "abcdefghijklmnopqrstuvwxyz")
            .toCharArray();
    private final Random random = new Random();

    private char[] getRandomChars() {
        final int firstSwapIdx = random.nextInt(randomChars.length);
        final int secondSwapIdx = random.nextInt(randomChars.length);

        final char tmp = randomChars[firstSwapIdx];
        randomChars[firstSwapIdx] = randomChars[secondSwapIdx];
        randomChars[secondSwapIdx] = tmp;
        return randomChars;
    }

    private boolean tryRead() {
        int counter = 0;
        int fragmentsRead;
        do {
            fragmentsRead = library.poll(100);
            counter++;
            if ((counter & DummyUtils.COUNTER_MASK) == 0) {
                System.out.println("Maker too many messages in tryRead !!!!!");
            }
        }
        while (fragmentsRead > 0);
        return true;
    }

    @Override
    public void onLogon(final LogonDecoder decoder) {

    }

    @Override
    public void onExampleMessage(final ExampleMessageDecoder decoder) {

    }

    @Override
    public void onResendRequest(ResendRequestDecoder decoder) {

    }

    @Override
    public void onReject(RejectDecoder decoder) {

    }

    @Override
    public void onSequenceReset(SequenceResetDecoder decoder) {

    }

    @Override
    public void onHeartbeat(final HeartbeatDecoder decoder) {

    }

    @Override
    public void onTestRequest(TestRequestDecoder decoder) {

    }

    @Override
    public void onLogout(final LogoutDecoder decoder) {

    }
}
