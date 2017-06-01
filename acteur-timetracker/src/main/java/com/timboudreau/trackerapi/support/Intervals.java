package com.timboudreau.trackerapi.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastfrog.util.time.Interval;
import com.mastfrog.util.time.MutableInterval;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
public final class Intervals implements Iterable<Interval> {

    private List<MI> value = new ArrayList<>();

    private List<MI> winnow() {
        List<MI> result = new ArrayList<>();
        MI prev = null;
        for (MI i : value) {
            i = i.dup();
            if (prev != null) {
                if (!merge(prev, i.toInterval(), null)) {
                    result.add(i);
                } else {
                    prev.ids.addAll(i.ids);
                }
            } else {
                result.add(i);
            }
            prev = i;
        }
        return result;
    }

    public String toJSON(boolean ivals, boolean summary) throws IOException {
        List<Stub> stubs = new LinkedList<>();
        long duration = 0;
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        List<String> allIds = new ArrayList<>();
        for (MI i : winnow()) {
            duration += i.getEndMillis() - i.getStartMillis();
            start = Math.min(i.getStartMillis(), start);
            end = Math.max(end, i.getEndMillis());
            stubs.add(new Stub(i));
            allIds.addAll(i.ids);
        }
        ObjectMapper m = new ObjectMapper();
        Map<String, Object> map = new HashMap<>();
        if (start == Long.MAX_VALUE) {
            start = 0;
            end = 0;
        }
        if (summary) {
            Stub all = new Stub(start, end);
            all.ids.addAll(allIds);
            map.put("period", all);
        }
        map.put("total", duration);
        if (ivals) {
            map.put("intervals", stubs);
        }
        return m.writeValueAsString(map) + '\n';
    }
    
    private boolean merge(MI i, Interval ival, String id) {
        if (ival.overlaps(i)) {
            Instant sa = i.start();
            Instant ea = i.end();
            Instant se = ival.start();
            Instant ne = ival.end();
            Instant a = sa.isBefore(se) ? sa : se;
            Instant b = ea.isAfter(ne) ? ea : ne;
            i.setStart(a);
            i.setEnd(b);
            i.ids.add(id);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void add(Interval ival, String id) {
        for (MI i : value) {
            if (merge(i, ival, id)) {
                return;
            }
        }
        value.add(new MI(ival.toMutableInterval(), id));
        Collections.sort(value, (a, b) -> {
            return a.start().compareTo(b.start());
        });
    }

    public Duration total() {
        long result = 0;
        for (MI i : value) {
            result += i.toDurationMillis();
        }
        return Duration.ofMillis(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Interval> iterator() {
        return new WrapI(winnow().iterator());
    }

    static class MI extends MutableInterval {

        Set<String> ids = new HashSet<>();

        MI(MutableInterval m, String id) {
            super(m);
            if (id != null) {
                ids.add(id);
            }
        }

        MI dup() {
            MI nue = new MI(this, null);
            nue.ids.addAll(ids);
            return nue;
        }
    }

    static class WrapI<T extends Interval> implements Iterator<Interval> {

        private final Iterator<T> real;

        public WrapI(Iterator<T> real) {
            this.real = real;
        }

        @Override
        public boolean hasNext() {
            return real.hasNext();
        }

        @Override
        public Interval next() {
            return real.next().toInterval();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class Stub {

        public final long start;
        public final long end;
        public final long dur;
        public final List<String> ids = new ArrayList<>();

        public Stub(MI iv) {
            this(iv.getStartMillis(), iv.getEndMillis());
            ids.addAll(iv.ids);
            Collections.sort(ids);
        }

        public Stub(long start, long end) {
            this.start = start;
            this.end = end;
            this.dur = end - start;
        }
    }
}
