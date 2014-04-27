package com.timboudreau.trackerapi;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.GuiceModule;
import com.mastfrog.acteur.auth.Authenticator;
import com.mastfrog.acteur.mongo.MongoConfig;
import com.mastfrog.acteur.mongo.MongoInitializer;
import com.mastfrog.acteur.mongo.MongoModule;
import com.mastfrog.acteur.mongo.util.EventToQuery;
import com.mastfrog.acteur.mongo.util.EventToQuery.QueryDecorator;
import com.mastfrog.acteur.mongo.util.EventToQueryConfig;
import com.mastfrog.acteur.util.Realm;
import com.mastfrog.acteur.util.RotatingRealmProvider;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import static com.timboudreau.trackerapi.Properties.added;
import static com.timboudreau.trackerapi.Properties.by;
import static com.timboudreau.trackerapi.Properties.detail;
import static com.timboudreau.trackerapi.Properties.duration;
import static com.timboudreau.trackerapi.Properties.end;
import static com.timboudreau.trackerapi.Properties.fields;
import static com.timboudreau.trackerapi.Properties.length;
import static com.timboudreau.trackerapi.Properties.moveTo;
import static com.timboudreau.trackerapi.Properties.newEnd;
import static com.timboudreau.trackerapi.Properties.newStart;
import static com.timboudreau.trackerapi.Properties.shift;
import static com.timboudreau.trackerapi.Properties.start;
import static com.timboudreau.trackerapi.Properties.summary;
import static com.timboudreau.trackerapi.Timetracker.OTHER_USER;
import static com.timboudreau.trackerapi.Timetracker.URL_USER;
import static com.timboudreau.trackerapi.Timetracker.USER_COLLECTION;
import com.timboudreau.trackerapi.support.AuthenticatorImpl;
import com.timboudreau.trackerapi.support.TTUser;
import com.timboudreau.trackerapi.support.UserFromUrl;
import org.bson.types.ObjectId;

/**
 * Initializes the Time Tracker application.  Implements MongoInitializer so
 * it can be used from other applications
 *
 * @author Tim Boudreau
 */
@GuiceModule
public final class TimeTrackerModule extends AbstractModule implements MongoConfig {

    private final MongoModule mongoModule = new MongoModule("timetracker");

    @Override
    protected void configure() {
        mongoModule.bindCollection(USER_COLLECTION);
        mongoModule.addInitializer(MI.class);
        install(mongoModule);
        bind(Authenticator.class).to(AuthenticatorImpl.class);
        bind(Realm.class).toProvider(RotatingRealmProvider.class);

        Provider<HttpEvent> httpEvents = binder().getProvider(HttpEvent.class);

        Provider<DBCollection> userCollectionProvider = binder().getProvider(Key.get(DBCollection.class, Names.named(USER_COLLECTION)));

        bind(TTUser.class).annotatedWith(Names.named(URL_USER))
                .toProvider(new UserFromUrl(userCollectionProvider, httpEvents, 1));

        bind(TTUser.class).annotatedWith(Names.named(OTHER_USER))
                .toProvider(new UserFromUrl(userCollectionProvider, httpEvents, 3));

        EventToQueryConfig eqconfig = EventToQueryConfig.builder()
                .addIgnored(fields, detail, summary, shift, length, moveTo, newStart, newEnd)
                .addNumeric(added, duration, end, start)
                .add(new QueryDecoratorImpl())
                .build();

        EventToQuery q = new EventToQuery(httpEvents, eqconfig);
        bind(BasicDBObject.class).toProvider(q);
    }

    private static class QueryDecoratorImpl implements QueryDecorator {

        @Override
        public BasicDBObject onQueryConstructed(HttpEvent evt, BasicDBObject obj) {
            obj.put(Properties.type.toString(), Properties.time.toString());
            String uid = evt.getParameter(by);
            if (uid != null) {
                obj.put("by", new ObjectId(uid));
            }
            return obj;
        }
    }

    @Override
    public MongoConfig addInitializer(Class type) {
        return mongoModule.addInitializer(type);
    }

    @Override
    public MongoConfig bindCollection(String bindingName) {
        return mongoModule.bindCollection(bindingName);
    }

    @Override
    public MongoConfig bindCollection(String bindingName, String collectionName) {
        return mongoModule.bindCollection(bindingName, collectionName);
    }

    private static class MI extends MongoInitializer {

        @Inject
        public MI(Registry registry) {
            super(registry);
        }

        @Override
        protected void onCreateCollection(DBCollection collection) {
            if (USER_COLLECTION.equals(collection.getName())) {
                collection.ensureIndex("name");
            }
        }
    }
}
