package com.timboudreau.trackerapi;

import com.google.common.net.MediaType;
import com.mastfrog.acteur.Acteur;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TTUser;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.guicy.annotations.Defaults;
import com.mastfrog.guicy.annotations.Namespace;
import com.mastfrog.acteur.Event;
import com.mastfrog.acteur.Help;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.ImplicitBindings;
import com.mastfrog.acteur.Page;
import com.mastfrog.acteur.Response;
import com.mastfrog.acteur.annotations.GenericApplication;
import com.mastfrog.acteur.annotations.GenericApplicationModule;
import com.mastfrog.acteur.headers.Headers;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.server.PathFactory;
import com.mastfrog.acteur.util.CacheControl;
import com.mastfrog.acteur.util.Server;
import com.mastfrog.acteur.util.ServerControl;
import com.mastfrog.jackson.JacksonModule;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import com.mastfrog.url.Path;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.timboudreau.trackerapi.ModifyEventsResource.Body;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import org.joda.time.Interval;

/**
 * The Timetracker main class
 *
 * @author Tim Boudreau
 */
// Classes which are injected:
@ImplicitBindings({TTUser.class, DBCollection.class, CreateCollectionPolicy.class,
    DBCursor.class, Interval.class, Body.class, AdjustTimeResource.AdjustParameters.class})
// Some default values for things
@Defaults(namespace
        = @Namespace(Timetracker.TIMETRACKER),
        value = {"periodicLiveWrites=true", "port=7739"})
@Namespace(Timetracker.TIMETRACKER)
@Help("Time Tracker")
@Description("A web api for recording blocks of time associated with ad-hoc attributes")
public class Timetracker extends GenericApplication {

    public static final String TIMETRACKER = "timetracker";
    public static final String URL_PATTERN_TIME = "^users/(.*?)/time/(.*?)$";
    public static final String USER_COLLECTION = "ttusers";
    public static final String URL_USER = "url";
    public static final String OTHER_USER = "other";

    public static void main(String[] args) throws IOException, InterruptedException {
        start(args).await();
    }
    
    public static ServerControl start(String... args) throws IOException {
        System.out.println("ARGS " + java.util.Arrays.asList(args));
        // Set up our defaults - can be overridden in
        // /etc/timetracker.json, ~/timetracker.json and ./timetracker.json
        // or with command-line arguments
        Settings settings = SettingsBuilder.forNamespace(TIMETRACKER)
                .addDefaultLocations()
                .add(PathFactory.BASE_PATH_SETTINGS_KEY, "time")
                .parseCommandLineArguments(args)
                .build();

        // Set up the Guice injector with our settings and modules.  Dependencies
        // will bind our settings as @Named
        Dependencies deps = Dependencies.builder()
                .add(settings, TIMETRACKER)
                .add(settings, Namespace.DEFAULT)
                .add(new JacksonModule())
                .add(new GenericApplicationModule(settings, Timetracker.class, new Class[0])
                ).build();

        // Get the port we're using
        int port = settings.getInt("port", 7739);
        // Insantiate the server, start it and wait for it to exit
        Server server = deps.getInstance(Server.class);
        return server.start(port);
    }

    @Override
    protected void onBeforeSendResponse(HttpResponseStatus status, Event<?> event, Response response, Acteur acteur, Page lockedPage) {
        // Adds cache control and content type headers to everything
        response.add(Headers.SERVER, getName());
        Path path = ((HttpEvent) event).getPath();
        // Leave it off for html help and error responses
        if (status.code() >= 200 && status.code() < 300 && !"help".equals(path.toString())) {
            response.add(Headers.CACHE_CONTROL, CacheControl.PRIVATE_NO_CACHE_NO_STORE);
            response.add(Headers.CONTENT_TYPE, MediaType.JSON_UTF_8);
        }
    }

    /**
     * Write a single key and value as json with minimal overhead
     * @param key The key
     * @param value The value
     * @return JSON
     */
    public static String quickJson(String key, Object value) {
        StringBuilder sb = new StringBuilder("{").append('"').append(key).append('"').append(':');
        if (value instanceof String) {
            sb.append('"');
        }
        sb.append(value);
        if (value instanceof String) {
            sb.append('"');
        }
        sb.append("}\n");
        return sb.toString();
    }
}