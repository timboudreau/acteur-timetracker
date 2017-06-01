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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.util.collections.MapBuilder;
import com.mastfrog.util.time.TimeUtil;
import com.mastfrog.webapi.Interpolator;
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
import io.netty.handler.codec.http.HttpHeaders;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
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

    /*
<Event{interval=01:00.000 from 2017-05-31T19:20:00.851Z to 2017-05-31T19:21:00.851Z, id=foo, duration=PT1M, created=2017-05-31T15:30:00.851-04:00[America/New_York], createdBy=bar, added=2017-05-31T15:30:02.851-04:00[America/New_York], version=1, type=wug, metadata={a=b}, tags=tag1,tag2}>
 Event{interval=01:00.000 from 2017-05-31T19:20:00.851Z to 2017-05-31T19:21:00.851Z, id=foo, duration=PT1M, created=2017-05-31T15:30:00.851-04:00[America/New_York], createdBy=bar, added=2017-05-31T15:30:02.851-04:00[America/New_York], version=1, type=wug, metadata={a=b, empty=false, endMillis=1496258460851, startMillis=1496258400851}, tags=tag1,tag2}>
     */
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
        System.out.println("OID -> " + s);
        OtherID test = mapper.readValue(s, OtherID.class);
        assertEquals(oid, test);

    }

    @Test
    public void test(ObjectMapper mapper, Interpreter interp) throws Throwable {

//        ZonedDateTime now = ZonedDateTime.now();
//        ZonedDateTime str = now.minus(Duration.ofMinutes(10));
//        ZonedDateTime then = str.plus(Duration.ofMinutes(1));
//        Event e = new Event(TimeUtil.toUnixTimestamp(str), TimeUtil.toUnixTimestamp(then), new EventID("foo"),
//                now, new OtherID("bar"), now.plusSeconds(2), 1, Duration.between(str, then), "wug", new MapBuilder().put("a", "b").build(),
//                new String[]{"tag1", "tag2"}, false, new EventID[]{new EventID("x"), new EventID("y")});
//
//        String s = mapper.writeValueAsString(e);
//        System.out.println("\n\n\n" + s + "\n\n\n");
//        Event found = mapper.readValue(s, Event.class);
//        assertEquals(e, found);
        Instant inst = Instant.now();
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

        Event evt = interp.interpret(OK, HttpHeaders.EMPTY_HEADERS, buf, Event.class);
        System.out.println("EVENT: " + evt);
        assertEquals(1, TimeUtil.toUnixTimestamp(evt.start));
        assertEquals(101, TimeUtil.toUnixTimestamp(evt.end));
        assertEquals("foob", evt.createdBy.name);
        assertEquals(100, evt.duration.toMillis());
        assertEquals(inst, evt.added.toInstant());
        assertEquals(35, evt.version);
    }
}
