package org.pimonitor;

import java.util.*;

public class LQI {


    public String dates;

    static class Measurement {
        String measureStart;
        String component;
        int measurement;

        public Measurement(String measureStart, String component, int measurement) {
            this.component = component;
            this.measurement = measurement;
            this.measureStart = measureStart;
        }
    }

    Comparator<Measurement> customComparator = Comparator.comparing(m -> m.component);
    Set<Measurement> measurements = new TreeSet<>(customComparator);
}
