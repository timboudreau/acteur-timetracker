/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.timboudreau.trackerclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.util.time.TimeUtil;
import com.mastfrog.webapi.Interpreter;
import com.mongodb.BasicDBObject;
import static com.timboudreau.trackerapi.Properties.added;
import static com.timboudreau.trackerapi.Properties.by;
import static com.timboudreau.trackerapi.Properties.duration;
import static com.timboudreau.trackerapi.Properties.end;
import static com.timboudreau.trackerapi.Properties.start;
import static com.timboudreau.trackerapi.Properties.time;
import static com.timboudreau.trackerapi.Properties.type;
import static com.timboudreau.trackerapi.Properties.version;
import com.timboudreau.trackerclient.impl.TrackerModule;
import com.timboudreau.trackerclient.pojos.Event;
import com.timboudreau.trackerclient.pojos.EventID;
import com.timboudreau.trackerclient.pojos.OtherID;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import java.io.OutputStream;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.regex.Pattern;
import org.bson.types.ObjectId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(GuiceRunner.class)
@TestWith(TrackerModule.class)
public class EventSerializationTest {

    private static final Pattern QUOTED_STRING = Pattern.compile("^\".*\"$");

    @Test
    public void testIdSerialization(ObjectMapper mapper) throws Throwable {
        EventID wid = new EventID("foo");
        String s1 = mapper.writeValueAsString(wid);
        System.out.println("WID -> " + s1);
        EventID test1 = mapper.readValue(s1, EventID.class);
        assertEquals(wid, test1);
        assertTrue("EventID should serialize to a string", QUOTED_STRING.matcher(s1).matches());
        assertTrue("OtherID should serialize to a string", QUOTED_STRING.matcher(mapper.writeValueAsString(test1)).matches());

        OtherID oid = new OtherID("bar");
        String s = mapper.writeValueAsString(oid);
        OtherID test = mapper.readValue(s, OtherID.class);
        assertEquals(oid, test);

    }

    @Test
    public void test(ObjectMapper mapper, Interpreter interp) throws Throwable {
        Instant inst = Instant.now().with(ChronoField.MICRO_OF_SECOND, 0);
        BasicDBObject toWrite = new BasicDBObject(type, time)
                .append(start, 1)
                .append(end, 101)
                .append(duration, 100)
                .append(added, inst.toEpochMilli())
                .append(by, "foob")
                .append(version, 35);

        ObjectId id = new ObjectId();
        toWrite.append("_id", id);

        ByteBuf buf = Unpooled.buffer();
        try (ByteBufOutputStream out = new ByteBufOutputStream(buf)) {
            mapper.writeValue((OutputStream) out, toWrite);
        }

        Event evt = interp.interpret(OK, EmptyHttpHeaders.INSTANCE, buf, Event.class);
        assertEquals(1, TimeUtil.toUnixTimestamp(evt.start));
        assertEquals(101, TimeUtil.toUnixTimestamp(evt.end));
        assertEquals("foob", evt.createdBy.name);
        assertEquals(100, evt.duration.toMillis());
        assertEquals(inst, evt.added.toInstant());
        assertEquals(35, evt.version);
    }
}
