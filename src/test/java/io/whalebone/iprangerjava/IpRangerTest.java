/*
 *   Copyright 2019 contributors to elastic2lmdb
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.whalebone.iprangerjava;


import io.whalebone.iprangerjava.tools.BigIntegerNormalizer;
import io.whalebone.iprangerjava.tools.ByteBufferUtils;
import io.whalebone.iprangerjava.tools.TestDataLoader;
import io.whalebone.iprangerjava.utils.CUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.lmdbjava.CursorIterator;
import org.lmdbjava.Dbi;
import org.lmdbjava.Env;
import org.lmdbjava.KeyRange;
import org.lmdbjava.Txn;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lmdbjava.DbiFlags.MDB_CREATE;
import static org.lmdbjava.DbiFlags.MDB_DUPSORT;

/**
 * @author Tomas Kozel
 */
public class IpRangerTest {
    private static final Logger logger = Logger.getLogger(IpRangerTest.class.getName());
    private static IpRanger ipRanger;
    private static Env<ByteBuffer> env;
    private static Dbi<ByteBuffer> ip4RangesToIdentity;
    private static Dbi<ByteBuffer> identitiesToIp4Ranges;
    private static Dbi<ByteBuffer> ip4Masks;
    private static Dbi<ByteBuffer> ip6RangesToIdentity;
    private static Dbi<ByteBuffer> identitiesToIp6Ranges;
    private static Dbi<ByteBuffer> ip6Masks;

    @BeforeAll
    public static void setUp() throws Exception {
        Path dir = Files.createTempDirectory("ipranger_" + System.currentTimeMillis());
        logger.log(Level.INFO, "DB dir is: " + dir);
        Configuration conf = new Configuration.ConfigurationBuilder()
                .maxEnvSize(1_000_000)
                .ip4RangesToIdentityDbName("IPv4")
                .identitiesToIp4RangesDbName("ID2IPv4")
                .ip4MasksDbName("IPv4_masks")
                .ip6RangesToIdentityDbName("IPv6")
                .ip6MasksDbName("IPv6_masks")
                .identitiesToIp6RangesDbName("ID2IPv6")
                .maxMaskKeySize(4)
                .maxIdentityKeySize(32)
                .build();
        ipRanger = IpRanger.create(dir, conf);
        TestDataLoader.load("ipv4.csv", ",", ipRanger);
        TestDataLoader.load("ipv6.csv", ",", ipRanger);
        ipRanger.close();
        env = Env.create()
                .setMapSize(1_000_000)
                .setMaxDbs(6)
                .open(dir.toFile());
        ip4RangesToIdentity = env.openDbi(conf.getIp4RangesToIdentityDbName(), MDB_CREATE);
        identitiesToIp4Ranges = env.openDbi(conf.getIdentitiesToIp4RangesDbName(), MDB_CREATE, MDB_DUPSORT);
        ip4Masks = env.openDbi(conf.getIp4MasksDbName(), MDB_CREATE);
        ip6RangesToIdentity = env.openDbi(conf.getIp6RangesToIdentityDbName(), MDB_CREATE);
        identitiesToIp6Ranges = env.openDbi(conf.getIdentitiesToIp6RangesDbName(), MDB_CREATE, MDB_DUPSORT);
        ip6Masks = env.openDbi(conf.getIp6MasksDbName(), MDB_CREATE);
        System.out.println("--------------- ipV4 ranges to identities ---------------");
        dbDump(ip4RangesToIdentity, ByteBufferUtils::toIpString, ByteBufferUtils::toStringFromCString);
        System.out.println("--------------- identities to ipV4 ranges ---------------");
        dbDump(identitiesToIp4Ranges, ByteBufferUtils::toStringFromCString, ByteBufferUtils::toIpString);
        System.out.println("--------------- ipV4 masks ------------------------------");
        dbDump(ip4Masks, ByteBufferUtils::toStringFromCString, ByteBufferUtils::toStringFromCString);
        System.out.println("--------------- ipV6 ranges to identities ---------------");
        dbDump(ip6RangesToIdentity, ByteBufferUtils::toIpString, ByteBufferUtils::toStringFromCString);
        System.out.println("--------------- identities to ipV6 ranges ---------------");
        dbDump(identitiesToIp6Ranges, ByteBufferUtils::toStringFromCString, ByteBufferUtils::toIpString);
        System.out.println("--------------- ipV6 masks ------------------------------");
        dbDump(ip6Masks, ByteBufferUtils::toStringFromCString, ByteBufferUtils::toStringFromCString);
    }

