package web.lab;

import io.quarkiverse.statiq.runtime.StatiqPage;
import io.quarkiverse.statiq.runtime.StatiqPages;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Produces;

@Singleton
public class Statiq {

    @Produces
    @Singleton
    @Transactional
    StatiqPages produce() {
        return new StatiqPages(Entry.<Entry>listAll().stream().map(e -> new StatiqPage("/post/"+ e.slug + "/")).toList());
    }

}