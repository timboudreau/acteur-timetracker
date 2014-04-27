package com.timboudreau.trackerapi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import com.mastfrog.acteur.util.Realm;
import com.mastfrog.acteur.util.RotatingRealmProvider;
import com.mastfrog.jackson.JacksonConfigurer;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import static com.timboudreau.trackerapi.Timetracker.OTHER_USER;
import static com.timboudreau.trackerapi.Timetracker.URL_USER;
import static com.timboudreau.trackerapi.Timetracker.USER_COLLECTION;
import com.timboudreau.trackerapi.support.AuthenticatorImpl;
import com.timboudreau.trackerapi.support.TTUser;
import com.timboudreau.trackerapi.support.UserFromUrl;
import java.io.IOException;
import org.bson.types.ObjectId;
import org.openide.util.lookup.ServiceProvider;

/**
 * Initializes the Time Tracker application
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
        bind(BasicDBObject.class).toProvider(EventToQuery.class);
        bind(Authenticator.class).to(AuthenticatorImpl.class);
        bind(Realm.class).toProvider(RotatingRealmProvider.class);

        Provider<DBCollection> userCollectionProvider = binder().getProvider(Key.get(DBCollection.class, Names.named(USER_COLLECTION)));

        bind(TTUser.class).annotatedWith(Names.named(URL_USER))
                .toProvider(new UserFromUrl(userCollectionProvider, binder().getProvider(HttpEvent.class), 1));

        bind(TTUser.class).annotatedWith(Names.named(OTHER_USER))
                .toProvider(new UserFromUrl(userCollectionProvider, binder().getProvider(HttpEvent.class), 3));
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

    @ServiceProvider(service = JacksonConfigurer.class)
    public static final class JacksonC implements JacksonConfigurer {

        @Override
        public ObjectMapper configure(ObjectMapper om) {
            om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            SimpleModule sm = new SimpleModule("mongo", new Version(1, 0, 0, null, "com.timboudreau", "trackerapi"));
            sm.addSerializer(new C());
            om.registerModule(sm);
            return om;
        }

        static class C extends JsonSerializer<ObjectId> {

            @Override
            public Class<ObjectId> handledType() {
                return ObjectId.class;
            }

            @Override
            public void serialize(ObjectId t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
                String id = t.toStringMongod();
                jg.writeString(id);
            }
        }
    }
}
