// Generated by com.dv.sourcetreetool.impl.App
open module com.mastfrog.trackerapi {
    exports com.timboudreau.trackerapi;
    exports com.timboudreau.trackerapi.support;

    // Sibling com.mastfrog/acteur-annotation-processors-3.0.0-dev:compile
    requires com.mastfrog.acteur.annotation.processors;

    // Transitive detected by source scan
    requires com.mastfrog.acteur.deprecated;

    // Sibling com.mastfrog/acteur-mongo-3.0.0-dev
    requires com.mastfrog.acteur.mongo;

    // Sibling com.mastfrog/giulius-annotations-3.0.0-dev
    requires com.mastfrog.giulius.annotations;

    // Sibling com.mastfrog/jackson-3.0.0-dev
    requires com.mastfrog.jackson;

    // Inferred from source scan
    requires com.mastfrog.misc;

    // Sibling com.mastfrog/numble-3.0.0-dev
    requires com.mastfrog.numble;

    // Inferred from source scan
    requires com.mastfrog.preconditions;

    // Inferred from source scan
    requires com.mastfrog.streams;

    // Inferred from source scan
    requires com.mastfrog.time;

    // derived from com.fasterxml.jackson.core/jackson-databind-0.0.0-? in com/fasterxml/jackson/core/jackson-databind/2.9.9.3/jackson-databind-2.9.9.3.pom
    requires transitive jackson.databind;
    requires java.logging;

    // derived from org.javassist/javassist-3.28.0-GA in org/javassist/javassist/3.28.0-GA/javassist-3.28.0-GA.pom
    requires transitive javassist.3.28.0.GA;

    // derived from org.mongodb/mongo-java-driver-0.0.0-? in org/mongodb/mongo-java-driver/3.12.11/mongo-java-driver-3.12.11.pom
    requires transitive mongo.java.driver.3.12.11;

}
