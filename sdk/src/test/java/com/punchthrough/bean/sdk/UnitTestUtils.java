package com.punchthrough.bean.sdk;

import android.os.Handler;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class UnitTestUtils {

    public static Handler ImmediatelyRunningHandler() {
        Handler h = mock(Handler.class);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Runnable r = (Runnable) args[0];
                r.run();  // run immediately
                return null;
            }
        }).when(h).post(any(Runnable.class));
        return h;
    }
}
