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

package io.whalebone.iprangerjava.tools;

import io.whalebone.iprangerjava.utils.CUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Tomas Kozel
 */
public class ByteBufferUtils {
    public static String toIpString(ByteBuffer bf) {
        try {
            return InetAddress.getByAddress(toArray(bf)).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot create ip from buffer", e);
        }
    }

    public static String toString(ByteBuffer bf) {
        return new String(toArray(bf), StandardCharsets.UTF_8);
    }

    public static String toStringFromCString(ByteBuffer bf) {
        return CUtils.fromCString(toString(bf));
    }

    public static String toByteString(ByteBuffer bf) {
        return String.valueOf(bf.get());
    }

    public static byte[] toArray(ByteBuffer bf) {
        byte[] bytes = new byte[bf.remaining()];
        bf.get(bytes);
        return bytes;
    }
}
