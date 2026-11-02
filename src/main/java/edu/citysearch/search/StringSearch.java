package edu.citysearch.search;

import edu.citysearch.metrics.Measurements;
import java.util.List;

public interface StringSearch {
    List<Integer> search(String text, String pattern);
    Measurements measurements();
    String name();
}
