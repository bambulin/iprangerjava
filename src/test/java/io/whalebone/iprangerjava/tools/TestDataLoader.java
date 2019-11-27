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

import io.whalebone.iprangerjava.IpRanger;
import io.whalebone.iprangerjava.IpRangerTest;
import org.lmdbjava.Dbi;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * @author Tomas Kozel
 */
public class TestDataLoader {
    public static void load(String fileName, String delimiter, IpRanger ipRanger) throws Exception {
        URI dataLocation = IpRangerTest.class.getClassLoader().getResource(fileName).toURI();
        try (Stream<String> lines = Files.lines(Paths.get(dataLocation))) {
            // skip header
            lines.skip(1).forEach(line -> {
                String[] args = line.split(delimiter);
                // expecting iprange in third column and identity in second column
                ipRanger.insertIpRange(args[2].trim(), args[1].trim());
            });
        }
    }
}
