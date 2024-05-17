package rest;

import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import model.Entry;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.Date;
import java.util.List;

@Blocking
@Path("/cms/blog")
public class Blog extends HxController {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance blog(List<Entry> entries, Long currentEntryId, Entry currentEntry, String search);
        public static native TemplateInstance blog$entries(List<Entry> entries, Long currentEntryId);
        public static native TemplateInstance blog$entryForm(Entry currentEntry);
    }

    @Path("")
    public TemplateInstance blog(@RestQuery("id") Long id, @RestQuery("search") String search) {
        final List<Entry> entries = Entry.search(search);
        if (isHxRequest()) {
            return Templates.blog$entries(entries, id);
        }
        Entry entry = id != null ? Entry.findById(id) : null;
        return Templates.blog(entries, id, entry, search);
    }

    public TemplateInstance newEntry() {
        // not really used
        Entry entry = new Entry();
        if (isHxRequest()) {
            this.hx(HxResponseHeader.TRIGGER, "refreshEntries");
            return  concatTemplates(Templates.blog$entries(Entry.listAllSortedByLastUpdated(), null),
                    Templates.blog$entryForm(entry)
            );
        }
        return Templates.blog(Entry.listAllSortedByLastUpdated(), null, entry, null);
    }

    public TemplateInstance editEntry(@RestPath("id") Long id) {
        final Entry entry = Entry.findById(id);
        if(entry == null) {
            blog(null, null);
            return null;
        }
        if (isHxRequest()) {
            this.hx(HxResponseHeader.TRIGGER, "refreshEntries");
            return  concatTemplates(Templates.blog$entries(Entry.listAllSortedByLastUpdated(), id),
                    Templates.blog$entryForm(entry)
                    );
        }
        return Templates.blog(Entry.listAllSortedByLastUpdated(), id, entry, null);
    }

    @DELETE
    public String deleteEntry(@RestPath("id") Long id) {
        onlyHxRequest();
        Entry entry = Entry.findById(id);
        notFoundIfNull(entry);
        entry.delete();
        // HTMX is not a fan of 204 No Content for swapping https://github.com/bigskysoftware/htmx/issues/1130
        return "";
    }


    @POST
    public void saveEntry(@RestPath("id") Long id, @RestForm @NotBlank String title, @RestForm String content) {
        if(validationFailed()) {
            editEntry(id);
            return;
        }
        Entry entry = Entry.findById(id);
        notFoundIfNull(entry);
        entry.title = title;
        entry.content = content;
        entry.updated = new Date();
        entry.persist();
        editEntry(id);
    }

    @POST
    public void saveNewEntry(@RestForm @NotBlank String title, @RestForm String content) {
        if(validationFailed()) {
            newEntry();
        }
        Entry entry = new Entry();
        entry.title = title;
        entry.content = content;
        entry.persist();
        editEntry(entry.id);
    }
}