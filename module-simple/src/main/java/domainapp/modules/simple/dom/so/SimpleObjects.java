package domainapp.modules.simple.dom.so;

import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TypedQuery;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.PriorityPrecedence;
import org.apache.isis.applib.annotation.PromptStyle;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.query.Query;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.persistence.jpa.applib.services.JpaSupportService;

import lombok.RequiredArgsConstructor;

import domainapp.modules.simple.SimpleModule;
import domainapp.modules.simple.types.Name;

@Named(SimpleModule.NAMESPACE + ".SimpleObjects")
@DomainService(nature = NatureOfService.VIEW)
@Priority(PriorityPrecedence.EARLY)
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
public class SimpleObjects {

    final RepositoryService repositoryService;
    final JpaSupportService jpaSupportService;
    final SimpleObjectRepository simpleObjectRepository;


    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_SIDEBAR)
    public SimpleObject create(
            @Name final String name) {
        return repositoryService.persist(SimpleObject.withName(name));
    }


    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_SIDEBAR)
    public List<SimpleObject> findByNameLike(
            @Name final String name) {
        return repositoryService.allMatches(
                Query.named(SimpleObject.class, SimpleObject.NAMED_QUERY__FIND_BY_NAME_LIKE)
                     .withParameter("name", "%" + name + "%"));
    }


    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(bookmarking = BookmarkPolicy.AS_ROOT, promptStyle = PromptStyle.DIALOG_SIDEBAR)
    public List<SimpleObject> findByName(
            @Name final String name
            ) {
        return simpleObjectRepository.findByNameContaining(name);
    }


    public SimpleObject findByNameExact(final String name) {
        return simpleObjectRepository.findByName(name);
    }



    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(bookmarking = BookmarkPolicy.AS_ROOT)
    public List<SimpleObject> listAll() {
        return simpleObjectRepository.findAll();
    }



    public void ping() {
        jpaSupportService.getEntityManager(SimpleObject.class)
            .mapSuccess(entityManager -> {
                final TypedQuery<SimpleObject> q = entityManager.createQuery(
                        "SELECT p FROM SimpleObject p ORDER BY p.name",
                        SimpleObject.class)
                    .setMaxResults(1);
                return q.getResultList();
            })
            .ifFailureFail();
    }

}
