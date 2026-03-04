/*
 *
 *
 *
 *
 *
 * 由 Doug Lea 撰写，JCP JSR-166 专家组成员协助，
 * 并发布到公共领域，如 http://creativecommons.org/publicdomain/zero/1.0/ 所述
 */

package java.util.concurrent;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一个 {@link ExecutorService}，使用线程池中的一个线程来执行每个提交的任务，
 * 通常使用 {@link Executors} 的工厂方法进行配置。
 *
 * <p>线程池解决了两个不同的问题：在执行大量异步任务时，由于减少了每个任务的调用开销，
 * 它们通常能提供更好的性能；并且它们提供了一种方式来限制和管理执行一组任务时所消耗的资源（包括线程）。
 * 每个 {@code ThreadPoolExecutor} 还维护一些基本统计信息，例如已完成任务的数量。
 *
 * <p>为了在各种场景下都有用，此类提供了许多可调整的参数和扩展钩子。
 * 但是，建议程序员使用更方便的 {@link Executors} 工厂方法：
 * {@link Executors#newCachedThreadPool}（无界线程池，具有自动线程回收功能）、
 * {@link Executors#newFixedThreadPool}（固定大小的线程池）和
 * {@link Executors#newSingleThreadExecutor}（单个后台线程），
 * 这些方法为最常见的使用场景预设了配置。
 * 否则，在手动配置和调优此类时，请使用以下指南：
 *
 * <dl>
 *
 * <dt>核心线程数和最大线程数</dt>
 *
 * <dd>{@code ThreadPoolExecutor} 将根据
 * corePoolSize（参见 {@link #getCorePoolSize}）和
 * maximumPoolSize（参见 {@link #getMaximumPoolSize}）设置的界限自动调整线程池大小（参见 {@link #getPoolSize}）。
 *
 * 当在 {@link #execute(Runnable)} 方法中提交新任务时，
 * 如果正在运行的线程数少于 corePoolSize，则会创建一个新线程来处理该请求，即使其他工作线程是空闲的。
 * 否则，如果正在运行的线程数少于 maximumPoolSize，则只有当队列已满时才会创建新线程来处理该请求。
 * 通过将 corePoolSize 和 maximumPoolSize 设置为相同，可以创建一个固定大小的线程池。
 * 通过将 maximumPoolSize 设置为一个本质上无界的值，例如 {@code Integer.MAX_VALUE}，
 * 可以允许线程池容纳任意数量的并发任务。
 * 通常，核心线程数和最大线程数仅在构造时设置，但也可以使用
 * {@link #setCorePoolSize} 和 {@link #setMaximumPoolSize} 动态更改。</dd>
 *
 * <dt>按需构造</dt>
 *
 * <dd>默认情况下，即使是核心线程也只有在有新任务到达时才会初始创建和启动，
 * 但可以使用 {@link #prestartCoreThread} 或 {@link #prestartAllCoreThreads} 方法动态覆盖此行为。
 * 如果使用非空队列构造线程池，您可能希望预先启动线程。</dd>
 *
 * <dt>创建新线程</dt>
 *
 * <dd>新线程使用 {@link ThreadFactory} 创建。
 * 如果未另外指定，则使用 {@link Executors#defaultThreadFactory}，
 * 该工厂创建的线程都属于同一个 {@link ThreadGroup}，具有相同的 {@code NORM_PRIORITY} 优先级和非守护状态。
 * 通过提供不同的 ThreadFactory，您可以更改线程的名称、线程组、优先级、守护状态等。
 * 如果 {@code ThreadFactory} 在请求时通过从 {@code newThread} 返回 null 而未能创建线程，
 * 执行器将继续执行，但可能无法执行任何任务。
 * 线程应该具有 "modifyThread" {@code RuntimePermission}。
 * 如果工作线程或使用该池的其他线程不具有此权限，服务可能会降级：
 * 配置更改可能无法及时生效，并且已关闭的池可能仍处于可以终止但未完成终止的状态。</dd>
 *
 * <dt>保持活跃时间</dt>
 *
 * <dd>如果池当前有超过 corePoolSize 个线程，那么多余的线程如果空闲时间超过 keepAliveTime（参见 {@link #getKeepAliveTime(TimeUnit)}）将被终止。
 * 这提供了一种在池未被积极使用时减少资源消耗的方法。
 * 如果池稍后变得更活跃，将构造新线程。
 * 也可以使用 {@link #setKeepAliveTime(long, TimeUnit)} 方法动态更改此参数。
 * 使用 {@code Long.MAX_VALUE} {@link TimeUnit#NANOSECONDS} 的值可以有效地禁止空闲线程在关闭之前终止。
 * 默认情况下，保持活跃策略仅适用于超过 corePoolSize 个线程的情况，
 * 但可以使用 {@link #allowCoreThreadTimeOut(boolean)} 方法将此超时策略也应用于核心线程，
 * 只要 keepAliveTime 值不为零。</dd>
 *
 * <dt>队列</dt>
 *
 * <dd>可以使用任何 {@link BlockingQueue} 来传输和保存提交的任务。
 * 此队列的使用与线程池大小调整交互：
 *
 * <ul>
 *
 * <li>如果正在运行的线程数少于 corePoolSize，执行器总是更喜欢添加新线程而不是排队。
 *
 * <li>如果正在运行的线程数为 corePoolSize 或更多，执行器总是更喜欢将请求排队而不是添加新线程。
 *
 * <li>如果请求无法排队，则会创建一个新线程，除非这会超过 maximumPoolSize，在这种情况下，任务将被拒绝。
 *
 * </ul>
 *
 * 排队有三种通用策略：
 * <ol>
 *
 * <li><em>直接移交。</em> 
 * 工作队列的一个很好的默认选择是 {@link SynchronousQueue}，它将任务移交给线程而无需另行保存。
 * 在这里，如果没有线程立即可用来运行任务，则尝试将任务排队将失败，因此将构造一个新线程。
 * 此策略在处理可能具有内部依赖关系的请求集时避免锁定。
 * 直接移交通常需要无界的 maximumPoolSizes 以避免拒绝新提交的任务。
 * 这反过来允许在命令持续到达的平均速度快于处理速度时，线程无限增长的可能性。
 *
 * <li><em>无界队列。</em> 
 * 使用无界队列（例如，没有预定义容量的 {@link LinkedBlockingQueue}）将导致新任务在所有 corePoolSize 线程都忙碌时在队列中等待。
 * 因此，将只创建不超过 corePoolSize 个线程。（因此 maximumPoolSize 的值没有任何效果。）
 * 当每个任务完全独立于其他任务时，这可能比较合适，因此任务不会影响彼此的执行；例如，在网页服务器中。
 * 虽然这种排队方式在平滑瞬态请求突发时很有用，但它允许在命令持续到达的平均速度快于处理速度时，工作队列无限增长的可能性。
 *
 * <li><em>有界队列。</em> 
 * 有界队列（例如，{@link ArrayBlockingQueue}）在与有限的 maximumPoolSizes 一起使用时有助于防止资源耗尽，但可能更难调优和控制。
 * 队列大小和最大线程池大小可以相互权衡：
 * 使用大队列和小线程池可以最小化 CPU 使用率、操作系统资源和上下文切换开销，但可能导致人为的低吞吐量。
 * 如果任务经常阻塞（例如，如果它们是 I/O 绑定的），系统可能能够安排时间运行比您允许的更多的线程。
 * 使用小队列通常需要更大的线程池大小，这会使 CPU 更繁忙，但可能会遇到不可接受的调度开销，这也会降低吞吐量。
 *
 * </ol>
 *
 * </dd>
 *
 * <dt>拒绝的任务</dt>
 *
 * <dd>当执行器已关闭，以及当执行器对最大线程数和工作队列容量使用有限界限且已饱和时，
 * 在 {@link #execute(Runnable)} 方法中提交的新任务将被<em>拒绝</em>。
 * 无论哪种情况，{@code execute} 方法都会调用其 {@link RejectedExecutionHandler} 的
 * {@link RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)} 方法。
 * 提供了四种预定义的处理器策略：
 *
 * <ol>
 *
 * <li>在默认的 {@link ThreadPoolExecutor.AbortPolicy} 中，处理程序在拒绝时抛出运行时 {@link RejectedExecutionException}。
 *
 * <li>在 {@link ThreadPoolExecutor.CallerRunsPolicy} 中，调用 {@code execute} 的线程本身运行该任务。
 * 这提供了一个简单的反馈控制机制，可以减慢新任务提交的速度。
 *
 * <li>在 {@link ThreadPoolExecutor.DiscardPolicy} 中，无法执行的任务将被直接丢弃。
 * 此策略仅设计用于那些从不依赖任务完成的极少数情况。
 *
 * <li>在 {@link ThreadPoolExecutor.DiscardOldestPolicy} 中，如果执行器未关闭，
 * 将丢弃工作队列头部的任务，然后重试执行（这可能会再次失败，导致重复此过程）。
 * 此策略很少被接受。在几乎所有情况下，您还应该取消该任务以在等待其完成的任何组件中引起异常，
 * 和/或记录失败，如 {@link ThreadPoolExecutor.DiscardOldestPolicy} 文档中所述。
 *
 * </ol>
 *
 * 可以定义和使用其他类型的 {@link RejectedExecutionHandler} 类。
 * 这样做需要一些注意，特别是当策略设计为仅在特定容量或排队策略下工作时。</dd>
 *
 * <dt>钩子方法</dt>
 *
 * <dd>此类提供 {@code protected} 可重写的 {@link #beforeExecute(Thread, Runnable)} 和
 * {@link #afterExecute(Runnable, Throwable)} 方法，它们在执行每个任务之前和之后被调用。
 * 这些可以用来操作执行环境；例如，重新初始化 ThreadLocal、收集统计信息或添加日志条目。
 * 此外，可以重写 {@link #terminated} 方法以在执行器完全终止后执行任何需要特殊处理的操作。
 *
 * <p>如果钩子、回调或 BlockingQueue 方法抛出异常，内部工作线程可能反过来失败、突然终止，并可能被替换。</dd>
 *
 * <dt>队列维护</dt>
 *
 * <dd>{@link #getQueue()} 方法允许访问工作队列以用于监视和调试目的。
 * 强烈建议不要将此方法用于任何其他目的。
 * 提供了两个方法 {@link #remove(Runnable)} 和 {@link #purge}，
 * 可用于在大量队列任务被取消时协助存储回收。</dd>
 *
 * <dt>回收</dt>
 *
 * <dd>程序中不再被引用<em>并且</em>没有剩余线程的池可以被回收（垃圾回收）而无需显式关闭。
 * 您可以通过设置适当的保持活跃时间、使用零个核心线程的下限和/或设置 {@link #allowCoreThreadTimeOut(boolean)}，
 * 将池配置为允许所有未使用的线程最终死亡。</dd>
 *
 * </dl>
 *
 * <p><b>扩展示例。</b> 此类的大多数扩展都会重写一个或多个受保护的钩子方法。
 * 例如，这里是一个添加简单暂停/恢复功能的子类：
 *
 * <pre> {@code
 * class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *   private boolean isPaused;
 *   private ReentrantLock pauseLock = new ReentrantLock();
 *   private Condition unpaused = pauseLock.newCondition();
 *
 *   public PausableThreadPoolExecutor(...) { super(...); }
 *
 *   protected void beforeExecute(Thread t, Runnable r) {
 *     super.beforeExecute(t, r);
 *     pauseLock.lock();
 *     try {
 *       while (isPaused) unpaused.await();
 *     } catch (InterruptedException ie) {
 *       t.interrupt();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void pause() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = true;
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void resume() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = false;
 *       unpaused.signalAll();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ThreadPoolExecutor extends AbstractExecutorService {
    /**
     * 主线程池控制状态 ctl，是一个原子整数，打包了两个概念字段：
     *   workerCount，表示有效线程数
     *   runState，表示是否正在运行、正在关闭等
     *
     * 为了将它们打包到一个 int 中，我们将 workerCount 限制为 (2^29)-1（约 5 亿）个线程，
     * 而不是 (2^31)-1（20 亿）个，否则可以表示后者。
     * 如果将来这成为一个问题，可以将该变量更改为 AtomicLong，并调整下面的移位/掩码常量。
     * 但在需要出现之前，使用 int 的代码稍微更快且更简单。
     *
     * workerCount 是已被允许启动且未被允许停止的工作线程数。
     * 该值可能与实际活动线程数暂时不同，
     * 例如，当 ThreadFactory 在请求时未能创建线程，以及退出的线程在终止前仍在执行簿记时。
     * 用户可见的池大小报告为工作线程集的当前大小。
     *
     * runState 提供主要的生命周期控制，采用以下值：
     *
     *   RUNNING: 接受新任务并处理排队任务
     *   SHUTDOWN: 不接受新任务，但处理排队任务
     *   STOP:     不接受新任务，不处理排队任务，并中断正在进行的任务
     *   TIDYING:  所有任务都已终止，workerCount 为零，
     *             转换到 TIDYING 状态的线程将运行 terminated() 钩子方法
     *   TERMINATED: terminated() 已完成
     *
     * 这些值之间的数值顺序很重要，以允许有序比较。
     * runState 随时间单调递增，但不必命中每个状态。
     * 转换如下：
     *
     * RUNNING -> SHUTDOWN
     *    在调用 shutdown() 时
     * (RUNNING 或 SHUTDOWN) -> STOP
     *    在调用 shutdownNow() 时
     * SHUTDOWN -> TIDYING
     *    当队列和池都为空时
     * STOP -> TIDYING
     *    当池为空时
     * TIDYING -> TERMINATED
     *    当 terminated() 钩子方法完成时
     *
     * 在 awaitTermination() 中等待的线程将在状态达到 TERMINATED 时返回。
     *
     * 检测从 SHUTDOWN 到 TIDYING 的转换不如您希望的那样简单，
     * 因为在 SHUTDOWN 状态期间，队列可能在非空之后变为空，反之亦然，
     * 但我们只有在看到它为空之后，看到 workerCount 为 0 时才能终止（有时需要重新检查 —— 见下文）。
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    //COUNT_BITS 29
    // (1 << COUNT_BITS)  001 00000000000000000000000000000  // 只有第29位是1
    // (1<< 29) -1 表示 000 11111111111111111111111111111
    // 000 11111111111111111111111111111
    private static final int COUNT_MASK = (1 << COUNT_BITS) - 1;

    // runState 存储在高位
    private static final int RUNNING    = -1 << COUNT_BITS;  //111   ->111 00000000000000000000000000000
    private static final int SHUTDOWN   =  0 << COUNT_BITS;  //000   ->000 00000000000000000000000000000
    private static final int STOP       =  1 << COUNT_BITS;  //001 00000000000000000000000000000
    private static final int TIDYING    =  2 << COUNT_BITS;  //010 00000000000000000000000000000
    private static final int TERMINATED =  3 << COUNT_BITS;  //011 00000000000000000000000000000

    // 打包和解包 ctl
    private static int runStateOf(int c)     { return c & ~COUNT_MASK; }
    private static int workerCountOf(int c)  { return c & COUNT_MASK; }
    private static int ctlOf(int rs, int wc) { return rs | wc; }

    /*
     * 不需要解包 ctl 的位字段访问器。
     * 这些依赖于位布局以及 workerCount 从不为负。
     */

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * 尝试 CAS 递增 ctl 的 workerCount 字段。
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * 尝试 CAS 递减 ctl 的 workerCount 字段。
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * 递减 ctl 的 workerCount 字段。这仅在线程突然终止时调用（参见 processWorkerExit）。
     * 其他递减在 getTask 中执行。
     */
    private void decrementWorkerCount() {
        ctl.addAndGet(-1);
    }

    /**
     * 用于保存任务并移交给工作线程的队列。
     * 我们不要求 workQueue.poll() 返回 null 必然意味着 workQueue.isEmpty()，
     * 因此仅依赖 isEmpty 来查看队列是否为空（例如，在决定是否从 SHUTDOWN 转换到 TIDYING 时必须这样做）。
     * 这容纳了特殊用途的队列，例如 DelayQueue，对于后者，即使稍后延迟到期时可能返回非空，poll() 也允许返回 null。
     */
    private final BlockingQueue<Runnable> workQueue;

    /**
     * 访问工作线程集和相关簿记时持有的锁。
     * 虽然我们可以使用某种并发集合，但事实证明通常最好使用锁。
     * 原因之一是这会序列化 interruptIdleWorkers，避免不必要的中断风暴，特别是在关闭期间。
     * 否则，退出的线程将并发中断那些尚未中断的线程。
     * 它还简化了 largestPoolSize 等相关统计信息的簿记。
     * 我们还在 shutdown 和 shutdownNow 上持有 mainLock，以确保在单独检查中断权限和实际中断时工作线程集是稳定的。
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * 包含池中所有工作线程的集合。仅在持有 mainLock 时访问。
     */
    private final HashSet<Worker> workers = new HashSet<>();

    /**
     * 支持 awaitTermination 的等待条件。
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * 跟踪达到的最大池大小。仅在 mainLock 下访问。
     */
    private int largestPoolSize;

    /**
     * 已完成任务的计数器。仅在工作线程终止时更新。仅在 mainLock 下访问。
     */
    private long completedTaskCount;

    /*
     * 所有用户控制参数都声明为 volatile，以便正在进行的操作基于最新值，
     * 但不需要锁定，因为没有内部不变量依赖于它们与其他操作同步更改。
     */

    /**
     * 新线程的工厂。所有线程都使用此工厂创建（通过 addWorker 方法）。
     * 所有调用者都必须准备好 addWorker 失败，这可能反映系统或用户的策略限制线程数量。
     * 即使它不被视为错误，但未能创建线程可能导致新任务被拒绝或现有任务卡在队列中。
     *
     * 我们更进一步，即使面对诸如 OutOfMemoryError 之类的错误（可能在尝试创建线程时抛出），也保持池不变量。
     * 由于需要在 Thread.start 中分配本机堆栈，此类错误相当常见，用户希望执行清理池关闭以进行清理。
     * 可能有足够的内存可供清理代码完成，而不会遇到另一个 OutOfMemoryError。
     */
    private volatile ThreadFactory threadFactory;

    /**
     * 在 execute 中饱和或关闭时调用的处理程序。
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * 空闲线程等待工作的超时时间（纳秒）。
     * 当存在超过 corePoolSize 个线程或如果 allowCoreThreadTimeOut 时，线程使用此超时。
     * 否则它们永远等待新工作。
     */
    private volatile long keepAliveTime;

    /**
     * 如果为 false（默认），核心线程即使在空闲时也保持活动状态。
     * 如果为 true，核心线程使用 keepAliveTime 来超时等待工作。
     */
    private volatile boolean allowCoreThreadTimeOut;

    /**
     * 核心线程数是保持活动状态（不允许超时等）的最小工作线程数，
     * 除非设置了 allowCoreThreadTimeOut，在这种情况下最小值为零。
     *
     * 由于工作线程数实际上存储在 COUNT_BITS 位中，有效限制是 {@code corePoolSize & COUNT_MASK}。
     */
    private volatile int corePoolSize;

    /**
     * 最大线程池大小。
     *
     * 由于工作线程数实际上存储在 COUNT_BITS 位中，有效限制是 {@code maximumPoolSize & COUNT_MASK}。
     */
    private volatile int maximumPoolSize;

    /**
     * 默认的拒绝执行处理程序。
     */
    private static final RejectedExecutionHandler defaultHandler =
        new AbortPolicy();

    /**
     * shutdown 和 shutdownNow 调用者所需的权限。
     * 我们还要求（参见 checkShutdownAccess）调用者有权实际中断工作线程集中的线程
     * （由 Thread.interrupt 控制，它依赖于 ThreadGroup.checkAccess，而后者又依赖于 SecurityManager.checkAccess）。
     * 只有通过这些检查才会尝试关闭。
     *
     * 对 Thread.interrupt 的所有实际调用（参见 interruptIdleWorkers 和 interruptWorkers）都忽略 SecurityExceptions，
     * 这意味着尝试的中断静默失败。
     * 在 shutdown 的情况下，除非 SecurityManager 具有不一致的策略，有时允许访问线程而有时不允许，否则它们不应失败。
     * 在这种情况下，未能实际中断线程可能会禁用或延迟完全终止。
     * interruptIdleWorkers 的其他用途是建议性的，未能实际中断只会延迟对配置更改的响应，因此不会异常处理。
     */
    private static final RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread");

    /**
     * Worker 类主要维护运行任务的线程的中断控制状态，以及其他次要簿记。
     * 此类机会性地扩展 AbstractQueuedSynchronizer 以简化获取和释放围绕每个任务执行的锁。
     * 这可以防止旨在唤醒等待任务的工作线程的中断转而中断正在运行的任务。
     * 我们实现了一个简单的不可重入互斥锁，而不是使用 ReentrantLock，
     * 因为我们不希望工作线程在调用 setCorePoolSize 等池控制方法时能够重新获取锁。
     * 此外，为了在线程实际开始运行任务之前抑制中断，我们将锁状态初始化为负值，并在启动时清除它（在 runWorker 中）。
     */
    private final class Worker
        extends AbstractQueuedSynchronizer
        implements Runnable
    {
        /**
         * 此类永远不会被序列化，但我们提供一个 serialVersionUID 来抑制 javac 警告。
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /** 此工作线程正在运行的线程。如果工厂失败，则为 Null。 */
        @SuppressWarnings("serial") // 不太可能被序列化
        final Thread thread;
        /** 要运行的初始任务。可能为 null。 */
        @SuppressWarnings("serial") // 未静态类型化为 Serializable
        Runnable firstTask;
        /** 每个线程的任务计数器 */
        volatile long completedTasks;

        // TODO: 切换到 AbstractQueuedLongSynchronizer 并将 completedTasks 移到锁字中。

        /**
         * 使用给定的第一个任务和来自 ThreadFactory 的线程创建。
         * @param firstTask 第一个任务（如果没有则为 null）
         */
        Worker(Runnable firstTask) {
            setState(-1); // 在 runWorker 之前禁止中断
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        /** 将主运行循环委托给外部 runWorker。 */
        public void run() {
            runWorker(this);
        }

        // 锁方法
        //
        // 值 0 表示未锁定状态。
        // 值 1 表示锁定状态。

        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    /*
     * 设置控制状态的方法
     */

    /**
     * 将 runState 转换到给定目标，如果已经至少达到给定目标，则保持不变。
     *
     * @param targetState 期望的状态，SHUTDOWN 或 STOP（但不是 TIDYING 或 TERMINATED —— 为此使用 tryTerminate）
     */
    private void advanceRunState(int targetState) {
        // 断言 targetState == SHUTDOWN || targetState == STOP;
        for (;;) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * 如果满足（SHUTDOWN 且池和队列为空）或（STOP 且池为空），则转换到 TERMINATED 状态。
     * 如果否则有资格终止但 workerCount 非零，则中断一个空闲工作线程以确保关闭信号传播。
     * 必须在任何可能使终止成为可能的操作之后调用此方法 —— 减少工作线程数或在关闭期间从队列中删除任务。
     * 该方法是非私有的，以允许从 ScheduledThreadPoolExecutor 访问。
     */
    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            if (isRunning(c) ||
                runStateAtLeast(c, TIDYING) ||
                (runStateLessThan(c, STOP) && ! workQueue.isEmpty()))
                return;
            if (workerCountOf(c) != 0) { // 有资格终止
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // 否则在 CAS 失败时重试
        }
    }

    /*
     * 控制对工作线程中断的方法。
     */

    /**
     * 如果有安全管理器，确保调用者有权通常关闭线程（参见 shutdownPerm）。
     * 如果通过，另外确保允许调用者中断每个工作线程。
     * 即使第一次检查通过，如果 SecurityManager 对某些线程进行特殊处理，这也可能不成立。
     */
    private void checkShutdownAccess() {
        // 断言 mainLock.isHeldByCurrentThread();
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            for (Worker w : workers)
                security.checkAccess(w.thread);
        }
    }

    /**
     * 中断所有线程，即使是活动的。忽略 SecurityExceptions（在这种情况下，某些线程可能保持未中断）。
     */
    private void interruptWorkers() {
        // 断言 mainLock.isHeldByCurrentThread();
        for (Worker w : workers)
            w.interruptIfStarted();
    }

    /**
     * 中断可能正在等待任务的线程（由未锁定指示），以便它们可以检查终止或配置更改。
     * 忽略 SecurityExceptions（在这种情况下，某些线程可能保持未中断）。
     *
     * @param onlyOne 如果为 true，则最多中断一个工作线程。
     * 这仅在终止否则已启用但仍有其他工作线程时从 tryTerminate 调用。
     * 在这种情况下，最多中断一个等待的工作线程以传播关闭信号，以防所有线程当前都在等待。
     * 中断任意线程可确保自关闭开始以来新到达的工作线程最终也会退出。
     * 为了保证最终终止，始终只中断一个空闲工作线程就足够了，但 shutdown() 中断所有空闲工作线程，
     * 以便冗余工作线程及时退出，而不是等待一个落后任务完成。
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * interruptIdleWorkers 的通用形式，以避免必须记住布尔参数的含义。
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    /*
     * 其他工具，其中大部分也导出到 ScheduledThreadPoolExecutor
     */

    /**
     * 为给定命令调用拒绝执行处理程序。
     * 包保护，供 ScheduledThreadPoolExecutor 使用。
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * 在调用 shutdown 时执行运行状态转换后的任何进一步清理。
     * 这里是无操作，但由 ScheduledThreadPoolExecutor 用于取消延迟任务。
     */
    void onShutdown() {
    }

    /**
     * 将任务队列排空到新列表中，通常使用 drainTo。
     * 但如果队列是 DelayQueue 或任何其他类型的队列，对于后者 poll 或 drainTo 可能无法删除某些元素，则逐个删除它们。
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    /*
     * 创建、运行和清理工作线程的方法
     */

    /**
     * 检查是否可以根据当前池状态和给定界限（核心或最大）添加新工作线程。
     * 如果是，则相应调整工作线程数，并且如果可能，创建并启动新工作线程，将 firstTask 作为其第一个任务运行。
     * 如果池已停止或有资格关闭，此方法返回 false。
     * 如果线程工厂在请求时未能创建线程，它也会返回 false。
     * 如果线程创建失败，无论是由于线程工厂返回 null，还是由于异常（通常是 Thread.start() 中的 OutOfMemoryError），我们都会干净地回滚。
     *
     * @param firstTask 新线程应首先运行的任务（如果没有则为 null）。
     * 工作线程在 execute() 方法中创建，并带有初始第一个任务，以在运行线程数少于 corePoolSize 时绕过排队（在这种情况下我们总是启动一个），
     * 或在队列已满时（在这种情况下我们必须绕过队列）。
     * 初始空闲线程通常通过 prestartCoreThread 创建，或用于替换其他正在死亡的工作线程。
     *
     * @param core 如果为 true 则使用 corePoolSize 作为界限，否则使用 maximumPoolSize。
     * （这里使用布尔指示符而不是值，以确保在检查其他池状态后读取新值）。
     * @return 如果成功则为 true
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (int c = ctl.get();;) {
            // 仅在必要时检查队列是否为空。
            if (runStateAtLeast(c, SHUTDOWN)
                && (runStateAtLeast(c, STOP)
                    || firstTask != null
                    || workQueue.isEmpty()))
                return false;

            for (;;) {
                if (workerCountOf(c)
                    >= ((core ? corePoolSize : maximumPoolSize) & COUNT_MASK))
                    return false;
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                c = ctl.get();  // 重新读取 ctl
                if (runStateAtLeast(c, SHUTDOWN))
                    continue retry;
                // 否则 CAS 由于 workerCount 更改而失败；重试内层循环
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // 持有锁时重新检查。
                    // 在 ThreadFactory 失败或获取锁之前关闭时退出。
                    int c = ctl.get();

                    if (isRunning(c) ||
                        (runStateLessThan(c, STOP) && firstTask == null)) {
                        if (t.getState() != Thread.State.NEW)
                            throw new IllegalThreadStateException();
                        workers.add(w);
                        workerAdded = true;
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (! workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * 回滚工作线程创建。
     * - 从 workers 中删除工作线程（如果存在）
     * - 递减工作线程数
     * - 重新检查终止，以防此工作线程的存在阻止了终止
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 为即将死亡的工作线程执行清理和簿记。
     * 仅由工作线程调用。
     * 除非设置了 completedAbruptly，否则假定 workerCount 已调整为退出账户。
     * 此方法从工作线程集中删除线程，并可能终止池或替换工作线程，
     * 如果它因用户任务异常而退出，或如果运行的工作线程少于 corePoolSize 个且队列非空但没有工作线程。
     *
     * @param w 工作线程
     * @param completedAbruptly 如果工作线程因用户异常而死亡
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) // 如果是突然的，则 workerCount 未被调整
            decrementWorkerCount();

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        tryTerminate();

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // 不需要替换
            }
            addWorker(null, false);
        }
    }

    /**
     * 根据当前配置设置执行阻塞或定时等待任务，或在以下任何情况下返回 null，此工作线程必须退出：
     * 1. 有超过 maximumPoolSize 个工作线程（由于对 setMaximumPoolSize 的调用）。
     * 2. 池已停止。
     * 3. 池已关闭且队列为空。
     * 4. 此工作线程等待任务超时，且超时工作线程可能被终止（即，
     *    {@code allowCoreThreadTimeOut || workerCount > corePoolSize}）
     *    在定时等待之前和之后都是如此，如果队列非空，此工作线程不是池中的最后一个线程。
     *
     * @return 任务，如果工作线程必须退出则为 null，在这种情况下 workerCount 被递减
     */
    private Runnable getTask() {
        boolean timedOut = false; // 上次 poll() 是否超时？

        for (;;) {
            int c = ctl.get();

            // 仅在必要时检查队列是否为空。
            if (runStateAtLeast(c, SHUTDOWN)
                && (runStateAtLeast(c, STOP) || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            // 工作线程是否会被淘汰？
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            if ((wc > maximumPoolSize || (timed && timedOut))
                && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }

            try {
                Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();
                if (r != null)
                    return r;
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    /**
     * 主工作线程运行循环。重复从队列获取任务并执行它们，同时处理一些问题：
     *
     * 1. 我们可能以初始任务开始，在这种情况下我们不需要获取第一个。
     * 否则，只要池在运行，我们就从 getTask 获取任务。
     * 如果它返回 null，则工作线程由于池状态或配置参数更改而退出。
     * 其他退出是由于外部代码中的异常抛出，在这种情况下 completedAbruptly 为真，这通常导致 processWorkerExit 替换此线程。
     *
     * 2. 在运行任何任务之前，获取锁以防止在执行任务时其他池中断，然后我们确保除非池正在停止，否则此线程没有设置中断。
     *
     * 3. 每个任务运行之前调用 beforeExecute，它可能抛出异常，在这种情况下我们导致线程死亡（以 completedAbruptly 为真打破循环）而不处理任务。
     *
     * 4. 假设 beforeExecute 正常完成，我们运行任务，收集其抛出的任何异常以发送给 afterExecute。
     * 我们分别处理 RuntimeException、Error（规范保证我们捕获这两者）和任意 Throwables。
     * 由于我们无法在 Runnable.run 中重新抛出 Throwables，我们在退出时将它们包装在 Errors 中（到线程的 UncaughtExceptionHandler）。
     * 任何抛出的异常也会保守地导致线程死亡。
     *
     * 5. 在 task.run 完成后，我们调用 afterExecute，它也可能抛出异常，这也会导致线程死亡。
     * 根据 JLS 第 14.20 节，即使 task.run 抛出，此异常也将生效。
     *
     * 异常机制的最终效果是 afterExecute 和线程的 UncaughtExceptionHandler 拥有关于用户代码遇到的任何问题的尽可能准确的信息。
     *
     * @param w 工作线程
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // 允许中断
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // 如果池正在停止，确保线程被中断；
                // 如果不是，确保线程未被中断。
                // 这需要在第二种情况下重新检查以处理在清除中断时 shutdownNow 竞争
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    try {
                        task.run();
                        afterExecute(task, null);
                    } catch (Throwable ex) {
                        afterExecute(task, ex);
                        throw ex;
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }

    // 公共构造函数和方法

    /**
     * 使用给定的初始参数、
     * {@linkplain Executors#defaultThreadFactory 默认线程工厂}
     * 和 {@linkplain ThreadPoolExecutor.AbortPolicy 默认拒绝执行处理程序}
     * 创建一个新的 {@code ThreadPoolExecutor}。
     *
     * <p>使用 {@link Executors} 工厂方法之一可能比使用此通用构造函数更方便。
     *
     * @param corePoolSize 保持在池中的线程数，即使它们是空闲的，除非设置了 {@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 允许在池中的最大线程数
     * @param keepAliveTime 当线程数大于核心时，这是多余空闲线程在终止前等待新任务的最长时间。
     * @param unit {@code keepAliveTime} 参数的时间单位
     * @param workQueue 用于在执行前保存任务的队列。此队列将仅保存由 {@code execute} 方法提交的 {@code Runnable} 任务。
     * @throws IllegalArgumentException 如果以下之一成立：<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果 {@code workQueue} 为 null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * 使用给定的初始参数和 {@linkplain ThreadPoolExecutor.AbortPolicy 默认拒绝执行处理程序}
     * 创建一个新的 {@code ThreadPoolExecutor}。
     *
     * @param corePoolSize 保持在池中的线程数，即使它们是空闲的，除非设置了 {@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 允许在池中的最大线程数
     * @param keepAliveTime 当线程数大于核心时，这是多余空闲线程在终止前等待新任务的最长时间。
     * @param unit {@code keepAliveTime} 参数的时间单位
     * @param workQueue 用于在执行前保存任务的队列。此队列将仅保存由 {@code execute} 方法提交的 {@code Runnable} 任务。
     * @param threadFactory 执行器创建新线程时使用的工厂
     * @throws IllegalArgumentException 如果以下之一成立：<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果 {@code workQueue}
     *         或 {@code threadFactory} 为 null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }

    /**
     * 使用给定的初始参数和
     * {@linkplain Executors#defaultThreadFactory 默认线程工厂}
     * 创建一个新的 {@code ThreadPoolExecutor}。
     *
     * @param corePoolSize 保持在池中的线程数，即使它们是空闲的，除非设置了 {@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 允许在池中的最大线程数
     * @param keepAliveTime 当线程数大于核心时，这是多余空闲线程在终止前等待新任务的最长时间。
     * @param unit {@code keepAliveTime} 参数的时间单位
     * @param workQueue 用于在执行前保存任务的队列。此队列将仅保存由 {@code execute} 方法提交的 {@code Runnable} 任务。
     * @param handler 由于达到线程界限和队列容量而阻塞执行时使用的处理程序
     * @throws IllegalArgumentException 如果以下之一成立：<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果 {@code workQueue}
     *         或 {@code handler} 为 null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    /**
     * 使用给定的初始参数创建一个新的 {@code ThreadPoolExecutor}。
     *
     * @param corePoolSize 保持在池中的线程数，即使它们是空闲的，除非设置了 {@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 允许在池中的最大线程数
     * @param keepAliveTime 当线程数大于核心时，这是多余空闲线程在终止前等待新任务的最长时间。
     * @param unit {@code keepAliveTime} 参数的时间单位
     * @param workQueue 用于在执行前保存任务的队列。此队列将仅保存由 {@code execute} 方法提交的 {@code Runnable} 任务。
     * @param threadFactory 执行器创建新线程时使用的工厂
     * @param handler 由于达到线程界限和队列容量而阻塞执行时使用的处理程序
     * @throws IllegalArgumentException 如果以下之一成立：<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果 {@code workQueue}
     *         或 {@code threadFactory} 或 {@code handler} 为 null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * 在将来的某个时间执行给定任务。该任务可能在新线程或现有池线程中执行。
     *
     * 如果由于此执行器已关闭或已达到其容量而无法提交任务执行，则该任务由当前的 {@link RejectedExecutionHandler} 处理。
     *
     * @param command 要执行的任务
     * @throws RejectedExecutionException 由 {@code RejectedExecutionHandler} 决定，如果任务不能被接受执行
     * @throws NullPointerException 如果 {@code command} 为 null
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * 分 3 步进行：
         *
         * 1. 如果正在运行的线程数少于 corePoolSize，尝试使用给定命令作为其第一个任务启动一个新线程。
         * 对 addWorker 的调用原子地检查 runState 和 workerCount，因此通过返回 false 防止在不应该添加线程时添加线程的错误警报。
         *
         * 2. 如果任务可以成功排队，那么我们仍然需要仔细检查我们是否应该添加一个线程
         * （因为自上次检查以来已有的线程已死亡）或自进入此方法以来池已关闭。
         * 因此我们重新检查状态，如果必要，如果已停止则回滚入队，如果没有线程则启动一个新线程。
         *
         * 3. 如果我们无法将任务排队，那么我们尝试添加一个新线程。
         * 如果失败，我们知道我们已关闭或已饱和，因此拒绝该任务。
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (! isRunning(recheck) && remove(command))
                reject(command);
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        else if (!addWorker(command, false))
            reject(command);
    }

    /**
     * 启动有序关闭，其中先前提交的任务将被执行，但不接受新任务。
     * 如果已关闭，则调用没有其他效果。
     *
     * <p>此方法不等待先前提交的任务完成执行。使用 {@link #awaitTermination awaitTermination} 来执行此操作。
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown(); // 供 ScheduledThreadPoolExecutor 使用的钩子
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    /**
     * 尝试停止所有正在执行的任务，停止处理等待的任务，并返回等待执行的任务列表。
     * 这些任务在此方法返回时从任务队列中排出（移除）。
     *
     * <p>此方法不等待正在执行的任务终止。使用 {@link #awaitTermination awaitTermination} 来执行此操作。
     *
     * <p>除了尽最大努力尝试停止处理正在执行的任务外，没有其他保证。
     * 此实现通过 {@link Thread#interrupt} 中断任务；任何未能响应中断的任务可能永远不会终止。
     *
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return runStateAtLeast(ctl.get(), SHUTDOWN);
    }

    /** 由 ScheduledThreadPoolExecutor 使用。 */
    boolean isStopped() {
        return runStateAtLeast(ctl.get(), STOP);
    }

    /**
     * 如果此执行器在 {@link #shutdown} 或 {@link #shutdownNow} 之后正在终止过程中但尚未完全终止，则返回 true。
     * 此方法可能可用于调试。在关闭后足够长的时间返回 {@code true} 可能表示提交的任务已忽略或抑制中断，导致此执行器无法正确终止。
     *
     * @return 如果正在终止但尚未终止则为 {@code true}
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return runStateAtLeast(c, SHUTDOWN) && runStateLessThan(c, TERMINATED);
    }

    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            while (runStateLessThan(ctl.get(), TERMINATED)) {
                if (nanos <= 0L)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
            return true;
        } finally {
            mainLock.unlock();
        }
    }

    // 覆盖而不带 "throws Throwable" 以与调用 super.finalize() 的子类兼容（如建议的那样）。
    // 在 JDK 11 之前，finalize() 有一个非空方法体。

    /**
     * @implNote 此类的先前版本有一个 finalize 方法来关闭此执行器，但在此版本中，finalize 不执行任何操作。
     */
    @Deprecated(since="9")
    protected void finalize() {}

    /**
     * 设置用于创建新线程的线程工厂。
     *
     * @param threadFactory 新线程工厂
     * @throws NullPointerException 如果 threadFactory 为 null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * 返回用于创建新线程的线程工厂。
     *
     * @return 当前线程工厂
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * 设置无法执行的任务的新处理程序。
     *
     * @param handler 新处理程序
     * @throws NullPointerException 如果 handler 为 null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * 返回当前无法执行的任务的处理程序。
     *
     * @return 当前处理程序
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * 设置核心线程数。这将覆盖构造函数中设置的任何值。
     * 如果新值小于当前值，则多余的工作线程将在下次空闲时终止。
     * 如果更大，如果需要，将启动新线程来执行任何排队的任务。
     *
     * @param corePoolSize 新核心大小
     * @throws IllegalArgumentException 如果 {@code corePoolSize < 0}
     *         或 {@code corePoolSize} 大于 {@linkplain
     *         #getMaximumPoolSize() 最大线程池大小}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        else if (delta > 0) {
            // 我们真的不知道有多少新线程是"需要的"。
            // 作为启发式方法，预先启动足够多的新工作线程（最多到新的核心大小）来处理队列中的当前任务数，
            // 但如果这样做时队列变为空则停止。
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    /**
     * 返回核心线程数。
     *
     * @return 核心线程数
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * 启动一个核心线程，使其空闲等待工作。
     * 这覆盖了仅在执行新任务时启动核心线程的默认策略。
     * 如果所有核心线程都已启动，此方法将返回 {@code false}。
     *
     * @return 如果启动了线程则为 {@code true}
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
            addWorker(null, true);
    }

    /**
     * 与 prestartCoreThread 相同，但确保至少启动一个线程，即使 corePoolSize 为 0。
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * 启动所有核心线程，使其空闲等待工作。
     * 这覆盖了仅在执行新任务时启动核心线程的默认策略。
     *
     * @return 启动的线程数
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * 如果此池允许核心线程在 keepAlive 时间内没有任务到达时超时并终止，并在新任务到达时根据需要替换，则返回 true。
     * 为 true 时，适用于非核心线程的相同保持活跃策略也适用于核心线程。
     * 为 false（默认）时，核心线程由于缺少传入任务而永远不会终止。
     *
     * @return 如果允许核心线程超时则为 {@code true}，否则为 {@code false}
     *
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * 设置控制核心线程是否可以在 keep-alive 时间内没有任务到达时超时并终止的策略，并在新任务到达时根据需要替换。
     * 为 false 时，核心线程由于缺少传入任务而永远不会终止。
     * 为 true 时，适用于非核心线程的相同保持活跃策略也适用于核心线程。
     * 为避免持续线程替换，在设置为 {@code true} 时保持活跃时间必须大于零。
     * 通常应在池被积极使用之前调用此方法。
     *
     * @param value 如果应该超时则为 {@code true}，否则为 {@code false}
     * @throws IllegalArgumentException 如果 value 为 {@code true}
     *         且当前保持活跃时间不大于零
     *
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    /**
     * 设置允许的最大线程数。这将覆盖构造函数中设置的任何值。
     * 如果新值小于当前值，则多余的工作线程将在下次空闲时终止。
     *
     * @param maximumPoolSize 新最大值
     * @throws IllegalArgumentException 如果新最大值
     *         小于或等于零，或
     *         小于 {@linkplain #getCorePoolSize 核心线程池大小}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    /**
     * 返回允许的最大线程数。
     *
     * @return 允许的最大线程数
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * 设置线程保持活跃时间，这是线程在被终止前可以保持空闲的时间量。
     * 如果等待此时间量而没有处理任务，则当池中的线程数超过核心线程数时，或如果此池
     * {@linkplain #allowsCoreThreadTimeOut() 允许核心线程超时}，线程将被终止。
     * 这将覆盖构造函数中设置的任何值。
     *
     * @param time 等待的时间。时间值为零将导致多余线程在执行任务后立即终止。
     * @param unit {@code time} 参数的时间单位
     * @throws IllegalArgumentException 如果 {@code time} 小于零或
     *         如果 {@code time} 为零且 {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)
            interruptIdleWorkers();
    }

    /**
     * 返回线程保持活跃时间，这是线程在被终止前可以保持空闲的时间量。
     * 如果等待此时间量而没有处理任务，则当池中的线程数超过核心线程数时，或如果此池
     * {@linkplain #allowsCoreThreadTimeOut() 允许核心线程超时}，线程将被终止。
     *
     * @param unit 结果的期望时间单位
     * @return 时间限制
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* 用户级队列工具 */

    /**
     * 返回此执行器使用的任务队列。访问任务队列主要用于调试和监视。
     * 此队列可能正在积极使用中。检索任务队列不会阻止队列任务执行。
     *
     * @return 任务队列
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * 如果此任务存在于执行器的内部队列中，则将其移除，从而如果它尚未开始，则不会运行它。
     *
     * <p>此方法可用作取消方案的一部分。它可能无法移除在放置到内部队列之前已转换为其他形式的任务。
     * 例如，使用 {@code submit} 输入的任务可能被转换为维护 {@code Future} 状态的形式。
     * 但是，在这种情况下，可以使用 {@link #purge} 方法移除那些已被取消的 Future。
     *
     * @param task 要移除的任务
     * @return 如果任务被移除则为 {@code true}
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // 以防 SHUTDOWN 且现在为空
        return removed;
    }

    /**
     * 尝试从工作队列中移除所有已取消的 {@link Future} 任务。
     * 此方法可用作存储回收操作，对功能没有其他影响。
     * 被取消的任务永远不会执行，但可能在工作线程能够主动移除它们之前积累在工作队列中。
     * 调用此方法会立即尝试移除它们。
     * 但是，如果存在其他线程的干扰，此方法可能无法移除任务。
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // 如果在遍历期间遇到干扰，则采用慢速路径。
            // 复制以进行遍历并对已取消的条目调用 remove。
            // 慢速路径更可能是 O(N*N)。
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        tryTerminate(); // 以防 SHUTDOWN 且现在为空
    }

    /* 统计 */

    /**
     * 返回池中的当前线程数。
     *
     * @return 线程数
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 消除 isTerminated() && getPoolSize() > 0 的罕见和令人惊讶的可能性
            return runStateAtLeast(ctl.get(), TIDYING) ? 0
                : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回正在积极执行任务的线程的大致数量。
     *
     * @return 线程数
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回池中曾经同时存在的最大线程数。
     *
     * @return 线程数
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回曾经计划执行的任务的近似总数。
     * 由于任务和线程的状态可能在计算过程中动态变化，返回的值只是一个近似值。
     *
     * @return 任务数
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回已完成执行的任务的近似总数。
     * 由于任务和线程的状态可能在计算过程中动态变化，返回的值只是一个近似值，
     * 但在连续调用中不会减少。
     *
     * @return 任务数
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回标识此池及其状态的字符串，包括运行状态的指示和估计的工作线程和任务计数。
     *
     * @return 标识此池及其状态的字符串
     */
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String runState =
            isRunning(c) ? "Running" :
            runStateAtLeast(c, TERMINATED) ? "Terminated" :
            "Shutting down";
        return super.toString() +
            "[" + runState +
            ", pool size = " + nworkers +
            ", active threads = " + nactive +
            ", queued tasks = " + workQueue.size() +
            ", completed tasks = " + ncompleted +
            "]";
    }

    /* 扩展钩子 */

    /**
     * 在给定线程中执行给定 Runnable 之前调用的方法。
     * 此方法由将执行任务 {@code r} 的线程 {@code t} 调用，可用于重新初始化 ThreadLocals 或执行日志记录。
     *
     * <p>此实现不执行任何操作，但可以在子类中自定义。
     * 注意：为了正确嵌套多个重写，子类通常应在此方法的末尾调用 {@code super.beforeExecute}。
     *
     * @param t 将运行任务 {@code r} 的线程
     * @param r 将执行的任务
     */
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * 在执行给定 Runnable 完成时调用的方法。
     * 此方法由执行任务的线程调用。
     * 如果非 null，则 Throwable 是导致执行突然终止的未捕获的 {@code RuntimeException} 或 {@code Error}。
     *
     * <p>此实现不执行任何操作，但可以在子类中自定义。
     * 注意：为了正确嵌套多个重写，子类通常应在此方法的开头调用 {@code super.afterExecute}。
     *
     * <p><b>注意：</b>当操作被封装在任务中（例如 {@link FutureTask}）时，无论是显式的还是通过 {@code submit} 等方法，
     * 这些任务对象捕获并维护计算异常，因此它们不会导致突然终止，并且内部异常<em>不会</em>传递给此方法。
     * 如果您想在此方法中捕获这两种故障，您可以进一步探测此类情况，如这个示例子类所示，它打印直接原因或基础异常（如果任务已中止）：
     *
     * <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null
     *         && r instanceof Future<?>
     *         && ((Future<?>)r).isDone()) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *         t = ce;
     *       } catch (ExecutionException ee) {
     *         t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *         // 忽略/重置
     *         Thread.currentThread().interrupt();
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r 已完成的 runnable
     * @param t 导致终止的异常，如果执行正常完成则为 null
     */
    protected void afterExecute(Runnable r, Throwable t) { }

    /**
     * 当 Executor 已终止时调用的方法。
     * 默认实现不执行任何操作。
     * 注意：为了正确嵌套多个重写，子类通常应在此方法中调用 {@code super.terminated}。
     */
    protected void terminated() { }

    /* 预定义的 RejectedExecutionHandlers */

    /**
     * 拒绝任务的处理程序，直接在 {@code execute} 方法的调用线程中运行被拒绝的任务，除非执行器已关闭，在这种情况下任务将被丢弃。
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * 创建一个 {@code CallerRunsPolicy}。
         */
        public CallerRunsPolicy() { }

        /**
         * 在调用者的线程中执行任务 r，除非执行器已关闭，在这种情况下任务将被丢弃。
         *
         * @param r 请求执行的 runnable 任务
         * @param e 尝试执行此任务的执行器
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * 拒绝任务的处理程序，抛出 {@link RejectedExecutionException}。
     *
     * 这是 {@link ThreadPoolExecutor} 和 {@link ScheduledThreadPoolExecutor} 的默认处理程序。
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * 创建一个 {@code AbortPolicy}。
         */
        public AbortPolicy() { }

        /**
         * 始终抛出 RejectedExecutionException。
         *
         * @param r 请求执行的 runnable 任务
         * @param e 尝试执行此任务的执行器
         * @throws RejectedExecutionException 始终
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                                                 " rejected from " +
                                                 e.toString());
        }
    }

    /**
     * 拒绝任务的处理程序，静默丢弃被拒绝的任务。
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * 创建一个 {@code DiscardPolicy}。
         */
        public DiscardPolicy() { }

        /**
         * 不执行任何操作，这具有丢弃任务 r 的效果。
         *
         * @param r 请求执行的 runnable 任务
         * @param e 尝试执行此任务的执行器
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    /**
     * 拒绝任务的处理程序，丢弃最旧的未处理请求，然后重试 {@code execute}，除非执行器已关闭，在这种情况下任务将被丢弃。
     * 此策略在可能有其他线程等待任务终止或必须记录故障的情况下很少有用。
     * 相反，请考虑使用以下形式的处理程序：
     * <pre> {@code
     * new RejectedExecutionHandler() {
     *   public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
     *     Runnable dropped = e.getQueue().poll();
     *     if (dropped instanceof Future<?>) {
     *       ((Future<?>)dropped).cancel(false);
     *       // 还考虑记录失败
     *     }
     *     e.execute(r);  // 重试
     * }}}</pre>
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * 为给定执行器创建一个 {@code DiscardOldestPolicy}。
         */
        public DiscardOldestPolicy() { }

        /**
         * 获取并忽略执行器将执行的下一个任务（如果立即可用），然后重试任务 r 的执行，除非执行器已关闭，在这种情况下任务 r 被丢弃。
         *
         * @param r 请求执行的 runnable 任务
         * @param e 尝试执行此任务的执行器
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
