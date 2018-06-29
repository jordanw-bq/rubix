/**
 * Copyright (c) 2016. Qubole Inc
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. See accompanying LICENSE file.
 */

package com.qubole.rubix.bookkeeper;

import com.codahale.metrics.MetricRegistry;
import com.qubole.rubix.spi.BookKeeperFactory;
import com.qubole.rubix.spi.CacheConfig;
import com.qubole.rubix.spi.RetryingBookkeeperClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.thrift.shaded.transport.TSocket;
import org.apache.thrift.shaded.transport.TTransportException;
import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestWorkerBookKeeper
{
  private static final Log log = LogFactory.getLog(TestBookKeeperServer.class.getName());
  private static final String cacheTestDirPrefix = System.getProperty("java.io.tmpdir") + "/workerBookKeeperTest/";

  private final Configuration conf = new Configuration();

  @BeforeClass
  public void initializeCacheDirectories() throws IOException
  {
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix);

    Files.createDirectories(Paths.get(cacheTestDirPrefix));
    for (int i = 0; i < CacheConfig.getCacheMaxDisks(conf); i++) {
      Files.createDirectories(Paths.get(cacheTestDirPrefix, String.valueOf(i)));
    }
  }

  /**
   * Verify that WorkerBookKeeper throws the correct exception when asked to handle heartbeats.
   *
   * @throws FileNotFoundException if the parent directory for the cache cannot be found when initializing the BookKeeper.
   * @throws InterruptedException if the current thread is interrupted while starting the BookKeeper server.
   */
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testHandleHeartbeat() throws FileNotFoundException, InterruptedException
  {
    CacheConfig.setServiceRetryInterval(conf, 2000);
    CacheConfig.setOnMaster(conf, true);

    startBookKeeperServer();
    final WorkerBookKeeper workerBookKeeper = new WorkerBookKeeper(conf, new MetricRegistry());
    workerBookKeeper.handleHeartbeat("");
  }

  /**
   * Verify that the heartbeat service correctly makes a connection using a BookKeeper client.
   *
   * @throws TTransportException if the BookKeeper client cannot be created.
   * @throws InterruptedException if the current thread is interrupted while starting the BookKeeper server.
   */
  @Test
  public void testHeartbeatRetryLogic_noRetriesNeeded() throws TTransportException, InterruptedException
  {
    final BookKeeperFactory bookKeeperFactory = mock(BookKeeperFactory.class);
    CacheConfig.setServiceRetryInterval(conf, 2000);
    CacheConfig.setOnMaster(conf, true);

    when(bookKeeperFactory.createBookKeeperClient(anyString(), ArgumentMatchers.<Configuration>any())).thenReturn(
        new RetryingBookkeeperClient(
            new TSocket("localhost", CacheConfig.getServerPort(conf), CacheConfig.getClientTimeout(conf)),
            CacheConfig.getMaxRetries(conf)));

    startBookKeeperServer();
    final WorkerBookKeeper.HeartbeatService heartbeatService = new WorkerBookKeeper.HeartbeatService(conf, bookKeeperFactory);
  }

  /**
   * Verify that the heartbeat service correctly makes a connection using a BookKeeper client after a number of retries.
   */
  @Test
  public void testHeartbeatRetryLogic_connectAfterRetries()
  {
    final int retryInterval = 500;
    CacheConfig.setServiceRetryInterval(conf, retryInterval);
    CacheConfig.setServiceMaxRetries(conf, 10);
    CacheConfig.setOnMaster(conf, true);

    startBookKeeperServerWithDelay(retryInterval * 5);

    final WorkerBookKeeper.HeartbeatService heartbeatService = new WorkerBookKeeper.HeartbeatService(conf, new BookKeeperFactory());
  }

  /**
   * Verify that the heartbeat service no longer attempts to connect once it runs out of retry attempts.
   *
   * @throws TTransportException if the BookKeeper client cannot be created.
   * @throws InterruptedException if the current thread is interrupted while starting the BookKeeper server.
   */
  @Test(expectedExceptions = RuntimeException.class)
  public void testHeartbeatRetryLogic_outOfRetries() throws TTransportException, InterruptedException
  {
    final BookKeeperFactory bookKeeperFactory = mock(BookKeeperFactory.class);
    CacheConfig.setServiceRetryInterval(conf, 1000);
    CacheConfig.setServiceMaxRetries(conf, 3);
    CacheConfig.setOnMaster(conf, true);

    when(bookKeeperFactory.createBookKeeperClient(anyString(), ArgumentMatchers.<Configuration>any())).thenThrow(TTransportException.class);

    startBookKeeperServer();
    final WorkerBookKeeper.HeartbeatService heartbeatService = new WorkerBookKeeper.HeartbeatService(conf, bookKeeperFactory);
  }

  /**
   * Start an instance of the BookKeeper server.
   *
   * @throws InterruptedException if the current thread is interrupted while sleeping.
   */
  private void startBookKeeperServer() throws InterruptedException
  {
    final Thread thread = new Thread()
    {
      public void run()
      {
        BookKeeperServer.startServer(conf, new MetricRegistry());
      }
    };
    thread.start();

    while (!BookKeeperServer.isServerUp()) {
      Thread.sleep(200);
      log.info("Waiting for BookKeeper Server to come up");
    }
  }

  /**
   * Start an instance of the BookKeeper server with an initial delay.
   */
  private void startBookKeeperServerWithDelay(final int initialDelay)
  {
    final Thread thread = new Thread()
    {
      public void run()
      {
        try {
          Thread.sleep(initialDelay);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        System.out.println("BKS started!");
        BookKeeperServer.startServer(conf, new MetricRegistry());
      }
    };
    thread.start();
  }

  /**
   * Stop the currently running BookKeeper server instance.
   *
   * @throws InterruptedException if the current thread is interrupted while sleeping.
   */
  private void stopBookKeeperServer() throws InterruptedException
  {
    BookKeeperServer.stopServer();
    while (BookKeeperServer.isServerUp()) {
      Thread.sleep(200);
      log.info("Waiting for BookKeeper Server to shut down");
    }
  }
}
