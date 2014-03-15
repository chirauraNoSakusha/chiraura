package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import nippon.kawauso.chiraura.lib.process.Reporter;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class OperationAggregatorTest {

    private final Random random;
    private final OperationAggregator<OperationAggregator.TestOperation, OperationAggregator.TestResult> instance;

    /**
     * 初期化。
     */
    public OperationAggregatorTest() {
        this.random = new Random();
        this.instance = new OperationAggregator<>();
    }

    private void testTimeout(final long timeout) throws InterruptedException {
        final OperationAggregator.TestOperation operation = new OperationAggregator.TestOperation(this.random.nextInt());
        Assert.assertNull(this.instance.register(operation));
        final CheckingStation.Instrument<OperationAggregator.TestResult> instrument = this.instance.register(operation);
        Assert.assertNotNull(instrument);
        final long start = System.nanoTime();
        final OperationAggregator.TestResult result = instrument.get(timeout);
        final long end = System.nanoTime();
        Assert.assertNull(result);
        Assert.assertTrue(timeout <= (end - start) / 1_000_000);
    }

    /**
     * 時間切れ動作の検査。
     * @throws Exception 異常
     */
    @Test
    public void testTimeout() throws Exception {
        testTimeout(100);
        testTimeout(1);
        testTimeout(0);
        testTimeout(-1);
        testTimeout(-100);
    }

    /**
     * 自作自演の正常系。
     * @throws Exception 異常
     */
    @Test
    public void testAlone() throws Exception {
        final OperationAggregator.TestOperation operation = new OperationAggregator.TestOperation(this.random.nextInt());
        Assert.assertNull(this.instance.register(operation));
        final CheckingStation.Instrument<OperationAggregator.TestResult> instrument = this.instance.register(operation);
        Assert.assertNotNull(instrument);

        final OperationAggregator.TestResult result = new OperationAggregator.TestResult(this.random.nextInt());
        this.instance.free(operation, result);

        final OperationAggregator.TestResult result2 = instrument.get(100);

        Assert.assertEquals(result, result2);
    }

    /**
     * 一対一正常系。
     * @throws Exception 異常
     */
    @Test
    public void testOneToOne() throws Exception {
        final long timeout = 100;
        final OperationAggregator.TestOperation operation = new OperationAggregator.TestOperation(this.random.nextInt());
        final OperationAggregator.TestResult result = new OperationAggregator.TestResult(this.random.nextInt());

        final List<Callable<Void>> workers = new ArrayList<>();
        workers.add(new Reporter<Void>(Level.SEVERE) {
            @Override
            public Void subCall() throws Exception {
                Assert.assertNull(OperationAggregatorTest.this.instance.register(operation));
                Thread.sleep(timeout / 2);
                OperationAggregatorTest.this.instance.free(operation, result);
                return null;
            }
        });
        workers.add(new Reporter<Void>(Level.SEVERE) {
            @Override
            public Void subCall() throws Exception {
                Thread.sleep(timeout / 4);
                final CheckingStation.Instrument<OperationAggregator.TestResult> instrument = OperationAggregatorTest.this.instance.register(operation);
                Assert.assertNotNull(instrument);
                final OperationAggregator.TestResult result2 = instrument.get(timeout);
                Assert.assertEquals(result, result2);
                return null;
            }
        });

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final long start = System.nanoTime();
        final List<Future<Void>> futures = executor.invokeAll(workers);
        final long end = System.nanoTime();

        for (final Future<Void> future : futures) {
            future.get();
        }

        Assert.assertTrue(timeout / 2 <= (end - start) / 1_000_000);
        Assert.assertTrue((end - start) / 1_000_000 <= timeout);
    }

    /**
     * 正常系。
     * @throws Exception 異常
     */
    @Test
    public void testConcurrency() throws Exception {
        final long timeout = 100;
        final int numOfProcesses = 100;
        final OperationAggregator.TestOperation operation = new OperationAggregator.TestOperation(this.random.nextInt());
        final OperationAggregator.TestResult result = new OperationAggregator.TestResult(this.random.nextInt());

        final List<Callable<Integer>> workers = new ArrayList<>();
        for (int i = 0; i < numOfProcesses; i++) {
            workers.add(new Reporter<Integer>(Level.SEVERE) {
                @Override
                public Integer subCall() throws Exception {
                    final CheckingStation.Instrument<OperationAggregator.TestResult> instrument = OperationAggregatorTest.this.instance.register(operation);
                    if (instrument == null) {
                        Thread.sleep(timeout / 2);
                        OperationAggregatorTest.this.instance.free(operation, result);
                        return 1;
                    } else {
                        final OperationAggregator.TestResult result2 = instrument.get(timeout);
                        Assert.assertEquals(result, result2);
                        return 0;
                    }
                }
            });
        }

        final ExecutorService executor = Executors.newFixedThreadPool(numOfProcesses);
        final long start = System.nanoTime();
        final List<Future<Integer>> futures = executor.invokeAll(workers);
        final long end = System.nanoTime();

        int sum = 0;
        for (final Future<Integer> future : futures) {
            sum += future.get();
        }

        Assert.assertEquals(1, sum);
        Assert.assertTrue(timeout / 2 <= (end - start) / 1_000_000);
        Assert.assertTrue((end - start) / 1_000_000 <= timeout);
    }

}
