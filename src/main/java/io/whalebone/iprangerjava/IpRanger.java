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

import io.whalebone.iprangerjava.utils.CIDRUtils;
import io.whalebone.iprangerjava.utils.CUtils;
import org.lmdbjava.Dbi;
import org.lmdbjava.Env;
import org.lmdbjava.PutFlags;
import org.lmdbjava.Txn;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import static org.lmdbjava.DbiFlags.MDB_CREATE;
import static org.lmdbjava.DbiFlags.MDB_DUPSORT;

/**
 * @author Tomas Kozel
 */
public class IpRanger implements Closeable {
    private static final String DATA_FILE = "data.mdb";
    private static final int IPV4_SIZE = 4;
    private static final int IPV6_SIZE = 16;

    private Path dir;
    private Env<ByteBuffer> env;
    private Dbi<ByteBuffer> ip4RangesToIdentity;   // 1 : 1
    private Dbi<ByteBuffer> identitiesToIp4Ranges; // 1 : n
    private Dbi<ByteBuffer> ip4Masks;              // 1 : 1
    private Dbi<ByteBuffer> ip6RangesToIdentity;    // 1 : 1
    private Dbi<ByteBuffer> identitiesToIp6Ranges;  // 1 : n
    private Dbi<ByteBuffer> ip6Masks;              // 1 : 1
    private Configuration conf;

    public static IpRanger create(final Path dir, final Configuration conf) {
        final IpRanger ipRanger = new IpRanger(dir, conf);
        ipRanger.open();
        return ipRanger;
    }

    private IpRanger(final Path dir, final Configuration configuration) {
        Objects.requireNonNull(dir, "Dir can't be null and must exist");
        Objects.requireNonNull(configuration, "Configuration can't be null");
        this.dir = dir;
        this.conf = configuration;
    }

    public Path getDir() {
        return dir;
    }

    public Path getDataFile() {
        return dir.resolve(DATA_FILE);
    }

    private void open() {
        env = Env.create()
                // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                .setMapSize(conf.getMaxEnvSize())
                // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                .setMaxDbs(6)
                // Now let's open the Env. The same path can be concurrently opened andgetDb
                // used in different processes, but do not open the same path twice in
                // the same process at the same time.
                .open(dir.toFile());

        validateKeySize(IPV4_SIZE, "ipv4");
        validateKeySize(IPV6_SIZE, "ipv6");
        validateKeySize(conf.getMaxMaskKeySize(), "mask");
        validateKeySize(conf.getMaxIdentityKeySize(), "identity");

        // We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
        // MDB_CREATE flag causes the DB to be created if it doesn't already exist.
        ip4RangesToIdentity = env.openDbi(conf.getIp4RangesToIdentityDbName(), MDB_CREATE);
        // MDB_DUPSORT for duplicate keys (or multiple values for one key)
        identitiesToIp4Ranges = env.openDbi(conf.getIdentitiesToIp4RangesDbName(), MDB_CREATE, MDB_DUPSORT);
        ip4Masks = env.openDbi(conf.getIp4MasksDbName(), MDB_CREATE);
        ip6RangesToIdentity = env.openDbi(conf.getIp6RangesToIdentityDbName(), MDB_CREATE);
        // MDB_DUPSORT for duplicate keys (or multiple values for one key)
        identitiesToIp6Ranges = env.openDbi(conf.getIdentitiesToIp6RangesDbName(), MDB_CREATE, MDB_DUPSORT);
        ip6Masks = env.openDbi(conf.getIp6MasksDbName(), MDB_CREATE);
    }

    public void insertIpRange(final String ipRange, final String identity) {
        try {
            final CIDRUtils cidr = new CIDRUtils(ipRange.trim());
            final int mask = cidr.getMask();
            final byte[] maskBytes = CUtils.toCString(Integer.toString(mask)).getBytes(StandardCharsets.UTF_8);
            if (maskBytes.length > conf.getMaxMaskKeySize()) {
                throw new IllegalArgumentException("Mask as a string is too long (as a C string with null ending). " +
                        "Mask (including ending NULL char) cannot be longer than " + conf.getMaxMaskKeySize());
            }
            final byte[] identityBytes = CUtils.toCString(identity.trim()).getBytes(StandardCharsets.UTF_8);
            if (identityBytes.length > conf.getMaxIdentityKeySize()) {
                throw new IllegalArgumentException("Identity is too long (as a C string with null ending). " +
                        "Identity (including ending NULL char) cannot be longer than " + conf.getMaxIdentityKeySize());
            }
            final byte[] endAddressBytes = cidr.getEndAddress().getAddress();
            insertIntoIpToIdentities(endAddressBytes, identityBytes);
            insertIntoIdentityToIps(identityBytes, endAddressBytes);
            insertIntoMasks(maskBytes, endAddressBytes.length == IPV6_SIZE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unparsable ip range " + ipRange +
                    " of identity " + identity, e);
        }
    }

    private void insertIntoIpToIdentities(final byte[] ipAddressBytes, final byte[] identityBytes) {
        Dbi<ByteBuffer> db = ipAddressBytes.length == IPV6_SIZE ? ip6RangesToIdentity : ip4RangesToIdentity;
        final ByteBuffer key = ByteBuffer.allocateDirect(ipAddressBytes.length);
        key.put(ipAddressBytes).flip();
        final ByteBuffer value = ByteBuffer.allocateDirect(identityBytes.length);
        value.put(identityBytes).flip();
        db.put(key, value);
    }

    private void insertIntoIdentityToIps(final byte[] identityBytes, final byte[] ipAddressBytes) {
        Dbi<ByteBuffer> db = ipAddressBytes.length == IPV6_SIZE ? identitiesToIp6Ranges : identitiesToIp4Ranges;
        final ByteBuffer key = ByteBuffer.allocateDirect(identityBytes.length);
        key.put(identityBytes).flip();
        final ByteBuffer value = ByteBuffer.allocateDirect(ipAddressBytes.length);
        value.put(ipAddressBytes).flip();
        db.put(key, value);
    }

    private void insertIntoMasks(final byte[] maskBytes, final boolean ipv6) {
        final ByteBuffer key = ByteBuffer.allocateDirect(maskBytes.length);
        key.put(maskBytes).flip();
        final ByteBuffer value = ByteBuffer.allocateDirect(maskBytes.length);
        value.put(maskBytes).flip();
        Dbi<ByteBuffer> db = ipv6 ? ip6Masks : ip4Masks;
        try (Txn<ByteBuffer> txn = env.txnWrite()) {
            // don't overwrite the same values
            db.put(txn, key, value, PutFlags.MDB_NOOVERWRITE);
            txn.commit();
        }
    }

    public void close() {
        if (env != null) {
            env.close();
        }
    }

    private void validateKeySize(int keySize, String keyName) {
        if (env.getMaxKeySize() < keySize) {
            throw new IllegalStateException("The size " + keySize + " + of " + keyName + " cannot be bigger than " + env.getMaxKeySize());
        }
    }
}
