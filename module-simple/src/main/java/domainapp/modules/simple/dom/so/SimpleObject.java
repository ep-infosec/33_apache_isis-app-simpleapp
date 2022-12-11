package domainapp.modules.simple.dom.so;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.springframework.lang.Nullable;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.MemberSupport;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.PromptStyle;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.Publishing;
import org.apache.isis.applib.annotation.TableDecoration;
import org.apache.isis.applib.annotation.Title;
import org.apache.isis.applib.jaxb.PersistentEntityAdapter;
import org.apache.isis.applib.layout.LayoutConstants;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.iactnlayer.InteractionContext;
import org.apache.isis.applib.services.iactnlayer.InteractionService;
import org.apache.isis.applib.services.message.MessageService;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.applib.services.title.TitleService;
import org.apache.isis.applib.services.user.UserCurrentSessionTimeZoneHolder;
import org.apache.isis.applib.services.user.UserService;
import org.apache.isis.applib.value.Blob;
import org.apache.isis.extensions.fullcalendar.applib.CalendarEventable;
import org.apache.isis.extensions.fullcalendar.applib.value.CalendarEvent;
import org.apache.isis.extensions.pdfjs.applib.annotations.PdfJsViewer;
import org.apache.isis.persistence.jpa.applib.integration.IsisEntityListener;
import org.apache.isis.persistence.jpa.applib.types.BlobJpaEmbeddable;

import static org.apache.isis.applib.annotation.SemanticsOf.IDEMPOTENT;
import static org.apache.isis.applib.annotation.SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.val;

import domainapp.modules.simple.SimpleModule;
import domainapp.modules.simple.types.Name;
import domainapp.modules.simple.types.Notes;


@Entity
@Table(
    schema= SimpleModule.SCHEMA,
    uniqueConstraints = {
        @UniqueConstraint(name = "SimpleObject__name__UNQ", columnNames = {"name"})
    }
)
@NamedQueries({
        @NamedQuery(
                name = SimpleObject.NAMED_QUERY__FIND_BY_NAME_LIKE,
                query = "SELECT so " +
                        "FROM SimpleObject so " +
                        "WHERE so.name LIKE :name"
        )
})
@EntityListeners(IsisEntityListener.class)
@Named(SimpleModule.NAMESPACE + ".SimpleObject")
@DomainObject(entityChangePublishing = Publishing.ENABLED)
@DomainObjectLayout(tableDecoration = TableDecoration.DATATABLES_NET)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@XmlJavaTypeAdapter(PersistentEntityAdapter.class)
@ToString(onlyExplicitlyIncluded = true)
public class SimpleObject implements Comparable<SimpleObject>, CalendarEventable {

    static final String NAMED_QUERY__FIND_BY_NAME_LIKE = "SimpleObject.findByNameLike";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    @PropertyLayout(fieldSetId = "metadata", sequence = "999")
    @Getter @Setter
    private long version;

    public static SimpleObject withName(String name) {
        val simpleObject = new SimpleObject();
        simpleObject.setName(name);
        return simpleObject;
    }

    @Inject @Transient RepositoryService repositoryService;
    @Inject @Transient TitleService titleService;
    @Inject @Transient MessageService messageService;



    @Title
    @Name
    @Column(length = Name.MAX_LEN, nullable = false, name = "name")
    @Getter @Setter @ToString.Include
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.IDENTITY, sequence = "1")
    private String name;

    @Notes
    @Column(length = Notes.MAX_LEN, nullable = true)
    @Getter @Setter
    @Property(commandPublishing = Publishing.ENABLED, executionPublishing = Publishing.ENABLED)
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.DETAILS, sequence = "2")
    private String notes;

    @AttributeOverrides({
            @AttributeOverride(name="name",    column=@Column(name="attachment_name")),
            @AttributeOverride(name="mimeType",column=@Column(name="attachment_mimeType")),
            @AttributeOverride(name="bytes",   column=@Column(name="attachment_bytes"))
    })
    @Embedded
    private BlobJpaEmbeddable attachment;

    @PdfJsViewer
    @Property()
    @PropertyLayout(fieldSetId = "content", sequence = "1")
    public Blob getAttachment() {
        return BlobJpaEmbeddable.toBlob(attachment);
    }
    public void setAttachment(final Blob attachment) {
        this.attachment = BlobJpaEmbeddable.fromBlob(attachment);
    }



    @Property(optionality = Optionality.OPTIONAL, editing = Editing.ENABLED)
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.DETAILS, sequence = "3")
    @Column(nullable = true)
    @Getter @Setter
    private java.time.LocalDate lastCheckedIn;


    @Override
    public String getCalendarName() {
        return "Last checked-in";
    }

    @Override
    public CalendarEvent toCalendarEvent() {
        if (getLastCheckedIn() != null) {
            long epochMillis = getLastCheckedIn().toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.systemDefault().getRules().getOffset(getLastCheckedIn().atStartOfDay())) * 1000L;
            return new CalendarEvent(epochMillis, getCalendarName(), titleService.titleOf(this), getNotes());
        } else {
            return null;
        }
    }


    @Action(semantics = IDEMPOTENT, commandPublishing = Publishing.ENABLED, executionPublishing = Publishing.ENABLED)
    @ActionLayout(
            associateWith = "name", promptStyle = PromptStyle.INLINE,
            describedAs = "Updates the name of this object, certain characters (" + PROHIBITED_CHARACTERS + ") are not allowed.")
    public SimpleObject updateName(
            @Name final String name) {
        setName(name);
        return this;
    }
    @MemberSupport public String default0UpdateName() {
        return getName();
    }
    @MemberSupport public String validate0UpdateName(String newName) {
        for (char prohibitedCharacter : PROHIBITED_CHARACTERS.toCharArray()) {
            if( newName.contains(""+prohibitedCharacter)) {
                return "Character '" + prohibitedCharacter + "' is not allowed.";
            }
        }
        return null;
    }
    static final String PROHIBITED_CHARACTERS = "&%$!";



    @Action(semantics = IDEMPOTENT, commandPublishing = Publishing.ENABLED, executionPublishing = Publishing.ENABLED)
    @ActionLayout(associateWith = "attachment", position = ActionLayout.Position.PANEL)
    public SimpleObject updateAttachment(
            @Nullable final Blob attachment) {
        setAttachment(attachment);
        return this;
    }
    @MemberSupport public Blob default0UpdateAttachment() {
        return getAttachment();
    }



    @Action(semantics = NON_IDEMPOTENT_ARE_YOU_SURE)
    @ActionLayout(
            fieldSetId = LayoutConstants.FieldSetId.IDENTITY,
            position = ActionLayout.Position.PANEL,
            describedAs = "Deletes this object from the persistent datastore")
    public void delete() {
        final String title = titleService.titleOf(this);
        messageService.informUser(String.format("'%s' deleted", title));
        repositoryService.removeAndFlush(this);
    }



    private final static Comparator<SimpleObject> comparator =
            Comparator.comparing(SimpleObject::getName);

    @Override
    public int compareTo(final SimpleObject other) {
        return comparator.compare(this, other);
    }

}
