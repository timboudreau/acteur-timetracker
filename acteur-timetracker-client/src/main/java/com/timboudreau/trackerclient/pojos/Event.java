package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.MutableInterval;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadableInterval;
import org.joda.time.ReadablePeriod;

/**
 *
 * @author Tim Boudreau
 */
public final class Event implements ReadableInterval {

    public final Interval interval;
    public final EventID id;
    @JsonIgnore
    public final Duration duration;
    public final DateTime created;
    public final OtherID createdBy;
    private final DateTime added;
    public final int version;
    public final String type;
    public Map<String, Object> metadata = new LinkedHashMap<>();
    public final String[] tags;
    public final EventID[] ids;

    @JsonCreator
    public Event(@JsonProperty("start") long start,
            @JsonProperty("end") long end,
            @JsonProperty(value="_id", required=false) EventID id,
            @JsonProperty(value ="created", required=false) DateTime created,
            @JsonProperty(value="by", required=false) OtherID createdBy,
            @JsonProperty(value = "added") DateTime added,
            @JsonProperty(value = "version", required = false) int version,
            @JsonProperty(value = "dur", required = false) Duration duration,
            @JsonProperty(value = "type", required = false) String type,
            @JsonProperty(value = "metadata", required = false) Map<String, Object> metadata,
            @JsonProperty(value = "tags", required = false) String[] tags,
            @JsonProperty(value = "ids", required=false) EventID[] ids) {
        this.interval = new Interval(start, end);
        this.id = id;
        this.ids = ids == null ? id == null ? new EventID[0] : new EventID[]{id} : ids;
        this.created = created;
        this.createdBy = createdBy;
        this.duration = duration == null ? this.interval.toDuration() : duration;
        this.type = type == null ? "time" : type;
        this.added = added;
        this.version = version;
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        this.tags = tags == null ? new String[0] : tags;
    }

    @JsonAnySetter
    public void any(String key, Object value) {
        metadata.put(key, value);
    }

    public Event shift(Duration dur) {
        Interval nue = new Interval(interval.getStartMillis() + dur.getMillis(),
                interval.getEndMillis() + dur.getMillis());
        return new Event(nue.getStartMillis(), nue.getEndMillis(), id,
                created, createdBy, added, version + 1, nue.toDuration(), type,
                Maps.newHashMap(metadata), tags.clone(), ids);
    }
    
    public Object getProperty(String name) {
        return metadata.get(name);
    }

    public Interval toInterval() {
        return interval.toInterval();
    }

    public Interval overlap(ReadableInterval interval) {
        return this.interval.overlap(interval);
    }

    public Interval gap(ReadableInterval interval) {
        return this.interval.gap(interval);
    }

    public boolean abuts(ReadableInterval interval) {
        return this.interval.abuts(interval);
    }

    public Interval withChronology(Chronology chronology) {
        return interval.withChronology(chronology);
    }

    public Interval withStartMillis(long startInstant) {
        return interval.withStartMillis(startInstant);
    }

    public Interval withStart(ReadableInstant start) {
        return interval.withStart(start);
    }

    public Interval withEndMillis(long endInstant) {
        return interval.withEndMillis(endInstant);
    }

    public Interval withEnd(ReadableInstant end) {
        return interval.withEnd(end);
    }

    public Interval withDurationAfterStart(ReadableDuration duration) {
        return interval.withDurationAfterStart(duration);
    }

    public Interval withDurationBeforeEnd(ReadableDuration duration) {
        return interval.withDurationBeforeEnd(duration);
    }

    public Interval withPeriodAfterStart(ReadablePeriod period) {
        return interval.withPeriodAfterStart(period);
    }

    public Interval withPeriodBeforeEnd(ReadablePeriod period) {
        return interval.withPeriodBeforeEnd(period);
    }

    public Chronology getChronology() {
        return interval.getChronology();
    }

    @Override
    public String toString() {
        return "Event{" + "interval=" + interval + ", id=" + id + ", duration=" 
                + duration + ", created=" + created + ", createdBy=" + createdBy 
                + ", added=" + added + ", version=" + version + ", type=" 
                + type + ", metadata=" + metadata + ", tags=" + tags + '}';
    }

    public long getStartMillis() {
        return interval.getStartMillis();
    }

    public long getEndMillis() {
        return interval.getEndMillis();
    }

    public Duration toDuration() {
        return interval.toDuration();
    }

    public long toDurationMillis() {
        return interval.toDurationMillis();
    }

    @Override
    public DateTime getStart() {
        return interval.getStart();
    }

    @Override
    public DateTime getEnd() {
        return interval.getEnd();
    }

    @Override
    public boolean contains(ReadableInstant instant) {
        return interval.contains(instant);
    }

    @Override
    public boolean contains(ReadableInterval interval) {
        return this.interval.contains(interval);
    }

    @Override
    public boolean overlaps(ReadableInterval interval) {
        return this.interval.overlaps(interval);
    }

    @Override
    public boolean isAfter(ReadableInstant instant) {
        return interval.isAfter(instant);
    }

    @Override
    public boolean isAfter(ReadableInterval interval) {
        return this.interval.isAfter(created);
    }

    @Override
    public boolean isBefore(ReadableInstant instant) {
        return interval.isBefore(instant);
    }

    @Override
    public boolean isBefore(ReadableInterval interval) {
        return this.interval.isBefore(interval);
    }

    @Override
    public MutableInterval toMutableInterval() {
        return this.interval.toMutableInterval();
    }

    @Override
    public Period toPeriod() {
        return interval.toPeriod();
    }

    @Override
    public Period toPeriod(PeriodType type) {
        return interval.toPeriod(type);
    }
}
