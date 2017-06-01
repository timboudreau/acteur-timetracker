package com.timboudreau.trackerapi;

import com.google.common.net.MediaType;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.giulius.annotations.Defaults;
import com.mastfrog.giulius.annotations.Namespace;
import com.mastfrog.acteur.Event;
import com.mastfrog.acteur.Help;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Page;
import com.mastfrog.acteur.Response;
import com.mastfrog.acteur.annotations.GenericApplication;
import com.mastfrog.acteur.headers.Headers;
import com.mastfrog.acteur.mongo.util.UpdateBuilder;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.server.PathFactory;
import com.mastfrog.acteur.server.ServerBuilder;
import com.mastfrog.acteur.util.CacheControl;
import com.mastfrog.acteur.util.PasswordHasher;
import com.mastfrog.acteur.util.Server;
import com.mastfrog.acteur.util.ServerControl;
import com.mastfrog.jackson.DurationSerializationMode;
import com.mastfrog.jackson.JacksonModule;
import com.mastfrog.jackson.TimeSerializationMode;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import com.mastfrog.url.Path;
import com.mastfrog.util.Exceptions;
import com.mastfrog.util.Streams;
import com.mastfrog.util.time.Interval;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.timboudreau.trackerapi.ModifyEventsResource.Body;
import static com.timboudreau.trackerapi.Properties.name;
import static com.timboudreau.trackerapi.Properties.pass;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.io.InputStream;

/**
 * The Timetracker main class
 *
 * @author Tim Boudreau
 */
// Classes which are injected:
//@ImplicitBindings({TTUser.class, DBCollection.class, CreateCollectionPolicy.class,
//    DBCursor.class, Interval.class, Body.class, AdjustTimeResource.AdjustParameters.class})
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
        // Set up our defaults - can be overridden in
        // /etc/timetracker.json, ~/timetracker.json and ./timetracker.json
        // or with command-line arguments
        Settings settings = SettingsBuilder.forNamespace(TIMETRACKER)
                .add("port", "7739")
                .addDefaultLocations()
                .add(PathFactory.BASE_PATH_SETTINGS_KEY, "time")
                .parseCommandLineArguments(args)
                .add(loadVersionProperties())
                .build();

        // Set up the Guice injector with our settings and modules.  Dependencies
        // will bind our settings as @Named
        Server server = new ServerBuilder()
                .add(new JacksonModule().withJavaTimeSerializationMode(TimeSerializationMode.TIME_AS_EPOCH_MILLIS,
                            DurationSerializationMode.DURATION_AS_MILLIS))
                .add(new ResetPasswordModule())
                .withType(Interval.class, 
                        DBCursor.class, 
                        Interval.class, 
                        Body.class
                        )
                .applicationClass(Timetracker.class)
                .add(settings).build();

        return server.start();
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
     *
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

    static class PasswordResetAndExit {

        @Inject
        PasswordResetAndExit(Settings settings, @Named(USER_COLLECTION) DBCollection coll, PasswordHasher hasher) {
            boolean isReset = settings.getBoolean("reset", false);
            if (isReset) {
                String user = settings.getString("user");
                String password = settings.getString("password");
                System.out.println("Attempt to reset password for " + user);
                if (user != null && password != null) {
                    String hashed = hasher.encryptPassword(password);
                    DBObject query = coll.findOne(new BasicDBObject(name, user));
                    if (query != null) {
                        DBObject update = UpdateBuilder.$().increment(Properties.version).set(pass, hashed).build();
                        WriteResult res = coll.update(query, update, false, false, WriteConcern.FSYNCED);
                        System.out.println("Updated password for user " + user + " with result " + res);
                    } else {
                        System.out.println("Failed to update password for user - no such user " + user);
                    }
                } else {
                    System.out.println("Could not update password - user " + user + " password " + password);
                }
                System.exit(0);
            }
        }
    }

    static class ResetPasswordModule implements Module {

        @Override
        public void configure(Binder binder) {
            // Allows a user's password to be reset by starting the application
            // on the command-line with --reset true --user user --password password
            binder.bind(PasswordResetAndExit.class).asEagerSingleton();
        }

    }

    static java.util.Properties loadVersionProperties() {
        String pth = "META-INF/maven/com/mastfrog/trackerapi/pom.properties";
        InputStream[] streams = Streams.locate(pth);
        java.util.Properties result = new java.util.Properties();
        if (streams != null && streams.length > 0) {
            try (InputStream in = streams[0]) {
                System.out.println("Load maven properties");
                result.load(in);
            } catch (IOException ioe) {
                return Exceptions.chuck(ioe);
            }
        } else {
            result.setProperty("version", "1.5.2");
        }
        return result;
    }
}
