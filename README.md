acteur-timetracker
==================

A generic JSON REST web api for keeping track of little blobs of time and 
associating ad-hoc data with them.  In addition to being useful, it serves as 
a demo of the [acteur](../../acteur) Netty-based server framework, 
and [giulius](../../giulius) for simple Guice injection.

Prerequisites
-------------

You need [MongoDB](http://mongodb.org) installed, or running somewhere accessible, to run this project.
By default it will try to talk to MongoDB on ``localhost:27017``.  If it is somewhere else, create
a file ``timetracker.properties`` in the process working dir, current user's home directory or `/etc/` with
that name, and include in it and set the properties ``mongoHost`` and ``mongoPort`` appropriately.

Build and Run
-------------

The project can be built and run using Maven.  To build it as a standalone JAR file, run

    mvn clean install assembly:assembly

which will result in a server JAR file ``target/timetracker-1.0-standalone.jar`` which you
can simply run with ``java -jar timetracker-1.0-standalone.jar``.
