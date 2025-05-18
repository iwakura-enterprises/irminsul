package enterprises.iwakura.irminsul;

import lombok.Data;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Context for thread local within {@link enterprises.iwakura.irminsul.DatabaseService}
 */
@Data
public final class IrminsulContext {

    private static final ThreadLocal<IrminsulContext> THREAD_LOCAL = new ThreadLocal<>();

    private final Long ID = System.currentTimeMillis();
    private final List<Runnable> afterCommitActions = new ArrayList<>();
    private final List<Runnable> rollbackActions = new ArrayList<>();
    private Session session;
    private Transaction transaction;

    /**
     * Checks if the current thread has an IrminsulContext.
     *
     * @return true if the current thread has an IrminsulContext, false otherwise
     */
    public static boolean hasCurrent() {
        return THREAD_LOCAL.get() != null;
    }

    /**
     * Returns the current IrminsulContext for the current thread.
     *
     * @return the current IrminsulContext
     */
    public static IrminsulContext getCurrent() {
        return Optional.ofNullable(THREAD_LOCAL.get())
                       .orElseThrow(() -> new IllegalStateException("No IrminsulContext found for the current thread"));
    }

    /**
     * Initializes the current IrminsulContext for the current thread.
     *
     * @param session the Hibernate session to set
     *
     * @return the initialized IrminsulContext
     */
    public static IrminsulContext initializeCurrent(Session session) {
        IrminsulContext context = new IrminsulContext();
        context.setSession(session);
        THREAD_LOCAL.set(context);
        return context;
    }

    /**
     * Begins a transaction for the current IrminsulContext if none exists.
     */
    public void beginTransaction() {
        IrminsulContext context = getCurrent();
        if (context.transaction == null) {
            context.transaction = context.session.beginTransaction();
        }
    }

    /**
     * Commits the current transaction.
     *
     * @throws IllegalStateException if no transaction is found
     */
    public void commit() {
        IrminsulContext context = getCurrent();
        if (context.transaction == null) {
            throw new IllegalStateException("No transaction found to commit");
        }
        context.transaction.commit();
    }

    /**
     * Rollbacks the current transaction.
     */
    public void rollback() {
        IrminsulContext context = getCurrent();
        if (context.transaction == null) {
            throw new IllegalStateException("No transaction found to rollback");
        }
        context.transaction.rollback();
    }

    /**
     * Runs all after commit actions for the current IrminsulContext and clears the list.
     */
    public void runAfterCommitActions() {
        IrminsulContext context = getCurrent();
        for (Runnable action : context.afterCommitActions) {
            action.run();
        }
        context.afterCommitActions.clear();
    }

    /**
     * Runs all rollback actions for the current IrminsulContext and clears the list.
     */
    public void runAfterRollbackActions() {
        IrminsulContext context = getCurrent();
        for (Runnable action : context.rollbackActions) {
            action.run();
        }
        context.rollbackActions.clear();
    }

    /**
     * Clears the current IrminsulContext for the current thread.
     */
    public void clear() {
        THREAD_LOCAL.remove();
    }

    /**
     * Adds an action to be executed after the transaction is committed.
     *
     * @param action the action to be executed
     */
    public static void addAfterCommitAction(Runnable action) {
        getCurrent().afterCommitActions.add(action);
    }

    /**
     * Adds an action to be executed on transaction rollback.
     *
     * @param action the action to be executed
     */
    public static void addRollbackAction(Runnable action) {
        getCurrent().rollbackActions.add(action);
    }
}
