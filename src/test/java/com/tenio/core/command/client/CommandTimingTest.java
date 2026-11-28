/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.command.client;

import com.tenio.common.data.DataCollection;
import com.tenio.core.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTimingTest {

    @Test
    void durationTrackingIsDisabledUntilExplicitlyEnabledAndCountsFailures() {
        var manager = new ClientCommandManager();
        var handler = new AbstractClientCommandHandler<>() {
            @Override
            public void execute(Player player, DataCollection message) {
            }
        };
        manager.registerCommand((short) 1, handler);
        manager.invoke((short) 1, null, null);
        assertEquals(0, manager.executionStatistics().completed());
        manager.enableExecutionMonitoring();
        manager.invoke((short) 1, null, null);
        assertEquals(1, manager.executionStatistics().completed());
        manager.registerCommand(
                (short) 2,
                new AbstractClientCommandHandler<>() {
                    @Override
                    public void execute(Player player, DataCollection message) {
                        throw new IllegalStateException("failure");
                    }
                }
        );
        assertThrows(IllegalStateException.class, () -> manager.invoke((short) 2, null, null));
        var result = manager.executionStatistics();
        assertEquals(2, result.completed());
        assertEquals(1, result.failures());
        assertTrue(result.maximumNanos() >= result.averageNanos());
        assertTrue(result.p95UpperBoundNanos() >= result.maximumNanos());
        manager.clear();
        assertEquals(0, manager.executionStatistics().completed());
    }
}
