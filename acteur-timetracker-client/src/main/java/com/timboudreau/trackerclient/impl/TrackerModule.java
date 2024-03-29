package com.timboudreau.trackerclient.impl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.jackson.JacksonModule;
import com.mastfrog.jackson.configuration.DurationSerializationMode;
import com.mastfrog.jackson.configuration.TimeSerializationMode;
import com.mastfrog.netty.http.client.HttpClient;
import com.mastfrog.url.URL;
import com.mastfrog.webapi.WebApiModule;
import com.timboudreau.trackerclient.TrackerAPI;
import com.timboudreau.trackerclient.TrackerClientSPI;
import java.io.IOException;
import org.netbeans.validation.api.Problems;

/**
 *
 * @author Tim Boudreau
 */
public class TrackerModule extends AbstractModule {

    private final String url;

    public TrackerModule() {
        this("http://localhost:7739");
    }

    public TrackerModule(String url) {
        this.url = url == null ? "http://localhost:7739" : url;
    }

    @Override
    protected void configure() {
        install(new WebApiModule<TrackerAPI>(TrackerAPI.class));
        URL u = URL.parse(url);
        bind(HttpClient.class).toInstance(HttpClient.builder().noCompression().followRedirects().build());
        Problems p = u.getProblems();
        if (p != null) {
            p.throwIfFatalPresent("Bad URL " + u + ": ");
        }
        bind(URL.class).toInstance(u);
        bind(String.class).annotatedWith(Names.named("baseUrl")).toInstance(url);
        install(new JacksonModule()
                    .withJavaTimeSerializationMode(TimeSerializationMode.TIME_AS_EPOCH_MILLIS,
                            DurationSerializationMode.DURATION_AS_MILLIS));
    }

    public static TrackerClientSPI create() throws IOException {
        return create(null);
    }

    public static TrackerClientSPI create(String url) throws IOException {
        Dependencies deps = new Dependencies(new TrackerModule(url));
        return deps.getInstance(TrackerClientSPI.class);
    }
}
