package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.Formatter;

/**
 * Test that the CDM Index Creation works
 *
 * @author caron
 * @since 11/14/2014
 */
public class TestGribIndexCreation {
  private static CollectionUpdateType updateMode = CollectionUpdateType.always;

  @BeforeClass
  static public void before() {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    // GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.util.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();
  }

  @AfterClass
  static public void after() {
    GribIosp.setDebugFlags(new DebugFlagsImpl());
    Formatter out = new Formatter(System.out);

    FileCacheIF cache = GribCdmIndex.gribCollectionCache;
    if (cache != null) {
      cache.showTracking(out);
      cache.showCache(out);
      cache.clearCache(false);
    }

    FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
    if (rafCache != null) {
      rafCache.showCache(out);
    }

    System.out.printf("            countGC=%7d%n", GribCollectionImmutable.countGC);
    System.out.printf("            countPC=%7d%n", PartitionCollectionImmutable.countPC);
    System.out.printf("    countDataAccess=%7d%n", GribIosp.debugIndexOnlyCount);
    System.out.printf(" total files needed=%7d%n", GribCollectionImmutable.countGC + PartitionCollectionImmutable.countPC + GribIosp.debugIndexOnlyCount);

    FileCache.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
    TestDir.checkLeaks();
    RandomAccessFile.setDebugLeaks(false);
  }


  @Test
  public void testGdsHashChange() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("NDFD-CONUS_5km_conduit", "test/NDFD-CONUS_5km_conduit", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/gdsHashChange/.*grib2", null, null, "file", null, null);
    config.gribConfig.addGdsHash("-328645426", "1821883766");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    // LOOK add check that records were combined
  }


  @Test
  public void testCfrsAnalysisOnly() throws IOException {
     // this dataset 0-6 hour forecasts  x 124 runtimes (4x31)
    // there are  2 groups, likely miscoded, the smaller group are 0 hour,  duplicates, possibly miscoded
    FeatureCollectionConfig config = new FeatureCollectionConfig("cfrsAnalysis_46", "test/testCfrsAnalysisOnly", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/cfsr/.*grb2", null, null, "directory", null, null);
    // <gdsHash from="1450192070" to="1450218978"/>
    config.gribConfig.addGdsHash("1450192070", "1450218978");
    //config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testDgex() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("dgex_46", "test/dgex", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/dgex/**/.*grib2", null, null, "file", null, null);
    //config.gribConfig.useTableVersion = false;
    //config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testWwwCoastalAlaska() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("www_46", "test/www", FeatureCollectionType.GRIB2,
 //           TestDir.cdmUnitTestDir + "gribCollections/www/.*grib2",
            "B:/idd/WWW/Coastal_Alaska/.*grib2",
            null, null, "file", null, null);
    config.gribConfig.addGdsHash("-804803647", "-804803709");

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testGFSconus80() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("gfsConus80_46", "test/gfsConus80", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/**/.*grib1", null, null, "file", null, null);
    //config.gribConfig.useTableVersion = false;
    //config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  @Test
  public void testGFSglobalOnedeg() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("gfsOnedeg_46", "test/gfsOnedeg", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_onedeg/.*grib2", null, null, "file", null, null);
    //config.gribConfig.useTableVersion = false;
    //config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

  ////////////////

  @Test
  public void testRdvamds083p2_SampleMonth() throws IOException {
    //GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2-union", "test/GCpass1", FeatureCollectionType.GRIB1,
           TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/sampleMonth/.*grib1",
            null, null, "directory", null, null);
    config.gribConfig.useTableVersion = false;
    config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testRdvamds083p2() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds083.2-union", "test/ds083.2", FeatureCollectionType.GRIB1,
//            "B:/rdavm/ds083.2/grib1/**/.*gbx9",
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/grib1/**/.*gbx9",
            null, null, "directory", null, null);
    config.gribConfig.useTableVersion = false;
    config.gribConfig.unionRuntimeCoord = true;
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("always");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

  @Test
  public void testRdvamds627p0() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("ds627.0_46", "test/ds627.0", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.0/ei.oper.an.pv/**/.*gbx9", "#ei.oper.an.pv/#yyyyMM", null, "directory", null, null);

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }


  @Test   // has one file for for each month, all in same directory
  public void testRdvamds627p1() throws IOException {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config = new FeatureCollectionConfig("GCpass1-union", "test/GCpass1", FeatureCollectionType.GRIB1,
            TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.1/.*gbx9", null, null, "directory", null, null);
    config.gribConfig.useTableVersion = false;
    config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    GribIosp.setDebugFlags(new DebugFlagsImpl());
  }

}
