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


import org.junit.jupiter.api.Test;

/**
 * @author Tomas Kozel
 */
public class IpRangerTest {
    @Test
    public void test() {
        IpRanger ipRanger = IpRanger.getInstance();
        int i = ipRanger.initDb("/Users/bambula/ipranger/lmdb", false);
        System.out.println(i);
    }
}
