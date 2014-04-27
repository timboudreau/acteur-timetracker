Timetracker Web API
===================

This is a sample application for [Acteur](https://github.com/timboudreau/acteur) in the form of a
simple multi-user web api backed by MongoDB for recording time intervals which have ad-hoc metadata
associated with them.

It also includes a sample web API client which uses the Netty http client and generic-web-api framework.

Build And Run
-------------

To build and run the application, simply build the ``acteur-timetracker`` project and run the resulting
JAR file with ``java -jar`` - all dependencies are included in it.  The port to run on, location of the
mongodb database and related settings can be specified with command-line arguments if different than
the defaults, e.g.

		java -jar trackerapi.jar --port 8080 --mongoHost localhost --mongoPort 27017

Web API
-------

The API is self-documenting - calls are specified by Acteurs with annotations, which are also used to 
generate live help - just run the application and navigate to ``/help?html=true`` or read [this static copy
of the help](help.html).

The API is built around the concept of _time events_, which are records in the database which have a start
and an end.  These can be generated either by making an HTTP ``PUT`` to ``/users/$USER_NAME/time/$SERIES``
with url parameters ``start`` and ``end`` in standard milliseconds-since-epoch (1/1/1970).  Or you can
open a connection to ``/users/$USER_NAME/sessions/$SERIES`` to start a time period which will end when the
connection is closed - since we're using Netty, we do not pay with one thread for every open connection.

The ``$USER_NAME`` is specified when you sign up, which is done by making an HTTP ``PUT`` to 
``/users/$USER_NAME/signup?displayName=$DISPLAY_NAME`` with the password as the request payload.
Authentication uses HTTP basic auth.

The ``$SERIES`` is just a name for a collection of time events - users may have many.

Users can also authorize other users to see their data, by making an HTTP ``PUT`` to 
``/users/$USER_NAME/authorize/$OTHER_USER_NAME``.

  * Users have names, display names, zero or more series, and zero or more other users they authorize
  * Series have time events
  * Time events have a start, end and duration, along with whatever other properties you want to attach to
them

Querying is flexible.  In particular, numeric properties such as ``start``, ``end`` and ``dur`` can be
queried for using a simple syntax for greater/less than:

 * ``?start=>=1398477982294`` will return events with a start time greater than or equal to 1398477982294
 * ``?start=>1398477982294`` will return events with a start time greater than 1398477982294
 * ``?start=<=1398477982294`` will return events with a start time less than or equal to 1398477982294
 * ``?start=<1398477982294`` will return events with a start time less than 1398477982294


Code
----

The code demonstrates a number of the newer features of Acteur and its MongoDB support.  For example, to
handle a query along the lines of the ones described above, you simply ask for a ``BasicDBObject`` to be
injected and pass that as a query to the collection you want - acteur-mongodb's ``EventToQuery`` provides
configurable generation of queries from URL parameters.

Annotations are used heavily, and most ``Page`` classes are generated from them - so a typical acteur will
look like this:

	@HttpCall
	@BasicAuth
	@Methods({PUT, POST})
	@PathRegex(Timetracker.URL_PATTERN_TIME)
	@RequiredUrlParameters({"start", "end"})
	@BannedUrlParameters({"added", "type"})
	@Precursors({CheckParameters.class, CreateCollectionPolicy.CreatePolicy.class, 
	    AuthorizedChecker.class, TimeCollectionFinder.class})
	@Description("Add A Time Event")
	final class AddTimeResource extends Acteur {

	    @Inject
	    AddTimeResource(HttpEvent evt, DBCollection coll, TTUser user, Interval interval) throws IOException {   
		...

thus an individual Acteur is a small object whose constructor runs only that logic that it is 
concerned with.  Its precursors list contains other Acteurs which implement other bits of logic
and may provide objects for injection into this one, or abort the request if some validation
condition fails.

The application itself extends ``GenericApplication``;  this uses an annotation processor to generate
files in ``META-INF/http`` which list the ``Page`` classes that make up the application (which in our
case, are themselves generated).  So the application does not need to have the set of HTTP endpoints
it knows about hard-coded into it - rather it looks up the set of all pages annotated with ``@HttpCall``
on the classpath.  This means that this application is also usable as a library within a larger web
application.


Java Client API
---------------

The Java client is largely self explanatory, and uses the generic-web-api project to define its API
as a set of annotations with templated URLs and argument types.  Its unit tests form a test suite for
both itself and the time tracker server.

To use it, create a Guice injector including ``TrackerModule``.  Then ask the injector for an instance of
``TrackerClientSPI``.  Call its ``signIn`` or ``signup()`` methods to create a ``TrackerSession`` (there
is no server-side state, but it will keep track of your credentials for you).  The methods on that
provide access to the full web api.

API access is callback-based - this is an asychronous api, so you do not wait for http calls to return,
but pass a callback which will be invoked once it has.



