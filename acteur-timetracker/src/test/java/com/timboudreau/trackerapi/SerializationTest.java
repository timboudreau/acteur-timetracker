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
package com.timboudreau.trackerapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.mastfrog.acteur.server.ServerModule;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.jackson.DurationSerializationMode;
import com.mastfrog.jackson.TimeSerializationMode;
import com.mastfrog.jackson.*;
import com.mongodb.BasicDBObject;
import static com.timboudreau.trackerapi.Properties.added;
import static com.timboudreau.trackerapi.Properties.by;
import static com.timboudreau.trackerapi.Properties.duration;
import static com.timboudreau.trackerapi.Properties.end;
import static com.timboudreau.trackerapi.Properties.start;
import static com.timboudreau.trackerapi.Properties.time;
import static com.timboudreau.trackerapi.Properties.type;
import static com.timboudreau.trackerapi.Properties.version;
import com.timboudreau.trackerapi.SerializationTest.TestConfig;
import java.time.Instant;
import org.bson.types.ObjectId;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(GuiceRunner.class)
@TestWith(TestConfig.class)
public class SerializationTest {

    @Test
    public void test(ObjectMapper mapper) throws JsonProcessingException {
        BasicDBObject toWrite = new BasicDBObject(type, time)
                .append(start, 1)
                .append(end, 101)
                .append(duration, 100)
                .append(added, Instant.now().toEpochMilli())
                .append(by, "foob")
                .append(version, 0);
        
        ObjectId id = new ObjectId();
        toWrite.append("_id", id);
        
        String ids = mapper.writeValueAsString(id);
        assertTrue("ID serialized should look like a quoted string, not '" + ids + "'", ids.startsWith("\""));
        assertTrue("ID serialized should look like a quoted string, not '" + ids + "'",ids.endsWith("\""));
        System.out.println(mapper.writeValueAsString(toWrite));
        

    }

    static final class TestConfig extends AbstractModule {

        @Override
        protected void configure() {
            install(new JacksonModule().withJavaTimeSerializationMode(TimeSerializationMode.TIME_AS_EPOCH_MILLIS,
                    DurationSerializationMode.DURATION_AS_MILLIS));
            install(new TimeTrackerModule());
//            install(new Timetracker.ResetPasswordModule());
//            bind(Interval.class);
//            bind(DBCursor.class);
//            bind(ModifyEventsResource.Body.class);
            install(new ServerModule<>(Timetracker.class));
        }

    }
}
