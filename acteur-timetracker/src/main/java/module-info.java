open module com.mastfrog.trackerapi {
// Generated by com.dv.sourcetreetool.impl.App

    exports com.timboudreau.trackerapi;
    exports com.timboudreau.trackerapi.support;

    // derived from com.fasterxml.jackson.core/jackson-databind-2.13.2.2 in com/fasterxml/jackson/core/jackson-databind/2.13.2.2/jackson-databind-2.13.2.2.pom
    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.mastfrog.acteur.annotation.processors;
    requires transitive com.mastfrog.acteur.mongo;
    requires transitive com.mastfrog.giulius.annotations;
    requires transitive com.mastfrog.giulius.tests;
    requires transitive com.mastfrog.jackson;
    requires transitive com.mastfrog.numble;
    requires java.logging;

    // derived from junit/junit-4.13.2 in junit/junit/4.13.2/junit-4.13.2.pom
    requires transitive junit.junit;

    // derived from org.javassist/javassist-3.28.0-GA in org/javassist/javassist/3.28.0-GA/javassist-3.28.0-GA.pom
    requires transitive org.javassist.javassist;

    // derived from org.mongodb/mongo-java-driver-3.12.11 in org/mongodb/mongo-java-driver/3.12.11/mongo-java-driver-3.12.11.pom
    requires transitive org.mongodb.mongo.java.driver;

}
