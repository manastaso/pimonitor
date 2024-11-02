package org.pimonitor;

public class Pollen {
    class Forecast {
        String tomorrow;
        String dayafter_to;
        String today;

        @Override
        public String toString() {
            return "tomorrow='" + tomorrow + '\'' +
                    ", dayafter_to='" + dayafter_to + '\'' +
                    ", today='" + today + '\'';
        }
    }

    class Erle extends Forecast { };
    class Graeser extends Forecast { };

    class Roggen extends Forecast { };

    class Beifuss extends Forecast { };

    class Esche extends Forecast { };

    class Birke extends Forecast { };

    class Ambrosia extends Forecast { };

    class Hasel extends Forecast { };

    Erle Erle;
    Graeser Graeser;
    Roggen Roggen;
    Beifuss Beifuss;
    Esche Esche;
    Birke Birke;
    Ambrosia Ambrosia;
    Hasel Hasel;

    String last_update;

    @Override
    public String toString() {
        return "Erle: " + Erle + System.lineSeparator() +
                "Graeser: " + Graeser +  System.lineSeparator() +
                "Roggen: " + Roggen +  System.lineSeparator() +
                "Beifuss: " + Beifuss +  System.lineSeparator() +
                "Esche: " + Esche +  System.lineSeparator() +
                "Birke: " + Birke +  System.lineSeparator() +
                "Ambrosia: " + Ambrosia +  System.lineSeparator() +
                "Hasel: " + Hasel +
                "last_update:" + last_update;
    }
}
