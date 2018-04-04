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
package com.qubole.rubix.spi;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestCacheUtil
{
  private static final String cacheTestDirPrefix = System.getProperty("java.io.tmpdir") + "/cacheUtilTest/";
  private static final int maxDisks = 5;

  private final Configuration conf = new Configuration();

  @BeforeClass
  public void initializeCacheDirectories() throws IOException
  {
    Files.createDirectories(Paths.get(cacheTestDirPrefix));
    for (int i = 0; i < maxDisks; i++) {
      Files.createDirectories(Paths.get(cacheTestDirPrefix, String.valueOf(i)));
    }
  }

  @BeforeMethod
  public void setUpConfiguration()
  {
    conf.clear();
  }

  @Test
  public void testCreateCacheDirectories_cacheParentDoesNotExist()
  {
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix + "doesNotExist/");
    CacheConfig.setCacheDataDirSuffix(conf, "/fcache/");
    CacheConfig.setMaxDisks(conf, maxDisks);

    CacheUtil.createCacheDirectories(conf);

    boolean cacheDirExists = Files.exists(Paths.get(cacheTestDirPrefix, "doesNotExist", "0", "/fcache"));

    assertFalse(cacheDirExists, "Cache directory should not be created!");
  }

  @Test
  public void testGetCacheDiskCount()
  {
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix);
    CacheConfig.setCacheDataDirSuffix(conf, "/fcache/");
    CacheConfig.setMaxDisks(conf, maxDisks);

    CacheUtil.createCacheDirectories(conf);

    int diskCount = CacheUtil.getCacheDiskCount(conf);
    assertEquals(diskCount, maxDisks, "Sizes don't match!");
  }

  @Test
  public void testGetDiskPathsMap()
  {
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix);
    CacheConfig.setCacheDataDirSuffix(conf, "/fcache/");
    CacheConfig.setMaxDisks(conf, maxDisks);

    CacheUtil.createCacheDirectories(conf);

    HashMap<Integer, String> diskPathsMap = CacheUtil.getCacheDiskPathsMap(conf);
    assertEquals(diskPathsMap.get(0), cacheTestDirPrefix + "0", "Sizes don't match!");
  }

  @Test
  public void testGetDirPath()
  {
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix);
    CacheConfig.setCacheDataDirSuffix(conf, "/fcache/");
    CacheConfig.setMaxDisks(conf, 2);

    CacheUtil.createCacheDirectories(conf);
    String dirPath = CacheUtil.getDirPath(1, conf);

    assertEquals(dirPath, cacheTestDirPrefix + "1", "Paths don't match");
  }

  @Test
  public void testGetLocalPath()
  {
    String localRelPath = "testbucket/123/4566/789";
    String remotePath = "s3://" + localRelPath;
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix);
    CacheConfig.setCacheDataDirSuffix(conf, "/fcache/");
    CacheConfig.setMaxDisks(conf, 1);

    CacheUtil.createCacheDirectories(conf);

    String localPath = CacheUtil.getLocalPath(remotePath, conf);
    assertEquals(localPath, cacheTestDirPrefix + "0" + "/fcache/" + localRelPath, "Paths not equal!");
  }

  @Test
  public void testGetLocalPath_noRemotePathScheme()
  {
    String localRelPath = "testbucket/123/4566/789";
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix);
    CacheConfig.setCacheDataDirSuffix(conf, "/fcache/");
    CacheConfig.setMaxDisks(conf, 1);

    CacheUtil.createCacheDirectories(conf);

    String localPath = CacheUtil.getLocalPath(localRelPath, conf);
    assertEquals(localPath, cacheTestDirPrefix + "0" + "/fcache/" + localRelPath, "Paths not equal!");
  }

  @Test
  public void testGetLocalPath_singleLevel()
  {
    String localRelPath = "testbucket";
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix);
    CacheConfig.setCacheDataDirSuffix(conf, "/fcache/");
    CacheConfig.setMaxDisks(conf, 1);

    CacheUtil.createCacheDirectories(conf);

    String localPath = CacheUtil.getLocalPath(localRelPath, conf);
    assertEquals(localPath, cacheTestDirPrefix + "0" + "/fcache//" + localRelPath, "Paths not equal!");
  }

  @Test
  public void testGetMetadataFilePath()
  {
    String localRelPath = "testbucket/123/456/789";
    String remotePath = "s3://" + localRelPath;
    CacheConfig.setCacheDataDirPrefix(conf, cacheTestDirPrefix);
    CacheConfig.setCacheDataDirSuffix(conf, "/fcache/");
    CacheConfig.setMaxDisks(conf, 1);

    CacheUtil.createCacheDirectories(conf);

    String localPath = CacheUtil.getMetadataFilePath(remotePath, conf);
    assertEquals(localPath, cacheTestDirPrefix + "0" + "/fcache/" + localRelPath + "_mdfile", "Paths not equal!");
  }

  @Test
  public void testSkipCache_cachingDisabled()
  {
    CacheConfig.setCacheDataEnabled(conf, false);
    Path testPath = new Path("/test/path");

    boolean skipCache = CacheUtil.skipCache(testPath, conf);

    assertTrue(skipCache, "Cache is not being skipped!");
  }

  @Test
  public void testSkipCache_LocationNotOnWhitelist()
  {
    CacheConfig.setCacheDataEnabled(conf, true);
    CacheConfig.setCacheDataLocationWhitelist(conf, "^((?!test).)*$");
    Path testPath = new Path("/test/path");

    boolean skipCache = CacheUtil.skipCache(testPath, conf);

    assertTrue(skipCache, "Cache is not being skipped!");
  }

  @Test
  public void testSkipCache_LocationOnBlackList()
  {
    CacheConfig.setCacheDataEnabled(conf, true);
    CacheConfig.setCacheDataLocationBlacklist(conf, ".*test.*");
    Path testPath = new Path("/test/path");

    boolean skipCache = CacheUtil.skipCache(testPath, conf);

    assertTrue(skipCache, "Cache is not being skipped!");
  }

  @Test
  public void testSkipCache_LocationOnWhitelistAndBlackList()
  {
    CacheConfig.setCacheDataEnabled(conf, true);
    CacheConfig.setCacheDataLocationWhitelist(conf, ".*test.*");
    CacheConfig.setCacheDataLocationBlacklist(conf, ".*test.*");
    Path testPath = new Path("/test/path");

    boolean skipCache = CacheUtil.skipCache(testPath, conf);

    assertTrue(skipCache, "Cache is not being skipped!");
  }

  @Test
  public void testSkipCache_tableNotAllowed()
  {
    CacheConfig.setCacheDataEnabled(conf, true);
    CacheConfig.setCacheDataLocationWhitelist(conf, ".*test.*");
    CacheConfig.setCacheDataTable(conf, "testTable");
    CacheConfig.setCacheDataTableWhitelist(conf, "^((?!testTable).)*$");
    Path testPath = new Path("/test/path");

    boolean skipCache = CacheUtil.skipCache(testPath, conf);

    assertTrue(skipCache, "Cache is not being skipped!");
  }

  @Test
  public void testSkipCache_noTableName()
  {
    CacheConfig.setCacheDataEnabled(conf, true);
    Path testPath = new Path("/test/path");

    boolean skipCache = CacheUtil.skipCache(testPath, conf);

    assertFalse(skipCache, "Cache is being skipped!");
  }

  @Test
  public void testSkipCache_minColsGreaterThanChosenColumns()
  {
    CacheConfig.setCacheDataEnabled(conf, true);
    CacheConfig.setCacheDataLocationWhitelist(conf, ".*test.*");
    CacheConfig.setCacheDataTable(conf, "testTable");
    CacheConfig.setCacheDataTableWhitelist(conf, ".*testTable.*");
    CacheConfig.setCacheDataMinColumns(conf, 5);
    CacheConfig.setCacheDataChosenColumns(conf, 3);
    Path testPath = new Path("/test/path");

    boolean skipCache = CacheUtil.skipCache(testPath, conf);

    assertTrue(skipCache, "Cache is not being skipped!");
  }

  @Test
  public void testSkipCache_cacheNotSkipped()
  {
    CacheConfig.setCacheDataEnabled(conf, true);
    CacheConfig.setCacheDataLocationWhitelist(conf, ".*test.*");
    CacheConfig.setCacheDataTable(conf, "testTable");
    CacheConfig.setCacheDataTableWhitelist(conf, ".*testTable.*");
    CacheConfig.setCacheDataMinColumns(conf, 3);
    CacheConfig.setCacheDataChosenColumns(conf, 5);
    Path testPath = new Path("/test/path");

    boolean skipCache = CacheUtil.skipCache(testPath, conf);

    assertFalse(skipCache, "Cache is being skipped!");
  }
}
