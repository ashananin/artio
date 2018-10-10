package failing_single_jvm;

import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import uk.co.real_logic.artio.Reply;
import uk.co.real_logic.artio.decoder.*;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.library.SessionAcquireHandler;
import uk.co.real_logic.artio.library.SessionConfiguration;
import uk.co.real_logic.artio.session.Session;

import static failing_single_jvm.SamplePingPong.PORT_TO_LIBRARY_MAKER;
import static java.util.Collections.singletonList;

public class DummyTaker implements DictionaryAcceptor {
    private FixLibrary library;
    private final SessionConfiguration sessionConfig;

    public DummyTaker(final SessionConfiguration sessionConfig) {
        this.sessionConfig = sessionConfig;
        new Thread(this::run).start();
    }

    private void run() {
        Thread.currentThread().setName("DummyTaker");

        final SessionAcquireHandler sessionAcquireHandler = (session1, isSlow) -> new MessageHandler(this);
        library = DummyUtils.blockingConnect(new LibraryConfiguration()
                .sessionAcquireHandler(sessionAcquireHandler)
                .libraryAeronChannels(singletonList(DummyUtils.buildChannelString(PORT_TO_LIBRARY_MAKER))));
        final IdleStrategy idleStrategy = new SleepingIdleStrategy(1);
        final Reply<Session> reply = library.initiate(sessionConfig);
        while (reply.isExecuting()) {
            idleStrategy.idle(library.poll(1));
        }
        System.out.println("Replied with: " + reply.state());

        final SleepingIdleStrategy sleepingIdleStrategy = new SleepingIdleStrategy(10);
        while (true) {
            sleepingIdleStrategy.idle((tryRead() ? 1 : 0));
        }
    }

    public boolean tryRead() {
        int counter = 0;
        int fragmentsRead;
        do {
            fragmentsRead = library.poll(10);
            counter++;
            if ((counter & DummyUtils.COUNTER_MASK) == 0) {
                System.out.println("Taker too many messages in tryRead !!!!!");
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
        System.out.println("Taker gets: " + decoder);
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
