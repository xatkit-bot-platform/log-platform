package com.xatkit.plugins.log.platform.action;

import com.xatkit.plugins.log.platform.LogPlatform;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.XatkitCore;
import com.xatkit.stubs.StubXatkitCore;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.*;

import java.io.IOException;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class LogActionTest<T extends LogAction> extends AbstractXatkitTest {

    protected static String VALID_MESSAGE = "test";

    protected ListAppender listAppender;

    protected LogPlatform logPlatform;

    private static XatkitCore xatkitCore;

    @BeforeClass
    public static void setUpBeforeClass() {
        xatkitCore = new StubXatkitCore();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (nonNull(xatkitCore)) {
            xatkitCore.shutdown();
        }
    }

    @Before
    public void setUp() throws InterruptedException {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        listAppender = loggerContext.getConfiguration().getAppender("List");
        /*
         * Clear before the test, this logger is used by all the test cases and may contain messages before the first
         * test of this class. We also need to wait in case some messages are pending in the logger.
         */
        Thread.sleep(200);
        listAppender.clear();
        logPlatform = new LogPlatform(xatkitCore, new BaseConfiguration());
    }

    @After
    public void tearDown() {
        listAppender.clear();
    }

    protected abstract T createLogAction(String message);

    protected abstract String expectedLogTag();

    @Test(expected = NullPointerException.class)
    public void constructLogActionNullMessage() throws Exception {
        createLogAction(null);
    }

    @Test
    public void constructLogActionValidMessage() {
        LogAction logAction = createLogAction(VALID_MESSAGE);
        assertThat(logAction.getMessage()).as("Not null message").isNotNull();
        assertThat(logAction.getMessage()).as("Valid message").isEqualTo(VALID_MESSAGE);
    }

    @Test
    public void runValidLogAction() throws IOException, InterruptedException {
        LogAction logAction = createLogAction(VALID_MESSAGE);
        logAction.call();
        /*
         * The underlying logger is asynchronous, wait to ensure that the message has been processed at the logger
         * level.
         */
        Thread.sleep(200);
        assertThat(listAppender.getMessages()).as("Logger contains a single message").hasSize(1);
        assertThat(listAppender.getMessages().get(0)).as(expectedLogTag() + " tag").contains(expectedLogTag());
        assertThat(listAppender.getMessages().get(0)).as("Action message in log").contains(VALID_MESSAGE);
    }

}
