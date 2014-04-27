package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.Duration;

/**
 *
 * @author tim
 */
public class Totals {

    public final Duration total;
    public final Event period;
    public final Event[] intervals;

    @JsonCreator
    public Totals(@JsonProperty("total") Duration total, @JsonProperty("period") Event period, @JsonProperty("intervals") Event[] intervals) {
        this.total = total;
        this.period = period;
        this.intervals = intervals;
    }
}
