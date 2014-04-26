package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.timboudreau.trackerapi.support.CreateCollectionPolicy;
import com.timboudreau.trackerapi.support.TTUser;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.guicy.annotations.Defaults;
import com.mastfrog.guicy.annotations.Namespace;
import com.mastfrog.acteur.Event;
import com.mastfrog.acteur.Help;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.ImplicitBindings;
import com.mastfrog.acteur.annotations.GenericApplication;
import com.mastfrog.acteur.annotations.GenericApplicationModule;
import com.mastfrog.acteur.server.PathFactory;
import com.mastfrog.acteur.server.ServerModule;
import com.mastfrog.acteur.util.RequestID;
import com.mastfrog.acteur.util.Server;
import com.mastfrog.jackson.JacksonModule;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.timboudreau.trackerapi.ModifyEventsResource.Body;
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
public class Timetracker extends GenericApplication {

    public static final String TIMETRACKER = "timetracker";
    public static final String URL_PATTERN_TIME = "^users/(.*?)/time/(.*?)$";

    public static void main(String[] args) throws IOException, InterruptedException {
        // Set up our defaults - can be overridden in
        // /etc/timetracker.json, ~/timetracker.json and ./timetracker.json
        Settings settings = SettingsBuilder.forNamespace(TIMETRACKER)
                .addDefaultLocations()
                .add(PathFactory.BASE_PATH_SETTINGS_KEY, "time")
                .add("neverKeepAlive", "true").build();

        // Set up the Guice injector
        Dependencies deps = Dependencies.builder()
                .add(settings, TIMETRACKER).
                add(settings, Namespace.DEFAULT).add(
//                        new ServerModule<>(Timetracker.class),
                        new JacksonModule(),
                        new GenericApplicationModule(settings, Timetracker.class, new Class[0])
//                        new TimeTrackerModule(settings)
                ).build();

        // Insantiate the server, start it and wait for it to exit
        Server server = deps.getInstance(Server.class);
        server.start(settings.getInt("port", 7739)).await();
    }

    @Inject
    Timetracker(DB db) {
        db.getCollection("users");
    }

    @Override
    protected void onBeforeEvent(RequestID id, Event<?> event) {
        HttpEvent evt = (HttpEvent) event;
        System.out.println("EVENT: " + evt.getPath());
        super.onBeforeEvent(id, event);
    }

//    @Override
//    protected HttpResponse decorateResponse(Event<?> event, Page page, Acteur action, HttpResponse response) {
//        response.headers().add("Server", getName());
//        // Do no-cache cache control headers for everything
//        if (((HttpEvent) event).getMethod() != Method.OPTIONS) {
//            CacheControl cc = new CacheControl(CacheControlTypes.Private).add(
//                    CacheControlTypes.no_cache).add(CacheControlTypes.no_store);
//            response.headers().add(Headers.CACHE_CONTROL.name().toString(), Headers.CACHE_CONTROL.toString(cc));
//        }
//        // We do JSON for everything, so save setting the content type on every page
//        int code = response.getStatus().code();
//        if (code >= 200 && code < 300) {
//            if (response.headers().get(Headers.CONTENT_TYPE.name()) == null) {
//                Headers.write(Headers.CONTENT_TYPE, MediaType.JSON_UTF_8, response);
//            }
//        }
//        return super.decorateResponse(event, page, action, response);
//    }

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
