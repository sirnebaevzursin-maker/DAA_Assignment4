package edu.citysearch;

import edu.citysearch.edit.*;
import edu.citysearch.search.*;
import edu.citysearch.experiment.ExperimentRunner;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    private Main() {}
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("experiment")) {
            if (args.length > 2) throw new IllegalArgumentException("Usage: experiment [results-directory]");
            ExperimentRunner.run(Path.of(args.length == 2 ? args[1] : "results"));
        } else if (args.length == 0 || (args.length == 1 && args[0].equals("demo"))) {
            System.out.println("City Search & Spell-Check | Zhursin Sirnebayev | SE-2509");
            for (EditDistance algorithm : List.of(new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance()))
                System.out.println(algorithm.name() + ": Abylai Han -> Abylai Khan = "
                        + algorithm.distance("Abylai Han", "Abylai Khan"));
            for (StringSearch algorithm : List.of(new NaiveSearch(), new KmpSearch()))
                System.out.println(algorithm.name() + ": search(ABABABA, ABA) = "
                        + algorithm.search("ABABABA", "ABA"));
            System.out.println("Run with argument 'experiment' to write the complete measurements.");
        } else throw new IllegalArgumentException("Usage: [demo | experiment [results-directory]]");
    }
}
