package edu.citysearch.edit;

import edu.citysearch.metrics.Measurements;

public interface EditDistance {
    int distance(String a, String b);
    Measurements measurements();
    String name();
}
