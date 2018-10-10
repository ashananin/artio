package failing_single_jvm;

import io.aeron.driver.MediaDriver;
import org.agrona.IoUtil;
import uk.co.real_logic.artio.builder.LogonEncoder;
import uk.co.real_logic.artio.builder.LogoutEncoder;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.session.SessionCustomisationStrategy;

import java.io.File;

import static io.aeron.CommonContext.IPC_CHANNEL;
import static io.aeron.driver.ThreadingMode.*;

public class DummyUtils
{
    static final int COUNTER_MASK = ((pow2(20)) - 1);
    public static final SessionCustomisationStrategy NORMAL_LOGON_STRATEGY = new SessionCustomisationStrategy()
    {
        @Override
        public void configureLogon(final LogonEncoder logonEncoder, final long l)
        {
            logonEncoder.resetResetSeqNumFlag();
        }

        @Override
        public void configureLogout(final LogoutEncoder logoutEncoder, final long l)
        {
        }
    };

    public static FixLibrary blockingConnect(final LibraryConfiguration configuration)
    {
        final FixLibrary library = FixLibrary.connect(configuration);
        while (!library.isConnected())
        {
            library.poll(1);
            Thread.yield();
        }
        return library;
    }

    public static String buildChannelString(final int port)
    {
        //return "aeron:udp?endpoint=localhost:" + port; // todo: builder
        return IPC_CHANNEL;
    }

    public static int pow2(final int power)
    {
        return 1 << power;
    }

    private static final int TERM_BUF_LENGTH = 64 * 1024;

    public static MediaDriver startDefaultMediaDriver()
    {

        final MediaDriver.Context context = new MediaDriver.Context()
            .threadingMode(SHARED)
            .publicationTermBufferLength(TERM_BUF_LENGTH)
            .ipcTermBufferLength(TERM_BUF_LENGTH)
            .dirDeleteOnStart(true);
        return MediaDriver.launch(context);
    }

    public static void cleanupOldLogFileDir(final EngineConfiguration configuration)
    {
        IoUtil.delete(new File(configuration.logFileDir()), true);
    }

    public static EngineConfiguration getConfiguration(final int port)
    {
        return new EngineConfiguration().libraryAeronChannel(DummyUtils.buildChannelString(port));
    }
}
