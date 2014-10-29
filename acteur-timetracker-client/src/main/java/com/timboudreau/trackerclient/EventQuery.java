package com.timboudreau.trackerclient;

import com.mastfrog.netty.http.client.HttpRequestBuilder;
import com.mastfrog.util.Exceptions;
import com.timboudreau.trackerclient.pojos.EventID;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 *
 * @author Tim Boudreau
 */
public final class EventQuery {

    private final List<Term> terms = new LinkedList<>();

    private EventQuery(boolean whatever) {
        // ensures we can't actually be instantiated by Guice with a default
        // constructor - must be bound in scope.
    }

    public static EventQuery create() {
        return new EventQuery(false);
    }
    
    public static EventQuery forId(EventID id) {
        EventQuery eq = new EventQuery(false);
        eq.terms.add(new Term<>(Properties._id, Relations.EQUAL_TO, id));
        return eq;
    }

    public EventQuery lastslessThanOrEqual(Duration dur) {
        terms.add(new Term<>(Properties.duration, Relations.LESS_THAN_OR_EQUAL, dur));
        return this;
    }

    public EventQuery lastsLessThan(Duration dur) {
        terms.add(new Term<>(Properties.duration, Relations.LESS_THAN, dur));
        return this;
    }

    public EventQuery lastsAtLeast(Duration dur) {
        terms.add(new Term<>(Properties.duration, Relations.GREATER_THAN_OR_EQUAL, dur));
        return this;
    }

    public EventQuery lastsLongerThan(Duration dur) {
        terms.add(new Term<>(Properties.duration, Relations.GREATER_THAN, dur));
        return this;
    }

    public EventQuery lasts(Duration dur) {
        terms.add(new Term<>(Properties.duration, Relations.EQUAL_TO, dur));
        return this;
    }

    public EventQuery startsAt(DateTime start) {
        terms.add(new Term<>(Properties.start, Relations.EQUAL_TO, start));
        return this;
    }

    public EventQuery startsAtOrAfter(DateTime start) {
        terms.add(new Term<>(Properties.start, Relations.GREATER_THAN_OR_EQUAL, start));
        return this;
    }

    public EventQuery startsBeforeOrAt(DateTime start) {
        terms.add(new Term<>(Properties.start, Relations.LESS_THAN_OR_EQUAL, start));
        return this;
    }

    public EventQuery endsAt(DateTime start) {
        terms.add(new Term<>(Properties.end, Relations.EQUAL_TO, start));
        return this;
    }

    public EventQuery endsAfter(DateTime start) {
        terms.add(new Term<>(Properties.end, Relations.GREATER_THAN, start));
        return this;
    }

    public EventQuery endsAtOrAfter(DateTime start) {
        terms.add(new Term<>(Properties.end, Relations.GREATER_THAN_OR_EQUAL, start));
        return this;
    }

    public EventQuery endsBeforeOrAt(DateTime start) {
        terms.add(new Term<>(Properties.end, Relations.LESS_THAN_OR_EQUAL, start));
        return this;
    }

    public EventQuery endsBefore(DateTime start) {
        terms.add(new Term<>(Properties.end, Relations.LESS_THAN, start));
        return this;
    }

    public EventQuery add(String name, Relations relation, long value) {
        terms.add(new Term<>(name, relation, value + ""));
        return this;
    }

    public EventQuery add(String name, String value) {
        terms.add(new Term<>(name, Relations.EQUAL_TO, value));
        return this;
    }

    public void appendTo(HttpRequestBuilder bldr) {
        for (Term term : terms) {
            term.appendTo(bldr);
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        boolean first = sb.indexOf("?") < 0;
        if (terms.isEmpty()) {
            System.err.println("EMPTY TERM!");
        }
        for (Term term : terms) {
            if (first) {
                sb.append('?');
            } else {
                sb.append('&');
            }
            System.err.println("TERM " + term);
            sb.append(term);
        }
        return sb;
    }

    public boolean equals(Object o) {
        if (o instanceof EventQuery) {
            Set<Term> s = new HashSet<>(((EventQuery) o).terms);
            Set<Term> t = new HashSet<>(terms);
            return s.equals(t);
        }
        return false;
    }

    public int hashCode() {
        return new HashSet<>(terms).hashCode();
    }

    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    private static final class Term<T> {

        private final String name;
        private final Relations relation;
        private final T term;

        public Term(String name, Relations relation, T term) {
            this.name = name;
            this.relation = relation;
            this.term = term;
        }

        void appendTo(HttpRequestBuilder b) {
            String val;
            if (term instanceof DateTime) {
                val = ((DateTime) term).getMillis() + "";
            } else if (term instanceof Duration) {
                val = ((Duration) term).getMillis() + "";
            } else {
                val = term.toString();
            }
            b.addQueryPair(name, relation + val);
        }

        public String toString() {
            try {
                String n = URLEncoder.encode(name, "UTF-8");
                String val = term.toString();
                if (term instanceof DateTime) {
                    val = ((DateTime) term).getMillis() + "";
                } else if (term instanceof Duration) {
                    val = ((Duration) term).getMillis() + "";
                }
                if (term instanceof EventID) {
                    return n + '=' + URLEncoder.encode(val, "UTF-8");
                }
                return n + '=' + relation + URLEncoder.encode(val, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                return Exceptions.chuck(ex);
            }
        }

        public boolean equals(Object o) {
            return o instanceof Term && o.toString().equals(toString());
        }

        public int hashCode() {
            return toString().hashCode();
        }
    }

    public enum Relations {

        GREATER_THAN_OR_EQUAL(">="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        EQUAL_TO("");
        private final String exp;

        private Relations(String exp) {
            this.exp = exp;
        }

        public String toString() {
            try {
                return URLEncoder.encode(exp, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                return Exceptions.chuck(ex);
            }
        }
    }
}