    @AfterAll
    public static void tearDown() {
        ipRanger.close();
        env.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/ipv4.csv", numLinesToSkip = 1)
    public void ip4ToIdentitiesTest(String expectedUnsignedIp, String identity) throws Exception {
        ByteBuffer ipKey = ByteBuffer.allocateDirect(4);
        ipKey.putInt(Integer.parseUnsignedInt(expectedUnsignedIp)).flip();
        ByteBuffer foundIdentityVal = get(ip4RangesToIdentity, ipKey);
        assertThat(foundIdentityVal, is(notNullValue()));
        String foundIdentity = new String(ByteBufferUtils.toArray(foundIdentityVal), StandardCharsets.UTF_8);
        assertThat(foundIdentity, is(CUtils.toCString(identity)));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/ipv4.csv", numLinesToSkip = 1)
    public void ip4MasksTest(String irrelevant1, String irrelevant2, String irrelevant3, String expectedUnsignedMask) throws Exception {
        String mask = CUtils.toCString(expectedUnsignedMask.trim());
        ByteBuffer maskKey = ByteBuffer.allocateDirect(32);
        maskKey.put(mask.getBytes(StandardCharsets.UTF_8)).flip();
        ByteBuffer foundMaskVal = get(ip4Masks, maskKey);
        assertThat(foundMaskVal, is(notNullValue()));
        String maskVal = ByteBufferUtils.toString(foundMaskVal);
        assertThat(maskVal, is(mask));
    }

    @ParameterizedTest()
    @CsvFileSource(resources = "/ipv6.csv", numLinesToSkip = 1)
    public void ip6ToIdentitiesTest(String expectedUnsignedIp, String identity) throws Exception {
        ByteBuffer ipKey = ByteBuffer.allocateDirect(16);
        BigInteger b = new BigInteger(expectedUnsignedIp);
        ipKey.put(BigIntegerNormalizer.unsignedBigEndian(b.toByteArray(), 16)).flip();
        ByteBuffer foundIdentityVal = get(ip6RangesToIdentity, ipKey);
        assertThat(foundIdentityVal, is(notNullValue()));
        String foundIdentity = new String(ByteBufferUtils.toArray(foundIdentityVal), StandardCharsets.UTF_8);
        assertThat(foundIdentity, is(CUtils.toCString(identity)));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/ipv6.csv", numLinesToSkip = 1)
    public void ip6MasksTest(String irrelevant1, String irrelevant2, String irrelevant3, String expectedUnsignedMask) throws Exception {
        String mask = CUtils.toCString(expectedUnsignedMask.trim());
        ByteBuffer maskKey = ByteBuffer.allocateDirect(32);
        maskKey.put(mask.getBytes(StandardCharsets.UTF_8)).flip();
        ByteBuffer foundMaskVal = get(ip6Masks, maskKey);
        assertThat(foundMaskVal, is(notNullValue()));
        String maskVal = ByteBufferUtils.toString(foundMaskVal);
        assertThat(maskVal, is(mask));
    }

    @Test
    public void getDataFileTest() {
        Path dataFile = ipRanger.getDataFile();
        assertTrue(dataFile.endsWith(Paths.get("data.mdb")));
    }

    private ByteBuffer get(Dbi<ByteBuffer> db, ByteBuffer key) throws Exception {
        try (Txn<ByteBuffer> txn = env.txnRead()) {
            return db.get(txn, key);
        }
    }

    private static void dbDump(Dbi<ByteBuffer> db,
                        Function<ByteBuffer, String> keyProducer,
                        Function<ByteBuffer, String> valProducer
    ) {
        Txn<ByteBuffer> txn = env.txnRead();
        try (CursorIterator<ByteBuffer> it = db.iterate(txn, KeyRange.all())) {
            for (final CursorIterator.KeyVal<ByteBuffer> kv : it.iterable()) {
                String key = keyProducer.apply(kv.key());
                String val = valProducer.apply(kv.val());
                System.out.println("key: " + key + " \tvalue: " + val);
            }
        }
        txn.close();
    }
}
