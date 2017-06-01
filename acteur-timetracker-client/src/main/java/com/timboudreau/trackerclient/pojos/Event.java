package com.timboudreau.trackerclient.pojos;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.collect.Maps;
import com.mastfrog.util.Strings;
import com.mastfrog.util.time.Interval;
import com.mastfrog.util.time.MutableInterval;
import com.mastfrog.util.time.ReadableInterval;
import com.mastfrog.util.time.TimeUtil;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
public final class Event implements ReadableInterval {

    @JsonIgnore
    public final Interval interval;
    public final EventID id;
    @JsonIgnore
    public final Duration duration;
    public final ZonedDateTime created;
    public final OtherID createdBy;
    public final ZonedDateTime added;
    public final int version;
    public final String type;
    public Map<String, Object> metadata = new LinkedHashMap<>();
    public final String[] tags;
    public final EventID[] ids;
    public final ZonedDateTime start;
    public final ZonedDateTime end;
    public final boolean running;

    @JsonCreator
    public Event(@JsonProperty("start") ZonedDateTime start,
            @JsonProperty("end") ZonedDateTime end,
            @JsonProperty(value="_id", required=false) EventID id,
            @JsonProperty(value ="created", required=false) ZonedDateTime created,
            @JsonProperty(value="by", required=false) OtherID createdBy,
            @JsonProperty(value = "added") ZonedDateTime added,
            @JsonProperty(value = "version", required = false) int version,
            @JsonProperty(value = "dur", required = false) Duration duration,
            @JsonProperty(value = "type", required = false) String type,
            @JsonProperty(value = "metadata", required = false) Map<String, Object> metadata,
            @JsonProperty(value = "tags", required = false) String[] tags,
            @JsonProperty(value = "running", required = false) Boolean running,
            @JsonProperty(value = "ids", required=false) EventID[] ids) {
        this.interval = Interval.create(start, end);
        this.id = id;
        this.ids = ids == null ? id == null ? new EventID[0] : new EventID[]{id} : ids;
        assert !start.equals(end);
        this.start = start;
        this.end = end;
        this.created = created;
        this.createdBy = createdBy;
        this.running = running == null ? false : running;
        this.duration = duration == null ? this.interval.toDuration() : duration;
        this.type = type == null ? "time" : type;
        this.added = added;
        this.version = version;
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        System.out.println("MEATDATA IS: " + metadata);
        this.tags = tags == null ? new String[0] : tags;
    }

    @JsonAnySetter
    public void any(String key, Object value) {
        metadata.put(key, value);
    }

    public Event shift(Duration dur) {
        Interval nue = Interval.create(interval.getStartMillis() + dur.toMillis(),
                interval.getEndMillis() + dur.toMillis());
        return new Event(nue.startTime(), nue.endTime(), id,
                created, createdBy, added, version + 1, nue.toDuration(), type,
                Maps.newHashMap(metadata), tags.clone(), running, ids);
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

    public Interval withStartMillis(long startInstant) {
        return interval.withStartMillis(startInstant);
    }

    public Interval withStart(Instant start) {
        return interval.withStart(start);
    }

    public Interval withEndMillis(long endInstant) {
        return interval.withEndMillis(endInstant);
    }

    public Interval withEnd(Instant end) {
        return interval.withEnd(end);
    }

    @Override
    public String toString() {
        return "Event{" + "interval=" + interval + ", id=" + id + ", duration=" 
                + duration + ", created=" + created + ", createdBy=" + createdBy 
                + ", added=" + added + ", version=" + version + ", type=" 
                + type + ", metadata=" + metadata + ", tags=" + Strings.join(',', tags) + '}';
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
    public ZonedDateTime startTime() {
        return interval.startTime();
    }

    @Override
    public ZonedDateTime endTime() {
        return interval.endTime();
    }

    @Override
    public boolean contains(Instant instant) {
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
    public boolean isAfter(Instant instant) {
        return interval.isAfter(instant);
    }

    @Override
    public boolean isAfter(ReadableInterval interval) {
        return this.interval.isAfter(interval);
    }

    @Override
    public boolean isBefore(Instant instant) {
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
    public Instant end() {
        return interval.end();
    }

    @Override
    public boolean isEmpty() {
        return interval.isEmpty();
    }

    @Override
    public boolean overlaps(ChronoZonedDateTime when) {
        return interval.overlaps(when);
    }

    @Override
    public boolean overlaps(Instant instant) {
        return interval.overlaps(instant);
    }

    @Override
    public Instant start() {
        return interval.start();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.id);
        hash = 79 * hash + Objects.hashCode(this.duration.toMillis());
        hash = 79 * hash + Objects.hashCode(this.created.toInstant());
        hash = 79 * hash + Objects.hashCode(this.createdBy);
        hash = 79 * hash + Objects.hashCode(this.added.toInstant());
        hash = 79 * hash + this.version;
        hash = 79 * hash + Objects.hashCode(this.type);
        hash = 79 * hash + Objects.hashCode(this.metadata);
        hash = 79 * hash + Arrays.deepHashCode(this.tags);
        hash = 79 * hash + Arrays.deepHashCode(this.ids);
        hash = 79 * hash + Objects.hashCode(this.start);
        hash = 79 * hash + Objects.hashCode(this.end);
        hash = 79 * hash + (this.running ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Event other = (Event) obj;
        if (this.version != other.version) {
            return false;
        }
        if (this.running != other.running) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.duration, other.duration)) {
            return false;
        }
        if (!Objects.equals(this.created.toInstant(), other.created.toInstant())) {
            return false;
        }
        if (!Objects.equals(this.createdBy, other.createdBy)) {
            return false;
        }
        if (!Objects.equals(this.added == null ? null : this.added.toInstant(), other.added == null ? null : other.added.toInstant())) {
            return false;
        }
        if (!Objects.equals(this.metadata, other.metadata)) {
            return false;
        }
        if (!Arrays.deepEquals(this.tags, other.tags)) {
            return false;
        }
        if (!Arrays.deepEquals(this.ids, other.ids)) {
            return false;
        }
        if (!Objects.equals(this.start, other.start)) {
            return false;
        }
        if (!Objects.equals(this.end, other.end)) {
            return false;
        }
        return true;
    }
    
    
}
